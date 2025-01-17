// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use serde::{Deserialize, Deserializer};
use serde_json::Value;

/// Helper function to convert Python-style list strings to JSON format
fn to_json(s: &str) -> Option<Value> {
    let mut json_string = s.trim().to_string();

    if json_string.starts_with('[') && json_string.ends_with(']') {
        let json_like = json_string.replace('\'', "\""); // Replace single quotes with double quotes
        json_string = json_like;
    }

    // Validate that the result is valid JSON
    if let Ok(value) = serde_json::from_str::<Value>(json_string.as_str()) {
        return Some(value);
    }
    None
}

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
    if let Value::String(s) = &value {
        if let Some(parsed_value) = to_json(s) {
            if let Ok(result) = T::deserialize(parsed_value) {
                return Ok(result);
            }
        }
    }

    // Try normal deserialization
    T::deserialize(value).map_err(serde::de::Error::custom)
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
        vector_int: Vec<i32>,
        #[serde(deserialize_with = "fallback_deserialize", default)]
        vector_str: Vec<String>,
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
    fn test_fallback_deserialize_basic_types() {
        let test_cases = vec![
            (
                r#"{"value": 42, "optional": "test", "vector_int": [1, 2, 3]}"#,
                TestStruct {
                    value: 42,
                    optional: Some("test".to_string()),
                    vector_int: vec![1, 2, 3],
                    vector_str: vec![],
                },
            ),
            (
                r#"{"value": "42", "optional": null, "vector_int": [4, 5, 6]}"#,
                TestStruct {
                    value: 42,
                    optional: None,
                    vector_int: vec![4, 5, 6],
                    vector_str: vec![],
                },
            ),
            (
                r#"{"value": 42, "optional": "null", "vector_int": [4, 5, 6]}"#,
                TestStruct {
                    value: 42,
                    optional: None,
                    vector_int: vec![4, 5, 6],
                    vector_str: vec![],
                },
            ),
            (
                r#"{"value": 42, "optional": null, "vector_int": "[4, 5, 6]"}"#,
                TestStruct {
                    value: 42,
                    optional: None,
                    vector_int: vec![4, 5, 6],
                    vector_str: vec![],
                },
            ),
            (r#"{"value": 42, "vector_int": []}"#, TestStruct {
                value: 42,
                optional: None,
                vector_int: vec![],
                vector_str: vec![],
            }),
            (
                r#"{"value": 42, "optional": "test", "vector_int": []}"#,
                TestStruct {
                    value: 42,
                    optional: Some("test".to_string()),
                    vector_int: vec![],
                    vector_str: vec![],
                },
            ),
        ];

        for (json, expected) in test_cases {
            let result: TestStruct = serde_json::from_str(json).unwrap();
            assert_eq!(result, expected);
        }
    }

    #[test]
    fn test_fallback_deserialize_string_vectors() {
        let test_cases = vec![
            (
                r#"{"value": 42, "optional": null, "vector_str": ["a", "b", "c"]}"#,
                vec!["a".to_string(), "b".to_string(), "c".to_string()],
            ),
            (
                r#"{"value": 42, "optional": null, "vector_str": "[\"a\", \"b\", \"c\"]"}"#,
                vec!["a".to_string(), "b".to_string(), "c".to_string()],
            ),
            (r#"{"vector_str": "['sub', 'email']"}"#, vec![
                "sub".to_string(),
                "email".to_string(),
            ]),
            (r#"{"vector_str": "['sub', 'email', 'username']"}"#, vec![
                "sub".to_string(),
                "email".to_string(),
                "username".to_string(),
            ]),
            (
                r#"{"value": 42, "optional": null, "vector_str": []}"#,
                vec![],
            ),
        ];

        for (json, expected_vec) in test_cases {
            let result: TestStruct = serde_json::from_str(json).unwrap();
            assert_eq!(result.vector_str, expected_vec);
        }
    }

    #[test]
    fn test_fallback_deserialize_complex_types() {
        let test_cases = vec![
            (
                r#"{"boolean": "true", "float": 3.14, "nested": null}"#,
                ComplexTestStruct {
                    boolean: true,
                    float: 3.14,
                    nested: None,
                },
            ),
            (
                r#"{"boolean": false, "float": "3.14", "nested": null}"#,
                ComplexTestStruct {
                    boolean: false,
                    float: 3.14,
                    nested: None,
                },
            ),
            (
                r#"{
                    "boolean": true,
                    "float": 1.23,
                    "nested": "{\"value\":42,\"optional\":\"test\",\"vector_int\":[1,2,3]}"
                }"#,
                ComplexTestStruct {
                    boolean: true,
                    float: 1.23,
                    nested: Some(TestStruct {
                        value: 42,
                        optional: Some("test".to_string()),
                        vector_int: vec![1, 2, 3],
                        vector_str: vec![],
                    }),
                },
            ),
        ];

        for (json, expected) in test_cases {
            let result: ComplexTestStruct = serde_json::from_str(json).unwrap();
            assert_eq!(result, expected);
        }
    }

    #[test]
    fn test_fallback_deserialize_error_cases() {
        let test_cases = vec![
            (
                r#"{"value": "not a number", "optional": null, "vector_int": [], "vector_str": []}"#,
                "non-numeric string as i32",
            ),
            (
                r#"{"value": 42, "optional": null, "vector_int": [], "vector_str": "invalid"}"#,
                "invalid string as Vec<String>",
            ),
            (
                r#"{"value": 42, "optional": null, "vector_int": [], "vector_str": "[sub, email]"}"#,
                "invalid Python-style list",
            ),
            (
                r#"{"value": 42, "optional": null, "vector_int": "invalid", "vector_str": []}"#,
                "invalid string as Vec<i32>",
            ),
        ];

        for (json, error_desc) in test_cases {
            let result: Result<TestStruct, _> = serde_json::from_str(json);
            result.expect_err(&format!("should fail to parse {}", error_desc));
        }
    }
}
