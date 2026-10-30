// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! TRACE emission from `Cedarling` against a mock Lock: one signed record per
//! decision when the host passes a TRACE context, and nothing otherwise.

use std::num::NonZeroUsize;
use std::path::Path;
use std::sync::{Arc, Mutex};

use jans_trace_core::assertion::{SignedAssertion, VerificationContext, VerifyError, parse_batch};
use jans_trace_core::inputs::compute_bundle_hash;
use jans_trace_core::key::PublicKey;
use jans_trace_core::{Digest, Limits};
use mockito::{Server, ServerGuard};
use serde_json::{Value, json};

use crate::authz::request::{AuthorizeMultiIssuerRequest, EntityData, RequestUnsigned};
use crate::bootstrap_config::{BootstrapConfig, LockServiceConfig};
use crate::common::policy_store::test_utils::fixtures;
use crate::log::interface::LogStorage;
use crate::tests::utils::cedarling_util::get_config;
use crate::trace::test_utils::{dolphin_token, multi_issuer_store};
use crate::{
    AuthorizeOptions, Cedarling, PolicyStoreSource, TraceConfig, TraceContext, TraceEndpoint,
    TraceSigningKey,
};

const PRODUCER: &str = "cedarling-fleet-1";
const BULK_PATH: &str = "/audit/trace/bulk";
const EXECUTION_ID: &str = "exec-1";
const EXECUTION_AUTHORITY: &str = "https://gateway.example.com";
const WORKLOAD_ID: &str = "workload-1";

/// Public key of the RFC 8410 section 10.3 example key, which signs every record here.
const PUBLIC_KEY: &[u8; 32] = include_bytes!("../../../test_files/trace/producer_ed25519.pub");

/// A mock Lock that answers every bulk request with all records accepted and
/// keeps the bodies it received.
struct MockLock {
    server: ServerGuard,
    bodies: Arc<Mutex<Vec<String>>>,
}

impl MockLock {
    /// `trace_bulk` says whether the well-known configuration publishes
    /// `trace_bulk_endpoint`, as Lock does when its TRACE is enabled.
    async fn start(trace_bulk: bool) -> Self {
        let mut server = Server::new_async().await;
        let base = server.url();
        let mut audit = json!({
            "log_endpoint": format!("{base}/audit/log"),
            "health_endpoint": format!("{base}/audit/health"),
            "telemetry_endpoint": format!("{base}/audit/telemetry"),
        });
        if trace_bulk {
            audit["trace_bulk_endpoint"] = json!(format!("{base}{BULK_PATH}"));
        }
        server
            .mock("GET", "/.well-known/lock-server-configuration")
            .with_body(
                json!({
                    "version": "1.0",
                    "issuer": base,
                    "audit": audit,
                    "config": {
                        "config_endpoint": format!("{base}/config"),
                        "issuers_endpoint": format!("{base}/config/issuers"),
                        "policy_endpoint": format!("{base}/config/policy"),
                        "schema_endpoint": format!("{base}/config/schema"),
                        "sse_endpoint": format!("{base}/sse"),
                    },
                })
                .to_string(),
            )
            .create_async()
            .await;

        let bodies = Arc::new(Mutex::new(Vec::new()));
        let received = bodies.clone();
        server
            .mock("POST", BULK_PATH)
            .with_status(207)
            .with_body_from_request(move |request| {
                let body =
                    String::from_utf8_lossy(request.body().expect("a bulk body")).into_owned();
                let count = parse_batch(&body, &Limits::default())
                    .expect("the bulk body is an array of records")
                    .len();
                received.lock().expect("bodies lock").push(body);
                let results: Vec<Value> = (0..count)
                    .map(|index| json!({"index": index, "accepted": true}))
                    .collect();
                json!({ "results": results }).to_string().into_bytes()
            })
            .create_async()
            .await;
        Self { server, bodies }
    }

    fn lock_config(&self) -> LockServiceConfig {
        LockServiceConfig {
            config_uri: format!(
                "{}/.well-known/lock-server-configuration",
                self.server.url()
            )
            .parse()
            .expect("mock Lock URL should parse"),
            access_token_jwt: Some("pre.issued.token".to_string()), // # gitleaks:allow
            ..Default::default()
        }
    }

    /// Every record Lock received, in sending order.
    fn records(&self) -> Vec<SignedAssertion> {
        self.bodies
            .lock()
            .expect("bodies lock")
            .iter()
            .flat_map(|body| {
                parse_batch(body, &Limits::default()).expect("the bulk body is an array of records")
            })
            .map(|record| record.expect("each record is well formed"))
            .collect()
    }
}

/// A `.cjar` store and the `bundle_hash` of its bytes.
struct Archive {
    source: PolicyStoreSource,
    bundle_hash: Digest,
}

fn archive(bytes: Vec<u8>) -> Archive {
    Archive {
        bundle_hash: compute_bundle_hash(&bytes),
        source: PolicyStoreSource::ArchiveBytes(bytes),
    }
}

fn minimal_archive() -> Archive {
    archive(
        fixtures::minimal_valid()
            .build_archive()
            .expect("test archive should build"),
    )
}

fn trace_config() -> TraceConfig {
    TraceConfig {
        producer_id: PRODUCER.parse().expect("valid producer id"),
        signing_key: TraceSigningKey::File(
            Path::new(env!("CARGO_MANIFEST_DIR")).join("../test_files/trace/producer_ed25519.pem"),
        ),
        signing_kid: None,
        endpoint: TraceEndpoint::Discovered,
        transport: crate::LockTransport::Rest,
        bulk_max_records: NonZeroUsize::new(100).expect("non-zero"),
        bulk_max_bytes: NonZeroUsize::new(4_194_304).expect("non-zero"),
    }
}

/// Lock with TRACE on or off; metrics are collected so their keys can be checked.
fn config(lock: &MockLock, store: PolicyStoreSource, trace: bool) -> BootstrapConfig {
    let mut config = get_config(store);
    config.lock_config = Some(lock.lock_config());
    config.trace_config = trace.then(trace_config);
    config.authorization_config.metrics_collection = true;
    config
}

fn verification() -> VerificationContext {
    let public_key = PublicKey::from_bytes(PUBLIC_KEY).expect("RFC 8410 public key is valid");
    VerificationContext {
        expected_producer: PRODUCER.to_string(),
        expected_kid: public_key.thumbprint().to_string(),
        public_key,
    }
}

fn with_trace() -> AuthorizeOptions {
    AuthorizeOptions::with_trace(TraceContext::new(
        EXECUTION_ID.to_string(),
        EXECUTION_AUTHORITY.to_string(),
        WORKLOAD_ID.to_string(),
    ))
}

fn entity(entity_type: &str, id: &str) -> EntityData {
    EntityData::from_json(
        &json!({"cedar_entity_mapping": {"entity_type": entity_type, "id": id}}).to_string(),
    )
    .expect("test entity data")
}

fn unsigned(options: AuthorizeOptions) -> RequestUnsigned {
    RequestUnsigned {
        principal: Some(entity("TestApp::User", "u1")),
        action: "TestApp::Action::\"read\"".to_string(),
        resource: entity("TestApp::Resource", "r1"),
        context: json!({}),
        options,
    }
}

fn json_of(record: &SignedAssertion) -> Value {
    serde_json::from_str(record.as_str()).expect("a record is JSON")
}

/// `trace_record_status` of every decision log entry, in logging order.
fn decision_statuses(cedarling: &Cedarling) -> Vec<Option<String>> {
    cedarling
        .pop_logs()
        .into_iter()
        .filter(|entry| entry["log_kind"] == "Decision")
        .map(|entry| entry["trace_record_status"].as_str().map(str::to_owned))
        .collect()
}

#[tokio::test]
async fn acceptance_1_disabled_is_unchanged() {
    let lock = MockLock::start(true).await;
    let cedarling = Cedarling::new(&config(&lock, minimal_archive().source, false))
        .await
        .expect("Cedarling should start without TRACE");

    cedarling
        .authorize_unsigned(unsigned(with_trace()))
        .await
        .expect("the decision should be made");
    cedarling.shut_down().await;

    assert!(lock.records().is_empty(), "TRACE disabled sends no records");
    assert_eq!(
        decision_statuses(&cedarling),
        [None],
        "TRACE disabled adds no trace_record_status to the decision log"
    );
    let metrics = cedarling.drain_metrics().expect("metrics are collected");
    let trace_keys: Vec<_> = metrics
        .operational_stats
        .keys()
        .filter(|key| key.starts_with("trace."))
        .collect();
    assert!(
        trace_keys.is_empty(),
        "TRACE disabled adds no trace.* metrics, got {trace_keys:?}"
    );
}

#[tokio::test]
async fn acceptance_2_one_record_per_decision() {
    let lock = MockLock::start(true).await;
    let Archive {
        source,
        bundle_hash,
    } = minimal_archive();
    let cedarling = Cedarling::new(&config(&lock, source, true))
        .await
        .expect("Cedarling should start with TRACE");

    let mut request_ids = Vec::new();
    for _ in 0..3 {
        let result = cedarling
            .authorize_unsigned(unsigned(with_trace()))
            .await
            .expect("the decision should be made");
        request_ids.push(result.request_id);
    }
    cedarling.shut_down().await;

    let records = lock.records();
    assert_eq!(records.len(), 3, "one record per decision");
    for ((record, sequence), request_id) in records.iter().zip(1..).zip(&request_ids) {
        record
            .verify(&verification())
            .expect("each record verifies with the producer key");
        let record = json_of(record);
        assert_eq!(
            record["producer_chain"]["sequence_number"], sequence,
            "records follow the decision order"
        );
        assert_eq!(
            record["record_id"].as_str(),
            Some(request_id.as_str()),
            "record_id is the request_id of the decision"
        );
        let trace = &record["trace"];
        assert_eq!(
            trace["trace_execution_id"], EXECUTION_ID,
            "from the context"
        );
        assert_eq!(
            trace["execution_authority"], EXECUTION_AUTHORITY,
            "from the context"
        );
        assert_eq!(
            trace["subject"]["workload_id"], WORKLOAD_ID,
            "from the context"
        );
        assert_eq!(
            trace["policy"]["bundle_hash"].as_str(),
            Some(bundle_hash.to_string().as_str()),
            "bundle_hash is the hash of the .cjar bytes"
        );
    }
}

#[tokio::test]
async fn acceptance_3_missing_context() {
    let lock = MockLock::start(true).await;
    let traced = Cedarling::new(&config(&lock, minimal_archive().source, true))
        .await
        .expect("Cedarling should start with TRACE");
    let untraced = Cedarling::new(&config(&lock, minimal_archive().source, false))
        .await
        .expect("Cedarling should start without TRACE");

    let result = traced
        .authorize_unsigned(unsigned(AuthorizeOptions::default()))
        .await
        .expect("the decision should be made");
    let expected = untraced
        .authorize_unsigned(unsigned(AuthorizeOptions::default()))
        .await
        .expect("the decision should be made");
    traced.shut_down().await;

    assert!(
        lock.records().is_empty(),
        "a decision without a TRACE context has no record"
    );
    assert_eq!(
        decision_statuses(&traced),
        [Some("not_emitted_missing_context".to_string())],
        "the decision log says why there is no record"
    );
    assert_eq!(
        result.decision, expected.decision,
        "TRACE does not change the decision"
    );
}

#[tokio::test]
async fn acceptance_5_no_deferred_fields() {
    let lock = MockLock::start(true).await;
    let Archive { source, .. } = archive(
        multi_issuer_store()
            .build_archive()
            .expect("test archive should build"),
    );
    let cedarling = Cedarling::new(&config(&lock, source, true))
        .await
        .expect("Cedarling should start with TRACE");
    let token = dolphin_token();

    cedarling
        .authorize_multi_issuer(
            AuthorizeMultiIssuerRequest::new_with_fields(
                vec![token.clone()],
                entity("Dolphin::Resource", "d1"),
                "Dolphin::Action::\"Read\"".to_string(),
                None,
            )
            .with_options(with_trace()),
        )
        .await
        .expect("the decision should be made");
    cedarling.shut_down().await;

    let records = lock.records();
    assert_eq!(records.len(), 1, "one record for the decision");
    let text = records[0].as_str();
    for deferred in [
        "validation_at_decision",
        "token_claims",
        "request_digest",
        "tool_call_digest",
        "input_digest",
        "trust_tier",
        "capability_id",
    ] {
        assert!(
            !text.contains(&format!("\"{deferred}\"")),
            "the record has no `{deferred}`"
        );
    }
    assert!(
        !text.contains(&token.payload),
        "the record never carries the raw token"
    );
    assert_eq!(
        json_of(&records[0])["trace"]["event"]["tokens"][0]["jti"],
        "dolphin-1",
        "the record references the token by its jti"
    );
}

#[tokio::test]
async fn acceptance_6_one_bit_breaks_signature() {
    let lock = MockLock::start(true).await;
    let cedarling = Cedarling::new(&config(&lock, minimal_archive().source, true))
        .await
        .expect("Cedarling should start with TRACE");

    cedarling
        .authorize_unsigned(unsigned(with_trace()))
        .await
        .expect("the decision should be made");
    cedarling.shut_down().await;

    let records = lock.records();
    let original = records[0].as_str();
    let tampered = original.replacen("\\\"read\\\"", "\\\"reae\\\"", 1);
    assert_ne!(tampered, original, "the action is changed in one byte");
    let tampered = SignedAssertion::parse(&tampered, &Limits::default())
        .expect("the tampered record is still well formed");

    let err = tampered
        .verify(&verification())
        .expect_err("a changed action should break the signature");
    assert_eq!(
        err,
        VerifyError::SignatureInvalid,
        "the signature no longer covers the record"
    );
}

#[tokio::test]
async fn no_trace_bulk_endpoint_starts_with_no_endpoint_status() {
    let lock = MockLock::start(false).await;
    let cedarling = Cedarling::new(&config(&lock, minimal_archive().source, true))
        .await
        .expect("Cedarling should start when Lock publishes no TRACE endpoint");

    cedarling
        .authorize_unsigned(unsigned(with_trace()))
        .await
        .expect("the decision should be made");
    cedarling.shut_down().await;

    assert!(
        lock.records().is_empty(),
        "there is nowhere to send records"
    );
    assert_eq!(
        decision_statuses(&cedarling),
        [Some("not_emitted_no_endpoint".to_string())],
        "the decision log says why there is no record"
    );
}
