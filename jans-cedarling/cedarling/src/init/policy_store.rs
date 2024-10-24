/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use crate::bootstrap_config::policy_store_config::{PolicyStoreConfig, PolicyStoreSource};
use crate::common::policy_store::{PolicyStore, TokenKind};

/// Errors that can occur when loading a policy store.
#[derive(Debug, thiserror::Error)]
pub enum PolicyStoreLoadError {
    #[error("failed to parse the policy store from `policy_store.json`: {0}")]
    Parse(#[from] serde_json::Error),
    #[error("failed to fetch the policy store from the lock server")]
    FetchFromLockServer,
    #[error("only one token should be assigned a `role_mapping`. The following tokens have a `role_mapping` field in your `policy_store.json`: {0:?}")]
    MultipleRoleMappings(Vec<String>),
}

/// Loads the policy store based on the provided configuration.
///
/// This function supports multiple sources for loading policies.
pub(crate) fn load_policy_store(
    config: &PolicyStoreConfig,
) -> Result<PolicyStore, PolicyStoreLoadError> {
    // let mut policy_store_map = load_policy_store_map(&config.source)?;
    //
    let policy_store = match &config.source {
        PolicyStoreSource::Json(policy_json) => {
            load_policy_store_from_json(&policy_json).map_err(PolicyStoreLoadError::Parse)?
        },
        PolicyStoreSource::LockMaster(policy_store_id) => {
            load_policy_store_from_lock_master(&policy_store_id)?
        },
    };

    // Check if there are any trusted issuers in the policy
    if let Some(trusted_issuers) = &policy_store.trusted_issuers {
        for issuer in trusted_issuers {
            if let Some(metadata) = &issuer.token_metadata {
                let tokens_with_role_mapping: Vec<TokenKind> = metadata
                    .iter()
                    .filter_map(|x| x.role_mapping.as_ref().map(|_| x.kind))
                    .collect();

                // If there are more than one token with `role_mapping`, return an error
                if tokens_with_role_mapping.len() > 1 {
                    let token_kinds = tokens_with_role_mapping
                        .iter()
                        .map(|token| token.to_string())
                        .collect::<Vec<String>>();
                    return Err(PolicyStoreLoadError::MultipleRoleMappings(token_kinds));
                }
            }
        }
    }

    Ok(policy_store)
}

/// Loads the policy store from a JSON string.
fn load_policy_store_from_json(policies_json: &str) -> Result<PolicyStore, serde_json::Error> {
    let policy_store = serde_json::from_str::<PolicyStore>(policies_json)?;

    Ok(policy_store)
}

/// Loads the policy store from the Lock Master service.
fn load_policy_store_from_lock_master(
    _policy_store_id: &str,
) -> Result<PolicyStore, PolicyStoreLoadError> {
    todo!()
}

#[cfg(test)]
mod test {
    use super::{load_policy_store, PolicyStoreLoadError};
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
        };
        let result = load_policy_store(&config);

        assert!(matches!(
            result,
            Err(PolicyStoreLoadError::MultipleRoleMappings(
                ref token_kinds
            )) if token_kinds == &vec!["id", "userinfo"]
        ));
    }
}
