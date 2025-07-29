// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedarling::*;
use jsonwebtoken::Algorithm;
use std::collections::{HashMap, HashSet};

#[tokio::main(flavor = "current_thread")]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Configure JWT validation settings
    let jwt_config = JwtConfig {
        jwks: None,
        jwt_sig_validation: false, // Disable signature validation for demo
        jwt_status_validation: false,
        signature_algorithms_supported: HashSet::from_iter([
            Algorithm::HS256,
            Algorithm::RS256,
        ]),
    };

    // Create bootstrap configuration
    let bootstrap_config = BootstrapConfig {
        application_name: "Multi-Context Authorization Example".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::StdOut,
            log_level: LogLevel::INFO,
        },
        jwt_config,
        policy_store_config: PolicyStoreConfig {
            source: PolicyStoreSource::Yaml(include_str!("../../test_files/policy-store_ok_2.yaml").to_string()),
        },
        entity_builder_config: EntityBuilderConfig {
            entity_names: EntityNames {
                user: "Jans::User".to_string(),
                workload: "Jans::Workload".to_string(),
                role: "Jans::Role".to_string(),
                iss: "Jans::TrustedIssuer".to_string(),
            },
            build_workload: false,
            build_user: false,
            unsigned_role_id_src: UnsignedRoleIdSrc("role".to_string()),
        },
        authorization_config: AuthorizationConfig {
            use_user_principal: false,
            use_workload_principal: false,
            principal_bool_operator: JsonRule::default(),
            decision_log_user_claims: vec![],
            decision_log_workload_claims: vec![],
            decision_log_default_jwt_id: "jti".to_string(),
            id_token_trust_mode: IdTokenTrustMode::Never,
        },
        lock_config: None,
    };

    // Initialize Cedarling
    let cedarling = Cedarling::new(&bootstrap_config).await?;

    println!("=== Multi-Context Authorization Example ===");
    println!("This example demonstrates mixed signed and unsigned requests\n");

    // Example 1: Mixed signed and unsigned requests
    println!("1. Mixed Signed and Unsigned Requests:");
    
    // Signed request bundle (with JWT tokens)
    let signed_bundle = MultiContextTokenBundle {
        tokens: Some(HashMap::from([
            ("access_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJzaWduZWRfdXNlciIsImlzcyI6Imh0dHBzOi8vYXV0aC5leGFtcGxlLmNvbSIsImlhdCI6MTYzNDU2Nzg5MCwiZXhwIjoxNjM0NTcxNDkwLCJzY29wZSI6InRlc3QiLCJvcmdfaWQiOiJTaWduZWRPcmcifQ.signature".to_string()),
        ])),
        principals: None,
        context_id: Some("signed_context".to_string()),
    };

    // Unsigned request bundle (with pre-built principals)
    let unsigned_bundle = MultiContextTokenBundle {
        tokens: None,
        principals: Some(vec![
            EntityData {
                entity_type: "Jans::User".to_string(),
                id: "unsigned_user".to_string(),
                attributes: HashMap::from([
                    ("sub".to_string(), serde_json::json!("unsigned_user")),
                    ("department".to_string(), serde_json::json!("Engineering")),
                    ("role".to_string(), serde_json::json!("Developer")),
                    ("country".to_string(), serde_json::json!("US")),
                ]),
            },
        ]),
        context_id: Some("unsigned_context".to_string()),
    };

    let multi_context_request = MultiContextRequest {
        token_bundles: vec![signed_bundle, unsigned_bundle],
        action: "Jans::Action::\"Update\"".to_string(),
        resource: EntityData {
            entity_type: "Jans::Issue".to_string(),
            id: "test_resource".to_string(),
            attributes: HashMap::from([
                ("org_id".to_string(), serde_json::json!("TestOrg")),
                ("country".to_string(), serde_json::json!("US")),
            ]),
        },
        context: serde_json::json!({}),
    };

    let result = cedarling.authorize_multi_context(multi_context_request).await?;

    println!("   Overall decision: {}", if result.overall_decision { "ALLOW" } else { "DENY" });
    println!("   Request ID: {}", result.request_id);
    println!("   Context results:");
    for (context_id, context_result) in &result.context_results {
        println!("     - {}: {}", context_id, if context_result.decision { "ALLOW" } else { "DENY" });
    }

    println!("\n2. All Signed Requests:");
    
    // Example with only signed requests
    let signed_bundle1 = MultiContextTokenBundle {
        tokens: Some(HashMap::from([
            ("access_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzEiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzEifQ.signature".to_string()),
        ])),
        principals: None,
        context_id: Some("context_1".to_string()),
    };

    let signed_bundle2 = MultiContextTokenBundle {
        tokens: Some(HashMap::from([
            ("access_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzIiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzIifQ.signature".to_string()),
        ])),
        principals: None,
        context_id: Some("context_2".to_string()),
    };

    let signed_request = MultiContextRequest {
        token_bundles: vec![signed_bundle1, signed_bundle2],
        action: "Jans::Action::\"Update\"".to_string(),
        resource: EntityData {
            entity_type: "Jans::Issue".to_string(),
            id: "test_resource".to_string(),
            attributes: HashMap::from([
                ("org_id".to_string(), serde_json::json!("TestOrg")),
                ("country".to_string(), serde_json::json!("US")),
            ]),
        },
        context: serde_json::json!({}),
    };

    let signed_result = cedarling.authorize_multi_context(signed_request).await?;

    println!("   Overall decision: {}", if signed_result.overall_decision { "ALLOW" } else { "DENY" });
    println!("   Request ID: {}", signed_result.request_id);
    println!("   Context results:");
    for (context_id, context_result) in &signed_result.context_results {
        println!("     - {}: {}", context_id, if context_result.decision { "ALLOW" } else { "DENY" });
    }

    println!("\n3. All Unsigned Requests:");
    
    // Example with only unsigned requests
    let unsigned_bundle1 = MultiContextTokenBundle {
        tokens: None,
        principals: Some(vec![
            EntityData {
                entity_type: "Jans::User".to_string(),
                id: "user1".to_string(),
                attributes: HashMap::from([
                    ("sub".to_string(), serde_json::json!("user1")),
                    ("department".to_string(), serde_json::json!("Sales")),
                    ("role".to_string(), serde_json::json!("Manager")),
                    ("country".to_string(), serde_json::json!("US")),
                ]),
            },
        ]),
        context_id: Some("unsigned_1".to_string()),
    };

    let unsigned_bundle2 = MultiContextTokenBundle {
        tokens: None,
        principals: Some(vec![
            EntityData {
                entity_type: "Jans::User".to_string(),
                id: "user2".to_string(),
                attributes: HashMap::from([
                    ("sub".to_string(), serde_json::json!("user2")),
                    ("department".to_string(), serde_json::json!("Marketing")),
                    ("role".to_string(), serde_json::json!("Director")),
                    ("country".to_string(), serde_json::json!("US")),
                ]),
            },
        ]),
        context_id: Some("unsigned_2".to_string()),
    };

    let unsigned_request = MultiContextRequest {
        token_bundles: vec![unsigned_bundle1, unsigned_bundle2],
        action: "Jans::Action::\"Update\"".to_string(),
        resource: EntityData {
            entity_type: "Jans::Issue".to_string(),
            id: "test_resource".to_string(),
            attributes: HashMap::from([
                ("org_id".to_string(), serde_json::json!("TestOrg")),
                ("country".to_string(), serde_json::json!("US")),
            ]),
        },
        context: serde_json::json!({}),
    };

    let unsigned_result = cedarling.authorize_multi_context(unsigned_request).await?;

    println!("   Overall decision: {}", if unsigned_result.overall_decision { "ALLOW" } else { "DENY" });
    println!("   Request ID: {}", unsigned_result.request_id);
    println!("   Context results:");
    for (context_id, context_result) in &unsigned_result.context_results {
        println!("     - {}: {}", context_id, if context_result.decision { "ALLOW" } else { "DENY" });
    }

    println!("\n=== Example completed successfully! ===");
    println!("The multi-context authorization now supports:");
    println!("  ✓ Mixed signed and unsigned requests");
    println!("  ✓ All signed requests (original functionality)");
    println!("  ✓ All unsigned requests (new functionality)");
    println!("  ✓ Dynamic token types (like dolphin_token)");
    println!("  ✓ Context-specific authorization decisions");

    Ok(())
} 