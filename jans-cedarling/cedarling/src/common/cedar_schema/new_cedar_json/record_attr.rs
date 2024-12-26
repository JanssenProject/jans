// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use serde::{de, Deserialize};
use serde_json::Value;
use std::collections::HashMap;

#[derive(Debug, PartialEq)]
#[allow(dead_code)]
pub enum RecordAttr {
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
        attrs: HashMap<String, RecordAttr>,
    },
    Set {
        required: bool,
        element: Box<RecordAttr>,
    },
    Entity {
        required: bool,
        name: String,
    },
    Extension {
        required: bool,
        name: String,
    },
    EntityOrCommon {
        required: bool,
        name: String,
    },
}

impl RecordAttr {
    const ATTR_VARIANTS: [&str; 8] = [
        "String",
        "Long",
        "Boolean",
        "Record",
        "Set",
        "Entity",
        "Extension",
        "EntityOrCommon",
    ];
}

#[cfg(test)]
#[allow(dead_code)]
/// Helper methods to easily create required attributes
impl RecordAttr {
    pub fn string() -> Self {
        Self::String { required: true }
    }

    pub fn long() -> Self {
        Self::Long { required: true }
    }

    pub fn boolean() -> Self {
        Self::Boolean { required: true }
    }

    pub fn record(attrs: HashMap<String, Self>) -> Self {
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

    pub fn entity(name: impl ToString) -> Self {
        Self::Entity {
            required: true,
            name: name.to_string(),
        }
    }

    pub fn extension(name: impl ToString) -> Self {
        Self::Extension {
            required: true,
            name: name.to_string(),
        }
    }

    pub fn entity_or_common(name: impl ToString) -> Self {
        Self::EntityOrCommon {
            required: true,
            name: name.to_string(),
        }
    }
}

impl<'de> Deserialize<'de> for RecordAttr {
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
            .map(|v| serde_json::from_value::<bool>(v))
            .transpose()
            .map_err(|e| {
                de::Error::custom(format!("error while deserializing JSON Value to bool: {e}"))
            })?
            .unwrap_or(true);
        let kind = deserialize_to_string::<D>(kind)?;
        let attr = match kind.as_str() {
            "String" => RecordAttr::String { required },
            "Long" => RecordAttr::Long { required },
            "Boolean" => RecordAttr::Boolean { required },
            "Record" => {
                let record_attrs = attr
                    .remove("attributes")
                    .ok_or(de::Error::missing_field("attributes"))?;
                let attrs_json = serde_json::from_value::<HashMap<String, Value>>(record_attrs)
                    .map_err(|e| {
                        de::Error::custom(format!(
                            "error while deserializing cedar record attribute: {e}"
                        ))
                    })?;

                // loop through each attr then deserialize into Self
                let mut attrs = HashMap::<String, RecordAttr>::new();
                for (key, val) in attrs_json.into_iter() {
                    let val = serde_json::from_value::<RecordAttr>(val).map_err(|e| {
                        de::Error::custom(format!(
                            "error while deserializing cedar record attribute: {e}"
                        ))
                    })?;
                    attrs.insert(key, val);
                }
                Self::Record { required, attrs }
            },
            "Set" => {
                let element = attr
                    .remove("element")
                    .ok_or(de::Error::missing_field("element"))?;
                let element = serde_json::from_value::<RecordAttr>(element).map_err(|e| {
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

            variant => return Err(de::Error::unknown_variant(variant, &Self::ATTR_VARIANTS)),
        };

        Ok(attr)
    }
}

/// Deserializes a [`Value`] to a String
fn deserialize_to_string<'de, D>(value: Value) -> Result<String, D::Error>
where
    D: serde::Deserializer<'de>,
{
    serde_json::from_value::<String>(value).map_err(|e| {
        de::Error::custom(format!(
            "error while desrializing JSON Value to a String: {e}"
        ))
    })
}

#[cfg(test)]
mod test_deserialize_record_attr {
    use super::RecordAttr;
    use serde_json::json;
    use std::collections::HashMap;

    #[test]
    fn can_deserialize_string() {
        let attr_json = json!({"type": "String"});
        let deserialized = serde_json::from_value::<RecordAttr>(attr_json).unwrap();
        assert_eq!(deserialized, RecordAttr::string());
    }

    #[test]
    fn can_deserialize_long() {
        let attr_json = json!({"type": "Long"});
        let deserialized = serde_json::from_value::<RecordAttr>(attr_json).unwrap();
        assert_eq!(deserialized, RecordAttr::long());
    }

    #[test]
    fn can_deserialize_boolean() {
        let attr_json = json!({"type": "Boolean"});
        let deserialized = serde_json::from_value::<RecordAttr>(attr_json).unwrap();
        assert_eq!(deserialized, RecordAttr::boolean());
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
        let deserialized = serde_json::from_value::<RecordAttr>(attr_json).unwrap();
        let expected = HashMap::from([
            ("primary".into(), RecordAttr::string()),
            ("secondary".into(), RecordAttr::string()),
        ]);
        assert_eq!(deserialized, RecordAttr::record(expected));
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
        let deserialized = serde_json::from_value::<RecordAttr>(attr_json).unwrap();
        assert_eq!(
            deserialized,
            RecordAttr::set(RecordAttr::entity_or_common("Subscription"))
        );
    }

    #[test]
    fn can_deserialize_entity() {
        let attr_json = json!({
            "type": "Entity",
            "name": "Role",
        });
        let deserialized = serde_json::from_value::<RecordAttr>(attr_json).unwrap();
        assert_eq!(deserialized, RecordAttr::entity("Role"));
    }

    #[test]
    fn can_deserialize_extension() {
        let attr_json = json!({
            "type": "Extension",
            "name": "decimal",
        });
        let deserialized = serde_json::from_value::<RecordAttr>(attr_json).unwrap();
        assert_eq!(deserialized, RecordAttr::extension("decimal"),);
    }

    #[test]
    fn can_deserialize_entity_or_common() {
        let attr_json = json!({
            "type": "EntityOrCommon",
            "name": "String",
        });
        let deserialized = serde_json::from_value::<RecordAttr>(attr_json).unwrap();
        assert_eq!(deserialized, RecordAttr::entity_or_common("String"),);
    }

    #[test]
    fn can_deserialize_non_required_attr() {
        let attr_json = json!({"type": "String", "required": false});
        let deserialized = serde_json::from_value::<RecordAttr>(attr_json).unwrap();
        assert_eq!(deserialized, RecordAttr::String { required: false });
    }

    #[test]
    fn errors_on_invalid_type() {
        let attr_json = json!({"type": "InvalidType"});
        let err = serde_json::from_value::<RecordAttr>(attr_json)
            .unwrap_err()
            .to_string();
        assert!(
            err.contains("unknown variant `InvalidType`"),
            "unexpected error: {err}"
        );
    }
}
