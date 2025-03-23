// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedarling::*;
use criterion::{BenchmarkId, Criterion, black_box, criterion_group, criterion_main};
use jsonwebtoken::Algorithm;
use jsonwebtoken::jwk::JwkSet;
use mockito::{Server, ServerGuard};
use serde::Deserialize;
use serde_json::json;
use std::error::Error;
use test_utils::generate_token;
use tokio::runtime::Runtime;

const POLICY_STORE: &str = include_str!("../../test_files/policy-store_ok.yaml");

fn without_jwt_validation_benchmark(c: &mut Criterion) {
    let mut mock_server = Server::new();
    let runtime = Runtime::new().expect("init tokio runtime");

    let request = prepare_request(generate_token::Algorithm::HS256, &mut mock_server)
        .expect("should prepare request");

    let cedarling = runtime
        .block_on(prepare_cedarling_config(
            false,
            &[Algorithm::HS256],
            &mock_server.url(),
        ))
        .expect("should initialize Cedarling");

    c.bench_with_input(
        BenchmarkId::new("authz_without_jwt_validation", "tokio runtime"),
        &runtime,
        |b, rt| {
            b.to_async(rt).iter(|| async {
                let cloned_request = request.clone();
                let result = black_box(cedarling.authorize(cloned_request).await);
                assert!(result.is_ok(), "succesful authz: {:?}", result);
            });
        },
    );
}

fn with_jwt_validation_benchmark_hs256(c: &mut Criterion) {
    let mut mock_server = Server::new();
    let runtime = Runtime::new().expect("init tokio runtime");

    let request = prepare_request(generate_token::Algorithm::HS256, &mut mock_server)
        .expect("should prepare request");

    let cedarling = runtime
        .block_on(prepare_cedarling_config(
            true,
            &[Algorithm::HS256],
            &mock_server.url(),
        ))
        .expect("should initialize Cedarling");

    c.bench_with_input(
        BenchmarkId::new("authz_with_jwt_validation_hs256", "tokio runtime"),
        &runtime,
        |b, rt| {
            b.to_async(rt).iter(|| async {
                let cloned_request = request.clone();
                let result = black_box(cedarling.authorize(cloned_request).await);
                assert!(result.is_ok(), "succesful authz: {:?}", result);
            });
        },
    );
}

fn with_jwt_validation_benchmark_rs256(c: &mut Criterion) {
    let mut mock_server = Server::new();
    let runtime = Runtime::new().expect("init tokio runtime");

    let request = prepare_request(generate_token::Algorithm::RS256, &mut mock_server)
        .expect("should prepare request");

    let cedarling = runtime
        .block_on(prepare_cedarling_config(
            true,
            &[Algorithm::RS256],
            &mock_server.url(),
        ))
        .expect("should initialize Cedarling");

    c.bench_with_input(
        BenchmarkId::new("authz_with_jwt_validation_rs256", "tokio runtime"),
        &runtime,
        |b, rt| {
            b.to_async(rt).iter(|| async {
                let cloned_request = request.clone();
                let result = black_box(cedarling.authorize(cloned_request).await);
                assert!(result.is_ok(), "succesful authz: {:?}", result);
            });
        },
    );
}

fn with_jwt_validation_benchmark_es256(c: &mut Criterion) {
    let mut mock_server = Server::new();
    let runtime = Runtime::new().expect("init tokio runtime");

    let request = prepare_request(generate_token::Algorithm::ES256, &mut mock_server)
        .expect("should prepare request");

    let cedarling = runtime
        .block_on(prepare_cedarling_config(
            true,
            &[Algorithm::ES256],
            &mock_server.url(),
        ))
        .expect("should initialize Cedarling");

    c.bench_with_input(
        BenchmarkId::new("authz_with_jwt_validation_es256", "tokio runtime"),
        &runtime,
        |b, rt| {
            b.to_async(rt).iter(|| async {
                let cloned_request = request.clone();
                let result = black_box(cedarling.authorize(cloned_request).await);
                assert!(result.is_ok(), "succesful authz: {:?}", result);
            });
        },
    );
}

fn with_jwt_validation_benchmark_eddsa(c: &mut Criterion) {
    let mut mock_server = Server::new();
    let runtime = Runtime::new().expect("init tokio runtime");

    let request = prepare_request(generate_token::Algorithm::EdDSA, &mut mock_server)
        .expect("should prepare request");

    let cedarling = runtime
        .block_on(prepare_cedarling_config(
            true,
            &[Algorithm::EdDSA],
            &mock_server.url(),
        ))
        .expect("should initialize Cedarling");

    c.bench_with_input(
        BenchmarkId::new("authz_with_jwt_validation_eddsa", "tokio runtime"),
        &runtime,
        |b, rt| {
            b.to_async(rt).iter(|| async {
                let cloned_request = request.clone();
                let result = black_box(cedarling.authorize(cloned_request).await);
                assert!(result.is_ok(), "succesful authz: {:?}", result);
            });
        },
    );
}

criterion_group!(
    authz_benchmark,
    without_jwt_validation_benchmark,
    with_jwt_validation_benchmark_hs256,
    with_jwt_validation_benchmark_rs256,
    with_jwt_validation_benchmark_es256,
    with_jwt_validation_benchmark_eddsa,
);
criterion_main!(authz_benchmark);

async fn prepare_cedarling_config(
    sig_validation: bool,
    algs_supported: &[Algorithm],
    server_url: &str,
) -> Result<Cedarling, Box<dyn Error>> {
    // Edit JWT validation configs
    let mut jwt_config = JwtConfig::new_without_validation();
    jwt_config.jwt_sig_validation = sig_validation;
    jwt_config.signature_algorithms_supported = algs_supported.iter().copied().collect();

    // Point the policy store openid config endpoint to the mock server
    let mut policy_store_src =
        serde_yml::from_str::<serde_yml::Value>(POLICY_STORE).expect("parse policy store YAML");
    policy_store_src["policy_stores"]["a1bf93115de86de760ee0bea1d529b521489e5a11747"]["trusted_issuers"]
        ["Jans123123"]["openid_configuration_endpoint"] =
        format!("{server_url}/.well-known/openid-configuration").into();
    let policy_store_src = serde_yml::to_string(&policy_store_src)
        .expect("convert edited yaml policy store to string");

    let bootstrap_config = BootstrapConfig {
        application_name: "test_app".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::Off,
            log_level: LogLevel::DEBUG,
        },
        policy_store_config: PolicyStoreConfig {
            source: cedarling::PolicyStoreSource::Yaml(policy_store_src),
        },
        jwt_config,
        authorization_config: AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: true,
            principal_bool_operator: JsonRule::default(),
            id_token_trust_mode: IdTokenTrustMode::None,
            ..Default::default()
        },
        entity_builder_config: EntityBuilderConfig::default().with_workload().with_user(),
    };

    let cedarling = Cedarling::new(&bootstrap_config).await?;

    Ok(cedarling)
}

fn prepare_request(
    jwt_algorithm: generate_token::Algorithm,
    mock_server: &mut ServerGuard,
) -> Result<Request, Box<dyn Error>> {
    let jwt_iss = mock_server.url();

    let access_token = test_utils::jwt!(
        jwt_algorithm,
        {
            "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
            "code": "bf1934f6-3905-420a-8299-6b2e3ffddd6e",
            "iss": jwt_iss,
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
            "auth_time": 1724830746,
            "exp": 1724945978,
            "iat": 1724832259,
            "jti": "lxTmCVRFTxOjJgvEEpozMQ",
            "name": "Default Admin User",
            "status": {
              "status_list": {
                "idx": 201,
                "uri": "https://admin-ui-test.gluu.org/jans-auth/restv1/status_list"
                }
            }
        }
    )?;

    let id_token = test_utils::jwt!(
        jwt_algorithm,
        {
            "acr": "basic",
            "amr": "10",
            "aud": "5b4487c4-8db1-409d-a653-f907b8094039",
            "exp": 1724835859,
            "iat": 1724832259,
            "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
            "iss": jwt_iss,
            "jti": "sk3T40NYSYuk5saHZNpkZw",
            "nonce": "c3872af9-a0f5-4c3f-a1af-f9d0e8846e81",
            "sid": "6a7fe50a-d810-454d-be5d-549d29595a09",
            "jansOpenIDConnectVersion": "openidconnect-1.0",
            "c_hash": "pGoK6Y_RKcWHkUecM9uw6Q",
            "auth_time": 1724830746,
            "grant": "authorization_code",
            "status": {
              "status_list": {
                "idx": 202,
                "uri": "https://admin-ui-test.gluu.org/jans-auth/restv1/status_list"
                }
            },
            "role":"Admin"
        }
    )?;

    let userinfo_token = test_utils::jwt!(
        jwt_algorithm,
        {
            "country": "US",
            "email": "user@example.com",
            "username": "UserNameExample",
            "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
            "iss": jwt_iss,
            "given_name": "Admin",
            "middle_name": "Admin",
            "inum": "8d1cde6a-1447-4766-b3c8-16663e13b458",
            "client_id": "5b4487c4-8db1-409d-a653-f907b8094039",
            "aud": "5b4487c4-8db1-409d-a653-f907b8094039",
            "updated_at": 1724778591,
            "name": "Default Admin User",
            "nickname": "Admin",
            "family_name": "User",
            "jti": "faiYvaYIT0cDAT7Fow0pQw",
            "jansAdminUIRole": [
                "api-admin"
            ],
            "exp": 1724945978
        }
    )?;

    let jwk_set = JwkSet {
        keys: [access_token.jwk, id_token.jwk, userinfo_token.jwk]
            .iter()
            // transfromation from `josekit::jwk:Jwk` to `jsonwebtoken::jwk::Jwk`
            .map(|k| serde_json::to_value(k).unwrap())
            .map(|k| serde_json::from_value::<jsonwebtoken::jwk::Jwk>(k).unwrap())
            .collect(),
    };

    // Setup OpenId config endpoint
    let oidc = json!({
        "issuer": mock_server.url(),
        "jwks_uri": &format!("{}/jwks", mock_server.url()),
    });
    let _oidc_endpoint = mock_server
        .mock("GET", "/.well-known/openid-configuration")
        .with_status(200)
        .with_header("content-type", "application/json")
        .with_body(oidc.to_string())
        .expect_at_least(1)
        .create();

    // Setup JWKS endpoint
    let jwk_set = serde_json::to_string(&jwk_set).expect("serialize jwk set");
    let _jwks_endpoint = mock_server
        .mock("GET", "/jwks")
        .with_status(200)
        .with_header("content-type", "application/json")
        .with_body(jwk_set)
        .expect_at_least(1)
        .create();

    let request = Request::deserialize(serde_json::json!(
        {
            "tokens": {
                "access_token": access_token.token,
                "id_token": id_token.token,
                "userinfo_token":userinfo_token.token,
            },
            "action": "Jans::Action::\"Update\"",
            "resource": {
                "id": "random_id",
                "type": "Jans::Issue",
                "org_id": "some_long_id",
                "country": "US"
            },
            "context": {},
        }
    ))?;

    Ok(request)
}
