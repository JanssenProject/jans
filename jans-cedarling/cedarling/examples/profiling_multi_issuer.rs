// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

#![allow(unused_imports)]
#![allow(dead_code)]

use cedarling::{
    AuthorizationConfig, AuthorizeMultiIssuerRequest, BootstrapConfig, Cedarling, DataStoreConfig,
    EntityBuilderConfig, EntityData, InitCedarlingError, JsonRule, JwtConfig, LogConfig, LogLevel,
    LogTypeConfig, PolicyStoreConfig, TokenInput,
};
use serde_json::json;
use std::collections::HashSet;
use std::fs::File;

#[cfg(not(any(target_arch = "wasm32", target_os = "windows")))]
use jsonwebtoken::Algorithm;
#[cfg(not(any(target_arch = "wasm32", target_os = "windows")))]
use test_utils::token_claims::generate_token_using_claims_and_keypair;
#[cfg(not(any(target_arch = "wasm32", target_os = "windows")))]
use test_utils::{MockServer, gen_mock_server};

const POLICY_STORE_RAW: &str =
    include_str!("../../test_files/policy-store-multi-issuer-basic.yaml");

#[cfg(not(any(target_arch = "wasm32", target_os = "windows")))]
#[tokio::main(flavor = "current_thread")]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let mock1 = gen_mock_server();
    let mock2 = gen_mock_server();

    let cedarling = init_cedarling_multi_issuer(&mock1.base_idp_url, &mock2.base_idp_url).await?;

    let request = prepare_cedarling_request_for_multi_issuer_jwt_validation(&mock1, &mock2);

    // Validate that the authorization request executes correctly before profiling
    let validation_result = cedarling
        .authorize_multi_issuer(request.clone())
        .await
        .expect("authorization validation should succeed");
    assert!(
        validation_result.decision,
        "authorization validation should return Allow decision"
    );

    // init profiler guard
    let guard = pprof::ProfilerGuardBuilder::default()
        .frequency(1000)
        .blocklist(&["libc", "libgcc", "pthread", "vdso"])
        .build()
        .unwrap();

    for _ in 0..1000 {
        call_authorize_multi_issuer(&cedarling, &request).await;
    }

    if let Ok(report) = guard.report().build() {
        println!("report: {:?}", &report);

        // write output flamegraph to an SVG file
        let file = File::create(format!(
            "{}/../{}",
            env!("CARGO_MANIFEST_DIR"),
            "cedarling_profiling_multi_issuer_flamegraph.svg",
        ))
        .unwrap();
        let mut options = pprof::flamegraph::Options::default();
        options.image_width = Some(3000);
        report.flamegraph_with_options(file, &mut options).unwrap();
    }

    Ok(())
}

#[cfg(not(any(target_arch = "wasm32", target_os = "windows")))]
async fn init_cedarling_multi_issuer(
    base_idp_url1: &str,
    base_idp_url2: &str,
) -> Result<Cedarling, InitCedarlingError> {
    let mut policy_store = serde_yml::from_str::<serde_yml::Value>(POLICY_STORE_RAW)
        .expect("a valid YAML policy store");

    policy_store["policy_stores"]["multi_issuer_basic_store"]["trusted_issuers"]["AcmeIssuer"]["openid_configuration_endpoint"] =
        format!("{base_idp_url1}/.well-known/openid-configuration").into();

    policy_store["policy_stores"]["multi_issuer_basic_store"]["trusted_issuers"]["DolphinIssuer"]
        ["openid_configuration_endpoint"] =
        format!("{base_idp_url2}/.well-known/openid-configuration").into();

    let bootstrap_config = BootstrapConfig {
        application_name: "test_app".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::Off,
            log_level: LogLevel::INFO,
        },
        policy_store_config: PolicyStoreConfig {
            source: cedarling::PolicyStoreSource::Yaml(
                serde_yml::to_string(&policy_store).expect("serialize policy store to YAML"),
            ),
        },
        jwt_config: JwtConfig {
            jwks: None,
            jwt_sig_validation: true,
            jwt_status_validation: false,
            signature_algorithms_supported: HashSet::from([Algorithm::HS256]),
            ..Default::default()
        },
        authorization_config: AuthorizationConfig {
            principal_bool_operator: JsonRule::default(),
            ..Default::default()
        },
        entity_builder_config: EntityBuilderConfig::default(),
        lock_config: None,
        max_base64_size: None,
        max_default_entities: None,
        data_store_config: DataStoreConfig::default(),
    };

    Cedarling::new(&bootstrap_config).await
}

#[cfg(not(any(target_arch = "wasm32", target_os = "windows")))]
fn prepare_cedarling_request_for_multi_issuer_jwt_validation(
    mock1: &MockServer,
    mock2: &MockServer,
) -> AuthorizeMultiIssuerRequest {
    let acme_multi_token = generate_token_using_claims_and_keypair(
        &json!({
            "iss": mock1.base_idp_url,
            "sub": "acme_user_multi",
            "jti": "acme_multi_123",
            "client_id": "acme_multi_client_123",
            "aud": "acme_multi_audience",
            "scope": ["read:wiki", "write:profile"],
            "exp": 2_000_000_000,
            "iat": 1_516_239_022
        }),
        &mock1.keys,
    );

    let dolphin_multi_token = generate_token_using_claims_and_keypair(
        &json!({
            "iss": mock2.base_idp_url,
            "sub": "dolphin_user_multi",
            "jti": "dolphin_multi_456",
            "client_id": "dolphin_multi_client_456",
            "aud": "dolphin_multi_audience",
            "location": ["miami"],
            "exp": 2_000_000_000,
            "iat": 1_516_239_022
        }),
        &mock2.keys,
    );

    AuthorizeMultiIssuerRequest::new_with_fields(
        vec![
            TokenInput::new("Acme::Access_Token".to_string(), acme_multi_token),
            TokenInput::new("Dolphin::Access_Token".to_string(), dolphin_multi_token),
        ],
        EntityData::from_json(
            &json!({
                "cedar_entity_mapping": {
                    "entity_type": "Acme::Resource",
                    "id": "WikiPages"
                },
                "name": "Wiki Pages"
            })
            .to_string(),
        )
        .expect("Failed to create resource entity"),
        "Acme::Action::\"ReadProfile\"".to_string(),
        None,
    )
}

#[cfg(not(any(target_arch = "wasm32", target_os = "windows")))]
async fn call_authorize_multi_issuer(cedarling: &Cedarling, request: &AuthorizeMultiIssuerRequest) {
    let _result = cedarling.authorize_multi_issuer(request.clone()).await;
}

/// just define a main function to satisfy the compiler.
#[cfg(any(target_arch = "wasm32", target_os = "windows"))]
#[tokio::main(flavor = "current_thread")]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    unimplemented!("Profiling is not supported on wasm32 or windows.")
}
