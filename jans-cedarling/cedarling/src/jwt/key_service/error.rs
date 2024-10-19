/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

#[derive(Debug, thiserror::Error)]
pub enum Error {
    #[error("http error: {0}")]
    HttpError(#[from] reqwest::Error),
    #[error("error revieved from GET request from {0}: {0}")]
    HttpGetError(Box<str>, Box<str>),
    #[error("failed to deserialize: {0}")]
    JsonDeserializationError(#[from] serde_json::Error),
    #[error("failed to deserialize jwk: {0}")]
    JwkDeserializationError(#[from] jsonwebtoken::errors::Error),
    #[error("jwk missing `kid`")]
    JwkMissingKeyId,
    #[error("tried to fetch keys for unknown issuer")]
    UnknownIssuer(Box<str>),
    #[error("key no key with the `kid`=\"{0}\" was found in the jwks")]
    KeyNotFound(Box<str>),
}
