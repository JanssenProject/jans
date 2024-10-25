/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::cedar_schema::CedarSchema;
use base64::prelude::*;
use cedar_policy::PolicyId;
use serde::{de::Visitor, Deserialize, Deserializer};
use std::{collections::HashMap, fmt};

/// Represents the store of policies used for JWT validation and policy evaluation in Cedarling.
///
/// The `PolicyStore` contains the schema and a set of policies encoded in base64,
/// which are parsed during deserialization.
#[derive(Debug, Clone, serde::Deserialize)]
pub struct PolicyStore {
    /// The cedar version to use when parsing the schema and policies.
    #[serde(deserialize_with = "check_cedar_version")]
    #[allow(dead_code)]
    pub cedar_version: String,

    /// Cedar schema
    pub cedar_schema: CedarSchema, // currently being loaded from a base64-encoded string

    /// Cedar policy set
    #[serde(deserialize_with = "parse_cedar_policy")]
    pub cedar_policies: cedar_policy::PolicySet, // currently being loaded from a base64-encoded string

    /// An optional list of trusted issuers.
    ///
    /// This field may contain issuers that are trusted to provide tokens, allowing for additional
    /// verification and security when handling JWTs.
    #[allow(dead_code)]
    pub trusted_issuers: Option<Vec<TrustedIssuer>>,
}

/// Represents a trusted issuer that can provide JWTs.
///
/// This struct includes the issuer's name, description, and the OpenID configuration endpoint
/// for discovering issuer-related information.
#[derive(Debug, Clone, Deserialize)]
#[allow(dead_code)]
pub struct TrustedIssuer {
    /// The name of the trusted issuer.
    pub name: String,

    /// A brief description of the trusted issuer.
    pub description: String,

    /// The OpenID configuration endpoint for the issuer.
    ///
    /// This endpoint is used to obtain information about the issuer's capabilities.
    pub openid_configuration_endpoint: String,

    /// Optional metadata related to the tokens issued by this issuer.
    ///
    /// This field may include role mappings that help define the access levels for issued tokens.
    #[serde(deserialize_with = "check_token_metadata")]
    pub token_metadata: Option<Vec<TokenMetadata>>,
}

/// Validates the `token_metadata` field.
///
/// This function ensures that the metadata contains at most one `TokenMetadata` with a `role_mapping`
/// to prevent ambiguous role assignments.
fn check_token_metadata<'de, D>(deserializer: D) -> Result<Option<Vec<TokenMetadata>>, D::Error>
where
    D: Deserializer<'de>,
{
    let token_metadata: Option<Vec<TokenMetadata>> = Option::deserialize(deserializer)?;

    if let Some(metadata) = &token_metadata {
        let count = metadata
            .iter()
            .filter(|token| token.role_mapping.is_some())
            .count();

        if count > 1 {
            return Err(serde::de::Error::custom(
                "there can only be one TokenMetadata with a role_mapping".to_string(),
            ));
        }
    }

    Ok(token_metadata)
}

/// Represents metadata associated with a token.
///
/// This struct includes the type of token, the ID of the person associated with the token,
/// and an optional role mapping for access control.
#[derive(Debug, Clone, Deserialize)]
#[allow(dead_code)]
pub struct TokenMetadata {
    /// The type of token (e.g., Access, ID, Userinfo, Transaction).
    #[serde(rename = "type")]
    pub kind: TokenKind,

    /// The claim used to create the person entity associated with this token.
    pub person_id: String,

    /// An optional claim used to create a role for the token.
    pub role_mapping: Option<String>,
}

#[derive(Debug, Copy, Clone)]
#[allow(dead_code)]
pub enum TokenKind {
    /// Access token used for granting access to resources.
    Access,

    /// ID token used for authentication.
    Id,

    /// Userinfo token containing user-specific information.
    Userinfo,

    /// Transaction token used for tracking transactions.
    Transaction,
}

/// Enum representing the different kinds of tokens used by Cedarling.
impl fmt::Display for TokenKind {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        let kind_str = match self {
            TokenKind::Access => "access",
            TokenKind::Id => "id",
            TokenKind::Userinfo => "userinfo",
            TokenKind::Transaction => "transaction",
        };
        write!(f, "{}", kind_str)
    }
}

impl<'de> Deserialize<'de> for TokenKind {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        struct TokenKindVisitor;

        impl<'de> Visitor<'de> for TokenKindVisitor {
            type Value = TokenKind;

            fn expecting(&self, formatter: &mut fmt::Formatter) -> fmt::Result {
                formatter.write_str("a valid token kind string")
            }

            fn visit_str<E>(self, value: &str) -> Result<Self::Value, E>
            where
                E: serde::de::Error,
            {
                match value.to_lowercase().as_str() {
                    "id" => Ok(TokenKind::Id),
                    "userinfo" => Ok(TokenKind::Userinfo),
                    "access" => Ok(TokenKind::Access),
                    "transaction" => Ok(TokenKind::Transaction),
                    _ => Err(serde::de::Error::unknown_variant(
                        &value,
                        &[
                            "access_token",
                            "id_token",
                            "userinfo_token",
                            "transaction_token",
                        ],
                    )),
                }
            }
        }

        deserializer.deserialize_string(TokenKindVisitor)
    }
}

/// Validates the `cedar_version` field.
///
/// This function checks that the version string follows the format `major.minor.patch`,
/// where each component is a valid number. This also supports having a "v" prefix in the
/// version, e.g. `v1.0.1`.
fn check_cedar_version<'de, D>(deserializer: D) -> Result<String, D::Error>
where
    D: Deserializer<'de>,
{
    let version: String = String::deserialize(deserializer)?;

    // Check for "v" prefix
    let stripped_version = if version.starts_with('v') {
        &version[1..] // Remove the 'v' prefix
    } else {
        &version
    };

    // Split the version by '.'
    let parts: Vec<&str> = stripped_version.split('.').collect();

    // Check that we have exactly three parts (major, minor, patch)
    if parts.len() != 3 {
        return Err(serde::de::Error::custom(format!(
            "invalid version format: '{}', expected 'major.minor.patch'",
            version
        )));
    }

    // Check that each part is a valid number
    for part in &parts {
        if part.parse::<u32>().is_err() {
            return Err(serde::de::Error::custom(format!(
                "invalid version part: '{}', expected a number",
                part
            )));
        }
    }

    Ok(version)
}

/// Enum representing various error messages that can occur while parsing policy sets.
///
/// This enum is used to provide detailed error information during the deserialization
/// of policy sets within the `PolicyStore`.
#[derive(Debug, thiserror::Error)]
enum ParsePolicySetMessage {
    /// Indicates failure to decode policy content as base64.
    #[error("unable to decode policy_content as base64")]
    Base64,

    /// Indicates failure to decode policy content to a UTF-8 string.
    #[error("unable to decode policy_content to utf8 string")]
    String,

    /// Indicates failure to decode policy content from a human-readable format.
    #[error("unable to decode policy_content from human readable format")]
    HumanReadable,

    /// Indicates failure to collect policies into a policy set.
    #[error("could not collect policy store's to policy set")]
    CreatePolicySet,
}

/// Represents a raw policy entry from the `PolicyStore`.
///
/// This is a helper struct used internally for parsing base64-encoded policies.
#[derive(Debug, serde::Deserialize)]
struct RawPolicy {
    /// Base64-encoded content of the policy.
    pub policy_content: String,
}

/// Custom deserializer for converting base64-encoded policies into a `PolicySet`.
///
/// This function is used to deserialize the `policies` field in `PolicyStore`.
pub fn parse_cedar_policy<'de, D>(deserializer: D) -> Result<cedar_policy::PolicySet, D::Error>
where
    D: serde::Deserializer<'de>,
{
    let policies = <HashMap<String, RawPolicy> as serde::Deserialize>::deserialize(deserializer)?;

    let policy_vec = policies
        .into_iter()
        .map(|(id, policy_raw)| {
            let policy = parse_single_policy::<D>(&id, policy_raw).map_err(|err| {
                serde::de::Error::custom(format!(
                    "unable to decode policy with id: {id}, error: {err}"
                ))
            })?;
            Ok(policy)
        })
        .collect::<Result<Vec<cedar_policy::Policy>, _>>()?;

    cedar_policy::PolicySet::from_policies(policy_vec).map_err(|err| {
        serde::de::Error::custom(format!("{}: {err}", ParsePolicySetMessage::CreatePolicySet))
    })
}

/// Parses a single policy from its base64-encoded format.
///
/// This function is responsible for decoding the base64-encoded policy content,
/// converting it to a UTF-8 string, and parsing it into a `Policy`.
fn parse_single_policy<'de, D>(
    id: &str,
    policy_raw: RawPolicy,
) -> Result<cedar_policy::Policy, D::Error>
where
    D: serde::Deserializer<'de>,
{
    let decoded = BASE64_STANDARD
        .decode(policy_raw.policy_content.as_str())
        .map_err(|err| {
            serde::de::Error::custom(format!("{}: {err}", ParsePolicySetMessage::Base64))
        })?;
    let decoded_str = String::from_utf8(decoded).map_err(|err| {
        serde::de::Error::custom(format!("{}: {err}", ParsePolicySetMessage::String))
    })?;

    let policy =
        cedar_policy::Policy::parse(Some(PolicyId::new(id)), decoded_str).map_err(|err| {
            serde::de::Error::custom(format!("{}: {err}", ParsePolicySetMessage::HumanReadable))
        })?;

    Ok(policy)
}

#[cfg(test)]
mod test {
    use super::ParsePolicySetMessage;
    use super::PolicyStore;
    use crate::common::policy_store::check_cedar_version;
    use crate::common::policy_store::check_token_metadata;

    /// Tests successful deserialization of a valid policy store JSON.
    #[test]
    fn test_policy_store_deserialization_success() {
        static POLICY_STORE_RAW: &str = include_str!("../../../test_files/policy-store_ok.json");
        assert!(serde_json::from_str::<PolicyStore>(POLICY_STORE_RAW).is_ok());
    }

    /// Tests for base64 decoding error in the policy store.
    #[test]
    fn test_base64_decoding_error_in_policy_store() {
        static POLICY_STORE_RAW: &str =
            include_str!("../../../test_files/policy-store_policy_err_base64.json");

        let policy_result = serde_json::from_str::<PolicyStore>(POLICY_STORE_RAW);
        assert!(policy_result
            .unwrap_err()
            .to_string()
            .contains(&ParsePolicySetMessage::Base64.to_string()));
    }

    /// Tests for parsing error due to broken UTF-8 in the policy store.
    #[test]
    fn test_policy_parsing_error_in_policy_store() {
        static POLICY_STORE_RAW: &str =
            include_str!("../../../test_files/policy-store_policy_err_broken_utf8.json");

        let policy_result = serde_json::from_str::<PolicyStore>(POLICY_STORE_RAW);
        assert!(policy_result
            .unwrap_err()
            .to_string()
            .contains(&ParsePolicySetMessage::String.to_string()));
    }

    /// Tests for broken policy parsing error in the policy store.
    #[test]
    fn test_broken_policy_parsing_error_in_policy_store() {
        static POLICY_STORE_RAW: &str =
            include_str!("../../../test_files/policy-store_policy_err_broken_policy.json");

        let policy_result = serde_json::from_str::<PolicyStore>(POLICY_STORE_RAW);
        let err_msg = policy_result.unwrap_err().to_string();

        // TODO: this isn't really a human readable format so idk why the error message is
        // like this. Look into this in the future.
        assert_eq!(err_msg, "unable to decode policy with id: 840da5d85403f35ea76519ed1a18a33989f855bf1cf8, error: unable to decode policy_content from human readable format: unexpected token `)` at line 9 column 5")
    }

    /// Tests that a valid version string is accepted.
    #[test]
    fn test_valid_version() {
        let valid_version = "1.2.3".to_string();
        assert!(check_cedar_version(serde_json::Value::String(valid_version)).is_ok());
    }

    /// Tests that a valid version string with 'v' prefix is accepted.
    #[test]
    fn test_valid_version_with_v() {
        let valid_version_with_v = "v1.2.3".to_string();
        assert!(check_cedar_version(serde_json::Value::String(valid_version_with_v)).is_ok());
    }

    /// Tests that an invalid version format is rejected.
    #[test]
    fn test_invalid_version_format() {
        let invalid_version = "1.2".to_string();
        assert!(check_cedar_version(serde_json::Value::String(invalid_version)).is_err());
    }

    /// Tests that an invalid version part (non-numeric) is rejected.
    #[test]
    fn test_invalid_version_part() {
        let invalid_version = "1.two.3".to_string();
        assert!(check_cedar_version(serde_json::Value::String(invalid_version)).is_err());
    }

    /// Tests that an invalid version format with 'v' prefix is rejected.
    #[test]
    fn test_invalid_version_format_with_v() {
        let invalid_version_with_v = "v1.2".to_string();
        assert!(check_cedar_version(serde_json::Value::String(invalid_version_with_v)).is_err());
    }

    /// Tests that an error is returned for multiple role mappings in token metadata.
    #[test]
    fn test_invalid_multiple_role_mappings_in_token_metadata() {
        let invalid_token_metadata = r#"[ 
            { "type": "Access", "person_id": "aud" }, 
            { "type": "Id", "person_id": "sub", "role_mapping": "role" }, 
            { "type": "userinfo", "person_id": "email", "role_mapping": "role" } 
        ]"#;

        let token_metadata_value: serde_json::Value =
            serde_json::from_str(invalid_token_metadata).expect("Failed to parse JSON string");

        assert!(
            check_token_metadata(token_metadata_value).is_err(),
            "expected an error for multiple role mappings"
        );
    }

    /// Tests successful parsing of role mappings in token metadata.
    #[test]
    fn test_successful_parsing_of_role_mappings() {
        let valid_token_metadata = r#"[ 
            { "type": "Access", "person_id": "aud" }, 
            { "type": "Id", "person_id": "sub", "role_mapping": "role" }, 
            { "type": "userinfo", "person_id": "email" } 
        ]"#;

        let token_metadata_value: serde_json::Value =
            serde_json::from_str(valid_token_metadata).expect("Failed to parse JSON string");

        assert!(
            check_token_metadata(token_metadata_value).is_ok(),
            "expected successful parsing of role mappings"
        );
    }
}
