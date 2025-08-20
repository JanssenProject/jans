// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::sync::LazyLock;
use std::time::{Duration, SystemTime, UNIX_EPOCH};

use super::http_utils::OpenIdConfig;
use super::status_list::{self, StatusBitSize};
use super::validation::RefJwtStatusList;
use crate::common::policy_store::TrustedIssuer;
use jsonwebtoken::DecodingKey;
use mockito::{Mock, Server, ServerGuard};
use reqwest::Client;
use serde::Serialize;
use serde_json::{Value, json};
use url::Url;

use {jsonwebkey as jwk, jsonwebtoken as jwt};

/// A pair of encoding and decoding keys.
#[derive(Clone)]
pub struct KeyPair {
    kid: Option<String>,
    encoding_key: jwt::EncodingKey,
    decoding_key: jwt::jwk::Jwk,
    alg: jwt::Algorithm,
}

impl KeyPair {
    pub fn decoding_key(&self) -> Result<DecodingKey, jsonwebtoken::errors::Error> {
        DecodingKey::from_jwk(&self.decoding_key)
    }
}

#[derive(Debug, thiserror::Error)]
pub enum KeyGenerationError {
    #[error("Failed to serialize the decoding key onto the right struct")]
    SerializeDecodingKey(#[from] serde_json::Error),
    #[error("The given key was generated with the wrong algorithm")]
    KeyMismatch,
}

/// Generates a HS256-signed token using the given claims.
pub fn generate_keypair_hs256(kid: Option<impl ToString>) -> Result<KeyPair, KeyGenerationError> {
    let mut jwk = jwk::JsonWebKey::new(jwk::Key::generate_symmetric(256));
    jwk.set_algorithm(jwk::Algorithm::HS256)
        .expect("should set encryption algorithm");
    jwk.key_id = Some("some_id".to_string());

    // since this is a symmetric key, the public key is the same as the private
    let mut decoding_key = serde_json::to_value(jwk.key.clone())?;

    // set the key parameters
    if let Some(kid) = &kid {
        decoding_key["kid"] = serde_json::Value::String(kid.to_string());
    }
    let mut decoding_key: jwt::jwk::Jwk = serde_json::from_value(decoding_key)?;
    decoding_key.common.key_algorithm = Some(jwt::jwk::KeyAlgorithm::HS256);

    let encoding_key = match *jwk.key {
        jsonwebkey::Key::Symmetric { key } => jwt::EncodingKey::from_secret(&key),
        _ => Err(KeyGenerationError::KeyMismatch)?,
    };

    Ok(KeyPair {
        kid: kid.map(|s| s.to_string()),
        encoding_key,
        decoding_key,
        alg: jwt::Algorithm::HS256,
    })
}

#[derive(Debug, thiserror::Error)]
pub enum TokenGenerationError {
    #[error("Failed to encode token into a JWT string")]
    Encode(#[from] jwt::errors::Error),
}

/// Generates a token string in the given format: `"header.claim.signature"`
pub fn generate_token_using_claims(
    claims: &impl Serialize,
    keypair: &KeyPair,
) -> Result<String, TokenGenerationError> {
    let header = jwt::Header {
        alg: keypair.alg,
        kid: keypair.kid.clone(),
        ..Default::default()
    };

    // serialize token to a string
    Ok(jwt::encode(&header, &claims, &keypair.encoding_key)?)
}

/// Generates a JwkSet from the given keys
pub fn generate_jwks(keys: &[KeyPair]) -> jwt::jwk::JwkSet {
    let keys = keys
        .iter()
        .map(|key_pair| key_pair.decoding_key.clone())
        .collect::<Vec<jwt::jwk::Jwk>>();
    jwt::jwk::JwkSet { keys }
}

pub struct MockServer {
    pub endpoints: MockEndpoints,
    server: ServerGuard,
    keys: KeyPair,
}

pub struct MockEndpoints {
    pub oidc: Option<Mock>,
    pub jwks: Option<Mock>,
    pub status_list: Option<Mock>,
}

impl MockEndpoints {
    pub fn new_with_defaults(server: &mut Server, keys: &KeyPair) -> Self {
        let oidc = Some(
            server
                .mock("GET", "/.well-known/openid-configuration")
                .with_status(200)
                .with_header("content-type", "application/json")
                .with_body(
                    json!({
                        "issuer": server.url(),
                        "jwks_uri": server.url() + MOCK_JWKS_URI,
                    })
                    .to_string(),
                )
                .expect(1)
                .create(),
        );

        let jwks = Some(
            server
                .mock("GET", MOCK_JWKS_URI)
                .with_status(200)
                .with_header("content-type", "application/json")
                .with_body(json!({"keys": generate_jwks(&[keys.clone()]).keys}).to_string())
                .expect(1)
                .create(),
        );

        Self {
            oidc,
            jwks,
            status_list: None,
        }
    }

    #[track_caller]
    pub fn assert(&self) {
        self.oidc.as_ref().map(|x| x.assert());
        self.jwks.as_ref().map(|x| x.assert());
        self.status_list.as_ref().map(|x| x.assert());
    }
}

#[derive(Clone, Copy)]
pub enum TokenTypeHeader {
    /// "typ": "statuslist+jwt"
    StatusListJwt,
    /// "typ": "JWT"
    Jwt,
}

impl Into<&str> for TokenTypeHeader {
    fn into(self) -> &'static str {
        match self {
            TokenTypeHeader::StatusListJwt => "statuslist+jwt",
            TokenTypeHeader::Jwt => "JWT",
        }
    }
}

impl Into<Option<String>> for TokenTypeHeader {
    fn into(self) -> Option<String> {
        let typ_str: &str = self.into();
        Some(typ_str.into())
    }
}

const MOCK_OIDC_ENDPOINT: &str = "/.well-known/openid-configuration";
const MOCK_STATUS_LIST_ENDPOINT: &str = "/jans-auth/restv1/status_list";
const MOCK_JWKS_URI: &str = "/jans-auth/restv1/jwks";

impl MockServer {
    pub async fn new_with_defaults() -> Result<Self, KeyGenerationError> {
        let mut server = Server::new_async().await;

        let keys = generate_keypair_hs256(Some("some_hs256_key"))?;

        let endpoints = MockEndpoints::new_with_defaults(&mut server, &keys);

        Ok(Self {
            server,
            endpoints,
            keys,
        })
    }

    /// Generates an HS256 signed token string in the given format: `"header.claim.signature"`
    ///
    /// If `jwt_status_idx` is [`Some`], the jwt will also havae a `status` claim.
    #[track_caller]
    pub fn generate_token_with_hs256sig(
        &mut self,
        claims: &mut Value,
        jwt_status_idx: Option<usize>,
    ) -> Result<String, TokenGenerationError> {
        let header = jwt::Header {
            alg: self.keys.alg,
            kid: self.keys.kid.clone(),
            typ: TokenTypeHeader::StatusListJwt.into(),
            ..Default::default()
        };

        claims["iss"] = json!(self.server.url());

        if let Some(idx) = jwt_status_idx {
            claims["status"] = json!({"status_list": RefJwtStatusList {
                idx,
                uri: self
                    .status_list_endpoint()
                    .expect("the status list endpoint hasn't been generated yet. call `generate_status_list_endpoint` first")
                    .to_string(),
                ttl: None,
            }});
        }

        // serialize token to a string
        let jwt = jwt::encode(&header, &claims, &self.keys.encoding_key)?;

        Ok(jwt)
    }

    /// Generates a [`TrustedIssuer`] for this instance of the [`MockServer`].
    pub fn trusted_issuer(&self) -> TrustedIssuer {
        TrustedIssuer {
            oidc_endpoint: Url::parse(&(self.server.url() + MOCK_OIDC_ENDPOINT))
                .expect("should be a valid url"),
            ..Default::default()
        }
    }

    /// Use this to generate the status list JWT
    #[track_caller]
    pub fn generate_status_list_endpoint(
        &mut self,
        status_list_bits: StatusBitSize,
        status_list: &[u8],
        ttl: Option<u64>,
    ) {
        // building the status list JWT
        let bits: u8 = status_list_bits.into();
        let lst = status_list::compress_and_encode(status_list);
        let sub = format!("{}{}", self.server.url(), MOCK_STATUS_LIST_ENDPOINT);
        let iss = self.server.url();
        let header = jwt::Header {
            alg: self.keys.alg,
            kid: self.keys.kid.clone(),
            typ: TokenTypeHeader::StatusListJwt.into(),
            ..Default::default()
        };
        let encoding_key = self.keys.encoding_key.clone();
        let build_jwt_claims = move || {
            let iat = SystemTime::now().duration_since(UNIX_EPOCH).unwrap();
            let exp = iat + Duration::from_secs(3600); // defaults to 1 hour
            let ttl = ttl
                .map(|x| Duration::from_secs(x))
                .unwrap_or_else(|| 
                    // defaults to 5 mins if the ttl is None
                    Duration::from_secs(600)
                );
            let claims = json!({
                "sub": sub,
                "status_list": {
                  "bits": bits,
                  "lst": lst,
                },
                "iss": iss,
                "exp": exp.as_secs(),
                "ttl": ttl.as_secs(),
                "iat": iat.as_secs(),
            });

            jwt::encode(&header, &claims, &encoding_key)
                .expect("encode status list JWT")
                .as_bytes()
                .to_vec()
        };

        let endpoint = Some(
            self.server
                .mock("GET", MOCK_STATUS_LIST_ENDPOINT)
                .with_status(200)
                .with_header("content-type", "application/statuslist+jwt")
                .with_body_from_request(move |_| build_jwt_claims())
                .expect(1)
                .create(),
        );
        self.endpoints.status_list = endpoint;
    }

    /// Helper function for generating a status list JWT for this mock server
    pub async fn status_list_jwt(&self) -> Result<String, reqwest::Error> {
        static CLIENT: LazyLock<Client> = LazyLock::new(Client::new);
        let url = self.status_list_endpoint().expect("the status list endpoint hasn't been generated yet. call `generate_status_list_endpoint` first");
        CLIENT
            .get(url.to_string())
            .send()
            .await?
            .error_for_status()?
            .text()
            .await
    }

    pub fn status_list_endpoint(&self) -> Option<Url> {
        if self.endpoints.status_list.is_none() {
            return None;
        }

        Some(
            Url::parse(&(self.server.url() + MOCK_STATUS_LIST_ENDPOINT))
                .expect("invalid status list url"),
        )
    }

    pub fn openid_config_endpoint(&self) -> Option<Url> {
        if self.endpoints.oidc.is_none() {
            return None;
        }

        Some(
            Url::parse(&(self.server.url() + MOCK_OIDC_ENDPOINT)).expect("invalid status list url"),
        )
    }

    pub fn jwks_endpoint(&self) -> Option<Url> {
        if self.endpoints.jwks.is_none() {
            return None;
        }

        Some(Url::parse(&(self.server.url() + MOCK_JWKS_URI)).expect("invalid status list url"))
    }

    pub fn jwt_decoding_key(&self) -> Result<DecodingKey, jsonwebtoken::errors::Error> {
        self.keys.decoding_key()
    }

    pub fn jwt_decoding_key_and_id(
        &self,
    ) -> Result<(DecodingKey, Option<String>), jsonwebtoken::errors::Error> {
        Ok((self.keys.decoding_key()?, self.keys.kid.clone()))
    }

    pub fn issuer(&self) -> String {
        self.server.url()
    }

    #[track_caller]
    pub fn openid_config(&self) -> OpenIdConfig {
        OpenIdConfig {
            issuer: self.issuer(),
            jwks_uri: self.jwks_endpoint().unwrap(),
            status_list_endpoint: self.status_list_endpoint(),
        }
    }
}
