// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Schema-Aware Data Validation
//!
//! Provides validation of JSON data against Cedar schema constraints,
//! ensuring type safety and data integrity before values are pushed
//! to the DataStore or used in policy evaluation.

use crate::ValidationError;

use super::{CedarType, CedarValueMapper, ExtensionValue};
use serde_json::Value;
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
    pub fn ok() -> Self {
        Self {
            is_valid: true,
            errors: Vec::new(),
            warnings: Vec::new(),
        }
    }

    /// Create a failed result with a single error
    pub fn error(err: ValidationError) -> Self {
        Self {
            is_valid: false,
            errors: vec![err],
            warnings: Vec::new(),
        }
    }

    /// Create a failed result with multiple errors
    pub fn errors(errs: Vec<ValidationError>) -> Self {
        Self {
            is_valid: errs.is_empty(),
            errors: errs,
            warnings: Vec::new(),
        }
    }

    /// Add a warning
    pub fn with_warning(mut self, warning: String) -> Self {
        self.warnings.push(warning);
        self
    }

    /// Convert to Result type
    pub fn into_result(self) -> Result<(), ValidationError> {
        if self.is_valid {
            Ok(())
        } else if self.errors.len() == 1 {
            Err(self.errors.into_iter().next().unwrap())
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
    pub fn new() -> Self {
        Self {
            config: ValidationConfig::default(),
        }
    }

    /// Create a validator with custom configuration.
    pub fn with_config(config: ValidationConfig) -> Self {
        Self { config }
    }

    /// Set maximum nesting depth.
    pub fn with_max_depth(mut self, max_depth: usize) -> Self {
        self.config.max_depth = max_depth;
        self
    }

    /// Set maximum string length.
    pub fn with_max_string_length(mut self, max_length: usize) -> Self {
        self.config.max_string_length = max_length;
        self
    }

    /// Set maximum array length.
    pub fn with_max_array_length(mut self, max_length: usize) -> Self {
        self.config.max_array_length = max_length;
        self
    }

    /// Set maximum object keys.
    pub fn with_max_object_keys(mut self, max_keys: usize) -> Self {
        self.config.max_object_keys = max_keys;
        self
    }

    /// Enable strict extension format validation.
    pub fn with_strict_extensions(mut self, strict: bool) -> Self {
        self.config.strict_extension_validation = strict;
        self
    }

    /// Enable collecting all errors instead of failing fast.
    pub fn collect_all_errors(mut self, collect: bool) -> Self {
        self.config.collect_all_errors = collect;
        self
    }

    /// Validate a JSON value for Cedar compatibility.
    pub fn validate(&self, value: &Value) -> Result<(), ValidationError> {
        self.validate_at_path(value, "$", 0)?;
        Ok(())
    }

    /// Validate a value and get a detailed result.
    pub fn validate_detailed(&self, value: &Value) -> ValidationResult {
        let mut errors = Vec::new();
        self.validate_collecting(value, "$", 0, &mut errors);
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
                expected: format!("{:?}", expected),
                actual: format!("{:?}", actual),
            })
        }
    }

    /// Validate an extension value format.
    pub fn validate_extension(
        &self,
        value: &str,
        extension_type: &str,
    ) -> Result<(), ValidationError> {
        match extension_type {
            "ipaddr" | "ip" => {
                if IpAddr::from_str(value).is_err() {
                    return Err(ValidationError::InvalidExtensionFormat {
                        path: "$".to_string(),
                        extension_type: extension_type.to_string(),
                        value: value.to_string(),
                    });
                }
            },
            "decimal" => {
                if value.parse::<f64>().is_err() {
                    return Err(ValidationError::InvalidExtensionFormat {
                        path: "$".to_string(),
                        extension_type: extension_type.to_string(),
                        value: value.to_string(),
                    });
                }
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
            },
        }
        Ok(())
    }

    /// Sanitize a value by removing null fields and invalid characters.
    ///
    /// Returns a new value with:
    /// - Null values removed from objects
    /// - Null values in arrays replaced with empty strings
    /// - Control characters removed from strings
    pub fn sanitize(&self, value: &Value) -> Value {
        match value {
            Value::Null => Value::String(String::new()),
            Value::String(s) => {
                // Remove control characters
                let sanitized: String = s.chars().filter(|c| !c.is_control()).collect();
                Value::String(sanitized)
            },
            Value::Array(arr) => Value::Array(arr.iter().map(|v| self.sanitize(v)).collect()),
            Value::Object(obj) => {
                let sanitized: serde_json::Map<String, Value> = obj
                    .iter()
                    .filter(|(_, v)| !v.is_null())
                    .map(|(k, v)| {
                        // Sanitize key
                        let clean_key: String = k.chars().filter(|c| !c.is_control()).collect();
                        (clean_key, self.sanitize(v))
                    })
                    .collect();
                Value::Object(sanitized)
            },
            // Numbers and booleans pass through unchanged
            _ => value.clone(),
        }
    }

    // Internal validation at a specific path and depth
    fn validate_at_path(
        &self,
        value: &Value,
        path: &str,
        depth: usize,
    ) -> Result<(), ValidationError> {
        // Check depth
        if depth > self.config.max_depth {
            return Err(ValidationError::MaxDepthExceeded {
                path: path.to_string(),
                max: self.config.max_depth,
            });
        }

        match value {
            Value::Null => Err(ValidationError::NullNotSupported {
                path: path.to_string(),
            }),
            Value::Bool(_) => Ok(()),
            Value::Number(n) => {
                // Check that it fits in i64 for Cedar Long type
                if n.is_i64() || n.is_u64() {
                    Ok(())
                } else if n.is_f64() {
                    // Floats are allowed (converted to decimal)
                    Ok(())
                } else {
                    Err(ValidationError::NumberOutOfRange {
                        path: path.to_string(),
                    })
                }
            },
            Value::String(s) => {
                if s.len() > self.config.max_string_length {
                    return Err(ValidationError::StringTooLong {
                        path: path.to_string(),
                        length: s.len(),
                        max: self.config.max_string_length,
                    });
                }

                // If it looks like an extension type, validate its format
                if self.config.strict_extension_validation {
                    if let Some(ext) = CedarValueMapper::detect_extension(s) {
                        match ext {
                            ExtensionValue::IpAddr(ip) => {
                                if IpAddr::from_str(&ip).is_err() {
                                    return Err(ValidationError::InvalidExtensionFormat {
                                        path: path.to_string(),
                                        extension_type: "ipaddr".to_string(),
                                        value: ip,
                                    });
                                }
                            },
                            ExtensionValue::Decimal(d) => {
                                if d.parse::<f64>().is_err() {
                                    return Err(ValidationError::InvalidExtensionFormat {
                                        path: path.to_string(),
                                        extension_type: "decimal".to_string(),
                                        value: d,
                                    });
                                }
                            },
                        }
                    }
                }

                Ok(())
            },
            Value::Array(arr) => {
                if arr.len() > self.config.max_array_length {
                    return Err(ValidationError::ArrayTooLong {
                        path: path.to_string(),
                        length: arr.len(),
                        max: self.config.max_array_length,
                    });
                }

                for (i, item) in arr.iter().enumerate() {
                    let item_path = format!("{}[{}]", path, i);
                    self.validate_at_path(item, &item_path, depth + 1)?;
                }

                Ok(())
            },
            Value::Object(obj) => {
                if obj.len() > self.config.max_object_keys {
                    return Err(ValidationError::TooManyKeys {
                        path: path.to_string(),
                        count: obj.len(),
                        max: self.config.max_object_keys,
                    });
                }

                // Check for entity reference
                if CedarValueMapper::is_entity_reference(value) {
                    return self.validate_entity_reference(value, path);
                }

                // Check for extension type marker
                if let Some(extn) = obj.get("__extn") {
                    return self.validate_extension_marker(extn, path);
                }

                // Validate keys
                for key in obj.keys() {
                    if key.is_empty() {
                        return Err(ValidationError::InvalidKey {
                            path: path.to_string(),
                            reason: "empty key".to_string(),
                        });
                    }
                    // Check for control characters in keys
                    if key.chars().any(|c| c.is_control()) {
                        return Err(ValidationError::InvalidKey {
                            path: path.to_string(),
                            reason: "key contains control characters".to_string(),
                        });
                    }
                }

                // Recursively validate values
                for (key, val) in obj {
                    let field_path = format!("{}.{}", path, key);
                    self.validate_at_path(val, &field_path, depth + 1)?;
                }

                Ok(())
            },
        }
    }

    // Collect all errors instead of failing fast
    fn validate_collecting(
        &self,
        value: &Value,
        path: &str,
        depth: usize,
        errors: &mut Vec<ValidationError>,
    ) {
        if depth > self.config.max_depth {
            errors.push(ValidationError::MaxDepthExceeded {
                path: path.to_string(),
                max: self.config.max_depth,
            });
            return;
        }

        match value {
            Value::Null => {
                errors.push(ValidationError::NullNotSupported {
                    path: path.to_string(),
                });
            },
            Value::Bool(_) => {},
            Value::Number(n) => {
                if !n.is_i64() && !n.is_u64() && !n.is_f64() {
                    errors.push(ValidationError::NumberOutOfRange {
                        path: path.to_string(),
                    });
                }
            },
            Value::String(s) => {
                if s.len() > self.config.max_string_length {
                    errors.push(ValidationError::StringTooLong {
                        path: path.to_string(),
                        length: s.len(),
                        max: self.config.max_string_length,
                    });
                }
            },
            Value::Array(arr) => {
                if arr.len() > self.config.max_array_length {
                    errors.push(ValidationError::ArrayTooLong {
                        path: path.to_string(),
                        length: arr.len(),
                        max: self.config.max_array_length,
                    });
                }
                for (i, item) in arr.iter().enumerate() {
                    let item_path = format!("{}[{}]", path, i);
                    self.validate_collecting(item, &item_path, depth + 1, errors);
                }
            },
            Value::Object(obj) => {
                if obj.len() > self.config.max_object_keys {
                    errors.push(ValidationError::TooManyKeys {
                        path: path.to_string(),
                        count: obj.len(),
                        max: self.config.max_object_keys,
                    });
                }
                for (key, val) in obj {
                    let field_path = format!("{}.{}", path, key);
                    self.validate_collecting(val, &field_path, depth + 1, errors);
                }
            },
        }
    }

    // Validate entity reference format
    fn validate_entity_reference(&self, value: &Value, path: &str) -> Result<(), ValidationError> {
        match CedarValueMapper::parse_entity_reference(value) {
            Ok((entity_type, entity_id)) => {
                // Validate entity type format (should be like "Namespace::Type" or just "Type")
                if entity_type.is_empty() {
                    return Err(ValidationError::InvalidEntityReference {
                        path: path.to_string(),
                        reason: "empty entity type".to_string(),
                    });
                }
                if entity_id.is_empty() {
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
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    #[test]
    fn test_validate_primitives() {
        let validator = DataValidator::new();

        assert!(validator.validate(&json!(true)).is_ok());
        assert!(validator.validate(&json!(42)).is_ok());
        assert!(validator.validate(&json!("hello")).is_ok());
        assert!(validator.validate(&json!(3.14)).is_ok());
    }

    #[test]
    fn test_validate_null_rejected() {
        let validator = DataValidator::new();
        let result = validator.validate(&json!(null));
        assert!(matches!(
            result,
            Err(ValidationError::NullNotSupported { .. })
        ));
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
