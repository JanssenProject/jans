// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Trusted issuer configuration parsing and validation.
//!
//! This module provides functionality to parse and validate trusted issuer configuration files,
//! ensuring they conform to the required schema with proper token metadata and required fields.

use super::errors::{PolicyStoreError, ValidationError};
use super::{TokenEntityMetadata, TrustedIssuer};
use serde_json::Value as JsonValue;
use std::collections::HashMap;
use url::Url;

/// A parsed trusted issuer configuration with metadata.
#[derive(Debug, Clone)]
pub struct ParsedIssuer {
    /// The issuer name (used as key/id)
    pub id: String,
    /// The trusted issuer configuration
    pub issuer: TrustedIssuer,
    /// Source filename
    pub filename: String,
    /// Raw JSON content
    pub content: String,
}

/// Issuer parser for loading and validating trusted issuer configurations.
pub struct IssuerParser;

impl IssuerParser {
    /// Parse a trusted issuer configuration from JSON content.
    ///
    /// Validates the required fields and token metadata structure.
    pub fn parse_issuer(
        content: &str,
        filename: &str,
    ) -> Result<Vec<ParsedIssuer>, PolicyStoreError> {
        // Parse JSON
        let json_value: JsonValue =
            serde_json::from_str(content).map_err(|e| PolicyStoreError::JsonParsing {
                file: filename.to_string(),
                source: e,
            })?;

        // Trusted issuer files should be objects mapping issuer IDs to configurations
        let obj = json_value
            .as_object()
            .ok_or_else(|| ValidationError::InvalidTrustedIssuer {
                file: filename.to_string(),
                message: "Trusted issuer file must be a JSON object".to_string(),
            })?;

        let mut parsed_issuers = Vec::with_capacity(obj.len());

        for (issuer_id, issuer_json) in obj {
            // Validate and parse the issuer configuration
            let issuer = Self::parse_single_issuer(issuer_json, issuer_id, filename)?;

            // Store only this issuer's JSON, not the entire file content
            let issuer_content = serde_json::to_string(issuer_json).unwrap_or_default();

            parsed_issuers.push(ParsedIssuer {
                id: issuer_id.clone(),
                issuer,
                filename: filename.to_string(),
                content: issuer_content,
            });
        }

        Ok(parsed_issuers)
    }

    /// Parse a single trusted issuer configuration.
    fn parse_single_issuer(
        issuer_json: &JsonValue,
        issuer_id: &str,
        filename: &str,
    ) -> Result<TrustedIssuer, PolicyStoreError> {
        let obj = issuer_json
            .as_object()
            .ok_or_else(|| ValidationError::InvalidTrustedIssuer {
                file: filename.to_string(),
                message: format!("Issuer '{}' must be a JSON object", issuer_id),
            })?;

        // Validate required fields
        let name = obj.get("name").and_then(|v| v.as_str()).ok_or_else(|| {
            ValidationError::InvalidTrustedIssuer {
                file: filename.to_string(),
                message: format!("Issuer '{}': missing required field 'name'", issuer_id),
            }
        })?;

        let description = obj
            .get("description")
            .and_then(|v| v.as_str())
            .unwrap_or("");

        // Validate openid_configuration_endpoint
        let oidc_endpoint_str = obj
            .get("openid_configuration_endpoint")
            .and_then(|v| v.as_str())
            .ok_or_else(|| ValidationError::InvalidTrustedIssuer {
                file: filename.to_string(),
                message: format!(
                    "Issuer '{}': missing required field 'openid_configuration_endpoint'",
                    issuer_id
                ),
            })?;

        let oidc_endpoint =
            Url::parse(oidc_endpoint_str).map_err(|e| ValidationError::InvalidTrustedIssuer {
                file: filename.to_string(),
                message: format!(
                    "Issuer '{}': invalid URL for 'openid_configuration_endpoint': {}",
                    issuer_id, e
                ),
            })?;

        // Parse token_metadata (optional but recommended)
        let token_metadata = if let Some(metadata_json) = obj.get("token_metadata") {
            Self::parse_token_metadata(metadata_json, issuer_id, filename)?
        } else {
            HashMap::new()
        };

        Ok(TrustedIssuer {
            name: name.to_string(),
            description: description.to_string(),
            oidc_endpoint,
            token_metadata,
        })
    }

    /// Parse token metadata configurations.
    fn parse_token_metadata(
        metadata_json: &JsonValue,
        issuer_id: &str,
        filename: &str,
    ) -> Result<HashMap<String, TokenEntityMetadata>, PolicyStoreError> {
        let metadata_obj =
            metadata_json
                .as_object()
                .ok_or_else(|| ValidationError::InvalidTrustedIssuer {
                    file: filename.to_string(),
                    message: format!(
                        "Issuer '{}': 'token_metadata' must be a JSON object",
                        issuer_id
                    ),
                })?;

        // Convert to owned map to avoid cloning during iteration
        let metadata_map: serde_json::Map<String, JsonValue> = metadata_obj.clone();
        let mut token_metadata = HashMap::with_capacity(metadata_map.len());

        for (token_type, token_config) in metadata_map {
            // Deserialize the TokenEntityMetadata
            let metadata: TokenEntityMetadata =
                serde_json::from_value(token_config).map_err(|e| {
                    ValidationError::InvalidTrustedIssuer {
                        file: filename.to_string(),
                        message: format!(
                            "Issuer '{}': invalid token metadata for '{}': {}",
                            issuer_id, token_type, e
                        ),
                    }
                })?;

            // Validate required field: entity_type_name
            if metadata.entity_type_name.is_empty() {
                return Err(ValidationError::InvalidTrustedIssuer {
                    file: filename.to_string(),
                    message: format!(
                        "Issuer '{}': token type '{}' missing required field 'entity_type_name'",
                        issuer_id, token_type
                    ),
                }
                .into());
            }

            token_metadata.insert(token_type, metadata);
        }

        Ok(token_metadata)
    }

    /// Validate a collection of parsed issuers for conflicts and completeness.
    pub fn validate_issuers(issuers: &[ParsedIssuer]) -> Result<(), Vec<String>> {
        let mut errors = Vec::new();
        let mut seen_ids = HashMap::with_capacity(issuers.len());

        for parsed in issuers {
            // Check for duplicate issuer IDs (only insert if not duplicate)
            if let Some(existing_file) = seen_ids.get(&parsed.id) {
                errors.push(format!(
                    "Duplicate issuer ID '{}' found in files '{}' and '{}'",
                    parsed.id, existing_file, parsed.filename
                ));
                // Don't insert the duplicate - keep the first occurrence
            } else {
                seen_ids.insert(parsed.id.clone(), parsed.filename.clone());
            }

            // Validate token metadata completeness
            if parsed.issuer.token_metadata.is_empty() {
                errors.push(format!(
                    "Issuer '{}' in file '{}' has no token metadata configured",
                    parsed.id, parsed.filename
                ));
            }
        }

        if errors.is_empty() {
            Ok(())
        } else {
            Err(errors)
        }
    }

    /// Create a consolidated map of all issuers.
    pub fn create_issuer_map(
        issuers: Vec<ParsedIssuer>,
    ) -> Result<HashMap<String, TrustedIssuer>, PolicyStoreError> {
        let mut issuer_map = HashMap::with_capacity(issuers.len());

        for parsed in issuers {
            // Check for duplicates (shouldn't happen if validate_issuers was called)
            if issuer_map.contains_key(&parsed.id) {
                return Err(ValidationError::InvalidTrustedIssuer {
                    file: parsed.filename,
                    message: format!("Duplicate issuer ID '{}'", parsed.id),
                }
                .into());
            }

            issuer_map.insert(parsed.id, parsed.issuer);
        }

        Ok(issuer_map)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_simple_issuer() {
        let content = r#"{
            "test_issuer": {
                "name": "Test Issuer",
                "description": "A test OpenID Connect provider",
                "openid_configuration_endpoint": "https://accounts.test.com/.well-known/openid-configuration"
            }
        }"#;

        let result = IssuerParser::parse_issuer(content, "issuer1.json");
        assert!(result.is_ok(), "Should parse simple issuer");

        let parsed = result.unwrap();
        assert_eq!(parsed.len(), 1, "Should have 1 issuer");
        assert_eq!(parsed[0].id, "test_issuer");
        assert_eq!(parsed[0].issuer.name, "Test Issuer");
        assert_eq!(
            parsed[0].issuer.description,
            "A test OpenID Connect provider"
        );
        assert_eq!(
            parsed[0].issuer.oidc_endpoint.as_str(),
            "https://accounts.test.com/.well-known/openid-configuration"
        );
    }

    #[test]
    fn test_parse_issuer_with_token_metadata() {
        let content = r#"{
            "jans_issuer": {
                "name": "Jans Server",
                "description": "Jans OpenID Connect Provider",
                "openid_configuration_endpoint": "https://jans.test/.well-known/openid-configuration",
                "token_metadata": {
                    "access_token": {
                        "trusted": true,
                        "entity_type_name": "Jans::access_token",
                        "user_id": "sub",
                        "role_mapping": "role"
                    },
                    "id_token": {
                        "trusted": true,
                        "entity_type_name": "Jans::id_token",
                        "user_id": "sub"
                    }
                }
            }
        }"#;

        let result = IssuerParser::parse_issuer(content, "jans.json");
        assert!(result.is_ok(), "Should parse issuer with token metadata");

        let parsed = result.unwrap();
        assert_eq!(parsed.len(), 1);
        assert_eq!(parsed[0].issuer.token_metadata.len(), 2);

        let access_token = parsed[0].issuer.token_metadata.get("access_token").unwrap();
        assert_eq!(access_token.entity_type_name, "Jans::access_token");
        assert_eq!(access_token.user_id, Some("sub".to_string()));
    }

    #[test]
    fn test_parse_multiple_issuers() {
        let content = r#"{
            "issuer1": {
                "name": "Issuer One",
                "description": "First issuer",
                "openid_configuration_endpoint": "https://issuer1.com/.well-known/openid-configuration"
            },
            "issuer2": {
                "name": "Issuer Two",
                "description": "Second issuer",
                "openid_configuration_endpoint": "https://issuer2.com/.well-known/openid-configuration"
            }
        }"#;

        let result = IssuerParser::parse_issuer(content, "issuers.json");
        assert!(result.is_ok(), "Should parse multiple issuers");

        let parsed = result.unwrap();
        assert_eq!(parsed.len(), 2, "Should have 2 issuers");
    }

    #[test]
    fn test_parse_issuer_missing_name() {
        let content = r#"{
            "bad_issuer": {
                "description": "Missing name field",
                "openid_configuration_endpoint": "https://test.com/.well-known/openid-configuration"
            }
        }"#;

        let result = IssuerParser::parse_issuer(content, "bad.json");
        assert!(result.is_err(), "Should fail on missing name");

        if let Err(PolicyStoreError::Validation(ValidationError::InvalidTrustedIssuer {
            file,
            message,
        })) = result
        {
            assert_eq!(file, "bad.json");
            assert!(message.contains("name"));
        } else {
            panic!("Expected ValidationError::InvalidTrustedIssuer");
        }
    }

    #[test]
    fn test_parse_issuer_missing_endpoint() {
        let content = r#"{
            "bad_issuer": {
                "name": "Test",
                "description": "Missing endpoint"
            }
        }"#;

        let result = IssuerParser::parse_issuer(content, "bad.json");
        assert!(result.is_err(), "Should fail on missing endpoint");

        if let Err(PolicyStoreError::Validation(ValidationError::InvalidTrustedIssuer {
            message,
            ..
        })) = result
        {
            assert!(message.contains("openid_configuration_endpoint"));
        } else {
            panic!("Expected ValidationError::InvalidTrustedIssuer");
        }
    }

    #[test]
    fn test_parse_issuer_invalid_url() {
        let content = r#"{
            "bad_issuer": {
                "name": "Test",
                "description": "Invalid URL",
                "openid_configuration_endpoint": "not a valid url"
            }
        }"#;

        let result = IssuerParser::parse_issuer(content, "bad.json");
        assert!(result.is_err(), "Should fail on invalid URL");

        if let Err(PolicyStoreError::Validation(ValidationError::InvalidTrustedIssuer {
            message,
            ..
        })) = result
        {
            assert!(message.contains("invalid URL"));
        } else {
            panic!("Expected ValidationError::InvalidTrustedIssuer");
        }
    }

    #[test]
    fn test_parse_issuer_invalid_json() {
        let content = "{ invalid json }";

        let result = IssuerParser::parse_issuer(content, "invalid.json");
        assert!(result.is_err(), "Should fail on invalid JSON");

        if let Err(PolicyStoreError::JsonParsing { file, .. }) = result {
            assert_eq!(file, "invalid.json");
        } else {
            panic!("Expected JsonParsing error");
        }
    }

    #[test]
    fn test_parse_token_metadata_missing_entity_type() {
        let content = r#"{
            "issuer1": {
                "name": "Test",
                "description": "Test",
                "openid_configuration_endpoint": "https://test.com/.well-known/openid-configuration",
                "token_metadata": {
                    "access_token": {
                        "trusted": true
                    }
                }
            }
        }"#;

        let result = IssuerParser::parse_issuer(content, "bad.json");
        assert!(
            result.is_err(),
            "Should fail on missing entity_type_name in token metadata"
        );
    }

    #[test]
    fn test_validate_issuers_no_duplicates() {
        let issuers = vec![
            ParsedIssuer {
                id: "issuer1".to_string(),
                issuer: TrustedIssuer {
                    name: "Issuer 1".to_string(),
                    description: "First".to_string(),
                    oidc_endpoint: Url::parse(
                        "https://issuer1.com/.well-known/openid-configuration",
                    )
                    .unwrap(),
                    token_metadata: HashMap::from([(
                        "access_token".to_string(),
                        TokenEntityMetadata::access_token(),
                    )]),
                },
                filename: "file1.json".to_string(),
                content: String::new(),
            },
            ParsedIssuer {
                id: "issuer2".to_string(),
                issuer: TrustedIssuer {
                    name: "Issuer 2".to_string(),
                    description: "Second".to_string(),
                    oidc_endpoint: Url::parse(
                        "https://issuer2.com/.well-known/openid-configuration",
                    )
                    .unwrap(),
                    token_metadata: HashMap::from([(
                        "id_token".to_string(),
                        TokenEntityMetadata::id_token(),
                    )]),
                },
                filename: "file2.json".to_string(),
                content: String::new(),
            },
        ];

        let result = IssuerParser::validate_issuers(&issuers);
        assert!(result.is_ok(), "Should have no validation errors");
    }

    #[test]
    fn test_validate_issuers_duplicate_ids() {
        let issuers = vec![
            ParsedIssuer {
                id: "issuer1".to_string(),
                issuer: TrustedIssuer {
                    name: "Issuer 1".to_string(),
                    description: "First".to_string(),
                    oidc_endpoint: Url::parse(
                        "https://issuer1.com/.well-known/openid-configuration",
                    )
                    .unwrap(),
                    token_metadata: HashMap::from([(
                        "access_token".to_string(),
                        TokenEntityMetadata::access_token(),
                    )]),
                },
                filename: "file1.json".to_string(),
                content: String::new(),
            },
            ParsedIssuer {
                id: "issuer1".to_string(),
                issuer: TrustedIssuer {
                    name: "Issuer 1 Duplicate".to_string(),
                    description: "Duplicate".to_string(),
                    oidc_endpoint: Url::parse(
                        "https://issuer1.com/.well-known/openid-configuration",
                    )
                    .unwrap(),
                    token_metadata: HashMap::from([(
                        "id_token".to_string(),
                        TokenEntityMetadata::id_token(),
                    )]),
                },
                filename: "file2.json".to_string(),
                content: String::new(),
            },
        ];

        let result = IssuerParser::validate_issuers(&issuers);
        assert!(result.is_err(), "Should detect duplicate issuer IDs");

        let errors = result.unwrap_err();
        assert_eq!(errors.len(), 1);
        assert!(errors[0].contains("issuer1"));
        assert!(errors[0].contains("file1.json"));
        assert!(errors[0].contains("file2.json"));
    }

    #[test]
    fn test_validate_issuers_no_token_metadata() {
        let issuers = vec![ParsedIssuer {
            id: "issuer1".to_string(),
            issuer: TrustedIssuer {
                name: "Issuer 1".to_string(),
                description: "No tokens".to_string(),
                oidc_endpoint: Url::parse("https://issuer1.com/.well-known/openid-configuration")
                    .unwrap(),
                token_metadata: HashMap::new(),
            },
            filename: "file1.json".to_string(),
            content: String::new(),
        }];

        let result = IssuerParser::validate_issuers(&issuers);
        assert!(result.is_err(), "Should warn about missing token metadata");

        let errors = result.unwrap_err();
        assert_eq!(errors.len(), 1);
        assert!(errors[0].contains("no token metadata"));
    }

    #[test]
    fn test_create_issuer_map() {
        let issuers = vec![
            ParsedIssuer {
                id: "issuer1".to_string(),
                issuer: TrustedIssuer {
                    name: "Issuer 1".to_string(),
                    description: "First".to_string(),
                    oidc_endpoint: Url::parse(
                        "https://issuer1.com/.well-known/openid-configuration",
                    )
                    .unwrap(),
                    token_metadata: HashMap::from([(
                        "access_token".to_string(),
                        TokenEntityMetadata::access_token(),
                    )]),
                },
                filename: "file1.json".to_string(),
                content: String::new(),
            },
            ParsedIssuer {
                id: "issuer2".to_string(),
                issuer: TrustedIssuer {
                    name: "Issuer 2".to_string(),
                    description: "Second".to_string(),
                    oidc_endpoint: Url::parse(
                        "https://issuer2.com/.well-known/openid-configuration",
                    )
                    .unwrap(),
                    token_metadata: HashMap::from([(
                        "id_token".to_string(),
                        TokenEntityMetadata::id_token(),
                    )]),
                },
                filename: "file2.json".to_string(),
                content: String::new(),
            },
        ];

        let result = IssuerParser::create_issuer_map(issuers);
        assert!(result.is_ok(), "Should create issuer map");

        let map = result.unwrap();
        assert_eq!(map.len(), 2);
        assert!(map.contains_key("issuer1"));
        assert!(map.contains_key("issuer2"));
    }
}
