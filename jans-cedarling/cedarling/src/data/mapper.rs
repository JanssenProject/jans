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

/// Represents a parsed Cedar entity reference.
#[derive(Debug, Clone, PartialEq)]
pub(super) struct EntityReference {
    /// The entity type (e.g., "User", "Namespace::Type")
    pub entity_type: String,
    /// The entity identifier
    pub entity_id: String,
}

/// Represents a detected extension type with its parsed value.
#[derive(Debug, Clone, PartialEq)]
pub enum ExtensionValue {
    /// An IP address (IPv4 or IPv6) or CIDR range
    IpAddr(String),
    /// A fixed-precision decimal number (up to 4 decimal places)
    Decimal(String),
    /// An instant of time with millisecond precision (RFC 3339 / ISO 8601)
    DateTime(String),
    /// A duration of time with millisecond precision
    Duration(String),
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
    pub fn new_without_auto_detect() -> Self {
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
    ///
    /// Detects the following extension types:
    /// - `ipaddr`: IP addresses (IPv4/IPv6) and CIDR ranges (e.g., "192.168.1.1", "10.0.0.0/8")
    /// - `decimal`: Fixed-precision decimals (e.g., "3.14", "-12.345")
    /// - `datetime`: ISO 8601 / RFC 3339 timestamps (e.g., "2024-10-15T11:35:00Z")
    /// - `duration`: Duration strings (e.g., "2h30m", "1d12h", "500ms")
    ///
    /// See: <https://docs.cedarpolicy.com/policies/syntax-datatypes.html#datatype-extension>
    pub fn detect_extension(value: &str) -> Option<ExtensionValue> {
        // Check for plain IP address (IPv4 or IPv6)
        if IpAddr::from_str(value).is_ok() {
            return Some(ExtensionValue::IpAddr(value.to_string()));
        }

        // Check for CIDR notation (e.g., "192.168.1.0/24", "fe80::/10")
        if let Some((ip_part, prefix_part)) = value.split_once('/') {
            if let Ok(ip) = IpAddr::from_str(ip_part) {
                if let Ok(prefix_len) = prefix_part.parse::<u8>() {
                    // Validate prefix length: 0-32 for IPv4, 0-128 for IPv6
                    let max_prefix = if ip.is_ipv4() { 32 } else { 128 };
                    if prefix_len <= max_prefix {
                        return Some(ExtensionValue::IpAddr(value.to_string()));
                    }
                }
            }
        }

        // Check for datetime (ISO 8601 / RFC 3339 format)
        // Examples: "2024-10-15", "2024-10-15T11:35:00Z", "2024-10-15T11:35:00.000+0100"
        if Self::is_datetime_format(value) {
            return Some(ExtensionValue::DateTime(value.to_string()));
        }

        // Check for duration format (e.g., "2h30m", "-1d12h", "500ms")
        if Self::is_duration_format(value) {
            return Some(ExtensionValue::Duration(value.to_string()));
        }

        // Check for decimal (must contain decimal point and be parseable as f64)
        // Must have exactly one decimal point and not end with it
        if value.contains('.') {
            if value.parse::<f64>().is_ok()
                && !value.ends_with('.')
                && value.chars().filter(|&c| c == '.').count() == 1
            {
                return Some(ExtensionValue::Decimal(value.to_string()));
            }
        }

        None
    }

    /// Check if a string looks like an ISO 8601 / RFC 3339 datetime.
    ///
    /// Supported formats:
    /// - "2024-10-15" (date only)
    /// - "2024-10-15T11:35:00Z" (UTC)
    /// - "2024-10-15T11:35:00.000Z" (UTC with milliseconds)
    /// - "2024-10-15T11:35:00+0100" (with timezone offset)
    /// - "2024-10-15T11:35:00.000+0100" (with timezone and milliseconds)
    fn is_datetime_format(value: &str) -> bool {
        // Quick length check - datetime strings are typically 10-29 chars
        if value.len() < 10 || value.len() > 35 {
            return false;
        }

        // Must start with a 4-digit year
        let bytes = value.as_bytes();
        if bytes.len() < 10 {
            return false;
        }

        // Check YYYY-MM-DD pattern
        if !bytes[0..4].iter().all(|b| b.is_ascii_digit())
            || bytes[4] != b'-'
            || !bytes[5..7].iter().all(|b| b.is_ascii_digit())
            || bytes[7] != b'-'
            || !bytes[8..10].iter().all(|b| b.is_ascii_digit())
        {
            return false;
        }

        // Date only format
        if value.len() == 10 {
            return true;
        }

        // Must have 'T' separator for datetime
        if bytes.len() > 10 && bytes[10] != b'T' {
            return false;
        }

        // Check for time portion (HH:MM:SS)
        if bytes.len() >= 19 {
            if !bytes[11..13].iter().all(|b| b.is_ascii_digit())
                || bytes[13] != b':'
                || !bytes[14..16].iter().all(|b| b.is_ascii_digit())
                || bytes[16] != b':'
                || !bytes[17..19].iter().all(|b| b.is_ascii_digit())
            {
                return false;
            }
        }

        // Accept various valid suffixes (Z, +HHMM, -HHMM, .sss, etc.)
        true
    }

    /// Check if a string looks like a Cedar duration format.
    ///
    /// Supported formats:
    /// - "2h30m" (hours and minutes)
    /// - "-1d12h" (negative, days and hours)
    /// - "1h30m45s" (hours, minutes, seconds)
    /// - "500ms" (milliseconds only)
    /// - "1d" (days only)
    fn is_duration_format(value: &str) -> bool {
        if value.is_empty() {
            return false;
        }

        let s = if value.starts_with('-') {
            &value[1..]
        } else {
            value
        };

        if s.is_empty() {
            return false;
        }

        // Duration must contain at least one unit suffix (d, h, m, s, ms)
        // and consist of digits followed by unit suffixes
        let has_unit = s.contains('d')
            || s.contains('h')
            || s.ends_with('m')
            || s.ends_with('s')
            || s.ends_with("ms");

        if !has_unit {
            return false;
        }

        // Check that it follows the pattern: digits followed by units, repeated
        // Valid: "1d2h3m4s", "500ms", "2h30m"
        // Invalid: "abc", "1.5h" (no decimals in duration)
        let mut chars = s.chars().peekable();
        let mut has_digits = false;

        while let Some(c) = chars.next() {
            if c.is_ascii_digit() {
                has_digits = true;
            } else if c == 'd' || c == 'h' || c == 's' {
                if !has_digits {
                    return false;
                }
                has_digits = false;
            } else if c == 'm' {
                if !has_digits {
                    return false;
                }
                // Could be 'm' (minutes) or 'ms' (milliseconds)
                if chars.peek() == Some(&'s') {
                    chars.next(); // consume 's'
                }
                has_digits = false;
            } else {
                // Invalid character
                return false;
            }
        }

        true
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
    pub(super) fn parse_entity_reference(
        value: &Value,
    ) -> Result<EntityReference, ValueMappingError> {
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

            Ok(EntityReference {
                entity_type: entity_type.to_string(),
                entity_id: entity_id.to_string(),
            })
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
                    return Err(ValueMappingError::NumberNotRepresentable {
                        value: n.to_string(),
                    });
                }
            },
            Value::String(s) => {
                if self.auto_detect_extensions {
                    match Self::detect_extension(s) {
                        Some(ExtensionValue::IpAddr(ip)) => RestrictedExpression::new_ip(ip),
                        Some(ExtensionValue::Decimal(d)) => RestrictedExpression::new_decimal(d),
                        Some(ExtensionValue::DateTime(dt)) => {
                            RestrictedExpression::new_datetime(dt)
                        },
                        Some(ExtensionValue::Duration(dur)) => {
                            RestrictedExpression::new_duration(dur)
                        },
                        None => RestrictedExpression::new_string(s.to_string()),
                    }
                } else {
                    RestrictedExpression::new_string(s.to_string())
                }
            },
            Value::Array(arr) => {
                let mut exprs = Vec::with_capacity(arr.len());

                for item in arr {
                    let expr = self.convert_value(item)?;
                    // All code paths in convert_value now return Some or Err,
                    // so unwrap is safe here
                    exprs.push(expr.expect("convert_value should always return Some"));
                }

                RestrictedExpression::new_set(exprs)
            },
            Value::Object(obj) => {
                // Check for entity reference
                if Self::is_entity_reference(value) {
                    let entity_ref = Self::parse_entity_reference(value)?;
                    let uid_str =
                        format!("{}::\"{}\"", entity_ref.entity_type, entity_ref.entity_id);
                    let uid = cedar_policy::EntityUid::from_str(&uid_str).map_err(|e| {
                        ValueMappingError::InvalidEntityReference {
                            reason: e.to_string(),
                        }
                    })?;
                    return Ok(Some(RestrictedExpression::new_entity_uid(uid)));
                }

                // Check for extension type markers (__extn)
                if let Some(extn) = obj.get("__extn") {
                    // __extn is present, so we must validate it strictly
                    let extn_obj = extn.as_object().ok_or_else(|| {
                        ValueMappingError::InvalidExtensionFormat {
                            extension_type: "__extn".to_string(),
                            value: extn.to_string(),
                        }
                    })?;

                    let fn_name = extn_obj.get("fn").and_then(|v| v.as_str()).ok_or_else(|| {
                        ValueMappingError::InvalidExtensionFormat {
                            extension_type: "__extn".to_string(),
                            value: format!(
                                "missing or invalid 'fn' field in {}",
                                serde_json::to_string(extn_obj).unwrap_or_default()
                            ),
                        }
                    })?;

                    let arg = extn_obj
                        .get("arg")
                        .and_then(|v| v.as_str())
                        .ok_or_else(|| ValueMappingError::InvalidExtensionFormat {
                            extension_type: fn_name.to_string(),
                            value: format!(
                                "missing or invalid 'arg' field in {}",
                                serde_json::to_string(extn_obj).unwrap_or_default()
                            ),
                        })?;

                    return match fn_name {
                        "decimal" => Ok(Some(RestrictedExpression::new_decimal(arg))),
                        "ip" | "ipaddr" => Ok(Some(RestrictedExpression::new_ip(arg))),
                        "datetime" => Ok(Some(RestrictedExpression::new_datetime(arg))),
                        "duration" => Ok(Some(RestrictedExpression::new_duration(arg))),
                        _ => Err(ValueMappingError::InvalidExtensionFormat {
                            extension_type: fn_name.to_string(),
                            value: arg.to_string(),
                        }),
                    };
                }

                // Regular record
                let mut fields = HashMap::with_capacity(obj.len());

                for (key, val) in obj {
                    let expr = self.convert_value(val)?;
                    // All code paths in convert_value now return Some or Err,
                    // so unwrap is safe here
                    fields.insert(
                        key.clone(),
                        expr.expect("convert_value should always return Some"),
                    );
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
                // Preserve the entire wrapper so json_to_cedar can consume it
                if obj.contains_key("__extn") {
                    return Ok(Value::Object(obj.clone()));
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
    use test_utils::assert_eq;

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

        // IPv4 CIDR notation
        assert!(matches!(
            CedarValueMapper::detect_extension("10.0.0.0/8"),
            Some(ExtensionValue::IpAddr(_))
        ));
        assert!(matches!(
            CedarValueMapper::detect_extension("192.168.1.0/24"),
            Some(ExtensionValue::IpAddr(_))
        ));

        // IPv6 CIDR notation
        assert!(matches!(
            CedarValueMapper::detect_extension("fe80::/10"),
            Some(ExtensionValue::IpAddr(_))
        ));
        assert!(matches!(
            CedarValueMapper::detect_extension("2001:db8::/32"),
            Some(ExtensionValue::IpAddr(_))
        ));

        // Invalid CIDR prefix (too large for IPv4)
        assert!(CedarValueMapper::detect_extension("192.168.1.0/33").is_none());

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

        // Multiple dots is not decimal (would be detected as IP first if valid)
        assert!(CedarValueMapper::detect_extension("1.2.3.4.5").is_none());
    }

    #[test]
    fn test_extension_detection_datetime() {
        // Date only
        assert!(matches!(
            CedarValueMapper::detect_extension("2024-10-15"),
            Some(ExtensionValue::DateTime(_))
        ));

        // UTC datetime
        assert!(matches!(
            CedarValueMapper::detect_extension("2024-10-15T11:35:00Z"),
            Some(ExtensionValue::DateTime(_))
        ));

        // UTC with milliseconds
        assert!(matches!(
            CedarValueMapper::detect_extension("2024-10-15T11:35:00.000Z"),
            Some(ExtensionValue::DateTime(_))
        ));

        // With timezone offset
        assert!(matches!(
            CedarValueMapper::detect_extension("2024-10-15T11:35:00+0100"),
            Some(ExtensionValue::DateTime(_))
        ));

        // Invalid datetime
        assert!(!matches!(
            CedarValueMapper::detect_extension("not-a-date"),
            Some(ExtensionValue::DateTime(_))
        ));
    }

    #[test]
    fn test_extension_detection_duration() {
        // Hours and minutes
        assert!(matches!(
            CedarValueMapper::detect_extension("2h30m"),
            Some(ExtensionValue::Duration(_))
        ));

        // Negative duration
        assert!(matches!(
            CedarValueMapper::detect_extension("-1d12h"),
            Some(ExtensionValue::Duration(_))
        ));

        // Hours, minutes, seconds
        assert!(matches!(
            CedarValueMapper::detect_extension("1h30m45s"),
            Some(ExtensionValue::Duration(_))
        ));

        // Milliseconds only
        assert!(matches!(
            CedarValueMapper::detect_extension("500ms"),
            Some(ExtensionValue::Duration(_))
        ));

        // Days only
        assert!(matches!(
            CedarValueMapper::detect_extension("1d"),
            Some(ExtensionValue::Duration(_))
        ));

        // Invalid duration
        assert!(!matches!(
            CedarValueMapper::detect_extension("not-a-duration"),
            Some(ExtensionValue::Duration(_))
        ));
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
        let mapper = CedarValueMapper::new_without_auto_detect();

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
        let entity_ref = result.expect("should parse");
        assert_eq!(entity_ref.entity_type, "User");
        assert_eq!(entity_ref.entity_id, "alice");
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
        assert!(result.is_ok(), "decimal extension should parse");

        // IP with explicit marker
        let ip = json!({"__extn": {"fn": "ip", "arg": "10.0.0.1"}});
        let result = mapper.json_to_cedar(&ip);
        assert!(result.is_ok(), "ip extension should parse");

        // IP CIDR with explicit marker
        let ip_cidr = json!({"__extn": {"fn": "ip", "arg": "192.168.0.0/16"}});
        let result = mapper.json_to_cedar(&ip_cidr);
        assert!(result.is_ok(), "ip CIDR extension should parse");

        // Datetime with explicit marker
        let datetime = json!({"__extn": {"fn": "datetime", "arg": "2024-10-15T11:35:00Z"}});
        let result = mapper.json_to_cedar(&datetime);
        assert!(result.is_ok(), "datetime extension should parse");

        // Duration with explicit marker
        let duration = json!({"__extn": {"fn": "duration", "arg": "2h30m"}});
        let result = mapper.json_to_cedar(&duration);
        assert!(result.is_ok(), "duration extension should parse");
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
