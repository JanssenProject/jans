/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use crate::common::policy_store::TrustedIssuer;
use serde_json::Value;
use std::collections::HashMap;

#[allow(dead_code)]
pub enum TokenStr<'a> {
    AccessToken(&'a str),
    IdToken(&'a str),
    UserinfoToken(&'a str),
}

#[derive(Debug, PartialEq)]
pub struct DecodedToken<'a> {
    pub claims: HashMap<String, serde_json::Value>,
    pub iss: Option<&'a TrustedIssuer>,
}

#[allow(dead_code)]
impl<'a> DecodedToken<'a> {
    pub fn new(claims: HashMap<String, serde_json::Value>, iss: Option<&'a TrustedIssuer>) -> Self {
        Self { claims, iss }
    }

    pub fn from_json_map(map: serde_json::Map<String, serde_json::Value>) -> Self {
        Self::new(HashMap::from_iter(map), None)
    }

    pub fn get_claim(&self, name: &str) -> Result<Claim, TokenClaimError> {
        self.claims
            .get(name)
            .map(|value| Claim {
                key: name.to_string(),
                value,
            })
            .ok_or(TokenClaimError::MissingKey(name.to_string()))
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
pub(crate) struct Claim<'a> {
    key: String,
    value: &'a serde_json::Value,
}

#[allow(dead_code)]
impl Claim<'_> {
    pub fn key(&self) -> &str {
        &self.key
    }

    pub fn value(&self) -> &serde_json::Value {
        self.value
    }

    pub fn as_i64(&self) -> Result<i64, TokenClaimError> {
        self.value
            .as_i64()
            .ok_or(TokenClaimError::type_mismatch(&self.key, "i64", self.value))
    }

    pub fn as_str(&self) -> Result<&str, TokenClaimError> {
        self.value.as_str().ok_or(TokenClaimError::type_mismatch(
            &self.key, "String", self.value,
        ))
    }

    pub fn as_bool(&self) -> Result<bool, TokenClaimError> {
        self.value.as_bool().ok_or(TokenClaimError::type_mismatch(
            &self.key, "bool", self.value,
        ))
    }

    pub fn as_array(&self) -> Result<Vec<Claim>, TokenClaimError> {
        self.value
            .as_array()
            .map(|array| {
                array
                    .iter()
                    .enumerate()
                    .map(|(i, v)| Claim {
                        // show current key and index in array
                        key: format!("{}[{}]", self.key, i),
                        value: v,
                    })
                    .collect()
            })
            .ok_or(TokenClaimError::type_mismatch(
                &self.key, "Array", self.value,
            ))
    }
}

/// Errors that can occur when trying to get claim attribute value from token data
#[derive(Debug, thiserror::Error)]
pub enum TokenClaimError {
    #[error("Key not found: {0}")]
    MissingKey(String),
    #[error(
        "Type mismatch for key '{key}': expected: '{expected_type}', but found: '{actual_type}'"
    )]
    TypeMismatch {
        key: String,
        expected_type: String,
        actual_type: String,
    },
}

impl TokenClaimError {
    /// Returns the JSON type name of the given value.
    fn json_value_type_name(value: &Value) -> String {
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
    fn type_mismatch(key: &str, expected_type_name: &str, got_value: &Value) -> TokenClaimError {
        let got_value_type_name = Self::json_value_type_name(got_value);

        TokenClaimError::TypeMismatch {
            key: key.to_string(),
            expected_type: expected_type_name.to_string(),
            actual_type: got_value_type_name,
        }
    }
}
