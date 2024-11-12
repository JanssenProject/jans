/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::AgamaPolicyStore;
use super::ParsePolicySetMessage;
use super::PolicyStore;
use super::{parse_option_hashmap, parse_option_string};
use crate::common::policy_store::parse_cedar_version;
use base64::prelude::*;
use cedar_policy::Policy;
use cedar_policy::PolicyId;
use serde::Deserialize;
use serde_json::json;
use std::collections::HashMap;
use std::str::FromStr;
use test_utils::assert_eq;

/// Tests successful deserialization of a valid policy store JSON.
#[test]
fn test_policy_store_deserialization_success() {
    let policy = r#"
        permit (
            principal is Jans::Workload, 
            action in [Jans::Action::"Update"], 
            resource is Jans::Issue
        ) when { 
            principal.org_id == resource.org_id 
        };
    "#;
    // check if the string is a valid policy
    cedar_policy::Policy::from_str(policy).expect("invalid cedar policy");

    let schema = include_str!("./cedar-schema.json");
    // check if the string is a valid schema
    cedar_policy::Schema::from_json_str(schema).expect("invalid cedar schema");

    // represents the `policy_store.json`
    let policy_store_json = json!({
        "cedar_version": "v4.0.0",
        "cedar_policies": {
            "840da5d85403f35ea76519ed1a18a33989f855bf1cf8": {
                "description": "simple policy example",
                "creation_date": "2024-09-20T17:22:39.996050",
                "policy_content": BASE64_STANDARD.encode(policy)
            }
        },
        "cedar_schema": BASE64_STANDARD.encode(schema),
    });

    serde_json::from_str::<PolicyStore>(policy_store_json.to_string().as_str()).unwrap();
}

#[test]
/// Tests for base64 decoding error in the policy store.
fn test_base64_decoding_error_in_policy_store() {
    let policy = r#"
        permit (
            principal is Jans::Workload, 
            action in [Jans::Action::"Update"], 
            resource is Jans::Issue
        ) when { 
            principal.org_id == resource.org_id 
        };
    "#;
    // check if the string is a valid policy
    cedar_policy::Policy::from_str(policy).expect("invalid cedar policy");
    let mut encoded_policy = BASE64_STANDARD.encode(policy);
    // simulate an invalid base64 encoding by adding an invalid character
    encoded_policy.push('!');

    let schema = include_str!("./cedar-schema.json");
    // check if the string is a valid schema
    cedar_policy::Schema::from_json_str(schema).expect("invalid cedar schema");

    // represents the `policy_store.json`
    let policy_store_json = json!({
        "cedar_version": "v4.0.0",
        "cedar_policies": {
            "840da5d85403f35ea76519ed1a18a33989f855bf1cf8": {
                "description": "simple policy example",
                "creation_date": "2024-09-20T17:22:39.996050",
                "policy_content": encoded_policy,
            }
        },
        "cedar_schema": BASE64_STANDARD.encode(schema),
    });

    let policy_result = serde_json::from_str::<PolicyStore>(policy_store_json.to_string().as_str());
    assert!(policy_result
        .unwrap_err()
        .to_string()
        .contains(&ParsePolicySetMessage::Base64.to_string()));
}

/// Tests for parsing error due to broken UTF-8 in the policy store.
#[test]
fn test_policy_parsing_error_in_policy_store() {
    let policy = r#"
        permit (
            principal is Jans::Workload, 
            action in [Jans::Action::"Update"], 
            resource is Jans::Issue
        ) when { 
            principal.org_id == resource.org_id 
        };
    "#;
    // check if the string is a valid policy
    cedar_policy::Policy::from_str(policy).expect("invalid cedar policy");

    // base64 encode the policy
    let mut encoded_policy = BASE64_STANDARD.encode(policy);

    // Simulate invalid UTF-8 by manually inserting invalid byte sequences
    let mut invalid_utf8_bytes = BASE64_STANDARD
        .decode(&encoded_policy)
        .expect("Failed to decode Base64");
    invalid_utf8_bytes[10] = 0xFF; // inserting invalid byte
    encoded_policy = BASE64_STANDARD.encode(&invalid_utf8_bytes); // re-encode with invalid UTF-8

    let schema = include_str!("./cedar-schema.json");
    // check if the string is a valid schema
    cedar_policy::Schema::from_json_str(schema).expect("invalid cedar schema");

    // represents the `policy_store.json`
    let policy_store_json = json!({
        "cedar_version": "v4.0.0",
        "cedar_policies": {
            "840da5d85403f35ea76519ed1a18a33989f855bf1cf8": {
                "description": "simple policy example",
                "creation_date": "2024-09-20T17:22:39.996050",
                "policy_content": encoded_policy,
            }
        },
        "cedar_schema": BASE64_STANDARD.encode(schema),
    });

    let policy_result = serde_json::from_str::<PolicyStore>(policy_store_json.to_string().as_str());
    assert!(policy_result
        .unwrap_err()
        .to_string()
        .contains(&ParsePolicySetMessage::String.to_string()));
}

/// Tests for broken policy parsing error in the policy store.
#[test]
fn test_broken_policy_parsing_error_in_policy_store() {
    static POLICY_STORE_RAW_YAML: &str =
        include_str!("../../../../test_files/policy-store_policy_err_broken_policy.yaml");

    let policy_result = serde_yml::from_str::<AgamaPolicyStore>(POLICY_STORE_RAW_YAML);
    let err_msg = policy_result.unwrap_err().to_string();

    // TODO: this isn't really a human readable format but the current plan is to fetch it from
    // a which will respond with the policy encoded in base64. This could probably be improved
    // in the future once the structure of the project is clearer.
    assert_eq!(err_msg, "policy_stores.ba1f39115ed86ed760ee0bea1d529b52189e5a117474: Errors encountered while parsing policies: [Error(\"unable to decode policy with id: 840da5d85403f35ea76519ed1a18a33989f855bf1cf8, error: unable to decode policy_content from human readable format: unexpected token `)`\")] at line 4 column 5")
}

/// Tests that a valid version string is accepted.
#[test]
fn test_valid_version() {
    let valid_version = "1.2.3".to_string();
    assert!(parse_cedar_version(serde_json::Value::String(valid_version)).is_ok());
}

/// Tests that a valid version string with 'v' prefix is accepted.
#[test]
fn test_valid_version_with_v() {
    let valid_version_with_v = "v1.2.3".to_string();
    assert!(parse_cedar_version(serde_json::Value::String(valid_version_with_v)).is_ok());
}

/// Tests that an invalid version format is rejected.
#[test]
fn test_invalid_version_format() {
    let invalid_version = "1.2".to_string();
    assert!(parse_cedar_version(serde_json::Value::String(invalid_version)).is_err());
}

/// Tests that an invalid version part (non-numeric) is rejected.
#[test]
fn test_invalid_version_part() {
    let invalid_version = "1.two.3".to_string();
    assert!(parse_cedar_version(serde_json::Value::String(invalid_version)).is_err());
}

/// Tests that an invalid version format with 'v' prefix is rejected.
#[test]
fn test_invalid_version_format_with_v() {
    let invalid_version_with_v = "v1.2".to_string();
    assert!(parse_cedar_version(serde_json::Value::String(invalid_version_with_v)).is_err());
}

#[test]
fn test_parsing_policy_store_from_yaml() {
    use super::parse_cedar_policy_new;

    let expected_policies = Vec::from([
        Policy::parse(
            Some(PolicyId::new("some_policy_id")),
            r#"permit(
  principal is Jans::Workload,
  action in [Jans::Action::"Update"],
  resource is Jans::Issue
)when{
  principal.org_id == resource.org_id
};"#,
        )
        .expect("Should parse Cedar policy."),
        Policy::parse(
            Some(PolicyId::new("another_policy_id")),
            r#"permit(
  principal is Jans::User,
  action in [Jans::Action::"Update"],
  resource is Jans::Issue
)when{
  principal.country == resource.country
};"#,
        )
        .expect("Should parse Cedar policy."),
    ]);

    #[derive(Debug, Deserialize)]
    struct TestPolicyStore {
        #[serde(alias = "policies", deserialize_with = "parse_cedar_policy_new")]
        pub cedar_policies: cedar_policy::PolicySet,
    }

    static POLICY_STORE_RAW_YAML: &str = include_str!("./policy-store_ok.yaml");
    let parsed = serde_yml::from_str::<TestPolicyStore>(POLICY_STORE_RAW_YAML)
        .expect("should parse policy store from YAML");
    let parsed_policies = parsed
        .cedar_policies
        .policies()
        .cloned()
        .collect::<Vec<Policy>>();

    // Check that all expected policies are found in the parsed set
    for policy in &expected_policies {
        assert!(
            parsed_policies.contains(policy),
            "Expected to find policy in the parsed policies: {:?}",
            policy.id()
        );
    }
}

#[test]
fn test_parse_option_string() {
    #[derive(Deserialize)]
    struct Data {
        #[serde(deserialize_with = "parse_option_string", default)]
        maybe_string: Option<String>,
    }

    // If key can not be found in the JSON, we expect it to be
    // deserialized into None.
    let json = json!({});
    let deserialized = serde_json::from_value::<Data>(json).expect("Should parse JSON");
    assert_eq!(deserialized.maybe_string, None);

    // If the value is an empty String, we expect it to be
    // deserialized into None.
    let json = json!({
        "maybe_string": ""
    });
    let deserialized = serde_json::from_value::<Data>(json).expect("Should parse JSON");
    assert_eq!(deserialized.maybe_string, None);

    // If the value is a non-empty String, we expect it to be
    // deserialized into Some(String).
    let json = json!({
        "maybe_string": "some_string"
    });
    let deserialized = serde_json::from_value::<Data>(json).expect("Should parse JSON");
    assert_eq!(deserialized.maybe_string, Some("some_string".to_string()));
}

#[test]
fn test_parse_option_hashmap() {
    #[derive(Deserialize)]
    struct Data {
        #[serde(deserialize_with = "parse_option_hashmap", default)]
        maybe_hashmap: Option<HashMap<String, String>>,
    }

    // If key can not be found in the JSON, we expect it to be
    // deserialized into None.
    let json = json!({});
    let deserialized = serde_json::from_value::<Data>(json).expect("Should parse JSON");
    assert_eq!(deserialized.maybe_hashmap, None);

    // If the value is an empty Object, we expect it to be
    // deserialized into None.
    let json = json!({
        "maybe_hashmap": {}
    });
    let deserialized = serde_json::from_value::<Data>(json).expect("Should parse JSON");
    assert_eq!(deserialized.maybe_hashmap, None);

    // If the value is a non-empty String, we expect it to be
    // deserialized into Some(String).
    let json = json!({
        "maybe_hashmap": { "some_key": "some_value" }
    });
    let deserialized = serde_json::from_value::<Data>(json).expect("Should parse JSON");

    assert_eq!(
        deserialized.maybe_hashmap,
        Some(HashMap::from([(
            "some_key".to_string(),
            "some_value".to_string()
        )]))
    );
}
