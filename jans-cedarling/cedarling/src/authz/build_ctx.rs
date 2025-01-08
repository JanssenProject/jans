// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashMap;

use super::{AuthorizeEntitiesData, AuthzConfig};
use crate::common::cedar_schema::cedar_json::attribute::Attribute;
use crate::common::cedar_schema::cedar_json::CedarSchemaJson;
use crate::common::cedar_schema::CEDAR_NAMESPACE_SEPARATOR;
use cedar_policy::ContextJsonError;
use serde_json::{json, map::Entry, Value};

/// Constructs the authorization context by adding the built entities from the tokens
pub fn build_context(
    config: &AuthzConfig,
    request_context: Value,
    entities_data: &AuthorizeEntitiesData,
    schema: &cedar_policy::Schema,
    action: &cedar_policy::EntityUid,
) -> Result<cedar_policy::Context, BuildContextError> {
    let namespace = config.policy_store.namespace();
    let action_name = &action.id().escaped();
    let json_schema = &config.policy_store.schema.json;
    let action_schema = json_schema
        .get_action(namespace, action_name)
        .ok_or(BuildContextError::UnknownAction(action_name.to_string()))?;

    // Get the entities required for the context
    let mut ctx_entity_refs = json!({});
    let type_ids = entities_data.type_ids();
    if let Some(ctx) = action_schema.applies_to.context.as_ref() {
        match ctx {
            Attribute::Record { attrs, .. } => {
                for (key, attr) in attrs.iter() {
                    if let Some(entity_ref) =
                        build_entity_refs_from_attr(namespace, attr, &type_ids, json_schema)?
                    {
                        ctx_entity_refs[key] = entity_ref;
                    }
                }
            },
            Attribute::EntityOrCommon { name, .. } => {
                // TODO: handle potential namespace collisions when Cedarling starts
                // supporting multiple namespaces
                if let Some((_namespace, attr)) = json_schema.get_common_type(name) {
                    match attr {
                        Attribute::Record { attrs, .. } => {
                            for (key, attr) in attrs.iter() {
                                if let Some(entity_ref) = build_entity_refs_from_attr(
                                    namespace,
                                    attr,
                                    &type_ids,
                                    json_schema,
                                )? {
                                    ctx_entity_refs[key] = entity_ref;
                                }
                            }
                        },
                        attr => {
                            return Err(BuildContextError::InvalidKind(
                                attr.kind_str().to_string(),
                                "record".to_string(),
                            ))
                        },
                    }
                }
            },
            attr => {
                return Err(BuildContextError::InvalidKind(
                    attr.kind_str().to_string(),
                    "record or common".to_string(),
                ))
            },
        }
    }

    let context = merge_json_values(ctx_entity_refs, request_context)?;
    let context: cedar_policy::Context =
        cedar_policy::Context::from_json_value(context, Some((schema, action)))?;

    Ok(context)
}

/// Builds the JSON entity references from a given attribute.
///
/// Returns `Ok(None)` if the attr is not an Entity Reference
fn build_entity_refs_from_attr(
    namespace: &str,
    attr: &Attribute,
    type_ids: &HashMap<String, String>,
    schema: &CedarSchemaJson,
) -> Result<Option<Value>, BuildContextError> {
    // TODO: handle errors here
    match attr {
        Attribute::Entity { name, .. } => map_entity_id(namespace, name, type_ids),
        Attribute::EntityOrCommon { name, .. } => {
            if let Some((entity_namespace, _)) = schema.get_entity_from_base_name(name) {
                if namespace == entity_namespace {
                    return map_entity_id(namespace, name, type_ids);
                }
            }
            Ok(None)
        },
        _ => Ok(None),
    }
}

/// Maps a known entity ID to the entity reference
fn map_entity_id(
    namespace: &str,
    name: &str,
    type_ids: &HashMap<String, String>,
) -> Result<Option<Value>, BuildContextError> {
    if let Some(type_id) = type_ids.get(name).as_ref() {
        let name = try_join_namespace(namespace, name);
        Ok(Some(json!({"type": name, "id": type_id})))
    } else {
        Err(BuildContextError::MissingEntityId(name.to_string()))
    }
}

/// Joins the given type name with the given namespace if it's not an empty string.
fn try_join_namespace(namespace: &str, type_name: &str) -> String {
    if namespace.is_empty() {
        return type_name.to_string();
    }
    [namespace, type_name].join(CEDAR_NAMESPACE_SEPARATOR)
}

#[derive(Debug, thiserror::Error)]
pub enum BuildContextError {
    /// Error encountered while validating context according to the schema
    #[error("failed to merge JSON objects due to conflicting keys: {0}")]
    KeyConflict(String),
    /// Error encountered while deserializing the Context from JSON
    #[error(transparent)]
    DeserializeFromJson(#[from] ContextJsonError),
    /// Error encountered if the action being used as the reference to build the Context
    /// is not in the schema
    #[error("failed to find the action `{0}` in the schema")]
    UnknownAction(String),
    /// Error encountered while building entity references in the Context
    #[error("failed to build entity reference for `{0}` since an entity id was not provided")]
    MissingEntityId(String),
    #[error("invalid action context type: {0}. expected: {1}")]
    InvalidKind(String, String),
}

pub fn merge_json_values(mut base: Value, other: Value) -> Result<Value, BuildContextError> {
    if let (Some(base_map), Some(additional_map)) = (base.as_object_mut(), other.as_object()) {
        for (key, value) in additional_map {
            if let Entry::Vacant(entry) = base_map.entry(key) {
                entry.insert(value.clone());
            } else {
                return Err(BuildContextError::KeyConflict(key.clone()));
            }
        }
    }
    Ok(base)
}

#[cfg(test)]
mod test {
    use super::*;
    use serde_json::json;

    #[test]
    fn can_merge_json_objects() {
        let obj1 = json!({ "a": 1, "b": 2 });
        let obj2 = json!({ "c": 3, "d": 4 });
        let expected = json!({"a": 1, "b": 2, "c": 3, "d": 4});

        let result = merge_json_values(obj1, obj2).expect("Should merge JSON objects");

        assert_eq!(result, expected);
    }

    #[test]
    fn errors_on_same_keys() {
        // Test for only two objects
        let obj1 = json!({ "a": 1, "b": 2 });
        let obj2 = json!({ "b": 3, "c": 4 });
        let result = merge_json_values(obj1, obj2);

        assert!(
            matches!(result, Err(BuildContextError::KeyConflict(key)) if key.as_str() == "b"),
            "Expected an error due to conflicting keys"
        );
    }
}
