// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

mod config;
#[cfg(test)]
mod test;

use std::collections::HashMap;
use std::sync::Arc;

use base64::prelude::*;
pub use config::*;
use jsonwebtoken::{self as jwt, Algorithm, Validation, decode_header};
use serde_json::Value;
use url::Url;

use super::issuers_store::TrustedIssuersStore;
use super::key_service::KeyService;
use super::status_list_service::JwtStatusError;
use crate::common::policy_store::TrustedIssuer;

type IssuerId = String;
type TokenClaims = Value;

/// Validates Json Web Tokens.
pub struct JwtValidator {
    config: JwtValidatorConfig,
    key_service: Option<Arc<KeyService>>,
    validators: HashMap<Algorithm, Validation>,
    iss_store: TrustedIssuersStore,
}

#[derive(Debug, PartialEq)]
pub struct ProcessedJwt<'a> {
    pub claims: TokenClaims,
    pub trusted_iss: Option<&'a TrustedIssuer>,
}

impl JwtValidator {
    pub fn new(
        config: JwtValidatorConfig,
        key_service: Option<Arc<KeyService>>,
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

        let iss_store = TrustedIssuersStore::new(config.trusted_issuers.clone());

        Ok(Self {
            config,
            key_service,
            validators,
            iss_store,
        })
    }

    /// Decodes the JWT and optionally validates it depending on the config.
    pub fn validate_jwt<'a>(&'a self, jwt: &'a str) -> Result<ProcessedJwt<'a>, JwtValidatorError> {
        let jwt = self.validate_jwt_sig(jwt)?;
        self.validate_jwt_claims(&jwt)?;
        // if !self.validate_jwt_status(&jwt)? {
        //     todo!("add logic for invalid status")
        // };

        Ok(jwt)
    }

    fn validate_jwt_sig<'a>(&'a self, jwt: &'a str) -> Result<ProcessedJwt<'a>, JwtValidatorError> {
        let processed_jwt = match *self.config.sig_validation {
            true => self.decode_and_validate_jwt(jwt)?,
            false => self.decode_jwt(jwt)?,
        };

        Ok(processed_jwt)
    }

    fn validate_jwt_claims(&self, processed_jwt: &ProcessedJwt) -> Result<(), JwtValidatorError> {
        let missing_claims = self
            .config
            .required_claims
            .iter()
            .filter(|claim| processed_jwt.claims.get(claim.as_ref()).is_none())
            .cloned()
            .collect::<Vec<Box<str>>>();

        if !missing_claims.is_empty() {
            Err(JwtValidatorError::MissingClaims(missing_claims))?
        }

        // Check if the `iss` claim's scheme is `https`
        if self.config.required_claims.contains("iss") {
            let iss = processed_jwt
                .claims
                .get("iss")
                .map(|iss| serde_json::from_value::<String>(iss.clone()))
                .transpose()
                .map_err(JwtValidatorError::DeserializeJwt)?
                .ok_or(JwtValidatorError::MissingClaims(vec!["iss".into()]))?;
            let url = Url::parse(&iss)?;
            if url.scheme() != "https" {
                Err(JwtValidatorError::InvalidIssScheme(url.scheme().into()))?
            }
        }

        Ok(())
    }

    /// Returns `true` if the JWT can be used and `false` if not.
    ///
    /// TODO: application-specific statuses are always allowed since we do not have a
    /// way to map them yet.
    fn validate_jwt_status(&self, _jwt: &ProcessedJwt) -> Result<bool, JwtValidatorError> {
        todo!()
        // let Some(status_list_service) = self.status_list_service.as_ref() else {
        //     return Ok(true);
        // };
        //
        // match status_list_service.get_status(jwt)? {
        //     JwtStatus::Valid => Ok(true),
        //     JwtStatus::Invalid | JwtStatus::Suspended => Ok(false),
        //     // Application specific. We will just allow all custom statuses for now while
        //     // we do not have a way to map custom statuses yet in Cedarling.
        //     JwtStatus::Custom(_) => Ok(true),
        // }
    }

    /// Decodes a JWT without validating the signature.
    fn decode_jwt(&self, jwt: &str) -> Result<ProcessedJwt, JwtValidatorError> {
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

        // fetch the trusted issuer using the `iss` claim
        let trusted_iss = claims
            .get("iss")
            .map(|x| serde_json::from_value::<String>(x.clone()))
            .transpose()
            .map_err(JwtValidatorError::DeserializeJwt)?
            .and_then(|x| self.iss_store.get(&x));

        Ok(ProcessedJwt {
            claims,
            trusted_iss,
        })
    }

    /// Decodes and validates the JWT's signature and optionally, the `exp` and `nbf` claims.
    fn decode_and_validate_jwt(&self, jwt: &str) -> Result<ProcessedJwt, JwtValidatorError> {
        let key_service = self
            .key_service
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
            .map_err(|e| {
                match e.kind() {
                    jsonwebtoken::errors::ErrorKind::InvalidToken => {
                        JwtValidatorError::InvalidShape
                    },
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
                }
            })?;

        Ok(ProcessedJwt {
            claims: decode_result.claims,
            trusted_iss: decoding_key.key_iss,
        })
    }
}

#[derive(Debug, thiserror::Error)]
pub enum JwtValidatorError {
    #[error("JWT signature validation is on but no key service was provided.")]
    MissingKeyService,
    #[error("JWT status validation is on but no status list service was provided.")]
    MissingStatusListService,
    #[error("Invalid JWT format. The JWT must be in the shape: `header.payload.signature`")]
    InvalidShape,
    #[error("Failed to decode JWT Header: {0}")]
    DecodeHeader(#[source] jwt::errors::Error),
    #[error("Failed to decode JWT from Base64: {0}")]
    DecodeJwt(String),
    #[error("Failed to deserialize JWT from JSON string: {0}")]
    DeserializeJwt(#[source] serde_json::Error),
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
    #[error("Failed to parse URL: {0}")]
    ParseUrl(#[from] url::ParseError),
    #[error(
        "The `iss` claim on the token has an invalid scheme: `{0}`. The scheme must be `https`"
    )]
    InvalidIssScheme(String),
    #[error("failed to validate jwt status: {0}")]
    JwtStatus(#[from] JwtStatusError),
}
