// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::utils::cedarling_util::get_cedarling_with_callback;
use super::utils::*;
use crate::authz::request::{AuthorizeMultiIssuerRequest, EntityData, TokenInput};
use serde_json::json;

/// Test successful multi-issuer authorization scenarios
/// Uses the policy-store-multi-issuer-basic.yaml configuration
/// Tests both single and multiple token scenarios
#[tokio::test]
async fn test_multi_issuer_authorization_success() {
    // Load the policy store configuration
    static POLICY_STORE_RAW_YAML: &str =
        include_str!("../../../test_files/policy-store-multi-issuer-basic.yaml");
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        |config| {
            // Disable workload and user entity building for multi-issuer tests
            // since the policies work with token entities in context, not principal entities
            config.entity_builder_config.build_workload = false;
            config.entity_builder_config.build_user = false;
            config.authorization_config.use_workload_principal = false;
            config.authorization_config.use_user_principal = false;
        },
    )
    .await;

    // Test Case 1: Single Dolphin access token with location claim
    let dolphin_access_token = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_123",
        "jti": "dolphin123",
        "client_id": "dolphin_client_123",
        "aud": "dolphin_audience",
        "location": ["miami", "orlando"],
        "scope": ["read", "write"],
        "exp": 2000000000,
        "iat": 1516239022
    }));

    // Create a dolphin_token for the user entity
    let dolphin_user_token = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_123",
        "jti": "dolphin_user_123",
        "client_id": "dolphin_client_123",
        "aud": "dolphin_audience",
        "exp": 2000000000,
        "iat": 1516239022
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

    let result = cedarling.authorize_multi_issuer(request).await;
    assert!(
        result.is_ok(),
        "Dolphin access token authorization should succeed"
    );

    let authz_result = result.unwrap();
    assert!(
        authz_result.decision,
        "Authorization should be ALLOW for dolphin access token"
    );

    // Test Case 2: Single Acme access token with scope claim
    let acme_access_token = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_456",
        "jti": "acme123",
        "client_id": "acme_client_456",
        "scope": ["read:wiki", "write:profile"],
        "aud": "my-client-id",
        "exp": 2000000000,
        "iat": 1516239022
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

    let result = cedarling.authorize_multi_issuer(request).await;
    assert!(
        result.is_ok(),
        "Acme access token authorization should succeed"
    );

    let authz_result = result.unwrap();
    assert!(
        authz_result.decision,
        "Authorization should be ALLOW for acme access token"
    );

    // Test Case 3: Single Dolphin custom token with waiver claim
    let dolphin_custom_token = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_789",
        "jti": "dolphin_custom_789",
        "client_id": "dolphin_custom_client_789",
        "aud": "dolphin_custom_audience",
        "waiver": ["signed", "approved"],
        "exp": 2000000000,
        "iat": 1516239022
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

    let result = cedarling.authorize_multi_issuer(request).await;
    assert!(
        result.is_ok(),
        "Dolphin custom token authorization should succeed"
    );

    let authz_result = result.unwrap();
    assert!(
        authz_result.decision,
        "Authorization should be ALLOW for dolphin custom token"
    );

    // Test Case 4: Multiple tokens from different issuers
    let acme_multi_token = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_multi",
        "jti": "acme_multi_123",
        "client_id": "acme_multi_client_123",
        "aud": "acme_multi_audience",
        "scope": ["read:wiki", "write:profile"],
        "exp": 2000000000,
        "iat": 1516239022
    }));

    let dolphin_multi_token = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_multi",
        "jti": "dolphin_multi_456",
        "client_id": "dolphin_multi_client_456",
        "aud": "dolphin_multi_audience",
        "location": ["miami"],
        "exp": 2000000000,
        "iat": 1516239022
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

    let result = cedarling.authorize_multi_issuer(request).await;
    assert!(result.is_ok(), "Multi-token authorization should succeed");

    let authz_result = result.unwrap();
    assert!(
        authz_result.decision,
        "Authorization should be ALLOW for multi-token request"
    );
}

/// Test multi-token scope requirements scenarios
/// Tests both OR logic (any token has scope) and AND logic (multiple tokens required)
#[tokio::test]
async fn test_multi_token_scope_requirements() {
    // Load the policy store configuration
    static POLICY_STORE_RAW_YAML: &str =
        include_str!("../../../test_files/policy-store-multi-issuer-basic.yaml");
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        |config| {
            // Disable workload and user entity building for multi-issuer tests
            config.entity_builder_config.build_workload = false;
            config.entity_builder_config.build_user = false;
            config.authorization_config.use_workload_principal = false;
            config.authorization_config.use_user_principal = false;
        },
    )
    .await;

    // Test Case 1: OR Logic - Only Acme token has the required scope
    let acme_token_with_scope = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_1",
        "jti": "acme_token_1",
        "client_id": "acme_client_1",
        "scope": ["write:documents", "read:profile"],
        "aud": "my-client-id",
        "exp": 2000000000,
        "iat": 1516239022
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

    let result = cedarling.authorize_multi_issuer(request).await;
    assert!(
        result.is_ok(),
        "Authorization should succeed with Acme token having write:documents scope"
    );
    assert!(
        result.unwrap().decision,
        "Should be ALLOW when Acme token has required scope"
    );

    // Test Case 2: OR Logic - Only Dolphin token has the required scope
    let dolphin_token_with_scope = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_2",
        "jti": "dolphin_token_2",
        "client_id": "dolphin_client_2",
        "scope": ["write:documents"],
        "aud": "dolphin-client",
        "exp": 2000000000,
        "iat": 1516239022
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

    let result = cedarling.authorize_multi_issuer(request).await;
    assert!(
        result.is_ok(),
        "Authorization should succeed with Dolphin token having write:documents scope"
    );
    assert!(
        result.unwrap().decision,
        "Should be ALLOW when Dolphin token has required scope"
    );

    // Test Case 3: OR Logic - Both tokens have the scope
    let acme_token = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_3",
        "jti": "acme_token_3",
        "client_id": "acme_client_3",
        "scope": ["write:documents"],
        "aud": "my-client-id",
        "exp": 2000000000,
        "iat": 1516239022
    }));

    let dolphin_token = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_3",
        "jti": "dolphin_token_3",
        "client_id": "dolphin_client_3",
        "scope": ["write:documents"],
        "aud": "dolphin-client",
        "exp": 2000000000,
        "iat": 1516239022
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

    let result = cedarling.authorize_multi_issuer(request).await;
    assert!(
        result.is_ok(),
        "Authorization should succeed when both tokens have the scope"
    );
    assert!(
        result.unwrap().decision,
        "Should be ALLOW when both tokens have required scope"
    );

    // Test Case 4: OR Logic - Neither token has the scope (should deny)
    let acme_token_no_scope = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_4",
        "jti": "acme_token_4",
        "client_id": "acme_client_4",
        "scope": ["read:profile"],  // Wrong scope
        "aud": "my-client-id",
        "exp": 2000000000,
        "iat": 1516239022
    }));

    let dolphin_token_no_scope = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_4",
        "jti": "dolphin_token_4",
        "client_id": "dolphin_client_4",
        "scope": ["read:data"],  // Wrong scope
        "aud": "dolphin-client",
        "exp": 2000000000,
        "iat": 1516239022
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

    let result = cedarling.authorize_multi_issuer(request).await;
    assert!(
        result.is_ok(),
        "Authorization should execute successfully"
    );
    assert!(
        !result.unwrap().decision,
        "Should be DENY when neither token has required scope"
    );

    // Test Case 5: AND Logic - Both tokens present with required attributes
    let acme_vote_token = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_5",
        "jti": "acme_token_5",
        "client_id": "acme_client_5",
        "scope": ["trade:vote"],  // Required scope
        "aud": "my-client-id",
        "exp": 2000000000,
        "iat": 1516239022
    }));

    let dolphin_member_token = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_5",
        "jti": "dolphin_token_5",
        "client_id": "dolphin_client_5",
        "member_status": ["verified"],  // Required member status
        "aud": "dolphin-client",
        "exp": 2000000000,
        "iat": 1516239022
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

    let result = cedarling.authorize_multi_issuer(request).await;
    assert!(
        result.is_ok(),
        "Authorization should succeed when both tokens have required attributes"
    );
    assert!(
        result.unwrap().decision,
        "Should be ALLOW when both required tokens are present with correct attributes"
    );

    // Test Case 6: AND Logic - Only one token present (should deny)
    let acme_vote_token_only = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_6",
        "jti": "acme_token_6",
        "client_id": "acme_client_6",
        "scope": ["trade:vote"],
        "aud": "my-client-id",
        "exp": 2000000000,
        "iat": 1516239022
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

    let result = cedarling.authorize_multi_issuer(request).await;
    assert!(
        result.is_ok(),
        "Authorization should execute successfully"
    );
    assert!(
        !result.unwrap().decision,
        "Should be DENY when only one of two required tokens is present"
    );

    // Test Case 7: AND Logic - Both tokens present but missing required attributes (should deny)
    let acme_wrong_scope = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_7",
        "jti": "acme_token_7",
        "client_id": "acme_client_7",
        "scope": ["read:profile"],  // Wrong scope
        "aud": "my-client-id",
        "exp": 2000000000,
        "iat": 1516239022
    }));

    let dolphin_wrong_status = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_7",
        "jti": "dolphin_token_7",
        "client_id": "dolphin_client_7",
        "member_status": ["pending"],  // Wrong status
        "aud": "dolphin-client",
        "exp": 2000000000,
        "iat": 1516239022
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

    let result = cedarling.authorize_multi_issuer(request).await;
    assert!(
        result.is_ok(),
        "Authorization should execute successfully"
    );
    assert!(
        !result.unwrap().decision,
        "Should be DENY when tokens don't have required attributes"
    );
}

/// Test custom token type handling
/// Verifies that the system can handle arbitrary custom token types dynamically
#[tokio::test]
async fn test_custom_token_type_handling() {
    // Load the policy store configuration
    static POLICY_STORE_RAW_YAML: &str =
        include_str!("../../../test_files/policy-store-multi-issuer-basic.yaml");
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        |config| {
            // Disable workload and user entity building for multi-issuer tests
            config.entity_builder_config.build_workload = false;
            config.entity_builder_config.build_user = false;
            config.authorization_config.use_workload_principal = false;
            config.authorization_config.use_user_principal = false;
        },
    )
    .await;

    // Test Case 1: Custom DolphinToken with waiver claim
    let dolphin_custom_token = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_1",
        "jti": "dolphin_custom_1",
        "client_id": "dolphin_client_1",
        "aud": "dolphin-audience",
        "waiver": ["signed", "approved", "notarized"],
        "exp": 2000000000,
        "iat": 1516239022
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

    let result = cedarling.authorize_multi_issuer(request).await;
    assert!(
        result.is_ok(),
        "Authorization should succeed with custom DolphinToken"
    );
    assert!(
        result.unwrap().decision,
        "Should be ALLOW when custom token has required waiver claim"
    );

    // Test Case 2: Custom token without required claim (should deny)
    let dolphin_token_no_waiver = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_2",
        "jti": "dolphin_custom_2",
        "client_id": "dolphin_client_2",
        "aud": "dolphin-audience",
        "waiver": ["unsigned"],  // Wrong waiver status
        "exp": 2000000000,
        "iat": 1516239022
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

    let result = cedarling.authorize_multi_issuer(request).await;
    assert!(
        result.is_ok(),
        "Authorization should execute successfully"
    );
    assert!(
        !result.unwrap().decision,
        "Should be DENY when custom token doesn't have correct waiver"
    );

    // Test Case 3: Multiple custom token types together
    let acme_access_token = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_3",
        "jti": "acme_token_3",
        "client_id": "acme_client_3",
        "scope": ["read:wiki"],
        "aud": "my-client-id",
        "exp": 2000000000,
        "iat": 1516239022
    }));

    let dolphin_custom = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_3",
        "jti": "dolphin_custom_3",
        "client_id": "dolphin_client_3",
        "aud": "dolphin-audience",
        "waiver": ["signed"],
        "exp": 2000000000,
        "iat": 1516239022
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

    let result = cedarling.authorize_multi_issuer(request).await;
    assert!(
        result.is_ok(),
        "Authorization should succeed with multiple custom and standard tokens"
    );
    assert!(
        result.unwrap().decision,
        "Should be ALLOW when custom DolphinToken has required waiver"
    );

    // Test Case 4: Custom token type with complex nested claims
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
        "exp": 2000000000,
        "iat": 1516239022
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

    let result = cedarling.authorize_multi_issuer(request).await;
    assert!(
        result.is_ok(),
        "Authorization should succeed with complex custom token containing multiple claims"
    );
    assert!(
        result.unwrap().decision,
        "Should be ALLOW - policy only checks waiver claim, other claims are preserved as tags"
    );

    // Test Case 5: Mix of standard and custom tokens from multiple issuers
    // Demonstrates that custom token types work alongside standard token types
    let acme_standard = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_5",
        "jti": "acme_token_5",
        "client_id": "acme_client_5",
        "scope": ["write:documents"],
        "aud": "my-client-id",
        "exp": 2000000000,
        "iat": 1516239022
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
        "exp": 2000000000,
        "iat": 1516239022
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

    let result = cedarling.authorize_multi_issuer(request).await;
    assert!(
        result.is_ok(),
        "Authorization should succeed with mix of standard and custom token types"
    );
    assert!(
        result.unwrap().decision,
        "Should be ALLOW - Acme token has write:documents scope"
    );
}

/// Test multi-issuer validation scenarios
/// Tests non-deterministic token detection, graceful degradation, and validation requirements
#[tokio::test]
async fn test_multi_issuer_validation() {
    // Load the policy store configuration
    static POLICY_STORE_RAW_YAML: &str =
        include_str!("../../../test_files/policy-store-multi-issuer-basic.yaml");
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        |config| {
            config.entity_builder_config.build_workload = false;
            config.entity_builder_config.build_user = false;
            config.authorization_config.use_workload_principal = false;
            config.authorization_config.use_user_principal = false;
        },
    )
    .await;

    // Test Case 1: Empty token array should fail
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
    assert!(
        result.is_err(),
        "Should fail with empty token array"
    );

    // Test Case 2: Non-deterministic tokens - multiple tokens of same type from same issuer
    // The system uses graceful validation: first token is kept, duplicates are logged and skipped
    let acme_token_1 = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_1",
        "jti": "acme_token_1",
        "client_id": "acme_client_1",
        "scope": ["write:documents"],
        "aud": "my-client-id",
        "exp": 2000000000,
        "iat": 1516239022
    }));

    let acme_token_2 = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_2",
        "jti": "acme_token_2",
        "client_id": "acme_client_2",
        "scope": ["read:profile"],
        "aud": "my-client-id",
        "exp": 2000000000,
        "iat": 1516239022
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

    let result = cedarling.authorize_multi_issuer(request).await;
    // Graceful validation: first token is kept, duplicate is skipped with warning
    assert!(
        result.is_ok(),
        "Should handle non-deterministic tokens gracefully (keep first, skip duplicates)"
    );
    assert!(
        result.unwrap().decision,
        "Should be ALLOW - first Acme token has write:documents scope"
    );

    // Test Case 3: Valid multiple tokens - same type from different issuers (should work)
    let acme_access = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_3",
        "jti": "acme_token_3",
        "client_id": "acme_client_3",
        "scope": ["write:documents"],
        "aud": "my-client-id",
        "exp": 2000000000,
        "iat": 1516239022
    }));

    let dolphin_access = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_3",
        "jti": "dolphin_token_3",
        "client_id": "dolphin_client_3",
        "scope": ["write:documents"],
        "aud": "dolphin-client",
        "exp": 2000000000,
        "iat": 1516239022
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

    let result = cedarling.authorize_multi_issuer(request).await;
    assert!(
        result.is_ok(),
        "Should accept same token type from different issuers"
    );
    assert!(
        result.unwrap().decision,
        "Should be ALLOW when either issuer's token has required scope"
    );

    // Test Case 4: Different custom token types from same issuer (should work)
    let dolphin_access_token = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_4",
        "jti": "dolphin_access_4",
        "client_id": "dolphin_client_4",
        "scope": ["write:documents"],
        "aud": "dolphin-client",
        "exp": 2000000000,
        "iat": 1516239022
    }));

    let dolphin_custom_token = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_4",
        "jti": "dolphin_custom_4",
        "client_id": "dolphin_client_4",
        "aud": "dolphin-client",
        "waiver": ["signed"],
        "exp": 2000000000,
        "iat": 1516239022
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

    let result = cedarling.authorize_multi_issuer(request).await;
    assert!(
        result.is_ok(),
        "Should accept different token types from same issuer"
    );
    assert!(
        result.unwrap().decision,
        "Should be ALLOW when Dolphin access token has write:documents scope"
    );

    // Test Case 5: Graceful degradation - invalid token should be ignored
    let valid_token = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_5",
        "jti": "acme_token_5",
        "client_id": "acme_client_5",
        "scope": ["write:documents"],
        "aud": "my-client-id",
        "exp": 2000000000,
        "iat": 1516239022
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

    let result = cedarling.authorize_multi_issuer(request).await;
    // Graceful degradation: invalid tokens are ignored, valid tokens are processed
    assert!(
        result.is_ok(),
        "Should succeed gracefully when some tokens are invalid (ignore invalid, process valid)"
    );
    assert!(
        result.unwrap().decision,
        "Should be ALLOW - valid Acme token has required scope despite invalid token being present"
    );

    // Test Case 6: TokenInput validation - empty mapping string
    let token_for_validation = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_6",
        "jti": "acme_token_6",
        "client_id": "acme_client_6",
        "scope": ["write:documents"],
        "aud": "my-client-id",
        "exp": 2000000000,
        "iat": 1516239022
    }));

    let request_result = AuthorizeMultiIssuerRequest::new_with_fields(
        vec![TokenInput::new("".to_string(), token_for_validation)], // Empty mapping
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

    // Test Case 7: TokenInput validation - empty payload
    let request_result = AuthorizeMultiIssuerRequest::new_with_fields(
        vec![TokenInput::new("Acme::Access_Token".to_string(), "".to_string())], // Empty payload
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
