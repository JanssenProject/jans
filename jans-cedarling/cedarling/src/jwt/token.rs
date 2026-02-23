// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::common::{
    issuer_utils::normalize_issuer,
    policy_store::{ClaimMappings, TokenEntityMetadata, TrustedIssuer},
};
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

    pub(crate) fn get_metadata(&self) -> Option<&TokenEntityMetadata> {
        self.iss.as_ref()?.get_token_metadata(&self.name)
    }

    pub(crate) fn claim_mappings(&self) -> Option<&ClaimMappings> {
        self.iss.as_ref()?.get_claim_mapping(&self.name)
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
    pub(crate) fn extract_normalized_issuer(&self) -> Option<String> {
        // Method 1: From TrustedIssuer reference (preferred)
        if let Some(trusted_issuer) = &self.iss {
            return Some(trusted_issuer.normalized_issuer());
        }

        // Method 2: From token claims (fallback)
        self.claims
            .get_claim("iss")
            .and_then(|claim| claim.value().as_str().map(normalize_issuer))
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
        self.claims.get(name).map(|value| TokenClaim {
            key: name.to_string(),
            value,
        })
    }

    pub(crate) fn logging_info(&self, claim: &str) -> HashMap<String, serde_json::Value> {
        let claim = if claim.is_empty() { "jti" } else { claim };

        self.claims
            .get(claim)
            .map(|value| HashMap::from([(claim.to_string(), value.clone())]))
            .unwrap_or_default()
    }

    // Update TokenClaims claim value by consuming itself.
    // Consuming allows to be sure that only one instance exist.
    pub(crate) fn with_claim(self, k: String, v: Value) -> Self {
        let mut claims = self;
        claims.claims.insert(k, v);

        claims
    }
}

pub(crate) struct TokenClaim<'a> {
    key: String,
    value: &'a serde_json::Value,
}

impl TokenClaim<'_> {
    pub(crate) fn as_str(&self) -> Result<&str, TokenClaimTypeError> {
        self.value
            .as_str()
            .ok_or(TokenClaimTypeError::type_mismatch(
                &self.key, "String", self.value,
            ))
    }

    pub(crate) fn value(&self) -> &Value {
        self.value
    }
}

#[derive(Debug, thiserror::Error, PartialEq)]
#[error(
    "type mismatch for token claim '{key}'. expected: '{expected_type}', but found: '{actual_type}'"
)]
pub struct TokenClaimTypeError {
    pub key: String,
    pub expected_type: String,
    pub actual_type: String,
}

impl TokenClaimTypeError {
    /// Returns the JSON type name of the given value.
    pub fn json_value_type_name(value: &Value) -> String {
        match value {
            Value::Null => "null".to_string(),
            Value::Bool(_) => "bool".to_string(),
            Value::Number(_) => "number".to_string(),
            Value::String(_) => "string".to_string(),
            Value::Array(_) => "array".to_string(),
            Value::Object(_) => "object".to_string(),
        }
    }

    /// Constructs a `TypeMismatch` error with detailed information about the expected and actual types.
    pub fn type_mismatch(key: &str, expected_type_name: &str, got_value: &Value) -> Self {
        let got_value_type_name = Self::json_value_type_name(got_value);

        Self {
            key: key.to_string(),
            expected_type: expected_type_name.to_string(),
            actual_type: got_value_type_name,
        }
    }
}
