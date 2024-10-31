/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

#[cfg(test)]
mod test;

use super::cedar_schema::CedarSchema;
use base64::prelude::*;
use cedar_policy::PolicyId;
use semver::Version;
use serde::{Deserialize, Deserializer};
use std::{collections::HashMap, fmt};

/// Represents the store of policies used for JWT validation and policy evaluation in Cedarling.
///
/// The `PolicyStore` contains the schema and a set of policies encoded in base64,
/// which are parsed during deserialization.
#[derive(Debug, Clone, serde::Deserialize)]
pub struct PolicyStore {
    /// The cedar version to use when parsing the schema and policies.
    #[serde(deserialize_with = "parse_cedar_version")]
    #[allow(dead_code)]
    pub cedar_version: Version,

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
    #[serde(deserialize_with = "parse_and_check_token_metadata")]
    pub token_metadata: Option<Vec<TokenMetadata>>,
}

/// Parses and validates the `token_metadata` field.
///
/// This function ensures that the metadata contains at most one `TokenMetadata` with a `role_mapping`
/// to prevent ambiguous role assignments.
fn parse_and_check_token_metadata<'de, D>(
    deserializer: D,
) -> Result<Option<Vec<TokenMetadata>>, D::Error>
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

    /// The claim used to create the user entity associated with this token.
    pub user_id: String,

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
        let token_kind = String::deserialize(deserializer)?;

        match token_kind.to_lowercase().as_str() {
            "id_token" => Ok(TokenKind::Id),
            "userinfo_token" => Ok(TokenKind::Userinfo),
            "access_token" => Ok(TokenKind::Access),
            "transaction_token" => Ok(TokenKind::Transaction),
            _ => Err(serde::de::Error::unknown_variant(
                &token_kind,
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

/// Parses the `cedar_version` field.
///
/// This function checks that the version string follows the format `major.minor.patch`,
/// where each component is a valid number. This also supports having a "v" prefix in the
/// version, e.g. `v1.0.1`.
fn parse_cedar_version<'de, D>(deserializer: D) -> Result<Version, D::Error>
where
    D: Deserializer<'de>,
{
    let version: String = String::deserialize(deserializer)?;

    // Check for "v" prefix
    let version = version.strip_prefix('v').unwrap_or(&version);

    let version = Version::parse(version)
        .map_err(|e| serde::de::Error::custom(format!("error parsing cedar version :{}", e)))?;

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

    let results: Vec<Result<cedar_policy::Policy, D::Error>> = policies
        .into_iter()
        .map(|(id, policy_raw)| {
            parse_single_policy::<D>(&id, policy_raw).map_err(|err| {
                serde::de::Error::custom(format!(
                    "unable to decode policy with id: {id}, error: {err}"
                ))
            })
        })
        .collect();

    let (successful_policies, errors): (Vec<_>, Vec<_>) =
        results.into_iter().partition(Result::is_ok);

    // Collect all errors into a single error message or return them as a vector.
    if !errors.is_empty() {
        let error_messages: Vec<D::Error> = errors.into_iter().filter_map(Result::err).collect();

        return Err(serde::de::Error::custom(format!(
            "Errors encountered while parsing policies: {:?}",
            error_messages
        )));
    }

    let policy_vec = successful_policies
        .into_iter()
        .filter_map(Result::ok)
        .collect::<Vec<_>>();

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
