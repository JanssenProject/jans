// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Package for generating JWT tokens for testing purpose.

use std::sync::LazyLock;

use {jsonwebkey as jwk, jsonwebtoken as jwt};

// Represent meta information about entity from cedar-policy schema.
static ENCODING_KEYS: LazyLock<GeneratedKeys> = LazyLock::new(generate_keys);

pub struct GeneratedKeys {
    pub private_key_id: String,
    pub private_encoding_key: jwt::EncodingKey,
}

/// Generates a set of private and public keys using ES256
///
/// Returns a tuple: (Vec<(key_id, private_key)>, jwks)
fn generate_keys() -> GeneratedKeys {
    let kid = 1;
    // Generate a private key
    let mut jwk = jwk::JsonWebKey::new(jwk::Key::generate_p256());
    jwk.set_algorithm(jwk::Algorithm::ES256)
        .expect("should set encryption algorithm");
    jwk.key_id = Some("some_id".to_string());

    // Generate public key
    let mut public_key =
        serde_json::to_value(jwk.key.to_public()).expect("should serialize public key");
    public_key["kid"] = serde_json::Value::String(kid.to_string()); // set `kid`
    let public_key: jwt::jwk::Jwk =
        serde_json::from_value(public_key).expect("should deserialize public key");

    let private_key = jwt::EncodingKey::from_ec_pem(jwk.key.to_pem().as_bytes())
        .expect("should generate encoding key");

    let public_keys = jwt::jwk::JwkSet {
        keys: vec![public_key],
    };
    let _public_keys = serde_json::to_string(&public_keys).expect("should serialize keyset");

    GeneratedKeys {
        private_key_id: kid.to_string(),
        private_encoding_key: private_key,
    }
}

/// Generates a token string signed with ES256
pub fn generate_token_using_claims(claims: impl serde::Serialize) -> String {
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
