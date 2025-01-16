// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashMap;

use regex;
use regex::Regex;
use serde::{Deserialize, de};
use serde_json::Value;

/// Structure for storing `claim mappings`
///
/// wrapper around hash map
#[derive(Debug, Default, PartialEq, Clone, Deserialize)]
pub struct ClaimMappings(HashMap<String, ClaimMapping>);

impl ClaimMappings {
    pub fn get(&self, claim: &str) -> Option<&ClaimMapping> {
        self.0.get(claim)
    }

    // returns (claim_name, &ClaimMapping)
    pub fn get_mapping_for_type(&self, type_name: &str) -> Option<(&String, &ClaimMapping)> {
        // PERF: we can probably avoiding iterating through all of this by changing the
        // `claim_mapping` in the Token Entity Metadata Schema
        self.0
            .iter()
            .find_map(|(claim_name, mapping)| match mapping {
                ClaimMapping::Regex(regex_mapping) => {
                    (regex_mapping.cedar_policy_type == type_name).then_some((claim_name, mapping))
                },
                ClaimMapping::Json { r#type } => {
                    (r#type == type_name).then_some((claim_name, mapping))
                },
            })
    }

    pub fn get_mapping(&self, claim: &str, cedar_policy_type: &str) -> Option<&ClaimMapping> {
        self.0
            .get(claim)
            .filter(|claim_mapping| match claim_mapping {
                ClaimMapping::Regex(regexp_mapping) => {
                    regexp_mapping.cedar_policy_type == cedar_policy_type
                },
                ClaimMapping::Json { r#type } => r#type == cedar_policy_type,
            })
    }
}

/// Represents the mapping of claims based on the parser type.
///
/// This enum can either be:
/// - `Regex`: For extracting claims using regular expressions with fields.
/// - `Json`: For extracting claims using a JSON parser.
#[derive(Debug, PartialEq, Clone)]
pub enum ClaimMapping {
    /// Represents a claim mapping using regular expressions.
    Regex(RegexMapping),

    /// Represents a claim mapping using a JSON parser.
    ///
    /// # Fields
    /// - `type`: The type of the claim (e.g., "Acme::Dolphin").
    Json { r#type: String },
}

impl ClaimMapping {
    /// Apply mapping to the json value
    ///
    /// if `Regex` mapping value will be converted to json value, if has error on converting, return default value
    /// if `Json` mapping value convert JSON object to HashMap or return empty HashMap
    pub fn apply_mapping(&self, value: &serde_json::Value) -> HashMap<String, serde_json::Value> {
        match self {
            ClaimMapping::Regex(regexp_mapping) => regexp_mapping.apply_mapping(value),
            ClaimMapping::Json { r#type: _ } => {
                // convert JSON object to HashMap or return empty HashMap
                value
                    .as_object()
                    .map(|v| HashMap::from_iter(v.to_owned()))
                    .unwrap_or_default()
            },
        }
    }
}

/// Represents a claim mapping using regular expressions.
///
/// # Fields
/// - `type`: The type of the claim (e.g., "Acme::Email").
/// - `regex_expression`: The regular expression used to extract fields.
/// - `fields`: A map of field names to `RegexField` values.
#[derive(Debug, Clone)]
pub struct RegexMapping {
    cedar_policy_type: String,
    regex_expression: String,
    regex: Regex,

    // hashmap key is name of regex group
    // hashmap value describe how to map field found in group
    regex_group_mapping: HashMap<String, RegexFieldMapping>,
}

impl RegexMapping {
    // builder function, used in testing
    #[allow(dead_code)]
    fn new(
        cedar_policy_type: String,
        regex_expression: String,

        fields: HashMap<String, RegexFieldMapping>,
    ) -> Result<Self, regex::Error> {
        Ok(Self {
            regex: Regex::new(regex_expression.as_str())?,
            cedar_policy_type,
            regex_expression,
            regex_group_mapping: fields,
        })
    }

    /// Apply regex mapping to json value
    ///
    /// the function tries to map json value to string before search values using regex
    fn apply_mapping(&self, value: &serde_json::Value) -> HashMap<String, serde_json::Value> {
        let str_value = match value {
            Value::Number(number) => number.to_string(),
            // we need manually map value to string instead calling `serde_json::Value::to_string()` method
            // to avoid having `"` quotes in start and end of string
            Value::String(string_value) => string_value.to_owned(),
            v => v.to_string(),
        };

        // we use only first capture
        let Some(captures) = self.regex.captures(str_value.as_str()) else {
            // if we no have capture return empty json object
            return HashMap::new();
        };

        HashMap::from_iter(
            self.regex_group_mapping
                .iter()
                .map(|(key, regex_field_map_info)| {
                    let capture_value = &captures[key.as_str()];

                    (
                        regex_field_map_info.attr.clone(),
                        regex_field_map_info.r#type.apply_mapping(capture_value),
                    )
                }),
        )
    }
}

impl PartialEq for RegexMapping {
    // impl operator "==" to compare struct in test cases
    // `regex` is ignored because it is result of `regex_expression` string and actually not comparable
    fn eq(&self, other: &Self) -> bool {
        self.cedar_policy_type == other.cedar_policy_type
            && self.regex_expression == other.regex_expression
            && self.regex_group_mapping == other.regex_group_mapping
    }
}

impl<'de> Deserialize<'de> for ClaimMapping {
    /// Custom deserialization logic for `ClaimMapping`.
    ///
    /// Parses a JSON object to determine whether the parser type is `regex` or `json`.
    /// Depending on the parser type, it deserializes the corresponding fields into
    /// either a `Regex` or `Json` variant of the `ClaimMapping` enum.
    ///
    /// # Errors
    /// Returns a deserialization error if:
    /// - The `parser` field is missing.
    /// - The `type` field is missing.
    /// - The `regex_expression` field is missing for `regex` type.
    /// - The parser type is unrecognized.
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        const PARSER_KEY: &str = "parser";
        const TYPE_KEY: &str = "type";
        const REGEX_EXPRESSION_KEY: &str = "regex_expression";

        let value = Value::deserialize(deserializer)?;

        let parser = value
            .get(PARSER_KEY)
            .and_then(|v| v.as_str())
            .ok_or_else(|| de::Error::missing_field(PARSER_KEY))?;

        match parser {
            "regex" => {
                let r#type = value
                    .get(TYPE_KEY)
                    .and_then(|v| v.as_str())
                    .ok_or(de::Error::missing_field(TYPE_KEY))?
                    .to_string();

                let regex_expression = value
                    .get(REGEX_EXPRESSION_KEY)
                    .and_then(|v| v.as_str())
                    .ok_or(de::Error::missing_field(REGEX_EXPRESSION_KEY))?
                    .to_string();

                let mut fields = HashMap::new();
                if let Some(obj) = value.as_object() {
                    for (key, val) in obj.iter() {
                        if key != PARSER_KEY && key != TYPE_KEY && key != REGEX_EXPRESSION_KEY {
                            let field = serde_json::from_value::<RegexFieldMapping>(val.to_owned())
                                .map_err(de::Error::custom)?;
                            fields.insert(key.clone(), field);
                        }
                    }
                }
                Ok(ClaimMapping::Regex(RegexMapping {
                    regex: Regex::new(&regex_expression).map_err(|err| {
                        de::Error::custom(format!(
                            "could not parse field regex as regular expression:{err}"
                        ))
                    })?,
                    cedar_policy_type: r#type,
                    regex_expression,
                    regex_group_mapping: fields,
                }))
            },
            "json" => {
                let r#type = value
                    .get(TYPE_KEY)
                    .and_then(|v| v.as_str())
                    .ok_or_else(|| de::Error::missing_field(TYPE_KEY))?
                    .to_string();

                Ok(ClaimMapping::Json { r#type })
            },
            _ => Err(de::Error::custom("unknown parser type")),
        }
    }
}

/// Represents a field extracted using a regular expression.
///
/// # Fields
/// - `attr`: The attribute name associated with the field (e.g., "uid").
/// - `type`: The type of the attribute (e.g., "string").
#[derive(Debug, PartialEq, Deserialize, Clone)]

pub struct RegexFieldMapping {
    pub attr: String,
    pub r#type: RegexFieldMappingType,
}

/// Enum represent possible types to map result from regex group
///
/// Result of regex search is always string.
/// So string will be converted to next json types.
///
/// If string is empty or not correct will be used default value for according type.
#[derive(Debug, PartialEq, Deserialize, Copy, Clone)]
pub enum RegexFieldMappingType {
    String,
    Number,
    Boolean,
}

impl RegexFieldMappingType {
    /// Apply mapping string to json value
    ///
    /// `String` - to string without transformation
    /// `Number` - parse string to float64 if error returns default value
    /// `Boolean` - if string NOT empty map to true else false
    pub fn apply_mapping(&self, value: &str) -> serde_json::Value {
        match self {
            RegexFieldMappingType::String => serde_json::json!(value),
            RegexFieldMappingType::Number => {
                serde_json::json!(value.parse::<f64>().unwrap_or_default())
            },
            RegexFieldMappingType::Boolean => {
                serde_json::json!(!value.is_empty())
            },
        }
    }
}

#[cfg(test)]
mod test {
    use std::collections::HashMap;

    use serde_json::json;
    use test_utils::assert_eq;

    use super::{ClaimMapping, RegexMapping, *};
    use crate::common::policy_store::claim_mapping::RegexFieldMapping;

    /// Tests if a token entity metadata with a RegEx parser can be parsed
    /// from a JSON string
    #[test]
    fn can_parse_regex_from_json() {
        let re_mapping = RegexMapping::new(
            "Acme::Email".to_string(),
            r#"^(?P<UID>[^@]+)@(?P<DOMAIN>.+)$"#.to_string(),
            HashMap::from([
                ("UID".to_string(), RegexFieldMapping {
                    attr: "uid".to_string(),
                    r#type: RegexFieldMappingType::String,
                }),
                ("DOMAIN".to_string(), RegexFieldMapping {
                    attr: "domain".to_string(),
                    r#type: RegexFieldMappingType::String,
                }),
            ]),
        )
        .expect("regexp should parse correctly");

        // Setup expected output
        let expected = ClaimMapping::Regex(re_mapping);

        // Setup JSON
        let claim_mapping_json = json!({
            "parser": "regex",
            "type": "Acme::Email",
            "regex_expression": "^(?P<UID>[^@]+)@(?P<DOMAIN>.+)$",
            "UID": { "attr": "uid", "type": "String" },
            "DOMAIN": { "attr": "domain", "type": "String" },
        })
        .to_string();

        // Parse JSON
        let parsed = serde_json::from_str::<ClaimMapping>(&claim_mapping_json)
            .expect("should parse token entity metadata");

        // Assert if the JSON got parsed correctly
        assert_eq!(
            parsed, expected,
            "Expected the Claim Mapping to be parsed correctly: {:?}",
            parsed
        );
    }

    /// Tests if a token entity metadata with a JSON parser can be parsed
    /// from a JSON string

    #[test]
    fn can_parse_json_from_json() {
        // Setup expected output
        let expected = ClaimMapping::Json {
            r#type: "Acme::Dolphin".to_string(),
        };

        // Setup JSON
        let claim_mapping_json = json!({
            "parser": "json",
            "type": "Acme::Dolphin",
        })
        .to_string();

        // Parse JSON
        let parsed = serde_json::from_str::<ClaimMapping>(&claim_mapping_json)
            .expect("should parse token entity metadata");

        // Assert if the JSON got parsed correctly
        assert_eq!(
            parsed, expected,
            "Expected the Claim Mapping to be parsed correctly: {:?}",
            parsed
        );
    }

    /// Tests if a token entity metadata with a RegEx parser can be parsed
    /// from a YAML string
    #[test]
    fn can_parse_regex_from_yaml() {
        let re_mapping = RegexMapping::new(
            "Acme::Email".to_string(),
            r#"^(?P<UID>[^@]+)@(?P<DOMAIN>.+)$"#.to_string(),
            HashMap::from([
                ("UID".to_string(), RegexFieldMapping {
                    attr: "uid".to_string(),
                    r#type: RegexFieldMappingType::String,
                }),
                ("DOMAIN".to_string(), RegexFieldMapping {
                    attr: "domain".to_string(),

                    r#type: RegexFieldMappingType::String,
                }),
            ]),
        )
        .expect("regexp should parse correctly");

        // Setup expected output
        let expected = ClaimMapping::Regex(re_mapping);

        // Setup Yaml
        let claim_mapping_yaml = "
            parser: 'regex'
            type: 'Acme::Email'
            regex_expression: '^(?P<UID>[^@]+)@(?P<DOMAIN>.+)$'
            UID:
                attr: 'uid'
                type: 'String'
            DOMAIN:
                attr: 'domain'
                type: 'String'
        ";

        // Parse YAML
        let parsed = serde_yml::from_str::<ClaimMapping>(&claim_mapping_yaml)
            .expect("should parse token entity metadata");

        // Assert if the JSON got parsed correctly
        assert_eq!(
            parsed, expected,
            "Expected the Claim Mapping to be parsed correctly: {:?}",
            parsed
        );
    }

    /// Tests if a token entity metadata with a JSON parser can be parsed
    /// from a YAML string
    #[test]
    fn can_parse_json_from_yaml() {
        // Setup expected output
        let expected = ClaimMapping::Json {
            r#type: "Acme::Dolphin".to_string(),
        };

        // Setup YAML
        let claim_mapping_yaml = "
          parser: 'json'
          type: 'Acme::Dolphin'
        "
        .to_string();

        // Parse YAML
        let parsed = serde_yml::from_str::<ClaimMapping>(&claim_mapping_yaml)
            .expect("should parse token entity metadata");

        // Assert if the YAML got parsed correctly
        assert_eq!(
            parsed, expected,
            "Expected the Claim Mapping to be parsed correctly: {:?}",
            parsed
        );
    }

    /// Tests if an error is thrown for an unknown parser type
    #[test]
    fn errors_on_unkown_parser_type() {
        // Setup JSON
        let claim_mapping_json = json!({
            "parser": "",
            "type": "Acme::Dolphin",
        })
        .to_string();

        // Parse JSON
        let parsed = serde_json::from_str::<ClaimMapping>(&claim_mapping_json);

        assert!(
            matches!(parsed, Err(ref e) if e.to_string().contains("unknown parser type")),
            "Expected an error when encountering an unknown parser: {:?}",
            parsed
        );
    }
}
