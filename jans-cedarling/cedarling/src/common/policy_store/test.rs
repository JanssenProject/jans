// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::str::FromStr;

use base64::prelude::*;
use serde::Deserialize;
use serde_json::json;
use test_utils::assert_eq;

use super::{AgamaPolicyStore, ParsePolicySetMessage, PolicyStore, parse_option_string};
use crate::common::policy_store::parse_cedar_version;

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
        "name": "Jans",
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
        "name": "Jans",
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
    assert!(
        policy_result
            .unwrap_err()
            .to_string()
            .contains(&ParsePolicySetMessage::Base64.to_string())
    );
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
        "name": "Jans",
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
    assert!(
        policy_result
            .unwrap_err()
            .to_string()
            .contains(&ParsePolicySetMessage::String.to_string())
    );
}

/// Tests for broken policy parsing error in the policy store.
#[test]
fn test_broken_policy_parsing_error_in_policy_store() {
    static POLICY_STORE_RAW_YAML: &str =
        include_str!("../../../../test_files/policy-store_policy_err_broken_policy.yaml");

    let policy_result = serde_yml::from_str::<AgamaPolicyStore>(POLICY_STORE_RAW_YAML);
    let err_msg = policy_result.unwrap_err().to_string();

    assert!(err_msg.contains("unable to decode policy with id: 840da5d85403f35ea76519ed1a18a33989f855bf1cf8"));
    assert!(err_msg.contains("unable to decode policy_content from human readable format: unexpected token `)`"));
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
fn test_missing_required_fields() {
    let json = json!({
        // Missing cedar_version
        "policy_stores": {
            "test": {
                "name": "test",
                "schema": "test",
                "policies": {}
            }
        }
    });

    let result = serde_json::from_str::<AgamaPolicyStore>(&json.to_string());
    assert!(result.is_err());
    let err = result.unwrap_err();
    assert!(err.to_string().contains("missing required field 'cedar_version' in policy store"));

    let json = json!({
        "cedar_version": "v4.0.0",
        // Missing policy_stores
    });

    let result = serde_json::from_str::<AgamaPolicyStore>(&json.to_string());
    assert!(result.is_err());
    let err = result.unwrap_err();
    assert!(err.to_string().contains("missing required field 'policy_stores' in policy store"));
}

#[test]
fn test_invalid_policy_store_entry() {
    let json = json!({
        "cedar_version": "v4.0.0",
        "policy_stores": {
            "test": {
                // Missing name
                "schema": "test",
                "policies": {}
            }
        }
    });

    let result = serde_json::from_str::<AgamaPolicyStore>(&json.to_string());
    assert!(result.is_err());
    let err = result.unwrap_err();
    assert!(err.to_string().contains("missing required field 'name' in policy store entry"));

    let json = json!({
        "cedar_version": "v4.0.0",
        "policy_stores": {
            "test": {
                "name": "test",
                // Missing schema
                "policies": {}
            }
        }
    });

    let result = serde_json::from_str::<AgamaPolicyStore>(&json.to_string());
    assert!(result.is_err());
    let err = result.unwrap_err();
    assert!(err.to_string().contains("missing required field 'schema' or 'cedar_schema' in policy store entry"));

    let json = json!({
        "cedar_version": "v4.0.0",
        "policy_stores": {
            "test": {
                "name": "test",
                "schema": "test",
                // Missing policies
            }
        }
    });

    let result = serde_json::from_str::<AgamaPolicyStore>(&json.to_string());
    assert!(result.is_err());
    let err = result.unwrap_err();
    assert!(err.to_string().contains("missing required field 'policies' or 'cedar_policies' in policy store entry"));
}

#[test]
fn test_invalid_cedar_version() {
    let json = json!({
        "cedar_version": "invalid",
        "policy_stores": {
            "test": {
                "name": "test",
                "schema": "test",
                "policies": {}
            }
        }
    });

    let result = serde_json::from_str::<AgamaPolicyStore>(&json.to_string());
    assert!(result.is_err());
    let err = result.unwrap_err();
    assert!(err.to_string().contains("invalid cedar_version format"));
}

#[test]
fn test_invalid_schema_format() {
    let json = json!({
        "cedar_version": "v4.0.0",
        "policy_stores": {
            "test": {
                "name": "test",
                "schema": "invalid_schema",
                "policies": {}
            }
        }
    });

    let result = serde_json::from_str::<AgamaPolicyStore>(&json.to_string());
    assert!(result.is_err());
    let err = result.unwrap_err();
    assert!(err.to_string().contains("error parsing schema"));
}

#[test]
fn test_invalid_policies_format() {
    let schema = base64::prelude::BASE64_STANDARD.encode("{}"); // minimal valid JSON schema
    let json = json!({
        "cedar_version": "v4.0.0",
        "policy_stores": {
            "test": {
                "name": "test",
                "schema": schema,
                "policies": {
                    "invalid_policy": {
                        "description": "test",
                        "policy_content": "invalid_content"
                    }
                }
            }
        }
    });

    let result = serde_json::from_str::<AgamaPolicyStore>(&json.to_string());
    assert!(result.is_err());
    let err = result.unwrap_err();
    println!("actual error: {}", err);
    assert!(err.to_string().contains("unable to decode policy with id"));
}

#[test]
fn test_invalid_trusted_issuers_format() {
    let schema = base64::prelude::BASE64_STANDARD.encode("{}"); // minimal valid JSON schema
    let json = json!({
        "cedar_version": "v4.0.0",
        "policy_stores": {
            "test": {
                "name": "test",
                "schema": schema,
                "policies": {},
                "trusted_issuers": {
                    "invalid_issuer": {
                        "name": "test",
                        "description": "test",
                        "openid_configuration_endpoint": "invalid_url"
                    }
                }
            }
        }
    });

    let result = serde_json::from_str::<AgamaPolicyStore>(&json.to_string());
    assert!(result.is_err());
    let err = result.unwrap_err();
    println!("actual error: {:?}", err);
    assert!(err.to_string().contains("the `\"openid_configuration_endpoint\"` is not a valid url"));
}
