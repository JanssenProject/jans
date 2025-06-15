// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::http_utils::HttpError;
use super::key_service;
use super::status_list::UpdateStatusListError;
use super::validation::ValidateJwtError;

#[derive(Debug, thiserror::Error)]
pub enum JwtProcessingError {
    #[error("Failed to deserialize from Value to String: {0}")]
    StringDeserialization(#[from] serde_json::Error),
    #[error("failed to validate '{0}' token: {1}")]
    ValidateJwt(String, ValidateJwtError),
}

#[derive(Debug, thiserror::Error)]
pub enum JwtServiceInitError {
    #[error(
        "Failed to initialize Key Service for JwtService due to a conflictig config: both a local \
         JWKS and trusted issuers was provided."
    )]
    ConflictingJwksConfig,
    #[error(
        "Failed to initialize Key Service for JwtService due to a missing config: no local JWKS \
         or trusted issuers was provided."
    )]
    MissingJwksConfig,
    #[error("Encountered an unsupported algorithm in the config: {0}")]
    UnsupportedAlgorithm(String),
    #[error("failed to parse the openid_configuration_endpoint for the trusted issuer `{0}`: {1}")]
    ParseOidcUrl(String, url::ParseError),
    #[error("failed to prepare keys for the KeyService: {0}")]
    PrepareKeys(#[from] key_service::KeyServiceError),
    #[error(
        "the key service has no decoding keys. please provide a local JWKS or make sure your policy store has trustsed issuers"
    )]
    KeyServiceMissingKeys,
    #[error("failed to GET the openid configuration for the trusted issuers: {0}")]
    GetOpenidConfigurations(#[from] HttpError),
    #[error("failed to update JWT status list: {0}")]
    UpdateStatusList(#[from] UpdateStatusListError),
}
