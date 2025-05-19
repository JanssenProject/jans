// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

mod config;
mod decode;
#[cfg(test)]
mod test;

pub use config::*;

use std::collections::HashMap;
use std::sync::Arc;

use super::new_key_service::NewKeyService;
use super::status_list_service::JwtStatusError;
use super::{issuers_store::TrustedIssuersStore, new_key_service::DecodingKeyInfo};
use crate::common::policy_store::TrustedIssuer;
use base64::prelude::*;
use decode::*;
use jsonwebtoken::{self as jwt, Algorithm, Validation};
use serde::Deserialize;
use serde_json::Value;

type IssuerId = String;
type TokenClaims = Value;

/// Validates Json Web Tokens.
pub struct JwtValidator {
    config: JwtValidatorConfig,
    key_service: Arc<NewKeyService>,
    validators: HashMap<Algorithm, Validation>,
    iss_store: TrustedIssuersStore,
}

#[derive(Debug, PartialEq, Deserialize)]
pub struct ValidatedJwt<'a> {
    #[serde(flatten)]
    pub claims: TokenClaims,
    #[serde(skip)]
    pub trusted_iss: Option<&'a TrustedIssuer>,
}

impl JwtValidator {
    pub fn new(config: JwtValidatorConfig, key_service: Arc<NewKeyService>) -> Self {
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

        Self {
            config,
            key_service,
            validators,
            iss_store,
        }
    }

    /// Decodes the JWT and optionally validates it depending on the config.
    pub fn validate_jwt(&self, jwt: &str) -> Result<ValidatedJwt, ValidateJwtError> {
        let decoded_jwt = decode_jwt(jwt)?;

        if self.config.sig_validation {
            let mut sig_validated_jwt = self.validate_jwt_sig(jwt, &decoded_jwt)?;
            self.validate_jwt_claims(&sig_validated_jwt)?;

            // TODO: this stinks
            sig_validated_jwt.trusted_iss =
                decoded_jwt.iss().and_then(|iss| self.iss_store.get(iss));

            return Ok(sig_validated_jwt);
        }

        // TODO: this stinks
        let iss = decoded_jwt.iss().and_then(|iss| self.iss_store.get(iss));
        let mut validated_jwt: ValidatedJwt = decoded_jwt.into();
        validated_jwt.trusted_iss = iss;

        Ok(validated_jwt)
    }

    fn validate_jwt_sig(
        &self,
        jwt_str: &str,
        decoded_jwt: &DecodedJwt,
    ) -> Result<ValidatedJwt, ValidateJwtError> {
        dbg!(decoded_jwt);
        let key_info = decoded_jwt.get_decoding_key_info();
        dbg!(&key_info);
        #[cfg(test)]
        dbg!(self.key_service.keys());
        let key = self
            .key_service
            .get_key(&key_info)
            .ok_or(ValidateJwtError::MissingValidationKey)?;
        let validation = self
            .validators
            .get(&decoded_jwt.header.alg)
            .ok_or(ValidateJwtError::MissingValidator)?;
        let validated_jwt = jwt::decode::<ValidatedJwt>(jwt_str, key, validation)?;

        return Ok(validated_jwt.claims);
    }

    fn validate_jwt_claims(
        &self,
        sig_validated_jwt: &ValidatedJwt,
    ) -> Result<(), ValidateJwtError> {
        let missing_claims = self
            .config
            .required_claims
            .iter()
            .filter(|claim| sig_validated_jwt.claims.get(claim.as_ref()).is_none())
            .cloned()
            .collect::<Vec<Box<str>>>();

        if !missing_claims.is_empty() {
            Err(ValidateJwtError::MissingClaims(missing_claims))?
        }

        Ok(())
    }

    /// Returns `true` if the JWT can be used and `false` if not.
    ///
    /// TODO: application-specific statuses are always allowed since we do not have a
    /// way to map them yet.
    fn validate_jwt_status(&self, _jwt: &ValidatedJwt) -> Result<bool, JwtValidatorError> {
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
    fn decode_jwt(&self, jwt: &str) -> Result<ValidatedJwt, JwtValidatorError> {
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

        Ok(ValidatedJwt {
            claims,
            trusted_iss,
        })
    }
}

impl DecodedJwt {
    pub fn iss(&self) -> Option<&str> {
        self.claims.claims.get("iss").and_then(|x| x.as_str())
    }

    pub fn get_decoding_key_info(&self) -> DecodingKeyInfo {
        DecodingKeyInfo {
            issuer: self.iss().map(|x| x.to_string()),
            kid: self.header.kid.clone(),
            algorithm: self.header.alg,
        }
    }
}

impl From<DecodedJwt> for ValidatedJwt<'_> {
    fn from(decoded_jwt: DecodedJwt) -> Self {
        Self {
            claims: decoded_jwt.claims.claims,
            trusted_iss: None,
        }
    }
}

#[derive(Debug, thiserror::Error)]
pub enum ValidateJwtError {
    #[error("failed to decode the JWT: {0}")]
    DecodeJwt(#[from] DecodeJwtError),
    #[error("failed to validate the JWT since no key was available")]
    MissingValidationKey,
    #[error("failed to validate the JWT because no validator was initialized for its issuer")]
    MissingValidator,
    #[error("failed to validate the JWT: {0}")]
    ValidateJwt(#[from] jwt::errors::Error),
    #[error("validation failed since the JWT is missing the following required claims: {0:#?}")]
    MissingClaims(Vec<Box<str>>),
}

#[deprecated]
#[derive(Debug, thiserror::Error)]
pub enum JwtValidatorError {
    #[error("JWT signature validation is on but the key service has no keys")]
    KeyServiceIsMissingKeys,
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
