// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! This module is responsible for deserializing the JSON Cedar schema

use crate::common::cedar_schema::CEDAR_NAMESPACE_SEPARATOR;
use action::Action;
use attribute::Attribute;
use cedar_policy::ParseErrors;
use serde::Deserialize;
use std::collections::HashSet;
use std::{collections::HashMap, str::FromStr};

pub(crate) mod action;
pub(crate) mod attribute;

mod deserialize;

const CEDAR_EMPTY_NAMESPACE: &str = "";

type ActionName = String;
type ActionGroupName = String;
type AttributeName = String;
type CommonTypeName = String;
type EntityName = String;
type EntityTypeName = String;
type EntityOrCommonName = String;
type NamespaceName = String;

/// Joins the given type name with the given namespace if it's not an empty string.
fn join_namespace(namespace: &str, type_name: &str) -> String {
    if namespace.is_empty() {
        return type_name.to_string();
    }
    [namespace, type_name].join(CEDAR_NAMESPACE_SEPARATOR)
}

#[derive(Debug, Deserialize, Clone)]
#[cfg_attr(test, derive(PartialEq))]
pub(crate) struct CedarSchemaJson {
    #[serde(flatten)]
    namespaces: HashMap<NamespaceName, Namespace>,
}

impl CedarSchemaJson {
    pub(crate) fn get_action(&self, namespace: &str, name: &str) -> Option<&Action> {
        self.namespaces
            .get(namespace)
            .and_then(|nmspce| nmspce.actions.get(name))
    }

    pub(crate) fn get_common_type(
        &self,
        type_name: &str,
        default_namespace: Option<&str>,
    ) -> Result<Option<(cedar_policy::EntityTypeName, &Attribute)>, Box<ParseErrors>> {
        let entity_type_name = cedar_policy::EntityTypeName::from_str(type_name)?;

        let namespace = entity_type_name.namespace();
        let basename = entity_type_name.basename();

        if !namespace.is_empty()
            && let Some(entity_schema) = self.get_comon_type_from_namespace(&namespace, basename)
        {
            return Ok(Some((entity_type_name, entity_schema)));
        }

        // If namespace is empty (in type_name), look for the type in the default namespace.
        if let Some(namespace) = default_namespace {
            if let Some(entity_schema) = self.get_comon_type_from_namespace(namespace, basename) {
                let entity_type_name =
                    cedar_policy::EntityTypeName::from_str(&join_namespace(namespace, type_name))?;

                return Ok(Some((entity_type_name, entity_schema)));
            }
        } else {
            // If the type is not found in the default namespace, look for it in the empty namespace.
            if let Some(entity_schema) =
                self.get_comon_type_from_namespace(CEDAR_EMPTY_NAMESPACE, basename)
            {
                let entity_type_name = cedar_policy::EntityTypeName::from_str(type_name)?;

                return Ok(Some((entity_type_name, entity_schema)));
            }
        }

        Ok(None)
    }

    fn get_comon_type_from_namespace(&self, namespace: &str, basename: &str) -> Option<&Attribute> {
        if let Some(namespace) = self.namespaces.get(namespace)
            && let Some(entity_type) = namespace.common_types.get(basename)
        {
            return Some(entity_type);
        }
        None
    }

    /// Resolves and returns the fully qualified entity type name for a given type name string.
    /// If `type_name` already carries a namespace, the lookup is performed in that namespace.
    /// Otherwise, or if not found, `default_namespace` is tried; if that also fails, the
    /// empty namespace is used as a final fallback. Returns `None` when no match is found.
    pub(crate) fn get_entity_type_name(
        &self,
        type_name: &str,
        default_namespace: Option<&str>,
    ) -> Result<Option<cedar_policy::EntityTypeName>, Box<ParseErrors>> {
        let entity_type_name = cedar_policy::EntityTypeName::from_str(type_name)?;

        let namespace = entity_type_name.namespace();
        let basename = entity_type_name.basename();

        if !namespace.is_empty()
            && self.entity_type_exists(&namespace, basename)
        {
            return Ok(Some(entity_type_name));
        }

        if let Some(namespace) = default_namespace
            && self.entity_type_exists(namespace, basename)
        {
            let entity_type_name =
                cedar_policy::EntityTypeName::from_str(&join_namespace(namespace, type_name))?;

            return Ok(Some(entity_type_name));
        }

        if self.entity_type_exists(CEDAR_EMPTY_NAMESPACE, basename) {
            let entity_type_name = cedar_policy::EntityTypeName::from_str(type_name)?;

            return Ok(Some(entity_type_name));
        }

        Ok(None)
    }

    fn entity_type_exists(&self, namespace: &str, basename: &str) -> bool {
        self.namespaces
            .get(namespace)
            .is_some_and(|nmspce| nmspce.entity_types.contains(basename))
    }
}

#[derive(Debug, Deserialize, Clone)]
#[cfg_attr(test, derive(PartialEq))]
struct Namespace {
    #[serde(
        rename = "entityTypes",
        default,
        deserialize_with = "deserialize_entity_type_names"
    )]
    entity_types: HashSet<EntityTypeName>,
    #[serde(rename = "commonTypes", default)]
    common_types: HashMap<CommonTypeName, Attribute>,
    #[serde(default)]
    actions: HashMap<ActionName, Action>,
}

fn deserialize_entity_type_names<'de, D>(
    deserializer: D,
) -> Result<HashSet<EntityTypeName>, D::Error>
where
    D: serde::Deserializer<'de>,
{
    struct EntityTypeNamesVisitor;

    impl<'de> serde::de::Visitor<'de> for EntityTypeNamesVisitor {
        type Value = HashSet<EntityTypeName>;

        fn expecting(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
            f.write_str("a map of entity type names")
        }

        fn visit_map<A>(self, mut map: A) -> Result<Self::Value, A::Error>
        where
            A: serde::de::MapAccess<'de>,
        {
            let mut names = HashSet::new();
            // Only the keys (entity type names) are needed; values are ignored.
            while let Some(key) = map.next_key::<EntityTypeName>()? {
                map.next_value::<serde::de::IgnoredAny>()?;
                names.insert(key);
            }
            Ok(names)
        }
    }

    deserializer.deserialize_map(EntityTypeNamesVisitor)
}

#[cfg(test)]
mod test_deserialize_json_cedar_schema {
    use super::*;
    use serde_json::json;
    use test_utils::assert_eq;

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
            entity_types: HashSet::from(["User".into(), "UserGroup".into()]),
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

    /// Tests if the entity can be found in the given `default_namespace`
    #[test]
    fn can_get_entity_from_default_namespace() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": {
                "entityTypes": {
                    "Workload": {
                        "shape": {
                            "type": "Record",
                            "attributes":  {},
                        }
                    }
                }
            },
        }))
        .expect("should successfully build schema");
        assert!(
            schema.namespaces.contains_key("Jans"),
            "schema should contain the \"Jans\" namespace"
        );
        let entity_type_name = schema
            .get_entity_type_name("Workload", Some("Jans"))
            .expect("should not error while calling getting schema for Workload")
            .expect("should find workload entity in schema");
        assert_eq!(
            entity_type_name,
            cedar_policy::EntityTypeName::from_str("Jans::Workload")
                .expect("should parse workload entity type name")
        );
    }

    /// Tests if the entity wont be found if it's not in the `""` namespace or
    /// in the given `default_namespace`
    #[test]
    fn should_not_get_entity_from_another_namespace() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "Custom": {
                "entityTypes": {
                    "Another_entity": {},
                }
            }
        }))
        .expect("should successfully build schema");
        assert!(
            schema.namespaces.contains_key("Custom"),
            "schema should contain the \"Custom\" namespace"
        );
        let result = schema
            .get_entity_type_name("Another_entity", Some("Jans"))
            .expect("should not error while calling getting schema for Another_entity");
        assert_eq!(result, None);
    }

    // Test if the entity can be found on the `""` namespace if it's not
    // in the given `default_namespace`
    #[test]
    fn should_get_entity_from_default_namespace() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "": {
                "entityTypes": {
                    "Some_entity": {},
                }
            },
        }))
        .expect("should successfully build schema");
        assert!(
            schema.namespaces.contains_key(""),
            "schema should countain the `\"\"` namespace"
        );
        let entity_type_name = schema
            .get_entity_type_name("Some_entity", Some("Jans"))
            .expect("should not error while calling getting schema for Some_entity")
            .expect("should find Some_entity in schema");
        assert_eq!(
            entity_type_name,
            cedar_policy::EntityTypeName::from_str("Some_entity")
                .expect("should parse Some_entity entity type name")
        );
    }

    #[test]
    fn can_get_entity_from_namespace() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": {
                "entityTypes": {
                    "SomeEntity": {
                        "shape": {
                            "type": "Record",
                            "attributes":  {},
                        }
                    }
                }
            },
            "": {
                "entityTypes": {
                    "AnotherEntity": {
                        "shape": {
                            "type": "Record",
                            "attributes":  {},
                        }
                    }
                }
            },
        }))
        .expect("should successfully build schema");
        assert_eq!(
            schema
                .namespaces
                .keys()
                .map(std::string::String::as_str)
                .collect::<HashSet<&str>>(),
            HashSet::from(["", "Jans"])
        );
        assert!(
            schema.entity_type_exists("Jans", "SomeEntity"),
            "should get entity from \"Jans\" namespace"
        );
        assert!(
            schema.entity_type_exists("", "AnotherEntity"),
            "should get entity from `\"\"` namespace"
        );
    }
}
