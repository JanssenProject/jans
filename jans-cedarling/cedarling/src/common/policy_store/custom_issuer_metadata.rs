// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Policy-store configuration for custom (non-JWT) issuers.
//!
//! A custom issuer's tokens are validated by a registered
//! [`CustomTokenProcessor`](crate::CustomTokenProcessor) rather than the JWT
//! pipeline. Unlike [`TokenEntityMetadata`](super::TokenEntityMetadata), a custom
//! issuer has no `openid_configuration_endpoint` and its `token_id` is supplied
//! by the processor (not read from a claim).

use serde::Deserialize;
use std::collections::{HashMap, HashSet};

/// Configuration for a single custom issuer, keyed by issuer name in the policy
/// store's `custom_issuers` map.
#[derive(Debug, Clone, PartialEq, Deserialize)]
pub(crate) struct CustomIssuerMetadata {
    /// Token types this issuer emits, keyed by the request `TokenInput.mapping`
    /// (a Cedar entity type name) that routes a token to the custom path.
    ///
    /// Keying by the mapping keeps it a single source of truth (no separate
    /// token-name key as in [`TrustedIssuer`](super::TrustedIssuer)) and makes
    /// duplicate types within one issuer unrepresentable.
    pub(crate) tokens_mappings: HashMap<String, CustomTokenMetadata>,
}

/// Per-token configuration for a custom issuer.
#[derive(Debug, Clone, Default, PartialEq, Deserialize)]
pub(crate) struct CustomTokenMetadata {
    /// When true, a processing failure (or timeout) for this token fails the whole
    /// authorization request instead of being skipped.
    #[serde(default)]
    pub(crate) required: bool,
    /// Claims required to be present in the processed output.
    #[serde(default)]
    pub(crate) required_claims: HashSet<String>,
}
