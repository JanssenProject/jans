// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

#[cfg(test)]
mod archive_security_tests;
pub(crate) mod log_entry;
#[cfg(test)]
pub(crate) mod test_utils;
mod token_entity_metadata;

use crate::common::{
    default_entities::DefaultEntitiesWithWarns,
    default_entities_limits::{DefaultEntitiesLimits, DefaultEntitiesLimitsError},
    issuer_utils::IssClaim,
};

pub(crate) mod archive_handler;
pub(crate) mod entity_parser;
pub(crate) mod errors;
pub(crate) mod issuer_parser;
pub(crate) mod legacy_store;
pub(crate) mod loader;
pub(crate) mod manager;
pub(crate) mod metadata;
pub(crate) mod policy_parser;
pub(crate) mod schema_parser;
pub(crate) mod validator;
pub(crate) mod vfs_adapter;

use super::cedar_schema::CedarSchema;
use semver::Version;
use std::collections::HashMap;
use url::Url;

pub(crate) use token_entity_metadata::TokenEntityMetadata;

// Re-export types used by init/policy_store.rs and external consumers
pub(crate) use manager::ConversionError;
pub(crate) use metadata::PolicyStoreMetadata;

/// Represents the store of policies used for JWT validation and policy evaluation in Cedarling.
///
/// The `PolicyStore` contains the schema and a set of policies encoded in base64,
/// which are parsed during deserialization.
#[derive(Debug, Clone)]
#[cfg_attr(test, derive(PartialEq))]
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

    /// An optional `HashMap` of trusted issuers.
    ///
    /// This field may contain issuers that are trusted to provide tokens, allowing for additional
    /// verification and security when handling JWTs.
    pub trusted_issuers: Option<HashMap<String, TrustedIssuer>>,

    /// Default entities for the policy store.
    pub default_entities: DefaultEntitiesWithWarns,
}

impl PolicyStore {
    pub(crate) fn get_store_version(&self) -> &str {
        self.version.as_deref().unwrap_or("undefined")
    }

    /// Apply configuration limits to default entities
    // TODO: add bootstrap configuration parameters and use it for check
    pub fn apply_default_entities_limits(
        &mut self,
        max_entities: Option<usize>,
        max_base64_size: Option<usize>,
    ) -> Result<(), DefaultEntitiesLimitsError> {
        let limits = DefaultEntitiesLimits {
            max_entities: max_entities.unwrap_or(DefaultEntitiesLimits::DEFAULT_MAX_ENTITIES),
            max_entity_size: max_base64_size
                .unwrap_or(DefaultEntitiesLimits::DEFAULT_MAX_ENTITY_SIZE),
        };
        limits.validate_default_entities(self.default_entities.entities())
    }

    pub(crate) fn validate_trusted_issuers(&self) -> Result<(), TrustedIssuersValidationError> {
        // check if iss already present in other policy store
        let mut oidc_to_trusted_issuer: HashMap<String, String> = HashMap::new();

        for (issuer_name, trusted_issuer) in self.trusted_issuers.iter().flatten() {
            let oidc_url = trusted_issuer.oidc_endpoint.to_string();
            if let Some(_previous_issuer_name) = oidc_to_trusted_issuer.get(&oidc_url) {
                return Err(TrustedIssuersValidationError {
                    oidc_url: format!(
                        "openid_configuration_endpoint: '{oidc_url}' is used for more than one issuer"
                    ),
                });
            }
            oidc_to_trusted_issuer.insert(oidc_url, issuer_name.to_owned());
        }

        Ok(())
    }
}

#[derive(Debug, derive_more::Display, derive_more::Error)]
#[display("openid_configuration_endpoint: '{oidc_url}' is used for more than one issuer")]
pub struct TrustedIssuersValidationError {
    oidc_url: String,
}

/// Wrapper around [`PolicyStore`] to have access to it and ID of policy store.
///
/// When loaded from the new directory/archive format, includes optional metadata
/// containing version, description, and other policy store information.
#[derive(Clone, derive_more::Deref)]
pub struct PolicyStoreWithID {
    /// ID of policy store
    pub(crate) id: String,
    /// Policy store value
    #[deref]
    pub(crate) store: PolicyStore,
    /// Optional metadata from new format policy stores.
    /// Contains `cedar_version`, `policy_store` info (name, version, description, etc.)
    pub(crate) metadata: Option<metadata::PolicyStoreMetadata>,
}

/// Represents a trusted issuer that can provide JWTs.
///
/// This struct includes the issuer's name, description, and the `OpenID` configuration endpoint
/// for discovering issuer-related information.
#[derive(Debug, Clone, PartialEq)]
pub struct TrustedIssuer {
    /// The name of the trusted issuer.
    /// Name also describe namespace in Cedar policy where entity `TrustedIssuer` is located.
    pub(crate) name: String,
    /// A brief description of the trusted issuer.
    pub(crate) description: String,
    /// The `OpenID` configuration endpoint for the issuer.
    ///
    /// This endpoint is used to obtain information about the issuer's capabilities.
    // Private to force usage of `iss_claim` method to get normalized iss claim
    oidc_endpoint: Url,
    /// Metadata for tokens issued by the trusted issuer.
    pub(crate) token_metadata: HashMap<String, TokenEntityMetadata>,
}

#[cfg(test)]
impl Default for TrustedIssuer {
    fn default() -> Self {
        Self {
            name: "Jans".to_string(),
            description: String::default(),
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
    pub(crate) fn new(
        name: String,
        description: String,
        oidc_endpoint: Url,
        metadata: HashMap<String, TokenEntityMetadata>,
    ) -> Self {
        Self {
            name,
            description,
            oidc_endpoint,
            token_metadata: metadata,
        }
    }

    #[cfg(test)]
    pub(crate) fn set_oidc_endpoint(&mut self, url: Url) {
        self.oidc_endpoint = url;
    }

    /// Get the OIDC endpoint URL.
    /// Should be used when we need to make requests to the OIDC endpoint.
    ///
    /// If you need comparison with `iss` claim, use `iss_claim` method instead.
    pub(crate) fn get_oidc_endpoint(&self) -> &Url {
        &self.oidc_endpoint
    }

    pub(crate) fn iss_claim(&self) -> IssClaim {
        IssClaim::new(&self.oidc_endpoint.origin().ascii_serialization())
    }
}


/// Container for compiled Cedar policies and their descriptions.
#[derive(Debug, Clone)]
pub struct PoliciesContainer {
    /// Policy descriptions by ID
    descriptions: HashMap<String, String>,
    /// Compiled `cedar_policy` Policy set
    policy_set: cedar_policy::PolicySet,
}

#[cfg(test)]
impl PartialEq for PoliciesContainer {
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

impl PoliciesContainer {
    /// Create a new `PoliciesContainer` from a policy set and description map.
    pub fn new(policy_set: cedar_policy::PolicySet, descriptions: HashMap<String, String>) -> Self {
        Self {
            descriptions,
            policy_set,
        }
    }

    /// Create an empty `PoliciesContainer` with the given policy set.
    pub fn new_empty(policy_set: cedar_policy::PolicySet) -> Self {
        Self {
            policy_set,
            descriptions: HashMap::new(),
        }
    }

    /// Get [`cedar_policy::PolicySet`]
    pub fn get_set(&self) -> &cedar_policy::PolicySet {
        &self.policy_set
    }

    /// Get policy description based on id of policy
    pub fn get_policy_description(&self, id: &str) -> Option<&str> {
        self.descriptions.get(id).map(String::as_str)
    }
}
