/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

mod claim_mapping;
pub mod policy_store_source;
#[cfg(test)]
mod test;
mod token_entity_metadata;
mod trusted_issuer_metadata;

use super::cedar_schema::CedarSchema;
pub use policy_store_source::LoadPolicyStoreError;
use semver::Version;
use serde::{Deserialize, Deserializer};
use std::{collections::HashMap, fmt};
use token_entity_metadata::TokenEntityMetadata;

/// Represents the store of policies used for JWT validation and policy evaluation in Cedarling.
///
/// The `PolicyStore` contains the schema and a set of policies encoded in base64,
/// which are parsed during deserialization.
#[derive(Debug, Clone, PartialEq)]
pub struct PolicyStore {
    pub name: Option<String>,
    pub description: Option<String>,
    /// The cedar version to use when parsing the schema and policies.
    #[allow(dead_code)]
    pub cedar_version: Option<Version>,
    /// Cedar schema
    pub cedar_schema: CedarSchema,
    /// Cedar policy set
    pub cedar_policies: cedar_policy::PolicySet,
    /// An optional list of trusted issuers.
    ///
    /// This field may contain issuers that are trusted to provide tokens, allowing for additional
    /// verification and security when handling JWTs.
    #[allow(dead_code)]
    pub trusted_issuers: Option<Vec<TrustedIssuer>>,
}

impl PolicyStore {
    pub fn load_from_json(json: &str) -> Result<Self, LoadPolicyStoreError> {
        let source = policy_store_source::PolicyStoreSource::load_from_json(json)?;
        Ok(source.into())
    }

    pub fn load_from_yaml(yaml: &str) -> Result<Self, LoadPolicyStoreError> {
        let source = policy_store_source::PolicyStoreSource::load_from_yaml(yaml)?;
        Ok(source.into())
    }
}

/// Represents a trusted issuer that can provide JWTs.
///
/// This struct includes the issuer's name, description, and the OpenID configuration endpoint
/// for discovering issuer-related information.
#[derive(Debug, Clone, Deserialize, PartialEq)]
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
    pub token_metadata: Option<HashMap<TokenKind, TokenEntityMetadata>>,
}

/// Structure define the source from which role mappings are retrieved.
pub struct RoleMapping<'a> {
    pub kind: TokenKind,
    pub role_mapping_field: &'a str,
}

// By default we will search role in the User token
impl Default for RoleMapping<'_> {
    fn default() -> Self {
        Self {
            kind: TokenKind::Userinfo,
            role_mapping_field: "role",
        }
    }
}

impl TrustedIssuer {
    /// Retrieves the available `RoleMapping` from the token metadata.
    //
    // in `token_metadata` list only one element with mapping
    // it is maximum 3 elements in list so iterating is efficient enouf
    pub fn get_role_mapping(&self) -> Option<RoleMapping> {
        if let Some(token_metadata) = &self.token_metadata {
            for (token_kind, metadata) in token_metadata {
                if let Some(role_mapping_field) = &metadata.role_mapping {
                    // TODO: why are we only returning the first one here?
                    return Some(RoleMapping {
                        kind: *token_kind,
                        role_mapping_field: &role_mapping_field,
                    });
                }
            }
        }

        None
    }
}

/// Represents metadata associated with a token.
///
/// This struct includes the type of token, the ID of the person associated with the token,
/// and an optional role mapping for access control.
#[derive(Debug, Clone, Deserialize, PartialEq)]
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

#[derive(Debug, Copy, Clone, PartialEq, Eq, Hash)]
#[allow(dead_code)]
pub enum TokenKind {
    /// Access token used for granting access to resources.
    Access,

    /// ID token used for authentication.
    Id,

    /// Userinfo token containing user-specific information.
    Userinfo,

    /// Transaction token containing transaction-specific information.
    Transaction,
}

/// Enum representing the different kinds of tokens used by Cedarling.
impl fmt::Display for TokenKind {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        let kind_str = match self {
            TokenKind::Access => "access",
            TokenKind::Id => "id",
            TokenKind::Userinfo => "userinfo",
            TokenKind::Transaction => "transacton",
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
            _ => Err(serde::de::Error::unknown_variant(
                &token_kind,
                &["access_token", "id_token", "userinfo_token"],
            )),
        }
    }
}

/// Parses the `cedar_version` field.
///
/// This function checks that the version string follows the format `major.minor.patch`,
/// where each component is a valid number. This also supports having a "v" prefix in the
/// version, e.g. `v1.0.1`.
#[allow(dead_code)]
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

/// Parses the optional `cedar_version` field.
///
/// This function checks that the version string follows the format `major.minor.patch`,
/// where each component is a valid number. This also supports having a "v" prefix in the
/// version, e.g. `v1.0.1`.
fn parse_maybe_cedar_version<'de, D>(deserializer: D) -> Result<Option<Version>, D::Error>
where
    D: Deserializer<'de>,
{
    let maybe_version: Option<String> = Option::<String>::deserialize(deserializer)?;

    match maybe_version {
        Some(version) => {
            // Check for "v" prefix
            let version = version.strip_prefix('v').unwrap_or(&version);

            let version = Version::parse(version).map_err(|e| {
                serde::de::Error::custom(format!("error parsing cedar version :{}", e))
            })?;

            Ok(Some(version))
        },
        None => Ok(None),
    }
}

/// content_type for the policy_content field.
///
/// Only contains a single member, because as of 31-Oct-2024, cedar-policy 4.2.1
/// cedar_policy::Policy:from_json does not work with a single policy.
///
/// NOTE if/when cedar_policy::Policy:from_json gains this ability, this type
/// can be replaced by super::ContentType
#[derive(Debug, Clone, serde::Deserialize)]
enum PolicyContentType {
    /// indicates that the related value is in the cedar policy / schema language
    #[serde(rename = "cedar")]
    Cedar,
}

/// Custom parser for an Option<String> which return None if the string is empty.
pub fn parse_option_string<'de, D>(deserializer: D) -> Result<Option<String>, D::Error>
where
    D: Deserializer<'de>,
{
    let value = Option::<String>::deserialize(deserializer)?;

    Ok(value.filter(|s| !s.is_empty()))
}

/// Custom parser for Option<HashMap<_, _>> which returns None if the HashMap is empty
pub fn parse_option_hashmap<'de, D, K, V>(
    deserializer: D,
) -> Result<Option<HashMap<K, V>>, D::Error>
where
    D: Deserializer<'de>,
    K: Eq + std::hash::Hash + Deserialize<'de>,
    V: Deserialize<'de>,
{
    let option = Option::<HashMap<K, V>>::deserialize(deserializer)?;

    match option {
        Some(ref hashmap) => match hashmap.is_empty() {
            true => Ok(None),
            false => Ok(option),
        },
        None => Ok(None),
    }
}
