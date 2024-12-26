// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::attr_kind::AttributeKind;
use super::*;
use serde::Deserialize;
use std::collections::HashSet;

#[derive(Debug, PartialEq, Deserialize)]
pub struct EntityType {
    #[serde(rename = "memberOfTypes")]
    pub member_of: Option<HashSet<EntityTypeName>>,
    #[serde(default)]
    pub shape: Option<AttributeKind>,
    #[serde(default)]
    pub tags: Option<AttributeKind>,
}

#[cfg(test)]
mod test_deserialize_entity_type {
    use super::super::attr_kind::AttributeKind;
    use super::EntityType;
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
                shape: Some(AttributeKind::record(HashMap::from([
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
                shape: Some(AttributeKind::record(HashMap::from([
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
                shape: Some(AttributeKind::record(HashMap::from([
                    ("name".into(), AttributeKind::string()),
                    ("age".into(), AttributeKind::long())
                ]))),
                tags: Some(AttributeKind::set(AttributeKind::entity_or_common(
                    "String",
                )))
            }
        );
    }
}
