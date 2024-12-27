// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::attr_kind::AttributeKind;
use super::deserialize::*;
use super::*;
use serde::{de, Deserialize};
use serde_json::Value;
use std::collections::HashSet;

#[derive(Debug, PartialEq)]
pub struct EntityShape {
    required: bool,
    attrs: HashMap<AttributeName, AttributeKind>,
}

#[cfg(test)]
impl EntityShape {
    pub fn required(attrs: HashMap<AttributeName, AttributeKind>) -> Self {
        Self {
            required: true,
            attrs,
        }
    }
}

#[derive(Debug, PartialEq, Deserialize)]
pub struct EntityType {
    #[serde(rename = "memberOfTypes")]
    pub member_of: Option<HashSet<EntityTypeName>>,
    #[serde(deserialize_with = "deserialize_entity_shape", default)]
    pub shape: Option<EntityShape>,
    #[serde(default)]
    pub tags: Option<AttributeKind>,
}

// Forces the `shape` field into the [`AttributeKind::Shape`] variant.
fn deserialize_entity_shape<'de, D>(deserializer: D) -> Result<Option<EntityShape>, D::Error>
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
        "Record" => {
            let attrs = attr
                .remove("attributes")
                .ok_or(de::Error::missing_field("attributes"))?;
            let attrs = deserialize_record_attrs::<D>(attrs)?;
            EntityShape { required, attrs }
        },
        variant => {
            return Err(de::Error::custom(format!(
                "invalid type: {}, expected {}",
                variant, "Record"
            )))
        },
    };

    Ok(Some(attr))
}

#[cfg(test)]
mod test_deserialize_entity_type {
    use super::super::attr_kind::AttributeKind;
    use super::*;
    use serde_json::json;
    use std::collections::{HashMap, HashSet};
    use test_utils::assert_eq;

    #[test]
    fn can_deserialize() {
        let entity_type = json!({
            "shape": {
                "type": "Record",
                "attributes": {
                    "name": {"type": "String"},
                    "age": {"type": "Long"},
                },
            },
        });
        let entity_type = serde_json::from_value::<EntityType>(entity_type).unwrap();
        assert_eq!(
            entity_type,
            EntityType {
                member_of: None,
                shape: Some(EntityShape::required(HashMap::from([
                    ("name".into(), AttributeKind::string()),
                    ("age".into(), AttributeKind::long())
                ]))),
                tags: None,
            }
        );
    }

    #[test]
    fn can_deserialize_with_member_of() {
        let with_member_of = json!({
            "memberOfTypes": ["UserGroup"],
            "shape": {
                "type": "Record",
                "attributes": {
                    "name": {"type": "String"},
                    "age": {"type": "Long"},
                },
            },
        });
        let with_member_of = serde_json::from_value::<EntityType>(with_member_of).unwrap();
        assert_eq!(
            with_member_of,
            EntityType {
                member_of: Some(HashSet::from(["UserGroup".into()])),
                shape: Some(EntityShape::required(HashMap::from([
                    ("name".into(), AttributeKind::string()),
                    ("age".into(), AttributeKind::long())
                ]))),
                tags: None,
            }
        );
    }

    #[test]
    fn can_deserialize_with_tags() {
        let with_tags = json!({
            "shape": {
                "type": "Record",
                "attributes": {
                    "name": {"type": "String"},
                    "age": {"type": "Long"},
                },
            },
            "tags": {
                "type": "Set",
                "element": {
                    "type": "EntityOrCommon",
                    "name": "String"
                }
            }
        });
        let with_tags = serde_json::from_value::<EntityType>(with_tags).unwrap();
        assert_eq!(
            with_tags,
            EntityType {
                member_of: None,
                shape: Some(EntityShape::required(HashMap::from([
                    ("name".into(), AttributeKind::string()),
                    ("age".into(), AttributeKind::long())
                ]))),
                tags: Some(AttributeKind::set(AttributeKind::entity_or_common(
                    "String",
                )))
            }
        );
    }

    #[test]
    fn errors_on_invalid_shape() {
        let entity_type = json!({
            "shape": {
                "type": "Set",
            },
        });
        let err = serde_json::from_value::<EntityType>(entity_type).unwrap_err();
        assert!(err
            .to_string()
            .contains("invalid type: Set, expected Record"));
    }
}
