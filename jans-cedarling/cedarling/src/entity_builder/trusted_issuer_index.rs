// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashMap;
use std::sync::Arc;

#[cfg(test)]
use crate::common::issuer_utils::IssClaim;
use crate::common::policy_store::TrustedIssuer;

/// The trusted issuers the entity builder builds `TrustedIssuer` entities for.
///
/// Holds no lookup by `iss`: a validated token already carries the issuer its
/// mapping names, and several issuers may share one `iss`.
#[derive(Debug, Clone)]
pub(crate) struct TrustedIssuerIndex {
    issuers: Vec<Arc<TrustedIssuer>>,
}

impl TrustedIssuerIndex {
    pub(crate) fn new(issuers: &HashMap<String, TrustedIssuer>) -> Self {
        let mut sorted: Vec<(&String, &TrustedIssuer)> = issuers.iter().collect();
        sorted.sort_by(|a, b| a.0.cmp(b.0));
        Self {
            issuers: sorted
                .into_iter()
                .map(|(_, issuer)| Arc::new(issuer.clone()))
                .collect(),
        }
    }

    /// Finds the first trusted issuer with the given `iss`.
    #[cfg(test)]
    pub(super) fn find(&self, iss: &IssClaim) -> Option<&Arc<TrustedIssuer>> {
        self.issuers
            .iter()
            .find(|issuer| issuer.iss_claim() == *iss)
    }

    /// Returns an iterator over references to all trusted issuers.
    pub(super) fn values(&self) -> impl Iterator<Item = &TrustedIssuer> + '_ {
        self.issuers.iter().map(AsRef::as_ref)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use url::Url;

    #[test]
    fn values_lists_every_issuer_once() {
        let issuer = |name: &str, iss: &str| {
            TrustedIssuer::discovery_at(
                name.to_string(),
                String::new(),
                Url::parse(iss).expect("valid url"),
                HashMap::new(),
            )
        };
        // Same origin, different paths: both must be kept.
        let issuers = HashMap::from([
            (
                "a".to_string(),
                issuer("RealmA", "https://kc.example/realms/a"),
            ),
            (
                "b".to_string(),
                issuer("RealmB", "https://kc.example/realms/b"),
            ),
        ]);

        let index = TrustedIssuerIndex::new(&issuers);
        assert_eq!(
            index.values().map(|i| i.name.as_str()).collect::<Vec<_>>(),
            vec!["RealmA", "RealmB"],
            "every issuer should be listed once, in issuer id order"
        );
    }
}
