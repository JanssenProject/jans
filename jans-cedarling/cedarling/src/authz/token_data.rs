/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use std::collections::HashMap;

use serde_json::Value;

/// A container for storing token data or data attributes for the .
/// Provides methods for retrieving payload from the token or attributes for the .
#[derive(Debug, Clone, serde::Deserialize)]
pub(crate) struct TokenPayload {
    #[serde(flatten)]
    pub payload: HashMap<String, serde_json::Value>,
}

impl TokenPayload {
    fn new(payload: HashMap<String, serde_json::Value>) -> Self {
        Self { payload }
    }

    pub fn get(&self, key: &str) -> Result<Payload, GetTokenClaimValue> {
        self.payload
            .get(key)
            .map(|value| Payload {
                key: key.to_string(),
                value,
            })
            .ok_or(GetTokenClaimValue::KeyNotFound(key.to_string()))
    }
}

impl From<HashMap<String, serde_json::Value>> for TokenPayload {
    fn from(value: HashMap<String, serde_json::Value>) -> Self {
        TokenPayload::new(value)
    }
}

/// Errors that can occur when trying to get claim attribute value from token data
#[derive(Debug, thiserror::Error)]
pub enum GetTokenClaimValue {
    #[error("could not find field with key: {0}")]
    KeyNotFound(String),
    #[error("could not convert json field with key: {key} to: {expected_type}, got: {got_type}")]
    KeyNotCorrectType {
        key: String,
        expected_type: String,
        got_type: String,
    },
    #[error("could not convert json value to: {expected_type}, got: {got_type}")]
    NotCorrectType {
        expected_type: String,
        got_type: String,
    },
}

impl GetTokenClaimValue {
    pub(crate) fn json_value_type_name(value: &Value) -> String {
        match value {
            Value::Null => "null".to_string(),
            Value::Bool(_) => "bool".to_string(),
            Value::Number(_) => "number".to_string(),
            Value::String(_) => "string".to_string(),
            Value::Array(_) => "array".to_string(),
            Value::Object(_) => "object".to_string(),
        }
    }

    /// Returns `KeyNotCorrectType` error case
    /// is used for useful error message
    fn not_correct_type(
        key: &str,
        expected_type_name: &str,
        got_value: &Value,
    ) -> GetTokenClaimValue {
        let got_value_type_name = Self::json_value_type_name(got_value);

        GetTokenClaimValue::KeyNotCorrectType {
            key: key.to_string(),
            expected_type: expected_type_name.to_string(),
            got_type: got_value_type_name,
        }
    }
}

pub(crate) struct Payload<'a> {
    key: String,
    value: &'a serde_json::Value,
}

impl<'a> Payload<'a> {
    pub fn as_i64(&self) -> Result<i64, GetTokenClaimValue> {
        self.value
            .as_i64()
            .ok_or(GetTokenClaimValue::not_correct_type(
                &self.key, "i64", self.value,
            ))
    }

    pub fn as_str(&self) -> Result<&str, GetTokenClaimValue> {
        self.value
            .as_str()
            .ok_or(GetTokenClaimValue::not_correct_type(
                &self.key, "String", self.value,
            ))
    }

    pub fn as_bool(&self) -> Result<bool, GetTokenClaimValue> {
        self.value
            .as_bool()
            .ok_or(GetTokenClaimValue::not_correct_type(
                &self.key, "bool", self.value,
            ))
    }

    pub fn as_array(&self) -> Result<Vec<Payload>, GetTokenClaimValue> {
        self.value
            .as_array()
            .map(|array| {
                array
                    .iter()
                    .enumerate()
                    .map(|(i, v)| Payload {
                        // show current key and index in array
                        key: format!("{}[{}]", self.key, i),
                        value: v,
                    })
                    .collect()
            })
            .ok_or(GetTokenClaimValue::not_correct_type(
                &self.key, "Array", self.value,
            ))
    }
}
