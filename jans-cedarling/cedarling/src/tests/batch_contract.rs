// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Cross-flow contract tests for the batch authorize APIs: result equivalence
//! vs sequence-of-single, shuffle-preserves-order, and `batch_id` UUIDv7 +
//! log correlation. Per-flow tests live alongside each flow's implementation.

use super::utils::cedarling_util::get_cedarling_with_callback;
use super::utils::*;
use crate::authz::request::{
    AuthorizeMultiIssuerRequest, BatchAuthorizeMultiIssuerRequest, BatchAuthorizeUnsignedRequest,
    BatchItem, EntityData, RequestUnsigned, TokenInput,
};
use crate::tests::utils::test_helpers::create_test_principal;
use crate::{
    AuthorizeResult, BatchItemError, Cedarling, LogStorage, MultiIssuerAuthorizeResult,
};

fn expect_ok_unsigned(
    r: &Result<AuthorizeResult, BatchItemError>,
    idx: usize,
) -> &AuthorizeResult {
    r.as_ref()
        .unwrap_or_else(|e| panic!("item {idx} expected Ok, got Err: {e:?}"))
}

fn expect_ok_multi(
    r: &Result<MultiIssuerAuthorizeResult, BatchItemError>,
    idx: usize,
) -> &MultiIssuerAuthorizeResult {
    r.as_ref()
        .unwrap_or_else(|e| panic!("item {idx} expected Ok, got Err: {e:?}"))
}

static UNSIGNED_POLICY_STORE: &str =
    include_str!("../../../test_files/policy-store_no_trusted_issuers.yaml");
static MULTI_ISSUER_POLICY_STORE: &str =
    include_str!("../../../test_files/policy-store-multi-issuer-basic.yaml");

async fn unsigned_cedarling() -> Cedarling {
    get_cedarling_with_callback(
        PolicyStoreSource::Yaml(UNSIGNED_POLICY_STORE.to_string()),
        |_| {},
    )
    .await
}

async fn multi_issuer_cedarling() -> Cedarling {
    get_cedarling_with_callback(
        PolicyStoreSource::Yaml(MULTI_ISSUER_POLICY_STORE.to_string()),
        |_| {},
    )
    .await
}

fn make_issue(id: &str, org_id: &str) -> EntityData {
    create_test_principal(
        "Jans::Issue",
        id,
        json!({"org_id": org_id, "country": "US"}),
    )
    .expect("resource should build")
}

fn unsigned_item(action: &str, resource: EntityData) -> BatchItem {
    BatchItem {
        resource,
        action: action.to_string(),
        context: json!({}),
    }
}

fn dolphin_userinfo_token() -> TokenInput {
    TokenInput::new(
        "Dolphin::Userinfo_token".to_string(),
        generate_token_using_claims(json!({
            "iss": "https://idp.dolphin.sea",
            "sub": "dolphin_user_123",
            "jti": "dolphin_user_123",
            "client_id": "dolphin_client_123",
            "aud": "dolphin_audience",
            "exp": 2_000_000_000,
            "iat": 1_516_239_022,
            "role": ["admin", "user"],
        })),
    )
}

fn approved_resource(id: &str) -> EntityData {
    EntityData::from_json(
        &json!({
            "cedar_entity_mapping": { "entity_type": "Acme::Resource", "id": id },
            "name": "Approved Dolphin Foods",
        })
        .to_string(),
    )
    .expect("resource should build")
}

fn multi_issuer_allow_item() -> BatchItem {
    BatchItem {
        resource: approved_resource("ApprovedDolphinFoods"),
        action: "Acme::Action::\"CheckRoleFoodApprover\"".to_string(),
        context: json!({}),
    }
}

fn multi_issuer_deny_item(resource_id: &str) -> BatchItem {
    BatchItem {
        resource: approved_resource(resource_id),
        action: "Acme::Action::\"CheckRoleFoodApprover\"".to_string(),
        context: json!({}),
    }
}

fn diagnostic_reasons(result_response: &cedar_policy::Response) -> Vec<String> {
    let mut r: Vec<String> = result_response
        .diagnostics()
        .reason()
        .map(ToString::to_string)
        .collect();
    r.sort();
    r
}

// ── Result equivalence ──────────────────────────────────────────────

/// Batch must produce the same per-item decisions and diagnostic reasons as
/// the sequence of single-item calls with the same inputs.
#[tokio::test]
async fn batch_unsigned_matches_sequence_of_single() {
    let cedarling = unsigned_cedarling().await;
    let principal_ok = create_test_principal("Jans::TestPrincipal1", "p1", json!({"is_ok": true}))
        .expect("principal should build");
    let principal_bad =
        create_test_principal("Jans::TestPrincipal1", "p1", json!({"is_ok": false}))
            .expect("principal should build");

    // Two same-shaped items — the allow/deny split comes from swapping
    // principal_ok vs. principal_bad on the two batches below.
    let items = vec![
        unsigned_item(
            "Jans::Action::\"UpdateForTestPrincipals\"",
            make_issue("issue-0", "acme"),
        ),
        unsigned_item(
            "Jans::Action::\"UpdateForTestPrincipals\"",
            make_issue("issue-1", "acme"),
        ),
    ];

    // Sequence-of-single with the allowing principal.
    let mut seq_ok = Vec::with_capacity(items.len());
    for item in &items {
        let r = cedarling
            .authorize_unsigned(RequestUnsigned {
                principal: Some(principal_ok.clone()),
                action: item.action.clone(),
                resource: item.resource.clone(),
                context: item.context.clone(),
            })
            .await
            .expect("single call should succeed");
        seq_ok.push(r);
    }

    // Same inputs via the batch call.
    let batch_ok = cedarling
        .authorize_unsigned_batch(BatchAuthorizeUnsignedRequest::new(
            Some(principal_ok.clone()),
            items.clone(),
        ))
        .await
        .expect("batch should succeed");

    assert_eq!(
        seq_ok.len(),
        batch_ok.results.len(),
        "batch must return same number of results as sequence"
    );
    assert!(
        batch_ok
            .results
            .iter()
            .enumerate()
            .all(|(i, r)| expect_ok_unsigned(r, i).decision),
        "is_ok=true principal must Allow every item — check fixture drift"
    );
    for (i, (s, b)) in seq_ok.iter().zip(batch_ok.results.iter()).enumerate() {
        let bo = expect_ok_unsigned(b, i);
        assert_eq!(s.decision, bo.decision, "decision mismatch at item {i}");
        test_utils::assert_eq!(
            diagnostic_reasons(&s.response),
            diagnostic_reasons(&bo.response),
            "diagnostic reasons mismatch at item {i}"
        );
    }

    // Same equivalence must hold on Deny outcomes, not only Allow.
    let mut seq_bad = Vec::with_capacity(items.len());
    for item in &items {
        let r = cedarling
            .authorize_unsigned(RequestUnsigned {
                principal: Some(principal_bad.clone()),
                action: item.action.clone(),
                resource: item.resource.clone(),
                context: item.context.clone(),
            })
            .await
            .expect("single call should succeed");
        seq_bad.push(r);
    }
    let batch_bad = cedarling
        .authorize_unsigned_batch(BatchAuthorizeUnsignedRequest::new(
            Some(principal_bad),
            items,
        ))
        .await
        .expect("batch should succeed");

    assert!(
        batch_bad
            .results
            .iter()
            .enumerate()
            .all(|(i, r)| !expect_ok_unsigned(r, i).decision),
        "is_ok=false principal must Deny every item — check fixture drift"
    );
    for (i, (s, b)) in seq_bad.iter().zip(batch_bad.results.iter()).enumerate() {
        let bo = expect_ok_unsigned(b, i);
        assert_eq!(s.decision, bo.decision, "deny decision mismatch at item {i}");
        test_utils::assert_eq!(
            diagnostic_reasons(&s.response),
            diagnostic_reasons(&bo.response),
            "deny diagnostic reasons mismatch at item {i}"
        );
    }
}

/// Same equivalence for multi-issuer: token validation once vs. per call must
/// not change per-item outcomes.
#[tokio::test]
async fn batch_multi_issuer_matches_sequence_of_single() {
    let cedarling = multi_issuer_cedarling().await;
    let tokens = vec![dolphin_userinfo_token()];

    // Mix of allow (right resource id) and deny (wrong resource id).
    let items = vec![
        multi_issuer_allow_item(),
        multi_issuer_deny_item("wrong-id-1"),
        multi_issuer_allow_item(),
        multi_issuer_deny_item("wrong-id-2"),
    ];

    let mut sequence = Vec::with_capacity(items.len());
    for item in &items {
        let r = cedarling
            .authorize_multi_issuer(AuthorizeMultiIssuerRequest::new_with_fields(
                tokens.clone(),
                item.resource.clone(),
                item.action.clone(),
                Some(item.context.clone()),
            ))
            .await
            .expect("single multi-issuer call should succeed");
        sequence.push(r);
    }

    let batch = cedarling
        .authorize_multi_issuer_batch(BatchAuthorizeMultiIssuerRequest::new(tokens, items.clone()))
        .await
        .expect("multi-issuer batch should succeed");

    assert_eq!(
        sequence.len(),
        batch.results.len(),
        "multi-issuer batch must return same number of results as sequence"
    );
    // Positive/negative anchors — allow items at 0/2, deny items at 1/3.
    // Guards against a fixture drift where both sides silently all-Deny.
    let decisions: Vec<bool> = batch
        .results
        .iter()
        .enumerate()
        .map(|(i, r)| expect_ok_multi(r, i).decision)
        .collect();
    assert_eq!(
        decisions,
        vec![true, false, true, false],
        "batch decisions must match the allow/deny/allow/deny item pattern"
    );
    for (i, (s, b)) in sequence.iter().zip(batch.results.iter()).enumerate() {
        let bo = expect_ok_multi(b, i);
        assert_eq!(s.decision, bo.decision, "decision mismatch at item {i}");
        test_utils::assert_eq!(
            diagnostic_reasons(&s.response),
            diagnostic_reasons(&bo.response),
            "diagnostic reasons mismatch at item {i}"
        );
    }
}

// ── Shuffle ordering ───────────────────────────────────────────────

/// Reversing input order reverses result order — proves the positional
/// mapping isn't accidentally driven by item content.
#[tokio::test]
async fn batch_unsigned_reverse_order_preserves_positional_mapping() {
    let cedarling = unsigned_cedarling().await;
    let principal = create_test_principal("Jans::TestPrincipal1", "p1", json!({"is_ok": true}))
        .expect("principal");

    // Even index → good action (Allow); odd → bad action (Err/ActionParse).
    let items: Vec<BatchItem> = (0..8)
        .map(|i| {
            let action = if i % 2 == 0 {
                "Jans::Action::\"UpdateForTestPrincipals\""
            } else {
                "this is not a valid uid"
            };
            unsigned_item(action, make_issue(&format!("res-{i}"), "acme"))
        })
        .collect();

    let baseline = cedarling
        .authorize_unsigned_batch(BatchAuthorizeUnsignedRequest::new(
            Some(principal.clone()),
            items.clone(),
        ))
        .await
        .expect("baseline batch should succeed");

    let mut reversed = items;
    reversed.reverse();
    let shuffled = cedarling
        .authorize_unsigned_batch(BatchAuthorizeUnsignedRequest::new(
            Some(principal),
            reversed,
        ))
        .await
        .expect("reversed batch should succeed");

    // Map each per-item Result to a Some(bool) / None slot so an Err from a
    // bad-action item is a distinguishable position (not silently folded into
    // the same "false" as a Cedar-Deny).
    let baseline_slots: Vec<Option<bool>> = baseline
        .results
        .iter()
        .map(|r| r.as_ref().ok().map(|ok| ok.decision))
        .collect();
    let shuffled_slots: Vec<Option<bool>> = shuffled
        .results
        .iter()
        .map(|r| r.as_ref().ok().map(|ok| ok.decision))
        .collect();

    let mut expected = baseline_slots.clone();
    expected.reverse();
    test_utils::assert_eq!(
        shuffled_slots, expected,
        "reversing items must reverse the result positions exactly"
    );
    assert_eq!(
        baseline_slots,
        vec![
            Some(true),
            None,
            Some(true),
            None,
            Some(true),
            None,
            Some(true),
            None
        ],
        "sanity: even indices Ok(true), odd indices Err(ActionParse)"
    );
}

// ── batch_id + log correlation ─────────────────────────────────────

/// `batch_id` is a UUIDv7 and every per-item decision-log entry emitted for
/// the batch is retrievable via `get_logs_by_request_id(batch_id)`.
#[tokio::test]
async fn batch_unsigned_batch_id_is_uuidv7_and_indexes_per_item_logs() {
    let cedarling = unsigned_cedarling().await;
    let principal = create_test_principal("Jans::TestPrincipal1", "p1", json!({"is_ok": true}))
        .expect("principal");

    let items: Vec<BatchItem> = (0..3)
        .map(|i| {
            unsigned_item(
                "Jans::Action::\"UpdateForTestPrincipals\"",
                make_issue(&format!("issue-{i}"), "acme"),
            )
        })
        .collect();

    let response = cedarling
        .authorize_unsigned_batch(BatchAuthorizeUnsignedRequest::new(Some(principal), items))
        .await
        .expect("batch should succeed");

    assert_eq!(
        response.batch_id.version(),
        Some(7),
        "batch_id must be a UUIDv7"
    );

    let batch_id_str = response.batch_id.to_string();
    let logs = cedarling.get_logs_by_request_id(&batch_id_str);
    assert!(
        !logs.is_empty(),
        "logs indexed by batch_id must not be empty"
    );

    for entry in &logs {
        let entry_batch_id = entry
            .get("batch_id")
            .and_then(|v| v.as_str())
            .expect("every batch log entry has a batch_id field");
        assert_eq!(entry_batch_id, batch_id_str);
    }
}
