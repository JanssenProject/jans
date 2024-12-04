/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use std::collections::HashMap;

use derive_more::Deref;
use serde_json::Value;

/// Wrapper around access token decode result
#[derive(Clone, Deref, serde::Deserialize)]
pub(crate) struct AccessTokenData(TokenPayload);

/// Wrapper around id token decode result
#[derive(Clone, Deref, serde::Deserialize)]
pub(crate) struct IdTokenData(TokenPayload);

/// Wrapper around userinfo token decode result
#[derive(Clone, Deref, serde::Deserialize)]
pub(crate) struct UserInfoTokenData(TokenPayload);

/// A container for storing token data or data attributes for the .
/// Provides methods for retrieving payload from the token or attributes for the .
#[derive(Debug, Clone, Default, serde::Deserialize, serde::Serialize)]
pub(crate) struct TokenPayload {
    #[serde(flatten)]
    pub payload: HashMap<String, serde_json::Value>,
}

impl TokenPayload {
    pub fn new(payload: HashMap<String, serde_json::Value>) -> Self {
        Self { payload }
    }

    pub fn from_json_map(map: serde_json::Map<String, serde_json::Value>) -> Self {
        Self::new(HashMap::from_iter(map))
    }

    /// Get [`Payload`] structure that contain key and [serde_json::Value] value.
    pub fn get_payload(&self, key: &str) -> Result<Payload, GetTokenClaimValue> {
        self.payload
            .get(key)
            .map(|value| Payload {
                key: key.to_string(),
                value,
            })
            .ok_or(GetTokenClaimValue::KeyNotFound(key.to_string()))
    }

    /// get tokens info claim for [`LogTokensInfo`] structure
    /// we have no bootstrap property to define that claims should be in result,
    /// so hardcoded only 'jti' but it can be changed in future
    pub(crate) fn get_log_tokens_info<'a>(&'a self) -> HashMap<&'a str, &'a serde_json::Value> {
        const TOKEN_CLAIMS: [&str; 1] = ["jti"];

        HashMap::from_iter(
            TOKEN_CLAIMS
                .iter()
                .map(|&claim| self.payload.get(claim).map(|value| (claim, value)))
                .flatten(),
        )
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

/// Structure that contains  information about token claim, only about one attribute
/// key and json value
///
/// Wrapper to get more readable error messages when we get not correct type of  value from json.
/// Is used in the [`TokenPayload::get_payload`] method
pub(crate) struct Payload<'a> {
    key: String,
    value: &'a serde_json::Value,
}

impl Payload<'_> {
    /// Get key value of payload
    pub fn get_key(&self) -> &str {
        &self.key
    }

    /// Get value of payload
    pub fn get_value(&self) -> &serde_json::Value {
        self.value
    }

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
