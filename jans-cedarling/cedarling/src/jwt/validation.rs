// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

mod config;
mod decode;
#[cfg(test)]
mod test;

use std::collections::{HashMap, HashSet};
use std::sync::Arc;

use super::key_service::DecodingKeyInfo;
use super::key_service::KeyService;
use super::log_entry::JwtLogEntry;
use crate::common::policy_store::{TokenEntityMetadata, TrustedIssuer};
use crate::log::Logger;
use crate::{JwtConfig, LogWriter};
use decode::*;
use jsonwebtoken::{self as jwt, Algorithm, DecodingKey, Validation};
use serde::Deserialize;
use serde_json::Value;

type IssuerId = String;
type TokenClaims = Value;

/// Manages initialized validators
pub struct JwtValidationService {
    validators: HashMap<ValidatorKey, JwtValidator>,
    key_service: Arc<KeyService>,
    validate_jwt_signatures: bool,
    trusted_issuers: Option<HashMap<String, TrustedIssuer>>,
}

#[derive(Debug, Hash, Eq, PartialEq)]
pub struct ValidatorKey {
    iss: Option<String>,
    token_name: String,
    algorithm: Algorithm,
}

impl JwtValidationService {
    pub fn new(
        config: &JwtConfig,
        trusted_issuers: Option<HashMap<String, TrustedIssuer>>,
        key_service: KeyService,
        logger: Option<Logger>,
    ) -> Self {
        // we make a map of <jwt_iss_claim, TrustedIssuer>
        // where jwt_iss_claim is what we expect to find in the `iss` claim of the JWTs
        // from the given issuer
        let trusted_issuers = trusted_issuers.map(|issuers| {
            issuers
                .into_values()
                .map(|iss| {
                    // TODO: here, we are relying on the IDP to use it's own domain
                    // for the issued tokens. We might need to fetch the openid config
                    // in the future and use the `issuer` field to support more funky IDPs.
                    let jwt_iss_claim = iss.oidc_endpoint.origin().ascii_serialization();
                    (jwt_iss_claim, iss)
                })
                .collect::<HashMap<String, TrustedIssuer>>()
        });
        let key_service = Arc::new(key_service);

        let mut validators = HashMap::default();
        if let Some(issuers) = trusted_issuers.as_ref() {
            for iss in issuers.values() {
                init_validators_for_iss(&mut validators, config, iss, logger.clone());
            }
        }

        Self {
            validators,
            key_service,
            validate_jwt_signatures: config.jwt_sig_validation,
            trusted_issuers,
        }
    }

    pub fn validate_jwt(
        &self,
        token_name: String,
        jwt: &str,
    ) -> Result<ValidatedJwt, ValidateJwtError> {
        let decoded_jwt = decode_jwt(jwt)?;

        if !self.validate_jwt_signatures {
            // The users of the validated JWT will need a reference to the TrustedIssuer
            // to do some processing so we include it here for convenience
            let issuer_ref = decoded_jwt.iss().and_then(|iss| self.get_issuer_ref(iss));
            return Ok(ValidatedJwt {
                claims: decoded_jwt.claims.inner,
                trusted_iss: issuer_ref,
            });
        }

        // Get decoding key
        let decoding_key_info = decoded_jwt.decoding_key_info();
        let decoding_key = self
            .key_service
            .get_key(&decoding_key_info)
            .ok_or(ValidateJwtError::MissingValidationKey)?;

        // get validator
        let validator_key = ValidatorKey {
            iss: decoded_jwt.iss().map(|x| x.to_string()),
            token_name,
            algorithm: decoded_jwt.header.alg,
        };
        let validator =
            self.validators
                .get(&validator_key)
                .ok_or(ValidateJwtError::MissingValidator(
                    decoded_jwt.iss().map(|s| s.to_string()),
                ))?;

        // validate JWT
        let mut validated_jwt = validator.validate_jwt(jwt, decoding_key)?;

        // The users of the validated JWT will need a reference to the TrustedIssuer
        // to do some processing so we include it here for convenience
        validated_jwt.trusted_iss = decoded_jwt.iss().and_then(|iss| self.get_issuer_ref(iss));

        return Ok(validated_jwt);
    }

    /// Uses the `iss` claim of a token to retrieve a reference to a [`TrustedIssuer`]
    #[inline]
    fn get_issuer_ref(&self, iss: &str) -> Option<&TrustedIssuer> {
        self.trusted_issuers
            .as_ref()
            .and_then(|issuers| issuers.get(iss))
    }
}

#[derive(Debug, PartialEq, Deserialize)]
pub struct ValidatedJwt<'a> {
    #[serde(flatten)]
    pub claims: TokenClaims,
    #[serde(skip)]
    pub trusted_iss: Option<&'a TrustedIssuer>,
}

/// This struct is a wrapper over [`jsonwebtoken::Validation`] which implements an
/// additional check for requiring custom JWT claims.
struct JwtValidator {
    validation: Validation,
    required_claims: HashSet<Box<str>>,
}

impl JwtValidator {
    fn new(
        iss: Option<String>,
        token_name: String,
        token_metadata: &TokenEntityMetadata,
        algorithm: Algorithm,
    ) -> (Self, ValidatorKey) {
        let mut validation = Validation::new(algorithm);
        validation.validate_exp = token_metadata.required_claims.contains("exp");
        validation.validate_nbf = token_metadata.required_claims.contains("nbf");

        // we will validate the missing claims in another function since the
        // jsonwebtoken crate does not support required custom claims
        // ... but this defaults to true so we need to set it to false.
        validation.required_spec_claims.clear();
        validation.validate_aud = false;

        let required_claims = token_metadata
            .required_claims
            .iter()
            .cloned()
            .map(|s| s.into_boxed_str())
            .collect();

        let key = ValidatorKey {
            iss,
            token_name,
            algorithm,
        };

        let validator = JwtValidator {
            validation,
            required_claims,
        };

        (validator, key)
    }

    fn validate_jwt(
        &self,
        jwt: &str,
        decoding_key: &DecodingKey,
    ) -> Result<ValidatedJwt, ValidateJwtError> {
        let validated_jwt =
            jwt::decode::<ValidatedJwt>(jwt, decoding_key, &self.validation)?.claims;

        // Custom implementation of requiring custom claims
        let missing_claims = self
            .required_claims
            .iter()
            .filter(|claim| validated_jwt.claims.get(claim.as_ref()).is_none())
            .cloned()
            .collect::<Vec<Box<str>>>();
        if !missing_claims.is_empty() {
            Err(ValidateJwtError::MissingClaims(missing_claims))?
        }

        return Ok(validated_jwt);
    }
}

/// Initializes the necessary JWT validators for the given issuer
fn init_validators_for_iss(
    validators: &mut HashMap<ValidatorKey, JwtValidator>,
    config: &JwtConfig,
    iss: &TrustedIssuer,
    logger: Option<Logger>,
) {
    // TODO: it's better to get the OIDC and get the issuer there
    let origin = iss.oidc_endpoint.origin().ascii_serialization();

    for (token_name, metadata) in iss.token_metadata.iter() {
        if !metadata.trusted {
            if let Some(logger) = logger.as_ref() {
                logger.log_any(JwtLogEntry::system(format!(
                    "skipping metadata for '{token_name}' from '{origin}' since `trusted == false`"
                )));
            }
            continue;
        }

        for algorithm in config.signature_algorithms_supported.iter().copied() {
            let (validator, key) = JwtValidator::new(
                Some(origin.clone()),
                token_name.clone(),
                metadata,
                algorithm,
            );
            validators.insert(key, validator);
        }
    }
}

impl DecodedJwt {
    pub fn iss(&self) -> Option<&str> {
        self.claims.inner.get("iss").and_then(|x| x.as_str())
    }

    pub fn decoding_key_info(&self) -> DecodingKeyInfo {
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
            claims: decoded_jwt.claims.inner,
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
    #[error("failed to validate the JWT from '{0:?}' since it's not a trusted issuer")]
    MissingValidator(Option<String>),
    #[error("failed to validate the JWT: {0}")]
    ValidateJwt(#[from] jwt::errors::Error),
    #[error("validation failed since the JWT is missing the following required claims: {0:#?}")]
    MissingClaims(Vec<Box<str>>),
}
