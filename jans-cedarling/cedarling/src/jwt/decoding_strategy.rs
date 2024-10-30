/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

pub mod key_service;
use crate::common::policy_store::TrustedIssuer;

use super::InitError;
use jsonwebtoken as jwt;
pub use key_service::KeyService;
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
        supported_algs: Vec<jwt::Algorithm>,
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
    /// # Arguments
    /// * `dep_map` - A reference to the dependency map containing necessary services.
    /// * `config_algs` - A vector of strings representing supported algorithms for validation.
    ///
    /// # Errors
    /// Returns an error if the specified algorithm is unrecognized or the key service initialization fails.
    pub fn new_with_validation(
        config_algs: Vec<String>,
        trusted_idps: Vec<TrustedIssuer>,
    ) -> Result<Self, InitError> {
        // initialize the key service with OpenID configuration endpoints
        let openid_conf_endpoints = trusted_idps
            .iter()
            .map(|x| x.openid_configuration_endpoint.as_ref())
            .collect();
        let key_service = KeyService::new(openid_conf_endpoints)?;

        let config_algs = parse_jwt_algorithms(config_algs).map_err(InitError::DecodingStrategy)?;

        Ok(Self::WithValidation {
            key_service,
            supported_algs: config_algs,
        })
    }

    pub fn decode<T: DeserializeOwned>(
        &self,
        jwt: &str,
        iss: Option<impl ToString>,
        aud: Option<impl ToString>,
        req_sub: bool,
    ) -> Result<T, super::Error> {
        match self {
            DecodingStrategy::WithoutValidation => self.extract_claims(jwt),
            DecodingStrategy::WithValidation {
                key_service,
                supported_algs,
            } => decode_and_validate_jwt(jwt, iss, aud, req_sub, supported_algs, key_service),
        }
    }

    /// Extracts the claims from a JWT token without performing validation.
    ///
    /// This method uses a default insecure validator that skips signature
    /// validation and other checks (e.g., expiration). Only use in trusted environments.
    ///
    /// # Errors
    /// Returns an error if the claims cannot be extracted.
    pub fn extract_claims<T: DeserializeOwned>(&self, jwt_str: &str) -> Result<T, super::Error> {
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
    supported_algs: &[jwt::Algorithm],
    key_service: &KeyService,
) -> Result<T, super::Error> {
    let header = jwt::decode_header(jwt).map_err(Error::Parsing)?;

    // reject unsupported algorithms early
    if !supported_algs.contains(&header.alg) {
        return Err(Error::TokenSignedWithUnsupportedAlgorithm(header.alg).into());
    }

    // set up validation rules
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
    // TODO: figure out how to handle tokens without a `kid` in the header
    // there's no plans yet if we need to support this or not.

    // decode and validate the jwt
    let claims = jwt::decode::<T>(jwt, &key, &validator)
        .map_err(Error::Validation)?
        .claims;
    Ok(claims)
}

fn parse_jwt_algorithms(algorithms: Vec<String>) -> Result<Vec<jwt::Algorithm>, Error> {
    let parsing_results = algorithms
        .iter()
        .map(|alg| string_to_alg(alg))
        .collect::<Vec<Result<jwt::Algorithm, Box<str>>>>();

    let (successes, errors): (Vec<_>, Vec<_>) =
        parsing_results.into_iter().partition(Result::is_ok);

    // Collect all errors into a single error message or return them as a vector.
    if !errors.is_empty() {
        let unsupported_algs = errors
            .into_iter()
            .filter_map(Result::err)
            .collect::<Vec<Box<str>>>()
            .join(", ");
        return Err(Error::UnimplementedAlgorithm(unsupported_algs));
    }

    let algorithms = successes
        .into_iter()
        .filter_map(Result::ok)
        .collect::<Vec<jwt::Algorithm>>();

    Ok(algorithms)
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
pub fn string_to_alg(algorithm: &str) -> Result<jwt::Algorithm, Box<str>> {
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
        _ => Err(algorithm.into()),
    }
}

/// Error type for issues encountered during JWT decoding and validation.
///
/// The `DecodingStrategy` is responsible for parsing, validating, and verifying JWTs.
/// This enum represents various errors that can occur during these processes, such as
/// issues with parsing the token, unsupported algorithms, or validation failures.
#[derive(thiserror::Error, Debug)]
pub enum Error {
    /// An error occurred while parsing the JWT.
    ///
    /// This error occurs when the provided JWT cannot be properly parsed.
    /// This might happen if the token is malformed or contains invalid data
    /// that does not conform to the JWT structure.
    #[error("Error parsing the JWT: {0}")]
    Parsing(#[source] jsonwebtoken::errors::Error),

    /// The token was signed using an unsupported algorithm.
    ///
    /// This error occurs when the JWT's header specifies an algorithm that
    /// is not supported by the current validation configuration. Common causes
    /// include encountering an algorithm that is disallowed or not yet implemented
    /// by the decoding strategy.
    #[error("The JWT is signed with an unsupported algorithm: {0:?}")]
    TokenSignedWithUnsupportedAlgorithm(jsonwebtoken::Algorithm),

    /// A required header is missing from the JWT.
    ///
    /// This error occurs when a necessary header is missing from the JWT's header section,
    /// preventing proper verification or validation of the token.
    ///
    /// *Certain headers, such as `kid` (Key ID), are required for proper JWT processing.*
    #[error("The JWT is missing a required header: {0}")]
    MissingRequiredHeader(String),

    /// JWT validation failed.
    ///
    /// This error occurs when the JWT fails to pass validation checks. Common causes
    /// include an invalid signature, expired tokens, or claims that do not meet
    /// the expected criteria. The underlying validation error provides more context
    /// on why the token was considered invalid.
    #[error("Failed to validate the JWT: {0}")]
    Validation(#[source] jsonwebtoken::errors::Error),

    /// The configuration defines an unsupported or unimplemented algorithm.
    ///
    /// This error occurs when the validation configuration specifies an algorithm
    /// that is either not implemented by the `DecodingStrategy` or not recognized
    /// by the JWT service. This typically happens when the application tries to use
    /// a newer or less common algorithm that has not been added to the supported set.
    #[error("An algorithm(s) defined in the configuration is not yet implemented: {0}")]
    UnimplementedAlgorithm(String),
}
