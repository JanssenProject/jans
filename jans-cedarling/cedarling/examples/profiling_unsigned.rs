// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

#![allow(unused_imports)]
#![allow(dead_code)]

use cedarling::{
    AuthorizationConfig, BootstrapConfig, Cedarling, DataStoreConfig, EntityBuilderConfig,
    EntityData, JsonRule, JwtConfig, LogConfig, LogLevel, LogTypeConfig, PolicyStoreConfig,
    PolicyStoreSource, RequestUnsigned,
};
use serde::Deserialize;
use serde_json::json;
use std::fs::File;

// Inline policy store with realistic multi-condition policies for unsigned authorization.
// Uses attribute comparisons, role hierarchy, and forbid rules to simulate real workloads.
const POLICY_STORE_RAW: &str = r#"
cedar_version: v4.0.0
policy_stores:
  profiling_store:
    cedar_version: v4.0.0
    name: "Profiling"
    description: "Policy store for authorize_unsigned profiling"
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

#[cfg(not(any(target_arch = "wasm32", target_os = "windows")))]
#[tokio::main(flavor = "current_thread")]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let cedarling = init_cedarling().await;
    let request = prepare_unsigned_request();

    // Validate that the authorization request executes correctly before profiling
    let validation_result = cedarling
        .authorize_unsigned(request.clone())
        .await
        .expect("authorization should succeed");
    assert!(
        validation_result.decision,
        "authorization should return Allow decision"
    );

    // init profiler guard
    let guard = pprof::ProfilerGuardBuilder::default()
        .frequency(1000)
        .blocklist(&["libc", "libgcc", "pthread", "vdso"])
        .build()
        .unwrap();

    for _ in 0..1000 {
        let _ = cedarling.authorize_unsigned(request.clone()).await;
    }

    if let Ok(report) = guard.report().build() {
        println!("report: {:?}", &report);

        // write output flamegraph to an SVG file
        let file = File::create(format!(
            "{}/../{}",
            env!("CARGO_MANIFEST_DIR"),
            "cedarling_profiling_flamegraph.svg",
        ))
        .unwrap();
        let mut options = pprof::flamegraph::Options::default();
        options.image_width = Some(3000);
        report.flamegraph_with_options(file, &mut options).unwrap();
    }

    Ok(())
}

async fn init_cedarling() -> Cedarling {
    Cedarling::new(&BootstrapConfig {
        application_name: "test_app".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::Off,
            log_level: LogLevel::INFO,
        },
        policy_store_config: PolicyStoreConfig {
            source: PolicyStoreSource::Yaml(POLICY_STORE_RAW.to_string()),
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
        max_default_entities: None,
        max_base64_size: None,
        data_store_config: DataStoreConfig::default(),
    })
    .await
    .expect("should initialize cedarling")
}

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

/// just define a main function to satisfy the compiler.
#[cfg(any(target_arch = "wasm32", target_os = "windows"))]
#[tokio::main(flavor = "current_thread")]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    unimplemented!("Profiling is not supported on wasm32 or windows.")
}
