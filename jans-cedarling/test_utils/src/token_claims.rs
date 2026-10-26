// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Package for generating JWT tokens for testing purpose.

use std::sync::LazyLock;

use serde::Serialize;

use base64::prelude::{BASE64_URL_SAFE_NO_PAD, Engine};
use jsonwebtoken as jwt;
use p256::pkcs8::EncodePrivateKey;

// Represent meta information about entity from cedar-policy schema.
static ENCODING_KEYS: LazyLock<GeneratedKeys> = LazyLock::new(generate_keys);

pub struct GeneratedKeys {
    pub private_key_id: String,
    pub private_encoding_key: jwt::EncodingKey,
}

/// Generates an ES256 private key
fn generate_keys() -> GeneratedKeys {
    let kid = 1;
    let private_key = p256::ecdsa::SigningKey::from_slice(&rand::random::<[u8; 32]>())
        .expect("should generate P-256 key")
        .to_pkcs8_der()
        .expect("should encode P-256 key");

    GeneratedKeys {
        private_key_id: kid.to_string(),
        private_encoding_key: jwt::EncodingKey::from_ec_der(private_key.as_bytes()),
    }
}

/// Generates a token string signed with ES256
pub fn generate_token_using_claims(claims: impl serde::Serialize) -> String {
    #[cfg(target_arch = "wasm32")]
    cedarling_jwt_crypto::install();

    let key_id = ENCODING_KEYS.private_key_id.clone();
    let encoding_key = &ENCODING_KEYS.private_encoding_key;

    // select a key from the keyset
    // for simplicity, were just choosing the second one

    // specify the header
    let header = jwt::Header {
        alg: jwt::Algorithm::ES256,
        kid: Some(key_id.to_string()),
        ..Default::default()
    };

    // serialize token to a string
    jwt::encode(&header, &claims, encoding_key).expect("should generate token")
}

/// A pair of encoding and decoding keys.
#[derive(Clone)]
pub struct KeyPair {
    kid: Option<String>,
    encoding_key: jwt::EncodingKey,
    decoding_key: jwt::jwk::Jwk,
    alg: jwt::Algorithm,
}

/// Generates a HS256-signed token using the given claims.
#[track_caller]
pub fn generate_keypair_hs256(kid: Option<impl ToString>) -> KeyPair {
    let secret: [u8; 32] = rand::random();
    let mut decoding_key =
        serde_json::json!({"kty": "oct", "k": BASE64_URL_SAFE_NO_PAD.encode(secret)});

    if let Some(kid) = &kid {
        decoding_key["kid"] = serde_json::Value::String(kid.to_string());
    }
    let mut decoding_key: jwt::jwk::Jwk =
        serde_json::from_value(decoding_key).expect("shold generate decoding key");
    decoding_key.common.key_algorithm = Some(jwt::jwk::KeyAlgorithm::HS256);

    let encoding_key = jwt::EncodingKey::from_secret(&secret);

    KeyPair {
        kid: kid.map(|s| s.to_string()),
        encoding_key,
        decoding_key,
        alg: jwt::Algorithm::HS256,
    }
}

/// Generates a token string in the given format: `"header.claim.signature"`
#[track_caller]
pub fn generate_token_using_claims_and_keypair(
    claims: &impl Serialize,
    keypair: &KeyPair,
) -> String {
    #[cfg(target_arch = "wasm32")]
    cedarling_jwt_crypto::install();

    let header = jwt::Header {
        alg: keypair.alg,
        kid: keypair.kid.clone(),
        ..Default::default()
    };

    // encode token to a string
    jwt::encode(&header, &claims, &keypair.encoding_key).expect("should encode JWT")
}

/// Generates a JwkSet from the given keys
#[track_caller]
pub fn generate_jwks(keys: &[KeyPair]) -> jwt::jwk::JwkSet {
    let keys = keys
        .iter()
        .map(|key_pair| key_pair.decoding_key.clone())
        .collect::<Vec<jwt::jwk::Jwk>>();
    jwt::jwk::JwkSet { keys }
}
