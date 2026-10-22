// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Facts of one authorization decision, checked against what Lock accepts.
//!
//! Everything here is checked before signing: a record Lock would reject is
//! never signed, so the producer chain gets no gap. Lengths are
//! counted in UTF-16 units, because Lock counts Java `String` length.

use std::sync::Arc;

use jans_trace_core::Digest;
use jans_trace_core::inputs::token_fingerprint;
use url::Url;
use uuid7::Uuid;

use crate::authz::TraceContext;
use crate::common::policy_store::PolicyStoreMetadata;

/// Lock `MAX_FACT_TEXT_LENGTH` for `action` and `resource_type`
/// (`EventKindValidationSupport`).
const MAX_FACT_TEXT: usize = 512;
/// Lock `MAX_SHORT_STRING` for `trace_execution_id` and `execution_authority`
/// (`CommonAssertionValidator`).
const MAX_EXECUTION_FIELD: usize = 255;
/// Lock `MAX_SUBJECT_ID_LENGTH` for `workload_id` and `workload_instance_id`.
const MAX_SUBJECT_ID: usize = 255;
/// Lock `MAX_TOKEN_TYPE_LENGTH`.
const MAX_TOKEN_TYPE: usize = 64;
/// Lock `MAX_JTI_LENGTH`.
const MAX_JTI: usize = 256;
/// Operational limit from the TRACE design; Lock itself caps arrays at 256.
const MAX_TOKENS: usize = 32;

/// Cedar decision as Lock names it.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum Outcome {
    Allow,
    Deny,
}

impl Outcome {
    pub(crate) fn from_allowed(allowed: bool) -> Self {
        if allowed { Self::Allow } else { Self::Deny }
    }

    /// Read only by the producer chain, which signs on native targets only.
    #[cfg(not(target_arch = "wasm32"))]
    pub(crate) fn as_str(self) -> &'static str {
        match self {
            Self::Allow => "ALLOW",
            Self::Deny => "DENY",
        }
    }
}

/// What was decided: the singular `decision` and the tokens it used.
#[derive(Debug, Clone, PartialEq)]
pub(crate) struct DecisionFacts {
    pub(crate) action: String,
    pub(crate) resource_type: String,
    pub(crate) outcome: Outcome,
    /// Empty for `authorize_unsigned`, which uses no tokens.
    pub(crate) tokens: Vec<TokenRef>,
}

/// A token reference: never the token itself, which must not leave Cedarling.
#[derive(Debug, Clone, PartialEq)]
pub(crate) struct TokenRef {
    pub(crate) issuer: String,
    /// The request `mapping` as is, e.g. `Jans::Access_Token`.
    pub(crate) token_type: String,
    pub(crate) id: TokenId,
}

/// `jti` when the token has one, otherwise a fingerprint of the token text.
#[derive(Debug, Clone, PartialEq)]
pub(crate) enum TokenId {
    Jti(String),
    Fingerprint(Digest),
}

impl TokenRef {
    /// An empty `jti` counts as none: Lock rejects an empty one. Refuses a field
    /// that holds `raw_token` itself, so a wiring mistake cannot sign the token
    /// into the record.
    pub(crate) fn new(
        issuer: &str,
        mapping: &str,
        jti: Option<&str>,
        raw_token: &str,
    ) -> Result<Self, RawTokenInField> {
        let jti = jti.filter(|jti| !jti.is_empty());
        let holds_token = |text: &str| !raw_token.is_empty() && text.contains(raw_token);
        for (field, text) in [
            ("issuer", Some(issuer)),
            ("token_type", Some(mapping)),
            ("jti", jti),
        ] {
            if text.is_some_and(holds_token) {
                return Err(RawTokenInField(field));
            }
        }
        let id = match jti {
            Some(jti) => TokenId::Jti(jti.to_string()),
            None => TokenId::Fingerprint(token_fingerprint(raw_token)),
        };
        Ok(Self {
            issuer: issuer.to_string(),
            token_type: mapping.to_string(),
            id,
        })
    }
}

/// A token reference field holds the token it describes.
#[derive(Debug, Clone, PartialEq, thiserror::Error)]
#[error("TRACE token field `{0}` holds the raw token")]
pub(crate) struct RawTokenInField(pub(crate) &'static str);

/// Which policy store decided.
#[derive(Debug, Clone, PartialEq)]
pub(crate) struct PolicyFacts {
    pub(crate) store_id: String,
    pub(crate) store_version: String,
    pub(crate) language_version: String,
    pub(crate) bundle_hash: Digest,
}

impl PolicyFacts {
    /// `None` when the store cannot back a record: no `.cjar` (`bundle_hash`),
    /// no metadata, or an empty id or version, all of which Lock requires.
    pub(crate) fn from_archive(
        bundle_hash: Option<Digest>,
        metadata: Option<&PolicyStoreMetadata>,
    ) -> Option<Self> {
        let bundle_hash = bundle_hash?;
        let metadata = metadata?;
        let non_empty = |value: &str| (!value.is_empty()).then(|| value.to_string());
        Some(Self {
            store_id: non_empty(metadata.id())?,
            store_version: non_empty(metadata.version())?,
            // The Cedar version this build evaluates with, as Cedarling logs it.
            language_version: cedar_policy::get_lang_version().to_string(),
            bundle_hash,
        })
    }
}

/// Facts of one record. The producer, its instance (`pdp_id`), the chain
/// position and the signature come from the producer chain.
#[derive(Debug, Clone, PartialEq)]
pub(crate) struct RecordDraft {
    pub(crate) record_id: Uuid,
    pub(crate) signed_at: u64,
    pub(crate) trace: TraceContext,
    pub(crate) decision: DecisionFacts,
    pub(crate) policy: Arc<PolicyFacts>,
}

/// Why no record is built. Each maps to a `trace_record_status`:
/// `MissingContext` to `not_emitted_missing_context`, `OverLimit` to
/// `not_emitted_over_limit`, `InvalidField` to `not_emitted_invalid_field`.
#[derive(Debug, Clone, PartialEq, thiserror::Error)]
pub(crate) enum DraftError {
    /// A mandatory `TraceContext` field is empty, which means no context.
    #[error("TRACE context field `{0}` is empty")]
    MissingContext(&'static str),
    /// A value is longer, or a list larger, than Lock accepts; truncating would
    /// sign incomplete facts.
    #[error("`{path}` is over the Lock limit of {limit}")]
    OverLimit { path: String, limit: usize },
    /// A value Lock rejects for its form: empty, or a non-`https` issuer.
    #[error("`{path}` has a value Lock rejects")]
    InvalidField { path: String },
}

impl RecordDraft {
    pub(crate) fn new(
        record_id: Uuid,
        signed_at: u64,
        trace: &TraceContext,
        decision: DecisionFacts,
        policy: Arc<PolicyFacts>,
    ) -> Result<Self, DraftError> {
        require_context(&trace.execution_id, "execution_id")?;
        require_context(&trace.execution_authority, "execution_authority")?;
        require_context(&trace.workload_id, "workload_id")?;
        check_len(
            &trace.execution_id,
            "trace.trace_execution_id",
            MAX_EXECUTION_FIELD,
        )?;
        check_len(
            &trace.execution_authority,
            "trace.execution_authority",
            MAX_EXECUTION_FIELD,
        )?;
        check_len(
            &trace.workload_id,
            "trace.subject.workload_id",
            MAX_SUBJECT_ID,
        )?;
        if let Some(instance) = &trace.workload_instance_id {
            check_len(
                instance,
                "trace.subject.workload_instance_id",
                MAX_SUBJECT_ID,
            )?;
        }

        check_fact_text(&decision.action, "trace.event.decision.action")?;
        check_fact_text(
            &decision.resource_type,
            "trace.event.decision.resource_type",
        )?;
        if decision.tokens.len() > MAX_TOKENS {
            return Err(DraftError::OverLimit {
                path: "trace.event.tokens".to_string(),
                limit: MAX_TOKENS,
            });
        }
        for (index, token) in decision.tokens.iter().enumerate() {
            check_token(token, index)?;
        }

        Ok(Self {
            record_id,
            signed_at,
            trace: trace.clone(),
            decision,
            policy,
        })
    }
}

fn require_context(value: &str, field: &'static str) -> Result<(), DraftError> {
    if value.is_empty() {
        return Err(DraftError::MissingContext(field));
    }
    Ok(())
}

fn check_len(value: &str, path: &str, limit: usize) -> Result<(), DraftError> {
    if value.encode_utf16().count() > limit {
        return Err(DraftError::OverLimit {
            path: path.to_string(),
            limit,
        });
    }
    Ok(())
}

fn check_fact_text(value: &str, path: &str) -> Result<(), DraftError> {
    if value.is_empty() {
        return Err(invalid(path));
    }
    check_len(value, path, MAX_FACT_TEXT)
}

fn check_token(token: &TokenRef, index: usize) -> Result<(), DraftError> {
    let path = |field: &str| format!("trace.event.tokens[{index}].{field}");
    if !is_lock_issuer(&token.issuer) {
        return Err(invalid(&path("issuer")));
    }
    if token.token_type.is_empty() {
        return Err(invalid(&path("token_type")));
    }
    check_len(&token.token_type, &path("token_type"), MAX_TOKEN_TYPE)?;
    if let TokenId::Jti(jti) = &token.id {
        check_len(jti, &path("jti"), MAX_JTI)?;
    }
    Ok(())
}

fn invalid(path: &str) -> DraftError {
    DraftError::InvalidField {
        path: path.to_string(),
    }
}

/// Lock `TraceIssuers`: an absolute `https` URI, or `http` with host `localhost`.
pub(super) fn is_lock_issuer(issuer: &str) -> bool {
    let Ok(url) = Url::parse(issuer) else {
        return false;
    };
    match (url.scheme(), url.host_str()) {
        ("https", Some(_)) => true,
        ("http", Some(host)) => host.eq_ignore_ascii_case("localhost"),
        _ => false,
    }
}

#[cfg(test)]
mod tests {
    use jans_trace_core::inputs::token_fingerprint;

    use super::*;
    use crate::common::policy_store::PolicyStoreWithID;
    use crate::common::policy_store::test_utils::fixtures;
    use crate::log::gen_uuid7;
    use crate::trace::test_utils::{CjarStore, cjar_store};

    const ISSUER: &str = "https://idp.example.com";

    fn context() -> TraceContext {
        TraceContext::new(
            "exec-1".to_string(),
            "https://gateway.example.com".to_string(),
            "workload-1".to_string(),
        )
    }

    fn token(issuer: &str) -> TokenRef {
        TokenRef::new(
            issuer,
            "Jans::Access_Token",
            Some("jti-1"),
            "header.payload.sig",
        )
        .expect("test token ref should build")
    }

    fn decision(action: &str, tokens: Vec<TokenRef>) -> DecisionFacts {
        DecisionFacts {
            action: action.to_string(),
            resource_type: "Jans::Issue".to_string(),
            outcome: Outcome::Allow,
            tokens,
        }
    }

    fn policy() -> PolicyFacts {
        PolicyFacts {
            store_id: "store-1".to_string(),
            store_version: "1.0.0".to_string(),
            language_version: "4.4".to_string(),
            bundle_hash: jans_trace_core::inputs::compute_bundle_hash(b"archive"),
        }
    }

    fn draft_with(
        trace: &TraceContext,
        decision: DecisionFacts,
    ) -> Result<RecordDraft, DraftError> {
        RecordDraft::new(
            gen_uuid7(),
            1_700_000_000,
            trace,
            decision,
            Arc::new(policy()),
        )
    }

    fn draft(decision: DecisionFacts) -> Result<RecordDraft, DraftError> {
        draft_with(&context(), decision)
    }

    fn over_limit(err: &DraftError, expected_path: &str, expected_limit: usize) -> bool {
        matches!(
            err,
            DraftError::OverLimit { path, limit }
                if path == expected_path && *limit == expected_limit
        )
    }

    fn invalid_field(err: &DraftError, expected_path: &str) -> bool {
        matches!(err, DraftError::InvalidField { path } if path == expected_path)
    }

    #[test]
    fn valid_draft_builds() {
        draft(decision("Jans::Action::\"Read\"", vec![token(ISSUER)]))
            .expect("a draft within Lock limits should build");
    }

    #[test]
    fn empty_mandatory_context_is_missing_context() {
        for field in ["execution_id", "execution_authority", "workload_id"] {
            let mut trace = context();
            match field {
                "execution_id" => trace.execution_id.clear(),
                "execution_authority" => trace.execution_authority.clear(),
                _ => trace.workload_id.clear(),
            }
            let err = draft_with(&trace, decision("a", vec![]))
                .expect_err("an empty mandatory context field should be rejected");
            assert!(
                matches!(err, DraftError::MissingContext(f) if f == field),
                "expected MissingContext({field}), got {err:?}"
            );
        }
    }

    #[test]
    fn action_512_ascii_is_ok() {
        draft(decision(&"a".repeat(512), vec![])).expect("512 ASCII characters fit the limit");
    }

    #[test]
    fn action_513_utf16_units_is_over_limit() {
        let err = draft(decision(&"a".repeat(513), vec![]))
            .expect_err("513 characters should be over the limit");
        assert!(
            over_limit(&err, "trace.event.decision.action", 512),
            "expected OverLimit for action, got {err:?}"
        );
    }

    #[test]
    fn action_with_astral_chars_counts_utf16() {
        // Each emoji is one `char` but two UTF-16 units, as Java counts it.
        draft(decision(&"\u{1F600}".repeat(256), vec![]))
            .expect("256 emoji are 512 UTF-16 units and fit");
        let err = draft(decision(&"\u{1F600}".repeat(257), vec![]))
            .expect_err("257 emoji are 514 UTF-16 units");
        assert!(
            over_limit(&err, "trace.event.decision.action", 512),
            "expected OverLimit counted in UTF-16, got {err:?}"
        );
    }

    #[test]
    fn empty_action_is_invalid_field() {
        let err = draft(decision("", vec![])).expect_err("an empty action should be rejected");
        assert!(
            invalid_field(&err, "trace.event.decision.action"),
            "expected InvalidField for action, got {err:?}"
        );
    }

    #[test]
    fn workload_id_256_is_over_limit() {
        let mut trace = context();
        trace.workload_id = "w".repeat(256);
        let err = draft_with(&trace, decision("a", vec![]))
            .expect_err("a 256-character workload_id should be over the limit");
        assert!(
            over_limit(&err, "trace.subject.workload_id", 255),
            "expected OverLimit for workload_id, got {err:?}"
        );
    }

    #[test]
    fn execution_id_256_is_over_limit() {
        let mut trace = context();
        trace.execution_id = "e".repeat(256);
        let err = draft_with(&trace, decision("a", vec![]))
            .expect_err("a 256-character execution_id should be over the limit");
        assert!(
            over_limit(&err, "trace.trace_execution_id", 255),
            "expected OverLimit for trace_execution_id, got {err:?}"
        );
    }

    #[test]
    fn token_type_65_is_over_limit() {
        let long = TokenRef::new(ISSUER, &"T".repeat(65), Some("jti-1"), "raw")
            .expect("test token ref should build");
        let err = draft(decision("a", vec![long]))
            .expect_err("a 65-character token_type should be over the limit");
        assert!(
            over_limit(&err, "trace.event.tokens[0].token_type", 64),
            "expected OverLimit for token_type, got {err:?}"
        );
    }

    #[test]
    fn jti_257_is_over_limit() {
        let long = TokenRef::new(ISSUER, "Jans::Access_Token", Some(&"j".repeat(257)), "raw")
            .expect("test token ref should build");
        let err = draft(decision("a", vec![long]))
            .expect_err("a 257-character jti should be over the limit");
        assert!(
            over_limit(&err, "trace.event.tokens[0].jti", 256),
            "expected OverLimit for jti, got {err:?}"
        );
    }

    #[test]
    fn tokens_33_is_over_limit() {
        draft(decision("a", vec![token(ISSUER); 32])).expect("32 tokens fit the limit");
        let err = draft(decision("a", vec![token(ISSUER); 33]))
            .expect_err("33 tokens should be over the limit");
        assert!(
            over_limit(&err, "trace.event.tokens", 32),
            "expected OverLimit for tokens, got {err:?}"
        );
    }

    #[test]
    fn http_issuer_not_localhost_is_invalid() {
        for issuer in [
            "http://idp.example.com",
            "",
            "idp.example.com",
            "ftp://idp.example.com",
        ] {
            let err = draft(decision("a", vec![token(ISSUER), token(issuer)]))
                .expect_err("an issuer Lock rejects should be rejected");
            assert!(
                invalid_field(&err, "trace.event.tokens[1].issuer"),
                "expected InvalidField for issuer `{issuer}`, got {err:?}"
            );
        }
    }

    #[test]
    fn http_localhost_issuer_is_ok() {
        for issuer in [
            "http://localhost:8080/realm",
            "http://LOCALHOST",
            "https://idp.example.com",
        ] {
            assert_eq!(
                draft(decision("a", vec![token(issuer)])).err(),
                None,
                "issuer `{issuer}` should be accepted"
            );
        }
    }

    #[test]
    fn token_field_holding_raw_token_is_error() {
        let raw = "eyJhbGciOiJFZERTQSJ9.eyJzdWIiOiIxIn0.c2lnbmF0dXJl";
        for (issuer, mapping, jti, field) in [
            (ISSUER, "Jans::Access_Token", Some(raw), "jti"),
            (
                ISSUER,
                &*format!("Jans::{raw}"),
                Some("jti-1"),
                "token_type",
            ),
            (
                &*format!("https://idp.example.com/{raw}"),
                "Jans::Access_Token",
                None,
                "issuer",
            ),
        ] {
            let err = TokenRef::new(issuer, mapping, jti, raw)
                .expect_err("a field that holds the raw token should be rejected");
            assert_eq!(
                err,
                RawTokenInField(field),
                "the error should name `{field}`"
            );
        }
    }

    #[test]
    fn jws_shaped_jti_is_allowed() {
        // Only the token itself is refused, not values that merely look like one.
        let jti = "aaaaaaaaaa.bbbbbbbbbb.cccccccccc";
        let token = TokenRef::new(
            ISSUER,
            "Jans::Access_Token",
            Some(jti),
            "header.payload.sig",
        )
        .expect("a JWS-shaped jti that is not the token should be accepted");
        draft(decision("a", vec![token])).expect("the record should build");
    }

    #[test]
    fn empty_raw_token_is_never_found() {
        TokenRef::new(ISSUER, "Jans::Access_Token", Some("jti-1"), "")
            .expect("an empty raw token cannot leak into a field");
    }

    #[test]
    fn token_without_jti_uses_core_fingerprint() {
        let raw = "header.payload.signature";
        for jti in [None, Some("")] {
            let token = TokenRef::new(ISSUER, "Jans::Access_Token", jti, raw)
                .expect("test token ref should build");
            assert_eq!(
                token.id,
                TokenId::Fingerprint(token_fingerprint(raw)),
                "a token without jti ({jti:?}) should use the core fingerprint"
            );
        }
        let token = TokenRef::new(ISSUER, "Jans::Access_Token", Some("jti-1"), raw)
            .expect("test token ref should build");
        assert_eq!(
            token.id,
            TokenId::Jti("jti-1".to_string()),
            "a token with jti should use it"
        );
        assert_eq!(
            token.token_type, "Jans::Access_Token",
            "token_type is the mapping as is"
        );
    }

    #[cfg(not(target_arch = "wasm32"))]
    #[test]
    fn outcome_serializes_as_lock_vocabulary() {
        assert_eq!(Outcome::Allow.as_str(), "ALLOW", "allow outcome");
        assert_eq!(Outcome::Deny.as_str(), "DENY", "deny outcome");
    }

    #[test]
    fn outcome_from_allowed_decision() {
        assert_eq!(Outcome::from_allowed(true), Outcome::Allow, "allowed");
        assert_eq!(Outcome::from_allowed(false), Outcome::Deny, "denied");
    }

    #[test]
    fn policy_facts_from_cjar_store() {
        let CjarStore { store, bundle_hash } =
            cjar_store(&fixtures::minimal_valid().with_version("2.1.0"));
        let facts = store
            .trace_policy()
            .expect("a .cjar store has policy facts");
        assert_eq!(
            facts.bundle_hash, bundle_hash,
            "bundle_hash comes from the store"
        );
        assert_eq!(facts.store_version, "2.1.0", "store version from metadata");
        assert_eq!(
            facts.store_id,
            store.metadata.as_ref().expect("metadata").id(),
            "store id from metadata"
        );
        assert_eq!(
            facts.language_version,
            cedar_policy::get_lang_version().to_string(),
            "language version is the Cedar version Cedarling evaluates with"
        );
    }

    #[test]
    fn policy_facts_none_without_bundle_hash() {
        let CjarStore {
            store: archived, ..
        } = cjar_store(&fixtures::minimal_valid());
        let store = PolicyStoreWithID::without_archive(
            archived.id.clone(),
            archived.store.clone(),
            archived.metadata.clone(),
        );
        assert!(
            store.trace_policy().is_none(),
            "a store without bundle_hash cannot back a TRACE record"
        );
    }

    #[test]
    fn policy_facts_none_without_store_version() {
        let CjarStore { store, .. } = cjar_store(&fixtures::minimal_valid().with_version(""));
        assert!(
            store.trace_policy().is_none(),
            "a .cjar whose metadata has no policy store version cannot back a TRACE record"
        );
    }
}
