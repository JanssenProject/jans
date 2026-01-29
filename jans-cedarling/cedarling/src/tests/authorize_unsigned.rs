// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::sync::LazyLock;

use test_utils::assert_eq;
use tokio::test;

use super::utils::*;
use crate::log::interface::LogStorage;
use crate::{
    JsonRule, cmp_decision, cmp_policy,
    tests::utils::cedarling_util::get_cedarling_with_callback,
    tests::utils::test_helpers::{create_test_principal, create_test_unsigned_request},
};

static POLICY_STORE_RAW_YAML: &str =
    include_str!("../../../test_files/policy-store_no_trusted_issuers.yaml");

static OPERATOR_AND: LazyLock<JsonRule> = LazyLock::new(|| {
    JsonRule::new(json!({
        "and" : [
            {"===": [{"var": "Jans::TestPrincipal1"}, "ALLOW"]},
            {"===": [{"var": "Jans::TestPrincipal2"}, "ALLOW"]},
            {"===": [{"var": "Jans::TestPrincipal3"}, "ALLOW"]}
        ]
    }))
    .unwrap()
});

/// Check if action executes for next principals: `TestPrincipal1`, `TestPrincipal2`, `TestPrincipal3`
#[test]
async fn test_authorize_unsigned_for_all_principals_success() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        |config| config.authorization_config.principal_bool_operator = OPERATOR_AND.clone(),
    )
    .await;

    let request = create_test_unsigned_request(
        "Jans::Action::\"UpdateForTestPrincipals\"",
        vec![
            create_test_principal("Jans::TestPrincipal1", "random_id", json!({"is_ok": true}))
                .unwrap(),
            create_test_principal("Jans::TestPrincipal2", "random_id", json!({"is_ok": true}))
                .unwrap(),
            create_test_principal("Jans::TestPrincipal3", "random_id", json!({"is_ok": true}))
                .unwrap(),
        ],
        create_test_principal(
            "Jans::Issue",
            "random_id",
            json!({"org_id": "some_long_id", "country": "US"}),
        )
        .unwrap(),
    );

    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("request should be parsed without errors");

    let test_principal_1_result = result
        .principals
        .get("Jans::TestPrincipal1")
        .map(std::borrow::ToOwned::to_owned);

    let test_principal_2_result = result
        .principals
        .get("Jans::TestPrincipal2")
        .map(std::borrow::ToOwned::to_owned);

    let test_principal_3_result = result
        .principals
        .get("Jans::TestPrincipal3")
        .map(std::borrow::ToOwned::to_owned);

    cmp_decision!(
        test_principal_1_result,
        Decision::Allow,
        "request result should be allowed for TestPrincipal1"
    );
    cmp_policy!(
        test_principal_1_result,
        ["5"],
        "reason of permit should be '5' for TestPrincipal1"
    );

    cmp_decision!(
        test_principal_2_result,
        Decision::Allow,
        "request result should be allowed for TestPrincipal2"
    );

    cmp_policy!(
        test_principal_2_result,
        ["5"],
        "reason of permit should be '5' for TestPrincipal2"
    );

    cmp_decision!(
        test_principal_3_result,
        Decision::Allow,
        "request result should be allowed for TestPrincipal3"
    );

    cmp_policy!(
        test_principal_3_result,
        ["5"],
        "reason of permit should be '5' for TestPrincipal3"
    );

    assert!(result.decision, "request result should be allowed");
}

/// Check if action executes for same type of principals but different ids
#[test]
async fn test_authorize_unsigned_for_all_principals_success_using_entity_same_type() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        |config| {
            config.authorization_config.principal_bool_operator = JsonRule::new(json!({
                "and" : [
                    {"===": [{"var": "Jans::TestPrincipal1::\"id1\""}, "ALLOW"]},
                    {"===": [{"var": "Jans::TestPrincipal1::\"id2\""}, "ALLOW"]},
                    {"===": [{"var": "Jans::TestPrincipal1::\"id3\""}, "DENY"]}
                ]
            }))
            .unwrap();
        },
    )
    .await;

    let request = create_test_unsigned_request(
        "Jans::Action::\"UpdateForTestPrincipals\"",
        vec![
            create_test_principal("Jans::TestPrincipal1", "id1", json!({"is_ok": true})).unwrap(),
            create_test_principal("Jans::TestPrincipal1", "id2", json!({"is_ok": true})).unwrap(),
            create_test_principal("Jans::TestPrincipal1", "id3", json!({"is_ok": false})).unwrap(),
        ],
        create_test_principal(
            "Jans::Issue",
            "random_id",
            json!({"org_id": "some_long_id", "country": "US"}),
        )
        .unwrap(),
    );

    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("request should be parsed without errors");

    let test_principal_1_result = result
        .principals
        .get("Jans::TestPrincipal1::\"id1\"")
        .map(std::borrow::ToOwned::to_owned);

    let test_principal_2_result = result
        .principals
        .get("Jans::TestPrincipal1::\"id1\"")
        .map(std::borrow::ToOwned::to_owned);

    let test_principal_3_result = result
        .principals
        .get("Jans::TestPrincipal1::\"id1\"")
        .map(std::borrow::ToOwned::to_owned);

    cmp_decision!(
        test_principal_1_result,
        Decision::Allow,
        "request result should be allowed for TestPrincipal1"
    );
    cmp_policy!(
        test_principal_1_result,
        ["5"],
        "reason of permit should be '5' for TestPrincipal1"
    );

    cmp_decision!(
        test_principal_2_result,
        Decision::Allow,
        "request result should be allowed for TestPrincipal2"
    );

    cmp_policy!(
        test_principal_2_result,
        ["5"],
        "reason of permit should be '5' for TestPrincipal2"
    );

    cmp_decision!(
        test_principal_3_result,
        Decision::Allow,
        "request result should be allowed for TestPrincipal3"
    );

    cmp_policy!(
        test_principal_3_result,
        ["5"],
        "reason of permit should be '5' for TestPrincipal3"
    );

    assert!(result.decision, "request result should be allowed");
}

#[test]
async fn test_authorize_unsigned_for_all_principals_failure() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        |config| config.authorization_config.principal_bool_operator = OPERATOR_AND.clone(),
    )
    .await;

    let request = create_test_unsigned_request(
        "Jans::Action::\"UpdateForTestPrincipals\"",
        vec![
            create_test_principal("Jans::TestPrincipal1", "random_id", json!({"is_ok": false}))
                .unwrap(),
            create_test_principal("Jans::TestPrincipal2", "random_id", json!({"is_ok": true}))
                .unwrap(),
            create_test_principal("Jans::TestPrincipal3", "random_id", json!({"is_ok": false}))
                .unwrap(),
        ],
        create_test_principal(
            "Jans::Issue",
            "random_id",
            json!({"org_id": "some_long_id", "country": "US"}),
        )
        .unwrap(),
    );

    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("request should be parsed without errors");

    let empty_vec: Vec<&str> = Vec::new();

    let test_principal_1_result = result
        .principals
        .get("Jans::TestPrincipal1")
        .map(std::borrow::ToOwned::to_owned);

    let test_principal_2_result = result
        .principals
        .get("Jans::TestPrincipal2")
        .map(std::borrow::ToOwned::to_owned);

    let test_principal_3_result = result
        .principals
        .get("Jans::TestPrincipal3")
        .map(std::borrow::ToOwned::to_owned);

    cmp_decision!(
        test_principal_1_result,
        Decision::Deny,
        "request result should be denied for TestPrincipal1"
    );
    cmp_policy!(
        test_principal_1_result,
        empty_vec,
        "reason of permit workload should be empty for TestPrincipal1"
    );

    cmp_decision!(
        test_principal_2_result,
        Decision::Allow,
        "request result should be allowed for TestPrincipal2"
    );

    cmp_policy!(
        test_principal_2_result,
        ["5"],
        "reason of permit should be '5' for TestPrincipal2"
    );

    cmp_decision!(
        test_principal_3_result,
        Decision::Deny,
        "request result should be denied for TestPrincipal3"
    );

    cmp_policy!(
        test_principal_3_result,
        empty_vec,
        "reason of permit should be empty for TestPrincipal3"
    );

    assert!(!result.decision, "request result should be denied");
}

/// Test policy evaluation errors are logged for unsigned authorization
#[test]
async fn test_policy_evaluation_errors_logging_unsigned() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        |config| config.authorization_config.principal_bool_operator = OPERATOR_AND.clone(),
    )
    .await;
    let create_principal = |id: &str, role: &str| {
        create_test_principal(
            "Jans::User",
            id,
            json!({"country": "US", "role": [role], "sub": id}),
        )
        .unwrap()
    };

    let principals = vec![
        create_principal("user1", "Admin"),
        create_principal("user1", "Admin"),
    ];
    let resource = create_test_principal(
        "Jans::Issue",
        "issue1",
        json!({"org_id": "invalid", "country": "US"}),
    )
    .unwrap();

    let request =
        create_test_unsigned_request("Jans::Action::\"AlwaysDeny\"", principals, resource);

    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("request should be parsed without errors");

    // Verify that logs were created and contain the request ID
    let logs = cedarling.pop_logs();
    assert!(!logs.is_empty(), "Should have created logs");

    let logs_with_request_id: Vec<&serde_json::Value> = logs
        .iter()
        .filter(|log| log.get("request_id") == Some(&serde_json::json!(result.request_id)))
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
            "Log kind should be Decision or System, got: {log_kind:?}"
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

/// Verifies that Cedarling can initialize and perform unsigned authorization
/// even when no trusted issuers are configured and JWT signature validation is enabled.
#[test]
async fn test_unsigned_authz_works_without_trusted_issuers() {
    use crate::JwtConfig;
    use jsonwebtoken::Algorithm;
    use std::collections::HashSet;

    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        |config| {
            // Enable JWT signature validation but don't provide trusted issuers
            config.jwt_config = JwtConfig {
                jwks: None,
                jwt_sig_validation: true,
                jwt_status_validation: false,
                signature_algorithms_supported: HashSet::from_iter([
                    Algorithm::HS256,
                    Algorithm::RS256,
                ]),
                ..Default::default()
            };
        },
    )
    .await;

    // Verify unsigned authorization works
    let request = create_test_unsigned_request(
        "Jans::Action::\"UpdateForTestPrincipals\"",
        vec![
            create_test_principal("Jans::TestPrincipal1", "test_id", json!({"is_ok": true}))
                .unwrap(),
        ],
        create_test_principal(
            "Jans::Issue",
            "issue1",
            json!({"org_id": "some_id", "country": "US"}),
        )
        .unwrap(),
    );

    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("unsigned authorization should work without trusted issuers");

    // Verify the request was processed successfully
    assert!(
        result.principals.contains_key("Jans::TestPrincipal1"),
        "Should have result for TestPrincipal1"
    );

    // Verify logs were created
    let logs = cedarling.pop_logs();
    assert!(!logs.is_empty(), "Should have created logs");

    // Verify there's a warning about signed authorization being unavailable
    let warning_logs: Vec<&serde_json::Value> = logs
        .iter()
        .filter(|log| {
            log.get("msg")
                .and_then(|m| m.as_str())
                .is_some_and(|m| m.contains("signed authorization is unavailable"))
        })
        .collect();

    assert!(
        !warning_logs.is_empty(),
        "Should have logged a warning about signed authorization being unavailable"
    );
}
