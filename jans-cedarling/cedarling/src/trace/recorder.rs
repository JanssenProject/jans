// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Builds the TRACE record of each decision on the authorize path and hands it
//! to the emitter without waiting, so TRACE never delays or changes a decision.

use std::collections::HashMap;
use std::sync::Arc;
use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};

use chrono::Utc;
use serde_json::Value;
use tokio::sync::mpsc;
use tokio::sync::mpsc::error::TrySendError;
use uuid7::Uuid;

use super::record::{DecisionFacts, DraftError, IssuerRef, RawTokenInField, RecordDraft, TokenRef};
use super::status::TraceRecordStatus;
use crate::authz::TraceContext;
use crate::authz::metrics::MetricsCollector;
use crate::authz::request::TokenInput;
use crate::common::policy_store::PolicyStoreWithID;
use crate::jwt::{TokenIssuer, ValidatedToken};
use crate::log::interface::LogWriter;
use crate::log::{BaseLogEntry, LogEntry, LogLevel, Logger};

/// Turns decisions into records for the emitter. Cloned into every `Authz`,
/// including the ones a policy store refresh builds, so all of them feed one
/// emitter and one producer chain.
#[derive(Clone)]
pub(crate) struct TraceRecorder {
    /// `None` when there is no TRACE endpoint to send records to.
    tx: Option<mpsc::Sender<RecordDraft>>,
    log: Logger,
    metrics: Arc<MetricsCollector>,
    /// Shared by the clones, so all of them log one full queue once.
    queue: Arc<QueueState>,
    /// Shared by the clones, like `queue`.
    warned: Arc<WarnedOnce>,
}

/// Whether the emitter queue turns records away. Under a steady load above
/// what Lock takes it stays full, so it is logged when it fills and when it
/// has room again, not for every decision: the decision log and the metrics
/// already carry the status of each one.
#[derive(Default)]
struct QueueState {
    full: AtomicBool,
    turned_away: AtomicU64,
    /// The emitter does not start again, so its stop is warned once and
    /// apart from a full queue.
    stopped: AtomicBool,
}

/// Causes that come from the configuration or from how a client calls, so
/// they repeat on every decision. Each kind is warned the first time and
/// logged at DEBUG after; the metrics count every decision.
#[derive(Default)]
struct WarnedOnce {
    over_limit: AtomicBool,
    invalid_field: AtomicBool,
}

impl TraceRecorder {
    #[cfg_attr(
        target_arch = "wasm32",
        expect(dead_code, reason = "TRACE is not started on WASM")
    )]
    pub(crate) fn new(
        tx: Option<mpsc::Sender<RecordDraft>>,
        log: Logger,
        metrics: Arc<MetricsCollector>,
    ) -> Self {
        Self {
            tx,
            log,
            metrics,
            queue: Arc::default(),
            warned: Arc::default(),
        }
    }

    /// Warns that `store` cannot back a record. Called when an `Authz` is built
    /// on the store, so decisions on it only count the status.
    pub(crate) fn warn_if_untraceable(&self, store: &PolicyStoreWithID) {
        if store.trace_policy().is_some() {
            return;
        }
        self.log.log_any(
            LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                LogLevel::WARN,
                None,
            ))
            .set_message(
                "TRACE records will not be emitted: `trace.policy` needs a policy store loaded \
                 from a .cjar, with an id and a version"
                    .to_string(),
            ),
        );
    }

    /// Hands the record of one decision to the emitter and counts the status.
    /// `facts` runs only when the host passed a TRACE context, so a call without
    /// one does not pay for reading its tokens.
    pub(crate) fn record(
        &self,
        request_id: Uuid,
        trace_context: Option<&Arc<TraceContext>>,
        policy_store: &PolicyStoreWithID,
        facts: impl FnOnce() -> Result<DecisionFacts, DraftError>,
    ) -> TraceRecordStatus {
        let status = self.try_record(request_id, trace_context, policy_store, facts);
        self.metrics.record_trace_status(status);
        status
    }

    fn try_record(
        &self,
        request_id: Uuid,
        trace_context: Option<&Arc<TraceContext>>,
        policy_store: &PolicyStoreWithID,
        facts: impl FnOnce() -> Result<DecisionFacts, DraftError>,
    ) -> TraceRecordStatus {
        let Some(trace) = trace_context else {
            return TraceRecordStatus::NotEmittedMissingContext;
        };
        // Warned once, when the `Authz` was built on this store.
        let Some(policy) = policy_store.trace_policy() else {
            return TraceRecordStatus::NotEmittedInvalidField;
        };
        let draft = facts().and_then(|decision| {
            RecordDraft::new(request_id, signed_at(), trace, decision, Arc::clone(policy))
        });
        let draft = match draft {
            Ok(draft) => draft,
            Err(DraftError::MissingContext(_)) => {
                return TraceRecordStatus::NotEmittedMissingContext;
            },
            Err(err @ DraftError::OverLimit { .. }) => {
                self.warn_first(
                    &self.warned.over_limit,
                    request_id,
                    "later ones are logged at DEBUG and counted in \
                     `trace.not_emitted_over_limit`",
                    || format!("TRACE record not emitted: {err}"),
                );
                return TraceRecordStatus::NotEmittedOverLimit;
            },
            Err(err @ DraftError::InvalidField { .. }) => {
                self.warn_first(
                    &self.warned.invalid_field,
                    request_id,
                    "later ones are logged at DEBUG and counted in \
                     `trace.not_emitted_invalid_field`",
                    || format!("TRACE record not emitted: {err}"),
                );
                return TraceRecordStatus::NotEmittedInvalidField;
            },
        };
        let Some(tx) = &self.tx else {
            return TraceRecordStatus::NotEmittedNoEndpoint;
        };
        match tx.try_send(draft) {
            Ok(()) => {
                self.queue_has_room();
                TraceRecordStatus::Emitted
            },
            Err(TrySendError::Full(_)) => {
                self.turned_away(request_id);
                TraceRecordStatus::NotEmittedQueueFull
            },
            Err(TrySendError::Closed(_)) => {
                if !self.queue.stopped.swap(true, Ordering::Relaxed) {
                    self.warn(
                        request_id,
                        "the emitter has stopped (Cedarling was shut down, or the emitter \
                         ended unexpectedly); this instance emits no more records",
                    );
                }
                TraceRecordStatus::NotEmittedStopped
            },
        }
    }

    /// Counts a record the queue turned away and warns only for the first one.
    fn turned_away(&self, request_id: Uuid) {
        self.queue.turned_away.fetch_add(1, Ordering::Relaxed);
        if !self.queue.full.swap(true, Ordering::Relaxed) {
            self.warn(
                request_id,
                "the emitter queue is full; records are not emitted until it has room",
            );
        }
    }

    /// Ends a full queue, if there was one. The `load` keeps the common case
    /// to one read on the authorize path.
    fn queue_has_room(&self) {
        if !self.queue.full.load(Ordering::Relaxed)
            || !self.queue.full.swap(false, Ordering::Relaxed)
        {
            return;
        }
        let turned_away = self.queue.turned_away.swap(0, Ordering::Relaxed);
        self.log.log_any(
            LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                LogLevel::INFO,
                None,
            ))
            .set_message(format!(
                "TRACE emitter queue has room again; {turned_away} records were not emitted \
                 while it was full"
            )),
        );
    }

    /// WARN for the first of its kind, with `later` saying where the rest go,
    /// and DEBUG for the rest. The message is built only when its level is
    /// logged.
    fn warn_first(
        &self,
        first: &AtomicBool,
        request_id: Uuid,
        later: &str,
        message: impl Fn() -> String,
    ) {
        // The `load` keeps the repeated case to one read on the authorize path.
        let is_first = !first.load(Ordering::Relaxed) && !first.swap(true, Ordering::Relaxed);
        let level = if is_first {
            LogLevel::WARN
        } else {
            LogLevel::DEBUG
        };
        self.log
            .log_fn(BaseLogEntry::new_system(level, request_id).with_fn(|base| {
                let message = if is_first {
                    format!("{}; {later}", message())
                } else {
                    message()
                };
                LogEntry::new(base).set_message(message)
            }));
    }

    fn warn(&self, request_id: Uuid, reason: &str) {
        self.log.log_any(
            LogEntry::new(BaseLogEntry::new_system(LogLevel::WARN, request_id))
                .set_message(format!("TRACE record not emitted: {reason}")),
        );
    }
}

/// Unix seconds of the decision.
fn signed_at() -> u64 {
    u64::try_from(Utc::now().timestamp()).unwrap_or_default()
}

/// References to the tokens a multi-issuer decision used, in request order. The
/// `jti` comes from the token claims; the `mapping` and the raw text come from
/// the request input at the token's index.
///
/// A JWT is named by its `iss` claim, so an issuer Lock rejects stops the
/// record. A custom (non-JWT) token has no issuer URI: it is named by the URN of
/// its custom issuer in the policy store, whatever the host's processor put in
/// `iss`, so the record does not depend on host code.
pub(crate) fn token_facts(
    validated: &HashMap<String, ValidatedToken>,
    inputs: &[TokenInput],
) -> Result<Vec<TokenRef>, DraftError> {
    let mut ordered: Vec<&ValidatedToken> = validated.values().collect();
    ordered.sort_by_key(|validated| validated.index);

    let mut refs = Vec::with_capacity(ordered.len());
    for validated in ordered {
        let position = refs.len();
        // The index comes from validating these same inputs, so this does not
        // happen; if it does, no record is better than one missing a token.
        let Some(input) = inputs.get(validated.index) else {
            return Err(DraftError::InvalidField {
                path: format!("trace.event.tokens[{position}]"),
            });
        };
        let token = &validated.token;
        let claim = |name| token.get_claim_val(name).and_then(Value::as_str);
        let issuer = match &token.iss {
            Some(TokenIssuer::Custom(meta)) => IssuerRef::CustomIssuer(meta.issuer_id.clone()),
            _ => IssuerRef::Url(claim("iss").unwrap_or_default().to_string()),
        };

        let token_ref = TokenRef::new(issuer, &input.mapping, claim("jti"), &input.payload)
            .map_err(|RawTokenInField(field)| DraftError::InvalidField {
                path: format!("trace.event.tokens[{position}].{field}"),
            })?;
        refs.push(token_ref);
    }
    Ok(refs)
}

#[cfg(test)]
mod tests {
    use std::cell::Cell;
    use std::collections::HashMap;
    use std::sync::Arc;

    use jans_trace_core::inputs::token_fingerprint;
    use serde_json::{Value, json};
    use tokio::sync::mpsc;
    use tokio::sync::mpsc::error::TryRecvError;

    use super::*;
    use crate::app_types::PdpID;
    use crate::authz::TraceContext;
    use crate::authz::metrics::{MetricsCollector, MetricsMode};
    use crate::common::policy_store::TrustedIssuer;
    use crate::common::policy_store::test_utils::fixtures;
    use crate::jwt::{CustomTokenIssuerMeta, Token, TokenClaims, TokenIssuer, ValidatedToken};
    use crate::log::{LogStrategy, Logger, TEST_LOGGER, gen_uuid7};
    use crate::trace::TraceRecordStatus;
    use crate::trace::record::{DecisionFacts, DraftError, Outcome, TokenId};
    use crate::trace::test_utils::{CjarStore, cjar_store};
    use crate::{LogConfig, LogLevel, LogStorage, LogTypeConfig, MemoryLogConfig, TokenInput};

    const ISSUER: &str = "https://idp.example.com";

    fn trace() -> Arc<TraceContext> {
        Arc::new(TraceContext::new(
            "exec-1".to_string(),
            "https://gateway.example.com".to_string(),
            "workload-1".to_string(),
        ))
    }

    fn minimal_store() -> PolicyStoreWithID {
        cjar_store(&fixtures::minimal_valid()).store
    }

    fn facts(tokens: impl Into<Arc<[TokenRef]>>) -> DecisionFacts {
        DecisionFacts {
            action: "TestApp::Action::\"read\"".to_string(),
            resource_type: "TestApp::Resource".to_string(),
            outcome: Outcome::Allow,
            tokens: tokens.into(),
        }
    }

    fn metrics() -> Arc<MetricsCollector> {
        Arc::new(MetricsCollector::new(MetricsMode::Local))
    }

    fn memory_log() -> Logger {
        Arc::new(LogStrategy::new(
            &LogConfig {
                log_type: LogTypeConfig::Memory(MemoryLogConfig {
                    log_ttl: 60,
                    max_item_size: None,
                    max_items: None,
                }),
                log_level: LogLevel::DEBUG,
            },
            PdpID::new(),
            None,
        ))
    }

    fn warned(log: &Logger, needle: &str) -> bool {
        log.pop_logs().iter().any(|entry| {
            entry["level"] == "WARN"
                && entry["msg"]
                    .as_str()
                    .is_some_and(|message| message.contains(needle))
        })
    }

    fn validated(mapping: &str, claims: Value, iss: TokenIssuer, index: usize) -> ValidatedToken {
        let claims = TokenClaims::try_from(claims).expect("test claims are a JSON object");
        let token = Token::new(mapping, claims, Some(iss));
        ValidatedToken {
            token: Arc::new(token),
            index,
        }
    }

    /// Validated tokens keyed by their mapping, as multi-issuer validation returns them.
    fn by_mapping(
        tokens: impl IntoIterator<Item = ValidatedToken>,
    ) -> HashMap<String, ValidatedToken> {
        tokens
            .into_iter()
            .map(|validated| (validated.token.name.clone(), validated))
            .collect()
    }

    fn jwt(mapping: &str, claims: Value, index: usize) -> ValidatedToken {
        let issuer = TokenIssuer::Jwt(Arc::new(TrustedIssuer::default()));
        validated(mapping, claims, issuer, index)
    }

    fn custom(mapping: &str, claims: Value, index: usize) -> ValidatedToken {
        let issuer = TokenIssuer::Custom(CustomTokenIssuerMeta {
            issuer_id: "dolphin".to_string(),
            entity_type_name: Some(mapping.to_string()),
            token_id: "dolphin-1".to_string(),
            expiration: None,
        });
        validated(mapping, claims, issuer, index)
    }

    fn input(mapping: &str, payload: &str) -> TokenInput {
        TokenInput::new(mapping.to_string(), payload.to_string())
    }

    #[test]
    fn no_context_is_missing_context_and_facts_not_built() {
        let (tx, mut rx) = mpsc::channel(1);
        let metrics = metrics();
        let recorder = TraceRecorder::new(Some(tx), TEST_LOGGER.clone(), metrics.clone());
        let built = Cell::new(false);

        let status = recorder.record(gen_uuid7(), None, &minimal_store(), || {
            built.set(true);
            Ok(facts(Vec::new()))
        });

        assert_eq!(
            status,
            TraceRecordStatus::NotEmittedMissingContext,
            "a call without a TRACE context has no record"
        );
        assert!(
            !built.get(),
            "without a TRACE context the decision facts should not be built"
        );
        let err = rx.try_recv().expect_err("nothing should reach the emitter");
        assert!(
            matches!(err, TryRecvError::Empty),
            "the channel should be open and empty, got {err:?}"
        );
        assert_eq!(
            metrics
                .snapshot_and_reset()
                .operational_stats
                .get("trace.not_emitted_missing_context"),
            Some(&1),
            "the status should be counted"
        );
    }

    /// Messages of the entries logged at `level` since the last call.
    fn messages(log: &Logger, level: &str) -> Vec<String> {
        log.pop_logs()
            .iter()
            .filter(|entry| entry["level"] == level)
            .filter_map(|entry| entry["msg"].as_str().map(str::to_string))
            .collect()
    }

    #[test]
    fn full_channel_is_queue_full_and_warned_once() {
        let (tx, _rx) = mpsc::channel(1);
        let log = memory_log();
        let recorder = TraceRecorder::new(Some(tx), log.clone(), metrics());
        let clone = recorder.clone();
        let CjarStore { store, .. } = cjar_store(&fixtures::minimal_valid());
        let record = |recorder: &TraceRecorder| {
            recorder.record(gen_uuid7(), Some(&trace()), &store, || {
                Ok(facts(Vec::new()))
            })
        };
        assert_eq!(
            record(&recorder),
            TraceRecordStatus::Emitted,
            "the first record should fill the channel"
        );

        let statuses = [record(&recorder), record(&clone), record(&recorder)];

        assert_eq!(
            statuses,
            [TraceRecordStatus::NotEmittedQueueFull; 3],
            "a full channel should not block the decision"
        );
        let warnings = messages(&log, "WARN");
        assert_eq!(
            warnings.len(),
            1,
            "a full queue is warned once, not per decision, also through a clone: {warnings:?}"
        );
        assert!(
            warnings[0].contains("queue is full"),
            "the warning names the full queue: {}",
            warnings[0]
        );
    }

    #[test]
    fn queue_with_room_again_is_logged_with_the_count() {
        let (tx, mut rx) = mpsc::channel(1);
        let log = memory_log();
        let recorder = TraceRecorder::new(Some(tx), log.clone(), metrics());
        let CjarStore { store, .. } = cjar_store(&fixtures::minimal_valid());
        let record = || {
            recorder.record(gen_uuid7(), Some(&trace()), &store, || {
                Ok(facts(Vec::new()))
            })
        };
        record();
        record();
        record();
        log.pop_logs();

        rx.try_recv().expect("the channel holds the first record");
        assert_eq!(
            record(),
            TraceRecordStatus::Emitted,
            "the drained channel takes a record"
        );
        let infos = messages(&log, "INFO");
        assert!(
            infos
                .iter()
                .any(|message| message.contains("room again") && message.contains('2')),
            "the end of a full queue is logged with how many records it turned away: {infos:?}"
        );

        assert_eq!(
            record(),
            TraceRecordStatus::NotEmittedQueueFull,
            "the channel is full again"
        );
        assert_eq!(
            messages(&log, "WARN").len(),
            1,
            "a queue that fills again is warned again"
        );
    }

    #[test]
    fn closed_channel_is_stopped_and_warned_once() {
        let (tx, rx) = mpsc::channel(1);
        drop(rx);
        let log = memory_log();
        let metrics = metrics();
        let recorder = TraceRecorder::new(Some(tx), log.clone(), metrics.clone());
        let store = minimal_store();
        let statuses: Vec<_> = (0..2)
            .map(|_| {
                recorder.record(gen_uuid7(), Some(&trace()), &store, || {
                    Ok(facts(Vec::new()))
                })
            })
            .collect();

        assert_eq!(
            statuses,
            [TraceRecordStatus::NotEmittedStopped; 2],
            "a stopped emitter is reported as stopped, not as a full queue"
        );
        let stats = metrics.snapshot_and_reset().operational_stats;
        assert_eq!(
            stats.get("trace.not_emitted_stopped"),
            Some(&2),
            "each decision is counted as not emitted for the stopped emitter"
        );
        assert_eq!(
            stats.get("trace.not_emitted_queue_full"),
            None,
            "a stopped emitter is not counted as a full queue"
        );
        let warnings = messages(&log, "WARN");
        assert_eq!(warnings.len(), 1, "the stop is warned once: {warnings:?}");
        assert!(
            warnings[0].contains("emitter has stopped"),
            "the warning names the stop: {}",
            warnings[0]
        );
    }

    #[test]
    fn no_sender_is_no_endpoint() {
        let metrics = metrics();
        let status = TraceRecorder::new(None, TEST_LOGGER.clone(), metrics.clone()).record(
            gen_uuid7(),
            Some(&trace()),
            &minimal_store(),
            || Ok(facts(Vec::new())),
        );
        assert_eq!(
            status,
            TraceRecordStatus::NotEmittedNoEndpoint,
            "without an endpoint there is nowhere to send the record"
        );
        assert_eq!(
            metrics
                .snapshot_and_reset()
                .operational_stats
                .get("trace.not_emitted_no_endpoint"),
            Some(&1),
            "the status should be counted"
        );
    }

    #[test]
    fn records_of_one_store_share_its_policy_facts() {
        let (tx, mut rx) = mpsc::channel(2);
        let CjarStore { store, .. } = cjar_store(&fixtures::minimal_valid());
        let recorder = TraceRecorder::new(Some(tx), TEST_LOGGER.clone(), metrics());
        for _ in 0..2 {
            let status = recorder.record(gen_uuid7(), Some(&trace()), &store, || {
                Ok(facts(Vec::new()))
            });
            assert_eq!(status, TraceRecordStatus::Emitted, "both records are sent");
        }

        let policy = store
            .trace_policy()
            .expect("a .cjar store has policy facts");
        for _ in 0..2 {
            let draft = rx.try_recv().expect("the record is sent");
            assert!(
                Arc::ptr_eq(&draft.policy, policy),
                "a record should share the policy facts of its store instead of copying them"
            );
        }
    }

    #[test]
    fn store_without_bundle_hash_warns_once_and_is_invalid_field() {
        let CjarStore {
            store: archived, ..
        } = cjar_store(&fixtures::minimal_valid());
        let store = PolicyStoreWithID::without_archive(
            archived.id.clone(),
            archived.store.clone(),
            archived.metadata.clone(),
        );
        let log = memory_log();
        let recorder = TraceRecorder::new(None, log.clone(), metrics());
        recorder.warn_if_untraceable(&store);
        assert!(
            warned(&log, "trace.policy"),
            "a store that cannot back a record should be warned about, naming the field"
        );

        for _ in 0..2 {
            let status = recorder.record(gen_uuid7(), Some(&trace()), &store, || {
                Ok(facts(Vec::new()))
            });
            assert_eq!(
                status,
                TraceRecordStatus::NotEmittedInvalidField,
                "a store not loaded from .cjar cannot back a record"
            );
        }
        assert!(
            !warned(&log, "trace.policy"),
            "decisions should not repeat the warning given when the store was loaded"
        );
    }

    #[test]
    fn over_limit_draft_is_over_limit() {
        let log = memory_log();
        let request_id = gen_uuid7();
        let status = TraceRecorder::new(None, log.clone(), metrics()).record(
            request_id,
            Some(&trace()),
            &minimal_store(),
            || {
                let mut facts = facts(Vec::new());
                facts.action = "a".repeat(513);
                Ok(facts)
            },
        );
        assert_eq!(
            status,
            TraceRecordStatus::NotEmittedOverLimit,
            "an action over the Lock limit is not signed"
        );
        let warned = log.pop_logs().iter().any(|entry| {
            entry["level"] == "WARN"
                && entry["request_id"] == request_id.to_string()
                && entry["msg"]
                    .as_str()
                    .is_some_and(|message| message.contains("trace.event.decision.action"))
        });
        assert!(
            warned,
            "the warning should carry the request_id and the path"
        );
    }

    #[test]
    fn custom_token_is_named_by_its_issuer_urn() {
        let CjarStore { store, .. } = cjar_store(&fixtures::minimal_valid());
        let raw = "dolphin-opaque";
        for claims in [
            json!({"sub": "d1"}),
            json!({"iss": "dolphin", "sub": "d1"}),
            json!({"iss": "https://dolphin.example.com", "sub": "d1"}),
        ] {
            let inputs = [
                input("Jans::Access_Token", "header.payload.sig"),
                input("Acme::DolphinToken", raw),
            ];
            let tokens = by_mapping([
                jwt(
                    "Jans::Access_Token",
                    json!({"iss": ISSUER, "jti": "jti-1"}),
                    0,
                ),
                custom("Acme::DolphinToken", claims.clone(), 1),
            ]);

            let refs = token_facts(&tokens, &inputs).expect("token facts build");
            assert_eq!(
                refs[1],
                TokenRef::new(
                    IssuerRef::CustomIssuer("dolphin".to_string()),
                    "Acme::DolphinToken",
                    None,
                    raw,
                )
                .expect("test token ref should build"),
                "a custom token is named by its custom issuer, whatever its iss ({claims})"
            );

            let (tx, mut rx) = mpsc::channel(1);
            let status = TraceRecorder::new(Some(tx), TEST_LOGGER.clone(), metrics()).record(
                gen_uuid7(),
                Some(&trace()),
                &store,
                || Ok(facts(refs)),
            );
            assert_eq!(
                status,
                TraceRecordStatus::Emitted,
                "Lock accepts the issuer URN ({claims})"
            );
            let draft = rx.try_recv().expect("the emitter should get the draft");
            let types: Vec<_> = draft
                .decision
                .tokens
                .iter()
                .map(|token| token.token_type.as_str())
                .collect();
            assert_eq!(
                types,
                ["Jans::Access_Token", "Acme::DolphinToken"],
                "both tokens are in the record ({claims})"
            );
        }
    }

    #[test]
    fn custom_token_keeps_its_jti() {
        let inputs = [input("Acme::DolphinToken", "dolphin-opaque")];
        let tokens = by_mapping([custom("Acme::DolphinToken", json!({"jti": "d-1"}), 0)]);
        let refs = token_facts(&tokens, &inputs).expect("token facts build");
        assert_eq!(
            refs[0].id,
            TokenId::Jti("d-1".to_string()),
            "a custom token is referenced by the jti its processor returned"
        );
    }

    #[test]
    fn jwt_without_lock_issuer_is_invalid_field() {
        let inputs = [input("Jans::Access_Token", "header.payload.sig")];
        let tokens = by_mapping([jwt("Jans::Access_Token", json!({"jti": "jti-1"}), 0)]);
        let log = memory_log();
        let tokens =
            token_facts(&tokens, &inputs).expect("a JWT stays in the list even without an issuer");
        assert_eq!(tokens.len(), 1, "the JWT is not left out");

        let status = TraceRecorder::new(None, log.clone(), metrics()).record(
            gen_uuid7(),
            Some(&trace()),
            &minimal_store(),
            || Ok(facts(tokens)),
        );
        assert_eq!(
            status,
            TraceRecordStatus::NotEmittedInvalidField,
            "a JWT issuer Lock rejects stops the record"
        );
        assert!(
            warned(&log, "trace.event.tokens[0].issuer"),
            "the warning should name the field"
        );
    }

    #[test]
    fn token_without_jti_uses_fingerprint() {
        let raw = "header.payload.sig";
        let inputs = [input("Jans::Access_Token", raw)];
        let tokens = by_mapping([jwt("Jans::Access_Token", json!({"iss": ISSUER}), 0)]);
        let refs = token_facts(&tokens, &inputs).expect("token facts build");
        assert_eq!(
            refs[0].id,
            TokenId::Fingerprint(token_fingerprint(raw)),
            "a token without jti is referenced by the fingerprint of its text"
        );
        assert_eq!(
            refs[0].issuer,
            IssuerRef::Url(ISSUER.to_string()),
            "issuer comes from the iss claim"
        );
    }

    #[test]
    fn tokens_follow_request_order() {
        let inputs = [
            input("A::T", "raw-a"),
            input("B::T", "raw-b"),
            input("C::T", "raw-c"),
        ];
        let tokens = by_mapping([
            jwt("C::T", json!({"iss": ISSUER, "jti": "c"}), 2),
            jwt("A::T", json!({"iss": ISSUER, "jti": "a"}), 0),
            jwt("B::T", json!({"iss": ISSUER, "jti": "b"}), 1),
        ]);
        let refs = token_facts(&tokens, &inputs).expect("token facts build");
        let types: Vec<_> = refs.iter().map(|token| token.token_type.as_str()).collect();
        assert_eq!(
            types,
            ["A::T", "B::T", "C::T"],
            "tokens keep the request order"
        );
    }

    #[test]
    fn raw_token_in_jti_is_invalid_field() {
        let raw = "header.payload.sig";
        let inputs = [input("Jans::Access_Token", raw)];
        let tokens = by_mapping([jwt(
            "Jans::Access_Token",
            json!({"iss": ISSUER, "jti": raw}),
            0,
        )]);
        let err = token_facts(&tokens, &inputs)
            .expect_err("a jti that holds the token must not be signed");
        assert!(
            matches!(&err, DraftError::InvalidField { path } if path == "trace.event.tokens[0].jti"),
            "expected InvalidField for jti, got {err:?}"
        );
    }

    fn over_limit() -> DecisionFacts {
        let mut facts = facts(Vec::new());
        facts.action = "a".repeat(513);
        facts
    }

    fn invalid_field() -> DecisionFacts {
        facts(vec![
            TokenRef::new(
                IssuerRef::Url(String::new()),
                "Jans::Access_Token",
                Some("jti-1"),
                "header.payload.sig",
            )
            .expect("test token ref should build"),
        ])
    }

    #[test]
    fn repeated_over_limit_is_warned_once_then_logged_at_debug() {
        let log = memory_log();
        let metrics = metrics();
        let recorder = TraceRecorder::new(None, log.clone(), metrics.clone());
        let clone = recorder.clone();
        let store = minimal_store();
        for recorder in [&recorder, &clone, &recorder] {
            let status = recorder.record(gen_uuid7(), Some(&trace()), &store, || Ok(over_limit()));
            assert_eq!(
                status,
                TraceRecordStatus::NotEmittedOverLimit,
                "each decision is over the limit"
            );
        }

        let logs = log.pop_logs();
        let at = |level: &str| -> Vec<String> {
            logs.iter()
                .filter(|entry| entry["level"] == level)
                .filter_map(|entry| entry["msg"].as_str().map(str::to_string))
                .collect()
        };
        let warnings = at("WARN");
        assert_eq!(
            warnings.len(),
            1,
            "one cause is warned once, also through a clone: {warnings:?}"
        );
        assert!(
            warnings[0].contains("trace.event.decision.action")
                && warnings[0].contains("trace.not_emitted_over_limit"),
            "the warning names the field and where the rest are counted: {}",
            warnings[0]
        );
        let debugs = at("DEBUG");
        assert_eq!(
            debugs.len(),
            2,
            "the later decisions are logged at DEBUG: {debugs:?}"
        );
        assert!(
            debugs
                .iter()
                .all(|message| message.contains("trace.event.decision.action")),
            "the DEBUG entries keep the field: {debugs:?}"
        );
        assert_eq!(
            metrics
                .snapshot_and_reset()
                .operational_stats
                .get("trace.not_emitted_over_limit"),
            Some(&3),
            "every decision is counted"
        );
    }

    #[test]
    fn each_kind_is_warned_once_on_its_own() {
        let log = memory_log();
        let recorder = TraceRecorder::new(None, log.clone(), metrics());
        let store = minimal_store();
        let record = |facts: fn() -> DecisionFacts| {
            recorder.record(gen_uuid7(), Some(&trace()), &store, || Ok(facts()))
        };

        let mut warnings = Vec::new();
        for _ in 0..2 {
            assert_eq!(
                record(over_limit),
                TraceRecordStatus::NotEmittedOverLimit,
                "an action over the limit is over the limit"
            );
            assert_eq!(
                record(invalid_field),
                TraceRecordStatus::NotEmittedInvalidField,
                "an empty issuer is an invalid field"
            );
            warnings.extend(messages(&log, "WARN"));
        }

        assert_eq!(
            warnings.len(),
            2,
            "each kind is warned once, whatever the other kinds did: {warnings:?}"
        );
        for needle in [
            "trace.event.decision.action",
            "trace.event.tokens[0].issuer",
        ] {
            assert!(
                warnings.iter().any(|message| message.contains(needle)),
                "a warning names {needle}: {warnings:?}"
            );
        }
    }

    #[test]
    fn repeated_cause_is_not_built_above_its_log_level() {
        let log = Arc::new(LogStrategy::new(
            &LogConfig {
                log_type: LogTypeConfig::Memory(MemoryLogConfig {
                    log_ttl: 60,
                    max_item_size: None,
                    max_items: None,
                }),
                log_level: LogLevel::WARN,
            },
            PdpID::new(),
            None,
        ));
        let recorder = TraceRecorder::new(None, log.clone(), metrics());
        let first = AtomicBool::new(false);
        let built = Cell::new(0);
        for _ in 0..3 {
            recorder.warn_first(
                &first,
                gen_uuid7(),
                "later ones are logged at DEBUG",
                || {
                    built.set(built.get() + 1);
                    "cause".to_string()
                },
            );
        }

        assert_eq!(
            built.get(),
            1,
            "only the WARN message is built when DEBUG is not logged"
        );
        assert_eq!(
            messages(&log, "WARN"),
            ["cause; later ones are logged at DEBUG"],
            "the first one is warned with where the rest go"
        );
    }
}
