// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Trusted issuer configuration parsing and validation.
//!
//! This module provides functionality to parse and validate trusted issuer configuration files,
//! ensuring they conform to the required schema with proper token metadata and required fields.

use crate::common::policy_store::errors::{PolicyStoreError, TrustedIssuerErrorType};
use crate::common::policy_store::formats::file_id::id_from_filename;
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

/// One trusted issuer file body.
#[derive(Debug, Clone, PartialEq)]
pub(crate) struct TrustedIssuerDoc {
    pub(crate) name: String,
    pub(crate) description: String,
    pub(crate) oidc_endpoint: Url,
    pub(crate) token_metadata: HashMap<String, TokenMetadataDoc>,
}

/// A parsed trusted issuer configuration with metadata.
#[derive(Debug, Clone)]
pub(crate) struct ParsedIssuer {
    /// The issuer id; v2 identifies issuers by name, so it is not migrated.
    #[cfg_attr(not(test), expect(dead_code, reason = "v2 has no issuer id"))]
    pub(crate) id: String,
    /// The trusted issuer configuration
    pub(crate) issuer: TrustedIssuerDoc,
    /// Source filename
    pub(crate) filename: String,
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

        // Reachable from an explicit `"id": ""` and from a file named only
        // `.json`; an empty id would key the issuer map on "".
        if issuer_id.is_empty() {
            return Err(PolicyStoreError::TrustedIssuerError {
                file: filename.to_string(),
                err: TrustedIssuerErrorType::EmptyIssuerId,
            });
        }

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
}

#[cfg(test)]
mod tests {
    use super::*;

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

        let parsed = serde_saphyr::from_str::<TokenMetadataDoc>(
            "
            entity_type_name: Jans::Access_token
        ",
        )
        .expect("minimal YAML token metadata should parse");
        assert_eq!(parsed, expected, "minimal YAML should use v0 defaults");

        let parsed = serde_saphyr::from_str::<TokenMetadataDoc>(
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
    fn parse_rejects_id_derived_from_bare_extension() {
        let content = r#"{
            "name": "Test Issuer",
            "openid_configuration_endpoint": "https://test.com/.well-known/openid-configuration"
        }"#;
        let err = IssuerParser::parse_issuer(content, ".json")
            .expect_err("a file named only '.json' must be rejected");
        assert!(
            matches!(
                &err,
                PolicyStoreError::TrustedIssuerError {
                    file,
                    err: TrustedIssuerErrorType::EmptyIssuerId
                } if file == ".json"
            ),
            "expected EmptyIssuerId naming the file, got: {err:?}"
        );
    }

    #[test]
    fn parse_rejects_explicit_empty_id() {
        let content = r#"{
            "id": "",
            "name": "Test Issuer",
            "openid_configuration_endpoint": "https://test.com/.well-known/openid-configuration"
        }"#;
        let err = IssuerParser::parse_issuer(content, "issuer.json")
            .expect_err("an explicit empty id must be rejected");
        assert!(
            matches!(
                &err,
                PolicyStoreError::TrustedIssuerError {
                    file,
                    err: TrustedIssuerErrorType::EmptyIssuerId
                } if file == "issuer.json"
            ),
            "expected EmptyIssuerId naming the file, got: {err:?}"
        );
    }
}
