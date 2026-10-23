// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Policy-store configuration for custom (non-JWT) issuers.
//!
//! A custom issuer's tokens are validated by a registered
//! [`CustomTokenProcessor`](crate::CustomTokenProcessor) rather than the JWT
//! pipeline. Unlike a JWT issuer, a custom issuer has no `iss` or key source, and
//! its `token_id` is supplied by the processor (not read from a claim).

#[cfg(test)]
use std::collections::HashMap;
use std::collections::HashSet;

/// Configuration for a single custom issuer, keyed by issuer name. Only tests
/// still describe custom issuers this way, through `TokenTypes::build`.
#[cfg(test)]
#[derive(Debug, Clone, PartialEq)]
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
#[derive(Debug, Clone, Default, PartialEq)]
pub(crate) struct CustomTokenMetadata {
    /// Claims required to be present in the processed output.
    pub(crate) required_claims: HashSet<String>,
}
