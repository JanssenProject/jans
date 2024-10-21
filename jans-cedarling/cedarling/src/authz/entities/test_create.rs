/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

//! Testing the creating entities

use test_utils::assert_eq;

use crate::authz::token_data::{GetTokenClaimValue, TokenPayload};
use crate::common::cedar_schema::CedarSchemaJson;

use super::create::*;
use test_utils::SortedJson;

// test all successful cases
// with empty namespace
#[test]
fn successful_scenario_empty_namespace() {
    let schema_json = include_str!("test_create_data/successful_scenario_schema.json");
    let schema: CedarSchemaJson = serde_json::from_str(schema_json).unwrap();

    let metadata = EntityMetadata::new("Test", "test_id_key");

    let json = serde_json::json!( {
        "test_id_key": "test_id",
        "string_key": "test string value",
        "long_key": 12345,
        "entity_uid_key": "unique_id",
        "bool_key": true,
    });

    let payload: TokenPayload = serde_json::from_value(json).unwrap();

    let entity = metadata
        .create_entity(&schema, &payload)
        .expect("entity should be created");

    let entity_json = entity.to_json_value().expect("should serialize to json");

    let expected = serde_json::json!({
        "uid": {
            "type": "Test",
            "id": "test_id"
        },
        "attrs": {
            "entity_uid_key": {
                "__entity": {
                    "type": "Test2",
                    "id": "unique_id"
                }
            },
            "long_key": 12345,
            "string_key": "test string value",
            "bool_key": true
        },
        "parents": []
    });

    assert_eq!(expected.sorted(), entity_json.sorted());
}

// test all successful cases
// with empty namespace
#[test]
fn successful_scenario_not_empty_namespace() {
    let schema_json = include_str!("test_create_data/successful_scenario_schema.json");
    let schema: CedarSchemaJson = serde_json::from_str(schema_json).unwrap();

    let metadata = EntityMetadata::new("Jans::Test", "test_id_key");

    let json = serde_json::json!( {
        "test_id_key": "test_id",
        "string_key": "test string value",
        "long_key": 12345,
        "entity_uid_key": "unique_id",
        "bool_key": true,
    });

    let payload: TokenPayload = serde_json::from_value(json).unwrap();

    let entity = metadata
        .create_entity(&schema, &payload)
        .expect("entity should be created");

    let entity_json = entity.to_json_value().expect("should serialize to json");

    let expected = serde_json::json!({
        "uid": {
            "type": "Jans::Test",
            "id": "test_id"
        },
        "attrs": {
            "entity_uid_key": {
                "__entity": {
                    "type": "Jans::Test2",
                    "id": "unique_id"
                }
            },
            "long_key": 12345,
            "string_key": "test string value",
            "bool_key": true
        },
        "parents": []
    });

    assert_eq!(expected.sorted(), entity_json.sorted());
}

/// test wrong string type in token payload
#[test]
fn get_token_claim_type_string_error() {
    let schema_json = include_str!("test_create_data/successful_scenario_schema.json");
    let schema: CedarSchemaJson = serde_json::from_str(schema_json).unwrap();

    let metadata = EntityMetadata::new("Test", "test_id_key");

    let test_key = "string_key";
    let json = serde_json::json!( {
        "test_id_key": "test_id",
        // This will trigger the type error, because it's not a String.
        test_key: 123,
        "long_key": 12345,
        "entity_uid_key": "unique_id",
        "bool_key": true,
    });

    let payload: TokenPayload = serde_json::from_value(json.clone()).unwrap();

    let entity_creation_error = metadata
        .create_entity(&schema, &payload)
        .expect_err("entity creating should throw error");

    if let CedarPolicyCreateTypeError::GetTokenClaimValue(GetTokenClaimValue::KeyNotCorrectType {
        key,
        got_type,
        ..
    }) = entity_creation_error
    {
        let json_attr_value = json.as_object().unwrap().get(test_key).unwrap();
        let origin_type = GetTokenClaimValue::json_value_type_name(json_attr_value);

        assert!(key == test_key, "expected key: {test_key}, but got: {key}");
        assert!(
            got_type == origin_type,
            "expected type: {origin_type}, but got: {got_type}"
        );
    } else {
        panic!("expected error type: CedarPolicyCreateTypeError::GetTokenClaimValue(GetTokenClaimValue::KeyNotCorrectType), but got: {entity_creation_error}");
    }
}

/// test wrong long type in token payload
#[test]
fn get_token_claim_type_long_error() {
    let schema_json = include_str!("test_create_data/successful_scenario_schema.json");
    let schema: CedarSchemaJson = serde_json::from_str(schema_json).unwrap();

    let metadata = EntityMetadata::new("Test", "test_id_key");

    let test_key = "long_key";
    let json = serde_json::json!( {
        "test_id_key": "test_id",
        "string_key": "test string",
        // This will trigger the type error, because it's not an i64.
        "long_key": "str",
        "entity_uid_key": "unique_id",
        "bool_key": true,
    });

    let payload: TokenPayload = serde_json::from_value(json.clone()).unwrap();

    let entity_creation_error = metadata
        .create_entity(&schema, &payload)
        .expect_err("entity creating should throw error");

    if let CedarPolicyCreateTypeError::GetTokenClaimValue(GetTokenClaimValue::KeyNotCorrectType {
        key,
        got_type,
        ..
    }) = entity_creation_error
    {
        let json_attr_value = json.as_object().unwrap().get(test_key).unwrap();
        let origin_type = GetTokenClaimValue::json_value_type_name(json_attr_value);

        assert!(key == test_key, "expected key: {test_key}, but got: {key}");
        assert!(
            got_type == origin_type,
            "expected type: {origin_type}, but got: {got_type}"
        );
    } else {
        panic!("expected error type: CedarPolicyCreateTypeError::GetTokenClaimValue(GetTokenClaimValue::KeyNotCorrectType), but got: {entity_creation_error}");
    }
}

/// test wrong entity_uid type in token payload
#[test]
fn get_token_claim_type_entity_uid_error() {
    let schema_json = include_str!("test_create_data/successful_scenario_schema.json");
    let schema: CedarSchemaJson = serde_json::from_str(schema_json).unwrap();

    let metadata = EntityMetadata::new("Test", "test_id_key");

    let test_key = "entity_uid_key";
    let json = serde_json::json!( {
        "test_id_key": "test_id",
        "string_key": "test string",
        "long_key": 1234,
        // This will trigger the type error, because it's not a String.
        "entity_uid_key": 123,
        "bool_key": true,
    });

    let payload: TokenPayload = serde_json::from_value(json.clone()).unwrap();

    let entity_creation_error = metadata
        .create_entity(&schema, &payload)
        .expect_err("entity creating should throw error");

    if let CedarPolicyCreateTypeError::GetTokenClaimValue(GetTokenClaimValue::KeyNotCorrectType {
        key,
        got_type,
        ..
    }) = entity_creation_error
    {
        let json_attr_value = json.as_object().unwrap().get(test_key).unwrap();
        let origin_type = GetTokenClaimValue::json_value_type_name(json_attr_value);

        assert!(key == test_key, "expected key: {test_key}, but got: {key}");
        assert!(
            got_type == origin_type,
            "expected type: {origin_type}, but got: {got_type}"
        );
    } else {
        panic!("expected error type: CedarPolicyCreateTypeError::GetTokenClaimValue(GetTokenClaimValue::KeyNotCorrectType), but got: {entity_creation_error}");
    }
}

/// test wrong boolean type in token payload
#[test]
fn get_token_claim_type_boolean_error() {
    let schema_json = include_str!("test_create_data/successful_scenario_schema.json");
    let schema: CedarSchemaJson = serde_json::from_str(schema_json).unwrap();

    let metadata = EntityMetadata::new("Test", "test_id_key");

    let test_key = "bool_key";
    let json = serde_json::json!( {
        "test_id_key": "test_id",
        "string_key": "test string",
        "long_key": 1234,
        "entity_uid_key": "ff910f15-d5a4-4227-828e-11cb8463f1b7",
        // This will trigger the type error, because it's not a bool.
        "bool_key": 123,
    });

    let payload: TokenPayload = serde_json::from_value(json.clone()).unwrap();

    let entity_creation_error = metadata
        .create_entity(&schema, &payload)
        .expect_err("entity creating should throw error");

    if let CedarPolicyCreateTypeError::GetTokenClaimValue(GetTokenClaimValue::KeyNotCorrectType {
        key,
        got_type,
        expected_type,
    }) = entity_creation_error
    {
        let json_attr_value = json.as_object().unwrap().get(test_key).unwrap();
        let origin_type = GetTokenClaimValue::json_value_type_name(json_attr_value);

        assert!(
            key == test_key,
            "expected key: {test_key}, but got: {key} with schema expected_type: {expected_type}"
        );
        assert!(
            got_type == origin_type,
            "expected type: {origin_type}, but got: {got_type} with schema expected_type: {expected_type}"
        );
    } else {
        panic!("expected error type: CedarPolicyCreateTypeError::GetTokenClaimValue(GetTokenClaimValue::KeyNotCorrectType), but got: {entity_creation_error}");
    }
}

/// create entity with wrong cedar typename
#[test]
fn get_token_claim_cedar_typename_error() {
    let schema_json = include_str!("test_create_data/successful_scenario_schema.json");
    let schema: CedarSchemaJson = serde_json::from_str(schema_json).unwrap();

    // Mistake in entity type name, should be `"Jans::Test"`, it will trigger error
    let metadata = EntityMetadata::new("Jans:::Test", "test_id_key");

    let json = serde_json::json!( {
        "test_id_key": "test_id",
        "string_key": "test string value",
        "long_key": 12345,
        "entity_uid_key": "unique_id",
        "bool_key": true,
    });

    let payload: TokenPayload = serde_json::from_value(json).unwrap();

    let entity_creation_error = metadata
        .create_entity(&schema, &payload)
        .expect_err("entity creating should throw error");

    if let CedarPolicyCreateTypeError::EntityTypeName(typename, _) = &entity_creation_error {
        assert_eq!("Jans:::Test", typename);
    } else {
        panic!(
            "error should be CedarPolicyCreateTypeError::EntityTypeName, but got {:?}",
            entity_creation_error
        );
    }
}

/// create entity with wrong cedar typename in the attribute
// The JSON schema contains an error.r:
//
// "entity_uid_key": {
//     "type": "EntityOrCommon",
//     "name": ":Test2"
// },
//
// ":Test2" is not correct type definition, it will trigger error
#[test]
fn get_token_claim_cedar_typename_in_attr_error() {
    let schema_json = include_str!("test_create_data/type_error_schema.json");
    let schema: CedarSchemaJson = serde_json::from_str(schema_json).unwrap();

    let metadata = EntityMetadata::new("Jans::Test", "test_id_key");

    let json = serde_json::json!( {
        "test_id_key": "test_id",
        "string_key": "test string value",
        "long_key": 12345,
        "entity_uid_key": "unique_id",
        "bool_key": true,
    });

    let payload: TokenPayload = serde_json::from_value(json).unwrap();

    let entity_creation_error = metadata
        .create_entity(&schema, &payload)
        .expect_err("entity creating should throw error");

    if let CedarPolicyCreateTypeError::EntityTypeName(typename, _) = &entity_creation_error {
        assert_eq!("Jans:::Test2", typename);
    } else {
        panic!(
            "error should be CedarPolicyCreateTypeError::EntityTypeName, but got {:?}",
            entity_creation_error
        );
    }
}
