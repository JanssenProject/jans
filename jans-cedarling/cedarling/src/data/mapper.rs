// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Cedar Value Mapping
//!
//! Provides bidirectional conversion between JSON values and Cedar values,
//! with support for all Cedar data types including extension types.

use crate::data::error::ValueMappingError;

use super::CedarType;
use cedar_policy::RestrictedExpression;
use serde_json::{Map, Value};
use std::collections::HashMap;
use std::net::IpAddr;
use std::str::FromStr;

/// Represents a detected extension type with its parsed value.
#[derive(Debug, Clone, PartialEq)]
pub enum ExtensionValue {
    /// An IP address (IPv4 or IPv6)
    IpAddr(String),
    /// A decimal number
    Decimal(String),
}

/// Mapper for bidirectional JSON ↔ Cedar value conversion.
///
/// Provides methods to convert between `serde_json::Value` and Cedar's
/// `RestrictedExpression`, with support for all Cedar data types including
/// extension types (IP addresses, decimals).
#[derive(Debug, Clone, Default)]
pub struct CedarValueMapper {
    /// Whether to auto-detect extension types from string patterns
    auto_detect_extensions: bool,
    /// Maximum allowed value size in bytes (0 = no limit)
    max_value_size: usize,
}

impl CedarValueMapper {
    /// Create a new mapper with default settings.
    pub fn new() -> Self {
        Self {
            auto_detect_extensions: true,
            max_value_size: 0,
        }
    }

    /// Create a mapper with auto-detection of extension types disabled.
    pub fn without_auto_detect() -> Self {
        Self {
            auto_detect_extensions: false,
            max_value_size: 0,
        }
    }

    /// Set the maximum allowed value size in bytes.
    ///
    /// A value of 0 means no limit.
    pub fn with_max_size(mut self, max_size: usize) -> Self {
        self.max_value_size = max_size;
        self
    }

    /// Convert a JSON value to a Cedar `RestrictedExpression`.
    ///
    /// Supports all Cedar primitive types, collections, and extension types.
    /// Null values are not supported and will return an error.
    pub fn json_to_cedar(
        &self,
        value: &Value,
    ) -> Result<Option<RestrictedExpression>, ValueMappingError> {
        // Check size limit
        if self.max_value_size > 0 {
            let size = self.estimate_value_size(value);
            if size > self.max_value_size {
                return Err(ValueMappingError::ValueTooLarge {
                    size,
                    limit: self.max_value_size,
                });
            }
        }

        self.convert_value(value)
    }

    /// Convert a JSON value to Cedar, returning the inferred Cedar type.
    ///
    /// This is useful when you need both the expression and type information.
    pub fn json_to_cedar_with_type(
        &self,
        value: &Value,
    ) -> Result<Option<(RestrictedExpression, CedarType)>, ValueMappingError> {
        let cedar_type = CedarType::from_value(value);
        let expr = self.json_to_cedar(value)?;
        Ok(expr.map(|e| (e, cedar_type)))
    }

    /// Convert a Cedar expression back to JSON format.
    ///
    /// This is useful for serializing Cedar values for storage or transmission.
    /// Entity references are converted to `{"type": "...", "id": "..."}` format.
    /// Extension types are converted to `{"__extn": {"fn": "...", "arg": "..."}}` format.
    ///
    /// # Note
    ///
    /// This method works with the JSON representation of Cedar values,
    /// not the evaluated result. For evaluated results, use `eval_result_to_json`.
    pub fn cedar_to_json(expr_json: &Value) -> Result<Value, ValueMappingError> {
        // Cedar's JSON format uses special markers for extension types and entities
        // This method normalizes those to a consistent format
        Self::normalize_cedar_json(expr_json)
    }

    /// Access a nested value using dot notation.
    pub fn get_nested<'a>(
        &self,
        value: &'a Value,
        path: &str,
    ) -> Result<&'a Value, ValueMappingError> {
        if path.is_empty() {
            return Ok(value);
        }

        let mut current = value;
        for component in path.split('.') {
            if component.is_empty() {
                return Err(ValueMappingError::InvalidPath {
                    path: path.to_string(),
                });
            }

            current = match current {
                Value::Object(obj) => {
                    obj.get(component)
                        .ok_or_else(|| ValueMappingError::PathNotFound {
                            path: path.to_string(),
                        })?
                },
                Value::Array(arr) => {
                    // Support numeric indexing for arrays
                    let index: usize =
                        component
                            .parse()
                            .map_err(|_| ValueMappingError::PathNotFound {
                                path: path.to_string(),
                            })?;
                    arr.get(index)
                        .ok_or_else(|| ValueMappingError::PathNotFound {
                            path: path.to_string(),
                        })?
                },
                _ => {
                    return Err(ValueMappingError::PathNotFound {
                        path: path.to_string(),
                    });
                },
            };
        }

        Ok(current)
    }

    /// Set a value at a nested path, creating intermediate objects as needed.
    pub fn set_nested(
        &self,
        value: &mut Value,
        path: &str,
        new_value: Value,
    ) -> Result<(), ValueMappingError> {
        if path.is_empty() {
            *value = new_value;
            return Ok(());
        }

        let components: Vec<&str> = path.split('.').collect();
        let mut current = value;

        for (i, component) in components.iter().enumerate() {
            if component.is_empty() {
                return Err(ValueMappingError::InvalidPath {
                    path: path.to_string(),
                });
            }

            let is_last = i == components.len() - 1;

            if is_last {
                // Set the value
                if let Value::Object(obj) = current {
                    obj.insert(component.to_string(), new_value);
                    return Ok(());
                } else {
                    return Err(ValueMappingError::TypeMismatch {
                        expected: "object".to_string(),
                        actual: Self::value_type_name(current).to_string(),
                    });
                }
            } else {
                // Navigate or create intermediate objects
                if let Value::Object(obj) = current {
                    current = obj
                        .entry(component.to_string())
                        .or_insert_with(|| Value::Object(Map::new()));
                } else {
                    return Err(ValueMappingError::TypeMismatch {
                        expected: "object".to_string(),
                        actual: Self::value_type_name(current).to_string(),
                    });
                }
            }
        }

        Ok(())
    }

    /// Detect if a string value represents a Cedar extension type.
    pub fn detect_extension(value: &str) -> Option<ExtensionValue> {
        // Check for IP address
        if IpAddr::from_str(value).is_ok() {
            return Some(ExtensionValue::IpAddr(value.to_string()));
        }

        // Check for decimal (must contain decimal point and be parseable as f64)
        if value.contains('.') {
            if let Ok(_) = value.parse::<f64>() {
                // Additional check: not just a number with trailing zeros
                if !value.ends_with('.') && value.chars().filter(|&c| c == '.').count() == 1 {
                    return Some(ExtensionValue::Decimal(value.to_string()));
                }
            }
        }

        None
    }

    /// Check if a value represents a Cedar entity reference.
    pub fn is_entity_reference(value: &Value) -> bool {
        if let Value::Object(obj) = value {
            obj.len() == 2
                && obj.get("type").map_or(false, |v| v.is_string())
                && obj.get("id").map_or(false, |v| v.is_string())
        } else {
            false
        }
    }

    /// Parse an entity reference from JSON.
    ///
    /// # Returns
    ///
    /// A tuple of (entity_type, entity_id) if valid.
    pub fn parse_entity_reference(value: &Value) -> Result<(String, String), ValueMappingError> {
        if let Value::Object(obj) = value {
            let entity_type = obj.get("type").and_then(|v| v.as_str()).ok_or_else(|| {
                ValueMappingError::InvalidEntityReference {
                    reason: "missing or invalid 'type' field".to_string(),
                }
            })?;

            let entity_id = obj.get("id").and_then(|v| v.as_str()).ok_or_else(|| {
                ValueMappingError::InvalidEntityReference {
                    reason: "missing or invalid 'id' field".to_string(),
                }
            })?;

            Ok((entity_type.to_string(), entity_id.to_string()))
        } else {
            Err(ValueMappingError::InvalidEntityReference {
                reason: "expected object with 'type' and 'id' fields".to_string(),
            })
        }
    }

    /// Get the JSON type name of a value.
    pub fn value_type_name(value: &Value) -> &'static str {
        match value {
            Value::Null => "null",
            Value::Bool(_) => "bool",
            Value::Number(_) => "number",
            Value::String(_) => "string",
            Value::Array(_) => "array",
            Value::Object(_) => "object",
        }
    }

    // Internal conversion method
    fn convert_value(
        &self,
        value: &Value,
    ) -> Result<Option<RestrictedExpression>, ValueMappingError> {
        let expr = match value {
            Value::Null => return Err(ValueMappingError::NullNotSupported),
            Value::Bool(b) => RestrictedExpression::new_bool(*b),
            Value::Number(n) => {
                if let Some(i) = n.as_i64() {
                    RestrictedExpression::new_long(i)
                } else if let Some(f) = n.as_f64() {
                    // Convert floating point to decimal extension
                    RestrictedExpression::new_decimal(f.to_string())
                } else {
                    return Ok(None);
                }
            },
            Value::String(s) => {
                if self.auto_detect_extensions {
                    match Self::detect_extension(s) {
                        Some(ExtensionValue::IpAddr(ip)) => RestrictedExpression::new_ip(ip),
                        Some(ExtensionValue::Decimal(d)) => RestrictedExpression::new_decimal(d),
                        None => RestrictedExpression::new_string(s.to_string()),
                    }
                } else {
                    RestrictedExpression::new_string(s.to_string())
                }
            },
            Value::Array(arr) => {
                let mut exprs = Vec::with_capacity(arr.len());
                let mut errors = Vec::new();

                for item in arr {
                    match self.convert_value(item) {
                        Ok(Some(expr)) => exprs.push(expr),
                        Ok(None) => {},
                        Err(e) => errors.push(e),
                    }
                }

                if !errors.is_empty() {
                    return Err(ValueMappingError::CollectionErrors(errors));
                }

                RestrictedExpression::new_set(exprs)
            },
            Value::Object(obj) => {
                // Check for entity reference
                if Self::is_entity_reference(value) {
                    let (entity_type, entity_id) = Self::parse_entity_reference(value)?;
                    let uid_str = format!("{}::\"{}\"", entity_type, entity_id);
                    let uid = cedar_policy::EntityUid::from_str(&uid_str).map_err(|e| {
                        ValueMappingError::InvalidEntityReference {
                            reason: e.to_string(),
                        }
                    })?;
                    return Ok(Some(RestrictedExpression::new_entity_uid(uid)));
                }

                // Check for extension type markers (__extn)
                if let Some(extn) = obj.get("__extn") {
                    if let Some(extn_obj) = extn.as_object() {
                        if let (Some(fn_name), Some(arg)) = (
                            extn_obj.get("fn").and_then(|v| v.as_str()),
                            extn_obj.get("arg").and_then(|v| v.as_str()),
                        ) {
                            return match fn_name {
                                "decimal" => Ok(Some(RestrictedExpression::new_decimal(arg))),
                                "ip" => Ok(Some(RestrictedExpression::new_ip(arg))),
                                _ => Err(ValueMappingError::InvalidExtensionFormat {
                                    extension_type: fn_name.to_string(),
                                    value: arg.to_string(),
                                }),
                            };
                        }
                    }
                }

                // Regular record
                let mut fields = HashMap::with_capacity(obj.len());
                let mut errors = Vec::new();

                for (key, val) in obj {
                    match self.convert_value(val) {
                        Ok(Some(expr)) => {
                            fields.insert(key.clone(), expr);
                        },
                        Ok(None) => {},
                        Err(e) => errors.push(e),
                    }
                }

                if !errors.is_empty() {
                    return Err(ValueMappingError::CollectionErrors(errors));
                }

                RestrictedExpression::new_record(fields)?
            },
        };

        Ok(Some(expr))
    }

    // Estimate the size of a value in bytes
    fn estimate_value_size(&self, value: &Value) -> usize {
        match value {
            Value::Null => 4,
            Value::Bool(_) => 5,
            Value::Number(n) => n.to_string().len(),
            Value::String(s) => s.len() + 2,
            Value::Array(arr) => {
                2 + arr
                    .iter()
                    .map(|v| self.estimate_value_size(v) + 1)
                    .sum::<usize>()
            },
            Value::Object(obj) => {
                2 + obj
                    .iter()
                    .map(|(k, v)| k.len() + 3 + self.estimate_value_size(v) + 1)
                    .sum::<usize>()
            },
        }
    }

    // Normalize Cedar JSON format to standard JSON
    fn normalize_cedar_json(value: &Value) -> Result<Value, ValueMappingError> {
        match value {
            Value::Object(obj) => {
                // Check for __entity marker (Cedar format for entity references)
                if let Some(entity) = obj.get("__entity") {
                    if let Some(entity_obj) = entity.as_object() {
                        return Ok(serde_json::json!({
                            "type": entity_obj.get("type"),
                            "id": entity_obj.get("id")
                        }));
                    }
                }

                // Check for __extn marker (extension types)
                if let Some(extn) = obj.get("__extn") {
                    return Ok(extn.clone());
                }

                // Regular object - recursively process
                let mut normalized = Map::new();
                for (key, val) in obj {
                    normalized.insert(key.clone(), Self::normalize_cedar_json(val)?);
                }
                Ok(Value::Object(normalized))
            },
            Value::Array(arr) => {
                let normalized: Result<Vec<_>, _> =
                    arr.iter().map(Self::normalize_cedar_json).collect();
                Ok(Value::Array(normalized?))
            },
            // Primitives pass through unchanged
            _ => Ok(value.clone()),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    #[test]
    fn test_json_to_cedar_primitives() {
        let mapper = CedarValueMapper::new();

        // Boolean
        let result = mapper.json_to_cedar(&json!(true));
        assert!(result.is_ok());
        assert!(result.unwrap().is_some());

        // Long
        let result = mapper.json_to_cedar(&json!(42));
        assert!(result.is_ok());
        assert!(result.unwrap().is_some());

        // String
        let result = mapper.json_to_cedar(&json!("hello"));
        assert!(result.is_ok());
        assert!(result.unwrap().is_some());
    }

    #[test]
    fn test_json_to_cedar_null_error() {
        let mapper = CedarValueMapper::new();
        let result = mapper.json_to_cedar(&json!(null));
        assert!(matches!(result, Err(ValueMappingError::NullNotSupported)));
    }

    #[test]
    fn test_json_to_cedar_collections() {
        let mapper = CedarValueMapper::new();

        // Set (array)
        let result = mapper.json_to_cedar(&json!([1, 2, 3]));
        assert!(result.is_ok());
        assert!(result.unwrap().is_some());

        // Record (object)
        let result = mapper.json_to_cedar(&json!({"name": "Alice", "age": 30}));
        assert!(result.is_ok());
        assert!(result.unwrap().is_some());
    }

    #[test]
    fn test_extension_detection_ipaddr() {
        // IPv4
        assert!(matches!(
            CedarValueMapper::detect_extension("192.168.1.1"),
            Some(ExtensionValue::IpAddr(_))
        ));

        // IPv6
        assert!(matches!(
            CedarValueMapper::detect_extension("::1"),
            Some(ExtensionValue::IpAddr(_))
        ));

        // Not an IP
        assert!(CedarValueMapper::detect_extension("hello").is_none());
    }

    #[test]
    fn test_extension_detection_decimal() {
        assert!(matches!(
            CedarValueMapper::detect_extension("3.14"),
            Some(ExtensionValue::Decimal(_))
        ));

        // Integer is not decimal
        assert!(CedarValueMapper::detect_extension("42").is_none());

        // Multiple dots is not decimal
        assert!(CedarValueMapper::detect_extension("1.2.3").is_none());
    }

    #[test]
    fn test_json_to_cedar_with_auto_detect() {
        let mapper = CedarValueMapper::new();

        // IP address should be detected
        let result = mapper.json_to_cedar(&json!("192.168.1.1"));
        assert!(result.is_ok());
    }

    #[test]
    fn test_json_to_cedar_without_auto_detect() {
        let mapper = CedarValueMapper::without_auto_detect();

        // IP address should be treated as string
        let result = mapper.json_to_cedar(&json!("192.168.1.1"));
        assert!(result.is_ok());
    }

    #[test]
    fn test_is_entity_reference() {
        assert!(CedarValueMapper::is_entity_reference(&json!({
            "type": "User",
            "id": "123"
        })));

        // Missing type
        assert!(!CedarValueMapper::is_entity_reference(&json!({
            "id": "123"
        })));

        // Extra field
        assert!(!CedarValueMapper::is_entity_reference(&json!({
            "type": "User",
            "id": "123",
            "extra": true
        })));

        // Wrong types
        assert!(!CedarValueMapper::is_entity_reference(&json!({
            "type": 123,
            "id": "123"
        })));
    }

    #[test]
    fn test_parse_entity_reference() {
        let value = json!({"type": "User", "id": "alice"});
        let result = CedarValueMapper::parse_entity_reference(&value);
        assert!(result.is_ok());
        let (entity_type, entity_id) = result.expect("should parse");
        assert_eq!(entity_type, "User");
        assert_eq!(entity_id, "alice");
    }

    #[test]
    fn test_dot_notation_access() {
        let mapper = CedarValueMapper::new();
        let data = json!({
            "user": {
                "profile": {
                    "name": "Alice",
                    "age": 30
                }
            }
        });

        // Valid paths
        let name = mapper.get_nested(&data, "user.profile.name");
        assert!(name.is_ok());
        assert_eq!(name.unwrap(), &json!("Alice"));

        let age = mapper.get_nested(&data, "user.profile.age");
        assert!(age.is_ok());
        assert_eq!(age.unwrap(), &json!(30));

        // Invalid path
        let missing = mapper.get_nested(&data, "user.missing.field");
        assert!(matches!(
            missing,
            Err(ValueMappingError::PathNotFound { .. })
        ));
    }

    #[test]
    fn test_dot_notation_array_access() {
        let mapper = CedarValueMapper::new();
        let data = json!({
            "items": ["a", "b", "c"]
        });

        let item = mapper.get_nested(&data, "items.1");
        assert!(item.is_ok());
        assert_eq!(item.unwrap(), &json!("b"));
    }

    #[test]
    fn test_set_nested() {
        let mapper = CedarValueMapper::new();
        let mut data = json!({});

        mapper
            .set_nested(&mut data, "user.profile.name", json!("Alice"))
            .expect("should set nested value");

        assert_eq!(data, json!({"user": {"profile": {"name": "Alice"}}}));
    }

    #[test]
    fn test_value_size_limit() {
        let mapper = CedarValueMapper::new().with_max_size(10);

        // Small value should pass
        let result = mapper.json_to_cedar(&json!("hi"));
        assert!(result.is_ok());

        // Large value should fail
        let result = mapper.json_to_cedar(&json!("this is a very long string"));
        assert!(matches!(
            result,
            Err(ValueMappingError::ValueTooLarge { .. })
        ));
    }

    #[test]
    fn test_explicit_extension_marker() {
        let mapper = CedarValueMapper::new();

        // Decimal with explicit marker
        let decimal = json!({"__extn": {"fn": "decimal", "arg": "3.14159"}});
        let result = mapper.json_to_cedar(&decimal);
        assert!(result.is_ok());

        // IP with explicit marker
        let ip = json!({"__extn": {"fn": "ip", "arg": "10.0.0.1"}});
        let result = mapper.json_to_cedar(&ip);
        assert!(result.is_ok());
    }

    #[test]
    fn test_json_to_cedar_with_type() {
        let mapper = CedarValueMapper::new();

        let result = mapper.json_to_cedar_with_type(&json!("hello"));
        assert!(result.is_ok());
        let (_, cedar_type) = result.expect("should convert").expect("should have value");
        assert_eq!(cedar_type, CedarType::String);

        let result = mapper.json_to_cedar_with_type(&json!(42));
        assert!(result.is_ok());
        let (_, cedar_type) = result.expect("should convert").expect("should have value");
        assert_eq!(cedar_type, CedarType::Long);

        let result = mapper.json_to_cedar_with_type(&json!({"a": 1}));
        assert!(result.is_ok());
        let (_, cedar_type) = result.expect("should convert").expect("should have value");
        assert_eq!(cedar_type, CedarType::Record);
    }

    #[test]
    fn test_nested_structures() {
        let mapper = CedarValueMapper::new();

        let complex = json!({
            "user": {
                "name": "Alice",
                "roles": ["admin", "user"],
                "profile": {
                    "age": 30,
                    "verified": true
                }
            },
            "metadata": {
                "version": 1
            }
        });

        let result = mapper.json_to_cedar(&complex);
        assert!(result.is_ok());
        assert!(result.unwrap().is_some());
    }
}
