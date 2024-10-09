use jsonwebtoken::{decode, decode_header, DecodingKey, Header, Validation};
use std::sync::Arc;

use crate::{models::token_data::TokenPayload, JwtConfig};

use super::DecodeJwtError;

// const SUPPORTED_ALGORITHMS: [&str; 1] = ["HS256"];

/// Service for JWT validation
#[derive(Clone)]
pub struct JwtService {
    config: Arc<JwtConfig>,
}

impl JwtService {
    /// Create new JWT service
    pub fn new(config: JwtConfig) -> Self {
        Self {
            config: Arc::new(config),
        }
    }

    /// Generic decoder for JWT
    #[allow(unused_variables)]
    pub fn decode<T: serde::de::DeserializeOwned>(&self, jwt: &str) -> Result<T, DecodeJwtError> {
        match self.config.as_ref() {
            JwtConfig::Disabled => decode_jwt_without_validation(jwt),
            JwtConfig::Enabled {
                signature_algorithms,
            } => todo!(),
        }
    }

    /// Decode JWT to `TokenData`
    pub fn decode_token_data(&self, jwt: &str) -> Result<TokenPayload, DecodeJwtError> {
        self.decode(jwt)
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
        Ok(token_data) => token_data.claims,
        Err(e) => match e.kind() {
            jsonwebtoken::errors::ErrorKind::InvalidToken => Err(DecodeJwtError::MalformedJWT)?,
            jsonwebtoken::errors::ErrorKind::InvalidSignature => {
                Err(DecodeJwtError::InvalidSignature)?
            },
            jsonwebtoken::errors::ErrorKind::InvalidEcdsaKey => Err(DecodeJwtError::InvalidKey)?,
            jsonwebtoken::errors::ErrorKind::InvalidRsaKey(_) => Err(DecodeJwtError::InvalidKey)?,
            jsonwebtoken::errors::ErrorKind::ExpiredSignature => {
                Err(DecodeJwtError::ExpiredSignature)?
            },
            jsonwebtoken::errors::ErrorKind::InvalidIssuer => Err(DecodeJwtError::InvalidIssuer)?,
            jsonwebtoken::errors::ErrorKind::InvalidAudience => {
                Err(DecodeJwtError::InvalidAudience)?
            },
            jsonwebtoken::errors::ErrorKind::Base64(decode_error) => Err(
                DecodeJwtError::UnableToDecodeBase64(decode_error.to_string()),
            )?,
            jsonwebtoken::errors::ErrorKind::Json(arc) => {
                Err(DecodeJwtError::UnableToParseJson(arc.clone()))?
            },
            jsonwebtoken::errors::ErrorKind::Utf8(err) => {
                Err(DecodeJwtError::UnableToString(err.clone()))?
            },
            _ => Err(DecodeJwtError::Unexpected(e.to_string()))?,
        },
    };

    Ok(claims)
}
