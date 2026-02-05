// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::Cedarling;
use crate::{EntityData, JsonValue};
use serde_json::json;
use std::{collections::HashMap, sync::Arc};
use test_utils::token_claims::generate_token_using_claims;

#[test]
fn test_authorize_success_with_tokens() {
    //reading bootstra.json and instantiate cedarling
    let cedarling = Cedarling::load_from_file(String::from(
        "../../bindings/cedarling_uniffi/test_files/bootstrap.json",
    ))
    .expect("Error in initializing Cedarling");

    let resource = Arc::new(
        EntityData::from_json(
            r#"
  {
           "cedar_entity_mapping": {
             "entity_type": "Jans::Issue",
             "id": "some_id"
           },
          "app_id": "admin_ui_id",
          "name": "My App",
          "permission": "view_clients",
          "sub": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0"
  }
"#
            .to_string(),
        )
        .expect("EntityData should be correctly parsed"),
    );

    //execute authz
    let result = cedarling
        .authorize(
            HashMap::from([
                (
                    "access_token".to_string(),
                    generate_token_using_claims(json!({
                      "iss": "https://account.gluu.org",
                      "aud": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62",
                      "sub": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0",
                      "exp": 1732121460,
                      "nbf": 1731953030,
                      "iat": 1731953030,
                      "code": "3e2a2012-099c-464f-890b-448160c2ab25",
                      "token_type": "Bearer",
                      "client_id": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62",
                      "acr": "simple_password_auth",
                      "x5t#S256": "",
                      "scope": [
                        "role",
                        "openid",
                        "profile",
                        "email"
                      ],
                      "auth_time": 1731953027,
                      "jti": "uZUh1hDUQo6PFkBPnwpGzg",
                      "username": "Default Admin User",
                      "status": {
                        "status_list": {
                          "idx": 306,
                          "uri": "https://jans.test/jans-auth/restv1/status_list"
                        }
                      }
                    })),
                ),
                (
                    "id_token".to_string(),
                    generate_token_using_claims(json!({
                      "iss": "https://account.gluu.org",
                      "sub": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0",
                      "aud": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62",
                      "exp": 1731956630,
                      "nbf": 1731953030,
                      "iat": 1731953030,
                      "jti": "ijLZO1ooRyWrgIn7cIdNyA",
                      "at_hash": "bxaCT0ZQXbv4sbzjSDrNiA",
                      "amr": [],
                      "nonce": "25b2b16b-32a2-42d6-8a8e-e5fa9ab888c0",
                      "sid": "6d443734-b7a2-4ed8-9d3a-1606d2f99244",
                      "jansOpenIDConnectVersion": "openidconnect-1.0",
                      "acr": "simple_password_auth",
                      "c_hash": "V8h4sO9NzuLKawPO-3DNLA",
                      "auth_time": 1731953027,
                      "grant": "authorization_code",
                      "status": {
                        "status_list": {
                          "idx": 307,
                          "uri": "https://jans.test/jans-auth/restv1/status_list"
                        }
                      }
                    })),
                ),
                (
                    "userinfo_token".to_string(),
                    generate_token_using_claims(json!({
                      "iss": "https://account.gluu.org",
                      "sub": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0",
                      "aud": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62",
                      "exp": 1724945978,
                      "iat": 1724832259,
                      "jti": "OIn3g1SPSDSKAYDzENVoug",
                      "email_verified": true,
                      "role": [
                        "CasaAdmin"
                      ],
                      "given_name": "Admin",
                      "middle_name": "Admin",
                      "inum": "a6a70301-af49-4901-9687-0bcdcf4e34fa",
                      "updated_at": 1731698135,
                      "name": "Default Admin User",
                      "nickname": "Admin",
                      "family_name": "User",
                      "email": "admin@jans.test",
                      "jansAdminUIRole": [
                        "api-admin"
                      ],
                      "username": "admin",
                      "acr": "pass",
                      "amr": [
                        "pass"
                      ]
                    })),
                ),
            ]),
            r#"Jans::Action::"Update""#.to_string(),
            resource,
            JsonValue("{}".to_string()),
        )
        .expect("Should be executed successfully.");

    assert!(
        result.decision,
        "authz result should be ALLOW: {:?}",
        result
    );
}

#[test]
fn test_authorize_unsigned_success() {
    //reading bootstra.json and instantiate cedarling
    let cedarling = Cedarling::load_from_file(String::from(
        "../../bindings/cedarling_uniffi/test_files/bootstrap.json",
    ))
    .expect("Error in initializing Cedarling");

    let resource = Arc::new(
        EntityData::from_json(
            r#"
        {
          "cedar_entity_mapping": {
            "entity_type": "Jans::Issue",
            "id": "some_id"
          },
          "app_id": "admin_ui_id",
          "name": "My App",
          "permission": "view_clients",
          "sub": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0"
        }
        "#
            .to_string(),
        )
        .expect("EntityData should be correctly parsed"),
    );

    let principals = [
        json!({
          "cedar_entity_mapping": {
            "entity_type": "Jans::TestPrincipal1",
            "id": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0"
          },
          "is_ok": true
        }),
        json!({
          "cedar_entity_mapping": {
            "entity_type": "Jans::TestPrincipal2",
            "id": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYkt1"
          },
          "is_ok": true
        }),
    ];

    //execute authz
    let result = cedarling
        .authorize_unsigned(
            principals
                .into_iter()
                .map(|json_value| Arc::new(EntityData::from_json(json_value.to_string()).unwrap()))
                .collect(),
            r#"Jans::Action::"UpdateTestPrincipal""#.to_string(),
            resource,
            JsonValue("{}".to_string()),
        )
        .expect("Should be executed successfully.");

    assert!(
        result.decision,
        "authz result should be ALLOW: {:?}",
        result
    );
}

#[test]
fn test_data_api_push_and_get() {
    let cedarling = Cedarling::load_from_file(String::from(
        "../../bindings/cedarling_uniffi/test_files/bootstrap.json",
    ))
    .expect("Error in initializing Cedarling");

    // Push data without TTL
    cedarling
        .push_data_ctx(
            "key1".to_string(),
            JsonValue(r#""value1""#.to_string()),
            None,
        )
        .expect("push_data_ctx should succeed");

    let result = cedarling
        .get_data_ctx("key1".to_string())
        .expect("get_data_ctx should succeed");
    assert!(result.is_some(), "result should not be None");
    let value: String = serde_json::from_str(&result.unwrap().0)
        .expect("result should be deserializable to string");
    assert_eq!(value, "value1", "retrieved value should match pushed value");

    // Push data with TTL
    cedarling
        .push_data_ctx(
            "key2".to_string(),
            JsonValue(r#"{"nested": "data"}"#.to_string()),
            Some(60),
        )
        .expect("push_data_ctx with TTL should succeed");

    let result2 = cedarling
        .get_data_ctx("key2".to_string())
        .expect("get_data_ctx should succeed");
    assert!(result2.is_some(), "result should not be None");

    // Push array
    cedarling
        .push_data_ctx(
            "key3".to_string(),
            JsonValue(r#"[1, 2, 3]"#.to_string()),
            None,
        )
        .expect("push_data_ctx should succeed");

    let result3 = cedarling
        .get_data_ctx("key3".to_string())
        .expect("get_data_ctx should succeed");
    assert!(result3.is_some(), "result should not be None");
}

#[test]
fn test_data_api_get_data_entry_ctx() {
    let cedarling = Cedarling::load_from_file(String::from(
        "../../bindings/cedarling_uniffi/test_files/bootstrap.json",
    ))
    .expect("Error in initializing Cedarling");

    cedarling
        .push_data_ctx(
            "test_key".to_string(),
            JsonValue(r#"{"foo": "bar"}"#.to_string()),
            None,
        )
        .expect("push_data_ctx should succeed");

    let entry = cedarling
        .get_data_entry_ctx("test_key".to_string())
        .expect("get_data_entry_ctx should succeed");
    assert!(entry.is_some(), "entry should not be None");

    let entry = entry.unwrap();
    assert_eq!(entry.key, "test_key", "entry key should match");
    assert!(
        !entry.created_at.is_empty(),
        "created_at should not be empty"
    );
}

#[test]
fn test_data_api_remove_data_ctx() {
    let cedarling = Cedarling::load_from_file(String::from(
        "../../bindings/cedarling_uniffi/test_files/bootstrap.json",
    ))
    .expect("Error in initializing Cedarling");

    cedarling
        .push_data_ctx(
            "to_remove".to_string(),
            JsonValue(r#""data""#.to_string()),
            None,
        )
        .expect("push_data_ctx should succeed");

    let result = cedarling
        .get_data_ctx("to_remove".to_string())
        .expect("get_data_ctx should succeed");
    assert!(result.is_some(), "data should exist before removal");

    let removed = cedarling
        .remove_data_ctx("to_remove".to_string())
        .expect("remove_data_ctx should succeed");
    assert!(
        removed,
        "remove_data_ctx should return true for existing key"
    );

    let result_after = cedarling
        .get_data_ctx("to_remove".to_string())
        .expect("get_data_ctx should succeed");
    assert!(result_after.is_none(), "data should be None after removal");

    // Try removing non-existent key
    let removed_nonexistent = cedarling
        .remove_data_ctx("non_existent".to_string())
        .expect("remove_data_ctx should succeed");
    assert!(
        !removed_nonexistent,
        "remove_data_ctx should return false for non-existent key"
    );
}

#[test]
fn test_data_api_clear_data_ctx() {
    let cedarling = Cedarling::load_from_file(String::from(
        "../../bindings/cedarling_uniffi/test_files/bootstrap.json",
    ))
    .expect("Error in initializing Cedarling");

    cedarling
        .push_data_ctx(
            "key1".to_string(),
            JsonValue(r#""value1""#.to_string()),
            None,
        )
        .expect("push_data_ctx should succeed");
    cedarling
        .push_data_ctx(
            "key2".to_string(),
            JsonValue(r#""value2""#.to_string()),
            None,
        )
        .expect("push_data_ctx should succeed");
    cedarling
        .push_data_ctx(
            "key3".to_string(),
            JsonValue(r#""value3""#.to_string()),
            None,
        )
        .expect("push_data_ctx should succeed");

    assert!(
        cedarling
            .get_data_ctx("key1".to_string())
            .unwrap()
            .is_some(),
        "key1 should exist"
    );
    assert!(
        cedarling
            .get_data_ctx("key2".to_string())
            .unwrap()
            .is_some(),
        "key2 should exist"
    );
    assert!(
        cedarling
            .get_data_ctx("key3".to_string())
            .unwrap()
            .is_some(),
        "key3 should exist"
    );

    cedarling
        .clear_data_ctx()
        .expect("clear_data_ctx should succeed");

    assert!(
        cedarling
            .get_data_ctx("key1".to_string())
            .unwrap()
            .is_none(),
        "key1 should be None after clear"
    );
    assert!(
        cedarling
            .get_data_ctx("key2".to_string())
            .unwrap()
            .is_none(),
        "key2 should be None after clear"
    );
    assert!(
        cedarling
            .get_data_ctx("key3".to_string())
            .unwrap()
            .is_none(),
        "key3 should be None after clear"
    );
}

#[test]
fn test_data_api_list_data_ctx() {
    let cedarling = Cedarling::load_from_file(String::from(
        "../../bindings/cedarling_uniffi/test_files/bootstrap.json",
    ))
    .expect("Error in initializing Cedarling");

    cedarling
        .push_data_ctx(
            "key1".to_string(),
            JsonValue(r#""value1""#.to_string()),
            None,
        )
        .expect("push_data_ctx should succeed");
    cedarling
        .push_data_ctx(
            "key2".to_string(),
            JsonValue(r#"{"nested": "data"}"#.to_string()),
            None,
        )
        .expect("push_data_ctx should succeed");
    cedarling
        .push_data_ctx(
            "key3".to_string(),
            JsonValue(r#"[1, 2, 3]"#.to_string()),
            None,
        )
        .expect("push_data_ctx should succeed");

    let entries = cedarling
        .list_data_ctx()
        .expect("list_data_ctx should succeed");
    assert_eq!(entries.len(), 3, "should have 3 entries");

    let keys: Vec<String> = entries.iter().map(|e| e.key.clone()).collect();
    assert!(
        keys.contains(&"key1".to_string()),
        "entries should contain key1"
    );
    assert!(
        keys.contains(&"key2".to_string()),
        "entries should contain key2"
    );
    assert!(
        keys.contains(&"key3".to_string()),
        "entries should contain key3"
    );
}

#[test]
fn test_data_api_get_stats_ctx() {
    let cedarling = Cedarling::load_from_file(String::from(
        "../../bindings/cedarling_uniffi/test_files/bootstrap.json",
    ))
    .expect("Error in initializing Cedarling");

    let stats = cedarling
        .get_stats_ctx()
        .expect("get_stats_ctx should succeed");
    assert_eq!(stats.entry_count, 0, "initial entry count should be 0");

    cedarling
        .push_data_ctx(
            "key1".to_string(),
            JsonValue(r#""value1""#.to_string()),
            None,
        )
        .expect("push_data_ctx should succeed");
    cedarling
        .push_data_ctx(
            "key2".to_string(),
            JsonValue(r#""value2""#.to_string()),
            None,
        )
        .expect("push_data_ctx should succeed");

    let stats_after = cedarling
        .get_stats_ctx()
        .expect("get_stats_ctx should succeed");
    assert_eq!(
        stats_after.entry_count, 2,
        "entry count should be 2 after pushing data"
    );
}

#[test]
fn test_data_api_invalid_key() {
    let cedarling = Cedarling::load_from_file(String::from(
        "../../bindings/cedarling_uniffi/test_files/bootstrap.json",
    ))
    .expect("Error in initializing Cedarling");

    let result = cedarling.push_data_ctx("".to_string(), JsonValue(r#""value""#.to_string()), None);
    assert!(result.is_err(), "push_data_ctx with empty key should fail");
}
