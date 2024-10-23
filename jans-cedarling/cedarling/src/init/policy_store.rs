/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use crate::bootstrap_config::policy_store_config::{PolicyStoreConfig, PolicyStoreSource};
use crate::common::policy_store::{PolicyStore, PolicyStoreMap, TokenKind};

/// Error cases for loading policy
#[derive(Debug, thiserror::Error)]
pub enum LoadPolicyStoreError {
    #[error("{0}")]
    Parse(#[from] serde_json::Error),
    #[error("store policy is empty")]
    PolicyEmpty,
    #[error("the `store_key` is not specified and the count on policies more than 1")]
    MoreThanOnePolicy,
    #[error("could not found policy by id: {0}")]
    FindPolicy(String),
    #[error("only one token should be assigned a `role_mapping`. The following tokens have a `role_mapping` field in your `policy_store.json`: {0}")]
    MultipleRoleMappings(String),
}

/// Load policy store from source
fn load_policy_store_map(
    source: &PolicyStoreSource,
) -> Result<PolicyStoreMap, LoadPolicyStoreError> {
    let policy_store_map: PolicyStoreMap = match source {
        PolicyStoreSource::Json(json_raw) => serde_json::from_str(json_raw.as_str())?,
    };
    Ok(policy_store_map)
}

/// Load policy store based on config
//
// Unit tests will be added when will be implemented other types of sources
pub(crate) fn load_policy_store(
    config: &PolicyStoreConfig,
) -> Result<PolicyStore, LoadPolicyStoreError> {
    let mut policy_store_map = load_policy_store_map(&config.source)?;

    let policy: PolicyStore = match (&config.store_id, policy_store_map.policy_stores.len()) {
        (Some(store_id), _) => policy_store_map
            .policy_stores
            .remove(store_id.as_str())
            .ok_or(LoadPolicyStoreError::FindPolicy(store_id.to_string()))?,
        (None, 0) => {
            return Err(LoadPolicyStoreError::PolicyEmpty);
        },
        (None, 1) => {
            // getting first element and we know it is save to use unwrap here,
            // because we know that there is only one element in the map
            policy_store_map
                .policy_stores
                .into_values()
                .next()
                .expect("In policy store map field policy_stores should be one element")
        },
        (None, 2..) => {
            return Err(LoadPolicyStoreError::MoreThanOnePolicy);
        },
    };

    // Check if there are any trusted issuers in the policy
    if let Some(trusted_issuers) = &policy.trusted_issuers {
        for issuer in trusted_issuers {
            if let Some(metadata) = &issuer.token_metadata {
                let tokens_with_role_mapping: Vec<TokenKind> = metadata
                    .iter()
                    .filter_map(|x| x.role_mapping.as_ref().map(|_| x.kind.clone()))
                    .collect();

                // If there are more than one token with `role_mapping`, return an error
                if tokens_with_role_mapping.len() > 1 {
                    let token_kinds: String = tokens_with_role_mapping
                        .iter()
                        .map(|token| token.to_string())
                        .collect::<Vec<_>>()
                        .join(", ");
                    return Err(LoadPolicyStoreError::MultipleRoleMappings(token_kinds));
                }
            }
        }
    }

    Ok(policy)
}

#[cfg(test)]
mod test {
    use super::{load_policy_store, LoadPolicyStoreError};
    use crate::PolicyStoreConfig;

    /// Tests the behavior of the `load_policy_store` function when multiple role mappings
    /// are defined in the policy store configuration.
    ///
    /// This test verifies that:
    /// 1. An error is returned when the policy store JSON contains multiple tokens with
    ///    a `role_mapping` field.
    /// 2. The specific error returned is of type `LoadPolicyStoreError::MultipleRoleMappings`
    ///    with the correct token kinds included in the error message.
    ///
    /// The test uses a sample JSON file (`policy-store_with_multiple_role_mappings_err.json`)
    /// that is expected to trigger this error scenario.
    #[test]
    fn errors_on_multiple_role_mappings() {
        static POLICY_STORE_RAW: &str =
            include_str!("../../../test_files/policy-store_with_multiple_role_mappings_err.json");
        let config = PolicyStoreConfig {
            source: crate::PolicyStoreSource::Json(POLICY_STORE_RAW.to_string()),
            store_id: None,
        };
        let result = load_policy_store(&config);

        assert!(matches!(
            result,
            Err(LoadPolicyStoreError::MultipleRoleMappings(
                ref token_kinds
            )) if token_kinds == "id, userinfo"
        ));
    }
}
