// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::BatchItem;
use crate::Cedarling;
use crate::CedarlingError;
use crate::TokenInput;
use crate::{EntityData, JsonValue};
use serde_json::json;
use std::sync::Arc;
use test_utils::token_claims::generate_token_using_claims;

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

    let principal = Some(Arc::new(
        EntityData::from_json(
            json!({
              "cedar_entity_mapping": {
                "entity_type": "Jans::TestPrincipal1",
                "id": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0"
              },
              "is_ok": true
            })
            .to_string(),
        )
        .unwrap(),
    ));

    //execute authz
    let result = cedarling
        .authorize_unsigned(
            principal,
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

fn create_test_cedarling() -> Cedarling {
    Cedarling::load_from_file(String::from(
        "../../bindings/cedarling_uniffi/test_files/bootstrap.json",
    ))
    .expect("Error in initializing Cedarling")
}

#[test]
fn test_load_from_json_with_archive_bytes_rejects_invalid() {
    let config =
        std::fs::read_to_string("../../bindings/cedarling_uniffi/test_files/bootstrap.json")
            .expect("bootstrap.json should be readable");
    let result = Cedarling::load_from_json_with_archive_bytes(config, vec![0x00, 0x01, 0x02, 0x03]);
    assert!(
        matches!(&result, Err(CedarlingError::InitializationFailed { .. })),
        "invalid archive bytes should yield InitializationFailed, is_ok={}",
        result.is_ok()
    );
}

#[test]
fn test_data_api_push_and_get() {
    let cedarling = create_test_cedarling();

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
    let value2: serde_json::Value =
        serde_json::from_str(&result2.unwrap().0).expect("result should be deserializable to JSON");
    assert_eq!(
        value2,
        serde_json::json!({"nested": "data"}),
        "retrieved nested object should match pushed value"
    );

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
    let value3: Vec<i32> = serde_json::from_str(&result3.unwrap().0)
        .expect("result should be deserializable to array");
    assert_eq!(
        value3,
        vec![1, 2, 3],
        "retrieved array should match pushed value"
    );
}

#[test]
fn test_data_api_get_data_entry_ctx() {
    let cedarling = create_test_cedarling();

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
    let cedarling = create_test_cedarling();

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
    let cedarling = create_test_cedarling();

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
    let cedarling = create_test_cedarling();

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
    let cedarling = create_test_cedarling();

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
    let cedarling = create_test_cedarling();

    let result = cedarling.push_data_ctx("".to_string(), JsonValue(r#""value""#.to_string()), None);
    result.expect_err("push_data_ctx with empty key should fail");
}

#[test]
fn test_trusted_issuer_loading_info_defaults() {
    let cedarling = create_test_cedarling();

    assert!(
        !cedarling.is_trusted_issuer_loaded_by_name("missing_issuer"),
        "unknown issuer id should not be loaded"
    );
    assert!(
        !cedarling.is_trusted_issuer_loaded_by_iss("https://missing.example.org"),
        "unknown issuer iss should not be loaded"
    );

    let total = cedarling.total_issuers();
    let loaded = cedarling.loaded_trusted_issuers_count();
    let loaded_ids = cedarling.loaded_trusted_issuer_ids();
    assert!(
        loaded <= total,
        "loaded count {loaded} should not exceed total {total}"
    );
    assert_eq!(
        loaded_ids.len() as i64,
        loaded,
        "loaded ids length should match loaded_trusted_issuers_count"
    );
    for id in &loaded_ids {
        assert!(
            cedarling.is_trusted_issuer_loaded_by_name(id),
            "loaded id {id:?} should satisfy is_trusted_issuer_loaded_by_name"
        );
    }
}

fn batch_resource(id: &str) -> Arc<EntityData> {
    Arc::new(
        EntityData::from_json(
            json!({
                "cedar_entity_mapping": { "entity_type": "Jans::Issue", "id": id },
                "app_id": "admin_ui_id",
                "name": "My App",
                "permission": "view_clients",
                "sub": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0",
            })
            .to_string(),
        )
        .expect("resource EntityData should parse"),
    )
}

fn batch_principal(is_ok: bool) -> Arc<EntityData> {
    Arc::new(
        EntityData::from_json(
            json!({
                "cedar_entity_mapping": {
                    "entity_type": "Jans::TestPrincipal1",
                    "id": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0",
                },
                "is_ok": is_ok,
            })
            .to_string(),
        )
        .expect("principal EntityData should parse"),
    )
}

fn batch_item(resource_id: &str) -> BatchItem {
    BatchItem {
        resource: batch_resource(resource_id),
        action: r#"Jans::Action::"UpdateTestPrincipal""#.to_string(),
        context: Some(JsonValue("{}".to_string())),
    }
}

#[test]
fn test_authorize_unsigned_batch_ordered_mixed_decisions() {
    // Mixed items: [ok, bad-action-fail-closed-deny, ok]. Verifies that
    // results[i] carries the decision produced by items[i] rather than a
    // uniform pass/fail.
    let cedarling = create_test_cedarling();
    let ok_item = batch_item("ok-0");
    let bad_item = BatchItem {
        resource: batch_resource("bad-1"),
        action: "this is not a valid uid".to_string(),
        context: Some(JsonValue("{}".to_string())),
    };
    let items = vec![ok_item, bad_item, batch_item("ok-2")];

    let response = cedarling
        .authorize_unsigned_batch(Some(batch_principal(true)), items)
        .expect("batch call should succeed");

    assert_eq!(response.results.len(), 3, "N=3 items → N=3 results");
    assert!(response.results[0].decision, "item 0 must allow");
    assert!(
        !response.results[1].decision,
        "item 1 with bad action must fail closed"
    );
    assert!(response.results[2].decision, "item 2 must allow");
    assert!(!response.batch_id.is_empty(), "batch_id must be populated");
}

#[test]
fn test_authorize_unsigned_batch_empty_items_rejected() {
    let cedarling = create_test_cedarling();

    let err = cedarling
        .authorize_unsigned_batch(Some(batch_principal(true)), Vec::new())
        .expect_err("empty items must be rejected");
    let msg = format!("{err:?}");
    assert!(
        msg.to_lowercase().contains("empty"),
        "error should mention empty items, got: {msg}"
    );
}

#[test]
fn test_authorize_unsigned_batch_context_defaults_when_none() {
    let cedarling = create_test_cedarling();
    let item = BatchItem {
        resource: batch_resource("no-ctx"),
        action: r#"Jans::Action::"UpdateTestPrincipal""#.to_string(),
        context: None,
    };

    let response = cedarling
        .authorize_unsigned_batch(Some(batch_principal(true)), vec![item])
        .expect("batch call should succeed");

    assert_eq!(response.results.len(), 1);
    assert!(response.results[0].decision);
}

// ── Multi-issuer batch tests ────────────────────────────────────────
//
// These tests exercise the UniFFI marshalling for the multi-issuer batch
// entry point: `Vec<TokenInput>` + `Vec<BatchItem>` in, `BatchAuthorizeMultiIssuerResponse`
// out. The actual authorization outcome (Allow/Deny) is exhaustively covered
// by the core lib tests; here we care that the boundary works and the batch_id +
// per-item results are populated with the correct arity.

fn multi_issuer_resource(id: &str) -> Arc<EntityData> {
    Arc::new(
        EntityData::from_json(
            json!({
                "cedar_entity_mapping": { "entity_type": "Jans::Issue", "id": id },
                "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
            })
            .to_string(),
        )
        .expect("resource EntityData should parse"),
    )
}

fn multi_issuer_access_token() -> TokenInput {
    let payload = generate_token_using_claims(json!({
        "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
        "iss": "https://account.gluu.org",
        "jti": "test-jti",
        "client_id": "test-client",
        "aud": "test-aud",
        "exp": 9_999_999_999_i64,
        "iat": 1_724_832_259,
    }));
    TokenInput {
        mapping: "Jans::Access_token".to_string(),
        payload,
    }
}

fn multi_issuer_item(id: &str) -> BatchItem {
    BatchItem {
        resource: multi_issuer_resource(id),
        action: r#"Jans::Action::"Update""#.to_string(),
        context: Some(JsonValue("{}".to_string())),
    }
}

#[test]
fn test_authorize_multi_issuer_batch_ordered_results() {
    let cedarling = create_test_cedarling();
    let items = (0..3).map(|i| multi_issuer_item(&format!("res-{i}"))).collect();

    let response = cedarling
        .authorize_multi_issuer_batch(vec![multi_issuer_access_token()], items)
        .expect("batch call should succeed at the UniFFI boundary");

    assert_eq!(response.results.len(), 3, "N=3 items → N=3 results");
    assert!(!response.batch_id.is_empty(), "batch_id must be populated");
}

#[test]
fn test_authorize_multi_issuer_batch_empty_tokens_rejected() {
    let cedarling = create_test_cedarling();

    let err = cedarling
        .authorize_multi_issuer_batch(Vec::new(), vec![multi_issuer_item("x")])
        .expect_err("empty tokens must be rejected");
    let msg = format!("{err:?}");
    assert!(
        msg.to_lowercase().contains("empty"),
        "error should mention empty tokens, got: {msg}"
    );
}

#[test]
fn test_authorize_multi_issuer_batch_empty_items_rejected() {
    let cedarling = create_test_cedarling();

    let err = cedarling
        .authorize_multi_issuer_batch(vec![multi_issuer_access_token()], Vec::new())
        .expect_err("empty items must be rejected");
    let msg = format!("{err:?}");
    assert!(
        msg.to_lowercase().contains("empty"),
        "error should mention empty items, got: {msg}"
    );
}

#[test]
fn test_authorize_multi_issuer_batch_context_none_defaults() {
    let cedarling = create_test_cedarling();
    let item = BatchItem {
        resource: multi_issuer_resource("no-ctx"),
        action: r#"Jans::Action::"Update""#.to_string(),
        context: None,
    };

    let response = cedarling
        .authorize_multi_issuer_batch(vec![multi_issuer_access_token()], vec![item])
        .expect("None context should default to {} and marshal cleanly");

    assert_eq!(response.results.len(), 1);
    assert!(!response.batch_id.is_empty());
}
