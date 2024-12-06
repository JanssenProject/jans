use serde::{de, Deserialize};
use serde_json::Value;
use std::collections::{HashMap, HashSet};

/// Represents an action in the Cedar JSON schema
#[derive(Default, Debug, PartialEq)]
pub struct Action {
    resource_types: HashSet<String>,
    principal_types: HashSet<String>,
    context: Option<Context>,
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
            .map(|val| serde_json::from_value::<Context>(val).map_err(de::Error::custom))
            .transpose()?;

        Ok(Self {
            resource_types,
            principal_types,
            context,
        })
    }
}

type AttrName = String;
type EntityOrCommonName = String;

#[derive(Debug, PartialEq)]
pub enum Context {
    Record {
        attrs: HashMap<AttrName, ContextAttr>,
    },
    EntityOrCommon(EntityOrCommonName),
}

#[derive(Debug, PartialEq, Deserialize)]
pub struct ContextAttr {
    r#type: String,
    name: String,
}

impl<'de> Deserialize<'de> for Context {
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

        let context = match context_type.as_str() {
            "Record" => {
                let attrs = context
                    .remove("attributes")
                    .map(|val| {
                        serde_json::from_value::<HashMap<AttrName, ContextAttr>>(val)
                            .map_err(de::Error::custom)
                    })
                    .transpose()?
                    .ok_or(de::Error::missing_field("attributes"))?;
                Context::Record { attrs }
            },
            ctx_type => Context::EntityOrCommon(ctx_type.to_string()),
        };

        Ok(context)
    }
}

#[cfg(test)]
mod test {
    use super::{Action, Context, ContextAttr};
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

    fn build_expected(ctx: Option<Context>) -> MockJsonSchema {
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

        let expected = build_expected(Some(Context::Record {
            attrs: HashMap::from([
                (
                    "token".to_string(),
                    ContextAttr {
                        r#type: "EntityOrCommon".to_string(),
                        name: "Access_token".to_string(),
                    },
                ),
                (
                    "username".to_string(),
                    ContextAttr {
                        r#type: "EntityOrCommon".to_string(),
                        name: "String".to_string(),
                    },
                ),
            ]),
        }));

        assert_eq!(result, expected)
    }

    #[test]
    pub fn can_deserialize_entity_or_common_ctx() {
        let schema = build_schema(Some(json!({
            "type": "Context",
        })));

        let result = serde_json::from_value::<MockJsonSchema>(schema)
            .expect("Value should be deserialized successfully");

        let expected = build_expected(Some(Context::EntityOrCommon("Context".to_string())));

        assert_eq!(result, expected)
    }
}
