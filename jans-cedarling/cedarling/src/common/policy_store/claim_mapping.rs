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
//
// probably we can store as key composite key of `claim` name and `cedar type`
// for example using as key of hash map:
// #[derive(Hash)]
// struct ClaimMappingKey {
//     claim_name: String,
//     cedar_policy_type: String,
// }
// but for now current approach is OK
#[derive(Debug, Default, PartialEq, Clone, Deserialize)]
pub struct ClaimMappings(HashMap<String, ClaimMapping>);

impl ClaimMappings {
    pub fn mapping(&self, claim: &str) -> Option<&ClaimMapping> {
        self.0.get(claim)
    }

    #[cfg(test)]
    pub fn builder() -> ClaimMappingsBuilder {
        ClaimMappingsBuilder(HashMap::new())
    }
}

impl From<HashMap<String, ClaimMapping>> for ClaimMappings {
    fn from(mappings: HashMap<String, ClaimMapping>) -> Self {
        Self(mappings)
    }
}

#[cfg(test)]
pub struct ClaimMappingsBuilder(HashMap<String, ClaimMapping>);

/// Helper struct for building claim mappings in tests
#[cfg(test)]
impl ClaimMappingsBuilder {
    pub fn build(self) -> ClaimMappings {
        ClaimMappings(self.0)
    }

    pub fn email(mut self, claim: &str) -> Self {
        self.0.insert(
            claim.to_string(),
            serde_json::from_value(serde_json::json!({
                "parser": "regex",
                "type": "Jans::email_address",
                "regex_expression" : "^(?P<UID>[^@]+)@(?P<DOMAIN>.+)$",
                "UID": {"attr": "uid", "type":"String"},
                "DOMAIN": {"attr": "domain", "type":"String"},
            }))
            .expect("failed to deserialize claim mapping"),
        );
        self
    }

    pub fn url(mut self, claim: &str) -> Self {
        self.0.insert(
            claim.to_string(),
            serde_json::from_value(serde_json::json!({
                "parser": "regex",
                "type": "Jans::Url",
                "regex_expression": r#"^(?P<SCHEME>[a-zA-Z][a-zA-Z0-9+.-]*):\/\/(?P<DOMAIN>[^\/]+)(?P<PATH>\/.*)?$"#,
                "SCHEME": {"attr": "scheme", "type": "String"},
                "DOMAIN": {"attr": "domain", "type": "String"},
                "PATH": {"attr": "path", "type": "String"}
            }))
            .expect("failed to deserialize claim mapping"),
        );
        self
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
    Json,
}

impl ClaimMapping {
    /// Apply mapping to the json value
    ///
    /// if `Regex` mapping value will be converted to json value, if has error on converting, return default value
    /// if `Json` mapping value convert JSON object to HashMap or return empty HashMap
    pub fn apply_mapping(&self, value: &serde_json::Value) -> HashMap<String, serde_json::Value> {
        match self {
            ClaimMapping::Regex(regexp_mapping) => regexp_mapping.apply_mapping(value),
            ClaimMapping::Json => {
                // convert JSON object to HashMap or return empty HashMap
                value
                    .as_object()
                    .map(|v| HashMap::from_iter(v.to_owned()))
                    .unwrap_or_default()
            },
        }
    }

    pub fn apply_mapping_value(&self, value: &serde_json::Value) -> serde_json::Value {
        // this should always be a valid JSON since the input is a valid JSON
        serde_json::to_value(self.apply_mapping(value)).expect("a valid JSON")
    }
}

/// Represents a claim mapping using regular expressions.
///
/// # Fields
/// - `regex_expression`: The regular expression used to extract fields.
/// - `fields`: A map of field names to `RegexField` values.
#[derive(Debug, Clone)]
pub struct RegexMapping {
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
        regex_expression: String,
        fields: HashMap<String, RegexFieldMapping>,
    ) -> Result<Self, regex::Error> {
        Ok(Self {
            regex: Regex::new(regex_expression.as_str())?,
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
        self.regex_expression == other.regex_expression
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
                    regex_expression,
                    regex_group_mapping: fields,
                }))
            },
            "json" => Ok(ClaimMapping::Json),
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
        let expected = ClaimMapping::Json;

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
        let parsed = serde_yml::from_str::<ClaimMapping>(claim_mapping_yaml)
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
        let expected = ClaimMapping::Json;

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
