use cedar_policy::entities_errors::EntitiesError;

use crate::common::default_entities::DefaultEntities;

/// Error type for default entities limits validation
#[derive(Debug, thiserror::Error)]
pub enum DefaultEntitiesLimitsError {
    #[error(
        "Cedar entity data size ({size}) for default entity '{entity_id}' exceeds maximum allowed size ({max_size})"
    )]
    DataSizeExceeded {
        entity_id: String,
        size: usize,
        max_size: usize,
    },
    #[error("Maximum number of default entities ({max_entities}) exceeded, found {found}")]
    CountExceeded { max_entities: usize, found: usize },
    #[error(
        "Could not convert cedar entity '{entity_id}' to JSON value for size validation: {source}"
    )]
    ConversionError {
        entity_id: String,
        #[source]
        source: Box<EntitiesError>,
    },
}

/// Configuration for limiting default entities to prevent DoS and memory exhaustion attacks
#[derive(Debug, Clone)]
pub(super) struct DefaultEntitiesLimits {
    /// Maximum number of default entities allowed
    pub max_entities: usize,
    /// Maximum size of entity JSON representation in bytes
    pub max_entity_size: usize,
}

impl Default for DefaultEntitiesLimits {
    fn default() -> Self {
        Self {
            max_entities: Self::DEFAULT_MAX_ENTITIES,
            max_entity_size: Self::DEFAULT_MAX_ENTITY_SIZE,
        }
    }
}

impl DefaultEntitiesLimits {
    /// Default maximum number of entities allowed
    pub(super) const DEFAULT_MAX_ENTITIES: usize = 1000;
    /// Default maximum size of entity JSON representation in bytes
    pub(super) const DEFAULT_MAX_ENTITY_SIZE: usize = 1024 * 1024;

    fn validate_default_entity_data_size(
        &self,
        entity_id: &str,
        entity_str: &str,
    ) -> Result<(), DefaultEntitiesLimitsError> {
        if entity_str.len() > self.max_entity_size {
            Err(DefaultEntitiesLimitsError::DataSizeExceeded {
                entity_id: entity_id.to_string(),
                size: entity_str.len(),
                max_size: self.max_entity_size,
            })
        } else {
            Ok(())
        }
    }

    pub(super) fn validate_default_entity(
        &self,
        entity_id: &str,
        entity_data: &serde_json::Value,
    ) -> Result<(), DefaultEntitiesLimitsError> {
        // If string check string size (should be base64 encoded data)
        if let Some(entity_str) = entity_data.as_str() {
            self.validate_default_entity_data_size(entity_id, entity_str)
        } else {
            Ok(())
        }
    }

    fn validate_entities_len(
        &self,
        entities: &DefaultEntities,
    ) -> Result<(), DefaultEntitiesLimitsError> {
        self.validate_entities_count(entities.len())
    }

    pub(super) fn validate_entities_count(
        &self,
        entity_count: usize,
    ) -> Result<(), DefaultEntitiesLimitsError> {
        // Check entity count limit
        if entity_count > self.max_entities {
            Err(DefaultEntitiesLimitsError::CountExceeded {
                max_entities: self.max_entities,
                found: entity_count,
            })
        } else {
            Ok(())
        }
    }

    pub(super) fn validate_default_entities(
        &self,
        entities: &DefaultEntities,
    ) -> Result<(), DefaultEntitiesLimitsError> {
        self.validate_entities_len(entities)?;

        for (euid, e) in &entities.inner {
            let json_entity_data = e.to_json_string().map_err(|source| {
                DefaultEntitiesLimitsError::ConversionError {
                    entity_id: euid.to_string(),
                    source: Box::new(source),
                }
            })?;
            self.validate_default_entity_data_size(euid.to_string().as_str(), &json_entity_data)?;
        }

        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use cedar_policy::{Entity, EntityId, EntityTypeName, EntityUid};
    use std::collections::HashMap;
    use std::collections::HashSet;
    use std::str::FromStr;

    use super::*;
    use serde_json::json;

    #[test]
    fn test_validate_default_entity_data_size() {
        let limits = DefaultEntitiesLimits {
            max_entities: 2,
            max_entity_size: 100,
        };

        // Test valid base64 size
        let result = limits.validate_default_entity_data_size("entity1", "dGVzdA==");
        assert!(result.is_ok());

        // Test base64 size limit
        let large_base64 = "dGVzdA==".repeat(20); // Much larger than 100 bytes
        let result = limits.validate_default_entity_data_size("entity1", &large_base64);
        assert!(result.is_err());
        assert!(
            result
                .unwrap_err()
                .to_string()
                .contains("exceeds maximum allowed size")
        );
    }

    #[test]
    fn test_validate_default_entity() {
        let limits = DefaultEntitiesLimits {
            max_entities: 2,
            max_entity_size: 100,
        };

        // Test valid entity with string value
        let valid_entity = json!("dGVzdA==");
        let result = limits.validate_default_entity("entity1", &valid_entity);
        assert!(result.is_ok());

        // Test entity with non-string value (should be ok)
        let non_string_entity = json!({ "key": "value" });
        let result = limits.validate_default_entity("entity1", &non_string_entity);
        assert!(result.is_ok());

        // Test entity with large base64 string
        let large_base64 = "dGVzdA==".repeat(20); // Much larger than 100 bytes
        let large_entity = json!(large_base64);
        let result = limits.validate_default_entity("entity1", &large_entity);
        assert!(
            result
                .unwrap_err()
                .to_string()
                .contains("exceeds maximum allowed size")
        );
    }

    #[test]
    fn test_validate_entities_count() {
        let limits = DefaultEntitiesLimits {
            max_entities: 2,
            max_entity_size: 100,
        };

        // Test valid entity count
        let result = limits.validate_entities_count(2);
        assert!(result.is_ok());

        // Test entity count limit
        let result = limits.validate_entities_count(3);
        assert!(
            result
                .unwrap_err()
                .to_string()
                .contains("Maximum number of default entities (2) exceeded")
        );
    }

    /// Validate method DefaultEntitiesLimits::validate_default_entities
    #[test]
    fn test_validate_default_entities() {
        let limits = DefaultEntitiesLimits {
            max_entities: 2,
            max_entity_size: 100,
        };

        // Test with valid entities within limits
        let mut entities_map = HashMap::new();
        let type_name = EntityTypeName::from_str("User").unwrap();
        let entity_id = EntityId::from_str("alice").unwrap();
        let euid = EntityUid::from_type_name_and_id(type_name.clone(), entity_id);
        let entity = Entity::new_no_attrs(euid.clone(), HashSet::new());
        entities_map.insert(euid, entity);

        let valid_entities = DefaultEntities {
            inner: entities_map.clone(),
        };
        limits
            .validate_default_entities(&valid_entities)
            .expect("added only one entity, should not exceed limit");

        // Test entity count exceeded
        let type_name2 = EntityTypeName::from_str("User").unwrap();
        let entity_id2 = EntityId::from_str("bob").unwrap();
        let euid2 = EntityUid::from_type_name_and_id(type_name2.clone(), entity_id2);
        let entity2 = Entity::new_no_attrs(euid2.clone(), HashSet::new());
        entities_map.insert(euid2.clone(), entity2);

        let entity_id3 = EntityId::from_str("charlie").unwrap();
        let euid3 = EntityUid::from_type_name_and_id(type_name2, entity_id3);
        let entity3 = Entity::new_no_attrs(euid3.clone(), HashSet::new());
        entities_map.insert(euid3, entity3);

        let too_many_entities = DefaultEntities {
            inner: entities_map,
        };
        let err = limits
            .validate_default_entities(&too_many_entities)
            .expect_err("added 3 entities, should exceed limit");
        assert!(
            matches!(err, DefaultEntitiesLimitsError::CountExceeded { .. }),
            "should get error CountExceeded"
        );
    }
}
