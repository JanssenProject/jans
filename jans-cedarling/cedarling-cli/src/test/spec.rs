// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedarling::{CedarEntityMapping, EntityData};
use serde::Deserialize;
use std::collections::HashMap;

fn default_context() -> serde_json::Value {
    serde_json::Value::Object(serde_json::Map::new())
}

#[derive(Debug, Deserialize)]
pub(crate) struct TestFile {
    pub tests: Vec<TestCase>,
}

#[derive(Debug, Deserialize)]
pub(crate) struct TestCase {
    pub name: String,
    pub request: TestRequest,
    pub result: TestExpected,
}

#[derive(Debug, Deserialize)]
pub(crate) struct TestRequest {
    pub principal: Option<TestEntity>,
    pub action: String,
    pub resource: TestEntity,
    #[serde(default = "default_context")]
    pub context: serde_json::Value,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_valid() {
        let yaml = r#"
tests:
  - name: "Test 1"
    request:
      action: "Action"
      resource:
        type: "Res"
        id: "1"
    result:
      decision: Allow
        "#;
        let parsed: Result<TestFile, _> = serde_yaml_ng::from_str(yaml);
        parsed.expect("Valid YAML specification should parse successfully");
    }

    #[test]
    fn test_reject_bad_decision() {
        let yaml = r#"
tests:
  - name: "Test 1"
    request:
      action: "Action"
      resource:
        type: "Res"
        id: "1"
    result:
      decision: Maybe
        "#;
        let parsed: Result<TestFile, _> = serde_yaml_ng::from_str(yaml);
        parsed.expect_err("Invalid decision 'Maybe' should fail to parse");
    }
}

#[derive(Debug, Deserialize)]
pub(crate) struct TestEntity {
    #[serde(rename = "type")]
    pub entity_type: String,
    pub id: String,
    #[serde(default)]
    pub attributes: HashMap<String, serde_json::Value>,
}

#[derive(Debug, Deserialize)]
pub(crate) struct TestExpected {
    pub decision: ExpectedDecision,
    pub reason_ids: Option<Vec<String>>,
    pub num_errors: Option<usize>,
}

#[derive(Debug, Deserialize, PartialEq)]
pub(crate) enum ExpectedDecision {
    Allow,
    Deny,
}

impl From<TestEntity> for EntityData {
    fn from(test_entity: TestEntity) -> Self {
        EntityData {
            cedar_mapping: CedarEntityMapping {
                entity_type: test_entity.entity_type,
                id: test_entity.id,
            },
            attributes: test_entity.attributes,
        }
    }
}
