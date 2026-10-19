// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Startup with a TRACE producer key: a bad key stops `Cedarling::new`, a good
//! one is logged so the admin can register it in Lock.

use std::num::NonZeroUsize;
use std::path::{Path, PathBuf};

use jans_trace_core::key::PublicKey;
use serde_json::Value;

use crate::log::interface::LogStorage;
use crate::tests::utils::cedarling_util::get_config;
use crate::{
    Cedarling, InitCedarlingError, LoadProducerKeyError, PolicyStoreSource, TraceConfig,
    TraceEndpoint, TraceSigningKey,
};

/// Public key of the RFC 8410 section 10.3 example key in `test_files/trace`.
const RFC8410_PUBLIC_KEY: [u8; 32] = [
    0x19, 0xbf, 0x44, 0x09, 0x69, 0x84, 0xcd, 0xfe, 0x85, 0x41, 0xba, 0xc1, 0x67, 0xdc, 0x3b, 0x96,
    0xc8, 0x50, 0x86, 0xaa, 0x30, 0xb6, 0xb6, 0xcb, 0x0c, 0x5c, 0x38, 0xad, 0x70, 0x31, 0x66, 0xe1,
];

fn fixture(name: &str) -> PathBuf {
    Path::new(env!("CARGO_MANIFEST_DIR"))
        .join("../test_files/trace")
        .join(name)
}

fn key_file() -> TraceSigningKey {
    TraceSigningKey::File(fixture("producer_ed25519.pem"))
}

fn rfc8410_thumbprint() -> String {
    PublicKey::from_bytes(&RFC8410_PUBLIC_KEY)
        .expect("RFC 8410 public key is valid")
        .thumbprint()
        .to_string()
}

async fn start_err(signing_key: TraceSigningKey) -> InitCedarlingError {
    let mut config = get_config(yaml_store());
    config.trace_config = Some(trace_config(signing_key));
    Cedarling::new(&config)
        .await
        .err()
        .expect("a bad TRACE key should stop startup")
}

async fn logged_key_message(signing_key: TraceSigningKey) -> Option<String> {
    let mut config = get_config(yaml_store());
    config.trace_config = Some(trace_config(signing_key));
    let cedarling = Cedarling::new(&config)
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

fn yaml_store() -> PolicyStoreSource {
    PolicyStoreSource::Yaml(
        include_str!("../../../test_files/policy-store_no_trusted_issuers.yaml").to_string(),
    )
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
