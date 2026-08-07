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

use crate::common::policy_store::CustomIssuerMetadata;
use crate::entity_builder::sanitize_issuer_name;
use async_trait::async_trait;
use serde_json::Value;
use std::collections::{HashMap, HashSet};
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
    /// The framework may race this future against a timeout deadline, but only on
    /// native (non-WASM) targets. Do not assume a timeout is enforced here: on WASM
    /// the future always runs to completion, so `process` must not rely on being
    /// cancelled.
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
    pub issuer_id: Option<String>,
    /// Optional expiration (unix seconds). Bounds the token-cache TTL when set.
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

    /// The processed claims were not a JSON object.
    #[error("custom token claims are not a JSON object")]
    InvalidClaims,

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
    /// Cedar entity type name (the request `mapping`).
    pub entity_type_name: String,
    /// If true, a processing failure for this issuer fails the whole request.
    pub required: bool,
    /// Claims that must be present in the processed output.
    pub required_claims: HashSet<String>,
}

/// Index of the policy store's configured custom issuers. Built once per
/// [`Authz`](crate::Authz) build (fresh on every policy-store swap, so it is
/// never stale even when the `JwtService` is reused).
#[derive(Debug, Default)]
pub(crate) struct CustomIssuerIndex {
    /// sanitized issuer id -> resolved issuer
    by_id: HashMap<String, ResolvedCustomIssuer>,
    /// `entity_type_name` (request mapping) -> sanitized issuer ids declaring it
    by_mapping: HashMap<String, Vec<String>>,
}

impl CustomIssuerIndex {
    /// Build the index from policy-store `custom_issuers`, sanitizing issuer names.
    pub(crate) fn build(custom_issuers: &HashMap<String, CustomIssuerMetadata>) -> Self {
        let mut by_id = HashMap::with_capacity(custom_issuers.len());
        let mut by_mapping: HashMap<String, Vec<String>> = HashMap::new();

        for (name, meta) in custom_issuers {
            let issuer_id = sanitize_issuer_name(name);
            if by_id.contains_key(&issuer_id) {
                continue;
            }
            by_mapping
                .entry(meta.entity_type_name.clone())
                .or_default()
                .push(issuer_id.clone());
            by_id.insert(
                issuer_id.clone(),
                ResolvedCustomIssuer {
                    issuer_id,
                    entity_type_name: meta.entity_type_name.clone(),
                    required: meta.required,
                    required_claims: meta.required_claims.clone(),
                },
            );
        }

        Self { by_id, by_mapping }
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

    /// Whether a request `mapping` routes to a custom issuer.
    pub(crate) fn mapping_is_custom(&self, mapping: &str) -> bool {
        self.by_mapping.contains_key(mapping)
    }

    /// Whether any custom issuer declaring this `mapping` is `required`. Used to
    /// decide fail-closed vs skip-and-continue on a processing error.
    pub(crate) fn mapping_required(&self, mapping: &str) -> bool {
        self.by_mapping
            .get(mapping)
            .into_iter()
            .flatten()
            .filter_map(|id| self.by_id.get(id))
            .any(|i| i.required)
    }

    /// Resolve the custom issuer for a processed token. Prefers an explicit
    /// `issuer_id` from the processor; otherwise falls back to the sole issuer
    /// declaring `mapping`. Errors if unknown or ambiguous.
    pub(crate) fn resolve(
        &self,
        mapping: &str,
        issuer_id: Option<&str>,
    ) -> Result<&ResolvedCustomIssuer, CustomTokenError> {
        if let Some(id) = issuer_id {
            let sanitized = sanitize_issuer_name(id);
            return self
                .by_id
                .get(&sanitized)
                .ok_or_else(|| CustomTokenError::UnknownIssuer(id.to_string()));
        }

        let ids = self
            .by_mapping
            .get(mapping)
            .ok_or_else(|| CustomTokenError::UnknownIssuer(mapping.to_string()))?;
        match ids.as_slice() {
            [only] => self
                .by_id
                .get(only)
                .ok_or_else(|| CustomTokenError::UnknownIssuer(mapping.to_string())),
            _ => Err(CustomTokenError::Processing(format!(
                "ambiguous mapping '{mapping}': multiple custom issuers declare it; \
                 the processor must return an issuer_id"
            ))),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn meta(entity_type_name: &str, required: bool) -> CustomIssuerMetadata {
        CustomIssuerMetadata {
            entity_type_name: entity_type_name.to_string(),
            required,
            required_claims: HashSet::new(),
        }
    }

    #[test]
    fn build_sanitizes_issuer_ids_and_routes_by_mapping() {
        let mut issuers = HashMap::new();
        issuers.insert("Ac-me Corp".to_string(), meta("Acme::CustomToken", false));
        let index = CustomIssuerIndex::build(&issuers);

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
    fn build_rejects_sanitized_duplicate_ids() {
        let mut issuers = HashMap::new();
        issuers.insert("Ac-Me".to_string(), meta("First::T", false));
        issuers.insert("ac me".to_string(), meta("Second::T", true));
        let index = CustomIssuerIndex::build(&issuers);

        let ids: Vec<&str> = index.sanitized_ids().collect();
        assert_eq!(
            ids,
            ["ac_me"],
            "distinct names sanitizing to the same id should collapse into a single entry"
        );
        assert!(
            index.mapping_is_custom("First::T") != index.mapping_is_custom("Second::T"),
            "the rejected duplicate must not register its mapping alongside the kept issuer"
        );
    }

    #[test]
    fn mapping_required_reflects_any_required_declarer() {
        let mut issuers = HashMap::new();
        issuers.insert("a".to_string(), meta("M::T", false));
        issuers.insert("b".to_string(), meta("M::T", true));
        let index = CustomIssuerIndex::build(&issuers);
        assert!(
            index.mapping_required("M::T"),
            "a mapping declared by any required issuer should be fail-closed"
        );
        assert!(
            !index.mapping_required("other"),
            "mappings with no required issuer should not be fail-closed"
        );
    }

    #[test]
    fn resolve_prefers_explicit_issuer_id() {
        let mut issuers = HashMap::new();
        issuers.insert("acme".to_string(), meta("M::T", false));
        issuers.insert("beta".to_string(), meta("M::T", false));
        let index = CustomIssuerIndex::build(&issuers);

        let resolved = index
            .resolve("M::T", Some("beta"))
            .expect("an explicit issuer_id should resolve to that issuer");
        assert_eq!(
            resolved.issuer_id, "beta",
            "an explicit issuer_id should win over the mapping's sole declarer"
        );

        // Unknown issuer id.
        let err = index
            .resolve("M::T", Some("nope"))
            .expect_err("an unknown issuer_id should fail to resolve");
        assert!(
            matches!(err, CustomTokenError::UnknownIssuer(_)),
            "an unknown issuer_id should surface as an UnknownIssuer error"
        );

        let err = index
            .resolve("M::T", None)
            .expect_err("an ambiguous mapping without an issuer_id should fail to resolve");
        assert!(
            matches!(err, CustomTokenError::Processing(_)),
            "an ambiguous mapping without an issuer_id should surface as a Processing error"
        );
    }

    #[test]
    fn resolve_uses_sole_declarer_when_unambiguous() {
        let mut issuers = HashMap::new();
        issuers.insert("Acme".to_string(), meta("Acme::CustomToken", false));
        let index = CustomIssuerIndex::build(&issuers);

        let resolved = index
            .resolve("Acme::CustomToken", None)
            .expect("a mapping with a sole declarer should resolve without an issuer_id");
        assert_eq!(
            resolved.issuer_id, "acme",
            "the sole declaring issuer should be resolved with its id sanitized"
        );
        assert_eq!(
            resolved.entity_type_name, "Acme::CustomToken",
            "the resolved issuer should keep its configured entity type name"
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
