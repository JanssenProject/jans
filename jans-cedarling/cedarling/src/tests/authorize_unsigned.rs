// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::sync::LazyLock;

use test_utils::assert_eq;
use tokio::test;

use super::utils::*;
use crate::{
    EntityData, JsonRule, RequestUnsigned, cmp_decision, cmp_policy,
    tests::utils::cedarling_util::get_cedarling_with_callback,
}; /* macros is defined in the cedarling\src\tests\utils\cedarling_util.rs */

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
            EntityData::deserialize(serde_json::json!(
                {
                "id": "random_id",
                "type": "Jans::TestPrincipal1",
                "is_ok": true
            }))
            .unwrap(),
            EntityData::deserialize(serde_json::json!(
                {
                "id": "random_id",
                "type": "Jans::TestPrincipal2",
                "is_ok": true
            }))
            .unwrap(),
            EntityData::deserialize(serde_json::json!(
                {
                "id": "random_id",
                "type": "Jans::TestPrincipal3",
                "is_ok": true
            }))
            .unwrap(),
        ],
        resource: EntityData::deserialize(serde_json::json!(
        {
            "id": "random_id",
            "type": "Jans::Issue",
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
        vec!["5"],
        "reason of permit should be '5' for TestPrincipal1"
    );

    cmp_decision!(
        test_principal_2_result,
        Decision::Allow,
        "request result should be allowed for TestPrincipal2"
    );

    cmp_policy!(
        test_principal_2_result,
        vec!["5"],
        "reason of permit should be '5' for TestPrincipal2"
    );

    cmp_decision!(
        test_principal_3_result,
        Decision::Allow,
        "request result should be allowed for TestPrincipal3"
    );

    cmp_policy!(
        test_principal_3_result,
        vec!["5"],
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
            EntityData::deserialize(serde_json::json!(
                {
                "id": "id1",
                "type": "Jans::TestPrincipal1",
                "is_ok": true
            }))
            .unwrap(),
            EntityData::deserialize(serde_json::json!(
                {
                "id": "id2",
                "type": "Jans::TestPrincipal1",
                "is_ok": true
            }))
            .unwrap(),
            EntityData::deserialize(serde_json::json!(
                {
                "id": "id3",
                "type": "Jans::TestPrincipal1",
                "is_ok": false
            }))
            .unwrap(),
        ],
        resource: EntityData::deserialize(serde_json::json!(
        {
            "id": "random_id",
            "type": "Jans::Issue",
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
        vec!["5"],
        "reason of permit should be '5' for TestPrincipal1"
    );

    cmp_decision!(
        test_principal_2_result,
        Decision::Allow,
        "request result should be allowed for TestPrincipal2"
    );

    cmp_policy!(
        test_principal_2_result,
        vec!["5"],
        "reason of permit should be '5' for TestPrincipal2"
    );

    cmp_decision!(
        test_principal_3_result,
        Decision::Allow,
        "request result should be allowed for TestPrincipal3"
    );

    cmp_policy!(
        test_principal_3_result,
        vec!["5"],
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
            EntityData::deserialize(serde_json::json!(
                {
                "id": "random_id",
                "type": "Jans::TestPrincipal1",
                "is_ok": false
            }))
            .unwrap(),
            EntityData::deserialize(serde_json::json!(
                {
                "id": "random_id",
                "type": "Jans::TestPrincipal2",
                "is_ok": true
            }))
            .unwrap(),
            EntityData::deserialize(serde_json::json!(
                {
                "id": "random_id",
                "type": "Jans::TestPrincipal3",
                "is_ok": false
            }))
            .unwrap(),
        ],
        resource: EntityData::deserialize(serde_json::json!(
        {
            "id": "random_id",
            "type": "Jans::Issue",
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
        vec!["5"],
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
