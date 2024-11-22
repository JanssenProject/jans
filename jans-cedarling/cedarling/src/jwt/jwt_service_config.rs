/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

//! Module that contains strong typed configuaration for the JWT service.
//! This configuration allows to initialize service without any errors.

use crate::common::policy_store::TrustedIssuer;
use crate::jwt;
use std::collections::HashSet;

use super::decoding_strategy::key_service::{fetch_openid_config, OpenIdConfig};

/// Configuration for JWT service
pub enum JwtServiceConfig {
    /// Decoding strategy that does not perform validation.
    WithoutValidation {
        trusted_idps: Vec<TrustedIssuerAndOpenIdConfig>,
    },

    /// Decoding strategy that performs validation using a key service and supported algorithms.
    WithValidation {
        supported_algs: HashSet<jwt::Algorithm>,
        trusted_idps: Vec<TrustedIssuerAndOpenIdConfig>,
    },
}

/// Structure to store `TrustedIssuer` and `OpenIdConfig` in one place.
#[derive(Clone)]
pub struct TrustedIssuerAndOpenIdConfig {
    pub trusted_issuer: TrustedIssuer,
    pub openid_config: OpenIdConfig,
}

impl TrustedIssuerAndOpenIdConfig {
    /// Fetch openid configuration based on the `TrustedIssuer` and return config
    pub fn fetch(
        trusted_issuer: TrustedIssuer,
        client: &jwt::HttpClient,
    ) -> Result<Self, jwt::KeyServiceError> {
        let openid_config = fetch_openid_config(
            trusted_issuer.openid_configuration_endpoint.as_str(),
            client,
        )?;

        Ok(Self {
            trusted_issuer,
            openid_config,
        })
    }
}
