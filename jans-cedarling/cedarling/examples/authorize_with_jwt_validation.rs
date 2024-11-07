/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use cedarling::{
    BootstrapConfig, Cedarling, JwtConfig, LogConfig, LogTypeConfig, PolicyStoreConfig,
    PolicyStoreSource, Request, ResourceData,
};
use std::collections::HashMap;

static POLICY_STORE_RAW_YAML: &str =
    include_str!("../../test_files/policy-store_with_trusted_issuers_ok.yaml");

fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Configure JWT validation settings. Enable the JwtService to validate JWT tokens
    // using specific algorithms: `HS256` and `RS256`. Only tokens signed with these algorithms
    // will be accepted; others will be marked as invalid during validation.
    let jwt_config = JwtConfig::Enabled {
        signature_algorithms: vec!["HS256".to_string(), "RS256".to_string()],
    };

    // You must change this with your own tokens
    let access_token = "your_access_token_here".to_string();
    let id_token = "your_id_token_here".to_string();
    let userinfo_token = "your_userinfo_token_here".to_string();

    // Initialize the main Cedarling instance, responsible for policy-based authorization.
    // This setup includes basic application information, logging configuration, and
    // policy store configuration.
    let cedarling = Cedarling::new(BootstrapConfig {
        application_name: "test_app".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::StdOut,
        },
        policy_store_config: PolicyStoreConfig {
            source: PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        },
        jwt_config,
    })?;

    // Perform an authorization request to Cedarling.
    // This request checks if the provided tokens have sufficient permission to perform an action
    // on a specific resource. Each token (access, ID, and userinfo) is required for the
    // authorization process, alongside resource and action details.
    let result = cedarling.authorize(Request {
        access_token,
        id_token,
        userinfo_token,
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
    });

    // Handle authorization result. If there's an error, print it.
    if let Err(ref e) = &result {
        eprintln!("Error while authorizing: {:?}\n\n", e)
    }

    Ok(())
}
