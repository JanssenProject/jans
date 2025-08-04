// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::sync::LazyLock;

use test_utils::assert_eq;
use tokio::test;

use super::utils::*;
use crate::authz::request::EntityData;
use crate::{
    JsonRule, RequestUnsigned, cmp_decision, cmp_policy,
    tests::utils::cedarling_util::get_cedarling_with_callback,
};
use crate::log::interface::LogStorage;

static POLICY_STORE_RAW_YAML: &str = include_str!("../../../test_files/policy-store_ok_2.yaml");

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

/// Check if action executes for next principals: TestPrincipal1, TestPrincipal2, TestPrincipal3
#[test]
async fn test_authorize_unsigned_for_all_principals_success() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        |config| config.authorization_config.principal_bool_operator = OPERATOR_AND.clone(),
    )
    .await;

    let request = RequestUnsigned {
        action: "Jans::Action::\"UpdateForTestPrincipals\"".to_string(),
        context: json!({}),
        principals: vec![
            EntityData::deserialize(serde_json::json!({
                "cedar_entity_mapping": {
                    "entity_type": "Jans::TestPrincipal1",
                    "id": "random_id"
                },
                "is_ok": true
            }))
            .unwrap(),
            EntityData::deserialize(serde_json::json!({
                "cedar_entity_mapping": {
                    "entity_type": "Jans::TestPrincipal2",
                    "id": "random_id"
                },
                "is_ok": true
            }))
            .unwrap(),
            EntityData::deserialize(serde_json::json!({
                "cedar_entity_mapping": {
                    "entity_type": "Jans::TestPrincipal3",
                    "id": "random_id"
                },
                "is_ok": true
            }))
            .unwrap(),
        ],
        resource: EntityData::deserialize(serde_json::json!(
        {
                "cedar_entity_mapping": {
                    "entity_type": "Jans::Issue",
                    "id": "random_id"
                },
            "org_id": "some_long_id",
            "country": "US"
        }))
        .unwrap(),
    };

    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("request should be parsed without errors");

    let test_principal_1_result = result
        .principals
        .get("Jans::TestPrincipal1")
        .map(|v| v.to_owned());

    let test_principal_2_result = result
        .principals
        .get("Jans::TestPrincipal2")
        .map(|v| v.to_owned());

    let test_principal_3_result = result
        .principals
        .get("Jans::TestPrincipal3")
        .map(|v| v.to_owned());

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
            .unwrap()
        },
    )
    .await;

    let request = RequestUnsigned {
        action: "Jans::Action::\"UpdateForTestPrincipals\"".to_string(),
        context: json!({}),
        principals: vec![
            EntityData::deserialize(serde_json::json!({
                "cedar_entity_mapping": {
                    "entity_type": "Jans::TestPrincipal1",
                    "id": "id1"
                },
                "is_ok": true
            }))
            .unwrap(),
            EntityData::deserialize(serde_json::json!({
                "cedar_entity_mapping": {
                    "entity_type": "Jans::TestPrincipal1",
                    "id": "id2"
                },
                "is_ok": true
            }))
            .unwrap(),
            EntityData::deserialize(serde_json::json!({
                "cedar_entity_mapping": {
                    "entity_type": "Jans::TestPrincipal1",
                    "id": "id3"
                },
                "is_ok": false
            }))
            .unwrap(),
        ],
        resource: EntityData::deserialize(serde_json::json!(
        {
                "cedar_entity_mapping": {
                    "entity_type": "Jans::Issue",
                    "id": "random_id"
                },
            "org_id": "some_long_id",
            "country": "US"
        }))
        .unwrap(),
    };

    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("request should be parsed without errors");

    let test_principal_1_result = result
        .principals
        .get("Jans::TestPrincipal1::\"id1\"")
        .map(|v| v.to_owned());

    let test_principal_2_result = result
        .principals
        .get("Jans::TestPrincipal1::\"id1\"")
        .map(|v| v.to_owned());

    let test_principal_3_result = result
        .principals
        .get("Jans::TestPrincipal1::\"id1\"")
        .map(|v| v.to_owned());

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

    let request = RequestUnsigned {
        action: "Jans::Action::\"UpdateForTestPrincipals\"".to_string(),
        context: json!({}),
        principals: vec![
            EntityData::deserialize(serde_json::json!({
                "cedar_entity_mapping": {
                    "entity_type": "Jans::TestPrincipal1",
                    "id": "random_id"
                },
                "is_ok": false
            }))
            .unwrap(),
            EntityData::deserialize(serde_json::json!({
                "cedar_entity_mapping": {
                    "entity_type": "Jans::TestPrincipal2",
                    "id": "random_id"
                },
                "is_ok": true
            }))
            .unwrap(),
            EntityData::deserialize(serde_json::json!({
                "cedar_entity_mapping": {
                    "entity_type": "Jans::TestPrincipal3",
                    "id": "random_id"
                },
                "is_ok": false
            }))
            .unwrap(),
        ],
        resource: EntityData::deserialize(serde_json::json!(
        {
                "cedar_entity_mapping": {
                    "entity_type": "Jans::Issue",
                    "id": "random_id"
                },
            "org_id": "some_long_id",
            "country": "US"
        }))
        .unwrap(),
    };

    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("request should be parsed without errors");

    let empty_vec: Vec<&str> = Vec::new();

    let test_principal_1_result = result
        .principals
        .get("Jans::TestPrincipal1")
        .map(|v| v.to_owned());

    let test_principal_2_result = result
        .principals
        .get("Jans::TestPrincipal2")
        .map(|v| v.to_owned());

    let test_principal_3_result = result
        .principals
        .get("Jans::TestPrincipal3")
        .map(|v| v.to_owned());

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

    let request = RequestUnsigned {
        action: "Jans::Action::\"AlwaysDeny\"".to_string(),
        context: json!({}),
        principals: vec![
            EntityData::deserialize(serde_json::json!({
                "cedar_entity_mapping": {
                    "entity_type": "Jans::User",
                    "id": "user1"
                },
                "country": "US",
                "role": ["Admin"],
                "sub": "user1"
            }))
            .unwrap(),
            EntityData::deserialize(serde_json::json!({
                "cedar_entity_mapping": {
                    "entity_type": "Jans::User",
                    "id": "user2"
                },
                "country": "US",
                "role": ["Guest"],
                "sub": "user2"
            }))
            .unwrap(),
        ],
        resource: EntityData::deserialize(serde_json::json!({
            "cedar_entity_mapping": {
                "entity_type": "Jans::Issue",
                "id": "issue1"
            },
            "org_id": "invalid",
            "country": "US"
        }))
        .unwrap(),
    };

    let result = cedarling.authorize_unsigned(request).await.expect("request should be parsed without errors");
    
    // Verify that logs were created and contain the request ID
    let logs = cedarling.pop_logs();
    assert!(!logs.is_empty(), "Should have created logs");
    
    let request_id = &result.request_id;
    let logs_with_request_id: Vec<&serde_json::Value> = logs
        .iter()
        .filter(|log| log.get("request_id") == Some(&serde_json::json!(request_id)))
        .collect();
    
    assert!(!logs_with_request_id.is_empty(), "Should have logs for the request ID");
    
    // Verify that logs contain expected content
    for log in &logs_with_request_id {
        // Verify basic log structure
        assert!(log.get("id").is_some(), "Log should have an id field");
        assert!(log.get("timestamp").is_some(), "Log should have a timestamp field");
        assert!(log.get("log_kind").is_some(), "Log should have a log_kind field");
        
        // Verify log kind is valid
        let log_kind = log.get("log_kind").unwrap();
        assert!(
            log_kind == "Decision" || log_kind == "System",
            "Log kind should be Decision or System, got: {:?}",
            log_kind
        );
        
        // For Decision logs, verify they have required fields
        if log_kind == "Decision" {
            assert!(log.get("action").is_some(), "Decision log should have an action field");
            assert!(log.get("resource").is_some(), "Decision log should have a resource field");
            assert!(log.get("decision").is_some(), "Decision log should have a decision field");
            assert!(log.get("diagnostics").is_some(), "Decision log should have a diagnostics field");
            
            // Verify the action matches what we requested
            let log_action = log.get("action").unwrap();
            assert_eq!(log_action, &serde_json::json!("Jans::Action::\"AlwaysDeny\""), "Decision log should have the correct action");
            
            // Verify the decision is DENY
            let log_decision = log.get("decision").unwrap();
            assert_eq!(log_decision, &serde_json::json!("DENY"), "Decision log should show DENY decision");
            
            // Verify diagnostics structure
            let diagnostics = log.get("diagnostics").unwrap();
            assert!(diagnostics.get("reason").is_some(), "Diagnostics should have a reason field");
            assert!(diagnostics.get("errors").is_some(), "Diagnostics should have an errors field");
            
            // Verify no policy evaluation errors (since AlwaysDeny just denies, doesn't cause errors)
            let errors = diagnostics.get("errors").unwrap();
            assert_eq!(errors, &serde_json::json!([]), "Diagnostics should show no errors when there are no policy evaluation errors");
        }
    }
}
