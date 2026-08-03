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
    let report = Cedarling::validate_policy_store(&config.policy_store_config)
        .await
        .expect("infra layer ok");

    assert!(!report.is_ok());
    assert!(report.parse.is_ok_or_skipped());
    assert!(!report.schema.is_ok_or_skipped());
    assert!(report.metadata.is_ok_or_skipped());
}

#[tokio::test]
async fn test_validate_parse_error() {
    let raw_config = BootstrapConfigRaw {
        local_policy_store: Some("{ broken json".to_string()),
        ..Default::default()
    };

    let config: crate::BootstrapConfig = raw_config.try_into().expect("should parse");
    let report = Cedarling::validate_policy_store(&config.policy_store_config)
        .await
        .expect("infra layer ok");

    assert!(!report.is_ok());

    // Parse level should have failed
    match report.parse {
        LevelResult::Failed { errors } => {
            assert!(!errors.is_empty());
        },
        _ => panic!("Expected parse to fail"),
    }

    // Schema and metadata should be skipped because parse failed
    assert!(matches!(report.schema, LevelResult::Skipped { .. }));
    assert!(matches!(report.metadata, LevelResult::Skipped { .. }));
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
    let report = Cedarling::validate_policy_store(&config.policy_store_config)
        .await
        .expect("infra layer ok");

    assert!(!report.is_ok());
    assert!(
        report.parse.is_ok_or_skipped(),
        "Parse failed: {:?}",
        report.parse
    );
    assert!(
        report.schema.is_ok_or_skipped(),
        "Schema failed: {:?}",
        report.schema
    );

    // Metadata level should have failed
    match report.metadata {
        LevelResult::Failed { errors } => {
            assert!(!errors.is_empty());
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
    let report = Cedarling::validate_policy_store(&config.policy_store_config)
        .await
        .expect("infra layer ok");

    assert!(report.is_ok());
}

#[tokio::test]
async fn test_validate_infra_error() {
    let raw_config = BootstrapConfigRaw {
        policy_store_local_fn: Some("../test_files/non_existent_file.yaml".to_string()),
        ..Default::default()
    };

    let config: crate::BootstrapConfig = raw_config.try_into().expect("should parse");
    let result = Cedarling::validate_policy_store(&config.policy_store_config).await;

    assert!(result.is_err());
    // Should be a ValidateInfraError
}

#[tokio::test]
async fn test_validate_schemaless() {
    let mut raw_config = BootstrapConfigRaw::default();

    // Store without a schema and valid cedar_version
    let schemaless_store = json!({
        "cedar_version": "v4.0.0",
        "policy_stores": {
            "test_store": {
                "name": "Test Store",
                "cedar_version": "v4.0.0",
                "policies": {}
            }
        }
    });

    raw_config.local_policy_store = Some(schemaless_store.to_string());
    raw_config.strict_schema_validation = crate::bootstrap_config::FeatureToggle::Disabled;

    let config: crate::BootstrapConfig = raw_config.try_into().expect("should parse");
    let report = Cedarling::validate_policy_store(&config.policy_store_config)
        .await
        .expect("infra layer ok");

    assert!(report.is_ok());
    assert!(report.parse.is_ok_or_skipped());
    assert!(matches!(report.schema, LevelResult::Skipped { .. }));
    assert!(report.metadata.is_ok_or_skipped());
}
