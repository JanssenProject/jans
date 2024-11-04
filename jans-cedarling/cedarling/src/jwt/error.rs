/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::decoding_strategy;

/// Error type for the JWT service.
#[derive(thiserror::Error, Debug)]
#[allow(clippy::enum_variant_names)]
pub enum JwtServiceError {
    /// Error indicating that the provided access token is invalid.
    ///
    /// This occurs when the given access token fails validation, such as when
    /// it has an invalid signature, has expired, or contains incorrect claims.
    #[error("The `access_token` is invalid or has failed validation: {0}")]
    InvalidAccessToken(#[source] decoding_strategy::JwtDecodingError),

    /// Error indicating that the provided id token is invalid.
    ///
    /// This occurs when the given id token fails validation, such as when
    /// it has an invali d signature, has expired, or contains incorrect claims.
    #[error("The `id_token` is invalid or has failed validation: {0}")]
    InvalidIdToken(#[source] decoding_strategy::JwtDecodingError),

    /// Error indicating that the provided userinfo token is invalid.
    ///
    /// This occurs when the given userinfo token fails validation, such as when
    /// it has an invalid signature, has expired, or contains incorrect claims.
    #[error("The `userinfo_token` is invalid or has failed validation: {0}")]
    InvalidUserinfoToken(#[source] decoding_strategy::JwtDecodingError),
}
