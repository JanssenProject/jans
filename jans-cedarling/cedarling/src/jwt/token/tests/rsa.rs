/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

//! This contains tests for validating `RSA` encrypted `AccessToken`s

use std::error::Error;
use std::time::{SystemTime, UNIX_EPOCH};

use jsonwebtoken::{encode, Algorithm, EncodingKey, Header};
use openssl::rsa::Rsa;
use serde::Serialize;

use crate::jwt::token::{AccessToken, Token, TokenKind};

fn generate_rsa_key() -> Result<(Vec<u8>, Vec<u8>), Box<dyn Error>> {
    let private = Rsa::generate(2048)?;
    let public_key = private.public_key_to_pem()?;
    let private_key = private.private_key_to_pem()?;

    Ok((private_key, public_key))
}

fn create_jwt(claims: &AccessToken, private_key: &Vec<u8>) -> Result<String, Box<dyn Error>> {
    let header = Header::new(Algorithm::RS256);

    let encoding_key = EncodingKey::from_rsa_pem(&private_key)?;

    let token = encode(&header, &claims, &encoding_key)?;

    Ok(token)
}

#[test]
fn can_validate_access_token_with_correct_claims() {
    let (private_key, public_key) = generate_rsa_key().expect("should generate keys");

    let now = SystemTime::now();
    let iat = now.duration_since(UNIX_EPOCH).unwrap().as_secs();
    let exp = now.duration_since(UNIX_EPOCH).unwrap().as_secs() + 3600; // Set expiration 1 hour from now
    let claims = AccessToken {
        aud: "https://auth.myapp.com".to_string(),
        exp: exp.try_into().expect("should convert `u64` to `i64"),
        iat: iat.try_into().expect("should convert `u64` to `i64"),
        iss: "https://auth.myapp.com".to_string(),
        jti: "1".to_string(),
        scope: "scope".to_string(),
    };

    let token = create_jwt(&claims, &private_key).expect("should create token");

    Token::validate(
        &token,
        TokenKind::AccessToken,
        &public_key,
        Algorithm::RS256,
    )
    .unwrap();
}

#[test]
#[should_panic]
fn returns_error_on_invalid_token() {
    let (_, public_key) = generate_rsa_key().expect("should generate keys");

    let invalid_token = "invalid_token";
    Token::validate(
        &invalid_token,
        TokenKind::AccessToken,
        &public_key,
        Algorithm::RS256,
    )
    .unwrap();
}

#[test]
#[should_panic]
fn returns_error_on_incomplete_claims() {
    let (private_key, public_key) = generate_rsa_key().expect("should generate keys");

    #[derive(Serialize)]
    struct TokenWithMissingClaims {
        pub aud: String,
        pub exp: i64,
        pub iat: i64,
        pub iss: String,
    }

    let now = SystemTime::now();
    let iat = now.duration_since(UNIX_EPOCH).unwrap().as_secs();
    let exp = now.duration_since(UNIX_EPOCH).unwrap().as_secs() + 3600; // Set expiration 1 hour from now
    let claims = TokenWithMissingClaims {
        aud: "https://auth.myapp.com".to_string(),
        exp: exp.try_into().expect("should convert `u64` to `i64"),
        iat: iat.try_into().expect("should convert `u64` to `i64"),
        iss: "https://auth.myapp.com".to_string(),
    };

    // let token = create_jwt(&claims, &private_key).expect("should create token");
    let header = Header::new(Algorithm::RS256);
    let encoding_key = EncodingKey::from_rsa_pem(&private_key).expect("should read encoding key");
    let token = encode(&header, &claims, &encoding_key).expect("should encode token");

    Token::validate(
        &token,
        TokenKind::AccessToken,
        &public_key,
        Algorithm::RS256,
    )
    .unwrap();
}
