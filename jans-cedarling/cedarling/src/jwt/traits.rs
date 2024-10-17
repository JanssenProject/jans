/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::Error;
use jsonwebtoken as jwt;
use serde::de::DeserializeOwned;

/// Trait for decoding a JWT token from a string representation.
///
/// # Usage
/// This trait is meant to be implemented for structs that need to handle JWT
/// token decoding logic. It is particularly useful for working with various
/// token types, ensuring they can be decoded into usable types in Rust.
pub trait Decode: Send + Sync {
    /// Decodes the JWT token string into the specified type.
    ///
    /// This method takes the token in its string format and decodes it into a
    /// generic type `T`. It can also optionally verify the audience (`aud`),
    /// issuer (`iss`), and the presence of the subject (`sub`) claim.
    ///
    /// # Errors
    /// Returns an error if the decoding process fails or if validation fails
    /// (e.g., the token is invalid or its signature is untrusted).
    fn decode<T: DeserializeOwned>(
        &self,
        jwt_str: &str,
        aud: Option<impl ToString>,
        iss: Option<impl ToString>,
        req_sub: bool,
    ) -> Result<T, Error>;
}

/// Trait for extracting claims from a JWT token without validation.
///
/// # Usage
/// Implement this trait when you need to handle tokens where validation is not
/// required, but you still want to extract the claims in a structured manner.
pub trait ExtractClaims: Send + Sync {
    /// Extracts the claims from the JWT token string without validating the
    /// signature or other parameters, returning the claims as the specified type.
    ///
    /// # Errors
    /// Returns an error if the decoding process fails or if the claims cannot
    /// be extracted.
    fn extract_claims<T: DeserializeOwned>(&self, jwt: &str) -> Result<T, Error>;
}

/// Trait for managing cryptographic keys used in JWT operations.
///
/// # Usage
/// Use this trait when implementing a key service that provides decoding keys
/// for verifying JWTs based on their header information (i.e., `kid`).
pub trait GetKey: Send + Sync {
    /// Retrieves the decoding key associated with the specified key identifier (`kid`).
    fn get_key(&self, kid: &str) -> Result<&jwt::DecodingKey, Error>;
}
