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

pub use custom_token::{CustomTokenError, CustomTokenProcessor, ProcessedTokenClaims};
pub(crate) use decode::*;
pub(crate) use error::*;
pub use loading_info::TrustedIssuerLoadingInfo;
pub(crate) use token::{CustomTokenIssuerMeta, Token, TokenClaims, TokenIssuer};
pub(crate) use token_cache::TokenCache;
pub(crate) use validation::ValidateJwtError;

use crate::JwtConfig;
use crate::LogLevel;
use crate::LogWriter;
use crate::authz::MultiIssuerValidationError;
use crate::authz::metrics::MetricsCollector;
use crate::authz::request::TokenInput;
use crate::authz::{DropReason, DroppedToken};
use crate::common::issuer_utils::IssClaim;
use crate::common::policy_store::token_types::{EntityTypeName, IssuerName, TokenType};
use crate::common::policy_store::{
    CustomTokenMetadata, TokenEntityMetadata, TokenTypeKind, TokenTypes, TrustedIssuer,
};

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
    JwtValidator, JwtValidatorCache, OwnedValidatorInfo, TokenKind, ValidatedJwt, ValidatorInfo,
};

/// A JWT whose mapping no trusted issuer declares. Never returned to the caller: the
/// token is dropped and this only feeds the log and metrics.
#[derive(Debug, thiserror::Error)]
#[error("no trusted issuer declares a token for the mapping '{0}'")]
pub(crate) struct UnknownTokenMapping(String);

#[allow(clippy::cast_possible_truncation)]
pub(crate) fn parse_numeric_date(value: &serde_json::Value) -> Option<i64> {
    if let Some(i) = value.as_i64() {
        return Some(i);
    }
    if let Some(f) = value.as_f64() {
        return f.is_finite().then(|| f.floor() as i64);
    }
    value
        .as_str()
        .and_then(|s| s.trim().parse::<f64>().ok())
        .filter(|f| f.is_finite())
        .map(|f| f.floor() as i64)
}

/// Handles JWT validation
pub(crate) struct JwtService {
    validators: Arc<JwtValidatorCache>,
    key_service: Arc<KeyService>,
    issuer_configs: Arc<IssuerIndex>,
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
    /// The JWT token types this issuer declares, by entity type.
    tokens: HashMap<EntityTypeName, TokenEntityMetadata>,
    /// The [`OpenIdConfig`] loaded from the IDP's `/.well-known/openid-configuration` endpoint
    openid_config: Option<OpenIdConfig>,
}

/// The configured custom token type a token input routes to.
#[derive(Clone, Copy)]
struct CustomTarget<'a> {
    issuer: &'a IssuerName,
    token_meta: &'a CustomTokenMetadata,
}

struct TokenCallCtx<'a> {
    token: &'a TokenInput,
    index: usize,
    now: chrono::DateTime<Utc>,
    seen_combinations: &'a mut HashSet<SeenToken>,
}

/// Accepted `(issuer, mapping)` pair seen in the current multi-issuer request.
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
struct SeenToken {
    issuer: SmolStr,
    mapping: SmolStr,
}

/// Outcome of dispatching a single multi-issuer token input: either a
/// validated Cedar token or the claim-free reason it was dropped.
enum TokenOutcome {
    /// The token was validated successfully and produced a Cedar token.
    Validated(Arc<Token>),
    /// The token was dropped without failing the whole request.
    ///
    /// Carries the [`DropReason`] recorded in the drop metadata; the token's
    /// claims are intentionally not retained.
    Dropped(DropReason),
}

/// Result of [`JwtService::validate_multi_issuer_tokens`]: the validated tokens
/// plus the drop metadata the caller threads into the authorization result and
/// decision log. Access the fields explicitly (e.g. `result.tokens`).
#[derive(Debug)]
pub(crate) struct ValidatedMultiIssuerTokens {
    /// Validated tokens keyed by their mapping (token name). Each entry carries
    /// the token alongside its original request index so callers never need a
    /// separate index lookup.
    pub tokens: HashMap<String, ValidatedToken>,
    /// Tokens dropped during validation.
    pub dropped: Vec<DroppedToken>,
}

/// A validated token with its original request index.
#[derive(Debug, Clone)]
pub(crate) struct ValidatedToken {
    /// Validated Cedar token.
    pub token: Arc<Token>,
    /// Zero-based position in the request's `tokens` array.
    pub index: usize,
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
        token_types: &TokenTypes,
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

        let loading_state = Arc::new(TrustedIssuerLoadingState::new(
            token_types.jwt_issuers().len(),
        ));

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

        loader.load_trusted_issuers(token_types).await?;

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
            logger,
            token_cache,
            loading_state,
            jwks_refresh_notifiers,
            jwks_cancel_token,
            metrics,
        })
    }

    /// Validates a single JWT as a token of `trusted_iss`, the issuer its mapping
    /// names, and returns the decoded claims.
    fn validate_single_token(
        &self,
        token_kind: &TokenKind,
        trusted_iss: &Arc<TrustedIssuer>,
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

        // get validator; only the configured `iss` of `trusted_iss` has one for
        // this mapping, so a token of another issuer fails here
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
        // The validator for this `iss` and mapping has already required the
        // token type's `required_claims`.
        validated_jwt.trusted_iss = Some(trusted_iss.clone());

        Ok(validated_jwt)
    }

    /// Flush cached custom-token verdicts, leaving JWT cache entries intact.
    pub(crate) fn clear_custom_token_cache(&self) {
        self.token_cache.clear_custom_tokens();
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
        token_types: &TokenTypes,
        custom_timeout: Option<Duration>,
    ) -> Result<ValidatedMultiIssuerTokens, MultiIssuerValidationError> {
        if tokens.is_empty() {
            return Err(MultiIssuerValidationError::EmptyTokenArray);
        }

        let mut validated_tokens: HashMap<String, ValidatedToken> = HashMap::new();
        let mut dropped = Vec::new();
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
                dropped.push(DroppedToken::new(
                    token.mapping.clone(),
                    index,
                    DropReason::InvalidInput(err),
                ));
                continue;
            }

            // Create Token with the mapping as the name
            let token_name = token.mapping.clone();

            // Custom-token dispatch: a mapping that routes to a custom issuer is
            // never handled by the JWT path.
            //
            // DEBT: this seam belongs in `Authz::multi_issuer_setup`, which would
            // split the input into custom and JWT tokens and leave `JwtService`
            // JWT-only. It is not a pure move: two pieces of state the custom path
            // borrows live in here and would have to come along.
            //   - `seen_combinations` spans both branches, so the duplicate
            //     (issuer, mapping) check has to move up with it or custom and JWT
            //     tokens stop conflicting with each other.
            //   - `token_cache` is owned by `JwtService` and used by the custom path
            //     for `process` memoization, so it has to be shared or split.
            // Worth its own PR with those two covered by tests, not a drive-by.
            let mut ctx = TokenCallCtx {
                token,
                index,
                now,
                seen_combinations: &mut seen_combinations,
            };
            let result = match token_types.get(&token.mapping) {
                Some(TokenType {
                    issuer,
                    kind: TokenTypeKind::Custom(token_meta),
                    ..
                }) => {
                    let target = CustomTarget { issuer, token_meta };
                    self.handle_custom_token(custom_processor, target, custom_timeout, &mut ctx)
                        .await
                },
                Some(TokenType {
                    kind: TokenTypeKind::Jwt { issuer, .. },
                    ..
                }) => self.validate_jwt_token(issuer, &mut ctx)?,
                None => self.drop_unknown_mapping(&ctx),
            };

            match result {
                TokenOutcome::Validated(cedar_token) => {
                    if let Some(prior) = validated_tokens.get(&token_name) {
                        let prior_index = prior.index;
                        if let Some(logger) = &self.logger {
                            logger.log_any(JwtLogEntry::new(
                                format!(
                                    "Duplicate mapping detected: '{token_name}' \
                                    at index {index} replaces index {prior_index} (last wins)"
                                ),
                                Some(LogLevel::WARN),
                            ));
                        }
                        dropped.push(DroppedToken::new(
                            token_name.clone(),
                            prior_index,
                            DropReason::DuplicateMapping,
                        ));
                    }
                    validated_tokens.insert(
                        token_name.clone(),
                        ValidatedToken {
                            token: cedar_token,
                            index,
                        },
                    );
                },
                TokenOutcome::Dropped(reason) => {
                    dropped.push(DroppedToken::new(token.mapping.clone(), index, reason));
                },
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

        Ok(ValidatedMultiIssuerTokens {
            tokens: validated_tokens,
            dropped,
        })
    }

    /// Dispatch a single token input whose mapping routes to a custom issuer.
    ///
    /// Every failure (no processor, duplicate, processing error or timeout) drops
    /// only this token; a policy that needs it must check for it.
    async fn handle_custom_token(
        &self,
        custom_processor: Option<&Arc<dyn CustomTokenProcessor>>,
        target: CustomTarget<'_>,
        custom_timeout: Option<Duration>,
        ctx: &mut TokenCallCtx<'_>,
    ) -> TokenOutcome {
        let Some(processor) = custom_processor else {
            let err = MultiIssuerValidationError::CustomToken(
                CustomTokenError::NoProcessorRegistered(ctx.token.mapping.clone()),
            );
            self.metrics.record_error(&err);
            if let Some(logger) = &self.logger {
                logger.log_any(JwtLogEntry::new(
                    format!("Custom token dropped at index {}: {err}", ctx.index),
                    Some(LogLevel::WARN),
                ));
            }
            return TokenOutcome::Dropped(DropReason::NoProcessorRegistered);
        };

        let combination = SeenToken {
            issuer: SmolStr::from(target.issuer.key_prefix()),
            mapping: SmolStr::from(ctx.token.mapping.as_str()),
        };
        if ctx.seen_combinations.contains(&combination) {
            if let Some(logger) = &self.logger {
                logger.log_any(JwtLogEntry::new(
                    format!(
                        "Non-deterministic custom token detected: type '{}' (duplicate found, skipping before processing)",
                        ctx.token.mapping
                    ),
                    Some(LogLevel::WARN),
                ));
            }
            return TokenOutcome::Dropped(DropReason::DuplicateToken);
        }

        match self
            .process_custom_token(processor, target, ctx.token, custom_timeout, ctx.now)
            .await
        {
            Ok(cedar_token) => {
                let issuer = cedar_token
                    .extract_normalized_issuer()
                    .map(|i| SmolStr::from(i.as_str()))
                    .unwrap_or_default();
                if ctx.seen_combinations.insert(SeenToken {
                    issuer,
                    mapping: SmolStr::from(ctx.token.mapping.as_str()),
                }) {
                    TokenOutcome::Validated(cedar_token)
                } else {
                    if let Some(logger) = &self.logger {
                        logger.log_any(JwtLogEntry::new(
                            format!(
                                "Non-deterministic custom token detected: type '{}' (duplicate found, skipping)",
                                ctx.token.mapping
                            ),
                            Some(LogLevel::WARN),
                        ));
                    }
                    TokenOutcome::Dropped(DropReason::DuplicateToken)
                }
            },
            Err(err) => {
                // Skip and continue, logging timeout vs. processor error distinctly.
                self.metrics.record_error(&err);
                let timed_out = matches!(
                    err,
                    MultiIssuerValidationError::CustomToken(CustomTokenError::Timeout(_))
                );
                if let Some(logger) = &self.logger {
                    let reason = if timed_out {
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
                let reason = if timed_out {
                    DropReason::CustomProcessingTimedOut
                } else {
                    DropReason::CustomProcessingFailed
                };
                TokenOutcome::Dropped(reason)
            },
        }
    }

    /// Drops a token whose mapping no token type declares.
    fn drop_unknown_mapping(&self, ctx: &TokenCallCtx<'_>) -> TokenOutcome {
        let err = UnknownTokenMapping(ctx.token.mapping.clone());
        self.metrics.record_error(&err);
        if let Some(logger) = &self.logger {
            logger.log_any(JwtLogEntry::new(
                format!("Token dropped at index {}: {err}", ctx.index),
                Some(LogLevel::WARN),
            ));
        }
        TokenOutcome::Dropped(DropReason::JwtValidationFailed)
    }

    /// Validates a token as a JWT of `trusted_iss`, the issuer its mapping
    /// names, via the cache or single-token validation.
    ///
    /// Returns `TokenOutcome::Dropped` when the token is dropped (duplicate or
    /// validation failure); issuer/claim errors abort the whole batch.
    fn validate_jwt_token(
        &self,
        trusted_iss: &Arc<TrustedIssuer>,
        ctx: &mut TokenCallCtx<'_>,
    ) -> Result<TokenOutcome, MultiIssuerValidationError> {
        let token_kind = TokenKind::AuthorizeMultiIssuer(Cow::Borrowed(&ctx.token.mapping));

        if let Some(cedar_token) = self.token_cache.find(&token_kind, &ctx.token.payload) {
            let issuer = cedar_token
                .get_claim_val("iss")
                .and_then(|iss| iss.as_str())
                .ok_or(MultiIssuerValidationError::MissingIssuer)?;
            if !self.mark_jwt_seen(ctx, issuer) {
                return Ok(TokenOutcome::Dropped(DropReason::DuplicateToken));
            }
            return Ok(TokenOutcome::Validated(cedar_token));
        }

        // Validate JWT using existing single token validation
        match self.validate_single_token(&token_kind, trusted_iss, &ctx.token.payload) {
            Ok(validated_jwt) => {
                self.metrics.record_jwt_validation(true);
                // Extract issuer for non-deterministic check
                let issuer = validated_jwt
                    .claims
                    .get("iss")
                    .and_then(|iss| iss.as_str())
                    .ok_or(MultiIssuerValidationError::MissingIssuer)?;

                // Check for non-deterministic tokens (graceful validation)
                if !self.mark_jwt_seen(ctx, issuer) {
                    return Ok(TokenOutcome::Dropped(DropReason::DuplicateToken));
                }

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

                Ok(TokenOutcome::Validated(cedar_token))
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
                Ok(TokenOutcome::Dropped(DropReason::JwtValidationFailed))
            },
        }
    }

    /// Record the JWT's `(issuer, mapping)` pair as seen in this request.
    /// Returns `false` and logs a warning when the pair was already accepted,
    /// so the caller drops the token as [`DropReason::DuplicateToken`].
    fn mark_jwt_seen(&self, ctx: &mut TokenCallCtx<'_>, issuer: &str) -> bool {
        let inserted = ctx.seen_combinations.insert(SeenToken {
            issuer: SmolStr::from(issuer),
            mapping: SmolStr::from(ctx.token.mapping.as_str()),
        });
        if !inserted && let Some(logger) = &self.logger {
            logger.log_any(JwtLogEntry::new(
                format!(
                    "Non-deterministic token detected: type '{}' from issuer '{}' (duplicate found, skipping)",
                    ctx.token.mapping, issuer
                ),
                Some(LogLevel::WARN),
            ));
        }
        inserted
    }

    /// Process a single custom (non-JWT) token via the registered processor.
    ///
    /// Enforces required claims, and caches the result
    /// unless the processor opts out. Serves a still-valid cached result on hit.
    ///
    /// DEBT: lives in `JwtService` for a minimal v1 diff; see the note at the
    /// dispatch site in [`validate_multi_issuer_tokens`](Self::validate_multi_issuer_tokens)
    /// for what moving this seam up to `Authz` would take.
    async fn process_custom_token(
        &self,
        processor: &Arc<dyn CustomTokenProcessor>,
        target: CustomTarget<'_>,
        input: &TokenInput,
        timeout: Option<Duration>,
        now: chrono::DateTime<Utc>,
    ) -> Result<Arc<Token>, MultiIssuerValidationError> {
        let token_kind = TokenKind::AuthorizeCustom(Cow::Borrowed(input.mapping.as_str()));

        // Serve a still-valid cached result if present.
        if let Some(cached) = self.token_cache.find(&token_kind, &input.payload) {
            return Ok(cached);
        }

        let started = Utc::now();
        let outcome: Result<Arc<Token>, MultiIssuerValidationError> = async {
            let process_fut = processor.process(&input.mapping, &input.payload);
            let processed = match timeout {
                None => process_fut.await?,
                Some(dur) => {
                    tokio::select! {
                        res = process_fut => res?,
                        () = crate::async_sleep::sleep(dur) => {
                            return Err(CustomTokenError::Timeout(dur).into());
                        },
                    }
                },
            };

            if processed.token_id.is_empty() {
                return Err(CustomTokenError::EmptyTokenId(input.mapping.clone()).into());
            }

            for required in &target.token_meta.required_claims {
                if !processed.claims.contains_key(required) {
                    return Err(CustomTokenError::MissingRequiredClaim(required.clone()).into());
                }
            }

            // Cedarling does not validate a custom payload, but it does honor the
            // expiration the processor reported: the explicit `expiration` field, or an
            // `exp` claim when the processor put it there instead. Without this the
            // fresh path and the cache disagree — the cache refuses to store or serve an
            // expired token, so an expired one would be accepted on every request via
            // re-processing.
            let expiration = processed
                .expiration
                .or_else(|| processed.claims.get("exp").and_then(parse_numeric_date));
            if let Some(exp) = expiration.filter(|exp| *exp <= now.timestamp()) {
                return Err(CustomTokenError::Expired {
                    mapping: input.mapping.clone(),
                    exp,
                }
                .into());
            }

            let meta = CustomTokenIssuerMeta {
                issuer_id: target.issuer.key_prefix(),
                entity_type_name: Some(input.mapping.clone()),
                token_id: processed.token_id.clone(),
                expiration,
            };
            let cacheable = processed.cacheable;
            let claims = TokenClaims::from(processed.claims);
            let cedar_token = Arc::new(Token::new(
                &input.mapping,
                claims,
                Some(TokenIssuer::Custom(meta)),
            ));

            if cacheable {
                self.token_cache.save_with_expiration(
                    &token_kind,
                    &input.payload,
                    cedar_token.clone(),
                    now,
                    expiration,
                );
            }

            Ok(cedar_token)
        }
        .await;

        let elapsed_us = Utc::now()
            .signed_duration_since(started)
            .num_microseconds()
            .unwrap_or(i64::MAX);
        self.metrics
            .record_custom_token(outcome.is_ok(), elapsed_us);

        outcome
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
}

impl Drop for JwtService {
    fn drop(&mut self) {
        self.jwks_cancel_token.cancel();
    }
}

impl TrustedIssuerLoadingInfo for JwtService {
    fn is_trusted_issuer_loaded_by_name(&self, issuer_name: &str) -> bool {
        self.issuer_configs.is_issuer_id_present(issuer_name)
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
    use super::{CustomTokenError, CustomTokenProcessor, ProcessedTokenClaims, Token, TokenIssuer};
    use crate::JwtConfig;
    use crate::app_types::PdpID;
    use crate::authz::MultiIssuerValidationError;
    use crate::authz::metrics::{MetricsCollector, MetricsMode};
    use crate::authz::request::TokenInput;
    use crate::common::policy_store::TokenEntityMetadata;
    use crate::common::policy_store::{
        CustomIssuerMetadata, CustomTokenMetadata, KeySource, TokenTypeKind, TokenTypes,
        TrustedIssuer,
    };
    use crate::common::policy_store::{IssuerName, TestJwtIssuer};
    use crate::http::HttpClient;
    use crate::http::HttpClientConfig;
    use crate::log::{LogStrategy, Logger};
    use crate::{
        DropReason, DroppedToken, LogConfig, LogLevel, LogStorage, LogTypeConfig, MemoryLogConfig,
    };
    use async_trait::async_trait;
    use jsonwebtoken::Algorithm;
    use serde_json::json;
    use std::collections::{HashMap, HashSet};
    use std::sync::Arc;
    use std::sync::LazyLock;
    use std::sync::atomic::{AtomicUsize, Ordering};
    use std::time::Duration;
    use tokio::test;
    use url::Url;

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
        Ok {
            cacheable: bool,
        },
        /// Emits `sub` plus an `exp` claim at the given offset from now (seconds).
        OkWithExp {
            exp_offset_secs: i64,
        },
        /// Emits `sub` and reports expiration through `ProcessedTokenClaims`
        /// instead of an `exp` claim.
        OkWithExpirationField {
            exp_offset_secs: i64,
        },
        Fail,
        Slow,
        SlowOk,
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
                        expiration: None,
                        cacheable: *cacheable,
                    })
                },
                StubBehavior::OkWithExp { exp_offset_secs } => {
                    let mut claims = HashMap::new();
                    claims.insert("sub".to_string(), json!("custom-sub"));
                    claims.insert(
                        "exp".to_string(),
                        json!(chrono::Utc::now().timestamp() + exp_offset_secs),
                    );
                    Ok(ProcessedTokenClaims::new(claims, "cid-exp"))
                },
                StubBehavior::OkWithExpirationField { exp_offset_secs } => {
                    let mut claims = HashMap::new();
                    claims.insert("sub".to_string(), json!("custom-sub"));
                    let mut processed = ProcessedTokenClaims::new(claims, "cid-expiration");
                    processed.expiration = Some(chrono::Utc::now().timestamp() + exp_offset_secs);
                    Ok(processed)
                },
                StubBehavior::Fail => Err(CustomTokenError::Processing("boom".to_string())),
                StubBehavior::Slow => {
                    tokio::time::sleep(Duration::from_secs(10)).await;
                    Ok(ProcessedTokenClaims::new(HashMap::new(), "slow"))
                },
                StubBehavior::SlowOk => {
                    tokio::time::sleep(Duration::from_millis(30)).await;
                    let mut claims = HashMap::new();
                    claims.insert("sub".to_string(), json!("slow-ok"));
                    Ok(ProcessedTokenClaims::new(claims, "slow-ok"))
                },
            }
        }
    }

    /// Processor that fails for any mapping containing "Fail", hangs for one
    /// containing "Slow", and succeeds otherwise.
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
            } else if mapping.contains("Slow") {
                tokio::time::sleep(Duration::from_secs(10)).await;
                Ok(ProcessedTokenClaims::new(HashMap::new(), "slow"))
            } else {
                let mut claims = HashMap::new();
                claims.insert("sub".to_string(), json!("ok"));
                Ok(ProcessedTokenClaims::new(claims, "cid"))
            }
        }
    }

    #[test]
    async fn parse_numeric_date_accepts_int_float_and_string() {
        // Integer (the common case).
        assert_eq!(
            super::parse_numeric_date(&json!(1_700_000_000)),
            Some(1_700_000_000)
        );
        // RFC 7519 permits non-integer NumericDate
        assert_eq!(
            super::parse_numeric_date(&json!(1_700_000_000.9)),
            Some(1_700_000_000)
        );
        // Numeric string (upstream sometimes forwards it as text).
        assert_eq!(
            super::parse_numeric_date(&json!("1700000000")),
            Some(1_700_000_000)
        );
        assert_eq!(
            super::parse_numeric_date(&json!(" 1700000000.5 ")),
            Some(1_700_000_000)
        );
    }

    #[test]
    async fn parse_numeric_date_rejects_non_finite_and_garbage() {
        // Non-finite floats cannot round-trip through JSON literals, but a string
        // that parses to one must not slip through, and neither must plain garbage.
        assert_eq!(super::parse_numeric_date(&json!("inf")), None);
        assert_eq!(super::parse_numeric_date(&json!("NaN")), None);
        assert_eq!(super::parse_numeric_date(&json!("not-a-date")), None);
        assert_eq!(super::parse_numeric_date(&json!(true)), None);
        assert_eq!(super::parse_numeric_date(&json!(null)), None);
    }

    /// Builds custom token types from `(issuer name, mapping)` pairs.
    fn custom_index(entries: &[(&str, &str)]) -> TokenTypes {
        let mut m: HashMap<String, CustomIssuerMetadata> = HashMap::new();
        for (name, mapping) in entries {
            m.entry((*name).to_string())
                .or_insert_with(|| CustomIssuerMetadata {
                    tokens_mappings: HashMap::new(),
                })
                .tokens_mappings
                .insert((*mapping).to_string(), CustomTokenMetadata::default());
        }
        TokenTypes::build([], m).expect("test custom token types should build")
    }

    /// Processes one `Acme::CustomToken` directly, to see the error a dropped
    /// token was dropped for.
    async fn process_one(
        processor: Arc<dyn CustomTokenProcessor>,
        index: &TokenTypes,
        timeout: Option<Duration>,
    ) -> Result<Arc<Token>, MultiIssuerValidationError> {
        let svc = custom_only_service().await;
        let input = TokenInput::new("Acme::CustomToken".to_string(), "payload-1".to_string());
        let token_type = index
            .get(&input.mapping)
            .expect("Acme::CustomToken should be configured");
        let TokenTypeKind::Custom(token_meta) = &token_type.kind else {
            unreachable!("Acme::CustomToken is configured as a custom token")
        };
        let target = super::CustomTarget {
            issuer: &token_type.issuer,
            token_meta,
        };
        svc.process_custom_token(&processor, target, &input, timeout, chrono::Utc::now())
            .await
    }

    async fn custom_only_service() -> JwtService {
        JwtService::new(
            &JwtConfig::new_without_validation(),
            &TokenTypes::default(),
            None,
            Arc::new(MetricsCollector::new(MetricsMode::Local)),
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
        let index = custom_index(&[("Acme", "Acme::CustomToken")]);
        let tokens = vec![TokenInput::new(
            "Acme::CustomToken".to_string(),
            "payload-1".to_string(),
        )];

        let out = svc
            .validate_multi_issuer_tokens(&tokens, Some(&processor), &index, None)
            .await
            .expect("custom token should validate");
        assert!(
            out.tokens.contains_key("Acme::CustomToken"),
            "validated output should contain the custom token under its mapping name"
        );

        // Second call is served from the token cache: processor not re-invoked.
        svc.validate_multi_issuer_tokens(&tokens, Some(&processor), &index, None)
            .await
            .expect("cached custom token should validate");
        assert_eq!(
            calls.load(Ordering::SeqCst),
            1,
            "a cacheable custom token must be processed only once; the second call should hit the token cache"
        );
    }

    #[test]
    async fn custom_token_not_cacheable_reinvokes() {
        let svc = custom_only_service().await;
        let calls = Arc::new(AtomicUsize::new(0));
        let processor: Arc<dyn CustomTokenProcessor> = Arc::new(StubProcessor {
            calls: calls.clone(),
            behavior: StubBehavior::Ok { cacheable: false },
        });
        let index = custom_index(&[("Acme", "Acme::CustomToken")]);
        let tokens = vec![TokenInput::new(
            "Acme::CustomToken".to_string(),
            "payload-1".to_string(),
        )];

        svc.validate_multi_issuer_tokens(&tokens, Some(&processor), &index, None)
            .await
            .expect("first non-cacheable custom token should validate");
        svc.validate_multi_issuer_tokens(&tokens, Some(&processor), &index, None)
            .await
            .expect("second non-cacheable custom token should validate");
        assert_eq!(
            calls.load(Ordering::SeqCst),
            2,
            "a non-cacheable custom token must be re-processed on every call"
        );
    }

    #[test]
    async fn custom_token_failure_drops_the_only_token() {
        let svc = custom_only_service().await;
        let processor: Arc<dyn CustomTokenProcessor> = Arc::new(StubProcessor {
            calls: Arc::new(AtomicUsize::new(0)),
            behavior: StubBehavior::Fail,
        });
        let index = custom_index(&[("Acme", "Acme::CustomToken")]);
        let tokens = vec![TokenInput::new(
            "Acme::CustomToken".to_string(),
            "payload-1".to_string(),
        )];

        let err = svc
            .validate_multi_issuer_tokens(&tokens, Some(&processor), &index, None)
            .await
            .expect_err("no token survives, so the request has no valid tokens");
        assert!(
            matches!(err, MultiIssuerValidationError::TokenValidationFailed),
            "a failing custom token should be dropped, not fail the request itself, got {err:?}"
        );
    }

    #[test]
    async fn custom_token_failure_skips_and_keeps_others() {
        let svc = custom_only_service().await;
        let processor: Arc<dyn CustomTokenProcessor> = Arc::new(DispatchProcessor);
        let index = custom_index(&[("FailIss", "Fail::T"), ("OkIss", "Ok::T")]);
        let tokens = vec![
            TokenInput::new("Fail::T".to_string(), "p1".to_string()),
            TokenInput::new("Ok::T".to_string(), "p2".to_string()),
        ];

        let out = svc
            .validate_multi_issuer_tokens(&tokens, Some(&processor), &index, None)
            .await
            .expect("a custom failure must not fail the whole request");
        assert_eq!(
            out.tokens.keys().collect::<Vec<_>>(),
            vec!["Ok::T"],
            "only the succeeding custom token should be kept"
        );
        assert!(
            matches!(
                &out.dropped[..],
                [DroppedToken { mapping, index: 0, reason: DropReason::CustomProcessingFailed }]
                    if mapping == "Fail::T"
            ),
            "the failing token should be reported as dropped, got {:?}",
            out.dropped
        );
    }

    #[test]
    async fn custom_token_timeout_produces_timeout_error() {
        let processor: Arc<dyn CustomTokenProcessor> = Arc::new(StubProcessor {
            calls: Arc::new(AtomicUsize::new(0)),
            behavior: StubBehavior::Slow,
        });
        let index = custom_index(&[("Acme", "Acme::CustomToken")]);

        let err = process_one(processor, &index, Some(Duration::from_millis(50)))
            .await
            .expect_err("slow processor should time out");
        assert!(
            matches!(
                err,
                MultiIssuerValidationError::CustomToken(CustomTokenError::Timeout(_))
            ),
            "a slow custom processor past the deadline should surface as a Timeout error"
        );
    }

    #[test]
    async fn custom_token_timeout_drops_the_token() {
        let svc = custom_only_service().await;
        let processor: Arc<dyn CustomTokenProcessor> = Arc::new(DispatchProcessor);
        let index = custom_index(&[("SlowIss", "Slow::T"), ("OkIss", "Ok::T")]);
        let tokens = vec![
            TokenInput::new("Slow::T".to_string(), "p1".to_string()),
            TokenInput::new("Ok::T".to_string(), "p2".to_string()),
        ];

        let out = svc
            .validate_multi_issuer_tokens(
                &tokens,
                Some(&processor),
                &index,
                Some(Duration::from_millis(50)),
            )
            .await
            .expect("a timed-out custom token must not fail the whole request");
        assert!(
            out.tokens.contains_key("Ok::T"),
            "the token that finished in time should be kept"
        );
        assert!(
            matches!(
                &out.dropped[..],
                [DroppedToken {
                    index: 0,
                    reason: DropReason::CustomProcessingTimedOut,
                    ..
                }]
            ),
            "the slow token should be dropped as timed out, got {:?}",
            out.dropped
        );
    }

    #[test]
    async fn custom_token_missing_required_claim_errors() {
        let processor: Arc<dyn CustomTokenProcessor> = Arc::new(StubProcessor {
            calls: Arc::new(AtomicUsize::new(0)),
            // StubBehavior::Ok emits only `sub`, never `scope`.
            behavior: StubBehavior::Ok { cacheable: true },
        });
        let mut m = HashMap::new();
        m.insert(
            "Acme".to_string(),
            CustomIssuerMetadata {
                tokens_mappings: HashMap::from([(
                    "Acme::CustomToken".to_string(),
                    CustomTokenMetadata {
                        required_claims: HashSet::from(["scope".to_string()]),
                    },
                )]),
            },
        );
        let index = TokenTypes::build([], m).expect("test custom token types should build");

        let err = process_one(processor, &index, None)
            .await
            .expect_err("missing required claim should fail");
        assert!(
            matches!(
                err,
                MultiIssuerValidationError::CustomToken(CustomTokenError::MissingRequiredClaim(ref c))
                    if c == "scope"
            ),
            "expected MissingRequiredClaim(scope), got {err:?}"
        );
    }

    #[test]
    async fn custom_token_past_exp_errors() {
        let processor: Arc<dyn CustomTokenProcessor> = Arc::new(StubProcessor {
            calls: Arc::new(AtomicUsize::new(0)),
            behavior: StubBehavior::OkWithExp {
                exp_offset_secs: -60,
            },
        });
        let index = custom_index(&[("Acme", "Acme::CustomToken")]);

        let err = process_one(processor, &index, None)
            .await
            .expect_err("an already-expired custom token should be rejected");
        assert!(
            matches!(
                err,
                MultiIssuerValidationError::CustomToken(CustomTokenError::Expired {
                    ref mapping,
                    ..
                }) if mapping == "Acme::CustomToken"
            ),
            "expected Expired {{ mapping: Acme::CustomToken }}, got {err:?}"
        );
    }

    #[test]
    async fn custom_token_past_expiration_field_errors() {
        let processor: Arc<dyn CustomTokenProcessor> = Arc::new(StubProcessor {
            calls: Arc::new(AtomicUsize::new(0)),
            behavior: StubBehavior::OkWithExpirationField {
                exp_offset_secs: -60,
            },
        });
        let index = custom_index(&[("Acme", "Acme::CustomToken")]);

        let err = process_one(processor, &index, None)
            .await
            .expect_err("a past `expiration` should be rejected even without an exp claim");
        assert!(
            matches!(
                err,
                MultiIssuerValidationError::CustomToken(CustomTokenError::Expired {
                    ref mapping,
                    ..
                }) if mapping == "Acme::CustomToken"
            ),
            "expected Expired {{ mapping: Acme::CustomToken }}, got {err:?}"
        );
    }

    #[test]
    async fn custom_token_future_exp_is_accepted() {
        let svc = custom_only_service().await;
        let processor: Arc<dyn CustomTokenProcessor> = Arc::new(StubProcessor {
            calls: Arc::new(AtomicUsize::new(0)),
            behavior: StubBehavior::OkWithExp {
                exp_offset_secs: 3600,
            },
        });
        let index = custom_index(&[("Acme", "Acme::CustomToken")]);
        let tokens = vec![TokenInput::new(
            "Acme::CustomToken".to_string(),
            "payload-1".to_string(),
        )];

        let validated = svc
            .validate_multi_issuer_tokens(&tokens, Some(&processor), &index, None)
            .await
            .expect("a custom token whose exp is in the future should validate");
        assert!(
            validated.tokens.contains_key("Acme::CustomToken"),
            "an unexpired custom token should reach the validated token map"
        );
    }

    #[test]
    async fn custom_token_no_processor_skips() {
        let svc = custom_only_service().await;
        let index = custom_index(&[("Acme", "Acme::CustomToken")]);
        let tokens = vec![TokenInput::new(
            "Acme::CustomToken".to_string(),
            "payload-1".to_string(),
        )];

        let err = svc
            .validate_multi_issuer_tokens(&tokens, None, &index, None)
            .await
            .expect_err("only token skipped -> no validated tokens");
        assert!(
            matches!(err, MultiIssuerValidationError::TokenValidationFailed),
            "a custom mapping without a processor should skip, got {err:?}"
        );
    }

    #[test]
    async fn custom_token_zero_timeout_runs_to_completion() {
        let svc = custom_only_service().await;
        let calls = Arc::new(AtomicUsize::new(0));
        let processor: Arc<dyn CustomTokenProcessor> = Arc::new(StubProcessor {
            calls: calls.clone(),
            behavior: StubBehavior::SlowOk,
        });
        let index = custom_index(&[("Acme", "Acme::CustomToken")]);
        let tokens = vec![TokenInput::new(
            "Acme::CustomToken".to_string(),
            "payload-1".to_string(),
        )];

        let out = svc
            .validate_multi_issuer_tokens(&tokens, Some(&processor), &index, None)
            .await
            .expect("no timeout -> slow processor completes");
        assert!(out.tokens.contains_key("Acme::CustomToken"));
        assert_eq!(calls.load(Ordering::SeqCst), 1);
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

        let mut iss = server.trusted_issuer().with_tokens(HashMap::new());
        // Add token metadata for multi-issuer validation
        iss.tokens.insert(
            "Jans::Access_Token".to_string(),
            TokenEntityMetadata {
                token_id: "jti".to_string(),
                required_claims: HashSet::new(),
            },
        );
        iss.tokens.insert(
            "Jans::Id_Token".to_string(),
            TokenEntityMetadata {
                token_id: "jti".to_string(),
                required_claims: HashSet::new(),
            },
        );

        let token_types = TokenTypes::build([iss], []).expect("test token types should build");

        let jwt_service = JwtService::new(
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: true,
                jwt_status_validation: false,
                signature_algorithms_supported: HashSet::from_iter([Algorithm::HS256]),
                ..Default::default()
            },
            &token_types,
            None,
            Arc::new(MetricsCollector::new(MetricsMode::Local)),
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
            .validate_multi_issuer_tokens(&tokens, None, &token_types, None)
            .await;
        assert!(result.is_ok());

        let validated_tokens = result.unwrap().tokens;
        assert_eq!(validated_tokens.len(), 2);

        // Verify the tokens have the correct mapping
        assert!(validated_tokens.contains_key("Jans::Access_Token"));
        assert!(validated_tokens.contains_key("Jans::Id_Token"));
    }

    /// Two issuers sharing one `iss` and its keys both validate their own tokens,
    /// each token keeping the issuer its mapping names.
    #[test]
    async fn issuers_sharing_one_iss_keep_their_own_tokens() {
        let mut server = MockServer::new_with_defaults().await.unwrap();
        let issuer = |name: &str, entity_type: &str| {
            let mut issuer = server.trusted_issuer();
            issuer.name = IssuerName::for_test(name);
            issuer.with_tokens(HashMap::from([(
                entity_type.to_string(),
                TokenEntityMetadata::builder().build(),
            )]))
        };
        let token_types = TokenTypes::build(
            [
                issuer("Acme", "Acme::Access_token"),
                issuer("Beta", "Beta::Access_token"),
            ],
            [],
        )
        .expect("test token types should build");
        let svc = JwtService::new(
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: true,
                jwt_status_validation: false,
                signature_algorithms_supported: HashSet::from_iter([Algorithm::HS256]),
                ..Default::default()
            },
            &token_types,
            None,
            Arc::new(MetricsCollector::new(MetricsMode::Local)),
            HTTP_CLIENT.clone(),
        )
        .await
        .expect("issuers sharing an iss should load");

        let mut token = |jti: &str| {
            server
                .generate_token_with_hs256sig(
                    &mut json!({ "sub": "user", "jti": jti, "exp": u64::MAX }),
                    None,
                )
                .expect("should sign token")
        };
        let tokens = vec![
            TokenInput::new("Acme::Access_token".to_string(), token("a")),
            TokenInput::new("Beta::Access_token".to_string(), token("b")),
        ];

        let out = svc
            .validate_multi_issuer_tokens(&tokens, None, &token_types, None)
            .await
            .expect("both tokens should validate");
        for (mapping, name) in [
            ("Acme::Access_token", "Acme"),
            ("Beta::Access_token", "Beta"),
        ] {
            let iss = &out.tokens[mapping].token.iss;
            assert!(
                matches!(iss, Some(TokenIssuer::Jwt(issuer)) if issuer.name.to_string() == name),
                "{mapping} should keep the issuer its mapping names, got {iss:?}"
            );
        }
        assert!(
            out.dropped.is_empty(),
            "no token should be dropped: {:?}",
            out.dropped
        );
    }

    /// A static issuer validates tokens carrying its configured `iss`, without a
    /// discovery request, and drops one signed by its keys under another `iss`.
    #[test]
    async fn static_issuer_validates_only_its_configured_iss() {
        let mut server = mockito::Server::new_async().await;
        let keys = generate_keypair_hs256(Some("static_key")).expect("should generate keys");
        let jwks = server
            .mock("GET", "/keys")
            .with_status(200)
            .with_header("content-type", "application/json")
            .with_body(
                json!({ "keys": generate_jwks(std::slice::from_ref(&keys)).keys }).to_string(),
            )
            .expect(1)
            .create();
        let discovery = server
            .mock("GET", "/.well-known/openid-configuration")
            .expect(0)
            .create();
        let iss = server.url();
        let issuer = TrustedIssuer::new(
            IssuerName::for_test("Partner"),
            Url::parse(&iss).expect("valid url"),
            KeySource::Static {
                jwks: Url::parse(&format!("{iss}/keys")).expect("valid url"),
            },
        )
        .with_tokens(HashMap::from([(
            "Partner::Access_token".to_string(),
            TokenEntityMetadata::builder().build(),
        )]));
        let token_types = TokenTypes::build([issuer], []).expect("test token types should build");
        let svc = JwtService::new(
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: true,
                jwt_status_validation: false,
                signature_algorithms_supported: HashSet::from_iter([Algorithm::HS256]),
                ..Default::default()
            },
            &token_types,
            None,
            Arc::new(MetricsCollector::new(MetricsMode::Local)),
            HTTP_CLIENT.clone(),
        )
        .await
        .expect("a static issuer should load");

        let token = |iss: &str| {
            generate_token_using_claims(
                &json!({ "iss": iss, "sub": "user", "jti": "1", "exp": u64::MAX }),
                &keys,
            )
            .expect("should sign token")
        };
        let tokens = vec![
            TokenInput::new("Partner::Access_token".to_string(), token(&iss)),
            TokenInput::new(
                "Partner::Access_token".to_string(),
                token("https://other.example"),
            ),
        ];

        let out = svc
            .validate_multi_issuer_tokens(&tokens, None, &token_types, None)
            .await
            .expect("the token with the configured iss should validate");
        assert_eq!(
            out.tokens.get("Partner::Access_token").map(|t| t.index),
            Some(0),
            "the token carrying the configured iss should be kept"
        );
        assert!(
            matches!(
                &out.dropped[..],
                [DroppedToken {
                    index: 1,
                    reason: DropReason::JwtValidationFailed,
                    ..
                }]
            ),
            "the token with another iss should be dropped, got {:?}",
            out.dropped
        );
        jwks.assert();
        discovery.assert();
    }

    /// A static issuer has no status list, so with status validation on its tokens
    /// that carry `status` are dropped, and the load warning says so by name.
    #[test]
    async fn static_issuer_with_status_validation_drops_tokens_with_status() {
        let mut server = mockito::Server::new_async().await;
        let keys = generate_keypair_hs256(Some("static_key")).expect("should generate keys");
        server
            .mock("GET", "/keys")
            .with_status(200)
            .with_header("content-type", "application/json")
            .with_body(
                json!({ "keys": generate_jwks(std::slice::from_ref(&keys)).keys }).to_string(),
            )
            .create();
        let iss = server.url();
        let issuer = TrustedIssuer::new(
            IssuerName::for_test("Partner"),
            Url::parse(&iss).expect("valid url"),
            KeySource::Static {
                jwks: Url::parse(&format!("{iss}/keys")).expect("valid url"),
            },
        )
        .with_tokens(HashMap::from([(
            "Partner::Access_token".to_string(),
            TokenEntityMetadata::builder().build(),
        )]));
        let logger: Logger = Arc::new(LogStrategy::new(
            &LogConfig {
                log_level: LogLevel::WARN,
                log_type: LogTypeConfig::Memory(MemoryLogConfig {
                    log_ttl: 60,
                    max_items: None,
                    max_item_size: None,
                }),
            },
            PdpID::new(),
            None,
        ));
        let token_types = TokenTypes::build([issuer], []).expect("test token types should build");
        let svc = JwtService::new(
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: true,
                jwt_status_validation: true,
                signature_algorithms_supported: HashSet::from_iter([Algorithm::HS256]),
                ..Default::default()
            },
            &token_types,
            Some(logger.clone()),
            Arc::new(MetricsCollector::new(MetricsMode::Local)),
            HTTP_CLIENT.clone(),
        )
        .await
        .expect("a static issuer should load with status validation on");

        let warning = "trusted issuer 'Partner': its static JWKS comes with no status list; \
                       with status validation on, its tokens that carry a `status` claim will \
                       be rejected";
        assert!(
            logger
                .pop_logs()
                .iter()
                .any(|log| log.get("msg").and_then(|m| m.as_str()) == Some(warning)),
            "the load should warn about the static issuer by name"
        );

        let token = |claims: serde_json::Value| {
            generate_token_using_claims(&claims, &keys).expect("should sign token")
        };
        let claims = json!({ "iss": iss, "sub": "user", "jti": "1", "exp": u64::MAX });
        let mut with_status = claims.clone();
        with_status["status"] =
            json!({ "status_list": { "idx": 0, "uri": format!("{iss}/status") } });
        let tokens = vec![
            TokenInput::new("Partner::Access_token".to_string(), token(with_status)),
            TokenInput::new("Partner::Access_token".to_string(), token(claims)),
        ];

        let out = svc
            .validate_multi_issuer_tokens(&tokens, None, &token_types, None)
            .await
            .expect("the token without `status` should validate");
        assert_eq!(
            out.tokens.get("Partner::Access_token").map(|t| t.index),
            Some(1),
            "the token without `status` should be kept"
        );
        assert!(
            matches!(
                &out.dropped[..],
                [DroppedToken {
                    index: 0,
                    reason: DropReason::JwtValidationFailed,
                    ..
                }]
            ),
            "the token with `status` should be dropped, got {:?}",
            out.dropped
        );
    }

    #[test]
    async fn test_validate_multi_issuer_tokens_empty_array() {
        let server = MockServer::new_with_defaults().await.unwrap();
        let iss = server
            .trusted_issuer()
            .with_tokens(TestJwtIssuer::default().tokens);

        let token_types = TokenTypes::build([iss], []).expect("test token types should build");

        let jwt_service = JwtService::new(
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: true,
                jwt_status_validation: false,
                signature_algorithms_supported: HashSet::from_iter([Algorithm::HS256]),
                ..Default::default()
            },
            &token_types,
            None,
            Arc::new(MetricsCollector::new(MetricsMode::Local)),
            HTTP_CLIENT.clone(),
        )
        .await
        .expect("Should create JwtService");

        let result = jwt_service
            .validate_multi_issuer_tokens(&[], None, &token_types, None)
            .await;
        assert!(matches!(
            result,
            Err(MultiIssuerValidationError::EmptyTokenArray)
        ));
    }

    #[test]
    async fn test_validate_multi_issuer_tokens_invalid_token_format() {
        let server = MockServer::new_with_defaults().await.unwrap();
        let iss = server
            .trusted_issuer()
            .with_tokens(TestJwtIssuer::default().tokens);

        let token_types = TokenTypes::build([iss], []).expect("test token types should build");

        let jwt_service = JwtService::new(
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: true,
                jwt_status_validation: false,
                signature_algorithms_supported: HashSet::from_iter([Algorithm::HS256]),
                ..Default::default()
            },
            &token_types,
            None,
            Arc::new(MetricsCollector::new(MetricsMode::Local)),
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
            .validate_multi_issuer_tokens(&tokens, None, &token_types, None)
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

        let mut iss = server.trusted_issuer().with_tokens(HashMap::new());
        // Add token metadata for multi-issuer validation
        iss.tokens.insert(
            "Jans::Access_Token".to_string(),
            TokenEntityMetadata {
                token_id: "jti".to_string(),

                required_claims: HashSet::new(),
            },
        );
        iss.tokens.insert(
            "Jans::Id_Token".to_string(),
            TokenEntityMetadata {
                token_id: "jti".to_string(),

                required_claims: HashSet::new(),
            },
        );

        let token_types = TokenTypes::build([iss], []).expect("test token types should build");

        let jwt_service = JwtService::new(
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: true,
                jwt_status_validation: false,
                signature_algorithms_supported: HashSet::from_iter([Algorithm::HS256]),
                ..Default::default()
            },
            &token_types,
            None,
            Arc::new(MetricsCollector::new(MetricsMode::Local)),
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
            .validate_multi_issuer_tokens(&tokens, None, &token_types, None)
            .await;
        assert!(result.is_ok());

        let validated_tokens = result.unwrap().tokens;
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

        let mut iss = server.trusted_issuer().with_tokens(HashMap::new());
        // Add token metadata for multi-issuer validation
        iss.tokens.insert(
            "Jans::Access_Token".to_string(),
            TokenEntityMetadata {
                token_id: "jti".to_string(),

                required_claims: HashSet::new(),
            },
        );

        let token_types = TokenTypes::build([iss], []).expect("test token types should build");

        let jwt_service = JwtService::new(
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: true,
                jwt_status_validation: false,
                signature_algorithms_supported: HashSet::from_iter([Algorithm::HS256]),
                ..Default::default()
            },
            &token_types,
            None,
            Arc::new(MetricsCollector::new(MetricsMode::Local)),
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
            .validate_multi_issuer_tokens(&tokens, None, &token_types, None)
            .await;
        assert!(result.is_ok());

        let validated_tokens = result.unwrap().tokens;
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

        let iss = server
            .trusted_issuer()
            .with_tokens(TestJwtIssuer::default().tokens);

        let token_types = TokenTypes::build([iss], []).expect("test token types should build");

        let jwt_service = JwtService::new(
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: true,
                jwt_status_validation: false,
                signature_algorithms_supported: HashSet::from_iter([Algorithm::HS256]),
                ..Default::default()
            },
            &token_types,
            None,
            Arc::new(MetricsCollector::new(MetricsMode::Local)),
            HTTP_CLIENT.clone(),
        )
        .await
        .expect("Should create JwtService");

        let tokens = vec![TokenInput::new("Jans::Access_Token".to_string(), token)];

        let result = jwt_service
            .validate_multi_issuer_tokens(&tokens, None, &token_types, None)
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
        let mut iss = server.trusted_issuer().with_tokens(HashMap::new());
        iss.tokens.insert(
            mapping.clone(),
            TokenEntityMetadata {
                token_id: "jti".to_string(),
                required_claims: HashSet::new(),
            },
        );

        let token_types = TokenTypes::build([iss], []).expect("test token types should build");

        let jwt_service = JwtService::new(
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: true,
                jwt_status_validation: false,
                signature_algorithms_supported: HashSet::from_iter([Algorithm::HS256]),
                jwks_refresh_min_interval: 0,
                ..Default::default()
            },
            &token_types,
            None,
            Arc::new(MetricsCollector::new(MetricsMode::Local)),
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
                &token_types,
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
                &token_types,
                None,
            )
            .await
            .expect_err("First call after rotation should fail");

        tokio::time::sleep(std::time::Duration::from_millis(200)).await;

        jwt_service
            .validate_multi_issuer_tokens(
                &[TokenInput::new(mapping, token_after_rotation)],
                None,
                &token_types,
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
        let mut iss = server.trusted_issuer().with_tokens(HashMap::new());
        iss.tokens.insert(
            mapping.clone(),
            TokenEntityMetadata {
                token_id: "jti".to_string(),
                required_claims: HashSet::new(),
            },
        );

        let token_types = TokenTypes::build([iss], []).expect("test token types should build");

        let jwt_service = JwtService::new(
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: true,
                jwt_status_validation: false,
                signature_algorithms_supported: HashSet::from_iter([Algorithm::HS256]),
                jwks_refresh_min_interval: 0,
                ..Default::default()
            },
            &token_types,
            None,
            Arc::new(MetricsCollector::new(MetricsMode::Local)),
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
                &token_types,
                None,
            )
            .await
            .expect_err("First call with rotated kid should fail (stale key)");

        tokio::time::sleep(std::time::Duration::from_millis(200)).await;

        jwt_service
            .validate_multi_issuer_tokens(
                &[TokenInput::new(mapping, token_after_rotation)],
                None,
                &token_types,
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
        let iss = server
            .trusted_issuer()
            .with_tokens(TestJwtIssuer::default().tokens);

        let token_types = TokenTypes::build([iss], []).expect("test token types should build");

        let jwt_service = JwtService::new(
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: true,
                jwt_status_validation: false,
                signature_algorithms_supported: HashSet::from_iter([Algorithm::HS256]),
                ..Default::default()
            },
            &token_types,
            None,
            Arc::new(MetricsCollector::new(MetricsMode::Local)),
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
