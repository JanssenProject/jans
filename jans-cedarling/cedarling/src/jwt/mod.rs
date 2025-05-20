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

mod decode;
mod error;
mod key_service;
mod log_entry;
#[allow(dead_code)]
mod status_list_service;
mod token;
mod validation;

#[cfg(test)]
#[allow(dead_code)]
mod test_utils;

use crate::JwtConfig;
use crate::LogWriter;
use crate::common::policy_store::TrustedIssuer;
use crate::log::Logger;
use decode::*;
use jsonwebtoken::Algorithm;
use key_service::*;
use log_entry::*;
use std::collections::HashMap;
use std::sync::Arc;
use validation::*;

pub use error::*;
pub use token::{Token, TokenClaimTypeError, TokenClaims};

/// Handles JWT validation
pub struct JwtService {
    validators: HashMap<ValidatorKey, JwtValidator>,
    key_service: Arc<KeyService>,
    validate_jwt_signatures: bool,
    trusted_issuers: Option<HashMap<String, TrustedIssuer>>,
    logger: Option<Logger>,
}

impl JwtService {
    pub async fn new(
        config: &JwtConfig,
        trusted_issuers: Option<HashMap<String, TrustedIssuer>>,
        logger: Option<Logger>,
    ) -> Result<Self, JwtServiceInitError> {
        // we make a map of <jwt_iss_claim, TrustedIssuer>
        // where jwt_iss_claim is what we expect to find in the `iss` claim of the JWTs
        // from the given issuer
        let trusted_issuers = trusted_issuers.map(|issuers| {
            issuers
                .into_values()
                .map(|iss| {
                    // TODO: here, we are relying on the IDP to use it's own domain
                    // for the issued tokens. We might need to fetch the openid config
                    // in the future and use the `issuer` field to support more funky IDPs.
                    let jwt_iss_claim = iss.oidc_endpoint.origin().ascii_serialization();
                    (jwt_iss_claim, iss)
                })
                .collect::<HashMap<String, TrustedIssuer>>()
        });

        // Initialize JWT validators
        let validators = trusted_issuers
            .as_ref()
            .map(|issuers| init_jwt_validators(config, issuers, logger.clone()))
            .unwrap_or_default();

        // Initialize Key Service to manage JWKs
        let mut key_service = KeyService::new();
        if config.jwt_sig_validation {
            if let Some(issuers) = trusted_issuers.as_ref() {
                for iss in issuers.values() {
                    key_service.fetch_keys_for_iss(iss).await?;
                }
            }

            if let Some(jwks) = config.jwks.as_ref() {
                key_service.insert_keys_from_str(jwks)?;
            }

            if !key_service.has_keys() && config.jwt_sig_validation {
                return Err(JwtServiceInitError::KeyServiceMissingKeys);
            }
        }
        let key_service = Arc::new(key_service);

        Ok(Self {
            validators,
            key_service,
            validate_jwt_signatures: config.jwt_sig_validation,
            trusted_issuers,
            logger,
        })
    }

    /// Helper for making [`crate::LogType::System`] logs.
    fn system_log(&self, msg: String) {
        if let Some(logger) = self.logger.as_ref() {
            logger.log_any(JwtLogEntry::system(msg));
        }
    }

    pub async fn validate_tokens<'a>(
        &'a self,
        tokens: &'a HashMap<String, String>,
    ) -> Result<HashMap<String, Token<'a>>, JwtProcessingError> {
        let mut validated_tokens = HashMap::new();

        for (token_name, jwt) in tokens.iter() {
            let validation_result = self.validate_jwt(token_name.clone(), jwt);
            let Ok(validated_jwt) = validation_result else {
                match validation_result.unwrap_err() {
                    ValidateJwtError::MissingValidator(iss) => {
                        self.system_log(format!(
                            "ignoring {token_name} since it's from an untrusted issuer: '{iss:?}'"
                        ));
                        continue;
                    },
                    err => return Err(JwtProcessingError::ValidateJwt(token_name.clone(), err)),
                }
            };

            let claims = serde_json::from_value::<TokenClaims>(validated_jwt.claims)
                .map_err(JwtProcessingError::StringDeserialization)?;
            validated_tokens.insert(
                token_name.to_string(),
                Token::new(token_name, claims, validated_jwt.trusted_iss),
            );
        }

        Ok(validated_tokens)
    }

    fn validate_jwt(
        &self,
        token_name: String,
        jwt: &str,
    ) -> Result<ValidatedJwt, ValidateJwtError> {
        let decoded_jwt = decode_jwt(jwt)?;

        if !self.validate_jwt_signatures {
            // The users of the validated JWT will need a reference to the TrustedIssuer
            // to do some processing so we include it here for convenience
            let issuer_ref = decoded_jwt.iss().and_then(|iss| self.get_issuer_ref(iss));
            return Ok(ValidatedJwt {
                claims: decoded_jwt.claims.inner,
                trusted_iss: issuer_ref,
            });
        }

        // Get decoding key
        let decoding_key_info = decoded_jwt.decoding_key_info();
        let decoding_key = self
            .key_service
            .get_key(&decoding_key_info)
            .ok_or(ValidateJwtError::MissingValidationKey)?;

        // get validator
        let validator_key = ValidatorKey {
            iss: decoded_jwt.iss().map(|x| x.to_string()),
            token_name,
            algorithm: decoded_jwt.header.alg,
        };
        let validator =
            self.validators
                .get(&validator_key)
                .ok_or(ValidateJwtError::MissingValidator(
                    decoded_jwt.iss().map(|s| s.to_string()),
                ))?;

        // validate JWT
        let mut validated_jwt = validator.validate_jwt(jwt, decoding_key)?;

        // The users of the validated JWT will need a reference to the TrustedIssuer
        // to do some processing so we include it here for convenience
        validated_jwt.trusted_iss = decoded_jwt.iss().and_then(|iss| self.get_issuer_ref(iss));

        return Ok(validated_jwt);
    }

    /// Use the `iss` claim of a token to retrieve a reference to a [`TrustedIssuer`]
    #[inline]
    fn get_issuer_ref(&self, iss: &str) -> Option<&TrustedIssuer> {
        self.trusted_issuers
            .as_ref()
            .and_then(|issuers| issuers.get(iss))
    }
}

fn init_jwt_validators(
    config: &JwtConfig,
    issuers: &HashMap<String, TrustedIssuer>,
    logger: Option<Logger>,
) -> HashMap<ValidatorKey, JwtValidator> {
    let mut validators = HashMap::default();

    for iss in issuers.values() {
        // TODO: it's better to get the OIDC and get the issuer there
        let origin = iss.oidc_endpoint.origin().ascii_serialization();

        for (token_name, metadata) in iss.token_metadata.iter() {
            if !metadata.trusted {
                if let Some(logger) = logger.as_ref() {
                    logger.log_any(JwtLogEntry::system(format!(
                    "skipping metadata for '{token_name}' from '{origin}' since `trusted == false`"
                )));
                }
                continue;
            }

            for algorithm in config.signature_algorithms_supported.iter().copied() {
                let (validator, key) = JwtValidator::new(
                    Some(origin.clone()),
                    token_name.clone(),
                    metadata,
                    algorithm,
                );
                validators.insert(key, validator);
            }
        }
    }

    validators
}

#[derive(Debug, Hash, Eq, PartialEq)]
// TODO: using strings here that we need to clone might be expensive, 
// we should find an alternative
pub struct ValidatorKey {
    iss: Option<String>,
    token_name: String,
    algorithm: Algorithm,
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

        let mut iss = TrustedIssuer {
            oidc_endpoint: Url::parse(&(server.url() + "/.well-known/openid-configuration"))
                .expect("should be a valid url"),
            ..Default::default()
        };
        // we remove the `iss` claims since mockito can't really create https
        // endpoints and the validation requires the `iss` to be https.
        for (_name, metadata) in iss.token_metadata.iter_mut() {
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
            None,
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
