/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

pub mod key_service;
use crate::common::policy_store::TrustedIssuer;

use super::TokenValidationError;
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
    /// # Errors
    /// Returns an error if the specified algorithm is unrecognized or the key service initialization fails.
    pub fn new_with_validation(
        config_algs: Vec<jwt::Algorithm>,
        trusted_idps: Vec<TrustedIssuer>,
    ) -> Result<Self, TokenValidationError> {
        // initialize the key service with OpenID configuration endpoints
        let openid_conf_endpoints = trusted_idps
            .iter()
            .map(|x| x.openid_configuration_endpoint.as_ref())
            .collect();
        let key_service =
            KeyService::new(openid_conf_endpoints).map_err(TokenValidationError::KeyService)?;

        Ok(Self::WithValidation {
            key_service,
            supported_algs: config_algs,
        })
    }

    /// Decodes a JWT token according to the current decoding strategy.
    ///
    /// # Arguments
    /// - `jwt` - The JWT string to decode.
    /// - `iss` - Optional expected issuer for validation - use None for no validation.
    /// - `aud` - Optional expected audience for validation - use None for no validation.
    /// - `sub` - Optional expected subject for validation - use None for no validation.
    /// - `validate_nbf`: A boolean indicating whether to validate the "not before" claim.
    /// - `validate_exp`: A boolean indicating whether to validate the expiration claim.
    ///
    /// # Errors
    /// Returns an error if decoding or validation fails.
    pub fn decode<T: DeserializeOwned>(
        &self,
        jwt: &str,
        iss: Option<impl ToString>,
        aud: Option<impl ToString>,
        sub: Option<impl ToString>,
        validate_nbf: bool,
        validate_exp: bool,
    ) -> Result<T, TokenValidationError> {
        match self {
            DecodingStrategy::WithoutValidation => Self::extract_claims(jwt),
            DecodingStrategy::WithValidation {
                key_service,
                supported_algs,
            } => decode_and_validate_jwt(DecodeAndValidateArgs {
                jwt,
                iss: iss.map(|iss| iss.to_string()),
                aud: aud.map(|aud| aud.to_string()),
                sub: sub.map(|sub| sub.to_string()),
                validate_nbf,
                validate_exp,
                supported_algs,
                key_service,
            }),
        }
    }

    /// Extracts the claims from a JWT token without performing validation.
    ///
    /// This method uses a default insecure validator that skips signature
    /// validation and other checks (e.g., expiration). Only use in trusted environments.
    ///
    /// # Errors
    /// Returns an error if the claims cannot be extracted.
    pub fn extract_claims<T: DeserializeOwned>(jwt_str: &str) -> Result<T, TokenValidationError> {
        let mut validator = jwt::Validation::default();
        validator.insecure_disable_signature_validation();
        validator.required_spec_claims.clear();
        validator.validate_exp = false;
        validator.validate_aud = false;
        validator.validate_nbf = false;

        let key = jwt::DecodingKey::from_secret("some_secret".as_ref());

        let claims = jwt::decode::<T>(jwt_str, &key, &validator)
            .map_err(TokenValidationError::Parsing)?
            .claims;

        Ok(claims)
    }
}

struct DecodeAndValidateArgs<'a> {
    jwt: &'a str,
    iss: Option<String>,
    aud: Option<String>,
    sub: Option<String>,
    validate_nbf: bool,
    validate_exp: bool,
    supported_algs: &'a Vec<jwt::Algorithm>,
    key_service: &'a KeyService,
}

/// Decodes and validates a JWT token using supported algorithms and a key service.
///
/// # Arguments
/// - `jwt` - The JWT string to decode.
/// - `iss` - Optional expected issuer for validation.
/// - `aud` - Optional expected audience for validation.
/// - `sub` - Optional expected subject for validation.
/// - `validate_nbf`: A boolean indicating whether to validate the "not before" claim.
/// - `validate_exp`: A boolean indicating whether to validate the expiration claim.
/// - `supported_algs` - A reference to a vector of supported algorithms for validation.
/// - `key_service` - A reference to a `KeyService` to retrieve keys for signature validation.
///
/// # Errors
/// Returns an error if the token uses an unsupported algorithm or if validation fails.
fn decode_and_validate_jwt<T: DeserializeOwned>(
    args: DecodeAndValidateArgs,
) -> Result<T, TokenValidationError> {
    let header = jwt::decode_header(args.jwt).map_err(TokenValidationError::Parsing)?;

    // reject unsupported algorithms early
    if !args.supported_algs.contains(&header.alg) {
        return Err(TokenValidationError::TokenSignedWithUnsupportedAlgorithm(
            header.alg,
        ));
    }

    // set up validation rules
    let mut validator = jwt::Validation::new(header.alg);
    validator.required_spec_claims.clear();
    validator.validate_nbf = args.validate_nbf;
    validator.validate_exp = args.validate_exp;
    if let Some(iss) = args.iss {
        let mut iss = iss.strip_prefix("\"").unwrap_or(&iss);
        iss = iss.strip_suffix("\"").unwrap_or(&iss);
        validator.set_issuer(&[iss]);
    } else {
        validator.iss = None;
    }
    if let Some(aud) = args.aud {
        let mut aud = aud.strip_prefix("\"").unwrap_or(&aud);
        aud = aud.strip_suffix("\"").unwrap_or(&aud);
        validator.set_audience(&[aud]);
    } else {
        validator.validate_aud = false;
    }
    if let Some(sub) = args.sub {
        let mut sub = sub.strip_prefix("\"").unwrap_or(&sub);
        sub = sub.strip_suffix("\"").unwrap_or(&sub);
        validator.sub = Some(sub.to_string());
    } else {
        validator.sub = None;
    }

    // fetch decoding key from the KeyService
    let kid = &header
        .kid
        .ok_or_else(|| TokenValidationError::MissingRequiredHeader("kid".into()))?;
    let key = args
        .key_service
        .get_key(kid)
        .map_err(TokenValidationError::KeyService)?;
    // TODO: handle tokens without a `kid` in the header

    // decode and validate the jwt
    let claims = jwt::decode::<T>(args.jwt, &key, &validator)
        .map_err(TokenValidationError::Validation)?
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
/// This function maps algorithm names (e.g., "HS256", "RS256") to corresponding
/// `jwt::Algorithm` enum values. Returns an error if the algorithm is unsupported.
///
/// # Arguments
/// * `algorithm` - The string representing the algorithm to convert.
///
/// # Errors
/// Returns an error if the algorithm is not implemented.
pub fn string_to_alg(algorithm: &str) -> Result<jwt::Algorithm, ParseAlgorithmError> {
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
            algorithm.into(),
        )),
    }
}
