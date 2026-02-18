// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Schema-Aware Data Validation
//!
//! Provides validation of JSON data against Cedar schema constraints,
//! ensuring type safety and data integrity before values are pushed
//! to the `DataStore` or used in policy evaluation.

use crate::ValidationError;

use super::{CedarType, CedarValueMapper, ExtensionValue};
use serde_json::Value;
use std::borrow::Cow;
use std::net::IpAddr;
use std::str::FromStr;

/// Maximum default nesting depth for validated values
const DEFAULT_MAX_DEPTH: usize = 32;

/// Maximum default string length
const DEFAULT_MAX_STRING_LENGTH: usize = 1_048_576; // 1MB

/// Maximum default array length
const DEFAULT_MAX_ARRAY_LENGTH: usize = 10_000;

/// Maximum default object keys
const DEFAULT_MAX_OBJECT_KEYS: usize = 1_000;

/// Result of validation with optional collected errors.
#[derive(Debug)]
pub struct ValidationResult {
    /// Whether validation passed
    pub is_valid: bool,
    /// Collected errors (if any)
    pub errors: Vec<ValidationError>,
    /// Warnings that don't prevent validation
    pub warnings: Vec<String>,
}

impl ValidationResult {
    /// Create a successful result
    #[must_use]
    pub fn ok() -> Self {
        Self {
            is_valid: true,
            errors: Vec::new(),
            warnings: Vec::new(),
        }
    }

    /// Create a failed result with a single error
    #[must_use]
    pub fn error(err: ValidationError) -> Self {
        Self {
            is_valid: false,
            errors: vec![err],
            warnings: Vec::new(),
        }
    }

    /// Create a failed result with multiple errors
    #[must_use]
    pub fn errors(errs: Vec<ValidationError>) -> Self {
        Self {
            is_valid: errs.is_empty(),
            errors: errs,
            warnings: Vec::new(),
        }
    }

    /// Add a warning
    #[must_use]
    pub fn with_warning(mut self, warning: String) -> Self {
        self.warnings.push(warning);
        self
    }

    /// Convert to Result type.
    ///
    /// Returns `Ok(())` if validation passed, or an error with validation failures.
    /// When there's exactly one error, it returns that error directly.
    /// When there are multiple errors, they are wrapped in `ValidationError::Multiple`.
    ///
    /// # Panics
    ///
    /// This function will not panic. The `expect` call is guarded by a length check.
    pub fn into_result(mut self) -> Result<(), ValidationError> {
        if self.is_valid {
            Ok(())
        } else if self.errors.len() == 1 {
            // pop() is safe here since we verified len() == 1
            Err(self.errors.pop().expect("errors has exactly one element"))
        } else {
            Err(ValidationError::Multiple(self.errors))
        }
    }
}

/// Configuration for validation constraints.
#[derive(Debug, Clone)]
pub struct ValidationConfig {
    /// Maximum nesting depth
    pub max_depth: usize,
    /// Maximum string length in characters
    pub max_string_length: usize,
    /// Maximum array length
    pub max_array_length: usize,
    /// Maximum number of keys in an object
    pub max_object_keys: usize,
    /// Whether to validate extension type formats strictly
    pub strict_extension_validation: bool,
    /// Whether to collect all errors or fail fast
    pub collect_all_errors: bool,
}

impl Default for ValidationConfig {
    fn default() -> Self {
        Self {
            max_depth: DEFAULT_MAX_DEPTH,
            max_string_length: DEFAULT_MAX_STRING_LENGTH,
            max_array_length: DEFAULT_MAX_ARRAY_LENGTH,
            max_object_keys: DEFAULT_MAX_OBJECT_KEYS,
            strict_extension_validation: true,
            collect_all_errors: false,
        }
    }
}

/// Validator for JSON data against Cedar type constraints.
///
/// Provides comprehensive validation including type checking, size limits,
/// and format validation for extension types.
#[derive(Debug, Clone)]
pub struct DataValidator {
    config: ValidationConfig,
}

impl Default for DataValidator {
    fn default() -> Self {
        Self::new()
    }
}

impl DataValidator {
    /// Create a new validator with default configuration.
    #[must_use]
    pub fn new() -> Self {
        Self {
            config: ValidationConfig::default(),
        }
    }

    /// Create a validator with custom configuration.
    #[must_use]
    pub fn with_config(config: ValidationConfig) -> Self {
        Self { config }
    }

    /// Set maximum nesting depth.
    #[must_use]
    pub fn with_max_depth(mut self, max_depth: usize) -> Self {
        self.config.max_depth = max_depth;
        self
    }

    /// Set maximum string length.
    #[must_use]
    pub fn with_max_string_length(mut self, max_length: usize) -> Self {
        self.config.max_string_length = max_length;
        self
    }

    /// Set maximum array length.
    #[must_use]
    pub fn with_max_array_length(mut self, max_length: usize) -> Self {
        self.config.max_array_length = max_length;
        self
    }

    /// Set maximum object keys.
    #[must_use]
    pub fn with_max_object_keys(mut self, max_keys: usize) -> Self {
        self.config.max_object_keys = max_keys;
        self
    }

    /// Enable strict extension format validation.
    #[must_use]
    pub fn with_strict_extensions(mut self, strict: bool) -> Self {
        self.config.strict_extension_validation = strict;
        self
    }

    /// Enable collecting all errors instead of failing fast.
    #[must_use]
    pub fn collect_all_errors(mut self, collect: bool) -> Self {
        self.config.collect_all_errors = collect;
        self
    }

    /// Validate a JSON value for Cedar compatibility.
    ///
    /// If `collect_all_errors` is set in the config, this will collect all
    /// errors before returning. Otherwise, it fails fast on the first error.
    ///
    /// # Panics
    ///
    /// This function will not panic. The `expect` call is guarded by a length check.
    pub fn validate(&self, value: &Value) -> Result<(), ValidationError> {
        if self.config.collect_all_errors {
            let mut errors = Vec::new();
            self.validate_collecting(value, Cow::Borrowed("$"), 0, &mut errors);
            if errors.is_empty() {
                Ok(())
            } else if errors.len() == 1 {
                // pop() is safe here since we verified len() == 1
                Err(errors.pop().expect("errors has exactly one element"))
            } else {
                Err(ValidationError::Multiple(errors))
            }
        } else {
            self.validate_at_path(value, Cow::Borrowed("$"), 0)?;
            Ok(())
        }
    }

    /// Validate a value and get a detailed result.
    #[must_use]
    pub fn validate_detailed(&self, value: &Value) -> ValidationResult {
        let mut errors = Vec::new();
        self.validate_collecting(value, Cow::Borrowed("$"), 0, &mut errors);
        ValidationResult::errors(errors)
    }

    /// Validate that a value matches an expected Cedar type.
    pub fn validate_type(&self, value: &Value, expected: CedarType) -> Result<(), ValidationError> {
        let actual = CedarType::from_value(value);
        if actual == expected {
            Ok(())
        } else {
            Err(ValidationError::TypeMismatch {
                path: "$".to_string(),
                expected: format!("{expected:?}"),
                actual: format!("{actual:?}"),
            })
        }
    }

    /// Validate an extension value format.
    ///
    /// Supports all Cedar extension types:
    /// - `ipaddr` / `ip`: IP addresses and CIDR ranges
    /// - `decimal`: Fixed-precision decimal numbers
    /// - `datetime`: ISO 8601 / RFC 3339 timestamps
    /// - `duration`: Duration strings (e.g., "2h30m")
    pub fn validate_extension(
        &self,
        value: &str,
        extension_type: &str,
    ) -> Result<(), ValidationError> {
        match extension_type {
            "ipaddr" | "ip" => {
                // Check for plain IP address
                if IpAddr::from_str(value).is_ok() {
                    return Ok(());
                }
                // Check for CIDR notation
                if let Some((ip_part, prefix_part)) = value.split_once('/')
                    && let Ok(ip) = IpAddr::from_str(ip_part)
                    && let Ok(prefix_len) = prefix_part.parse::<u8>()
                {
                    let max_prefix = if ip.is_ipv4() { 32 } else { 128 };
                    if prefix_len <= max_prefix {
                        return Ok(());
                    }
                }
                Err(ValidationError::InvalidExtensionFormat {
                    path: "$".to_string(),
                    extension_type: extension_type.to_string(),
                    value: value.to_string(),
                })
            },
            "decimal" => {
                if value.parse::<f64>().is_err() {
                    return Err(ValidationError::InvalidExtensionFormat {
                        path: "$".to_string(),
                        extension_type: extension_type.to_string(),
                        value: value.to_string(),
                    });
                }
                Ok(())
            },
            "datetime" => {
                // Validate datetime format (ISO 8601 / RFC 3339)
                if !Self::is_valid_datetime(value) {
                    return Err(ValidationError::InvalidExtensionFormat {
                        path: "$".to_string(),
                        extension_type: extension_type.to_string(),
                        value: value.to_string(),
                    });
                }
                Ok(())
            },
            "duration" => {
                // Validate duration format
                if !Self::is_valid_duration(value) {
                    return Err(ValidationError::InvalidExtensionFormat {
                        path: "$".to_string(),
                        extension_type: extension_type.to_string(),
                        value: value.to_string(),
                    });
                }
                Ok(())
            },
            _ => {
                // Unknown extension type - allow if not strict
                if self.config.strict_extension_validation {
                    return Err(ValidationError::InvalidExtensionFormat {
                        path: "$".to_string(),
                        extension_type: extension_type.to_string(),
                        value: value.to_string(),
                    });
                }
                Ok(())
            },
        }
    }

    /// Sanitize a value by removing null fields and invalid characters.
    ///
    /// Returns a new value with:
    /// - Null values removed from objects
    /// - Null values in arrays replaced with empty strings
    /// - Control characters removed from strings
    #[must_use]
    pub fn sanitize(&self, value: &Value) -> Value {
        sanitize_value_recursive(value)
    }

    /// Validates a JSON value at a specific path and depth.
    fn validate_at_path(
        &self,
        value: &Value,
        path: Cow<'_, str>,
        depth: usize,
    ) -> Result<(), ValidationError> {
        if depth > self.config.max_depth {
            return Err(ValidationError::MaxDepthExceeded {
                path: path.into_owned(),
                max: self.config.max_depth,
            });
        }

        match value {
            Value::Null => Err(ValidationError::NullNotSupported {
                path: path.into_owned(),
            }),
            Value::Bool(_) => Ok(()),
            Value::Number(n) if n.is_i64() || n.is_u64() || n.is_f64() => Ok(()),
            Value::Number(_) => Err(ValidationError::NumberOutOfRange {
                path: path.into_owned(),
            }),
            Value::String(s) => self.validate_string(s, path),
            Value::Array(arr) => self.validate_array(arr, path, depth),
            Value::Object(obj) => self.validate_object(value, obj, path, depth),
        }
    }

    /// Validates a string value, including extension type detection.
    #[inline]
    fn validate_string(&self, s: &str, path: Cow<'_, str>) -> Result<(), ValidationError> {
        if s.len() > self.config.max_string_length {
            return Err(ValidationError::StringTooLong {
                path: path.into_owned(),
                length: s.len(),
                max: self.config.max_string_length,
            });
        }

        if self.config.strict_extension_validation
            && let Some(ref ext) = CedarValueMapper::detect_extension(s)
        {
            self.validate_detected_extension(ext, path)?;
        }
        Ok(())
    }

    /// Validates a detected extension value.
    #[inline]
    fn validate_detected_extension(
        &self,
        ext: &ExtensionValue,
        path: Cow<'_, str>,
    ) -> Result<(), ValidationError> {
        let (ext_type, value, is_valid) = match ext {
            ExtensionValue::IpAddr(v) => (
                "ipaddr",
                v.clone(),
                self.validate_extension(v, "ip").is_ok(),
            ),
            ExtensionValue::Decimal(v) => ("decimal", v.clone(), v.parse::<f64>().is_ok()),
            ExtensionValue::DateTime(v) => ("datetime", v.clone(), Self::is_valid_datetime(v)),
            ExtensionValue::Duration(v) => ("duration", v.clone(), Self::is_valid_duration(v)),
        };

        if is_valid {
            Ok(())
        } else {
            Err(ValidationError::InvalidExtensionFormat {
                path: path.into_owned(),
                extension_type: ext_type.to_string(),
                value,
            })
        }
    }

    /// Validates an array value.
    #[inline]
    fn validate_array(
        &self,
        arr: &[Value],
        path: Cow<'_, str>,
        depth: usize,
    ) -> Result<(), ValidationError> {
        if arr.len() > self.config.max_array_length {
            return Err(ValidationError::ArrayTooLong {
                path: path.into_owned(),
                length: arr.len(),
                max: self.config.max_array_length,
            });
        }

        for (i, item) in arr.iter().enumerate() {
            let item_path = Cow::Owned(format!("{path}[{i}]"));
            self.validate_at_path(item, item_path, depth + 1)?;
        }
        Ok(())
    }

    /// Validates an object value.
    #[inline]
    fn validate_object(
        &self,
        value: &Value,
        obj: &serde_json::Map<String, Value>,
        path: Cow<'_, str>,
        depth: usize,
    ) -> Result<(), ValidationError> {
        if obj.len() > self.config.max_object_keys {
            return Err(ValidationError::TooManyKeys {
                path: path.into_owned(),
                count: obj.len(),
                max: self.config.max_object_keys,
            });
        }

        // Fast path for special object types
        if CedarValueMapper::is_entity_reference(value) {
            return Self::validate_entity_reference(value, path.as_ref());
        }
        if let Some(extn) = obj.get("__extn") {
            return self.validate_extension_marker(extn, path.as_ref());
        }

        // Validate keys and values
        for (key, val) in obj {
            Self::validate_key(key, path.as_ref())?;
            let field_path = Cow::Owned(format!("{path}.{key}"));
            self.validate_at_path(val, field_path, depth + 1)?;
        }
        Ok(())
    }

    /// Validates an object key.
    #[inline]
    fn validate_key(key: &str, path: &str) -> Result<(), ValidationError> {
        if key.is_empty() {
            return Err(ValidationError::InvalidKey {
                path: path.to_string(),
                reason: "empty key".to_string(),
            });
        }
        if key.chars().any(char::is_control) {
            return Err(ValidationError::InvalidKey {
                path: path.to_string(),
                reason: "key contains control characters".to_string(),
            });
        }
        Ok(())
    }

    // Collect all errors instead of failing fast
    fn validate_collecting(
        &self,
        value: &Value,
        path: Cow<'_, str>,
        depth: usize,
        errors: &mut Vec<ValidationError>,
    ) {
        if depth > self.config.max_depth {
            errors.push(ValidationError::MaxDepthExceeded {
                path: path.into_owned(),
                max: self.config.max_depth,
            });
            return;
        }

        match value {
            Value::Null => {
                errors.push(ValidationError::NullNotSupported {
                    path: path.into_owned(),
                });
            },
            Value::Bool(_) => {},
            Value::Number(n) => {
                if !n.is_i64() && !n.is_u64() && !n.is_f64() {
                    errors.push(ValidationError::NumberOutOfRange {
                        path: path.into_owned(),
                    });
                }
            },
            Value::String(s) => self.validate_string_collecting(s, path.as_ref(), errors),
            Value::Array(arr) => self.validate_array_collecting(arr, path.as_ref(), depth, errors),
            Value::Object(obj) => {
                self.validate_object_collecting(value, obj, path.as_ref(), depth, errors);
            },
        }
    }

    /// Validate a string value, collecting all errors.
    fn validate_string_collecting(&self, s: &str, path: &str, errors: &mut Vec<ValidationError>) {
        if s.len() > self.config.max_string_length {
            errors.push(ValidationError::StringTooLong {
                path: path.to_string(),
                length: s.len(),
                max: self.config.max_string_length,
            });
        }

        // If it looks like an extension type, validate its format
        if self.config.strict_extension_validation
            && let Some(ext) = CedarValueMapper::detect_extension(s)
            && let Some(err) = self.check_extension_format(&ext, path)
        {
            errors.push(err);
        }
    }

    /// Check extension format and return an error if invalid.
    fn check_extension_format(&self, ext: &ExtensionValue, path: &str) -> Option<ValidationError> {
        match ext {
            ExtensionValue::IpAddr(ip) if self.validate_extension(ip, "ip").is_err() => {
                Some(ValidationError::InvalidExtensionFormat {
                    path: path.to_string(),
                    extension_type: "ipaddr".to_string(),
                    value: ip.clone(),
                })
            },
            ExtensionValue::Decimal(d) if d.parse::<f64>().is_err() => {
                Some(ValidationError::InvalidExtensionFormat {
                    path: path.to_string(),
                    extension_type: "decimal".to_string(),
                    value: d.clone(),
                })
            },
            ExtensionValue::DateTime(dt) if !Self::is_valid_datetime(dt) => {
                Some(ValidationError::InvalidExtensionFormat {
                    path: path.to_string(),
                    extension_type: "datetime".to_string(),
                    value: dt.clone(),
                })
            },
            ExtensionValue::Duration(dur) if !Self::is_valid_duration(dur) => {
                Some(ValidationError::InvalidExtensionFormat {
                    path: path.to_string(),
                    extension_type: "duration".to_string(),
                    value: dur.clone(),
                })
            },
            _ => None,
        }
    }

    /// Validate an array value, collecting all errors.
    fn validate_array_collecting(
        &self,
        arr: &[Value],
        path: &str,
        depth: usize,
        errors: &mut Vec<ValidationError>,
    ) {
        if arr.len() > self.config.max_array_length {
            errors.push(ValidationError::ArrayTooLong {
                path: path.to_string(),
                length: arr.len(),
                max: self.config.max_array_length,
            });
        }
        for (i, item) in arr.iter().enumerate() {
            let item_path = Cow::Owned(format!("{path}[{i}]"));
            self.validate_collecting(item, item_path, depth + 1, errors);
        }
    }

    /// Validate an object value, collecting all errors.
    fn validate_object_collecting(
        &self,
        value: &Value,
        obj: &serde_json::Map<String, Value>,
        path: &str,
        depth: usize,
        errors: &mut Vec<ValidationError>,
    ) {
        if obj.len() > self.config.max_object_keys {
            errors.push(ValidationError::TooManyKeys {
                path: path.to_string(),
                count: obj.len(),
                max: self.config.max_object_keys,
            });
        }

        // Check for entity reference
        if CedarValueMapper::is_entity_reference(value) {
            if let Err(e) = Self::validate_entity_reference(value, path) {
                errors.push(e);
            }
            return;
        }

        // Check for extension type marker
        if let Some(extn) = obj.get("__extn") {
            if let Err(e) = self.validate_extension_marker(extn, path) {
                errors.push(e);
            }
            return;
        }

        // Validate keys and recursively validate values
        for (key, val) in obj {
            if key.is_empty() {
                errors.push(ValidationError::InvalidKey {
                    path: path.to_string(),
                    reason: "empty key".to_string(),
                });
            }
            if key.chars().any(char::is_control) {
                errors.push(ValidationError::InvalidKey {
                    path: path.to_string(),
                    reason: "key contains control characters".to_string(),
                });
            }
            let field_path = Cow::Owned(format!("{path}.{key}"));
            self.validate_collecting(val, field_path, depth + 1, errors);
        }
    }

    // Validate entity reference format
    fn validate_entity_reference(value: &Value, path: &str) -> Result<(), ValidationError> {
        match CedarValueMapper::parse_entity_reference(value) {
            Ok(entity_ref) => {
                // Validate entity type format (should be like "Namespace::Type" or just "Type")
                if entity_ref.entity_type.is_empty() {
                    return Err(ValidationError::InvalidEntityReference {
                        path: path.to_string(),
                        reason: "empty entity type".to_string(),
                    });
                }
                if entity_ref.entity_id.is_empty() {
                    return Err(ValidationError::InvalidEntityReference {
                        path: path.to_string(),
                        reason: "empty entity id".to_string(),
                    });
                }
                Ok(())
            },
            Err(_) => Err(ValidationError::InvalidEntityReference {
                path: path.to_string(),
                reason: "invalid format".to_string(),
            }),
        }
    }

    // Validate extension type marker
    fn validate_extension_marker(&self, extn: &Value, path: &str) -> Result<(), ValidationError> {
        if let Some(obj) = extn.as_object() {
            let fn_name = obj.get("fn").and_then(|v| v.as_str());
            let arg = obj.get("arg").and_then(|v| v.as_str());

            match (fn_name, arg) {
                (Some(fn_name), Some(arg)) => self.validate_extension(arg, fn_name).map_err(|_| {
                    ValidationError::InvalidExtensionFormat {
                        path: path.to_string(),
                        extension_type: fn_name.to_string(),
                        value: arg.to_string(),
                    }
                }),
                _ => Err(ValidationError::InvalidExtensionFormat {
                    path: path.to_string(),
                    extension_type: "unknown".to_string(),
                    value: extn.to_string(),
                }),
            }
        } else {
            Err(ValidationError::InvalidExtensionFormat {
                path: path.to_string(),
                extension_type: "unknown".to_string(),
                value: extn.to_string(),
            })
        }
    }

    /// Check if a string is a valid ISO 8601 / RFC 3339 datetime.
    /// Uses the canonical implementation from entry.rs for consistency.
    fn is_valid_datetime(value: &str) -> bool {
        crate::context_data_api::entry::CedarType::is_datetime_format(value)
    }

    /// Check if a string is a valid Cedar duration format.
    /// Uses the canonical implementation from entry.rs for consistency.
    fn is_valid_duration(value: &str) -> bool {
        crate::context_data_api::entry::CedarType::is_duration_format(value)
    }
}

/// Recursive helper for sanitizing JSON values.
///
/// This is a free function to avoid the `only_used_in_recursion` clippy warning
/// when `self` is passed only to enable recursion.
fn sanitize_value_recursive(value: &Value) -> Value {
    match value {
        Value::Null => Value::String(String::new()),
        Value::String(s) => {
            // Remove control characters
            let sanitized: String = s.chars().filter(|c| !c.is_control()).collect();
            Value::String(sanitized)
        },
        Value::Array(arr) => Value::Array(arr.iter().map(sanitize_value_recursive).collect()),
        Value::Object(obj) => {
            let sanitized: serde_json::Map<String, Value> = obj
                .iter()
                .filter(|(_, v)| !v.is_null())
                .map(|(k, v)| {
                    // Sanitize key
                    let clean_key: String = k.chars().filter(|c| !c.is_control()).collect();
                    (clean_key, sanitize_value_recursive(v))
                })
                .collect();
            Value::Object(sanitized)
        },
        // Numbers and booleans pass through unchanged
        _ => value.clone(),
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;
    use test_utils::assert_eq;

    #[test]
    fn test_validate_primitives() {
        let validator = DataValidator::new();

        assert!(validator.validate(&json!(true)).is_ok());
        assert!(validator.validate(&json!(42)).is_ok());
        assert!(validator.validate(&json!("hello")).is_ok());
        assert!(validator.validate(&json!(3.5)).is_ok());
    }

    #[test]
    fn test_validate_null_rejected() {
        let validator = DataValidator::new();
        let result = validator.validate(&json!(null));
        assert!(
            matches!(result, Err(ValidationError::NullNotSupported { .. })),
            "expected NullNotSupported error when validating null"
        );
    }

    #[test]
    fn test_validate_collections() {
        let validator = DataValidator::new();

        assert!(validator.validate(&json!([1, 2, 3])).is_ok());
        assert!(validator.validate(&json!({"a": 1, "b": 2})).is_ok());
    }

    #[test]
    fn test_validate_nested() {
        let validator = DataValidator::new();

        let nested = json!({
            "user": {
                "profile": {
                    "settings": {
                        "theme": "dark"
                    }
                }
            }
        });

        assert!(validator.validate(&nested).is_ok());
    }

    #[test]
    fn test_max_depth_exceeded() {
        let validator = DataValidator::new().with_max_depth(2);

        let deep = json!({
            "a": {
                "b": {
                    "c": {
                        "d": 1
                    }
                }
            }
        });

        let result = validator.validate(&deep);
        assert!(matches!(
            result,
            Err(ValidationError::MaxDepthExceeded { .. })
        ));
    }

    #[test]
    fn test_string_too_long() {
        let validator = DataValidator::new().with_max_string_length(10);

        let long_string = json!("this is a very long string");
        let result = validator.validate(&long_string);
        assert!(matches!(result, Err(ValidationError::StringTooLong { .. })));
    }

    #[test]
    fn test_array_too_long() {
        let validator = DataValidator::new().with_max_array_length(3);

        let long_array = json!([1, 2, 3, 4, 5]);
        let result = validator.validate(&long_array);
        assert!(matches!(result, Err(ValidationError::ArrayTooLong { .. })));
    }

    #[test]
    fn test_too_many_keys() {
        let validator = DataValidator::new().with_max_object_keys(2);

        let many_keys = json!({"a": 1, "b": 2, "c": 3});
        let result = validator.validate(&many_keys);
        assert!(matches!(result, Err(ValidationError::TooManyKeys { .. })));
    }

    #[test]
    fn test_validate_entity_reference() {
        let validator = DataValidator::new();

        // Valid entity reference
        let entity = json!({"type": "User", "id": "alice"});
        assert!(validator.validate(&entity).is_ok());

        // Invalid - empty type
        let invalid_type = json!({"type": "", "id": "alice"});
        let result = validator.validate(&invalid_type);
        assert!(matches!(
            result,
            Err(ValidationError::InvalidEntityReference { .. })
        ));

        // Invalid - empty id
        let invalid_id = json!({"type": "User", "id": ""});
        let result = validator.validate(&invalid_id);
        assert!(matches!(
            result,
            Err(ValidationError::InvalidEntityReference { .. })
        ));
    }

    #[test]
    fn test_validate_extension_marker() {
        let validator = DataValidator::new();

        // Valid decimal
        let decimal = json!({"__extn": {"fn": "decimal", "arg": "3.14"}});
        assert!(validator.validate(&decimal).is_ok());

        // Valid IP
        let ip = json!({"__extn": {"fn": "ip", "arg": "192.168.1.1"}});
        assert!(validator.validate(&ip).is_ok());

        // Invalid IP format
        let invalid_ip = json!({"__extn": {"fn": "ip", "arg": "not-an-ip"}});
        let result = validator.validate(&invalid_ip);
        assert!(matches!(
            result,
            Err(ValidationError::InvalidExtensionFormat { .. })
        ));
    }

    #[test]
    fn test_validate_type() {
        let validator = DataValidator::new();

        assert!(
            validator
                .validate_type(&json!("hello"), CedarType::String)
                .is_ok()
        );
        assert!(validator.validate_type(&json!(42), CedarType::Long).is_ok());
        assert!(
            validator
                .validate_type(&json!(true), CedarType::Bool)
                .is_ok()
        );
        assert!(
            validator
                .validate_type(&json!([1, 2]), CedarType::Set)
                .is_ok()
        );
        assert!(
            validator
                .validate_type(&json!({"a": 1}), CedarType::Record)
                .is_ok()
        );

        // Type mismatch
        let result = validator.validate_type(&json!("hello"), CedarType::Long);
        assert!(matches!(result, Err(ValidationError::TypeMismatch { .. })));
    }

    #[test]
    fn test_sanitize() {
        let validator = DataValidator::new();

        // Null becomes empty string
        assert_eq!(validator.sanitize(&json!(null)), json!(""));

        // Control characters removed
        let with_control = json!("hello\x00world");
        let sanitized = validator.sanitize(&with_control);
        assert_eq!(sanitized, json!("helloworld"));

        // Null fields removed from objects
        let with_null = json!({"a": 1, "b": null, "c": 3});
        let sanitized = validator.sanitize(&with_null);
        assert_eq!(sanitized, json!({"a": 1, "c": 3}));
    }

    #[test]
    fn test_validate_detailed() {
        let validator = DataValidator::new()
            .with_max_string_length(5)
            .collect_all_errors(true);

        let data = json!({
            "short": "hi",
            "long": "this is too long",
            "also_long": "another long string"
        });

        let result = validator.validate_detailed(&data);
        assert!(!result.is_valid);
        assert_eq!(result.errors.len(), 2);
    }

    #[test]
    fn test_validation_result() {
        let result = ValidationResult::ok();
        assert!(result.is_valid);
        assert!(result.into_result().is_ok());

        let result = ValidationResult::error(ValidationError::NullNotSupported {
            path: "$".to_string(),
        });
        assert!(!result.is_valid);
        assert!(result.into_result().is_err());
    }

    #[test]
    fn test_invalid_key() {
        let validator = DataValidator::new();

        // Empty key
        let data = json!({"": 1});
        let result = validator.validate(&data);
        assert!(matches!(result, Err(ValidationError::InvalidKey { .. })));
    }
}
