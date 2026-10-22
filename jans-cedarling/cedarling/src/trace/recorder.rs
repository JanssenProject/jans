// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Builds the TRACE record of each decision on the authorize path and hands it
//! to the emitter without waiting, so TRACE never delays or changes a decision.

use std::collections::HashMap;
use std::sync::Arc;

use chrono::Utc;
use serde_json::Value;
use tokio::sync::mpsc;
use tokio::sync::mpsc::error::TrySendError;
use uuid7::Uuid;

use super::record::{
    DecisionFacts, DraftError, RawTokenInField, RecordDraft, TokenRef, is_lock_issuer,
};
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
        Self { tx, log, metrics }
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
                self.warn(request_id, &err.to_string());
                return TraceRecordStatus::NotEmittedOverLimit;
            },
            Err(err @ DraftError::InvalidField { .. }) => {
                self.warn(request_id, &err.to_string());
                return TraceRecordStatus::NotEmittedInvalidField;
            },
        };
        let Some(tx) = &self.tx else {
            return TraceRecordStatus::NotEmittedNoEndpoint;
        };
        match tx.try_send(draft) {
            Ok(()) => TraceRecordStatus::Emitted,
            Err(TrySendError::Full(_)) => {
                self.warn(request_id, "the emitter queue is full");
                TraceRecordStatus::NotEmittedQueueFull
            },
            Err(TrySendError::Closed(_)) => {
                self.warn(request_id, "the emitter has stopped");
                TraceRecordStatus::NotEmittedQueueFull
            },
        }
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
/// `issuer` and `jti` come from the token claims; the `mapping` and the raw text
/// come from the request input at the token's index.
///
/// A custom token whose issuer Lock would reject is left out with a warning:
/// Cedarling cannot name its issuer, and the decision still has a record. A JWT
/// stays in, so an issuer Lock rejects stops the record instead.
pub(crate) fn token_facts(
    validated: &HashMap<String, ValidatedToken>,
    inputs: &[TokenInput],
    log: &Logger,
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
        let issuer = claim("iss");

        if matches!(token.iss, Some(TokenIssuer::Custom(_))) && !issuer.is_some_and(is_lock_issuer)
        {
            log.log_any(
                LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                    LogLevel::WARN,
                    None,
                ))
                .set_message(format!(
                    "custom token `{}` has no issuer Lock accepts; it is left out of the TRACE \
                     record",
                    input.mapping
                )),
            );
            continue;
        }

        let token_ref = TokenRef::new(
            issuer.unwrap_or_default(),
            &input.mapping,
            claim("jti"),
            &input.payload,
        )
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

    fn facts(tokens: Vec<TokenRef>) -> DecisionFacts {
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
        assert!(rx.try_recv().is_err(), "nothing should reach the emitter");
        assert_eq!(
            metrics
                .snapshot_and_reset()
                .operational_stats
                .get("trace.not_emitted_missing_context"),
            Some(&1),
            "the status should be counted"
        );
    }

    #[test]
    fn full_channel_is_queue_full() {
        let (tx, _rx) = mpsc::channel(1);
        let log = memory_log();
        let recorder = TraceRecorder::new(Some(tx), log.clone(), metrics());
        let CjarStore { store, .. } = cjar_store(&fixtures::minimal_valid());
        let first = recorder.record(gen_uuid7(), Some(&trace()), &store, || {
            Ok(facts(Vec::new()))
        });
        assert_eq!(
            first,
            TraceRecordStatus::Emitted,
            "the first record should fill the channel"
        );

        let status = recorder.record(gen_uuid7(), Some(&trace()), &store, || {
            Ok(facts(Vec::new()))
        });

        assert_eq!(
            status,
            TraceRecordStatus::NotEmittedQueueFull,
            "a full channel should not block the decision"
        );
        assert!(warned(&log, "queue"), "a dropped record should be warned");
    }

    #[test]
    fn closed_channel_is_queue_full() {
        let (tx, rx) = mpsc::channel(1);
        drop(rx);
        let status = TraceRecorder::new(Some(tx), TEST_LOGGER.clone(), metrics()).record(
            gen_uuid7(),
            Some(&trace()),
            &minimal_store(),
            || Ok(facts(Vec::new())),
        );
        assert_eq!(
            status,
            TraceRecordStatus::NotEmittedQueueFull,
            "a stopped emitter takes no records"
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
    fn custom_token_without_lock_issuer_is_left_out() {
        let CjarStore { store, .. } = cjar_store(&fixtures::minimal_valid());
        for claims in [json!({"sub": "d1"}), json!({"iss": "dolphin", "sub": "d1"})] {
            let log = memory_log();
            let inputs = [
                input("Jans::Access_Token", "header.payload.sig"),
                input("Acme::DolphinToken", "dolphin-opaque"),
            ];
            let tokens = by_mapping([
                jwt(
                    "Jans::Access_Token",
                    json!({"iss": ISSUER, "jti": "jti-1"}),
                    0,
                ),
                custom("Acme::DolphinToken", claims.clone(), 1),
            ]);

            let refs = token_facts(&tokens, &inputs, &log)
                .expect("a custom token Lock cannot name is left out, not an error");
            assert!(
                warned(&log, "Acme::DolphinToken"),
                "leaving out a token should be warned with its mapping ({claims})"
            );

            let (tx, mut rx) = mpsc::channel(1);
            let status = TraceRecorder::new(Some(tx), log, metrics()).record(
                gen_uuid7(),
                Some(&trace()),
                &store,
                || Ok(facts(refs)),
            );
            assert_eq!(
                status,
                TraceRecordStatus::Emitted,
                "the record is still emitted"
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
                ["Jans::Access_Token"],
                "only the token Lock can name is in the record ({claims})"
            );
        }
    }

    #[test]
    fn custom_token_with_lock_issuer_is_kept() {
        let inputs = [input("Acme::DolphinToken", "dolphin-opaque")];
        let tokens = by_mapping([custom(
            "Acme::DolphinToken",
            json!({"iss": "https://dolphin.example.com", "jti": "d-1"}),
            0,
        )]);
        let refs = token_facts(&tokens, &inputs, &TEST_LOGGER).expect("token facts build");
        assert_eq!(
            refs,
            [TokenRef::new(
                "https://dolphin.example.com",
                "Acme::DolphinToken",
                Some("d-1"),
                "dolphin-opaque",
            )
            .expect("test token ref should build")],
            "a custom token with an issuer Lock accepts is referenced"
        );
    }

    #[test]
    fn jwt_without_lock_issuer_is_invalid_field() {
        let inputs = [input("Jans::Access_Token", "header.payload.sig")];
        let tokens = by_mapping([jwt("Jans::Access_Token", json!({"jti": "jti-1"}), 0)]);
        let log = memory_log();
        let refs = token_facts(&tokens, &inputs, &log)
            .expect("a JWT stays in the list even without an issuer");
        assert_eq!(refs.len(), 1, "the JWT is not left out");

        let status = TraceRecorder::new(None, log.clone(), metrics()).record(
            gen_uuid7(),
            Some(&trace()),
            &minimal_store(),
            || Ok(facts(refs)),
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
        let refs = token_facts(&tokens, &inputs, &TEST_LOGGER).expect("token facts build");
        assert_eq!(
            refs[0].id,
            TokenId::Fingerprint(token_fingerprint(raw)),
            "a token without jti is referenced by the fingerprint of its text"
        );
        assert_eq!(refs[0].issuer, ISSUER, "issuer comes from the iss claim");
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
        let refs = token_facts(&tokens, &inputs, &TEST_LOGGER).expect("token facts build");
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
        let err = token_facts(&tokens, &inputs, &TEST_LOGGER)
            .expect_err("a jti that holds the token must not be signed");
        assert!(
            matches!(&err, DraftError::InvalidField { path } if path == "trace.event.tokens[0].jti"),
            "expected InvalidField for jti, got {err:?}"
        );
    }
}
