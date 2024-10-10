/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */
//! # JWT Engine
//! Part of Cedarling that main purpose is:
//! - validate JWT signature
//! - validate JWT status
//! - extract JWT claims
use base64::prelude::*;
use std::{string::FromUtf8Error, sync::Arc};

use crate::models::{jwt_config::JwtConfig, token_data::TokenPayload};

/// Service for JWT validation
#[derive(Clone)]
pub(crate) struct JwtService {
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

/// Error type for JWT decoding
#[derive(thiserror::Error, Debug)]
pub enum DecodeJwtError {
    /// JWT is malformed
    #[error("Malformed JWT provided")]
    MalformedJWT,
    /// Base64 value in payload is malformed
    #[error("Unable to decode base64 JWT value: {0}, payload: {1}")]
    UnableToDecodeBase64(base64::DecodeError, String),
    /// Unmarshaled JWT payload base64 value is not valid UTF-8
    #[error("Unable to convert decoded base64 JWT value to string: {0}")]
    UnableToString(#[from] FromUtf8Error),
    /// Unable to parse json in unmarshaled JWT payload string value
    #[error("Unable to parse JWT JSON data: {0}, payload: {1}")]
    UnableToParseJson(serde_json::Error, String),
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
