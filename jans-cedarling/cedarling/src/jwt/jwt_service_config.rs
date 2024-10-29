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

/// Configuration for JWT service
pub enum JwtServiceConfig {
    /// Decoding strategy that does not perform validation.
    WithoutValidation,

    /// Decoding strategy that performs validation using a key service and supported algorithms.
    WithValidation {
        supported_algs: Vec<jwt::Algorithm>,
        trusted_idps: Vec<TrustedIssuer>,
    },
}
