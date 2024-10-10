/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use jsonwebtoken::{decode, decode_header, DecodingKey, Header, Validation};
use std::sync::Arc;

use crate::{models::token_data::TokenPayload, JwtConfig};

use super::{key_service::KeyService, DecodeJwtError};

/// Service for JWT validation.
///
/// The `JwtService` is responsible for decoding and validating JSON Web Tokens (JWTs).
/// It can operate in two modes: with validation, using a specified key service for key retrieval,
/// and without validation, for cases where the token's validity is not required.
#[derive(Clone)]
pub struct JwtService {
    config: Arc<JwtConfig>,
    key_service: Option<Arc<dyn KeyService>>,
}

impl JwtService {
    /// Creates a new instance of `JwtService`.
    ///
    /// If `CEDARLING_JWT_VALIDATION` is set to `Enabled` in the
    /// bootstrap properties `key_service` cannot not be `None`.
    pub fn new(config: JwtConfig, key_service: Option<Arc<dyn KeyService>>) -> Self {
        Self {
            config: Arc::new(config),
            key_service,
        }
    }

    /// Decodes a JWT, optionally validating it based on the configuration settings.
    pub fn decode<T: serde::de::DeserializeOwned>(&self, jwt: &str) -> Result<T, DecodeJwtError> {
        match self.config.as_ref() {
            JwtConfig::Disabled => decode_jwt_without_validation(jwt),
            JwtConfig::Enabled {
                signature_algorithms,
            } => self.decode_and_validate_jwt(jwt, signature_algorithms),
        }
    }

    /// Decodes a JWT and returns the `TokenPayload`.
    pub fn decode_token_data(&self, jwt: &str) -> Result<TokenPayload, DecodeJwtError> {
        self.decode(jwt)
    }

    /// Decodes and validates a JWT using the specified algorithms and keys.
    fn decode_and_validate_jwt<T: serde::de::DeserializeOwned>(
        &self,
        jwt: &str,
        // TODO: figure out if `signature_algorithms` still needed since
        // the algorithms is defined in the JWT header anyways
        _signature_algorithms: &Vec<String>,
    ) -> Result<T, DecodeJwtError> {
        let header = extract_jwt_header(jwt).map_err(|_| DecodeJwtError::MalformedJWT)?;

        let validator = Validation::new(header.alg);

        // fetch decoding key from the KeyService
        let key_service = self
            .key_service
            .as_ref()
            .ok_or_else(|| DecodeJwtError::KeyServiceNotFound)?;
        let key = key_service
            .get_key(&header.kid.ok_or(DecodeJwtError::MissingKeyId)?)
            .map_err(|e| DecodeJwtError::KeyNotFound(e))?;

        let claims = match decode::<T>(&jwt, &key, &validator) {
            Ok(data) => data.claims,
            Err(e) => return Err(e.into()),
        };

        Ok(claims)
    }
}

/// Extracts the JWT header from the given JWT string without validation.
fn extract_jwt_header(jwt: &str) -> Result<Header, jsonwebtoken::errors::Error> {
    let header = decode_header(jwt)?;
    Ok(header)
}

/// Decodes a JWT without performing any validation.
fn decode_jwt_without_validation<T: serde::de::DeserializeOwned>(
    jwt: &str,
) -> Result<T, DecodeJwtError> {
    let header = extract_jwt_header(jwt).map_err(|_| DecodeJwtError::MalformedJWT)?;

    let mut validator = Validation::new(header.alg);
    validator.insecure_disable_signature_validation();
    validator.validate_exp = false;
    validator.validate_aud = false;

    let key = DecodingKey::from_secret("secret".as_ref());

    let claims = match decode::<T>(&jwt, &key, &validator) {
        Ok(data) => data.claims,
        Err(e) => return Err(e.into()),
    };

    Ok(claims)
}
