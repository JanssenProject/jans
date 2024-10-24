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

    /// Cedar schema in base64-encoded string format.
    ///
    /// Extracted from the `policy_store.json` file.
    pub cedar_schema: CedarSchema,

    /// Cedar policy set in base64-encoded string format.
    ///
    /// Extracted from the `policy_store.json` file and deserialized using `deserialize_policies`.
    #[serde(deserialize_with = "parse_cedar_policy")]
    pub cedar_policies: cedar_policy::PolicySet,

    #[allow(dead_code)]
    pub trusted_issuers: Option<Vec<TrustedIssuer>>,
}

#[derive(Debug, Clone, Deserialize)]
#[allow(dead_code)]
pub struct TrustedIssuer {
    pub name: String,
    pub description: String,
    pub openid_configuration_endpoint: String,
    pub token_metadata: Option<Vec<TokenMetadata>>,
}

#[derive(Debug, Clone, Deserialize)]
#[allow(dead_code)]
pub struct TokenMetadata {
    #[serde(rename = "type")]
    pub kind: TokenKind,
    pub person_id: String, // the claim used to create the person entity
    pub role_mapping: Option<String>, // the claim used to create a role for the token
}

#[derive(Debug, Copy, Clone)]
#[allow(dead_code)]
pub enum TokenKind {
    Access,
    Id,
    Userinfo,
    Transaction,
}

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

/// Used to to validate the cedar_version field.
///
/// Ensures the version follows the format `major.minor.patch`, where each part is a valid number.
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

/// Enum representing the various error messages that can occur while parsing policy sets.
///
/// This enum is used to provide detailed error information during the deserialization
/// of policy sets in the `PolicyStore`.
#[derive(Debug, thiserror::Error)]
enum ParsePolicySetMessage {
    /// Error indicating failure to decode policy content as base64.
    #[error("unable to decode policy_content as base64")]
    Base64,

    /// Error indicating failure to decode policy content to a UTF-8 string.
    #[error("unable to decode policy_content to utf8 string")]
    String,

    /// Error indicating failure to decode policy content from a human-readable format.
    #[error("unable to decode policy_content from human readable format")]
    HumanReadable,

    /// Error indicating failure to collect policies into a policy set.
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
            let policy = parse_policy::<D>(&id, policy_raw).map_err(|err| {
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
fn parse_policy<'de, D>(id: &str, policy_raw: RawPolicy) -> Result<cedar_policy::Policy, D::Error>
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
    use crate::common::policy_store::check_cedar_version;

    use super::ParsePolicySetMessage;
    use super::PolicyStore;

    #[test]
    fn test_policy_store_deserialization_success() {
        static POLICY_STORE_RAW: &str = include_str!("../../../test_files/policy-store_ok.json");
        assert!(serde_json::from_str::<PolicyStore>(POLICY_STORE_RAW).is_ok());
    }

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

    #[test]
    fn test_broken_policy_parsing_error_in_policy_store() {
        static POLICY_STORE_RAW: &str =
            include_str!("../../../test_files/policy-store_policy_err_broken_policy.json");

        let policy_result = serde_json::from_str::<PolicyStore>(POLICY_STORE_RAW);
        let err_msg = policy_result.unwrap_err().to_string();

        // TODO: this isn't really a human readable format so idk why the error message is
        // like this.
        assert_eq!(err_msg, "unable to decode policy with id: 840da5d85403f35ea76519ed1a18a33989f855bf1cf8, error: unable to decode policy_content from human readable format: unexpected token `)` at line 9 column 5")
    }

    #[test]
    fn test_valid_version() {
        let valid_version = "1.2.3".to_string();
        assert!(check_cedar_version(serde_json::Value::String(valid_version)).is_ok());
    }

    #[test]
    fn test_valid_version_with_v() {
        let valid_version_with_v = "v1.2.3".to_string();
        assert!(check_cedar_version(serde_json::Value::String(valid_version_with_v)).is_ok());
    }

    #[test]
    fn test_invalid_version_format() {
        let invalid_version = "1.2".to_string();
        assert!(check_cedar_version(serde_json::Value::String(invalid_version)).is_err());
    }

    #[test]
    fn test_invalid_version_part() {
        let invalid_version = "1.two.3".to_string();
        assert!(check_cedar_version(serde_json::Value::String(invalid_version)).is_err());
    }

    #[test]
    fn test_invalid_version_format_with_v() {
        let invalid_version_with_v = "v1.2".to_string();
        assert!(check_cedar_version(serde_json::Value::String(invalid_version_with_v)).is_err());
    }
}
