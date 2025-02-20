// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashMap;
use std::sync::Arc;

use url::Url;

use crate::common::policy_store::TrustedIssuer;

type IssuerId = String; // e.g. '7ca8ccc6e8682ad91f47e651cf7e3dcea4f8133663ae'
type IssuerOrigin = String; // e.g. 'https://account.gluu.org'

/// A pre-processed mapping of trusted issuers, optimized for efficient lookups by domain.
///
/// This structure reorganizes trusted issuers to facilitate fast retrieval based on
/// their associated domains, represented by the `IssuerDomain` type. The internal
/// `HashMap` uses the domain as the key, enabling quick access to a `TrustedIssuer`
/// without requiring complex or iterative searches.
pub struct TrustedIssuersStore {
    issuers: HashMap<IssuerOrigin, TrustedIssuer>,
}

impl TrustedIssuersStore {
    // NOTE: once this store is initialized, it is not expected that the source
    // will be updated. If ever Cedarling supports updating the Trusted Issuers in the
    // future, make sure this implementation is updated.
    pub fn new(source: Arc<Option<HashMap<IssuerId, TrustedIssuer>>>) -> Self {
        let issuers = match source.as_ref() {
            None => HashMap::new(),
            Some(issuers) => issuers
                .values()
                .map(|iss| {
                    let endpoint = Url::parse(&iss.oidc_endpoint).unwrap();
                    let iss_origin: IssuerOrigin = endpoint.origin().ascii_serialization();
                    (iss_origin, iss.clone())
                })
                .collect::<HashMap<IssuerOrigin, TrustedIssuer>>(),
        };

        Self { issuers }
    }

    pub fn get(&self, iss_domain: &str) -> Option<&TrustedIssuer> {
        self.issuers.get(iss_domain)
    }
}
