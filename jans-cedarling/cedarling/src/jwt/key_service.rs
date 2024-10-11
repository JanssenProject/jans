/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use jsonwebtoken::DecodingKey;

/// Custom error type for `KeyService` operations.
#[derive(thiserror::Error, Debug)]
#[non_exhaustive]
pub enum KeyServiceError {
    /// Key is not in the KeyService Storage
    #[error("no key with the given `kid` was found: {0}")]
    KeyNotFound(Box<str>),
}

/// Trait for a service that manages cryptographic keys used for JWT operations.
pub trait KeyService: Send + Sync {
    /// Retrieves the decoding key associated with the specified key identifier (`kid`).
    fn get_key(&self, kid: &str) -> Result<DecodingKey, KeyServiceError>;
}
