// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use build_attrs::build_entity_attrs_from_values;
use cedar_policy::{EntityAttrEvaluationError, ExpressionConstructionError, ParseErrors};
use serde_json::Value;

use super::*;
use crate::ResourceData;

impl EntityBuilder {
    pub fn build_resource_entity(
        &self,
        resource: &ResourceData,
    ) -> Result<Entity, BuildResourceEntityError> {
        let entity_type_name = EntityTypeName::from_str(&resource.resource_type)?;
        let (_namespace_name, entity_type) = self
            .schema
            .get_entity_from_base_name(entity_type_name.basename())
            .ok_or(BuildEntityError::EntityNotInSchema(
                entity_type_name.to_string(),
            ))?;

        let entity_attrs =
            build_entity_attrs_from_values(&self.schema, entity_type, &resource.payload)?;

        // Build cedar entity
        let entity_id =
            EntityId::from_str(&resource.id).map_err(BuildEntityError::ParseEntityId)?;
        let entity_uid = EntityUid::from_type_name_and_id(entity_type_name, entity_id);
        Ok(Entity::new(entity_uid, entity_attrs, HashSet::new())?)
    }
}

#[derive(Debug, thiserror::Error)]
pub enum BuildResourceEntityError {
    #[error(transparent)]
    BuildEntity(#[from] BuildEntityError),
    #[error(transparent)]
    TypeMismatch(#[from] JsonTypeError),
    #[error(transparent)]
    ExpressionConstructExpression(#[from] ExpressionConstructionError),
    #[error(transparent)]
    EntityAttrEvaluationError(#[from] EntityAttrEvaluationError),
    #[error(transparent)]
    BuildAttr(#[from] BuildAttrError),
    #[error("invalid entity name: {0}")]
    InvalidEntityName(#[from] ParseErrors),
}

#[derive(Debug, thiserror::Error, PartialEq)]
#[error("JSON value type mismatch: expected '{expected_type}', but found '{actual_type}'")]
pub struct JsonTypeError {
    pub expected_type: String,
    pub actual_type: String,
}

impl JsonTypeError {
    /// Returns the JSON type name of the given value.
    pub fn value_type_name(value: &Value) -> String {
        match value {
            Value::Null => "null".to_string(),
            Value::Bool(_) => "bool".to_string(),
            Value::Number(_) => "number".to_string(),
            Value::String(_) => "string".to_string(),
            Value::Array(_) => "array".to_string(),
            Value::Object(_) => "object".to_string(),
        }
    }

    /// Constructs a `TypeMismatch` error with detailed information about the expected and actual types.
    pub fn type_mismatch(expected_type_name: &str, got_value: &Value) -> Self {
        let got_value_type_name = Self::value_type_name(got_value);

        Self {
            expected_type: expected_type_name.to_string(),
            actual_type: got_value_type_name,
        }
    }
}

#[cfg(test)]
mod test {
    use super::super::*;
    use super::*;
    use crate::common::cedar_schema::cedar_json::CedarSchemaJson;
    use cedar_policy::EvalResult;
    use serde_json::json;

    #[test]
    fn can_build_entity() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": {
                "commonTypes": {
                    "Url": {
                        "type": "Record",
                        "attributes": {
                            "host": { "type": "String" },
                            "path": { "type": "String" },
                            "protocol": { "type": "String" },
                        },
                    },
                },
                "entityTypes": {
                    "Role": {},
                    "HttpRequest": {
                        "shape": {
                            "type": "Record",
                            "attributes":  {
                                "header": {
                                    "type": "Record",
                                    "attributes": {
                                        "Accept": { "type": "EntityOrCommon", "name": "String" },
                                    },
                                },
                                "url": { "type": "EntityOrCommon", "name": "Url" },
                            },
                        }
                    }
                }
            }
        }))
        .expect("should successfully create test schema");
        let builder = EntityBuilder::new(schema, EntityNames::default(), false, false);
        let resource_data = ResourceData {
            resource_type: "HttpRequest".to_string(),
            id: "request-123".to_string(),
            payload: HashMap::from([
                ("header".to_string(), json!({"Accept": "test"})),
                (
                    "url".to_string(),
                    json!({"host": "protected.host", "protocol": "http", "path": "/protected"}),
                ),
            ]),
        };
        let entity = builder
            .build_resource_entity(&resource_data)
            .expect("expected to build resource entity");

        let url = entity
            .attr("url")
            .expect("entity must have an `url` attribute")
            .unwrap();
        if let EvalResult::Record(ref record) = url {
            assert_eq!(record.len(), 3);
            assert_eq!(
                record
                    .get("host")
                    .expect("expected `url` to have a `host` attribute"),
                &EvalResult::String("protected.host".to_string())
            );
            assert_eq!(
                record
                    .get("protocol")
                    .expect("expected `url` to have a `domain` attribute"),
                &EvalResult::String("http".to_string())
            );
            assert_eq!(
                record
                    .get("path")
                    .expect("expected `url` to have a `path` attribute"),
                &EvalResult::String("/protected".to_string())
            );
        } else {
            panic!(
                "expected the attribute `url` to be a record, got: {:?}",
                url
            );
        }

        let header = entity
            .attr("header")
            .expect("entity must have an `header` attribute")
            .unwrap();
        if let EvalResult::Record(ref record) = header {
            assert_eq!(record.len(), 1);
            assert_eq!(
                record
                    .get("Accept")
                    .expect("expected `url` to have an `Accept` attribute"),
                &EvalResult::String("test".to_string())
            );
        } else {
            panic!(
                "expected the attribute `header` to be a record, got: {:?}",
                header
            );
        }
    }

    #[test]
    fn can_build_entity_with_optional_attr() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": {
                "commonTypes": {
                    "Url": {
                        "type": "Record",
                        "attributes": {
                            "host": { "type": "String" },
                            "path": { "type": "String" },
                            "protocol": { "type": "String" },
                        },
                    },
                },
                "entityTypes": {
                    "Role": {},
                    "HttpRequest": {
                        "shape": {
                            "type": "Record",
                            "attributes":  {
                                "url": { "type": "EntityOrCommon", "name": "Url", "required": false},
                            },
                        }
                    }
                }
            }
        }))
        .expect("should successfully create test schema");
        let builder = EntityBuilder::new(schema, EntityNames::default(), false, false);
        let resource_data = ResourceData {
            resource_type: "HttpRequest".to_string(),
            id: "request-123".to_string(),
            payload: HashMap::new(),
        };
        let entity = builder
            .build_resource_entity(&resource_data)
            .expect("expected to build resource entity");

        assert!(
            entity.attr("url").is_none(),
            "entity should not have a `url` attribute"
        );
    }

    #[test]
    fn can_build_entity_with_optional_record_attr() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": {
                "commonTypes": {
                    "Url": {
                        "type": "Record",
                        "attributes": {
                            "host": { "type": "String" },
                            "path": { "type": "String" },
                            "protocol": { "type": "String" },
                        },
                    },
                },
                "entityTypes": {
                    "Role": {},
                    "HttpRequest": {
                        "shape": {
                            "type": "Record",
                            "attributes":  {
                                "header": {
                                    "type": "Record",
                                    "attributes": {
                                        "Accept": { "type": "EntityOrCommon", "name": "String", "required": false },
                                    },
                                },
                                "url": { "type": "EntityOrCommon", "name": "Url" },
                            },
                        }
                    }
                }
            }
        }))
        .expect("should successfully create test schema");
        let builder = EntityBuilder::new(schema, EntityNames::default(), false, false);
        let resource_data = ResourceData {
            resource_type: "HttpRequest".to_string(),
            id: "request-123".to_string(),
            payload: HashMap::from([
                (
                    "url".to_string(),
                    json!({"host": "protected.host", "protocol": "http", "path": "/protected"}),
                ),
                ("header".to_string(), json!({})),
            ]),
        };
        let entity = builder
            .build_resource_entity(&resource_data)
            .expect("expected to build resource entity");

        let url = entity
            .attr("url")
            .expect("entity must have an `url` attribute")
            .unwrap();
        if let EvalResult::Record(ref record) = url {
            assert_eq!(record.len(), 3);
            assert_eq!(
                record
                    .get("host")
                    .expect("expected `url` to have a `host` attribute"),
                &EvalResult::String("protected.host".to_string())
            );
            assert_eq!(
                record
                    .get("protocol")
                    .expect("expected `url` to have a `domain` attribute"),
                &EvalResult::String("http".to_string())
            );
            assert_eq!(
                record
                    .get("path")
                    .expect("expected `url` to have a `path` attribute"),
                &EvalResult::String("/protected".to_string())
            );
        } else {
            panic!(
                "expected the attribute `url` to be a record, got: {:?}",
                url
            );
        }

        let header = entity
            .attr("header")
            .expect("entity must have an `header` attribute")
            .unwrap();
        if let EvalResult::Record(ref record) = header {
            assert_eq!(record.len(), 0, "the header attribute must be empty");
        } else {
            panic!(
                "expected the attribute `header` to be a record, got: {:?}",
                header
            );
        }
    }
}
