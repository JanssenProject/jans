// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedarling::{
    AuthorizationConfig, BootstrapConfig, Cedarling, DataStoreConfig, EntityBuilderConfig,
    EntityData, InitCedarlingError, JsonRule, JwtConfig, LogConfig, LogLevel, LogTypeConfig,
    PolicyStoreConfig, PolicyStoreSource, RequestUnsigned,
};
use criterion::{BenchmarkId, Criterion, criterion_group, criterion_main};
use serde::Deserialize;
use serde_json::json;
use std::hint::black_box;
use std::time::Duration;
use tokio::runtime::Runtime;

// Inline policy store with realistic multi-condition policies for unsigned authorization.
// Uses attribute comparisons, set membership, and string equality to simulate real workloads.
const POLICY_STORE: &str = r#"
cedar_version: v4.0.0
policy_stores:
  bench_store:
    cedar_version: v4.0.0
    name: "Benchmark"
    description: "Policy store for authorize_unsigned benchmarks"
    policies:
      allow_user_update:
        description: "Allow update when country matches and role is Admin"
        policy_content:
          encoding: none
          content_type: cedar
          body: |-
            permit(
                principal is Jans::User,
                action in [Jans::Action::"Update"],
                resource is Jans::Issue
            ) when {
                principal.country == resource.country &&
                principal in Jans::Role::"Admin" &&
                principal.department == "engineering"
            };
      allow_user_read:
        description: "Allow read for any authenticated user"
        policy_content:
          encoding: none
          content_type: cedar
          body: |-
            permit(
                principal is Jans::User,
                action in [Jans::Action::"Read"],
                resource is Jans::Issue
            ) when {
                principal.active == true
            };
      deny_suspended:
        description: "Deny if user is suspended"
        policy_content:
          encoding: none
          content_type: cedar
          body: |-
            forbid(
                principal is Jans::User,
                action,
                resource
            ) when {
                principal.suspended == true
            };
    schema:
      encoding: none
      content_type: cedar
      body: |-
        namespace Jans {
          entity Issue = {
            "country": String,
            "org_id": String,
            "priority": Long,
          };
          entity Role;
          entity User in [Role] = {
            "country": String,
            "department": String,
            "active": Bool,
            "suspended": Bool,
          };
          action "Update" appliesTo {
            principal: [User, Role],
            resource: [Issue],
            context: {}
          };
          action "Read" appliesTo {
            principal: [User, Role],
            resource: [Issue],
            context: {}
          };
        }
"#;

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
        },
        jwt_config: JwtConfig::new_without_validation(),
        authorization_config: AuthorizationConfig {
            principal_bool_operator: JsonRule::new(json!(
                {"===": [{"var": "Jans::User"}, "ALLOW"]}
            ))
            .expect("valid rule"),
            ..Default::default()
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
    let principal = EntityData::deserialize(json!({
        "cedar_entity_mapping": {
            "entity_type": "Jans::User",
            "id": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0"
        },
        "country": "US",
        "role": ["Admin"],
        "department": "engineering",
        "active": true,
        "suspended": false
    }))
    .expect("valid principal entity");

    let resource = EntityData::deserialize(json!({
        "cedar_entity_mapping": {
            "entity_type": "Jans::Issue",
            "id": "random_id"
        },
        "org_id": "some_long_id",
        "country": "US",
        "priority": 1
    }))
    .expect("valid resource entity");

    RequestUnsigned {
        principals: vec![principal],
        action: "Jans::Action::\"Update\"".to_string(),
        context: json!({}),
        resource,
    }
}
