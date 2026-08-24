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

use crate::common::policy_store::{CustomIssuerMetadata, CustomTokenMetadata};
use crate::entity_builder::{is_valid_issuer_id, sanitize_issuer_name};
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
    /// Returns [`ProcessedTokenClaims`] on success. On error the token is dropped
    /// (skip-and-continue) unless its custom issuer is marked `required`, in which
    /// case the whole authorization request fails.
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
    /// Identifies which configured custom issuer this token belongs to. Resolved
    /// against the custom-issuer index after processing. `None` falls back to the
    /// issuer declared by the `mapping`'s custom metadata.
    ///
    /// TODO(#14747): `entity_type_name` is currently assumed unique across custom
    /// issuers, which makes `mapping` sufficient to resolve the issuer and this hint
    /// redundant. Keep it only if #14747 needs a runtime discriminator; otherwise
    /// remove it.
    pub issuer_id: Option<String>,
    /// Optional expiration (unix seconds). When set, the token is rejected once
    /// that time has passed and the value bounds the token-cache TTL, without the
    /// processor having to put an `exp` into [`claims`](Self::claims). Falls back to
    /// an `exp` claim when `None`; an explicit value wins over the claim.
    ///
    /// Beyond rejection and cache TTL, this also populates the token entity's `exp`
    /// attribute — so it satisfies a schema that declares `exp` required and is
    /// readable by a policy as `context.tokens.*.exp`, without a separate `exp` claim.
    pub expiration: Option<i64>,
    /// Whether this validation result may be cached. Set to `false` for
    /// revocation-sensitive tokens so every request re-runs `process`.
    pub cacheable: bool,
}

impl ProcessedTokenClaims {
    /// Build a cacheable result with no issuer hint or expiration.
    pub fn new(claims: HashMap<String, Value>, token_id: impl Into<String>) -> Self {
        Self {
            claims,
            token_id: token_id.into(),
            issuer_id: None,
            expiration: None,
            cacheable: true,
        }
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

    /// The processor returned an `issuer_id` (or the mapping resolved to one) that
    /// is not registered as a custom issuer in the policy store.
    #[error("custom issuer '{0}' not found among registered custom issuers")]
    UnknownIssuer(String),

    /// The resolved custom issuer does not declare the requested token type. Only
    /// reachable when a processor returns an `issuer_id` inconsistent with the
    /// request's `mapping`.
    #[error("custom issuer '{issuer}' does not declare token type '{mapping}'")]
    UnknownTokenType {
        /// Sanitized id of the resolved issuer.
        issuer: String,
        /// The requested Cedar entity type name.
        mapping: String,
    },

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

/// A configured custom issuer resolved into a ready-to-use form, keyed by its
/// sanitized issuer id.
#[derive(Debug, Clone)]
pub(crate) struct ResolvedCustomIssuer {
    /// Sanitized issuer id (used for the `context.tokens.{issuer}_{type}` key).
    pub issuer_id: String,
    /// Token types this issuer emits, keyed by Cedar entity type name (the request
    /// `mapping`).
    pub tokens_mappings: HashMap<String, CustomTokenMetadata>,
}

/// Index of the policy store's configured custom issuers. Built once per
/// [`Authz`](crate::Authz) build (fresh on every policy-store swap, so it is
/// never stale even when the `JwtService` is reused).
#[derive(Debug, Default)]
pub(crate) struct CustomIssuerIndex {
    /// sanitized issuer id -> resolved issuer
    by_id: HashMap<String, ResolvedCustomIssuer>,
    /// `entity_type_name` (request mapping) -> sanitized issuer ids declaring it
    ///
    /// TODO(#14747): `entity_type_name` is treated as globally unique across custom
    /// issuers, so this is effectively a 1:1 map. The `Vec` only exists to carry the
    /// not-yet-supported "several issuers declare the same mapping" case; collapse it
    /// to a plain `String` once #14747 settles how such issuers are distinguished.
    by_mapping: HashMap<String, Vec<String>>,
}

/// Errors from building a [`CustomIssuerIndex`] out of policy-store config.
#[derive(Debug, thiserror::Error, PartialEq, Eq)]
pub(crate) enum CustomIssuerIndexError {
    /// Two issuer names collapse to the same id once sanitized. Their tokens would
    /// share a `context.tokens.{issuer}_{type}` namespace, so this is rejected at
    /// build time rather than silently dropping one of them.
    #[error("custom issuer names collide after sanitization: '{0}'")]
    DuplicateIssuerId(String),

    /// An issuer name sanitizes to the empty string. Applies to both config paths;
    /// the directory parser derives the id from the filename, but the embedded/legacy
    /// map is keyed directly and can carry `""`.
    #[error("custom issuer name is empty after sanitization")]
    EmptyIssuerId,

    /// A sanitized issuer id contains characters that are not a valid Cedar
    /// `context.tokens` field-name component (`^[a-z_][a-z0-9_]*$`).
    #[error(
        "custom issuer id '{0}' is not a valid context.tokens key component (expected ^[a-z_][a-z0-9_]*$)"
    )]
    InvalidIssuerId(String),

    /// An issuer declares no token types, or a token type with an empty name.
    #[error("custom issuer '{0}' has an invalid (empty or unnamed) token type")]
    InvalidTokenType(String),

    /// A Cedar entity type name is declared by more than one custom issuer.
    #[error("Cedar entity type '{0}' is declared by more than one custom issuer")]
    DuplicateMapping(String),
}

impl CustomIssuerIndex {
    /// Build the index from policy-store `custom_issuers`, sanitizing issuer names.
    pub(crate) fn build(
        custom_issuers: &HashMap<String, CustomIssuerMetadata>,
    ) -> Result<Self, CustomIssuerIndexError> {
        let mut by_id = HashMap::with_capacity(custom_issuers.len());
        let mut by_mapping: HashMap<String, Vec<String>> = HashMap::new();

        for (name, meta) in custom_issuers {
            let issuer_id = sanitize_issuer_name(name);
            if issuer_id.is_empty() {
                return Err(CustomIssuerIndexError::EmptyIssuerId);
            }
            if !is_valid_issuer_id(&issuer_id) {
                return Err(CustomIssuerIndexError::InvalidIssuerId(issuer_id));
            }
            if by_id.contains_key(&issuer_id) {
                return Err(CustomIssuerIndexError::DuplicateIssuerId(issuer_id));
            }
            if meta.tokens_mappings.is_empty() {
                return Err(CustomIssuerIndexError::InvalidTokenType(issuer_id));
            }
            for mapping in meta.tokens_mappings.keys() {
                if mapping.is_empty() {
                    return Err(CustomIssuerIndexError::InvalidTokenType(issuer_id));
                }
                let declarers = by_mapping.entry(mapping.clone()).or_default();
                declarers.push(issuer_id.clone());
                if declarers.len() > 1 {
                    return Err(CustomIssuerIndexError::DuplicateMapping(mapping.clone()));
                }
            }
            by_id.insert(
                issuer_id.clone(),
                ResolvedCustomIssuer {
                    issuer_id,
                    tokens_mappings: meta.tokens_mappings.clone(),
                },
            );
        }

        Ok(Self { by_id, by_mapping })
    }

    /// Whether any custom issuers are configured.
    pub(crate) fn is_empty(&self) -> bool {
        self.by_id.is_empty()
    }

    /// Sanitized issuer ids, for the init-time namespace collision check against
    /// JWT trusted-issuer names.
    pub(crate) fn sanitized_ids(&self) -> impl Iterator<Item = &str> {
        self.by_id.keys().map(String::as_str)
    }

    /// Cedar entity type names (request mappings) declared by custom issuers, for
    /// the init-time collision check against JWT trusted-issuer token types.
    pub(crate) fn mappings(&self) -> impl Iterator<Item = &str> {
        self.by_mapping.keys().map(String::as_str)
    }

    /// Whether a request `mapping` routes to a custom issuer.
    pub(crate) fn mapping_is_custom(&self, mapping: &str) -> bool {
        self.by_mapping.contains_key(mapping)
    }

    /// Whether any custom issuer declaring this `mapping` is `required`. Used to
    /// decide fail-closed vs skip-and-continue on a processing error.
    ///
    /// The issuer is not yet known at call time (processing has not run, or has just
    /// failed), so this is deliberately an OR across every issuer declaring the
    /// mapping: fail-closed wins. Under the unique-`entity_type_name` assumption
    /// there is only ever one such issuer anyway.
    pub(crate) fn mapping_required(&self, mapping: &str) -> bool {
        self.by_mapping
            .get(mapping)
            .into_iter()
            .flatten()
            .filter_map(|id| self.by_id.get(id))
            .filter_map(|issuer| issuer.tokens_mappings.get(mapping))
            .any(|token| token.required)
    }

    /// Resolve the custom issuer and the token metadata for a processed token.
    /// Prefers an explicit `issuer_id` from the processor; otherwise falls back to
    /// the sole issuer declaring `mapping`. Errors if unknown or ambiguous.
    ///
    /// TODO(#14747): with `entity_type_name` assumed unique, `mapping` alone always
    /// resolves the issuer and the `issuer_id` hint is redundant — drop the parameter
    /// (and `ProcessedTokenClaims::issuer_id`) unless #14747 keeps it as the runtime
    /// discriminator.
    pub(crate) fn resolve(
        &self,
        mapping: &str,
        issuer_id: Option<&str>,
    ) -> Result<(&ResolvedCustomIssuer, &CustomTokenMetadata), CustomTokenError> {
        let issuer = if let Some(id) = issuer_id {
            let sanitized = sanitize_issuer_name(id);
            self.by_id
                .get(&sanitized)
                .ok_or_else(|| CustomTokenError::UnknownIssuer(id.to_string()))?
        } else {
            let ids = self
                .by_mapping
                .get(mapping)
                .ok_or_else(|| CustomTokenError::UnknownIssuer(mapping.to_string()))?;
            match ids.as_slice() {
                [only] => self
                    .by_id
                    .get(only)
                    .ok_or_else(|| CustomTokenError::UnknownIssuer(mapping.to_string()))?,
                _ => {
                    return Err(CustomTokenError::Processing(format!(
                        "ambiguous mapping '{mapping}': multiple custom issuers declare it; \
                         the processor must return an issuer_id"
                    )));
                },
            }
        };

        // Also guards the explicit-`issuer_id` branch: a processor may not name an
        // issuer that does not declare the requested type.
        let token = issuer.tokens_mappings.get(mapping).ok_or_else(|| {
            CustomTokenError::UnknownTokenType {
                issuer: issuer.issuer_id.clone(),
                mapping: mapping.to_string(),
            }
        })?;

        Ok((issuer, token))
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::collections::HashSet;

    /// An issuer declaring a single token type.
    fn meta(entity_type_name: &str, required: bool) -> CustomIssuerMetadata {
        multi_meta(&[(entity_type_name, required)])
    }

    /// An issuer declaring several token types.
    fn multi_meta(tokens: &[(&str, bool)]) -> CustomIssuerMetadata {
        CustomIssuerMetadata {
            tokens_mappings: tokens
                .iter()
                .map(|(name, required)| {
                    (
                        (*name).to_string(),
                        CustomTokenMetadata {
                            required: *required,
                            required_claims: HashSet::new(),
                        },
                    )
                })
                .collect(),
        }
    }

    #[test]
    fn build_sanitizes_issuer_ids_and_routes_by_mapping() {
        let mut issuers = HashMap::new();
        issuers.insert("Ac-me Corp".to_string(), meta("Acme::CustomToken", false));
        let index = CustomIssuerIndex::build(&issuers).expect("index should build");

        assert!(
            !index.is_empty(),
            "an index built from one configured issuer should not be empty"
        );
        assert!(
            index.mapping_is_custom("Acme::CustomToken"),
            "the custom issuer's entity type mapping should route to the custom path"
        );
        assert!(
            !index.mapping_is_custom("Jans::Access_token"),
            "an unrelated JWT mapping should not be treated as custom"
        );
        let ids: Vec<&str> = index.sanitized_ids().collect();
        assert_eq!(
            ids,
            ["ac_me_corp"],
            "issuer name 'Ac-me Corp' should sanitize to the single id 'ac_me_corp'"
        );
    }

    #[test]
    fn build_routes_every_token_type_of_one_issuer() {
        let mut issuers = HashMap::new();
        issuers.insert(
            "Acme".to_string(),
            multi_meta(&[("Acme::DolphinToken", true), ("Acme::WhaleToken", false)]),
        );
        let index = CustomIssuerIndex::build(&issuers).expect("index should build");

        assert!(
            index.mapping_is_custom("Acme::DolphinToken")
                && index.mapping_is_custom("Acme::WhaleToken"),
            "both token types declared by a single issuer should route to the custom path"
        );
        let ids: Vec<&str> = index.sanitized_ids().collect();
        assert_eq!(
            ids,
            ["acme"],
            "several token types should share the one issuer id, not fan out into several issuers"
        );
    }

    #[test]
    fn build_rejects_sanitized_duplicate_ids() {
        let mut issuers = HashMap::new();
        issuers.insert("Ac-Me".to_string(), meta("First::T", false));
        issuers.insert("ac me".to_string(), meta("Second::T", true));

        let err = CustomIssuerIndex::build(&issuers)
            .expect_err("names colliding after sanitization should be rejected at build time");
        assert_eq!(
            err,
            CustomIssuerIndexError::DuplicateIssuerId("ac_me".to_string()),
            "the error should name the sanitized id both issuers collapse to"
        );
    }

    #[test]
    fn mapping_required_is_per_token_not_per_issuer() {
        let mut issuers = HashMap::new();
        issuers.insert(
            "acme".to_string(),
            multi_meta(&[("Acme::DolphinToken", true), ("Acme::WhaleToken", false)]),
        );
        let index = CustomIssuerIndex::build(&issuers).expect("index should build");

        assert!(
            index.mapping_required("Acme::DolphinToken"),
            "a token declared required should be fail-closed"
        );
        assert!(
            !index.mapping_required("Acme::WhaleToken"),
            "a sibling token of the same issuer should keep its own required=false"
        );
        assert!(
            !index.mapping_required("other"),
            "an unknown mapping should not be fail-closed"
        );
    }

    #[test]
    fn build_rejects_duplicate_mapping() {
        let mut issuers = HashMap::new();
        issuers.insert("a".to_string(), meta("M::T", false));
        issuers.insert("b".to_string(), meta("M::T", true));
        let err = CustomIssuerIndex::build(&issuers)
            .expect_err("two issuers declaring the same Cedar type must be rejected");
        assert!(
            matches!(err, CustomIssuerIndexError::DuplicateMapping(ref m) if m == "M::T"),
            "expected DuplicateMapping(M::T), got {err:?}"
        );
    }

    #[test]
    fn resolve_prefers_explicit_issuer_id() {
        let mut issuers = HashMap::new();
        issuers.insert("beta".to_string(), meta("M::T", false));
        let index = CustomIssuerIndex::build(&issuers).expect("index should build");

        let (issuer, _) = index
            .resolve("M::T", Some("beta"))
            .expect("an explicit issuer_id should resolve to that issuer");
        assert_eq!(issuer.issuer_id, "beta");

        let err = index
            .resolve("M::T", Some("nope"))
            .expect_err("an unknown issuer_id should fail to resolve");
        assert!(
            matches!(err, CustomTokenError::UnknownIssuer(ref id) if id == "nope"),
            "an unknown issuer_id should surface as UnknownIssuer(nope), got {err:?}"
        );
    }

    #[test]
    fn build_rejects_invalid_issuer_id() {
        let mut issuers = HashMap::new();
        issuers.insert("acme+corp".to_string(), meta("M::T", false));
        let err = CustomIssuerIndex::build(&issuers)
            .expect_err("a non-`[a-z_][a-z0-9_]*` issuer id must be rejected");
        assert!(
            matches!(err, CustomIssuerIndexError::InvalidIssuerId(ref id) if id == "acme+corp"),
            "expected InvalidIssuerId(acme+corp), got {err:?}"
        );
    }

    #[test]
    fn build_rejects_empty_issuer_id() {
        let mut issuers = HashMap::new();
        issuers.insert(String::new(), meta("M::T", false));
        let err = CustomIssuerIndex::build(&issuers)
            .expect_err("an issuer name that sanitizes to empty must be rejected");
        assert!(
            matches!(err, CustomIssuerIndexError::EmptyIssuerId),
            "expected EmptyIssuerId, got {err:?}"
        );
    }

    #[test]
    fn build_rejects_empty_tokens_mappings() {
        let mut issuers = HashMap::new();
        issuers.insert(
            "acme".to_string(),
            CustomIssuerMetadata {
                tokens_mappings: HashMap::new(),
            },
        );
        let err = CustomIssuerIndex::build(&issuers)
            .expect_err("an issuer declaring no token types must be rejected");
        assert!(
            matches!(err, CustomIssuerIndexError::InvalidTokenType(ref id) if id == "acme"),
            "expected InvalidTokenType(acme), got {err:?}"
        );
    }

    #[test]
    fn build_rejects_empty_mapping_key() {
        let mut mappings = HashMap::new();
        mappings.insert(String::new(), CustomTokenMetadata::default());
        let mut issuers = HashMap::new();
        issuers.insert(
            "acme".to_string(),
            CustomIssuerMetadata {
                tokens_mappings: mappings,
            },
        );
        let err = CustomIssuerIndex::build(&issuers)
            .expect_err("an empty token type name must be rejected");
        assert!(
            matches!(err, CustomIssuerIndexError::InvalidTokenType(ref id) if id == "acme"),
            "expected InvalidTokenType(acme), got {err:?}"
        );
    }

    #[test]
    fn resolve_rejects_issuer_id_not_declaring_the_mapping() {
        let mut issuers = HashMap::new();
        issuers.insert("acme".to_string(), meta("Acme::CustomToken", false));
        issuers.insert("beta".to_string(), meta("Beta::CustomToken", false));
        let index = CustomIssuerIndex::build(&issuers).expect("index should build");

        let err = index
            .resolve("Acme::CustomToken", Some("beta"))
            .expect_err("an issuer that does not declare the requested type should be rejected");
        assert!(
            matches!(
                err,
                CustomTokenError::UnknownTokenType { ref issuer, ref mapping }
                    if issuer == "beta" && mapping == "Acme::CustomToken"
            ),
            "expected UnknownTokenType {{ issuer: beta, mapping: Acme::CustomToken }}, got {err:?}"
        );
    }

    #[test]
    fn resolve_uses_sole_declarer_when_unambiguous() {
        let mut issuers = HashMap::new();
        issuers.insert(
            "Acme".to_string(),
            multi_meta(&[("Acme::DolphinToken", false), ("Acme::WhaleToken", true)]),
        );
        let index = CustomIssuerIndex::build(&issuers).expect("index should build");

        let (issuer, token) = index
            .resolve("Acme::WhaleToken", None)
            .expect("a mapping with a sole declarer should resolve without an issuer_id");
        assert_eq!(
            issuer.issuer_id, "acme",
            "the sole declaring issuer should be resolved with its id sanitized"
        );
        assert!(
            token.required,
            "resolve should return the metadata of the requested token, not of a sibling"
        );

        // Unknown mapping.
        let err = index
            .resolve("Unknown::Type", None)
            .expect_err("an unknown mapping should fail to resolve");
        assert!(
            matches!(err, CustomTokenError::UnknownIssuer(_)),
            "an unknown mapping should surface as an UnknownIssuer error"
        );
    }
}
