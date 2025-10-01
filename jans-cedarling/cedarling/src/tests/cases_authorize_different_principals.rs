// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! In this module we test authorize different action
//! where not all principals can be applied
//!
//! all case scenario should have `result.decision == true`
//! because we have checked different scenarios in `cases_authorize_without_check_jwt.rs`

use std::sync::LazyLock;

use test_utils::assert_eq;
use tokio::test;

use super::utils::*;
use crate::log::interface::LogStorage;
use crate::{IdTokenTrustMode, JsonRule, cmp_decision, cmp_policy};

static POLICY_STORE_RAW_YAML: &str = include_str!("../../../test_files/policy-store_ok_2.yaml");

static OPERATOR_AND: LazyLock<JsonRule> = LazyLock::new(|| {
    JsonRule::new(json!({
        "and" : [
            {"===": [{"var": "Jans::Workload"}, "ALLOW"]},
            {"===": [{"var": "Jans::User"}, "ALLOW"]}
        ]
    }))
    .unwrap()
});

static OPERATOR_OR: LazyLock<JsonRule> = LazyLock::new(|| {
    JsonRule::new(json!({
        "or" : [
            {"===": [{"var": "Jans::Workload"}, "ALLOW"]},
            {"===": [{"var": "Jans::User"}, "ALLOW"]}
        ]
    }))
    .unwrap()
});

static OPERATOR_USER: LazyLock<JsonRule> =
    LazyLock::new(|| JsonRule::new(json!({"===": [{"var": "Jans::User"}, "ALLOW"]})).unwrap());

static OPERATOR_WORKLOAD: LazyLock<JsonRule> =
    LazyLock::new(|| JsonRule::new(json!({"===": [{"var": "Jans::Workload"}, "ALLOW"]})).unwrap());

pub(crate) static AUTH_REQUEST_BASE: LazyLock<Request> = LazyLock::new(|| {
    Request::deserialize(serde_json::json!(
        {
            "tokens": {
                "access_token": generate_token_using_claims(json!({
                    "org_id": "some_long_id",
                    "jti": "some_jti",
                    "client_id": "some_client_id",
                    "iss": "https://account.gluu.org",
                    "aud": "some_aud",
                })),
                "id_token": generate_token_using_claims(json!({
                    "jti": "some_jti",
                    "iss": "https://account.gluu.org",
                    "aud": "some_aud",
                    "sub": "some_sub",
                })),
                "userinfo_token":  generate_token_using_claims(json!({
                    "jti": "some_jti",
                    "country": "US",
                    "sub": "some_sub",
                    "iss": "https://account.gluu.org",
                    "role": ["Admin"],
                })),
            },
            // we need specify action name in each test case
            "action": "",
            "resource": {
                "cedar_entity_mapping": {
                    "entity_type": "Jans::Issue",
                    "id": "random_id"
                },
                "org_id": "some_long_id",
                "country": "US"
            },
            "context": {},
        }
    ))
    .expect("Request should be deserialized from json")
});

/// Check if action executes for next principals: Workload, User
#[test]
async fn success_test_for_all_principals() {
    let cedarling = get_cedarling(PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string())).await;

    let mut request = AUTH_REQUEST_BASE.clone();
    request.action = "Jans::Action::\"Update\"".to_string();

    let result = cedarling
        .authorize(request)
        .await
        .expect("request should be parsed without errors");

    cmp_decision!(
        result.workload,
        Decision::Allow,
        "request result should be allowed for workload"
    );
    cmp_policy!(
        result.workload,
        ["1"],
        "reason of permit workload should be '1'"
    );

    cmp_decision!(
        result.person,
        Decision::Allow,
        "request result should be allowed for person"
    );

    cmp_policy!(
        result.person,
        ["2", "3"],
        "reason of permit person should be '2'"
    );

    assert!(result.decision, "request result should be allowed");
}

/// Check if action executes for next principals: Workload
#[test]
async fn success_test_for_principal_workload() {
    let cedarling = get_cedarling_with_authorization_conf(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        crate::AuthorizationConfig {
            use_user_principal: false,
            use_workload_principal: true,
            principal_bool_operator: OPERATOR_WORKLOAD.to_owned(),
            id_token_trust_mode: IdTokenTrustMode::Never,
            ..Default::default()
        },
        {
            let mut config = crate::EntityBuilderConfig::default().with_workload();
            config.build_user = false;
            config
        },
    )
    .await;

    let mut request = AUTH_REQUEST_BASE.clone();
    request.action = "Jans::Action::\"UpdateForWorkload\"".to_string();

    let result = cedarling
        .authorize(request)
        .await
        .expect("request should be parsed without errors");

    cmp_decision!(
        result.workload,
        Decision::Allow,
        "request result should be allowed for workload"
    );
    cmp_policy!(
        result.workload,
        ["1"],
        "reason of permit workload should be '1'"
    );

    assert!(result.person.is_none(), "result for person should be none");

    assert!(result.decision, "request result should be allowed");
}

/// Check if action executes for next principals: User
#[test]
async fn success_test_for_principal_user() {
    let cedarling = get_cedarling_with_authorization_conf(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        crate::AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: false,
            principal_bool_operator: OPERATOR_USER.to_owned(),
            id_token_trust_mode: IdTokenTrustMode::Never,
            ..Default::default()
        },
        {
            let mut config = crate::EntityBuilderConfig::default().with_user();
            config.build_workload = false;
            config
        },
    )
    .await;

    let mut request = AUTH_REQUEST_BASE.clone();
    request.action = "Jans::Action::\"UpdateForUser\"".to_string();

    let result = cedarling
        .authorize(request)
        .await
        .expect("request should be parsed without errors");

    cmp_decision!(
        result.person,
        Decision::Allow,
        "request result should be allowed for person"
    );
    cmp_policy!(
        result.person,
        ["2"],
        "reason of permit person should be '2'"
    );

    assert!(
        result.workload.is_none(),
        "result for workload should be none"
    );

    assert!(result.decision, "request result should be allowed");
}

/// Check if action executes for next principals: Person (only)
/// check for user and role
#[test]
async fn success_test_for_principal_person_role() {
    let cedarling = get_cedarling_with_authorization_conf(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        crate::AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: false,
            principal_bool_operator: OPERATOR_USER.to_owned(),
            id_token_trust_mode: IdTokenTrustMode::Never,
            ..Default::default()
        },
        {
            let mut config = crate::EntityBuilderConfig::default().with_user();
            config.build_workload = false;
            config
        },
    )
    .await;

    let mut request = AUTH_REQUEST_BASE.clone();
    request.action = "Jans::Action::\"UpdateForUserAndRole\"".to_string();

    let result = cedarling
        .authorize(request)
        .await
        .expect("request should be parsed without errors");

    cmp_policy!(
        result.person,
        ["2", "3"],
        "reason of permit person should be '2'"
    );

    cmp_decision!(
        result.person,
        Decision::Allow,
        "request result should be allowed for person"
    );

    assert!(
        result.workload.is_none(),
        "result for workload should be none"
    );

    assert!(result.decision, "request result should be allowed");
}

/// Check if action executes for next principals: Workload AND Person (Role)
#[test]
async fn success_test_for_principal_workload_role() {
    let cedarling = get_cedarling_with_authorization_conf(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        crate::AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: true,
            principal_bool_operator: OPERATOR_AND.to_owned(),
            id_token_trust_mode: IdTokenTrustMode::Never,
            ..Default::default()
        },
        crate::EntityBuilderConfig::default()
            .with_workload()
            .with_user(),
    )
    .await;

    let mut request = AUTH_REQUEST_BASE.clone();
    request.action = "Jans::Action::\"UpdateForWorkloadAndRole\"".to_string();

    let result = cedarling
        .authorize(request)
        .await
        .expect("request should be parsed without errors");

    cmp_decision!(
        result.workload,
        Decision::Allow,
        "request result should be allowed for workload"
    );
    cmp_policy!(
        result.workload,
        ["1"],
        "reason of permit workload should be '1'"
    );

    cmp_decision!(
        result.person,
        Decision::Allow,
        "request result should be allowed for person"
    );
    cmp_policy!(
        result.person,
        ["3"],
        "reason of permit person should be '3'"
    );

    assert!(result.decision, "request result should be allowed");
}

/// Check if action executes for next principals: Workload (true) OR Person (false)
/// is used operator OR
#[test]
async fn success_test_for_principal_workload_true_or_user_false() {
    let cedarling = get_cedarling_with_authorization_conf(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        crate::AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: true,
            principal_bool_operator: OPERATOR_OR.to_owned(),
            id_token_trust_mode: IdTokenTrustMode::Never,
            ..Default::default()
        },
        crate::EntityBuilderConfig::default()
            .with_user()
            .with_workload(),
    )
    .await;

    let mut request = AUTH_REQUEST_BASE.clone();
    request.action = "Jans::Action::\"UpdateForWorkload\"".to_string();

    let result = cedarling
        .authorize(request)
        .await
        .expect("request should be parsed without errors");

    cmp_decision!(
        result.workload,
        Decision::Allow,
        "request result should be allowed for workload"
    );
    cmp_policy!(
        result.workload,
        ["1"],
        "reason of permit workload should be '1'"
    );

    cmp_decision!(
        result.person,
        Decision::Deny,
        "request result should be allowed for person"
    );
    cmp_policy!(
        result.person,
        Vec::new() as Vec<String>,
        "reason of permit person should be empty"
    );

    assert!(result.decision, "request result should be allowed");
}

/// Check if action executes for next principals: Workload (false) OR Person (true)
/// is used operator OR
#[test]
async fn success_test_for_principal_workload_false_or_user_true() {
    let cedarling = get_cedarling_with_authorization_conf(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        crate::AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: true,
            principal_bool_operator: OPERATOR_OR.to_owned(),
            id_token_trust_mode: IdTokenTrustMode::Never,
            ..Default::default()
        },
        crate::EntityBuilderConfig::default()
            .with_user()
            .with_workload(),
    )
    .await;

    let mut request = AUTH_REQUEST_BASE.clone();
    request.action = "Jans::Action::\"UpdateForUser\"".to_string();

    let result = cedarling
        .authorize(request)
        .await
        .expect("request should be parsed without errors");

    cmp_decision!(
        result.workload,
        Decision::Deny,
        "request result should be not allowed for workload"
    );
    cmp_policy!(
        result.workload,
        Vec::new() as Vec<String>,
        "reason of permit workload should be empty"
    );

    cmp_decision!(
        result.person,
        Decision::Allow,
        "request result should be allowed for person"
    );
    cmp_policy!(
        result.person,
        ["2"],
        "reason of permit person should be '2'"
    );

    assert!(result.decision, "request result should be allowed");
}

/// Check if action executes for next principals: Workload (false) OR Person (false)
/// is used operator OR
#[test]
async fn success_test_for_principal_workload_false_or_user_false() {
    let cedarling = get_cedarling_with_authorization_conf(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        crate::AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: true,
            principal_bool_operator: OPERATOR_OR.to_owned(),
            id_token_trust_mode: IdTokenTrustMode::Never,
            ..Default::default()
        },
        crate::EntityBuilderConfig::default()
            .with_user()
            .with_workload(),
    )
    .await;

    let mut request = AUTH_REQUEST_BASE.clone();
    request.action = "Jans::Action::\"AlwaysDeny\"".to_string();

    let result = cedarling
        .authorize(request)
        .await
        .expect("request should be parsed without errors");

    cmp_decision!(
        result.workload,
        Decision::Deny,
        "request result should be not allowed for workload"
    );
    cmp_policy!(
        result.workload,
        Vec::new() as Vec<String>,
        "reason of permit workload should be empty"
    );

    cmp_decision!(
        result.person,
        Decision::Deny,
        "request result should be not allowed for person"
    );
    cmp_policy!(
        result.person,
        Vec::new() as Vec<String>,
        "reason of permit person should be empty"
    );

    assert!(!result.decision, "request result should be not allowed");
}

/// Check if action executes when principal workload can't be applied
#[test]
async fn test_where_principal_workload_cant_be_applied() {
    let cedarling = get_cedarling_with_authorization_conf(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        crate::AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: true,
            principal_bool_operator: Default::default(),
            id_token_trust_mode: IdTokenTrustMode::Never,
            ..Default::default()
        },
        crate::EntityBuilderConfig::default()
            .with_user()
            .with_workload(),
    )
    .await;

    let mut request = AUTH_REQUEST_BASE.clone();
    request.action = "Jans::Action::\"NoApplies\"".to_string();

    let result = cedarling
        .authorize(request)
        .await
        .expect_err("request should be parsed with error");

    assert!(matches!(result, crate::AuthorizeError::InvalidPrincipal(_)))
}

/// Check if action executes when principal user can't be applied
#[test]
async fn test_where_principal_user_cant_be_applied() {
    let cedarling = get_cedarling_with_authorization_conf(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        crate::AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: false,
            principal_bool_operator: JsonRule::default(),
            id_token_trust_mode: IdTokenTrustMode::Never,
            ..Default::default()
        },
        crate::EntityBuilderConfig::default().with_user(),
    )
    .await;

    let mut request = AUTH_REQUEST_BASE.clone();
    request.action = "Jans::Action::\"NoApplies\"".to_string();

    let result = cedarling
        .authorize(request)
        .await
        .expect_err("request should be parsed with error");

    assert!(
        matches!(result, crate::AuthorizeError::InvalidPrincipal(_)),
        "expected error InvalidPrincipal, got: {}",
        result
    )
}

/// Test policy evaluation errors are logged for signed authorization
#[test]
async fn test_policy_evaluation_errors_logging() {
    let cedarling = get_cedarling_with_authorization_conf(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        crate::AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: true,
            principal_bool_operator: OPERATOR_AND.to_owned(),
            id_token_trust_mode: IdTokenTrustMode::Never,
            ..Default::default()
        },
        crate::EntityBuilderConfig::default()
            .with_user()
            .with_workload(),
    )
    .await;

    let mut request = AUTH_REQUEST_BASE.clone();
    request.action = "Jans::Action::\"AlwaysDeny\"".to_string();

    let result = cedarling
        .authorize(request)
        .await
        .expect("request should be parsed without errors");

    // Verify that logs were created and contain the request ID
    let logs = cedarling.pop_logs();
    assert!(!logs.is_empty(), "Should have created logs");

    let request_id = &result.request_id;
    let logs_with_request_id: Vec<&serde_json::Value> = logs
        .iter()
        .filter(|log| log.get("request_id") == Some(&serde_json::json!(request_id)))
        .collect();

    assert!(
        !logs_with_request_id.is_empty(),
        "Should have logs for the request ID"
    );

    // Verify that logs contain expected content
    for log in &logs_with_request_id {
        // Verify basic log structure
        assert!(log.get("id").is_some(), "Log should have an id field");
        assert!(
            log.get("timestamp").is_some(),
            "Log should have a timestamp field"
        );
        assert!(
            log.get("log_kind").is_some(),
            "Log should have a log_kind field"
        );

        // Verify log kind is valid
        let log_kind = log.get("log_kind").unwrap();
        assert!(
            log_kind == "Decision" || log_kind == "System",
            "Log kind should be Decision or System, got: {:?}",
            log_kind
        );

        // For Decision logs, verify they have required fields
        if log_kind == "Decision" {
            assert!(
                log.get("action").is_some(),
                "Decision log should have an action field"
            );
            assert!(
                log.get("resource").is_some(),
                "Decision log should have a resource field"
            );
            assert!(
                log.get("decision").is_some(),
                "Decision log should have a decision field"
            );
            assert!(
                log.get("diagnostics").is_some(),
                "Decision log should have a diagnostics field"
            );

            // Verify the action matches what we requested
            let log_action = log.get("action").unwrap();
            assert_eq!(
                log_action,
                &serde_json::json!("Jans::Action::\"AlwaysDeny\""),
                "Decision log should have the correct action"
            );

            // Verify the decision is DENY
            let log_decision = log.get("decision").unwrap();
            assert_eq!(
                log_decision,
                &serde_json::json!("DENY"),
                "Decision log should show DENY decision"
            );

            // Verify diagnostics structure
            let diagnostics = log.get("diagnostics").unwrap();
            assert!(
                diagnostics.get("reason").is_some(),
                "Diagnostics should have a reason field"
            );
            assert!(
                diagnostics.get("errors").is_some(),
                "Diagnostics should have an errors field"
            );

            // Verify no policy evaluation errors (since AlwaysDeny just denies, doesn't cause errors)
            let errors = diagnostics.get("errors").unwrap();
            assert_eq!(
                errors,
                &serde_json::json!([]),
                "Diagnostics should show no errors when there are no policy evaluation errors"
            );
        }
    }
}

/// Test Auth0 array aud claims work correctly with automatic string-to-array conversion
#[test]
async fn test_auth0_array_aud_claims() {
    let cedarling = get_cedarling_with_authorization_conf(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        crate::AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: false,
            principal_bool_operator: OPERATOR_USER.to_owned(),
            id_token_trust_mode: IdTokenTrustMode::Strict,
            ..Default::default()
        },
        {
            let mut config = crate::EntityBuilderConfig::default().with_user();
            config.build_workload = false;
            config
        },
    )
    .await;

    // Create a new request with Jans-style string aud claim in id_token
    let request = Request::deserialize(serde_json::json!({
        "tokens": {
            "access_token": generate_token_using_claims(json!({
                "org_id": "some_long_id",
                "jti": "some_jti",
                "client_id": "some_client_id",
                "iss": "https://account.gluu.org",
                "aud": "some_aud",
            })),
            "id_token": generate_token_using_claims(json!({
                "jti": "jans_test_jti",
                "iss": "https://account.gluu.org",
                "aud": [
                    "some_client_id",
                    "some_aud"
                ],
                "sub": "jans_user_sub",
            })),
            "userinfo_token": generate_token_using_claims(json!({
                "jti": "some_jti",
                "country": "US",
                "sub": "some_sub",
                "aud": "some_client_id",
                "iss": "https://account.gluu.org",
                "role": ["Admin"],
            })),
        },
        "action": "Jans::Action::\"Update\"",
        "resource": {
            "cedar_entity_mapping": {
                "entity_type": "Jans::Issue",
                "id": "random_id"
            },
            "org_id": "some_long_id",
            "country": "US"
        },
        "context": {},
    }))
    .expect("Request should be deserialized from json");

    let result = cedarling
        .authorize(request)
        .await
        .expect("request should be parsed without errors");

    // Verify the authorization succeeded
    cmp_decision!(
        result.person,
        Decision::Allow,
        "request result should be allowed for person with Jans string aud"
    );

    assert!(
        result.workload.is_none(),
        "result for workload should be none"
    );

    assert!(result.decision, "request result should be allowed");
}

/// Test Jans string aud claims still work correctly (backward compatibility)
#[test]
async fn test_jans_string_aud_claims() {
    let cedarling = get_cedarling_with_authorization_conf(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        crate::AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: false,
            principal_bool_operator: OPERATOR_USER.to_owned(),
            id_token_trust_mode: IdTokenTrustMode::Strict,
            ..Default::default()
        },
        {
            let mut config = crate::EntityBuilderConfig::default().with_user();
            config.build_workload = false;
            config
        },
    )
    .await;

    // Create a new request with Jans-style string aud claim in id_token
    let request = Request::deserialize(serde_json::json!({
        "tokens": {
            "access_token": generate_token_using_claims(json!({
                "org_id": "some_long_id",
                "jti": "some_jti",
                "client_id": "some_client_id",
                "iss": "https://account.gluu.org",
                "aud": "some_aud",
            })),
            "id_token": generate_token_using_claims(json!({
                "jti": "jans_test_jti",
                "iss": "https://account.gluu.org",
                "aud": "some_client_id",
                "sub": "jans_user_sub",
            })),
            "userinfo_token": generate_token_using_claims(json!({
                "jti": "some_jti",
                "country": "US",
                "sub": "some_sub",
                "aud": "some_client_id",
                "iss": "https://account.gluu.org",
                "role": ["Admin"],
            })),
        },
        "action": "Jans::Action::\"Update\"",
        "resource": {
            "cedar_entity_mapping": {
                "entity_type": "Jans::Issue",
                "id": "random_id"
            },
            "org_id": "some_long_id",
            "country": "US"
        },
        "context": {},
    }))
    .expect("Request should be deserialized from json");

    let result = cedarling
        .authorize(request)
        .await
        .expect("request should be parsed without errors");

    // Verify the authorization succeeded
    cmp_decision!(
        result.person,
        Decision::Allow,
        "request result should be allowed for person with Jans string aud"
    );

    assert!(
        result.workload.is_none(),
        "result for workload should be none"
    );

    assert!(result.decision, "request result should be allowed");
}
