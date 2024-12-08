/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::{
    entity_types::{
        CedarSchemaEntityAttribute, CedarSchemaEntityType, PrimitiveType, PrimitiveTypeKind,
    },
    CedarSchemaJson, CedarSchemaRecord, CedarType, GetCedarTypeError, SchemaDefinedType,
};
use serde::{de, ser::SerializeMap, Deserialize, Serialize};
use serde_json::{json, Value};
use std::collections::{HashMap, HashSet};

type EntityRef = String;
type AttrName = String;

pub struct Action {
    pub entities: HashSet<String>,
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

        let mut entities = HashSet::new();

        for res_type in action_schema.principal_types.iter().map(|s| s.as_str()) {
            self.collect_entities(res_type, namespace, &mut entities)?;
        }
        for res_type in action_schema.resource_types.iter().map(|s| s.as_str()) {
            self.collect_entities(res_type, namespace, &mut entities)?;
        }
        if let Some(ctx) = &action_schema.context {
            self.process_action_context(ctx, namespace, &mut entities)?;
        }

        Ok(Some(Action { entities }))
    }

    /// Collect all entities recursively for a given type
    fn collect_entities(
        &self,
        type_name: &str,
        namespace: &str,
        entities: &mut HashSet<String>,
    ) -> Result<(), FindActionError> {
        if let Some(defined_type) = self.find_type(type_name, namespace) {
            match defined_type {
                SchemaDefinedType::Entity(shape) => {
                    if !entities.contains(type_name) {
                        entities.insert(type_name.to_string());
                    }
                    if let Some(shape) = &shape.shape {
                        for attr in shape.attributes.values() {
                            self.process_attribute(attr, namespace, entities)?;
                        }
                    }
                },
                SchemaDefinedType::CommonType(record) => {
                    for attr in record.attributes.values() {
                        self.process_attribute(attr, namespace, entities)?;
                    }
                },
            }
        }

        Ok(())
    }

    /// Process a single attribute and collect its entities recursively
    fn process_attribute<'a>(
        &'a self,
        attr: &CedarSchemaEntityAttribute,
        namespace: &str,
        entities: &mut HashSet<String>,
    ) -> Result<(), FindActionError> {
        match attr.get_type()? {
            CedarType::TypeName(type_name) => {
                self.collect_entities(&type_name, namespace, entities)?;
            },
            CedarType::Set(inner_type) => {
                if let CedarType::TypeName(type_name) = *inner_type {
                    self.collect_entities(&type_name, namespace, entities)?;
                }
            },
            _ => {}, // Ignore non-entity types
        }

        Ok(())
    }

    fn process_action_context(
        &self,
        ctx: &RecordOrType,
        namespace: &str,
        entities: &mut HashSet<String>,
    ) -> Result<(), FindActionError> {
        match ctx {
            RecordOrType::Record(record) => {
                for attr in record.attributes.values() {
                    self.process_attribute(attr, namespace, entities)?;
                }
            },
            RecordOrType::Type(entity_type) => match entity_type {
                CedarSchemaEntityType::Typed(entity_type) => {
                    self.collect_entities(&entity_type.kind, namespace, entities)?;
                },
                CedarSchemaEntityType::Primitive(primitive_type) => {
                    if let PrimitiveTypeKind::TypeName(type_name) = &primitive_type.kind {
                        self.collect_entities(type_name, namespace, entities)?;
                    }
                },
                CedarSchemaEntityType::Set(set_entity_type) => match &set_entity_type.element {
                    CedarSchemaEntityType::Set(inner) => {
                        self.process_action_context(
                            &RecordOrType::Type(CedarSchemaEntityType::Set(inner.clone())),
                            namespace,
                            entities,
                        )?;
                    },
                    CedarSchemaEntityType::Typed(entity_type) => {
                        self.collect_entities(&entity_type.kind, namespace, entities)?;
                    },
                    CedarSchemaEntityType::Primitive(primitive_type) => {
                        if let PrimitiveTypeKind::TypeName(type_name) = &primitive_type.kind {
                            self.collect_entities(type_name, namespace, entities)?;
                        }
                    },
                },
            },
        }

        Ok(())
    }
}

/// Represents an action in the Cedar JSON schema
#[derive(Default, Debug, PartialEq, Clone)]
pub struct ActionSchema {
    pub resource_types: HashSet<EntityRef>,
    pub principal_types: HashSet<EntityRef>,
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
