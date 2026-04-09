// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::common::{issuer_utils::IssClaim, policy_store::TrustedIssuer};
use serde::Deserialize;
use serde_json::Value;
use std::{collections::HashMap, sync::Arc};

/// Structure representing a validated JWT token, used to derive a Cedar token entity.
/// Make sure to provide a `TrustedIssuer`; otherwise, the `iss` field may not be constructed.
#[derive(Debug, PartialEq, Clone)]
pub(crate) struct Token {
    pub name: String,
    pub iss: Option<Arc<TrustedIssuer>>,
    pub(crate) claims: TokenClaims,
}

impl Token {
    pub(crate) fn new(name: &str, claims: TokenClaims, iss: Option<Arc<TrustedIssuer>>) -> Token {
        Self {
            name: name.to_string(),
            iss,
            claims,
        }
    }

    pub(crate) fn get_claim(&self, name: &str) -> Option<TokenClaim<'_>> {
        self.claims.get_claim(name)
    }

    pub(crate) fn get_claim_val(&self, name: &str) -> Option<&Value> {
        self.claims.claims.get(name)
    }

    pub(crate) fn logging_info(&self, claim: &str) -> HashMap<String, serde_json::Value> {
        self.claims.logging_info(claim)
    }

    pub(crate) fn claims_value(&self) -> &HashMap<String, Value> {
        &self.claims.claims
    }

    /// Extract normalized issuer URL from a token
    pub(crate) fn extract_normalized_issuer(&self) -> Option<IssClaim> {
        // Method 1: From TrustedIssuer reference (preferred)
        if let Some(trusted_issuer) = &self.iss {
            return Some(trusted_issuer.iss_claim());
        }

        // Method 2: From token claims (fallback)
        self.claims
            .get_claim("iss")
            .and_then(|claim| claim.value().as_str().map(IssClaim::new))
    }
}

#[derive(Debug, PartialEq, Default, Deserialize, Clone)]
pub(crate) struct TokenClaims {
    #[serde(flatten)]
    claims: HashMap<String, Value>,
}

impl From<HashMap<String, Value>> for TokenClaims {
    fn from(claims: HashMap<String, Value>) -> Self {
        Self { claims }
    }
}

impl From<Value> for TokenClaims {
    fn from(claims: Value) -> Self {
        let claims = serde_json::from_value(claims).expect("should deserialize claims to hashmap");
        Self { claims }
    }
}

impl TokenClaims {
    pub(crate) fn get_claim(&self, name: &str) -> Option<TokenClaim<'_>> {
        self.claims.get(name).map(|value| TokenClaim { value })
    }

    pub(crate) fn logging_info(&self, claim: &str) -> HashMap<String, serde_json::Value> {
        let claim = if claim.is_empty() { "jti" } else { claim };

        self.claims
            .get(claim)
            .map(|value| HashMap::from([(claim.to_string(), value.clone())]))
            .unwrap_or_default()
    }
}

pub(crate) struct TokenClaim<'a> {
    value: &'a serde_json::Value,
}

impl TokenClaim<'_> {
    pub(crate) fn value(&self) -> &Value {
        self.value
    }
}
