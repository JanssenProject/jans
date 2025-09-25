// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedarling::*;
use serde_json::json;
use std::collections::{HashMap, HashSet};

static POLICY_STORE_RAW: &str = include_str!("../../test_files/policy-store_ok.yaml");

#[tokio::main(flavor = "current_thread")]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let cedarling = Cedarling::new(&BootstrapConfig {
        application_name: "test_app".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::StdOut,
            log_level: LogLevel::INFO,
        },
        policy_store_config: PolicyStoreConfig {
            source: PolicyStoreSource::Yaml(POLICY_STORE_RAW.to_string()),
        },
        jwt_config: JwtConfig {
            jwks: None,
            jwt_sig_validation: false,
            jwt_status_validation: false,
            signature_algorithms_supported: HashSet::new(),
        }
        .allow_all_algorithms(),
        authorization_config: AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: true,

            decision_log_default_jwt_id: "jti".to_string(),
            decision_log_user_claims: vec!["client_id".to_string(), "username".to_string()],
            decision_log_workload_claims: vec!["org_id".to_string()],
            id_token_trust_mode: IdTokenTrustMode::Never,
            principal_bool_operator: JsonRule::new(serde_json::json!(
                {"===": [{"var": "Jans::User"}, "ALLOW"]}
            ))
            .unwrap(),
        },
        entity_builder_config: EntityBuilderConfig::default().with_user().with_workload(),
        lock_config: None,
        max_default_entities: None,
        max_base64_size: None,
        token_cache_max_ttl_secs: 60,
    })
    .await?;

    let principals = vec![EntityData {
        cedar_mapping: CedarEntityMapping {
            entity_type: "Jans::User".to_string(),
            id: "some_user".to_string(),
        },
        attributes: HashMap::from([
            ("sub".to_string(), json!("some_sub")),
            ("email".to_string(), json!("email@email.com")),
            ("username".to_string(), json!("some_username")),
            ("country".to_string(), json!("US")),
            ("role".to_string(), json!("SuperUser")),
        ]),
    }];

    let result = cedarling
        .authorize_unsigned(RequestUnsigned {
            principals,
            action: "Jans::Action::\"Update\"".to_string(),
            context: serde_json::json!({}),
            resource: EntityData {
                cedar_mapping: CedarEntityMapping {
                    entity_type: "Jans::Issue".to_string(),
                    id: "random_id".to_string(),
                },
                attributes: HashMap::from_iter([
                    (
                        "org_id".to_string(),
                        serde_json::Value::String("some_long_id".to_string()),
                    ),
                    (
                        "country".to_string(),
                        serde_json::Value::String("US".to_string()),
                    ),
                ]),
            },
        })
        .await;

    match result {
        Ok(result) => {
            println!("\n\nis allowed: {}", result.decision);
        },
        Err(e) => eprintln!("Error while authorizing: {}\n {:?}\n\n", e, e),
    }

    Ok(())
}
