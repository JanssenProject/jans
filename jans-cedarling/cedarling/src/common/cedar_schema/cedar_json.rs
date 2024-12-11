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

mod action;
mod entity_types;

use action::ActionSchema;
use derive_more::derive::Display;
use std::collections::HashMap;

pub use entity_types::{CedarSchemaEntityShape, CedarSchemaRecord};

/// Represent `cedar-policy` schema type for external usage.
#[derive(Debug, PartialEq, Hash, Eq, Display)]
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

/// Enum to get info about type based on name.
/// Is used as a result in [`CedarSchemaJson::find_type`]
pub enum SchemaDefinedType<'a> {
    Entity(&'a CedarSchemaEntityShape),
    CommonType(&'a CedarSchemaRecord),
}

/// JSON representation of a [`cedar_policy::Schema`]
#[derive(Debug, Clone, serde::Deserialize, serde::Serialize, PartialEq)]
pub struct CedarSchemaJson {
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

    /// Find the typename if exist in the schema and return it definition
    pub fn find_type(&self, type_name: &str, namespace: &str) -> Option<SchemaDefinedType> {
        let namespace = self.namespace.get(namespace)?;

        let schema_type = namespace
            .common_types
            .get(type_name)
            .as_ref()
            .map(|common_type| SchemaDefinedType::CommonType(common_type));

        if schema_type.is_some() {
            return schema_type;
        }

        let schema_type = namespace
            .entity_types
            .get(type_name)
            .as_ref()
            .map(|entity| SchemaDefinedType::Entity(entity));
        if schema_type.is_some() {
            return schema_type;
        }

        None
    }
}

/// CedarSchemaEntities hold all entities and their shapes in the namespace.
//
// It may contain more fields, but we don't need all of them.
#[derive(Debug, Clone, serde::Deserialize, serde::Serialize, PartialEq)]
pub struct CedarSchemaEntities {
    #[serde(rename = "entityTypes", default)]
    pub entity_types: HashMap<String, CedarSchemaEntityShape>,
    #[serde(rename = "commonTypes", default)]
    pub common_types: HashMap<String, CedarSchemaRecord>,
    pub actions: HashMap<String, ActionSchema>,
}

#[cfg(test)]
mod tests {
    use super::entity_types::*;
    use super::*;
    use action::CtxAttribute;
    use serde_json::json;
    use std::collections::HashSet;
    use test_utils::assert_eq;
    use test_utils::SortedJson;

    /// Test to parse the cedar json schema
    /// to debug deserialize the schema
    #[test]
    fn parse_correct_example() {
        let json_value = include_str!("test_files/test_data_cedar.json");

        let parsed_cedar_schema: CedarSchemaJson =
            serde_json::from_str(json_value).expect("failed to parse json");

        let entity_types = HashMap::from_iter(vec![
            (
                "Access_token".to_string(),
                CedarSchemaEntityShape {
                    shape: Some(CedarSchemaRecord {
                        entity_type: "Record".to_string(),
                        attributes: HashMap::from_iter(vec![
                            (
                                "aud".to_string(),
                                CedarSchemaEntityAttribute {
                                    cedar_type: CedarSchemaEntityType::Typed(EntityType {
                                        kind: "EntityOrCommon".to_string(),
                                        name: "String".to_string(),
                                    }),
                                    required: true,
                                },
                            ),
                            (
                                "exp".to_string(),
                                CedarSchemaEntityAttribute {
                                    cedar_type: CedarSchemaEntityType::Typed(EntityType {
                                        kind: "EntityOrCommon".to_string(),
                                        name: "Long".to_string(),
                                    }),
                                    required: true,
                                },
                            ),
                            (
                                "iat".to_string(),
                                CedarSchemaEntityAttribute {
                                    cedar_type: CedarSchemaEntityType::Primitive(PrimitiveType {
                                        kind: PrimitiveTypeKind::Long,
                                    }),
                                    required: true,
                                },
                            ),
                            (
                                "scope".to_string(),
                                CedarSchemaEntityAttribute {
                                    cedar_type: CedarSchemaEntityType::Set(Box::new(
                                        SetEntityType {
                                            element: CedarSchemaEntityType::Typed(EntityType {
                                                kind: "EntityOrCommon".to_string(),
                                                name: "String".to_string(),
                                            }),
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
            (
                "TrustedIssuer".to_string(),
                CedarSchemaEntityShape {
                    shape: Some(CedarSchemaRecord {
                        entity_type: "Record".to_string(),
                        attributes: HashMap::from_iter([(
                            "issuer_entity_id".to_string(),
                            CedarSchemaEntityAttribute {
                                required: true,
                                cedar_type: CedarSchemaEntityType::Typed(EntityType {
                                    name: "Url".to_string(),
                                    kind: "EntityOrCommon".to_string(),
                                }),
                            },
                        )]),
                    }),
                },
            ),
            ("Issue".to_string(), CedarSchemaEntityShape { shape: None }),
        ]);

        let common_types = HashMap::from_iter([(
            "Url".to_string(),
            CedarSchemaRecord {
                entity_type: "Record".to_string(),
                attributes: HashMap::from_iter([
                    (
                        "host".to_string(),
                        CedarSchemaEntityAttribute {
                            cedar_type: CedarSchemaEntityType::Typed(EntityType {
                                kind: "EntityOrCommon".to_string(),
                                name: "String".to_string(),
                            }),
                            required: true,
                        },
                    ),
                    (
                        "path".to_string(),
                        CedarSchemaEntityAttribute {
                            cedar_type: CedarSchemaEntityType::Typed(EntityType {
                                kind: "EntityOrCommon".to_string(),
                                name: "String".to_string(),
                            }),
                            required: true,
                        },
                    ),
                    (
                        "protocol".to_string(),
                        CedarSchemaEntityAttribute {
                            cedar_type: CedarSchemaEntityType::Typed(EntityType {
                                kind: "EntityOrCommon".to_string(),
                                name: "String".to_string(),
                            }),
                            required: true,
                        },
                    ),
                ]),
            },
        )]);

        let actions = HashMap::from([(
            "Update".to_string(),
            ActionSchema {
                resource_types: HashSet::from(["Issue"].map(|x| x.to_string())),
                principal_types: HashSet::from(["Access_token", "Role"].map(|x| x.to_string())),
                context: None,
            },
        )]);

        let schema_to_compare = CedarSchemaJson {
            namespace: HashMap::from_iter(vec![(
                "Jans".to_string(),
                CedarSchemaEntities {
                    entity_types,
                    common_types,
                    actions,
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

    #[test]
    fn can_parse_action_with_ctx() {
        let expected_principal_entities =
            HashSet::from(["Jans::Workload".into(), "Jans::User".into()]);
        let expected_resource_entities = HashSet::from(["Jans::Issue".into()]);
        let expected_context_entities = HashSet::from([
            CtxAttribute {
                namespace: "Jans".into(),
                key: "access_token".into(),
                kind: CedarType::TypeName("Access_token".to_string()),
            },
            CtxAttribute {
                namespace: "Jans".into(),
                key: "time".into(),
                kind: CedarType::Long,
            },
            CtxAttribute {
                namespace: "Jans".into(),
                key: "user".into(),
                kind: CedarType::TypeName("User".to_string()),
            },
            CtxAttribute {
                namespace: "Jans".into(),
                key: "workload".into(),
                kind: CedarType::TypeName("Workload".to_string()),
            },
        ]);

        // Test case where the context is a record:
        // action "Update" appliesTo {
        //  principal: [Workload, User],
        //  resource: [Issue],
        //  context: {
        //      time: Long,
        //      user: User,
        //      workload: Workload,
        //      access_token: Access_token,
        //  }};
        let json_value = include_str!("./test_files/test_schema_with_record_ctx.json");
        let parsed_cedar_schema: CedarSchemaJson =
            serde_json::from_str(json_value).expect("Should parse JSON schema");
        let action = parsed_cedar_schema
            .find_action("Update", "Jans")
            .expect("Should not error while finding action")
            .expect("Action should not be none");
        assert_eq!(action.principal_entities, expected_principal_entities);
        assert_eq!(action.resource_entities, expected_resource_entities);
        assert_eq!(action.context_entities, expected_context_entities);

        // Test case where the context is a type:
        // action "Update" appliesTo {
        //  principal: [Workload, User],
        //  resource: [Issue],
        //  context: Context
        // };
        let json_value = include_str!("./test_files/test_schema_with_type_ctx.json");
        let parsed_cedar_schema: CedarSchemaJson =
            serde_json::from_str(json_value).expect("Should parse JSON schema");
        let action = parsed_cedar_schema
            .find_action("Update", "Jans")
            .expect("Should not error while finding action")
            .expect("Action should not be none");
        assert_eq!(action.principal_entities, expected_principal_entities);
        assert_eq!(action.resource_entities, expected_resource_entities);
        assert_eq!(action.context_entities, expected_context_entities);

        let id_mapping = HashMap::from([
            ("access_token".into(), "tkn-1".into()),
            ("user".into(), "user-123".into()),
            ("workload".into(), "workload-321".into()),
        ]);
        let value_mapping = HashMap::from([("time".into(), json!(123123123))]);
        let ctx_json = action
            .build_ctx_entities_json(id_mapping, value_mapping)
            .expect("Should build JSON context");
        assert_eq!(
            ctx_json,
            json!({
                "access_token": { "type": "Jans::Access_token", "id": "tkn-1" },
                "user": { "type": "Jans::User", "id": "user-123" },
                "workload": { "type": "Jans::Workload", "id": "workload-321" },
                "time": 123123123,
            })
        )
    }
}
