// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::utils::cedarling_util::get_cedarling_with_callback;
use super::utils::*;
use crate::Cedarling;
use crate::authz::request::{AuthorizeMultiIssuerRequest, EntityData, TokenInput};
use serde_json::json;

/// Helper function to create a Cedarling instance for multi-issuer tests
/// with the standard configuration that disables workload and user entity building
async fn get_cedarling_for_multi_issuer_tests() -> Cedarling {
    static POLICY_STORE_RAW_YAML: &str =
        include_str!("../../../test_files/policy-store-multi-issuer-basic.yaml");

    get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        |config| {
            // for debugging purposes set log level to DEBUG and log to stdout
            // config.log_config.log_level = crate::LogLevel::DEBUG;
            // config.log_config.log_type =
            //     crate::LogTypeConfig::StdOut(crate::log::StdOutLoggerMode::Immediate);
            // Disable workload and user entity building for multi-issuer tests
            // since the policies work with token entities in context, not principal entities
            config.entity_builder_config.build_workload = false;
            config.entity_builder_config.build_user = false;
            config.authorization_config.use_workload_principal = false;
            config.authorization_config.use_user_principal = false;
        },
    )
    .await
}

/// Test single Dolphin user token authorization with role_mapping claim
#[tokio::test]
async fn test_single_dolphin_userinfo_token_role_mapping() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;

    // Create a dolphin_token for the user entity
    let dolphin_user_token = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_123",
        "jti": "dolphin_user_123",
        "client_id": "dolphin_client_123",
        "aud": "dolphin_audience",
        "exp": 2000000000,
        "iat": 1516239022,
        "role": ["admin", "user"]
    }));

    let request = AuthorizeMultiIssuerRequest::new_with_fields(
        vec![TokenInput::new(
            "Dolphin::Userinfo_token".to_string(),
            dolphin_user_token,
        )],
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
        .expect("Failed to create resource entity"),
        "Acme::Action::\"CheckRoleFoodApprover\"".to_string(),
        None,
    );

    let authz_result = cedarling
        .authorize_multi_issuer(request)
        .await
        .expect("Dolphin Userinfo_token token authorization should succeed");

    assert!(
        authz_result.decision,
        "Authorization should be ALLOW for dolphin userinfo token, DENY means role mapping failed"
    );
}

/// Test single Dolphin access token authorization with location claim
#[tokio::test]
async fn test_single_dolphin_access_token_authorization() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;

    let dolphin_access_token = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_123",
        "jti": "dolphin123",
        "client_id": "dolphin_client_123",
        "aud": "dolphin_audience",
        "location": ["miami", "orlando"],
        "scope": ["read", "write"],
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    // Create a dolphin_token for the user entity
    let dolphin_user_token = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_123",
        "jti": "dolphin_user_123",
        "client_id": "dolphin_client_123",
        "aud": "dolphin_audience",
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let request = AuthorizeMultiIssuerRequest::new_with_fields(
        vec![
            TokenInput::new("Dolphin::Access_Token".to_string(), dolphin_access_token),
            TokenInput::new("Dolphin::Dolphin_Token".to_string(), dolphin_user_token),
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
        .expect("Failed to create resource entity"),
        "Acme::Action::\"GetFood\"".to_string(),
        None,
    );

    let authz_result = cedarling
        .authorize_multi_issuer(request)
        .await
        .expect("Dolphin access token authorization should succeed");

    assert!(
        authz_result.decision,
        "Authorization should be ALLOW for dolphin access token"
    );
}

/// Test `authorize_multi_issuer` single Dolphin access token.
/// And use resource from default entities.
#[tokio::test]
async fn test_authorize_multi_issuer_single_token_with_default_resource() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;

    let dolphin_access_token = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_123",
        "jti": "dolphin123",
        "client_id": "dolphin_client_123",
        "aud": "dolphin_audience",
        "location": ["miami", "orlando"],
        "scope": ["read", "write"],
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    // Create a dolphin_token for the user entity
    let dolphin_user_token = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_123",
        "jti": "dolphin_user_123",
        "client_id": "dolphin_client_123",
        "aud": "dolphin_audience",
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let request = AuthorizeMultiIssuerRequest::new_with_fields(
        vec![
            TokenInput::new("Dolphin::Access_Token".to_string(), dolphin_access_token),
            TokenInput::new("Dolphin::Dolphin_Token".to_string(), dolphin_user_token),
        ],
        EntityData::from_json(
            &json!({
                "cedar_entity_mapping": {
                    "entity_type": "Acme::Resource",
                    "id": "1694c954f8a3"
                },
            })
            .to_string(),
        )
        .expect("Failed to create resource entity"),
        "Acme::Action::\"GetFood\"".to_string(),
        None,
    );

    let authz_result = cedarling.authorize_multi_issuer(request).await.expect(
        "Dolphin access token authorization should succeed, maybe default entity is not loaded",
    );

    assert!(
        authz_result.decision,
        "Authorization should be ALLOW for dolphin access token"
    );
}

/// Test single Acme access token authorization with scope claim
#[tokio::test]
async fn test_single_acme_access_token_authorization() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;

    let acme_access_token = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_456",
        "jti": "acme123",
        "client_id": "acme_client_456",
        "scope": ["read:wiki", "write:profile"],
        "aud": "my-client-id",
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let request = AuthorizeMultiIssuerRequest::new_with_fields(
        vec![TokenInput::new(
            "Acme::Access_Token".to_string(),
            acme_access_token,
        )],
        EntityData::from_json(
            &json!({
                "cedar_entity_mapping": {
                    "entity_type": "Acme::Resource",
                    "id": "WikiPages"
                },
                "name": "Wiki Pages"
            })
            .to_string(),
        )
        .expect("Failed to create resource entity"),
        "Acme::Action::\"ReadProfile\"".to_string(),
        None,
    );

    let authz_result = cedarling
        .authorize_multi_issuer(request)
        .await
        .expect("Acme access token authorization should succeed");

    assert!(
        authz_result.decision,
        "Authorization should be ALLOW for acme access token"
    );
}

/// Test single Dolphin custom token authorization with waiver claim
#[tokio::test]
async fn test_single_dolphin_custom_token_authorization() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;

    let dolphin_custom_token = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_789",
        "jti": "dolphin_custom_789",
        "client_id": "dolphin_custom_client_789",
        "aud": "dolphin_custom_audience",
        "waiver": ["signed", "approved"],
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let request = AuthorizeMultiIssuerRequest::new_with_fields(
        vec![TokenInput::new(
            "dolphin_token".to_string(),
            dolphin_custom_token,
        )],
        EntityData::from_json(
            &json!({
                "cedar_entity_mapping": {
                    "entity_type": "Acme::Resource",
                    "id": "MiamiAcquarium"
                },
                "name": "Miami Aquarium"
            })
            .to_string(),
        )
        .expect("Failed to create resource entity"),
        "Acme::Action::\"SwimWithOrca\"".to_string(),
        None,
    );

    let authz_result = cedarling
        .authorize_multi_issuer(request)
        .await
        .expect("Dolphin custom token authorization should succeed");

    assert!(
        authz_result.decision,
        "Authorization should be ALLOW for dolphin custom token"
    );
}

/// Test multiple tokens from different issuers authorization
#[tokio::test]
async fn test_multiple_tokens_from_different_issuers() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;

    let acme_multi_token = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_multi",
        "jti": "acme_multi_123",
        "client_id": "acme_multi_client_123",
        "aud": "acme_multi_audience",
        "scope": ["read:wiki", "write:profile"],
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let dolphin_multi_token = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_multi",
        "jti": "dolphin_multi_456",
        "client_id": "dolphin_multi_client_456",
        "aud": "dolphin_multi_audience",
        "location": ["miami"],
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let request = AuthorizeMultiIssuerRequest::new_with_fields(
        vec![
            TokenInput::new("Acme::Access_Token".to_string(), acme_multi_token),
            TokenInput::new("Dolphin::Access_Token".to_string(), dolphin_multi_token),
        ],
        EntityData::from_json(
            &json!({
                "cedar_entity_mapping": {
                    "entity_type": "Acme::Resource",
                    "id": "WikiPages"
                },
                "name": "Wiki Pages"
            })
            .to_string(),
        )
        .expect("Failed to create resource entity"),
        "Acme::Action::\"ReadProfile\"".to_string(),
        None,
    );

    let authz_result = cedarling
        .authorize_multi_issuer(request)
        .await
        .expect("Multi-token authorization should succeed");

    assert!(
        authz_result.decision,
        "Authorization should be ALLOW for multi-token request"
    );
}

/// Test OR logic - Only Acme token has the required scope
#[tokio::test]
async fn test_or_logic_acme_token_has_required_scope() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;
    let acme_token_with_scope = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_1",
        "jti": "acme_token_1",
        "client_id": "acme_client_1",
        "scope": ["write:documents", "read:profile"],
        "aud": "my-client-id",
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let request = AuthorizeMultiIssuerRequest::new_with_fields(
        vec![TokenInput::new(
            "Acme::Access_Token".to_string(),
            acme_token_with_scope,
        )],
        EntityData::from_json(
            &json!({
                "cedar_entity_mapping": {
                    "entity_type": "Acme::Resource",
                    "id": "SharedDocs"
                },
                "name": "Shared Documents"
            })
            .to_string(),
        )
        .expect("Failed to create resource entity"),
        "Acme::Action::\"ManageDocuments\"".to_string(),
        None,
    );

    let result = cedarling
        .authorize_multi_issuer(request)
        .await
        .expect("Authorization should succeed with Acme token having write:documents scope");

    assert!(
        result.decision,
        "Should be ALLOW when Acme token has required scope"
    );
}

/// Test OR logic - Only Dolphin token has the required scope
#[tokio::test]
async fn test_or_logic_dolphin_token_has_required_scope() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;
    let dolphin_token_with_scope = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_2",
        "jti": "dolphin_token_2",
        "client_id": "dolphin_client_2",
        "scope": ["write:documents"],
        "aud": "dolphin-client",
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let request = AuthorizeMultiIssuerRequest::new_with_fields(
        vec![TokenInput::new(
            "Dolphin::Access_Token".to_string(),
            dolphin_token_with_scope,
        )],
        EntityData::from_json(
            &json!({
                "cedar_entity_mapping": {
                    "entity_type": "Acme::Resource",
                    "id": "SharedDocs"
                },
                "name": "Shared Documents"
            })
            .to_string(),
        )
        .expect("Failed to create resource entity"),
        "Acme::Action::\"ManageDocuments\"".to_string(),
        None,
    );

    let result = cedarling
        .authorize_multi_issuer(request)
        .await
        .expect("Authorization should succeed with Dolphin token having write:documents scope");
    assert!(
        result.decision,
        "Should be ALLOW when Dolphin token has required scope"
    );
}

/// Test OR logic - Both tokens have the required scope
#[tokio::test]
async fn test_or_logic_both_tokens_have_scope() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;
    let acme_token = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_3",
        "jti": "acme_token_3",
        "client_id": "acme_client_3",
        "scope": ["write:documents"],
        "aud": "my-client-id",
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let dolphin_token = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_3",
        "jti": "dolphin_token_3",
        "client_id": "dolphin_client_3",
        "scope": ["write:documents"],
        "aud": "dolphin-client",
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let request = AuthorizeMultiIssuerRequest::new_with_fields(
        vec![
            TokenInput::new("Acme::Access_Token".to_string(), acme_token),
            TokenInput::new("Dolphin::Access_Token".to_string(), dolphin_token),
        ],
        EntityData::from_json(
            &json!({
                "cedar_entity_mapping": {
                    "entity_type": "Acme::Resource",
                    "id": "SharedDocs"
                },
                "name": "Shared Documents"
            })
            .to_string(),
        )
        .expect("Failed to create resource entity"),
        "Acme::Action::\"ManageDocuments\"".to_string(),
        None,
    );

    let result = cedarling
        .authorize_multi_issuer(request)
        .await
        .expect("Authorization should succeed when both tokens have the scope");

    assert!(
        result.decision,
        "Should be ALLOW when both tokens have required scope"
    );
}

/// Test OR logic - Neither token has the required scope (should deny)
#[tokio::test]
async fn test_or_logic_neither_token_has_scope() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;
    let acme_token_no_scope = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_4",
        "jti": "acme_token_4",
        "client_id": "acme_client_4",
        "scope": ["read:profile"],  // Wrong scope
        "aud": "my-client-id",
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let dolphin_token_no_scope = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_4",
        "jti": "dolphin_token_4",
        "client_id": "dolphin_client_4",
        "scope": ["read:data"],  // Wrong scope
        "aud": "dolphin-client",
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let request = AuthorizeMultiIssuerRequest::new_with_fields(
        vec![
            TokenInput::new("Acme::Access_Token".to_string(), acme_token_no_scope),
            TokenInput::new("Dolphin::Access_Token".to_string(), dolphin_token_no_scope),
        ],
        EntityData::from_json(
            &json!({
                "cedar_entity_mapping": {
                    "entity_type": "Acme::Resource",
                    "id": "SharedDocs"
                },
                "name": "Shared Documents"
            })
            .to_string(),
        )
        .expect("Failed to create resource entity"),
        "Acme::Action::\"ManageDocuments\"".to_string(),
        None,
    );

    let result = cedarling
        .authorize_multi_issuer(request)
        .await
        .expect("Authorization should execute successfully");
    assert!(
        !result.decision,
        "Should be DENY when neither token has required scope"
    );
}

/// Test AND logic - Both tokens present with required attributes
#[tokio::test]
async fn test_and_logic_both_tokens_with_required_attributes() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;
    let acme_vote_token = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_5",
        "jti": "acme_token_5",
        "client_id": "acme_client_5",
        "scope": ["trade:vote"],  // Required scope
        "aud": "my-client-id",
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let dolphin_member_token = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_5",
        "jti": "dolphin_token_5",
        "client_id": "dolphin_client_5",
        "member_status": ["verified"],  // Required member status
        "aud": "dolphin-client",
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let request = AuthorizeMultiIssuerRequest::new_with_fields(
        vec![
            TokenInput::new("Acme::Access_Token".to_string(), acme_vote_token),
            TokenInput::new("Dolphin::Access_Token".to_string(), dolphin_member_token),
        ],
        EntityData::from_json(
            &json!({
                "cedar_entity_mapping": {
                    "entity_type": "Acme::Resource",
                    "id": "TradeAssociationElection"
                },
                "name": "Trade Association Election"
            })
            .to_string(),
        )
        .expect("Failed to create resource entity"),
        "Acme::Action::\"SubmitVote\"".to_string(),
        None,
    );

    let result = cedarling
        .authorize_multi_issuer(request)
        .await
        .expect("Authorization should succeed when both tokens have required attributes");
    assert!(
        result.decision,
        "Should be ALLOW when both required tokens are present with correct attributes"
    );
}

/// Test AND logic - Only one token present (should deny)
#[tokio::test]
async fn test_and_logic_only_one_token_present() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;
    let acme_vote_token_only = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_6",
        "jti": "acme_token_6",
        "client_id": "acme_client_6",
        "scope": ["trade:vote"],
        "aud": "my-client-id",
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let request = AuthorizeMultiIssuerRequest::new_with_fields(
        vec![TokenInput::new(
            "Acme::Access_Token".to_string(),
            acme_vote_token_only,
        )],
        EntityData::from_json(
            &json!({
                "cedar_entity_mapping": {
                    "entity_type": "Acme::Resource",
                    "id": "TradeAssociationElection"
                },
                "name": "Trade Association Election"
            })
            .to_string(),
        )
        .expect("Failed to create resource entity"),
        "Acme::Action::\"SubmitVote\"".to_string(),
        None,
    );

    let result = cedarling
        .authorize_multi_issuer(request)
        .await
        .expect("Authorization should execute successfully");
    assert!(
        !result.decision,
        "Should be DENY when only one of two required tokens is present"
    );
}

/// Test AND logic - Both tokens present but missing required attributes (should deny)
#[tokio::test]
async fn test_and_logic_both_tokens_missing_required_attributes() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;
    let acme_wrong_scope = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_7",
        "jti": "acme_token_7",
        "client_id": "acme_client_7",
        "scope": ["read:profile"],  // Wrong scope
        "aud": "my-client-id",
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let dolphin_wrong_status = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_7",
        "jti": "dolphin_token_7",
        "client_id": "dolphin_client_7",
        "member_status": ["pending"],  // Wrong status
        "aud": "dolphin-client",
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let request = AuthorizeMultiIssuerRequest::new_with_fields(
        vec![
            TokenInput::new("Acme::Access_Token".to_string(), acme_wrong_scope),
            TokenInput::new("Dolphin::Access_Token".to_string(), dolphin_wrong_status),
        ],
        EntityData::from_json(
            &json!({
                "cedar_entity_mapping": {
                    "entity_type": "Acme::Resource",
                    "id": "TradeAssociationElection"
                },
                "name": "Trade Association Election"
            })
            .to_string(),
        )
        .expect("Failed to create resource entity"),
        "Acme::Action::\"SubmitVote\"".to_string(),
        None,
    );

    let result = cedarling
        .authorize_multi_issuer(request)
        .await
        .expect("Authorization should execute successfully");
    assert!(
        !result.decision,
        "Should be DENY when tokens don't have required attributes"
    );
}

/// Test custom `DolphinToken` with waiver claim
#[tokio::test]
async fn test_custom_dolphin_token_with_waiver() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;
    let dolphin_custom_token = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_1",
        "jti": "dolphin_custom_1",
        "client_id": "dolphin_client_1",
        "aud": "dolphin-audience",
        "waiver": ["signed", "approved", "notarized"],
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let request = AuthorizeMultiIssuerRequest::new_with_fields(
        vec![TokenInput::new(
            "dolphin_token".to_string(),
            dolphin_custom_token,
        )],
        EntityData::from_json(
            &json!({
                "cedar_entity_mapping": {
                    "entity_type": "Acme::Resource",
                    "id": "MiamiAcquarium"
                },
                "name": "Miami Aquarium"
            })
            .to_string(),
        )
        .expect("Failed to create resource entity"),
        "Acme::Action::\"SwimWithOrca\"".to_string(),
        None,
    );

    let result = cedarling
        .authorize_multi_issuer(request)
        .await
        .expect("Authorization should succeed with custom DolphinToken");

    assert!(
        result.decision,
        "Should be ALLOW when custom token has required waiver claim"
    );
}

/// Test custom token without required claim (should deny)
#[tokio::test]
async fn test_custom_token_without_required_claim() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;
    let dolphin_token_no_waiver = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_2",
        "jti": "dolphin_custom_2",
        "client_id": "dolphin_client_2",
        "aud": "dolphin-audience",
        "waiver": ["unsigned"],  // Wrong waiver status
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let request = AuthorizeMultiIssuerRequest::new_with_fields(
        vec![TokenInput::new(
            "dolphin_token".to_string(),
            dolphin_token_no_waiver,
        )],
        EntityData::from_json(
            &json!({
                "cedar_entity_mapping": {
                    "entity_type": "Acme::Resource",
                    "id": "MiamiAcquarium"
                },
                "name": "Miami Aquarium"
            })
            .to_string(),
        )
        .expect("Failed to create resource entity"),
        "Acme::Action::\"SwimWithOrca\"".to_string(),
        None,
    );

    let result = cedarling
        .authorize_multi_issuer(request)
        .await
        .expect("Authorization should execute successfully");

    assert!(
        !result.decision,
        "Should be DENY when custom token doesn't have correct waiver"
    );
}

/// Test multiple custom token types together
#[tokio::test]
async fn test_multiple_custom_token_types_together() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;
    let acme_access_token = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_3",
        "jti": "acme_token_3",
        "client_id": "acme_client_3",
        "scope": ["read:wiki"],
        "aud": "my-client-id",
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let dolphin_custom = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_3",
        "jti": "dolphin_custom_3",
        "client_id": "dolphin_client_3",
        "aud": "dolphin-audience",
        "waiver": ["signed"],
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let request = AuthorizeMultiIssuerRequest::new_with_fields(
        vec![
            TokenInput::new("Acme::Access_Token".to_string(), acme_access_token),
            TokenInput::new("dolphin_token".to_string(), dolphin_custom),
        ],
        EntityData::from_json(
            &json!({
                "cedar_entity_mapping": {
                    "entity_type": "Acme::Resource",
                    "id": "MiamiAcquarium"
                },
                "name": "Miami Aquarium"
            })
            .to_string(),
        )
        .expect("Failed to create resource entity"),
        "Acme::Action::\"SwimWithOrca\"".to_string(),
        None,
    );

    let result = cedarling
        .authorize_multi_issuer(request)
        .await
        .expect("Authorization should succeed with multiple custom and standard tokens");
    assert!(
        result.decision,
        "Should be ALLOW when custom DolphinToken has required waiver"
    );
}

/// Test custom token type with complex nested claims
#[tokio::test]
async fn test_custom_token_with_complex_nested_claims() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;
    let custom_token_complex = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_4",
        "jti": "dolphin_custom_4",
        "client_id": "dolphin_client_4",
        "aud": "dolphin-audience",
        "waiver": ["signed", "approved"],
        "training_certificates": ["marine_biology", "safety", "first_aid"],
        "clearance_level": ["3"],
        "experience_years": ["5"],
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let request = AuthorizeMultiIssuerRequest::new_with_fields(
        vec![TokenInput::new(
            "dolphin_token".to_string(),
            custom_token_complex,
        )],
        EntityData::from_json(
            &json!({
                "cedar_entity_mapping": {
                    "entity_type": "Acme::Resource",
                    "id": "MiamiAcquarium"
                },
                "name": "Miami Aquarium"
            })
            .to_string(),
        )
        .expect("Failed to create resource entity"),
        "Acme::Action::\"SwimWithOrca\"".to_string(),
        None,
    );

    let result = cedarling.authorize_multi_issuer(request).await.expect(
        "Authorization should succeed with complex custom token containing multiple claims",
    );
    assert!(
        result.decision,
        "Should be ALLOW - policy only checks waiver claim, other claims are preserved as tags"
    );
}

/// Test mix of standard and custom tokens from multiple issuers
/// Demonstrates that custom token types work alongside standard token types
#[tokio::test]
async fn test_mix_of_standard_and_custom_tokens() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;
    let acme_standard = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_5",
        "jti": "acme_token_5",
        "client_id": "acme_client_5",
        "scope": ["write:documents"],
        "aud": "my-client-id",
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let dolphin_custom = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_5",
        "jti": "dolphin_custom_5",
        "client_id": "dolphin_client_5",
        "aud": "dolphin-audience",
        "waiver": ["signed"],
        "marine_certification": ["advanced"],
        "insurance_verified": ["yes"],
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    // Test with ManageDocuments action which accepts tokens from either issuer
    let request = AuthorizeMultiIssuerRequest::new_with_fields(
        vec![
            TokenInput::new("Acme::Access_Token".to_string(), acme_standard),
            TokenInput::new("dolphin_token".to_string(), dolphin_custom),
        ],
        EntityData::from_json(
            &json!({
                "cedar_entity_mapping": {
                    "entity_type": "Acme::Resource",
                    "id": "SharedDocs"
                },
                "name": "Shared Documents"
            })
            .to_string(),
        )
        .expect("Failed to create resource entity"),
        "Acme::Action::\"ManageDocuments\"".to_string(),
        None,
    );

    let result = cedarling
        .authorize_multi_issuer(request)
        .await
        .expect("Authorization should succeed with mix of standard and custom token types");

    assert!(
        result.decision,
        "Should be ALLOW - Acme token has write:documents scope"
    );
}

/// Test validation - Empty token array should fail
#[tokio::test]
async fn test_validation_empty_token_array() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;
    let request = AuthorizeMultiIssuerRequest::new_with_fields(
        vec![], // Empty tokens array
        EntityData::from_json(
            &json!({
                "cedar_entity_mapping": {
                    "entity_type": "Acme::Resource",
                    "id": "SharedDocs"
                },
                "name": "Shared Documents"
            })
            .to_string(),
        )
        .expect("Failed to create resource entity"),
        "Acme::Action::\"ManageDocuments\"".to_string(),
        None,
    );

    let result = cedarling.authorize_multi_issuer(request).await;
    assert!(result.is_err(), "Should fail with empty token array");
}

/// Test validation - Non-deterministic tokens (multiple tokens of same type from same issuer)
/// The system uses graceful validation: first token is kept, duplicates are logged and skipped
#[tokio::test]
async fn test_validation_non_deterministic_tokens() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;
    let acme_token_1 = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_1",
        "jti": "acme_token_1",
        "client_id": "acme_client_1",
        "scope": ["write:documents"],
        "aud": "my-client-id",
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let acme_token_2 = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_2",
        "jti": "acme_token_2",
        "client_id": "acme_client_2",
        "scope": ["read:profile"],
        "aud": "my-client-id",
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let request = AuthorizeMultiIssuerRequest::new_with_fields(
        vec![
            TokenInput::new("Acme::Access_Token".to_string(), acme_token_1),
            TokenInput::new("Acme::Access_Token".to_string(), acme_token_2), // Same type from same issuer
        ],
        EntityData::from_json(
            &json!({
                "cedar_entity_mapping": {
                    "entity_type": "Acme::Resource",
                    "id": "SharedDocs"
                },
                "name": "Shared Documents"
            })
            .to_string(),
        )
        .expect("Failed to create resource entity"),
        "Acme::Action::\"ManageDocuments\"".to_string(),
        None,
    );

    let result = cedarling
        .authorize_multi_issuer(request)
        .await
        .expect("Should handle non-deterministic tokens gracefully (keep first, skip duplicates)");

    assert!(
        result.decision,
        "Should be ALLOW - first Acme token has write:documents scope"
    );
}

/// Test validation - Valid multiple tokens with same type from different issuers
#[tokio::test]
async fn test_validation_same_type_different_issuers() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;
    let acme_access = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_3",
        "jti": "acme_token_3",
        "client_id": "acme_client_3",
        "scope": ["write:documents"],
        "aud": "my-client-id",
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let dolphin_access = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_3",
        "jti": "dolphin_token_3",
        "client_id": "dolphin_client_3",
        "scope": ["write:documents"],
        "aud": "dolphin-client",
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let request = AuthorizeMultiIssuerRequest::new_with_fields(
        vec![
            TokenInput::new("Acme::Access_Token".to_string(), acme_access),
            TokenInput::new("Dolphin::Access_Token".to_string(), dolphin_access), // Same type, different issuer - OK
        ],
        EntityData::from_json(
            &json!({
                "cedar_entity_mapping": {
                    "entity_type": "Acme::Resource",
                    "id": "SharedDocs"
                },
                "name": "Shared Documents"
            })
            .to_string(),
        )
        .expect("Failed to create resource entity"),
        "Acme::Action::\"ManageDocuments\"".to_string(),
        None,
    );

    let result = cedarling
        .authorize_multi_issuer(request)
        .await
        .expect("Should accept same token type from different issuers");

    assert!(
        result.decision,
        "Should be ALLOW when either issuer's token has required scope"
    );
}

/// Test validation - Different custom token types from same issuer
#[tokio::test]
async fn test_validation_different_types_same_issuer() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;
    let dolphin_access_token = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_4",
        "jti": "dolphin_access_4",
        "client_id": "dolphin_client_4",
        "scope": ["write:documents"],
        "aud": "dolphin-client",
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let dolphin_custom_token = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_4",
        "jti": "dolphin_custom_4",
        "client_id": "dolphin_client_4",
        "aud": "dolphin-client",
        "waiver": ["signed"],
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let request = AuthorizeMultiIssuerRequest::new_with_fields(
        vec![
            TokenInput::new("Dolphin::Access_Token".to_string(), dolphin_access_token),
            TokenInput::new("Dolphin::Dolphin_Token".to_string(), dolphin_custom_token), // Different type, same issuer - OK
        ],
        EntityData::from_json(
            &json!({
                "cedar_entity_mapping": {
                    "entity_type": "Acme::Resource",
                    "id": "SharedDocs"
                },
                "name": "Shared Documents"
            })
            .to_string(),
        )
        .expect("Failed to create resource entity"),
        "Acme::Action::\"ManageDocuments\"".to_string(),
        None,
    );

    let result = cedarling
        .authorize_multi_issuer(request)
        .await
        .expect("Should accept different token types from same issuer");

    assert!(
        result.decision,
        "Should be ALLOW when Dolphin access token has write:documents scope"
    );
}

/// Test validation - Graceful degradation when invalid token is present
// Graceful degradation: invalid tokens are ignored, valid tokens are processed
#[tokio::test]
async fn test_validation_graceful_degradation_invalid_token() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;
    let valid_token = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_5",
        "jti": "acme_token_5",
        "client_id": "acme_client_5",
        "scope": ["write:documents"],
        "aud": "my-client-id",
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let request = AuthorizeMultiIssuerRequest::new_with_fields(
        vec![
            TokenInput::new("Acme::Access_Token".to_string(), valid_token),
            TokenInput::new("Invalid::Token".to_string(), "not-a-valid-jwt".to_string()), // Invalid JWT
        ],
        EntityData::from_json(
            &json!({
                "cedar_entity_mapping": {
                    "entity_type": "Acme::Resource",
                    "id": "SharedDocs"
                },
                "name": "Shared Documents"
            })
            .to_string(),
        )
        .expect("Failed to create resource entity"),
        "Acme::Action::\"ManageDocuments\"".to_string(),
        None,
    );

    let result = cedarling.authorize_multi_issuer(request).await.expect(
        "Should succeed gracefully when some tokens are invalid (ignore invalid, process valid)",
    );

    assert!(
        result.decision,
        "Should be ALLOW - valid Acme token has required scope despite invalid token being present"
    );
}

/// Test validation - `TokenInput` with empty mapping string should fail
// Graceful degradation: invalid tokens are ignored, valid tokens are processed
#[tokio::test]
async fn test_validation_empty_mapping_string() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;
    let token_for_validation = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_6",
        "jti": "acme_token_6",
        "client_id": "acme_client_6",
        "scope": ["write:documents"],
        "aud": "my-client-id",
        "exp": 2_000_000_000,
        "iat": 1_516_239_022
    }));

    let request_result = AuthorizeMultiIssuerRequest::new_with_fields(
        vec![TokenInput::new(String::new(), token_for_validation)], // Empty mapping
        EntityData::from_json(
            &json!({
                "cedar_entity_mapping": {
                    "entity_type": "Acme::Resource",
                    "id": "SharedDocs"
                },
                "name": "Shared Documents"
            })
            .to_string(),
        )
        .expect("Failed to create resource entity"),
        "Acme::Action::\"ManageDocuments\"".to_string(),
        None,
    );

    // The request should validate and catch empty mapping
    let result = cedarling.authorize_multi_issuer(request_result).await;
    assert!(
        result.is_err(),
        "Should fail when TokenInput has empty mapping string"
    );
}

/// Test validation - `TokenInput` with empty payload should fail
#[tokio::test]
async fn test_validation_empty_payload() {
    let cedarling = get_cedarling_for_multi_issuer_tests().await;
    let request_result = AuthorizeMultiIssuerRequest::new_with_fields(
        vec![TokenInput::new(
            "Acme::Access_Token".to_string(),
            String::new(),
        )], // Empty payload
        EntityData::from_json(
            &json!({
                "cedar_entity_mapping": {
                    "entity_type": "Acme::Resource",
                    "id": "SharedDocs"
                },
                "name": "Shared Documents"
            })
            .to_string(),
        )
        .expect("Failed to create resource entity"),
        "Acme::Action::\"ManageDocuments\"".to_string(),
        None,
    );

    let result = cedarling.authorize_multi_issuer(request_result).await;
    assert!(
        result.is_err(),
        "Should fail when TokenInput has empty payload"
    );
}
