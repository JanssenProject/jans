// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::deserialize::*;
use super::*;
use serde::{de, Deserialize};
use serde_json::Value;
use std::collections::HashMap;

#[derive(Debug, PartialEq, Clone)]
pub enum Attribute {
    String {
        required: bool,
    },
    Long {
        required: bool,
    },
    Boolean {
        required: bool,
    },
    Record {
        required: bool,
        attrs: HashMap<AttributeName, Attribute>,
    },
    Set {
        required: bool,
        element: Box<Attribute>,
    },
    Entity {
        required: bool,
        name: EntityName,
    },
    Extension {
        required: bool,
        name: ExtensionName,
    },
    EntityOrCommon {
        required: bool,
        name: EntityOrCommonName,
    },
}

impl<'de> Deserialize<'de> for Attribute {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let mut attr = HashMap::<String, Value>::deserialize(deserializer)?;
        let kind = attr
            .remove("type")
            .ok_or(de::Error::missing_field("type"))?;
        let required = attr
            .remove("required")
            .map(serde_json::from_value::<bool>)
            .transpose()
            .map_err(|e| {
                de::Error::custom(format!("error while deserializing JSON Value to bool: {e}"))
            })?
            .unwrap_or(true);
        let kind = deserialize_to_string::<D>(kind)?;
        let attr = match kind.as_str() {
            "String" => Attribute::String { required },
            "Long" => Attribute::Long { required },
            "Boolean" => Attribute::Boolean { required },
            "Record" => {
                let attrs = attr
                    .remove("attributes")
                    .ok_or(de::Error::missing_field("attributes"))?;
                let attrs = deserialize_record_attrs::<D>(attrs)?;
                Self::Record { required, attrs }
            },
            "Set" => {
                let element = attr
                    .remove("element")
                    .ok_or(de::Error::missing_field("element"))?;
                let element = serde_json::from_value::<Attribute>(element).map_err(|e| {
                    de::Error::custom(format!(
                        "error while deserializing cedar element attribute: {e}"
                    ))
                })?;

                Self::Set {
                    required,
                    element: Box::new(element),
                }
            },
            "Entity" => {
                let name = attr
                    .remove("name")
                    .ok_or(de::Error::missing_field("name"))?;
                let name = deserialize_to_string::<D>(name)?;
                Self::Entity { required, name }
            },
            "Extension" => {
                let name = attr
                    .remove("name")
                    .ok_or(de::Error::missing_field("name"))?;
                let name = deserialize_to_string::<D>(name)?;
                Self::Extension { required, name }
            },
            "EntityOrCommon" => {
                let name = attr
                    .remove("name")
                    .ok_or(de::Error::missing_field("name"))?;
                let name = deserialize_to_string::<D>(name)?;
                Self::EntityOrCommon { required, name }
            },
            name => Self::EntityOrCommon {
                required,
                name: name.to_string(),
            },
        };

        Ok(attr)
    }
}

impl Attribute {
    pub fn is_required(&self) -> bool {
        *match self {
            Attribute::String { required } => required,
            Attribute::Long { required } => required,
            Attribute::Boolean { required } => required,
            Attribute::Record { required, .. } => required,
            Attribute::Set { required, .. } => required,
            Attribute::Entity { required, .. } => required,
            Attribute::Extension { required, .. } => required,
            Attribute::EntityOrCommon { required, .. } => required,
        }
    }
}

#[cfg(test)]
/// Helper methods to easily create required attributes
impl Attribute {
    pub fn string() -> Self {
        Self::String { required: true }
    }

    pub fn long() -> Self {
        Self::Long { required: true }
    }

    pub fn boolean() -> Self {
        Self::Boolean { required: true }
    }

    pub fn record(attrs: HashMap<AttributeName, Self>) -> Self {
        Self::Record {
            required: true,
            attrs,
        }
    }

    pub fn set(element: Self) -> Self {
        Self::Set {
            required: true,

            element: Box::new(element),
        }
    }

    pub fn entity(name: &str) -> Self {
        Self::Entity {
            required: true,
            name: name.into(),
        }
    }

    pub fn extension(name: &str) -> Self {
        Self::Extension {
            required: true,
            name: name.into(),
        }
    }

    pub fn entity_or_common(name: &str) -> Self {
        Self::EntityOrCommon {
            required: true,
            name: name.into(),
        }
    }
}

#[cfg(test)]
mod test {
    use super::Attribute;
    use serde_json::json;
    use std::collections::HashMap;

    #[test]
    fn can_deserialize_string() {
        let attr_json = json!({"type": "String"});
        let deserialized = serde_json::from_value::<Attribute>(attr_json).unwrap();
        assert_eq!(deserialized, Attribute::string());
    }

    #[test]
    fn can_deserialize_long() {
        let attr_json = json!({"type": "Long"});
        let deserialized = serde_json::from_value::<Attribute>(attr_json).unwrap();
        assert_eq!(deserialized, Attribute::long());
    }

    #[test]
    fn can_deserialize_boolean() {
        let attr_json = json!({"type": "Boolean"});
        let deserialized = serde_json::from_value::<Attribute>(attr_json).unwrap();
        assert_eq!(deserialized, Attribute::boolean());
    }

    #[test]
    fn can_deserialize_record() {
        let attr_json = json!({
            "type": "Record",
            "attributes": {
                "primary": { "type": "String" },
                "secondary": { "type": "String" },
            },
        });
        let deserialized = serde_json::from_value::<Attribute>(attr_json).unwrap();
        let expected = HashMap::from([
            ("primary".into(), Attribute::string()),
            ("secondary".into(), Attribute::string()),
        ]);
        assert_eq!(deserialized, Attribute::record(expected));
    }

    #[test]
    fn can_deserialize_set() {
        let attr_json = json!({
            "type": "Set",
            "element": {
                "type": "EntityOrCommon",
                "name": "Subscription"
            }
        });
        let deserialized = serde_json::from_value::<Attribute>(attr_json).unwrap();
        assert_eq!(
            deserialized,
            Attribute::set(Attribute::entity_or_common("Subscription"))
        );
    }

    #[test]
    fn can_deserialize_entity() {
        let attr_json = json!({
            "type": "Entity",
            "name": "Role",
        });
        let deserialized = serde_json::from_value::<Attribute>(attr_json).unwrap();
        assert_eq!(deserialized, Attribute::entity("Role"));
    }

    #[test]
    fn can_deserialize_extension() {
        let attr_json = json!({
            "type": "Extension",
            "name": "decimal",
        });
        let deserialized = serde_json::from_value::<Attribute>(attr_json).unwrap();
        assert_eq!(deserialized, Attribute::extension("decimal"),);
    }

    #[test]
    fn can_deserialize_entity_or_common() {
        let attr_json = json!({
            "type": "EntityOrCommon",
            "name": "String",
        });
        let deserialized = serde_json::from_value::<Attribute>(attr_json).unwrap();
        assert_eq!(deserialized, Attribute::entity_or_common("String"),);
    }

    #[test]
    fn can_deserialize_non_required_attr() {
        let attr_json = json!({"type": "String", "required": false});
        let deserialized = serde_json::from_value::<Attribute>(attr_json).unwrap();
        assert_eq!(deserialized, Attribute::String { required: false });
    }
}
