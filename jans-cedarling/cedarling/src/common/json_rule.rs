// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use datalogic_rs::Rule;
use serde::{Deserialize, Serialize};
use serde_json::Value;

/// JsonLogic rule using [JsonLogic](https://jsonlogic.com/)
/// Have default implementation:
/// ```json
/// {
///     "and" : [
///         {"==": [{"var": "Jans::Workload"}, "ALLOW"]},
///         {"==": [{"var": "Jans::User"}, "ALLOW"]}
///     ]
/// }
/// ```
#[derive(Debug)]
pub struct JsonRule {
    rule: Rule,
    value: Value,
}

#[derive(Debug, derive_more::Display, derive_more::Error)]
#[display("Parse json rule error: {source}")]
pub struct Error {
    source: datalogic_rs::Error,
}

impl From<datalogic_rs::Error> for Error {
    fn from(source: datalogic_rs::Error) -> Self {
        Self { source }
    }
}

impl JsonRule {
    /// Create a new `JsonRule` from a JSON value.
    pub fn new(rule_value: Value) -> Result<Self, Error> {
        let rule = Rule::from_value(&rule_value)?;

        Ok(JsonRule {
            rule,
            value: rule_value,
        })
    }

    /// Get the underlying `Rule` object.
    #[inline]
    pub(crate) fn rule(&self) -> &Rule {
        &self.rule
    }
}

impl Default for JsonRule {
    fn default() -> Self {
        let value = serde_json::json!({
            "and" : [
                {"==": [{"var": "Jans::Workload"}, "ALLOW"]},
                {"==": [{"var": "Jans::User"}, "ALLOW"]}
            ]
        });

        JsonRule::new(value).unwrap()
    }
}

impl PartialEq for JsonRule {
    fn eq(&self, other: &Self) -> bool {
        self.value == other.value
    }
}

impl<'de> Deserialize<'de> for JsonRule {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = Value::deserialize(deserializer)?;

        let rule = JsonRule::new(value).map_err(|e| {
            serde::de::Error::custom(format!(
                "Failed to deserialize JSON rule: {}",
                e.to_string()
            ))
        })?;
        Ok(rule)
    }
}

impl Serialize for JsonRule {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: serde::Serializer,
    {
        self.value.serialize(serializer)
    }
}
