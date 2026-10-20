// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Startup with TRACE: a configuration TRACE cannot work with or a bad key stops
//! `Cedarling::new`, a good key is logged so the admin can register it in Lock.

use std::num::NonZeroUsize;
use std::path::{Path, PathBuf};

use jans_trace_core::key::PublicKey;
use mockito::{Server, ServerGuard};
use serde_json::{Value, json};

use crate::bootstrap_config::{BootstrapConfig, LockServiceConfig};
use crate::common::policy_store::test_utils::fixtures;
use crate::log::interface::LogStorage;
use crate::tests::utils::cedarling_util::get_config;
use crate::{
    Cedarling, InitCedarlingError, LoadProducerKeyError, PolicyStoreSource, TraceConfig,
    TraceEndpoint, TraceSetupError, TraceSigningKey,
};

/// Public key of the RFC 8410 section 10.3 example key, extracted with `openssl` so the
/// thumbprint is checked against bytes this crate did not derive.
const RFC8410_PUBLIC_KEY: &[u8; 32] =
    include_bytes!("../../../test_files/trace/producer_ed25519.pub");

fn fixture(name: &str) -> PathBuf {
    Path::new(env!("CARGO_MANIFEST_DIR"))
        .join("../test_files/trace")
        .join(name)
}

fn key_file() -> TraceSigningKey {
    TraceSigningKey::File(fixture("producer_ed25519.pem"))
}

fn rfc8410_thumbprint() -> String {
    PublicKey::from_bytes(RFC8410_PUBLIC_KEY)
        .expect("RFC 8410 public key is valid")
        .thumbprint()
        .to_string()
}

/// Lock that serves only its well-known configuration; with a pre-issued access
/// token and no intervals, Cedarling needs nothing else from it at startup.
async fn lock_server() -> ServerGuard {
    let mut server = Server::new_async().await;
    let base = server.url();
    server
        .mock("GET", "/.well-known/lock-server-configuration")
        .with_body(
            json!({
                "version": "1.0",
                "issuer": base,
                "audit": {
                    "log_endpoint": format!("{base}/audit/log"),
                    "health_endpoint": format!("{base}/audit/health"),
                    "telemetry_endpoint": format!("{base}/audit/telemetry"),
                },
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
    server
}

fn lock_config(server: &ServerGuard) -> LockServiceConfig {
    LockServiceConfig {
        config_uri: format!("{}/.well-known/lock-server-configuration", server.url())
            .parse()
            .expect("mock Lock URL should parse"),
        access_token_jwt: Some("pre.issued.token".to_string()), // # gitleaks:allow
        ..Default::default()
    }
}

/// Everything TRACE needs: Lock and a `.cjar` store.
fn trace_ready_config(server: &ServerGuard, signing_key: TraceSigningKey) -> BootstrapConfig {
    let mut config = get_config(cjar_store());
    config.lock_config = Some(lock_config(server));
    config.trace_config = Some(trace_config(signing_key));
    config
}

async fn start_err(signing_key: TraceSigningKey) -> InitCedarlingError {
    let server = lock_server().await;
    Cedarling::new(&trace_ready_config(&server, signing_key))
        .await
        .err()
        .expect("a bad TRACE key should stop startup")
}

async fn logged_key_message(signing_key: TraceSigningKey) -> Option<String> {
    let server = lock_server().await;
    let cedarling = Cedarling::new(&trace_ready_config(&server, signing_key))
        .await
        .expect("Cedarling should start with a valid TRACE key");
    cedarling
        .pop_logs()
        .iter()
        .filter_map(|log| log.get("msg").and_then(Value::as_str))
        .find(|msg| msg.starts_with("TRACE producer key loaded"))
        .map(str::to_owned)
}

fn trace_config(signing_key: TraceSigningKey) -> TraceConfig {
    TraceConfig {
        producer_id: "cedarling-fleet-1".parse().expect("valid producer id"),
        signing_key,
        signing_kid: None,
        endpoint: TraceEndpoint::Discovered,
        bulk_max_records: NonZeroUsize::new(100).expect("non-zero"),
        bulk_max_bytes: NonZeroUsize::new(4_194_304).expect("non-zero"),
    }
}

fn cjar_store() -> PolicyStoreSource {
    PolicyStoreSource::ArchiveBytes(
        fixtures::minimal_valid()
            .build_archive()
            .expect("test archive should build"),
    )
}

fn yaml_store() -> PolicyStoreSource {
    PolicyStoreSource::Yaml(
        include_str!("../../../test_files/policy-store_no_trusted_issuers.yaml").to_string(),
    )
}

/// A `BootstrapConfig` built directly skips bootstrap decoding, so startup checks
/// what TRACE needs from the rest of the configuration again.
#[tokio::test]
async fn cedarling_new_rejects_trace_without_lock() {
    let mut config = get_config(cjar_store());
    config.trace_config = Some(trace_config(key_file()));
    let err = Cedarling::new(&config)
        .await
        .err()
        .expect("TRACE without Lock should stop startup");
    assert!(
        matches!(
            err,
            InitCedarlingError::TraceSetup(TraceSetupError::RequiresLock)
        ),
        "expected TraceSetup(RequiresLock), got {err:?}"
    );
}

#[tokio::test]
async fn cedarling_new_rejects_trace_with_yaml_store() {
    let server = lock_server().await;
    let mut config = trace_ready_config(&server, key_file());
    config.policy_store_config.source = yaml_store();
    let err = Cedarling::new(&config)
        .await
        .err()
        .expect("TRACE with a YAML store should stop startup");
    assert!(
        matches!(
            err,
            InitCedarlingError::TraceSetup(TraceSetupError::RequiresArchivePolicyStore)
        ),
        "expected TraceSetup(RequiresArchivePolicyStore), got {err:?}"
    );
}

#[tokio::test]
async fn cedarling_new_fails_on_bad_trace_key() {
    let err = start_err(TraceSigningKey::Pem("not a key".to_string())).await;
    assert!(
        matches!(
            err,
            InitCedarlingError::TraceKey(LoadProducerKeyError::InvalidKey {
                property: "CEDARLING_TRACE_SIGNING_KEY_PEM",
                path: None,
                ..
            })
        ),
        "expected TraceKey(InvalidKey) from the inline property, got {err:?}"
    );
}

#[tokio::test]
async fn cedarling_new_fails_on_missing_key_file() {
    let path = fixture("does-not-exist.pem");
    let err = start_err(TraceSigningKey::File(path.clone())).await;
    assert!(
        matches!(
            &err,
            InitCedarlingError::TraceKey(LoadProducerKeyError::ReadFile { path: p, .. })
                if *p == path
        ),
        "expected TraceKey(ReadFile) with the path, got {err:?}"
    );
}

#[tokio::test]
async fn cedarling_new_fails_on_encrypted_key_file() {
    let path = fixture("encrypted_ed25519.pem");
    let err = start_err(TraceSigningKey::File(path.clone())).await;
    assert!(
        matches!(
            &err,
            InitCedarlingError::TraceKey(LoadProducerKeyError::InvalidKey {
                property: "CEDARLING_TRACE_SIGNING_KEY",
                path: Some(p),
                ..
            }) if *p == path
        ),
        "expected TraceKey(InvalidKey) with the file path, got {err:?}"
    );
}

#[tokio::test]
async fn cedarling_new_logs_trace_key() {
    let thumbprint = rfc8410_thumbprint();
    assert_eq!(
        logged_key_message(key_file()).await,
        Some(format!(
            "TRACE producer key loaded: producer=cedarling-fleet-1 kid={thumbprint} \
             thumbprint={thumbprint}"
        )),
        "startup should log the producer, kid and thumbprint"
    );
}

#[tokio::test]
async fn inline_pem_key_matches_file() {
    let pem = std::fs::read_to_string(fixture("producer_ed25519.pem"))
        .expect("test key fixture should be readable");
    assert_eq!(
        logged_key_message(TraceSigningKey::Pem(pem)).await,
        logged_key_message(key_file()).await,
        "inline PEM and key file should load the same key"
    );
}
