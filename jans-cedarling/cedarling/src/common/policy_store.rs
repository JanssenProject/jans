// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

mod claim_mapping;
#[cfg(test)]
mod test;
mod token_entity_metadata;

use std::collections::HashMap;
use std::fmt;
use std::sync::LazyLock;

use cedar_policy::PolicyId;
use semver::Version;
use serde::{Deserialize, Deserializer};
pub use token_entity_metadata::{ClaimMappings, TokenEntityMetadata};

use super::cedar_schema::CedarSchema;

/// This is the top-level struct in compliance with the Agama Lab Policy Designer format.
#[derive(Debug, Clone, serde::Deserialize, PartialEq)]
pub struct AgamaPolicyStore {
    /// The cedar version to use when parsing the schema and policies.
    #[serde(deserialize_with = "parse_cedar_version")]
    pub cedar_version: Version,

    pub policy_stores: HashMap<String, PolicyStore>,
}

/// Represents the store of policies used for JWT validation and policy evaluation in Cedarling.
///
/// The `PolicyStore` contains the schema and a set of policies encoded in base64,
/// which are parsed during deserialization.
#[derive(Debug, Clone, serde::Deserialize, PartialEq)]
pub struct PolicyStore {
    /// version of policy store
    pub version: Option<String>,

    /// Name is also name of namespace in `cedar-policy`
    pub name: String,

    /// Description comment to policy store
    #[serde(default)]
    pub description: Option<String>,

    /// The cedar version to use when parsing the schema and policies.
    #[serde(deserialize_with = "parse_maybe_cedar_version", default)]
    pub cedar_version: Option<Version>,

    /// Cedar schema
    #[serde(alias = "cedar_schema")]
    pub schema: CedarSchema,

    /// Cedar policy set
    #[serde(alias = "cedar_policies")]
    pub policies: PoliciesContainer,

    /// An optional HashMap of trusted issuers.
    ///
    /// This field may contain issuers that are trusted to provide tokens, allowing for additional
    /// verification and security when handling JWTs.
    pub trusted_issuers: Option<HashMap<String, TrustedIssuer>>,
}

impl PolicyStore {
    pub(crate) fn namespace(&self) -> &str {
        &self.name
    }

    pub(crate) fn get_store_version(&self) -> &str {
        self.version.as_deref().unwrap_or("undefined")
    }
}

/// Wrapper around [`PolicyStore`] to have access to it and ID of policy store
#[derive(Clone, derive_more::Deref)]
pub struct PolicyStoreWithID {
    /// ID of policy store
    pub id: String,
    /// Policy store value
    #[deref]
    pub store: PolicyStore,
}

/// Represents a trusted issuer that can provide JWTs.
///
/// This struct includes the issuer's name, description, and the OpenID configuration endpoint
/// for discovering issuer-related information.
#[derive(Debug, Clone, Deserialize, PartialEq)]
pub struct TrustedIssuer {
    /// The name of the trusted issuer.
    pub name: String,

    /// A brief description of the trusted issuer.
    pub description: String,

    /// The OpenID configuration endpoint for the issuer.
    ///
    /// This endpoint is used to obtain information about the issuer's capabilities.
    pub openid_configuration_endpoint: String,

    /// Metadata for access tokens issued by the trusted issuer.
    #[serde(default)]
    pub access_tokens: TokenEntityMetadata,

    /// Metadata for ID tokens issued by the trusted issuer.
    #[serde(default)]
    pub id_tokens: TokenEntityMetadata,

    /// Metadata for userinfo tokens issued by the trusted issuer.
    #[serde(default)]
    pub userinfo_tokens: TokenEntityMetadata,

    /// Metadata for transaction tokens issued by the trusted issuer.
    #[serde(default)]
    pub tx_tokens: TokenEntityMetadata,
}

impl Default for TrustedIssuer {
    fn default() -> Self {
        Self {
            name: "Jans".to_string(),
            description: Default::default(),
            openid_configuration_endpoint: Default::default(),
            access_tokens: Default::default(),
            id_tokens: Default::default(),
            userinfo_tokens: Default::default(),
            tx_tokens: Default::default(),
        }
    }
}

impl Default for &TrustedIssuer {
    fn default() -> Self {
        static DEFAULT: LazyLock<TrustedIssuer> = LazyLock::new(TrustedIssuer::default);
        &DEFAULT
    }
}

impl TrustedIssuer {
    /// Retrieves the claim that defines the `Role` for a given token type.
    pub fn role_mapping(&self, token_kind: TokenKind) -> Option<&str> {
        match token_kind {
            TokenKind::Access => self.access_tokens.role_mapping.as_deref(),
            TokenKind::Id => self.id_tokens.role_mapping.as_deref(),
            TokenKind::Userinfo => self.userinfo_tokens.role_mapping.as_deref(),
            TokenKind::Transaction => self.tx_tokens.role_mapping.as_deref(),
        }
    }

    /// Retrieves the claim that defines the `User` for a given token type.
    pub fn user_mapping(&self, token_kind: TokenKind) -> Option<&str> {
        match token_kind {
            TokenKind::Access => self.access_tokens.user_id.as_deref(),
            TokenKind::Id => self.id_tokens.user_id.as_deref(),
            TokenKind::Userinfo => self.userinfo_tokens.user_id.as_deref(),
            TokenKind::Transaction => self.tx_tokens.user_id.as_deref(),
        }
    }

    pub fn claim_mapping(&self, token_kind: TokenKind) -> &ClaimMappings {
        match token_kind {
            TokenKind::Access => &self.access_tokens.claim_mapping,
            TokenKind::Id => &self.id_tokens.claim_mapping,
            TokenKind::Userinfo => &self.userinfo_tokens.claim_mapping,
            TokenKind::Transaction => &self.tx_tokens.claim_mapping,
        }
    }

    pub fn token_metadata(&self, token_kind: TokenKind) -> &TokenEntityMetadata {
        match token_kind {
            TokenKind::Access => self.tokens_metadata().access_tokens,
            TokenKind::Id => self.tokens_metadata().id_tokens,
            TokenKind::Userinfo => self.tokens_metadata().userinfo_tokens,
            TokenKind::Transaction => self.tokens_metadata().tx_tokens,
        }
    }

    pub fn tokens_metadata(&self) -> TokensMetadata<'_> {
        TokensMetadata {
            access_tokens: &self.access_tokens,
            id_tokens: &self.id_tokens,
            userinfo_tokens: &self.userinfo_tokens,
            tx_tokens: &self.tx_tokens,
        }
    }
}

// Hold reference to tokens metadata
pub struct TokensMetadata<'a> {
    /// Metadata for access tokens issued by the trusted issuer.
    pub access_tokens: &'a TokenEntityMetadata,

    /// Metadata for ID tokens issued by the trusted issuer.
    pub id_tokens: &'a TokenEntityMetadata,
    /// Metadata for userinfo tokens issued by the trusted issuer.
    pub userinfo_tokens: &'a TokenEntityMetadata,

    /// Metadata for transaction tokens issued by the trusted issuer.
    pub tx_tokens: &'a TokenEntityMetadata,
}

#[derive(Debug, Copy, Clone, PartialEq, Eq)]
pub enum TokenKind {
    /// Access token used for granting access to resources.
    Access,

    /// ID token used for authentication.
    Id,

    /// Userinfo token containing user-specific information.
    Userinfo,

    /// Token containing transaction-specific information.
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
            _ => Err(serde::de::Error::unknown_variant(&token_kind, &[
                "access_token",
                "id_token",
                "userinfo_token",
            ])),
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

/// content_type for the policy_content field.
///
/// Only contains a single member, because as of 31-Oct-2024, cedar-policy 4.2.1
/// cedar_policy::Policy:from_json does not work with a single policy.
///
/// NOTE if/when cedar_policy::Policy:from_json gains this ability, this type
/// can be replaced by super::ContentType
#[derive(Debug, Copy, Clone, PartialEq, serde::Deserialize)]
enum PolicyContentType {
    /// indicates that the related value is in the cedar policy / schema language
    #[serde(rename = "cedar")]
    Cedar,
}

/// policy_content value which specifies both encoding and content_type
///
/// encoding is one of none or base64
/// content_type is one of cedar or cedar-json
#[derive(Debug, Clone, PartialEq, serde::Deserialize)]
struct EncodedPolicy {
    pub encoding: super::Encoding,
    pub content_type: PolicyContentType,
    pub body: String,
}

/// Intermediate struct to handler both kinds of policy_content values.
///
/// Either
///   "policy_content": "cGVybWl0KA..."
/// OR
///   "policy_content": { "encoding": "...", "content_type": "...", "body": "permit(...)"}
#[derive(Debug, Clone, PartialEq, serde::Deserialize)]
#[serde(untagged)]
enum MaybeEncoded {
    Plain(String),
    Tagged(EncodedPolicy),
}

/// Represents a raw policy entry from the `PolicyStore`.
///
/// This is a helper struct used internally for parsing base64-encoded policies.
#[derive(Debug, Clone, PartialEq, serde::Deserialize)]
struct RawPolicy {
    /// Base64-encoded content of the policy.
    pub policy_content: MaybeEncoded,

    /// Description of policy
    pub description: String,
}

/// Container to decode policy stores into container
///
/// Contain compiled [`cedar_policy::PolicySet`] and raw policy info to get description or other information.
#[derive(Debug, Clone, PartialEq)]
pub struct PoliciesContainer {
    /// HasMap to store raw policy info
    /// Is used to get policy description by ID
    // In HasMap ID is ID of policy
    raw_policy_info: HashMap<String, RawPolicy>,

    /// compiled `cedar_policy`` Policy set
    policy_set: cedar_policy::PolicySet,
}

impl PoliciesContainer {
    /// Get [`cedar_policy::PolicySet`]
    pub fn get_set(&self) -> &cedar_policy::PolicySet {
        &self.policy_set
    }

    /// Get policy description based on id of policy
    pub fn get_policy_description(&self, id: &str) -> Option<&str> {
        self.raw_policy_info.get(id).map(|v| v.description.as_str())
    }
}

/// Custom deserializer for converting base64-encoded policies into a `PolicySet`.
///
/// This function is used to deserialize the `policies` field in `PolicyStore`.
impl<'de> serde::Deserialize<'de> for PoliciesContainer {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: Deserializer<'de>,
    {
        let policies =
            <HashMap<String, RawPolicy> as serde::Deserialize>::deserialize(deserializer)?;

        let results: Vec<Result<cedar_policy::Policy, D::Error>> = policies
            .iter()
            .map(|(id, policy_raw)| {
                parse_single_policy::<D>(id, policy_raw).map_err(|err| {
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
            let error_messages: Vec<D::Error> =
                errors.into_iter().filter_map(Result::err).collect();

            return Err(serde::de::Error::custom(format!(
                "Errors encountered while parsing policies: {:?}",
                error_messages
            )));
        }

        let policy_vec = successful_policies
            .into_iter()
            .filter_map(Result::ok)
            .collect::<Vec<_>>();

        let policy_set = cedar_policy::PolicySet::from_policies(policy_vec).map_err(|err| {
            serde::de::Error::custom(format!("{}: {err}", ParsePolicySetMessage::CreatePolicySet))
        })?;

        Ok(PoliciesContainer {
            policy_set,
            raw_policy_info: policies,
        })
    }
}

/// Parses a single policy from its base64-encoded format.
///
/// This function is responsible for decoding the base64-encoded policy content,
/// converting it to a UTF-8 string, and parsing it into a `Policy`.
fn parse_single_policy<'de, D>(
    id: &str,
    policy_raw: &RawPolicy,
) -> Result<cedar_policy::Policy, D::Error>
where
    D: serde::Deserializer<'de>,
{
    let policy_with_metadata = match &policy_raw.policy_content {
        // It's a plain string, so assume its cedar inside base64
        MaybeEncoded::Plain(base64_encoded) => &EncodedPolicy {
            encoding: super::Encoding::Base64,
            content_type: PolicyContentType::Cedar,
            body: base64_encoded.to_owned(),
        },
        MaybeEncoded::Tagged(policy_with_metadata) => policy_with_metadata,
    };

    let decoded_body = match policy_with_metadata.encoding {
        super::Encoding::None => policy_with_metadata.body.to_string(),
        super::Encoding::Base64 => {
            use base64::prelude::*;
            let buf = BASE64_STANDARD
                .decode(policy_with_metadata.body.as_str())
                .map_err(|err| {
                    serde::de::Error::custom(format!("{}: {}", ParsePolicySetMessage::Base64, err))
                })?;

            String::from_utf8(buf).map_err(|err| {
                serde::de::Error::custom(format!("{}: {}", ParsePolicySetMessage::String, err))
            })?
        },
    };

    let policy = match policy_with_metadata.content_type {
        // see comments for PolicyContentType
        PolicyContentType::Cedar => {
            cedar_policy::Policy::parse(Some(PolicyId::new(id)), decoded_body).map_err(|err| {
                serde::de::Error::custom(format!("{}: {err}", ParsePolicySetMessage::HumanReadable))
            })?
        },
    };

    Ok(policy)
}

/// Custom parser for an Option<String> which returns `None` if the string is empty.
pub fn parse_option_string<'de, D>(deserializer: D) -> Result<Option<String>, D::Error>
where
    D: Deserializer<'de>,
{
    let value = Option::<String>::deserialize(deserializer)?;

    Ok(value.filter(|s| !s.is_empty()))
}
