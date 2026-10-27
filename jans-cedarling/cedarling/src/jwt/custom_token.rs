// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Custom token processing extension point.
//!
//! JWT validation is hardcoded in [`JwtService`](crate::jwt). This module adds a
//! pluggable interface so Rust consumers can authorize on **non-JWT** tokens,
//! opaque tokens, API keys, vendor-specific formats, or tokens whose validation
//! uses crypto operations Cedarling does not implement (e.g. attenuation /
//! chain-of-custody proofs).
//!
//! A consumer implements [`CustomTokenProcessor`] and registers it on a live
//! instance via [`Cedarling::set_custom_token_processor`](crate::Cedarling::set_custom_token_processor).
//! The processor turns a raw payload into [`ProcessedTokenClaims`]; those claims
//! then flow through the *existing* entity-builder and context machinery, so a
//! custom token becomes a Cedar entity under `context.tokens.*` exactly like a JWT.

use async_trait::async_trait;
use serde_json::Value;
use std::collections::HashMap;
use std::time::Duration;

/// A consumer-supplied processor that validates a raw token payload and maps it
/// to claims.
///
/// One processor handles all custom tokens and dispatches internally on
/// `mapping`. Its output is authoritative: Cedarling performs no signature or
/// issuer verification of its own on the payload, so the processor is fully
/// trusted.
#[cfg_attr(not(any(target_arch = "wasm32", target_arch = "wasm64")), async_trait)]
#[cfg_attr(any(target_arch = "wasm32", target_arch = "wasm64"), async_trait(?Send))]
pub trait CustomTokenProcessor: Send + Sync {
    /// Validate `payload` and map it to claims.
    ///
    /// - `mapping`: the entity type mapping from the request's `TokenInput`
    ///   (e.g. `"Acme::CustomToken"`); use it to dispatch between token formats.
    /// - `payload`: the raw token string, opaque to Cedarling.
    ///
    /// Returns [`ProcessedTokenClaims`] on success. On error only this token is
    /// dropped and the request continues with the others.
    ///
    /// The framework may race this future against a configured timeout deadline.
    /// Cancellation still requires the future to yield, so a `process` that blocks
    /// without an await point runs to completion regardless: treat the timeout as a
    /// backstop, not as a guarantee that work stops.
    async fn process(
        &self,
        mapping: &str,
        payload: &str,
    ) -> Result<ProcessedTokenClaims, CustomTokenError>;
}

/// The result of [`CustomTokenProcessor::process`].
#[derive(Debug, Clone)]
pub struct ProcessedTokenClaims {
    /// Claims extracted from the token, used to build the Cedar token entity.
    pub claims: HashMap<String, Value>,
    /// The entity id for the resulting token entity.
    pub token_id: String,
    /// Optional expiration (unix seconds). When set, the token is rejected once
    /// that time has passed and the value bounds the token-cache TTL, without the
    /// processor having to put an `exp` into [`claims`](Self::claims). Falls back to
    /// an `exp` claim when `None`; an explicit value wins over the claim.
    ///
    /// Beyond rejection and cache TTL, this also populates the token entity's `exp`
    /// attribute — so it satisfies a schema that declares `exp` required and is
    /// readable by a policy as `context.tokens.*.exp`, without a separate `exp` claim.
    pub expiration: Option<i64>,
    /// Whether this validation result may be cached. [`new`](Self::new) sets it to
    /// `true`, so revocation-sensitive tokens must opt out explicitly via
    /// [`with_cacheable(false)`](Self::with_cacheable) to make every request
    /// re-run `process`.
    pub cacheable: bool,
}

impl ProcessedTokenClaims {
    /// Build a cacheable result with no expiration.
    pub fn new(claims: HashMap<String, Value>, token_id: impl Into<String>) -> Self {
        Self {
            claims,
            token_id: token_id.into(),
            expiration: None,
            cacheable: true,
        }
    }

    /// Set whether this result may be cached. See [`cacheable`](Self::cacheable).
    #[must_use]
    pub fn with_cacheable(mut self, cacheable: bool) -> Self {
        self.cacheable = cacheable;
        self
    }

    /// Set the expiration (unix seconds). See [`expiration`](Self::expiration).
    #[must_use]
    pub fn with_expiration(mut self, exp: i64) -> Self {
        self.expiration = Some(exp);
        self
    }
}

/// Errors returned by a [`CustomTokenProcessor`], plus the framework-side timeout
/// variant.
#[derive(Debug, thiserror::Error)]
pub enum CustomTokenError {
    /// The processor rejected the token or failed to validate it.
    #[error("custom token processing failed: {0}")]
    Processing(String),

    /// `process` did not complete within the configured timeout. Kept distinct
    /// from [`Processing`](Self::Processing) so telemetry can separate "slow" from
    /// "rejected".
    #[error("custom token processing timed out after {0:?}")]
    Timeout(Duration),

    /// No custom issuer declares the requested token type. Cedarling itself no
    /// longer returns this: an undeclared mapping is dropped before any processor
    /// runs. A processor may still return it; kept so existing processors build.
    #[error("no custom issuer declares token type '{0}'")]
    UnknownMapping(String),

    /// The processor reported an expiration that has already passed, via
    /// [`ProcessedTokenClaims::expiration`] or an `exp` claim. Cedarling does not
    /// validate a custom payload, but it does honor an expiration the processor
    /// chose to report, matching what the token cache already enforces.
    #[error("custom token for '{mapping}' expired at {exp}")]
    Expired {
        /// The requested Cedar entity type name.
        mapping: String,
        /// The expiration (unix seconds) that had already passed.
        exp: i64,
    },

    /// The processor returned an empty `token_id`, which would produce a degenerate
    /// `EntityType::""` Cedar UID shared by every token from that processor.
    #[error("custom token for '{0}' has an empty token_id")]
    EmptyTokenId(String),

    /// A required claim was absent from the processed output.
    #[error("custom token missing required claim: {0}")]
    MissingRequiredClaim(String),

    /// A token routed to a custom issuer but no [`CustomTokenProcessor`] is registered.
    #[error("no CustomTokenProcessor registered for custom mapping '{0}'")]
    NoProcessorRegistered(String),
}
