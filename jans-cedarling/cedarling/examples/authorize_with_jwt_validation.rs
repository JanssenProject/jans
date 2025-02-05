// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedarling::{
    AuthorizationConfig, BootstrapConfig, Cedarling, JwtConfig, LogConfig, LogLevel, LogTypeConfig,
    PolicyStoreConfig, PolicyStoreSource, Request, ResourceData, TokenValidationConfig,
    WorkloadBoolOp,
};
use jsonwebtoken::Algorithm;
use std::collections::{HashMap, HashSet};

static POLICY_STORE_RAW_YAML: &str =
    include_str!("../../test_files/policy-store_with_trusted_issuers_ok.yaml");

#[tokio::main(flavor = "current_thread")]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Configure JWT validation settings. Enable the JwtService to validate JWT tokens
    // using specific algorithms: `HS256` and `RS256`. Only tokens signed with these algorithms
    // will be accepted; others will be marked as invalid during validation.
    let jwt_config = JwtConfig {
        jwks: None,
        jwt_sig_validation: true,
        jwt_status_validation: false,
        signature_algorithms_supported: HashSet::from_iter([Algorithm::HS256, Algorithm::RS256]),
        token_validation_settings: HashMap::from([
            (
                "access_token".to_string(),
                TokenValidationConfig::access_token(),
            ),
            ("id_token".to_string(), TokenValidationConfig::id_token()),
            (
                "userinfo_token".to_string(),
                TokenValidationConfig::userinfo_token(),
            ),
        ]),
    };

    // You must change this with your own tokens
    let access_token = "your_access_token_here".to_string();
    let id_token = "your_id_token_here".to_string();
    let userinfo_token = "your_userinfo_token_here".to_string();

    // Initialize the main Cedarling instance, responsible for policy-based authorization.
    // This setup includes basic application information, logging configuration, and
    // policy store configuration.
    let cedarling = Cedarling::new(&BootstrapConfig {
        application_name: "test_app".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::StdOut,
            log_level: LogLevel::INFO,
        },
        policy_store_config: PolicyStoreConfig {
            source: PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        },
        jwt_config,
        authorization_config: AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: true,
            user_workload_operator: WorkloadBoolOp::And,
            ..Default::default()
        },
    })
    .await?;

    // Perform an authorization request to Cedarling.
    // This request checks if the provided tokens have sufficient permission to perform an action
    // on a specific resource. Each token (access, ID, and userinfo) is required for the
    // authorization process, alongside resource and action details.
    let result = cedarling
        .authorize(Request {
            tokens: HashMap::from([
                ("access_token".to_string(), access_token.clone()),
                ("id_token".to_string(), id_token.clone()),
                ("userinfo_token".to_string(), userinfo_token.clone()),
            ]),
            action: "Jans::Action::\"Update\"".to_string(),
            context: serde_json::json!({}),
            resource: ResourceData {
                id: "random_id".to_string(),
                resource_type: "Jans::Issue".to_string(),
                payload: HashMap::from_iter([(
                    "org_id".to_string(),
                    serde_json::Value::String("some_long_id".to_string()),
                )]),
            },
        })
        .await;

    // Handle authorization result. If there's an error, print it.
    if let Err(ref e) = &result {
        eprintln!("Error while authorizing: {:?}\n\n", e)
    }

    Ok(())
}
