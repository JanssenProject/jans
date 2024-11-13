/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

pub mod error;
pub mod key_service;

pub mod open_id_storage;
pub use error::JwtDecodingError;
use jsonwebtoken as jwt;
use key_service::KeyService;
use serde::de::DeserializeOwned;

use crate::common::policy_store::TrustedIssuerMetadata;

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

/// Arguments for decoding a JWT (JSON Web Token).
///
/// This struct allows you to specify parameters for decoding and validating a JWT.
///
/// - Use `iss: Some("some_iss")` to enforce validation of the `iss` (issuer) claim.
/// - Use `iss: None` to skip validation of the `iss` claim.
///
/// The same logic applies to the `aud` (audience) and `sub` (subject) claims. Additionally,
/// you can control whether to validate the token's expiration (`exp`) and not-before (`nbf`)
/// claims with the `validate_exp` and `validate_nbf` flags, respectively.
pub struct DecodingArgs<'a> {
    pub jwt: &'a str,
    pub iss: Option<&'a str>,
    pub aud: Option<&'a str>,
    pub sub: Option<&'a str>,
    pub validate_nbf: bool,
    pub validate_exp: bool,
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
        trusted_idps: Vec<TrustedIssuerMetadata>,
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
    pub fn decode<T: DeserializeOwned>(
        &self,
        decoding_args: DecodingArgs,
    ) -> Result<T, JwtDecodingError> {
        match self {
            DecodingStrategy::WithoutValidation => Self::extract_claims(decoding_args.jwt),
            DecodingStrategy::WithValidation {
                key_service,
                supported_algs,
            } => decode_and_validate_jwt(decoding_args, supported_algs, key_service),
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
    decoding_args: DecodingArgs,
    supported_algs: &[jwt::Algorithm],
    key_service: &KeyService,
) -> Result<T, JwtDecodingError> {
    let header = jwt::decode_header(decoding_args.jwt).map_err(JwtDecodingError::Parsing)?;

    // reject unsupported algorithms early
    if !supported_algs.contains(&header.alg) {
        return Err(JwtDecodingError::TokenSignedWithUnsupportedAlgorithm(
            header.alg,
        ));
    }

    // set up validation rules
    let mut validator = jwt::Validation::new(header.alg);
    let mut required_spec_claims = vec!["iss", "sub", "aud"];
    if decoding_args.validate_exp {
        required_spec_claims.push("exp");
    }
    validator.set_required_spec_claims(&required_spec_claims);
    validator.validate_nbf = decoding_args.validate_nbf;
    validator.validate_exp = decoding_args.validate_exp;
    if let Some(iss) = decoding_args.iss {
        validator.set_issuer(&[iss]);
    } else {
        validator.iss = None;
    }
    if let Some(aud) = decoding_args.aud {
        validator.set_audience(&[aud]);
    } else {
        validator.validate_aud = false;
    }
    validator.sub = decoding_args.sub.map(|sub| sub.to_string());

    // fetch decoding key from the KeyService
    let kid = &header
        .kid
        .ok_or_else(|| JwtDecodingError::JwtMissingKeyId)?;
    let key = key_service
        .get_key(kid)
        .map_err(JwtDecodingError::KeyService)?;
    // TODO: potentially handle JWTs without a `kid` in the future

    // decode and validate the jwt
    let claims = jwt::decode::<T>(decoding_args.jwt, &key, &validator)
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
