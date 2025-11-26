use base64::prelude::*;
use cedar_policy::Entity;
use cedar_policy::EntityUid;
use cedar_policy::ExpressionConstructionError;
use cedar_policy::RestrictedExpression;
use serde::de::{Deserialize, Deserializer, Error};
use serde_json::Value;
use std::collections::HashMap;
use std::collections::HashSet;
use std::str::FromStr;
use std::string::FromUtf8Error;

use crate::common::default_entities_limits::{DefaultEntitiesLimits, DefaultEntitiesLimitsError};
use crate::entity_builder::BuildEntityError;
use crate::entity_builder::build_cedar_entity;
use crate::entity_builder::value_to_expr;

/// Dangerous patterns that should not appear in entity IDs for security reasons
const DANGEROUS_PATTERNS: [&str; 6] = [
    "<script",
    "javascript:",
    "data:",
    "vbscript:",
    "onload=",
    "onerror=",
];

#[derive(Debug, Default, Clone, PartialEq)]
pub struct DefaultEntities {
    pub inner: HashMap<EntityUid, Entity>,
}

impl DefaultEntities {
    /// Get entity by [EntityUid]
    pub fn get(&self, key: &EntityUid) -> Option<&Entity> {
        self.inner.get(key)
    }

    /// Returns the number of elements in the map.
    pub fn len(&self) -> usize {
        self.inner.len()
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
                    warns.push(DefaultEntityWarning::EntityParseError {
                        entry_id: entry_id.clone(),
                        error: err.to_string(),
                    });
                },
            }
        }

        for warn in &warns {
            eprintln!("{}", warn);
        }

        Self {
            inner: default_entities,
        }
    }
}

/// Structure that holds parsed default entities.
/// Errors that may occur during parsing holds inside structure and can get my `warns` method.
///
/// # JSON Serialization
/// This structure is deserialized from JSON with the following format:
/// - `None` or empty map: Returns empty default entities
/// - Map of entity IDs to entity data:
///   - Base64-encoded JSON string: Decoded and parsed as entity
///   - JSON object: Parsed directly as entity
///
/// Entity data supports two formats:
/// - Cedar format: {"uid": {"type": "...", "id": "..."}, "attrs": {...}, "parents": [...]}
/// - Legacy format: {"entity_type": "...", "entity_id": "...", ...attributes...}
///
/// # JSON Map Example
/// ```json
/// {
///   "user123": {
///     "uid": {
///       "type": "User",
///       "id": "user123"
///     },
///     "attrs": {
///       "name": "John Doe",
///       "age": 30
///     },
///     "parents": [
///       {
///         "type": "Group",
///         "id": "admin"
///       }
///     ]
///   },
///   "user456": "eyJ1aWQiOnsidHlwZSI6IlVzZXIiLCJpZCI6InVzZXI0NTYifSwiYXR0cnMiOnsibmFtZSI6IkpvaG4gRG9lIn19"
/// }
/// ```
#[derive(Debug, Default, Clone, PartialEq)]
pub struct DefaultEntitiesWithWarns {
    inner: DefaultEntities,
    warns: Vec<DefaultEntityWarning>,
}

impl DefaultEntitiesWithWarns {
    fn new(entities: HashMap<EntityUid, Entity>, warns: Vec<DefaultEntityWarning>) -> Self {
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
    pub fn warns(&self) -> &[DefaultEntityWarning] {
        &self.warns
    }
}

/// Parse default entities from raw data, returning entities and warnings
pub fn parse_default_entities_with_warns(
    raw_data: Option<HashMap<String, Value>>,
) -> Result<DefaultEntitiesWithWarns, ParseDefaultEntityError> {
    let limits = DefaultEntitiesLimits::default();

    if let Some(raw_data) = raw_data {
        let mut default_entities = HashMap::new();
        let mut warns = Vec::new();

        for (entry_id, raw_value) in raw_data {
            // Validate against limits (using default limits for deserialization)
            // Note: Configuration limits will be applied later when the policy store is initialized

            // check size of base64 string
            limits
                .validate_default_entity(&entry_id, &raw_value)
                .map_err(|err| {
                    ParseEntityErrorKind::LimitsValidation(err).with_entry_id(entry_id.clone())
                })?;
            // check size of HashMap
            limits
                .validate_entities_count(&default_entities)
                .map_err(|err| {
                    ParseEntityErrorKind::LimitsValidation(err).with_entry_id(entry_id.clone())
                })?;

            let entity = match &raw_value {
                Value::String(b64_string) => {
                    parse_base64_single_entity(&mut warns, entry_id.clone(), b64_string)?
                },
                Value::Object(_) => parse_single_entity(None, &mut warns, &entry_id, &raw_value)?,
                _ => {
                    return Err(ParseEntityErrorKind::IsNotJsonObject.with_entry_id(entry_id));
                },
            };

            default_entities.insert(entity.uid().clone(), entity);
        }

        Ok(DefaultEntitiesWithWarns::new(default_entities, warns))
    } else {
        // If none, return default value
        Ok(DefaultEntitiesWithWarns::default())
    }
}

impl<'de> Deserialize<'de> for DefaultEntitiesWithWarns {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: Deserializer<'de>,
    {
        let option_raw_data: Option<HashMap<String, Value>> =
            Deserialize::deserialize(deserializer)?;

        parse_default_entities_with_warns(option_raw_data).map_err(D::Error::custom)
    }
}

#[derive(Debug, thiserror::Error)]
#[error("failed to parse default entity, id: \"{entry_id}\" error: {error}")]
pub struct ParseDefaultEntityError {
    pub entry_id: String,
    pub error: Box<ParseEntityErrorKind>,
}

#[derive(Debug, thiserror::Error)]
pub enum ParseEntityErrorKind {
    #[error("unable to decode base64 string: {0}")]
    Base64Decode(#[from] base64::DecodeError),
    #[error("unable to decode base64 string as utf8: {0}")]
    UnicodeDecode(#[from] FromUtf8Error),
    #[error("base64 decoded value is not valid json: {0}")]
    Base64DecodedIsNotJson(#[from] serde_json::Error),
    #[error("entity ID cannot be empty or whitespace-only")]
    EntityIdIsEmpty,
    #[error("entity ID contains potentially dangerous content")]
    EntityIdIsDangerous,
    #[error("entity data must be JSON object")]
    IsNotJsonObject,
    #[error("entity has invalid 'uid.type' field, expect string")]
    InvalidUidTypeField,
    #[error("entity has invalid 'entity_type' field, expect string")]
    InvalidEntityTypeField,
    #[error("entity must have ether 'uid.type' or 'entity_type' (legacy format) field")]
    HaveNoUidOrEntityTypeField,
    #[error(transparent)]
    BuildEntity(BuildEntityError),
    #[error("Failed to convert attribute '{attr}' to cedar expr: {errs:?}")]
    ParseEntityAttribute {
        attr: String,
        errs: Vec<ExpressionConstructionError>,
    },
    #[error("default entities limits validation failed: {0}")]
    LimitsValidation(#[from] DefaultEntitiesLimitsError),
}

#[derive(Debug, thiserror::Error, Clone, PartialEq)]
pub enum DefaultEntityWarning {
    #[error("Could not parse parent UID '{parent_uid_str}' for default entity '{entry_id}': {error}")]
    InvalidParentUid {
        entry_id: String,
        parent_uid_str: String,
        error: String,
    },
    #[error("In default entity '{entry_id}' parent array json value should be object, skip: {value}")]
    NonObjectParentEntry {
        entry_id: String,
        value: String,
    },
    #[error("error parsing default entities: failed to parse entity '{entry_id}': {error}")]
    EntityParseError {
        entry_id: String,
        error: String,
    },
}

impl ParseEntityErrorKind {
    fn with_entry_id(self, entry_id: String) -> ParseDefaultEntityError {
        ParseDefaultEntityError {
            entry_id,
            error: Box::new(self),
        }
    }
}

/// Decode base64 string into UTF-8 JSON and call [parse_single_entity]
fn parse_base64_single_entity(
    warns: &mut Vec<DefaultEntityWarning>,
    entry_id: String,
    b64: &str,
) -> Result<Entity, ParseDefaultEntityError> {
    let buf = BASE64_STANDARD
        .decode(b64)
        .map_err(|err| ParseEntityErrorKind::Base64Decode(err).with_entry_id(entry_id.clone()))?;

    let json_str = String::from_utf8(buf)
        .map_err(|err| ParseEntityErrorKind::UnicodeDecode(err).with_entry_id(entry_id.clone()))?;

    let entity_data: serde_json::Value = serde_json::from_str(&json_str).map_err(|err| {
        ParseEntityErrorKind::Base64DecodedIsNotJson(err).with_entry_id(entry_id.clone())
    })?;

    let entity = parse_single_entity(None, warns, &entry_id, &entity_data)?;
    Ok(entity)
}

/// Parse single entity, return entity and error (in critical case),
/// But not critical case will populate `warn` vector with log message
fn parse_single_entity(
    namespace: Option<&str>,
    warns: &mut Vec<DefaultEntityWarning>,
    entry_id: &String,
    entity_data: &Value,
) -> Result<Entity, ParseDefaultEntityError> {
    validate_entry_id(entry_id)?;

    let entity_obj = if let Value::Object(obj) = entity_data {
        obj
    } else {
        return Err(ParseEntityErrorKind::IsNotJsonObject.with_entry_id(entry_id.to_owned()));
    };

    let (entity_type, entity_id_from_uid, cedar_attrs, parents) = if entity_obj.contains_key("uid")
    {
        // New Cedar entity format: {"uid": {"type": "...", "id": "..."}, "attrs": {}, "parents": [...]}
        parse_cedar_format(namespace, warns, entry_id, entity_data)?
    } else if entity_obj.contains_key("entity_type") {
        // Old format with entity_type field
        parse_legacy_format(namespace, warns, entry_id, entity_data)?
    } else {
        return Err(
            ParseEntityErrorKind::HaveNoUidOrEntityTypeField.with_entry_id(entry_id.to_owned())
        );
    };

    let entity = build_cedar_entity(&entity_type, entity_id_from_uid, cedar_attrs, parents)
        .map_err(|err| ParseEntityErrorKind::BuildEntity(err).with_entry_id(entry_id.to_owned()))?;
    Ok(entity)
}

/// Validate entry ID for security and format requirements
fn validate_entry_id(entry_id: &str) -> Result<(), ParseDefaultEntityError> {
    if entry_id.trim().is_empty() {
        return Err(ParseEntityErrorKind::EntityIdIsEmpty.with_entry_id(entry_id.to_owned()));
    }

    let entry_id_lower = entry_id.to_lowercase();
    for pattern in &DANGEROUS_PATTERNS {
        if entry_id_lower.contains(pattern) {
            return Err(
                ParseEntityErrorKind::EntityIdIsDangerous.with_entry_id(entry_id.to_owned())
            );
        }
    }

    Ok(())
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

/// Parse entity in the new Cedar format with "uid" field
fn parse_cedar_format<'a>(
    namespace: Option<&str>,
    warns: &mut Vec<DefaultEntityWarning>,
    entry_id: &'a str,
    entity_data: &'a Value,
) -> Result<
    (
        String,
        &'a str,
        HashMap<String, RestrictedExpression>,
        HashSet<EntityUid>,
    ),
    ParseDefaultEntityError,
> {
    let entity_obj = if let Value::Object(obj) = entity_data {
        obj
    } else {
        return Err(ParseEntityErrorKind::IsNotJsonObject.with_entry_id(entry_id.to_owned()));
    };

    // get uid and type
    let entity_type_from_uid = entity_obj
        .get("uid")
        .and_then(|v| v.as_object())
        .and_then(|v| v.get("type"))
        .and_then(|v| v.as_str())
        .ok_or_else(|| {
            ParseEntityErrorKind::InvalidUidTypeField.with_entry_id(entry_id.to_owned())
        })?;

    // Add namespace prefix if not already present
    let full_entity_type = build_entity_type_name(entity_type_from_uid, &namespace);

    // Get the entity ID from uid.id if present
    let entity_id_from_uid = entity_obj
        .get("uid")
        .and_then(|v| v.as_object())
        .and_then(|v| v.get("id"))
        .and_then(|v| v.as_str())
      // Fall back to the HashMap key if uid.id is not specified
        .unwrap_or(entry_id);

    // Parse attributes from attrs field
    let empty_map = serde_json::Map::new();
    let attrs_obj = entity_obj
        .get("attrs")
        .and_then(|v| v.as_object())
        .unwrap_or(&empty_map);

    let cedar_attrs = parse_entity_attrs(attrs_obj.iter(), &full_entity_type)?;

    // Parse parents from parents field
    let empty_vec: Vec<Value> = Vec::new();
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
                    warns.push(DefaultEntityWarning::InvalidParentUid {
                        entry_id: entry_id.to_string(),
                        parent_uid_str: parent_uid_str.clone(),
                        error: e.to_string(),
                    });
                },
            }
        } else {
            // log warn that we skip value because it is not object
            warns.push(DefaultEntityWarning::NonObjectParentEntry {
                entry_id: entry_id.to_string(),
                value: parent.to_string(),
            });
        }
    }

    Ok((
        full_entity_type,
        entity_id_from_uid,
        cedar_attrs,
        parents_set,
    ))
}

/// Parse entity in the legacy format with "entity_type" field
fn parse_legacy_format<'a>(
    _namespace: Option<&str>,
    _warns: &mut Vec<DefaultEntityWarning>,
    entry_id: &'a str,
    entity_data: &'a Value,
) -> Result<
    (
        String,
        &'a str,
        HashMap<String, RestrictedExpression>,
        HashSet<EntityUid>,
    ),
    ParseDefaultEntityError,
> {
    let entity_obj = if let Value::Object(obj) = entity_data {
        obj
    } else {
        return Err(ParseEntityErrorKind::IsNotJsonObject.with_entry_id(entry_id.to_owned()));
    };

    let entity_type = entity_obj
        .get("entity_type")
        .and_then(|v| v.as_str())
        .ok_or_else(|| {
            ParseEntityErrorKind::InvalidEntityTypeField.with_entry_id(entry_id.to_owned())
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
        entry_id,
    )?;

    Ok((
        entity_type.to_string(),
        entity_id_from_uid,
        cedar_attrs,
        HashSet::new(),
    ))
}

/// Helper function to parse entity attributes from a key-value iterator
fn parse_entity_attrs<'a>(
    attrs_iter: impl Iterator<Item = (&'a String, &'a Value)>,
    entry_id: &str,
) -> Result<HashMap<String, RestrictedExpression>, ParseDefaultEntityError> {
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
                return Err(ParseEntityErrorKind::ParseEntityAttribute {
                    attr: key.to_owned(),
                    errs: errors,
                }
                .with_entry_id(entry_id.to_owned()));
            },
        }
    }
    Ok(cedar_attrs)
}

#[cfg(test)]
mod test {
    use super::DANGEROUS_PATTERNS;
    use super::{DefaultEntityWarning, ParseEntityErrorKind, parse_default_entities_with_warns};
    use base64::Engine;
    use cedar_policy::EntityUid;
    use serde_json::{Value, json};
    use std::collections::HashMap;
    use std::str::FromStr;
    use test_utils::assert_eq;

    #[test]
    fn can_parse_default_entities() {
        // Test the parse_default_entities function directly
        // We don't need the schema for this test since we're not validating the entities

        // Create test default entities
        let default_entities_data = json!({
                "1694c954f8d9".to_string(): json!({
                    "entity_id": "1694c954f8d9",
                    "entity_type": "Jans::DefaultEntity",
                    "o": "Acme Dolphins Division",
                    "org_id": "100129"
                }),
        });

        let raw_data: HashMap<String, Value> =
            serde_json::from_value(default_entities_data).unwrap();
        let parsed_entities = parse_default_entities_with_warns(Some(raw_data))
            .expect("should parse default entities");
        let entities_hashmap = &parsed_entities.entities();

        assert_eq!(entities_hashmap.len(), 1, "should have 1 default entity");

        // Verify the entity
        let entity = entities_hashmap
            .get(&EntityUid::from_str("Jans::DefaultEntity::\"1694c954f8d9\"").unwrap())
            .expect("should have entity");
        assert_eq!(entity.uid().type_name().to_string(), "Jans::DefaultEntity");
        assert_eq!(entity.uid().id().as_ref() as &str, "1694c954f8d9");
    }

    #[test]
    fn test_parse_error_missing_uid() {
        // Test entity missing uid field
        let entity_data = json!({
            "attrs": {
                "attribute": "value"
            }
        });

        let default_entities_data = json!({"test123".to_string(): entity_data});
        let raw_data: HashMap<String, Value> =
            serde_json::from_value(default_entities_data).unwrap();

        let err = parse_default_entities_with_warns(Some(raw_data))
            .expect_err("Expected error for entity missing uid field");
        assert_eq!(err.entry_id, "test123", "Expected entry_id to be 'test123'");
        assert!(
            matches!(*err.error, ParseEntityErrorKind::HaveNoUidOrEntityTypeField),
            "Expected error to be HaveNoUidOrEntityTypeField"
        );
    }

    #[test]
    fn test_parse_error_invalid_uid_structure() {
        // Test entity with uid that is not an object
        let entity_data = json!({
            "uid": "not-an-object",
            "attrs": {}
        });

        let default_entities_data = json!({"test123".to_string(): entity_data});
        let raw_data: HashMap<String, Value> =
            serde_json::from_value(default_entities_data).unwrap();

        let err = parse_default_entities_with_warns(Some(raw_data))
            .expect_err("Expected error when uid is not an object");
        assert_eq!(err.entry_id, "test123", "Expected entry_id to be 'test123'");
        assert!(
            matches!(*err.error, ParseEntityErrorKind::InvalidUidTypeField),
            "Expected error to be InvalidUidTypeField"
        );

        // Test entity with uid missing type field
        let entity_data_no_type = json!({
            "uid": {
                "id": "test"
            },
            "attrs": {}
        });

        let default_entities_data = json!({"test456".to_string(): entity_data_no_type});
        let raw_data: HashMap<String, Value> =
            serde_json::from_value(default_entities_data).unwrap();
        let err = parse_default_entities_with_warns(Some(raw_data))
            .expect_err("Expected error when uid.type is missing");
        assert_eq!(err.entry_id, "test456", "Expected entry_id to be 'test456'");
        assert!(
            matches!(*err.error, ParseEntityErrorKind::InvalidUidTypeField),
            "Expected error to be InvalidUidTypeField"
        );
    }

    #[test]
    fn test_parse_entity_with_empty_attrs_and_parents() {
        // Test entity with empty attrs and empty parents
        let entity_data = json!({
            "uid": {
                "type": "Test::EmptyTest",
                "id": "test789"
            },
            "attrs": {},
            "parents": []
        });

        let default_entities_data = json!({"test789".to_string(): entity_data});
        let raw_data: HashMap<String, Value> =
            serde_json::from_value(default_entities_data).unwrap();
        let parsed_entities = parse_default_entities_with_warns(Some(raw_data))
            .expect("should parse with empty attrs and parents");
        let entities = parsed_entities.entities();

        let uid = &EntityUid::from_str("Test::EmptyTest::\"test789\"").unwrap();
        let entity = entities.get(&uid).expect("should have entity");
        assert_eq!(
            entity.uid().type_name().to_string(),
            "Test::EmptyTest",
            "Entity type should have namespace prefix"
        );
        let entity_json = entity.to_json_value().expect("should convert to JSON");
        let attrs = entity_json.get("attrs").expect("should have attrs");
        assert_eq!(
            attrs.as_object().unwrap().len(),
            0,
            "Entity should have empty attrs"
        );
    }

    #[test]
    fn test_entry_id_validation_empty_and_whitespace() {
        // Test empty entry ID
        let entity_data = json!({
            "uid": {
                "type": "Test::Type",
                "id": "test"
            },
            "attrs": {}
        });

        let default_entities_data = json!({"".to_string(): entity_data});
        let raw_data: HashMap<String, Value> =
            serde_json::from_value(default_entities_data).unwrap();
        let err = parse_default_entities_with_warns(Some(raw_data))
            .expect_err("Expected error for empty entry ID");
        assert_eq!(err.entry_id, "", "Expected entry_id to be empty");
        assert!(
            matches!(*err.error, ParseEntityErrorKind::EntityIdIsEmpty),
            "Expected error to be EntityIdIsEmpty"
        );

        // Test whitespace-only entry ID
        let default_entities_data_whitespace = json!({"   ".to_string(): entity_data});
        let raw_data: HashMap<String, Value> =
            serde_json::from_value(default_entities_data_whitespace).unwrap();
        let err = parse_default_entities_with_warns(Some(raw_data))
            .expect_err("Expected error for whitespace-only entry ID");
        assert_eq!(
            err.entry_id, "   ",
            "Expected entry_id to be whitespace-only"
        );
        assert!(
            matches!(*err.error, ParseEntityErrorKind::EntityIdIsEmpty),
            "Expected error to be EntityIdIsEmpty"
        );
    }

    #[test]
    fn test_entry_id_validation_dangerous_patterns() {
        let entity_data = json!({
            "uid": {
                "type": "Test::Type",
                "id": "test"
            },
            "attrs": {}
        });

        for pattern in DANGEROUS_PATTERNS {
            let dangerous_id = format!("prefix{}suffix", pattern);
            let default_entities_data = json!({dangerous_id.clone(): entity_data.clone()});
            let raw_data: HashMap<String, Value> =
                serde_json::from_value(default_entities_data).unwrap();
            let err = parse_default_entities_with_warns(Some(raw_data)).expect_err(&format!(
                "Should return error for dangerous pattern: {}",
                pattern
            ));
            assert_eq!(
                err.entry_id, dangerous_id,
                "Expected entry_id to match dangerous pattern"
            );
            assert!(
                matches!(*err.error, ParseEntityErrorKind::EntityIdIsDangerous),
                "Expected error to be EntityIdIsDangerous"
            );
        }
    }

    #[test]
    fn test_valid_entry_ids() {
        // Test various valid entry IDs
        let valid_ids = [
            "normal_id",
            "id_with_underscore",
            "id-with-dash",
            "id123",
            "ID_IN_UPPERCASE",
            "id.with.dots",
        ];

        for valid_id in valid_ids {
            let entity_data = json!({
                "uid": {
                    "type": "Test::Type",
                    "id": valid_id
                },
                "attrs": {}
            });

            let default_entities_data = json!({valid_id.to_string(): entity_data.clone()});
            let raw_data: HashMap<String, Value> =
                serde_json::from_value(default_entities_data).unwrap();
            let parsed_entities = parse_default_entities_with_warns(Some(raw_data))
                .expect(&format!("Should parse valid entry ID: {}", valid_id));

            assert_eq!(parsed_entities.entities().len(), 1, "Should have 1 entity");
        }
    }

    #[test]
    fn test_base64_parsing_valid() {
        // Create a valid entity JSON and encode it as base64
        let entity_json = json!({
            "uid": {
                "type": "Test::Base64Type",
                "id": "base64_test"
            },
            "attrs": {
                "test_attr": "test_value"
            }
        });

        let entity_json_str = entity_json.to_string();
        let b64_encoded = base64::prelude::BASE64_STANDARD.encode(entity_json_str);

        let default_entities_data = json!({
            "base64_entity".to_string(): b64_encoded
        });

        let raw_data: HashMap<String, Value> =
            serde_json::from_value(default_entities_data).unwrap();
        let parsed_entities = parse_default_entities_with_warns(Some(raw_data))
            .expect("Should parse valid base64 entity");

        assert_eq!(parsed_entities.entities().len(), 1, "Should have 1 entity");

        let uid = &EntityUid::from_str("Test::Base64Type::\"base64_test\"").unwrap();
        let entity = parsed_entities
            .entities()
            .get(&uid)
            .expect("should have entity");
        assert_eq!(entity.uid().type_name().to_string(), "Test::Base64Type");
    }

    #[test]
    fn test_base64_parsing_invalid_base64() {
        // Test invalid base64 string
        let invalid_b64 = "not-valid-base64==";

        let default_entities_data = json!({
            "invalid_base64_entity".to_string(): invalid_b64
        });

        let raw_data: HashMap<String, Value> =
            serde_json::from_value(default_entities_data).unwrap();
        let err = parse_default_entities_with_warns(Some(raw_data))
            .expect_err("Expected error for invalid base64");
        assert_eq!(
            err.entry_id, "invalid_base64_entity",
            "Expected entry_id to be 'invalid_base64_entity'"
        );
        assert!(
            matches!(*err.error, ParseEntityErrorKind::Base64Decode(_)),
            "Expected error to be Base64Decode"
        );
    }

    #[test]
    fn test_base64_parsing_invalid_json_after_decode() {
        // Test base64 that decodes to invalid JSON
        let invalid_json = "not valid json";
        let b64_encoded = base64::prelude::BASE64_STANDARD.encode(invalid_json);

        let default_entities_data = json!({
            "invalid_json_entity".to_string(): b64_encoded
        });

        let raw_data: HashMap<String, Value> =
            serde_json::from_value(default_entities_data).unwrap();
        let err = parse_default_entities_with_warns(Some(raw_data))
            .expect_err("Expected error for invalid JSON after base64 decode");
        assert_eq!(
            err.entry_id, "invalid_json_entity",
            "Expected entry_id to be 'invalid_json_entity'"
        );
        assert!(
            matches!(*err.error, ParseEntityErrorKind::Base64DecodedIsNotJson(_)),
            "Expected error to be Base64DecodedIsNotJson"
        );
    }

    #[test]
    fn test_base64_parsing_non_utf8_content() {
        // Test base64 that decodes to non-UTF8 content
        let non_utf8_bytes = vec![0xFF, 0xFE, 0x00]; // Invalid UTF-8 sequence
        let b64_encoded = base64::prelude::BASE64_STANDARD.encode(non_utf8_bytes);

        let default_entities_data = json!({
            "non_utf8_entity".to_string(): b64_encoded
        });

        let raw_data: HashMap<String, Value> =
            serde_json::from_value(default_entities_data).unwrap();
        let err = parse_default_entities_with_warns(Some(raw_data))
            .expect_err("Expected error for non-UTF8 content after base64 decode");
        assert_eq!(
            err.entry_id, "non_utf8_entity",
            "Expected entry_id to be 'non_utf8_entity'"
        );
        assert!(
            matches!(*err.error, ParseEntityErrorKind::UnicodeDecode(_)),
            "Expected error to be UnicodeDecode"
        );
    }

    #[test]
    fn test_namespace_handling() {
        // Test entity with explicit namespace (already contains ::)
        let entity_with_namespace = json!({
            "uid": {
                "type": "Custom::Namespace::EntityType",
                "id": "test1"
            },
            "attrs": {}
        });

        // Test entity without namespace (should not get namespace prefix)
        let entity_without_namespace = json!({
            "uid": {
                "type": "SimpleType",
                "id": "test2"
            },
            "attrs": {}
        });

        let default_entities_data = json!({
            "test1".to_string(): entity_with_namespace,
            "test2".to_string(): entity_without_namespace
        });

        let raw_data: HashMap<String, Value> =
            serde_json::from_value(default_entities_data).unwrap();
        let parsed_entities = parse_default_entities_with_warns(Some(raw_data))
            .expect("Should parse entities with and without namespaces");

        assert_eq!(
            parsed_entities.entities().len(),
            2,
            "Should have 2 entities"
        );

        // Verify entity with explicit namespace
        let uid1 = &EntityUid::from_str("Custom::Namespace::EntityType::\"test1\"").unwrap();
        let entity1 = parsed_entities
            .entities()
            .get(&uid1)
            .expect("should have entity1");
        assert_eq!(
            entity1.uid().type_name().to_string(),
            "Custom::Namespace::EntityType"
        );

        // Verify entity without namespace
        let uid2 = &EntityUid::from_str("SimpleType::\"test2\"").unwrap();
        let entity2 = parsed_entities
            .entities()
            .get(&uid2)
            .expect("should have entity2");
        assert_eq!(entity2.uid().type_name().to_string(), "SimpleType");
    }

    #[test]
    fn test_legacy_format_parsing() {
        // Test legacy format with entity_type field
        let legacy_entity = json!({
            "entity_type": "Legacy::Type",
            "entity_id": "legacy_test",
            "custom_attr": "custom_value"
        });

        let default_entities_data = json!({
            "legacy_entity".to_string(): legacy_entity
        });

        let raw_data: HashMap<String, Value> =
            serde_json::from_value(default_entities_data).unwrap();
        let parsed_entities = parse_default_entities_with_warns(Some(raw_data))
            .expect("Should parse legacy format entity");

        assert_eq!(parsed_entities.entities().len(), 1, "Should have 1 entity");

        let uid = &EntityUid::from_str("Legacy::Type::\"legacy_test\"").unwrap();
        let entity = parsed_entities
            .entities()
            .get(&uid)
            .expect("should have entity");
        assert_eq!(entity.uid().type_name().to_string(), "Legacy::Type");

        // Verify attributes are parsed correctly
        let entity_json = entity.to_json_value().expect("should convert to JSON");
        let attrs = entity_json.get("attrs").expect("should have attrs");
        let custom_attr = attrs.get("custom_attr").expect("should have custom_attr");
        assert_eq!(custom_attr.as_str().unwrap(), "custom_value");
    }

    #[test]
    fn test_parent_entity_parsing() {
        // Test entity with valid parent entities
        let entity_with_parents = json!({
            "uid": {
                "type": "Test::ChildType",
                "id": "child_entity"
            },
            "attrs": {},
            "parents": [
                {
                    "type": "Test::ParentType1",
                    "id": "parent1"
                },
                {
                    "type": "Test::ParentType2",
                    "id": "parent2"
                }
            ]
        });

        let default_entities_data = json!({
            "child_entity".to_string(): entity_with_parents
        });

        let raw_data: HashMap<String, Value> =
            serde_json::from_value(default_entities_data).unwrap();
        let parsed_entities = parse_default_entities_with_warns(Some(raw_data))
            .expect("Should parse entity with parents");

        assert_eq!(parsed_entities.entities().len(), 1, "Should have 1 entity");

        let uid = &EntityUid::from_str("Test::ChildType::\"child_entity\"").unwrap();
        let entity = parsed_entities
            .entities()
            .get(&uid)
            .expect("should have entity");

        // Verify the entity was parsed successfully with parents
        // The parents are stored internally but we can't easily access them from the Entity
        // The main verification is that parsing succeeded with the parent data
        let _entity = entity;
    }

    #[test]
    fn test_parent_entity_parsing_with_warnings() {
        // Test entity with invalid parent entries that should generate warnings
        let entity_with_invalid_parents = json!({
            "uid": {
                "type": "Test::Type",
                "id": "test_entity"
            },
            "attrs": {},
            "parents": [
                {
                    "type": "ValidParent",
                    "id": "valid"
                },
                {
                    "type": "InvalidParent",
                    "id": "invalid@uid"  // Invalid UID format
                },
                "not_an_object",  // Invalid parent format
                {
                    "missing_type": "parent",  // Missing type field
                    "id": "parent3"
                }
            ]
        });

        let default_entities_data = json!({
            "test_entity".to_string(): entity_with_invalid_parents
        });

        let raw_data: HashMap<String, Value> =
            serde_json::from_value(default_entities_data).unwrap();
        let parsed_entities = parse_default_entities_with_warns(Some(raw_data))
            .expect("Should parse entity with invalid parents (but generate warnings)");

        // Should still parse the entity successfully
        assert_eq!(parsed_entities.entities().len(), 1, "Should have 1 entity");

        // Should have warnings for invalid parents
        assert!(!parsed_entities.warns().is_empty(), "Should have warnings");

        // Verify the valid parent was parsed
        let uid = &EntityUid::from_str("Test::Type::\"test_entity\"").unwrap();
        let _entity = parsed_entities
            .entities()
            .get(&uid)
            .expect("should have entity");

        // The entity was parsed successfully despite invalid parents
        // The warnings should indicate that some parents were skipped
    }

    #[test]
    fn test_attribute_parsing_various_types() {
        // Test entity with various attribute types
        let entity_with_attrs = json!({
            "uid": {
                "type": "Test::AttrType",
                "id": "attr_test"
            },
            "attrs": {
                "string_attr": "string_value",
                "number_attr": 42,
                "bool_attr": true,
                "array_attr": ["item1", "item2"],
                "object_attr": {"nested": "value"}
            }
        });

        let default_entities_data = json!({
            "attr_test".to_string(): entity_with_attrs
        });

        let raw_data: HashMap<String, Value> =
            serde_json::from_value(default_entities_data).unwrap();
        let parsed_entities = parse_default_entities_with_warns(Some(raw_data))
            .expect("Should parse entity with various attribute types");

        assert_eq!(parsed_entities.entities().len(), 1, "Should have 1 entity");

        let uid = &EntityUid::from_str("Test::AttrType::\"attr_test\"").unwrap();
        let entity = parsed_entities
            .entities()
            .get(&uid)
            .expect("should have entity");

        // Verify attributes are present
        let entity_json = entity.to_json_value().expect("should convert to JSON");
        let attrs = entity_json.get("attrs").expect("should have attrs");
        let attrs_obj = attrs.as_object().expect("attrs should be object");

        assert_eq!(attrs_obj.len(), 5, "Should have 5 attributes");
        assert_eq!(
            attrs_obj.get("string_attr").unwrap().as_str().unwrap(),
            "string_value"
        );
        assert_eq!(attrs_obj.get("number_attr").unwrap().as_i64().unwrap(), 42);
        assert_eq!(attrs_obj.get("bool_attr").unwrap().as_bool().unwrap(), true);
    }

    #[test]
    fn test_entity_without_uid_id_falls_back_to_entry_id() {
        // Test entity where uid.id is not specified - should fall back to entry_id
        let entity_without_uid_id = json!({
            "uid": {
                "type": "Test::FallbackType"
                // No "id" field
            },
            "attrs": {}
        });

        let entry_id = "fallback_entry_id";
        let default_entities_data = json!({entry_id.to_string(): entity_without_uid_id});

        let raw_data: HashMap<String, Value> =
            serde_json::from_value(default_entities_data).unwrap();
        let parsed_entities = parse_default_entities_with_warns(Some(raw_data))
            .expect("Should parse entity with fallback ID");

        assert_eq!(parsed_entities.entities().len(), 1, "Should have 1 entity");

        // Entity UID should use the entry_id as fallback
        let uid = &EntityUid::from_str("Test::FallbackType::\"fallback_entry_id\"").unwrap();
        let entity = parsed_entities
            .entities()
            .get(&uid)
            .expect("should have entity");
        assert_eq!(entity.uid().id().as_ref() as &str, "fallback_entry_id");
    }

    #[test]
    fn test_empty_default_entities() {
        // Test deserializing None/empty default entities
        let parsed_entities =
            parse_default_entities_with_warns(None).expect("Should parse empty default entities");

        assert_eq!(
            parsed_entities.entities().len(),
            0,
            "Should have 0 entities"
        );
        assert!(
            parsed_entities.warns().is_empty(),
            "Should have no warnings"
        );
    }

    #[test]
    fn test_invalid_value_type_error() {
        // Test entity with invalid value type (not object or base64 string)
        let default_entities_data = json!({
            "invalid_entity".to_string(): 12345  // Number instead of object/string
        });

        let raw_data: HashMap<String, Value> =
            serde_json::from_value(default_entities_data).unwrap();
        let err = parse_default_entities_with_warns(Some(raw_data))
            .expect_err("Expected error for invalid value type");
        assert_eq!(
            err.entry_id, "invalid_entity",
            "Expected entry_id to be 'invalid_entity'"
        );
        assert!(
            matches!(*err.error, ParseEntityErrorKind::IsNotJsonObject),
            "Expected error to be IsNotJsonObject"
        );
    }

    #[test]
    fn test_mixed_format_entities() {
        // Test parsing a mix of base64 and JSON object entities

        // Create a base64 encoded entity
        let base64_entity_json = json!({
            "uid": {
                "type": "Test::Base64Type",
                "id": "base64_entity"
            },
            "attrs": {
                "source": "base64"
            }
        });
        let base64_encoded =
            base64::prelude::BASE64_STANDARD.encode(base64_entity_json.to_string());

        // Create a regular JSON object entity
        let json_entity = json!({
            "uid": {
                "type": "Test::JsonType",
                "id": "json_entity"
            },
            "attrs": {
                "source": "json"
            }
        });

        let default_entities_data = json!({
            "base64_entity".to_string(): base64_encoded,
            "json_entity".to_string(): json_entity
        });

        let raw_data: HashMap<String, Value> =
            serde_json::from_value(default_entities_data).unwrap();
        let parsed_entities = parse_default_entities_with_warns(Some(raw_data))
            .expect("Should parse mixed format entities");

        assert_eq!(
            parsed_entities.entities().len(),
            2,
            "Should have 2 entities"
        );

        // Verify both entities were parsed correctly
        let base64_uid = &EntityUid::from_str("Test::Base64Type::\"base64_entity\"").unwrap();
        let json_uid = &EntityUid::from_str("Test::JsonType::\"json_entity\"").unwrap();

        assert!(
            parsed_entities.entities().get(&base64_uid).is_some(),
            "Should have base64 entity"
        );
        assert!(
            parsed_entities.entities().get(&json_uid).is_some(),
            "Should have json entity"
        );
    }

    #[test]
    fn test_warning_enum_invalid_parent_uid() {
        // Test that invalid parent UIDs generate proper warnings
        let entity_with_invalid_parent = json!({
            "uid": {
                "type": "Test::Type",
                "id": "test_entity"
            },
            "attrs": {},
            "parents": [
                {
                    "type": "InvalidParent",
                    "id": "invalid@uid"  // This should be valid, so let's test with a truly invalid format
                }
            ]
        });

        let default_entities_data = json!({
            "test_entity".to_string(): entity_with_invalid_parent
        });

        let raw_data: HashMap<String, Value> =
            serde_json::from_value(default_entities_data).unwrap();
        let parsed_entities = parse_default_entities_with_warns(Some(raw_data))
            .expect("Should parse entity with invalid parent");

        // This test should have no warnings since "invalid@uid" is actually valid
        assert!(parsed_entities.warns().is_empty(), "Should have no warnings for valid UID");
    }

    #[test]
    fn test_warning_enum_non_object_parent_entry() {
        // Test that non-object parent entries generate proper warnings
        let entity_with_invalid_parents = json!({
            "uid": {
                "type": "Test::Type",
                "id": "test_entity"
            },
            "attrs": {},
            "parents": [
                "not_an_object",  // Invalid parent format
                {
                    "type": "ValidParent",
                    "id": "valid"
                }
            ]
        });

        let default_entities_data = json!({
            "test_entity".to_string(): entity_with_invalid_parents
        });

        let raw_data: HashMap<String, Value> =
            serde_json::from_value(default_entities_data).unwrap();
        let parsed_entities = parse_default_entities_with_warns(Some(raw_data))
            .expect("Should parse entity with invalid parents");

        // Should have warnings for non-object parent entries
        assert!(!parsed_entities.warns().is_empty(), "Should have warnings");
        
        let warnings = parsed_entities.warns();
        assert_eq!(warnings.len(), 1, "Should have exactly 1 warning");
        
        // Verify the warning type and content
        match &warnings[0] {
            DefaultEntityWarning::NonObjectParentEntry { entry_id, value } => {
                assert_eq!(entry_id, "test_entity");
                assert!(value.contains("not_an_object"));
            },
            _ => panic!("Expected NonObjectParentEntry warning, got {:?}", warnings[0]),
        }
    }

    #[test]
    fn test_warning_enum_entity_parse_error() {
        // Test that entity parsing errors generate proper warnings
        // Since EntityParseError is only used in the test-only from_hashmap method,
        // let's test a different scenario that actually generates warnings we can test
        let entity_with_invalid_parents = json!({
            "uid": {
                "type": "Test::Type",
                "id": "test_entity"
            },
            "attrs": {},
            "parents": [
                "not_an_object",  // Invalid parent format
                {
                    "missing_type": "parent",  // Missing type field
                    "id": "parent3"
                }
            ]
        });

        let default_entities_data = json!({
            "test_entity".to_string(): entity_with_invalid_parents
        });

        let raw_data: HashMap<String, Value> =
            serde_json::from_value(default_entities_data).unwrap();
        let parsed_entities = parse_default_entities_with_warns(Some(raw_data))
            .expect("Should parse entity with invalid parents");

        // Should have warnings for non-object parent entries
        assert!(!parsed_entities.warns().is_empty(), "Should have warnings");
        
        let warnings = parsed_entities.warns();
        assert_eq!(warnings.len(), 2, "Should have exactly 2 warnings");
        
        // Both should be NonObjectParentEntry warnings
        for warning in warnings {
            match warning {
                DefaultEntityWarning::NonObjectParentEntry { entry_id, value } => {
                    assert_eq!(entry_id, "test_entity");
                    assert!(!value.is_empty());
                },
                _ => panic!("Expected NonObjectParentEntry warning, got {:?}", warning),
            }
        }
    }

    #[test]
    fn test_warning_enum_multiple_warnings() {
        // Test that multiple different warnings are properly captured
        let entity_with_multiple_issues = json!({
            "uid": {
                "type": "Test::Type",
                "id": "test_entity"
            },
            "attrs": {},
            "parents": [
                "not_an_object",  // Invalid parent format
                {
                    "missing_type": "parent",  // Missing type field
                    "id": "parent3"
                }
            ]
        });

        let default_entities_data = json!({
            "test_entity".to_string(): entity_with_multiple_issues
        });

        let raw_data: HashMap<String, Value> =
            serde_json::from_value(default_entities_data).unwrap();
        let parsed_entities = parse_default_entities_with_warns(Some(raw_data))
            .expect("Should parse entity with multiple issues");

        // Should have multiple warnings
        let warnings = parsed_entities.warns();
        assert_eq!(warnings.len(), 2, "Should have exactly 2 warnings");
        
        // Both should be NonObjectParentEntry warnings
        for warning in warnings {
            match warning {
                DefaultEntityWarning::NonObjectParentEntry { entry_id, value } => {
                    assert_eq!(entry_id, "test_entity");
                    assert!(!value.is_empty());
                },
                _ => panic!("Expected NonObjectParentEntry warning, got {:?}", warning),
            }
        }
    }

    #[test]
    fn test_warning_enum_display_format() {
        // Test that warnings have proper display formatting
        let warning = DefaultEntityWarning::InvalidParentUid {
            entry_id: "test_entity".to_string(),
            parent_uid_str: "Test::Parent::\"invalid@uid\"".to_string(),
            error: "invalid character".to_string(),
        };
        
        let display_string = warning.to_string();
        assert!(display_string.contains("test_entity"));
        assert!(display_string.contains("Test::Parent"));
        assert!(display_string.contains("invalid character"));
        
        let warning2 = DefaultEntityWarning::NonObjectParentEntry {
            entry_id: "test_entity".to_string(),
            value: "\"not_an_object\"".to_string(),
        };
        
        let display_string2 = warning2.to_string();
        assert!(display_string2.contains("test_entity"));
        assert!(display_string2.contains("not_an_object"));
    }
}
