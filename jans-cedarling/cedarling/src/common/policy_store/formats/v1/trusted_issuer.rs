// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Trusted issuer configuration parsing and validation.
//!
//! This module provides functionality to parse and validate trusted issuer configuration files,
//! ensuring they conform to the required schema with proper token metadata and required fields.

use crate::common::policy_store::errors::{
    PolicyStoreError, TrustedIssuerErrorType, TrustedIssuerValidateError,
};
use crate::common::policy_store::formats::file_id::id_from_filename;
use crate::common::policy_store::{TokenEntityMetadata, TrustedIssuer};
use serde::Deserialize;
use serde_json::Value as JsonValue;
use std::collections::{HashMap, HashSet};
use url::Url;

/// A `token_metadata` entry. Unknown keys are ignored, as in v0.
#[derive(Debug, Clone, PartialEq, Deserialize)]
pub(crate) struct TokenMetadataDoc {
    #[serde(default = "default_trusted")]
    pub(crate) trusted: bool,
    pub(crate) entity_type_name: String,
    #[serde(default = "default_token_id")]
    pub(crate) token_id: String,
    #[serde(default)]
    pub(crate) required_claims: HashSet<String>,
}

fn default_trusted() -> bool {
    true
}

// Frozen v1 default; independent of the runtime `DEFAULT_TKN_ID`.
fn default_token_id() -> String {
    "jti".to_string()
}

impl From<TokenMetadataDoc> for TokenEntityMetadata {
    fn from(doc: TokenMetadataDoc) -> Self {
        TokenEntityMetadata::builder()
            .trusted(doc.trusted)
            .entity_type_name(doc.entity_type_name)
            .token_id(doc.token_id)
            .required_claims(doc.required_claims)
            .build()
    }
}

/// One trusted issuer file body.
#[derive(Debug, Clone, PartialEq)]
pub(crate) struct TrustedIssuerDoc {
    pub(crate) name: String,
    pub(crate) description: String,
    pub(crate) oidc_endpoint: Url,
    pub(crate) token_metadata: HashMap<String, TokenMetadataDoc>,
}

impl From<TrustedIssuerDoc> for TrustedIssuer {
    fn from(doc: TrustedIssuerDoc) -> Self {
        TrustedIssuer::new(
            doc.name,
            doc.description,
            doc.oidc_endpoint,
            doc.token_metadata
                .into_iter()
                .map(|(key, token)| (key, token.into()))
                .collect(),
        )
    }
}

/// A parsed trusted issuer configuration with metadata.
#[derive(Debug, Clone)]
pub(crate) struct ParsedIssuer {
    /// The issuer name (used as key/id)
    pub id: String,
    /// The trusted issuer configuration
    pub issuer: TrustedIssuerDoc,
    /// Source filename
    pub filename: String,
}

/// Issuer parser for loading and validating trusted issuer configurations.
pub(crate) struct IssuerParser;

impl IssuerParser {
    /// Parse a trusted issuer configuration from JSON content.
    ///
    /// Validates the required fields and token metadata structure.
    pub(crate) fn parse_issuer(
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
        let issuer_id = obj.get("id").and_then(|v| v.as_str()).map_or_else(
            || id_from_filename(filename).to_string(),
            std::string::ToString::to_string,
        );

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

        let oidc_endpoint_str = obj
            .get("openid_configuration_endpoint") // canonical and more readable key
            .or_else(|| obj.get("configuration_endpoint")) // key that was used in RFC
            .and_then(|v| v.as_str())
            .ok_or_else(|| PolicyStoreError::TrustedIssuerError {
                file: filename.to_string(),
                err: TrustedIssuerErrorType::MissingRequiredField {
                    issuer_id: issuer_id.clone(),
                    field: "openid_configuration_endpoint".to_string(),
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

        let issuer = TrustedIssuerDoc {
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
    ) -> Result<HashMap<String, TokenMetadataDoc>, PolicyStoreError> {
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

            let metadata: TokenMetadataDoc = serde_json::from_value(token_config).map_err(|e| {
                PolicyStoreError::TrustedIssuerError {
                    file: filename.to_string(),
                    err: TrustedIssuerErrorType::MissingRequiredField {
                        issuer_id: issuer_id.to_string(),
                        field: format!("token_metadata.{token_type}: {e}"),
                    },
                }
            })?;

            // Validate required field: entity_type_name
            if metadata.entity_type_name.is_empty() {
                return Err(PolicyStoreError::TrustedIssuerError {
                    file: filename.to_string(),
                    err: TrustedIssuerErrorType::MissingRequiredField {
                        issuer_id: issuer_id.to_string(),
                        field: format!("token_metadata.{token_type}.entity_type_name"),
                    },
                });
            }

            token_metadata.insert(token_type, metadata);
        }

        Ok(token_metadata)
    }

    /// Validate a collection of parsed issuers for conflicts and completeness.
    pub(crate) fn validate_issuers(
        issuers: &[ParsedIssuer],
    ) -> Result<(), Vec<TrustedIssuerValidateError>> {
        let mut errors = Vec::new();
        let mut seen_ids: HashMap<String, String> = HashMap::with_capacity(issuers.len());

        for parsed in issuers {
            // Check for duplicate issuer IDs (only insert if not duplicate)
            if let Some(existing_file) = seen_ids.get(&parsed.id) {
                errors.push(TrustedIssuerValidateError::DuplicateId {
                    id: parsed.id.clone(),
                    first_file: existing_file.clone(),
                    second_file: parsed.filename.clone(),
                });
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
    pub(crate) fn create_issuer_map(issuers: Vec<ParsedIssuer>) -> HashMap<String, TrustedIssuer> {
        let mut issuer_map = HashMap::with_capacity(issuers.len());

        for parsed in issuers {
            // Check for duplicates (shouldn't happen if validate_issuers was called)
            // Note: This is a defensive check - duplicates should be caught earlier
            if let std::collections::hash_map::Entry::Vacant(e) =
                issuer_map.entry(parsed.id.clone())
            {
                e.insert(parsed.issuer.into());
            }
        }

        issuer_map
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn token_doc(entity_type_name: &str) -> TokenMetadataDoc {
        TokenMetadataDoc {
            trusted: true,
            entity_type_name: entity_type_name.to_string(),
            token_id: "jti".to_string(),
            required_claims: HashSet::new(),
        }
    }

    /// Minimal `token_metadata` entries fall back to the v0 defaults, and
    /// unknown keys such as `user_id` / `role_mapping` are ignored.
    #[test]
    fn token_metadata_doc_from_json() {
        let expected = TokenMetadataDoc {
            trusted: true,
            entity_type_name: "Jans::Access_token".into(),
            token_id: "jti".into(),
            required_claims: HashSet::new(),
        };

        let parsed = serde_json::from_value::<TokenMetadataDoc>(serde_json::json!({
            "entity_type_name": "Jans::Access_token",
        }))
        .expect("minimal JSON token metadata should parse");
        assert_eq!(parsed, expected, "minimal JSON should use v0 defaults");

        let parsed = serde_json::from_value::<TokenMetadataDoc>(serde_json::json!({
            "entity_type_name": "Jans::Access_token",
            "user_id": "sub",
            "role_mapping": "",
        }))
        .expect("JSON token metadata with unknown keys should parse");
        assert_eq!(parsed, expected, "unknown JSON keys should be ignored");
    }

    #[test]
    fn token_metadata_doc_from_yaml() {
        let expected = TokenMetadataDoc {
            trusted: true,
            entity_type_name: "Jans::Access_token".into(),
            token_id: "jti".into(),
            required_claims: HashSet::new(),
        };

        let parsed = serde_yaml_ng::from_str::<TokenMetadataDoc>(
            "
            entity_type_name: Jans::Access_token
        ",
        )
        .expect("minimal YAML token metadata should parse");
        assert_eq!(parsed, expected, "minimal YAML should use v0 defaults");

        let parsed = serde_yaml_ng::from_str::<TokenMetadataDoc>(
            "
            user_id: 'sub'
            role_mapping: ''
            entity_type_name: Jans::Access_token
        ",
        )
        .expect("YAML token metadata with unknown keys should parse");
        assert_eq!(parsed, expected, "unknown YAML keys should be ignored");
    }

    #[test]
    fn token_metadata_doc_converts_to_runtime() {
        let doc = TokenMetadataDoc {
            trusted: false,
            entity_type_name: "Jans::Id_token".into(),
            token_id: "sid".into(),
            required_claims: HashSet::from(["iss".to_string()]),
        };

        assert_eq!(
            TokenEntityMetadata::from(doc),
            TokenEntityMetadata::builder()
                .trusted(false)
                .entity_type_name("Jans::Id_token".into())
                .token_id("sid".into())
                .required_claims(HashSet::from(["iss".to_string()]))
                .build(),
            "every TokenMetadataDoc field should map onto the runtime type"
        );
    }

    #[test]
    fn test_parse_issuer_with_id() {
        let content = r#"{
            "id": "3af079fa58a915a4d37a668fb874b7a25b70a37c03cf",
            "name": "Test Issuer",
            "description": "A test OpenID Connect provider",
            "configuration_endpoint": "https://accounts.test.com/.well-known/openid-configuration"
        }"#;

        let parsed = IssuerParser::parse_issuer(content, "issuer1.json")
            .expect("an issuer with an explicit id should parse");
        assert_eq!(parsed.len(), 1, "Should have 1 issuer");
        assert_eq!(
            parsed[0].id, "3af079fa58a915a4d37a668fb874b7a25b70a37c03cf",
            "the explicit id should win over the file name"
        );
        assert_eq!(parsed[0].issuer.name, "Test Issuer", "name mismatch");
        assert_eq!(
            parsed[0].issuer.description, "A test OpenID Connect provider",
            "description mismatch"
        );
        assert_eq!(
            parsed[0].issuer.oidc_endpoint.as_str(),
            "https://accounts.test.com/.well-known/openid-configuration",
            "oidc endpoint mismatch"
        );
    }

    #[test]
    fn test_parse_issuer_without_id() {
        let content = r#"{
            "name": "Test Issuer",
            "description": "A test OpenID Connect provider",
            "configuration_endpoint": "https://accounts.test.com/.well-known/openid-configuration"
        }"#;

        let parsed = IssuerParser::parse_issuer(content, "test-issuer.json")
            .expect("an issuer without an explicit id should parse");
        assert_eq!(parsed.len(), 1, "Should have 1 issuer");
        assert_eq!(
            parsed[0].id, "test-issuer",
            "the id should be derived from the file name"
        );
        assert_eq!(parsed[0].issuer.name, "Test Issuer", "name mismatch");
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
                    "entity_type_name": "Jans::access_token"
                },
                "id_token": {
                    "trusted": true,
                    "entity_type_name": "Jans::id_token"
                }
            }
        }"#;

        let parsed = IssuerParser::parse_issuer(content, "jans.json")
            .expect("an issuer with token metadata should parse");
        assert_eq!(parsed.len(), 1, "Should have 1 issuer");
        assert_eq!(
            parsed[0].id, "abd948a5665f6050d6e3ba440bd33ec0884234163aa3",
            "the explicit id should win over the file name"
        );
        assert_eq!(
            parsed[0].issuer.token_metadata.len(),
            2,
            "both token_metadata entries should be parsed"
        );

        let access_token = parsed[0]
            .issuer
            .token_metadata
            .get("access_token")
            .expect("access_token metadata should be present");
        assert_eq!(
            access_token.entity_type_name, "Jans::access_token",
            "entity_type_name mismatch"
        );
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
            "Expected MissingRequiredField error for name, got: {err:?}"
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
                } if file == "bad.json" && issuer_id == "bad" && field == "openid_configuration_endpoint"
            ),
            "Expected MissingRequiredField error for endpoint, got: {err:?}"
        );
    }

    #[test]
    fn test_parse_issuer_with_openid_configuration_endpoint() {
        let content = r#"{
            "name": "Test Issuer",
            "description": "Using canonical field name",
            "openid_configuration_endpoint": "https://accounts.test.com/.well-known/openid-configuration"
        }"#;

        let parsed = IssuerParser::parse_issuer(content, "issuer2.json")
            .expect("the canonical openid_configuration_endpoint key should parse");
        assert_eq!(
            parsed[0].issuer.oidc_endpoint.as_str(),
            "https://accounts.test.com/.well-known/openid-configuration",
            "oidc endpoint mismatch"
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
            "Expected InvalidOidcEndpoint error, got: {err:?}"
        );
    }

    #[test]
    fn test_parse_issuer_invalid_json() {
        let content = "{ invalid json }";

        let result = IssuerParser::parse_issuer(content, "invalid.json");
        let err = result.expect_err("Should fail on invalid JSON");

        assert!(
            matches!(&err, PolicyStoreError::JsonParsing { file, .. } if file == "invalid.json"),
            "Expected JsonParsing error, got: {err:?}"
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
            "Expected TrustedIssuerError, got: {err:?}"
        );
    }

    #[test]
    fn test_validate_issuers_no_duplicates() {
        let issuers = vec![
            ParsedIssuer {
                id: "issuer1".to_string(),
                issuer: TrustedIssuerDoc {
                    name: "Issuer 1".to_string(),
                    description: "First".to_string(),
                    oidc_endpoint: Url::parse(
                        "https://issuer1.com/.well-known/openid-configuration",
                    )
                    .expect("test oidc endpoint should be a valid url"),
                    token_metadata: HashMap::from([(
                        "access_token".to_string(),
                        token_doc("Jans::Access_token"),
                    )]),
                },
                filename: "file1.json".to_string(),
            },
            ParsedIssuer {
                id: "issuer2".to_string(),
                issuer: TrustedIssuerDoc {
                    name: "Issuer 2".to_string(),
                    description: "Second".to_string(),
                    oidc_endpoint: Url::parse(
                        "https://issuer2.com/.well-known/openid-configuration",
                    )
                    .expect("test oidc endpoint should be a valid url"),
                    token_metadata: HashMap::from([(
                        "id_token".to_string(),
                        token_doc("Jans::Id_token"),
                    )]),
                },
                filename: "file2.json".to_string(),
            },
        ];

        IssuerParser::validate_issuers(&issuers)
            .expect("distinct issuer ids should pass validation");
    }

    #[test]
    fn test_validate_issuers_duplicate_ids() {
        let issuers = vec![
            ParsedIssuer {
                id: "issuer1".to_string(),
                issuer: TrustedIssuerDoc {
                    name: "Issuer 1".to_string(),
                    description: "First".to_string(),
                    oidc_endpoint: Url::parse(
                        "https://issuer1.com/.well-known/openid-configuration",
                    )
                    .expect("test oidc endpoint should be a valid url"),
                    token_metadata: HashMap::from([(
                        "access_token".to_string(),
                        token_doc("Jans::Access_token"),
                    )]),
                },
                filename: "file1.json".to_string(),
            },
            ParsedIssuer {
                id: "issuer1".to_string(),
                issuer: TrustedIssuerDoc {
                    name: "Issuer 1 Duplicate".to_string(),
                    description: "Duplicate".to_string(),
                    oidc_endpoint: Url::parse(
                        "https://issuer1.com/.well-known/openid-configuration",
                    )
                    .expect("test oidc endpoint should be a valid url"),
                    token_metadata: HashMap::from([(
                        "id_token".to_string(),
                        token_doc("Jans::Id_token"),
                    )]),
                },
                filename: "file2.json".to_string(),
            },
        ];

        let result = IssuerParser::validate_issuers(&issuers);
        let errors = result.expect_err("Should detect duplicate issuer IDs");

        assert_eq!(errors.len(), 1, "Expected exactly one duplicate error");
        assert!(
            matches!(&errors[0], TrustedIssuerValidateError::DuplicateId { id, first_file, second_file }
                if id == "issuer1" && first_file == "file1.json" && second_file == "file2.json"),
            "expected DuplicateId naming the id and both files, got: {:?}",
            errors[0]
        );
    }

    #[test]
    fn test_validate_issuers_no_token_metadata() {
        let issuers = vec![ParsedIssuer {
            id: "issuer1".to_string(),
            issuer: TrustedIssuerDoc {
                name: "Issuer 1".to_string(),
                description: "No tokens".to_string(),
                oidc_endpoint: Url::parse("https://issuer1.com/.well-known/openid-configuration")
                    .expect("test oidc endpoint should be a valid url"),
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
                issuer: TrustedIssuerDoc {
                    name: "Issuer 1".to_string(),
                    description: "First".to_string(),
                    oidc_endpoint: Url::parse(
                        "https://issuer1.com/.well-known/openid-configuration",
                    )
                    .expect("test oidc endpoint should be a valid url"),
                    token_metadata: HashMap::from([(
                        "access_token".to_string(),
                        token_doc("Jans::Access_token"),
                    )]),
                },
                filename: "file1.json".to_string(),
            },
            ParsedIssuer {
                id: "issuer2".to_string(),
                issuer: TrustedIssuerDoc {
                    name: "Issuer 2".to_string(),
                    description: "Second".to_string(),
                    oidc_endpoint: Url::parse(
                        "https://issuer2.com/.well-known/openid-configuration",
                    )
                    .expect("test oidc endpoint should be a valid url"),
                    token_metadata: HashMap::from([(
                        "id_token".to_string(),
                        token_doc("Jans::Id_token"),
                    )]),
                },
                filename: "file2.json".to_string(),
            },
        ];

        let map = IssuerParser::create_issuer_map(issuers);

        assert_eq!(map.len(), 2, "both issuers should reach the map");
        assert!(
            map.contains_key("issuer1"),
            "issuer1 should be keyed by its id"
        );
        assert!(
            map.contains_key("issuer2"),
            "issuer2 should be keyed by its id"
        );
    }
}
