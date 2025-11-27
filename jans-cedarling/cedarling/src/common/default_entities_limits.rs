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
pub struct DefaultEntitiesLimits {
    /// Maximum number of default entities allowed
    pub max_entities: usize,
    /// Maximum size of base64-encoded strings in bytes
    pub max_base64_size: usize,
}

impl Default for DefaultEntitiesLimits {
    fn default() -> Self {
        Self {
            max_entities: Self::DEFAULT_MAX_ENTITIES,
            max_base64_size: Self::DEFAULT_MAX_BASE64_SIZE,
        }
    }
}

impl DefaultEntitiesLimits {
    /// Default maximum number of entities allowed
    pub const DEFAULT_MAX_ENTITIES: usize = 1000;
    /// Default maximum size of base64-encoded strings in bytes
    pub const DEFAULT_MAX_BASE64_SIZE: usize = 1024 * 1024;

    pub fn validate_default_entity_data_size(
        &self,
        entity_id: &str,
        entity_str: &str,
    ) -> Result<(), DefaultEntitiesLimitsError> {
        if entity_str.len() > self.max_base64_size {
            Err(DefaultEntitiesLimitsError::DataSizeExceeded {
                entity_id: entity_id.to_string(),
                size: entity_str.len(),
                max_size: self.max_base64_size,
            })
        } else {
            Ok(())
        }
    }

    pub fn validate_default_entity(
        &self,
        entity_id: &str,
        entity_data: &serde_json::Value,
    ) -> Result<(), DefaultEntitiesLimitsError> {
        if let Some(entity_str) = entity_data.as_str() {
            self.validate_default_entity_data_size(entity_id, entity_str)
        } else {
            Ok(())
        }
    }

    pub fn validate_entities_len(
        &self,
        entities: &DefaultEntities,
    ) -> Result<(), DefaultEntitiesLimitsError> {
        self.validate_entities_count(entities.len())
    }

    pub fn validate_entities_count(
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

    pub fn validate_default_entities(
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
    use super::*;
    use serde_json::json;

    #[test]
    fn test_validate_default_entity_data_size() {
        let limits = DefaultEntitiesLimits {
            max_entities: 2,
            max_base64_size: 100,
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
            max_base64_size: 100,
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
            max_base64_size: 100,
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
}
