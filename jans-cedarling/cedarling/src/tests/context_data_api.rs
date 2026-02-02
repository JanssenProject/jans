// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Integration tests for the Data API.
//!
//! These tests verify:
//! - End-to-end authorization with pushed data
//! - Policy evaluation accessing `context.data`
//! - Multiple Cedarling instances (isolation)
//! - Data lifecycle management

use std::sync::LazyLock;
use std::time::Duration;

use serde_json::json;
use test_utils::assert_eq;
use tokio::test;

use super::utils::*;
use crate::authz::request::EntityData;
use crate::tests::utils::cedarling_util::get_cedarling_with_callback;
use crate::{DataApi, JsonRule, RequestUnsigned};

// Custom principal operator that matches our test principal
static TEST_PRINCIPAL_OPERATOR: LazyLock<JsonRule> = LazyLock::new(|| {
    JsonRule::new(json!({
        "===": [{"var": "Jans::TestPrincipal1"}, "ALLOW"]
    }))
    .unwrap()
});

// Policy store that includes policies checking context.data
static POLICY_STORE_WITH_DATA_ACCESS: &str = r#"
cedar_version: v4.0.0
policy_stores:
  data_api_test_store:
    cedar_version: v4.0.0
    name: "Data API Test Policy Store"
    description: "Test policy store for Data API integration tests"
    policies:
      allow_premium_users:
        description: "Policy that checks context.data.user_level"
        policy_content:
          encoding: none
          content_type: cedar
          body: |-
            permit(
              principal is Jans::TestPrincipal1,
              action == Jans::Action::"AccessPremiumContent",
              resource is Jans::Document
            ) when {
              context has data && context.data has user_level && context.data.user_level == "premium"
            };
      allow_feature_flag:
        description: "Policy that checks context.data.feature_flags"
        policy_content:
          encoding: none
          content_type: cedar
          body: |-
            permit(
              principal is Jans::TestPrincipal1,
              action == Jans::Action::"UseFeature",
              resource is Jans::Document
            ) when {
              context has data && context.data has feature_enabled && context.data.feature_enabled == true
            };
      allow_basic_access:
        description: "Policy that always allows basic access (no data check)"
        policy_content:
          encoding: none
          content_type: cedar
          body: |-
            permit(
              principal is Jans::TestPrincipal1,
              action == Jans::Action::"BasicAccess",
              resource is Jans::Document
            );
      allow_nested_data:
        description: "Policy that checks nested data"
        policy_content:
          encoding: none
          content_type: cedar
          body: |-
            permit(
              principal is Jans::TestPrincipal1,
              action == Jans::Action::"NestedAccess",
              resource is Jans::Document
            ) when {
              context has data && context.data has config && context.data.config has enabled && context.data.config.enabled == true
            };
    schema:
      encoding: none
      content_type: cedar
      body: |-
        namespace Jans {
          entity TestPrincipal1;
          entity Document;
          type DataConfig = {"enabled": Bool};
          type DataContext = {
            "user_level"?: String,
            "feature_enabled"?: Bool,
            "config"?: DataConfig
          };
          type Context = {
            "data"?: DataContext
          };
          action "AccessPremiumContent" appliesTo {
            principal: [TestPrincipal1],
            resource: [Document],
            context: Context
          };
          action "UseFeature" appliesTo {
            principal: [TestPrincipal1],
            resource: [Document],
            context: Context
          };
          action "BasicAccess" appliesTo {
            principal: [TestPrincipal1],
            resource: [Document],
            context: Context
          };
          action "NestedAccess" appliesTo {
            principal: [TestPrincipal1],
            resource: [Document],
            context: Context
          };
        }
"#;

fn create_test_request(action: &str) -> RequestUnsigned {
    RequestUnsigned {
        action: format!("Jans::Action::\"{action}\""),
        context: json!({}),
        principals: vec![
            EntityData::deserialize(json!({
                "cedar_entity_mapping": {
                    "entity_type": "Jans::TestPrincipal1",
                    "id": "test_user_1"
                }
            }))
            .unwrap(),
        ],
        resource: EntityData::deserialize(json!({
            "cedar_entity_mapping": {
                "entity_type": "Jans::Document",
                "id": "doc_1"
            }
        }))
        .unwrap(),
    }
}

// =============================================================================
// End-to-End Authorization with Pushed Data
// =============================================================================

#[test]
async fn test_authorization_with_pushed_data_allows_access() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_WITH_DATA_ACCESS.to_string()),
        |config| {
            config.authorization_config.principal_bool_operator = TEST_PRINCIPAL_OPERATOR.clone();
        },
    )
    .await;

    // Push data that satisfies the policy
    cedarling
        .push_data(
            "user_level",
            json!("premium"),
            Some(Duration::from_secs(60)),
        )
        .expect("push_data should succeed");

    // Verify data was pushed correctly
    let data = cedarling
        .get_data("user_level")
        .expect("get_data should succeed");
    assert_eq!(
        data,
        Some(json!("premium")),
        "Data should be stored correctly"
    );

    // Verify the authorization succeeds
    let request = create_test_request("AccessPremiumContent");
    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("authorization should succeed");

    assert!(result.decision, "Premium user should be allowed access");
}

#[test]
async fn test_authorization_without_pushed_data_denies_access() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_WITH_DATA_ACCESS.to_string()),
        |config| {
            config.authorization_config.principal_bool_operator = TEST_PRINCIPAL_OPERATOR.clone();
        },
    )
    .await;

    // Don't push any data - policy requires context.data.user_level == "premium"
    let request = create_test_request("AccessPremiumContent");
    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("authorization should succeed");

    assert!(
        !result.decision,
        "Without pushed data, access should be denied"
    );
}

#[test]
async fn test_authorization_with_wrong_data_value_denies_access() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_WITH_DATA_ACCESS.to_string()),
        |config| {
            config.authorization_config.principal_bool_operator = TEST_PRINCIPAL_OPERATOR.clone();
        },
    )
    .await;

    // Push data with wrong value
    cedarling
        .push_data("user_level", json!("basic"), Some(Duration::from_secs(60)))
        .expect("push_data should succeed");

    let request = create_test_request("AccessPremiumContent");
    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("authorization should succeed");

    assert!(
        !result.decision,
        "With wrong data value, access should be denied"
    );
}

#[test]
async fn test_authorization_with_boolean_flag() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_WITH_DATA_ACCESS.to_string()),
        |config| {
            config.authorization_config.principal_bool_operator = TEST_PRINCIPAL_OPERATOR.clone();
        },
    )
    .await;

    // Push feature flag as true
    cedarling
        .push_data(
            "feature_enabled",
            json!(true),
            Some(Duration::from_secs(60)),
        )
        .expect("push_data should succeed");

    let request = create_test_request("UseFeature");
    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("authorization should succeed");

    assert!(
        result.decision,
        "Feature should be allowed when flag is true"
    );
}

#[test]
async fn test_authorization_with_nested_data() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_WITH_DATA_ACCESS.to_string()),
        |config| {
            config.authorization_config.principal_bool_operator = TEST_PRINCIPAL_OPERATOR.clone();
        },
    )
    .await;

    // Push nested configuration data (only fields declared in schema)
    cedarling
        .push_data(
            "config",
            json!({"enabled": true}),
            Some(Duration::from_secs(60)),
        )
        .expect("push_data should succeed");

    let request = create_test_request("NestedAccess");
    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("authorization should succeed");

    assert!(
        result.decision,
        "Nested data should be accessible in policy"
    );
}

#[test]
async fn test_basic_access_without_data_requirement() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_WITH_DATA_ACCESS.to_string()),
        |config| {
            config.authorization_config.principal_bool_operator = TEST_PRINCIPAL_OPERATOR.clone();
        },
    )
    .await;

    // BasicAccess policy doesn't require any data
    let request = create_test_request("BasicAccess");
    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("authorization should succeed");

    assert!(
        result.decision,
        "Basic access should work without pushed data"
    );
}

// =============================================================================
// Multiple Cedarling Instances (Isolation)
// =============================================================================

#[test]
async fn test_multiple_instances_have_isolated_data() {
    let cedarling1 = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_WITH_DATA_ACCESS.to_string()),
        |config| {
            config.authorization_config.principal_bool_operator = TEST_PRINCIPAL_OPERATOR.clone();
        },
    )
    .await;
    let cedarling2 = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_WITH_DATA_ACCESS.to_string()),
        |config| {
            config.authorization_config.principal_bool_operator = TEST_PRINCIPAL_OPERATOR.clone();
        },
    )
    .await;

    // Push data only to cedarling1
    cedarling1
        .push_data(
            "user_level",
            json!("premium"),
            Some(Duration::from_secs(60)),
        )
        .expect("push_data should succeed");

    // cedarling1 should have the data
    let data1 = cedarling1
        .get_data("user_level")
        .expect("get_data should succeed");
    assert_eq!(
        data1,
        Some(json!("premium")),
        "cedarling1 should have the data"
    );

    // cedarling2 should NOT have the data (isolated)
    let data2 = cedarling2
        .get_data("user_level")
        .expect("get_data should succeed");
    assert_eq!(
        data2, None,
        "cedarling2 should NOT have the data (isolated)"
    );

    // Authorization on cedarling1 should succeed
    let request1 = create_test_request("AccessPremiumContent");
    let result1 = cedarling1
        .authorize_unsigned(request1)
        .await
        .expect("authorization should succeed");
    assert!(result1.decision, "cedarling1 should allow access");

    // Authorization on cedarling2 should fail (no data)
    let request2 = create_test_request("AccessPremiumContent");
    let result2 = cedarling2
        .authorize_unsigned(request2)
        .await
        .expect("authorization should succeed");
    assert!(!result2.decision, "cedarling2 should deny access (no data)");
}

// =============================================================================
// Data Lifecycle Management
// =============================================================================

#[test]
async fn test_data_removal_affects_authorization() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_WITH_DATA_ACCESS.to_string()),
        |config| {
            config.authorization_config.principal_bool_operator = TEST_PRINCIPAL_OPERATOR.clone();
        },
    )
    .await;

    // Push data
    cedarling
        .push_data(
            "user_level",
            json!("premium"),
            Some(Duration::from_secs(60)),
        )
        .expect("push_data should succeed");

    // First authorization should succeed
    let request1 = create_test_request("AccessPremiumContent");
    let result1 = cedarling
        .authorize_unsigned(request1)
        .await
        .expect("authorization should succeed");
    assert!(result1.decision, "Should allow access with data");

    // Remove the data
    let removed = cedarling
        .remove_data("user_level")
        .expect("remove should succeed");
    assert!(removed, "Data should have been removed");

    // Second authorization should fail
    let request2 = create_test_request("AccessPremiumContent");
    let result2 = cedarling
        .authorize_unsigned(request2)
        .await
        .expect("authorization should succeed");
    assert!(!result2.decision, "Should deny access after data removal");
}

#[test]
async fn test_data_clear_affects_authorization() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_WITH_DATA_ACCESS.to_string()),
        |config| {
            config.authorization_config.principal_bool_operator = TEST_PRINCIPAL_OPERATOR.clone();
        },
    )
    .await;

    // Push multiple data entries
    cedarling
        .push_data(
            "user_level",
            json!("premium"),
            Some(Duration::from_secs(60)),
        )
        .expect("push_data should succeed");
    cedarling
        .push_data(
            "feature_enabled",
            json!(true),
            Some(Duration::from_secs(60)),
        )
        .expect("push_data should succeed");

    // Verify data exists
    let stats = cedarling.get_stats().expect("get_stats should succeed");
    assert_eq!(stats.entry_count, 2, "Should have 2 entries");

    // Clear all data
    cedarling.clear_data().expect("clear should succeed");

    // Verify data is gone
    let stats_after = cedarling.get_stats().expect("get_stats should succeed");
    assert_eq!(
        stats_after.entry_count, 0,
        "Should have 0 entries after clear"
    );

    // Authorization should now fail
    let request = create_test_request("AccessPremiumContent");
    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("authorization should succeed");
    assert!(!result.decision, "Should deny access after clear");
}

#[test]
async fn test_data_update_affects_authorization() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_WITH_DATA_ACCESS.to_string()),
        |config| {
            config.authorization_config.principal_bool_operator = TEST_PRINCIPAL_OPERATOR.clone();
        },
    )
    .await;

    // Push initial data (basic user)
    cedarling
        .push_data("user_level", json!("basic"), Some(Duration::from_secs(60)))
        .expect("push_data should succeed");

    // Authorization should fail (not premium)
    let request1 = create_test_request("AccessPremiumContent");
    let result1 = cedarling
        .authorize_unsigned(request1)
        .await
        .expect("authorization should succeed");
    assert!(!result1.decision, "Basic user should be denied");

    // Update to premium
    cedarling
        .push_data(
            "user_level",
            json!("premium"),
            Some(Duration::from_secs(60)),
        )
        .expect("push_data update should succeed");

    // Authorization should now succeed
    let request2 = create_test_request("AccessPremiumContent");
    let result2 = cedarling
        .authorize_unsigned(request2)
        .await
        .expect("authorization should succeed");
    assert!(
        result2.decision,
        "Premium user should be allowed after update"
    );
}

#[test]
async fn test_data_list_and_stats() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_WITH_DATA_ACCESS.to_string()),
        |config| {
            config.authorization_config.principal_bool_operator = TEST_PRINCIPAL_OPERATOR.clone();
        },
    )
    .await;

    // Push multiple entries
    cedarling
        .push_data("key1", json!("value1"), Some(Duration::from_secs(60)))
        .expect("push should succeed");
    cedarling
        .push_data(
            "key2",
            json!({"nested": true}),
            Some(Duration::from_secs(60)),
        )
        .expect("push should succeed");
    cedarling
        .push_data("key3", json!([1, 2, 3]), Some(Duration::from_secs(60)))
        .expect("push should succeed");

    // Check stats
    let stats = cedarling.get_stats().expect("get_stats should succeed");
    assert_eq!(stats.entry_count, 3, "Should have 3 entries");
    assert!(stats.total_size_bytes > 0, "Total size should be positive");
    assert!(
        stats.avg_entry_size_bytes > 0,
        "Average size should be positive"
    );

    // List entries
    let entries = cedarling.list_data().expect("list_data should succeed");
    assert_eq!(entries.len(), 3, "Should list 3 entries");

    // Verify keys are present
    let keys: Vec<&str> = entries.iter().map(|e| e.key.as_str()).collect();
    assert!(keys.contains(&"key1"), "Should contain key1");
    assert!(keys.contains(&"key2"), "Should contain key2");
    assert!(keys.contains(&"key3"), "Should contain key3");
}

#[test]
async fn test_get_data_entry_with_metadata() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_WITH_DATA_ACCESS.to_string()),
        |config| {
            config.authorization_config.principal_bool_operator = TEST_PRINCIPAL_OPERATOR.clone();
        },
    )
    .await;

    // Push an entry
    cedarling
        .push_data(
            "test_key",
            json!("test_value"),
            Some(Duration::from_secs(300)),
        )
        .expect("push should succeed");

    // Get entry with metadata
    let entry = cedarling
        .get_data_entry("test_key")
        .expect("get_data_entry should succeed")
        .expect("entry should exist");

    assert_eq!(entry.key, "test_key", "Key should match");
    assert_eq!(entry.value, json!("test_value"), "Value should match");
    // created_at is always set (not an Option)
    assert!(
        entry.created_at.timestamp() > 0,
        "Created time should be valid"
    );
    assert!(entry.expires_at.is_some(), "Expiration time should be set");
    assert!(entry.access_count >= 1, "Access count should be at least 1");
}
