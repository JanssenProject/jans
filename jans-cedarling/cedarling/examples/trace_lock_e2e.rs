// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Sends TRACE records for real decisions to a running Lock Server.
//!
//! Half of `DECISIONS` go through `authorize_unsigned_batch` and the rest through
//! `authorize_multi_issuer_batch`, all under one `TRACE_EXECUTION_ID`, so the records can be
//! read back from Lock's execution view. The Cedarling log goes to stdout at DEBUG: it names
//! the producer chain and the `content_digest` of every record Lock accepted.
//!
//! Environment:
//! - `LOCK_CONFIG_URI`: Lock well-known configuration
//! - `SSA_JWT_FILE`: SSA for dynamic client registration with `trace.write`
//! - `TRACE_KEY_FILE`: Ed25519 PKCS#8 PEM, registered in Lock under its thumbprint `kid`
//! - `POLICY_STORE_CJAR`: the `.cjar` Lock serves, so `bundle_hash` matches Lock's
//! - `TRACE_EXECUTION_ID`: execution the records are filed under
//! - `DECISIONS`: number of decisions, default 20
//! - `TRACE_PRODUCER_ID`: default `cedarling-fleet-1`
//!
//! Run: `cargo run --example trace_lock_e2e`

use std::collections::{HashMap, HashSet};
use std::num::NonZeroUsize;
use std::path::PathBuf;

use cedarling::log_config::StdOutLoggerMode;
use cedarling::{
    AuthorizationConfig, AuthorizeOptions, BatchAuthorizeMultiIssuerRequest,
    BatchAuthorizeUnsignedRequest, BatchItem, BootstrapConfig, CedarEntityMapping, Cedarling,
    DataStoreConfig, EntityData, HttpClientConfig, JwtConfig, LockServiceConfig, LockTransport,
    LogConfig, LogLevel, LogTypeConfig, PolicyStoreConfig, PolicyStoreSource, TokenInput,
    TraceConfig, TraceContext, TraceEndpoint, TraceSigningKey,
};
use serde_json::json;

const EXECUTION_AUTHORITY: &str = "https://e2e.example.com";
const WORKLOAD_ID: &str = "trace-lock-e2e";
/// Issuer of `MyStore.cjar`'s trusted issuer; signatures are not validated here.
const TOKEN_ISSUER: &str = "https://admin-ui-test.gluu.org";

fn env(name: &str) -> Result<String, Box<dyn std::error::Error>> {
    std::env::var(name).map_err(|_| format!("set {name}").into())
}

#[tokio::main(flavor = "multi_thread")]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let decisions: usize = std::env::var("DECISIONS").map_or(Ok(20), |n| n.parse())?;
    let execution_id = env("TRACE_EXECUTION_ID")?;
    let cedarling = Cedarling::new(&bootstrap_config()?).await?;

    let options = AuthorizeOptions::with_trace(TraceContext::new(
        execution_id,
        EXECUTION_AUTHORITY.to_string(),
        WORKLOAD_ID.to_string(),
    ));
    let unsigned = decisions / 2;
    let multi_issuer = decisions - unsigned;

    let unsigned_batch = cedarling
        .authorize_unsigned_batch(
            BatchAuthorizeUnsignedRequest::new(
                Some(entity("Jans::Any", "e2e-principal")),
                items(unsigned),
            )
            .with_options(options.clone()),
        )
        .await?;
    let multi_issuer_batch = cedarling
        .authorize_multi_issuer_batch(
            BatchAuthorizeMultiIssuerRequest::new(vec![userinfo_token()], items(multi_issuer))
                .with_options(options),
        )
        .await?;

    for result in &unsigned_batch.results {
        let result = result.as_ref().map_err(ToString::to_string)?;
        println!(
            "decision unsigned request_id={} allowed={}",
            result.request_id, result.decision
        );
    }
    for result in &multi_issuer_batch.results {
        let result = result.as_ref().map_err(ToString::to_string)?;
        println!(
            "decision multi_issuer request_id={} allowed={}",
            result.request_id, result.decision
        );
    }

    // `shut_down` sends every record still queued, with full retries.
    cedarling.shut_down().await;
    let metrics = cedarling.drain_metrics()?;
    let mut trace: Vec<_> = metrics
        .operational_stats
        .iter()
        .filter(|(key, _)| key.starts_with("trace."))
        .collect();
    trace.sort();
    for (key, value) in trace {
        println!("metric {key}={value}");
    }
    Ok(())
}

/// Cedarling with Lock reached through DCR and TRACE signing with `TRACE_KEY_FILE`.
fn bootstrap_config() -> Result<BootstrapConfig, Box<dyn std::error::Error>> {
    let producer_id =
        std::env::var("TRACE_PRODUCER_ID").unwrap_or_else(|_| "cedarling-fleet-1".to_string());

    let lock_config = LockServiceConfig {
        log_level: LogLevel::WARN,
        config_uri: env("LOCK_CONFIG_URI")?.parse()?,
        ssa_jwt: Some(
            std::fs::read_to_string(env("SSA_JWT_FILE")?)?
                .trim()
                .to_string(),
        ),
        log_interval: None,
        health_interval: None,
        telemetry_interval: None,
        listen_sse: false,
        accept_invalid_certs: true,
        transport: LockTransport::Rest,
        ..Default::default()
    };

    Ok(BootstrapConfig {
        application_name: "trace_lock_e2e".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::StdOut(StdOutLoggerMode::Immediate),
            log_level: LogLevel::DEBUG,
        },
        policy_store_config: PolicyStoreConfig {
            source: PolicyStoreSource::ArchiveBytes(std::fs::read(env("POLICY_STORE_CJAR")?)?),
            ..Default::default()
        },
        jwt_config: JwtConfig {
            jwks: None,
            jwt_sig_validation: false,
            jwt_status_validation: false,
            signature_algorithms_supported: HashSet::new(),
            ..Default::default()
        }
        .allow_all_algorithms(),
        authorization_config: AuthorizationConfig {
            metrics_collection: true,
            ..Default::default()
        },
        lock_config: Some(lock_config),
        max_default_entities: None,
        max_base64_size: None,
        data_store_config: DataStoreConfig::default(),
        http_client_config: HttpClientConfig::default(),
        trace_config: Some(TraceConfig {
            producer_id: producer_id.parse()?,
            signing_key: TraceSigningKey::File(PathBuf::from(env("TRACE_KEY_FILE")?)),
            signing_kid: None,
            endpoint: TraceEndpoint::Discovered,
            bulk_max_records: NonZeroUsize::new(100).expect("non-zero"),
            bulk_max_bytes: NonZeroUsize::new(4_194_304).expect("non-zero"),
        }),
    })
}

fn entity(entity_type: &str, id: &str) -> EntityData {
    EntityData {
        cedar_mapping: CedarEntityMapping {
            entity_type: entity_type.to_string(),
            id: id.to_string(),
        },
        attributes: HashMap::new(),
    }
}

/// Alternates `Read` and `Write` on the dashboard, which `MyStore.cjar` allows to an admin.
fn items(count: usize) -> Vec<BatchItem> {
    (0..count)
        .map(|index| BatchItem {
            resource: entity("Jans::Application", "dashboard"),
            action: if index % 2 == 0 {
                "Jans::Action::\"Read\""
            } else {
                "Jans::Action::\"Write\""
            }
            .to_string(),
            context: json!({}),
        })
        .collect()
}

fn userinfo_token() -> TokenInput {
    let claims = json!({
        "iss": TOKEN_ISSUER,
        "sub": "e2e-user",
        "jti": "e2e-userinfo",
        "role": ["admin"],
        "exp": 4_000_000_000_i64,
        "iat": 1_700_000_000_i64,
    });
    TokenInput::new("Jans::Userinfo_token".to_string(), unsigned_jwt(&claims))
}

/// A JWT with a placeholder signature: Cedarling here does not validate signatures, but
/// still needs an algorithm it knows in the header.
fn unsigned_jwt(claims: &serde_json::Value) -> String {
    use base64::Engine as _;
    let encode = |value: &serde_json::Value| {
        base64::engine::general_purpose::URL_SAFE_NO_PAD.encode(value.to_string())
    };
    format!(
        "{}.{}.c2lnbmF0dXJl",
        encode(&json!({"alg": "HS256", "typ": "JWT"})),
        encode(claims)
    )
}
