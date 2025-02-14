// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! This module is responsible for deserializing the JSON Cedar schema

use crate::common::cedar_schema::CEDAR_NAMESPACE_SEPARATOR;
use action::*;
use attribute::*;
use cedar_policy::ParseErrors;
use entity_type::*;
use serde::Deserialize;
use std::{collections::HashMap, str::FromStr};

pub(crate) mod action;
pub(crate) mod attribute;
pub(crate) mod entity_type;

mod deserialize;

const CEDAR_EMPTY_NAMESPACE: &str = "";

pub type ActionName = String;
pub type ActionGroupName = String;
pub type AttributeName = String;
pub type CommonTypeName = String;
pub type EntityName = String;
pub type EntityTypeName = String;
pub type EntityOrCommonName = String;
pub type ExtensionName = String;
pub type NamespaceName = String;

/// Joins the given type name with the given namespace if it's not an empty string.
fn join_namespace(namespace: &str, type_name: &str) -> String {
    if namespace.is_empty() {
        return type_name.to_string();
    }
    [namespace, type_name].join(CEDAR_NAMESPACE_SEPARATOR)
}

#[derive(Debug, Deserialize, PartialEq, Clone)]
pub struct CedarSchemaJson {
    #[serde(flatten)]
    namespaces: HashMap<NamespaceName, Namespace>,
}

impl CedarSchemaJson {
    pub fn get_action(&self, namespace: &str, name: &str) -> Option<&Action> {
        self.namespaces
            .get(namespace)
            .and_then(|nmspce| nmspce.actions.get(name))
    }

    pub fn get_common_type(
        &self,
        type_name: &str,
        default_namespace: Option<&str>,
    ) -> Result<Option<(cedar_policy::EntityTypeName, &Attribute)>, ParseErrors> {
        let entity_type_name = cedar_policy::EntityTypeName::from_str(type_name)?;

        let namespace = entity_type_name.namespace();
        let basename = entity_type_name.basename();

        if !namespace.is_empty() {
            if let Some(entity_schema) = self.get_comon_type_from_namespace(&namespace, basename) {
                return Ok(Some((entity_type_name, entity_schema)));
            }
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
        if let Some(namespace) = self.namespaces.get(namespace) {
            if let Some(entity_type) = namespace.common_types.get(basename) {
                return Some(entity_type);
            }
        }
        None
    }

    /// Get the entity schema for a given type name.
    /// `default_namespace` is the default namespace for entities to search if no namespace is provided in type_name.
    ///
    /// If the type name does not have namespace, it will look for the type in the default namespace.
    /// If not found in default namespace, it will look with `empty` namespace (value: "").
    pub fn get_entity_schema(
        &self,
        type_name: &str,
        default_namespace: Option<&str>,
    ) -> Result<Option<(cedar_policy::EntityTypeName, &EntityType)>, ParseErrors> {
        let entity_type_name = cedar_policy::EntityTypeName::from_str(type_name)?;

        let namespace = entity_type_name.namespace();
        let basename = entity_type_name.basename();

        if !namespace.is_empty() {
            if let Some(entity_schema) = self.get_entity_schema_from_namespace(&namespace, basename)
            {
                return Ok(Some((entity_type_name, entity_schema)));
            }
        }

        // If namespace is empty (in type_name), look for the type in the default namespace.
        if let Some(namespace) = default_namespace {
            if let Some(entity_schema) = self.get_entity_schema_from_namespace(namespace, basename)
            {
                let entity_type_name =
                    cedar_policy::EntityTypeName::from_str(&join_namespace(namespace, type_name))?;

                return Ok(Some((entity_type_name, entity_schema)));
            }
        } else {
            // If the type is not found in the default namespace, look for it in the empty namespace.
            if let Some(entity_schema) =
                self.get_entity_schema_from_namespace(CEDAR_EMPTY_NAMESPACE, basename)
            {
                let entity_type_name = cedar_policy::EntityTypeName::from_str(type_name)?;

                return Ok(Some((entity_type_name, entity_schema)));
            }
        }

        Ok(None)
    }

    fn get_entity_schema_from_namespace(
        &self,
        namespace: &str,
        basename: &str,
    ) -> Option<&EntityType> {
        if let Some(namespace) = self.namespaces.get(namespace) {
            if let Some(entity_type) = namespace.entity_types.get(basename) {
                return Some(entity_type);
            }
        }
        None
    }
}

#[derive(Debug, Deserialize, PartialEq, Clone)]
pub struct Namespace {
    #[serde(rename = "entityTypes", default)]
    entity_types: HashMap<EntityTypeName, EntityType>,
    #[serde(rename = "commonTypes", default)]
    common_types: HashMap<CommonTypeName, Attribute>,
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
                ("User".into(), EntityType {
                    member_of: Some(HashSet::from(["UserGroup".into()])),
                    shape: Some(EntityShape::required(HashMap::from([
                        ("department".into(), Attribute::string()),
                        ("jobLevel".into(), Attribute::long()),
                    ]))),
                    tags: None,
                }),
                ("UserGroup".into(), EntityType {
                    member_of: None,
                    shape: None,
                    tags: None,
                }),
            ]),
            common_types: HashMap::new(),
            actions: HashMap::new(),
        };
        assert_eq!(schema, CedarSchemaJson {
            namespaces: HashMap::from([("Jans".into(), namespace)])
        });
    }
}
