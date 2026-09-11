// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::str::FromStr;

use base64::prelude::*;
use serde_json::json;

use super::{LegacyAgamaPolicyStore, LegacyPolicyStore, ParsePolicySetMessage};

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

    serde_json::from_str::<LegacyPolicyStore>(policy_store_json.to_string().as_str())
        .expect("failed to deserialize LegacyPolicyStore from policy_store_json");
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

    let policy_result = serde_yaml_ng::from_str::<LegacyAgamaPolicyStore>(POLICY_STORE_RAW_YAML);
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
fn test_invalid_policy_store_entry() {
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
    result.expect("schema is now optional, should succeed without schema field");

    let json = json!({
        "cedar_version": "v4.0.0",
        "policy_stores": {
            "test": {
                "name": "test",
                "schema": null,
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
fn test_legacy_policy_store_with_null_schema_succeeds() {
    let json = json!({
        "cedar_version": "v4.0.0",
        "policy_stores": {
            "test": {
                "name": "test",
                "schema": null,
                "policies": {}
            }
        }
    });

    let result = serde_json::from_str::<LegacyAgamaPolicyStore>(&json.to_string());
    let agama = result.expect("should deserialize with null schema");
    let (id, legacy_store) = agama.policy_stores.iter().next().expect("has one store");
    assert_eq!(id, "test", "store id should match");
    assert!(
        legacy_store.schema.is_none(),
        "schema should be None when null in JSON"
    );

    // Verify conversion to PolicyStore also yields schema: None
    let store: super::super::PolicyStore = legacy_store.clone().into();
    assert!(
        store.schema.is_none(),
        "converted PolicyStore should have None schema"
    );
}

#[test]
fn test_legacy_policy_store_missing_schema_field_succeeds() {
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
    let agama = result.expect("should deserialize with missing schema field");
    let (_, legacy_store) = agama.policy_stores.iter().next().expect("has one store");
    assert!(
        legacy_store.schema.is_none(),
        "schema should be None when field is absent"
    );

    let store: super::super::PolicyStore = legacy_store.clone().into();
    assert!(
        store.schema.is_none(),
        "converted PolicyStore should have None schema"
    );
}

#[test]
fn test_legacy_policy_store_invalid_schema_format_with_non_null_still_errors() {
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
    let err = result.expect_err("should error on non-null invalid schema");
    assert!(
        err.to_string().contains("error parsing schema"),
        "error should indicate schema parsing failure, got: {err}"
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
        err.to_string().contains("openid_configuration_endpoint")
            && err.to_string().contains("invalid_url"),
        "Error should name the field and the rejected url, got: {err}"
    );
}

/// Builds a legacy policy store carrying a single trusted issuer.
fn policy_store_with_trusted_issuer(issuer: &serde_json::Value) -> String {
    let schema = base64::prelude::BASE64_STANDARD.encode("{}");
    json!({
        "cedar_version": "v4.0.0",
        "policy_stores": {
            "test": {
                "name": "test",
                "schema": schema,
                "policies": {},
                "trusted_issuers": {
                    "test_issuer": issuer
                }
            }
        }
    })
    .to_string()
}

/// Reads back the single trusted issuer's `OpenID` configuration endpoint.
fn parsed_oidc_endpoint(json: &str) -> String {
    let store = serde_json::from_str::<LegacyAgamaPolicyStore>(json).expect("should parse");
    store
        .policy_stores
        .get("test")
        .expect("policy store")
        .trusted_issuers
        .as_ref()
        .expect("trusted issuers")
        .get("test_issuer")
        .expect("trusted issuer")
        .oidc_endpoint
        .to_string()
}

#[test]
fn test_trusted_issuer_derives_endpoint_from_issuer() {
    let json = policy_store_with_trusted_issuer(&json!({
        "name": "test",
        "description": "test",
        "issuer": "https://accounts.test.com"
    }));

    assert_eq!(
        parsed_oidc_endpoint(&json),
        "https://accounts.test.com/.well-known/openid-configuration",
        "Should derive the discovery endpoint from the issuer base url"
    );
}

#[test]
fn test_trusted_issuer_endpoint_takes_precedence_over_issuer() {
    let json = policy_store_with_trusted_issuer(&json!({
        "name": "test",
        "description": "test",
        "issuer": "https://accounts.test.com",
        "openid_configuration_endpoint": "https://accounts.test.com/custom/openid-configuration"
    }));

    assert_eq!(
        parsed_oidc_endpoint(&json),
        "https://accounts.test.com/custom/openid-configuration",
        "Should prefer the explicitly configured endpoint over the derived one"
    );
}

#[test]
fn test_trusted_issuer_without_endpoint_or_issuer_errors() {
    let json = policy_store_with_trusted_issuer(&json!({
        "name": "test",
        "description": "test"
    }));

    let result = serde_json::from_str::<LegacyAgamaPolicyStore>(&json);
    let err = result.expect_err("Expected error when neither field is present");
    assert!(
        err.to_string().contains("issuer"),
        "Error should name the missing fields, got: {err}"
    );
}

#[test]
fn test_trusted_issuer_null_endpoint_does_not_fall_back_to_issuer() {
    let json = policy_store_with_trusted_issuer(&json!({
        "name": "test",
        "description": "test",
        "issuer": "https://accounts.test.com",
        "openid_configuration_endpoint": null
    }));

    let result = serde_json::from_str::<LegacyAgamaPolicyStore>(&json);
    let err = result.expect_err("A present but null endpoint must not derive from the issuer");
    assert!(
        err.to_string().contains("must be a string"),
        "Error should say the endpoint must be a string, got: {err}"
    );
}

#[test]
fn test_trusted_issuer_rejects_non_https_issuer() {
    let json = policy_store_with_trusted_issuer(&json!({
        "name": "test",
        "description": "test",
        "issuer": "http://accounts.test.com"
    }));

    let result = serde_json::from_str::<LegacyAgamaPolicyStore>(&json);
    let err = result.expect_err("An OIDC issuer identifier must use https");
    assert!(
        err.to_string().contains("https"),
        "Error should say the issuer must use https, got: {err}"
    );
}

#[test]
fn test_trusted_issuer_rejects_issuer_with_query() {
    let json = policy_store_with_trusted_issuer(&json!({
        "name": "test",
        "description": "test",
        "issuer": "https://accounts.test.com/realms?tenant=a"
    }));

    let result = serde_json::from_str::<LegacyAgamaPolicyStore>(&json);
    let err = result.expect_err("An issuer carrying a query cannot identify an OIDC provider");
    assert!(
        err.to_string().contains("query"),
        "Error should say the issuer must not carry a query, got: {err}"
    );
}

#[test]
fn test_trusted_issuer_invalid_issuer_url() {
    let json = policy_store_with_trusted_issuer(&json!({
        "name": "test",
        "description": "test",
        "issuer": "invalid_url"
    }));

    let result = serde_json::from_str::<LegacyAgamaPolicyStore>(&json);
    let err = result.expect_err("Expected error for invalid issuer URL");
    assert!(
        err.to_string().contains("invalid_url"),
        "Error should name the rejected issuer url, got: {err}"
    );
}
