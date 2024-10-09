use std::time::{SystemTime, UNIX_EPOCH};

use base64::prelude::*;
use rand::Rng;
use serde::{Deserialize, Serialize};

use jsonwebtoken::{encode, EncodingKey, Header};
use test_utils::assert_eq;

use crate::{jwt::JwtService, JwtConfig};

#[derive(Serialize, Deserialize, Debug, Clone, PartialEq)]
struct Claims {
    iss: String,
    sub: String,
    aud: String,
    exp: u64,
    iat: u64,
}

#[test]
fn can_decode_claims_unvalidated() {
    let config = JwtConfig::Disabled;
    let service = JwtService::new(config);

    let (claims, token, _) =
        generate_token(true).expect("should generate an expired token with a random key");

    let result = service
        .decode::<Claims>(&token)
        .expect("should decode token");

    assert_eq!(claims, result)
}

fn generate_token(expired: bool) -> Result<(Claims, String, String), Box<dyn std::error::Error>> {
    let iat: u64;
    let exp: u64;
    if expired {
        iat = 1695198692;
        exp = 1695195092;
    } else {
        let now = SystemTime::now();
        iat = now.duration_since(UNIX_EPOCH)?.as_secs();
        exp = iat + 3600; // Set expiration 1 hour from now
    }

    let claims = Claims {
        iss: "https://auth.myapp.com".to_string(),
        sub: "user123".to_string(),
        aud: "myapp.com".to_string(),
        exp,
        iat,
    };

    let secret = generate_hmac_key();
    let jwt = sign_jwt(claims.clone(), &secret)?;

    Ok((claims, jwt, secret))
}

fn generate_hmac_key() -> String {
    let key: [u8; 32] = rand::thread_rng().gen();
    BASE64_STANDARD.encode(key)
}

fn sign_jwt(claims: Claims, secret: &str) -> Result<String, jsonwebtoken::errors::Error> {
    let key = EncodingKey::from_secret(secret.as_ref());
    encode(&Header::default(), &claims, &key)
}
