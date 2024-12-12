/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use crate::common::cedar_schema::cedar_json::SchemaDefinedType;

use super::{
    entity_types::{
        CedarSchemaEntityAttribute, CedarSchemaEntityType, PrimitiveType, PrimitiveTypeKind,
    },
    CedarSchemaEntities, CedarSchemaJson, CedarSchemaRecord, CedarType, GetCedarTypeError,
};
use serde::{de, ser::SerializeMap, Deserialize, Serialize};
use serde_json::{json, Value};
use std::collections::{HashMap, HashSet};

type AttrName = String;

#[derive(Debug, Eq, Hash, PartialEq)]
pub struct CtxAttribute {
    pub namespace: String,
    pub key: String,
    pub kind: CedarType,
}

pub struct Action<'a> {
    pub principal_entities: HashSet<String>,
    pub resource_entities: HashSet<String>,
    pub context_entities: HashSet<CtxAttribute>,
    pub schema_entities: &'a CedarSchemaEntities,
    pub schema: &'a ActionSchema,
}

impl Action<'_> {
    /// Builds the JSON representation of context entities for a given action.
    ///
    /// This method processes the context attributes of the action and generates a
    /// corresponding JSON value. The context may include entity references (with
    /// `type` and `id`) and other values, which can be mapped through the provided
    /// `id_mapping` and `value_mapping`.
    ///
    /// The `id_mapping` param is a A `HashMap` that maps context attribute keys
    /// (like `"access_token"`) to their corresponding `id`s (like `"acs-tkn-1"`).
    ///
    /// # Usage Example
    ///
    /// ```rs
    /// let id_mapping = HashMap::from([("access_token".to_string(), "acs-tkn-1".to_string())]);
    /// let json = action.build_ctx_entities_json(id_mapping, value_mapping);
    /// ```
    pub fn build_ctx_entity_refs_json(
        &self,
        id_mapping: HashMap<String, String>,
    ) -> Result<Value, BuildJsonCtxError> {
        let mut json = json!({});

        for attr in self.context_entities.iter() {
            if let CedarType::TypeName(type_name) = &attr.kind {
                let id = match id_mapping.get(&attr.key) {
                    Some(val) => val,
                    None => Err(BuildJsonCtxError::MissingIdMapping(attr.key.clone()))?,
                };
                let type_name = format!("{}::{}", attr.namespace, type_name);
                json[attr.key.clone()] = json!({"type": type_name, "id": id});
            }
        }

        Ok(json)
    }
}

#[derive(Debug, thiserror::Error)]
pub enum BuildJsonCtxError {
    /// If an entity reference is provided but the ID is missing from `id_mapping`.
    #[error("An entity reference for `{0}` is required by the schema but an ID was not provided via the `id_mapping`")]
    MissingIdMapping(String),
    /// If a non-entity attribute is provided but the value is missing from `value_mapping`.
    #[error("A non-entity attribute for `{0}` is required by the schema but a value was not provided via the `value_mapping`")]
    MissingValueMapping(String),
}

impl CedarSchemaJson {
    /// Find the action in the schema
    pub fn find_action(
        &self,
        action_name: &str,
        namespace: &str,
    ) -> Result<Option<Action>, FindActionError> {
        let schema_entities = match self.namespace.get(namespace) {
            Some(entities) => entities,
            None => return Ok(None),
        };

        let action_schema = match schema_entities.actions.get(action_name) {
            Some(schema) => schema,
            None => return Ok(None),
        };

        let mut principal_entities = HashSet::new();
        for res_type in action_schema.principal_types.iter().map(|s| s.as_str()) {
            principal_entities.insert(format!("{}::{}", namespace, res_type));
        }
        let mut resource_entities = HashSet::new();
        for res_type in action_schema.resource_types.iter().map(|s| s.as_str()) {
            resource_entities.insert(format!("{}::{}", namespace, res_type));
        }
        let mut context_entities = HashSet::new();
        if let Some(ctx) = &action_schema.context {
            self.process_action_context(ctx, namespace, &mut context_entities)?;
        }

        Ok(Some(Action {
            principal_entities,
            resource_entities,
            context_entities,
            schema_entities,
            schema: action_schema,
        }))
    }

    fn process_action_context(
        &self,
        ctx: &RecordOrType,
        namespace: &str,
        entities: &mut HashSet<CtxAttribute>,
    ) -> Result<(), FindActionError> {
        match ctx {
            RecordOrType::Record(record) => {
                for (key, attr) in record.attributes.iter() {
                    entities.insert(CtxAttribute {
                        namespace: namespace.to_string(),
                        key: key.to_string(),
                        kind: attr.get_type()?,
                    });
                }
            },
            RecordOrType::Type(entity_type) => match entity_type {
                CedarSchemaEntityType::Primitive(primitive_type) => {
                    if let PrimitiveTypeKind::TypeName(type_name) = &primitive_type.kind {
                        let cedar_type = self.find_type(type_name, namespace).unwrap();
                        match cedar_type {
                            SchemaDefinedType::CommonType(common) => {
                                for (key, attr) in common.attributes.iter() {
                                    entities.insert(CtxAttribute {
                                        namespace: namespace.to_string(),
                                        key: key.to_string(),
                                        kind: attr.get_type()?,
                                    });
                                }
                            },
                            SchemaDefinedType::Entity(_) => {
                                Err(FindActionError::EntityContext(entity_type.clone()))?
                            },
                        }
                    }
                },
                CedarSchemaEntityType::Set(_) => {
                    Err(FindActionError::SetContext(entity_type.clone()))?
                },
                CedarSchemaEntityType::Typed(_) => {
                    Err(FindActionError::TypedContext(entity_type.clone()))?
                },
            },
        }

        Ok(())
    }
}

/// Represents an action in the Cedar JSON schema
#[derive(Default, Debug, PartialEq, Clone)]
pub struct ActionSchema {
    pub resource_types: HashSet<String>,
    pub principal_types: HashSet<String>,
    pub context: Option<RecordOrType>,
}

impl Serialize for ActionSchema {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: serde::Serializer,
    {
        let mut state = serializer.serialize_map(Some(1))?;
        state.serialize_entry(
            "appliesTo",
            &json!({
                "resourceTypes": self.resource_types,
                "principalTypes": self.principal_types,
                "context": self.context,
            }),
        )?;
        state.end()
    }
}

impl<'de> Deserialize<'de> for ActionSchema {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let mut action = HashMap::<String, HashMap<String, Value>>::deserialize(deserializer)?;
        let mut action = action
            .remove("appliesTo")
            .ok_or(de::Error::missing_field("appliesTo"))?;

        let resource_types = action
            .remove("resourceTypes")
            .map(|val| serde_json::from_value::<HashSet<String>>(val).map_err(de::Error::custom))
            .transpose()?
            .ok_or(de::Error::missing_field("resourceTypes"))?;

        let principal_types = action
            .remove("principalTypes")
            .map(|val| serde_json::from_value::<HashSet<String>>(val).map_err(de::Error::custom))
            .transpose()?
            .ok_or(de::Error::missing_field("principalTypes"))?;

        let context = action
            .remove("context")
            .map(|val| serde_json::from_value::<RecordOrType>(val).map_err(de::Error::custom))
            .transpose()?;

        Ok(Self {
            resource_types,
            principal_types,
            context,
        })
    }
}

#[derive(Debug, PartialEq, Clone, Serialize)]
pub enum RecordOrType {
    Record(CedarSchemaRecord),
    Type(CedarSchemaEntityType),
}

impl<'de> Deserialize<'de> for RecordOrType {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: de::Deserializer<'de>,
    {
        let mut context = HashMap::<String, Value>::deserialize(deserializer)?;
        let context_type = context
            .remove("type")
            .map(|val| serde_json::from_value::<String>(val).map_err(de::Error::custom))
            .transpose()?
            .ok_or(de::Error::missing_field("type"))?;

        match context_type.as_str() {
            "Record" => {
                let attributes = context
                    .remove("attributes")
                    .map(|val| {
                        serde_json::from_value::<HashMap<AttrName, CedarSchemaEntityAttribute>>(val)
                            .map_err(de::Error::custom)
                    })
                    .transpose()?
                    .ok_or(de::Error::missing_field("attributes"))?;
                Ok(RecordOrType::Record(CedarSchemaRecord {
                    entity_type: "Record".to_string(),
                    attributes,
                }))
            },
            type_name => Ok(RecordOrType::Type(CedarSchemaEntityType::Primitive(
                PrimitiveType {
                    kind: PrimitiveTypeKind::TypeName(type_name.to_string()),
                },
            ))),
        }
    }
}

#[derive(Debug, thiserror::Error)]
pub enum FindActionError {
    #[error("Error while collecting entities from action schema: {0}")]
    CollectEntities(#[from] GetCedarTypeError),
    #[error("Using `Set` as the context type is unsupported: {0:#?}")]
    SetContext(CedarSchemaEntityType),
    #[error("Using `Entity` as the context type is unsupported: {0:#?}")]
    EntityContext(CedarSchemaEntityType),
    #[error("Using `Typed` as the context type is unsupported: {0:#?}")]
    TypedContext(CedarSchemaEntityType),
}

#[cfg(test)]
mod test {
    use super::ActionSchema;
    use crate::common::cedar_schema::cedar_json::{
        action::RecordOrType,
        entity_types::{
            CedarSchemaEntityAttribute, CedarSchemaEntityType, EntityType, PrimitiveType,
            PrimitiveTypeKind,
        },
        CedarSchemaRecord,
    };
    use serde::Deserialize;
    use serde_json::{json, Value};
    use std::collections::{HashMap, HashSet};

    type ActionType = String;
    #[derive(Deserialize, Debug, PartialEq)]
    struct MockJsonSchema {
        actions: HashMap<ActionType, ActionSchema>,
    }

    fn build_schema(ctx: Option<Value>) -> Value {
        let mut schema = json!({
            "actions": {
                "Update": {
                    "appliesTo": {
                        "resourceTypes": ["Issue"],
                        "principalTypes": ["Workload", "User"]
                    }
                }
            }
        });
        if let Some(ctx) = ctx {
            schema["actions"]["Update"]["appliesTo"]["context"] = ctx;
        }
        schema
    }

    fn build_expected(ctx: Option<RecordOrType>) -> MockJsonSchema {
        MockJsonSchema {
            actions: HashMap::from([(
                "Update".to_string(),
                ActionSchema {
                    resource_types: HashSet::from(["Issue"].map(|s| s.to_string())),
                    principal_types: HashSet::from(["Workload", "User"].map(|s| s.to_string())),
                    context: ctx,
                },
            )]),
        }
    }

    #[test]
    pub fn can_deserialize_empty_ctx() {
        let schema = build_schema(None);

        let result = serde_json::from_value::<MockJsonSchema>(schema)
            .expect("Value should be deserialized successfully");

        let expected = build_expected(None);

        assert_eq!(result, expected)
    }

    #[test]
    pub fn can_deserialize_record_ctx() {
        let schema = build_schema(Some(json!({
            "type": "Record",
            "attributes": {
                "token": {
                    "type": "EntityOrCommon",
                    "name": "Access_token"
                },
                "username": {
                    "type": "EntityOrCommon",
                    "name": "String"
                }
            }
        })));

        let result = serde_json::from_value::<MockJsonSchema>(schema)
            .expect("Value should be deserialized successfully");

        let expected = build_expected(Some(RecordOrType::Record(CedarSchemaRecord {
            entity_type: "Record".to_string(),
            attributes: HashMap::from([
                (
                    "token".to_string(),
                    CedarSchemaEntityAttribute {
                        cedar_type: CedarSchemaEntityType::Typed(EntityType {
                            kind: "EntityOrCommon".to_string(),
                            name: "Access_token".to_string(),
                        }),
                        required: true,
                    },
                ),
                (
                    "username".to_string(),
                    CedarSchemaEntityAttribute {
                        cedar_type: CedarSchemaEntityType::Typed(EntityType {
                            kind: "EntityOrCommon".to_string(),
                            name: "String".to_string(),
                        }),
                        required: true,
                    },
                ),
            ]),
        })));

        assert_eq!(result, expected)
    }

    #[test]
    pub fn can_deserialize_entity_or_common_ctx() {
        let schema = build_schema(Some(json!({
            "type": "Context",
        })));

        let result = serde_json::from_value::<MockJsonSchema>(schema)
            .expect("Value should be deserialized successfully");

        let expected = build_expected(Some(RecordOrType::Type(CedarSchemaEntityType::Primitive(
            PrimitiveType {
                kind: PrimitiveTypeKind::TypeName("Context".to_string()),
            },
        ))));

        assert_eq!(result, expected)
    }
}
