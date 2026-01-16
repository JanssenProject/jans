// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Trusted issuer configuration parsing and validation.
//!
//! This module provides functionality to parse and validate trusted issuer configuration files,
//! ensuring they conform to the required schema with proper token metadata and required fields.

use super::errors::{PolicyStoreError, TrustedIssuerErrorType};
use super::{TokenEntityMetadata, TrustedIssuer};
use serde_json::Value as JsonValue;
use std::collections::HashMap;
use url::Url;

/// A parsed trusted issuer configuration with metadata.
#[derive(Debug, Clone)]
pub(super) struct ParsedIssuer {
    /// The issuer name (used as key/id)
    pub id: String,
    /// The trusted issuer configuration
    pub issuer: TrustedIssuer,
    /// Source filename
    pub filename: String,
}

/// Issuer parser for loading and validating trusted issuer configurations.
pub(super) struct IssuerParser;

impl IssuerParser {
    /// Parse a trusted issuer configuration from JSON content.
    ///
    /// Validates the required fields and token metadata structure.
    pub(super) fn parse_issuer(
        content: &str,
        filename: &str,
    ) -> Result<Vec<ParsedIssuer>, PolicyStoreError> {
        // Parse JSON
        let json_value: JsonValue =
            serde_json::from_str(content).map_err(|e| PolicyStoreError::JsonParsing {
                file: filename.to_string(),
                source: e,
            })?;

        let obj = json_value
            .as_object()
            .ok_or_else(|| PolicyStoreError::TrustedIssuerError {
                file: filename.to_string(),
                err: TrustedIssuerErrorType::NotAnObject,
            })?;

        // Get issuer ID from "id" field, or derive from filename
        let issuer_id = obj
            .get("id")
            .and_then(|v| v.as_str())
            .map(|s| s.to_string())
            .unwrap_or_else(|| {
                // Derive ID from filename (strip .json extension)
                filename
                    .strip_suffix(".json")
                    .or_else(|| filename.strip_suffix(".JSON"))
                    .unwrap_or(filename)
                    .to_string()
            });

        // Validate required fields
        let name = obj.get("name").and_then(|v| v.as_str()).ok_or_else(|| {
            PolicyStoreError::TrustedIssuerError {
                file: filename.to_string(),
                err: TrustedIssuerErrorType::MissingRequiredField {
                    issuer_id: issuer_id.clone(),
                    field: "name".to_string(),
                },
            }
        })?;

        let description = obj
            .get("description")
            .and_then(|v| v.as_str())
            .unwrap_or("");

        // RFC uses "configuration_endpoint"
        let oidc_endpoint_str = obj
            .get("configuration_endpoint")
            .and_then(|v| v.as_str())
            .ok_or_else(|| PolicyStoreError::TrustedIssuerError {
                file: filename.to_string(),
                err: TrustedIssuerErrorType::MissingRequiredField {
                    issuer_id: issuer_id.clone(),
                    field: "configuration_endpoint".to_string(),
                },
            })?;

        let oidc_endpoint =
            Url::parse(oidc_endpoint_str).map_err(|e| PolicyStoreError::TrustedIssuerError {
                file: filename.to_string(),
                err: TrustedIssuerErrorType::InvalidOidcEndpoint {
                    issuer_id: issuer_id.clone(),
                    url: oidc_endpoint_str.to_string(),
                    reason: e.to_string(),
                },
            })?;

        // Parse token_metadata (optional but recommended)
        let token_metadata = if let Some(metadata_json) = obj.get("token_metadata") {
            Self::parse_token_metadata(metadata_json, &issuer_id, filename)?
        } else {
            HashMap::new()
        };

        let issuer = TrustedIssuer {
            name: name.to_string(),
            description: description.to_string(),
            oidc_endpoint,
            token_metadata,
        };

        Ok(vec![ParsedIssuer {
            id: issuer_id,
            issuer,
            filename: filename.to_string(),
        }])
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
                .ok_or_else(|| PolicyStoreError::TrustedIssuerError {
                    file: filename.to_string(),
                    err: TrustedIssuerErrorType::TokenMetadataNotAnObject {
                        issuer_id: issuer_id.to_string(),
                    },
                })?;

        // Convert to owned map to avoid cloning during iteration
        let metadata_map: serde_json::Map<String, JsonValue> = metadata_obj.clone();
        let mut token_metadata = HashMap::with_capacity(metadata_map.len());

        for (token_type, token_config) in metadata_map {
            // Validate that token config is an object
            if !token_config.is_object() {
                return Err(PolicyStoreError::TrustedIssuerError {
                    file: filename.to_string(),
                    err: TrustedIssuerErrorType::TokenMetadataEntryNotAnObject {
                        issuer_id: issuer_id.to_string(),
                        token_type: token_type.clone(),
                    },
                });
            }

            // Deserialize the TokenEntityMetadata
            let metadata: TokenEntityMetadata =
                serde_json::from_value(token_config).map_err(|e| {
                    PolicyStoreError::TrustedIssuerError {
                        file: filename.to_string(),
                        err: TrustedIssuerErrorType::MissingRequiredField {
                            issuer_id: issuer_id.to_string(),
                            field: format!("token_metadata.{}: {}", token_type, e),
                        },
                    }
                })?;

            // Validate required field: entity_type_name
            if metadata.entity_type_name.is_empty() {
                return Err(PolicyStoreError::TrustedIssuerError {
                    file: filename.to_string(),
                    err: TrustedIssuerErrorType::MissingRequiredField {
                        issuer_id: issuer_id.to_string(),
                        field: format!("token_metadata.{}.entity_type_name", token_type),
                    },
                });
            }

            token_metadata.insert(token_type, metadata);
        }

        Ok(token_metadata)
    }

    /// Validate a collection of parsed issuers for conflicts and completeness.
    pub(super) fn validate_issuers(issuers: &[ParsedIssuer]) -> Result<(), Vec<String>> {
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

            // Token metadata is optional for JWKS-only configurations
            // It's only required when token_metadata entries specify entity_type_name or required_claims
            // for signed-token/trusted-issuer validation. Since we can't determine this requirement
            // when token_metadata is empty, we allow empty token_metadata to support JWKS-only use cases.
            // Validation of required fields within token_metadata entries is handled in parse_token_metadata.
        }

        if errors.is_empty() {
            Ok(())
        } else {
            Err(errors)
        }
    }

    /// Create a consolidated map of all issuers.
    pub(super) fn create_issuer_map(
        issuers: Vec<ParsedIssuer>,
    ) -> Result<HashMap<String, TrustedIssuer>, PolicyStoreError> {
        let mut issuer_map = HashMap::with_capacity(issuers.len());

        for parsed in issuers {
            // Check for duplicates (shouldn't happen if validate_issuers was called)
            // Note: This is a defensive check - duplicates should be caught earlier
            if let std::collections::hash_map::Entry::Vacant(e) =
                issuer_map.entry(parsed.id.clone())
            {
                e.insert(parsed.issuer);
            } else {
                // Skip duplicate silently since validate_issuers should have reported it
                continue;
            }
        }

        Ok(issuer_map)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_issuer_with_id() {
        let content = r#"{
            "id": "3af079fa58a915a4d37a668fb874b7a25b70a37c03cf",
            "name": "Test Issuer",
            "description": "A test OpenID Connect provider",
            "configuration_endpoint": "https://accounts.test.com/.well-known/openid-configuration"
        }"#;

        let result = IssuerParser::parse_issuer(content, "issuer1.json");
        assert!(result.is_ok(), "Should parse issuer with id");

        let parsed = result.unwrap();
        assert_eq!(parsed.len(), 1, "Should have 1 issuer");
        assert_eq!(parsed[0].id, "3af079fa58a915a4d37a668fb874b7a25b70a37c03cf");
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
    fn test_parse_issuer_without_id() {
        let content = r#"{
            "name": "Test Issuer",
            "description": "A test OpenID Connect provider",
            "configuration_endpoint": "https://accounts.test.com/.well-known/openid-configuration"
        }"#;

        let result = IssuerParser::parse_issuer(content, "test-issuer.json");
        assert!(result.is_ok(), "Should parse issuer without explicit id");

        let parsed = result.unwrap();
        assert_eq!(parsed.len(), 1, "Should have 1 issuer");
        assert_eq!(parsed[0].id, "test-issuer"); // Derived from filename
        assert_eq!(parsed[0].issuer.name, "Test Issuer");
    }

    #[test]
    fn test_parse_issuer_with_token_metadata() {
        let content = r#"{
            "id": "abd948a5665f6050d6e3ba440bd33ec0884234163aa3",
            "name": "Jans Server",
            "description": "Jans OpenID Connect Provider",
            "configuration_endpoint": "https://jans.test/.well-known/openid-configuration",
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
        }"#;

        let result = IssuerParser::parse_issuer(content, "jans.json");
        assert!(result.is_ok(), "Should parse issuer with token metadata");

        let parsed = result.unwrap();
        assert_eq!(parsed.len(), 1);
        assert_eq!(parsed[0].id, "abd948a5665f6050d6e3ba440bd33ec0884234163aa3");
        assert_eq!(parsed[0].issuer.token_metadata.len(), 2);

        let access_token = parsed[0].issuer.token_metadata.get("access_token").unwrap();
        assert_eq!(access_token.entity_type_name, "Jans::access_token");
        assert_eq!(access_token.user_id, Some("sub".to_string()));
    }

    #[test]
    fn test_parse_issuer_missing_name() {
        let content = r#"{
            "description": "Missing name field",
            "configuration_endpoint": "https://test.com/.well-known/openid-configuration"
        }"#;

        let result = IssuerParser::parse_issuer(content, "bad.json");
        let err = result.expect_err("Should fail on missing name");

        assert!(
            matches!(
                &err,
                PolicyStoreError::TrustedIssuerError {
                    file,
                    err: TrustedIssuerErrorType::MissingRequiredField { issuer_id, field }
                } if file == "bad.json" && issuer_id == "bad" && field == "name"
            ),
            "Expected MissingRequiredField error for name, got: {:?}",
            err
        );
    }

    #[test]
    fn test_parse_issuer_missing_endpoint() {
        let content = r#"{
            "name": "Test",
            "description": "Missing endpoint"
        }"#;

        let result = IssuerParser::parse_issuer(content, "bad.json");
        let err = result.expect_err("Should fail on missing endpoint");

        assert!(
            matches!(
                &err,
                PolicyStoreError::TrustedIssuerError {
                    file,
                    err: TrustedIssuerErrorType::MissingRequiredField { issuer_id, field }
                } if file == "bad.json" && issuer_id == "bad" && field == "configuration_endpoint"
            ),
            "Expected MissingRequiredField error for endpoint, got: {:?}",
            err
        );
    }

    #[test]
    fn test_parse_issuer_invalid_url() {
        let content = r#"{
            "name": "Test",
            "description": "Invalid URL",
            "configuration_endpoint": "not a valid url"
        }"#;

        let result = IssuerParser::parse_issuer(content, "bad.json");
        let err = result.expect_err("Should fail on invalid URL");

        assert!(
            matches!(
                &err,
                PolicyStoreError::TrustedIssuerError {
                    file,
                    err: TrustedIssuerErrorType::InvalidOidcEndpoint { issuer_id, url, .. }
                } if file == "bad.json" && issuer_id == "bad" && url == "not a valid url"
            ),
            "Expected InvalidOidcEndpoint error, got: {:?}",
            err
        );
    }

    #[test]
    fn test_parse_issuer_invalid_json() {
        let content = "{ invalid json }";

        let result = IssuerParser::parse_issuer(content, "invalid.json");
        let err = result.expect_err("Should fail on invalid JSON");

        assert!(
            matches!(&err, PolicyStoreError::JsonParsing { file, .. } if file == "invalid.json"),
            "Expected JsonParsing error, got: {:?}",
            err
        );
    }

    #[test]
    fn test_parse_token_metadata_missing_entity_type() {
        let content = r#"{
            "name": "Test",
            "description": "Test",
            "configuration_endpoint": "https://test.com/.well-known/openid-configuration",
            "token_metadata": {
                "access_token": {
                    "trusted": true
                }
            }
        }"#;

        let result = IssuerParser::parse_issuer(content, "bad.json");
        let err = result.expect_err("Should fail on missing entity_type_name in token metadata");
        assert!(
            matches!(&err, PolicyStoreError::TrustedIssuerError { .. }),
            "Expected TrustedIssuerError, got: {:?}",
            err
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
            },
        ];

        let result = IssuerParser::validate_issuers(&issuers);
        let errors = result.expect_err("Should detect duplicate issuer IDs");

        assert_eq!(errors.len(), 1, "Expected exactly one duplicate error");
        assert!(
            errors[0].contains("issuer1")
                && errors[0].contains("file1.json")
                && errors[0].contains("file2.json"),
            "Error should reference issuer1, file1.json and file2.json, got: {}",
            errors[0]
        );
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
        }];

        // Empty token_metadata is allowed for JWKS-only configurations
        let result = IssuerParser::validate_issuers(&issuers);
        result.expect("Should accept issuer with empty token_metadata for JWKS-only use case");
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
