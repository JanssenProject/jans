// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::authz::request::{AuthorizeMultiIssuerRequest, EntityData, TokenInput};
use super::utils::*;
use super::utils::cedarling_util::get_cedarling_with_callback;
use serde_json::json;

/// Test successful multi-issuer authorization scenarios
/// Uses the policy-store-multi-issuer-basic.yaml configuration
/// Tests both single and multiple token scenarios
#[tokio::test]
async fn test_multi_issuer_authorization_success() {
    // Load the policy store configuration
    static POLICY_STORE_RAW_YAML: &str = include_str!("../../../test_files/policy-store-multi-issuer-basic.yaml");
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        |config| {
            // Disable workload and user entity building for multi-issuer tests
            // since the policies work with token entities in context, not principal entities
            config.entity_builder_config.build_workload = true;
            config.entity_builder_config.build_user = false;
            config.authorization_config.use_workload_principal = true;
            config.authorization_config.use_user_principal = false;
        }
    ).await;

    // Test Case 1: Single Dolphin access token with location claim
    let dolphin_access_token = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_123",
        "jti": "dolphin123",
        "client_id": "dolphin_client_123",
        "aud": "dolphin_audience",
        "location": ["miami", "orlando"],
        // "scope": ["read", "write"],
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
            TokenInput::new(
                "Dolphin::Access_Token".to_string(),
                dolphin_access_token,
            ),
            TokenInput::new(
                "Dolphin::Dolphin_Token".to_string(),
                dolphin_user_token,
            ),
        ],
        EntityData::from_json(&json!({
            "cedar_entity_mapping": {
                "entity_type": "Acme::Resources",
                "id": "ApprovedDolphinFoods"
            },
            "name": "Approved Dolphin Foods"
        }).to_string()).expect("Failed to create resource entity"),
        "Acme::Action::\"GetFood\"".to_string(),
        None,
    );

    let result = cedarling.authorize_multi_issuer(request).await;
    if let Err(e) = &result {
        println!("Error in dolphin access token authorization: {:?}", e);
    }
    assert!(result.is_ok(), "Dolphin access token authorization should succeed");
    
    let authz_result = result.unwrap();
    println!("Authorization result: {:?}", authz_result);
    assert!(authz_result.decision, "Authorization should be ALLOW for dolphin access token");

    // Test Case 2: Single Acme access token with scope claim
    let acme_access_token = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_456",
        "jti": "acme123",
        "client_id": "acme_client_456",
        // "scope": ["read:wiki", "write:profile"],
        "aud": "my-client-id",
        "exp": 2000000000,
        "iat": 1516239022
    }));

    let request = AuthorizeMultiIssuerRequest::new_with_fields(
        vec![TokenInput::new(
            "access_token".to_string(),
            acme_access_token,
        )],
        EntityData::from_json(&json!({
            "cedar_entity_mapping": {
                "entity_type": "Acme::Resource",
                "id": "WikiPages"
            },
            "name": "Wiki Pages"
        }).to_string()).expect("Failed to create resource entity"),
        "Acme::Action::\"ReadProfile\"".to_string(),
        None,
    );

    let result = cedarling.authorize_multi_issuer(request).await;
    assert!(result.is_ok(), "Acme access token authorization should succeed");
    
    let authz_result = result.unwrap();
    assert!(authz_result.decision, "Authorization should be ALLOW for acme access token");

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
        EntityData::from_json(&json!({
            "cedar_entity_mapping": {
                "entity_type": "Acme::Resource",
                "id": "MiamiAcquarium"
            },
            "name": "Miami Aquarium"
        }).to_string()).expect("Failed to create resource entity"),
        "Acme::Action::\"SwimWithOrca\"".to_string(),
        None,
    );

    let result = cedarling.authorize_multi_issuer(request).await;
    assert!(result.is_ok(), "Dolphin custom token authorization should succeed");
    
    let authz_result = result.unwrap();
    assert!(authz_result.decision, "Authorization should be ALLOW for dolphin custom token");

    // Test Case 4: Multiple tokens from different issuers
    let acme_multi_token = generate_token_using_claims(json!({
        "iss": "https://idp.acme.com",
        "sub": "acme_user_multi",
        "jti": "acme_multi_123",
        "client_id": "acme_multi_client_123",
        "aud": "acme_multi_audience",
        // "scope": ["read:wiki", "write:profile"],
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
            TokenInput::new(
                "access_token".to_string(),
                acme_multi_token,
            ),
            TokenInput::new(
                "access_token".to_string(),
                dolphin_multi_token,
            ),
        ],
        EntityData::from_json(&json!({
            "cedar_entity_mapping": {
                "entity_type": "Acme::Resource",
                "id": "WikiPages"
            },
            "name": "Wiki Pages"
        }).to_string()).expect("Failed to create resource entity"),
        "Acme::Action::\"ReadProfile\"".to_string(),
        None,
    );

    let result = cedarling.authorize_multi_issuer(request).await;
    assert!(result.is_ok(), "Multi-token authorization should succeed");
    
    let authz_result = result.unwrap();
    assert!(authz_result.decision, "Authorization should be ALLOW for multi-token request");
}

