// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Testing the creating entities

use std::collections::HashSet;

use test_utils::{SortedJson, assert_eq};

use super::create::*;
use crate::common::cedar_schema::CedarSchemaJson;
use crate::jwt::{Token, TokenClaimTypeError, TokenClaims};

// test all successful cases
// with empty namespace
#[test]
fn successful_scenario_empty_namespace() {
    let schema_json = include_str!("test_create_data/successful_scenario_schema.json");
    let schema: CedarSchemaJson = serde_json::from_str(schema_json).unwrap();

    let metadata = EntityMetadata::new(
        EntityParsedTypeName {
            namespace: "",
            type_name: "Test",
        },
        "test_id_key",
    );

    let json = serde_json::json!( {
        "test_id_key": "test_id",
        "string_key": "test string value",
        "long_key": 12345,
        "entity_uid_key": "unique_id",
        "bool_key": true,
        "set_key": ["some_string"],
        "set_set_key": [["some_string"]]
    });

    let payload: TokenClaims = serde_json::from_value(json).unwrap();

    let entity = metadata
        .create_entity(
            &schema,
            &Token::new_id(payload, None),
            HashSet::new(),
            &Default::default(),
        )
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
            "bool_key": true,
            "set_key": ["some_string"],
            "set_set_key": [["some_string"]]
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

    let metadata = EntityMetadata::new(
        EntityParsedTypeName {
            namespace: "Jans",
            type_name: "Test",
        },
        "test_id_key",
    );

    let json = serde_json::json!( {
        "test_id_key": "test_id",
        "string_key": "test string value",
        "long_key": 12345,
        "entity_uid_key": "unique_id",
        "bool_key": true,
    });

    let payload: TokenClaims = serde_json::from_value(json).unwrap();

    let entity = metadata
        .create_entity(
            &schema,
            &Token::new_id(payload, None),
            HashSet::new(),
            &Default::default(),
        )
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

    let metadata = EntityMetadata::new(
        EntityParsedTypeName {
            namespace: "",
            type_name: "Test",
        },
        "test_id_key",
    );

    let test_key = "string_key";
    let json = serde_json::json!( {
        "test_id_key": "test_id",
        // This will trigger the type error, because it's not a String.
        test_key: 123,
        "long_key": 12345,
        "entity_uid_key": "unique_id",
        "bool_key": true,
        "set_key": ["some_string"],
        "set_set_key": [["some_string"]]
    });

    let payload: TokenClaims = serde_json::from_value(json.clone()).unwrap();

    let entity_creation_error = metadata
        .create_entity(
            &schema,
            &Token::new_id(payload, None),
            HashSet::new(),
            &Default::default(),
        )
        .expect_err("entity creating should throw error");

    if let CreateCedarEntityError::GetTokenClaim(TokenClaimTypeError {
        key, actual_type, ..
    }) = entity_creation_error
    {
        let json_attr_value = json.as_object().unwrap().get(test_key).unwrap();
        let origin_type = TokenClaimTypeError::json_value_type_name(json_attr_value);

        assert!(key == test_key, "expected key: {test_key}, but got: {key}");
        assert!(
            actual_type == origin_type,
            "expected type: {origin_type}, but got: {actual_type}"
        );
    } else {
        panic!(
            "expected error type: CedarPolicyCreateTypeError::TokenClaimTypeError(GetTokenClaimError::KeyNotCorrectType), but got: {entity_creation_error}"
        );
    }
}

/// test wrong long type in token payload
#[test]
fn get_token_claim_type_long_error() {
    let schema_json = include_str!("test_create_data/successful_scenario_schema.json");
    let schema: CedarSchemaJson = serde_json::from_str(schema_json).unwrap();

    let metadata = EntityMetadata::new(
        EntityParsedTypeName {
            namespace: "",
            type_name: "Test",
        },
        "test_id_key",
    );

    let test_key = "long_key";
    let json = serde_json::json!( {
        "test_id_key": "test_id",
        "string_key": "test string",
        // This will trigger the type error, because it's not an i64.
        "long_key": "str",
        "entity_uid_key": "unique_id",
        "bool_key": true,
        "set_key": ["some_string"],
        "set_set_key": [["some_string"]]
    });

    let payload: TokenClaims = serde_json::from_value(json.clone()).unwrap();

    let entity_creation_error = metadata
        .create_entity(
            &schema,
            &Token::new_id(payload, None),
            HashSet::new(),
            &Default::default(),
        )
        .expect_err("entity creating should throw error");

    if let CreateCedarEntityError::GetTokenClaim(TokenClaimTypeError {
        key, actual_type, ..
    }) = entity_creation_error
    {
        let json_attr_value = json.as_object().unwrap().get(test_key).unwrap();
        let origin_type = TokenClaimTypeError::json_value_type_name(json_attr_value);

        assert!(key == test_key, "expected key: {test_key}, but got: {key}");
        assert!(
            actual_type == origin_type,
            "expected type: {origin_type}, but got: {actual_type}"
        );
    } else {
        panic!(
            "expected error type: CedarPolicyCreateTypeError::TokenClaimTypeError(GetTokenClaimError::KeyNotCorrectType), but got: {entity_creation_error}"
        );
    }
}

/// test wrong entity_uid type in token payload
#[test]
fn get_token_claim_type_entity_uid_error() {
    let schema_json = include_str!("test_create_data/successful_scenario_schema.json");
    let schema: CedarSchemaJson = serde_json::from_str(schema_json).unwrap();

    let metadata = EntityMetadata::new(
        EntityParsedTypeName {
            namespace: "",
            type_name: "Test",
        },
        "test_id_key",
    );

    let test_key = "entity_uid_key";
    let json = serde_json::json!( {
        "test_id_key": "test_id",
        "string_key": "test string",
        "long_key": 1234,
        // This will trigger the type error, because it's not a String.
        "entity_uid_key": 123,
        "bool_key": true,
        "set_key": ["some_string"],
        "set_set_key": [["some_string"]]
    });

    let payload: TokenClaims = serde_json::from_value(json.clone()).unwrap();

    let entity_creation_error = metadata
        .create_entity(
            &schema,
            &Token::new_id(payload, None),
            HashSet::new(),
            &Default::default(),
        )
        .expect_err("entity creating should throw error");

    if let CreateCedarEntityError::GetTokenClaim(TokenClaimTypeError {
        key, actual_type, ..
    }) = entity_creation_error
    {
        let json_attr_value = json.as_object().unwrap().get(test_key).unwrap();
        let origin_type = TokenClaimTypeError::json_value_type_name(json_attr_value);

        assert!(key == test_key, "expected key: {test_key}, but got: {key}");
        assert!(
            actual_type == origin_type,
            "expected type: {origin_type}, but got: {actual_type}"
        );
    } else {
        panic!(
            "expected error type: CedarPolicyCreateTypeError::TokenClaimTypeError(GetTokenClaimError::KeyNotCorrectType), but got: {entity_creation_error}"
        );
    }
}

/// test wrong boolean type in token payload
#[test]
fn get_token_claim_type_boolean_error() {
    let schema_json = include_str!("test_create_data/successful_scenario_schema.json");
    let schema: CedarSchemaJson = serde_json::from_str(schema_json).unwrap();

    let metadata = EntityMetadata::new(
        EntityParsedTypeName {
            namespace: "",
            type_name: "Test",
        },
        "test_id_key",
    );

    let test_key = "bool_key";
    let json = serde_json::json!( {
        "test_id_key": "test_id",
        "string_key": "test string",
        "long_key": 1234,
        "entity_uid_key": "ff910f15-d5a4-4227-828e-11cb8463f1b7",
        // This will trigger the type error, because it's not a bool.
        "bool_key": 123,
        "set_key": ["some_string"],
        "set_set_key": [["some_string"]]
    });

    let payload: TokenClaims = serde_json::from_value(json.clone()).unwrap();

    let entity_creation_error = metadata
        .create_entity(
            &schema,
            &Token::new_id(payload, None),
            HashSet::new(),
            &Default::default(),
        )
        .expect_err("entity creating should throw error");

    if let CreateCedarEntityError::GetTokenClaim(TokenClaimTypeError {
        key,
        actual_type,
        expected_type,
    }) = entity_creation_error
    {
        let json_attr_value = json.as_object().unwrap().get(test_key).unwrap();
        let origin_type = TokenClaimTypeError::json_value_type_name(json_attr_value);

        assert!(
            key == test_key,
            "expected key: {test_key}, but got: {key} with schema expected_type: {expected_type}"
        );
        assert!(
            actual_type == origin_type,
            "expected type: {origin_type}, but got: {actual_type} with schema expected_type: \
             {expected_type}"
        );
    } else {
        panic!(
            "expected error type: CedarPolicyCreateTypeError::TokenClaimTypeError(GetTokenClaimError::KeyNotCorrectType), but got: {entity_creation_error}"
        );
    }
}

/// test wrong set type in token payload, should be array of string
#[test]
fn get_token_claim_type_set_error() {
    let schema_json = include_str!("test_create_data/successful_scenario_schema.json");
    let schema: CedarSchemaJson = serde_json::from_str(schema_json).unwrap();

    let metadata = EntityMetadata::new(
        EntityParsedTypeName {
            namespace: "",
            type_name: "Test",
        },
        "test_id_key",
    );

    let test_key = "set_key";
    let json = serde_json::json!( {
        "test_id_key": "test_id",
        "string_key": "test string",
        "long_key": 1234,
        "entity_uid_key": "ff910f15-d5a4-4227-828e-11cb8463f1b7",
        "bool_key": false,
        // This will trigger the type error, because it's not a array of string.
        "set_key": 1,
        "set_set_key": [["some_string"]]
    });

    let payload: TokenClaims = serde_json::from_value(json.clone()).unwrap();

    let entity_creation_error = metadata
        .create_entity(
            &schema,
            &Token::new_id(payload, None),
            HashSet::new(),
            &Default::default(),
        )
        .expect_err("entity creating should throw error");

    if let CreateCedarEntityError::GetTokenClaim(TokenClaimTypeError {
        key,
        actual_type,
        expected_type,
    }) = entity_creation_error
    {
        let json_attr_value = json.as_object().unwrap().get(test_key).unwrap();
        let origin_type = TokenClaimTypeError::json_value_type_name(json_attr_value);

        assert!(
            key == test_key,
            "expected key: {test_key}, but got: {key} with schema expected_type: {expected_type}"
        );
        assert!(
            actual_type == origin_type,
            "expected type: {origin_type}, but got: {actual_type} with schema expected_type: \
             {expected_type}"
        );
    } else {
        panic!(
            "expected error type: CedarPolicyCreateTypeError::TokenClaimTypeError(GetTokenClaimError::KeyNotCorrectType), but got: {entity_creation_error}"
        );
    }
}

/// test wrong set type in token payload, should be array of array of string
#[test]
fn get_token_claim_type_set_of_set_error() {
    let schema_json = include_str!("test_create_data/successful_scenario_schema.json");
    let schema: CedarSchemaJson = serde_json::from_str(schema_json).unwrap();

    let metadata = EntityMetadata::new(
        EntityParsedTypeName {
            namespace: "",
            type_name: "Test",
        },
        "test_id_key",
    );

    let json = serde_json::json!( {
        "test_id_key": "test_id",
        "string_key": "test string",
        "long_key": 1234,
        "entity_uid_key": "ff910f15-d5a4-4227-828e-11cb8463f1b7",
        "bool_key": false,
        "set_key": ["some_string"],
        // This will trigger the type error, because it's not a array of array of string.
        "set_set_key": ["some_string"]
    });

    let payload: TokenClaims = serde_json::from_value(json.clone()).unwrap();

    let entity_creation_error = metadata
        .create_entity(
            &schema,
            &Token::new_id(payload, None),
            HashSet::new(),
            &Default::default(),
        )
        .expect_err("entity creating should throw error");

    if let CreateCedarEntityError::GetTokenClaim(TokenClaimTypeError {
        key,
        actual_type,
        expected_type,
    }) = entity_creation_error
    {
        let json_attr_value = json.as_object().unwrap().get("set_set_key").unwrap();
        let origin_type =
            TokenClaimTypeError::json_value_type_name(&json_attr_value.as_array().unwrap()[0]);

        // key set_set_key and zero element in array
        let test_key = "set_set_key[0]";

        assert!(
            key == test_key,
            "expected key: {test_key}, but got: {key} with schema expected_type: {expected_type}"
        );
        assert!(
            actual_type == origin_type,
            "expected type: {origin_type}, but got: {actual_type} with schema expected_type: \
             {expected_type}"
        );
    } else {
        panic!(
            "expected error type: CedarPolicyCreateTypeError::TokenClaimTypeError(GetTokenClaimError::KeyNotCorrectType), but got: {entity_creation_error}"
        );
    }
}

/// create entity with wrong cedar typename
#[test]
fn get_token_claim_cedar_typename_error() {
    let schema_json = include_str!("test_create_data/successful_scenario_schema.json");
    let schema: CedarSchemaJson = serde_json::from_str(schema_json).unwrap();

    // Mistake in entity type name, should be `"Jans::Test"`, it will trigger error
    let (typename, namespace) = parse_namespace_and_typename("Jans:::Test");
    let metadata = EntityMetadata::new(
        EntityParsedTypeName {
            namespace: &namespace,
            type_name: typename,
        },
        "test_id_key",
    );

    let json = serde_json::json!( {
        "test_id_key": "test_id",
        "string_key": "test string value",
        "long_key": 12345,
        "entity_uid_key": "unique_id",
        "bool_key": true,
    });

    let payload: TokenClaims = serde_json::from_value(json).unwrap();

    let entity_creation_error = metadata
        .create_entity(
            &schema,
            &Token::new_id(payload, None),
            HashSet::new(),
            &Default::default(),
        )
        .expect_err("entity creating should throw error");

    if let CreateCedarEntityError::EntityTypeName(typename, _) = &entity_creation_error {
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

    let metadata = EntityMetadata::new(
        EntityParsedTypeName {
            namespace: "Jans",
            type_name: "Test",
        },
        "test_id_key",
    );

    let json = serde_json::json!( {
        "test_id_key": "test_id",
        "string_key": "test string value",
        "long_key": 12345,
        "entity_uid_key": "unique_id",
        "bool_key": true,
    });

    let payload: TokenClaims = serde_json::from_value(json).unwrap();

    let entity_creation_error = metadata
        .create_entity(
            &schema,
            &Token::new_id(payload, None),
            HashSet::new(),
            &Default::default(),
        )
        .expect_err("entity creating should throw error");

    if let CreateCedarEntityError::FindType(typename) = &entity_creation_error {
        assert_eq!("Jans:::Test2", typename);
    } else {
        panic!(
            "error should be CedarPolicyCreateTypeError::EntityTypeName, but got {:?}",
            entity_creation_error
        );
    }
}
