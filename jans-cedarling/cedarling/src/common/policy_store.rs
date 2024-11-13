/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

mod claim_mapping;
mod json_store;
#[cfg(test)]
mod test;
mod token_entity_metadata;
mod trusted_issuer_metadata;
mod yaml_store;

use super::cedar_schema::CedarSchema;
use cedar_policy::{Policy, PolicySet};
use json_store::{LoadFromJsonError, PolicyStoreJson};
use semver::Version;
use serde::{Deserialize, Deserializer};
use std::{collections::HashMap, fmt};
pub use trusted_issuer_metadata::TrustedIssuerMetadata;
use yaml_store::{LoadFromYamlError, PolicyStoreYaml};

/// Represents errors that can occur when loading a policy store.
#[derive(Debug, thiserror::Error)]
pub enum LoadPolicyStoreError {
    /// Error when loading policy store from JSON.
    #[error("Failed to load policy store from JSON: {0}")]
    Json(#[from] LoadFromJsonError),
    /// Error when loading policy store from YAML.
    #[error("Failed to load policy store from YAML: {0}")]
    Yaml(#[from] LoadFromYamlError),
}

/// Represents the store of policies used for JWT validation and policy evaluation in Cedarling.
#[derive(Debug, PartialEq, Clone)]
#[allow(dead_code)]
pub struct PolicyStore {
    /// Optional name of the policy store.
    pub name: Option<String>,
    /// Optional description of the policy store.
    pub description: Option<String>,
    /// Optional version of Cedar being used.
    pub cedar_version: Option<Version>,
    /// A map of policy names to their content.
    pub policies: HashMap<String, PolicyContent>,
    /// The schema used for Cedar policies.
    pub cedar_schema: CedarSchema,
    /// A map of trusted issuers and their metadata.
    pub trusted_issuers: HashMap<String, TrustedIssuerMetadata>,
    /// The set of policies loaded in the store.
    policy_set: PolicySet,
}

impl PolicyStore {
    /// Loads a `PolicyStore` from a JSON string.
    pub fn load_from_json(json: &str) -> Result<Self, LoadPolicyStoreError> {
        let json_store = serde_json::from_str::<PolicyStoreJson>(json)
            .map_err(LoadFromJsonError::Deserialization)?;
        json_store.try_into().map_err(LoadPolicyStoreError::Json)
    }

    /// Loads a `PolicyStore` from a YAML string.
    pub fn load_from_yaml(yaml: &str) -> Result<Self, LoadPolicyStoreError> {
        let yaml_store = serde_yml::from_str::<PolicyStoreYaml>(yaml)
            .map_err(LoadFromYamlError::Deserialization)?;
        Ok(yaml_store.into())
    }

    /// Retrieves a reference to the policy set.
    pub fn policy_set(&self) -> &PolicySet {
        &self.policy_set
    }
}

/// Represents the content of a policy, including its description, creation date, and policy definition.
#[derive(Debug, PartialEq, Clone)]
pub struct PolicyContent {
    /// A description of the policy.
    pub description: String,
    /// The date the policy was created.
    pub creation_date: String,
    /// The actual content of the policy.
    pub policy_content: Policy,
}

/// Defines the source from which role mappings are retrieved.
pub struct RoleMapping<'a> {
    /// The type of token (e.g., Access, ID, Userinfo, Transaction).
    pub kind: TokenKind,
    /// The field used to extract the role mapping.
    pub role_mapping_field: &'a str,
}

/// Provides a default `RoleMapping` which looks for the role in the Userinfo token.
impl Default for RoleMapping<'_> {
    fn default() -> Self {
        Self {
            kind: TokenKind::Userinfo,
            role_mapping_field: "role",
        }
    }
}

impl TrustedIssuerMetadata {
    /// Retrieves the available `RoleMapping` from the token metadata.
    ///
    /// Checks each token metadata and returns the first one found with a `role_mapping` field.
    pub fn get_role_mapping(&self) -> Option<RoleMapping> {
        if let Some(role_mapping) = &self.access_tokens.entity_metadata.role_mapping {
            return Some(RoleMapping {
                kind: TokenKind::Access,
                role_mapping_field: role_mapping.as_str(),
            });
        }

        if let Some(role_mapping) = &self.id_tokens.role_mapping {
            return Some(RoleMapping {
                kind: TokenKind::Id,
                role_mapping_field: role_mapping.as_str(),
            });
        }

        if let Some(role_mapping) = &self.userinfo_tokens.role_mapping {
            return Some(RoleMapping {
                kind: TokenKind::Userinfo,
                role_mapping_field: role_mapping.as_str(),
            });
        }

        if let Some(role_mapping) = &self.tx_tokens.role_mapping {
            return Some(RoleMapping {
                kind: TokenKind::Transaction,
                role_mapping_field: role_mapping.as_str(),
            });
        }

        None
    }
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
            // return None for an empty string
            if version.is_empty() {
                return Ok(None);
            }

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

/// Custom parser for an Option<String> which returns `None` if the string is empty.
pub fn parse_option_string<'de, D>(deserializer: D) -> Result<Option<String>, D::Error>
where
    D: Deserializer<'de>,
{
    let value = Option::<String>::deserialize(deserializer)?;

    Ok(value.filter(|s| !s.is_empty()))
}

/// Custom parser for Option<HashMap<_, _>> which returns `None` if the HashMap is empty
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
