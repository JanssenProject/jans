// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

// run this example using `cargo run --example lock_integration`

use cedarling::*;
use serde_json::json;
use std::collections::{HashMap, HashSet};
use std::time::Duration;
use tokio::time::sleep;

static POLICY_STORE_RAW: &str = include_str!("../../test_files/policy-store_ok.yaml");

// NOTE: make sure you replace this with your own SSA
const SSA_JWT: &str = "eyJraWQiOiJzc2FfMmRmMGNkZDUtNTU2Yi00ZDRlLTkzNjItNjc2Mjk1NjEzMzMxX3NpZ19yczI1NiIsInR5cCI6IkpXVCIsImFsZyI6IlJTMjU2In0.eyJzb2Z0d2FyZV9pZCI6ImNlZGFybGluZyIsImdyYW50X3R5cGVzIjpbImNsaWVudF9jcmVkZW50aWFscyJdLCJvcmdfaWQiOiJteV9vcmciLCJpc3MiOiJodHRwczovL2RlbW9leGFtcGxlLmphbnMuaW8iLCJsaWZldGltZSI6MTU3Njc5OTk5OSwic29mdHdhcmVfcm9sZXMiOlsiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2xvY2svbG9nLndyaXRlIiwiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2xvY2svaGVhbHRoLndyaXRlIiwiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2xvY2svdGVsZW1ldHJ5LndyaXRlIl0sImV4cCI6MzMyMzA4NjI3NSwiaWF0IjoxNzQ2Mjg2Mjc2LCJqdGkiOiJkM2EwODI1Yi1kZjFhLTQ3ZTYtYmQ5MC0yMTk2NTkyYTVlNGQifQ.KxMQyxDZ3zsZDHj5OjZdAWi3J8fuYdxMbl2NC3fzS0e308Zd_t8CvtFX0F3edYAwvy3mdnva_MxKkxgSsXGniv2UfiFj7p8gKaqybYB4ngb1mX1BZCJZ27M0K5g9H3pa4g3csKp_UHjmV2LBHePkr3RA343E9ezDtR-4WwxQwJ6Lq_dmdXvtMW5iQLE9SsDT0f6rPNs2jt1mx-_PjT3mpOo2NG7mMB1TPX_runu53PqnJ844QoZNa1yjjIhJUGxLzE-DH8t6pjwiatnd1kDS7jDhfAn41l-t29IraIpYKTmPEGNxKd6EkIr-j7Si54HPIJZoZXY8E-UEnLHwDNo7hQ";

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
            id_token_trust_mode: IdTokenTrustMode::Never,
            principal_bool_operator: JsonRule::new(serde_json::json!(
                {"===": [{"var": "Jans::User"}, "ALLOW"]}
            ))
            .unwrap(),
        },
        entity_builder_config: EntityBuilderConfig::default().with_user().with_workload(),
        lock_config: Some(lock_config),
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

    // we sleep for a bit so we don't exit before any logs are sent
    println!("sleeping for 5 secs to give time for Cedarling's Lock service to send the logs");
    sleep(Duration::from_secs(5)).await;

    println!("logs should be sent to the lock server by now!");

    // make sure to call shut_down() to flush any remaining logs
    cedarling.shut_down().await;
    println!("exiting");

    Ok(())
}
