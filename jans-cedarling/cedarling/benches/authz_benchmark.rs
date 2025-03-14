// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedarling::*;
use criterion::{BenchmarkId, Criterion, black_box, criterion_group, criterion_main};
use jsonwebtoken::Algorithm;
use jsonwebtoken::jwk::JwkSet;
use serde::Deserialize;
use std::collections::HashMap;
use std::error::Error;
use test_utils::generate_token;
use tokio::runtime::Runtime;

const POLICY_STORE: &str = include_str!("../../test_files/policy-store_ok.yaml");

fn without_jwt_validation_benchmark(c: &mut Criterion) {
    let runtime = Runtime::new().expect("init tokio runtime");

    let (request, _jwk_set) = prepare_cedarling_request(generate_token::Algorithm::HS256)
        .expect("should prepare r:equest");

    let cedarling = runtime
        .block_on(prepare_cedarling_config(false, &[Algorithm::HS256], None))
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
    let runtime = Runtime::new().expect("init tokio runtime");

    let (request, jwk_set) = prepare_cedarling_request(generate_token::Algorithm::HS256)
        .expect("should prepare request");

    let cedarling = runtime
        .block_on(prepare_cedarling_config(
            true,
            &[Algorithm::HS256],
            Some(jwk_set),
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
    let runtime = Runtime::new().expect("init tokio runtime");

    let (request, jwk_set) = prepare_cedarling_request(generate_token::Algorithm::RS256)
        .expect("should prepare request");

    let cedarling = runtime
        .block_on(prepare_cedarling_config(
            true,
            &[Algorithm::RS256],
            Some(jwk_set),
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
    let runtime = Runtime::new().expect("init tokio runtime");

    let (request, jwk_set) = prepare_cedarling_request(generate_token::Algorithm::ES256)
        .expect("should prepare request");

    let cedarling = runtime
        .block_on(prepare_cedarling_config(
            true,
            &[Algorithm::ES256],
            Some(jwk_set),
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
    let runtime = Runtime::new().expect("init tokio runtime");

    let (request, jwk_set) = prepare_cedarling_request(generate_token::Algorithm::EdDSA)
        .expect("should prepare request");

    let cedarling = runtime
        .block_on(prepare_cedarling_config(
            true,
            &[Algorithm::EdDSA],
            Some(jwk_set),
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
    jwk_set: Option<JwkSet>,
) -> Result<Cedarling, Box<dyn Error>> {
    // Edit JWT validation configs
    let mut jwt_config = JwtConfig::new_without_validation();
    jwt_config.jwt_sig_validation = sig_validation;
    jwt_config.signature_algorithms_supported = algs_supported.iter().copied().collect();
    jwt_config.jwks = jwk_set
        .map(|jwk_set| serde_json::to_string(&jwk_set))
        .transpose()
        .map_err(|e| format!("failed to parse jwk set: {e}"))?;

    let bootstrap_config = BootstrapConfig {
        application_name: "test_app".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::Off,
            log_level: LogLevel::DEBUG,
        },
        policy_store_config: PolicyStoreConfig {
            source: cedarling::PolicyStoreSource::Yaml(POLICY_STORE.to_string()),
        },
        jwt_config,
        authorization_config: AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: true,
            principal_bool_operator: JsonRule::default(),
            mapping_user: Some("Jans::User".to_string()),
            mapping_workload: Some("Jans::Workload".to_string()),
            mapping_role: Some("Jans::Role".to_string()),
            mapping_tokens: HashMap::from([
                ("access_token".to_string(), "Jans::Access_token".to_string()),
                ("id_token".to_string(), "Jans::id_token".to_string()),
                (
                    "userinfo_token".to_string(),
                    "Jans::Userinfo_token".to_string(),
                ),
            ])
            .into(),
            id_token_trust_mode: IdTokenTrustMode::None,
            ..Default::default()
        },
    };

    let cedarling = Cedarling::new(&bootstrap_config).await?;

    Ok(cedarling)
}

pub fn prepare_cedarling_request(
    jwt_algorithm: generate_token::Algorithm,
) -> Result<(Request, JwkSet), Box<dyn Error>> {
    let access_token = test_utils::jwt!(
        jwt_algorithm,
        {
            "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
            "code": "bf1934f6-3905-420a-8299-6b2e3ffddd6e",
            "iss": "https://admin-ui-test.gluu.org",
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
            "iss": "https://admin-ui-test.gluu.org",
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
            "iss": "https://admin-ui-test.gluu.org",
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

    let jwks = JwkSet {
        keys: [access_token.jwk, id_token.jwk, userinfo_token.jwk]
            .iter()
            // transfromation from `josekit::jwk:Jwk` to `jsonwebtoken::jwk::Jwk`
            .map(|k| serde_json::to_value(k).unwrap())
            .map(|k| serde_json::from_value::<jsonwebtoken::jwk::Jwk>(k).unwrap())
            .collect(),
    };

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

    Ok((request, jwks))
}
