/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use jsonwebkey as jwk;
use jsonwebtoken as jwt;
use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::{
    collections::HashMap,
    time::{SystemTime, UNIX_EPOCH},
    u64,
};

#[derive(Serialize, Deserialize, Debug, Clone, PartialEq, Default)]
pub struct AccessTokenClaims {
    pub iss: String,
    pub aud: String,
    pub sub: String,
    pub scopes: String,
    pub exp: u64,
    pub iat: u64,
}

impl Into<HashMap<String, Value>> for AccessTokenClaims {
    fn into(self) -> HashMap<String, Value> {
        let mut map = HashMap::new();
        map.insert("iss".to_string(), Value::String(self.iss));
        map.insert("aud".to_string(), Value::String(self.aud));
        map.insert("sub".to_string(), Value::String(self.sub));
        map.insert("scopes".to_string(), Value::String(self.scopes));
        map.insert("exp".to_string(), Value::Number(self.exp.into()));
        map.insert("iat".to_string(), Value::Number(self.iat.into()));
        map
    }
}

#[derive(Serialize, Deserialize, Debug, Clone, PartialEq, Default)]
pub struct IdTokenClaims {
    pub iss: String,
    pub sub: String,
    pub aud: String,
    pub email: String,
    pub exp: u64,
    pub iat: u64,
}

impl Into<HashMap<String, Value>> for IdTokenClaims {
    fn into(self) -> HashMap<String, Value> {
        let mut map = HashMap::new();
        map.insert("iss".to_string(), Value::String(self.iss));
        map.insert("aud".to_string(), Value::String(self.aud));
        map.insert("sub".to_string(), Value::String(self.sub));
        map.insert("email".to_string(), Value::String(self.email));
        map.insert("exp".to_string(), Value::Number(self.exp.into()));
        map.insert("iat".to_string(), Value::Number(self.iat.into()));
        map
    }
}

#[derive(Serialize, Deserialize, Debug, Clone, PartialEq, Default)]
pub struct UserinfoTokenClaims {
    pub iss: String,
    pub sub: String,
    pub aud: String,
    pub client_id: String,
    pub name: String,
    pub email: String,
}

impl Into<HashMap<String, Value>> for UserinfoTokenClaims {
    fn into(self) -> HashMap<String, Value> {
        let mut map = HashMap::new();
        map.insert("iss".to_string(), Value::String(self.iss));
        map.insert("aud".to_string(), Value::String(self.aud));
        map.insert("sub".to_string(), Value::String(self.sub));
        map.insert("client_id".to_string(), Value::String(self.client_id));
        map.insert("name".to_string(), Value::String(self.name));
        map.insert("email".to_string(), Value::String(self.email));
        map
    }
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
            serde_json::to_value(jwk.key.to_public()).expect("should serialize public key");
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

/// Generates a token string signed with ES256
pub fn generate_token_using_claims(
    claims: &impl Serialize,
    key_id: impl ToString,
    encoding_key: &jwt::EncodingKey,
) -> String {
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
