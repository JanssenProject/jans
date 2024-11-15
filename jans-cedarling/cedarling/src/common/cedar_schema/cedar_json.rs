/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

//! Module contains the JSON representation of a [cedar_policy::Schema]  
//! Support translated schema from human representation to JSON via CLI version `cedar-policy-cli 4.1`.  
//! To translate human redable format to JSON via CLI use next command:  
//! `cedar translate-schema --direction cedar-to-json  -s .\cedar.schema`
//! [cedar json schema grammar](https://docs.cedarpolicy.com/schema/json-schema-grammar.html) - documentation about json structure of cedar schema.

use std::collections::HashMap;
mod entity_types;

pub use entity_types::{CedarSchemaEntityShape, CedarSchemaRecord};

/// Represent `cedar-policy` schema type for external usage.
pub enum CedarType {
    Long,
    String,
    Boolean,
    TypeName(String),
    Set(Box<CedarType>),
}

/// Possible errors that may occur when retrieving a [`CedarType`] from cedar-policy schema.
#[derive(Debug, thiserror::Error)]
pub enum GetCedarTypeError {
    /// Error while getting `cedar-policy` schema not implemented type
    #[error("could not get cedar-policy type {0}, it is not implemented")]
    TypeNotImplemented(String),
}

/// JSON representation of a [`cedar_policy::Schema`]
#[derive(Debug, Clone, serde::Deserialize, serde::Serialize, PartialEq)]
pub(crate) struct CedarSchemaJson {
    #[serde(flatten)]
    pub namespace: HashMap<String, CedarSchemaEntities>,
}

impl CedarSchemaJson {
    /// Get schema record by namespace name and entity type name
    pub fn entity_schema(
        &self,
        namespace: &str,
        typename: &str,
    ) -> Option<&CedarSchemaEntityShape> {
        let namespace = self.namespace.get(namespace)?;
        namespace.entity_types.get(typename)
    }
}

/// CedarSchemaEntities hold all entities and their shapes in the namespace.
//
// It may contain more fields, but we don't need all of them.
#[derive(Debug, Clone, serde::Deserialize, serde::Serialize, PartialEq)]
pub struct CedarSchemaEntities {
    #[serde(rename = "entityTypes")]
    pub entity_types: HashMap<String, CedarSchemaEntityShape>,
}

#[cfg(test)]
mod tests {

    use super::entity_types::*;

    use super::*;
    use test_utils::assert_eq;
    use test_utils::SortedJson;

    /// Test to parse the cedar json schema
    /// to debug deserialize the schema
    #[test]
    fn parse_correct_example() {
        let json_value = include_str!("test_files/test_data_cedar.json");

        let parsed_cedar_schema: CedarSchemaJson =
            serde_json::from_str(json_value).expect("failed to parse json");

        let schema_to_compare = CedarSchemaJson {
            namespace: HashMap::from_iter(vec![(
                "Jans".to_string(),
                CedarSchemaEntities {
                    entity_types: HashMap::from_iter(vec![
                        (
                            "Access_token".to_string(),
                            CedarSchemaEntityShape {
                                shape: Some(CedarSchemaRecord {
                                    entity_type: "Record".to_string(),
                                    attributes: HashMap::from_iter(vec![
                                        (
                                            "aud".to_string(),
                                            CedarSchemaEntityAttribute {
                                                cedar_type: CedarSchemaEntityType::Typed(
                                                    EntityType {
                                                        kind: "EntityOrCommon".to_string(),
                                                        name: "String".to_string(),
                                                    },
                                                ),
                                                required: true,
                                            },
                                        ),
                                        (
                                            "exp".to_string(),
                                            CedarSchemaEntityAttribute {
                                                cedar_type: CedarSchemaEntityType::Typed(
                                                    EntityType {
                                                        kind: "EntityOrCommon".to_string(),
                                                        name: "Long".to_string(),
                                                    },
                                                ),
                                                required: true,
                                            },
                                        ),
                                        (
                                            "iat".to_string(),
                                            CedarSchemaEntityAttribute {
                                                cedar_type: CedarSchemaEntityType::Primitive(
                                                    PrimitiveType {
                                                        kind: PrimitiveTypeKind::Long,
                                                    },
                                                ),
                                                required: true,
                                            },
                                        ),
                                        (
                                            "scope".to_string(),
                                            CedarSchemaEntityAttribute {
                                                cedar_type: CedarSchemaEntityType::Set(Box::new(
                                                    SetEntityType {
                                                        element: CedarSchemaEntityType::Typed(
                                                            EntityType {
                                                                kind: "EntityOrCommon".to_string(),
                                                                name: "String".to_string(),
                                                            },
                                                        ),
                                                    },
                                                )),

                                                required: false,
                                            },
                                        ),
                                    ]),
                                }),
                            },
                        ),
                        ("Role".to_string(), CedarSchemaEntityShape { shape: None }),
                        ("Issue".to_string(), CedarSchemaEntityShape { shape: None }),
                    ]),
                },
            )]),
        };

        assert_eq!(
            serde_json::json!(parsed_cedar_schema).sorted(),
            serde_json::json!(schema_to_compare).sorted()
        );
    }

    /// test to check if we get error on parsing invalid `EntityOrCommon` type
    #[test]
    fn parse_error_entity_or_common() {
        // In this file we skipped field `name` for `EntityOrCommon`
        let json_value = include_str!("test_files/test_data_cedar_err_entity_or_common.json");

        let parse_error =
            serde_json::from_str::<CedarSchemaJson>(json_value).expect_err("should fail to parse");
        assert_eq!(parse_error.to_string(),"could not deserialize CedarSchemaEntityType: failed to deserialize EntityOrCommon: missing field `name` at line 17 column 1")
    }

    /// test to check if we get error on parsing invalid `PrimitiveType` type
    #[test]
    fn parse_error_primitive_type() {
        // In this file we use `"type": 123` but in OK case should be `"type": "Long"`
        let json_value = include_str!("test_files/test_data_cedar_err_primitive_type.json");

        let parse_error =
            serde_json::from_str::<CedarSchemaJson>(json_value).expect_err("should fail to parse");
        assert_eq!(parse_error.to_string(),"could not deserialize CedarSchemaEntityType: invalid type: integer `123`, expected a string at line 17 column 1")
    }

    /// test to check if we get error on parsing invalid nested Sets :`Set<Set<EntityOrCommon>>` type
    #[test]
    fn parse_error_set_entity_or_common() {
        // In this file we skipped field `name` for `EntityOrCommon` in the nested set
        let json_value = include_str!("test_files/test_data_cedar_err_set.json");

        let parse_error =
            serde_json::from_str::<CedarSchemaJson>(json_value).expect_err("should fail to parse");
        assert_eq!(parse_error.to_string(),"could not deserialize CedarSchemaEntityType: failed to deserialize Set: failed to deserialize Set: failed to deserialize EntityOrCommon: missing field `name` at line 24 column 1")
    }

    /// test to check if we get error on parsing invalid type in field `is_required`
    #[test]
    fn parse_error_field_is_required() {
        // In this file we use ` "required": 1234` but in OK case should be ` "required": false` or omit
        let json_value = include_str!("test_files/test_data_cedar_err_field_is_required.json");

        let parse_error =
            serde_json::from_str::<CedarSchemaJson>(json_value).expect_err("should fail to parse");
        assert_eq!(parse_error.to_string(),"could not deserialize CedarSchemaEntityAttribute, field 'is_required': invalid type: integer `1234`, expected a boolean at line 22 column 1")
    }
}
