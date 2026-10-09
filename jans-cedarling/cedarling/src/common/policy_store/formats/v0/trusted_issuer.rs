// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Trusted issuer file parsing for format v0.
//!
//! Parses each `trusted-issuers/*.json` file into v0 structs. Cross-file checks
//! and conversion into runtime types happen after migration to the current version.

use crate::common::policy_store::errors::{PolicyStoreError, TrustedIssuerErrorType};
use crate::common::policy_store::formats::file_id::id_from_filename;
use serde::Deserialize;
use serde_json::Value as JsonValue;
use std::collections::{HashMap, HashSet};
use url::Url;

/// A `token_metadata` entry. Unknown keys are ignored.
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

// Frozen v0 default; independent of the runtime `DEFAULT_TKN_ID`.
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
    /// The issuer name (used as key/id)
    pub id: String,
    /// The trusted issuer configuration
    pub issuer: TrustedIssuerDoc,
    /// Source filename
    pub filename: String,
}

/// Parser for v0 trusted issuer files.
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
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_issuer_with_token_metadata() {
        let content = r#"{
            "name": "Jans Server",
            "description": "Jans OpenID Connect Provider",
            "configuration_endpoint": "https://jans.test/.well-known/openid-configuration",
            "token_metadata": {
                "access_token": { "entity_type_name": "Jans::Access_token", "user_id": "sub" }
            }
        }"#;

        let parsed =
            IssuerParser::parse_issuer(content, "jans.json").expect("v0 issuer should parse");
        assert_eq!(parsed.len(), 1, "one file should yield one issuer");
        assert_eq!(
            parsed[0].id, "jans",
            "id should be derived from the file name"
        );
        assert_eq!(parsed[0].issuer.name, "Jans Server", "name mismatch");
        assert_eq!(
            parsed[0].issuer.oidc_endpoint.as_str(),
            "https://jans.test/.well-known/openid-configuration",
            "the RFC `configuration_endpoint` key should be accepted"
        );
        assert_eq!(
            parsed[0].issuer.token_metadata["access_token"],
            TokenMetadataDoc {
                trusted: true,
                entity_type_name: "Jans::Access_token".to_string(),
                token_id: "jti".to_string(),
                required_claims: HashSet::new(),
            },
            "token metadata should use v0 defaults and ignore unknown keys"
        );
    }

    #[test]
    fn token_metadata_doc_from_yaml() {
        let parsed = serde_yaml_ng::from_str::<TokenMetadataDoc>(
            "
            user_id: 'sub'
            role_mapping: ''
            entity_type_name: Jans::Access_token
        ",
        )
        .expect("YAML token metadata with unknown keys should parse");
        assert_eq!(
            parsed.entity_type_name, "Jans::Access_token",
            "entity_type_name should be read from YAML"
        );
    }

    #[test]
    fn missing_name_is_rejected() {
        let content = r#"{
            "configuration_endpoint": "https://test.com/.well-known/openid-configuration"
        }"#;

        let err = IssuerParser::parse_issuer(content, "bad.json")
            .expect_err("an issuer without a name must be rejected");
        assert!(
            matches!(
                &err,
                PolicyStoreError::TrustedIssuerError {
                    err: TrustedIssuerErrorType::MissingRequiredField { field, .. },
                    ..
                } if field == "name"
            ),
            "Expected MissingRequiredField for name, got: {err:?}"
        );
    }

    #[test]
    fn missing_entity_type_name_is_rejected() {
        let content = r#"{
            "name": "Test",
            "openid_configuration_endpoint": "https://test.com/.well-known/openid-configuration",
            "token_metadata": { "access_token": { "trusted": true } }
        }"#;

        let err = IssuerParser::parse_issuer(content, "bad.json")
            .expect_err("token metadata without entity_type_name must be rejected");
        assert!(
            matches!(&err, PolicyStoreError::TrustedIssuerError { .. }),
            "Expected TrustedIssuerError, got: {err:?}"
        );
    }
}
