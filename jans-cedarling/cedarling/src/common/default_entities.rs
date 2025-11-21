use cedar_policy::Entity;
use cedar_policy::EntityUid;
use cedar_policy::RestrictedExpression;
use serde::de::{Deserialize, Deserializer, Error};
use serde_json::Value;
use std::collections::HashMap;
use std::collections::HashSet;
use std::str::FromStr;

use crate::common::default_entities_limits::DefaultEntitiesLimits;
use crate::entity_builder::BuildEntityError;
use crate::entity_builder::BuildEntityErrorKind;
use crate::entity_builder::build_cedar_entity;
use crate::entity_builder::value_to_expr;

#[derive(Debug, Default, Clone, PartialEq)]
pub struct DefaultEntities {
    pub inner: HashMap<EntityUid, Entity>,
}

impl DefaultEntities {
    pub fn get(&self, key: &EntityUid) -> Option<&Entity> {
        self.inner.get(key)
    }

    // This method expect in JSON Value object!
    #[cfg(test)]
    pub fn from_hashmap(raw_data: &HashMap<String, Value>) -> Self {
        let mut default_entities = HashMap::new();
        let mut warns = Vec::new();

        for (entry_id, entity_data) in raw_data {
            match parse_single_entity(None, &mut warns, &entry_id, &entity_data) {
                Ok(entity) => {
                    default_entities.insert(entity.uid().clone(), entity);
                },
                Err(err) => {
                    warns.push(format!(
                        "error parsing default entities: failed to parse entity '{}': {}",
                        entry_id, err
                    ));
                },
            }
        }

        for warn in warns {
            eprintln!("{}", warn);
        }

        Self {
            inner: default_entities,
        }
    }
}

/// Structure that holds parsed default entities.
/// Errors that may occur during parsing holds inside structure and can get my `warns` method.
#[derive(Debug, Default, Clone, PartialEq)]
pub struct DefaultEntitiesWithWarns {
    inner: DefaultEntities,
    warns: Vec<String>,
}

impl DefaultEntitiesWithWarns {
    fn new(entities: HashMap<EntityUid, Entity>, warns: Vec<String>) -> Self {
        Self {
            inner: DefaultEntities { inner: entities },
            warns,
        }
    }

    /// Get default entities
    pub fn entities(&self) -> &DefaultEntities {
        &self.inner
    }

    // Get warns (error messages) that was on parsing phase
    pub fn warns(&self) -> &[String] {
        &self.warns
    }
}

impl<'de> Deserialize<'de> for DefaultEntitiesWithWarns {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: Deserializer<'de>,
    {
        let limits = DefaultEntitiesLimits::default();

        let option_raw_data: Option<HashMap<String, Value>> =
            Deserialize::deserialize(deserializer)?;

        if let Some(raw_data) = option_raw_data {
            let mut default_entities = HashMap::new();
            let mut warns = Vec::new();

            for (entry_id, raw_value) in raw_data {
                // Validate against limits (using default limits for deserialization)
                // Note: Configuration limits will be applied later when the policy store is initialized

                // check size of base64 string
                let _ = limits
                    .validate_default_entity(&entry_id, &raw_value)
                    .map_err(|e| D::Error::custom(e));
                // check size of HashMap
                let _ = limits
                    .validate_entities_count(&default_entities)
                    .map_err(|e| D::Error::custom(e));

                // first decode from base64
                let b64 = raw_value.as_str().ok_or_else(|| {
                            D::Error::custom(format!(
                                "error parsing default entities: entity '{}' must be a base64-encoded JSON string",
                                entry_id
                            ))
                        })?;

                // Decode base64 string into UTF-8 JSON
                use base64::prelude::*;
                let buf = BASE64_STANDARD.decode(b64).map_err(|err| {
                    D::Error::custom(format!(
                        "error parsing default entities: failed to decode base64 for '{}': {}",
                        entry_id, err
                    ))
                })?;

                let json_str = String::from_utf8(buf).map_err(|err| {
                    D::Error::custom(format!(
                        "error parsing default entities: failed to decode utf8 for '{}': {}",
                        entry_id, err
                    ))
                })?;

                let entity_data: serde_json::Value =
                    serde_json::from_str(&json_str).map_err(|err| {
                        D::Error::custom(format!(
                            "error parsing default entities: invalid JSON for '{}': {}",
                            entry_id, err
                        ))
                    })?;

                let entity = parse_single_entity(None, &mut warns, &entry_id, &entity_data)
                    .map_err(|err| D::Error::custom(err))?;
                default_entities.insert(entity.uid().clone(), entity);
            }

            Ok(DefaultEntitiesWithWarns::new(default_entities, warns))
        } else {
            // If none, return default value
            Ok(Self::default())
        }
    }
}

/// Constant for unknown entity type in error messages
const UNKNOWN_ENTITY_TYPE: &str = "Unknown";

fn parse_single_entity(
    namespace: Option<&str>,
    warns: &mut Vec<String>,
    entry_id: &String,
    entity_data: &Value,
) -> Result<Entity, BuildEntityError> {
    if entry_id.trim().is_empty() {
        return Err(BuildEntityError::new(
            "DefaultEntity".to_string(),
            BuildEntityErrorKind::InvalidEntityData(
                "Entity ID cannot be empty or whitespace-only".to_string(),
            ),
        ));
    }
    let dangerous_patterns = [
        "<script",
        "javascript:",
        "data:",
        "vbscript:",
        "onload=",
        "onerror=",
    ];
    let entry_id_lower = entry_id.to_lowercase();
    for pattern in &dangerous_patterns {
        if entry_id_lower.contains(pattern) {
            return Err(BuildEntityError::new(
                "DefaultEntity".to_string(),
                BuildEntityErrorKind::InvalidEntityData(format!(
                    "Entity ID '{}' contains potentially dangerous content",
                    entry_id
                )),
            ));
        }
    }
    let entity_obj = if let Value::Object(obj) = entity_data {
        obj
    } else {
        return Err(BuildEntityError::new(
            UNKNOWN_ENTITY_TYPE.to_string(),
            BuildEntityErrorKind::InvalidEntityData(format!(
                "Default entity data for '{}' must be a JSON object",
                entry_id
            )),
        ));
    };
    let (entity_type, entity_id_from_uid, cedar_attrs, parents) = if entity_obj.contains_key("uid")
    {
        // New Cedar entity format: {"uid": {"type": "...", "id": "..."}, "attrs": {}, "parents": [...]}
        let uid_obj = entity_obj
            .get("uid")
            .and_then(|v| v.as_object())
            .ok_or_else(|| {
                BuildEntityError::new(
                    UNKNOWN_ENTITY_TYPE.to_string(),
                    BuildEntityErrorKind::InvalidEntityData(format!(
                        "Default entity '{}' has invalid uid field",
                        entry_id
                    )),
                )
            })?;

        let entity_type_from_uid =
            uid_obj
                .get("type")
                .and_then(|v| v.as_str())
                .ok_or_else(|| {
                    BuildEntityError::new(
                        UNKNOWN_ENTITY_TYPE.to_string(),
                        BuildEntityErrorKind::InvalidEntityData(format!(
                            "Default entity '{}' has invalid uid.type field",
                            entry_id
                        )),
                    )
                })?;

        // Add namespace prefix if not already present
        let full_entity_type = build_entity_type_name(entity_type_from_uid, &namespace);

        // Get the entity ID from uid.id if present
        let entity_id_from_uid = uid_obj.get("id")
            .and_then(|v| v.as_str())
            // Fall back to the HashMap key if uid.id is not specified
            .unwrap_or(entry_id);

        // Parse attributes from attrs field
        let empty_map = serde_json::Map::new();
        let attrs_obj = entity_obj
            .get("attrs")
            .and_then(|v| v.as_object())
            .unwrap_or(&empty_map);

        let cedar_attrs =
            parse_entity_attrs(attrs_obj.iter(), &full_entity_type, entity_id_from_uid)?;

        // Parse parents from parents field
        let empty_vec: Vec<Value> = vec![];
        let parents_array = entity_obj
            .get("parents")
            .and_then(|v| v.as_array())
            .unwrap_or(&empty_vec);

        let mut parents_set = HashSet::new();
        for parent in parents_array {
            if let Value::Object(parent_obj) = parent
                && let (Some(type_v), Some(id_v)) = (
                    parent_obj.get("type").and_then(|v| v.as_str()),
                    parent_obj.get("id").and_then(|v| v.as_str()),
                )
            {
                // Add namespace if not present
                let full_parent_entity_type = build_entity_type_name(type_v, &namespace);
                let parent_uid_str = format!("{}::\"{}\"", full_parent_entity_type, id_v);
                match EntityUid::from_str(&parent_uid_str) {
                    Ok(parent_uid) => {
                        parents_set.insert(parent_uid);
                    },
                    Err(e) => {
                        // log warn that we could not parse uid
                        let msg = format!(
                            "Could not parse parent UID '{}' for default entity '{}': {}",
                            parent_uid_str, entry_id, e
                        );
                        warns.push(msg);
                    },
                }
            } else {
                // log warn that we skip value because it is not object
                let msg = format!(
                    "In default entity parent array json value should be object, skip: {}",
                    parent
                );
                warns.push(msg);
            }
        }

        (
            full_entity_type,
            entity_id_from_uid,
            cedar_attrs,
            parents_set,
        )
    } else if entity_obj.contains_key("entity_type") {
        // Old format with entity_type field
        let entity_type = entity_obj
            .get("entity_type")
            .and_then(|v| v.as_str())
            .ok_or_else(|| {
                BuildEntityError::new(
                    UNKNOWN_ENTITY_TYPE.to_string(),
                    BuildEntityErrorKind::InvalidEntityData(format!(
                        "Default entity '{}' has invalid entity_type field",
                        entry_id
                    )),
                )
            })?;

        let entity_id_from_uid = entity_obj
            .get("entity_id")
            .and_then(|v| v.as_str())
            .unwrap_or(entry_id);

        // Convert JSON attributes to Cedar expressions
        let cedar_attrs = parse_entity_attrs(
            entity_obj
                .iter()
                .filter(|(key, _)| key != &"entity_type" && key != &"entity_id"),
            entity_type,
            entry_id,
        )?;

        (
            entity_type.to_string(),
            entity_id_from_uid,
            cedar_attrs,
            HashSet::new(),
        )
    } else {
        return Err(BuildEntityError::new(
            UNKNOWN_ENTITY_TYPE.to_string(),
            BuildEntityErrorKind::InvalidEntityData(format!(
                "Default entity '{}' must have either uid field (Cedar format) or entity_type field (legacy format)",
                entry_id
            )),
        ));
    };
    let entity = build_cedar_entity(&entity_type, entity_id_from_uid, cedar_attrs, parents)?;
    Ok(entity)
}

fn build_entity_type_name(entity_type_from_uid: &str, namespace: &Option<&str>) -> String {
    if entity_type_from_uid.contains("::") {
        entity_type_from_uid.to_string()
    } else if let Some(ns) = namespace
        && !ns.is_empty()
    {
        format!("{}::{}", ns, entity_type_from_uid)
    } else {
        entity_type_from_uid.to_string()
    }
}

/// Helper function to parse entity attributes from a key-value iterator
fn parse_entity_attrs<'a>(
    attrs_iter: impl Iterator<Item = (&'a String, &'a Value)>,
    entity_type: &str,
    entity_id: &str,
) -> Result<HashMap<String, RestrictedExpression>, BuildEntityError> {
    let mut cedar_attrs = HashMap::new();
    for (key, value) in attrs_iter {
        match value_to_expr::value_to_expr(value) {
            Ok(Some(expr)) => {
                cedar_attrs.insert(key.clone(), expr);
            },
            Ok(None) => {
                continue;
            },
            Err(errors) => {
                return Err(BuildEntityError::new(
                    entity_type.to_string(),
                    BuildEntityErrorKind::InvalidEntityData(format!(
                        "Failed to convert attribute '{}' for entity '{}': {:?}",
                        key, entity_id, errors
                    )),
                ));
            },
        }
    }
    Ok(cedar_attrs)
}
