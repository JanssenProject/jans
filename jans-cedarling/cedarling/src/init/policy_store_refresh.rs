// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Background refresh worker for remote policy store sources.
//!
//! On each tick the worker sends a conditional GET to the configured URL,
//! handling `304 Not Modified` cheaply. When the upstream returns a new body
//! and it parses successfully, the worker builds a new [`Authz`] (reusing the
//! existing [`JwtService`] when `trusted_issuers` is unchanged) and publishes it via
//! [`ArcSwap`] so in-flight authorizations are unaffected. Any failure leaves
//! the previously loaded [`Authz`] in place.
//!
//! The worker observes a shutdown signal from a [`futures::channel::oneshot`]
//! receiver — dropping the [`PolicyStoreRefreshHandle`] closes the channel and
//! the worker exits at its next select boundary.

use ahash::AHasher;
use arc_swap::ArcSwap;
use chrono::{DateTime, Utc};
use futures::channel::oneshot;
use futures::future::{Either, select};
use std::hash::Hasher;
use std::sync::Arc;
use std::sync::atomic::{AtomicU64, Ordering};
use std::time::Duration;

use crate::PolicyStoreConfig;
use crate::PolicyStoreSource;
use crate::async_sleep::sleep;
use crate::authz::Authz;
use crate::authz::metrics::MetricsCollector;
use crate::bootstrap_config::{AuthorizationConfig, JwtConfig};
use crate::common::policy_store::{PolicyStoreWithID, TrustedIssuer};
use crate::context_data_api::DataStore;
use crate::http::cache_headers::CacheHeadersState;
use crate::http::{ConditionalFetch, HeadOutcome, HttpClient};
use crate::http::{JoinHandle, spawn_task};
use crate::log::interface::LogWriter;
use crate::log::{BaseLogEntry, LogEntry, LogLevel, Logger};

use super::authz_builder::{BuildAuthzError, build_authz};

use super::policy_store::{
    PolicyStoreLoadError, ZIP_MAGIC, parse_cjar_bytes, parse_lock_master_bytes,
};

/// Upper bound on exponential backoff between failed refresh attempts.
const REFRESH_FAILURE_BACKOFF_MAX_SECS: u64 = 600;

/// Consecutive unhelpful ticks before degrading to a less-efficient strategy.
const STRATEGY_DEGRADE_THRESHOLD: u32 = 3;

/// Minimum seconds between upgrade-probe attempts after a degrade.
const STRATEGY_REPROBE_INTERVAL_SECS: i64 = 1800;

/// Outcome of a single refresh tick. Encoded as `i64`; `0` is reserved
/// for "no attempt yet" so it doesn't alias with `Success`.
#[repr(i64)]
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum RefreshOutcome {
    Success = 1,
    NotModified = 2,
    HttpError = 3,
    NetworkError = 4,
    /// Body fetched but failed to parse (malformed JSON, broken archive, etc.).
    ParseError = 5,
    /// Body parsed but service rebuild failed (issuer validation, JWT init, etc.).
    RebuildError = 6,
    /// Response received but body read failed (TCP drop, decode error, etc.).
    DecodeError = 7,
}

impl RefreshOutcome {
    pub(crate) fn metric_value(self) -> i64 {
        self as i64
    }
}

/// Refresh strategy ladder: `Conditional → HeadThenGet → PlainGet`.
/// Degrades when the upstream doesn't honor the current mode; periodic probes
/// attempt to upgrade back. Encoded as `i64` for the `strategy_current` metric.
#[repr(i64)]
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum RefreshStrategy {
    /// Conditional GET (`If-None-Match` / `If-Modified-Since`); expects `304`.
    Conditional = 1,
    /// HEAD to compare validators; full GET only when they differ.
    HeadThenGet = 2,
    /// Plain GET every tick; falls back to body-hash comparison.
    PlainGet = 3,
}

impl RefreshStrategy {
    pub(crate) fn metric_value(self) -> i64 {
        self as i64
    }
}

/// Per-source strategy tracking and transition counters.
struct StrategyState {
    current: RefreshStrategy,
    /// Consecutive ticks where the current strategy didn't help.
    degraded_count: u32,
    last_probe_at: Option<DateTime<Utc>>,
    conditional_to_head_transitions: u32,
    head_to_plain_transitions: u32,
    upgrade_to_head_transitions: u32,
    upgrade_to_conditional_transitions: u32,
}

impl Default for StrategyState {
    fn default() -> Self {
        Self {
            current: RefreshStrategy::Conditional,
            degraded_count: 0,
            last_probe_at: None,
            conditional_to_head_transitions: 0,
            head_to_plain_transitions: 0,
            upgrade_to_head_transitions: 0,
            upgrade_to_conditional_transitions: 0,
        }
    }
}

struct StrategyChoice {
    strategy: RefreshStrategy,
    /// `Some(target)` when this is an upgrade probe; `None` for steady-state.
    probe_target: Option<RefreshStrategy>,
}

impl StrategyState {
    /// Choose the strategy for this tick. Probes climb one rung at a time:
    /// `PlainGet` → `HeadThenGet`, `HeadThenGet` → `Conditional`.
    fn choose_for_tick(&mut self) -> StrategyChoice {
        if self.current == RefreshStrategy::Conditional {
            return StrategyChoice {
                strategy: RefreshStrategy::Conditional,
                probe_target: None,
            };
        }
        let now = Utc::now();
        let due = self.last_probe_at.is_none_or(|t| {
            now.signed_duration_since(t).num_seconds() >= STRATEGY_REPROBE_INTERVAL_SECS
        });
        if due {
            self.last_probe_at = Some(now);
            let target = match self.current {
                RefreshStrategy::HeadThenGet => RefreshStrategy::Conditional,
                RefreshStrategy::PlainGet => RefreshStrategy::HeadThenGet,
                RefreshStrategy::Conditional => unreachable!(),
            };
            StrategyChoice {
                strategy: target,
                probe_target: Some(target),
            }
        } else {
            StrategyChoice {
                strategy: self.current,
                probe_target: None,
            }
        }
    }

    /// Current strategy didn't help; degrade when the threshold is reached.
    fn record_degraded(&mut self) {
        self.degraded_count = self.degraded_count.saturating_add(1);
        if self.degraded_count < STRATEGY_DEGRADE_THRESHOLD {
            return;
        }
        self.degraded_count = 0;
        self.degrade_one_step();
    }

    fn record_helped(&mut self) {
        self.degraded_count = 0;
    }

    /// Immediate downgrade, bypassing the threshold (e.g. HEAD `405`).
    fn force_degrade(&mut self) {
        self.degraded_count = 0;
        self.degrade_one_step();
    }

    fn degrade_one_step(&mut self) {
        match self.current {
            RefreshStrategy::Conditional => {
                self.current = RefreshStrategy::HeadThenGet;
                self.conditional_to_head_transitions =
                    self.conditional_to_head_transitions.saturating_add(1);
                self.last_probe_at = Some(Utc::now());
            },
            RefreshStrategy::HeadThenGet => {
                self.current = RefreshStrategy::PlainGet;
                self.head_to_plain_transitions = self.head_to_plain_transitions.saturating_add(1);
                self.last_probe_at = Some(Utc::now());
            },
            RefreshStrategy::PlainGet => {},
        }
    }

    fn record_probe_success(&mut self) {
        if self.current != RefreshStrategy::Conditional {
            self.current = RefreshStrategy::Conditional;
            self.degraded_count = 0;
            self.upgrade_to_conditional_transitions =
                self.upgrade_to_conditional_transitions.saturating_add(1);
        }
    }

    fn record_head_probe_success(&mut self) {
        if self.current == RefreshStrategy::PlainGet {
            self.current = RefreshStrategy::HeadThenGet;
            self.upgrade_to_head_transitions = self.upgrade_to_head_transitions.saturating_add(1);
        }
    }
}

/// Captures everything the refresh worker needs to rebuild a complete [`Authz`]
/// from a freshly loaded [`PolicyStoreWithID`].
pub(crate) struct AuthzRebuilder {
    pub(crate) jwt_config: JwtConfig,
    pub(crate) authorization_config: AuthorizationConfig,
    pub(crate) http_client: HttpClient,
    pub(crate) log: Logger,
    pub(crate) data_store: Arc<DataStore>,
    pub(crate) metrics: Arc<MetricsCollector>,
}

impl AuthzRebuilder {
    /// Build a new [`Authz`] from `policy_store`, reusing `prior_authz`'s
    /// [`JwtService`] when `trusted_issuers` is unchanged (avoids OIDC/JWKS
    /// refetch on policy-only updates). [`EntityBuilder`] is always rebuilt.
    pub(crate) async fn rebuild(
        &self,
        policy_store: PolicyStoreWithID,
        prior_authz: &Authz,
    ) -> Result<Authz, RebuildError> {
        let prior_jwt = if issuers_unchanged(
            prior_authz.trusted_issuers(),
            policy_store.trusted_issuers.as_ref(),
        ) {
            Some(prior_authz.clone_jwt_service())
        } else {
            None
        };

        build_authz(
            policy_store,
            &self.jwt_config,
            &self.authorization_config,
            self.http_client.clone(),
            &self.log,
            self.data_store.clone(),
            self.metrics.clone(),
            prior_jwt,
        )
        .await
    }
}

/// `true` when both issuer maps are equal; `false` triggers a [`JwtService`] rebuild.
fn issuers_unchanged(
    prior: Option<&std::collections::HashMap<String, TrustedIssuer>>,
    new: Option<&std::collections::HashMap<String, TrustedIssuer>>,
) -> bool {
    prior == new
}

pub(crate) type RebuildError = BuildAuthzError;

/// Mutable per-source state — what we learned from the previous response.
#[derive(Default)]
struct RefreshState {
    validators: CacheHeadersState,
    last_body_hash: Option<u64>,
    consecutive_failures: u32,
    strategy: StrategyState,
}

impl RefreshState {
    /// Returns the delay before the next refresh attempt. Starts from
    /// `base_secs`; a server `Cache-Control: max-age` / `Expires` hint may
    /// *shorten* the next interval but never extends it. After consecutive
    /// failures the delay is exponentially backed off, capped at
    /// [`REFRESH_FAILURE_BACKOFF_MAX_SECS`]. Result is floored at
    /// [`PolicyStoreConfig::MIN_REFRESH_INTERVAL_SECS`] and gets ±10% jitter.
    fn next_delay(&self, base_secs: u64) -> Duration {
        let min_secs = PolicyStoreConfig::MIN_REFRESH_INTERVAL_SECS;
        let base_secs = base_secs.max(min_secs);

        let server_fresh = self.validators.fresh_for.map(|d| d.as_secs());

        let mut secs = match server_fresh {
            Some(s) if s > 0 => s.min(base_secs),
            _ => base_secs,
        };

        if self.consecutive_failures > 0 {
            // Cap the doubling at 2^10 = 1024× so we don't overflow u64; the
            // overall multiplied value is also clamped to
            // REFRESH_FAILURE_BACKOFF_MAX_SECS below.
            let shift = self.consecutive_failures.min(10);
            let exp = 1u64.checked_shl(shift).unwrap_or(u64::MAX);
            secs = secs
                .saturating_mul(exp)
                .min(REFRESH_FAILURE_BACKOFF_MAX_SECS);
        }

        let secs = secs.max(min_secs);
        let jitter_pct = jitter_pct();
        let secs_i128 = i128::from(secs);
        let adjusted = secs_i128 + (secs_i128 * jitter_pct / 100);
        let adjusted = adjusted.max(i128::from(min_secs));
        // Adjusted is bounded by `[min_secs, secs * 110/100]`, both of which fit
        // in u64 since `secs` is itself a u64.
        Duration::from_secs(u64::try_from(adjusted).unwrap_or(min_secs))
    }
}

/// Deterministic but desynchronized jitter in `[-10, +10]`, derived from a
/// process-local counter so we avoid taking a `rand` dependency in WASM builds.
fn jitter_pct() -> i128 {
    static COUNTER: AtomicU64 = AtomicU64::new(0);
    let n = COUNTER.fetch_add(1, Ordering::Relaxed);
    i128::from(n % 21) - 10
}

/// Hash policy-store body bytes for within-process equality comparisons
/// (refresh-worker short-circuit + initial-load seed). Uses `ahash` —
/// process-local seeds, deterministic within the process, not cryptographically
/// strong nor stable across processes (we don't need either).
pub(crate) fn body_hash(bytes: &[u8]) -> u64 {
    let mut h = AHasher::default();
    h.write(bytes);
    h.finish()
}

/// `true` when `new_hash` matches the last body hash recorded in `state` —
/// meaning the upstream returned a byte-identical body and the worker can skip
/// parse / rebuild / swap. Returns `false` on the first tick of the worker
/// **unless** `RefreshState.last_body_hash` was seeded from the initial
/// bootstrap load (see `WorkerContext::initial_body_hash`). Extracted so
/// `tick_conditional`, `tick_plain_get`, and the unit tests share one
/// definition of the short-circuit decision.
fn should_short_circuit(new_hash: u64, state: &RefreshState) -> bool {
    Some(new_hash) == state.last_body_hash
}

/// Identifies which kind of URL-based source we are refreshing — the parse step
/// differs (JSON for Lock Master, ZIP archive for `.cjar`).
#[derive(Debug, Clone)]
pub(crate) enum RefreshSource {
    LockServer { url: String },
    CjarUrl { url: String },
    Uri { url: String },
}

impl RefreshSource {
    pub(crate) fn from_policy_store_source(src: &PolicyStoreSource) -> Option<Self> {
        match src {
            PolicyStoreSource::LockServer(u) => Some(Self::LockServer { url: u.clone() }),
            PolicyStoreSource::CjarUrl(u) => Some(Self::CjarUrl { url: u.clone() }),
            PolicyStoreSource::Uri(u) => Some(Self::Uri { url: u.clone() }),
            _ => None,
        }
    }

    fn url(&self) -> &str {
        match self {
            Self::LockServer { url } | Self::CjarUrl { url } | Self::Uri { url } => url,
        }
    }

    async fn parse(
        &self,
        bytes: &[u8],
        strict_schema_validation: bool,
    ) -> Result<PolicyStoreWithID, PolicyStoreLoadError> {
        // Magic-byte sniff — the ZIP local-file-header signature `PK\x03\x04`
        // disambiguates `.cjar` archives from JSON regardless of source type.
        // Future-proofs the Lock Server path: if Lock Server starts serving
        // `.cjar` archives at a URL whose suffix doesn't end in `.cjar`, we
        // route to the archive parser instead of failing with a JSON error.
        if bytes.starts_with(&ZIP_MAGIC) {
            return parse_cjar_bytes(bytes, strict_schema_validation).await;
        }
        match self {
            Self::LockServer { .. } | Self::Uri { .. } => {
                parse_lock_master_bytes(bytes, strict_schema_validation)
            },
            Self::CjarUrl { .. } => parse_cjar_bytes(bytes, strict_schema_validation).await,
        }
    }
}

/// Handle to a running refresh worker. Dropping the handle signals shutdown.
pub(crate) struct PolicyStoreRefreshHandle {
    shutdown: Option<oneshot::Sender<()>>,
    // Held only so the worker future is not detached. The worker observes the
    // closed shutdown channel and exits on its next select boundary.
    _join: JoinHandle<()>,
}

impl Drop for PolicyStoreRefreshHandle {
    fn drop(&mut self) {
        drop(self.shutdown.take());
    }
}

/// Snapshot of the bootstrap-load result that the refresh worker uses to
/// short-circuit its very first periodic tick. Carried as a small named
/// struct rather than two loose parameters so the spawn-site signature
/// stays under the clippy `too_many_arguments` threshold without needing
/// a `#[allow]` attribute.
pub(crate) struct RefreshWorkerSeed {
    pub initial_body_hash: Option<u64>,
    pub initial_validators: CacheHeadersState,
}

/// Read-only worker context — handles and config that don't change between
/// ticks. Owned by [`spawn_refresh_worker`] / [`run_worker`] for the lifetime
/// of the background task; tick functions take `&WorkerContext`. Per-tick
/// mutable state lives in [`RefreshState`].
pub(crate) struct WorkerContext {
    pub(crate) source: RefreshSource,
    pub(crate) interval_secs: u64,
    pub(crate) http_client: HttpClient,
    pub(crate) rebuilder: AuthzRebuilder,
    pub(crate) authz_swap: Arc<ArcSwap<Authz>>,
    pub(crate) metrics: Arc<MetricsCollector>,
    pub(crate) log: Logger,
    /// Body-hash of the policy-store bytes captured during initial bootstrap
    /// (`None` for non-URL sources). Used to seed `RefreshState.last_body_hash`
    /// so the very first periodic tick can short-circuit the parse/rebuild/swap
    /// path when the upstream returns a byte-identical body.
    pub(crate) initial_body_hash: Option<u64>,
    /// Cache validators (`ETag`, `Last-Modified`, `max-age` / `Expires`)
    /// captured from the initial bootstrap response. Used to seed
    /// `RefreshState.validators` so the very first periodic conditional GET
    /// can send `If-None-Match` / `If-Modified-Since` and potentially get a
    /// `304` back with zero body bytes downloaded — the optimal first-tick
    /// path. Empty for non-URL sources.
    pub(crate) initial_validators: CacheHeadersState,
    /// Forwarded from `BootstrapConfig.authorization_config` so each refresh
    /// tick enforces the same "schema must be present" invariant the
    /// bootstrap load did. Without this, a refresh against a store that
    /// dropped its schema could install a configuration the startup path
    /// would have rejected.
    pub(crate) strict_schema_validation: bool,
}

/// Spawn the background refresh worker. Returns a [`PolicyStoreRefreshHandle`]
/// whose `Drop` signals the worker to exit.
pub(crate) fn spawn_refresh_worker(ctx: WorkerContext) -> PolicyStoreRefreshHandle {
    let (shutdown_tx, shutdown_rx) = oneshot::channel::<()>();

    let join = spawn_task(async move {
        run_worker(ctx, shutdown_rx).await;
    });

    PolicyStoreRefreshHandle {
        shutdown: Some(shutdown_tx),
        _join: join,
    }
}

async fn run_worker(ctx: WorkerContext, shutdown_rx: oneshot::Receiver<()>) {
    let mut state = RefreshState {
        last_body_hash: ctx.initial_body_hash,
        validators: ctx.initial_validators.clone(),
        ..RefreshState::default()
    };
    let mut shutdown_rx = shutdown_rx;

    ctx.log.log_any(
        LogEntry::new(BaseLogEntry::new_system_opt_request_id(
            LogLevel::INFO,
            None,
        ))
        .set_message(format!(
            "policy store refresh worker started (url={}, interval={}s)",
            ctx.source.url(),
            ctx.interval_secs,
        )),
    );

    loop {
        let delay = state.next_delay(ctx.interval_secs);
        let sleep_fut = sleep(delay);
        futures::pin_mut!(sleep_fut);

        match select(sleep_fut, &mut shutdown_rx).await {
            Either::Left(((), _)) => {
                let outcome = tick(&ctx, &mut state).await;
                ctx.metrics.record_policy_store_refresh(outcome);
                ctx.metrics.record_policy_store_refresh_strategy(
                    state.strategy.current,
                    state.strategy.conditional_to_head_transitions,
                    state.strategy.head_to_plain_transitions,
                    state.strategy.upgrade_to_head_transitions,
                    state.strategy.upgrade_to_conditional_transitions,
                );
            },
            Either::Right(_) => {
                ctx.log.log_any(
                    LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                        LogLevel::DEBUG,
                        None,
                    ))
                    .set_message("policy store refresh worker shutting down".to_string()),
                );
                break;
            },
        }
    }
}

async fn tick(ctx: &WorkerContext, state: &mut RefreshState) -> RefreshOutcome {
    let choice = state.strategy.choose_for_tick();
    let is_probe = choice.probe_target.is_some();
    match choice.strategy {
        RefreshStrategy::Conditional => tick_conditional(ctx, state, is_probe).await,
        RefreshStrategy::HeadThenGet => tick_head_then_get(ctx, state, is_probe).await,
        RefreshStrategy::PlainGet => tick_plain_get(ctx, state).await,
    }
}

async fn tick_conditional(
    ctx: &WorkerContext,
    state: &mut RefreshState,
    is_probe: bool,
) -> RefreshOutcome {
    let url = ctx.source.url();
    let fetch_result = ctx
        .http_client
        .get_bytes_conditional(url, &state.validators)
        .await;

    match fetch_result {
        Err(e) => fetch_error(ctx, state, &e),
        Ok(ConditionalFetch::NotModified { validators }) => {
            state.validators.merge_from(validators);
            state.consecutive_failures = 0;
            if is_probe {
                state.strategy.record_probe_success();
            } else {
                state.strategy.record_helped();
            }
            RefreshOutcome::NotModified
        },
        Ok(ConditionalFetch::Modified { bytes, validators }) => {
            let new_hash = body_hash(&bytes);
            if should_short_circuit(new_hash, state) {
                state.validators.merge_from(validators);
                state.consecutive_failures = 0;
                if !is_probe {
                    state.strategy.record_degraded();
                }
                return RefreshOutcome::NotModified;
            }
            parse_swap_and_record(ctx, state, bytes, new_hash, validators).await
        },
    }
}

async fn tick_head_then_get(
    ctx: &WorkerContext,
    state: &mut RefreshState,
    is_probe: bool,
) -> RefreshOutcome {
    let url = ctx.source.url();
    let head = match ctx.http_client.head_validators(url).await {
        Ok(h) => h,
        Err(e) => return fetch_error(ctx, state, &e),
    };
    match head {
        HeadOutcome::NotSupported => {
            if !is_probe {
                state.strategy.force_degrade();
            }
            tick_plain_get(ctx, state).await
        },
        HeadOutcome::Headers(new_validators) => {
            if !new_validators.has_validator() {
                if !is_probe {
                    state.strategy.record_degraded();
                }
                return tick_plain_get(ctx, state).await;
            }
            if validators_match(&new_validators, &state.validators) {
                state.validators.merge_from(new_validators);
                state.consecutive_failures = 0;
                if is_probe {
                    state.strategy.record_head_probe_success();
                } else {
                    state.strategy.record_helped();
                }
                return RefreshOutcome::NotModified;
            }
            // Validators differ — GET first; commit success only if GET succeeds.
            let outcome = tick_plain_get(ctx, state).await;
            if matches!(
                outcome,
                RefreshOutcome::Success | RefreshOutcome::NotModified
            ) {
                if is_probe {
                    state.strategy.record_head_probe_success();
                } else {
                    state.strategy.record_helped();
                }
            }
            outcome
        },
    }
}

async fn tick_plain_get(ctx: &WorkerContext, state: &mut RefreshState) -> RefreshOutcome {
    let url = ctx.source.url();
    let response = match ctx.http_client.get_with_retry(url).await {
        Ok(r) => r,
        Err(e) => return fetch_error(ctx, state, &e),
    };
    let status = response.status();
    let new_validators = CacheHeadersState::from_headers(response.headers(), Utc::now());
    let bytes = match ctx.http_client.read_response_capped(response).await {
        Ok(b) => b,
        Err(e) => {
            state.consecutive_failures = state.consecutive_failures.saturating_add(1);
            ctx.log.log_any(
                LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                    LogLevel::WARN,
                    None,
                ))
                .set_message(format!(
                    "policy store refresh: body decode error for {url}: {e} (status {status})"
                )),
            );
            // HTTP transaction completed (we have a status); body read failed
            // (decode error OR cap exceeded). Classify as DecodeError, not
            // NetworkError, so triage can distinguish "couldn't reach upstream"
            // from "read response body".
            return RefreshOutcome::DecodeError;
        },
    };
    let new_hash = body_hash(&bytes);
    if should_short_circuit(new_hash, state) {
        // Same body as last successful load — same resource version. Merge
        // (don't replace) so a 200 that drops ETag (some proxies strip it)
        // doesn't erase the validators that let the next conditional GET
        // potentially win a 304.
        state.validators.merge_from(new_validators);
        state.consecutive_failures = 0;
        return RefreshOutcome::NotModified;
    }
    parse_swap_and_record(ctx, state, bytes, new_hash, new_validators).await
}

async fn parse_swap_and_record(
    ctx: &WorkerContext,
    state: &mut RefreshState,
    bytes: Vec<u8>,
    new_hash: u64,
    new_validators: CacheHeadersState,
) -> RefreshOutcome {
    let url = ctx.source.url();
    let start = Utc::now();
    let parsed = match ctx.source.parse(&bytes, ctx.strict_schema_validation).await {
        Ok(p) => p,
        Err(e) => {
            state.consecutive_failures = state.consecutive_failures.saturating_add(1);
            ctx.log.log_any(
                LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                    LogLevel::ERROR,
                    None,
                ))
                .set_message(format!(
                    "policy store refresh: parse failure for {url}: {e}"
                )),
            );
            return RefreshOutcome::ParseError;
        },
    };

    // Capture the refreshed policy count BEFORE `parsed` is moved into
    // `rebuild`. Used to refresh the `instance.policy_count` gauge after the
    // swap — otherwise the gauge would stay pinned to the bootstrap value
    // forever even though authorization decisions use the new set.
    let new_policy_count = parsed.policies.get_set().num_of_policies();

    let prior_authz = ctx.authz_swap.load_full();
    let new_authz = match ctx.rebuilder.rebuild(parsed, &prior_authz).await {
        Ok(a) => a,
        Err(e) => {
            state.consecutive_failures = state.consecutive_failures.saturating_add(1);
            ctx.log.log_any(
                LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                    LogLevel::ERROR,
                    None,
                ))
                .set_message(format!(
                    "policy store refresh: rebuild failure for {url}: {e}"
                )),
            );
            return RefreshOutcome::RebuildError;
        },
    };

    ctx.authz_swap.store(Arc::new(new_authz));
    // Keep the `instance.policy_count` gauge in sync with the freshly-swapped
    // store. Before this update the gauge reported the bootstrap count
    // indefinitely, diverging silently from what the authz engine actually
    // evaluates.
    ctx.metrics.set_policy_count(new_policy_count);
    state.validators = new_validators;
    state.last_body_hash = Some(new_hash);
    state.consecutive_failures = 0;

    let elapsed = Utc::now().signed_duration_since(start).num_milliseconds();
    ctx.log.log_any(
        LogEntry::new(BaseLogEntry::new_system_opt_request_id(
            LogLevel::INFO,
            None,
        ))
        .set_message(format!(
            "policy store refresh: swapped new store from {url} in {elapsed} ms"
        )),
    );

    RefreshOutcome::Success
}

fn fetch_error(
    ctx: &WorkerContext,
    state: &mut RefreshState,
    e: &crate::http::HttpClientError,
) -> RefreshOutcome {
    let url = ctx.source.url();
    state.consecutive_failures = state.consecutive_failures.saturating_add(1);
    let (kind, outcome) = classify_fetch_error(e);
    ctx.log.log_any(
        LogEntry::new(BaseLogEntry::new_system_opt_request_id(
            LogLevel::WARN,
            None,
        ))
        .set_message(format!("policy store refresh: {kind} against {url}: {e}")),
    );
    outcome
}

/// Pure classifier for [`fetch_error`] — extracted so unit tests can pin the
/// routing without constructing a full [`WorkerContext`].
///
/// Order matters:
/// 1. **Body decode failure** (`DecodeResponseBytes`) — distinct bucket so
///    truncated bodies don't conflate with status errors.
/// 2. **HTTP status error** (`status_code.is_some()`) — covers the case the
///    retry layer collapses 4xx/5xx exhaustion into `MaxRetriesExceeded` but
///    still carries the response status; without this, *every* GET-path
///    `HttpError` outcome would silently land in `NetworkError`.
/// 3. **Transport failure** (`is_max_retries_exceeded()` with no status) —
///    DNS / TCP / TLS; nothing arrived.
/// 4. **Fallback** — any other variant routes to `HttpError`.
fn classify_fetch_error(e: &crate::http::HttpClientError) -> (&'static str, RefreshOutcome) {
    if e.is_decode_error() {
        ("body decode error", RefreshOutcome::DecodeError)
    } else if e.status_code().is_some() {
        ("HTTP error", RefreshOutcome::HttpError)
    } else if e.is_max_retries_exceeded() {
        ("network error", RefreshOutcome::NetworkError)
    } else {
        ("HTTP error", RefreshOutcome::HttpError)
    }
}

/// `true` if the two validator sets refer to the same resource version — used
/// by the HEAD-then-GET fallback to decide whether to skip the body fetch.
///
/// `ETag`s are compared per RFC 7232 §2.3.2 **weak comparison** semantics —
/// this is the comparison required by `If-None-Match` (the conditional we
/// actually send). Concretely: ignore each side's optional `W/` prefix and
/// compare the opaque quoted-string portion. Without this, a server that
/// returns `W/"v1"` from HEAD but stored `"v1"` from a prior GET would force
/// an unnecessary full GET every tick, defeating the bandwidth-saving point
/// of the HEAD probe.
fn validators_match(a: &CacheHeadersState, b: &CacheHeadersState) -> bool {
    if let (Some(ea), Some(eb)) = (a.etag.as_ref(), b.etag.as_ref()) {
        return etag_opaque_tag(ea) == etag_opaque_tag(eb);
    }
    if let (Some(la), Some(lb)) = (a.last_modified.as_ref(), b.last_modified.as_ref()) {
        return la == lb;
    }
    false
}

/// Strip the `W/` weak-validator prefix (RFC 7232 §2.3.1) for weak comparison.
fn etag_opaque_tag(etag: &str) -> &str {
    etag.strip_prefix("W/").unwrap_or(etag)
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::collections::HashMap;

    #[test]
    fn choose_for_tick_from_plain_get_probes_head_then_get() {
        let mut state = StrategyState {
            current: RefreshStrategy::PlainGet,
            last_probe_at: None,
            ..StrategyState::default()
        };
        let choice = state.choose_for_tick();
        assert_eq!(choice.strategy, RefreshStrategy::HeadThenGet);
        assert!(choice.probe_target.is_some());
    }

    #[test]
    fn choose_for_tick_from_head_then_get_probes_conditional() {
        let mut state = StrategyState {
            current: RefreshStrategy::HeadThenGet,
            last_probe_at: None,
            ..StrategyState::default()
        };
        let choice = state.choose_for_tick();
        assert_eq!(choice.strategy, RefreshStrategy::Conditional);
        assert!(choice.probe_target.is_some());
    }

    #[test]
    fn choose_for_tick_no_probe_when_cooldown_active() {
        let mut state = StrategyState {
            current: RefreshStrategy::PlainGet,
            last_probe_at: Some(Utc::now()),
            ..StrategyState::default()
        };
        let choice = state.choose_for_tick();
        assert_eq!(choice.strategy, RefreshStrategy::PlainGet);
        assert!(choice.probe_target.is_none());
    }

    #[test]
    fn record_head_probe_success_upgrades_plain_get_to_head_then_get() {
        let mut state = StrategyState {
            current: RefreshStrategy::PlainGet,
            ..StrategyState::default()
        };
        state.record_head_probe_success();
        assert_eq!(state.current, RefreshStrategy::HeadThenGet);
        assert_eq!(state.upgrade_to_head_transitions, 1);
    }

    #[test]
    fn record_head_probe_success_is_noop_from_head_then_get() {
        let mut state = StrategyState {
            current: RefreshStrategy::HeadThenGet,
            ..StrategyState::default()
        };
        state.record_head_probe_success();
        assert_eq!(state.current, RefreshStrategy::HeadThenGet);
        assert_eq!(state.upgrade_to_head_transitions, 0);
    }

    fn one_issuer() -> HashMap<String, TrustedIssuer> {
        let mut m = HashMap::new();
        m.insert("issuer_a".to_string(), TrustedIssuer::default());
        m
    }

    #[test]
    fn identical_issuer_maps_signal_reuse() {
        let a = one_issuer();
        let b = one_issuer();
        assert!(
            issuers_unchanged(Some(&a), Some(&b)),
            "identical issuer maps must signal reuse so JwtService is not rebuilt",
        );
    }

    #[test]
    fn empty_vs_some_issuer_signals_rebuild() {
        let o = one_issuer();
        assert!(
            !issuers_unchanged(None, Some(&o)),
            "going from no issuers to some issuers must signal a JwtService rebuild",
        );
        assert!(
            !issuers_unchanged(Some(&o), None),
            "removing all issuers must signal a JwtService rebuild",
        );
    }

    #[test]
    fn added_issuer_signals_rebuild() {
        let base = one_issuer();
        let mut expanded = one_issuer();
        expanded.insert("issuer_b".to_string(), TrustedIssuer::default());
        assert!(
            !issuers_unchanged(Some(&base), Some(&expanded)),
            "adding a new issuer must signal a JwtService rebuild",
        );
    }

    #[test]
    fn removed_issuer_signals_rebuild() {
        let mut two = one_issuer();
        two.insert("issuer_b".to_string(), TrustedIssuer::default());
        let one = one_issuer();
        assert!(
            !issuers_unchanged(Some(&two), Some(&one)),
            "removing an issuer must signal a JwtService rebuild",
        );
    }

    #[test]
    fn next_delay_uses_base_when_no_server_hint() {
        let s = RefreshState::default();
        let d = s.next_delay(300).as_secs();
        // Allow for ±10% jitter.
        assert!((270..=330).contains(&d), "got {d}");
    }

    #[test]
    fn next_delay_honors_shorter_server_hint() {
        let s = RefreshState {
            validators: CacheHeadersState {
                fresh_for: Some(Duration::from_secs(60)),
                ..CacheHeadersState::default()
            },
            ..RefreshState::default()
        };
        let d = s.next_delay(300).as_secs();
        // Server says 60s — we pick the shorter of 60 and 300, plus jitter.
        assert!((54..=66).contains(&d), "got {d}");
    }

    #[test]
    fn next_delay_caps_server_hint_at_base() {
        let s = RefreshState {
            // Server says "fresh for an hour" but operator chose 30s.
            validators: CacheHeadersState {
                fresh_for: Some(Duration::from_secs(3600)),
                ..CacheHeadersState::default()
            },
            ..RefreshState::default()
        };
        let d = s.next_delay(30).as_secs();
        // Should track the operator's 30s base, ±10% jitter — never the server's 3600s.
        assert!((27..=33).contains(&d), "got {d}");
    }

    #[test]
    fn next_delay_clamped_above_min() {
        let s = RefreshState::default();
        // base = 1s but min is 5s — should be at least min.
        let d = s.next_delay(1).as_secs();
        assert!(d >= PolicyStoreConfig::MIN_REFRESH_INTERVAL_SECS, "got {d}");
    }

    #[test]
    fn next_delay_backs_off_on_failures() {
        let s = RefreshState {
            consecutive_failures: 3,
            ..RefreshState::default()
        };
        let d = s.next_delay(60).as_secs();
        // 60 * 2^3 = 480, plus jitter, capped at REFRESH_FAILURE_BACKOFF_MAX_SECS=600.
        // After ±10% jitter, expect between ~432 and ~528.
        assert!((400..=600).contains(&d), "got {d}");
    }

    #[test]
    fn body_hash_stable_for_same_bytes() {
        let a = body_hash(b"hello");
        let b = body_hash(b"hello");
        assert_eq!(
            a, b,
            "body_hash must be stable for identical byte slices — the short-circuit relies on byte-equal bodies producing the same hash within a process",
        );
    }

    #[test]
    fn body_hash_differs_for_different_bytes() {
        assert_ne!(
            body_hash(b"hello"),
            body_hash(b"world"),
            "body_hash must differ for different byte slices — otherwise the short-circuit would skip rebuilds when the policy store actually changed",
        );
    }

    #[test]
    fn refresh_state_seed_short_circuits_first_tick_on_identical_body() {
        // Seeding `last_body_hash` from the initial load is the whole point of
        // the plumbing through `WorkerContext::initial_body_hash`: when the
        // first periodic tick fetches a byte-identical body, the production
        // `should_short_circuit` helper used by `tick_conditional` and
        // `tick_plain_get` must return true so we skip parse / rebuild / swap.
        let bytes = b"identical-policy-store-body";
        let state = RefreshState {
            last_body_hash: Some(body_hash(bytes)),
            ..RefreshState::default()
        };

        assert!(
            should_short_circuit(body_hash(bytes), &state),
            "seeded state must short-circuit when the upstream returns the same bytes (would otherwise pay one wasted parse/rebuild on every process startup)",
        );
        assert!(
            !should_short_circuit(body_hash(b"different-body"), &state),
            "different bytes must NOT short-circuit — first tick has to fall through to parse + rebuild + swap so policy updates land",
        );
    }

    #[test]
    fn should_short_circuit_returns_false_on_unseeded_state() {
        // An un-seeded RefreshState (`last_body_hash = None`) must never claim
        // a match. This is the pre-seed-fix behavior — any hash compared
        // against `None` returns false, so the first tick always falls through
        // to parse/rebuild/swap. Seeding (above) is the optimization; this
        // test pins the safe default.
        let state = RefreshState::default();
        assert!(
            !should_short_circuit(body_hash(b"anything"), &state),
            "with no seed, no hash can match — first tick must always fall through",
        );
    }

    #[test]
    fn body_hash_short_circuit_merges_validators_rather_than_overwriting() {
        // Regression: a 200 that drops ETag on an identical body would
        // previously wipe `state.etag`, so the next conditional GET sent no
        // `If-None-Match` and the upstream could never reply 304 again — the
        // worker would download the full body every tick.
        //
        // We can't drive `tick_conditional` directly without a full
        // WorkerContext, so we exercise the contract on the state-mutation
        // helper merge_from (which the short-circuit now calls).
        let mut state = RefreshState {
            last_body_hash: Some(body_hash(b"unchanged body")),
            validators: CacheHeadersState {
                etag: Some("\"v1\"".to_string()),
                last_modified: Some("Mon".to_string()),
                fresh_for: Some(Duration::from_secs(60)),
                ..CacheHeadersState::default()
            },
            ..RefreshState::default()
        };

        // Simulate a 200-identical-body reply that omits ETag (some proxies
        // strip it). After merge: ETag survives, Last-Modified survives,
        // fresh_for overwrites (per-response semantics — see Finding #5).
        let response_with_no_etag = CacheHeadersState {
            // No ETag — the misbehaving proxy case.
            last_modified: Some("Mon".to_string()),
            ..CacheHeadersState::default()
        };
        state.validators.merge_from(response_with_no_etag);

        assert_eq!(
            state.validators.etag.as_deref(),
            Some("\"v1\""),
            "ETag must survive a 200-identical-body that drops the field — otherwise the next conditional GET has nothing to send and we'd re-download forever",
        );
        assert_eq!(
            state.validators.last_modified.as_deref(),
            Some("Mon"),
            "Last-Modified must survive on the short-circuit path",
        );
        assert_eq!(
            state.validators.fresh_for, None,
            "fresh_for is per-response — a response that omits Cache-Control must clear it (Finding #5)",
        );
    }

    #[test]
    fn refresh_source_construction_from_url() {
        let s =
            RefreshSource::from_policy_store_source(&PolicyStoreSource::CjarUrl("http://x".into()));
        assert!(
            matches!(s, Some(RefreshSource::CjarUrl { .. })),
            "PolicyStoreSource::CjarUrl must map to RefreshSource::CjarUrl, got {s:?}",
        );

        let s = RefreshSource::from_policy_store_source(&PolicyStoreSource::LockServer(
            "http://y".into(),
        ));
        assert!(
            matches!(s, Some(RefreshSource::LockServer { .. })),
            "PolicyStoreSource::LockServer must map to RefreshSource::LockServer, got {s:?}",
        );

        let s = RefreshSource::from_policy_store_source(&PolicyStoreSource::Uri("http://z".into()));
        assert!(
            matches!(s, Some(RefreshSource::Uri { .. })),
            "PolicyStoreSource::Uri must map to RefreshSource::Uri, got {s:?}",
        );

        let s = RefreshSource::from_policy_store_source(&PolicyStoreSource::Yaml("..".into()));
        assert!(
            s.is_none(),
            "non-URL sources (Yaml/Json/file paths/archive bytes) must produce no RefreshSource — the worker doesn't spawn for them, got {s:?}",
        );
    }

    // ---- Strategy state machine ----

    #[test]
    fn strategy_starts_at_conditional() {
        let s = StrategyState::default();
        assert_eq!(
            s.current,
            RefreshStrategy::Conditional,
            "fresh StrategyState must start at the most efficient strategy",
        );
    }

    #[test]
    fn strategy_degrades_after_threshold() {
        let mut s = StrategyState::default();
        for i in 0..STRATEGY_DEGRADE_THRESHOLD - 1 {
            s.record_degraded();
            assert_eq!(
                s.current,
                RefreshStrategy::Conditional,
                "degrade {} of {STRATEGY_DEGRADE_THRESHOLD} must not yet transition — threshold is consecutive ticks",
                i + 1,
            );
        }
        s.record_degraded();
        assert_eq!(
            s.current,
            RefreshStrategy::HeadThenGet,
            "exactly STRATEGY_DEGRADE_THRESHOLD consecutive degrades must transition Conditional → HeadThenGet",
        );
        assert_eq!(
            s.conditional_to_head_transitions, 1,
            "transition counter must increment exactly once per Conditional → HeadThenGet",
        );
        // Subsequent degrades climb further down.
        for _ in 0..STRATEGY_DEGRADE_THRESHOLD {
            s.record_degraded();
        }
        assert_eq!(
            s.current,
            RefreshStrategy::PlainGet,
            "another threshold's worth of degrades must transition HeadThenGet → PlainGet",
        );
        assert_eq!(
            s.head_to_plain_transitions, 1,
            "head_to_plain_transitions counter must increment exactly once per HeadThenGet → PlainGet",
        );
    }

    #[test]
    fn strategy_record_helped_resets_degrade_counter() {
        let mut s = StrategyState::default();
        s.record_degraded();
        s.record_degraded();
        s.record_helped();
        // After helping, threshold should be re-counted from zero.
        s.record_degraded();
        assert_eq!(
            s.current,
            RefreshStrategy::Conditional,
            "record_helped must reset the degrade counter so the next single degrade does not transition",
        );
    }

    #[test]
    fn strategy_force_degrade_skips_threshold() {
        let mut s = StrategyState::default();
        s.force_degrade();
        assert_eq!(
            s.current,
            RefreshStrategy::HeadThenGet,
            "force_degrade must transition immediately, without waiting for the threshold",
        );
        assert_eq!(
            s.conditional_to_head_transitions, 1,
            "force_degrade must increment the transition counter the same as a threshold-triggered degrade",
        );
    }

    #[test]
    fn strategy_probe_success_upgrades_back_to_conditional() {
        let mut s = StrategyState {
            current: RefreshStrategy::PlainGet,
            ..StrategyState::default()
        };
        s.record_probe_success();
        assert_eq!(
            s.current,
            RefreshStrategy::Conditional,
            "a successful Conditional probe must restore the most efficient strategy",
        );
        assert_eq!(
            s.upgrade_to_conditional_transitions, 1,
            "upgrade_to_conditional_transitions must increment exactly once per upgrade",
        );
    }

    #[test]
    fn strategy_choose_returns_conditional_when_current() {
        let mut s = StrategyState::default();
        let choice = s.choose_for_tick();
        assert_eq!(
            choice.strategy,
            RefreshStrategy::Conditional,
            "choose_for_tick must return the current strategy when already at Conditional",
        );
        assert!(
            choice.probe_target.is_none(),
            "is_probe must be false when not probing — we're already at the most efficient strategy",
        );
    }

    #[test]
    fn strategy_choose_first_probe_when_degraded() {
        let mut s = StrategyState {
            current: RefreshStrategy::HeadThenGet,
            ..StrategyState::default()
        };
        let choice = s.choose_for_tick();
        assert_eq!(
            choice.strategy,
            RefreshStrategy::Conditional,
            "a first choose_for_tick after a degrade must run a Conditional probe to test for upstream recovery",
        );
        assert!(
            choice.probe_target.is_some(),
            "is_probe must be true so tick logic interprets 304 as an upgrade signal rather than a steady-state result",
        );
        assert!(
            s.last_probe_at.is_some(),
            "choose_for_tick must stamp last_probe_at when scheduling a probe so the reprobe interval can be enforced",
        );
    }

    #[test]
    fn strategy_choose_returns_current_between_probes() {
        let mut s = StrategyState {
            current: RefreshStrategy::PlainGet,
            last_probe_at: Some(Utc::now()),
            ..StrategyState::default()
        };
        let choice = s.choose_for_tick();
        assert_eq!(
            choice.strategy,
            RefreshStrategy::PlainGet,
            "while inside the reprobe interval window, choose_for_tick must return the current strategy unchanged",
        );
        assert!(
            choice.probe_target.is_none(),
            "no probe is scheduled inside the reprobe window — is_probe must be false",
        );
    }

    #[test]
    fn validators_match_etag_takes_precedence() {
        let a = CacheHeadersState {
            etag: Some("\"v1\"".to_string()),
            last_modified: Some("Mon".to_string()),
            ..CacheHeadersState::default()
        };
        let b = CacheHeadersState {
            etag: Some("\"v1\"".to_string()),
            last_modified: Some("Tue".to_string()),
            ..CacheHeadersState::default()
        };
        assert!(validators_match(&a, &b), "matching ETag wins");
    }

    #[test]
    fn validators_match_falls_back_to_last_modified() {
        let a = CacheHeadersState {
            last_modified: Some("Mon".to_string()),
            ..CacheHeadersState::default()
        };
        let b = CacheHeadersState {
            last_modified: Some("Mon".to_string()),
            ..CacheHeadersState::default()
        };
        assert!(
            validators_match(&a, &b),
            "with no ETag on either side, equal Last-Modified must match",
        );
    }

    #[test]
    fn validators_match_returns_false_when_neither_present() {
        let a = CacheHeadersState::default();
        let b = CacheHeadersState::default();
        assert!(
            !validators_match(&a, &b),
            "with no validators on either side there is nothing to compare — must return false rather than treat absence as agreement",
        );
    }

    #[test]
    fn validators_match_returns_false_on_different_etag() {
        let a = CacheHeadersState {
            etag: Some("\"v1\"".to_string()),
            ..CacheHeadersState::default()
        };
        let b = CacheHeadersState {
            etag: Some("\"v2\"".to_string()),
            ..CacheHeadersState::default()
        };
        assert!(
            !validators_match(&a, &b),
            "different ETags must not match — short-circuit would skip a real update",
        );
    }

    #[test]
    fn validators_match_falls_back_to_last_modified_when_etag_only_on_one_side() {
        let a = CacheHeadersState {
            etag: Some("\"v1\"".to_string()),
            last_modified: Some("Mon".to_string()),
            ..CacheHeadersState::default()
        };
        let b = CacheHeadersState {
            etag: None,
            last_modified: Some("Mon".to_string()),
            ..CacheHeadersState::default()
        };
        assert!(
            validators_match(&a, &b),
            "ETag must be on BOTH sides to compare; asymmetric presence must fall back to Last-Modified",
        );
    }

    #[test]
    fn validators_match_returns_false_when_etag_differs_even_if_lm_matches() {
        let a = CacheHeadersState {
            etag: Some("\"v1\"".to_string()),
            last_modified: Some("Mon".to_string()),
            ..CacheHeadersState::default()
        };
        let b = CacheHeadersState {
            etag: Some("\"v2\"".to_string()),
            last_modified: Some("Mon".to_string()),
            ..CacheHeadersState::default()
        };
        assert!(
            !validators_match(&a, &b),
            "ETag mismatch is decisive — matching Last-Modified must not rescue the comparison",
        );
    }

    #[test]
    fn validators_match_returns_false_on_different_last_modified() {
        let a = CacheHeadersState {
            last_modified: Some("Mon, 01 Jan 2024 00:00:00 GMT".to_string()),
            ..CacheHeadersState::default()
        };
        let b = CacheHeadersState {
            last_modified: Some("Tue, 02 Jan 2024 00:00:00 GMT".to_string()),
            ..CacheHeadersState::default()
        };
        assert!(
            !validators_match(&a, &b),
            "different Last-Modified values must not match",
        );
    }

    #[test]
    fn validators_match_weak_etag_equals_strong_for_same_tag() {
        let weak = CacheHeadersState {
            etag: Some("W/\"v1\"".to_string()),
            ..CacheHeadersState::default()
        };
        let strong = CacheHeadersState {
            etag: Some("\"v1\"".to_string()),
            ..CacheHeadersState::default()
        };
        assert!(
            validators_match(&weak, &strong),
            "weak `W/\"v1\"` and strong `\"v1\"` must match under If-None-Match semantics (RFC 7232 §2.3.2)",
        );
        // Both directions.
        assert!(
            validators_match(&strong, &weak),
            "comparison must be symmetric — strong vs weak must also match",
        );
    }

    #[test]
    fn validators_match_weak_etags_match_each_other() {
        let a = CacheHeadersState {
            etag: Some("W/\"v1\"".to_string()),
            ..CacheHeadersState::default()
        };
        let b = CacheHeadersState {
            etag: Some("W/\"v1\"".to_string()),
            ..CacheHeadersState::default()
        };
        assert!(
            validators_match(&a, &b),
            "two identical weak ETags must match",
        );
    }

    #[test]
    fn validators_match_weak_etag_still_distinguishes_different_tags() {
        let a = CacheHeadersState {
            etag: Some("W/\"v1\"".to_string()),
            ..CacheHeadersState::default()
        };
        let b = CacheHeadersState {
            etag: Some("W/\"v2\"".to_string()),
            ..CacheHeadersState::default()
        };
        assert!(
            !validators_match(&a, &b),
            "different opaque tags must not match even when both are weak",
        );
    }

    #[test]
    fn validators_match_returns_false_when_lm_present_on_one_side_only() {
        let a = CacheHeadersState {
            last_modified: Some("Mon".to_string()),
            ..CacheHeadersState::default()
        };
        let b = CacheHeadersState::default();
        assert!(
            !validators_match(&a, &b),
            "Last-Modified on only one side cannot match — asymmetric presence must not be treated as agreement",
        );
    }

    #[test]
    fn strategy_plain_get_does_not_degrade_further() {
        let mut s = StrategyState {
            current: RefreshStrategy::PlainGet,
            ..StrategyState::default()
        };
        for _ in 0..STRATEGY_DEGRADE_THRESHOLD * 3 {
            s.record_degraded();
        }
        assert_eq!(
            s.current,
            RefreshStrategy::PlainGet,
            "PlainGet is the floor — repeated record_degraded() must not move below it",
        );
        // Transition counters should not budge — there's no transition target.
        assert_eq!(
            s.head_to_plain_transitions, 0,
            "no head→plain transition can fire from PlainGet",
        );
        assert_eq!(
            s.conditional_to_head_transitions, 0,
            "no conditional→head transition can fire from PlainGet",
        );
    }

    #[test]
    fn strategy_force_degrade_at_plain_get_is_noop() {
        let mut s = StrategyState {
            current: RefreshStrategy::PlainGet,
            ..StrategyState::default()
        };
        s.force_degrade();
        assert_eq!(
            s.current,
            RefreshStrategy::PlainGet,
            "force_degrade at the floor must leave current unchanged",
        );
        assert_eq!(
            s.conditional_to_head_transitions, 0,
            "no conditional→head transition can fire when force_degrade is invoked at the PlainGet floor",
        );
        assert_eq!(
            s.head_to_plain_transitions, 0,
            "no head→plain transition can fire when force_degrade is invoked at the PlainGet floor",
        );
    }

    #[test]
    fn strategy_probe_success_noop_when_already_conditional() {
        let mut s = StrategyState::default();
        assert_eq!(
            s.current,
            RefreshStrategy::Conditional,
            "default StrategyState must start at Conditional",
        );
        s.record_probe_success();
        assert_eq!(
            s.current,
            RefreshStrategy::Conditional,
            "record_probe_success while already at Conditional must be a no-op",
        );
        assert_eq!(
            s.upgrade_to_conditional_transitions, 0,
            "no upgrade transition counter increment when already at Conditional",
        );
    }

    #[test]
    fn strategy_record_helped_does_not_change_current() {
        let mut s = StrategyState {
            current: RefreshStrategy::HeadThenGet,
            ..StrategyState::default()
        };
        s.record_helped();
        assert_eq!(
            s.current,
            RefreshStrategy::HeadThenGet,
            "record_helped() only resets the degrade counter — current strategy must not change",
        );
    }

    #[test]
    fn strategy_cumulative_transitions_accumulate() {
        let mut s = StrategyState::default();
        for _ in 0..3 {
            for _ in 0..STRATEGY_DEGRADE_THRESHOLD {
                s.record_degraded();
            }
            s.record_probe_success();
        }
        assert_eq!(
            s.current,
            RefreshStrategy::Conditional,
            "after each cycle the probe must restore Conditional",
        );
        assert_eq!(
            s.conditional_to_head_transitions, 3,
            "each conditional→head transition must increment the counter cumulatively, not overwrite",
        );
        assert_eq!(
            s.upgrade_to_conditional_transitions, 3,
            "each upgrade-back-to-Conditional must increment cumulatively",
        );
    }

    #[test]
    fn strategy_full_walk_down() {
        let mut s = StrategyState::default();
        for _ in 0..STRATEGY_DEGRADE_THRESHOLD {
            s.record_degraded();
        }
        assert_eq!(
            s.current,
            RefreshStrategy::HeadThenGet,
            "threshold-many degrades from Conditional must land on HeadThenGet",
        );
        for _ in 0..STRATEGY_DEGRADE_THRESHOLD {
            s.record_degraded();
        }
        assert_eq!(
            s.current,
            RefreshStrategy::PlainGet,
            "another threshold-many degrades must reach the PlainGet floor",
        );
        assert_eq!(
            s.conditional_to_head_transitions, 1,
            "exactly one conditional→head transition expected on the full walk",
        );
        assert_eq!(
            s.head_to_plain_transitions, 1,
            "exactly one head→plain transition expected on the full walk",
        );
    }

    #[test]
    fn strategy_degrade_counter_resets_on_transition() {
        let mut s = StrategyState::default();
        for _ in 0..STRATEGY_DEGRADE_THRESHOLD {
            s.record_degraded();
        }
        assert_eq!(
            s.current,
            RefreshStrategy::HeadThenGet,
            "first transition lands on HeadThenGet",
        );
        // One more degrade should NOT trigger another transition.
        s.record_degraded();
        assert_eq!(
            s.current,
            RefreshStrategy::HeadThenGet,
            "single post-transition degrade must NOT fall straight through to PlainGet — counter must reset on transition",
        );
    }

    #[test]
    fn strategy_choose_does_not_advance_probe_clock_when_conditional() {
        let mut s = StrategyState::default();
        let _ = s.choose_for_tick();
        assert!(
            s.last_probe_at.is_none(),
            "last_probe_at must remain None while at Conditional (no probe to schedule), got {:?}",
            s.last_probe_at,
        );
    }

    #[test]
    fn strategy_choose_does_not_reset_degrade_counter() {
        let mut s = StrategyState {
            current: RefreshStrategy::HeadThenGet,
            degraded_count: 2,
            ..StrategyState::default()
        };
        let _ = s.choose_for_tick();
        assert_eq!(
            s.degraded_count, 2,
            "choose_for_tick must not mutate degraded_count — that's only changed by record_degraded / record_helped",
        );
    }

    // ---- body_hash edge cases ----

    #[test]
    fn body_hash_handles_empty_bytes() {
        let h1 = body_hash(b"");
        let h2 = body_hash(b"");
        assert_eq!(
            h1, h2,
            "empty bytes must hash consistently within a process — otherwise the short-circuit would never fire for empty responses",
        );
    }

    #[test]
    fn body_hash_differs_between_empty_and_single_byte() {
        assert_ne!(
            body_hash(b""),
            body_hash(b"\0"),
            "empty bytes and a single NUL byte must hash to different values — otherwise the short-circuit would conflate them",
        );
    }

    #[test]
    fn body_hash_sensitive_to_byte_order() {
        assert_ne!(
            body_hash(b"ab"),
            body_hash(b"ba"),
            "body_hash must be order-sensitive — same-multiset different-order inputs must hash differently or the short-circuit would skip real changes",
        );
    }

    // ---- next_delay edge cases ----

    #[test]
    fn next_delay_zero_fresh_for_falls_through_to_base() {
        // Server hint of zero seconds should NOT collapse the interval to zero —
        // it falls through to the operator base (which has its own min floor).
        let s = RefreshState {
            validators: CacheHeadersState {
                fresh_for: Some(Duration::ZERO),
                ..CacheHeadersState::default()
            },
            ..RefreshState::default()
        };
        let d = s.next_delay(60).as_secs();
        assert!((54..=66).contains(&d), "got {d}");
    }

    #[test]
    fn next_delay_saturates_under_maximum_failures() {
        let s = RefreshState {
            consecutive_failures: u32::MAX,
            ..RefreshState::default()
        };
        let d = s.next_delay(60).as_secs();
        assert!(d <= 660, "got {d}");
    }

    #[test]
    fn next_delay_huge_base_is_bounded_by_no_failure_path() {
        let s = RefreshState::default();
        let d = s.next_delay(u64::MAX / 1024).as_secs();
        assert!(d > 0, "got {d}");
    }

    // ---- classify_fetch_error: routing for HTTP / network / decode / status ----

    #[test]
    fn classify_max_retries_exceeded_without_status_routes_to_network_error() {
        use http_utils::{HttpRequestError, HttpRequestReasonError};
        let err = HttpRequestError::new(HttpRequestReasonError::MaxRetriesExceeded, None);
        let (kind, outcome) = classify_fetch_error(&err);
        assert_eq!(
            kind, "network error",
            "MaxRetriesExceeded with no status must classify as a transport-level network error",
        );
        assert_eq!(
            outcome,
            RefreshOutcome::NetworkError,
            "no-status MaxRetriesExceeded must surface as NetworkError, got {outcome:?}",
        );
    }

    #[test]
    fn classify_max_retries_exceeded_with_status_routes_to_http_error() {
        use http_utils::{HttpRequestError, HttpRequestReasonError};
        let err = HttpRequestError::new(
            HttpRequestReasonError::MaxRetriesExceeded,
            Some(reqwest::StatusCode::NOT_FOUND),
        );
        let (kind, outcome) = classify_fetch_error(&err);
        assert_eq!(
            kind, "HTTP error",
            "MaxRetriesExceeded carrying a status code means the upstream replied — classify as HttpError",
        );
        assert_eq!(
            outcome,
            RefreshOutcome::HttpError,
            "retry-exhausted 4xx/5xx must surface as HttpError, not NetworkError, got {outcome:?}",
        );
    }

    #[test]
    fn classify_http_status_error_routes_to_http_error() {
        use http_utils::{HttpRequestError, HttpRequestReasonError};
        let err = HttpRequestError::new(
            HttpRequestReasonError::HttpStatusError,
            Some(reqwest::StatusCode::INTERNAL_SERVER_ERROR),
        );
        let (kind, outcome) = classify_fetch_error(&err);
        assert_eq!(kind, "HTTP error");
        assert_eq!(
            outcome,
            RefreshOutcome::HttpError,
            "HttpStatusError variant must surface as HttpError, got {outcome:?}",
        );
    }
}
