/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use crate::common::policy_store::TrustedIssuer;
use serde::Deserialize;
use serde_json::Value;
use std::collections::HashMap;

#[allow(dead_code)]
pub enum TokenStr<'a> {
    AccessToken(&'a str),
    IdToken(&'a str),
    UserinfoToken(&'a str),
}

/// A struct holding information on a decoded JWT.
#[derive(Debug, PartialEq, Default)]
pub struct Token<'a> {
    claims: HashMap<String, Value>,
    pub iss: Option<&'a TrustedIssuer>,
}

impl<'de> Deserialize<'de> for Token<'_> {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let claims = HashMap::<String, Value>::deserialize(deserializer)?;
        Ok(Self { claims, iss: None })
    }
}

impl From<HashMap<String, Value>> for Token<'_> {
    fn from(claims: HashMap<String, Value>) -> Self {
        Self { claims, iss: None }
    }
}

#[allow(dead_code)]
impl<'a> Token<'a> {
    pub fn new(claims: HashMap<String, serde_json::Value>, iss: Option<&'a TrustedIssuer>) -> Self {
        Self { claims, iss }
    }

    pub fn from_json_map(map: serde_json::Map<String, serde_json::Value>) -> Self {
        Self::new(HashMap::from_iter(map), None)
    }
    
    pub fn has_claim(&self, name: &str) -> bool {
        self.claims.contains_key(name)
    }

    pub fn get_claim(&self, name: &str) -> Option<TokenClaim> {
        self.claims.get(name).map(|value| TokenClaim {
            key: name.to_string(),
            value,
        })
    }

    pub fn get_logging_info(
        &'a self,
        decision_log_default_jwt_id: &'a str,
    ) -> HashMap<&'a str, &'a serde_json::Value> {
        let claim = if !decision_log_default_jwt_id.is_empty() {
            decision_log_default_jwt_id
        } else {
            "jti"
        };

        let iter = [self.claims.get(claim).map(|value| (claim, value))]
            .into_iter()
            .flatten();

        HashMap::from_iter(iter)
    }
}

#[allow(dead_code)]
pub struct TokenClaim<'a> {
    key: String,
    value: &'a serde_json::Value,
}

#[allow(dead_code)]
impl TokenClaim<'_> {
    pub fn key(&self) -> &str {
        &self.key
    }

    pub fn value(&self) -> &serde_json::Value {
        self.value
    }

    pub fn as_i64(&self) -> Result<i64, TokenClaimTypeError> {
        self.value
            .as_i64()
            .ok_or(TokenClaimTypeError::type_mismatch(
                &self.key, "i64", self.value,
            ))
    }

    pub fn as_str(&self) -> Result<&str, TokenClaimTypeError> {
        self.value
            .as_str()
            .ok_or(TokenClaimTypeError::type_mismatch(
                &self.key, "String", self.value,
            ))
    }

    pub fn as_bool(&self) -> Result<bool, TokenClaimTypeError> {
        self.value
            .as_bool()
            .ok_or(TokenClaimTypeError::type_mismatch(
                &self.key, "bool", self.value,
            ))
    }

    pub fn as_array(&self) -> Result<Vec<TokenClaim>, TokenClaimTypeError> {
        self.value
            .as_array()
            .map(|array| {
                array
                    .iter()
                    .enumerate()
                    .map(|(i, v)| TokenClaim {
                        // show current key and index in array
                        key: format!("{}[{}]", self.key, i),
                        value: v,
                    })
                    .collect()
            })
            .ok_or(TokenClaimTypeError::type_mismatch(
                &self.key, "Array", self.value,
            ))
    }
}

#[derive(Debug, thiserror::Error)]
#[error("Type mismatch for key '{key}': expected: '{expected_type}', but found: '{actual_type}'")]
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
    fn type_mismatch(key: &str, expected_type_name: &str, got_value: &Value) -> Self {
        let got_value_type_name = Self::json_value_type_name(got_value);

        Self {
            key: key.to_string(),
            expected_type: expected_type_name.to_string(),
            actual_type: got_value_type_name,
        }
    }
}
