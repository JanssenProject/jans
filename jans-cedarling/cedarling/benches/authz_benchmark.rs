// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedarling::{
    AuthorizationConfig, BootstrapConfig, Cedarling, IdTokenTrustMode, InitCedarlingError,
    JwtConfig, LockConfig, LogConfig, LogLevel, LogTypeConfig, PolicyStoreConfig, Request,
    TokenValidationConfig, WorkloadBoolOp,
};
use criterion::{BenchmarkId, Criterion, black_box, criterion_group, criterion_main};
use jsonwebtoken::Algorithm;
use serde::Deserialize;
use serde_json::json;
use std::collections::{HashMap, HashSet};
use test_utils::token_claims::generate_token_using_claims;
use tokio::runtime::Runtime;

const POLICY_STORE: &str = include_str!("../../test_files/policy-store_ok.yaml");

fn without_jwt_validation_benchmark(c: &mut Criterion) {
    let runtime = Runtime::new().expect("init tokio runtime");

    let cedarling = runtime
        .block_on(prepare_cedarling_without_jwt_validation())
        .expect("should initialize Cedarling");

    let request = prepare_cedarling_request().expect("should prepare r:equest");

    c.bench_with_input(
        BenchmarkId::new("authz_without_jwt_validation", "tokio runtime"),
        &runtime,
        |b, rt| {
            b.to_async(rt)
                .iter(|| cedarling.authorize(black_box(request.clone())));
        },
    );
}

fn with_jwt_validation_benchmark(c: &mut Criterion) {
    let runtime = Runtime::new().expect("init tokio runtime");

    let cedarling = runtime
        .block_on(prepare_cedarling_with_jwt_validation())
        .expect("should initialize Cedarling");

    let request = prepare_cedarling_request().expect("should prepare r:equest");

    c.bench_with_input(
        BenchmarkId::new("authz_with_jwt_validation", "tokio runtime"),
        &runtime,
        |b, rt| {
            b.to_async(rt)
                .iter(|| cedarling.authorize(black_box(request.clone())));
        },
    );
}

criterion_group!(
    authz_benchmark,
    without_jwt_validation_benchmark,
    with_jwt_validation_benchmark,
);
criterion_main!(authz_benchmark);

async fn prepare_cedarling_without_jwt_validation() -> Result<Cedarling, InitCedarlingError> {
    let bootstrap_config = BootstrapConfig {
        application_name: "test_app".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::Off,
            log_level: LogLevel::DEBUG,
        },
        policy_store_config: PolicyStoreConfig {
            source: cedarling::PolicyStoreSource::Yaml(POLICY_STORE.to_string()),
        },
        jwt_config: JwtConfig::new_without_validation(),
        authorization_config: AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: true,
            user_workload_operator: WorkloadBoolOp::And,
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
        lock_config: LockConfig::default(),
    };

    Cedarling::new(&bootstrap_config).await
}

async fn prepare_cedarling_with_jwt_validation() -> Result<Cedarling, InitCedarlingError> {
    let bootstrap_config = BootstrapConfig {
        application_name: "test_app".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::Off,
            log_level: LogLevel::DEBUG,
        },
        policy_store_config: PolicyStoreConfig {
            source: cedarling::PolicyStoreSource::Yaml(POLICY_STORE.to_string()),
        },
        jwt_config: JwtConfig {
            jwks: None,
            jwt_sig_validation: false,
            jwt_status_validation: false,
            signature_algorithms_supported: HashSet::from([Algorithm::HS256]),
            token_validation_settings: HashMap::from([
                (
                    "access_token".to_string(),
                    TokenValidationConfig::access_token(),
                ),
                ("id_token".to_string(), TokenValidationConfig::id_token()),
                (
                    "userinfo_token".to_string(),
                    TokenValidationConfig::userinfo_token(),
                ),
            ]),
        },
        authorization_config: AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: true,
            user_workload_operator: WorkloadBoolOp::And,
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
        lock_config: LockConfig::default(),
    };

    Cedarling::new(&bootstrap_config).await
}

pub fn prepare_cedarling_request() -> Result<Request, serde_json::Error> {
    Request::deserialize(serde_json::json!(
        {
            "tokens": {
                "access_token": generate_token_using_claims(json!({
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
                })),
                "id_token": generate_token_using_claims(json!({
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
                })),
                "userinfo_token":  generate_token_using_claims(json!({
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
                })),
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
    ))
}
