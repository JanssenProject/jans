// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

mod claim_mapping;
#[cfg(test)]
mod test;
mod token_entity_metadata;

use super::{PartitionResult, cedar_schema::CedarSchema};
use cedar_policy::{Policy, PolicyId};
use semver::Version;
use serde::{Deserialize, Deserializer, de};
use std::collections::HashMap;
use url::Url;

pub(crate) use claim_mapping::ClaimMappings;
pub use token_entity_metadata::TokenEntityMetadata;

/// Default maximum number of entities allowed
const DEFAULT_MAX_ENTITIES: usize = 1000;
/// Default maximum size of base64-encoded strings in bytes
const DEFAULT_MAX_BASE64_SIZE: usize = 1024 * 1024;

/// Configuration for limiting default entities to prevent DoS and memory exhaustion attacks
#[derive(Debug, Clone)]
pub struct DefaultEntitiesLimits {
    /// Maximum number of default entities allowed
    pub max_entities: usize,
    /// Maximum size of base64-encoded strings in bytes
    pub max_base64_size: usize,
}

impl Default for DefaultEntitiesLimits {
    fn default() -> Self {
        Self {
            max_entities: DEFAULT_MAX_ENTITIES,
            max_base64_size: DEFAULT_MAX_BASE64_SIZE,
        }
    }
}

/// Validates default entities against size and count limits
fn validate_default_entities(
    entities: &HashMap<String, serde_json::Value>,
    limits: &DefaultEntitiesLimits,
) -> Result<(), String> {
    // Check entity count limit
    if entities.len() > limits.max_entities {
        return Err(format!(
            "Maximum number of default entities ({}) exceeded, found {}",
            limits.max_entities,
            entities.len()
        ));
    }

    // Check base64 size limit for each entity
    for (entity_id, entity_data) in entities {
        if let Some(entity_str) = entity_data.as_str()
            && entity_str.len() > limits.max_base64_size {
                return Err(format!(
                    "Base64 string size ({}) for entity '{}' exceeds maximum allowed size ({})",
                    entity_str.len(),
                    entity_id,
                    limits.max_base64_size
                ));
            }
    }

    Ok(())
}

/// This is the top-level struct in compliance with the Agama Lab Policy Designer format.
#[derive(Debug, Clone, PartialEq)]
pub struct AgamaPolicyStore {
    /// The cedar version to use when parsing the schema and policies.
    pub cedar_version: Version,
    pub policy_stores: HashMap<String, PolicyStore>,
}

impl<'de> Deserialize<'de> for AgamaPolicyStore {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: Deserializer<'de>,
    {
        // First try to deserialize into a Value to get better error messages
        let value = serde_json::Value::deserialize(deserializer)?;
        
        // Check for required fields
        let obj = value.as_object().ok_or_else(|| {
            de::Error::custom("policy store must be a JSON object")
        })?;

        // Check cedar_version field
        let cedar_version = obj.get("cedar_version").ok_or_else(|| {
            de::Error::custom("missing required field 'cedar_version' in policy store")
        })?;

        // Check policy_stores field
        let policy_stores = obj.get("policy_stores").ok_or_else(|| {
            de::Error::custom("missing required field 'policy_stores' in policy store")
        })?;

        // Now deserialize the actual struct
        let mut store = AgamaPolicyStore {
            cedar_version: parse_cedar_version(cedar_version).map_err(|e| {
                de::Error::custom(format!("invalid cedar_version format: {}", e))
            })?,
            policy_stores: HashMap::new(),
        };

        // Deserialize policy stores
        let stores_obj = policy_stores.as_object().ok_or_else(|| {
            de::Error::custom("'policy_stores' must be a JSON object")
        })?;

        for (key, value) in stores_obj {
            let policy_store = PolicyStore::deserialize(value).map_err(|e| {
                de::Error::custom(format!(
                    "error parsing policy store '{}': {}",
                    key, e
                ))
            })?;
            store.policy_stores.insert(key.clone(), policy_store);
        }

        Ok(store)
    }
}

/// Represents the store of policies used for JWT validation and policy evaluation in Cedarling.
///
/// The `PolicyStore` contains the schema and a set of policies encoded in base64,
/// which are parsed during deserialization.
#[derive(Debug, Clone, PartialEq)]
pub struct PolicyStore {
    /// version of policy store
    //
    // alias to support Agama lab format
    pub version: Option<String>,

    /// Name is also name of namespace in `cedar-policy`
    pub name: String,

    /// Description comment to policy store
    pub description: Option<String>,

    /// The cedar version to use when parsing the schema and policies.
    pub cedar_version: Option<Version>,

    /// Cedar schema
    pub schema: CedarSchema,

    /// Cedar policy set
    pub policies: PoliciesContainer,

    /// An optional HashMap of trusted issuers.
    ///
    /// This field may contain issuers that are trusted to provide tokens, allowing for additional
    /// verification and security when handling JWTs.
    pub trusted_issuers: Option<HashMap<String, TrustedIssuer>>,

    /// Default entities for the policy store.
    ///
    /// This optional field can be used to specify default entities that should be included
    /// in the policy evaluation context.
    pub default_entities: Option<HashMap<String, serde_json::Value>>,
}

impl PolicyStore {
    pub(crate) fn get_store_version(&self) -> &str {
        self.version.as_deref().unwrap_or("undefined")
    }

    /// Apply configuration limits to default entities
    pub fn apply_default_entities_limits(
        &mut self,
        max_entities: Option<usize>,
        max_base64_size: Option<usize>,
    ) -> Result<(), String> {
        if let Some(ref default_entities) = self.default_entities {
            let limits = DefaultEntitiesLimits {
                max_entities: max_entities.unwrap_or(DEFAULT_MAX_ENTITIES),
                max_base64_size: max_base64_size.unwrap_or(DEFAULT_MAX_BASE64_SIZE),
            };
            
            validate_default_entities(default_entities, &limits)?;
        }
        
        Ok(())
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
    #[serde(
        rename = "openid_configuration_endpoint",
        deserialize_with = "de_oidc_endpoint_url"
    )]
    pub oidc_endpoint: Url,
    /// Metadata for tokens issued by the trusted issuer.
    #[serde(default)]
    pub token_metadata: HashMap<String, TokenEntityMetadata>,
}

fn de_oidc_endpoint_url<'de, D>(deserializer: D) -> Result<Url, D::Error>
where
    D: Deserializer<'de>,
{
    let url_str = String::deserialize(deserializer)?;
    let url = Url::parse(&url_str).map_err(|_| {
        de::Error::custom("the `\"openid_configuration_endpoint\"` is not a valid url")
    })?;
    Ok(url)
}

#[cfg(test)]
impl Default for TrustedIssuer {
    fn default() -> Self {
        Self {
            name: "Jans".to_string(),
            description: Default::default(),
            // This will only really be called during testing so we just put this test value
            oidc_endpoint: Url::parse("https://test.jans.org/.well-known/openid-configuration")
                .unwrap(),
            token_metadata: HashMap::from([
                ("access_token".into(), TokenEntityMetadata::access_token()),
                ("id_token".into(), TokenEntityMetadata::id_token()),
                (
                    "userinfo_token".into(),
                    TokenEntityMetadata::userinfo_token(),
                ),
            ]),
        }
    }
}

#[cfg(test)]
impl Default for &TrustedIssuer {
    fn default() -> Self {
        static DEFAULT: std::sync::LazyLock<TrustedIssuer> =
            std::sync::LazyLock::new(TrustedIssuer::default);
        &DEFAULT
    }
}

impl TrustedIssuer {
    /// Retrieves the claim that defines the `Role` for a given token type.
    pub fn get_role_mapping(&self, token_name: &str) -> Option<&str> {
        self.token_metadata
            .get(token_name)
            .and_then(|x| x.role_mapping.as_deref())
    }

    /// Retrieves the claim that defines the `User` for a given token type.
    pub fn get_user_mapping(&self, token_name: &str) -> Option<&str> {
        self.token_metadata
            .get(token_name)
            .and_then(|x| x.user_id.as_deref())
    }

    pub fn get_claim_mapping(&self, token_name: &str) -> Option<&ClaimMappings> {
        self.token_metadata
            .get(token_name)
            .map(|x| &x.claim_mapping)
    }

    pub fn get_token_metadata(&self, token_name: &str) -> Option<&TokenEntityMetadata> {
        self.token_metadata.get(token_name)
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

        let (policy_vec, errs): (Vec<_>, Vec<_>) = policies
            .iter()
            .map(|(id, policy_raw)| {
                let policy: Result<Policy, D::Error> = parse_single_policy::<D>(id, policy_raw)
                    .map_err(|err| {
                        de::Error::custom(format!(
                            "unable to decode policy with id: {id}, error: {err}"
                        ))
                    });
                policy
            })
            .partition_result();

        // Collect all errors into a single error message or return them as a vector.
        if !errs.is_empty() {
            let error_messages: Vec<D::Error> = errs.into_iter().collect();

            return Err(serde::de::Error::custom(format!(
                "Errors encountered while parsing policies: {:?}",
                error_messages
            )));
        }

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

/// Custom deserializer for PolicyStore that provides better error messages
impl<'de> Deserialize<'de> for PolicyStore {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: Deserializer<'de>,
    {
        // First try to deserialize into a Value to get better error messages
        let value = serde_json::Value::deserialize(deserializer)?;
        
        // Check for required fields
        let obj = value.as_object().ok_or_else(|| {
            de::Error::custom("policy store entry must be a JSON object")
        })?;

        // Check name field
        let name = obj.get("name").ok_or_else(|| {
            de::Error::custom("missing required field 'name' in policy store entry")
        })?;
        let name = name.as_str().ok_or_else(|| {
            de::Error::custom("'name' must be a string")
        })?;

        // Check schema field
        let schema = obj.get("schema").or_else(|| obj.get("cedar_schema")).ok_or_else(|| {
            de::Error::custom("missing required field 'schema' or 'cedar_schema' in policy store entry")
        })?;

        // Check policies field
        let policies = obj.get("policies").or_else(|| obj.get("cedar_policies")).ok_or_else(|| {
            de::Error::custom("missing required field 'policies' or 'cedar_policies' in policy store entry")
        })?;

        // Now deserialize the actual struct
        let store = PolicyStore {
            version: obj.get("version")
                .or_else(|| obj.get("policy_store_version"))
                .and_then(|v| v.as_str())
                .map(|s| s.to_string()),
            name: name.to_string(),
            description: obj.get("description")
                .and_then(|v| v.as_str())
                .map(|s| s.to_string()),
            cedar_version: obj.get("cedar_version")
                .map(parse_maybe_cedar_version)
                .transpose()
                .map_err(|e| de::Error::custom(format!("invalid cedar_version format: {}", e)))?
                .flatten(),
            schema: CedarSchema::deserialize(schema).map_err(|e| {
                de::Error::custom(format!("error parsing schema: {}", e))
            })?,
            policies: PoliciesContainer::deserialize(policies).map_err(|e| {
                de::Error::custom(format!("error parsing policies: {}", e))
            })?,
            trusted_issuers: obj.get("trusted_issuers")
                .map(|v| {
                    HashMap::<String, TrustedIssuer>::deserialize(v)
                        .map_err(|e| de::Error::custom(format!("error parsing trusted issuers: {}", e)))
                })
                .transpose()?,
            default_entities: obj
                .get("default_entities")
                .map(|v| {
                    // Expect an object mapping entity_id -> base64 string
                    let map = v.as_object().ok_or_else(|| {
                        de::Error::custom("'default_entities' must be a JSON object")
                    })?;

                    let mut decoded: HashMap<String, serde_json::Value> = HashMap::new();

                    for (entity_id, raw_value) in map.iter() {
                        let b64 = raw_value.as_str().ok_or_else(|| {
                            de::Error::custom(format!(
                                "error parsing default entities: entity '{}' must be a base64-encoded JSON string",
                                entity_id
                            ))
                        })?;

                        // Decode base64 string into UTF-8 JSON
                        use base64::prelude::*;
                        let buf = BASE64_STANDARD.decode(b64).map_err(|err| {
                            de::Error::custom(format!(
                                "error parsing default entities: failed to decode base64 for '{}': {}",
                                entity_id, err
                            ))
                        })?;

                        let json_str = String::from_utf8(buf).map_err(|err| {
                            de::Error::custom(format!(
                                "error parsing default entities: failed to decode utf8 for '{}': {}",
                                entity_id, err
                            ))
                        })?;

                        let value: serde_json::Value = serde_json::from_str(&json_str).map_err(|err| {
                            de::Error::custom(format!(
                                "error parsing default entities: invalid JSON for '{}': {}",
                                entity_id, err
                            ))
                        })?;

                        // Require an object for each entity
                        if !value.is_object() {
                            return Err(de::Error::custom(format!(
                                "error parsing default entities: entity '{}' must decode to a JSON object",
                                entity_id
                            )));
                        }

                        decoded.insert(entity_id.clone(), value);
                    }

                    // Validate against limits (using default limits for deserialization)
                    // Note: Configuration limits will be applied later when the policy store is initialized
                    let limits = DefaultEntitiesLimits::default();
                    validate_default_entities(&decoded, &limits).map_err(|e| {
                        de::Error::custom(format!("error validating default entities: {}", e))
                    })?;

                    Ok(decoded)
                })
                .transpose()?,
        };

        Ok(store)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    #[test]
    fn test_validate_default_entities_limits() {
        let limits = DefaultEntitiesLimits {
            max_entities: 2,
            max_base64_size: 100,
        };

        // Test valid entities
        let valid_entities = HashMap::from([
            ("entity1".to_string(), json!("dGVzdA=="),),
            ("entity2".to_string(), json!("dGVzdDI="),),
        ]);
        assert!(validate_default_entities(&valid_entities, &limits).is_ok());

        // Test entity count limit
        let too_many_entities = HashMap::from([
            ("entity1".to_string(), json!("dGVzdA=="),),
            ("entity2".to_string(), json!("dGVzdDI="),),
            ("entity3".to_string(), json!("dGVzdDM="),),
        ]);
        let result = validate_default_entities(&too_many_entities, &limits);
        assert!(result.is_err());
        assert!(result.unwrap_err().contains("Maximum number of default entities (2) exceeded"));

        // Test base64 size limit
        let large_base64 = "dGVzdA==".repeat(20); // Much larger than 100 bytes
        let large_entities = HashMap::from([
            ("entity1".to_string(), json!(large_base64),),
        ]);
        let result = validate_default_entities(&large_entities, &limits);
        assert!(result.is_err());
        assert!(result.unwrap_err().contains("Base64 string size"));
    }
}
