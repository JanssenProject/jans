/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

pub mod key_service;
mod traits;

use super::Error;
use crate::models::policy_store::PolicyStore;
use di::DependencySupplier;
use jsonwebtoken as jwt;
pub use key_service::*;
use serde::de::DeserializeOwned;
use std::sync::Arc;
pub use traits::*;

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
        supported_algs: Vec<jwt::Algorithm>,
    },
}

impl DecodingStrategy {
    /// Creates a new decoding strategy that does not perform any validation.
    ///
    /// This strategy is useful for cases where token validation is not required,
    /// but claims still need to be extracted.
    pub fn new_without_validation() -> Self {
        Self::WithoutValidation
    }

    /// Creates a new decoding strategy that performs validation.
    ///
    /// This strategy uses the provided dependency map to configure a key service
    /// and validates tokens based on the specified algorithms.
    ///
    /// # Arguments
    /// * `dep_map` - A reference to the dependency map containing necessary services.
    /// * `config_algs` - A vector of strings representing supported algorithms for validation.
    ///
    /// # Errors
    /// Returns an error if the specified algorithm is unrecognized or the key service initialization fails.
    pub fn new_with_validation(
        dep_map: &di::DependencyMap,
        config_algs: &Vec<String>,
    ) -> Result<Self, Error> {
        let config: Arc<PolicyStore> = dep_map.get();

        // initialize the key service with OpenID configuration endpoints
        let openid_conf_endpoints = config
            .trusted_idps
            .iter()
            .map(|x| x.openid_configuration_endpoint.as_ref())
            .collect();
        let key_service = KeyService::new(openid_conf_endpoints).map_err(Error::KeyServiceError)?;

        // convert provided algorithm strings into `jwt::Algorithm` and collect them
        let mut supported_algs = vec![];
        for alg_str in config_algs {
            supported_algs.push(string_to_alg(&alg_str)?);
        }

        Ok(Self::WithValidation {
            key_service,
            supported_algs,
        })
    }
}

/// Trait implementation for decoding a JWT token.
impl Decode for DecodingStrategy {
    /// Decodes a JWT token according to the current decoding strategy.
    ///
    /// # Arguments
    /// * `jwt` - The JWT string to decode.
    /// * `iss` - Optional expected issuer for validation.
    /// * `aud` - Optional expected audience for validation.
    /// * `req_sub` - If true, requires the subject (`sub`) claim to be present.
    ///
    /// # Errors
    /// Returns an error if decoding or validation fails.
    fn decode<T: DeserializeOwned>(
        &self,
        jwt: &str,
        iss: Option<impl ToString>,
        aud: Option<impl ToString>,
        req_sub: bool,
    ) -> Result<T, Error> {
        match self {
            DecodingStrategy::WithoutValidation => self.extract_claims(jwt),
            DecodingStrategy::WithValidation {
                key_service,
                supported_algs,
            } => decode_and_validate_jwt(jwt, iss, aud, req_sub, supported_algs, key_service),
        }
    }
}

/// Trait implementation for extracting claims without validation.
impl ExtractClaims for DecodingStrategy {
    /// Extracts the claims from a JWT token without performing validation.
    ///
    /// This method uses a default insecure validator that skips signature
    /// validation and other checks (e.g., expiration). Only use in trusted environments.
    ///
    /// # Errors
    /// Returns an error if the claims cannot be extracted.
    fn extract_claims<T: DeserializeOwned>(&self, jwt_str: &str) -> Result<T, Error> {
        let mut validator = jwt::Validation::default();
        validator.insecure_disable_signature_validation();
        validator.validate_exp = false;
        validator.validate_aud = false;
        validator.validate_nbf = false;

        let key = jwt::DecodingKey::from_secret("some_secret".as_ref());

        let claims = jwt::decode::<T>(&jwt_str, &key, &validator)
            .map_err(Error::ValidationError)?
            .claims;

        Ok(claims)
    }
}

/// Decodes and validates a JWT token using supported algorithms and a key service.
///
/// # Arguments
/// * `jwt` - The JWT string to decode.
/// * `iss` - Optional expected issuer for validation.
/// * `aud` - Optional expected audience for validation.
/// * `req_sub` - Boolean indicating whether the `sub` (subject) claim is required.
/// * `supported_algs` - A reference to a vector of supported algorithms for validation.
/// * `key_service` - A reference to a `KeyService` to retrieve keys for signature validation.
///
/// # Errors
/// Returns an error if the token uses an unsupported algorithm or if validation fails.
fn decode_and_validate_jwt<T: DeserializeOwned>(
    jwt: &str,
    iss: Option<impl ToString>,
    aud: Option<impl ToString>,
    req_sub: bool,
    supported_algs: &Vec<jwt::Algorithm>,
    key_service: &KeyService,
) -> Result<T, Error> {
    let header = jwt::decode_header(jwt).map_err(Error::ParsingError)?;

    // reject unsupported algorithms early
    if !supported_algs.contains(&header.alg) {
        return Err(Error::TokenSignedWithUnsupportedAlgorithm(header.alg));
    }

    // set up validation rules
    let mut validator = jwt::Validation::new(header.alg);
    validator.validate_nbf = true;
    if let Some(iss) = iss {
        validator.set_issuer(&vec![iss]);
    }
    if let Some(aud) = aud {
        validator.set_audience(&vec![aud]);
    } else {
        validator.validate_aud = false;
    }
    if req_sub {
        validator.set_required_spec_claims(&["sub"]);
    }

    // fetch decoding key from the KeyService
    let kid = &header
        .kid
        .ok_or_else(|| Error::MissingRequiredHeader("kid".into()))?;
    let key = key_service.get_key(kid).map_err(Error::KeyServiceError)?;
    // TODO: handle tokens without a `kid` in the header

    // decode and validate the jwt
    let claims = jwt::decode::<T>(&jwt, &key, &validator)
        .map_err(Error::ValidationError)?
        .claims;
    Ok(claims)
}

/// Converts a string representation of an algorithm to a `jwt::Algorithm` enum.
///
/// This function maps algorithm names (e.g., "HS256", "RS256") to corresponding
/// `jwt::Algorithm` enum values. Returns an error if the algorithm is unsupported.
///
/// # Arguments
/// * `algorithm` - The string representing the algorithm to convert.
///
/// # Errors
/// Returns an error if the algorithm is not implemented.
fn string_to_alg(algorithm: &str) -> Result<jwt::Algorithm, Error> {
    match algorithm {
        "HS256" => Ok(jwt::Algorithm::HS256),
        "HS384" => Ok(jwt::Algorithm::HS384),
        "HS512" => Ok(jwt::Algorithm::HS512),
        "ES256" => Ok(jwt::Algorithm::ES256),
        "ES384" => Ok(jwt::Algorithm::ES384),
        "RS256" => Ok(jwt::Algorithm::RS256),
        "RS384" => Ok(jwt::Algorithm::RS384),
        "RS512" => Ok(jwt::Algorithm::RS512),
        "PS256" => Ok(jwt::Algorithm::PS256),
        "PS384" => Ok(jwt::Algorithm::PS384),
        "PS512" => Ok(jwt::Algorithm::PS512),
        "EdDSA" => Ok(jwt::Algorithm::EdDSA),
        _ => Err(Error::UnimplementedAlgorithm(algorithm.into())),
    }
}
