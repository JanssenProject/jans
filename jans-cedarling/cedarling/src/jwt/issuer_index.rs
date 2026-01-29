use crate::common::{issuer_utils::IssClaim, policy_store::TrustedIssuer};

use super::IssuerConfig;
use ahash::{HashMap, HashMapExt};
use std::sync::{Arc, RwLock};

const MUTEX_POISONED_ERR: &str =
    "IssuerIndex RwLock poisoned due to another thread panicking while holding the lock";

/// The value of the `iss` claim from a JWT
///
/// An index mapping `iss` claims to their corresponding `IssuerConfig`s
/// This structure allows efficient lookup of issuer configurations
///
/// This structure is thread-safe for concurrent reads and writes
pub(super) struct IssuerIndex {
    index: RwLock<HashMap<IssClaim, IssuerConfig>>,
}

impl IssuerIndex {
    // Create a new, empty IssuerIndex
    pub(super) fn new() -> Self {
        Self {
            index: RwLock::new(HashMap::new()),
        }
    }

    // Insert or update an IssuerConfig for a given iss claim
    pub(super) fn insert(&self, iss: IssClaim, config: IssuerConfig) {
        let mut index = self.index.write().expect(MUTEX_POISONED_ERR);
        index.insert(iss, config);
    }

    /// Get the `TrustedIssuer` for a given iss claim, if it exists
    pub(super) fn get_trusted_issuer(&self, iss: &IssClaim) -> Option<Arc<TrustedIssuer>> {
        let index = self.index.read().expect(MUTEX_POISONED_ERR);
        index.get(iss).map(|config| config.policy.clone())
    }

    /// Find the token metadata key for a given entity type name
    /// e.g., "`Dolphin::Access_Token`" -> "`access_token`"
    pub(super) fn find_token_metadata_key<'a>(
        &'a self,
        entity_type_name: &'a str,
    ) -> Option<String> {
        // Look through all trusted issuers to find the matching entity type name

        // TODO: Optimize this lookup to have O(1) complexity, to have index structure
        let index = self.index.read().expect(MUTEX_POISONED_ERR);
        for issuer_config in index.values() {
            for (token_key, token_metadata) in &issuer_config.policy.token_metadata {
                if token_metadata.entity_type_name == entity_type_name {
                    return Some(token_key.to_string());
                }
            }
        }
        None
    }
}
