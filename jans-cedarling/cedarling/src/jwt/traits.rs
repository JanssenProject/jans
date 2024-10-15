/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::Error;
use jsonwebtoken as jwt;
use serde::de::DeserializeOwned;

/// Trait for decoding a token from a string representation
///
/// This trait defines a method for decoding a JWT token string into a specified
/// type. The [`decode`] method will parse the token and return the claims as
/// the desired type, handling any potential errors in the process.
///
/// [`decode`]: Decode::decode
///
/// The `Decode` trait is intended to be implemented by types that provide
/// specific decoding logic for JWT tokens. It is useful for scenarios where
/// various types of tokens need to be handled, ensuring that they can be
/// converted from their string representation into usable Rust types.
pub trait Decode: Send + Sync {
    fn decode<T: DeserializeOwned>(&self, token: &str) -> Result<T, Error>;
}

/// Trait for a service that manages cryptographic keys used for JWT operations.
///
/// This trait defines a method for retrieving cryptographic keys based on a
/// specified key identifier (`kid`).
///
/// The `GetKey` trait is essential for services that need to interact with
/// different key sources, such as local storage or remote key management
/// services, enabling flexible key retrieval for JWT operations.
///
/// Implement this trait for any type that manages cryptographic keys and
/// can provide the necessary decoding keys for JWT handling.
pub trait GetKey: Send + Sync {
    /// Retrieves the decoding key associated with the specified key identifier (`kid`).
    fn get_key(&self, kid: &str) -> Result<&jwt::DecodingKey, Error>;
}
