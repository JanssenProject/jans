use super::{
    entity_types::{
        CedarSchemaEntityAttribute, CedarSchemaEntityType, PrimitiveType, PrimitiveTypeKind,
    },
    CedarSchemaRecord,
};
use serde::{de, ser::SerializeMap, Deserialize, Serialize};
use serde_json::{json, Value};
use std::collections::{HashMap, HashSet};

/// Represents an action in the Cedar JSON schema
#[derive(Default, Debug, PartialEq, Clone)]
pub struct Action {
    pub resource_types: HashSet<String>,
    pub principal_types: HashSet<String>,
    pub context: Option<RecordOrType>,
}

impl Serialize for Action {
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

impl<'de> Deserialize<'de> for Action {
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

type AttrName = String;

#[derive(Debug, PartialEq, Clone, Serialize)]
#[allow(dead_code)]
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

#[cfg(test)]
mod test {
    use super::Action;
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
        actions: HashMap<ActionType, Action>,
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
                Action {
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
