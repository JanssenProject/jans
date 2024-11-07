/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use core::panic;
use jsonwebkey as jwk;
use jsonwebtoken::{self as jwt};
use serde::Serialize;
use std::{
    time::{SystemTime, UNIX_EPOCH},
    u64,
};

#[derive(Clone)]
pub struct EncodingKey {
    pub key_id: String,
    pub key: jwt::EncodingKey,
    pub algorithm: jwt::Algorithm,
}

/// Generates a set of private and public keys using ES256
///
/// Returns a tuple: (encoding_keys, jwks as a string)
pub fn generate_keys() -> (Vec<EncodingKey>, String) {
    let mut public_keys = jwt::jwk::JwkSet { keys: vec![] };
    let mut encoding_keys = vec![];

    // Generate a private key using ES256
    let kid = 1;
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
    public_keys.keys.push(public_key);

    let encoding_key = jwt::EncodingKey::from_ec_pem(jwk.key.to_pem().as_bytes())
        .expect("should generate encoding key");
    encoding_keys.push(EncodingKey {
        key_id: kid.to_string(),
        key: encoding_key,
        algorithm: jwt::Algorithm::ES256,
    });

    // Generate another private key using HS256
    let kid = 2;
    let mut jwk = jwk::JsonWebKey::new(jwk::Key::generate_symmetric(256));
    jwk.set_algorithm(jwk::Algorithm::HS256)
        .expect("should set encryption algorithm");
    jwk.key_id = Some("some_id".to_string());

    // since this is a symmetric key, the public key is the same as the private
    let mut public_key =
        serde_json::to_value(jwk.key.clone()).expect("should serialize public key");

    // set the key parameters
    public_key["kid"] = serde_json::Value::String(kid.to_string()); // set `kid`
    let mut public_key: jwt::jwk::Jwk =
        serde_json::from_value(public_key).expect("should deserialize public key");
    public_key.common.key_algorithm = Some(jwt::jwk::KeyAlgorithm::HS256);
    public_keys.keys.push(public_key);

    let private_key = match *jwk.key {
        jsonwebkey::Key::Symmetric { key } => jwt::EncodingKey::from_secret(&key),
        _ => panic!("Expected symmetric key for HS256"), // this shouldn't really happen unless
                                                         // code within this function changes
    };
    encoding_keys.push(EncodingKey {
        key_id: kid.to_string(),
        key: private_key,
        algorithm: jwt::Algorithm::HS256,
    });

    // serialize public keys
    let public_keys = serde_json::to_string(&public_keys).expect("should serialize keyset");
    (encoding_keys, public_keys)
}

pub struct Timestamp;

impl Timestamp {
    pub fn now() -> u64 {
        let now = SystemTime::now();
        now.duration_since(UNIX_EPOCH).unwrap().as_secs()
    }

    pub fn one_hour_before_now() -> u64 {
        let now = SystemTime::now();
        now.duration_since(UNIX_EPOCH).unwrap().as_secs() - 3600
    }

    pub fn one_hour_after_now() -> u64 {
        let now = SystemTime::now();
        now.duration_since(UNIX_EPOCH).unwrap().as_secs() + 3600
    }
}

/// The arguments for [`generate_token_using_claims`]
pub struct GenerateTokensArgs {
    pub access_token_claims: serde_json::Value,
    pub id_token_claims: serde_json::Value,
    pub userinfo_token_claims: serde_json::Value,
    pub encoding_keys: Vec<EncodingKey>,
}

pub struct GeneratedTokens {
    pub access_token: String,
    pub id_token: String,
    pub userinfo_token: String,
}

/// Generates tokens using the given encoding keys.
///
/// The `access_token` and `userinfo_token` will be encoded by the first key in the
/// `Vec` and the `id_token` will be encoded by the second.
///
/// # Panics
///
/// Panics when a token cannot be encoded.
pub fn generate_tokens_using_claims(args: GenerateTokensArgs) -> GeneratedTokens {
    let access_token =
        generate_token_using_claims(&args.access_token_claims, &args.encoding_keys[0])
            .expect("Should generate access_token");
    let id_token = generate_token_using_claims(&args.id_token_claims, &args.encoding_keys[1])
        .expect("Should generate id_token");
    let userinfo_token =
        generate_token_using_claims(&args.userinfo_token_claims, &args.encoding_keys[0])
            .expect("Should generate userinfo_token");

    GeneratedTokens {
        access_token,
        id_token,
        userinfo_token,
    }
}

/// Generates a token string signed with ES256
pub fn generate_token_using_claims(
    claims: &impl Serialize,
    encoding_key: &EncodingKey,
) -> Result<String, jwt::errors::Error> {
    // select a key from the keyset
    // for simplicity, were just choosing the second one

    // specify the header
    let header = jwt::Header {
        alg: encoding_key.algorithm,
        kid: Some(encoding_key.key_id.clone()),
        ..Default::default()
    };

    // serialize token to a string
    Ok(jwt::encode(&header, &claims, &encoding_key.key)?)
}

/// Invalidates a JWT Token by altering the first two characters in its signature
///
/// # Panics
///
/// Panics when the input token is malformed.
pub fn invalidate_token(token: String) -> String {
    let mut token_parts: Vec<&str> = token.split('.').collect();

    if token_parts.len() < 3 {
        panic!("Token is malformed")
    }

    let mut new_signature = token_parts[2].to_string();
    let mut chars: Vec<char> = new_signature.chars().collect();

    if chars.len() >= 2 {
        // Ensure the first character is different from the second
        if chars[0] == chars[1] {
            let mut new_char = 'A';
            // Find a character that differs from the second character
            while new_char == chars[1] {
                new_char = (new_char as u8 + 1) as char; // Cycle through ASCII values
            }
            chars[0] = new_char;
        } else {
            // Swap the first two characters if they're already different
            chars.swap(0, 1);
        }
        new_signature = chars.into_iter().collect();
    }

    token_parts[2] = &new_signature;
    token_parts.join(".")
}
