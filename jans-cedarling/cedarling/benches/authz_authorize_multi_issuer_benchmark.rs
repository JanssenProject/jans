// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedarling::*;
use criterion::{BenchmarkId, Criterion, criterion_group, criterion_main};
use jsonwebtoken::Algorithm;
use serde_json::json;
use std::hint::black_box;
use std::{collections::HashSet, time::Duration};
use test_utils::token_claims::{
    KeyPair, generate_jwks, generate_keypair_hs256, generate_token_using_claims_and_keypair,
};
use tokio::runtime::Runtime;

const POLICY_STORE: &str = include_str!("../../test_files/policy-store-multi-issuer-basic.yaml");

fn authorize_multi_issuer(c: &mut Criterion) {
    let mut mock_server = mockito::Server::new();
    let runtime = Runtime::new().expect("init tokio runtime");

    // Setup OpenId config endpoint
    let oidc = json!({
        "issuer": mock_server.url(),
        "jwks_uri": &format!("{}/jwks", mock_server.url()),
    });
    let oidc_endpoint = mock_server
        .mock("GET", "/.well-known/openid-configuration")
        .with_status(200)
        .with_header("content-type", "application/json")
        .with_body(oidc.to_string())
        .expect_at_least(1)
        .create();

    // Setup JWKS endpoint
    let keys = generate_keypair_hs256(Some("some_hs256_key"));
    let jwks_endpoint = mock_server
        .mock("GET", "/jwks")
        .with_status(200)
        .with_header("content-type", "application/json")
        .with_body(json!({"keys": generate_jwks(&vec![keys.clone()]).keys}).to_string())
        .expect_at_least(1)
        .create();

    let cedarling = runtime
        .block_on(prepare_cedarling_with_jwt_validation(&mock_server.url()))
        .expect("should initialize Cedarling");

    let request = prepare_cedarling_request_for_multi_issuer_jwt_validation(keys);

    c.bench_with_input(
        BenchmarkId::new("authorize_multi_issuer", "tokio runtime"),
        &runtime,
        |b, rt| {
            b.to_async(rt)
                .iter(|| cedarling.authorize_multi_issuer(black_box(request.clone())));
        },
    );

    jwks_endpoint.assert();
    oidc_endpoint.assert();
}

fn measurement_config() -> Criterion {
    Criterion::default()
        .measurement_time(Duration::from_secs(5))
        .warm_up_time(Duration::from_secs(5))
}

criterion_group! {
    name = authz_authorize_multi_issuer_benchmark;
    config = measurement_config();
    targets = authorize_multi_issuer
}

criterion_main!(authz_authorize_multi_issuer_benchmark);

async fn prepare_cedarling_with_jwt_validation(
    base_idp_url: &str,
) -> Result<Cedarling, InitCedarlingError> {
    let mut policy_store =
        serde_yml::from_str::<serde_yml::Value>(POLICY_STORE).expect("a valid YAML policy store");

    policy_store["policy_stores"]["multi_issuer_basic_store"]["trusted_issuers"]["AcmeIssuer"]["openid_configuration_endpoint"] =
        format!("{}/.well-known/openid-configuration", base_idp_url).into();

    policy_store["policy_stores"]["multi_issuer_basic_store"]["trusted_issuers"]["DolphinIssuer"]
        ["openid_configuration_endpoint"] =
        format!("{}/.well-known/openid-configuration", base_idp_url).into();

    let bootstrap_config = BootstrapConfig {
        application_name: "test_app".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::Off,
            log_level: LogLevel::DEBUG,
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
        },
        authorization_config: AuthorizationConfig {
            use_user_principal: false,
            use_workload_principal: false,
            principal_bool_operator: JsonRule::default(),
            id_token_trust_mode: IdTokenTrustMode::Never,
            ..Default::default()
        },
        entity_builder_config: EntityBuilderConfig::default()
            .with_no_workload()
            .with_no_user(),
        lock_config: None,
        max_base64_size: None,
        max_default_entities: None,
        token_cache_max_ttl_secs: 60,
    };

    Cedarling::new(&bootstrap_config).await
}

pub fn prepare_cedarling_request_for_multi_issuer_jwt_validation(
    keys: KeyPair,
) -> AuthorizeMultiIssuerRequest {
    let dolphin_access_token = generate_token_using_claims_and_keypair(
        &json!({
            "iss": "https://idp.dolphin.sea",
            "sub": "dolphin_user_123",
            "jti": "dolphin123",
            "client_id": "dolphin_client_123",
            "aud": "dolphin_audience",
            "location": ["miami", "orlando"],
            "scope": ["read", "write"],
            "exp": 2000000000,
            "iat": 1516239022
        }),
        &keys,
    );

    // Create a dolphin_token for the user entity
    let dolphin_user_token = generate_token_using_claims_and_keypair(
        &json!({
            "iss": "https://idp.dolphin.sea",
            "sub": "dolphin_user_123",
            "jti": "dolphin_user_123",
            "client_id": "dolphin_client_123",
            "aud": "dolphin_audience",
            "exp": 2000000000,
            "iat": 1516239022
        }),
        &keys,
    );

    AuthorizeMultiIssuerRequest::new_with_fields(
        vec![
            TokenInput::new("Dolphin::Access_Token".to_string(), dolphin_access_token),
            TokenInput::new("Dolphin::Dolphin_Token".to_string(), dolphin_user_token),
        ],
        EntityData::from_json(
            &json!({
                "cedar_entity_mapping": {
                    "entity_type": "Acme::Resource",
                    "id": "1694c954f8a3"
                },
            })
            .to_string(),
        )
        .expect("Failed to create resource entity"),
        "Acme::Action::\"GetFood\"".to_string(),
        None,
    )
}
