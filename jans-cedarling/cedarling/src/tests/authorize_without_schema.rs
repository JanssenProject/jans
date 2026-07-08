// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use test_utils::assert_eq;
use tokio::test;

use super::utils::*;
use crate::{
    Cedarling,
    authz::request::{AuthorizeMultiIssuerRequest, EntityData, TokenInput},
    tests::utils::cedarling_util::get_cedarling_with_callback,
    tests::utils::test_helpers::{create_test_principal, create_test_unsigned_request},
};

static POLICY_STORE_NO_SCHEMA: &str =
    include_str!("../../../test_files/policy-store_no_schema.yaml");

static POLICY_STORE_WITH_SCHEMA: &str =
    include_str!("../../../test_files/policy-store_no_trusted_issuers.yaml");

static POLICY_STORE_MULTI_ISSUER_NO_SCHEMA: &str =
    include_str!("../../../test_files/policy-store-multi-issuer-no-schema.yaml");

// ── authorize_unsigned without schema ──────────────────────────

/// Schemaless: single principal, Allow
#[test]
async fn test_unsigned_without_schema_allow() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_NO_SCHEMA.to_string()),
        |config| {
            config.authorization_config.strict_schema_validation = false;
        },
    )
    .await;

    let request = create_test_unsigned_request(
        "Jans::Action::\"UpdateForTestPrincipals\"",
        Some(
            create_test_principal("Jans::TestPrincipal1", "id1", json!({"is_ok": true}))
                .expect("principal should build"),
        ),
        create_test_principal("Jans::Issue", "random_id", json!({}))
            .expect("resource should build"),
    );

    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("request should succeed without schema");

    assert!(result.decision, "should be allowed without schema");
    assert_eq!(
        result.response.decision(),
        Decision::Allow,
        "cedar response should allow"
    );
}

/// Schemaless: single principal, Deny via forbid policy
#[test]
async fn test_unsigned_without_schema_deny() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_NO_SCHEMA.to_string()),
        |config| {
            config.authorization_config.strict_schema_validation = false;
        },
    )
    .await;

    let request = create_test_unsigned_request(
        "Jans::Action::\"AlwaysDeny\"",
        Some(
            create_test_principal("Jans::TestPrincipal1", "id1", json!({"is_ok": true}))
                .expect("principal should build"),
        ),
        create_test_principal("Jans::Issue", "random_id", json!({}))
            .expect("resource should build"),
    );

    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("request should succeed without schema");

    assert!(!result.decision, "should be denied by forbid policy");
    assert_eq!(
        result.response.decision(),
        Decision::Deny,
        "cedar response should deny"
    );
}

/// Schema present in store, but strict=false → works without schema validation
#[test]
async fn test_unsigned_schema_present_flag_false() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_WITH_SCHEMA.to_string()),
        |config| {
            config.authorization_config.strict_schema_validation = false;
        },
    )
    .await;

    let request = create_test_unsigned_request(
        "Jans::Action::\"UpdateForTestPrincipals\"",
        Some(
            create_test_principal("Jans::TestPrincipal1", "id1", json!({"is_ok": true}))
                .expect("principal should build"),
        ),
        create_test_principal("Jans::Issue", "random_id", json!({"org_id": "some_long_id", "country": "US"}))
            .expect("resource should build"),
    );

    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("request should succeed with schema present and strict=false");

    assert!(result.decision, "should be allowed");
    assert_eq!(
        result.response.decision(),
        Decision::Allow,
        "cedar response should allow"
    );
}

/// strict=true + no schema → init error
#[test]
async fn test_unsigned_strict_true_missing_schema_fails_init() {
    let mut config = crate::tests::utils::cedarling_util::get_config(
        PolicyStoreSource::Yaml(POLICY_STORE_NO_SCHEMA.to_string()),
    );
    config.authorization_config.strict_schema_validation = true;

    let result = Cedarling::new(&config).await;

    assert!(
        result.is_err(),
        "init should fail: strict=true but no schema in policy store"
    );
}

/// Schemaless: no principal → residual evaluation
#[test]
async fn test_unsigned_no_principal_without_schema() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_NO_SCHEMA.to_string()),
        |config| {
            config.authorization_config.strict_schema_validation = false;
        },
    )
    .await;

    let request = create_test_unsigned_request(
        "Jans::Action::\"OpenPublicIssue\"",
        None,
        create_test_principal("Jans::Issue", "random_id", json!({}))
            .expect("resource should build"),
    );

    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("no-principal request should succeed without schema");

    assert!(result.decision, "no-principal public action should be allowed");
    assert_eq!(
        result.response.decision(),
        Decision::Allow,
        "cedar response should allow"
    );
}

// ── authorize_multi_issuer without schema ──────────────────────

/// Helper: create Cedarling with multi-issuer no-schema policy store
async fn get_cedarling_for_multi_issuer_no_schema_tests() -> Cedarling {
    get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_MULTI_ISSUER_NO_SCHEMA.to_string()),
        |config| {
            config.authorization_config.strict_schema_validation = false;
        },
    )
    .await
}

/// Multi-issuer without schema: single token, Allow
#[test]
async fn test_multi_issuer_without_schema_single_token() {
    let cedarling = get_cedarling_for_multi_issuer_no_schema_tests().await;

    let dolphin_token = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_123",
        "jti": "dolphin123",
        "client_id": "dolphin_client_123",
        "aud": "dolphin_audience",
        "location": ["miami", "orlando"],
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let request = AuthorizeMultiIssuerRequest::new_with_fields(
        vec![
            TokenInput::new("Dolphin::Access_Token".to_string(), dolphin_token),
        ],
        EntityData::from_json(
            &json!({
                "cedar_entity_mapping": {
                    "entity_type": "Acme::Resource",
                    "id": "ApprovedDolphinFoods"
                },
                "name": "Approved Dolphin Foods"
            })
            .to_string(),
        )
        .expect("resource entity should build"),
        "Acme::Action::\"GetFood\"".to_string(),
        None,
    );

    let result = cedarling
        .authorize_multi_issuer(request)
        .await
        .expect("multi-issuer request should succeed without schema");

    assert!(result.decision, "should be allowed without schema");
}

/// Multi-issuer without schema: strict=true → init error (schema-less fixture)
#[test]
async fn test_multi_issuer_without_schema_strict_true_fails() {
    let mut config = crate::tests::utils::cedarling_util::get_config(
        PolicyStoreSource::Yaml(POLICY_STORE_MULTI_ISSUER_NO_SCHEMA.to_string()),
    );
    config.authorization_config.strict_schema_validation = true;

    let result = Cedarling::new(&config).await;

    assert!(
        result.is_err(),
        "multi-issuer init should fail: strict=true but policy store has no schema"
    );
}
