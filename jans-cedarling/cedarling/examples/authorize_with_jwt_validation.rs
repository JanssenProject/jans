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
use serde::Deserialize;
use std::collections::HashMap;

// Load a JSON policy store file, containing policies and trusted issuers, at compile time.
// This file defines access control policies for different resources and actions.
static POLICY_STORE_RAW: &str =
    include_str!("../../test_files/policy-store_with_trusted_issuers_ok.json");

// Load example tokens from a JSON file, also at compile time.
// NOTE: `tokens.json` is ignored in version control for security reasons.
// To run this example, create a `tokens.json` file based on `tokens.example.json`.
static TOKENS: &str = include_str!("./tokens.json");

#[derive(Deserialize)]
struct Tokens {
    access_token: String,
    userinfo_token: String,
    id_token: String,
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Configure JWT validation settings. Enable the JwtService to validate JWT tokens
    // using specific algorithms: `HS256` and `RS256`. Only tokens signed with these algorithms
    // will be accepted; others will be marked as invalid during validation.
    let jwt_config = JwtConfig::Enabled {
        signature_algorithms: vec!["HS256".to_string(), "RS256".to_string()],
    };

    // Initialize the main Cedarling instance, responsible for policy-based authorization.
    // This setup includes basic application information, logging configuration, and
    // policy store configuration.
    let cedarling = Cedarling::new(BootstrapConfig {
        application_name: "test_app".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::StdOut,
        },
        policy_store_config: PolicyStoreConfig {
            source: PolicyStoreSource::Json(POLICY_STORE_RAW.to_string()),
        },
        jwt_config,
    })?;

    // Parse the tokens from the JSON string loaded from `tokens.json`.
    // This will create a `Tokens` struct populated with `access_token`, `userinfo_token`, and `id_token`.
    let tokens = serde_json::from_str::<Tokens>(TOKENS).expect("should deserialize tokens");

    // Perform an authorization request to Cedarling.
    // This request checks if the provided tokens have sufficient permission to perform an action
    // on a specific resource. Each token (access, ID, and userinfo) is required for the
    // authorization process, alongside resource and action details.
    let result = cedarling.authorize(Request {
        access_token: tokens.access_token,
        id_token: tokens.id_token,
        userinfo_token: tokens.userinfo_token,
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
