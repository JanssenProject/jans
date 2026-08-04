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
use std::collections::HashSet;

/// Configuration for a single custom issuer, keyed by issuer name in the policy
/// store's `custom_issuers` map.
#[derive(Debug, Clone, PartialEq, Deserialize)]
pub(crate) struct CustomIssuerMetadata {
    /// Cedar entity type name for tokens from this issuer. Matched against the
    /// request `TokenInput.mapping` to route a token to the custom path.
    pub(crate) entity_type_name: String,
    /// When true, a processing failure (or timeout) for this issuer's token fails
    /// the whole authorization request instead of being skipped.
    #[serde(default)]
    pub(crate) required: bool,
    /// Claims required to be present in the processed output.
    #[serde(default)]
    pub(crate) required_claims: HashSet<String>,
}
