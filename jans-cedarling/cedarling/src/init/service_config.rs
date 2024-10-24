/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::jwt_algorithm::parse_jwt_algorithms;
use super::policy_store::{load_policy_store, LoadPolicyStoreError};
use crate::common::policy_store::PolicyStore;
use crate::{
    bootstrap_config,
    jwt::{Algorithm, ParseAlgorithmError},
};
use bootstrap_config::BootstrapConfig;

/// Configuration that hold validated infomation from bootstrap config
#[derive(typed_builder::TypedBuilder, Clone)]
pub(crate) struct ServiceConfig {
    pub jwt_algorithms: Vec<Algorithm>,
    pub policy_store: PolicyStore,
}

#[derive(thiserror::Error, Debug)]
pub enum ServiceConfigError {
    /// Parse jwt algorithm error.
    #[error("could not parse an algorithim defined in the config: {0}")]
    ParseAlgorithm(#[from] ParseAlgorithmError),
    /// Error that may occur during loading the policy store.
    #[error("Could not load policy: {0}")]
    PolicyStore(#[from] LoadPolicyStoreError),
}

impl ServiceConfig {
    pub fn new(bootstrap: &BootstrapConfig) -> Result<Self, ServiceConfigError> {
        let builder = ServiceConfig::builder()
            .jwt_algorithms(parse_jwt_algorithms(bootstrap)?)
            .policy_store(load_policy_store(&bootstrap.policy_store_config)?);

        Ok(builder.build())
    }
}
