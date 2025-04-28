// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedarling::*;
use serde_json::json;
use std::collections::{HashMap, HashSet};
use std::time::Duration;
use tokio::time::sleep;

static POLICY_STORE_RAW: &str = include_str!("../../test_files/policy-store_ok.yaml");

const SSA_JWT: &str = "eyJraWQiOiJzc2FfZGNkYWRjZGUtNDJkZi00YzA4LTg0NjktMTRjZTg0Y2JmZTczX3NpZ19yczI1NiIsInR5cCI6IkpXVCIsImFsZyI6IlJTMjU2In0.eyJzb2Z0d2FyZV9pZCI6Im15X2NlZGFybGluZ19hcHAiLCJncmFudF90eXBlcyI6WyJjbGllbnRfY3JlZGVudGlhbHMiXSwib3JnX2lkIjoibXlfb3JnIiwiaXNzIjoiaHR0cHM6Ly9kZW1vZXhhbXBsZS5qYW5zLmlvIiwic29mdHdhcmVfcm9sZXMiOlsiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2xvY2svbG9nLndyaXRlIl0sImV4cCI6MzMyMjQ5NTc2MCwiaWF0IjoxNzQ1Njk1NzYwLCJqdGkiOiI3Mzg4OWI2ZC02ZmVmLTRlNDUtODg5NC1lMmRkMWM1ZTk3MDIifQ.Ta4aEagJDhgL4KS5Uv8GZaV9NGgp8wG4BhcXW1p6cm48dAvyNgSyx_Dscs2kwSxtNLwYoNnu56gvIX3RYoeDKfp1StRXo1sBfmqXYQe0zrl7vvqBM5ik9OkSd9uDc6PsCFqAbu_o-ijHJDlQmh0ZLiKbIgwrlSdV1vkrg66ez5O97Iia3iLTicpA0pomEl9nJu-nLMUS-SymAK0A0Rt456IfQB_QBhrWxu1Ld9Pf2BylKpNF9H8i9bQ4GVKk0_6y0b2sVboo_CEVIGt9jWZYkG7YSIJjixxy6qKtwjz3Cr2l08rFCJR2ROG_X4AQfji4-ukLfUhPfXKm79xIzTa0eA";

#[tokio::main(flavor = "current_thread")]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // This configuration is specifically for the lock server interaction
    let lock_config = LockServiceConfig {
        log_level: LogLevel::TRACE,
        config_uri: "https://demoexample.jans.io/.well-known/lock-server-configuration"
            .parse()
            .unwrap(),
        dynamic_config: false,
        ssa_jwt: Some(SSA_JWT.to_string()),
        log_interval: Some(Duration::from_secs(3)), // send logs every 3 secs
        health_interval: None,                      // don't send healthchecks
        telemetry_interval: None,                   // don't send telemetry
        listen_sse: false,
        accept_invalid_certs: true,
    };

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
            id_token_trust_mode: IdTokenTrustMode::None,
            principal_bool_operator: JsonRule::new(serde_json::json!(
                {"===": [{"var": "Jans::User"}, "ALLOW"]}
            ))
            .unwrap(),
        },
        entity_builder_config: EntityBuilderConfig::default().with_user().with_workload(),
        lock_config: Some(lock_config),
    })
    .await?;

    let principals = vec![EntityData {
        entity_type: "Jans::User".to_string(),
        id: "some_user".to_string(),
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
                id: "random_id".to_string(),
                entity_type: "Jans::Issue".to_string(),
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

    println!("sleeping for 10 secs to give time for Cedarling's Lock service to send the logs");
    // we sleep for a bit so we don't exit before any logs are sent
    sleep(Duration::from_secs(10)).await;
    println!("logs should be sent to the lock server by now!");

    Ok(())
}
