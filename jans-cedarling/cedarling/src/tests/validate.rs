// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::{BootstrapConfigRaw, Cedarling, LevelResult};
use serde_json::json;

#[tokio::test]
async fn test_validate_schema_error() {
    let raw_config = BootstrapConfigRaw {
        policy_store_local_fn: Some("../test_files/policy-store_schema_error.yaml".to_string()),
        ..Default::default()
    };

    let config: crate::BootstrapConfig = raw_config.try_into().expect("should parse");
    let report = Cedarling::validate_policy_store(
        &config.policy_store_config,
        &config.http_client_config,
    )
        .await
        .expect("infra layer ok");

    assert!(!report.is_ok(), "Expected overall report to fail due to schema errors");
    assert!(report.parse.is_ok_or_skipped(), "Expected parse level to pass or skip: {report:?}");
    assert!(!report.schema.is_ok_or_skipped(), "Expected schema level to fail");
    assert!(report.metadata.is_ok_or_skipped(), "Expected metadata level to pass or skip: {report:?}");
}

#[tokio::test]
async fn test_validate_parse_error() {
    let raw_config = BootstrapConfigRaw {
        local_policy_store: Some("{ broken json".to_string()),
        ..Default::default()
    };

    let config: crate::BootstrapConfig = raw_config.try_into().expect("should parse");
    let report = Cedarling::validate_policy_store(
        &config.policy_store_config,
        &config.http_client_config,
    )
        .await
        .expect("infra layer ok");

    assert!(!report.is_ok(), "Expected overall report to fail due to parse errors");

    // Parse level should have failed
    match report.parse {
        LevelResult::Failed { errors } => {
            assert!(!errors.is_empty(), "Expected parse failure to contain errors");
        },
        _ => panic!("Expected parse to fail"),
    }

    // Schema and metadata should be skipped because parse failed
    assert!(matches!(report.schema, LevelResult::Skipped { .. }), "Expected schema validation to be skipped");
    assert!(matches!(report.metadata, LevelResult::Skipped { .. }), "Expected metadata validation to be skipped");
}

#[tokio::test]
async fn test_validate_metadata_error() {
    let mut raw_config = BootstrapConfigRaw::default();
    // Valid JSON structure but invalid cedar_version (not semver)
    let bad_metadata_store = json!({
        "cedar_version": "invalid-version-format",
        "policy_stores": {
            "test_store": {
                "name": "Test Store",
                "cedar_version": "invalid-version-format",
                "schema": {
                    "encoding": "none",
                    "content_type": "cedar-json",
                    "body": "{\"Jans\": {\"entityTypes\": {}, \"actions\": {}}}"
                },
                "policies": {}
            }
        }
    });

    raw_config.local_policy_store = Some(bad_metadata_store.to_string());

    let config: crate::BootstrapConfig = raw_config.try_into().expect("should parse");
    let report = Cedarling::validate_policy_store(
        &config.policy_store_config,
        &config.http_client_config,
    )
        .await
        .expect("infra layer ok");

    assert!(!report.is_ok(), "Expected overall report to fail due to metadata errors");
    assert!(
        report.parse.is_ok_or_skipped(),
        "Parse failed: {report:?}"
    );
    assert!(
        report.schema.is_ok_or_skipped(),
        "Schema failed: {report:?}"
    );

    // Metadata level should have failed
    match report.metadata {
        LevelResult::Failed { errors } => {
            assert!(!errors.is_empty(), "Expected metadata failure to contain errors");
        },
        _ => panic!("Expected metadata to fail"),
    }
}

#[tokio::test]
async fn test_validate_ok() {
    let raw_config = BootstrapConfigRaw {
        policy_store_local_fn: Some("../test_files/policy-store_ok.yaml".to_string()),
        ..Default::default()
    };

    let config: crate::BootstrapConfig = raw_config.try_into().expect("should parse");
    let report = Cedarling::validate_policy_store(
        &config.policy_store_config,
        &config.http_client_config,
    )
        .await
        .expect("infra layer ok");

    assert!(report.is_ok(), "Expected report to be fully successful: {report:?}");
}

#[tokio::test]
async fn test_validate_infra_error() {
    let raw_config = BootstrapConfigRaw {
        policy_store_local_fn: Some("../test_files/non_existent_file.yaml".to_string()),
        ..Default::default()
    };

    let config: crate::BootstrapConfig = raw_config.try_into().expect("should parse");
    let result = Cedarling::validate_policy_store(
        &config.policy_store_config,
        &config.http_client_config,
    )
    .await;

    assert!(result.is_err(), "Expected an infrastructure error due to non-existent file");
    // Should be a ValidateInfraError
}

#[tokio::test]
async fn test_validate_schemaless() {
    let mut raw_config = BootstrapConfigRaw::default();

    // Store without a schema and valid cedar_version
    let schemaless_store = json!({
        "cedar_version": "4.0.0",
        "policy_stores": {
            "test_store": {
                "name": "Test Store",
                "cedar_version": "4.0.0",
                "policies": {}
            }
        }
    });

    raw_config.local_policy_store = Some(schemaless_store.to_string());
    raw_config.strict_schema_validation = crate::bootstrap_config::FeatureToggle::Disabled;

    let config: crate::BootstrapConfig = raw_config.try_into().expect("should parse");
    let report = Cedarling::validate_policy_store(
        &config.policy_store_config,
        &config.http_client_config,
    )
        .await
        .expect("infra layer ok");

    assert!(report.is_ok(), "Expected schemaless validation to succeed");
    assert!(report.parse.is_ok_or_skipped(), "Expected parse level to pass or skip: {report:?}");
    assert!(matches!(report.schema, LevelResult::Skipped { .. }), "Expected schema level to be skipped");
    assert!(report.metadata.is_ok_or_skipped(), "Expected metadata level to pass or skip: {report:?}");
}
