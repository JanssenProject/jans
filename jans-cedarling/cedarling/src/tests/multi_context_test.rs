// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::*;
use jsonwebtoken::Algorithm;
use std::collections::{HashMap, HashSet};
use crate::bootstrap_config::{
    AuthorizationConfig, EntityBuilderConfig, EntityNames, IdTokenTrustMode, 
    JwtConfig, LogConfig, LogTypeConfig, MemoryLogConfig, PolicyStoreConfig, 
    PolicyStoreSource, UnsignedRoleIdSrc
};
use crate::common::json_rules::JsonRule;

// Simple policy store for testing
static SIMPLE_POLICY_STORE: &str = include_str!("../../../test_files/policy-store_ok_2.yaml");

#[tokio::test]
async fn test_multi_context_authorization_basic() {
    // Configure JWT validation settings
    let jwt_config = JwtConfig {
        jwks: None,
        jwt_sig_validation: false, // Disable signature validation for testing
        jwt_status_validation: false,
        signature_algorithms_supported: HashSet::from_iter([
            Algorithm::HS256,
            Algorithm::RS256,
        ]),
    };

    // Create bootstrap configuration
    let bootstrap_config = BootstrapConfig {
        application_name: "multi_context_test".to_string(),
        jwt_config,
        log_config: LogConfig {
            log_type: LogTypeConfig::Memory(MemoryLogConfig {
                log_ttl: 3600,
                max_items: Some(1000),
                max_item_size: Some(1024),
            }),
            log_level: LogLevel::INFO,
        },
        policy_store_config: PolicyStoreConfig {
            source: PolicyStoreSource::Yaml(SIMPLE_POLICY_STORE.to_string()),
        },
        authorization_config: AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: false,
            principal_bool_operator: JsonRule::default(),
            decision_log_user_claims: vec!["sub".to_string(), "iss".to_string()],
            decision_log_workload_claims: vec!["client_id".to_string()],
            decision_log_default_jwt_id: "jti".to_string(),
            id_token_trust_mode: IdTokenTrustMode::Never,
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
        lock_config: None,
    };

    // Initialize Cedarling
    let cedarling = Cedarling::new(&bootstrap_config).await.unwrap();

    // Create token bundles for different contexts
    let bundle1 = MultiContextTokenBundle {
        tokens: Some(HashMap::from([
            ("tx_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzEiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzEifQ.signature".to_string()),
            ("access_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzEiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzEifQ.signature".to_string()),
            ("id_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzEiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzEifQ.signature".to_string()),
            ("userinfo_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzEiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzEifQ.signature".to_string()),
        ])),
        principals: None,
        context_id: Some("context_1".to_string()),
    };

    let bundle2 = MultiContextTokenBundle {
        tokens: Some(HashMap::from([
            ("tx_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzIiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzIifQ.signature".to_string()),
            ("access_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzIiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzIifQ.signature".to_string()),
            ("id_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzIiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzIifQ.signature".to_string()),
            ("userinfo_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzIiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzIifQ.signature".to_string()),
        ])),
        principals: None,
        context_id: Some("context_2".to_string()),
    };

    // Create multi-context request
    let multi_context_request = MultiContextRequest {
        token_bundles: vec![bundle1, bundle2],
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

    // Perform multi-context authorization
    let result = cedarling.authorize_multi_context(multi_context_request).await.unwrap();
    
    // Verify the result structure
    assert_eq!(result.context_results.len(), 2);
    assert!(result.context_results.contains_key("context_1"));
    assert!(result.context_results.contains_key("context_2"));
    assert!(!result.request_id.is_empty());
    
    // Verify that each context has a result
    for (context_id, context_result) in &result.context_results {
        assert!(!context_id.is_empty());
        assert!(!context_result.request_id.is_empty());
        // Note: The actual decision depends on the policy evaluation
        // We're just testing that the structure is correct
    }
}

#[tokio::test]
async fn test_multi_context_authorization_without_context_ids() {
    // Configure JWT validation settings
    let jwt_config = JwtConfig {
        jwks: None,
        jwt_sig_validation: false,
        jwt_status_validation: false,
        signature_algorithms_supported: HashSet::from_iter([
            Algorithm::HS256,
            Algorithm::RS256,
        ]),
    };

    // Create bootstrap configuration
    let bootstrap_config = BootstrapConfig {
        application_name: "multi_context_test_no_ids".to_string(),
        jwt_config,
        log_config: LogConfig {
            log_type: LogTypeConfig::Memory(MemoryLogConfig {
                log_ttl: 3600,
                max_items: Some(1000),
                max_item_size: Some(1024),
            }),
            log_level: LogLevel::INFO,
        },
        policy_store_config: PolicyStoreConfig {
            source: PolicyStoreSource::Yaml(SIMPLE_POLICY_STORE.to_string()),
        },
        authorization_config: AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: false,
            principal_bool_operator: JsonRule::default(),
            decision_log_user_claims: vec!["sub".to_string(), "iss".to_string()],
            decision_log_workload_claims: vec!["client_id".to_string()],
            decision_log_default_jwt_id: "jti".to_string(),
            id_token_trust_mode: IdTokenTrustMode::Never,
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
        lock_config: None,
    };

    // Initialize Cedarling
    let cedarling = Cedarling::new(&bootstrap_config).await.unwrap();

    // Create token bundles without context_ids
    let bundle1 = MultiContextTokenBundle {
        tokens: Some(HashMap::from([
            ("tx_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzEiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzEifQ.signature".to_string()),
            ("access_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzEiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzEifQ.signature".to_string()),
            ("id_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzEiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzEifQ.signature".to_string()),
            ("userinfo_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzEiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzEifQ.signature".to_string()),
        ])),
        principals: None,
        context_id: None,
    };

    let bundle2 = MultiContextTokenBundle {
        tokens: Some(HashMap::from([
            ("tx_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzIiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzIifQ.signature".to_string()),
            ("access_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzIiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzIifQ.signature".to_string()),
            ("id_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzIiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzIifQ.signature".to_string()),
            ("userinfo_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzIiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzIifQ.signature".to_string()),
        ])),
        principals: None,
        context_id: None,
    };

    // Create multi-context request
    let multi_context_request = MultiContextRequest {
        token_bundles: vec![bundle1, bundle2],
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

    // Perform multi-context authorization
    let result = cedarling.authorize_multi_context(multi_context_request).await.unwrap();
    
    // Verify the result structure
    assert_eq!(result.context_results.len(), 2);
    assert!(result.context_results.contains_key("0"));
    assert!(result.context_results.contains_key("1"));
    assert!(!result.request_id.is_empty());
}

#[tokio::test]
async fn test_multi_context_authorization_mixed_context_ids() {
    // Configure JWT validation settings
    let jwt_config = JwtConfig {
        jwks: None,
        jwt_sig_validation: false,
        jwt_status_validation: false,
        signature_algorithms_supported: HashSet::from_iter([
            Algorithm::HS256,
            Algorithm::RS256,
        ]),
    };

    // Create bootstrap configuration
    let bootstrap_config = BootstrapConfig {
        application_name: "multi_context_test_mixed".to_string(),
        jwt_config,
        log_config: LogConfig {
            log_type: LogTypeConfig::Memory(MemoryLogConfig {
                log_ttl: 3600,
                max_items: Some(1000),
                max_item_size: Some(1024),
            }),
            log_level: LogLevel::INFO,
        },
        policy_store_config: PolicyStoreConfig {
            source: PolicyStoreSource::Yaml(SIMPLE_POLICY_STORE.to_string()),
        },
        authorization_config: AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: false,
            principal_bool_operator: JsonRule::default(),
            decision_log_user_claims: vec!["sub".to_string(), "iss".to_string()],
            decision_log_workload_claims: vec!["client_id".to_string()],
            decision_log_default_jwt_id: "jti".to_string(),
            id_token_trust_mode: IdTokenTrustMode::Never,
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
        lock_config: None,
    };

    // Initialize Cedarling
    let cedarling = Cedarling::new(&bootstrap_config).await.unwrap();

    // Create token bundles with mixed context_ids
    let bundle1 = MultiContextTokenBundle {
        tokens: Some(HashMap::from([
            ("tx_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzEiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzEifQ.signature".to_string()),
            ("access_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzEiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzEifQ.signature".to_string()),
            ("id_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzEiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzEifQ.signature".to_string()),
            ("userinfo_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzEiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzEifQ.signature".to_string()),
        ])),
        principals: None,
        context_id: Some("named_context".to_string()),
    };

    let bundle2 = MultiContextTokenBundle {
        tokens: Some(HashMap::from([
            ("tx_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzIiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzIifQ.signature".to_string()),
            ("access_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzIiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzIifQ.signature".to_string()),
            ("id_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzIiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzIifQ.signature".to_string()),
            ("userinfo_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzIiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzIifQ.signature".to_string()),
        ])),
        principals: None,
        context_id: None,
    };

    // Create multi-context request
    let multi_context_request = MultiContextRequest {
        token_bundles: vec![bundle1, bundle2],
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

    // Perform multi-context authorization
    let result = cedarling.authorize_multi_context(multi_context_request).await.unwrap();
    
    // Verify the result structure
    assert_eq!(result.context_results.len(), 2);
    assert!(result.context_results.contains_key("named_context"));
    assert!(result.context_results.contains_key("1")); // Index 1 for the bundle without context_id
    assert!(!result.request_id.is_empty());
}

#[tokio::test]
async fn test_multi_context_authorization_empty_bundles() {
    // Configure JWT validation settings
    let jwt_config = JwtConfig {
        jwks: None,
        jwt_sig_validation: false,
        jwt_status_validation: false,
        signature_algorithms_supported: HashSet::from_iter([
            Algorithm::HS256,
            Algorithm::RS256,
        ]),
    };

    // Create bootstrap configuration
    let bootstrap_config = BootstrapConfig {
        application_name: "multi_context_test_empty".to_string(),
        jwt_config,
        log_config: LogConfig {
            log_type: LogTypeConfig::Memory(MemoryLogConfig {
                log_ttl: 3600,
                max_items: Some(1000),
                max_item_size: Some(1024),
            }),
            log_level: LogLevel::INFO,
        },
        policy_store_config: PolicyStoreConfig {
            source: PolicyStoreSource::Yaml(SIMPLE_POLICY_STORE.to_string()),
        },
        authorization_config: AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: false,
            principal_bool_operator: JsonRule::default(),
            decision_log_user_claims: vec!["sub".to_string(), "iss".to_string()],
            decision_log_workload_claims: vec!["client_id".to_string()],
            decision_log_default_jwt_id: "jti".to_string(),
            id_token_trust_mode: IdTokenTrustMode::Never,
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
        lock_config: None,
    };

    // Initialize Cedarling
    let cedarling = Cedarling::new(&bootstrap_config).await.unwrap();

    // Create token bundles with empty tokens
    let bundle1 = MultiContextTokenBundle {
        tokens: Some(HashMap::from([
            ("id_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzEiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzEifQ.signature".to_string()),
            ("userinfo_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzEiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzEifQ.signature".to_string()),
        ])),
        principals: None,
        context_id: Some("empty_bundle".to_string()),
    };

    let bundle2 = MultiContextTokenBundle {
        tokens: Some(HashMap::from([
            ("tx_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzIiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzIifQ.signature".to_string()),
            ("access_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzIiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzIifQ.signature".to_string()),
            ("id_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzIiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzIifQ.signature".to_string()),
            ("userinfo_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjb250ZXh0XzIiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGVzdE9yZzIifQ.signature".to_string()),
        ])),
        principals: None,
        context_id: Some("valid_bundle".to_string()),
    };

    // Create multi-context request
    let multi_context_request = MultiContextRequest {
        token_bundles: vec![bundle1, bundle2],
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

    // Perform multi-context authorization
    let result = cedarling.authorize_multi_context(multi_context_request).await.unwrap();
    
    // Verify the result structure
    assert_eq!(result.context_results.len(), 2);
    assert!(result.context_results.contains_key("empty_bundle"));
    assert!(result.context_results.contains_key("valid_bundle"));
    assert!(!result.request_id.is_empty());
} 

#[tokio::test]
async fn test_multi_context_authorization_with_custom_token_types() {
    // Configure JWT validation settings
    let jwt_config = JwtConfig {
        jwks: None,
        jwt_sig_validation: false,
        jwt_status_validation: false,
        signature_algorithms_supported: HashSet::from_iter([
            Algorithm::HS256,
            Algorithm::RS256,
        ]),
    };

    // Create bootstrap configuration
    let bootstrap_config = BootstrapConfig {
        application_name: "multi_context_test_custom_tokens".to_string(),
        jwt_config,
        log_config: LogConfig {
            log_type: LogTypeConfig::Memory(MemoryLogConfig {
                log_ttl: 3600,
                max_items: Some(1000),
                max_item_size: Some(1024),
            }),
            log_level: LogLevel::INFO,
        },
        policy_store_config: PolicyStoreConfig {
            source: PolicyStoreSource::Yaml(SIMPLE_POLICY_STORE.to_string()),
        },
        authorization_config: AuthorizationConfig {
            use_user_principal: false,
            use_workload_principal: false,
            principal_bool_operator: JsonRule::default(),
            decision_log_user_claims: vec!["sub".to_string(), "iss".to_string()],
            decision_log_workload_claims: vec!["client_id".to_string()],
            decision_log_default_jwt_id: "jti".to_string(),
            id_token_trust_mode: IdTokenTrustMode::Never,
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
        lock_config: None,
    };

    // Initialize Cedarling
    let cedarling = Cedarling::new(&bootstrap_config).await.unwrap();

    // Create token bundles with custom token types
    let bundle1 = MultiContextTokenBundle {
        tokens: Some(HashMap::from([
            ("dolphin_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXhhc19kbXYiLCJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5jb20iLCJpYXQiOjE2MzQ1Njc4OTAsImV4cCI6MTYzNDU3MTQ5MCwic2NvcGUiOiJ0ZXN0Iiwib3JnX2lkIjoiVGV4YXNEVlYifQ.signature".to_string()),
        ])),
        principals: None,
        context_id: Some("texas_dmv".to_string()),
    };

    let bundle2 = MultiContextTokenBundle {
        tokens: Some(HashMap::from([
            ("dolphin_token".to_string(), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJlbWlzc2lvbnNfc2hvcCIsImlzcyI6Imh0dHBzOi8vYXV0aC5leGFtcGxlLmNvbSIsImlhdCI6MTYzNDU2Nzg5MCwiZXhwIjoxNjM0NTcxNDkwLCJzY29wZSI6InRlc3QiLCJvcmdfaWQiOiJFbWlzc2lvbnNTaG9wIn0.signature".to_string()),
        ])),
        principals: None,
        context_id: Some("emissions_shop".to_string()),
    };

    // Create multi-context request
    let multi_context_request = MultiContextRequest {
        token_bundles: vec![bundle1, bundle2],
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

    // Perform multi-context authorization
    let result = cedarling.authorize_multi_context(multi_context_request).await.unwrap();
    
    // Verify the result structure
    assert_eq!(result.context_results.len(), 2);
    assert!(result.context_results.contains_key("texas_dmv"));
    assert!(result.context_results.contains_key("emissions_shop"));
    assert!(!result.request_id.is_empty());
    
    // Verify that each context has a result
    for (context_id, context_result) in &result.context_results {
        assert!(!context_id.is_empty());
        assert!(!context_result.request_id.is_empty());
        // Note: The actual decision depends on the policy evaluation
        // We're just testing that the structure is correct
    }
} 