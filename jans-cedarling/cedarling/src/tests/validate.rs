// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

#![cfg(feature = "tools")]

use crate::{BootstrapConfigRaw, Cedarling, LevelResult};

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
    let policy_store_config = crate::PolicyStoreConfig {
        source: crate::PolicyStoreSource::Yaml(": broken yaml - [".to_string()),
        ..Default::default()
    };
    let http_client_config = crate::http::HttpClientConfig::default();

    let report = Cedarling::validate_policy_store(
        &policy_store_config,
        &http_client_config,
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
    // Valid YAML structure but invalid cedar_version (not semver)
    let bad_metadata_store = r#"
cedar_version: invalid-version-format
policy_stores:
  test_store:
    name: Test Store
    cedar_version: invalid-version-format
    schema:
      encoding: none
      content_type: cedar-json
      body: '{"Jans": {"entityTypes": {}, "actions": {}}}'
    policies: {}
"#;

    let policy_store_config = crate::PolicyStoreConfig {
        source: crate::PolicyStoreSource::Yaml(bad_metadata_store.to_string()),
        ..Default::default()
    };
    let http_client_config = crate::http::HttpClientConfig::default();

    let report = Cedarling::validate_policy_store(
        &policy_store_config,
        &http_client_config,
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
    // Store without a schema and valid cedar_version
    let schemaless_store = r#"
cedar_version: "4.0.0"
policy_stores:
  test_store:
    name: Test Store
    cedar_version: "4.0.0"
    policies: {}
"#;

    let policy_store_config = crate::PolicyStoreConfig {
        source: crate::PolicyStoreSource::Yaml(schemaless_store.to_string()),
        ..Default::default()
    };
    let http_client_config = crate::http::HttpClientConfig::default();

    let report = Cedarling::validate_policy_store(
        &policy_store_config,
        &http_client_config,
    )
        .await
        .expect("infra layer ok");

    assert!(report.is_ok(), "Expected schemaless validation to succeed");
    assert!(report.parse.is_ok_or_skipped(), "Expected parse level to pass or skip: {report:?}");
    assert!(matches!(report.schema, LevelResult::Skipped { .. }), "Expected schema level to be skipped");
    assert!(report.metadata.is_ok_or_skipped(), "Expected metadata level to pass or skip: {report:?}");
}
