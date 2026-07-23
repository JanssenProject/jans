// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::deserialize::deserialize_record_attrs;
use super::{AttributeName, EntityName, EntityOrCommonName};
use serde::{Deserialize, de};
use serde_json::Value;
use std::collections::HashMap;

#[derive(Debug, Clone)]
#[cfg_attr(test, derive(PartialEq))]
pub(crate) enum Attribute {
    String,
    Long,
    Boolean,
    Record {
        attrs: HashMap<AttributeName, Attribute>,
    },
    Set,
    Entity {
        required: bool,
        name: EntityName,
    },
    Extension,
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
        let kind = String::deserialize(&kind).map_err(de::Error::custom)?;
        let attr = match kind.as_str() {
            "String" => Attribute::String,
            "Long" => Attribute::Long,
            "Boolean" => Attribute::Boolean,
            "Record" => {
                let attrs = attr
                    .remove("attributes")
                    .ok_or(de::Error::missing_field("attributes"))?;
                let attrs = deserialize_record_attrs::<D>(attrs)?;
                Self::Record { attrs }
            },
            "Set" => Self::Set,
            "Entity" => {
                let name = attr
                    .remove("name")
                    .ok_or(de::Error::missing_field("name"))?;
                let name = String::deserialize(&name).map_err(de::Error::custom)?;
                Self::Entity { required, name }
            },
            "Extension" => Self::Extension,
            "EntityOrCommon" => {
                let name = attr
                    .remove("name")
                    .ok_or(de::Error::missing_field("name"))?;
                let name = String::deserialize(&name).map_err(de::Error::custom)?;
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

#[cfg(test)]
/// Helper methods to easily create required attributes
impl Attribute {
    pub(crate) fn string() -> Self {
        Self::String
    }

    pub(crate) fn long() -> Self {
        Self::Long
    }

    pub(crate) fn boolean() -> Self {
        Self::Boolean
    }

    pub(crate) fn record(attrs: HashMap<AttributeName, Self>) -> Self {
        Self::Record { attrs }
    }

    pub(crate) fn set(_element: Self) -> Self {
        Self::Set
    }

    pub(crate) fn entity(name: &str) -> Self {
        Self::Entity {
            required: true,
            name: name.into(),
        }
    }

    pub(crate) fn extension(_name: &str) -> Self {
        Self::Extension
    }

    pub(crate) fn entity_or_common(name: &str) -> Self {
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
        assert_eq!(deserialized, Attribute::String);
    }
}
