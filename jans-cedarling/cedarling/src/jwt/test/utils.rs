/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use jsonwebkey as jwk;
use jsonwebtoken as jwt;
use serde::{Deserialize, Serialize};
use std::time::{SystemTime, UNIX_EPOCH};

/// A user-defined struct for holding token claims.
#[derive(Serialize, Deserialize, Debug, Clone, PartialEq)]
pub struct Claims {
    iss: String,
    sub: String,
    aud: String,
    exp: u64,
    iat: u64,
}

/// Generates a JWT Signed with ES256
///
/// Returns a tuple: (token, public_key, claims)
pub fn generate_token(expired: bool) -> (String, String, Claims) {
    let (enc_key, dec_key) = generate_key_pair();

    let claims = generate_claims(expired);
    let header = jwt::Header {
        typ: Some("JWT".to_string()),
        alg: jwt::Algorithm::ES256,
        cty: None,
        jku: None,
        jwk: None,
        kid: Some(enc_key.0.clone()),
        x5u: None,
        x5c: None,
        x5t: None,
        x5t_s256: None,
    };

    let token = jwt::encode(&header, &claims, &enc_key.1).expect("should generate token");
    (token, dec_key, claims)
}

// Generates claims for a JWT
fn generate_claims(expired: bool) -> Claims {
    let iat: u64;
    let exp: u64;
    if expired {
        iat = 1695198692; // timestamp for issued at (iat)
        exp = 1695195092; // timestamp for expiration (exp)
    } else {
        let now = SystemTime::now();
        // set issued at (iat) at the current time
        iat = now.duration_since(UNIX_EPOCH).unwrap().as_secs();
        exp = iat + 3600; // set expiration 1 hour from now
    }

    Claims {
        iss: "https://auth.myapp.com".to_string(),
        sub: "user123".to_string(),
        aud: "myapp.com".to_string(),
        exp,
        iat,
    }
}

/// Generates a pair of private and public keys for signing and verifying JWTs using ES256
fn generate_key_pair() -> ((String, jwt::EncodingKey), String) {
    let key_id = "some_key_id".to_string();

    // Generate a private key
    let mut jwk = jwk::JsonWebKey::new(jwk::Key::generate_p256());

    jwk.set_algorithm(jwk::Algorithm::ES256)
        .expect("should set encryption algorithm");
    jwk.key_id = Some(key_id.clone());

    let public_key =
        serde_json::to_string(&jwk.key.to_public()).expect("should serialize public key");

    let private_key = jwt::EncodingKey::from_ec_pem(jwk.key.to_pem().as_bytes())
        .expect("should generate encoding key");

    ((key_id, private_key), public_key)
}

/// Generates a set of private and public keys using ES256
///
/// Returns a tuple: (Vec<(key_id, private_key)>, jwks)
pub fn generate_keys() -> (Vec<(String, jwt::EncodingKey)>, String) {
    let mut public_keys = jwt::jwk::JwkSet { keys: vec![] };
    let mut private_keys = vec![];

    for kid in 1..=5 {
        // Generate a private key
        let mut jwk = jwk::JsonWebKey::new(jwk::Key::generate_p256());
        jwk.set_algorithm(jwk::Algorithm::ES256)
            .expect("should set encryption algorithm");
        jwk.key_id = Some("some_id".to_string());

        // Generate public key
        let mut public_key =
            serde_json::to_value(&jwk.key.to_public()).expect("should serialize public key");
        public_key["kid"] = serde_json::Value::String(kid.to_string()); // set `kid`
        let public_key: jwt::jwk::Jwk =
            serde_json::from_value(public_key).expect("should deserialize public key");
        public_keys.keys.push(public_key);

        let private_key = jwt::EncodingKey::from_ec_pem(jwk.key.to_pem().as_bytes())
            .expect("should generate encoding key");
        private_keys.push((kid.to_string(), private_key));
    }

    let public_keys = serde_json::to_string(&public_keys).expect("should serialize keyset");
    (private_keys, public_keys)
}

/// Generates a JWT using a keyset
pub fn generate_token_using_keys(
    encoding_keys: Vec<(String, jwt::EncodingKey)>,
    expired: bool,
) -> (String, Claims) {
    // set expiration
    let iat: u64;
    let exp: u64;
    if expired {
        iat = 1695198692;
        exp = 1695195092;
    } else {
        let now = SystemTime::now();
        iat = now.duration_since(UNIX_EPOCH).unwrap().as_secs();
        exp = iat + 3600; // Set expiration 1 hour from now
    }

    // select a key from the keyset
    // for simplicity, were just choosing the first one
    let key_id = encoding_keys[0].0.clone();
    let enc_key = &encoding_keys[0].1;

    // specify the token payload
    let claims = Claims {
        iss: "https://auth.myapp.com".to_string(),
        sub: "user123".to_string(),
        aud: "myapp.com".to_string(),
        exp,
        iat,
    };

    // specify the header
    let header = jwt::Header {
        typ: Some("JWT".to_string()),
        alg: jwt::Algorithm::ES256,
        cty: None,
        jku: None,
        jwk: None,
        kid: Some(key_id),
        x5u: None,
        x5c: None,
        x5t: None,
        x5t_s256: None,
    };

    // serialize token to a string
    let token = jwt::encode(&header, &claims, enc_key).expect("should generate token");

    (token, claims)
}
