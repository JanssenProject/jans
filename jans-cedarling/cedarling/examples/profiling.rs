// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

#![allow(unused_imports)]
#![allow(dead_code)]

use cedarling::{
    AuthorizationConfig, BootstrapConfig, CedarEntityMapping, Cedarling, DataStoreConfig,
    EntityBuilderConfig, EntityData, JsonRule, JwtConfig, LogConfig, LogLevel, LogTypeConfig,
    PolicyStoreConfig, PolicyStoreSource, RequestUnsigned,
};
use serde_json::json;
use std::collections::{HashMap, HashSet};
use std::fs::File;

#[cfg(not(any(target_arch = "wasm32", target_os = "windows")))]
#[tokio::main(flavor = "current_thread")]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let cedarling = init_cedarling().await;

    // init profiler guard
    let guard = pprof::ProfilerGuardBuilder::default()
        .frequency(1000)
        .blocklist(&["libc", "libgcc", "pthread", "vdso"])
        .build()
        .unwrap();

    for _ in 0..1000 {
        call_authorize_unsigned(&cedarling).await;
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

static POLICY_STORE_RAW: &str = include_str!("../../test_files/policy-store_ok.yaml");

async fn init_cedarling() -> Cedarling {
    Cedarling::new(&BootstrapConfig {
        application_name: "test_app".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::Off,
            log_level: LogLevel::INFO,
        },
        policy_store_config: PolicyStoreConfig {
            source: PolicyStoreSource::Yaml(POLICY_STORE_RAW.to_string()),
            validate_checksum: true,
        },
        jwt_config: JwtConfig {
            jwks: None,
            jwt_sig_validation: false,
            jwt_status_validation: false,
            signature_algorithms_supported: HashSet::new(),
            ..Default::default()
        }
        .allow_all_algorithms(),
        authorization_config: AuthorizationConfig {
            decision_log_default_jwt_id: "jti".to_string(),
            principal_bool_operator: JsonRule::new(serde_json::json!(
                {"===": [{"var": "Jans::User"}, "ALLOW"]}
            ))
            .unwrap(),
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

async fn call_authorize_unsigned(cedarling: &Cedarling) {
    let principal = EntityData {
        cedar_mapping: CedarEntityMapping {
            entity_type: "Jans::User".to_string(),
            id: "some_user".to_string(),
        },
        attributes: HashMap::from([
            ("sub".to_string(), json!("some_sub")),
            ("country".to_string(), json!("US")),
            ("role".to_string(), json!("SuperUser")),
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
    let _result = cedarling
        .authorize_unsigned(RequestUnsigned {
            principals: vec![principal],
            action: "Jans::Action::\"Update\"".to_string(),
            context: json!({}),
            resource,
        })
        .await;
}

/// just define a main function to satisfy the compiler.
#[cfg(any(target_arch = "wasm32", target_os = "windows"))]
#[tokio::main(flavor = "current_thread")]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    unimplemented!("Profiling is not supported on wasm32 or windows.")
}
