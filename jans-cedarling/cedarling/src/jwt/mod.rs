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
//! Token validation is used when evaluating requests via [`authorize_multi_issuer`](crate::Authz::authorize_multi_issuer).
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
//! - [x] JWK rotation: A per-issuer background task periodically re-fetches JWKS

mod custom_token;
mod decode;
mod error;
mod http_utils;
mod issuer_index;
mod key_service;
mod loading_info;
mod loading_state;
mod log_entry;
mod status_list;
mod token;
mod token_cache;
mod trusted_issuers_loader;
mod validation;

pub(crate) mod test_utils;

pub(crate) use custom_token::CustomIssuerIndex;
pub use custom_token::{CustomTokenError, CustomTokenProcessor, ProcessedTokenClaims};
pub(crate) use decode::*;
pub(crate) use error::*;
pub use loading_info::TrustedIssuerLoadingInfo;
pub(crate) use token::{CustomTokenIssuerMeta, Token, TokenClaims, TokenIssuer};
pub(crate) use token_cache::TokenCache;
pub(crate) use validation::{TrustedIssuerError, ValidateJwtError};

use crate::JwtConfig;
use crate::LogLevel;
use crate::LogWriter;
use crate::authz::MultiIssuerValidationError;
use crate::authz::metrics::MetricsCollector;
use crate::authz::request::TokenInput;
use crate::common::issuer_utils::IssClaim;
use crate::common::policy_store::TrustedIssuer;

use self::http_utils::{GetFromUrl, OpenIdConfig};
use crate::http::HttpClient;
use crate::log::Logger;
use chrono::Utc;
use issuer_index::IssuerIndex;
use key_service::KeyService;
use loading_state::TrustedIssuerLoadingState;
use log_entry::JwtLogEntry;
use smol_str::SmolStr;
use status_list::{JwtStatus, JwtStatusError, StatusListCache};
use std::borrow::Cow;
use std::collections::{HashMap, HashSet};
use std::sync::{Arc, Mutex};
use std::time::Duration;
use tokio::sync::Notify;
use tokio_util::sync::CancellationToken;
use trusted_issuers_loader::TrustedIssuerLoader;
use validation::{
    JwtValidator, JwtValidatorCache, OwnedValidatorInfo, TokenKind, TrustedIssuerValidator,
    ValidatedJwt, ValidatorInfo, validate_required_claims,
};

/// Handles JWT validation
pub(crate) struct JwtService {
    validators: Arc<JwtValidatorCache>,
    key_service: Arc<KeyService>,
    issuer_configs: Arc<IssuerIndex>,
    trusted_issuer_validator: TrustedIssuerValidator,
    logger: Option<Logger>,
    token_cache: TokenCache,
    loading_state: Arc<TrustedIssuerLoadingState>,
    /// Per-issuer notifiers for triggering on-demand JWKS re-fetch
    jwks_refresh_notifiers: Arc<Mutex<HashMap<IssClaim, Arc<Notify>>>>,
    /// Cancellation token to stop all background JWKS refresh tasks on drop
    jwks_cancel_token: CancellationToken,
    metrics: Arc<MetricsCollector>,
}

struct IssuerConfig {
    issuer_id: String,
    /// The [`TrustedIssuer`] config loaded from the policy store
    policy: Arc<TrustedIssuer>,
    /// The [`OpenIdConfig`] loaded from the IDP's `/.well-known/openid-configuration` endpoint
    openid_config: Option<OpenIdConfig>,
}

struct TokenCallCtx<'a> {
    token: &'a TokenInput,
    index: usize,
    now: chrono::DateTime<Utc>,
    seen_combinations: &'a mut HashSet<(SmolStr, SmolStr)>,
}

impl JwtService {
    /// Creates a new JWT service with the given configuration.
    ///
    /// # Arguments
    ///
    /// * `jwt_config` - JWT validation configuration (signature validation, algorithms, etc.)
    /// * `trusted_issuers` - Optional map of trusted issuer configurations from the policy store
    /// * `logger` - Optional logger for diagnostic messages
    /// * `metrics` - Shared metrics collector for telemetry
    ///
    /// # Errors
    ///
    /// Returns `JwtServiceInitError` if initialization fails (e.g., failed to fetch OIDC config)
    pub(crate) async fn new(
        jwt_config: &JwtConfig,
        trusted_issuers: Option<HashMap<String, TrustedIssuer>>,
        logger: Option<Logger>,
        metrics: Arc<MetricsCollector>,
        http_client: HttpClient,
    ) -> Result<Self, JwtServiceInitError> {
        if jwt_config.jwt_sig_validation && jwt_config.signature_algorithms_supported.is_empty() {
            return Err(JwtServiceInitError::NoSupportedAlgorithms);
        }

        // Apply field-level invariants once here so the bootstrap deserializer is
        // not the only line of defense - programmatic callers that build a
        // `JwtConfig` directly cannot otherwise reach the normalization.
        let mut jwt_config = jwt_config.clone();
        jwt_config.normalize();
        let jwt_config = &jwt_config;

        warn_if_jwt_validation_disabled(jwt_config, logger.as_ref());

        let status_lists = StatusListCache::default();
        let issuer_configs = Arc::new(IssuerIndex::new());
        let validators = Arc::new(JwtValidatorCache::default());
        let key_service = Arc::new(KeyService::new());

        let token_cache = TokenCache::new(
            jwt_config.token_cache_max_ttl_secs,
            jwt_config.token_cache_capacity,
            jwt_config.token_cache_earliest_expiration_eviction,
            logger.clone(),
            metrics.clone(),
        );

        let trusted_issuers = trusted_issuers.unwrap_or_default();
        let loading_state = Arc::new(TrustedIssuerLoadingState::new(trusted_issuers.len()));

        let jwks_refresh_notifiers = Arc::new(Mutex::new(HashMap::new()));
        let jwks_cancel_token = CancellationToken::new();

        let loader = TrustedIssuerLoader {
            jwt_config: jwt_config.clone(),
            status_lists: status_lists.clone(),
            issuer_configs: issuer_configs.clone(),
            validators: validators.clone(),
            key_service: key_service.clone(),
            token_cache: token_cache.clone(),
            logger: logger.clone(),
            loading_state: loading_state.clone(),
            http_client,
            jwks_refresh_notifiers: jwks_refresh_notifiers.clone(),
            jwks_cancel_token: jwks_cancel_token.clone(),
        };

        loader.load_trusted_issuers(trusted_issuers.clone()).await?;

        // Create TrustedIssuerValidator for advanced validation scenarios
        let trusted_issuer_validator = TrustedIssuerValidator::new(trusted_issuers);

        {
            let cache = token_cache.clone();
            let cancel = jwks_cancel_token.clone();
            crate::http::spawn_task(async move {
                loop {
                    tokio::select! {
                        () = crate::async_sleep::sleep(std::time::Duration::from_secs(30)) => {},
                        () = cancel.cancelled() => { break; },
                    }
                    cache.clear_expired();
                }
            });
        }

        Ok(Self {
            validators,
            key_service,
            issuer_configs,
            trusted_issuer_validator,
            logger,
            token_cache,
            loading_state,
            jwks_refresh_notifiers,
            jwks_cancel_token,
            metrics,
        })
    }

    /// Validates a single JWT and returns decoded claims and trusted issuer when valid.
    fn validate_single_token(
        &self,
        token_kind: &TokenKind,
        jwt: &str,
    ) -> Result<ValidatedJwt, ValidateJwtError> {
        let decoded_jwt = decode_jwt(jwt)?;

        // Get decoding key
        let decoding_key_info = decoded_jwt.decoding_key_info();
        let decoding_key = self.key_service.get_key(&decoding_key_info);

        if decoding_key.is_none()
            && let Some(iss) = &decoding_key_info.issuer
        {
            self.signal_jwks_refresh(iss);
        }

        // get validator
        let normalized_iss = decoded_jwt.iss();
        let validator_key = ValidatorInfo {
            iss: normalized_iss.as_ref(),
            token_kind: token_kind.clone(),
            algorithm: decoded_jwt.header.alg,
        };
        let validator: Arc<JwtValidator> = self
            .validators
            .get(&validator_key)
            .ok_or(ValidateJwtError::MissingValidator(validator_key.owned()))?;

        // validate JWT
        // NOTE: the JWT will be validated depending on the validator's settings that
        // was set on initialization
        let mut validated_jwt = validator.validate_jwt(jwt, decoding_key)?;

        // Use TrustedIssuerValidator to find and validate against trusted issuer
        // This implements Requirement 5: "WHEN processing JWT tokens THEN the Cedarling
        // SHALL check if the token issuer matches any configured trusted issuers"
        let iss_claim = decoded_jwt.iss();

        // Try to find trusted issuer using TrustedIssuerValidator
        let trusted_iss = if let Some(iss) = iss_claim {
            match self.trusted_issuer_validator.find_trusted_issuer(&iss) {
                Ok(issuer) => Some(issuer),
                Err(TrustedIssuerError::UntrustedIssuer(_)) => {
                    // Fall back to issuer_configs for backward compatibility
                    self.logger.log_any(JwtLogEntry::new(
                        format!("Untrusted issuer '{iss}', falling back to issuer_configs"),
                        Some(LogLevel::DEBUG),
                    ));
                    self.get_issuer_ref(&iss)
                },
                Err(e) => {
                    self.logger.log_any(JwtLogEntry::new(
                        format!(
                            "Error finding trusted issuer '{iss}': {e}, falling back to issuer_configs"
                        ),
                        Some(LogLevel::DEBUG),
                    ));
                    self.get_issuer_ref(&iss)
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
                TokenKind::AuthorizeMultiIssuer(name) | TokenKind::AuthorizeCustom(name) => {
                    Some(name)
                },
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
    pub(crate) async fn validate_multi_issuer_tokens(
        &self,
        tokens: &[TokenInput],
        custom_processor: Option<&Arc<dyn CustomTokenProcessor>>,
        custom_issuers: &CustomIssuerIndex,
        custom_timeout: Option<Duration>,
    ) -> Result<HashMap<String, Arc<Token>>, MultiIssuerValidationError> {
        if tokens.is_empty() {
            return Err(MultiIssuerValidationError::EmptyTokenArray);
        }

        let mut validated_tokens = HashMap::new();
        let mut seen_combinations = HashSet::new();

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

            // Create Token with the mapping as the name
            let token_name = token.mapping.clone();

            // Custom-token dispatch: a mapping that routes to a custom issuer is
            // never handled by the JWT path.
            let mut ctx = TokenCallCtx {
                token,
                index,
                now,
                seen_combinations: &mut seen_combinations,
            };
            let result = if custom_issuers.mapping_is_custom(&token.mapping) {
                self.handle_custom_token(custom_processor, custom_issuers, custom_timeout, &mut ctx)
                    .await?
            } else {
                self.validate_jwt_token(&mut ctx)?
            };

            if let Some(cedar_token) = result {
                validated_tokens.insert(token_name, cedar_token);
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

    /// Dispatch a single token input whose mapping routes to a custom issuer.
    ///
    /// Returns `Ok(None)` when the token is dropped (no processor, duplicate, or
    /// non-required failure) and `Err` only when the failure is fail-closed.
    async fn handle_custom_token(
        &self,
        custom_processor: Option<&Arc<dyn CustomTokenProcessor>>,
        custom_issuers: &CustomIssuerIndex,
        custom_timeout: Option<Duration>,
        ctx: &mut TokenCallCtx<'_>,
    ) -> Result<Option<Arc<Token>>, MultiIssuerValidationError> {
        let Some(processor) = custom_processor else {
            let err = MultiIssuerValidationError::CustomToken(
                CustomTokenError::NoProcessorRegistered(ctx.token.mapping.clone()),
            );
            self.metrics.record_error(&err);
            if custom_issuers.mapping_required(&ctx.token.mapping) {
                return Err(err);
            }
            if let Some(logger) = &self.logger {
                logger.log_any(JwtLogEntry::new(
                    format!("Custom token dropped at index {}: {err}", ctx.index),
                    Some(LogLevel::WARN),
                ));
            }
            return Ok(None);
        };
        match self
            .process_custom_token(
                processor,
                custom_issuers,
                ctx.token,
                custom_timeout,
                ctx.now,
            )
            .await
        {
            Ok(cedar_token) => {
                let issuer = cedar_token
                    .extract_normalized_issuer()
                    .map(|i| SmolStr::from(i.as_str()))
                    .unwrap_or_default();
                let combination = (issuer, SmolStr::from(ctx.token.mapping.as_str()));
                if ctx.seen_combinations.insert(combination) {
                    Ok(Some(cedar_token))
                } else if let Some(logger) = &self.logger {
                    logger.log_any(JwtLogEntry::new(
                        format!(
                            "Non-deterministic custom token detected: type '{}' (duplicate found, skipping)",
                            ctx.token.mapping
                        ),
                        Some(LogLevel::WARN),
                    ));
                    Ok(None)
                } else {
                    Ok(None)
                }
            },
            Err(err) => {
                self.metrics.record_error(&err);
                // Fail-closed for a `required` custom issuer; otherwise skip
                // and continue, logging timeout vs. processor error distinctly.
                if custom_issuers.mapping_required(&ctx.token.mapping) {
                    return Err(err);
                }
                if let Some(logger) = &self.logger {
                    let reason = if matches!(
                        err,
                        MultiIssuerValidationError::CustomToken(CustomTokenError::Timeout(_))
                    ) {
                        "timeout"
                    } else {
                        "processor error"
                    };
                    logger.log_any(JwtLogEntry::new(
                        format!(
                            "Custom token dropped at index {} ({reason}): {err}",
                            ctx.index
                        ),
                        Some(LogLevel::WARN),
                    ));
                }
                Ok(None)
            },
        }
    }

    /// Validate a single JWT token input via the cache or single-token validation.
    ///
    /// Returns `Ok(None)` when the token is dropped (duplicate or validation
    /// failure); issuer/claim errors abort the whole batch as before.
    fn validate_jwt_token(
        &self,
        ctx: &mut TokenCallCtx<'_>,
    ) -> Result<Option<Arc<Token>>, MultiIssuerValidationError> {
        // Find the corresponding token metadata key for the entity type name
        let token_type = self.find_token_metadata_key(&ctx.token.mapping);

        let token_kind = TokenKind::AuthorizeMultiIssuer(token_type);

        if let Some(cedar_token) = self.token_cache.find(&token_kind, &ctx.token.payload) {
            return Ok(Some(cedar_token));
        }

        // Validate JWT using existing single token validation
        match self.validate_single_token(&token_kind, &ctx.token.payload) {
            Ok(validated_jwt) => {
                self.metrics.record_jwt_validation(true);
                // Extract issuer for non-deterministic check
                let issuer = validated_jwt
                    .claims
                    .get("iss")
                    .and_then(|iss| iss.as_str())
                    .ok_or(MultiIssuerValidationError::MissingIssuer)?;

                // Check for non-deterministic tokens (graceful validation)
                let combination = (
                    SmolStr::from(issuer),
                    SmolStr::from(ctx.token.mapping.as_str()),
                );
                if ctx.seen_combinations.insert(combination) {
                    // Convert ValidatedJwt to Token
                    let claims = TokenClaims::try_from(validated_jwt.claims)
                        .map_err(MultiIssuerValidationError::InvalidClaims)?;

                    let cedar_token = Arc::new(Token::new(
                        &ctx.token.mapping,
                        claims,
                        validated_jwt.trusted_iss.map(TokenIssuer::Jwt),
                    ));

                    self.token_cache.save(
                        &token_kind,
                        &ctx.token.payload,
                        cedar_token.clone(),
                        ctx.now,
                    );

                    Ok(Some(cedar_token))
                } else {
                    // Log warning but continue processing
                    if let Some(logger) = &self.logger {
                        logger.log_any(JwtLogEntry::new(
                            format!(
                                "Non-deterministic token detected: type '{}' from issuer '{}' (duplicate found, skipping)",
                                ctx.token.mapping, issuer
                            ),
                            Some(LogLevel::WARN),
                        ));
                    }
                    Ok(None)
                }
            },
            Err(err) => {
                self.metrics.record_jwt_validation(false);
                if let Some(logger) = &self.logger {
                    logger.log_any(JwtLogEntry::new(
                        format!("Token validation failed at index {}: {err}", ctx.index),
                        Some(LogLevel::WARN),
                    ));
                }
                self.metrics.record_error(&err);
                Ok(None)
            },
        }
    }

    /// Process a single custom (non-JWT) token via the registered processor.
    ///
    /// Resolves the custom issuer, enforces required claims, and caches the result
    /// unless the processor opts out. Serves a still-valid cached result on hit.
    ///
    /// DEBT: lives in `JwtService` for a minimal v1 diff; the custom/JWT dispatch
    /// seam should move up to `Authz` in a follow-up so `JwtService` stays JWT-only.
    async fn process_custom_token(
        &self,
        processor: &Arc<dyn CustomTokenProcessor>,
        custom_issuers: &CustomIssuerIndex,
        input: &TokenInput,
        timeout: Option<Duration>,
        now: chrono::DateTime<Utc>,
    ) -> Result<Arc<Token>, MultiIssuerValidationError> {
        let token_kind = TokenKind::AuthorizeCustom(Cow::Borrowed(input.mapping.as_str()));

        // Serve a still-valid cached result if present.
        if let Some(cached) = self.token_cache.find(&token_kind, &input.payload) {
            return Ok(cached);
        }

        let process_fut = processor.process(&input.mapping, &input.payload);
        let processed = match timeout {
            None => process_fut.await?,
            Some(dur) => {
                #[cfg(not(any(target_arch = "wasm32", target_arch = "wasm64")))]
                {
                    match tokio::time::timeout(dur, process_fut).await {
                        Ok(res) => res?,
                        Err(_) => return Err(CustomTokenError::Timeout(dur).into()),
                    }
                }
                // Timeout is not enforced on wasm; the future runs to completion.
                #[cfg(any(target_arch = "wasm32", target_arch = "wasm64"))]
                {
                    let _ = dur;
                    process_fut.await?
                }
            },
        };

        let resolved = custom_issuers.resolve(&input.mapping, processed.issuer_id.as_deref())?;
        for required in &resolved.required_claims {
            if !processed.claims.contains_key(required) {
                return Err(CustomTokenError::MissingRequiredClaim(required.clone()).into());
            }
        }

        let meta = CustomTokenIssuerMeta {
            issuer_id: resolved.issuer_id.clone(),
            entity_type_name: Some(resolved.entity_type_name.clone()),
            token_id: processed.token_id.clone(),
        };
        let cacheable = processed.cacheable;
        let claims = TokenClaims::from(processed.claims);
        let cedar_token = Arc::new(Token::new(
            &input.mapping,
            claims,
            Some(TokenIssuer::Custom(meta)),
        ));

        if cacheable {
            self.token_cache
                .save(&token_kind, &input.payload, cedar_token.clone(), now);
        }

        Ok(cedar_token)
    }

    /// Use the `iss` claim of a token to retrieve a reference to a [`TrustedIssuer`]
    #[inline]
    fn get_issuer_ref(&self, iss_claim: &IssClaim) -> Option<Arc<TrustedIssuer>> {
        self.issuer_configs.get_trusted_issuer(iss_claim)
    }

    /// Signals the background JWKS refresher for the given issuer, if one exists.
    fn signal_jwks_refresh(&self, iss: &IssClaim) {
        let notify = {
            let notifiers = self
                .jwks_refresh_notifiers
                .lock()
                .expect("acquire jwks_refresh_notifiers lock");
            notifiers.get(iss).cloned()
        };

        if let Some(notify) = notify {
            notify.notify_one();
            self.logger.log_any(JwtLogEntry::new(
                format!(
                    "signalled background JWKS refresh for issuer '{}'",
                    iss.as_str()
                ),
                Some(LogLevel::INFO),
            ));
        }
    }

    /// Find the token metadata key for a given entity type name
    /// e.g., "`Dolphin::Access_Token`" -> "`access_token`"
    fn find_token_metadata_key<'a>(&'a self, entity_type_name: &'a str) -> Cow<'a, str> {
        if let Some(token_key) = self
            .issuer_configs
            .find_token_metadata_key(entity_type_name)
        {
            return Cow::Owned(token_key);
        }

        // If not found, return the original mapping (fallback)
        Cow::Borrowed(entity_type_name)
    }
}

impl Drop for JwtService {
    fn drop(&mut self) {
        self.jwks_cancel_token.cancel();
    }
}

impl TrustedIssuerLoadingInfo for JwtService {
    fn is_trusted_issuer_loaded_by_name(&self, issuer_id: &str) -> bool {
        self.issuer_configs.is_issuer_id_present(issuer_id)
    }

    fn is_trusted_issuer_loaded_by_iss(&self, iss_claim: &str) -> bool {
        let iss = IssClaim::new(iss_claim);
        self.issuer_configs.contains_iss(&iss)
    }

    fn loaded_trusted_issuers_count(&self) -> usize {
        self.issuer_configs.len()
    }

    fn total_issuers(&self) -> usize {
        self.loading_state.total_issuers()
    }

    fn loaded_trusted_issuer_ids(&self) -> HashSet<String> {
        self.issuer_configs.loaded_issuer_ids()
    }

    fn failed_trusted_issuer_ids(&self) -> HashSet<String> {
        self.loading_state.failed_issuers()
    }
}

/// Emits a WARN log entry once per `JwtService::new` when either JWT signature
/// or status validation has been explicitly disabled. The defaults are strict,
/// so reaching this code means an operator opted out.
fn warn_if_jwt_validation_disabled(jwt_config: &JwtConfig, logger: Option<&Logger>) {
    if !jwt_config.jwt_sig_validation {
        logger.log_any(JwtLogEntry::new(
            "JWT signature validation is disabled (CEDARLING_JWT_SIG_VALIDATION=disabled); \
             tokens are accepted without cryptographic verification"
                .to_string(),
            Some(LogLevel::WARN),
        ));
    }

    if !jwt_config.jwt_status_validation {
        logger.log_any(JwtLogEntry::new(
            "JWT status validation is disabled (CEDARLING_JWT_STATUS_VALIDATION=disabled); \
             revoked tokens may continue to be accepted"
                .to_string(),
            Some(LogLevel::WARN),
        ));
    }
}

#[cfg(test)]
mod test {
    use super::JwtService;
    use super::TrustedIssuerLoadingInfo;
    use super::test_utils::*;
    use super::{CustomIssuerIndex, CustomTokenError, CustomTokenProcessor, ProcessedTokenClaims};
    use crate::JwtConfig;
    use crate::authz::MultiIssuerValidationError;
    use crate::authz::metrics::MetricsCollector;
    use crate::authz::request::TokenInput;
    use crate::common::policy_store::CustomIssuerMetadata;
    use crate::common::policy_store::TokenEntityMetadata;
    use crate::http::HttpClient;
    use crate::http::HttpClientConfig;
    use async_trait::async_trait;
    use jsonwebtoken::Algorithm;
    use serde_json::json;
    use std::collections::{HashMap, HashSet};
    use std::sync::Arc;
    use std::sync::LazyLock;
    use std::sync::atomic::{AtomicUsize, Ordering};
    use std::time::Duration;
    use tokio::test;

    static HTTP_CLIENT: LazyLock<HttpClient> = LazyLock::new(|| {
        HttpClient::new(HttpClientConfig {
            max_retries: 0,
            retry_delay: Duration::from_millis(3),
            request_timeout: Duration::from_millis(500),
            max_response_size_bytes: None,
        })
        .expect("http client should be constructed")
    });

    enum StubBehavior {
        Ok { cacheable: bool },
        Fail,
        Slow,
    }

    struct StubProcessor {
        calls: Arc<AtomicUsize>,
        behavior: StubBehavior,
    }

    #[cfg_attr(not(any(target_arch = "wasm32", target_arch = "wasm64")), async_trait)]
    #[cfg_attr(any(target_arch = "wasm32", target_arch = "wasm64"), async_trait(?Send))]
    impl CustomTokenProcessor for StubProcessor {
        async fn process(
            &self,
            mapping: &str,
            _payload: &str,
        ) -> Result<ProcessedTokenClaims, CustomTokenError> {
            self.calls.fetch_add(1, Ordering::SeqCst);
            match &self.behavior {
                StubBehavior::Ok { cacheable } => {
                    let mut claims = HashMap::new();
                    claims.insert("sub".to_string(), json!("custom-sub"));
                    Ok(ProcessedTokenClaims {
                        claims,
                        token_id: format!("cid-{mapping}"),
                        issuer_id: None,
                        expiration: None,
                        cacheable: *cacheable,
                    })
                },
                StubBehavior::Fail => Err(CustomTokenError::Processing("boom".to_string())),
                StubBehavior::Slow => {
                    tokio::time::sleep(Duration::from_secs(10)).await;
                    Ok(ProcessedTokenClaims::new(HashMap::new(), "slow"))
                },
            }
        }
    }

    /// Processor that fails for any mapping containing "Fail", succeeds otherwise.
    struct DispatchProcessor;

    #[cfg_attr(not(any(target_arch = "wasm32", target_arch = "wasm64")), async_trait)]
    #[cfg_attr(any(target_arch = "wasm32", target_arch = "wasm64"), async_trait(?Send))]
    impl CustomTokenProcessor for DispatchProcessor {
        async fn process(
            &self,
            mapping: &str,
            _payload: &str,
        ) -> Result<ProcessedTokenClaims, CustomTokenError> {
            if mapping.contains("Fail") {
                Err(CustomTokenError::Processing("boom".to_string()))
            } else {
                let mut claims = HashMap::new();
                claims.insert("sub".to_string(), json!("ok"));
                Ok(ProcessedTokenClaims::new(claims, "cid"))
            }
        }
    }

    fn custom_index(entries: &[(&str, &str, bool)]) -> CustomIssuerIndex {
        let mut m = HashMap::new();
        for (name, mapping, required) in entries {
            m.insert(
                (*name).to_string(),
                CustomIssuerMetadata {
                    entity_type_name: (*mapping).to_string(),
                    required: *required,
                    required_claims: HashSet::new(),
                },
            );
        }
        CustomIssuerIndex::build(&m)
    }

    async fn custom_only_service() -> JwtService {
        JwtService::new(
            &JwtConfig::new_without_validation(),
            None,
            None,
            Arc::new(MetricsCollector::new(0)),
            HTTP_CLIENT.clone(),
        )
        .await
        .expect("custom-only JwtService should build")
    }

    #[test]
    async fn custom_token_success_and_caches() {
        let svc = custom_only_service().await;
        let calls = Arc::new(AtomicUsize::new(0));
        let processor: Arc<dyn CustomTokenProcessor> = Arc::new(StubProcessor {
            calls: calls.clone(),
            behavior: StubBehavior::Ok { cacheable: true },
        });
        let index = custom_index(&[("Acme", "Acme::CustomToken", false)]);
        let tokens = vec![TokenInput::new(
            "Acme::CustomToken".to_string(),
            "payload-1".to_string(),
        )];

        let out = svc
            .validate_multi_issuer_tokens(&tokens, Some(&processor), &index, None)
            .await
            .expect("custom token should validate");
        assert!(out.contains_key("Acme::CustomToken"));

        // Second call is served from the token cache: processor not re-invoked.
        svc.validate_multi_issuer_tokens(&tokens, Some(&processor), &index, None)
            .await
            .expect("cached custom token should validate");
        assert_eq!(calls.load(Ordering::SeqCst), 1);
    }

    #[test]
    async fn custom_token_not_cacheable_reinvokes() {
        let svc = custom_only_service().await;
        let calls = Arc::new(AtomicUsize::new(0));
        let processor: Arc<dyn CustomTokenProcessor> = Arc::new(StubProcessor {
            calls: calls.clone(),
            behavior: StubBehavior::Ok { cacheable: false },
        });
        let index = custom_index(&[("Acme", "Acme::CustomToken", false)]);
        let tokens = vec![TokenInput::new(
            "Acme::CustomToken".to_string(),
            "payload-1".to_string(),
        )];

        svc.validate_multi_issuer_tokens(&tokens, Some(&processor), &index, None)
            .await
            .unwrap();
        svc.validate_multi_issuer_tokens(&tokens, Some(&processor), &index, None)
            .await
            .unwrap();
        assert_eq!(calls.load(Ordering::SeqCst), 2);
    }

    #[test]
    async fn custom_token_required_failure_hard_errors() {
        let svc = custom_only_service().await;
        let processor: Arc<dyn CustomTokenProcessor> = Arc::new(StubProcessor {
            calls: Arc::new(AtomicUsize::new(0)),
            behavior: StubBehavior::Fail,
        });
        let index = custom_index(&[("Acme", "Acme::CustomToken", true)]);
        let tokens = vec![TokenInput::new(
            "Acme::CustomToken".to_string(),
            "payload-1".to_string(),
        )];

        let err = svc
            .validate_multi_issuer_tokens(&tokens, Some(&processor), &index, None)
            .await
            .expect_err("required custom token failure should hard-error");
        assert!(matches!(
            err,
            MultiIssuerValidationError::CustomToken(CustomTokenError::Processing(_))
        ));
    }

    #[test]
    async fn custom_token_non_required_failure_skips_and_keeps_others() {
        let svc = custom_only_service().await;
        let processor: Arc<dyn CustomTokenProcessor> = Arc::new(DispatchProcessor);
        let index = custom_index(&[("FailIss", "Fail::T", false), ("OkIss", "Ok::T", false)]);
        let tokens = vec![
            TokenInput::new("Fail::T".to_string(), "p1".to_string()),
            TokenInput::new("Ok::T".to_string(), "p2".to_string()),
        ];

        let out = svc
            .validate_multi_issuer_tokens(&tokens, Some(&processor), &index, None)
            .await
            .expect("a non-required custom failure must not fail the whole request");
        assert_eq!(out.len(), 1);
        assert!(out.contains_key("Ok::T"));
    }

    #[test]
    async fn custom_token_timeout_produces_timeout_error() {
        let svc = custom_only_service().await;
        let processor: Arc<dyn CustomTokenProcessor> = Arc::new(StubProcessor {
            calls: Arc::new(AtomicUsize::new(0)),
            behavior: StubBehavior::Slow,
        });
        let index = custom_index(&[("Acme", "Acme::CustomToken", true)]);
        let tokens = vec![TokenInput::new(
            "Acme::CustomToken".to_string(),
            "payload-1".to_string(),
        )];

        let err = svc
            .validate_multi_issuer_tokens(
                &tokens,
                Some(&processor),
                &index,
                Some(Duration::from_millis(50)),
            )
            .await
            .expect_err("slow processor should time out");
        assert!(matches!(
            err,
            MultiIssuerValidationError::CustomToken(CustomTokenError::Timeout(_))
        ));
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
                token_id: "jti".to_string(),
                required_claims: HashSet::new(),
            },
        );
        iss.token_metadata.insert(
            "Jans::Id_Token".to_string(),
            TokenEntityMetadata {
                trusted: true,
                entity_type_name: "Jans::Id_Token".to_string(),
                token_id: "jti".to_string(),
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
            Some(HashMap::from([(server.issuer().to_string(), iss)])),
            None,
            Arc::new(MetricsCollector::new(0)),
            HTTP_CLIENT.clone(),
        )
        .await
        .expect("Should create JwtService");

        // Create TokenInput structs
        let tokens = vec![
            TokenInput::new("Jans::Access_Token".to_string(), access_tkn),
            TokenInput::new("Jans::Id_Token".to_string(), id_tkn),
        ];

        let result = jwt_service
            .validate_multi_issuer_tokens(&tokens, None, &CustomIssuerIndex::default(), None)
            .await;
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
            Some(HashMap::from([(server.issuer().to_string(), iss)])),
            None,
            Arc::new(MetricsCollector::new(0)),
            HTTP_CLIENT.clone(),
        )
        .await
        .expect("Should create JwtService");

        let result = jwt_service
            .validate_multi_issuer_tokens(&[], None, &CustomIssuerIndex::default(), None)
            .await;
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
            Some(HashMap::from([(server.issuer().to_string(), iss)])),
            None,
            Arc::new(MetricsCollector::new(0)),
            HTTP_CLIENT.clone(),
        )
        .await
        .expect("Should create JwtService");

        // Create tokens with invalid JWT format
        let tokens = vec![
            TokenInput::new("Jans::Access_Token".to_string(), "invalid-jwt".to_string()),
            TokenInput::new("Jans::Id_Token".to_string(), "also-invalid".to_string()),
        ];

        let result = jwt_service
            .validate_multi_issuer_tokens(&tokens, None, &CustomIssuerIndex::default(), None)
            .await;
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
                token_id: "jti".to_string(),

                required_claims: HashSet::new(),
            },
        );
        iss.token_metadata.insert(
            "Jans::Id_Token".to_string(),
            TokenEntityMetadata {
                trusted: true,
                entity_type_name: "Jans::Id_Token".to_string(),
                token_id: "jti".to_string(),

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
            Some(HashMap::from([(server.issuer().to_string(), iss)])),
            None,
            Arc::new(MetricsCollector::new(0)),
            HTTP_CLIENT.clone(),
        )
        .await
        .expect("Should create JwtService");

        // Create tokens with one valid and one invalid
        let tokens = vec![
            TokenInput::new("Jans::Access_Token".to_string(), valid_token),
            TokenInput::new("Jans::Id_Token".to_string(), "invalid-jwt".to_string()),
        ];

        let result = jwt_service
            .validate_multi_issuer_tokens(&tokens, None, &CustomIssuerIndex::default(), None)
            .await;
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
                token_id: "jti".to_string(),

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
            Some(HashMap::from([(server.issuer().to_string(), iss)])),
            None,
            Arc::new(MetricsCollector::new(0)),
            HTTP_CLIENT.clone(),
        )
        .await
        .expect("Should create JwtService");

        // Create tokens with duplicate issuer+type combination
        let tokens = vec![
            TokenInput::new("Jans::Access_Token".to_string(), token_one),
            TokenInput::new("Jans::Access_Token".to_string(), token_two), // Duplicate type from same issuer
        ];

        let result = jwt_service
            .validate_multi_issuer_tokens(&tokens, None, &CustomIssuerIndex::default(), None)
            .await;
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
            Some(HashMap::from([(server.issuer().to_string(), iss)])),
            None,
            Arc::new(MetricsCollector::new(0)),
            HTTP_CLIENT.clone(),
        )
        .await
        .expect("Should create JwtService");

        let tokens = vec![TokenInput::new("Jans::Access_Token".to_string(), token)];

        let result = jwt_service
            .validate_multi_issuer_tokens(&tokens, None, &CustomIssuerIndex::default(), None)
            .await;
        assert!(matches!(
            result,
            Err(MultiIssuerValidationError::TokenValidationFailed)
        ));
    }

    #[test]
    async fn test_validate_multi_issuer_tokens_succeeds_after_key_rotation_without_reinit() {
        let mut server = MockServer::new_with_defaults()
            .await
            .expect("Mock server with default OIDC/JWKS should initialize");

        let mapping = "Jans::Access_Token".to_string();
        let mut iss = server.trusted_issuer();
        iss.token_metadata.insert(
            mapping.clone(),
            TokenEntityMetadata {
                trusted: true,
                entity_type_name: mapping.clone(),
                token_id: "jti".to_string(),
                required_claims: HashSet::new(),
            },
        );

        let jwt_service = JwtService::new(
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: true,
                jwt_status_validation: false,
                signature_algorithms_supported: HashSet::from_iter([Algorithm::HS256]),
                jwks_refresh_min_interval: 0,
                ..Default::default()
            },
            Some(HashMap::from([(server.issuer().to_string(), iss)])),
            None,
            Arc::new(MetricsCollector::new(0)),
            HTTP_CLIENT.clone(),
        )
        .await
        .expect("JwtService should initialize with trusted issuer metadata");

        let mut claims_before_rotation = json!({
            "iss": server.issuer(),
            "sub": "user-before-rotation",
            "jti": 1_111_111_111_u64,
            "exp": u64::MAX,
        });
        let token_before_rotation = server
            .generate_token_with_hs256sig(&mut claims_before_rotation, None)
            .expect("Token signed with initial key should be generated");

        jwt_service
            .validate_multi_issuer_tokens(
                &[TokenInput::new(mapping.clone(), token_before_rotation)],
                None,
                &CustomIssuerIndex::default(),
                None,
            )
            .await
            .expect("Token signed with initial key should validate before key rotation");

        server
            .rotate_signing_key_hs256("rotated_hs256_key_after_init")
            .expect("Mock issuer should rotate signing key and JWKS response");

        let mut claims_after_rotation = json!({
            "iss": server.issuer(),
            "sub": "user-after-rotation",
            "jti": 2_222_222_222_u64,
            "exp": u64::MAX,
        });
        let token_after_rotation = server
            .generate_token_with_hs256sig(&mut claims_after_rotation, None)
            .expect("Token signed with rotated key should be generated");

        jwt_service
            .validate_multi_issuer_tokens(
                &[TokenInput::new(
                    mapping.clone(),
                    token_after_rotation.clone(),
                )],
                None,
                &CustomIssuerIndex::default(),
                None,
            )
            .await
            .expect_err("First call after rotation should fail");

        tokio::time::sleep(std::time::Duration::from_millis(200)).await;

        jwt_service
            .validate_multi_issuer_tokens(
                &[TokenInput::new(mapping, token_after_rotation)],
                None,
                &CustomIssuerIndex::default(),
                None,
            )
            .await
            .expect("Token signed with rotated key should validate after background JWKS refresh");
    }

    #[test]
    async fn test_validate_multi_issuer_tokens_recovers_when_first_seen_kid_is_rotated() {
        let mut server = MockServer::new_with_defaults()
            .await
            .expect("Mock server with default OIDC/JWKS should initialize");

        let mapping = "Jans::Access_Token".to_string();
        let mut iss = server.trusted_issuer();
        iss.token_metadata.insert(
            mapping.clone(),
            TokenEntityMetadata {
                trusted: true,
                entity_type_name: mapping.clone(),
                token_id: "jti".to_string(),
                required_claims: HashSet::new(),
            },
        );

        let jwt_service = JwtService::new(
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: true,
                jwt_status_validation: false,
                signature_algorithms_supported: HashSet::from_iter([Algorithm::HS256]),
                jwks_refresh_min_interval: 0,
                ..Default::default()
            },
            Some(HashMap::from([(server.issuer().to_string(), iss)])),
            None,
            Arc::new(MetricsCollector::new(0)),
            HTTP_CLIENT.clone(),
        )
        .await
        .expect("JwtService should initialize with trusted issuer metadata");

        server
            .rotate_signing_key_hs256("rotated_hs256_key_before_first_validation")
            .expect("Mock issuer should rotate signing key and JWKS response");

        let mut claims_after_rotation = json!({
            "iss": server.issuer(),
            "sub": "user-after-rotation",
            "jti": 3_333_333_333_u64,
            "exp": u64::MAX,
        });
        let token_after_rotation = server
            .generate_token_with_hs256sig(&mut claims_after_rotation, None)
            .expect("Token signed with rotated key should be generated");

        jwt_service
            .validate_multi_issuer_tokens(
                &[TokenInput::new(
                    mapping.clone(),
                    token_after_rotation.clone(),
                )],
                None,
                &CustomIssuerIndex::default(),
                None,
            )
            .await
            .expect_err("First call with rotated kid should fail (stale key)");

        tokio::time::sleep(std::time::Duration::from_millis(200)).await;

        jwt_service
            .validate_multi_issuer_tokens(
                &[TokenInput::new(mapping, token_after_rotation)],
                None,
                &CustomIssuerIndex::default(),
                None,
            )
            .await
            .expect(
                "Validation should recover from unknown rotated kid after background JWKS refresh",
            );
    }

    #[test]
    async fn test_trusted_issuer_loading_info() {
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
            Some(HashMap::from([("Jans".into(), iss)])),
            None,
            Arc::new(MetricsCollector::new(0)),
            HTTP_CLIENT.clone(),
        )
        .await
        .expect("Should create JwtService");

        // Test is_trusted_issuer_loaded_by_name
        assert!(jwt_service.is_trusted_issuer_loaded_by_name("Jans"));
        assert!(!jwt_service.is_trusted_issuer_loaded_by_name("NonExistent"));

        // Test is_trusted_issuer_loaded_by_iss
        assert!(jwt_service.is_trusted_issuer_loaded_by_iss(server.issuer().as_str()));
        assert!(!jwt_service.is_trusted_issuer_loaded_by_iss("https://nonexistent.com"));

        // Test loaded_trusted_issuers_count
        assert_eq!(jwt_service.loaded_trusted_issuers_count(), 1);

        // Test loaded_trusted_issuer_ids
        let loaded_ids = jwt_service.loaded_trusted_issuer_ids();
        assert_eq!(loaded_ids.len(), 1);
        assert!(loaded_ids.contains("Jans"));

        // Test failed_trusted_issuer_ids
        let failed_ids = jwt_service.failed_trusted_issuer_ids();
        assert!(failed_ids.is_empty());
    }
}
