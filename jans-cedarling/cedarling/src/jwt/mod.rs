// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! # `JwtEngine`
//!
//! The `JwtEngine` is designed for managing JSON Web Tokens (JWTs) and provides the following functionalities:
//! - Fetching decoding keys from a JSON Web Key Set (JWKS) provided by Identity Providers (IDPs) and storing these keys.
//! - Extracting claims from JWTs for further processing and validation.
//! - Validating the signatures of JWTs to ensure their integrity and authenticity.
//! - Verifying the validity of JWTs based on claims such as expiration time and audience.

mod issuers_store;
mod jwk_store;
mod key_service;
#[cfg(test)]
mod test_utils;
mod token;
mod validator;

use crate::JwtConfig;
use crate::common::policy_store::TrustedIssuer;
use base64::DecodeError;
use base64::Engine;
use base64::prelude::*;
use key_service::{KeyService, KeyServiceError};
use std::collections::{HashMap, HashSet};
use std::sync::Arc;
use validator::{JwtValidator, JwtValidatorConfig, JwtValidatorError};

pub use jsonwebtoken::Algorithm;
pub use token::{Token, TokenClaimTypeError, TokenClaims};

/// Type alias for Trusted Issuers' ID.
type TrustedIssuerId = Arc<str>;

/// Type alias for a Json Web Key ID (`kid`).
type KeyId = Box<str>;

#[derive(Debug, thiserror::Error)]
pub enum JwtProcessingError {
    #[error("Invalid token `{0}`: {1}")]
    InvalidToken(String, JwtValidatorError),
    #[error("Failed to deserialize from Value to String: {0}")]
    StringDeserialization(#[from] serde_json::Error),
    #[error("error while trying to parse issuer from token: {0}")]
    GetIss(#[from] DecodeJwtError),
}

#[derive(Debug, thiserror::Error)]
pub enum JwtServiceInitError {
    #[error(
        "Failed to initialize Key Service for JwtService due to a conflictig config: both a local \
         JWKS and trusted issuers was provided."
    )]
    ConflictingJwksConfig,
    #[error(
        "Failed to initialize Key Service for JwtService due to a missing config: no local JWKS \
         or trusted issuers was provided."
    )]
    MissingJwksConfig,
    #[error("Failed to initialize Key Service: {0}")]
    KeyService(#[from] KeyServiceError),
    #[error("Encountered an unsupported algorithm in the config: {0}")]
    UnsupportedAlgorithm(String),
    #[error("Failed to initialize JwtValidator: {0}")]
    InitJwtValidator(#[from] JwtValidatorError),
    #[error("failed to parse the openid_configuration_endpoint for the trusted issuer `{0}`: {1}")]
    ParseOidcUrl(String, url::ParseError),
}

pub struct JwtService {
    validators: HashMap<ValidatorId, JwtValidator>,
}

#[derive(Eq, Hash, PartialEq, Debug)]
struct ValidatorId {
    iss: Option<String>,
    token_name: String,
}

impl JwtService {
    pub async fn new(
        config: &JwtConfig,
        trusted_issuers: Option<HashMap<String, TrustedIssuer>>,
    ) -> Result<Self, JwtServiceInitError> {
        let key_service: Arc<_> =
            match (&config.jwt_sig_validation, &config.jwks, &trusted_issuers) {
                // Case: no JWKS provided
                (true, None, None) => Err(JwtServiceInitError::MissingJwksConfig)?,
                // Case: Trusted issuers provided
                (true, None, Some(issuers)) => Some(
                    KeyService::new_from_trusted_issuers(issuers)
                        .await
                        .map_err(JwtServiceInitError::KeyService)?,
                ),
                // Case: Local JWKS provided
                (true, Some(jwks), None) => {
                    Some(KeyService::new_from_str(jwks).map_err(JwtServiceInitError::KeyService)?)
                },
                // Case: Both a local JWKS and trusted issuers were provided
                (true, Some(_), Some(_)) => Err(JwtServiceInitError::ConflictingJwksConfig)?,
                // Case: Signature validation is Off so no key service is needed.
                _ => None,
            }
            .into();

        // prepare shared configs
        let sig_validation: Arc<_> = config.jwt_sig_validation.into();
        let status_validation: Arc<_> = config.jwt_status_validation.into();
        let trusted_issuers: Arc<_> = trusted_issuers.clone().into();
        let algs_supported: Arc<HashSet<Algorithm>> =
            config.signature_algorithms_supported.clone().into();

        let mut validators = HashMap::new();
        if let Some(issuers) = trusted_issuers.as_ref() {
            for iss in issuers.values() {
                let origin = iss.oidc_endpoint.origin().ascii_serialization();
                for (tkn, metadata) in iss.tokens_metadata.iter() {
                    if !metadata.trusted {
                        continue;
                    }

                    let required_claims = metadata.required_claims.clone();
                    let validate_exp = required_claims.contains("exp");
                    let validate_nbf = required_claims.contains("nbf");
                    let validator = JwtValidator::new(
                        JwtValidatorConfig {
                            sig_validation: sig_validation.clone(),
                            status_validation: status_validation.clone(),
                            trusted_issuers: trusted_issuers.clone(),
                            algs_supported: algs_supported.clone(),
                            required_claims: required_claims
                                .into_iter()
                                .map(|x| x.into_boxed_str())
                                .collect(),
                            validate_exp,
                            validate_nbf,
                        },
                        key_service.clone(),
                    )?;
                    let id = ValidatorId {
                        iss: Some(origin.clone()),
                        token_name: tkn.clone(),
                    };
                    validators.insert(id, validator);
                }
            }
        }

        Ok(Self { validators })
    }

    pub async fn validate_tokens<'a>(
        &'a self,
        tokens: &'a HashMap<String, String>,
    ) -> Result<HashMap<String, Token<'a>>, JwtProcessingError> {
        let mut validated_tokens = HashMap::new();
        for (token_name, jwt) in tokens.iter() {
            // we do a deserialization here to get the issuer since we use
            // it to store the validators but the validators do another
            // deserialization so we are doing that operation twice,
            //
            // can't really easily fix this right now since the jsonwebtoken
            // crate doesn't support validation without deserialization.
            let claims = decode_without_validation(jwt)?;
            let iss = claims
                .get_claim("iss")
                .and_then(|x| x.as_str().ok().map(|x| x.to_string()));

            let validator_id = ValidatorId {
                iss: iss.clone(),
                token_name: token_name.clone(),
            };
            let validator = if let Some(validator) = self.validators.get(&validator_id) {
                validator
            } else {
                // we just ignore input tokens that are not defined
                // in the token entity mapper bootstrap config
                //
                // TODO: should we log that we skip some tokens?
                continue;
            };

            let validated_jwt = validator
                .process_jwt(jwt)
                .map_err(|e| JwtProcessingError::InvalidToken(token_name.to_string(), e))?;
            let claims = serde_json::from_value::<TokenClaims>(validated_jwt.claims)
                .map_err(JwtProcessingError::StringDeserialization)?;
            validated_tokens.insert(
                token_name.to_string(),
                Token::new(token_name, claims, validated_jwt.trusted_iss),
            );
        }

        Ok(validated_tokens)
    }
}

#[derive(Debug, thiserror::Error)]
pub enum DecodeJwtError {
    #[error("invalid JWT. the JWT must be of form: header.body.signature")]
    InvalidJwt,
    #[error("failed to decode JWT from base64 encoding: {0}")]
    DecodeFromB64(#[from] DecodeError),
    #[error("failed to deserialize JWT from base64 encoding: {0}")]
    DeserializeJwt(#[from] serde_json::Error),
}

fn decode_without_validation(jwt: &str) -> Result<TokenClaims, DecodeJwtError> {
    let parts = jwt.split(".").collect::<Vec<&str>>();
    if parts.len() != 3 {
        return Err(DecodeJwtError::InvalidJwt);
    }
    let decoded = &BASE64_STANDARD_NO_PAD.decode(parts[1])?;
    Ok(serde_json::from_slice::<TokenClaims>(decoded)?)
}

#[cfg(test)]
mod test {
    use super::test_utils::*;
    use super::{JwtService, Token};
    use crate::JwtConfig;
    use crate::common::policy_store::TrustedIssuer;
    use jsonwebtoken::Algorithm;
    use mockito::Server;
    use serde_json::{Value, json};
    use std::collections::{HashMap, HashSet};
    use tokio::test;
    use url::Url;

    #[test]
    pub async fn can_validate_token() {
        let mut server = Server::new_async().await;

        let oidc_endpoint = server
            .mock("GET", "/.well-known/openid-configuration")
            .with_status(200)
            .with_header("content-type", "application/json")
            .with_body(
                json!({
                    "issuer": server.url(),
                    "jwks_uri": server.url() + "/jwks",
                })
                .to_string(),
            )
            .expect(1)
            .create();

        let keys = generate_keypair_hs256(Some("some_hs256_key")).expect("Should generate keys");
        let access_tkn_claims = json!({
            "iss": server.url(),
            "sub": "some_sub",
            "jti": 1231231231,
            "exp": u64::MAX,
            "client_id": "test123",
        });
        let access_tkn = generate_token_using_claims(&access_tkn_claims, &keys)
            .expect("Should generate access token");
        let id_tkn_claims = json!({
            "iss": server.url(),
            "aud": "test123",
            "sub": "some_sub",
            "name": "John Doe",
            "exp": u64::MAX,
        });
        let id_tkn =
            generate_token_using_claims(&id_tkn_claims, &keys).expect("Should generate id token");
        let userinfo_tkn_claims = json!({
            "iss": server.url(),
            "aud": "test123",
            "sub": "some_sub",
            "name": "John Doe",
            "exp": u64::MAX,
        });
        let userinfo_tkn = generate_token_using_claims(&userinfo_tkn_claims, &keys)
            .expect("Should generate userinfo token");

        let jwks_endpoint = server
            .mock("GET", "/jwks")
            .with_status(200)
            .with_header("content-type", "application/json")
            .with_body(json!({"keys": generate_jwks(&vec![keys]).keys}).to_string())
            .expect(1)
            .create();

        let mut iss = TrustedIssuer::default();
        iss.oidc_endpoint = Url::parse(&(server.url() + "/.well-known/openid-configuration"))
            .expect("should be a valid url");
        // we remove the `iss` claims since mockito can't really create https
        // endpoints and the validation requires the `iss` to be https.
        for (_name, metadata) in iss.tokens_metadata.iter_mut() {
            metadata.required_claims.remove("iss");
        }

        let jwt_service = JwtService::new(
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: true,
                jwt_status_validation: false,
                signature_algorithms_supported: HashSet::from_iter([Algorithm::HS256]),
            },
            Some(HashMap::from([("Jans".into(), iss.clone())])),
        )
        .await
        .inspect_err(|e| eprintln!("error msg: {}", e))
        .expect("Should create JwtService");

        let tokens = HashMap::from([
            ("access_token".to_string(), access_tkn),
            ("id_token".to_string(), id_tkn),
            ("userinfo_token".to_string(), userinfo_tkn),
        ]);
        let validated_tokens = jwt_service
            .validate_tokens(&tokens)
            .await
            .expect("should validate tokens");

        // Test access_token
        let token = validated_tokens
            .get("access_token")
            .expect("should have an access_token");
        let expected_claims = serde_json::from_value::<HashMap<String, Value>>(access_tkn_claims)
            .expect("Should create expected access_token claims");
        assert_eq!(
            token,
            &Token::new("access_token", expected_claims.into(), Some(&iss))
        );

        // Test id_token
        let token = validated_tokens
            .get("id_token")
            .expect("should have an id_token");
        let expected_claims = serde_json::from_value::<HashMap<String, Value>>(id_tkn_claims)
            .expect("Should create expected id_token claims");
        assert_eq!(
            token,
            &Token::new("id_token", expected_claims.into(), Some(&iss))
        );

        // Test userinfo_token
        let token = validated_tokens
            .get("userinfo_token")
            .expect("should have an userinfo_token");
        let expected_claims = serde_json::from_value::<HashMap<String, Value>>(userinfo_tkn_claims)
            .expect("Should create expected userinfo_token claims");
        assert_eq!(
            token,
            &Token::new("userinfo_token", expected_claims.into(), Some(&iss))
        );

        oidc_endpoint.assert();
        jwks_endpoint.assert();
    }
}
