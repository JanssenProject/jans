use std::time::{SystemTime, UNIX_EPOCH};

use serde::{Deserialize, Serialize};

use jsonwebkey::{self as jwk, Algorithm};
use jsonwebtoken::{self as jwt};

#[derive(Serialize, Deserialize, Debug, Clone, PartialEq)]
pub struct Claims {
    iss: String,
    sub: String,
    aud: String,
    exp: u64,
    iat: u64,
}

/// This is a helper function that generates a token
///
/// Returns (token serialized as String, , claims)
/// Returns:
/// - 0: the token serialized as a String
/// - 1: the public jwk serialized as a String
/// - 2: the claims of the token as a `Claims` struct
pub fn generate_token(algorithm: jwt::Algorithm, expired: bool) -> (String, String, Claims) {
    let jwk_alg = match algorithm {
        jwt::Algorithm::HS256 => jwk::Algorithm::HS256,
        jwt::Algorithm::ES256 => jwk::Algorithm::ES256,
        jwt::Algorithm::RS256 => jwk::Algorithm::RS256,
        _ => panic!("not implemented"),
    };

    let (enc_key, dec_key) = generate_key_pair(jwk_alg);

    let claims = generate_claims(expired);
    let header = jwt::Header {
        typ: Some("JWT".to_string()),
        alg: algorithm,
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

fn generate_claims(expired: bool) -> Claims {
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

    Claims {
        iss: "https://auth.myapp.com".to_string(),
        sub: "user123".to_string(),
        aud: "myapp.com".to_string(),
        exp,
        iat,
    }
}

/// This is a helper function that generates a pair of private and public keys
/// that should be used to mimic the actual implementation
///
/// Returns:
/// - 0: a tuple of (key_id, encoding_key)
/// - 1: a JWK serialized to a string
fn generate_key_pair(algorithm: jwk::Algorithm) -> ((String, jwt::EncodingKey), String) {
    let key_id = "some_key_id".to_string();

    // Generate a private key
    let mut jwk = match algorithm {
        Algorithm::HS256 => jwk::JsonWebKey::new(jwk::Key::generate_symmetric(256)),
        _ => jwk::JsonWebKey::new(jwk::Key::generate_p256()),
    };

    jwk.set_algorithm(algorithm)
        .expect("should set encryption algorithm");
    jwk.key_id = Some(key_id.clone());

    // since `.to_public()` will error if used on a symmetric key
    let public_key = match algorithm {
        // I can't figure out how to get the symmetric key from the docs...
        // probably just use asymmetric keys for now for testing
        Algorithm::HS256 => todo!(),
        _ => serde_json::to_string(&jwk.key.to_public()).expect("should serialize public key"),
    };

    let private_key = match algorithm {
        Algorithm::HS256 => {
            jwt::EncodingKey::from_base64_secret(&public_key).expect("should generate encoding key")
        },
        _ => jwt::EncodingKey::from_ec_pem(jwk.key.to_pem().as_bytes())
            .expect("should generate encoding key"),
    };

    ((key_id, private_key), public_key)
}

/// This is a helper function that generates a set of private and public keys
/// that should be used to mimic the actual implementation
///
/// Returns:
/// - 0: a Vec of (key_id, encoding_key)
/// - 1: a JWKS serialized to a String
pub fn generate_keys(algorithm: jwk::Algorithm) -> (Vec<(String, jwt::EncodingKey)>, String) {
    let mut public_keys = jwt::jwk::JwkSet { keys: vec![] };
    let mut private_keys = vec![];

    for kid in 1..=5 {
        // Generate a private key
        let mut jwk = match algorithm {
            Algorithm::HS256 => jwk::JsonWebKey::new(jwk::Key::generate_symmetric(256)),
            _ => jwk::JsonWebKey::new(jwk::Key::generate_p256()),
        };
        jwk.set_algorithm(algorithm)
            .expect("should set encryption algorithm");
        jwk.key_id = Some("some_id".to_string());

        // since `.to_public()` will error if used on a symmetric key
        let public_key = match algorithm {
            Algorithm::HS256 => {
                serde_json::to_string(&jwk.key).expect("should serialize public key")
            },
            _ => serde_json::to_string(&jwk.key.to_public()).expect("should serialize public key"),
        };

        let key: jwt::jwk::Jwk =
            serde_json::from_str(&public_key).expect("should deserialize public key");
        public_keys.keys.push(key);

        let private_key = jwt::EncodingKey::from_ec_pem(jwk.key.to_pem().as_bytes())
            .expect("should generate encoding key");
        private_keys.push((kid.to_string(), private_key));
    }

    let public_keys = serde_json::to_string(&public_keys).expect("should serialize keyset");
    (private_keys, public_keys)
}

/// Generates a token and signs it using the specified keys and algorithm
///
/// Returns:
/// - 0: the token serialized as a String
/// - 1: the token's claims as a `Claims` struct
pub fn generate_token_using_keys(
    algorithm: jwt::Algorithm,
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
        alg: algorithm,
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
