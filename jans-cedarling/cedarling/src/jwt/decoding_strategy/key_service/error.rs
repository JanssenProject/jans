/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

/// Error type for the Key Service
#[derive(thiserror::Error, Debug)]
pub enum KeyServiceError {
    /// Indicates that a key with the specified `kid` was not found in the JWKS.
    #[error("No key with `kid`=\"{0}\" found in the JWKS.")]
    KeyNotFound(String),

    /// Indicates an HTTP error response received from an endpoint.
    #[error("Received error HTTP status: {0}")]
    HttpStatus(#[source] reqwest::Error),

    /// Indicates a failure to reach the endpoint after 3 attempts.
    #[error("Could not reach endpoint after trying 3 times: {0}")]
    MaxHttpRetriesReached(#[source] reqwest::Error),

    /// Indicates failure to deserialize the response from the HTTP request.
    #[error("Failed to deserialize the response from the HTTP request: {0}")]
    RequestDeserialization(#[source] reqwest::Error),

    /// Indicates failure to initialize the HTTP client.
    #[error("Failed to initilize HTTP client: {0}")]
    HttpClientInitialization(#[source] reqwest::Error),

    /// Indicates an error in parsing the decoding key from the JWKS JSON.
    #[error("Error parsing decoding key from JWKS JSON: {0}")]
    KeyParsing(#[source] jsonwebtoken::errors::Error),

    /// Indicates that the JWK is missing a `kid`.
    #[error("The JWK is missing a required `kid`.")]
    MissingKeyId,

    /// Indicates that acquiring a write lock on decoding keys failed.
    ///
    /// This error gets returned when a lock gets poisoned.
    #[error("Failed to acquire write lock on decoding keys.")]
    Lock,
}
