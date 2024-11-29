/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

pub mod error;
pub mod key_service;

pub mod open_id_storage;
use std::collections::HashSet;

use crate::common::policy_store::TrustedIssuer;
pub use error::JwtDecodingError;
use jsonwebtoken as jwt;
use key_service::KeyService;
use serde::de::DeserializeOwned;

/// Represents the decoding strategy for JWT tokens.
///
/// This enum defines two strategies for decoding JWT tokens: `WithoutValidation`
/// for decoding without validation and `WithValidation` for decoding with validation
/// using a key service and supported algorithms.
pub enum DecodingStrategy {
    /// Decoding strategy that skips all validation.
    WithoutValidation,

    /// Decoding strategy that performs validation using a key service and supported algorithms.
    WithValidation {
        key_service: KeyService,
        supported_algs: HashSet<jwt::Algorithm>,
    },
}

impl DecodingStrategy {
    /// Creates a new decoding strategy that does not perform validation.
    pub fn new_without_validation() -> Self {
        Self::WithoutValidation
    }

    /// Creates a new decoding strategy that performs validation.
    ///
    /// This strategy uses the provided dependency map to configure a key service
    /// and validates tokens based on the specified algorithms.
    ///
    /// # Errors
    /// Returns an error if the specified algorithm is unrecognized or the key service initialization fails.
    pub fn new_with_validation(
        config_algs: HashSet<jwt::Algorithm>,
        trusted_idps: Vec<TrustedIssuer>,
    ) -> Result<Self, JwtDecodingError> {
        // initialize the key service with OpenID configuration endpoints
        let openid_conf_endpoints = trusted_idps
            .iter()
            .map(|x| x.openid_configuration_endpoint.as_ref())
            .collect();
        let key_service = KeyService::new(openid_conf_endpoints)?;

        Ok(Self::WithValidation {
            key_service,
            supported_algs: config_algs,
        })
    }

    /// Decodes a JWT token according to the current decoding strategy.
    ///
    /// # Errors
    /// Returns an error if decoding or validation fails.
    pub fn decode<T: DeserializeOwned>(&self, jwt: &str) -> Result<T, JwtDecodingError> {
        match self {
            DecodingStrategy::WithoutValidation => Self::extract_claims(jwt),
            DecodingStrategy::WithValidation {
                key_service,
                supported_algs,
            } => decode_and_validate_jwt(jwt, supported_algs, key_service),
        }
    }

    /// Extracts the claims from a JWT token without performing validation.
    ///
    /// This method uses a default insecure validator that skips signature
    /// validation and other checks (e.g., expiration). Only use in trusted environments.
    ///
    /// # Errors
    /// Returns an error if the claims cannot be extracted.
    pub fn extract_claims<T: DeserializeOwned>(jwt_str: &str) -> Result<T, JwtDecodingError> {
        let mut validator = jwt::Validation::default();
        validator.insecure_disable_signature_validation();
        validator.required_spec_claims.clear();
        validator.validate_exp = false;
        validator.validate_aud = false;
        validator.validate_nbf = false;

        let key = jwt::DecodingKey::from_secret("some_secret".as_ref());

        let claims = jwt::decode::<T>(jwt_str, &key, &validator)
            .map_err(JwtDecodingError::Parsing)?
            .claims;

        Ok(claims)
    }
}

/// Decodes and validates a JWT token using supported algorithms and a key service.
///
/// # Errors
/// Returns an error if the token uses an unsupported algorithm or if validation fails.
fn decode_and_validate_jwt<T: DeserializeOwned>(
    jwt: &str,
    supported_algs: &HashSet<jwt::Algorithm>,
    key_service: &KeyService,
) -> Result<T, JwtDecodingError> {
    let header = jwt::decode_header(jwt).map_err(JwtDecodingError::Parsing)?;

    // reject unsupported algorithms early
    if !supported_algs.contains(&header.alg) {
        return Err(JwtDecodingError::TokenSignedWithUnsupportedAlgorithm(
            header.alg,
        ));
    }

    // set up validation rules
    let mut validator = jwt::Validation::new(header.alg);
    // We clear the required claims because the validator requires
    // `exp` by default.
    validator.required_spec_claims.clear();
    // `aud` should be optional.
    validator.validate_aud = false;
    validator.validate_exp = true;
    validator.validate_nbf = true;

    // fetch decoding key from the KeyService
    let kid = &header
        .kid
        .ok_or_else(|| JwtDecodingError::JwtMissingKeyId)?;
    let key = key_service
        .get_key(kid)
        .map_err(JwtDecodingError::KeyService)?;
    // TODO: potentially handle JWTs without a `kid` in the future

    // decode and validate the jwt
    let claims = jwt::decode::<T>(jwt, &key, &validator)
        .map_err(JwtDecodingError::Validation)?
        .claims;
    Ok(claims)
}

#[derive(thiserror::Error, Debug)]
pub enum ParseAlgorithmError {
    /// Config contains an unimplemented algorithm
    #[error("algorithim is not yet implemented: {0}")]
    UnimplementedAlgorithm(String),
}
