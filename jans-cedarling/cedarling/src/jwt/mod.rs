// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! # `JwtEngine`
//!
//! The `JwtEngine` is responsible for handling JSON Web Tokens (JWTs). It provides
//! robust functionality to support authentication and authorization flows, including:
//!
//! - Fetching and storing decoding keys from a JSON Web Key Set (JWKS) provided by
//!     Identity Providers (IDPs).
//! - Extracting and processing claims from JWTs.
//! - Validating JWT signatures to ensure token integrity and authenticity.
//! - Verifying token validity based on standard claims such as expiration (`exp`) and
//!     audience (`aud`).
//!
//! ## Initialization
//!
//! The behavior of the `JwtEngine` is determined by parameters passed to [`JwtService::new`].
//! These parameters are primarily configured via the [`jwt_config`] argument:
//!
//! - **JWKS (Optional)**: A JWKS string can be provided through the
//!     `CEDARLING_LOCAL_JWKS` bootstrap property.
//! - **Signature Validation**: JWT signature verification is supported using a wrapper
//!     around the [`jsonwebtoken`] crate.
//! - **Status Validation (WIP)**: Support for token status validation is being
//!     developed in accordance with the [`IETF draft spec`].
//! - **Algorithm Restrictions**: Only tokens signed using supported algorithms will
//!     be validated. Tokens with unsupported algorithms will trigger a warning.
//!
//! Additionally, you can provide a list of **trusted issuers** during initialization.
//! Only tokens issued by these trusted issuers, as defined in the [`policy store`],
//! should be validated.
//!
//! [`jwt_config`]: JwtConfig
//! [`policy store`]: crate::common::policy_store::PolicyStore
//! [`IETF draft spec`]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-10.html
//!
//! ## Usage
//!
//! The primary interface for token validation is [`JwtService::validate_tokens`].
//!
//! This method accepts a [`HashMap<String, String>`] representing the tokens, typically
//! passed in through [`Cedarling::authorize`]. Each entry in the map is a
//! `(token_name, jwt_string)` pair.
//!
//! Only tokens that:
//! - match a trusted issuer defined in the [`policy store`], and
//! - have a matching token name,
//!
//! will be processed. Untrusted tokens are ignored with a warning.
//!
//! If any token is invalid (e.g., malformed, expired, or fails validation), the
//! method returns an error. Successfully validated tokens are returned in a `HashMap`
//! keyed by token name.
//!
//! [`Cedarling::authorize`]: crate::Cedarling::authorize
//!
//! ## Security Features
//!
//! - [x] Only Accept tokens defined from the policy store
//! - [ ] JWK rotation (WIP): The service should automatically fetch new keys if the old
//!     ones expire.
//! - [ ] Statuslist Check (WIP): The `status` claim of a JWT should be validated if 
//!     present

mod decode;
mod error;
mod http_utils;
mod key_service;
mod log_entry;
mod token;
mod validation;
mod validator_store;

#[cfg(test)]
#[allow(dead_code)]
mod test_utils;

pub use error::*;
pub use token::{Token, TokenClaimTypeError, TokenClaims};

use crate::JwtConfig;
use crate::LogLevel;
use crate::LogWriter;
use crate::common::policy_store::TrustedIssuer;
use crate::log::Logger;
use decode::*;
use http_utils::*;
use key_service::*;
use log_entry::*;
use std::collections::HashMap;
use std::sync::Arc;
use validation::*;
use validator_store::*;

/// Handles JWT validation
pub struct JwtService {
    validators: ValidatorStore,
    key_service: Arc<KeyService>,
    validate_jwt_signatures: bool,
    issuer_configs: HashMap<String, IssuerConfig>,
    logger: Option<Logger>,
}

struct IssuerConfig {
    issuer_id: String,
    /// The [`TrustedIssuer`] config loaded from the policy store
    policy: TrustedIssuer,
    /// The [`OpenIdConfig`] loaded from the IDP's `/.well-known/openid-configuration` endpoint
    openid_config: Option<OpenIdConfig>,
}

impl JwtService {
    pub async fn new(
        jwt_config: &JwtConfig,
        trusted_issuers: Option<HashMap<String, TrustedIssuer>>,
        logger: Option<Logger>,
    ) -> Result<Self, JwtServiceInitError> {
        let mut issuer_configs = HashMap::default();
        let mut validators = ValidatorStore::default();
        let mut key_service = KeyService::new();

        for (issuer_id, iss) in trusted_issuers.unwrap_or_default().into_iter() {
            // this is what we expect to find in the JWT `iss` claim
            let mut iss_claim = iss.oidc_endpoint.origin().ascii_serialization();

            let mut iss_config = IssuerConfig {
                issuer_id,
                policy: iss,
                openid_config: None,
            };

            if jwt_config.jwt_sig_validation {
                iss_claim = update_openid_config(&mut iss_config, &logger).await?;
            }

            insert_keys(&mut key_service, jwt_config, &iss_config).await?;

            update_jwt_validators(&mut validators, jwt_config, &iss_config, &logger);

            issuer_configs.insert(iss_claim, iss_config);
        }

        // quick check so we don't get surprised if the program runs but can't validate
        // anything
        if !key_service.has_keys() && jwt_config.jwt_sig_validation {
            return Err(JwtServiceInitError::KeyServiceMissingKeys);
        }
        let key_service = Arc::new(key_service);

        Ok(Self {
            validators,
            key_service,
            validate_jwt_signatures: jwt_config.jwt_sig_validation,
            issuer_configs,
            logger,
        })
    }

    pub async fn validate_tokens<'a>(
        &'a self,
        tokens: &'a HashMap<String, String>,
    ) -> Result<HashMap<String, Token<'a>>, JwtProcessingError> {
        let mut validated_tokens = HashMap::new();

        for (token_name, jwt) in tokens.iter() {
            let validation_result = self.validate_single_token(token_name.clone(), jwt);
            let Ok(validated_jwt) = validation_result else {
                match validation_result.unwrap_err() {
                    ValidateJwtError::MissingValidator(iss) => {
                        self.logger.log_any(JwtLogEntry::new(
                            format!(
                                "ignoring {token_name} since it's from an untrusted issuer: '{iss:?}'"
                            ),
                            Some(LogLevel::WARN),
                        ));
                        continue;
                    },
                    err => return Err(JwtProcessingError::ValidateJwt(token_name.clone(), err)),
                }
            };

            let claims = serde_json::from_value::<TokenClaims>(validated_jwt.claims)
                .map_err(|err| {
                    self.logger.log_any(JwtLogEntry::new(
                        format!("failed to deserialize token claims: {err}"),
                        Some(LogLevel::ERROR),
                    ));
                    err
                })
                .map_err(JwtProcessingError::StringDeserialization)?;

            validated_tokens.insert(
                token_name.to_string(),
                Token::new(token_name, claims, validated_jwt.trusted_iss),
            );
        }

        Ok(validated_tokens)
    }

    fn validate_single_token(
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
        let validator_key = ValidatorInfo {
            iss: decoded_jwt.iss(),
            token_name: &token_name,
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

        Ok(validated_jwt)
    }

    /// Use the `iss` claim of a token to retrieve a reference to a [`TrustedIssuer`]
    #[inline]
    fn get_issuer_ref(&self, iss_claim: &str) -> Option<&TrustedIssuer> {
        self.issuer_configs
            .get(iss_claim)
            .map(|config| &config.policy)
    }
}

async fn update_openid_config(
    iss_config: &mut IssuerConfig,
    logger: &Option<Logger>,
) -> Result<String, JwtServiceInitError> {
    let openid_config = OpenIdConfig::get_from_url(&iss_config.policy.oidc_endpoint)
        .await
        .inspect_err(|e| {
            logger.log_any(JwtLogEntry::new(
                format!(
                    "failed to get openid configuration for trusted issuer: '{}': {}",
                    iss_config.issuer_id, e
                ),
                Some(LogLevel::ERROR),
            ))
        })?;

    let iss_claim = openid_config.issuer.clone();
    iss_config.openid_config = Some(openid_config);

    Ok(iss_claim)
}

async fn insert_keys(
    key_service: &mut KeyService,
    jwt_config: &JwtConfig,
    iss_config: &IssuerConfig,
) -> Result<(), KeyServiceError> {
    if !jwt_config.jwt_sig_validation {
        return Ok(());
    }

    if let Some(jwks) = jwt_config.jwks.as_ref() {
        key_service.insert_keys_from_str(jwks)?;
    }

    if let Some(openid_config) = iss_config.openid_config.as_ref() {
        key_service.get_keys(openid_config).await?;
    }

    Ok(())
}

fn update_jwt_validators(
    validators: &mut ValidatorStore,
    jwt_config: &JwtConfig,
    iss_config: &IssuerConfig,
    logger: &Option<Logger>,
) {
    for (token_name, tkn_metadata) in iss_config.policy.token_metadata.iter() {
        if !tkn_metadata.trusted {
            logger.log_any(JwtLogEntry::new(
                format!(
                    "skipping metadata for '{}' from '{}' since `trusted == false`",
                    token_name, iss_config.issuer_id,
                ),
                Some(LogLevel::WARN),
            ));
            continue;
        }

        for algorithm in jwt_config.signature_algorithms_supported.iter().copied() {
            let iss = iss_config
                .openid_config
                .as_ref()
                .map(|oidc| oidc.issuer.clone())
                .unwrap_or_else(|| {
                    iss_config
                        .policy
                        .oidc_endpoint
                        .origin()
                        .ascii_serialization()
                });
            let (validator, key) =
                JwtValidator::new(Some(&iss), token_name, tkn_metadata, algorithm);
            validators.insert(key, validator);
        }
    }
}

#[cfg(test)]
mod test {
    use super::test_utils::*;
    use super::{JwtService, Token};
    use crate::JwtConfig;
    use jsonwebtoken::Algorithm;
    use serde_json::{Value, json};
    use std::collections::{HashMap, HashSet};
    use tokio::test;

    #[test]
    pub async fn can_validate_token() {
        let mut server = MockServer::new_with_defaults().await.unwrap();

        // create tokens
        let access_tkn_claims = json!({
            "iss": server.issuer(),
            "sub": "some_sub",
            "jti": 1231231231,
            "exp": u64::MAX,
            "client_id": "test123",
        });
        let access_tkn = server
            .generate_token_string_hs256(TokenTypeHeader::Jwt, &access_tkn_claims)
            .unwrap();
        let id_tkn_claims = json!({
            "iss": server.issuer(),
            "aud": "test123",
            "sub": "some_sub",
            "name": "John Doe",
            "exp": u64::MAX,
        });
        let id_tkn = server
            .generate_token_string_hs256(TokenTypeHeader::Jwt, &id_tkn_claims)
            .unwrap();
        let userinfo_tkn_claims = json!({
            "iss": server.issuer(),
            "aud": "test123",
            "sub": "some_sub",
            "name": "John Doe",
            "exp": u64::MAX,
        });
        let userinfo_tkn = server
            .generate_token_string_hs256(TokenTypeHeader::Jwt, &userinfo_tkn_claims)
            .unwrap();

        let iss = server.trusted_issuer();

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
    }
}
