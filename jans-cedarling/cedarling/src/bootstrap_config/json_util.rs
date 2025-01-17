// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use serde::{Deserialize, Deserializer};
use serde_json::Value;

/// Attempts to deserialize a value, falling back to JSON parsing if the value is a string.
/// Returns the deserialized value or the original error if both attempts fail.
pub fn fallback_deserialize<'de, D, T>(deserializer: D) -> Result<T, D::Error>
where
    D: Deserializer<'de>,
    T: Deserialize<'de>,
{
    // First deserialize to serde_json::Value
    let value = Value::deserialize(deserializer)?;

    // If it's a string, try to parse it as JSON
    // we need do this step first to correctly handle json `null` value like Option<String> type
    if let Value::String(s) = &value {
        // First parse the string as JSON
        if let Ok(parsed_value) = serde_json::from_str::<Value>(s) {
            // Then try to deserialize into target type
            if let Ok(result) = T::deserialize(parsed_value) {
                return Ok(result);
            }
        }
    }

    // Try normal deserialization
    T::deserialize(value.clone()).map_err(serde::de::Error::custom)
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde::Deserialize;
    use test_utils::assert_eq;

    /// Test structure used to verify fallback_deserialize functionality
    /// Contains fields of different types to test various scenarios:
    /// - value: i32 - tests number deserialization
    /// - optional: Option<String> - tests optional string handling
    /// - vector: Vec<i32> - tests array deserialization
    #[derive(Debug, Deserialize, PartialEq)]
    struct TestStruct {
        #[serde(deserialize_with = "fallback_deserialize", default)]
        value: i32,
        #[serde(deserialize_with = "fallback_deserialize", default)]
        optional: Option<String>,
        #[serde(deserialize_with = "fallback_deserialize", default)]
        vector: Vec<i32>,
    }

    /// Additional test structure for more complex scenarios
    #[derive(Debug, Deserialize, PartialEq)]
    struct ComplexTestStruct {
        #[serde(deserialize_with = "fallback_deserialize", default)]
        boolean: bool,
        #[serde(deserialize_with = "fallback_deserialize", default)]
        float: f64,
        #[serde(deserialize_with = "fallback_deserialize", default)]
        nested: Option<TestStruct>,
    }

    #[test]
    fn test_fallback_deserialize_normal_json() {
        let json = r#"{"value": 42, "optional": "test", "vector": [1, 2, 3]}"#;
        let result: TestStruct = serde_json::from_str(json).unwrap();
        assert_eq!(result, TestStruct {
            value: 42,
            optional: Some("test".to_string()),
            vector: vec![1, 2, 3],
        });
    }

    #[test]
    fn test_fallback_deserialize_json_string_as_number() {
        let json_string = r#"{"value": "42", "optional": null, "vector": [4, 5, 6]}"#;
        let result: TestStruct = serde_json::from_str(json_string).unwrap();
        assert_eq!(result, TestStruct {
            value: 42,
            optional: None,
            vector: vec![4, 5, 6],
        });
    }

    #[test]
    fn test_fallback_deserialize_json_string_as_null() {
        let json_string = r#"{"value": 42, "optional": "null", "vector": [4, 5, 6]}"#;
        let result: TestStruct = serde_json::from_str(json_string).unwrap();
        assert_eq!(result, TestStruct {
            value: 42,
            optional: None,
            vector: vec![4, 5, 6],
        });
    }

    #[test]
    fn test_fallback_deserialize_json_string_as_vector() {
        let json_string = r#"{"value": 42, "optional": null, "vector": "[4, 5, 6]"}"#;
        let result: TestStruct = serde_json::from_str(json_string).unwrap();
        assert_eq!(result, TestStruct {
            value: 42,
            optional: None,
            vector: vec![4, 5, 6],
        });
    }

    #[test]
    fn test_fallback_deserialize_invalid_json() {
        let invalid_json = r#""not a valid json string""#;
        let result: Result<TestStruct, _> = serde_json::from_str(invalid_json);
        result.expect_err("should fail to parse invalid JSON string");
    }

    #[test]
    fn test_fallback_deserialize_non_string_value() {
        let json = r#"{"value": "not a number"}"#;
        let result: Result<TestStruct, _> = serde_json::from_str(json);
        result.expect_err("should fail to parse non-numeric string as i32");
    }

    #[test]
    fn test_fallback_deserialize_with_optional_field() {
        let json = r#"{"value": 42, "vector": []}"#;
        let result: TestStruct = serde_json::from_str(json).unwrap();
        assert_eq!(result, TestStruct {
            value: 42,
            optional: None,
            vector: vec![],
        });
    }

    #[test]
    fn test_fallback_deserialize_with_empty_vector() {
        let json = r#"{"value": 42, "optional": "test", "vector": []}"#;
        let result: TestStruct = serde_json::from_str(json).unwrap();
        assert_eq!(result, TestStruct {
            value: 42,
            optional: Some("test".to_string()),
            vector: vec![],
        });
    }

    #[test]
    fn test_fallback_deserialize_boolean() {
        let json = r#"{"boolean": "true", "float": 3.14, "nested": null}"#;
        let result: ComplexTestStruct = serde_json::from_str(json).unwrap();
        assert_eq!(result, ComplexTestStruct {
            boolean: true,
            float: 3.14,
            nested: None,
        });
    }

    #[test]
    fn test_fallback_deserialize_float() {
        let json = r#"{"boolean": false, "float": "3.14", "nested": null}"#;
        let result: ComplexTestStruct = serde_json::from_str(json).unwrap();
        assert_eq!(result, ComplexTestStruct {
            boolean: false,
            float: 3.14,
            nested: None,
        });
    }

    #[test]
    fn test_fallback_deserialize_nested_structure() {
        let json = r#"{
            "boolean": true,
            "float": 1.23,
            "nested": "{\"value\":42,\"optional\":\"test\",\"vector\":[1,2,3]}"
        }"#;
        let result: ComplexTestStruct = serde_json::from_str(json).unwrap();
        assert_eq!(result, ComplexTestStruct {
            boolean: true,
            float: 1.23,
            nested: Some(TestStruct {
                value: 42,
                optional: Some("test".to_string()),
                vector: vec![1, 2, 3],
            }),
        });
    }

    #[test]
    fn test_fallback_deserialize_invalid_nested_json() {
        let json = r#"{
            "boolean": true,
            "float": 1.23,
            "nested": "invalid json"
        }"#;
        let result: Result<ComplexTestStruct, _> = serde_json::from_str(json);
        result.expect_err("should fail to parse invalid nested JSON");
    }
}
