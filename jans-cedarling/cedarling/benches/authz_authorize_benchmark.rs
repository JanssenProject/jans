// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedarling::{
    AuthorizationConfig, BootstrapConfig, CedarEntityMapping, Cedarling, DataStoreConfig,
    EntityBuilderConfig, EntityData, InitCedarlingError, JsonRule, JwtConfig, LogConfig, LogLevel,
    LogTypeConfig, PolicyStoreConfig, PolicyStoreSource, RequestUnsigned,
};
use criterion::{BenchmarkId, Criterion, criterion_group, criterion_main};
use serde_json::json;
use std::hint::black_box;
use std::{collections::HashMap, time::Duration};
use tokio::runtime::Runtime;

const POLICY_STORE: &str = include_str!("../../test_files/policy-store_ok.yaml");

async fn validate_cedarling_works(cedarling: &Cedarling, request: &RequestUnsigned) {
    let result = cedarling
        .authorize_unsigned(request.clone())
        .await
        .expect("authorization call failed");
    assert!(result.decision, "expected allow for benchmark validation");
}

fn authorize_unsigned_benchmark(c: &mut Criterion) {
    let runtime = Runtime::new().expect("init tokio runtime");

    let cedarling = runtime
        .block_on(prepare_cedarling())
        .expect("should initialize Cedarling");

    let request = prepare_unsigned_request();

    runtime.block_on(validate_cedarling_works(&cedarling, &request));

    c.bench_with_input(
        BenchmarkId::new("authz_authorize_unsigned", "tokio runtime"),
        &runtime,
        |b, rt| {
            b.to_async(rt)
                .iter(|| cedarling.authorize_unsigned(black_box(request.clone())));
        },
    );
}

fn measurement_config() -> Criterion {
    Criterion::default()
        .measurement_time(Duration::from_secs(5))
        .warm_up_time(Duration::from_secs(5))
}

criterion_group! {
    name = authz_benchmark;
    config = measurement_config();
    targets = authorize_unsigned_benchmark
}

criterion_main!(authz_benchmark);

async fn prepare_cedarling() -> Result<Cedarling, InitCedarlingError> {
    let bootstrap_config = BootstrapConfig {
        application_name: "test_app".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::Off,
            log_level: LogLevel::DEBUG,
        },
        policy_store_config: PolicyStoreConfig {
            source: PolicyStoreSource::Yaml(POLICY_STORE.to_string()),
            validate_checksum: true,
        },
        jwt_config: JwtConfig::new_without_validation(),
        authorization_config: AuthorizationConfig {
            decision_log_default_jwt_id: "jti".to_string(),
            principal_bool_operator: JsonRule::new(json!(
                {"===": [{"var": "Jans::User"}, "ALLOW"]}
            ))
            .expect("valid rule"),
        },
        entity_builder_config: EntityBuilderConfig::default(),
        lock_config: None,
        max_base64_size: None,
        max_default_entities: None,
        data_store_config: DataStoreConfig::default(),
    };

    Cedarling::new(&bootstrap_config).await
}

#[must_use]
fn prepare_unsigned_request() -> RequestUnsigned {
    let principal = EntityData {
        cedar_mapping: CedarEntityMapping {
            entity_type: "Jans::User".to_string(),
            id: "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0".to_string(),
        },
        attributes: HashMap::from([
            (
                "sub".to_string(),
                json!("boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0"),
            ),
            ("country".to_string(), json!("US")),
            ("role".to_string(), json!("Admin")),
        ]),
    };
    let resource = EntityData {
        cedar_mapping: CedarEntityMapping {
            entity_type: "Jans::Issue".to_string(),
            id: "random_id".to_string(),
        },
        attributes: HashMap::from([
            ("org_id".to_string(), json!("some_long_id")),
            ("country".to_string(), json!("US")),
        ]),
    };
    RequestUnsigned {
        principals: vec![principal],
        action: "Jans::Action::\"Update\"".to_string(),
        context: json!({}),
        resource,
    }
}
