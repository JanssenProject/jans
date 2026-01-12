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

use super::entity_parser::EntityParser;
use super::issuer_parser::IssuerParser;
use super::loader::LoadedPolicyStore;
use super::log_entry::PolicyStoreLogEntry;
use super::policy_parser::PolicyParser;
use super::{PoliciesContainer, PolicyStore, TrustedIssuer};
use crate::common::cedar_schema::CedarSchema;
use crate::common::cedar_schema::cedar_json::CedarSchemaJson;
use crate::common::default_entities::parse_default_entities_with_warns;
use crate::log::Logger;
use crate::log::interface::LogWriter;
use cedar_policy::PolicySet;
use cedar_policy_core::extensions::Extensions;
use cedar_policy_core::validator::ValidatorSchema;
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
pub(crate) struct PolicyStoreManager;

impl PolicyStoreManager {
    /// Convert a `LoadedPolicyStore` (new format) to `PolicyStore` (legacy format).
    ///
    /// This is the main entry point for converting policy stores loaded from
    /// directory or archive format into the legacy format used by the rest of Cedarling.
    pub(crate) fn convert_to_legacy(
        loaded: LoadedPolicyStore,
    ) -> Result<PolicyStore, ConversionError> {
        Self::convert_to_legacy_with_logger(loaded, None)
    }

    /// Convert a `LoadedPolicyStore` to `PolicyStore` with optional logging.
    ///
    /// This version accepts an optional logger for structured logging during conversion.
    /// Use this when a logger is available to get detailed conversion logs.
    fn convert_to_legacy_with_logger(
        loaded: LoadedPolicyStore,
        logger: Option<Logger>,
    ) -> Result<PolicyStore, ConversionError> {
        // Log manifest info if available
        if let Some(manifest) = &loaded.manifest {
            logger.log_any(PolicyStoreLogEntry::info(format!(
                "Converting policy store '{}' (generated: {})",
                manifest.policy_store_id, manifest.generated_date
            )));
        }

        // 1. Convert schema
        let cedar_schema = Self::convert_schema(&loaded.schema)?;

        // 2. Convert policies and templates into a single PoliciesContainer
        let policies_container =
            Self::convert_policies_and_templates(&loaded.policies, &loaded.templates)?;

        // 3. Convert trusted issuers
        let trusted_issuers = Self::convert_trusted_issuers(&loaded.trusted_issuers)?;

        // 4. Convert entities (logs hierarchy warnings if logger provided)
        let raw_entities = Self::convert_entities(&loaded.entities, &logger)?;

        // Convert raw entities to DefaultEntitiesWithWarns
        let default_entities = parse_default_entities_with_warns(raw_entities).map_err(|e| {
            ConversionError::EntityConversion(format!("Failed to parse default entities: {}", e))
        })?;

        // 5. Parse cedar version
        let cedar_version = Self::parse_cedar_version(&loaded.metadata.cedar_version)?;

        logger.log_any(PolicyStoreLogEntry::info(format!(
            "Policy store conversion complete: {} policies, {} issuers, {} entities",
            policies_container.get_set().policies().count(),
            trusted_issuers.as_ref().map(|i| i.len()).unwrap_or(0),
            default_entities.entities().len()
        )));

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
    /// Uses `ParsedSchema::parse` to parse and validate the schema, then converts
    /// to the `CedarSchema` format required by the legacy system.
    ///
    /// The `CedarSchema` requires:
    /// - `schema: cedar_policy::Schema`
    /// - `json: CedarSchemaJson`
    /// - `validator_schema: ValidatorSchema`
    fn convert_schema(schema_content: &str) -> Result<CedarSchema, ConversionError> {
        use super::schema_parser::ParsedSchema;
        use cedar_policy::SchemaFragment;
        use std::str::FromStr;

        // Parse and validate schema
        let parsed_schema =
            ParsedSchema::parse(schema_content, "schema.cedarschema").map_err(|e| {
                ConversionError::SchemaConversion(format!("Failed to parse schema: {}", e))
            })?;

        // Validate the schema
        parsed_schema.validate().map_err(|e| {
            ConversionError::SchemaConversion(format!("Schema validation failed: {}", e))
        })?;

        // Get the Cedar schema from the parsed result
        let schema = parsed_schema.get_schema().clone();

        // Convert to JSON for CedarSchemaJson and ValidatorSchema
        // NOTE: This parses the schema content again (SchemaFragment::from_str).
        // For large schemas, this double-parsing could be optimized by having
        // ParsedSchema return both the validated schema and the fragment, but
        // this is a performance consideration rather than a correctness issue.
        let fragment = SchemaFragment::from_str(schema_content).map_err(|e| {
            ConversionError::SchemaConversion(format!("Failed to parse schema fragment: {}", e))
        })?;

        let json_string = fragment.to_json_string().map_err(|e| {
            ConversionError::SchemaConversion(format!("Failed to serialize schema to JSON: {}", e))
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

    /// Convert policy and template files to `PoliciesContainer`.
    ///
    /// The `PoliciesContainer` requires:
    /// - `policy_set: cedar_policy::PolicySet` (includes both policies and templates)
    /// - `raw_policy_info: HashMap<String, RawPolicy>` (for descriptions)
    fn convert_policies_and_templates(
        policy_files: &[super::loader::PolicyFile],
        template_files: &[super::loader::PolicyFile],
    ) -> Result<PoliciesContainer, ConversionError> {
        if policy_files.is_empty() && template_files.is_empty() {
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

        // Parse each template file
        let mut parsed_templates = Vec::with_capacity(template_files.len());
        for file in template_files {
            let parsed = PolicyParser::parse_template(&file.content, &file.name).map_err(|e| {
                ConversionError::PolicyConversion(format!(
                    "Failed to parse template '{}': {}",
                    file.name, e
                ))
            })?;
            parsed_templates.push(parsed);
        }

        // Create policy set using PolicyParser (includes both policies and templates)
        let policy_set =
            PolicyParser::create_policy_set(parsed_policies.clone(), parsed_templates.clone())
                .map_err(|e| ConversionError::PolicySetCreation(e.to_string()))?;

        // Build raw_policy_info for descriptions (policies only, templates don't have descriptions in legacy format)
        let raw_policy_info = parsed_policies
            .into_iter()
            .map(|p| (p.id.to_string(), format!("Policy from {}", p.filename)))
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

        // Validate for duplicates - include content in error for debugging
        if let Err(errors) = IssuerParser::validate_issuers(&all_issuers) {
            // Return validation errors directly, joined into a single string
            let error_details = errors
                .iter()
                .map(|e| e.to_string())
                .collect::<Vec<_>>()
                .join("; ");
            return Err(ConversionError::IssuerConversion(error_details));
        }

        // Create issuer map
        let issuer_map = IssuerParser::create_issuer_map(all_issuers)
            .map_err(|e| ConversionError::IssuerConversion(e.to_string()))?;

        Ok(Some(issuer_map))
    }

    /// Convert entity files to `HashMap<String, serde_json::Value>`.
    ///
    /// This function:
    /// 1. Parses all entity files
    /// 2. Detects duplicate entity UIDs (returns error if found)
    /// 3. Optionally validates entity hierarchy (parent references - logs warnings if logger provided)
    /// 4. Converts to the required HashMap format
    ///
    /// # Arguments
    ///
    /// * `entity_files` - The entity files to convert
    /// * `logger` - Optional logger for hierarchy warnings
    fn convert_entities(
        entity_files: &[super::loader::EntityFile],
        logger: &Option<Logger>,
    ) -> Result<Option<HashMap<String, serde_json::Value>>, ConversionError> {
        if entity_files.is_empty() {
            return Ok(None);
        }

        // Step 1: Parse all entity files
        let mut all_parsed_entities = Vec::new();
        for file in entity_files {
            let parsed =
                EntityParser::parse_entities(&file.content, &file.name, None).map_err(|e| {
                    ConversionError::EntityConversion(format!(
                        "Failed to parse '{}': {}",
                        file.name, e
                    ))
                })?;
            all_parsed_entities.extend(parsed);
        }

        // Step 2: Detect duplicate entity UIDs (warns but doesn't fail on duplicates)
        // Note: We clone all_parsed_entities here because EntityParser::detect_duplicates
        // takes ownership of the Vec. This preserves the original for later hierarchy validation.
        // Duplicates are handled gracefully - the latest entity wins and a warning is logged.
        let unique_entities = EntityParser::detect_duplicates(all_parsed_entities.clone(), logger);

        // Step 3: Validate entity hierarchy (optional - parent entities may be provided at runtime)
        // This ensures all parent references point to entities that exist in this store
        // Note: Hierarchy validation errors are non-fatal since parent entities
        // might be provided at runtime via authorization requests
        if let Err(warnings) = EntityParser::validate_hierarchy(&all_parsed_entities) {
            logger.log_any(PolicyStoreLogEntry::warn(format!(
                "Entity hierarchy validation warnings (non-fatal): {:?}",
                warnings
            )));
        }

        // Step 4: Validate entities can form a valid Cedar entity store
        // This validates entity constraints like types and attribute compatibility
        EntityParser::create_entities_store(all_parsed_entities).map_err(|e| {
            ConversionError::EntityConversion(format!("Failed to create entity store: {}", e))
        })?;

        // Step 5: Convert to HashMap<String, Value>
        let mut result = HashMap::with_capacity(unique_entities.len());
        for (uid, parsed_entity) in unique_entities {
            let json_value = parsed_entity.entity.to_json_value().map_err(|e| {
                // Include the original content in the error for debugging
                ConversionError::EntityConversion(format!(
                    "Failed to serialize entity '{}' from '{}': {}. Original content: {}",
                    uid,
                    parsed_entity.filename,
                    e,
                    if parsed_entity.content.len() > 200 {
                        format!("{}...(truncated)", &parsed_entity.content[..200])
                    } else {
                        parsed_entity.content.clone()
                    }
                ))
            })?;
            result.insert(uid.to_string(), json_value);
        }

        Ok(Some(result))
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
        let err = result.expect_err("Expected error for invalid version format");
        assert!(
            matches!(err, ConversionError::VersionParsing { .. }),
            "Expected VersionParsing error, got: {:?}",
            err
        );
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
        let err = result.expect_err("Expected error for invalid Cedar schema syntax");
        assert!(
            matches!(err, ConversionError::SchemaConversion(_)),
            "Expected SchemaConversion error, got: {:?}",
            err
        );
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
        let template_files: Vec<PolicyFile> = vec![];

        let result =
            PolicyStoreManager::convert_policies_and_templates(&policy_files, &template_files);
        assert!(
            result.is_ok(),
            "Policy conversion failed: {:?}",
            result.err()
        );

        let container = result.unwrap();
        assert!(!container.get_set().is_empty());
    }

    #[test]
    fn test_convert_policies_with_templates() {
        let policy_files = vec![PolicyFile {
            name: "allow.cedar".to_string(),
            content: "permit(principal, action, resource);".to_string(),
        }];
        let template_files = vec![PolicyFile {
            name: "template.cedar".to_string(),
            content: "permit(principal == ?principal, action, resource);".to_string(),
        }];

        let result =
            PolicyStoreManager::convert_policies_and_templates(&policy_files, &template_files);
        assert!(
            result.is_ok(),
            "Policy/template conversion failed: {:?}",
            result.err()
        );

        let container = result.unwrap();
        assert!(!container.get_set().is_empty());
    }

    #[test]
    fn test_convert_policies_empty() {
        let policy_files: Vec<PolicyFile> = vec![];
        let template_files: Vec<PolicyFile> = vec![];
        let result =
            PolicyStoreManager::convert_policies_and_templates(&policy_files, &template_files);
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
        let template_files: Vec<PolicyFile> = vec![];

        let result =
            PolicyStoreManager::convert_policies_and_templates(&policy_files, &template_files);
        let err = result.expect_err("Expected ConversionError for invalid policy syntax");
        assert!(
            matches!(err, ConversionError::PolicyConversion(_)),
            "Expected PolicyConversion error, got: {:?}",
            err
        );
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

        let result = PolicyStoreManager::convert_entities(&entity_files, &None);
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
        let result = PolicyStoreManager::convert_entities(&entity_files, &None);
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
        assert_eq!(store.default_entities.entities().len(), 0);
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
        assert!(store.default_entities.entities().len() > 0);

        let issuers = store.trusted_issuers.unwrap();
        assert!(issuers.contains_key("main"));

        assert_eq!(store.default_entities.entities().len(), 1);
    }
}
