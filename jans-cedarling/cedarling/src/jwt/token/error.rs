/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use jsonwebtoken::Algorithm;

/// Represents the different kinds of errors that can occur during JWT token validation.
///
/// The `Error` enum encapsulates various errors that can happen when handling
/// tokens, such as key decoding failures, unsupported algorithms, and token validation issues.
#[derive(Debug, PartialEq, thiserror::Error)]
pub enum Error {
    /// Occurs when there is a problem decoding the key.
    ///
    /// This error is triggered when the JWT decoding process encounters an issue with
    /// the provided key (e.g., incorrect format or invalid key type).
    ///
    /// - `0`: The underlying error from the `jsonwebtoken` crate.
    #[error("There was a problem decoding the key: {0}")]
    KeyError(jsonwebtoken::errors::Error),

    /// Indicates that an unsupported algorithm was used for the token.
    ///
    /// This error is returned when the token's algorithm does not match the ones
    /// supported by the validation logic.
    ///
    /// - `0`: The unsupported `Algorithm`.
    #[error("Unsupported algorithm: {0:?}")]
    UnsupportedAlgorithm(Algorithm),

    /// Occurs when the token validation process fails.
    ///
    /// This error captures validation failures, such as signature mismatches,
    /// expired tokens, or incorrect claims. The error includes detailed information
    /// from the `jsonwebtoken` crate regarding the validation failure.
    ///
    /// - `0`: The underlying validation error from the `jsonwebtoken` crate.
    #[error("Error validating token: {0}")]
    ValidationError(jsonwebtoken::errors::Error),
}
