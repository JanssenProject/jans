/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use std::collections::HashSet;

use super::policy_store::{load_policy_store, PolicyStoreLoadError};
use crate::common::policy_store::PolicyStore;
use crate::jwt::TrustedIssuerAndOpenIdConfig;
use crate::{bootstrap_config, jwt};
use bootstrap_config::BootstrapConfig;

/// Configuration that hold validated infomation from bootstrap config
#[derive(typed_builder::TypedBuilder, Clone)]
pub(crate) struct ServiceConfig {
    pub policy_store: PolicyStore,
    pub jwt_algorithms: HashSet<jwt::Algorithm>,
    pub trusted_issuers_and_openid: Vec<TrustedIssuerAndOpenIdConfig>,
}

#[derive(thiserror::Error, Debug)]
pub enum ServiceConfigError {
    /// Parse jwt algorithm error.
    #[error("could not parse an algorithim defined in the config: {0}")]
    ParseAlgorithm(#[from] jwt::ParseAlgorithmError),
    /// Error that may occur during loading the policy store.
    #[error("Could not load policy: {0}")]
    PolicyStore(#[from] PolicyStoreLoadError),
    #[error("Could not load openid config: {0}")]
    // TODO: refactor error when remove panicking on init JWT server
    OpenIdConfig(#[from] jwt::decoding_strategy::key_service::KeyServiceError),
}

impl ServiceConfig {
    pub fn new(bootstrap: &BootstrapConfig) -> Result<Self, ServiceConfigError> {
        let client = jwt::HttpClient::new()?;
        let policy_store = load_policy_store(&bootstrap.policy_store_config)?;

        // We  fetch `OpenidConfig` using `TrustedIssuer`
        // and store both in the `TrustedIssuerAndOpenIdConfig` structure.
        let trusted_issuers_and_openid = policy_store
            .trusted_issuers
            .clone()  // we need clone to avoid borrowing
            .unwrap_or_default()
            .values()
            .map(|trusted_issuer| {
                TrustedIssuerAndOpenIdConfig::fetch(trusted_issuer.clone(), &client)
            })
            .collect::<Result<Vec<_>, _>>()?;

        let builder = ServiceConfig::builder()
            .jwt_algorithms(match &bootstrap.jwt_config {
                crate::JwtConfig::Disabled => HashSet::new(),
                crate::JwtConfig::Enabled { signature_algorithms, .. } => signature_algorithms.clone(),
            })
            .policy_store(policy_store)
            .trusted_issuers_and_openid(trusted_issuers_and_openid);

        Ok(builder.build())
    }
}
