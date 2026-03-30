#[cfg(test)]
mod test;

use std::collections::{HashMap, HashSet};

use base64::prelude::*;
use cedar_policy::{Policy, PolicyId};
use cedar_policy_core::extensions::Extensions;
use cedar_policy_core::validator::ValidatorSchema;
use semver::Version;
use serde::de::{self, Error};
use serde::{Deserialize, Deserializer};
use url::Url;

use crate::common::cedar_schema::cedar_json::CedarSchemaJson;
use crate::common::default_entities::{
    DefaultEntitiesWithWarns, parse_default_entities_with_warns,
};

#[derive(Debug, Copy, Clone, PartialEq, Deserialize)]
enum Encoding {
    #[serde(rename = "base64")]
    Base64,
    #[serde(rename = "none")]
    None,
}

#[derive(Debug, Clone, Deserialize)]
enum ContentType {
    #[serde(rename = "cedar")]
    Cedar,
    #[serde(rename = "cedar-json")]
    CedarJson,
}

#[derive(Debug, Copy, Clone, PartialEq, Deserialize)]
enum PolicyContentType {
    #[serde(rename = "cedar")]
    Cedar,
}

#[derive(Debug, PartialEq, Clone, Deserialize)]
pub(crate) struct LegacyTokenEntityMetadata {
    #[serde(default = "default_trusted")]
    pub(crate) trusted: bool,
    pub(crate) entity_type_name: String,
    #[serde(default)]
    pub(crate) principal_mapping: HashSet<String>,
    #[serde(default = "default_token_id")]
    pub(crate) token_id: String,
    #[serde(default)]
    pub(crate) required_claims: HashSet<String>,
}

fn default_trusted() -> bool {
    true
}

fn default_token_id() -> String {
    "jti".to_string()
}

impl From<LegacyTokenEntityMetadata> for super::TokenEntityMetadata {
    fn from(v: LegacyTokenEntityMetadata) -> Self {
        super::TokenEntityMetadata::builder()
            .trusted(v.trusted)
            .entity_type_name(v.entity_type_name)
            .principal_mapping(v.principal_mapping)
            .token_id(v.token_id)
            .required_claims(v.required_claims)
            .build()
    }
}

#[derive(Debug, Clone, Deserialize, PartialEq)]
pub(crate) struct LegacyTrustedIssuer {
    pub(crate) name: String,
    pub(crate) description: String,
    #[serde(
        rename = "openid_configuration_endpoint",
        alias = "configuration_endpoint",
        deserialize_with = "de_oidc_endpoint_url"
    )]
    oidc_endpoint: Url,
    #[serde(default)]
    pub(crate) token_metadata: HashMap<String, LegacyTokenEntityMetadata>,
}

fn de_oidc_endpoint_url<'de, D>(deserializer: D) -> Result<Url, D::Error>
where
    D: Deserializer<'de>,
{
    let url_str = String::deserialize(deserializer)?;
    Url::parse(&url_str).map_err(|_| {
        de::Error::custom("the `\"openid_configuration_endpoint\"` is not a valid url")
    })
}

impl From<LegacyTrustedIssuer> for super::TrustedIssuer {
    fn from(v: LegacyTrustedIssuer) -> Self {
        super::TrustedIssuer::new(
            v.name,
            v.description,
            v.oidc_endpoint,
            v.token_metadata
                .into_iter()
                .map(|(k, v)| (k, v.into()))
                .collect(),
        )
    }
}

#[derive(Debug, Clone, Deserialize)]
struct CedarSchemaEncodedSchema {
    pub encoding: Encoding,
    pub content_type: ContentType,
    #[serde(deserialize_with = "trimmed_string")]
    pub body: String,
}

fn trimmed_string<'de, D>(deserializer: D) -> Result<String, D::Error>
where
    D: serde::Deserializer<'de>,
{
    let s = String::deserialize(deserializer)?;
    Ok(s.trim_end().to_string())
}

#[derive(Debug, Clone, Deserialize)]
#[serde(untagged)]
enum CedarSchemaMaybeEncoded {
    Plain(String),
    Tagged(CedarSchemaEncodedSchema),
}

#[derive(Debug, Clone)]
pub(crate) struct LegacyCedarSchema {
    pub schema: cedar_policy::Schema,
    pub json: CedarSchemaJson,
    pub validator_schema: ValidatorSchema,
}

#[cfg(test)]
impl PartialEq for LegacyCedarSchema {
    fn eq(&self, other: &Self) -> bool {
        self.json == other.json
    }
}

#[derive(Debug, thiserror::Error)]
enum ParseCedarSchemaSetMessage {
    #[error("unable to decode cedar policy schema base64")]
    Base64,
    #[error("unable to unmarshal cedar policy schema json to the structure")]
    CedarSchemaJsonFormat,
    #[error("unable to parse cedar policy schema")]
    Parse,
    #[error("invalid utf8 detected while decoding cedar policy")]
    Utf8,
    #[error("failed to parse cedar schema from JSON")]
    ParseCedarSchemaJson,
}

impl<'de> Deserialize<'de> for LegacyCedarSchema {
    fn deserialize<D: Deserializer<'de>>(deserializer: D) -> Result<Self, D::Error> {
        let encoded_schema = match CedarSchemaMaybeEncoded::deserialize(deserializer)? {
            CedarSchemaMaybeEncoded::Plain(body) => CedarSchemaEncodedSchema {
                encoding: Encoding::Base64,
                content_type: ContentType::CedarJson,
                body,
            },
            CedarSchemaMaybeEncoded::Tagged(encoded_schema) => encoded_schema,
        };

        let decoded_body = match encoded_schema.encoding {
            Encoding::None => encoded_schema.body,
            Encoding::Base64 => {
                let buf = BASE64_STANDARD.decode(encoded_schema.body).map_err(|err| {
                    de::Error::custom(format!("{}: {}", ParseCedarSchemaSetMessage::Base64, err))
                })?;
                String::from_utf8(buf).map_err(|err| {
                    de::Error::custom(format!("{}: {}", ParseCedarSchemaSetMessage::Utf8, err))
                })?
            },
        };
        let decoded_body = decoded_body.trim_end().to_string();

        let (schema_fragment, json_string) = match encoded_schema.content_type {
            ContentType::Cedar => {
                let (schema_fragment, _warning) =
                    cedar_policy::SchemaFragment::from_cedarschema_str(&decoded_body).map_err(
                        |err| {
                            de::Error::custom(format!(
                                "{}: {}",
                                ParseCedarSchemaSetMessage::Parse,
                                err
                            ))
                        },
                    )?;
                let json_string = schema_fragment.to_json_string().map_err(|err| {
                    de::Error::custom(format!(
                        "{}: {}",
                        ParseCedarSchemaSetMessage::CedarSchemaJsonFormat,
                        err
                    ))
                })?;
                (schema_fragment, json_string)
            },
            ContentType::CedarJson => {
                let schema_fragment = cedar_policy::SchemaFragment::from_json_str(&decoded_body)
                    .map_err(|err| {
                        de::Error::custom(format!(
                            "{}: {}",
                            ParseCedarSchemaSetMessage::CedarSchemaJsonFormat,
                            err
                        ))
                    })?;
                (schema_fragment, decoded_body)
            },
        };

        let fragment_iter = std::iter::once(schema_fragment);
        let schema = cedar_policy::Schema::from_schema_fragments(fragment_iter).map_err(|err| {
            de::Error::custom(format!("{}: {}", ParseCedarSchemaSetMessage::Parse, err))
        })?;

        let json = serde_json::from_str(&json_string).map_err(|err| {
            de::Error::custom(format!(
                "{}: {}",
                ParseCedarSchemaSetMessage::CedarSchemaJsonFormat,
                err
            ))
        })?;

        let validator_schema =
            ValidatorSchema::from_json_str(&json_string, Extensions::all_available()).map_err(
                |err| {
                    de::Error::custom(format!(
                        "{}: {}",
                        ParseCedarSchemaSetMessage::ParseCedarSchemaJson,
                        err
                    ))
                },
            )?;

        Ok(LegacyCedarSchema {
            schema,
            json,
            validator_schema,
        })
    }
}

impl From<LegacyCedarSchema> for crate::common::cedar_schema::CedarSchema {
    fn from(v: LegacyCedarSchema) -> Self {
        crate::common::cedar_schema::CedarSchema {
            schema: v.schema,
            json: v.json,
            validator_schema: v.validator_schema,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Deserialize)]
struct EncodedPolicy {
    pub encoding: Encoding,
    pub content_type: PolicyContentType,
    pub body: String,
}

#[derive(Debug, Clone, PartialEq, Deserialize)]
#[serde(untagged)]
enum PolicyMaybeEncoded {
    Plain(String),
    Tagged(EncodedPolicy),
}

#[derive(Debug, Clone, PartialEq, Deserialize)]
struct RawPolicy {
    pub policy_content: PolicyMaybeEncoded,
    pub description: String,
}

#[derive(Debug, thiserror::Error)]
enum ParsePolicySetMessage {
    #[error("unable to decode policy_content as base64")]
    Base64,
    #[error("unable to decode policy_content to utf8 string")]
    String,
    #[error("unable to decode policy_content from human readable format")]
    HumanReadable,
    #[error("could not collect policy store's to policy set")]
    CreatePolicySet,
}

#[derive(Debug, Clone)]
pub(crate) struct LegacyPoliciesContainer {
    raw_policy_info: HashMap<String, RawPolicy>,
    policy_set: cedar_policy::PolicySet,
}

#[cfg(test)]
impl PartialEq for LegacyPoliciesContainer {
    fn eq(&self, other: &Self) -> bool {
        use std::collections::BTreeMap;
        let self_policies: BTreeMap<_, _> = self
            .policy_set
            .policies()
            .map(|p| (p.id().clone(), p))
            .collect();
        let other_policies: BTreeMap<_, _> = other
            .policy_set
            .policies()
            .map(|p| (p.id().clone(), p))
            .collect();
        self_policies == other_policies
    }
}

impl<'de> Deserialize<'de> for LegacyPoliciesContainer {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: Deserializer<'de>,
    {
        let policies = HashMap::<String, RawPolicy>::deserialize(deserializer)?;

        let (policy_vec, errs): (Vec<_>, Vec<_>) = policies
            .iter()
            .map(|(id, policy_raw)| {
                parse_single_policy::<D>(id, policy_raw).map_err(|err| {
                    de::Error::custom(format!(
                        "unable to decode policy with id: {id}, error: {err}"
                    ))
                })
            })
            .partition_result();

        if !errs.is_empty() {
            let error_messages: Vec<D::Error> = errs.into_iter().collect();
            return Err(de::Error::custom(format!(
                "Errors encountered while parsing policies: {error_messages:?}"
            )));
        }

        let policy_set = cedar_policy::PolicySet::from_policies(policy_vec).map_err(|err| {
            de::Error::custom(format!("{}: {err}", ParsePolicySetMessage::CreatePolicySet))
        })?;

        Ok(LegacyPoliciesContainer {
            policy_set,
            raw_policy_info: policies,
        })
    }
}

fn parse_single_policy<'de, D>(id: &str, policy_raw: &RawPolicy) -> Result<Policy, D::Error>
where
    D: Deserializer<'de>,
{
    let policy_with_metadata = match &policy_raw.policy_content {
        PolicyMaybeEncoded::Plain(base64_encoded) => &EncodedPolicy {
            encoding: Encoding::Base64,
            content_type: PolicyContentType::Cedar,
            body: base64_encoded.to_owned(),
        },
        PolicyMaybeEncoded::Tagged(policy_with_metadata) => policy_with_metadata,
    };

    let decoded_body = match policy_with_metadata.encoding {
        Encoding::None => policy_with_metadata.body.clone(),
        Encoding::Base64 => {
            let buf = BASE64_STANDARD
                .decode(policy_with_metadata.body.as_str())
                .map_err(|err| {
                    de::Error::custom(format!("{}: {}", ParsePolicySetMessage::Base64, err))
                })?;
            String::from_utf8(buf).map_err(|err| {
                de::Error::custom(format!("{}: {}", ParsePolicySetMessage::String, err))
            })?
        },
    };

    match policy_with_metadata.content_type {
        PolicyContentType::Cedar => {
            cedar_policy::Policy::parse(Some(PolicyId::new(id)), decoded_body).map_err(|err| {
                de::Error::custom(format!("{}: {err}", ParsePolicySetMessage::HumanReadable))
            })
        },
    }
}

trait PartitionResult<T, E>: Iterator<Item = Result<T, E>> + Sized {
    fn partition_result(self) -> (Vec<T>, Vec<E>) {
        let mut ok = Vec::new();
        let mut errs = Vec::new();
        for r in self {
            match r {
                Ok(v) => ok.push(v),
                Err(e) => errs.push(e),
            }
        }
        (ok, errs)
    }
}

impl<T, E, I> PartitionResult<T, E> for I where I: Iterator<Item = Result<T, E>> {}

impl From<LegacyPoliciesContainer> for super::PoliciesContainer {
    fn from(v: LegacyPoliciesContainer) -> Self {
        let descriptions = v
            .raw_policy_info
            .into_iter()
            .map(|(id, raw)| (id, raw.description))
            .collect();
        super::PoliciesContainer::new(v.policy_set, descriptions)
    }
}

#[derive(Debug, Clone, Default, PartialEq)]
pub(crate) struct LegacyDefaultEntitiesWithWarns(DefaultEntitiesWithWarns);

impl<'de> Deserialize<'de> for LegacyDefaultEntitiesWithWarns {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: Deserializer<'de>,
    {
        let option_raw_data: Option<HashMap<String, serde_json::Value>> =
            Deserialize::deserialize(deserializer)
                .map_err(|err| D::Error::custom(format!("expect to be JSON object: {err}")))?;
        let inner = parse_default_entities_with_warns(option_raw_data).map_err(D::Error::custom)?;
        Ok(LegacyDefaultEntitiesWithWarns(inner))
    }
}

impl From<LegacyDefaultEntitiesWithWarns> for DefaultEntitiesWithWarns {
    fn from(v: LegacyDefaultEntitiesWithWarns) -> Self {
        v.0
    }
}

#[derive(Debug, Clone)]
#[cfg_attr(test, derive(PartialEq))]
pub(crate) struct LegacyPolicyStore {
    pub version: Option<String>,
    pub name: String,
    pub description: Option<String>,
    pub cedar_version: Option<Version>,
    pub schema: LegacyCedarSchema,
    pub policies: LegacyPoliciesContainer,
    pub trusted_issuers: Option<HashMap<String, LegacyTrustedIssuer>>,
    pub default_entities: LegacyDefaultEntitiesWithWarns,
}

impl<'de> Deserialize<'de> for LegacyPolicyStore {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: Deserializer<'de>,
    {
        let value = serde_json::Value::deserialize(deserializer)?;

        let obj = value
            .as_object()
            .ok_or_else(|| de::Error::custom("policy store entry must be a JSON object"))?;

        let name = obj.get("name").ok_or_else(|| {
            de::Error::custom("missing required field 'name' in policy store entry")
        })?;
        let name = name
            .as_str()
            .ok_or_else(|| de::Error::custom("'name' must be a string"))?;

        let schema = obj
            .get("schema")
            .or_else(|| obj.get("cedar_schema"))
            .ok_or_else(|| {
                de::Error::custom(
                    "missing required field 'schema' or 'cedar_schema' in policy store entry",
                )
            })?;

        let policies = obj
            .get("policies")
            .or_else(|| obj.get("cedar_policies"))
            .ok_or_else(|| {
                de::Error::custom(
                    "missing required field 'policies' or 'cedar_policies' in policy store entry",
                )
            })?;

        let store = LegacyPolicyStore {
            version: obj
                .get("version")
                .or_else(|| obj.get("policy_store_version"))
                .and_then(|v| v.as_str())
                .map(std::string::ToString::to_string),
            name: name.to_string(),
            description: obj
                .get("description")
                .and_then(|v| v.as_str())
                .map(std::string::ToString::to_string),
            cedar_version: obj
                .get("cedar_version")
                .map(parse_maybe_cedar_version)
                .transpose()
                .map_err(|e| de::Error::custom(format!("invalid cedar_version format: {e}")))?
                .flatten(),
            schema: LegacyCedarSchema::deserialize(schema)
                .map_err(|e| de::Error::custom(format!("error parsing schema: {e}")))?,
            policies: LegacyPoliciesContainer::deserialize(policies)
                .map_err(|e| de::Error::custom(format!("error parsing policies: {e}")))?,
            trusted_issuers: obj
                .get("trusted_issuers")
                .map(|v| {
                    HashMap::<String, LegacyTrustedIssuer>::deserialize(v).map_err(|e| {
                        de::Error::custom(format!("error parsing trusted issuers: {e}"))
                    })
                })
                .transpose()?,
            default_entities: obj
                .get("default_entities")
                .map(|v| {
                    LegacyDefaultEntitiesWithWarns::deserialize(v).map_err(|e| {
                        D::Error::custom(format!("could not deserialize `default entities`: {e}"))
                    })
                })
                .transpose()?
                .unwrap_or_default(),
        };

        Ok(store)
    }
}

impl From<LegacyPolicyStore> for super::PolicyStore {
    fn from(v: LegacyPolicyStore) -> Self {
        super::PolicyStore {
            version: v.version,
            name: v.name,
            description: v.description,
            cedar_version: v.cedar_version,
            schema: v.schema.into(),
            policies: v.policies.into(),
            trusted_issuers: v
                .trusted_issuers
                .map(|issuers| issuers.into_iter().map(|(k, v)| (k, v.into())).collect()),
            default_entities: v.default_entities.into(),
        }
    }
}

#[derive(Debug, Clone)]
#[cfg_attr(test, derive(PartialEq))]
pub(crate) struct LegacyAgamaPolicyStore {
    #[allow(dead_code)]
    pub cedar_version: Version,
    pub policy_stores: HashMap<String, LegacyPolicyStore>,
}

impl<'de> Deserialize<'de> for LegacyAgamaPolicyStore {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: Deserializer<'de>,
    {
        let value = serde_json::Value::deserialize(deserializer)?;

        let obj = value
            .as_object()
            .ok_or_else(|| de::Error::custom("policy store must be a JSON object"))?;

        let cedar_version = obj.get("cedar_version").ok_or_else(|| {
            de::Error::custom("missing required field 'cedar_version' in policy store")
        })?;

        let policy_stores = obj.get("policy_stores").ok_or_else(|| {
            de::Error::custom("missing required field 'policy_stores' in policy store")
        })?;

        let mut store = LegacyAgamaPolicyStore {
            cedar_version: parse_cedar_version(cedar_version)
                .map_err(|e| de::Error::custom(format!("invalid cedar_version format: {e}")))?,
            policy_stores: HashMap::new(),
        };

        let stores_obj = policy_stores
            .as_object()
            .ok_or_else(|| de::Error::custom("'policy_stores' must be a JSON object"))?;

        for (key, value) in stores_obj {
            let policy_store = LegacyPolicyStore::deserialize(value).map_err(|e| {
                de::Error::custom(format!("error parsing policy store '{key}': {e}"))
            })?;
            store.policy_stores.insert(key.clone(), policy_store);
        }

        Ok(store)
    }
}

pub(crate) fn parse_cedar_version(value: impl serde::Serialize) -> Result<Version, String> {
    let Ok(serde_json::Value::String(s)) = serde_json::to_value(&value) else {
        return Err("cedar_version must be a string".to_string());
    };
    let s = s.strip_prefix('v').unwrap_or(&s);
    Version::parse(s).map_err(|e| format!("error parsing cedar version :{e}"))
}

fn parse_maybe_cedar_version(value: &serde_json::Value) -> Result<Option<Version>, String> {
    match value {
        serde_json::Value::Null => Ok(None),
        serde_json::Value::String(s) => {
            let s = s.strip_prefix('v').unwrap_or(s);
            Version::parse(s)
                .map(Some)
                .map_err(|e| format!("error parsing cedar version :{e}"))
        },
        _ => Err("cedar_version must be a string or null".to_string()),
    }
}
