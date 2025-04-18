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

const SSA_JWT: &str = "eyJraWQiOiJzc2FfOTgwYTQ0ZDQtZWE3OS00YTM1LThlNjMtNzlhNzg4NTNmYzUwX3NpZ19yczI1NiIsInR5cCI6IkpXVCIsImFsZyI6IlJTMjU2In0.eyJzb2Z0d2FyZV9pZCI6IkNlZGFybGluZ1Rlc3QiLCJncmFudF90eXBlcyI6WyJhdXRob3JpemF0aW9uX2NvZGUiLCJyZWZyZXNoX3Rva2VuIl0sIm9yZ19pZCI6InRlc3QiLCJpc3MiOiJodHRwczovL2RlbW9leGFtcGxlLmphbnMuaW8iLCJzb2Z0d2FyZV9yb2xlcyI6WyJjZWRhcmxpbmciXSwiZXhwIjozMzE5NzE3ODEyLCJpYXQiOjE3NDI5MTc4MTMsImp0aSI6IjM5NTA0NTRlLTM5MWMtNDlhOS05YzYxLTY4MGMyNWE4MDk0ZCJ9.INA5qvpheWvJe6DJaeLkOYt1YH3W9gJQ3yy5Cr5G9_QbzazV23FMJDH2Rbysauk4YNC0oIsTL4MBQ_dRn3YaPLapOhizIlxZQF_uHBpYnopsk6KxgiRQTotg1Kw7Kwsi1RHtfHXpplSS15Dc-9QrOIGbNu44zEt1F5FYV5feW2c0u5HIRISoMNPutOYfMH18bZaBM28N8BssuqLv5X_Bc8EuSkmNTERP5L4khv6Mi3uVItkgK9xTbMKCpUstH_LchT1BKD_pTTMAQx6g6TOf3gnwKYQcmQhjJWFUbXnKCjghExV4PrYc6P8YaXdFnPBYoovd8FxS5qrX8trkh6pxeQ";

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
