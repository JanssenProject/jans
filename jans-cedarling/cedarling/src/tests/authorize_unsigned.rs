// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use test_utils::assert_eq;
use tokio::test;

use super::utils::*;
use crate::log::interface::LogStorage;
use crate::{
    tests::utils::cedarling_util::get_cedarling_with_callback,
    tests::utils::test_helpers::{create_test_principal, create_test_unsigned_request},
};

static POLICY_STORE_RAW_YAML: &str =
    include_str!("../../../test_files/policy-store_no_trusted_issuers.yaml");

/// Single principal, allow result.
#[test]
async fn test_authorize_unsigned_single_principal_allow() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        |_| {},
    )
    .await;

    let request = create_test_unsigned_request(
        "Jans::Action::\"UpdateForTestPrincipals\"",
        Some(
            create_test_principal("Jans::TestPrincipal1", "id1", json!({"is_ok": true}))
                .expect("principal should build"),
        ),
        create_test_principal(
            "Jans::Issue",
            "random_id",
            json!({"org_id": "some_long_id", "country": "US"}),
        )
        .expect("resource should build"),
    );

    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("request should be parsed without errors");

    assert!(result.decision, "request result should be allowed");
    assert_eq!(
        result.response.decision(),
        Decision::Allow,
        "cedar response should allow"
    );
}

/// Single principal, deny result (`principal.is_ok` = false).
#[test]
async fn test_authorize_unsigned_single_principal_deny() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        |_| {},
    )
    .await;

    let request = create_test_unsigned_request(
        "Jans::Action::\"UpdateForTestPrincipals\"",
        Some(
            create_test_principal("Jans::TestPrincipal1", "id1", json!({"is_ok": false}))
                .expect("principal should build"),
        ),
        create_test_principal(
            "Jans::Issue",
            "random_id",
            json!({"org_id": "some_long_id", "country": "US"}),
        )
        .expect("resource should build"),
    );

    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("request should be parsed without errors");

    assert!(!result.decision, "request result should be denied");
    assert_eq!(
        result.response.decision(),
        Decision::Deny,
        "cedar response should deny"
    );
}

/// No principal, partial-eval concretizes because the policy does not depend
/// on the principal attributes.
#[test]
async fn test_authorize_unsigned_no_principal_partial_eval() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        |_| {},
    )
    .await;

    let request = create_test_unsigned_request(
        "Jans::Action::\"OpenPublicIssue\"",
        None,
        create_test_principal(
            "Jans::Issue",
            "random_id",
            json!({"org_id": "some_long_id", "country": "US"}),
        )
        .expect("resource should build"),
    );

    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("request should be parsed without errors");

    assert!(
        result.decision,
        "partial eval should allow when policy does not depend on principal"
    );
}

/// No principal and the only matching permit policy depends on `principal.is_ok`.
/// The partial response cannot concretize, so `execute_authorize` must synthesize
/// a Deny and include the residual policy id in the reason set (fail-closed).
#[test]
async fn test_authorize_unsigned_no_principal_residual_denies() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        |_| {},
    )
    .await;

    let request = create_test_unsigned_request(
        "Jans::Action::\"UpdateForTestPrincipals\"",
        None,
        create_test_principal(
            "Jans::Issue",
            "random_id",
            json!({"org_id": "some_long_id", "country": "US"}),
        )
        .expect("resource should build"),
    );

    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("request should be parsed without errors");

    assert!(
        !result.decision,
        "residual (principal-dependent) policy must fail closed to Deny"
    );
    assert_eq!(
        result.response.decision(),
        Decision::Deny,
        "synthesized cedar response should be Deny"
    );

    let reason_ids: Vec<String> = result
        .response
        .diagnostics()
        .reason()
        .map(ToString::to_string)
        .collect();
    assert!(
        reason_ids.iter().any(|id| id == "5"),
        "residual policy id should be reported in diagnostics reason set, got: {reason_ids:?}"
    );
}

/// Exercises `get_matching_policies_unsigned` with `principal: Some(..)` and
/// with `principal: None`, covering both arms of the `match principal {..}`
/// branch introduced by the single-principal unsigned refactor.
#[test]
async fn test_get_matching_policies_unsigned_both_branches() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        |_| {},
    )
    .await;

    let principal = create_test_principal("Jans::TestPrincipal1", "id1", json!({"is_ok": true}))
        .expect("principal should build");
    let resource = create_test_principal(
        "Jans::Issue",
        "random_id",
        json!({"org_id": "x", "country": "US"}),
    )
    .expect("resource should build");

    let actions = vec!["Jans::Action::\"UpdateForTestPrincipals\"".to_string()];

    let with_principal = cedarling
        .get_matching_policies_unsigned(Some(&principal), &actions, std::slice::from_ref(&resource))
        .expect("Some-principal branch should succeed");
    assert!(
        with_principal.iter().any(|p| p.id == "5"),
        "policy 5 should match when principal type is provided, got: {with_principal:?}"
    );

    let no_principal = cedarling
        .get_matching_policies_unsigned(None, &actions, std::slice::from_ref(&resource))
        .expect("None-principal branch should succeed");
    assert!(
        no_principal.iter().any(|p| p.id == "5"),
        "policy 5 should still match when principal is absent, got: {no_principal:?}"
    );
}

/// Test policy evaluation errors are logged for unsigned authorization
#[test]
async fn test_policy_evaluation_errors_logging_unsigned() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        |_| {},
    )
    .await;

    let principal = create_test_principal(
        "Jans::User",
        "user1",
        json!({"country": "US", "role": ["Admin"], "sub": "user1"}),
    )
    .expect("principal should build");
    let resource = create_test_principal(
        "Jans::Issue",
        "issue1",
        json!({"org_id": "invalid", "country": "US"}),
    )
    .expect("resource should build");

    let request =
        create_test_unsigned_request("Jans::Action::\"AlwaysDeny\"", Some(principal), resource);

    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("request should be parsed without errors");

    // Verify that logs were created and contain the request ID
    let logs = cedarling.pop_logs();
    assert!(!logs.is_empty(), "Should have created logs");

    let logs_with_request_id: Vec<&serde_json::Value> = logs
        .iter()
        .filter(|log| log.get("request_id") == Some(&serde_json::json!(result.request_id)))
        .collect();

    assert!(
        !logs_with_request_id.is_empty(),
        "Should have logs for the request ID"
    );

    for log in &logs_with_request_id {
        assert!(log.get("id").is_some(), "Log should have an id field");
        assert!(
            log.get("timestamp").is_some(),
            "Log should have a timestamp field"
        );
        let log_kind = log.get("log_kind").expect("log_kind should exist");
        assert!(
            log_kind == "Decision" || log_kind == "System",
            "Log kind should be Decision or System, got: {log_kind:?}"
        );

        if log_kind == "Decision" {
            let log_action = log.get("action").expect("Decision log should have action");
            assert_eq!(
                log_action,
                &serde_json::json!("Jans::Action::\"AlwaysDeny\""),
                "Decision log should have the correct action"
            );
            let log_decision = log
                .get("decision")
                .expect("Decision log should have decision");
            assert_eq!(
                log_decision,
                &serde_json::json!("DENY"),
                "Decision log should show DENY decision"
            );
        }
    }
}

/// Verifies that Cedarling can initialize and perform unsigned authorization
/// even when no trusted issuers are configured and JWT signature validation is enabled.
#[test]
async fn test_unsigned_authz_works_without_trusted_issuers() {
    use crate::JwtConfig;
    use jsonwebtoken::Algorithm;
    use std::collections::HashSet;

    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        |config| {
            config.jwt_config = JwtConfig {
                jwks: None,
                jwt_sig_validation: true,
                jwt_status_validation: false,
                signature_algorithms_supported: HashSet::from_iter([
                    Algorithm::HS256,
                    Algorithm::RS256,
                ]),
                ..Default::default()
            };
        },
    )
    .await;

    let request = create_test_unsigned_request(
        "Jans::Action::\"UpdateForTestPrincipals\"",
        Some(
            create_test_principal("Jans::TestPrincipal1", "test_id", json!({"is_ok": true}))
                .expect("principal should build"),
        ),
        create_test_principal(
            "Jans::Issue",
            "issue1",
            json!({"org_id": "some_id", "country": "US"}),
        )
        .expect("resource should build"),
    );

    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("unsigned authorization should work without trusted issuers");

    assert!(result.decision, "authorization should be allowed");

    let logs = cedarling.pop_logs();
    assert!(!logs.is_empty(), "Should have created logs");

    let warning_logs: Vec<&serde_json::Value> = logs
        .iter()
        .filter(|log| {
            log.get("msg")
                .and_then(|m| m.as_str())
                .is_some_and(|m| m.contains("signed authorization is unavailable"))
        })
        .collect();

    assert!(
        !warning_logs.is_empty(),
        "Should have logged a warning about signed authorization being unavailable"
    );
}
