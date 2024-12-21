// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use serde::Serialize;
use {jsonwebkey as jwk, jsonwebtoken as jwt};

/// A pair of encoding and decoding keys.
pub struct KeyPair {
    kid: Option<String>,
    encoding_key: jwt::EncodingKey,
    decoding_key: jwt::jwk::Jwk,
    alg: jwt::Algorithm,
}

#[derive(Debug, thiserror::Error)]
pub enum KeyGenerationError {
    #[error("Failed to serialize the decoding key onto the right struct")]
    SerializeDecodingKey(#[from] serde_json::Error),
    #[error("The given key was generated with the wrong algorithm")]
    KeyMismatch,
}

/// Generates a HS256-signed token using the given claims.
pub fn generate_keypair_hs256(kid: Option<impl ToString>) -> Result<KeyPair, KeyGenerationError> {
    let mut jwk = jwk::JsonWebKey::new(jwk::Key::generate_symmetric(256));
    jwk.set_algorithm(jwk::Algorithm::HS256)
        .expect("should set encryption algorithm");
    jwk.key_id = Some("some_id".to_string());

    // since this is a symmetric key, the public key is the same as the private
    let mut decoding_key = serde_json::to_value(jwk.key.clone())?;

    // set the key parameters
    if let Some(kid) = &kid {
        decoding_key["kid"] = serde_json::Value::String(kid.to_string());
    }
    let mut decoding_key: jwt::jwk::Jwk = serde_json::from_value(decoding_key)?;
    decoding_key.common.key_algorithm = Some(jwt::jwk::KeyAlgorithm::HS256);

    let encoding_key = match *jwk.key {
        jsonwebkey::Key::Symmetric { key } => jwt::EncodingKey::from_secret(&key),
        _ => Err(KeyGenerationError::KeyMismatch)?,
    };

    Ok(KeyPair {
        kid: kid.map(|s| s.to_string()),
        encoding_key,
        decoding_key,
        alg: jwt::Algorithm::HS256,
    })
}

#[derive(Debug, thiserror::Error)]
pub enum TokenGenerationError {
    #[error("Failed to encode token into a JWT string")]
    Encode(#[from] jwt::errors::Error),
}

/// Generates a token string in the given format: `"header.claim.signature"`
pub fn generate_token_using_claims(
    claims: &impl Serialize,
    keypair: &KeyPair,
) -> Result<String, TokenGenerationError> {
    let header = jwt::Header {
        alg: keypair.alg,
        kid: keypair.kid.clone(),
        ..Default::default()
    };

    // serialize token to a string
    Ok(jwt::encode(&header, &claims, &keypair.encoding_key)?)
}

/// Generates a JwkSet from the given keys
pub fn generate_jwks(keys: &Vec<KeyPair>) -> jwt::jwk::JwkSet {
    let keys = keys
        .iter()
        .map(|key_pair| key_pair.decoding_key.clone())
        .collect::<Vec<jwt::jwk::Jwk>>();
    jwt::jwk::JwkSet { keys }
}
