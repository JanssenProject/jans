// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! `AuthorizeOptions` travel with the request but never take part in the
//! Cedar evaluation, so the same request must give the same decision with
//! and without them.

use serde_json::json;
use test_utils::assert_eq;
use tokio::test;

use super::utils::*;
use crate::tests::utils::cedarling_util::get_cedarling_with_callback;
use crate::tests::utils::test_helpers::{create_test_principal, create_test_unsigned_request};
use crate::{
    AuthorizeMultiIssuerRequest, AuthorizeOptions, BatchAuthorizeMultiIssuerRequest,
    BatchAuthorizeUnsignedRequest, BatchItem, Cedarling, EntityData, RequestUnsigned, TokenInput,
    TraceContext,
};

static UNSIGNED_POLICY_STORE: &str =
    include_str!("../../../test_files/policy-store_no_trusted_issuers.yaml");
static MULTI_ISSUER_POLICY_STORE: &str =
    include_str!("../../../test_files/policy-store-multi-issuer-basic.yaml");

fn trace_options() -> AuthorizeOptions {
    AuthorizeOptions::with_trace(TraceContext::new(
        "exec-1".to_string(),
        "https://gateway.example.com".to_string(),
        "workload-1".to_string(),
    ))
}

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

fn unsigned_principal(is_ok: bool) -> EntityData {
    create_test_principal("Jans::TestPrincipal1", "id1", json!({"is_ok": is_ok}))
        .expect("principal should build")
}

fn issue(id: &str) -> EntityData {
    create_test_principal(
        "Jans::Issue",
        id,
        json!({"org_id": "some_long_id", "country": "US"}),
    )
    .expect("resource should build")
}

fn unsigned_request(is_ok: bool) -> RequestUnsigned {
    create_test_unsigned_request(
        "Jans::Action::\"UpdateForTestPrincipals\"",
        Some(unsigned_principal(is_ok)),
        issue("random_id"),
    )
}

fn dolphin_token() -> TokenInput {
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

fn dolphin_resource(id: &str) -> EntityData {
    EntityData::from_json(
        &json!({
            "cedar_entity_mapping": {"entity_type": "Acme::Resource", "id": id},
            "name": "Approved Dolphin Foods",
        })
        .to_string(),
    )
    .expect("resource should build")
}

fn dolphin_item(id: &str) -> BatchItem {
    BatchItem {
        resource: dolphin_resource(id),
        action: "Acme::Action::\"CheckRoleFoodApprover\"".to_string(),
        context: json!({}),
    }
}

#[test]
async fn options_do_not_change_decision() {
    let cedarling = unsigned_cedarling().await;
    for is_ok in [true, false] {
        let plain = cedarling
            .authorize_unsigned(unsigned_request(is_ok))
            .await
            .expect("plain unsigned request should be authorized");
        let traced = cedarling
            .authorize_unsigned(unsigned_request(is_ok).with_options(trace_options()))
            .await
            .expect("unsigned request with options should be authorized");
        assert_eq!(
            plain.decision, traced.decision,
            "options must not change the unsigned decision (is_ok = {is_ok})"
        );
    }

    let items = || {
        vec![BatchItem {
            resource: issue("a"),
            action: "Jans::Action::\"UpdateForTestPrincipals\"".to_string(),
            context: json!({}),
        }]
    };
    let plain = cedarling
        .authorize_unsigned_batch(BatchAuthorizeUnsignedRequest::new(
            Some(unsigned_principal(true)),
            items(),
        ))
        .await
        .expect("plain unsigned batch should be processed");
    let traced = cedarling
        .authorize_unsigned_batch(
            BatchAuthorizeUnsignedRequest::new(Some(unsigned_principal(true)), items())
                .with_options(trace_options()),
        )
        .await
        .expect("unsigned batch with options should be processed");
    let decisions = |r: &[Result<crate::AuthorizeResult, crate::BatchItemError>]| {
        r.iter()
            .map(|r| r.as_ref().map(|r| r.decision).ok())
            .collect::<Vec<_>>()
    };
    assert_eq!(
        decisions(&plain.results),
        decisions(&traced.results),
        "options must not change unsigned batch decisions"
    );

    let cedarling = multi_issuer_cedarling().await;
    for id in ["ApprovedDolphinFoods", "Other"] {
        let request = || {
            AuthorizeMultiIssuerRequest::new(
                vec![dolphin_token()],
                dolphin_resource(id),
                "Acme::Action::\"CheckRoleFoodApprover\"".to_string(),
            )
        };
        let plain = cedarling
            .authorize_multi_issuer(request())
            .await
            .expect("plain multi-issuer request should be authorized");
        let traced = cedarling
            .authorize_multi_issuer(request().with_options(trace_options()))
            .await
            .expect("multi-issuer request with options should be authorized");
        assert_eq!(
            plain.decision, traced.decision,
            "options must not change the multi-issuer decision (resource = {id})"
        );
    }

    let items = || vec![dolphin_item("ApprovedDolphinFoods"), dolphin_item("Other")];
    let plain = cedarling
        .authorize_multi_issuer_batch(BatchAuthorizeMultiIssuerRequest::new(
            vec![dolphin_token()],
            items(),
        ))
        .await
        .expect("plain multi-issuer batch should be processed");
    let traced = cedarling
        .authorize_multi_issuer_batch(
            BatchAuthorizeMultiIssuerRequest::new(vec![dolphin_token()], items())
                .with_options(trace_options()),
        )
        .await
        .expect("multi-issuer batch with options should be processed");
    let decisions = |r: &[Result<crate::MultiIssuerAuthorizeResult, crate::BatchItemError>]| {
        r.iter()
            .map(|r| r.as_ref().map(|r| r.decision).ok())
            .collect::<Vec<_>>()
    };
    assert_eq!(
        decisions(&plain.results),
        decisions(&traced.results),
        "options must not change multi-issuer batch decisions"
    );
}

#[test]
async fn request_json_without_options_is_default() {
    let request: RequestUnsigned = serde_json::from_value(json!({
        "principal": null,
        "action": "Jans::Action::\"Read\"",
        "resource": {"cedar_entity_mapping": {"entity_type": "Jans::Issue", "id": "1"}},
        "context": {},
    }))
    .expect("request JSON without `options` should still deserialize");
    assert_eq!(
        request.options,
        AuthorizeOptions::default(),
        "missing `options` should give default options"
    );
}

#[test]
async fn request_json_with_trace_options() {
    let request: RequestUnsigned = serde_json::from_value(json!({
        "principal": null,
        "action": "Jans::Action::\"Read\"",
        "resource": {"cedar_entity_mapping": {"entity_type": "Jans::Issue", "id": "1"}},
        "context": {},
        "options": {"trace": {
            "execution_id": "e",
            "execution_authority": "a",
            "workload_id": "w",
        }},
    }))
    .expect("request JSON with trace options should deserialize");
    let trace = request
        .options
        .trace
        .expect("trace context should be present");
    assert_eq!(
        trace,
        TraceContext::new("e".to_string(), "a".to_string(), "w".to_string()),
        "trace context fields should come from JSON"
    );
    assert_eq!(
        trace.workload_instance_id, None,
        "missing workload_instance_id should be None"
    );
}

#[test]
async fn trace_context_json_requires_mandatory_fields() {
    let err = serde_json::from_value::<TraceContext>(json!({
        "execution_id": "e",
        "execution_authority": "a",
    }))
    .expect_err("trace context without workload_id should be rejected");
    assert!(
        err.to_string().contains("workload_id"),
        "error should name the missing field, got: {err}"
    );
}
