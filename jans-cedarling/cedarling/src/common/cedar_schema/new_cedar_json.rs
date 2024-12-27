// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! This module is responsible for deserializing the JSON Cedar schema

use serde::Deserialize;
use std::collections::HashMap;

mod action;
mod attr_kind;
mod deserialize;
mod entity_type;

use action::*;
use attr_kind::*;
use entity_type::*;

pub type ActionName = String;
pub type ActionGroupName = String;
pub type AttributeName = String;
pub type CommonTypeName = String;
pub type EntityName = String;
pub type EntityTypeName = String;
pub type EntityOrCommonName = String;
pub type ExtensionName = String;
pub type NamespaceName = String;

#[derive(Debug, Deserialize, PartialEq)]
pub struct CedarSchemaJson {
    #[serde(flatten)]
    namespaces: HashMap<NamespaceName, Namespace>,
}

#[derive(Debug, Deserialize, PartialEq)]
pub struct Namespace {
    #[serde(rename = "entityTypes", default)]
    entity_types: HashMap<EntityTypeName, EntityType>,
    #[serde(rename = "commonTypes", default)]
    common_types: HashMap<CommonTypeName, AttributeKind>,
    #[serde(default)]
    actions: HashMap<ActionName, Action>,
}

#[cfg(test)]
mod test_deserialize_json_cedar_schema {
    use super::*;
    use serde_json::json;
    use std::collections::HashSet;

    #[test]
    fn can_deserialize_entity_types() {
        let schema = json!({
            "Jans": {
                "entityTypes": {
                    "User": {
                        "memberOfTypes": [ "UserGroup" ],
                        "shape": {
                            "type": "Record",
                            "attributes": {
                                "department": { "type": "String" },
                                "jobLevel": { "type": "Long" }
                            }
                        }
                    },
                    "UserGroup": {},
                },
            }
        });
        let schema = serde_json::from_value::<CedarSchemaJson>(schema).unwrap();
        let namespace = Namespace {
            entity_types: HashMap::from([
                (
                    "User".into(),
                    EntityType {
                        member_of: Some(HashSet::from(["UserGroup".into()])),
                        shape: Some(EntityShape::required(HashMap::from([
                            ("department".into(), AttributeKind::string()),
                            ("jobLevel".into(), AttributeKind::long()),
                        ]))),
                        tags: None,
                    },
                ),
                (
                    "UserGroup".into(),
                    EntityType {
                        member_of: None,
                        shape: None,
                        tags: None,
                    },
                ),
            ]),
            common_types: HashMap::new(),
            actions: HashMap::new(),
        };
        assert_eq!(
            schema,
            CedarSchemaJson {
                namespaces: HashMap::from([("Jans".into(), namespace)])
            }
        );
    }
}
