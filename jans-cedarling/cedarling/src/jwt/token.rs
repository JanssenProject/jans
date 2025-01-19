// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::common::policy_store::{ClaimMappings, TokenEntityMetadata, TrustedIssuer};
use serde::Deserialize;
use serde_json::Value;
use std::collections::HashMap;

const DEFAULT_USER_ID_SRC_CLAIM: &str = "sub";
const DEFAULT_ROLE_SRC_CLAIM: &str = "role";

#[derive(Debug, PartialEq)]
pub struct Token<'a> {
    pub name: String,
    pub iss: Option<&'a TrustedIssuer>,
    claims: TokenClaims,
}

impl<'a> Token<'a> {
    pub fn new(name: &str, claims: TokenClaims, iss: Option<&'a TrustedIssuer>) -> Token<'a> {
        Self {
            name: name.to_string(),
            iss,
            claims,
        }
    }

    pub fn get_metadata(&self) -> Option<&TokenEntityMetadata> {
        self.iss.unwrap_or_default().get_token_metadata(&self.name)
    }

    pub fn user_mapping(&self) -> &str {
        self.iss
            .unwrap_or_default()
            .get_user_mapping(&self.name)
            .unwrap_or(DEFAULT_USER_ID_SRC_CLAIM)
    }

    pub fn claim_mapping(&self) -> Option<&ClaimMappings> {
        self.iss.unwrap_or_default().get_claim_mapping(&self.name)
    }

    pub fn role_mapping(&self) -> &str {
        self.iss
            .unwrap_or_default()
            .get_role_mapping(&self.name)
            .unwrap_or(DEFAULT_ROLE_SRC_CLAIM)
    }

    pub fn get_claim(&self, name: &str) -> Option<TokenClaim> {
        self.claims.get_claim(name)
    }

    pub fn logging_info(&'a self, claim: &'a str) -> HashMap<&'a str, &'a serde_json::Value> {
        self.claims.logging_info(claim)
    }

    pub fn claims_value(&self) -> &HashMap<String, Value> {
        &self.claims.claims
    }
}

#[derive(Debug, PartialEq, Default, Deserialize, Clone)]
pub struct TokenClaims {
    #[serde(flatten)]
    claims: HashMap<String, Value>,
}

impl From<HashMap<String, Value>> for TokenClaims {
    fn from(claims: HashMap<String, Value>) -> Self {
        Self { claims }
    }
}

impl TokenClaims {
    pub fn get_claim(&self, name: &str) -> Option<TokenClaim> {
        self.claims.get(name).map(|value| TokenClaim {
            key: name.to_string(),
            value,
        })
    }

    pub fn logging_info<'a>(&'a self, claim: &'a str) -> HashMap<&'a str, &'a serde_json::Value> {
        let claim = if !claim.is_empty() { claim } else { "jti" };

        let iter = [self.claims.get(claim).map(|value| (claim, value))]
            .into_iter()
            .flatten();

        HashMap::from_iter(iter)
    }
}

pub struct TokenClaim<'a> {
    key: String,
    value: &'a serde_json::Value,
}

impl TokenClaim<'_> {
    pub fn value(&self) -> &serde_json::Value {
        self.value
    }

    pub fn as_str(&self) -> Result<&str, TokenClaimTypeError> {
        self.value
            .as_str()
            .ok_or(TokenClaimTypeError::type_mismatch(
                &self.key, "String", self.value,
            ))
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
