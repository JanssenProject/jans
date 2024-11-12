use serde::{de, Deserialize};
use serde_json::Value;
use std::collections::HashMap;

#[derive(Debug, PartialEq, Clone)]
#[allow(dead_code)]
pub enum ClaimMapping {
    Regex {
        r#type: String,
        regex_expression: String,
        fields: HashMap<String, RegexField>,
    },
    Json {
        r#type: String,
    },
}

impl<'de> Deserialize<'de> for ClaimMapping {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = Value::deserialize(deserializer)?;

        let parser = value
            .get("parser")
            .and_then(|v| v.as_str())
            .ok_or_else(|| de::Error::missing_field("parser"))?;

        match parser {
            "regex" => {
                let r#type = value
                    .get("type")
                    .and_then(|v| v.as_str())
                    .ok_or(de::Error::missing_field("type"))?
                    .to_string();

                let regex_expression = value
                    .get("regex_expression")
                    .and_then(|v| v.as_str())
                    .ok_or(de::Error::missing_field("regex_expression"))?
                    .to_string();

                let mut fields = HashMap::new();
                if let Some(obj) = value.as_object() {
                    for (key, val) in obj.iter() {
                        if key != "parser" && key != "type" && key != "regex_expression" {
                            let field = serde_json::from_value::<RegexField>(val.clone())
                                .map_err(de::Error::custom)?;
                            fields.insert(key.clone(), field);
                        }
                    }
                }
                Ok(ClaimMapping::Regex {
                    r#type,
                    regex_expression,
                    fields,
                })
            },
            "json" => {
                let r#type = value
                    .get("type")
                    .and_then(|v| v.as_str())
                    .ok_or_else(|| de::Error::missing_field("type"))?
                    .to_string();

                Ok(ClaimMapping::Json { r#type })
            },
            _ => Err(de::Error::custom("unknown parser type")),
        }
    }
}

#[derive(Debug, PartialEq, Deserialize, Clone)]
pub struct RegexField {
    pub attr: String,
    pub r#type: String,
}

#[cfg(test)]
mod test {
    use super::ClaimMapping;
    use crate::common::policy_store::claim_mapping::RegexField;
    use serde_json::json;
    use std::collections::HashMap;
    use test_utils::assert_eq;

    #[test]
    /// Tests if a token entity metadata with a RegEx parser can be parsed
    /// from a JSON string
    fn can_parse_regex_from_json() {
        // Setup expected output
        let expected = ClaimMapping::Regex {
            r#type: "Acme::Email".to_string(),
            regex_expression: r#"^(?P<UID>[^@]+)@(?P<DOMAIN>.+)$"#.to_string(),
            fields: HashMap::from([
                (
                    "UID".to_string(),
                    RegexField {
                        attr: "uid".to_string(),
                        r#type: "string".to_string(),
                    },
                ),
                (
                    "DOMAIN".to_string(),
                    RegexField {
                        attr: "domain".to_string(),
                        r#type: "string".to_string(),
                    },
                ),
            ]),
        };

        // Setup JSON
        let claim_mapping_json = json!({
            "parser": "regex",
            "type": "Acme::Email",
            "regex_expression": "^(?P<UID>[^@]+)@(?P<DOMAIN>.+)$",
            "UID": { "attr": "uid", "type": "string" },
            "DOMAIN": { "attr": "domain", "type": "string" },
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

    #[test]
    /// Tests if a token entity metadata with a JSON parser can be parsed
    /// from a JSON string
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

    #[test]
    /// Tests if a token entity metadata with a RegEx parser can be parsed
    /// from a YAML string
    fn can_parse_regex_from_yaml() {
        // Setup expected output
        let expected = ClaimMapping::Regex {
            r#type: "Acme::Email".to_string(),
            regex_expression: r#"^(?P<UID>[^@]+)@(?P<DOMAIN>.+)$"#.to_string(),
            fields: HashMap::from([
                (
                    "UID".to_string(),
                    RegexField {
                        attr: "uid".to_string(),
                        r#type: "string".to_string(),
                    },
                ),
                (
                    "DOMAIN".to_string(),
                    RegexField {
                        attr: "domain".to_string(),
                        r#type: "string".to_string(),
                    },
                ),
            ]),
        };

        // Setup Yaml
        let claim_mapping_json = "
            parser: 'regex'
            type: 'Acme::Email'
            regex_expression: '^(?P<UID>[^@]+)@(?P<DOMAIN>.+)$'
            UID:
                attr: 'uid'
                type: 'string'
            DOMAIN:
                attr: 'domain'
                type: 'string'
        ";

        // Parse YAML
        let parsed = serde_yml::from_str::<ClaimMapping>(&claim_mapping_json)
            .expect("should parse token entity metadata");

        // Assert if the JSON got parsed correctly
        assert_eq!(
            parsed, expected,
            "Expected the Claim Mapping to be parsed correctly: {:?}",
            parsed
        );
    }

    #[test]
    /// Tests if a token entity metadata with a JSON parser can be parsed
    /// from a YAML string
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

    #[test]
    /// Tests if an error is thrown for an unknown parser type
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
