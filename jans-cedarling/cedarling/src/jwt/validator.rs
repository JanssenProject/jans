mod config;
#[cfg(test)]
mod test;

use super::new_key_service::NewKeyService;
use crate::common::policy_store::TrustedIssuer;
use base64::prelude::*;
pub use config::*;
use jsonwebtoken::{self as jwt};
use jsonwebtoken::{decode_header, Algorithm, Validation};
use serde_json::Value;
use std::collections::HashMap;
use std::sync::Arc;

type IssuerId = String;
pub type TokenClaims = Value;

/// Validates Json Web Tokens.
#[allow(dead_code)]
pub struct JwtValidator {
    config: JwtValidatorConfig,
    key_service: Arc<Option<NewKeyService>>,
    validators: HashMap<Algorithm, Validation>,
}

#[derive(Debug, PartialEq)]
pub struct ProcessedJwt<'a> {
    pub claims: TokenClaims,
    pub key_iss: Option<&'a TrustedIssuer>,
}

#[allow(dead_code)]
impl JwtValidator {
    pub fn new(
        config: JwtValidatorConfig,
        key_service: Arc<Option<NewKeyService>>,
    ) -> Result<Self, JwtValidatorError> {
        if *config.sig_validation && key_service.is_none() {
            Err(JwtValidatorError::MissingKeyService)?;
        }

        // we define all the signature validators at startup so we can reuse them.
        let validators = config
            .algs_supported
            .iter()
            .map(|alg| {
                let mut validation = Validation::new(*alg);

                validation.validate_exp = config.validate_exp;
                validation.validate_nbf = config.validate_nbf;

                // we will validate the missing claims in another function but this
                // defaults to true so we need to set it to false.
                validation.required_spec_claims.clear();
                validation.validate_aud = false;

                (*alg, validation)
            })
            .collect::<HashMap<Algorithm, Validation>>();

        Ok(Self {
            config,
            key_service,
            validators,
        })
    }

    /// Decodes the JWT and optionally validates it depending on the config.
    pub fn process_jwt<'a>(&'a self, jwt: &'a str) -> Result<ProcessedJwt<'a>, JwtValidatorError> {
        let processed_jwt = match *self.config.sig_validation {
            true => self.decode_and_validate_token(jwt)?,
            false => decode(jwt)?,
        };

        self.check_missing_claims(&processed_jwt.claims)?;

        Ok(processed_jwt)
    }

    /// Decodes and validates the JWT's signature and optionally, the `exp` and `nbf` claims.
    fn decode_and_validate_token(&self, jwt: &str) -> Result<ProcessedJwt, JwtValidatorError> {
        let key_service = self
            .key_service
            .as_ref()
            .as_ref()
            .ok_or(JwtValidatorError::MissingKeyService)?;

        let header = decode_header(jwt).map_err(JwtValidatorError::DecodeHeader)?;

        // since we already initialized all the validators on startup, not finding one
        // for a certain algorithm means it's unsupported.
        let validation = self.validators.get(&header.alg).ok_or(
            JwtValidatorError::JwtSignedWithUnsupportedAlgorithm(header.alg),
        )?;

        let decoding_key = match header.kid {
            Some(kid) => key_service
                .get_key(&kid)
                .ok_or(JwtValidatorError::MissingDecodingKey(kid))?,
            None => unimplemented!("Handling JWTs without `kid`s hasn't been implemented yet."),
        };

        let decode_result = jsonwebtoken::decode::<TokenClaims>(jwt, decoding_key.key, validation)
            .map_err(|e| match e.kind() {
                jsonwebtoken::errors::ErrorKind::InvalidToken => JwtValidatorError::InvalidShape,
                jsonwebtoken::errors::ErrorKind::InvalidSignature => {
                    JwtValidatorError::InvalidSignature(e)
                },
                jsonwebtoken::errors::ErrorKind::ExpiredSignature => {
                    JwtValidatorError::ExpiredToken
                },
                jsonwebtoken::errors::ErrorKind::ImmatureSignature => {
                    JwtValidatorError::ImmatureToken
                },
                jsonwebtoken::errors::ErrorKind::Base64(decode_error) => {
                    JwtValidatorError::DecodeJwt(decode_error.to_string())
                },
                // the jsonwebtoken crate placed all it's errors onto a single enum, even the errors
                // that wouldn't be returned when we call `decode`.
                _ => JwtValidatorError::Unexpected(e),
            })?;

        Ok(ProcessedJwt {
            claims: decode_result.claims,
            key_iss: decoding_key.key_iss,
        })
    }

    fn check_missing_claims(&self, claims: &TokenClaims) -> Result<(), JwtValidatorError> {
        let missing_claims = self
            .config
            .required_claims
            .iter()
            .filter(|claim| claims.get(claim.as_ref()).is_none())
            .cloned()
            .collect::<Vec<Box<str>>>();

        if !missing_claims.is_empty() {
            Err(JwtValidatorError::MissingClaims(missing_claims))?
        }

        Ok(())
    }
}

/// Decodes a JWT without validating the signature.
fn decode(jwt: &str) -> Result<ProcessedJwt, JwtValidatorError> {
    // Split the token into its three parts
    let parts = jwt.split('.').collect::<Vec<&str>>();
    if parts.len() != 3 {
        return Err(JwtValidatorError::InvalidShape);
    }

    // Base64 decode the payload (the second part)
    let decoded_payload = BASE64_STANDARD_NO_PAD
        .decode(parts[1])
        .map_err(|e| JwtValidatorError::DecodeJwt(e.to_string()))?;

    // Deserialize the claims into a Value
    let claims = serde_json::from_slice::<TokenClaims>(&decoded_payload)
        .map_err(JwtValidatorError::DeserializeJwt)?;

    Ok(ProcessedJwt {
        claims,
        key_iss: None,
    })
}

#[derive(Debug, thiserror::Error)]
#[allow(dead_code)]
pub enum JwtValidatorError {
    #[error("JWT signature validation is on but no key service was provided.")]
    MissingKeyService,
    #[error("Invalid JWT format. The JWT must be in the shape: `header.payload.signature`")]
    InvalidShape,
    #[error("Failed to decode JWT Header: {0}")]
    DecodeHeader(#[source] jwt::errors::Error),
    #[error("Failed to decode JWT from Base64: {0}")]
    DecodeJwt(String),
    #[error("Failed to deserialize JWT from JSON string: {0}")]
    DeserializeJwt(#[from] serde_json::Error),
    #[error("The JWT was singed with an unsupported algorithm: {0:?}")]
    JwtSignedWithUnsupportedAlgorithm(Algorithm),
    #[error("No decoding key with the matching `kid` was found: {0}")]
    MissingDecodingKey(String),
    #[error("Failed validating the JWT's signature: {0}")]
    InvalidSignature(#[source] jwt::errors::Error),
    #[error("Token is expired")]
    ExpiredToken,
    #[error("Token was used before the timestamp indicated in the `nbf` claim")]
    ImmatureToken,
    #[error("An unexpected error occured while validating the JWT: {0}")]
    Unexpected(#[source] jwt::errors::Error),
    #[error("Validation failed since the JWT is missing the following required claims: {0:#?}")]
    MissingClaims(Vec<Box<str>>),
}
