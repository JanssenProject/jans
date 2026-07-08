// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::utils::cedarling_util::get_cedarling_with_callback;
use super::utils::*;
use crate::Cedarling;
use crate::authz::BatchValidationError;
use crate::authz::request::{
    BatchAuthorizeMultiIssuerRequest, BatchItem, EntityData, TokenInput,
};
use serde_json::json;

async fn get_cedarling_for_multi_issuer_tests() -> Cedarling {
    static POLICY_STORE_RAW_YAML: &str =
        include_str!("../../../test_files/policy-store-multi-issuer-basic.yaml");
    get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        |_config| {},
    )
    .await
}

fn dolphin_userinfo_token() -> TokenInput {
    let payload = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_123",
        "jti": "dolphin_user_123",
        "client_id": "dolphin_client_123",
        "aud": "dolphin_audience",
        "exp": 2_000_000_000,
        "iat": 1_516_239_022,
        "role": ["admin", "user"],
    }));
    TokenInput::new("Dolphin::Userinfo_token".to_string(), payload)
}

fn approved_dolphin_foods_resource(id: &str) -> EntityData {
    EntityData::from_json(
        &json!({
            "cedar_entity_mapping": {
                "entity_type": "Acme::Resource",
                "id": id,
            },
            "name": "Approved Dolphin Foods",
        })
        .to_string(),
    )
    .expect("resource should build")
}

/// The policy in `policy-store-multi-issuer-basic.yaml` allows
/// `CheckRoleFoodApprover` only when `resource == Acme::Resource::"ApprovedDolphinFoods"`.
/// Use this helper for items that should evaluate to Allow.
fn allowing_item() -> BatchItem {
    BatchItem {
        resource: approved_dolphin_foods_resource("ApprovedDolphinFoods"),
        action: "Acme::Action::\"CheckRoleFoodApprover\"".to_string(),
        context: json!({}),
    }
}

/// A different resource id fails the policy's `resource == ApprovedDolphinFoods`
/// clause, so this item evaluates to Deny.
fn denying_item(id: &str) -> BatchItem {
    BatchItem {
        resource: approved_dolphin_foods_resource(id),
        action: "Acme::Action::\"CheckRoleFoodApprover\"".to_string(),
        context: json!({}),
    }
}

/// Happy path: N=1 batch behaves like a single-item call.
#[tokio::test]
async fn batch_multi_issuer_single_item_allow() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;

    let request = BatchAuthorizeMultiIssuerRequest::new(
        vec![dolphin_userinfo_token()],
        vec![allowing_item()],
    );

    let response = cedarling
        .authorize_multi_issuer_batch(request)
        .await
        .expect("batch should be processed");

    assert_eq!(response.results.len(), 1);
    assert!(
        response.results[0].decision,
        "single item should allow via role mapping"
    );
    assert!(!response.batch_id.to_string().is_empty());
}

/// Ordering: alternating allow/deny items must map back to input positions.
#[tokio::test]
async fn batch_multi_issuer_alternating_decisions_preserve_order() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;

    let items: Vec<BatchItem> = (0..10)
        .map(|i| {
            if i % 2 == 0 {
                allowing_item()
            } else {
                denying_item(&format!("wrong-resource-{i}"))
            }
        })
        .collect();

    let request = BatchAuthorizeMultiIssuerRequest::new(vec![dolphin_userinfo_token()], items);
    let response = cedarling
        .authorize_multi_issuer_batch(request)
        .await
        .expect("batch should be processed");

    assert_eq!(response.results.len(), 10);
    for (i, r) in response.results.iter().enumerate() {
        let expected_allow = i % 2 == 0;
        assert_eq!(
            r.decision, expected_allow,
            "item {i} decision must match its input position"
        );
    }
}

/// Happy path: N=25 identical allowing items — checks the batch loop scales
/// and preserves ordering without failing any item.
#[tokio::test]
async fn batch_multi_issuer_n25_all_allow() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;

    let items: Vec<BatchItem> = (0..25).map(|_| allowing_item()).collect();
    let request = BatchAuthorizeMultiIssuerRequest::new(vec![dolphin_userinfo_token()], items);

    let response = cedarling
        .authorize_multi_issuer_batch(request)
        .await
        .expect("batch should be processed");

    assert_eq!(response.results.len(), 25);
    for (i, r) in response.results.iter().enumerate() {
        assert!(r.decision, "item {i} should allow");
    }
}

/// Empty tokens rejected at the batch level.
#[tokio::test]
async fn batch_multi_issuer_empty_tokens_rejected() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;

    let request = BatchAuthorizeMultiIssuerRequest::new(vec![], vec![allowing_item()]);

    let err = cedarling
        .authorize_multi_issuer_batch(request)
        .await
        .expect_err("empty tokens should be rejected");

    assert!(
        matches!(
            err,
            crate::AuthorizeError::BatchValidation(BatchValidationError::EmptyTokens)
        ),
        "expected EmptyTokens, got: {err:?}"
    );
}

/// Empty items rejected at the batch level.
#[tokio::test]
async fn batch_multi_issuer_empty_items_rejected() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;

    let request = BatchAuthorizeMultiIssuerRequest::new(vec![dolphin_userinfo_token()], vec![]);

    let err = cedarling
        .authorize_multi_issuer_batch(request)
        .await
        .expect_err("empty items should be rejected");

    assert!(
        matches!(
            err,
            crate::AuthorizeError::BatchValidation(BatchValidationError::EmptyItems)
        ),
        "expected EmptyItems, got: {err:?}"
    );
}

/// Non-object context on an item is rejected at validate-time.
#[tokio::test]
async fn batch_multi_issuer_non_object_context_rejected() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;

    let bad = BatchItem {
        resource: approved_dolphin_foods_resource("bad"),
        action: "Acme::Action::\"CheckRoleFoodApprover\"".to_string(),
        context: json!("string-not-object"),
    };

    let request = BatchAuthorizeMultiIssuerRequest::new(
        vec![dolphin_userinfo_token()],
        vec![allowing_item(), bad],
    );

    let err = cedarling
        .authorize_multi_issuer_batch(request)
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

/// A per-item malformed action UID synthesizes a Deny for that item but does
/// not fail other items in the batch.
#[tokio::test]
async fn batch_multi_issuer_bad_action_denies_only_that_item() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;

    let request = BatchAuthorizeMultiIssuerRequest::new(
        vec![dolphin_userinfo_token()],
        vec![
            allowing_item(),
            BatchItem {
                resource: approved_dolphin_foods_resource("bad"),
                action: "this is not a valid uid".to_string(),
                context: json!({}),
            },
            allowing_item(),
        ],
    );

    let response = cedarling
        .authorize_multi_issuer_batch(request)
        .await
        .expect("batch succeeds even when one item has a bad action");

    assert_eq!(response.results.len(), 3);
    assert!(response.results[0].decision, "item 0 allowed");
    assert!(
        !response.results[1].decision,
        "item 1 with bad action must fail closed"
    );
    assert!(response.results[2].decision, "item 2 allowed");
}
