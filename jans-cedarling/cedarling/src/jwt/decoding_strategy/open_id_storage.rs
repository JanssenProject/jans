/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use std::collections::HashMap;

use crate::jwt::TrustedIssuerAndOpenIdConfig;

/// Storage to hold mapping issuer to trusted_issuer and openid config
/// ang get this values whe it is needed
#[derive(Default)]
pub struct OpenIdStorage {
    issuers_map: HashMap<Box<str>, TrustedIssuerAndOpenIdConfig>, // issuer => TrustedIssuerAndOpenIdConfig
}

impl OpenIdStorage {
    pub fn new(trusted_idps: Vec<TrustedIssuerAndOpenIdConfig>) -> OpenIdStorage {
        Self {
            issuers_map: trusted_idps
                .into_iter()
                .map(|config| (config.openid_config.issuer.clone(), config))
                .collect(),
        }
    }

    pub fn get(&self, issuer: &str) -> Option<&TrustedIssuerAndOpenIdConfig> {
        self.issuers_map.get(issuer)
    }
}
