//! Module wrapper to decode raw string token data to [`TokenPayload`]
//! We wrap [`TokenPayload`] to avoid accidentally assigning a variable to a neighboring cell

use crate::jwt::DecodeJwtError;
use crate::jwt::JwtService;
use crate::models::request::Request;
use crate::models::token_data::TokenPayload;

/// Wrapper around access token decode result
#[derive(Clone)]
pub(crate) struct AccessTokenData(pub TokenPayload);

/// Wrapper around id token decode result
#[derive(Clone)]
pub(crate) struct IdTokenData(pub TokenPayload);

/// Wrapper around userinfo token decode result
#[derive(Clone)]
pub(crate) struct UserInfoTokenData(pub TokenPayload);

/// Result of [`decode_tokens`] from [`Request`]
pub(crate) struct DecodedTokens {
    pub access_token: AccessTokenData,
    pub id_token: IdTokenData,
    pub userinfo: UserInfoTokenData,
}

/// Errors on decoding tokens from [`Request`]
#[derive(thiserror::Error, Debug)]
pub enum DecodeTokensError {
    /// Error encountered while decoding JWT token data
    #[error("could not decode access token: {0}")]
    AccessToken(DecodeJwtError),
    #[error("could not decode id token: {0}")]
    IdToken(DecodeJwtError),
    #[error("could not decode userinfo token: {0}")]
    UserInfoToken(DecodeJwtError),
}

/// Decode JWT tokens from [`Request`]
pub(crate) fn decode_tokens(
    request: &Request,
    jwt_service: &JwtService,
) -> Result<DecodedTokens, DecodeTokensError> {
    Ok(DecodedTokens {
        access_token: AccessTokenData(
            jwt_service
                .decode_token_data(request.access_token)
                .map_err(DecodeTokensError::AccessToken)?,
        ),
        id_token: IdTokenData(
            jwt_service
                .decode_token_data(request.id_token)
                .map_err(DecodeTokensError::IdToken)?,
        ),
        userinfo: UserInfoTokenData(
            jwt_service
                .decode_token_data(request.userinfo_token)
                .map_err(DecodeTokensError::UserInfoToken)?,
        ),
    })
}
