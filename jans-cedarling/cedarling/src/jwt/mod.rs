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
//!   Identity Providers (IDPs).
//! - Extracting and processing claims from JWTs.
//! - Validating JWT signatures to ensure token integrity and authenticity.
//! - Verifying token validity based on standard claims such as expiration (`exp`) and
//!   audience (`aud`).
//!
//! ## Initialization
//!
//! The behavior of the `JwtEngine` is determined by parameters passed to [`JwtService::new`].
//! These parameters are primarily configured via the [`jwt_config`] argument:
//!
//! - **JWKS (Optional)**: A JWKS string can be provided through the
//!   `CEDARLING_LOCAL_JWKS` bootstrap property.
//! - **Signature Validation**: JWT signature verification is supported using a wrapper
//!   around the [`jsonwebtoken`] crate.
//! - **Status Validation (WIP)**: Support for token status validation is being
//!   developed in accordance with the [`IETF draft spec`].
//! - **Algorithm Restrictions**: Only tokens signed using supported algorithms will
//!   be validated. Tokens with unsupported algorithms will trigger a warning.
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
//!   ones expire.
//! - [x] Statuslist Check: The `status` claim of a JWT should be validated if present.
//!   This is done through the [`status_list`] crate for the implementation.
//! - [ ] JWK rotation (WIP): The service should automatically fetch new keys if the old

mod decode;
mod error;
mod http_utils;
mod key_service;
mod log_entry;
mod status_list;
mod token;
mod validation;

#[cfg(test)]
#[allow(dead_code)]
mod test_utils;

use chrono::DateTime;
use chrono::Duration;
pub use decode::*;
pub use error::*;
pub use token::{Token, TokenClaimTypeError, TokenClaims};

use crate::JwtConfig;
use crate::LogLevel;
use crate::LogWriter;
use crate::common::issuer_utils::normalize_issuer;
use crate::common::policy_store::TrustedIssuer;
use crate::log::Logger;
use chrono::Utc;
use http_utils::*;
use key_service::*;
use log_entry::*;
use serde_json::json;
use sparkv::SparKV;
use status_list::*;
use std::collections::HashMap;
use std::sync::Arc;
use std::sync::RwLock;
use validation::*;

/// The value of the `iss` claim from a JWT
type IssClaim = String;

/// Handles JWT validation
pub struct JwtService {
    validators: JwtValidatorCache,
    key_service: Arc<KeyService>,
    issuer_configs: HashMap<IssClaim, IssuerConfig>,
    logger: Option<Logger>,
    token_cache: Arc<RwLock<SparKV<Arc<Token>>>>,
    token_cache_max_ttl: usize,
}

struct IssuerConfig {
    issuer_id: String,
    /// The [`TrustedIssuer`] config loaded from the policy store
    policy: Arc<TrustedIssuer>,
    /// The [`OpenIdConfig`] loaded from the IDP's `/.well-known/openid-configuration` endpoint
    openid_config: Option<OpenIdConfig>,
}

impl JwtService {
    pub async fn new(
        jwt_config: &JwtConfig,
        trusted_issuers: Option<HashMap<String, TrustedIssuer>>,
        logger: Option<Logger>,
        token_cache_max_ttl_sec: usize,
    ) -> Result<Self, JwtServiceInitError> {
        let mut status_lists = StatusListCache::default();
        let mut issuer_configs = HashMap::default();
        let mut validators = JwtValidatorCache::default();
        let mut key_service = KeyService::new();

        for (issuer_id, iss) in trusted_issuers.unwrap_or_default().into_iter() {
            // this is what we expect to find in the JWT `iss` claim
            let mut iss_claim = iss.oidc_endpoint.origin().ascii_serialization();

            let mut iss_config = IssuerConfig {
                issuer_id,
                policy: Arc::new(iss),
                openid_config: None,
            };

            if jwt_config.jwt_sig_validation || jwt_config.jwt_status_validation {
                iss_claim = update_openid_config(&mut iss_config, &logger).await?;
            }

            insert_keys(&mut key_service, jwt_config, &iss_config, &logger).await?;

            validators.init_for_iss(&iss_config, jwt_config, &status_lists, logger.clone());

            if jwt_config.jwt_status_validation {
                status_lists
                    .init_for_iss(&iss_config, &validators, &key_service, logger.clone())
                    .await?;
            }

            issuer_configs.insert(normalize_issuer(&iss_claim), iss_config);
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
            issuer_configs,
            logger,
            token_cache: Arc::new(RwLock::new(SparKV::new())),
            token_cache_max_ttl: token_cache_max_ttl_sec,
        })
    }

    pub async fn validate_tokens<'a>(
        &'a self,
        tokens: &'a HashMap<String, String>,
    ) -> Result<HashMap<String, Arc<Token>>, JwtProcessingError> {
        let mut validated_tokens = HashMap::new();
        const ID_TOKEN_NAME: &str = "id_token";

        let now = Utc::now();

        // clear expired tokens from cache
        self.token_cache
            .write()
            .expect("validated_jwt_cache mutex shouldn't be poisoned")
            .clear_expired();

        for (token_name, jwt) in tokens.iter() {
            let token = if let Some(validated_token) = self.find_token_in_cache(jwt) {
                validated_token
            } else {
                // validate token and save to cache
                let validated_jwt = match self.validate_single_token(token_name.clone(), jwt) {
                    Ok(jwt) => jwt,
                    Err(err) => {
                        if matches!(err, ValidateJwtError::MissingValidator(_)) {
                            self.logger
                                .log_any(JwtLogEntry::new(err.to_string(), Some(LogLevel::WARN)));
                            continue;
                        } else {
                            return Err(JwtProcessingError::ValidateJwt(token_name.clone(), err));
                        }
                    },
                };

                let mut claims = serde_json::from_value::<TokenClaims>(validated_jwt.claims)
                    .map_err(|err| {
                        self.logger.log_any(JwtLogEntry::new(
                            format!("failed to deserialize token claims: {err}"),
                            Some(LogLevel::ERROR),
                        ));
                        err
                    })
                    .map_err(JwtProcessingError::StringDeserialization)?;

                if token_name == ID_TOKEN_NAME {
                    claims = fix_aud_claim_value_to_array(claims);
                };

                let token = Arc::new(Token::new(token_name, claims, validated_jwt.trusted_iss));
                self.save_token_in_cache(jwt, token.clone(), now);
                token
            };

            validated_tokens.insert(token_name.to_string(), token);
        }

        Ok(validated_tokens)
    }

    fn find_token_in_cache(&self, jwt: &str) -> Option<Arc<Token>> {
        self.token_cache
            .read()
            .expect("validated_jwt_cache mutex shouldn't be poisoned")
            .get(&hash_str(jwt))
            .map(|v| v.to_owned())
    }

    fn save_token_in_cache(&self, jwt: &str, token: Arc<Token>, now: DateTime<Utc>) {
        let key = hash_str(jwt);

        let cache_duration_opt = token
            .claims
            .get_claim("exp")
            .and_then(|exp| exp.value().as_i64())
            .and_then(|exp| {
                // calculate duration until token expiration
                let duration = exp - now.timestamp();
                if duration > 0 {
                    // if duration bigger than configured max ttl, use the max ttl
                    Some(
                        if self.token_cache_max_ttl > 0
                            && duration > self.token_cache_max_ttl as i64
                        {
                            self.token_cache_max_ttl as i64
                        } else {
                            duration
                        },
                    )
                } else {
                    None
                }
            })
            .or({
                // if no exp claim, use the configured max ttl if set
                if self.token_cache_max_ttl > 0 {
                    Some(self.token_cache_max_ttl as i64)
                } else {
                    None
                }
            });

        if let Some(duration) = cache_duration_opt {
            let _ = self
                .token_cache
                .write()
                .expect("validated_jwt_cache mutex shouldn't be poisoned")
                .set_with_ttl(&key, token, Duration::seconds(duration), &[]);
        } else {
            // set with SparkKV default TTL (5 minutes)
            let _ = self
                .token_cache
                .write()
                .expect("validated_jwt_cache mutex shouldn't be poisoned")
                .set(&key, token, &[]);
        }
    }

    fn validate_single_token(
        &self,
        token_name: String,
        jwt: &str,
    ) -> Result<ValidatedJwt, ValidateJwtError> {
        let decoded_jwt = decode_jwt(jwt)?;

        // Get decoding key
        let decoding_key_info = decoded_jwt.decoding_key_info();
        let decoding_key = self.key_service.get_key(&decoding_key_info);

        // get validator
        let normalized_iss = decoded_jwt.iss().map(normalize_issuer);
        let validator_key = ValidatorInfo {
            iss: normalized_iss.as_deref(),
            token_kind: TokenKind::AuthzRequestInput(&token_name),
            algorithm: decoded_jwt.header.alg,
        };
        let validator: Arc<RwLock<JwtValidator>> = self
            .validators
            .get(&validator_key)
            .ok_or(ValidateJwtError::MissingValidator(validator_key.owned()))?;

        // validate JWT
        // NOTE: the JWT will be validated depending on the validator's settings that
        // was set on initialization
        let mut validated_jwt = {
            validator
                .read()
                .expect("acquire JwtValidator read lock")
                .validate_jwt(jwt, decoding_key)?
        };

        // The users of the validated JWT will need a reference to the TrustedIssuer
        // to do some processing so we include it here for convenience
        validated_jwt.trusted_iss = decoded_jwt.iss().and_then(|iss| self.get_issuer_ref(iss));

        Ok(validated_jwt)
    }

    /// Use the `iss` claim of a token to retrieve a reference to a [`TrustedIssuer`]
    #[inline]
    fn get_issuer_ref(&self, iss_claim: &str) -> Option<Arc<TrustedIssuer>> {
        self.issuer_configs
            .get(&normalize_issuer(iss_claim))
            .map(|config| config.policy.clone())
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
            ));
        })?;

    let iss_claim = openid_config.issuer.clone();
    iss_config.openid_config = Some(openid_config);

    Ok(iss_claim)
}

async fn insert_keys(
    key_service: &mut KeyService,
    jwt_config: &JwtConfig,
    iss_config: &IssuerConfig,
    logger: &Option<Logger>,
) -> Result<(), KeyServiceError> {
    if !jwt_config.jwt_sig_validation {
        return Ok(());
    }

    if let Some(jwks) = jwt_config.jwks.as_ref() {
        key_service.insert_keys_from_str(jwks)?;
    }

    if let Some(openid_config) = iss_config.openid_config.as_ref() {
        key_service
            .get_keys_using_oidc(openid_config, logger)
            .await?;
    }

    Ok(())
}

// Fix String `aud` claim value to array
fn fix_aud_claim_value_to_array(claims: TokenClaims) -> TokenClaims {
    // make owned value mutable
    let mut claims = claims;

    const AUD_KEY: &str = "aud";
    let mut aud_value = serde_json::Value::Null;

    if let Some(claim) = claims.get_claim(AUD_KEY) {
        if let Some(claim_str_value) = claim.value().as_str() {
            // convert String to Array for backward compatibility
            aud_value = json!([claim_str_value]);
        } else {
            aud_value = claim.value().clone()
        }
    }

    if aud_value != serde_json::Value::Null {
        claims = claims.with_claim(AUD_KEY.to_string(), aud_value)
    }

    claims
}

/// Hash a string using `ahash` and return the hash value as a string
/// This is used to create a key for caching tokens
/// The hash value is used instead of the original string to have shorter keys for SparKV which utilizes BTree.
fn hash_str(s: &str) -> String {
    use std::sync::LazyLock;
    static HASHER_KEYS: LazyLock<(u64, u64, u64, u64)> = LazyLock::new(|| {
        (
            rand::random(),
            rand::random(),
            rand::random(),
            rand::random(),
        )
    });

    let hasher =
        ahash::RandomState::with_seeds(HASHER_KEYS.0, HASHER_KEYS.1, HASHER_KEYS.2, HASHER_KEYS.3);

    hasher.hash_one(s).to_string()
}

#[cfg(test)]
mod test {
    use super::test_utils::*;
    use super::{JwtService, Token};
    use crate::JwtConfig;
    use jsonwebtoken::Algorithm;
    use serde_json::{Value, json};
    use std::collections::{HashMap, HashSet};
    use std::sync::Arc;
    use tokio::test;

    #[test]
    async fn can_validate_token() {
        let mut server = MockServer::new_with_defaults().await.unwrap();

        // create tokens
        let mut access_tkn_claims = json!({
            "iss": server.issuer(),
            "sub": "some_sub",
            "jti": 1231231231,
            "exp": u64::MAX,
            "client_id": "test123",
        });
        let access_tkn = server
            .generate_token_with_hs256sig(&mut access_tkn_claims, None)
            .unwrap();
        let mut id_tkn_claims = json!({
            "iss": server.issuer(),
            "aud": ["test123"],
            "sub": "some_sub",
            "name": "John Doe",
            "exp": u64::MAX,
        });
        let id_tkn = server
            .generate_token_with_hs256sig(&mut id_tkn_claims, None)
            .unwrap();
        let mut userinfo_tkn_claims = json!({
            "iss": server.issuer(),
            "aud": "test123",
            "sub": "some_sub",
            "name": "John Doe",
            "exp": u64::MAX,
        });
        let userinfo_tkn = server
            .generate_token_with_hs256sig(&mut userinfo_tkn_claims, None)
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
            0,
        )
        .await
        .inspect_err(|e| eprintln!("error msg: {}", e))
        .expect("Should create JwtService");
        let iss = Arc::new(iss);

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
            token.as_ref(),
            &Token::new("access_token", expected_claims.into(), Some(iss.clone()))
        );

        // Test id_token
        let token = validated_tokens
            .get("id_token")
            .expect("should have an id_token");
        let expected_claims = serde_json::from_value::<HashMap<String, Value>>(id_tkn_claims)
            .expect("Should create expected id_token claims");
        assert_eq!(
            token.as_ref(),
            &Token::new("id_token", expected_claims.into(), Some(iss.clone()))
        );

        // Test userinfo_token
        let token = validated_tokens
            .get("userinfo_token")
            .expect("should have an userinfo_token");
        let expected_claims = serde_json::from_value::<HashMap<String, Value>>(userinfo_tkn_claims)
            .expect("Should create expected userinfo_token claims");
        assert_eq!(
            token.as_ref(),
            &Token::new("userinfo_token", expected_claims.into(), Some(iss))
        );
    }
}
