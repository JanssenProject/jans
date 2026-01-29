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
use test_utils::token_claims::generate_token_using_claims_and_keypair;
use test_utils::{MockServer, gen_mock_server};
use tokio::runtime::Runtime;

const POLICY_STORE: &str = include_str!("../../test_files/policy-store-multi-issuer-basic.yaml");

fn authorize_multi_issuer(c: &mut Criterion) {
    let mock1 = gen_mock_server();
    let mock2 = gen_mock_server();

    let runtime = Runtime::new().expect("init tokio runtime");

    let cedarling = runtime
        .block_on(prepare_cedarling_with_jwt_validation(
            &mock1.base_idp_url,
            &mock2.base_idp_url,
        ))
        .expect("should initialize Cedarling");

    let request = prepare_cedarling_request_for_multi_issuer_jwt_validation(&mock1, &mock2);

    c.bench_with_input(
        BenchmarkId::new("authorize_multi_issuer", "tokio runtime"),
        &runtime,
        |b, rt| {
            b.to_async(rt).iter(|| {
                let req = black_box(request.clone());
                async {
                    let result = cedarling.authorize_multi_issuer(req).await;
                    match result {
                        Ok(v) => {
                            assert!(v.decision, "should be true")
                        },
                        Err(e) => {
                            panic!("{}, {:?}", e, e)
                        },
                    }
                }
            });
        },
    );
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
    base_idp_url1: &str,
    base_idp_url2: &str,
) -> Result<Cedarling, InitCedarlingError> {
    let mut policy_store =
        serde_yml::from_str::<serde_yml::Value>(POLICY_STORE).expect("a valid YAML policy store");

    policy_store["policy_stores"]["multi_issuer_basic_store"]["trusted_issuers"]["AcmeIssuer"]["openid_configuration_endpoint"] =
        format!("{}/.well-known/openid-configuration", base_idp_url1).into();

    policy_store["policy_stores"]["multi_issuer_basic_store"]["trusted_issuers"]["DolphinIssuer"]
        ["openid_configuration_endpoint"] =
        format!("{}/.well-known/openid-configuration", base_idp_url2).into();

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
        data_store_config: None,
    };

    Cedarling::new(&bootstrap_config).await
}

pub fn prepare_cedarling_request_for_multi_issuer_jwt_validation(
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
            "exp": 2000000000,
            "iat": 1516239022
        }),
        &mock1.keys.to_owned(),
    );

    let dolphin_multi_token = generate_token_using_claims_and_keypair(
        &json!({
            "iss": mock2.base_idp_url,
            "sub": "dolphin_user_multi",
            "jti": "dolphin_multi_456",
            "client_id": "dolphin_multi_client_456",
            "aud": "dolphin_multi_audience",
            "location": ["miami"],
            "exp": 2000000000,
            "iat": 1516239022
        }),
        &mock2.keys.to_owned(),
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
