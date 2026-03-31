// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::str::FromStr;

use base64::prelude::*;
use serde_json::json;

use super::{LegacyAgamaPolicyStore, LegacyPolicyStore, ParsePolicySetMessage, parse_maybe_cedar_version};

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
    cedar_policy::Policy::from_str(policy).expect("invalid cedar policy");

    let schema = include_str!("../cedar-schema.json");
    cedar_policy::Schema::from_json_str(schema).expect("invalid cedar schema");

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

    serde_json::from_str::<LegacyPolicyStore>(policy_store_json.to_string().as_str()).unwrap();
}

#[test]
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
    cedar_policy::Policy::from_str(policy).expect("invalid cedar policy");
    let mut encoded_policy = BASE64_STANDARD.encode(policy);
    encoded_policy.push('!');

    let schema = include_str!("../cedar-schema.json");
    cedar_policy::Schema::from_json_str(schema).expect("invalid cedar schema");

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

    let policy_result =
        serde_json::from_str::<LegacyPolicyStore>(policy_store_json.to_string().as_str());
    let err =
        policy_result.expect_err("Expected base64 decoding error for invalid base64 character");
    assert!(
        err.to_string()
            .contains(&ParsePolicySetMessage::Base64.to_string()),
        "Error message should indicate base64 decoding failure, got: {err}"
    );
}

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
    cedar_policy::Policy::from_str(policy).expect("invalid cedar policy");

    let mut encoded_policy = BASE64_STANDARD.encode(policy);
    let mut invalid_utf8_bytes = BASE64_STANDARD
        .decode(&encoded_policy)
        .expect("Failed to decode Base64");
    invalid_utf8_bytes[10] = 0xFF;
    encoded_policy = BASE64_STANDARD.encode(&invalid_utf8_bytes);

    let schema = include_str!("../cedar-schema.json");
    cedar_policy::Schema::from_json_str(schema).expect("invalid cedar schema");

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

    let policy_result =
        serde_json::from_str::<LegacyPolicyStore>(policy_store_json.to_string().as_str());
    let err = policy_result.expect_err("Expected UTF-8 parsing error for invalid byte sequence");
    assert!(
        err.to_string()
            .contains(&ParsePolicySetMessage::String.to_string()),
        "Error message should indicate string parsing failure, got: {err}"
    );
}

#[test]
fn test_broken_policy_parsing_error_in_policy_store() {
    static POLICY_STORE_RAW_YAML: &str =
        include_str!("../../../../../test_files/policy-store_policy_err_broken_policy.yaml");

    let policy_result = serde_yml::from_str::<LegacyAgamaPolicyStore>(POLICY_STORE_RAW_YAML);
    let err = policy_result.expect_err("Expected policy parsing error for broken policy syntax");
    let err_msg = err.to_string();

    assert!(
        err_msg.contains(
            "unable to decode policy with id: 840da5d85403f35ea76519ed1a18a33989f855bf1cf8"
        ),
        "Error should identify the policy ID that failed to decode, got: {err_msg}"
    );
    assert!(
        err_msg.contains(
            "unable to decode policy_content from human readable format: this policy is missing the `resource` variable in the scope"
        ),
        "Error should describe the syntax error, got: {err_msg}"
    );
}

#[test]
fn test_valid_version() {
    let valid_version = json!("1.2.3");
    parse_maybe_cedar_version(&valid_version).expect("expected valid Cedar version '1.2.3' to parse");
}

#[test]
fn test_valid_version_with_v() {
    let valid_version_with_v = json!("v1.2.3");
    parse_maybe_cedar_version(&valid_version_with_v).expect("expected valid Cedar version 'v1.2.3' to parse");
}

#[test]
fn test_invalid_version_format() {
    let invalid_version = json!("1.2");
    let err = parse_maybe_cedar_version(&invalid_version)
        .expect_err("Expected error for incomplete version format (missing patch)");
    assert!(
        err.contains("error parsing cedar version"),
        "Error should mention version parsing, got: {err}"
    );
}

#[test]
fn test_invalid_version_part() {
    let invalid_version = json!("1.two.3");
    let err = parse_maybe_cedar_version(&invalid_version)
        .expect_err("Expected error for non-numeric version part");
    assert!(
        err.contains("error parsing cedar version"),
        "Error should mention version parsing, got: {err}"
    );
}

#[test]
fn test_invalid_version_format_with_v() {
    let invalid_version_with_v = json!("v1.2");
    let err = parse_maybe_cedar_version(&invalid_version_with_v)
        .expect_err("Expected error for incomplete version format with v prefix");
    assert!(
        err.contains("error parsing cedar version"),
        "Error should mention version parsing, got: {err}"
    );
}

#[test]
fn test_missing_required_fields() {
    let json = json!({});

    let result = serde_json::from_str::<LegacyAgamaPolicyStore>(&json.to_string());
    let err = result.expect_err("Expected error for missing policy_stores field");
    assert!(
        err.to_string()
            .contains("missing required field 'policy_stores' in policy store"),
        "Error should mention missing policy_stores, got: {err}"
    );
}

#[test]
fn test_invalid_policy_store_entry() {
    let json = json!({
        "cedar_version": "v4.0.0",
        "policy_stores": {
            "test": {
                "schema": "test",
                "policies": {}
            }
        }
    });

    let result = serde_json::from_str::<LegacyAgamaPolicyStore>(&json.to_string());
    let err = result.expect_err("Expected error for missing name in policy store entry");
    assert!(
        err.to_string()
            .contains("missing required field 'name' in policy store entry"),
        "Error should mention missing name field, got: {err}"
    );

    let json = json!({
        "cedar_version": "v4.0.0",
        "policy_stores": {
            "test": {
                "name": "test",
                "policies": {}
            }
        }
    });

    let result = serde_json::from_str::<LegacyAgamaPolicyStore>(&json.to_string());
    let err = result.expect_err("Expected error for missing schema in policy store entry");
    assert!(
        err.to_string()
            .contains("missing required field 'schema' or 'cedar_schema' in policy store entry"),
        "Error should mention missing schema field, got: {err}"
    );

    let json = json!({
        "cedar_version": "v4.0.0",
        "policy_stores": {
            "test": {
                "name": "test",
                "schema": "test",
            }
        }
    });

    let result = serde_json::from_str::<LegacyAgamaPolicyStore>(&json.to_string());
    let err = result.expect_err("Expected error for missing policies in policy store entry");
    assert!(
        err.to_string().contains(
            "missing required field 'policies' or 'cedar_policies' in policy store entry"
        ),
        "Error should mention missing policies field, got: {err}"
    );
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

    let result = serde_json::from_str::<LegacyAgamaPolicyStore>(&json.to_string());
    let err = result.expect_err("Expected error for invalid schema format");
    assert!(
        err.to_string().contains("error parsing schema"),
        "Error should mention schema parsing error, got: {err}"
    );
}

#[test]
fn test_invalid_policies_format() {
    let schema = base64::prelude::BASE64_STANDARD.encode("{}");
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

    let result = serde_json::from_str::<LegacyAgamaPolicyStore>(&json.to_string());
    let err = result.expect_err("Expected error for invalid policy content");
    assert!(
        err.to_string().contains("unable to decode policy with id"),
        "Error should mention unable to decode policy, got: {err}"
    );
}

#[test]
fn test_invalid_trusted_issuers_format() {
    let schema = base64::prelude::BASE64_STANDARD.encode("{}");
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

    let result = serde_json::from_str::<LegacyAgamaPolicyStore>(&json.to_string());
    let err = result.expect_err("Expected error for invalid openid_configuration_endpoint URL");
    assert!(
        err.to_string()
            .contains("the `\"openid_configuration_endpoint\"` or `\"configuration_endpoint\"` is not a valid url"),
        "Error should mention invalid URL, got: {err}"
    );
}
