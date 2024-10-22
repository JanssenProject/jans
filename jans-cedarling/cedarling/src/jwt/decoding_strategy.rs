/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::traits::{Decode, ExtractClaims, GetKey};
use super::Error;
use jsonwebtoken as jwt;
use serde::de::DeserializeOwned;
use std::sync::Arc;

/// Represents the decoding strategy for JWT tokens.
///
/// This enum determines how JWT tokens are decoded, either without validation
/// or with validation using specified algorithms and a key service. The
/// appropriate strategy can be selected based on the bootstrap config.
pub enum DecodingStrategy {
    /// Decoding strategy that does not perform validation.
    WithoutValidation,

    /// Decoding strategy that performs validation using a key service and supported algorithms.
    WithValidation {
        key_service: Arc<dyn GetKey>,
        supported_algs: Vec<jwt::Algorithm>,
    },
}

impl DecodingStrategy {
    /// Creates a new decoding strategy that skips validation.
    ///
    /// This method initializes the `WithoutValidation` strategy, which
    /// decodes JWT tokens without any validation checks
    pub fn new_without_validation() -> Self {
        Self::WithoutValidation
    }
}

/// Trait implementation for decoding a JWT token.
impl Decode for DecodingStrategy {
    // Decode a JWT according to the current decoding strategy.
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
    /// Extracts the claims from a JWT token without validation.
    fn extract_claims<T: DeserializeOwned>(&self, jwt_str: &str) -> Result<T, Error> {
        let mut validator = jwt::Validation::default();
        validator.insecure_disable_signature_validation();
        validator.validate_exp = false;
        validator.validate_aud = false;
        validator.validate_nbf = false;

        let key = jwt::DecodingKey::from_secret("some_secret".as_ref());

        let claims = jwt::decode::<T>(jwt_str, &key, &validator)
            .map_err(Error::Validation)?
            .claims;

        Ok(claims)
    }
}

/// Decodes and validates a JWT token based on supported algorithms and a key service.
///
/// # Errors
/// - Returns an error if the JWT header specifies an unsupported algorithm.
/// - Returns an error if validation fails due to signature mismatch or claim validation failure.
fn decode_and_validate_jwt<T: DeserializeOwned>(
    jwt: &str,
    iss: Option<impl ToString>,
    aud: Option<impl ToString>,
    req_sub: bool,
    supported_algs: &[jwt::Algorithm],
    key_service: &Arc<dyn GetKey>,
) -> Result<T, Error> {
    let header = jwt::decode_header(jwt).map_err(Error::Parsing)?;

    // reject unsupported algorithms early
    if !supported_algs.contains(&header.alg) {
        return Err(Error::TokenSignedWithUnsupportedAlgorithm(header.alg));
    }

    // Set validator configs
    let mut validator = jwt::Validation::new(header.alg);
    validator.validate_nbf = true;
    if let Some(iss) = iss {
        validator.set_issuer(&[iss]);
    }
    if let Some(aud) = aud {
        validator.set_audience(&[aud]);
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
    let key = key_service.get_key(kid)?;
    // TODO: handle tokens without a `kid` in the header

    // extract claims
    let claims = jwt::decode::<T>(jwt, key, &validator)
        .map_err(Error::Validation)?
        .claims;
    Ok(claims)
}

#[derive(thiserror::Error, Debug)]
pub enum ParseAlgorithmError {
    /// Config contains an unimplemented algorithm
    #[error("algorithim is not yet implemented: {0}")]
    UnimplementedAlgorithm(String),
}

/// Converts a string representation of an algorithm to a `jwt::Algorithm` enum.
///
/// This function attempts to map a string representing an algorithm (e.g., "HS256")
/// to its corresponding `jwt::Algorithm` enum. If the algorithm is unsupported or
/// unrecognized, an error is returned.
pub(crate) fn string_to_alg(algorithm: &str) -> Result<jwt::Algorithm, ParseAlgorithmError> {
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
        _ => Err(ParseAlgorithmError::UnimplementedAlgorithm(
            algorithm.to_string(),
        )),
    }
}
