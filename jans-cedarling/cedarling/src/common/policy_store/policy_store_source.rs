mod json;
mod yaml;

use super::{
    trusted_issuer_metadata::TrustedIssuerMetadata, CedarSchema, PolicyStore, TokenKind,
    TrustedIssuer,
};
use cedar_policy::{Policy, PolicySet};
use json::{LoadFromJsonError, PolicyStoreSourceJson};
use semver::Version;
use std::collections::HashMap;
use yaml::{LoadFromYamlError, PolicyStoreSourceYaml};

#[derive(Debug, thiserror::Error)]
pub enum LoadPolicyStoreError {
    #[error("Failed to load policy store from JSON: {0}")]
    Json(#[from] LoadFromJsonError),
    #[error("Failed to load policy store from YAML: {0}")]
    Yaml(#[from] LoadFromYamlError),
}

// Policy Stores from the Agama Policy Designer
#[derive(Debug, PartialEq)]
#[allow(dead_code)]
pub struct PolicyStoreSource {
    pub name: Option<String>,
    pub description: Option<String>,
    pub cedar_version: Option<Version>,
    pub policies: HashMap<String, PolicyContent>,
    pub cedar_schema: CedarSchema,
    pub trusted_issuers: HashMap<String, TrustedIssuerMetadata>,
}

impl PolicyStoreSource {
    pub fn load_from_json(json: &str) -> Result<Self, LoadPolicyStoreError> {
        let json_store = serde_json::from_str::<PolicyStoreSourceJson>(&json)
            .map_err(LoadFromJsonError::Deserialization)?;
        json_store.try_into().map_err(LoadPolicyStoreError::Json)
    }

    pub fn load_from_yaml(yaml: &str) -> Result<Self, LoadPolicyStoreError> {
        let yaml_store = serde_yml::from_str::<PolicyStoreSourceYaml>(&yaml)
            .map_err(LoadFromYamlError::Deserialization)?;
        Ok(yaml_store.into())
    }
}

// Policy Store from the Agama Policy Designer
#[derive(Debug, PartialEq)]
pub struct PolicyContent {
    pub description: String,
    pub creation_date: String,
    pub policy_content: Policy,
}

impl From<PolicyStoreSource> for PolicyStore {
    fn from(agama_store: PolicyStoreSource) -> Self {
        let mut policy_set = PolicySet::new();
        for (_id, policy) in agama_store.policies {
            policy_set
                .add(policy.policy_content)
                .expect("A non-template linked policy should be used");
        }

        let mut trusted_issuers = Vec::new();
        // we lose the issuer id in this operation so we probably
        // need to update the main policy store as well so wen can log that
        for (_iss_id, iss_metadata) in agama_store.trusted_issuers {
            let mut token_metadata = HashMap::new();
            token_metadata.insert(TokenKind::Access, iss_metadata.access_tokens);
            token_metadata.insert(TokenKind::Id, iss_metadata.id_tokens);
            token_metadata.insert(TokenKind::Userinfo, iss_metadata.userinfo_tokens);
            token_metadata.insert(TokenKind::Transaction, iss_metadata.tx_tokens);

            trusted_issuers.push(TrustedIssuer {
                name: iss_metadata.name,
                description: iss_metadata.description,
                openid_configuration_endpoint: iss_metadata.openid_configuration_endpoint,
                token_metadata: Some(token_metadata),
            });
        }

        PolicyStore {
            name: agama_store.name,
            description: agama_store.description,
            cedar_version: agama_store.cedar_version,
            cedar_schema: agama_store.cedar_schema,
            cedar_policies: policy_set,
            trusted_issuers: Some(trusted_issuers),
        }
    }
}

#[cfg(test)]
mod test {
    use super::PolicyStoreSource;

    #[test]
    fn can_load_from_json() {
        let policy_store_json = include_str!("./test_agama_policy_store.json");

        // we're already testing if the data gets load properly from the other tests
        // so we just check if this function does not throw an error when reading the
        // whole policy store
        assert!(PolicyStoreSource::load_from_json(&policy_store_json).is_ok());
    }

    #[test]
    fn can_load_from_yaml() {
        let policy_store_json = include_str!("./test_policy_store.yaml");

        // we're already testing if the data gets load properly from the other tests
        // so we just check if this function does not throw an error when reading the
        // whole policy store
        assert!(PolicyStoreSource::load_from_yaml(&policy_store_json).is_ok());
    }
}
