// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Policy Store Manager - Converts new format to legacy format.
//!
//! This module provides the conversion layer between `LoadedPolicyStore` (new directory/archive format)
//! and `PolicyStore` (legacy format used by the rest of Cedarling).
//!
//! # Architecture
//!
//! ```text
//! LoadedPolicyStore (new)          PolicyStore (legacy)
//! ├── metadata                  →  name, version, description, cedar_version
//! ├── schema (raw string)       →  schema: CedarSchema
//! ├── policies: Vec<PolicyFile> →  policies: PoliciesContainer
//! ├── trusted_issuers           →  trusted_issuers: HashMap<String, TrustedIssuer>
//! └── entities                  →  default_entities: HashMap<String, Value>
//! ```
//!
//! # Usage
//!
//! ```no_run
//! use cedarling::common::policy_store::{
//!     DefaultPolicyStoreLoader, PhysicalVfs, PolicyStoreManager,
//! };
//!
//! // Load using new loader
//! let vfs = PhysicalVfs::new();
//! let loader = DefaultPolicyStoreLoader::new(vfs);
//! let loaded = loader.load_directory("/path/to/store")?;
//!
//! // Convert to legacy format
//! let legacy_store = PolicyStoreManager::convert_to_legacy(loaded)?;
//! # Ok::<(), Box<dyn std::error::Error>>(())
//! ```

use super::entity_parser::EntityParser;
use super::issuer_parser::IssuerParser;
use super::loader::LoadedPolicyStore;
use super::policy_parser::PolicyParser;
use super::{PoliciesContainer, PolicyStore, TrustedIssuer};
use crate::common::cedar_schema::CedarSchema;
use crate::common::cedar_schema::cedar_json::CedarSchemaJson;
use cedar_policy::PolicySet;
use cedar_policy_core::extensions::Extensions;
use cedar_policy_validator::ValidatorSchema;
use semver::Version;
use std::collections::HashMap;

/// Errors that can occur during policy store conversion.
#[derive(Debug, thiserror::Error)]
pub enum ConversionError {
    /// Schema conversion failed
    #[error("Failed to convert schema: {0}")]
    SchemaConversion(String),

    /// Policy conversion failed
    #[error("Failed to convert policies: {0}")]
    PolicyConversion(String),

    /// Trusted issuer conversion failed
    #[error("Failed to convert trusted issuers: {0}")]
    IssuerConversion(String),

    /// Entity conversion failed
    #[error("Failed to convert entities: {0}")]
    EntityConversion(String),

    /// Version parsing failed
    #[error("Failed to parse cedar version '{version}': {details}")]
    VersionParsing { version: String, details: String },

    /// Policy set creation failed
    #[error("Failed to create policy set: {0}")]
    PolicySetCreation(String),
}

/// Policy Store Manager handles conversion between new and legacy formats.
pub struct PolicyStoreManager;

impl PolicyStoreManager {
    /// Convert a `LoadedPolicyStore` (new format) to `PolicyStore` (legacy format).
    ///
    /// This is the main entry point for converting policy stores loaded from
    /// directory or archive format into the legacy format used by the rest of Cedarling.
    ///
    /// # Arguments
    ///
    /// * `loaded` - The loaded policy store from the new loader
    ///
    /// # Returns
    ///
    /// Returns a `PolicyStore` that can be used with existing Cedarling services.
    ///
    /// # Errors
    ///
    /// Returns `ConversionError` if any component fails to convert.
    pub fn convert_to_legacy(loaded: LoadedPolicyStore) -> Result<PolicyStore, ConversionError> {
        // 1. Convert schema
        let cedar_schema = Self::convert_schema(&loaded.schema)?;

        // 2. Convert policies
        let policies_container = Self::convert_policies(&loaded.policies)?;

        // 3. Convert trusted issuers
        let trusted_issuers = Self::convert_trusted_issuers(&loaded.trusted_issuers)?;

        // 4. Convert entities
        let default_entities = Self::convert_entities(&loaded.entities)?;

        // 5. Parse cedar version
        let cedar_version = Self::parse_cedar_version(&loaded.metadata.cedar_version)?;

        Ok(PolicyStore {
            name: loaded.metadata.policy_store.name,
            version: Some(loaded.metadata.policy_store.version),
            description: loaded.metadata.policy_store.description,
            cedar_version: Some(cedar_version),
            schema: cedar_schema,
            policies: policies_container,
            trusted_issuers,
            default_entities,
        })
    }

    /// Convert raw schema string to `CedarSchema`.
    ///
    /// The `CedarSchema` requires:
    /// - `schema: cedar_policy::Schema`
    /// - `json: CedarSchemaJson`
    /// - `validator_schema: ValidatorSchema`
    fn convert_schema(schema_content: &str) -> Result<CedarSchema, ConversionError> {
        use cedar_policy::SchemaFragment;
        use std::str::FromStr;

        // Parse schema fragment from content (human-readable Cedar schema format)
        let fragment = SchemaFragment::from_str(schema_content).map_err(|e| {
            ConversionError::SchemaConversion(format!("Failed to parse schema: {}", e))
        })?;

        // Convert fragment to JSON string
        let json_string = fragment.to_json_string().map_err(|e| {
            ConversionError::SchemaConversion(format!("Failed to serialize schema to JSON: {}", e))
        })?;

        // Create schema from fragment
        let schema = cedar_policy::Schema::from_schema_fragments([fragment]).map_err(|e| {
            ConversionError::SchemaConversion(format!("Failed to create schema: {}", e))
        })?;

        // Parse CedarSchemaJson
        let json: CedarSchemaJson = serde_json::from_str(&json_string).map_err(|e| {
            ConversionError::SchemaConversion(format!("Failed to parse CedarSchemaJson: {}", e))
        })?;

        // Create ValidatorSchema
        let validator_schema = ValidatorSchema::from_json_str(
            &json_string,
            Extensions::all_available(),
        )
        .map_err(|e| {
            ConversionError::SchemaConversion(format!("Failed to create ValidatorSchema: {}", e))
        })?;

        Ok(CedarSchema {
            schema,
            json,
            validator_schema,
        })
    }

    /// Convert policy files to `PoliciesContainer`.
    ///
    /// The `PoliciesContainer` requires:
    /// - `policy_set: cedar_policy::PolicySet`
    /// - `raw_policy_info: HashMap<String, RawPolicy>` (for descriptions)
    fn convert_policies(
        policy_files: &[super::loader::PolicyFile],
    ) -> Result<PoliciesContainer, ConversionError> {
        if policy_files.is_empty() {
            // Return empty policy set
            let policy_set = PolicySet::new();
            return Ok(PoliciesContainer::new_empty(policy_set));
        }

        // Parse each policy file
        let mut parsed_policies = Vec::with_capacity(policy_files.len());
        for file in policy_files {
            let parsed = PolicyParser::parse_policy(&file.content, &file.name).map_err(|e| {
                ConversionError::PolicyConversion(format!("Failed to parse '{}': {}", file.name, e))
            })?;
            parsed_policies.push(parsed);
        }

        // Create policy set using PolicyParser
        let policy_set = PolicyParser::create_policy_set(parsed_policies.clone(), vec![])
            .map_err(|e| ConversionError::PolicySetCreation(e.to_string()))?;

        // Build raw_policy_info for descriptions
        let raw_policy_info = parsed_policies
            .into_iter()
            .map(|p| (p.id.to_string(), p.filename))
            .collect();

        Ok(PoliciesContainer::new(policy_set, raw_policy_info))
    }

    /// Convert issuer files to `HashMap<String, TrustedIssuer>`.
    fn convert_trusted_issuers(
        issuer_files: &[super::loader::IssuerFile],
    ) -> Result<Option<HashMap<String, TrustedIssuer>>, ConversionError> {
        if issuer_files.is_empty() {
            return Ok(None);
        }

        let mut all_issuers = Vec::new();
        for file in issuer_files {
            let parsed = IssuerParser::parse_issuer(&file.content, &file.name).map_err(|e| {
                ConversionError::IssuerConversion(format!("Failed to parse '{}': {}", file.name, e))
            })?;
            all_issuers.extend(parsed);
        }

        // Validate for duplicates
        if let Err(errors) = IssuerParser::validate_issuers(&all_issuers) {
            return Err(ConversionError::IssuerConversion(errors.join("; ")));
        }

        // Create issuer map
        let issuer_map = IssuerParser::create_issuer_map(all_issuers)
            .map_err(|e| ConversionError::IssuerConversion(e.to_string()))?;

        Ok(Some(issuer_map))
    }

    /// Convert entity files to `HashMap<String, serde_json::Value>`.
    fn convert_entities(
        entity_files: &[super::loader::EntityFile],
    ) -> Result<Option<HashMap<String, serde_json::Value>>, ConversionError> {
        if entity_files.is_empty() {
            return Ok(None);
        }

        let mut all_entities = HashMap::new();
        for file in entity_files {
            let parsed =
                EntityParser::parse_entities(&file.content, &file.name, None).map_err(|e| {
                    ConversionError::EntityConversion(format!(
                        "Failed to parse '{}': {}",
                        file.name, e
                    ))
                })?;

            for entity in parsed {
                let json_value = entity.entity.to_json_value().map_err(|e| {
                    ConversionError::EntityConversion(format!(
                        "Failed to serialize entity '{}': {}",
                        entity.uid, e
                    ))
                })?;
                all_entities.insert(entity.uid.to_string(), json_value);
            }
        }

        Ok(Some(all_entities))
    }

    /// Parse cedar version string to `semver::Version`.
    fn parse_cedar_version(version_str: &str) -> Result<Version, ConversionError> {
        // Handle optional "v" prefix
        let version_str = version_str.strip_prefix('v').unwrap_or(version_str);

        Version::parse(version_str).map_err(|e| ConversionError::VersionParsing {
            version: version_str.to_string(),
            details: e.to_string(),
        })
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::common::policy_store::loader::{EntityFile, IssuerFile, PolicyFile};
    use crate::common::policy_store::metadata::{PolicyStoreInfo, PolicyStoreMetadata};

    fn create_test_metadata() -> PolicyStoreMetadata {
        PolicyStoreMetadata {
            cedar_version: "4.0.0".to_string(),
            policy_store: PolicyStoreInfo {
                id: "test123".to_string(),
                name: "Test Store".to_string(),
                description: Some("A test policy store".to_string()),
                version: "1.0.0".to_string(),
                created_date: None,
                updated_date: None,
            },
        }
    }

    #[test]
    fn test_parse_cedar_version_valid() {
        let version = PolicyStoreManager::parse_cedar_version("4.0.0").unwrap();
        assert_eq!(version.major, 4);
        assert_eq!(version.minor, 0);
        assert_eq!(version.patch, 0);
    }

    #[test]
    fn test_parse_cedar_version_with_v_prefix() {
        let version = PolicyStoreManager::parse_cedar_version("v4.1.2").unwrap();
        assert_eq!(version.major, 4);
        assert_eq!(version.minor, 1);
        assert_eq!(version.patch, 2);
    }

    #[test]
    fn test_parse_cedar_version_invalid() {
        let result = PolicyStoreManager::parse_cedar_version("invalid");
        assert!(result.is_err());
    }

    #[test]
    fn test_convert_schema_valid() {
        let schema_content = r#"
            namespace TestApp {
                entity User;
                entity Resource;
                action "read" appliesTo {
                    principal: [User],
                    resource: [Resource]
                };
            }
        "#;

        let result = PolicyStoreManager::convert_schema(schema_content);
        assert!(
            result.is_ok(),
            "Schema conversion failed: {:?}",
            result.err()
        );

        let cedar_schema = result.unwrap();
        // Verify schema has expected entity types
        let entity_types: Vec<_> = cedar_schema.schema.entity_types().collect();
        assert!(!entity_types.is_empty());
    }

    #[test]
    fn test_convert_schema_invalid() {
        let schema_content = "this is not valid cedar schema syntax {{{";
        let result = PolicyStoreManager::convert_schema(schema_content);
        assert!(result.is_err());
    }

    #[test]
    fn test_convert_policies_valid() {
        let policy_files = vec![
            PolicyFile {
                name: "allow.cedar".to_string(),
                content: "permit(principal, action, resource);".to_string(),
            },
            PolicyFile {
                name: "deny.cedar".to_string(),
                content: "forbid(principal, action, resource);".to_string(),
            },
        ];

        let result = PolicyStoreManager::convert_policies(&policy_files);
        assert!(
            result.is_ok(),
            "Policy conversion failed: {:?}",
            result.err()
        );

        let container = result.unwrap();
        assert!(!container.get_set().is_empty());
    }

    #[test]
    fn test_convert_policies_empty() {
        let policy_files: Vec<PolicyFile> = vec![];
        let result = PolicyStoreManager::convert_policies(&policy_files);
        assert!(result.is_ok());

        let container = result.unwrap();
        assert!(container.get_set().is_empty());
    }

    #[test]
    fn test_convert_policies_invalid() {
        let policy_files = vec![PolicyFile {
            name: "invalid.cedar".to_string(),
            content: "this is not valid cedar policy".to_string(),
        }];

        let result = PolicyStoreManager::convert_policies(&policy_files);
        assert!(result.is_err());
    }

    #[test]
    fn test_convert_trusted_issuers_valid() {
        let issuer_files = vec![IssuerFile {
            name: "issuer.json".to_string(),
            content: r#"{
                "test_issuer": {
                    "name": "Test Issuer",
                    "description": "A test issuer",
                    "openid_configuration_endpoint": "https://test.com/.well-known/openid-configuration",
                    "token_metadata": {
                        "access_token": {
                            "entity_type_name": "Test::access_token"
                        }
                    }
                }
            }"#
            .to_string(),
        }];

        let result = PolicyStoreManager::convert_trusted_issuers(&issuer_files);
        assert!(
            result.is_ok(),
            "Issuer conversion failed: {:?}",
            result.err()
        );

        let issuers = result.unwrap();
        assert!(issuers.is_some());
        let issuers = issuers.unwrap();
        assert_eq!(issuers.len(), 1);
        assert!(issuers.contains_key("test_issuer"));
    }

    #[test]
    fn test_convert_trusted_issuers_empty() {
        let issuer_files: Vec<IssuerFile> = vec![];
        let result = PolicyStoreManager::convert_trusted_issuers(&issuer_files);
        assert!(result.is_ok());
        assert!(result.unwrap().is_none());
    }

    #[test]
    fn test_convert_entities_valid() {
        let entity_files = vec![EntityFile {
            name: "users.json".to_string(),
            content: r#"[
                {
                    "uid": {"type": "User", "id": "alice"},
                    "attrs": {"name": "Alice"},
                    "parents": []
                }
            ]"#
            .to_string(),
        }];

        let result = PolicyStoreManager::convert_entities(&entity_files);
        assert!(
            result.is_ok(),
            "Entity conversion failed: {:?}",
            result.err()
        );

        let entities = result.unwrap();
        assert!(entities.is_some());
        let entities = entities.unwrap();
        assert_eq!(entities.len(), 1);
    }

    #[test]
    fn test_convert_entities_empty() {
        let entity_files: Vec<EntityFile> = vec![];
        let result = PolicyStoreManager::convert_entities(&entity_files);
        assert!(result.is_ok());
        assert!(result.unwrap().is_none());
    }

    #[test]
    fn test_convert_to_legacy_minimal() {
        let loaded = LoadedPolicyStore {
            metadata: create_test_metadata(),
            manifest: None,
            schema: r#"
                namespace TestApp {
                    entity User;
                    action "read" appliesTo {
                        principal: [User],
                        resource: [User]
                    };
                }
            "#
            .to_string(),
            policies: vec![PolicyFile {
                name: "test.cedar".to_string(),
                content: "permit(principal, action, resource);".to_string(),
            }],
            templates: vec![],
            entities: vec![],
            trusted_issuers: vec![],
        };

        let result = PolicyStoreManager::convert_to_legacy(loaded);
        assert!(result.is_ok(), "Conversion failed: {:?}", result.err());

        let store = result.unwrap();
        assert_eq!(store.name, "Test Store");
        assert_eq!(store.version, Some("1.0.0".to_string()));
        assert_eq!(store.description, Some("A test policy store".to_string()));
        assert!(store.cedar_version.is_some());
        assert!(!store.policies.get_set().is_empty());
        assert!(store.trusted_issuers.is_none());
        assert!(store.default_entities.is_none());
    }

    #[test]
    fn test_convert_to_legacy_full() {
        let loaded = LoadedPolicyStore {
            metadata: create_test_metadata(),
            manifest: None,
            schema: r#"
                namespace TestApp {
                    entity User;
                    action "read" appliesTo {
                        principal: [User],
                        resource: [User]
                    };
                }
            "#
            .to_string(),
            policies: vec![PolicyFile {
                name: "test.cedar".to_string(),
                content: "permit(principal, action, resource);".to_string(),
            }],
            templates: vec![],
            entities: vec![EntityFile {
                name: "users.json".to_string(),
                content: r#"[{"uid": {"type": "User", "id": "alice"}, "attrs": {}, "parents": []}]"#
                    .to_string(),
            }],
            trusted_issuers: vec![IssuerFile {
                name: "issuer.json".to_string(),
                content: r#"{
                    "main": {
                        "name": "Main Issuer",
                        "description": "Primary issuer",
                        "openid_configuration_endpoint": "https://auth.test/.well-known/openid-configuration",
                        "token_metadata": {
                            "access_token": {
                                "entity_type_name": "Test::access_token"
                            }
                        }
                    }
                }"#
                .to_string(),
            }],
        };

        let result = PolicyStoreManager::convert_to_legacy(loaded);
        assert!(result.is_ok(), "Conversion failed: {:?}", result.err());

        let store = result.unwrap();
        assert!(store.trusted_issuers.is_some());
        assert!(store.default_entities.is_some());

        let issuers = store.trusted_issuers.unwrap();
        assert!(issuers.contains_key("main"));

        let entities = store.default_entities.unwrap();
        assert_eq!(entities.len(), 1);
    }
}
