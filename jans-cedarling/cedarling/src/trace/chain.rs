// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! The producer chain: a new one on every Cedarling start. Each signed
//! record takes the next position and carries the `content_digest` of the one
//! before, so Lock sees a gap or a fork.

use jans_trace_core::assertion::{
    ParseUnsignedAssertionError, SignError, SignedAssertion, UnsignedAssertion,
};
use jans_trace_core::signature::TraceSigner;
use jans_trace_core::{Digest, Limits};
use serde_json::{Map, Value, json};
use uuid7::Uuid;

use super::ProducerId;
use super::record::{RecordDraft, TokenId, TokenRef};
use crate::common::app_types::PdpID;
use crate::log::gen_uuid7;

/// Record profile; Lock rejects any other.
const EAT_PROFILE: &str = "tag:jans.io,2026:trace-v1";
const EVENT_KIND: &str = "AUTHORIZATION_DECISION";
const POLICY_LANGUAGE: &str = "cedar";

/// Position of the next record in this instance's chain.
pub(crate) struct ProducerChain {
    producer_id: ProducerId,
    /// Both `producer_chain.producer_instance_id` and `trace.runtime.pdp_id`:
    /// one value, as the PDP is the producer instance, so it has one source.
    pdp_id: PdpID,
    chain_id: Uuid,
    next_sequence: u64,
    prev_record_hash: Digest,
}

/// Why `append` signed no record. The chain position stays free in both cases.
#[derive(Debug, Clone, PartialEq, thiserror::Error)]
pub(crate) enum AppendError {
    /// The record is not an assertion Lock accepts, e.g. over the size limit.
    #[error("TRACE record is not a valid assertion: {0}")]
    Parse(#[from] ParseUnsignedAssertionError),
    #[error("TRACE record signing failed: {0}")]
    Sign(#[from] SignError),
}

impl ProducerChain {
    /// A new chain on every start; Lock registers it from its
    /// genesis record (`first_observed`).
    pub(crate) fn open(producer_id: ProducerId, pdp_id: PdpID) -> Self {
        Self {
            producer_id,
            pdp_id,
            chain_id: gen_uuid7(),
            next_sequence: 1,
            prev_record_hash: Digest::GENESIS,
        }
    }

    /// Id of this chain, `producer_chain_id` in every record of it.
    pub(crate) fn chain_id(&self) -> Uuid {
        self.chain_id
    }

    /// `sequence_number` the next signed record gets; the first record is `1`.
    pub(crate) fn next_sequence(&self) -> u64 {
        self.next_sequence
    }

    /// The position advances only after the record is signed, so a record that
    /// fails here leaves no gap in the chain.
    pub(crate) fn append(
        &mut self,
        draft: &RecordDraft,
        signer: &dyn TraceSigner,
        limits: &Limits,
    ) -> Result<SignedAssertion, AppendError> {
        let record = self.record(draft, signer.kid()).to_string();
        let assertion = UnsignedAssertion::parse(&record, limits)?.sign(signer)?;
        self.next_sequence += 1;
        self.prev_record_hash = assertion.content_digest();
        Ok(assertion)
    }

    /// The Lock `AUTHORIZATION_DECISION` shape. `producer_version` is left out on
    /// purpose: it is only a build label, while `eat_profile`
    /// versions the format. The member is optional, so it can be added later
    /// without a new profile. `parent_record_ids` and `chain_link` are optional
    /// too and Cedarling has nothing to put there.
    fn record(&self, draft: &RecordDraft, kid: &str) -> Value {
        let trace = &draft.trace;
        let decision = &draft.decision;
        let policy = &draft.policy;
        let producer = self.producer_id.as_str();
        let pdp_id = self.pdp_id.to_string();

        let mut subject = Map::new();
        subject.insert("workload_id".to_string(), json!(trace.workload_id));
        if let Some(instance) = &trace.workload_instance_id {
            subject.insert("workload_instance_id".to_string(), json!(instance));
        }

        let mut event = Map::new();
        event.insert(
            "decision".to_string(),
            json!({
                "action": decision.action,
                "resource_type": decision.resource_type,
                "outcome": decision.outcome.as_str(),
            }),
        );
        // `authorize_unsigned` uses no tokens; Lock wants no empty `tokens`.
        if !decision.tokens.is_empty() {
            let tokens = decision.tokens.iter().map(token).collect();
            event.insert("tokens".to_string(), Value::Array(tokens));
        }

        json!({
            "producer": producer,
            "kid": kid,
            "record_id": draft.record_id.to_string(),
            "trace": {
                "eat_profile": EAT_PROFILE,
                "event_kind": EVENT_KIND,
                "signed_at": draft.signed_at,
                "trace_execution_id": trace.execution_id,
                "execution_authority": trace.execution_authority,
                "subject": subject,
                "event": event,
                "policy": {
                    "policy_store_id": policy.store_id,
                    "policy_store_version": policy.store_version,
                    "policy_language": POLICY_LANGUAGE,
                    "policy_language_version": policy.language_version,
                    "bundle_hash": policy.bundle_hash.to_string(),
                },
                "runtime": {"pdp_id": pdp_id},
            },
            "producer_chain": {
                "producer_id": producer,
                "producer_instance_id": pdp_id,
                "producer_chain_id": self.chain_id.to_string(),
                "sequence_number": self.next_sequence,
                "prev_record_hash": self.prev_record_hash.to_string(),
            },
        })
    }
}

fn token(token: &TokenRef) -> Value {
    let (name, id) = match &token.id {
        TokenId::Jti(jti) => ("jti", jti.clone()),
        TokenId::Fingerprint(digest) => ("fingerprint", digest.to_string()),
    };
    json!({
        "issuer": token.issuer,
        "token_type": token.token_type,
        name: id,
    })
}

#[cfg(test)]
mod tests {
    use std::collections::BTreeSet;

    use jans_trace_core::assertion::{
        ParseUnsignedAssertionError, SignError, SignedAssertion, VerificationContext, VerifyError,
    };
    use jans_trace_core::inputs::token_fingerprint;
    use jans_trace_core::signature::{Ed25519Signer, SignerError, TraceSigner};
    use jans_trace_core::{Digest, Limits};
    use serde_json::Value;

    use super::*;
    use crate::authz::TraceContext;
    use crate::common::app_types::PdpID;
    use crate::log::gen_uuid7;
    use crate::trace::record::{DecisionFacts, Outcome, PolicyFacts, RecordDraft, TokenRef};

    /// RFC 8410 section 10.3 example key: public test vector, not a secret.
    const KEY_PEM: &str = include_str!("../../../test_files/trace/producer_ed25519.pem");
    const PRODUCER: &str = "cedarling-fleet-1";
    const KID: &str = "fleet-key-1";
    const ACTION: &str = r#"Jans::Action::"Read""#;

    /// Same signer type `ProducerKey` wraps; the chain only sees `TraceSigner`.
    fn key() -> Ed25519Signer {
        Ed25519Signer::from_pkcs8_pem(KID, KEY_PEM).expect("test key should parse")
    }

    fn verification() -> VerificationContext {
        let public_key = key().public_key();
        VerificationContext {
            expected_producer: PRODUCER.to_string(),
            expected_kid: KID.to_string(),
            public_key,
        }
    }

    fn chain() -> ProducerChain {
        let producer_id = PRODUCER.parse().expect("test producer id should parse");
        ProducerChain::open(producer_id, PdpID::new())
    }

    fn draft(tokens: Vec<TokenRef>) -> RecordDraft {
        let mut trace = TraceContext::new(
            "exec-1".to_string(),
            "https://gateway.example.com".to_string(),
            "workload-1".to_string(),
        );
        trace.workload_instance_id = Some("workload-1-a".to_string());
        let decision = DecisionFacts {
            action: ACTION.to_string(),
            resource_type: "Jans::Issue".to_string(),
            outcome: Outcome::Allow,
            tokens,
        };
        let policy = PolicyFacts {
            store_id: "store-1".to_string(),
            store_version: "1.0.0".to_string(),
            language_version: "4.4".to_string(),
            bundle_hash: jans_trace_core::inputs::compute_bundle_hash(b"archive"),
        };
        RecordDraft::new(gen_uuid7(), 1_781_138_542, &trace, decision, policy)
            .expect("test draft should build")
    }

    fn tokens() -> Vec<TokenRef> {
        vec![
            TokenRef::new(
                "https://idp.example.com",
                "Jans::Access_Token",
                Some("jti-1"),
                "a.b.c",
            )
            .expect("test token ref should build"),
            TokenRef::new("https://idp.example.com", "Jans::Id_Token", None, "d.e.f")
                .expect("test token ref should build"),
        ]
    }

    fn json(signed: &SignedAssertion) -> Value {
        serde_json::from_str(signed.as_str()).expect("signed record should be JSON")
    }

    /// Where a record sits in its chain.
    #[derive(Debug, PartialEq)]
    struct ChainPosition {
        sequence: u64,
        prev_record_hash: String,
    }

    fn position(signed: &SignedAssertion) -> ChainPosition {
        let record = json(signed);
        let chain = &record["producer_chain"];
        ChainPosition {
            sequence: chain["sequence_number"]
                .as_u64()
                .expect("sequence_number should be an integer"),
            prev_record_hash: chain["prev_record_hash"]
                .as_str()
                .expect("prev_record_hash should be a string")
                .to_string(),
        }
    }

    fn genesis_position() -> ChainPosition {
        ChainPosition {
            sequence: 1,
            prev_record_hash: Digest::GENESIS.to_string(),
        }
    }

    fn member_names(value: &Value, names: &mut BTreeSet<String>) {
        match value {
            Value::Object(members) => {
                for (name, member) in members {
                    names.insert(name.clone());
                    member_names(member, names);
                }
            },
            Value::Array(items) => items.iter().for_each(|item| member_names(item, names)),
            _ => {},
        }
    }

    struct FailingSigner;

    impl TraceSigner for FailingSigner {
        fn kid(&self) -> &str {
            KID
        }

        fn sign(&self, _message: &[u8]) -> Result<[u8; 64], SignerError> {
            Err(SignerError)
        }
    }

    #[test]
    fn first_record_is_genesis() {
        let signed = chain()
            .append(&draft(tokens()), &key(), &Limits::default())
            .expect("first record should be signed");
        assert_eq!(
            position(&signed),
            genesis_position(),
            "a new chain should start at sequence 1 with the genesis hash"
        );
    }

    #[test]
    fn three_appends_link_by_content_digest() {
        let mut chain = chain();
        let key = key();
        let mut prev = Digest::GENESIS;
        for sequence in 1..=3 {
            let signed = chain
                .append(&draft(tokens()), &key, &Limits::default())
                .expect("record should be signed");
            assert_eq!(
                position(&signed),
                ChainPosition {
                    sequence,
                    prev_record_hash: prev.to_string(),
                },
                "record {sequence} should link to the content digest of the one before"
            );
            prev = signed.content_digest();
        }
    }

    #[test]
    fn record_carries_lock_members() {
        let chain = chain();
        let pdp_id = chain.pdp_id.to_string();
        let chain_id = chain.chain_id.to_string();
        let mut chain = chain;
        let draft = draft(tokens());
        let record = json(
            &chain
                .append(&draft, &key(), &Limits::default())
                .expect("record should be signed"),
        );
        let fingerprint = token_fingerprint("d.e.f").to_string();
        let expected = serde_json::json!({
            "producer": PRODUCER,
            "kid": KID,
            "record_id": draft.record_id.to_string(),
            "trace": {
                "eat_profile": "tag:jans.io,2026:trace-v1",
                "event_kind": "AUTHORIZATION_DECISION",
                "signed_at": 1_781_138_542,
                "trace_execution_id": "exec-1",
                "execution_authority": "https://gateway.example.com",
                "subject": {"workload_id": "workload-1", "workload_instance_id": "workload-1-a"},
                "event": {
                    "decision": {
                        "action": ACTION,
                        "resource_type": "Jans::Issue",
                        "outcome": "ALLOW",
                    },
                    "tokens": [
                        {
                            "issuer": "https://idp.example.com",
                            "token_type": "Jans::Access_Token",
                            "jti": "jti-1",
                        },
                        {
                            "issuer": "https://idp.example.com",
                            "token_type": "Jans::Id_Token",
                            "fingerprint": fingerprint,
                        },
                    ],
                },
                "policy": {
                    "policy_store_id": "store-1",
                    "policy_store_version": "1.0.0",
                    "policy_language": "cedar",
                    "policy_language_version": "4.4",
                    "bundle_hash": draft.policy.bundle_hash.to_string(),
                },
                "runtime": {"pdp_id": pdp_id},
            },
            "producer_chain": {
                "producer_id": PRODUCER,
                "producer_instance_id": pdp_id,
                "producer_chain_id": chain_id,
                "sequence_number": 1,
                "prev_record_hash": Digest::GENESIS.to_string(),
            },
        });
        let mut record = record;
        let signature = record
            .as_object_mut()
            .and_then(|members| members.remove("signature"));
        assert!(
            signature.as_ref().is_some_and(Value::is_string),
            "record should carry a string signature"
        );
        assert_eq!(
            record, expected,
            "record should have the Lock AUTHORIZATION_DECISION shape"
        );
    }

    #[test]
    fn signed_record_verifies_with_core() {
        let signed = chain()
            .append(&draft(tokens()), &key(), &Limits::default())
            .expect("record should be signed");
        SignedAssertion::parse(signed.as_str(), &Limits::default())
            .expect("signed record should parse as Lock parses it")
            .verify(&verification())
            .expect("signed record should verify under the producer key");
    }

    #[test]
    fn one_bit_change_breaks_signature() {
        let signed = chain()
            .append(&draft(tokens()), &key(), &Limits::default())
            .expect("record should be signed");
        let original = r#""action":"Jans::Action::\"Read\"""#;
        assert_eq!(
            signed.as_str().matches(original).count(),
            1,
            "the signed text should contain the action once"
        );
        let tampered = signed
            .as_str()
            .replace(original, r#""action":"Jans::Action::\"Reae\"""#);
        let err = SignedAssertion::parse(&tampered, &Limits::default())
            .expect("tampered record should still parse")
            .verify(&verification())
            .expect_err("a changed action should break the signature");
        assert!(
            matches!(err, VerifyError::SignatureInvalid),
            "expected SignatureInvalid, got {err:?}"
        );
    }

    #[test]
    fn record_has_no_forbidden_members() {
        let signed = chain()
            .append(&draft(tokens()), &key(), &Limits::default())
            .expect("record should be signed");
        let mut names = BTreeSet::new();
        member_names(&json(&signed), &mut names);
        for forbidden in [
            "producer_version",
            "capability_id",
            "validation_at_decision",
            "token_claims",
            "trust_tier",
            "content_digest",
            "decisions",
            "parent_record_ids",
            "chain_link",
        ] {
            assert!(
                !names.contains(forbidden),
                "record should not carry `{forbidden}`"
            );
        }
    }

    #[test]
    fn unsigned_decision_has_no_tokens_member() {
        let signed = chain()
            .append(&draft(Vec::new()), &key(), &Limits::default())
            .expect("record without tokens should be signed");
        let record = json(&signed);
        assert!(
            record["trace"]["event"].get("tokens").is_none(),
            "a decision without tokens should have no `tokens` member"
        );
    }

    #[test]
    fn failed_sign_keeps_position() {
        let mut chain = chain();
        let err = chain
            .append(&draft(tokens()), &FailingSigner, &Limits::default())
            .expect_err("a failing signer should give no record");
        assert!(
            matches!(err, AppendError::Sign(SignError::Signer(_))),
            "expected a signer error, got {err:?}"
        );
        let signed = chain
            .append(&draft(tokens()), &key(), &Limits::default())
            .expect("next record should be signed");
        assert_eq!(
            position(&signed),
            genesis_position(),
            "a failed signature should not take a chain position"
        );
    }

    #[test]
    fn oversized_record_keeps_position() {
        let mut chain = chain();
        let err = chain
            .append(
                &draft(tokens()),
                &key(),
                &Limits::default().with_max_input_bytes(256),
            )
            .expect_err("a record over the size limit should give no record");
        assert!(
            matches!(
                err,
                AppendError::Parse(ParseUnsignedAssertionError::SizeLimitExceeded(_))
            ),
            "expected a size limit error, got {err:?}"
        );
        let signed = chain
            .append(&draft(tokens()), &key(), &Limits::default())
            .expect("next record should be signed");
        assert_eq!(
            position(&signed),
            genesis_position(),
            "an oversized record should not take a chain position"
        );
    }

    /// Writes a signed record and the public JWK of the test key for the Lock
    /// stand (`stand/aio_admin.py register-producer-key`, then `post-trace`).
    /// Uses `ProducerKey` with the default `kid`, as a production start does.
    #[test]
    #[ignore = "writes files for a manual run against a Lock stand"]
    #[cfg(not(target_arch = "wasm32"))]
    fn dump_signed_record_for_stand() {
        use base64::Engine;
        use base64::engine::general_purpose::URL_SAFE_NO_PAD;

        use crate::trace::ProducerKey;

        let producer_key = ProducerKey::from_pem(KEY_PEM, None).expect("test key should parse");
        let mut draft = draft(tokens());
        draft.signed_at =
            u64::try_from(chrono::Utc::now().timestamp()).expect("clock should be after the epoch");
        let signed = chain()
            .append(&draft, &producer_key, &Limits::default())
            .expect("record should be signed");
        let jwk = serde_json::json!({
            "kty": "OKP",
            "crv": "Ed25519",
            "x": URL_SAFE_NO_PAD.encode(key().public_key_bytes()),
        });

        let dir = std::path::Path::new(env!("CARGO_MANIFEST_DIR")).join("../target/trace-stand");
        std::fs::create_dir_all(&dir).expect("dump directory should be created");
        for (name, text) in [
            ("record.json", signed.as_str().to_string()),
            ("producer.jwk", jwk.to_string()),
            ("kid", producer_key.kid().to_string()),
        ] {
            std::fs::write(dir.join(name), text).expect("dump file should be written");
        }
        println!("dumped to {}", dir.display());
    }

    #[test]
    fn chains_differ_per_open() {
        assert_ne!(
            chain().chain_id,
            chain().chain_id,
            "every start should open a new chain"
        );
    }
}
