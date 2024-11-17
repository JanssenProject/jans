mod config;
#[cfg(test)]
mod test;
#[cfg(test)]
mod test_utils;

use super::new_key_service::NewKeyService;
use base64::prelude::*;
pub use config::*;
use jsonwebtoken::{self as jwt};
use jsonwebtoken::{decode_header, Algorithm, Validation};
use serde_json::Value;
use std::collections::HashSet;
use std::rc::Rc;

type IssuerId = String;
type TokenClaims = Value;

/// Validates Json Web Tokens.
#[allow(dead_code)]
struct JwtValidator {
    config: JwtValidatorConfig,
    key_service: Rc<NewKeyService>,
}

#[allow(dead_code)]
impl JwtValidator {
    pub fn new(config: JwtValidatorConfig, key_service: Rc<NewKeyService>) -> Self {
        Self {
            config,
            key_service,
        }
    }

    /// Decodes the JWT and optionally validates it depending on the config.
    pub fn process_jwt(&self, jwt: &str) -> Result<TokenClaims, JwtValidatorError> {
        let token_claims = match *self.config.sig_validation {
            true => decode_and_validate_token(jwt, &self.config.algs_supported, &self.key_service)?,
            false => decode(jwt)?,
        };

        Ok(check_missing_claims(
            token_claims,
            &self.config.required_claims,
        )?)
    }
}

/// Decodes a JWT without validating the signature.
fn decode(jwt: &str) -> Result<TokenClaims, JwtValidatorError> {
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

    Ok(claims)
}

/// Decodes and validates the JWT's signature and optionally, the `exp` and `nbf` claims.
fn decode_and_validate_token(
    jwt: &str,
    supported_algs: &HashSet<Algorithm>,
    key_service: &NewKeyService,
) -> Result<TokenClaims, JwtValidatorError> {
    let header = decode_header(jwt).map_err(JwtValidatorError::DecodeHeader)?;

    // reject unsupported algorithms early
    if !supported_algs.contains(&header.alg) {
        return Err(JwtValidatorError::JwtSignedWithUnsupportedAlgorithm(
            header.alg,
        ));
    }

    // PERF: create multiple `Validation`s on startup then just reuse them.
    let mut validation = Validation::new(header.alg);

    // these settings should only validate the `exp` and `nbf` if the
    // claim is in the token -- otherwise, they shouldn't do anything.
    validation.validate_exp = true;
    validation.validate_nbf = true;

    // we will validate the missing claims in another function but this
    // defaults to true so we need to set it to false.
    validation.required_spec_claims.clear();
    validation.validate_aud = false;

    let decoding_key = match header.kid {
        Some(kid) => key_service
            .get_key(&kid)
            .ok_or(JwtValidatorError::MissingDecodingKey(kid))?,
        None => unimplemented!("Handling JWTs without `kid`s hasn't been implemented yet."),
    };

    let decode_result = jsonwebtoken::decode::<TokenClaims>(jwt, decoding_key, &validation);

    match decode_result {
        Ok(token_data) => Ok(token_data.claims),
        Err(err) => match err.kind() {
            jsonwebtoken::errors::ErrorKind::InvalidToken => Err(JwtValidatorError::InvalidShape),
            jsonwebtoken::errors::ErrorKind::InvalidSignature => {
                Err(JwtValidatorError::InvalidSignature(err))
            },
            jsonwebtoken::errors::ErrorKind::ExpiredSignature => {
                Err(JwtValidatorError::ExpiredToken)
            },
            jsonwebtoken::errors::ErrorKind::ImmatureSignature => {
                Err(JwtValidatorError::ImmatureToken)
            },
            jsonwebtoken::errors::ErrorKind::Base64(decode_error) => {
                Err(JwtValidatorError::DecodeJwt(decode_error.to_string()))
            },
            // the jsonwebtoken crate placed all it's errors onto a single enum, even the errors
            // that wouldn't be returned when we call `decode`.
            _ => Err(JwtValidatorError::Unexpected(err)),
        },
    }
}

fn check_missing_claims(
    claims: TokenClaims,
    required_claims: &HashSet<Box<str>>,
) -> Result<TokenClaims, JwtValidatorError> {
    let missing_claims = required_claims
        .iter()
        .filter(|claim| claims.get(claim.as_ref()).is_none())
        .cloned()
        .collect::<Vec<Box<str>>>();

    if missing_claims.len() > 0 {
        Err(JwtValidatorError::MissingClaims(missing_claims))?
    }

    Ok(claims)
}

#[derive(Debug, thiserror::Error)]
#[allow(dead_code)]
pub enum JwtValidatorError {
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
