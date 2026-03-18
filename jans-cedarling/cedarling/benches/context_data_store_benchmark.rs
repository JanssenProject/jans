// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Benchmarks for `DataStore` operations.
//!
//! Tests performance of:
//! - Push operations
//! - Get operations
//! - Mixed read/write workloads
//! - Authorization with pushed data

use std::hint::black_box;
use std::sync::LazyLock;
use std::time::Duration;

use cedarling::{
    AuthorizationConfig, BootstrapConfig, Cedarling, DataApi, DataStoreConfig, EntityBuilderConfig,
    EntityData, IdTokenTrustMode, JsonRule, JwtConfig, LogConfig, LogLevel, LogTypeConfig,
    PolicyStoreConfig, PolicyStoreSource, RequestUnsigned,
};
use criterion::{BenchmarkId, Criterion, Throughput, criterion_group, criterion_main};
use serde::Deserialize;
use serde_json::json;
use tokio::runtime::Runtime;

const POLICY_STORE: &str = include_str!("../../test_files/policy-store_ok.yaml");

// Policy store with data-aware policies for authorization benchmarks
const POLICY_STORE_WITH_DATA: &str = r#"
cedar_version: v4.0.0
policy_stores:
  data_bench_store:
    cedar_version: v4.0.0
    name: "Data Benchmark Store"
    description: "Benchmark policy store for data-aware policies"
    policies:
      allow_with_data:
        description: "Allow access when data.enabled is true"
        policy_content:
          encoding: none
          content_type: cedar
          body: |-
            permit(
              principal is Jans::TestPrincipal,
              action == Jans::Action::"DataAccess",
              resource is Jans::Resource
            ) when {
              context has data && context.data has enabled && context.data.enabled == true
            };
      allow_basic:
        description: "Always allow basic access"
        policy_content:
          encoding: none
          content_type: cedar
          body: |-
            permit(
              principal is Jans::TestPrincipal,
              action == Jans::Action::"BasicAccess",
              resource is Jans::Resource
            );
    schema:
      encoding: none
      content_type: cedar
      body: |-
        namespace Jans {
          entity TestPrincipal;
          entity Resource;
          type DataContext = {
            "enabled"?: Bool
          };
          type Context = {
            "data"?: DataContext
          };
          action "DataAccess" appliesTo {
            principal: [TestPrincipal],
            resource: [Resource],
            context: Context
          };
          action "BasicAccess" appliesTo {
            principal: [TestPrincipal],
            resource: [Resource],
            context: Context
          };
        }
"#;

static BSCONFIG: LazyLock<BootstrapConfig> = LazyLock::new(|| BootstrapConfig {
    application_name: "bench_app".to_string(),
    log_config: LogConfig {
        log_type: LogTypeConfig::Off,
        log_level: LogLevel::WARN,
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
    entity_builder_config: EntityBuilderConfig::default().with_user().with_workload(),
    lock_config: None,
    max_base64_size: None,
    max_default_entities: None,
    data_store_config: DataStoreConfig::default(),
});

// Custom principal operator for TestPrincipal
static TEST_PRINCIPAL_OPERATOR: LazyLock<JsonRule> = LazyLock::new(|| {
    JsonRule::new(json!({
        "===": [{"var": "Jans::TestPrincipal"}, "ALLOW"]
    }))
    .unwrap()
});

static BSCONFIG_WITH_DATA_POLICY: LazyLock<BootstrapConfig> = LazyLock::new(|| BootstrapConfig {
    application_name: "bench_app".to_string(),
    log_config: LogConfig {
        log_type: LogTypeConfig::Off,
        log_level: LogLevel::WARN,
    },
    policy_store_config: PolicyStoreConfig {
        source: PolicyStoreSource::Yaml(POLICY_STORE_WITH_DATA.to_string()),
    },
    jwt_config: JwtConfig::new_without_validation(),
    authorization_config: AuthorizationConfig {
        use_user_principal: false,
        use_workload_principal: false,
        principal_bool_operator: TEST_PRINCIPAL_OPERATOR.clone(),
        id_token_trust_mode: IdTokenTrustMode::Never,
        ..Default::default()
    },
    entity_builder_config: EntityBuilderConfig::default()
        .with_no_user()
        .with_no_workload(),
    lock_config: None,
    max_base64_size: None,
    max_default_entities: None,
    data_store_config: DataStoreConfig::default(),
});

// =============================================================================
// DataStore CRUD Benchmarks
// =============================================================================

fn bench_push_data_ctx(c: &mut Criterion) {
    let runtime = Runtime::new().expect("init tokio runtime");

    let cedarling = runtime
        .block_on(Cedarling::new(&BSCONFIG))
        .expect("init cedarling");

    let mut group = c.benchmark_group("data_store_push");

    // Benchmark push with small value
    group.bench_function("small_value", |b| {
        let mut counter = 0u64;
        b.iter(|| {
            // Use modulo to reuse keys and prevent storage limit exhaustion
            let key = format!("key_{}", counter % 1000);
            counter += 1;
            cedarling
                .push_data_ctx(
                    black_box(&key),
                    black_box(json!("small_value")),
                    black_box(Some(Duration::from_secs(60))),
                )
                .expect("push should succeed");
        });
    });

    // Clean up
    cedarling.clear_data_ctx().expect("clear should succeed");

    // Benchmark push with medium value (nested JSON)
    group.bench_function("medium_value", |b| {
        let mut counter = 0u64;
        let medium_value = json!({
            "user": {"id": "user_123", "name": "Test User", "roles": ["admin", "user"]},
            "settings": {"theme": "dark", "notifications": true, "language": "en"}
        });
        b.iter(|| {
            // Use modulo to reuse keys and prevent storage limit exhaustion
            let key = format!("key_{}", counter % 1000);
            counter += 1;
            cedarling
                .push_data_ctx(
                    black_box(&key),
                    black_box(medium_value.clone()),
                    black_box(Some(Duration::from_secs(60))),
                )
                .expect("push should succeed");
        });
    });

    // Clean up
    cedarling.clear_data_ctx().expect("clear should succeed");

    // Benchmark push with large value (array)
    group.bench_function("large_value", |b| {
        let mut counter = 0u64;
        let large_value: serde_json::Value = (0..100)
            .map(|i| json!({"id": i, "data": "x".repeat(50)}))
            .collect();
        b.iter(|| {
            // Use modulo to reuse keys and prevent storage limit exhaustion
            let key = format!("key_{}", counter % 1000);
            counter += 1;
            cedarling
                .push_data_ctx(
                    black_box(&key),
                    black_box(large_value.clone()),
                    black_box(Some(Duration::from_secs(60))),
                )
                .expect("push should succeed");
        });
    });

    group.finish();
}

fn bench_get_data_ctx(c: &mut Criterion) {
    let runtime = Runtime::new().expect("init tokio runtime");

    let cedarling = runtime
        .block_on(Cedarling::new(&BSCONFIG))
        .expect("init cedarling");

    // Pre-populate with data
    for i in 0..1000 {
        cedarling
            .push_data_ctx(
                &format!("key_{i}"),
                json!({"index": i, "data": "test_data"}),
                Some(Duration::from_secs(300)),
            )
            .expect("push should succeed");
    }

    let mut group = c.benchmark_group("data_store_get");
    group.throughput(Throughput::Elements(1));

    // Benchmark get existing key
    group.bench_function("existing_key", |b| {
        let mut counter = 0u64;
        b.iter(|| {
            let key = format!("key_{}", counter % 1000);
            counter += 1;
            black_box(
                cedarling
                    .get_data_ctx(black_box(&key))
                    .expect("get should succeed"),
            )
        });
    });

    // Benchmark get non-existing key
    group.bench_function("missing_key", |b| {
        b.iter(|| {
            black_box(
                cedarling
                    .get_data_ctx(black_box("nonexistent_key"))
                    .expect("get should succeed"),
            )
        });
    });

    group.finish();
}

fn bench_mixed_workload(c: &mut Criterion) {
    let runtime = Runtime::new().expect("init tokio runtime");

    let cedarling = runtime
        .block_on(Cedarling::new(&BSCONFIG))
        .expect("init cedarling");

    // Pre-populate
    for i in 0..100 {
        cedarling
            .push_data_ctx(
                &format!("key_{i}"),
                json!({"index": i}),
                Some(Duration::from_secs(300)),
            )
            .expect("push should succeed");
    }

    let mut group = c.benchmark_group("data_store_mixed");

    // 80% read, 20% write workload
    group.bench_function("80_read_20_write", |b| {
        let mut counter = 0u64;
        b.iter(|| {
            if counter.is_multiple_of(5) {
                // Write (20%) - Use modulo to reuse keys and prevent storage limit exhaustion
                let key = format!("new_key_{}", counter % 1000);
                cedarling
                    .push_data_ctx(
                        black_box(&key),
                        black_box(json!({"counter": counter})),
                        black_box(Some(Duration::from_secs(60))),
                    )
                    .expect("push should succeed");
            } else {
                // Read (80%)
                let key = format!("key_{}", counter % 100);
                black_box(
                    cedarling
                        .get_data_ctx(black_box(&key))
                        .expect("get should succeed"),
                );
            }
            counter += 1;
        });
    });

    group.finish();
}

// =============================================================================
// Authorization with Data Benchmarks
// =============================================================================

fn bench_authorization_with_data(c: &mut Criterion) {
    let runtime = Runtime::new().expect("init tokio runtime");

    let cedarling = runtime
        .block_on(Cedarling::new(&BSCONFIG_WITH_DATA_POLICY))
        .expect("init cedarling");

    // Push data that enables the policy
    cedarling
        .push_data_ctx("enabled", json!(true), Some(Duration::from_secs(300)))
        .expect("push should succeed");

    let request = RequestUnsigned {
        action: "Jans::Action::\"DataAccess\"".to_string(),
        context: json!({}),
        principals: vec![
            EntityData::deserialize(json!({
                "cedar_entity_mapping": {
                    "entity_type": "Jans::TestPrincipal",
                    "id": "test_user"
                }
            }))
            .unwrap(),
        ],
        resource: EntityData::deserialize(json!({
            "cedar_entity_mapping": {
                "entity_type": "Jans::Resource",
                "id": "resource_1"
            }
        }))
        .unwrap(),
    };

    // Validate that the authorization request executes correctly before benchmarking
    let validation_result = runtime
        .block_on(cedarling.authorize_unsigned(request.clone()))
        .expect("authorization validation should succeed");
    assert!(
        validation_result.decision,
        "authorization validation should return Allow decision"
    );

    let mut group = c.benchmark_group("authorization_with_data");

    group.bench_with_input(
        BenchmarkId::new("authorize_unsigned", "with_pushed_data"),
        &runtime,
        |b, rt| {
            b.to_async(rt).iter_batched(
                || request.clone(),
                |cloned_request| async {
                    black_box(
                        cedarling
                            .authorize_unsigned(black_box(cloned_request))
                            .await
                            .expect("authorization should succeed"),
                    )
                },
                criterion::BatchSize::SmallInput,
            );
        },
    );

    group.finish();
}

fn bench_authorization_varying_data_size(c: &mut Criterion) {
    let runtime = Runtime::new().expect("init tokio runtime");

    let mut group = c.benchmark_group("authorization_data_size");

    // Test authorization comparing with/without pushed data
    // Note: Cedar schema validates context.data, so we can only test with
    // the schema-defined "enabled" key. We test with varying numbers of
    // authorization calls to measure throughput.
    // Reduced call counts to stay within 1ms threshold per operation
    for call_count in [1, 5] {
        let cedarling = runtime
            .block_on(Cedarling::new(&BSCONFIG_WITH_DATA_POLICY))
            .expect("init cedarling");

        // Push the schema-defined "enabled" key
        cedarling
            .push_data_ctx("enabled", json!(true), Some(Duration::from_secs(300)))
            .expect("push should succeed");

        let request = RequestUnsigned {
            action: "Jans::Action::\"DataAccess\"".to_string(),
            context: json!({}),
            principals: vec![
                EntityData::deserialize(json!({
                    "cedar_entity_mapping": {
                        "entity_type": "Jans::TestPrincipal",
                        "id": "test_user"
                    }
                }))
                .unwrap(),
            ],
            resource: EntityData::deserialize(json!({
                "cedar_entity_mapping": {
                    "entity_type": "Jans::Resource",
                    "id": "resource_1"
                }
            }))
            .unwrap(),
        };

        // Validate that the authorization request executes correctly before benchmarking
        let validation_result = runtime
            .block_on(cedarling.authorize_unsigned(request.clone()))
            .expect("authorization validation should succeed");
        assert!(
            validation_result.decision,
            "authorization validation should return Allow decision"
        );

        group.throughput(Throughput::Elements(u64::try_from(call_count).unwrap_or(0)));
        group.bench_with_input(BenchmarkId::new("calls", call_count), &runtime, |b, rt| {
            b.to_async(rt).iter(|| async {
                for _ in 0..call_count {
                    black_box(
                        cedarling
                            .authorize_unsigned(black_box(request.clone()))
                            .await
                            .expect("authorization should succeed"),
                    );
                }
            });
        });
    }

    group.finish();
}

criterion_group!(
    data_store_benchmarks,
    bench_push_data_ctx,
    bench_get_data_ctx,
    bench_mixed_workload,
    bench_authorization_with_data,
    bench_authorization_varying_data_size,
);
criterion_main!(data_store_benchmarks);
