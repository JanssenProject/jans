// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use thiserror::Error;
use url::Url;

use crate::jwt::{decode::DecodeJwtError, http_utils::HttpError, validation::ValidateJwtError};

#[derive(Debug, Error)]
pub enum ParseStatusListError {
    #[error("invalid `bit` size in the status list. expected 1, 2, 4, or 8 but got: {0}")]
    InvalidBitSize(u8),
    #[error("failed to decode status list: {0}")]
    Decode(#[from] base64::DecodeError),
    #[error("failed to read status list: {0}")]
    Read(#[from] std::io::Error),
    #[error("failed to deserialize status list JWT: {0}")]
    DeserializeJwt(#[from] reqwest::Error),
    #[error("the status list JWT is missing the 'lst' claim")]
    JwtMissingListClaim,
    #[error("the status list JWT is missing the 'bits' claim")]
    JwtMissingBitsClaim,
    #[error("the status list JWT's 'bits' claim is not a valid u64: {0:?}")]
    JwtInvalidBitsType(serde_json::Value),
}

#[derive(Debug, Error)]
pub enum JwtStatusError {
    #[error("failed to deserialze jwt status list: {0}")]
    DeserializeStatusList(#[from] serde_json::Error),
    #[error("the `uri` of the `status_list` claim is invalid: {0}")]
    InvalidStatusListUri(#[from] url::ParseError),
    #[error("the `idx` of the `status_list` claim is out of bounds")]
    StatusListIdxOutOfBounds,
    #[error("status list for {0} is missing")]
    MissingStatusList(Url),
    #[error("status list for {0} needs to be updated")]
    ExpiredStatusList(Url),
}

#[derive(Debug, Error)]
pub enum UpdateStatusListError {
    #[error("the issuer config is missing an OpenIdConfig")]
    MissingOpenIdConfig,
    #[error("the openid configuration does not include a 'status_list_endpoint'")]
    MissingStatusListUri,
    #[error("failed to decode the status list JWT: {0}")]
    DecodeStatusListJwt(#[from] DecodeJwtError),
    #[error("missing validation key for the statuslist JWT from '{0}'")]
    MissingValidationKey(String),
    #[error("missing validator for the statuslist JWT from '{0}'")]
    MissingValidator(String),
    #[error("failed to validate statuslist JWT: {0}")]
    ValidateJwtError(#[from] ValidateJwtError),
    #[error("failed to parse status list: {0}")]
    ParseStatusList(#[from] ParseStatusListError),
    #[error("failed to get status list JWT: {0}")]
    GetStatusListJwt(#[source] HttpError),
}
