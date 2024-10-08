/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

//! This module tests the logic for validating `RSA` encrypted `AccessToken`s
//!
//! # TODO:
//! - add test for expired tokens

use std::error::Error;
use std::time::{SystemTime, UNIX_EPOCH};

use jsonwebtoken::{
    decode, encode, Algorithm, DecodingKey, EncodingKey, Header, TokenData, Validation,
};
use openssl::rsa::Rsa;

use crate::jwt::token::AccessToken;

fn generate_rsa_key() -> Result<(Vec<u8>, Vec<u8>), Box<dyn Error>> {
    let private = Rsa::generate(2048)?;
    let public_key = private.public_key_to_pem()?;
    let private_key = private.private_key_to_pem()?;

    Ok((private_key, public_key))
}

fn create_access_token_claims() -> Result<AccessToken, Box<dyn Error>> {
    let now = SystemTime::now();
    let iat = now.duration_since(UNIX_EPOCH)?.as_secs();
    let exp = now.duration_since(UNIX_EPOCH)?.as_secs() + 3600; // Set expiration 1 hour from now
    Ok(AccessToken {
        aud: "https://auth.myapp.com".to_string(),
        exp: exp.try_into()?,
        iat: iat.try_into()?,
        iss: "https://auth.myapp.com".to_string(),
        jti: "1".to_string(),
        scope: "scope".to_string(),
    })
}

fn create_jwt(claims: &AccessToken, private_key: &Vec<u8>) -> Result<String, Box<dyn Error>> {
    let header = Header::new(Algorithm::RS256);

    let encoding_key = EncodingKey::from_rsa_pem(&private_key)?;

    let token = encode(&header, &claims, &encoding_key)?;

    Ok(token)
}

fn validate_jwt(
    token: &str,
    public_key: &Vec<u8>,
) -> Result<TokenData<AccessToken>, Box<dyn Error>> {
    // Define the validation parameters (e.g., algorithm, claims)
    let mut validation = Validation::new(Algorithm::RS256);
    validation.validate_exp = true; // Ensure that the token hasn't expired
    validation.set_audience(&vec!["https://auth.myapp.com".to_string()]); // Set expected audience

    let decoding_key = DecodingKey::from_rsa_pem(public_key)?;

    // Verify the token using the public key
    let token_data = decode::<AccessToken>(&token, &decoding_key, &validation)?;

    Ok(token_data)
}

#[test]
fn can_validate_access_token() {
    let (private_key, public_key) = generate_rsa_key().expect("should generate keys");

    let claims = create_access_token_claims().expect("should create claims");
    let token = create_jwt(&claims, &private_key).expect("should create jwt");

    validate_jwt(&token, &public_key).unwrap();
    assert!(validate_jwt(&token, &public_key).is_ok());

    let wrong_token = "wrong_token";
    assert!(validate_jwt(&wrong_token, &public_key).is_err());
}
