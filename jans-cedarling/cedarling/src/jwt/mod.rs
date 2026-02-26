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
mod token_cache;
mod validation;

#[cfg(test)]
pub(crate) mod test_utils;

pub(crate) use decode::*;
pub(crate) use error::*;
pub(crate) use token::{Token, TokenClaimTypeError, TokenClaims};
pub(crate) use token_cache::TokenCache;
pub(crate) use validation::TrustedIssuerError;

use crate::JwtConfig;
use crate::LogLevel;
use crate::LogWriter;
use crate::authz::MultiIssuerValidationError;
use crate::authz::request::TokenInput;
use crate::common::issuer_utils::normalize_issuer;
use crate::common::policy_store::TrustedIssuer;
use crate::log::Logger;
use chrono::Utc;
use http_utils::{GetFromUrl, OpenIdConfig};
use key_service::{KeyService, KeyServiceError};
use log_entry::JwtLogEntry;
use serde_json::json;
use status_list::{JwtStatus, JwtStatusError, StatusListCache};
use std::collections::{HashMap, HashSet};
use std::sync::Arc;
use validation::{
    JwtValidator, JwtValidatorCache, OwnedValidatorInfo, TokenKind, TrustedIssuerValidator,
    ValidateJwtError, ValidatedJwt, ValidatorInfo, validate_required_claims,
};

/// The value of the `iss` claim from a JWT
type IssClaim = String;

/// Handles JWT validation
pub(crate) struct JwtService {
    validators: JwtValidatorCache,
    key_service: Arc<KeyService>,
    issuer_configs: HashMap<IssClaim, IssuerConfig>,
    token_metadata_keys_by_entity_type: HashMap<String, String>,
    /// Trusted issuer validator for advanced validation scenarios
    trusted_issuer_validator: TrustedIssuerValidator,
    logger: Option<Logger>,
    token_cache: TokenCache,
    signed_authz_available: bool,
    jwt_sig_validation_required: bool,
}

struct IssuerConfig {
    issuer_id: String,
    /// The [`TrustedIssuer`] config loaded from the policy store
    policy: Arc<TrustedIssuer>,
    /// The [`OpenIdConfig`] loaded from the IDP's `/.well-known/openid-configuration` endpoint
    openid_config: Option<OpenIdConfig>,
}

impl JwtService {
    /// Creates a new JWT service with the given configuration.
    ///
    /// # Arguments
    ///
    /// * `jwt_config` - JWT validation configuration (signature validation, algorithms, etc.)
    /// * `trusted_issuers` - Optional map of trusted issuer configurations from the policy store
    /// * `logger` - Optional logger for diagnostic messages
    /// * `token_cache_max_ttl_sec` - Maximum TTL for cached validated tokens (0 to disable caching)
    ///
    /// # Errors
    ///
    /// Returns `JwtServiceInitError` if initialization fails (e.g., failed to fetch OIDC config)
    pub(crate) async fn new(
        jwt_config: &JwtConfig,
        trusted_issuers: Option<HashMap<String, TrustedIssuer>>,
        logger: Option<Logger>,
    ) -> Result<Self, JwtServiceInitError> {
        let mut status_lists = StatusListCache::default();
        let mut issuer_configs = HashMap::default();
        let mut token_metadata_keys_by_entity_type = HashMap::default();
        let mut validators = JwtValidatorCache::default();
        let mut key_service = KeyService::new();

        let token_cache = TokenCache::new(
            jwt_config.token_cache_max_ttl_secs,
            jwt_config.token_cache_capacity,
            jwt_config.token_cache_earliest_expiration_eviction,
            logger.clone(),
        );

        let trusted_issuers = trusted_issuers.unwrap_or_default();
        let has_trusted_issuers = !trusted_issuers.is_empty();

        // Clone trusted_issuers before consumption - original is iterated and consumed below
        let trusted_issuers_for_validator = trusted_issuers.clone();

        for (issuer_id, iss) in trusted_issuers {
            // this is what we expect to find in the JWT `iss` claim
            let mut iss_claim = iss.oidc_endpoint.origin().ascii_serialization();

            let mut iss_config = IssuerConfig {
                issuer_id,
                policy: Arc::new(iss),
                openid_config: None,
            };

            if jwt_config.jwt_sig_validation || jwt_config.jwt_status_validation {
                iss_claim = update_openid_config(&mut iss_config, logger.as_ref()).await?;
            }

            insert_keys(&mut key_service, jwt_config, &iss_config, logger.as_ref()).await?;

            validators.init_for_iss(&iss_config, jwt_config, &status_lists, logger.as_ref());

            for (token_key, token_metadata) in &iss_config.policy.token_metadata {
                token_metadata_keys_by_entity_type
                    .insert(token_metadata.entity_type_name.clone(), token_key.clone());
            }

            if jwt_config.jwt_status_validation {
                status_lists
                    .init_for_iss(
                        &iss_config,
                        &validators,
                        &key_service,
                        token_cache.clone(),
                        logger.clone(),
                    )
                    .await?;
            }

            issuer_configs.insert(normalize_issuer(&iss_claim), iss_config);
        }

        // Load local JWKS if configured and no trusted issuers were provided
        // This ensures local JWKS-only configurations work correctly
        if !has_trusted_issuers
            && jwt_config.jwt_sig_validation
            && let Some(jwks) = jwt_config.jwks.as_ref()
        {
            key_service.insert_keys_from_str(jwks)?;
        }

        // quick check so we don't get surprised if the program runs but can't validate
        // anything
        let signed_authz_available = key_service.has_keys();
        if !signed_authz_available && jwt_config.jwt_sig_validation {
            logger.log_any(JwtLogEntry::new(
                "signed authorization is unavailable because no trusted issuers or JWKS were configured".to_string(),
                Some(LogLevel::WARN),
            ));
        }
        let key_service = Arc::new(key_service);

        // Create TrustedIssuerValidator for advanced validation scenarios
        let trusted_issuer_validator = TrustedIssuerValidator::new(trusted_issuers_for_validator);

        Ok(Self {
            validators,
            key_service,
            issuer_configs,
            token_metadata_keys_by_entity_type,
            trusted_issuer_validator,
            logger,
            token_cache,
            signed_authz_available,
            jwt_sig_validation_required: jwt_config.jwt_sig_validation,
        })
    }

    /// Validates multiple JWT tokens against trusted issuers.
    ///
    /// This method validates each token in the provided map, checking:
    /// - JWT signature validation (if enabled)
    /// - Token expiration and other standard claims
    /// - Required claims as specified in the trusted issuer configuration
    ///
    /// Tokens from untrusted issuers are skipped with a warning.
    ///
    /// # Arguments
    ///
    /// * `tokens` - Map of token names to JWT strings (e.g., "`access_token`" -> "eyJ...")
    ///
    /// # Returns
    ///
    /// Map of token names to validated `Token` objects, or an error if any token fails validation.
    pub(crate) fn validate_tokens<'a>(
        &'a self,
        tokens: &'a HashMap<String, String>,
    ) -> Result<HashMap<String, Arc<Token>>, JwtProcessingError> {
        const ID_TOKEN_NAME: &str = "id_token";

        if self.jwt_sig_validation_required && !self.signed_authz_available && !tokens.is_empty() {
            self.logger.log_any(JwtLogEntry::new(
                "signed authorization was attempted but Cedarling is not configured with trusted issuers or JWKS".to_string(),
                Some(LogLevel::ERROR),
            ));
            return Err(JwtProcessingError::SignedAuthzUnavailable);
        }

        let mut validated_tokens = HashMap::new();

        let now = Utc::now();

        // clear expired tokens from cache
        self.token_cache.clear_expired();

        for (token_name, jwt) in tokens {
            let token_kind = TokenKind::AuthzRequestInput(token_name);
            let token = if let Some(validated_token) = self.token_cache.find(&token_kind, jwt) {
                validated_token
            } else {
                // validate token and save to cache
                let validated_jwt = match self.validate_single_token(token_kind, jwt) {
                    Ok(jwt) => jwt,
                    Err(err) => {
                        if matches!(err, ValidateJwtError::MissingValidator(_)) {
                            self.logger
                                .log_any(JwtLogEntry::new(err.to_string(), Some(LogLevel::WARN)));
                            continue;
                        }
                        return Err(JwtProcessingError::ValidateJwt(token_name.clone(), err));
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
                }

                let token = Arc::new(Token::new(token_name, claims, validated_jwt.trusted_iss));
                self.token_cache.save(&token_kind, jwt, token.clone(), now);
                token
            };

            validated_tokens.insert(token_name.to_string(), token);
        }

        Ok(validated_tokens)
    }

    fn validate_single_token(
        &self,
        token_kind: TokenKind,
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
            token_kind,
            algorithm: decoded_jwt.header.alg,
        };
        let validator: Arc<std::sync::RwLock<JwtValidator>> =
            self.validators
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

        // Use TrustedIssuerValidator to find and validate against trusted issuer
        // This implements Requirement 5: "WHEN processing JWT tokens THEN the Cedarling
        // SHALL check if the token issuer matches any configured trusted issuers"
        let iss_claim = decoded_jwt.iss();

        // Try to find trusted issuer using TrustedIssuerValidator
        let trusted_iss = if let Some(iss) = iss_claim {
            match self.trusted_issuer_validator.find_trusted_issuer(iss) {
                Ok(issuer) => Some(issuer),
                Err(TrustedIssuerError::UntrustedIssuer(_)) => {
                    // Fall back to issuer_configs for backward compatibility
                    self.logger.log_any(JwtLogEntry::new(
                        format!("Untrusted issuer '{iss}', falling back to issuer_configs"),
                        Some(LogLevel::DEBUG),
                    ));
                    self.get_issuer_ref(iss)
                },
                Err(e) => {
                    self.logger.log_any(JwtLogEntry::new(
                        format!(
                            "Error finding trusted issuer '{iss}': {e}, falling back to issuer_configs"
                        ),
                        Some(LogLevel::DEBUG),
                    ));
                    self.get_issuer_ref(iss)
                },
            }
        } else {
            None
        };

        // Set trusted issuer reference on validated JWT
        validated_jwt.trusted_iss.clone_from(&trusted_iss);

        // Validate required claims based on trusted issuer configuration
        // This implements Requirement 5: "WHEN a JWT token is from a trusted issuer
        // THEN the Cedarling SHALL validate required claims as specified in the issuer configuration"
        if let Some(trusted_iss) = &trusted_iss {
            // Get the token type name from token_kind (skip for StatusList tokens)
            let token_type: Option<&str> = match &token_kind {
                TokenKind::AuthzRequestInput(name) => Some(*name),
                TokenKind::AuthorizeMultiIssuer(name) => Some(name),
                TokenKind::StatusList => None, // Skip required claims validation for status list tokens
            };

            if let Some(token_type) = token_type {
                // Get token metadata for this token type
                if let Some(token_metadata) = trusted_iss.token_metadata.get(token_type) {
                    // NOTE: This is the ONLY place where trusted-issuer-driven "required claims"
                    //       validation occurs. Standard JWT validation (signature, expiration,
                    //       audience, etc.) happens earlier in the validation pipeline (via the
                    //       JWT validator). The policy-driven required_claims are validated only
                    //       here, once per token, after we've resolved the TrustedIssuer and
                    //       token_metadata for that token type.
                    if let Err(err) =
                        validate_required_claims(&validated_jwt.claims, token_type, token_metadata)
                    {
                        self.logger.log_any(JwtLogEntry::new(
                            format!(
                                "Token '{token_type}' failed required claims validation: {err}"
                            ),
                            Some(LogLevel::ERROR),
                        ));
                        // Convert TrustedIssuerError to ValidateJwtError
                        match err {
                            TrustedIssuerError::MissingRequiredClaim { claim, .. } => {
                                return Err(ValidateJwtError::MissingClaims(vec![claim]));
                            },
                            _ => {
                                return Err(ValidateJwtError::TrustedIssuerValidation(err));
                            },
                        }
                    }
                }
            }
        }

        Ok(validated_jwt)
    }

    /// Validate multiple tokens from different issuers
    ///
    /// This method validates JWT tokens from multiple issuers, checking for:
    /// - JWT signature validation
    /// - Token expiration and other standard claims
    /// - Non-deterministic token detection (duplicate issuer+type combinations)
    ///
    /// Returns a result containing validated tokens or detailed error information.
    pub(crate) fn validate_multi_issuer_tokens(
        &self,
        tokens: &[TokenInput],
    ) -> Result<HashMap<String, Arc<Token>>, MultiIssuerValidationError> {
        if tokens.is_empty() {
            return Err(MultiIssuerValidationError::EmptyTokenArray);
        }

        let mut validated_tokens = HashMap::new();
        let mut seen_combinations = HashSet::new();

        // clear expired tokens from cache
        self.token_cache.clear_expired();

        let now = Utc::now();

        for (index, token) in tokens.iter().enumerate() {
            // Basic validation first
            if let Err(err) = token.validate() {
                if let Some(logger) = &self.logger {
                    logger.log_any(JwtLogEntry::new(
                        format!("Token validation failed at index {index}: {err}"),
                        Some(LogLevel::WARN),
                    ));
                }
                continue;
            }

            // Find the corresponding token metadata key for the entity type name
            let token_type = self.find_token_metadata_key(&token.mapping);

            let token_kind = TokenKind::AuthorizeMultiIssuer(token_type);
            // Create Token with the mapping as the name
            let token_name = token.mapping.clone();

            if let Some(cedar_token) = self.token_cache.find(&token_kind, &token.payload) {
                validated_tokens.insert(token_name, cedar_token);
            } else {
                // Validate JWT using existing single token validation
                match self.validate_single_token(token_kind, &token.payload) {
                    Ok(validated_jwt) => {
                        // Extract issuer for non-deterministic check
                        let issuer = validated_jwt
                            .claims
                            .get("iss")
                            .and_then(|iss| iss.as_str())
                            .ok_or(MultiIssuerValidationError::MissingIssuer)?;

                        // Check for non-deterministic tokens (graceful validation)
                        let combination = format!("{}:{}", issuer, token.mapping);
                        if seen_combinations.insert(combination.clone()) {
                            // Convert ValidatedJwt to Token
                            let claims =
                                serde_json::from_value::<TokenClaims>(validated_jwt.claims)
                                    .map_err(|err| {
                                        if let Some(logger) = &self.logger {
                                            logger.log_any(JwtLogEntry::new(
                                                format!(
                                                    "failed to deserialize token claims: {err}"
                                                ),
                                                Some(LogLevel::ERROR),
                                            ));
                                        }
                                        MultiIssuerValidationError::TokenValidationFailed
                                    })?;

                            let cedar_token = Arc::new(Token::new(
                                &token_name,
                                claims,
                                validated_jwt.trusted_iss,
                            ));

                            self.token_cache.save(
                                &token_kind,
                                &token.payload,
                                cedar_token.clone(),
                                now,
                            );

                            validated_tokens.insert(token_name, cedar_token);
                        } else {
                            // Log warning but continue processing
                            if let Some(logger) = &self.logger {
                                logger.log_any(JwtLogEntry::new(
                                format!(
                                    "Non-deterministic token detected: type '{}' from issuer '{}' (duplicate found, skipping)",
                                    token.mapping, issuer
                                ),
                                Some(LogLevel::WARN),
                            ));
                            }
                        }
                    },
                    Err(err) => {
                        if let Some(logger) = &self.logger {
                            logger.log_any(JwtLogEntry::new(
                                format!("Token validation failed at index {index}: {err}"),
                                Some(LogLevel::WARN),
                            ));
                        }
                    },
                }
            }
        }

        // If no tokens were successfully validated, return a detailed error
        if validated_tokens.is_empty() {
            if let Some(logger) = &self.logger {
                logger.log_any(JwtLogEntry::new(
                    "No valid tokens found in multi-issuer request".to_string(),
                    Some(LogLevel::ERROR),
                ));
            }

            return Err(MultiIssuerValidationError::TokenValidationFailed);
        }

        Ok(validated_tokens)
    }

    /// Use the `iss` claim of a token to retrieve a reference to a [`TrustedIssuer`]
    #[inline]
    fn get_issuer_ref(&self, iss_claim: &str) -> Option<Arc<TrustedIssuer>> {
        self.issuer_configs
            .get(&normalize_issuer(iss_claim))
            .map(|config| config.policy.clone())
    }

    /// Find the token metadata key for a given entity type name
    /// e.g., "`Dolphin::Access_Token`" -> "`access_token`"
    fn find_token_metadata_key<'a>(&'a self, entity_type_name: &'a str) -> &'a str {
        self.token_metadata_keys_by_entity_type
            .get(entity_type_name)
            .map_or(entity_type_name, String::as_str)
    }
}

async fn update_openid_config(
    iss_config: &mut IssuerConfig,
    logger: Option<&Logger>,
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
    logger: Option<&Logger>,
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
    const AUD_KEY: &str = "aud";

    // make owned value mutable
    let mut claims = claims;
    let mut aud_value = serde_json::Value::Null;

    if let Some(claim) = claims.get_claim(AUD_KEY) {
        if let Some(claim_str_value) = claim.value().as_str() {
            // convert String to Array for backward compatibility
            aud_value = json!([claim_str_value]);
        } else {
            aud_value = claim.value().clone();
        }
    }

    if aud_value != serde_json::Value::Null {
        claims = claims.with_claim(AUD_KEY.to_string(), aud_value);
    }

    claims
}

#[cfg(test)]
mod test {
    use super::test_utils::*;
    use super::{JwtService, Token};
    use crate::JwtConfig;
    use crate::authz::MultiIssuerValidationError;
    use crate::authz::request::TokenInput;
    use crate::common::policy_store::{ClaimMappings, TokenEntityMetadata};
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
            "jti": 1_231_231_231,
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
                ..Default::default()
            },
            Some(HashMap::from([("Jans".into(), iss.clone())])),
            None,
        )
        .await
        .expect("Should create JwtService");
        let iss = Arc::new(iss);

        let tokens = HashMap::from([
            ("access_token".to_string(), access_tkn),
            ("id_token".to_string(), id_tkn),
            ("userinfo_token".to_string(), userinfo_tkn),
        ]);
        let validated_tokens = jwt_service
            .validate_tokens(&tokens)
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

    #[test]
    async fn test_validate_multi_issuer_tokens_success() {
        let mut server = MockServer::new_with_defaults().await.unwrap();

        // Create tokens with different issuers
        let mut access_tkn_claims = json!({
            "iss": server.issuer(),
            "sub": "user123",
            "jti": 1_231_231_231,
            "exp": u64::MAX,
            "client_id": "test123",
        });
        let access_tkn = server
            .generate_token_with_hs256sig(&mut access_tkn_claims, None)
            .unwrap();

        let mut id_tkn_claims = json!({
            "iss": server.issuer(),
            "sub": "user123",
            "exp": u64::MAX,
            "aud": ["test123"],
        });
        let id_tkn = server
            .generate_token_with_hs256sig(&mut id_tkn_claims, None)
            .unwrap();

        let mut iss = server.trusted_issuer();
        // Add token metadata for multi-issuer validation
        iss.token_metadata.insert(
            "Jans::Access_Token".to_string(),
            TokenEntityMetadata {
                trusted: true,
                entity_type_name: "Jans::Access_Token".to_string(),
                principal_mapping: HashSet::new(),
                token_id: "jti".to_string(),
                user_id: None,
                role_mapping: None,
                workload_id: None,
                claim_mapping: ClaimMappings::default(),
                required_claims: HashSet::new(),
            },
        );
        iss.token_metadata.insert(
            "Jans::Id_Token".to_string(),
            TokenEntityMetadata {
                trusted: true,
                entity_type_name: "Jans::Id_Token".to_string(),
                principal_mapping: HashSet::new(),
                token_id: "jti".to_string(),
                user_id: None,
                role_mapping: None,
                workload_id: None,
                claim_mapping: ClaimMappings::default(),
                required_claims: HashSet::new(),
            },
        );

        let jwt_service = JwtService::new(
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: true,
                jwt_status_validation: false,
                signature_algorithms_supported: HashSet::from_iter([Algorithm::HS256]),
                ..Default::default()
            },
            Some(HashMap::from([(server.issuer(), iss)])),
            None,
        )
        .await
        .expect("Should create JwtService");

        // Create TokenInput structs
        let tokens = vec![
            TokenInput::new("Jans::Access_Token".to_string(), access_tkn),
            TokenInput::new("Jans::Id_Token".to_string(), id_tkn),
        ];

        let result = jwt_service.validate_multi_issuer_tokens(&tokens);
        assert!(result.is_ok());

        let validated_tokens = result.unwrap();
        assert_eq!(validated_tokens.len(), 2);

        // Verify the tokens have the correct mapping
        assert!(validated_tokens.contains_key("Jans::Access_Token"));
        assert!(validated_tokens.contains_key("Jans::Id_Token"));
    }

    #[test]
    async fn test_validate_multi_issuer_tokens_empty_array() {
        let server = MockServer::new_with_defaults().await.unwrap();
        let iss = server.trusted_issuer();

        let jwt_service = JwtService::new(
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: true,
                jwt_status_validation: false,
                signature_algorithms_supported: HashSet::from_iter([Algorithm::HS256]),
                ..Default::default()
            },
            Some(HashMap::from([(server.issuer(), iss)])),
            None,
        )
        .await
        .expect("Should create JwtService");

        let result = jwt_service.validate_multi_issuer_tokens(&[]);
        assert!(matches!(
            result,
            Err(MultiIssuerValidationError::EmptyTokenArray)
        ));
    }

    #[test]
    async fn test_validate_multi_issuer_tokens_invalid_token_format() {
        let server = MockServer::new_with_defaults().await.unwrap();
        let iss = server.trusted_issuer();

        let jwt_service = JwtService::new(
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: true,
                jwt_status_validation: false,
                signature_algorithms_supported: HashSet::from_iter([Algorithm::HS256]),
                ..Default::default()
            },
            Some(HashMap::from([(server.issuer(), iss)])),
            None,
        )
        .await
        .expect("Should create JwtService");

        // Create tokens with invalid JWT format
        let tokens = vec![
            TokenInput::new("Jans::Access_Token".to_string(), "invalid-jwt".to_string()),
            TokenInput::new("Jans::Id_Token".to_string(), "also-invalid".to_string()),
        ];

        let result = jwt_service.validate_multi_issuer_tokens(&tokens);
        assert!(matches!(
            result,
            Err(MultiIssuerValidationError::TokenValidationFailed)
        ));
    }

    #[test]
    async fn test_validate_multi_issuer_tokens_graceful_validation() {
        let mut server = MockServer::new_with_defaults().await.unwrap();

        // Create valid token
        let mut valid_claims = json!({
            "iss": server.issuer(),
            "sub": "user123",
            "jti": 1_231_231_231,
            "exp": u64::MAX,
        });
        let valid_token = server
            .generate_token_with_hs256sig(&mut valid_claims, None)
            .unwrap();

        let mut iss = server.trusted_issuer();
        // Add token metadata for multi-issuer validation
        iss.token_metadata.insert(
            "Jans::Access_Token".to_string(),
            TokenEntityMetadata {
                trusted: true,
                entity_type_name: "Jans::Access_Token".to_string(),
                principal_mapping: HashSet::new(),
                token_id: "jti".to_string(),
                user_id: None,
                role_mapping: None,
                workload_id: None,
                claim_mapping: ClaimMappings::default(),
                required_claims: HashSet::new(),
            },
        );
        iss.token_metadata.insert(
            "Jans::Id_Token".to_string(),
            TokenEntityMetadata {
                trusted: true,
                entity_type_name: "Jans::Id_Token".to_string(),
                principal_mapping: HashSet::new(),
                token_id: "jti".to_string(),
                user_id: None,
                role_mapping: None,
                workload_id: None,
                claim_mapping: ClaimMappings::default(),
                required_claims: HashSet::new(),
            },
        );

        let jwt_service = JwtService::new(
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: true,
                jwt_status_validation: false,
                signature_algorithms_supported: HashSet::from_iter([Algorithm::HS256]),
                ..Default::default()
            },
            Some(HashMap::from([(server.issuer(), iss)])),
            None,
        )
        .await
        .expect("Should create JwtService");

        // Create tokens with one valid and one invalid
        let tokens = vec![
            TokenInput::new("Jans::Access_Token".to_string(), valid_token),
            TokenInput::new("Jans::Id_Token".to_string(), "invalid-jwt".to_string()),
        ];

        let result = jwt_service.validate_multi_issuer_tokens(&tokens);
        assert!(result.is_ok());

        let validated_tokens = result.unwrap();
        assert_eq!(validated_tokens.len(), 1); // Only the valid token should be returned
    }

    #[test]
    async fn test_validate_multi_issuer_tokens_non_deterministic_graceful() {
        let mut server = MockServer::new_with_defaults().await.unwrap();

        // Create two tokens with same issuer and type (non-deterministic)
        let mut claims1 = json!({
            "iss": server.issuer(),
            "sub": "user123",
            "jti": 1_231_231_231,
            "exp": u64::MAX,
        });
        let token_one = server
            .generate_token_with_hs256sig(&mut claims1, None)
            .unwrap();

        let mut claims2 = json!({
            "iss": server.issuer(),
            "sub": "user456",
            "jti": 1_231_231_232,
            "exp": u64::MAX,
        });
        let token_two = server
            .generate_token_with_hs256sig(&mut claims2, None)
            .unwrap();

        let mut iss = server.trusted_issuer();
        // Add token metadata for multi-issuer validation
        iss.token_metadata.insert(
            "Jans::Access_Token".to_string(),
            TokenEntityMetadata {
                trusted: true,
                entity_type_name: "Jans::Access_Token".to_string(),
                principal_mapping: HashSet::new(),
                token_id: "jti".to_string(),
                user_id: None,
                role_mapping: None,
                workload_id: None,
                claim_mapping: ClaimMappings::default(),
                required_claims: HashSet::new(),
            },
        );

        let jwt_service = JwtService::new(
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: true,
                jwt_status_validation: false,
                signature_algorithms_supported: HashSet::from_iter([Algorithm::HS256]),
                ..Default::default()
            },
            Some(HashMap::from([(server.issuer(), iss)])),
            None,
        )
        .await
        .expect("Should create JwtService");

        // Create tokens with duplicate issuer+type combination
        let tokens = vec![
            TokenInput::new("Jans::Access_Token".to_string(), token_one),
            TokenInput::new("Jans::Access_Token".to_string(), token_two), // Duplicate type from same issuer
        ];

        let result = jwt_service.validate_multi_issuer_tokens(&tokens);
        assert!(result.is_ok());

        let validated_tokens = result.unwrap();
        assert_eq!(validated_tokens.len(), 1); // Only the first token should be returned (graceful validation)
    }

    #[test]
    async fn test_validate_multi_issuer_tokens_missing_issuer() {
        let mut server = MockServer::new_with_defaults().await.unwrap();

        // Create token without issuer claim
        let mut claims = json!({
            "sub": "user123",
            "exp": u64::MAX,
        });
        let token = server
            .generate_token_with_hs256sig(&mut claims, None)
            .unwrap();

        let iss = server.trusted_issuer();

        let jwt_service = JwtService::new(
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: true,
                jwt_status_validation: false,
                signature_algorithms_supported: HashSet::from_iter([Algorithm::HS256]),
                ..Default::default()
            },
            Some(HashMap::from([(server.issuer(), iss)])),
            None,
        )
        .await
        .expect("Should create JwtService");

        let tokens = vec![TokenInput::new("Jans::Access_Token".to_string(), token)];

        let result = jwt_service.validate_multi_issuer_tokens(&tokens);
        assert!(matches!(
            result,
            Err(MultiIssuerValidationError::TokenValidationFailed)
        ));
    }
}
