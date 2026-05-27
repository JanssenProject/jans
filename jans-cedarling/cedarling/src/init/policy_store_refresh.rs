// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Background refresh worker for remote policy store sources.
//!
//! On each tick the worker sends a conditional GET to the configured URL,
//! handling `304 Not Modified` cheaply. When the upstream returns a new body
//! and it parses successfully, the worker builds a fresh [`Authz`] (including a
//! freshly built [`JwtService`] and [`EntityBuilder`]) and publishes it via
//! [`ArcSwap`] so in-flight authorizations are unaffected. Any failure leaves
//! the previously loaded [`Authz`] in place.
//!
//! The worker observes a shutdown signal from a [`futures::channel::oneshot`]
//! receiver — dropping the [`PolicyStoreRefreshHandle`] closes the channel and
//! the worker exits at its next select boundary.

use arc_swap::ArcSwap;
use chrono::Utc;
use futures::channel::oneshot;
use futures::future::{Either, select};
use std::collections::hash_map::DefaultHasher;
use std::hash::Hasher;
use std::sync::Arc;
use std::sync::atomic::{AtomicU64, Ordering};
use std::time::Duration;

use crate::PolicyStoreConfig;
use crate::PolicyStoreSource;
use crate::async_sleep::sleep;
use crate::authz::metrics::MetricsCollector;
use crate::authz::{Authz, AuthzConfig};
use crate::bootstrap_config::{AuthorizationConfig, JwtConfig};
use crate::common::policy_store::PolicyStoreWithID;
use crate::context_data_api::DataStore;
use crate::entity_builder::{EntityBuilder, TrustedIssuerIndex};
use crate::http::cache_headers::CacheValidators;
use crate::http::{ConditionalFetch, HttpClient};
use crate::http::{JoinHandle, spawn_task};
use crate::jwt::JwtService;
use crate::log::interface::LogWriter;
use crate::log::{BaseLogEntry, LogEntry, LogLevel, Logger};

use super::policy_store::{PolicyStoreLoadError, parse_cjar_bytes, parse_lock_master_bytes};

/// Upper bound on exponential backoff between failed refresh attempts.
const REFRESH_FAILURE_BACKOFF_MAX_SECS: u64 = 600;

/// Outcome of a single refresh tick — encoded as `i64` for emission via the
/// integer-enum `policy_store_refresh.last_outcome` metric.
///
/// Note: `0` is intentionally **not** assigned to any outcome so that the
/// metrics snapshot's default value (also `0`) means "no refresh attempt
/// observed yet" rather than aliasing with `Success`.
#[repr(i64)]
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum RefreshOutcome {
    Success = 1,
    NotModified = 2,
    HttpError = 3,
    NetworkError = 4,
    ParseError = 5,
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
    /// Build a brand-new [`Authz`] from a fresh policy store. Rebuilds the JWT
    /// service and entity builder so trusted-issuer and schema changes take
    /// effect
    pub(crate) async fn rebuild(
        &self,
        policy_store: PolicyStoreWithID,
    ) -> Result<Authz, RebuildError> {
        policy_store
            .validate_trusted_issuers()
            .map_err(|e| RebuildError::TrustedIssuers(e.to_string()))?;

        let trusted_issuers = policy_store.trusted_issuers.clone();
        let jwt_service = JwtService::new(
            &self.jwt_config,
            trusted_issuers.clone(),
            Some(self.log.clone()),
            self.metrics.clone(),
            self.http_client.clone(),
        )
        .await
        .map_err(|e| RebuildError::JwtService(e.to_string()))?;
        let jwt_service = Arc::new(jwt_service);

        let issuers_map = trusted_issuers.unwrap_or_default();
        let issuers_index = TrustedIssuerIndex::new(&issuers_map, Some(&self.log));
        let schema = &policy_store.schema.validator_schema;
        let default_entities = policy_store.default_entities.entities().to_owned();
        let entity_builder = EntityBuilder::new(issuers_index, Some(schema), default_entities)
            .map_err(|e| RebuildError::EntityBuilder(e.to_string()))?;
        let entity_builder = Arc::new(entity_builder);

        let config = AuthzConfig {
            log_service: self.log.clone(),
            policy_store,
            jwt_service,
            entity_builder,
            authorization: self.authorization_config.clone(),
            data_store: self.data_store.clone(),
            metrics: self.metrics.clone(),
        };

        Ok(Authz::new(config))
    }
}

#[derive(Debug, thiserror::Error)]
pub(crate) enum RebuildError {
    #[error("trusted issuers validation failed: {0}")]
    TrustedIssuers(String),
    #[error("failed to initialize JWT service: {0}")]
    JwtService(String),
    #[error("failed to initialize entity builder: {0}")]
    EntityBuilder(String),
}

/// Mutable per-source state — what we learned from the previous response.
#[derive(Default)]
struct RefreshState {
    validators: CacheValidators,
    last_body_hash: Option<u64>,
    consecutive_failures: u32,
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

fn body_hash(bytes: &[u8]) -> u64 {
    let mut h = DefaultHasher::new();
    h.write(bytes);
    h.finish()
}

/// Identifies which kind of URL-based source we are refreshing — the parse step
/// differs (JSON for Lock Master, ZIP archive for `.cjar`).
#[derive(Debug, Clone)]
pub(crate) enum RefreshSource {
    LockServer { url: String },
    CjarUrl { url: String },
}

impl RefreshSource {
    pub(crate) fn from_policy_store_source(src: &PolicyStoreSource) -> Option<Self> {
        match src {
            PolicyStoreSource::LockServer(u) => Some(Self::LockServer { url: u.clone() }),
            PolicyStoreSource::CjarUrl(u) => Some(Self::CjarUrl { url: u.clone() }),
            _ => None,
        }
    }

    fn url(&self) -> &str {
        match self {
            Self::LockServer { url } | Self::CjarUrl { url } => url,
        }
    }

    async fn parse(&self, bytes: &[u8]) -> Result<PolicyStoreWithID, PolicyStoreLoadError> {
        match self {
            Self::LockServer { .. } => parse_lock_master_bytes(bytes),
            Self::CjarUrl { .. } => parse_cjar_bytes(bytes).await,
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

/// Spawn the background refresh worker. Returns a [`PolicyStoreRefreshHandle`]
/// whose `Drop` signals the worker to exit.
#[allow(clippy::too_many_arguments)]
pub(crate) fn spawn_refresh_worker(
    source: RefreshSource,
    interval_secs: u64,
    http_client: HttpClient,
    rebuilder: AuthzRebuilder,
    authz_swap: Arc<ArcSwap<Authz>>,
    metrics: Arc<MetricsCollector>,
    log: Logger,
) -> PolicyStoreRefreshHandle {
    let (shutdown_tx, shutdown_rx) = oneshot::channel::<()>();

    let join = spawn_task(async move {
        run_worker(
            source,
            interval_secs,
            http_client,
            rebuilder,
            authz_swap,
            metrics,
            log,
            shutdown_rx,
        )
        .await;
    });

    PolicyStoreRefreshHandle {
        shutdown: Some(shutdown_tx),
        _join: join,
    }
}

#[allow(clippy::too_many_arguments)]
async fn run_worker(
    source: RefreshSource,
    interval_secs: u64,
    http_client: HttpClient,
    rebuilder: AuthzRebuilder,
    authz_swap: Arc<ArcSwap<Authz>>,
    metrics: Arc<MetricsCollector>,
    log: Logger,
    shutdown_rx: oneshot::Receiver<()>,
) {
    let mut state = RefreshState::default();
    let mut shutdown_rx = shutdown_rx;

    log.log_any(
        LogEntry::new(BaseLogEntry::new_system_opt_request_id(
            LogLevel::INFO,
            None,
        ))
        .set_message(format!(
            "policy store refresh worker started (url={}, interval={}s)",
            source.url(),
            interval_secs,
        )),
    );

    loop {
        let delay = state.next_delay(interval_secs);
        let sleep_fut = sleep(delay);
        futures::pin_mut!(sleep_fut);

        match select(sleep_fut, &mut shutdown_rx).await {
            Either::Left(((), _)) => {
                let outcome = tick(
                    &source,
                    &http_client,
                    &rebuilder,
                    &authz_swap,
                    &mut state,
                    &log,
                )
                .await;
                metrics.record_policy_store_refresh(outcome);
            },
            Either::Right(_) => {
                log.log_any(
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

async fn tick(
    source: &RefreshSource,
    http_client: &HttpClient,
    rebuilder: &AuthzRebuilder,
    authz_swap: &Arc<ArcSwap<Authz>>,
    state: &mut RefreshState,
    log: &Logger,
) -> RefreshOutcome {
    let url = source.url();
    let start = Utc::now();

    let fetch_result = http_client
        .get_bytes_conditional(url, &state.validators)
        .await;

    match fetch_result {
        Err(e) => {
            state.consecutive_failures = state.consecutive_failures.saturating_add(1);
            let is_net = e.is_max_retries_exceeded();
            log.log_any(
                LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                    LogLevel::WARN,
                    None,
                ))
                .set_message(format!(
                    "policy store refresh: {} against {url}: {e}",
                    if is_net {
                        "network error"
                    } else {
                        "HTTP error"
                    }
                )),
            );
            if is_net {
                RefreshOutcome::NetworkError
            } else {
                RefreshOutcome::HttpError
            }
        },
        Ok(ConditionalFetch::NotModified) => {
            state.consecutive_failures = 0;
            RefreshOutcome::NotModified
        },
        Ok(ConditionalFetch::Modified { bytes, validators }) => {
            let new_hash = body_hash(&bytes);

            // Servers sometimes return 200 with byte-identical bodies even when
            // we sent valid conditional headers. Short-circuit.
            if Some(new_hash) == state.last_body_hash {
                state.validators = validators;
                state.consecutive_failures = 0;
                return RefreshOutcome::NotModified;
            }

            let parsed = match source.parse(&bytes).await {
                Ok(p) => p,
                Err(e) => {
                    state.consecutive_failures = state.consecutive_failures.saturating_add(1);
                    log.log_any(
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

            let new_authz = match rebuilder.rebuild(parsed).await {
                Ok(a) => a,
                Err(e) => {
                    state.consecutive_failures = state.consecutive_failures.saturating_add(1);
                    log.log_any(
                        LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                            LogLevel::ERROR,
                            None,
                        ))
                        .set_message(format!(
                            "policy store refresh: rebuild failure for {url}: {e}"
                        )),
                    );
                    return RefreshOutcome::ParseError;
                },
            };

            authz_swap.store(Arc::new(new_authz));
            state.validators = validators;
            state.last_body_hash = Some(new_hash);
            state.consecutive_failures = 0;

            let elapsed = Utc::now().signed_duration_since(start).num_milliseconds();
            log.log_any(
                LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                    LogLevel::INFO,
                    None,
                ))
                .set_message(format!(
                    "policy store refresh: swapped new store from {url} in {elapsed} ms"
                )),
            );

            RefreshOutcome::Success
        },
    }
}

#[cfg(test)]
mod tests {
    use super::*;

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
            validators: CacheValidators {
                fresh_for: Some(Duration::from_secs(60)),
                ..CacheValidators::default()
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
            validators: CacheValidators {
                fresh_for: Some(Duration::from_secs(3600)),
                ..CacheValidators::default()
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
        assert_eq!(a, b);
    }

    #[test]
    fn body_hash_differs_for_different_bytes() {
        assert_ne!(body_hash(b"hello"), body_hash(b"world"));
    }

    #[test]
    fn refresh_source_construction_from_url() {
        let s =
            RefreshSource::from_policy_store_source(&PolicyStoreSource::CjarUrl("http://x".into()));
        assert!(matches!(s, Some(RefreshSource::CjarUrl { .. })));

        let s = RefreshSource::from_policy_store_source(&PolicyStoreSource::LockServer(
            "http://y".into(),
        ));
        assert!(matches!(s, Some(RefreshSource::LockServer { .. })));

        let s = RefreshSource::from_policy_store_source(&PolicyStoreSource::Yaml("..".into()));
        assert!(s.is_none());
    }
}
