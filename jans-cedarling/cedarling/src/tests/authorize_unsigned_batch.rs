// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use test_utils::assert_eq;
use tokio::test;

use super::utils::*;
use crate::{
    AuthorizeResult, BatchAuthorizeUnsignedRequest, BatchItem, BatchItemError, EntityData,
    authz::BatchValidationError,
    tests::utils::cedarling_util::get_cedarling_with_callback,
    tests::utils::test_helpers::create_test_principal,
};

fn expect_ok(r: &Result<AuthorizeResult, BatchItemError>, idx: usize) -> &AuthorizeResult {
    r.as_ref()
        .unwrap_or_else(|e| panic!("item {idx} expected Ok, got Err: {e:?}"))
}

static POLICY_STORE_RAW_YAML: &str =
    include_str!("../../../test_files/policy-store_no_trusted_issuers.yaml");

fn make_issue(id: &str, org_id: &str) -> EntityData {
    create_test_principal(
        "Jans::Issue",
        id,
        json!({"org_id": org_id, "country": "US"}),
    )
    .expect("resource should build")
}

fn make_item(action: &str, resource: EntityData) -> BatchItem {
    BatchItem {
        resource,
        action: action.to_string(),
        context: json!({}),
    }
}

/// Happy path: N=1 batch behaves like a single-item call.
#[test]
async fn batch_unsigned_single_item_allow() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        |_| {},
    )
    .await;

    let request = BatchAuthorizeUnsignedRequest::new(
        Some(
            create_test_principal("Jans::TestPrincipal1", "id1", json!({"is_ok": true}))
                .expect("principal should build"),
        ),
        vec![make_item(
            "Jans::Action::\"UpdateForTestPrincipals\"",
            make_issue("only", "some_long_id"),
        )],
    );

    let response = cedarling
        .authorize_unsigned_batch(request)
        .await
        .expect("batch should be processed");

    assert_eq!(response.results.len(), 1);
    assert!(
        expect_ok(&response.results[0], 0).decision,
        "single item should allow"
    );
    // batch_id is a UUIDv7 — a non-nil value.
    assert!(!response.batch_id.to_string().is_empty());
}

/// Happy path: N=25, mixed allow/deny items keep their per-item decisions and
/// stay in input order.
#[test]
async fn batch_unsigned_n25_mixed_results_ordered() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        |_| {},
    )
    .await;

    let principal = Some(
        create_test_principal("Jans::TestPrincipal1", "id1", json!({"is_ok": true}))
            .expect("principal should build"),
    );

    // 25 items, all should allow with is_ok=true principal and this action.
    let items: Vec<BatchItem> = (0..25)
        .map(|i| {
            make_item(
                "Jans::Action::\"UpdateForTestPrincipals\"",
                make_issue(&format!("issue-{i}"), "some_long_id"),
            )
        })
        .collect();

    let request = BatchAuthorizeUnsignedRequest::new(principal, items);
    let response = cedarling
        .authorize_unsigned_batch(request)
        .await
        .expect("batch should be processed");

    assert_eq!(response.results.len(), 25);
    for (i, result) in response.results.iter().enumerate() {
        assert!(
            expect_ok(result, i).decision,
            "item {i} should allow with is_ok=true principal"
        );
    }
}

/// Deny path: `is_ok=false` principal denies every item.
#[test]
async fn batch_unsigned_deny_propagates_per_item() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        |_| {},
    )
    .await;

    let request = BatchAuthorizeUnsignedRequest::new(
        Some(
            create_test_principal("Jans::TestPrincipal1", "id1", json!({"is_ok": false}))
                .expect("principal should build"),
        ),
        vec![
            make_item(
                "Jans::Action::\"UpdateForTestPrincipals\"",
                make_issue("a", "org"),
            ),
            make_item(
                "Jans::Action::\"UpdateForTestPrincipals\"",
                make_issue("b", "org"),
            ),
        ],
    );

    let response = cedarling
        .authorize_unsigned_batch(request)
        .await
        .expect("batch should be processed");

    assert_eq!(response.results.len(), 2);
    for (i, r) in response.results.iter().enumerate() {
        assert!(!expect_ok(r, i).decision, "is_ok=false must deny");
    }
}

/// Partial-eval fail-closed: `principal: None` and a residual-dependent
/// policy — the item denies without failing the whole batch.
#[test]
async fn batch_unsigned_no_principal_residual_denies_that_item_only() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        |_| {},
    )
    .await;

    let request = BatchAuthorizeUnsignedRequest::new(
        None,
        vec![
            // Item 0 — public action, allowed even without a principal.
            make_item(
                "Jans::Action::\"OpenPublicIssue\"",
                make_issue("public", "org"),
            ),
            // Item 1 — principal-dependent action; residual → fail-closed Deny.
            make_item(
                "Jans::Action::\"UpdateForTestPrincipals\"",
                make_issue("private", "org"),
            ),
        ],
    );

    let response = cedarling
        .authorize_unsigned_batch(request)
        .await
        .expect("batch should be processed");

    assert_eq!(response.results.len(), 2);
    assert!(
        expect_ok(&response.results[0], 0).decision,
        "public action must allow under partial eval"
    );
    assert!(
        !expect_ok(&response.results[1], 1).decision,
        "residual-dependent action must fail closed"
    );
}

/// Empty-items rejection returns Err at the batch level.
#[test]
async fn batch_unsigned_empty_items_rejected() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        |_| {},
    )
    .await;

    let request = BatchAuthorizeUnsignedRequest::new(
        Some(
            create_test_principal("Jans::TestPrincipal1", "id1", json!({"is_ok": true}))
                .expect("principal should build"),
        ),
        vec![],
    );

    let err = cedarling
        .authorize_unsigned_batch(request)
        .await
        .expect_err("empty items should be rejected");

    assert!(
        matches!(
            err,
            crate::AuthorizeError::BatchValidation(BatchValidationError::EmptyItems)
        ),
        "expected EmptyItems error, got: {err:?}"
    );
}

/// Non-object context on an item rejects the whole batch (validation stage).
#[test]
async fn batch_unsigned_non_object_context_rejected() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        |_| {},
    )
    .await;

    let bad_item = BatchItem {
        resource: make_issue("bad", "org"),
        action: "Jans::Action::\"UpdateForTestPrincipals\"".to_string(),
        context: json!(42),
    };
    let good_item = make_item(
        "Jans::Action::\"UpdateForTestPrincipals\"",
        make_issue("good", "org"),
    );

    let request = BatchAuthorizeUnsignedRequest::new(
        Some(
            create_test_principal("Jans::TestPrincipal1", "id1", json!({"is_ok": true}))
                .expect("principal should build"),
        ),
        vec![good_item, bad_item],
    );

    let err = cedarling
        .authorize_unsigned_batch(request)
        .await
        .expect_err("non-object context should be rejected");

    assert!(
        matches!(
            err,
            crate::AuthorizeError::BatchValidation(
                BatchValidationError::InvalidItemContext { index: 1 }
            )
        ),
        "expected InvalidItemContext {{index: 1}}, got: {err:?}"
    );
}

/// A per-item malformed action UID surfaces as `Err(BatchItemError::ActionParse)`
/// at that position; adjacent items still evaluate cleanly.
#[test]
async fn batch_unsigned_bad_action_surfaces_error_only_at_that_item() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        |_| {},
    )
    .await;

    let request = BatchAuthorizeUnsignedRequest::new(
        Some(
            create_test_principal("Jans::TestPrincipal1", "id1", json!({"is_ok": true}))
                .expect("principal should build"),
        ),
        vec![
            make_item(
                "Jans::Action::\"UpdateForTestPrincipals\"",
                make_issue("ok", "org"),
            ),
            // Malformed action — fails EntityUid::from_str.
            make_item("this is not a valid uid", make_issue("bad", "org")),
            make_item(
                "Jans::Action::\"UpdateForTestPrincipals\"",
                make_issue("ok2", "org"),
            ),
        ],
    );

    let response = cedarling
        .authorize_unsigned_batch(request)
        .await
        .expect("batch itself succeeds even when one item has a bad action");

    assert_eq!(response.results.len(), 3);
    assert!(expect_ok(&response.results[0], 0).decision, "item 0 allowed");
    match &response.results[1] {
        Err(BatchItemError::ActionParse { item_index, .. }) => {
            assert_eq!(*item_index, 1, "item_index in error must match position");
        },
        other => panic!("item 1 must surface ActionParse error, got: {other:?}"),
    }
    assert!(expect_ok(&response.results[2], 2).decision, "item 2 allowed");
}

/// Two Err items at non-adjacent positions must not disturb the Ok items
/// between and around them — proves per-item Err partitioning.
#[test]
async fn batch_unsigned_multiple_errors_do_not_leak_across_items() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        |_| {},
    )
    .await;

    let ok_action = "Jans::Action::\"UpdateForTestPrincipals\"";
    let request = BatchAuthorizeUnsignedRequest::new(
        Some(
            create_test_principal("Jans::TestPrincipal1", "id1", json!({"is_ok": true}))
                .expect("principal should build"),
        ),
        vec![
            make_item(ok_action, make_issue("ok-0", "org")),
            make_item("bad-uid-1", make_issue("bad-1", "org")),
            make_item(ok_action, make_issue("ok-2", "org")),
            make_item(ok_action, make_issue("ok-3", "org")),
            make_item("bad-uid-4", make_issue("bad-4", "org")),
            make_item(ok_action, make_issue("ok-5", "org")),
        ],
    );

    let response = cedarling
        .authorize_unsigned_batch(request)
        .await
        .expect("batch itself succeeds");

    assert_eq!(response.results.len(), 6);
    for ok_idx in [0, 2, 3, 5] {
        assert!(
            expect_ok(&response.results[ok_idx], ok_idx).decision,
            "item {ok_idx} must Allow (unaffected by the bad items)"
        );
    }
    for err_idx in [1, 4] {
        match &response.results[err_idx] {
            Err(BatchItemError::ActionParse { item_index, .. }) => {
                assert_eq!(*item_index, err_idx);
            },
            other => panic!("item {err_idx} must surface ActionParse error, got: {other:?}"),
        }
    }
}
