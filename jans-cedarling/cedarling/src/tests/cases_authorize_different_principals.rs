// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! In this module we test authorize different action
//! where not all principals can be applied
//!
//! all case scenario should have `result.decision == true`
//! because we have checked different scenarios in `cases_authorize_without_check_jwt.rs`

use lazy_static::lazy_static;
use test_utils::assert_eq;
use tokio::test;

use super::utils::*;
use crate::{WorkloadBoolOp, authorization_config::IdTokenTrustMode, cmp_decision, cmp_policy}; /* macros is defined in the cedarling\src\tests\utils\cedarling_util.rs */

static POLICY_STORE_RAW_YAML: &str = include_str!("../../../test_files/policy-store_ok_2.yaml");

lazy_static! {
    pub(crate) static ref AuthRequestBase: Request = Request::deserialize(serde_json::json!(
        {
            "tokens": {
                "access_token": generate_token_using_claims(json!({
                    "org_id": "some_long_id",
                    "jti": "some_jti",
                    "client_id": "some_client_id",
                    "iss": "https://account.gluu.org",
                    "aud": "some_aud",
                })),
                "id_token": generate_token_using_claims(json!({
                    "jti": "some_jti",
                    "iss": "https://account.gluu.org",
                    "aud": "some_aud",
                    "sub": "some_sub",
                })),
                "userinfo_token":  generate_token_using_claims(json!({
                    "jti": "some_jti",
                    "country": "US",
                    "sub": "some_sub",
                    "iss": "https://account.gluu.org",
                    "role": ["Admin"],
                })),
            },
            // we need specify action name in each test case
            "action": "",
            "resource": {
                "id": "random_id",
                "type": "Jans::Issue",
                "org_id": "some_long_id",
                "country": "US"
            },
            "context": {},
        }
    ))
    .expect("Request should be deserialized from json");
}

/// Check if action executes for next principals: Workload, User
#[test]
async fn success_test_for_all_principals() {
    let cedarling = get_cedarling(PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string())).await;

    let mut request = AuthRequestBase.clone();
    request.action = "Jans::Action::\"Update\"".to_string();

    let result = cedarling
        .authorize(request)
        .await
        .expect("request should be parsed without errors");

    cmp_decision!(
        result.workload,
        Decision::Allow,
        "request result should be allowed for workload"
    );
    cmp_policy!(
        result.workload,
        vec!["1"],
        "reason of permit workload should be '1'"
    );

    cmp_decision!(
        result.person,
        Decision::Allow,
        "request result should be allowed for person"
    );

    cmp_policy!(
        result.person,
        vec!["2", "3"],
        "reason of permit person should be '2'"
    );

    assert!(result.decision, "request result should be allowed");
}

/// Check if action executes for next principals: Workload
#[test]
async fn success_test_for_principal_workload() {
    let cedarling = get_cedarling_with_authorization_conf(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        crate::AuthorizationConfig {
            use_user_principal: false,
            use_workload_principal: true,
            user_workload_operator: Default::default(),
            id_token_trust_mode: IdTokenTrustMode::None,
            ..Default::default()
        },
        crate::EntityBuilderConfig::default().build_workload(),
    )
    .await;

    let mut request = AuthRequestBase.clone();
    request.action = "Jans::Action::\"UpdateForWorkload\"".to_string();

    let result = cedarling
        .authorize(request)
        .await
        .expect("request should be parsed without errors");

    cmp_decision!(
        result.workload,
        Decision::Allow,
        "request result should be allowed for workload"
    );
    cmp_policy!(
        result.workload,
        vec!["1"],
        "reason of permit workload should be '1'"
    );

    assert!(result.person.is_none(), "result for person should be none");

    assert!(result.decision, "request result should be allowed");
}

/// Check if action executes for next principals: User
#[test]
async fn success_test_for_principal_user() {
    let cedarling = get_cedarling_with_authorization_conf(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        crate::AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: false,
            user_workload_operator: Default::default(),
            id_token_trust_mode: IdTokenTrustMode::None,
            ..Default::default()
        },
        crate::EntityBuilderConfig::default().build_user(),
    )
    .await;

    let mut request = AuthRequestBase.clone();
    request.action = "Jans::Action::\"UpdateForUser\"".to_string();

    let result = cedarling
        .authorize(request)
        .await
        .expect("request should be parsed without errors");

    cmp_decision!(
        result.person,
        Decision::Allow,
        "request result should be allowed for person"
    );
    cmp_policy!(
        result.person,
        vec!["2"],
        "reason of permit person should be '2'"
    );

    assert!(
        result.workload.is_none(),
        "result for workload should be none"
    );

    assert!(result.decision, "request result should be allowed");
}

/// Check if action executes for next principals: Person (only)
/// check for user and role
#[test]
async fn success_test_for_principal_person_role() {
    let cedarling = get_cedarling_with_authorization_conf(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        crate::AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: false,
            user_workload_operator: Default::default(),
            id_token_trust_mode: IdTokenTrustMode::None,
            ..Default::default()
        },
        crate::EntityBuilderConfig::default().build_user(),
    )
    .await;

    let mut request = AuthRequestBase.clone();
    request.action = "Jans::Action::\"UpdateForUserAndRole\"".to_string();

    let result = cedarling
        .authorize(request)
        .await
        .expect("request should be parsed without errors");

    cmp_policy!(
        result.person,
        vec!["2", "3"],
        "reason of permit person should be '2'"
    );

    cmp_decision!(
        result.person,
        Decision::Allow,
        "request result should be allowed for person"
    );

    assert!(
        result.workload.is_none(),
        "result for workload should be none"
    );

    assert!(result.decision, "request result should be allowed");
}

/// Check if action executes for next principals: Workload AND Person (Role)
#[test]
async fn success_test_for_principal_workload_role() {
    let cedarling = get_cedarling_with_authorization_conf(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        crate::AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: true,
            user_workload_operator: WorkloadBoolOp::And,
            id_token_trust_mode: IdTokenTrustMode::None,
            ..Default::default()
        },
        crate::EntityBuilderConfig::default()
            .build_workload()
            .build_user(),
    )
    .await;

    let mut request = AuthRequestBase.clone();
    request.action = "Jans::Action::\"UpdateForWorkloadAndRole\"".to_string();

    let result = cedarling
        .authorize(request)
        .await
        .expect("request should be parsed without errors");

    cmp_decision!(
        result.workload,
        Decision::Allow,
        "request result should be allowed for workload"
    );
    cmp_policy!(
        result.workload,
        vec!["1"],
        "reason of permit workload should be '1'"
    );

    cmp_decision!(
        result.person,
        Decision::Allow,
        "request result should be allowed for person"
    );
    cmp_policy!(
        result.person,
        vec!["3"],
        "reason of permit person should be '3'"
    );

    assert!(result.decision, "request result should be allowed");
}

/// Check if action executes for next principals: Workload (true) OR Person (false)
/// is used operator OR
#[test]
async fn success_test_for_principal_workload_true_or_user_false() {
    let cedarling = get_cedarling_with_authorization_conf(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        crate::AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: true,
            user_workload_operator: WorkloadBoolOp::Or,
            id_token_trust_mode: IdTokenTrustMode::None,
            ..Default::default()
        },
        crate::EntityBuilderConfig::default()
            .build_user()
            .build_workload(),
    )
    .await;

    let mut request = AuthRequestBase.clone();
    request.action = "Jans::Action::\"UpdateForWorkload\"".to_string();

    let result = cedarling
        .authorize(request)
        .await
        .expect("request should be parsed without errors");

    cmp_decision!(
        result.workload,
        Decision::Allow,
        "request result should be allowed for workload"
    );
    cmp_policy!(
        result.workload,
        vec!["1"],
        "reason of permit workload should be '1'"
    );

    cmp_decision!(
        result.person,
        Decision::Deny,
        "request result should be allowed for person"
    );
    cmp_policy!(
        result.person,
        Vec::new() as Vec<String>,
        "reason of permit person should be empty"
    );

    assert!(result.decision, "request result should be allowed");
}

/// Check if action executes for next principals: Workload (false) OR Person (true)
/// is used operator OR
#[test]
async fn success_test_for_principal_workload_false_or_user_true() {
    let cedarling = get_cedarling_with_authorization_conf(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        crate::AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: true,
            user_workload_operator: WorkloadBoolOp::Or,
            id_token_trust_mode: IdTokenTrustMode::None,
            ..Default::default()
        },
        crate::EntityBuilderConfig::default()
            .build_user()
            .build_workload(),
    )
    .await;

    let mut request = AuthRequestBase.clone();
    request.action = "Jans::Action::\"UpdateForUser\"".to_string();

    let result = cedarling
        .authorize(request)
        .await
        .expect("request should be parsed without errors");

    cmp_decision!(
        result.workload,
        Decision::Deny,
        "request result should be not allowed for workload"
    );
    cmp_policy!(
        result.workload,
        Vec::new() as Vec<String>,
        "reason of permit workload should be empty"
    );

    cmp_decision!(
        result.person,
        Decision::Allow,
        "request result should be allowed for person"
    );
    cmp_policy!(
        result.person,
        vec!["2"],
        "reason of permit person should be '2'"
    );

    assert!(result.decision, "request result should be allowed");
}

/// Check if action executes for next principals: Workload (false) OR Person (false)
/// is used operator OR
#[test]
async fn success_test_for_principal_workload_false_or_user_false() {
    let cedarling = get_cedarling_with_authorization_conf(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        crate::AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: true,
            user_workload_operator: WorkloadBoolOp::Or,
            id_token_trust_mode: IdTokenTrustMode::None,
            ..Default::default()
        },
        crate::EntityBuilderConfig::default()
            .build_user()
            .build_workload(),
    )
    .await;

    let mut request = AuthRequestBase.clone();
    request.action = "Jans::Action::\"AlwaysDeny\"".to_string();

    let result = cedarling
        .authorize(request)
        .await
        .expect("request should be parsed without errors");

    cmp_decision!(
        result.workload,
        Decision::Deny,
        "request result should be not allowed for workload"
    );
    cmp_policy!(
        result.workload,
        Vec::new() as Vec<String>,
        "reason of permit workload should be empty"
    );

    cmp_decision!(
        result.person,
        Decision::Deny,
        "request result should be not allowed for person"
    );
    cmp_policy!(
        result.person,
        Vec::new() as Vec<String>,
        "reason of permit person should be empty"
    );

    assert!(!result.decision, "request result should be not allowed");
}

/// Check if action executes when principal workload can't be applied
#[test]
async fn test_where_principal_workload_cant_be_applied() {
    let cedarling = get_cedarling_with_authorization_conf(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        crate::AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: true,
            user_workload_operator: Default::default(),
            id_token_trust_mode: IdTokenTrustMode::None,
            ..Default::default()
        },
        crate::EntityBuilderConfig::default()
            .build_user()
            .build_workload(),
    )
    .await;

    let mut request = AuthRequestBase.clone();
    request.action = "Jans::Action::\"NoApplies\"".to_string();

    let result = cedarling
        .authorize(request)
        .await
        .expect_err("request should be parsed with error");

    assert!(matches!(
        result,
        crate::AuthorizeError::WorkloadRequestValidation(_)
    ))
}

/// Check if action executes when principal user can't be applied
#[test]
async fn test_where_principal_user_cant_be_applied() {
    let cedarling = get_cedarling_with_authorization_conf(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        crate::AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: false,
            user_workload_operator: Default::default(),
            id_token_trust_mode: IdTokenTrustMode::None,
            ..Default::default()
        },
        crate::EntityBuilderConfig::default().build_user(),
    )
    .await;

    let mut request = AuthRequestBase.clone();
    request.action = "Jans::Action::\"NoApplies\"".to_string();

    let result = cedarling
        .authorize(request)
        .await
        .expect_err("request should be parsed with error");

    assert!(
        matches!(result, crate::AuthorizeError::UserRequestValidation(_)),
        "expected error UserRequestValidation, got: {}",
        result
    )
}
