/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use std::collections::HashMap;

use cedar_policy::RestrictedExpression;
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

    /// Get claim attribute value
    /// for all types that implements `ClaimGetter<T>`
    pub fn get_value<T>(&self, key: &str) -> Result<T, GetTokenClaimValue>
    where
        TokenPayload: AttributeGetter<T>,
    {
        self.get_attribute_value(key)
    }

    /// Get claim attribute [`RestrictedExpression`]
    /// for all types that implements `ClaimGetter<T>`
    pub fn get_expression<T>(&self, key: &str) -> Result<RestrictedExpression, GetTokenClaimValue>
    where
        TokenPayload: AttributeGetter<T>,
    {
        self.get_attribute_expression(key)
    }
}

impl From<HashMap<String, serde_json::Value>> for TokenPayload {
    fn from(value: HashMap<String, serde_json::Value>) -> Self {
        TokenPayload::new(value)
    }
}

/// Trait to get claim attribute value by key from token data
pub(crate) trait AttributeGetter<T> {
    /// Get claim attribute value
    fn get_attribute_value(&self, key: &str) -> Result<T, GetTokenClaimValue>;

    /// Get claim attribute [`RestrictedExpression`]
    fn get_attribute_expression(
        &self,
        key: &str,
    ) -> Result<RestrictedExpression, GetTokenClaimValue>;
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

// impl ClaimGetter for getting `i64` value
impl AttributeGetter<i64> for TokenPayload {
    fn get_attribute_value(&self, key: &str) -> Result<i64, GetTokenClaimValue> {
        if let Some(attr_value) = self.payload.get(key) {
            attr_value
                .as_i64()
                .ok_or(GetTokenClaimValue::not_correct_type(key, "i64", attr_value))
        } else {
            Err(GetTokenClaimValue::KeyNotFound(key.to_string()))
        }
    }

    fn get_attribute_expression(
        &self,
        key: &str,
    ) -> Result<RestrictedExpression, GetTokenClaimValue> {
        Ok(RestrictedExpression::new_long(self.get_value(key)?))
    }
}

// impl ClaimGetter for getting `String` value
impl AttributeGetter<String> for TokenPayload {
    fn get_attribute_value(&self, key: &str) -> Result<String, GetTokenClaimValue> {
        if let Some(attr_value) = self.payload.get(key) {
            let result = attr_value
                .as_str()
                .ok_or(GetTokenClaimValue::not_correct_type(
                    key, "String", attr_value,
                ))?
                .to_string();
            Ok(result)
        } else {
            Err(GetTokenClaimValue::KeyNotFound(key.to_string()))
        }
    }

    fn get_attribute_expression(
        &self,
        key: &str,
    ) -> Result<RestrictedExpression, GetTokenClaimValue> {
        Ok(RestrictedExpression::new_string(self.get_value(key)?))
    }
}

// impl ClaimGetter for getting `bool` value
impl AttributeGetter<bool> for TokenPayload {
    fn get_attribute_value(&self, key: &str) -> Result<bool, GetTokenClaimValue> {
        if let Some(attr_value) = self.payload.get(key) {
            attr_value
                .as_bool()
                .ok_or(GetTokenClaimValue::not_correct_type(
                    key, "bool", attr_value,
                ))
        } else {
            Err(GetTokenClaimValue::KeyNotFound(key.to_string()))
        }
    }

    fn get_attribute_expression(
        &self,
        key: &str,
    ) -> Result<RestrictedExpression, GetTokenClaimValue> {
        Ok(RestrictedExpression::new_bool(self.get_value(key)?))
    }
}
