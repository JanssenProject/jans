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

// eyJhbGciOiJSUzI1NiIsImtpZCI6IjA2Y2FlY2FiMzZiMDM5NmQ2MzJmZmQ3ZjYzODU5NTcxNTJlNzZiN2EiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjE2MjgxNzMwMDAsImlhdCI6MTYyODE2OTQwMCwiYXRfaGFzaCI6InUtWTBCY29XdXpXQm5xckR6NHFxY1EiLCJhdWQiOiI2MzE3NDM5MzQ1ODQtaWdvY2xvdWR1c2VyYXBwcy5jb20iLCJzdWIiOiIxMTIzNDU2Nzg5MCIsInNjb3BlcyI6Im9wZW5pZCBwcm9maWxlIGVtYWlsIn0.CzxgNDP1K2HlZL1hjmsby7dYPjtFmoqz5TK6DLbHmg4Rghv0eXqXpRpiCwEpzxl9FJYY7XJGLHg_zvORWiZKDpWcPfwlMfOPSSbWVZjPcDQdRar0kgtnxRnHk1nl-NdhkAb8dJfUuPnoIdqrq4ftcqDjsE1yMvoYyFLjx8JFCpA
#[derive(Serialize, Deserialize, Debug, Clone, PartialEq, Default)]
pub struct AccessTokenClaims {
    pub iss: String,
    pub aud: String,
    pub sub: String,
    pub scopes: String,
    pub exp: u64,
    pub iat: u64,
}

// eyJhbGciOiJSUzI1NiIsImtpZCI6IjA2Y2FlY2FiMzZiMDM5NmQ2MzJmZmQ3ZjYzODU5NTcxNTJlNzZiN2EiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJzdWIiOiIxMTIzNDU2Nzg5MCIsImF1ZCI6IjYzMTc0MzkzNDU4NC1pZ29jbG91ZHVzZXJhcHBzLmNvbSIsImVtYWlsIjoidXNlckBlbWFpbC5jb20iLCJuYW1lIjoiSm9obiBEb2UiLCJleHAiOjE2MjgxNzMwMDAsImlhdCI6MTYyODE2OTQwMCwiaXNzIjoiaHR0cHM6Ly9hY2NvdW50cy5nb29nbGUuY29tIiwibmFtZSI6IkpvaG4gRG9lIn0.YyyyzVDC7iFlZL1hjmsby7dYPjtFmoqz5TK6DLbHmg4Rghv0eXqXpRpiCwEpzxl9FJYY7XJGLHg_zvORWiZKDpWcPfwlMfOPSSbWVZjPcDQdRar0kgtnxRnHk1nl-NdhkAb8dJfUuPnoIdqrq4ftcqDjsE1yMvoYyFLjx8JFCpA
#[derive(Serialize, Deserialize, Debug, Clone, PartialEq, Default)]
pub struct IdTokenClaims {
    pub iss: String,
    pub sub: String,
    pub aud: String,
    pub email: String,
    pub exp: u64,
    pub iat: u64,
}

/// Generates a set of private and public keys using ES256
///
/// Returns a tuple: (Vec<(key_id, private_key)>, jwks)
pub fn generate_keys() -> (Vec<(String, jwt::EncodingKey)>, String) {
    let mut public_keys = jwt::jwk::JwkSet { keys: vec![] };
    let mut private_keys = vec![];

    for kid in 1..=2 {
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

/// Generates an access_token using a ket from a keyset
pub fn generate_access_token_using_keys(
    claims: &mut AccessTokenClaims,
    encoding_keys: &Vec<(String, jwt::EncodingKey)>,
    expired: bool,
) -> String {
    // set expiration
    if expired {
        // generate an expired token
        claims.iat = 1695198692;
        claims.exp = 1695195092;
    } else {
        // Set expiration 1 hour from now
        let now = SystemTime::now();
        claims.iat = now.duration_since(UNIX_EPOCH).unwrap().as_secs();
        claims.exp = claims.iat + 3600;
    }

    // select a key from the keyset
    // for simplicity, were just choosing the first one
    let key_id = encoding_keys[0].0.clone();
    let enc_key = &encoding_keys[0].1;

    // specify the header
    let header = jwt::Header {
        alg: jwt::Algorithm::ES256,
        kid: Some(key_id),
        ..Default::default()
    };

    // serialize token to a string
    jwt::encode(&header, &claims, enc_key).expect("should generate token")
}

/// Generates an access_token using a ket from a keyset
pub fn generate_id_token_using_keys(
    claims: &mut IdTokenClaims,
    encoding_keys: &Vec<(String, jwt::EncodingKey)>,
    expired: bool,
) -> String {
    // set expiration
    if expired {
        // generate an expired token
        claims.iat = 1695198692;
        claims.exp = 1695195092;
    } else {
        // Set expiration 1 hour from now
        let now = SystemTime::now();
        claims.iat = now.duration_since(UNIX_EPOCH).unwrap().as_secs();
        claims.exp = claims.iat + 3600;
    }

    // select a key from the keyset
    // for simplicity, were just choosing the second one
    let key_id = encoding_keys[1].0.clone();
    let enc_key = &encoding_keys[1].1;

    // specify the header
    let header = jwt::Header {
        alg: jwt::Algorithm::ES256,
        kid: Some(key_id),
        ..Default::default()
    };

    // serialize token to a string
    jwt::encode(&header, &claims, enc_key).expect("should generate token")
}
