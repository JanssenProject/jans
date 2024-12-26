// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::record_attr::RecordAttr;
use serde::Deserialize;
use std::collections::HashSet;

pub type EntityTypeName = String;

#[derive(Debug, PartialEq, Deserialize)]
pub struct EntityType {
    #[serde(rename = "memberOfTypes")]
    member_of: Option<HashSet<EntityTypeName>>,
    shape: RecordAttr,
    tags: Option<RecordAttr>,
}

#[cfg(test)]
mod test_deserialize_entity_type {
    use super::super::record_attr::RecordAttr;
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
                shape: RecordAttr::record(HashMap::from([
                    ("name".into(), RecordAttr::string()),
                    ("age".into(), RecordAttr::long())
                ])),
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
                shape: RecordAttr::record(HashMap::from([
                    ("name".into(), RecordAttr::string()),
                    ("age".into(), RecordAttr::long())
                ])),
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
                shape: RecordAttr::record(HashMap::from([
                    ("name".into(), RecordAttr::string()),
                    ("age".into(), RecordAttr::long())
                ])),
                tags: Some(RecordAttr::set(RecordAttr::entity_or_common("String",)))
            }
        );
    }
}
