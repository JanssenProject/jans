// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedarling::{
    AuthorizationConfig, BootstrapConfig, Cedarling, IdTokenTrustMode, JsonRule, JwtConfig,
    LogConfig, LogLevel, LogTypeConfig, PolicyStoreConfig,
};
use criterion::{BenchmarkId, Criterion, black_box, criterion_group, criterion_main};
use lazy_static::lazy_static;
use std::collections::HashMap;
use tokio::runtime::Runtime;

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

lazy_static! {
    static ref BSCONFIG_LOCAL: BootstrapConfig = BootstrapConfig {
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
}
