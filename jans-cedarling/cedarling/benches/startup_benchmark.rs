// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::{hint::black_box, sync::LazyLock};

use criterion::{BenchmarkId, Criterion, criterion_group, criterion_main};
use tokio::runtime::Runtime;

use cedarling::{
    AuthorizationConfig, BootstrapConfig, Cedarling, DataStoreConfig, EntityBuilderConfig,
    IdTokenTrustMode, JsonRule, JwtConfig, LogConfig, LogLevel, LogTypeConfig, PolicyStoreConfig,
    PolicyStoreSource,
};

const POLICY_STORE: &str = include_str!("../../test_files/policy-store_ok.yaml");

fn local_policy_store_benchmark(c: &mut Criterion) {
    let runtime = Runtime::new().expect("init tokio runtime");

    c.bench_with_input(
        BenchmarkId::new("cedarling_startup", "tokio runtime"),
        &runtime,
        |b, rt| {
            b.to_async(rt)
                .iter(|| Cedarling::new(black_box(&BSCONFIG_LOCAL)));
        },
    );
}

criterion_group!(cedarling_startup_benchmark, local_policy_store_benchmark,);
criterion_main!(cedarling_startup_benchmark);

static BSCONFIG_LOCAL: LazyLock<BootstrapConfig> = LazyLock::new(|| BootstrapConfig {
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
    entity_builder_config: EntityBuilderConfig::default().with_user().with_workload(),
    lock_config: None,
    max_base64_size: None,
    max_default_entities: None,
    data_store_config: DataStoreConfig::default(),
});
