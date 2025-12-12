// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Cedar entity parsing and validation.
//!
//! This module provides functionality to parse and validate Cedar entity files in JSON format,
//! ensuring they conform to Cedar's entity specification with proper UIDs, attributes, and
//! parent relationships.

use super::errors::{CedarEntityErrorType, PolicyStoreError};
use cedar_policy::{Entities, Entity, EntityId, EntityTypeName, EntityUid, Schema};
use serde::{Deserialize, Serialize};
use serde_json::Value as JsonValue;
use std::collections::{HashMap, HashSet};
use std::str::FromStr;

/// A parsed Cedar entity with metadata.
///
/// Contains the Cedar entity and metadata about the source file.
#[derive(Debug, Clone)]
pub struct ParsedEntity {
    /// The Cedar entity
    pub entity: Entity,
    /// The entity's UID
    pub uid: EntityUid,
    /// Source filename
    pub filename: String,
    /// Raw entity content (JSON)
    pub content: String,
}

/// Raw entity JSON structure as expected by Cedar.
///
/// This matches Cedar's JSON entity format with uid, attrs, and parents fields.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RawEntityJson {
    /// Entity unique identifier
    pub uid: EntityUidJson,
    /// Entity attributes as a map of attribute names to values (optional)
    #[serde(default)]
    pub attrs: HashMap<String, JsonValue>,
    /// Parent entity UIDs for hierarchy (optional)
    #[serde(default)]
    pub parents: Vec<EntityUidJson>,
}

/// Entity UID in JSON format.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq, Hash)]
pub struct EntityUidJson {
    /// Entity type (e.g., "Jans::User")
    #[serde(rename = "type")]
    pub entity_type: String,
    /// Entity ID
    pub id: String,
}

/// Wrapper for multiple entities that can be in array or object format.
///
/// This supports both formats commonly used in Cedar entity files:
/// - Array: `[{entity1}, {entity2}]`
/// - Object: `{"id1": {entity1}, "id2": {entity2}}`
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
enum RawEntitiesWrapper {
    /// Array of entity objects
    Array(Vec<JsonValue>),
    /// Object mapping IDs to entity objects
    Object(HashMap<String, JsonValue>),
}

/// Entity parser for loading and validating Cedar entities.
pub struct EntityParser;

impl EntityParser {
    /// Parse a single Cedar entity from JSON value.
    ///
    /// Validates the entity structure, parses the UID, attributes, and parent relationships.
    /// Optionally validates entity attributes against a schema.
    ///
    /// # Errors
    /// Returns `PolicyStoreError` if:
    /// - JSON parsing fails
    /// - Entity structure is invalid
    /// - UID format is invalid
    /// - Parent UID format is invalid
    /// - Schema validation fails (if schema provided)
    pub fn parse_entity(
        entity_json: &JsonValue,
        filename: &str,
        schema: Option<&Schema>,
    ) -> Result<ParsedEntity, PolicyStoreError> {
        // Parse the JSON structure
        let raw_entity: RawEntityJson =
            serde_json::from_value(entity_json.clone()).map_err(|e| {
                PolicyStoreError::JsonParsing {
                    file: filename.to_string(),
                    source: e,
                }
            })?;

        // Parse the entity UID
        let uid = Self::parse_entity_uid(&raw_entity.uid, filename)?;

        // Validate parent UIDs format (don't collect, just validate)
        for parent_uid in &raw_entity.parents {
            Self::parse_entity_uid(parent_uid, filename)?;
        }

        // Use Cedar's Entity::from_json_value to parse the entity with attributes
        // This properly handles attribute conversion to RestrictedExpression
        let entity_json_for_cedar = serde_json::json!({
            "uid": {
                "type": raw_entity.uid.entity_type,
                "id": raw_entity.uid.id
            },
            "attrs": raw_entity.attrs,
            "parents": raw_entity.parents
        });

        // Parse with optional schema validation using Entity::from_json_value
        let entity = Entity::from_json_value(entity_json_for_cedar, schema).map_err(|e| {
            PolicyStoreError::CedarEntityError {
                file: filename.to_string(),
                err: CedarEntityErrorType::JsonParseError(format!(
                    "Failed to parse entity{}: {}",
                    if schema.is_some() {
                        " (schema validation failed)"
                    } else {
                        ""
                    },
                    e
                )),
            }
        })?;

        Ok(ParsedEntity {
            entity,
            uid,
            filename: filename.to_string(),
            content: serde_json::to_string(entity_json).unwrap_or_default(),
        })
    }

    /// Parse multiple entities from a JSON array or object.
    ///
    /// Supports both array format: `[{entity1}, {entity2}]`
    /// And object format: `{"entity_id1": {entity1}, "entity_id2": {entity2}}`
    pub fn parse_entities(
        content: &str,
        filename: &str,
        schema: Option<&Schema>,
    ) -> Result<Vec<ParsedEntity>, PolicyStoreError> {
        let json_value: JsonValue =
            serde_json::from_str(content).map_err(|e| PolicyStoreError::JsonParsing {
                file: filename.to_string(),
                source: e,
            })?;

        // Use untagged enum to handle both array and object formats
        let wrapper: RawEntitiesWrapper =
            serde_json::from_value(json_value).map_err(|e| PolicyStoreError::CedarEntityError {
                file: filename.to_string(),
                err: CedarEntityErrorType::JsonParseError(format!(
                    "Entity file must contain a JSON array or object: {}",
                    e
                )),
            })?;

        let entity_values: Vec<&JsonValue> = match &wrapper {
            RawEntitiesWrapper::Array(arr) => arr.iter().collect(),
            RawEntitiesWrapper::Object(obj) => obj.values().collect(),
        };

        let mut parsed_entities = Vec::with_capacity(entity_values.len());
        for entity_json in entity_values {
            let parsed = Self::parse_entity(entity_json, filename, schema)?;
            parsed_entities.push(parsed);
        }

        Ok(parsed_entities)
    }

    /// Parse an EntityUid from JSON format.
    fn parse_entity_uid(
        uid_json: &EntityUidJson,
        filename: &str,
    ) -> Result<EntityUid, PolicyStoreError> {
        // Parse the entity type name
        let entity_type = EntityTypeName::from_str(&uid_json.entity_type).map_err(|e| {
            PolicyStoreError::CedarEntityError {
                file: filename.to_string(),
                err: CedarEntityErrorType::InvalidTypeName(
                    uid_json.entity_type.clone(),
                    e.to_string(),
                ),
            }
        })?;

        // Parse the entity ID
        let entity_id =
            EntityId::from_str(&uid_json.id).map_err(|e| PolicyStoreError::CedarEntityError {
                file: filename.to_string(),
                err: CedarEntityErrorType::InvalidEntityId(format!(
                    "Invalid entity ID '{}': {}",
                    uid_json.id, e
                )),
            })?;

        Ok(EntityUid::from_type_name_and_id(entity_type, entity_id))
    }

    /// Detect and resolve duplicate entity UIDs.
    ///
    /// Returns a map of entity UIDs to their parsed entities.
    /// If duplicates are found, returns an error with details about the conflicts.
    pub fn detect_duplicates(
        entities: Vec<ParsedEntity>,
    ) -> Result<HashMap<EntityUid, ParsedEntity>, Vec<String>> {
        let mut entity_map: HashMap<EntityUid, ParsedEntity> =
            HashMap::with_capacity(entities.len());
        let mut duplicates: Vec<String> = Vec::new();

        for entity in entities {
            if let Some(existing) = entity_map.get(&entity.uid) {
                duplicates.push(format!(
                    "Duplicate entity UID '{}' found in files '{}' and '{}'",
                    entity.uid, existing.filename, entity.filename
                ));
                // Don't insert the duplicate - keep the first occurrence
            } else {
                entity_map.insert(entity.uid.clone(), entity);
            }
        }

        if duplicates.is_empty() {
            Ok(entity_map)
        } else {
            Err(duplicates)
        }
    }

    /// Create a Cedar Entities store from parsed entities.
    ///
    /// Validates that all entities are compatible and can be used together.
    pub fn create_entities_store(
        entities: Vec<ParsedEntity>,
    ) -> Result<Entities, PolicyStoreError> {
        let entity_list: Vec<Entity> = entities.into_iter().map(|p| p.entity).collect();

        Entities::from_entities(entity_list, None).map_err(|e| PolicyStoreError::CedarEntityError {
            file: "entity_store".to_string(),
            err: CedarEntityErrorType::EntityStoreCreation(e.to_string()),
        })
    }

    /// Validate entity hierarchy.
    ///
    /// Ensures that all parent references point to entities that exist in the collection.
    pub fn validate_hierarchy(entities: &[ParsedEntity]) -> Result<(), Vec<String>> {
        let entity_uids: HashSet<&EntityUid> = entities.iter().map(|e| &e.uid).collect();
        let mut errors: Vec<String> = Vec::new();

        for parsed_entity in entities {
            // Get parents directly from the entity using into_inner()
            // into_inner() returns (uid, attrs, parents)
            let parents = &parsed_entity.entity.clone().into_inner().2;

            for parent_uid in parents {
                if !entity_uids.contains(parent_uid) {
                    errors.push(format!(
                        "Entity '{}' in file '{}' references non-existent parent '{}'",
                        parsed_entity.uid, parsed_entity.filename, parent_uid
                    ));
                }
            }
        }

        if errors.is_empty() {
            Ok(())
        } else {
            Err(errors)
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_simple_entity() {
        let content = serde_json::json!({
            "uid": {
                "type": "User",
                "id": "alice"
            },
            "attrs": {
                "name": "Alice",
                "age": 30
            },
            "parents": []
        });

        let result = EntityParser::parse_entity(&content, "user1.json", None);
        assert!(
            result.is_ok(),
            "Should parse simple entity: {:?}",
            result.err()
        );

        let parsed = result.unwrap();
        assert_eq!(parsed.filename, "user1.json");
        assert_eq!(parsed.uid.to_string(), "User::\"alice\"");
    }

    #[test]
    fn test_parse_entity_with_parents() {
        let content = serde_json::json!({
            "uid": {
                "type": "User",
                "id": "bob"
            },
            "attrs": {
                "name": "Bob"
            },
            "parents": [
                {
                    "type": "Role",
                    "id": "admin"
                },
                {
                    "type": "Role",
                    "id": "developer"
                }
            ]
        });

        let result = EntityParser::parse_entity(&content, "user2.json", None);
        assert!(result.is_ok(), "Should parse entity with parents");

        let parsed = result.unwrap();
        // Verify parents using into_inner()
        let parents = &parsed.entity.clone().into_inner().2;
        assert_eq!(parents.len(), 2, "Should have 2 parents");
    }

    #[test]
    fn test_parse_entity_with_namespace() {
        let content = serde_json::json!({
            "uid": {
                "type": "Jans::User",
                "id": "user123"
            },
            "attrs": {
                "email": "user@example.com"
            },
            "parents": []
        });

        let result = EntityParser::parse_entity(&content, "jans_user.json", None);
        assert!(result.is_ok(), "Should parse entity with namespace");

        let parsed = result.unwrap();
        assert_eq!(parsed.uid.to_string(), "Jans::User::\"user123\"");
    }

    #[test]
    fn test_parse_entity_empty_attrs() {
        let content = serde_json::json!({
            "uid": {
                "type": "Resource",
                "id": "res1"
            },
            "attrs": {},
            "parents": []
        });

        let result = EntityParser::parse_entity(&content, "resource.json", None);
        assert!(result.is_ok(), "Should parse entity with empty attrs");
    }

    #[test]
    fn test_parse_entity_invalid_json() {
        let content = serde_json::json!("not an object");

        let result = EntityParser::parse_entity(&content, "invalid.json", None);
        assert!(result.is_err(), "Should fail on invalid JSON");

        if let Err(PolicyStoreError::JsonParsing { file, .. }) = result {
            assert_eq!(file, "invalid.json");
        } else {
            panic!("Expected JsonParsing error");
        }
    }

    #[test]
    fn test_parse_entity_invalid_type() {
        let content = serde_json::json!({
            "uid": {
                "type": "Invalid Type Name!",
                "id": "test"
            },
            "attrs": {},
            "parents": []
        });

        let result = EntityParser::parse_entity(&content, "invalid_type.json", None);
        assert!(result.is_err(), "Should fail on invalid entity type");
    }

    #[test]
    fn test_parse_entities_array() {
        let content = r#"[
            {
                "uid": {"type": "User", "id": "user1"},
                "attrs": {"name": "User One"},
                "parents": []
            },
            {
                "uid": {"type": "User", "id": "user2"},
                "attrs": {"name": "User Two"},
                "parents": []
            }
        ]"#;

        let result = EntityParser::parse_entities(content, "users.json", None);
        assert!(result.is_ok(), "Should parse entity array");

        let parsed = result.unwrap();
        assert_eq!(parsed.len(), 2, "Should have 2 entities");
    }

    #[test]
    fn test_parse_entities_object() {
        let content = r#"{
            "user1": {
                "uid": {"type": "User", "id": "user1"},
                "attrs": {},
                "parents": []
            },
            "user2": {
                "uid": {"type": "User", "id": "user2"},
                "attrs": {},
                "parents": []
            }
        }"#;

        let result = EntityParser::parse_entities(content, "users.json", None);
        assert!(result.is_ok(), "Should parse entity object");

        let parsed = result.unwrap();
        assert_eq!(parsed.len(), 2, "Should have 2 entities");
    }

    #[test]
    fn test_detect_duplicates_none() {
        let entities = vec![
            ParsedEntity {
                entity: Entity::new(
                    "User::\"alice\"".parse().unwrap(),
                    HashMap::new(),
                    HashSet::new(),
                )
                .unwrap(),
                uid: "User::\"alice\"".parse().unwrap(),
                filename: "user1.json".to_string(),
                content: String::new(),
            },
            ParsedEntity {
                entity: Entity::new(
                    "User::\"bob\"".parse().unwrap(),
                    HashMap::new(),
                    HashSet::new(),
                )
                .unwrap(),
                uid: "User::\"bob\"".parse().unwrap(),
                filename: "user2.json".to_string(),
                content: String::new(),
            },
        ];

        let result = EntityParser::detect_duplicates(entities);
        assert!(result.is_ok(), "Should have no duplicates");

        let map = result.unwrap();
        assert_eq!(map.len(), 2, "Should have 2 unique entities");
    }

    #[test]
    fn test_detect_duplicates_found() {
        let entities = vec![
            ParsedEntity {
                entity: Entity::new(
                    "User::\"alice\"".parse().unwrap(),
                    HashMap::new(),
                    HashSet::new(),
                )
                .unwrap(),
                uid: "User::\"alice\"".parse().unwrap(),
                filename: "user1.json".to_string(),
                content: String::new(),
            },
            ParsedEntity {
                entity: Entity::new(
                    "User::\"alice\"".parse().unwrap(),
                    HashMap::new(),
                    HashSet::new(),
                )
                .unwrap(),
                uid: "User::\"alice\"".parse().unwrap(),
                filename: "user2.json".to_string(),
                content: String::new(),
            },
        ];

        let result = EntityParser::detect_duplicates(entities);
        assert!(result.is_err(), "Should detect duplicates");

        let errors = result.unwrap_err();
        assert_eq!(errors.len(), 1, "Should have 1 duplicate error");
        assert!(errors[0].contains("User::\"alice\""));
        assert!(errors[0].contains("user1.json"));
        assert!(errors[0].contains("user2.json"));
    }

    #[test]
    fn test_validate_hierarchy_valid() {
        // Create parent entity
        let parent = ParsedEntity {
            entity: Entity::new(
                "Role::\"admin\"".parse().unwrap(),
                HashMap::new(),
                HashSet::new(),
            )
            .unwrap(),
            uid: "Role::\"admin\"".parse().unwrap(),
            filename: "role.json".to_string(),
            content: String::new(),
        };

        // Create child entity with parent reference
        let mut parent_set = HashSet::new();
        parent_set.insert("Role::\"admin\"".parse().unwrap());

        let child = ParsedEntity {
            entity: Entity::new(
                "User::\"alice\"".parse().unwrap(),
                HashMap::new(),
                parent_set,
            )
            .unwrap(),
            uid: "User::\"alice\"".parse().unwrap(),
            filename: "user.json".to_string(),
            content: String::new(),
        };

        let entities = vec![parent, child];
        let result = EntityParser::validate_hierarchy(&entities);
        assert!(result.is_ok(), "Hierarchy should be valid");
    }

    #[test]
    fn test_validate_hierarchy_missing_parent() {
        // Create child entity with non-existent parent reference
        let mut parent_set = HashSet::new();
        parent_set.insert("Role::\"admin\"".parse().unwrap());

        let child = ParsedEntity {
            entity: Entity::new(
                "User::\"alice\"".parse().unwrap(),
                HashMap::new(),
                parent_set,
            )
            .unwrap(),
            uid: "User::\"alice\"".parse().unwrap(),
            filename: "user.json".to_string(),
            content: String::new(),
        };

        let entities = vec![child];
        let result = EntityParser::validate_hierarchy(&entities);
        assert!(result.is_err(), "Should detect missing parent");

        let errors = result.unwrap_err();
        assert_eq!(errors.len(), 1, "Should have 1 hierarchy error");
        assert!(errors[0].contains("Role::\"admin\""));
    }

    #[test]
    fn test_create_entities_store() {
        let entities = vec![
            ParsedEntity {
                entity: Entity::new(
                    "User::\"alice\"".parse().unwrap(),
                    HashMap::new(),
                    HashSet::new(),
                )
                .unwrap(),
                uid: "User::\"alice\"".parse().unwrap(),
                filename: "user1.json".to_string(),
                content: String::new(),
            },
            ParsedEntity {
                entity: Entity::new(
                    "User::\"bob\"".parse().unwrap(),
                    HashMap::new(),
                    HashSet::new(),
                )
                .unwrap(),
                uid: "User::\"bob\"".parse().unwrap(),
                filename: "user2.json".to_string(),
                content: String::new(),
            },
        ];

        let result = EntityParser::create_entities_store(entities);
        assert!(result.is_ok(), "Should create entity store");

        let store = result.unwrap();
        assert_eq!(store.iter().count(), 2, "Store should have 2 entities");
    }

    #[test]
    fn test_parse_entity_with_schema_validation() {
        use cedar_policy::{Schema, SchemaFragment};
        use std::str::FromStr;

        // Create a schema that defines User entity type
        let schema_src = r#"
            entity User = {
                name: String,
                age: Long
            };
        "#;

        let fragment = SchemaFragment::from_str(schema_src).expect("Should parse schema");
        let schema = Schema::from_schema_fragments([fragment]).expect("Should create schema");

        // Valid entity matching schema
        let valid_content = serde_json::json!({
            "uid": {
                "type": "User",
                "id": "alice"
            },
            "attrs": {
                "name": "Alice",
                "age": 30
            },
            "parents": []
        });

        let result = EntityParser::parse_entity(&valid_content, "user.json", Some(&schema));
        assert!(
            result.is_ok(),
            "Should parse entity with valid schema: {:?}",
            result.err()
        );
    }
}
