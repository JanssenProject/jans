// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedarling::{
    AuthorizationConfig, BootstrapConfig, Cedarling, EntityBuilderConfig, IdTokenTrustMode,
    InitCedarlingError, JsonRule, JwtConfig, LogConfig, LogLevel, LogTypeConfig, PolicyStoreConfig,
    PolicyStoreSource, Request,
};
use criterion::{BenchmarkId, Criterion, criterion_group, criterion_main};
use jsonwebtoken::Algorithm;
use serde::Deserialize;
use serde_json::json;
use std::hint::black_box;
use std::{collections::HashSet, time::Duration};
use test_utils::gen_mock_server;
use test_utils::token_claims::{
    KeyPair, generate_token_using_claims, generate_token_using_claims_and_keypair,
};
use tokio::runtime::Runtime;

const POLICY_STORE: &str = include_str!("../../test_files/policy-store_ok.yaml");

// Validates that the cedarling instance actually works before benchmarking.
async fn validate_cedarling_works(cedarling: &Cedarling, request: &Request) {
    let result = cedarling
        .authorize(request.clone())
        .await
        .expect("authorization call failed");

    let is_allowed = match result.cedar_decision() {
        cedar_policy::Decision::Allow => true,
        cedar_policy::Decision::Deny => false,
    };

    assert!(is_allowed, "got invalid authorization result");
}

fn without_jwt_validation_benchmark(c: &mut Criterion) {
    let runtime = Runtime::new().expect("init tokio runtime");

    let cedarling = runtime
        .block_on(prepare_cedarling_without_jwt_validation())
        .expect("should initialize Cedarling");

    let request = prepare_cedarling_request_for_without_jwt_validation();

    runtime.block_on(validate_cedarling_works(&cedarling, &request));

    c.bench_with_input(
        BenchmarkId::new("authz_authorize_without_jwt_validation", "tokio runtime"),
        &runtime,
        |b, rt| {
            b.to_async(rt)
                .iter(|| cedarling.authorize(black_box(request.clone())));
        },
    );
}

fn with_jwt_validation_hs256_benchmark(c: &mut Criterion) {
    let mock1 = gen_mock_server();
    let mock2 = gen_mock_server();

    let runtime = Runtime::new().expect("init tokio runtime");

    assert_ne!(&mock1.base_idp_url, &mock2.base_idp_url);

    let cedarling = runtime
        .block_on(prepare_cedarling_with_jwt_validation(
            &mock1.base_idp_url,
            &mock2.base_idp_url,
        ))
        .expect("should initialize Cedarling");

    let request =
        prepare_cedarling_request_for_with_jwt_validation(&mock1.keys, &mock1.base_idp_url);

    runtime.block_on(validate_cedarling_works(&cedarling, &request));

    c.bench_with_input(
        BenchmarkId::new("authz_authorize_with_jwt_validation_hs256", "tokio runtime"),
        &runtime,
        |b, rt| {
            b.to_async(rt)
                .iter(|| cedarling.authorize(black_box(request.clone())));
        },
    );

    mock1.jwks_endpoint.assert();
    mock1.oidc_endpoint.assert();
    mock2.jwks_endpoint.assert();
    mock2.oidc_endpoint.assert();
}

fn measurement_config() -> Criterion {
    Criterion::default()
        .measurement_time(Duration::from_secs(5))
        .warm_up_time(Duration::from_secs(5))
}

criterion_group! {
    name = authz_benchmark;
    config = measurement_config();
    targets = without_jwt_validation_benchmark, with_jwt_validation_hs256_benchmark
}

criterion_main!(authz_benchmark);

async fn prepare_cedarling_without_jwt_validation() -> Result<Cedarling, InitCedarlingError> {
    let bootstrap_config = BootstrapConfig {
        application_name: "test_app".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::Off,
            log_level: LogLevel::DEBUG,
        },
        policy_store_config: PolicyStoreConfig {
            source: PolicyStoreSource::Yaml(POLICY_STORE.to_string()),
        },
        jwt_config: JwtConfig::new_without_validation(),
        authorization_config: AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: true,
            principal_bool_operator: JsonRule::default(),
            id_token_trust_mode: IdTokenTrustMode::Never,
            ..Default::default()
        },
        entity_builder_config: EntityBuilderConfig::default().with_workload().with_user(),
        lock_config: None,
        max_base64_size: None,
        max_default_entities: None,
    };

    Cedarling::new(&bootstrap_config).await
}

async fn prepare_cedarling_with_jwt_validation(
    base_idp_url_1: &str,
    base_idp_url_2: &str,
) -> Result<Cedarling, InitCedarlingError> {
    let mut policy_store =
        serde_yml::from_str::<serde_yml::Value>(POLICY_STORE).expect("a valid YAML policy store");

    // We overwrite the idp endpoint here with out mock server
    policy_store["policy_stores"]["a1bf93115de86de760ee0bea1d529b521489e5a11747"]["trusted_issuers"]
        ["Jans"]["openid_configuration_endpoint"] =
        format!("{base_idp_url_1}/.well-known/openid-configuration").into();

    // Also update the AnotherIssuer to use the mock server
    policy_store["policy_stores"]["a1bf93115de86de760ee0bea1d529b521489e5a11747"]["trusted_issuers"]
        ["Jans2"]["openid_configuration_endpoint"] =
        format!("{base_idp_url_2}/.well-known/openid-configuration").into();

    let bootstrap_config = BootstrapConfig {
        application_name: "test_app".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::Off,
            log_level: LogLevel::DEBUG,
        },
        policy_store_config: PolicyStoreConfig {
            source: PolicyStoreSource::Yaml(
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
            use_user_principal: true,
            use_workload_principal: true,
            principal_bool_operator: JsonRule::default(),
            id_token_trust_mode: IdTokenTrustMode::Never,
            ..Default::default()
        },
        entity_builder_config: EntityBuilderConfig::default().with_workload().with_user(),
        lock_config: None,
        max_base64_size: None,
        max_default_entities: None,
    };

    Cedarling::new(&bootstrap_config).await
}

/// # Panics
///
/// Panics if the JSON is not valid.
#[must_use]
fn prepare_cedarling_request_for_without_jwt_validation() -> Request {
    Request::deserialize(serde_json::json!(
        {
            "tokens": {
                "access_token": generate_token_using_claims(json!({
                    "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
                    "code": "bf1934f6-3905-420a-8299-6b2e3ffddd6e",
                    "iss": "https://test.jans.org",
                    "token_type": "Bearer",
                    "client_id": "5b4487c4-8db1-409d-a653-f907b8094039",
                    "aud": "5b4487c4-8db1-409d-a653-f907b8094039",
                    "acr": "basic",
                    "x5t#S256": "",
                    "scope": [
                      "openid",
                      "profile"
                    ],
                    "org_id": "some_long_id",
                    "auth_time": 1_724_830_746,
                    "exp": 1_724_945_978,
                    "iat": 1_724_832_259,
                    "jti": "lxTmCVRFTxOjJgvEEpozMQ",
                    "name": "Default Admin User",
                    "status": {
                      "status_list": {
                        "idx": 201,
                        "uri": "https://admin-ui-test.gluu.org/jans-auth/restv1/status_list"
                      }
                    }
                })),
                "id_token": generate_token_using_claims(json!({
                    "acr": "basic",
                    "amr": "10",
                    "aud": "5b4487c4-8db1-409d-a653-f907b8094039",
                    "exp": 1_724_835_859,
                    "iat": 1_724_832_259,
                    "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
                    "iss": "https://test.jans.org",
                    "jti": "sk3T40NYSYuk5saHZNpkZw",
                    "nonce": "c3872af9-a0f5-4c3f-a1af-f9d0e8846e81",
                    "sid": "6a7fe50a-d810-454d-be5d-549d29595a09",
                    "jansOpenIDConnectVersion": "openidconnect-1.0",
                    "c_hash": "pGoK6Y_RKcWHkUecM9uw6Q",
                    "auth_time": 1_724_830_746,
                    "grant": "authorization_code",
                    "status": {
                      "status_list": {
                        "idx": 202,
                        "uri": "https://admin-ui-test.gluu.org/jans-auth/restv1/status_list"
                      }
                    },
                    "role":"Admin"
                })),
                "userinfo_token":  generate_token_using_claims(json!({
                    "country": "US",
                    "email": "user@example.com",
                    "username": "UserNameExample",
                    "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
                    "iss": "https://test.jans.org",
                    "given_name": "Admin",
                    "middle_name": "Admin",
                    "inum": "8d1cde6a-1447-4766-b3c8-16663e13b458",
                    "client_id": "5b4487c4-8db1-409d-a653-f907b8094039",
                    "aud": "5b4487c4-8db1-409d-a653-f907b8094039",
                    "updated_at": 1_724_778_591,
                    "name": "Default Admin User",
                    "nickname": "Admin",
                    "family_name": "User",
                    "jti": "faiYvaYIT0cDAT7Fow0pQw",
                    "jansAdminUIRole": [
                        "api-admin"
                    ],
                    "exp": 1_724_945_978
                })),
            },
            "action": "Jans::Action::\"Update\"",
            "resource": {
                "cedar_entity_mapping": {
                    "entity_type": "Jans::Issue",
                    "id": "random_id"
                },
                "org_id": "some_long_id",
                "country": "US"
            },
            "context": {},
        }
    ))
    .expect("should build request")
}

/// # Panics
///
/// Panics if the JSON is not valid.
#[must_use]
fn prepare_cedarling_request_for_with_jwt_validation(keys1: &KeyPair, issuer_url: &str) -> Request {
    Request::deserialize(serde_json::json!(
        {
            "tokens": {
                "access_token": generate_token_using_claims_and_keypair(&json!({
                    "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
                    "code": "bf1934f6-3905-420a-8299-6b2e3ffddd6e",
                    "iss": issuer_url,
                    "token_type": "Bearer",
                    "client_id": "5b4487c4-8db1-409d-a653-f907b8094039",
                    "aud": "5b4487c4-8db1-409d-a653-f907b8094039",
                    "acr": "basic",
                    "x5t#S256": "",
                    "scope": [
                      "openid",
                      "profile"
                    ],
                    "org_id": "some_long_id",
                    "auth_time": 1_724_830_746,
                    "exp": 1_724_945_978,
                    "iat": 1_724_832_259,
                    "jti": "lxTmCVRFTxOjJgvEEpozMQ",
                    "name": "Default Admin User",
                    "status": {
                      "status_list": {
                        "idx": 201,
                        "uri": "https://admin-ui-test.gluu.org/jans-auth/restv1/status_list"
                      }
                    }
                }), keys1),
                "id_token": generate_token_using_claims_and_keypair(&json!({
                    "acr": "basic",
                    "amr": "10",
                    "aud": "5b4487c4-8db1-409d-a653-f907b8094039",
                    "exp": 1_724_835_859,
                    "iat": 1_724_832_259,
                    "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
                    "iss": issuer_url,
                    "jti": "sk3T40NYSYuk5saHZNpkZw",
                    "nonce": "c3872af9-a0f5-4c3f-a1af-f9d0e8846e81",
                    "sid": "6a7fe50a-d810-454d-be5d-549d29595a09",
                    "jansOpenIDConnectVersion": "openidconnect-1.0",
                    "c_hash": "pGoK6Y_RKcWHkUecM9uw6Q",
                    "auth_time": 1_724_830_746,
                    "grant": "authorization_code",
                    "status": {
                      "status_list": {
                        "idx": 202,
                        "uri": "https://admin-ui-test.gluu.org/jans-auth/restv1/status_list"
                      }
                    },
                    "role":"Admin"
                }), keys1),
                "userinfo_token":  generate_token_using_claims_and_keypair(&json!({
                    "country": "US",
                    "email": "user@example.com",
                    "username": "UserNameExample",
                    "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
                    "iss": issuer_url,
                    "given_name": "Admin",
                    "middle_name": "Admin",
                    "inum": "8d1cde6a-1447-4766-b3c8-16663e13b458",
                    "client_id": "5b4487c4-8db1-409d-a653-f907b8094039",
                    "aud": "5b4487c4-8db1-409d-a653-f907b8094039",
                    "updated_at": 1_724_778_591,
                    "name": "Default Admin User",
                    "nickname": "Admin",
                    "family_name": "User",
                    "jti": "faiYvaYIT0cDAT7Fow0pQw",
                    "jansAdminUIRole": [
                        "api-admin"
                    ],
                    "exp": 1_724_945_978
                }), keys1),
            },
            "action": "Jans::Action::\"Update\"",
            "resource": {
                "cedar_entity_mapping": {
                    "entity_type": "Jans::Issue",
                    "id": "random_id"
                },
                "org_id": "some_long_id",
                "country": "US"
            },
            "context": {},
        }
    ))
    .expect("should build request")
}
