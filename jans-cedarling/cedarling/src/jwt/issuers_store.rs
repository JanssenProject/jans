// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::common::policy_store::TrustedIssuer;
use std::collections::{HashMap, hash_map};
use std::sync::Arc;
use url::Url;

type IssuerId = String; // e.g. '7ca8ccc6e8682ad91f47e651cf7e3dcea4f8133663ae'
type IssuerOrigin = String; // e.g. 'https://account.gluu.org'

/// A pre-processed mapping of trusted issuers, optimized for efficient lookups by domain.
///
/// This structure reorganizes trusted issuers to facilitate fast retrieval based on
/// their associated domains, represented by the `IssuerDomain` type. The internal
/// `HashMap` uses the domain as the key, enabling quick access to a `TrustedIssuer`
/// without requiring complex or iterative searches.
#[derive(Default)]
pub struct TrustedIssuersStore {
    issuers: HashMap<IssuerOrigin, Arc<TrustedIssuer>>,
}

impl TrustedIssuersStore {
    // NOTE: once this store is initialized, it is not expected that the source
    // will be updated. If ever Cedarling supports updating the Trusted Issuers in the
    // future, make sure this implementation is updated.
    pub fn new(issuers: HashMap<IssuerId, TrustedIssuer>) -> Self {
        let issuers = issuers
            .values()
            .map(|iss| {
                // TODO: Handle error
                let endpoint = Url::parse(&iss.openid_configuration_endpoint).expect(&format!(
                    "failed to parse url: {}",
                    iss.openid_configuration_endpoint
                ));
                let iss_origin: IssuerOrigin = endpoint.origin().ascii_serialization();
                (iss_origin, iss.clone().into())
            })
            .collect::<HashMap<IssuerOrigin, Arc<TrustedIssuer>>>();

        Self { issuers }
    }

    pub fn get(&self, iss_url: &str) -> Option<Arc<TrustedIssuer>> {
        self.issuers.get(iss_url).map(|x| x.clone())
    }

    pub fn iter(&self) -> hash_map::Iter<'_, String, Arc<TrustedIssuer>> {
        self.issuers.iter()
    }
}
