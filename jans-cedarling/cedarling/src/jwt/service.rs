use jsonwebtoken::{decode, decode_header, DecodingKey, Header, Validation};
use std::sync::Arc;

use crate::{models::token_data::TokenPayload, JwtConfig};

use super::DecodeJwtError;

/// Service for JWT validation
#[derive(Clone)]
pub struct JwtService {
    config: Arc<JwtConfig>,
    key_service: Option<Arc<dyn KeyService>>,
}

pub trait KeyService: Send + Sync {
    fn get_key(&self) -> Result<DecodingKey, DecodeJwtError>;
}

impl JwtService {
    /// Create new JWT service
    pub fn new(config: JwtConfig, key_service: Option<Arc<dyn KeyService>>) -> Self {
        Self {
            config: Arc::new(config),
            key_service,
        }
    }

    /// Generic decoder for JWT
    #[allow(unused_variables)]
    pub fn decode<T: serde::de::DeserializeOwned>(&self, jwt: &str) -> Result<T, DecodeJwtError> {
        match self.config.as_ref() {
            JwtConfig::Disabled => decode_jwt_without_validation(jwt),
            JwtConfig::Enabled {
                signature_algorithms,
            } => self.decode_and_validate_jwt(jwt, signature_algorithms),
        }
    }

    /// Decode JWT to `TokenData`
    pub fn decode_token_data(&self, jwt: &str) -> Result<TokenPayload, DecodeJwtError> {
        self.decode(jwt)
    }

    // Decodes the JWT with validation
    fn decode_and_validate_jwt<T: serde::de::DeserializeOwned>(
        &self,
        jwt: &str,
        _signature_algorithms: &Vec<String>,
    ) -> Result<T, DecodeJwtError> {
        let header = extract_jwt_header(jwt).map_err(|_| DecodeJwtError::MalformedJWT)?;

        let validator = Validation::new(header.alg);

        // fetch decoding key from the KeyService
        let key_service = self
            .key_service
            .as_ref()
            .ok_or_else(|| DecodeJwtError::KeyServiceNotFound)?;
        let key = key_service.get_key()?;

        let claims = match decode::<T>(&jwt, &key, &validator) {
            Ok(data) => data.claims,
            Err(e) => return Err(e.into()),
        };

        Ok(claims)
    }
}

// Decodes the JWT header without validation
fn extract_jwt_header(jwt: &str) -> Result<Header, jsonwebtoken::errors::Error> {
    let header = decode_header(jwt)?;
    Ok(header)
}

// Decodes the JWT without validation
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
