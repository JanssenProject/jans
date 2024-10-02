/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

//! Testing the creating entities

use core::panic;
use std::{collections::HashSet, str::FromStr};

use cedar_policy::{EntityId, EntityTypeName, EntityUid};
use pretty_assertions::assert_eq;
use serde_json::{Map, Value};

use crate::models::token_data::{GetTokenClaimValue, TokenClaim};

use super::create::*;

// Recursively sort the fields of JSON objects
// we need deterministic ordering of the JSON objects to be able to compare them
fn sort_json(value: &mut Value) {
    if let Value::Object(map) = value {
        // in serde_json, maps are opdered if used feature "preserve_order", it is enabled by default
        let mut sorted_map = Map::new();

        // Sort the map by key and recursively sort nested values
        let mut entries: Vec<_> = map.iter_mut().collect();
        entries.sort_by_key(|(k, _)| k.to_owned());

        for (key, val) in entries {
            sort_json(val); // Recursively sort nested JSON objects
            sorted_map.insert(key.to_owned(), val.clone());
        }

        *map = sorted_map;
    } else if let Value::Array(arr) = value {
        arr.sort_by(|a, b| a.to_string().cmp(&b.to_string())); // Sort the array elements

        for item in arr.iter_mut() {
            sort_json(item); // Sort each element in the array
        }
    }
}

// test all successful cases
#[test]
fn successful_scenario() {
    let metadata = EntityMetadata::new(
        "test",
        "test_id_key",
        vec![
            EntityAttributeMetadata {
                attribute_name: "string_key",
                token_claims_key: "string_key",
                cedar_policy_type: CedarPolicyType::String,
            },
            EntityAttributeMetadata {
                attribute_name: "long_key",
                token_claims_key: "long_key",
                cedar_policy_type: CedarPolicyType::Long,
            },
            EntityAttributeMetadata {
                attribute_name: "entity_uid_key",
                token_claims_key: "entity_uid_key",
                cedar_policy_type: CedarPolicyType::EntityUid {
                    entity_type: "test_entity",
                },
            },
        ],
    );

    let json = serde_json::json!( {
        "test_id_key": "test_id",
        "string_key": "test string value",
        "long_key": 12345,
        "entity_uid_key": "unique_id"
    });

    let claim: TokenClaim = serde_json::from_value(json).unwrap();

    let parents = vec![
        EntityUid::from_type_name_and_id(
            EntityTypeName::from_str("test_parent_type1").unwrap(),
            EntityId::from_str("test1").unwrap(),
        ),
        EntityUid::from_type_name_and_id(
            EntityTypeName::from_str("test_parent_type2").unwrap(),
            EntityId::from_str("test2").unwrap(),
        ),
    ];

    let entity = metadata
        .create_entity(&claim, HashSet::from_iter(parents))
        .expect("entity should be created");

    let mut entity_json = entity.to_json_value().expect("should serialize to json");
    sort_json(&mut entity_json);

    let mut expected = serde_json::json!({
        "uid": {
            "type": "test",
            "id": "test_id"
        },
        "attrs": {
            "entity_uid_key": {
                "__entity": {
                    "type": "test_entity",
                    "id": "unique_id"
                }
            },
            "long_key": 12345,
            "string_key": "test string value"
        },
        "parents": [
            {
                "type": "test_parent_type2",
                "id": "test2"
            },
            {
                "type": "test_parent_type1",
                "id": "test1"
            }
        ]
    });
    sort_json(&mut expected);

    assert_eq!(entity_json, expected);
}

#[test]
fn get_token_claim_type_string_error() {
    let metadata = EntityMetadata::new(
        "test",
        "test_id_key",
        vec![EntityAttributeMetadata {
            attribute_name: "key",
            token_claims_key: "string_key",
            cedar_policy_type: CedarPolicyType::String,
        }],
    );

    let json = serde_json::json!( {
        "test_id_key": "test_id",
        "string_key": 123,
    });

    let claim: TokenClaim = serde_json::from_value(json.clone()).unwrap();

    let entity_creation_error = metadata
        .create_entity(&claim, HashSet::new())
        .expect_err("entity creating should throw error");

    if let CedarPolicyCreateTypeError::GetTokenClaimValue(GetTokenClaimValue::KeyNotCorrectType {
        key,
        got_type,
        ..
    }) = entity_creation_error
    {
        let origin_expected_key = metadata.meta_attributes[0].token_claims_key;

        let json_attr_value = json
            .as_object()
            .unwrap()
            .get(metadata.meta_attributes[0].token_claims_key)
            .unwrap();
        let origin_type = GetTokenClaimValue::json_value_type_name(json_attr_value);

        assert!(
            key == origin_expected_key,
            "expected key: {origin_expected_key}, but got: {key}"
        );
        assert!(
            got_type == origin_type,
            "expected type: {origin_type}, but got: {got_type}"
        );
    } else {
        panic!("expected error type: CedarPolicyCreateTypeError::GetTokenClaimValue(GetTokenClaimValue::KeyNotCorrectType), but got: {entity_creation_error}");
    }
}

#[test]
fn get_token_claim_cedar_type_error() {
    let metadata = EntityMetadata::new(
        "test",
        "test_id_key",
        vec![EntityAttributeMetadata {
            attribute_name: "key",
            token_claims_key: "string_key",
            cedar_policy_type: CedarPolicyType::EntityUid {
                entity_type: "Test:",
            },
        }],
    );

    let json = serde_json::json!( {
        "test_id_key": "test_id",
        "string_key": "string value",
    });

    let claim: TokenClaim = serde_json::from_value(json.clone()).unwrap();

    let entity_creation_error = metadata
        .create_entity(&claim, HashSet::new())
        .expect_err("entity creating should throw error");

    assert!(matches!(
        entity_creation_error,
        CedarPolicyCreateTypeError::EntityTypeName(_, _)
    ))
}

#[test]
fn create_entity_cedar_type_error() {
    let metadata = EntityMetadata::new("Test:1", "test_id_key", vec![]);

    let json = serde_json::json!( {
        "test_id_key": "test_id",
        "string_key": "string value",
    });

    let claim: TokenClaim = serde_json::from_value(json.clone()).unwrap();

    let entity_creation_error = metadata
        .create_entity(&claim, HashSet::new())
        .expect_err("entity creating should throw error");

    assert!(matches!(
        entity_creation_error,
        CedarPolicyCreateTypeError::EntityTypeName(_, _)
    ))
}

macro_rules! get_claim_neg_test {
    ($suite:ident, $($name:ident: $cedar_policy_type:expr, $claim_value:expr)*) => {
        mod $suite {
            use super::*;
            $(
                #[test]
                fn $name() {
                    let metadata = EntityMetadata::new(
                        "test",
                        "test_id_key",
                        vec![EntityAttributeMetadata {
                            attribute_name: "key",
                            token_claims_key: "claim_key",
                            cedar_policy_type: $cedar_policy_type,
                        }],
                    );

                    let json = serde_json::json!( {
                        "test_id_key": "test_id",
                        "claim_key": $claim_value,
                    });

                    let claim: TokenClaim = serde_json::from_value(json.clone()).unwrap();

                    let entity_creation_error = metadata
                        .create_entity(&claim, HashSet::new())
                        .expect_err("entity creating should throw error");

                    if let CedarPolicyCreateTypeError::GetTokenClaimValue(GetTokenClaimValue::KeyNotCorrectType {
                        key,
                        got_type,
                        ..
                    }) = entity_creation_error
                    {
                        let origin_expected_key = metadata.meta_attributes[0].token_claims_key;

                        let json_attr_value = json
                            .as_object()
                            .unwrap()
                            .get(metadata.meta_attributes[0].token_claims_key)
                            .unwrap();
                        let origin_type = GetTokenClaimValue::json_value_type_name(json_attr_value);

                        assert!(
                            key == origin_expected_key,
                            "expected key: {origin_expected_key}, but got: {key}"
                        );
                        assert!(
                            got_type == origin_type,
                            "expected type: {origin_type}, but got: {got_type}"
                        );
                    } else {
                        std::panic!("expected error type: CedarPolicyCreateTypeError::GetTokenClaimValue(GetTokenClaimValue::KeyNotCorrectType), but got: {entity_creation_error}");
                    }
                }
            )*
        }
    }
}

// using table testing with rust macros
get_claim_neg_test! {get_claim_attr_type_negative_tests,
    get_string: CedarPolicyType::String, 123
    get_long: CedarPolicyType::Long, "some string"
    get_entity_uid: CedarPolicyType::EntityUid{entity_type:"SomeType"}, 123
}
