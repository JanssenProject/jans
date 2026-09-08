// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use test_utils::assert_eq;
use tokio::test;

use super::utils::*;
use crate::{
    MetricsError,
    tests::utils::cedarling_util::get_cedarling_with_callback,
    tests::utils::test_helpers::{create_test_principal, create_test_unsigned_request},
};

static POLICY_STORE_NO_SCHEMA: &str =
    include_str!("../../../test_files/policy-store_no_schema.yaml");

/// `metrics_snapshot_get_and_clean` must return a snapshot when
/// `metrics_collection` is enabled and collect counters for the interval.
#[test]
async fn test_metrics_snapshot_enabled_collects_and_resets() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_NO_SCHEMA.to_string()),
        |config| {
            config.authorization_config.strict_schema_validation = false;
            config.authorization_config.metrics_collection = true;
        },
    )
    .await;

    let snapshot = cedarling
        .metrics_snapshot_get_and_clean()
        .expect("snapshot must succeed when metrics collection is enabled");
    assert!(
        snapshot
            .operational_stats
            .contains_key("instance.policy_count"),
        "operational stats must include the policy count gauge"
    );

    let request = create_test_unsigned_request(
        "Jans::Action::\"UpdateForTestPrincipals\"",
        Some(
            create_test_principal("Jans::TestPrincipal1", "id1", json!({"is_ok": true}))
                .expect("principal should build"),
        ),
        create_test_principal("Jans::Issue", "random_id", json!({}))
            .expect("resource should build"),
    );

    let after_authz = cedarling
        .authorize_unsigned(request)
        .await
        .expect("authorization should succeed");
    assert!(after_authz.decision, "authorization should be allowed");

    let snapshot_after = cedarling
        .metrics_snapshot_get_and_clean()
        .expect("snapshot must succeed after authorization");
    assert_eq!(
        snapshot_after.operational_stats.get("authz.requests_total"),
        Some(&1),
        "authorized request must be counted in the interval"
    );

    let snapshot_reset = cedarling
        .metrics_snapshot_get_and_clean()
        .expect("snapshot must succeed after reset");
    assert_eq!(
        snapshot_reset.operational_stats.get("authz.requests_total"),
        Some(&0),
        "counters must reset to a fresh zeroed window after a snapshot"
    );
}

/// `metrics_snapshot_get_and_clean` must fail with `MetricsError::NotEnabled`
/// when `metrics_collection` is left disabled.
#[test]
async fn test_metrics_snapshot_disabled_returns_not_enabled() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_NO_SCHEMA.to_string()),
        |config| {
            config.authorization_config.strict_schema_validation = false;
        },
    )
    .await;

    let err = cedarling
        .metrics_snapshot_get_and_clean()
        .expect_err("snapshot must fail when metrics collection is disabled");
    assert!(
        matches!(err, MetricsError::NotEnabled),
        "expected NotEnabled error, got {err:?}"
    );
}
