use base64::prelude::*;
use std::sync::Arc;

use crate::{models::token_data::TokenPayload, JwtConfig};

use super::DecodeJwtError;

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

// decode JWT without validation when in config disabled value
fn decode_jwt_without_validation<T: serde::de::DeserializeOwned>(
    jwt: &str,
) -> Result<T, DecodeJwtError> {
    let payload_base64 = jwt.split('.').nth(1).ok_or(DecodeJwtError::MalformedJWT)?;
    let payload_json = BASE64_STANDARD_NO_PAD
        .decode(payload_base64)
        .map_err(|err| DecodeJwtError::UnableToDecodeBase64(err, payload_base64.to_owned()))?;

    let payload_json = String::from_utf8(payload_json)?;
    serde_json::from_str(payload_json.as_str())
        .map_err(|err| DecodeJwtError::UnableToParseJson(err, payload_json.to_owned()))
}
