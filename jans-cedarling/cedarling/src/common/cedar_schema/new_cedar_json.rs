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

/// A macro to define a newtype wrapping a `String` with automatic implementations for
/// converting from `&str` and `String`.
///
/// # Usage
///
/// ```rust,ignore
/// define_newtype!(AttributeName);
/// define_newtype!(ActionName);
///
/// let attr: AttributeName = "example_attr".into();
/// let action: ActionName = String::from("example_action").into();
///
/// println!("{:?}, {:?}", attr, action);
/// ```
macro_rules! define_newtype {
    ($name:ident) => {
        #[derive(Debug, Clone, PartialEq, Eq, Hash, Deserialize)]
        pub struct $name(pub String);

        impl From<&str> for $name {
            fn from(value: &str) -> Self {
                $name(value.to_string())
            }
        }

        impl From<String> for $name {
            fn from(value: String) -> Self {
                $name(value)
            }
        }
    };
}

define_newtype!(ActionName);
define_newtype!(ActionGroupName);
define_newtype!(AttributeName);
define_newtype!(CommonTypeName);
define_newtype!(EntityName);
define_newtype!(EntityTypeName);
define_newtype!(EntityOrCommonName);
define_newtype!(ExtensionName);
define_newtype!(NamespaceName);

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
