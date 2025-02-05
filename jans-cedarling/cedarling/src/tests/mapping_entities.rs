// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! In this test cases we check mapping entities using Bootstrap properties:
//! CEDARLING_MAPPING_USER
//! CEDARLING_MAPPING_WORKLOAD
//! CEDARLING_TOKEN_CONFIGS

use super::utils::*;
use crate::authz::entity_builder::{
    BuildCedarlingEntityError, BuildEntityError, BuildTokenEntityError,
};
use crate::{AuthorizeError, Cedarling, cmp_decision, cmp_policy};
use cedarling_util::get_raw_config;
use std::collections::HashSet;
use std::sync::LazyLock;
use test_utils::assert_eq;
use tokio::test;

static POLICY_STORE_RAW_YAML: &str =
    include_str!("../../../test_files/policy-store_entity_mapping.yaml");

/// static [`Request`] value that will be used in tests
static REQUEST: LazyLock<Request> = LazyLock::new(|| {
    // deserialize `Request` from json
    Request::deserialize(serde_json::json!(
        {
            "tokens": {
                "access_token": generate_token_using_claims(json!({
                    "org_id": "some_long_id",
                    "jti": "some_jti",
                    "client_id": "some_client_id",
                    "iss": "some_iss",
                    "aud": "some_aud",
                })),
                "id_token": generate_token_using_claims(json!({
                    "jti": "some_jti",
                    "iss": "some_iss",
                    "aud": "some_aud",
                    "sub": "some_sub",
                })),
                "userinfo_token":  generate_token_using_claims(json!({
                    "jti": "some_jti",
                    "country": "US",
                    "sub": "some_sub",
                    "iss": "some_iss",
                    "client_id": "some_client_id",
                    "role": "Admin",
                })),
            },
            "action": "Jans::Action::\"Update\"",
            "resource": {
                "id": "random_id",
                "type": "Jans::Issue",
                "org_id": "some_long_id",
                "country": "US"
            },
            "context": {},
        }
    ))
    .expect("Request should be deserialized from json")
});

/// we not specify any mapping to check if it works correctly with default mapping
#[test]
async fn test_default_mapping() {
    let raw_config = get_raw_config(POLICY_STORE_RAW_YAML);
    let config = crate::BootstrapConfig::from_raw_config(&raw_config)
        .expect("raw config should parse without errors");
    let cedarling = Cedarling::new(&config)
        .await
        .expect("could be created without error");

    let request = REQUEST.clone();

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
        "reason of permit person should be '2','3'"
    );

    assert!(result.decision, "request result should be allowed");
}

/// Validate mapping entities.
/// This function validates the mapping of users and workloads using the defined `cedar` schema.
/// For other entities, currently, it is not possible to automatically validate the mapping.
///
/// Note: Verified that the mapped entity types are present in the logs.
#[test]
async fn test_custom_mapping() {
    let mut raw_config = get_raw_config(POLICY_STORE_RAW_YAML);
    raw_config.mapping_user = Some("Jans::MappedUser".to_string());
    raw_config.mapping_workload = Some("Jans::MappedWorkload".to_string());
    raw_config.token_configs = serde_json::from_value(json!({
        "access_token": {
            "entity_type_name": "Jans::MappedAccess_token",
        },
        "id_token": {
            "entity_type_name": "Jans::MappedIdToken",
        },
        "userinfo_token": {
            "entity_type_name": "Jans::MappedUserinfo_token",
        },
    }))
    .expect("valid token configs");

    let config = crate::BootstrapConfig::from_raw_config(&raw_config)
        .expect("raw config should parse without errors");
    let cedarling = Cedarling::new(&config)
        .await
        .expect("could be created without error");

    let mut request = REQUEST.clone();
    request.action = "Jans::Action::\"UpdateMappedWorkloadAndUser\"".to_string();

    let result = cedarling
        .authorize(request)
        .await
        .expect("request should be parsed without errors");

    cmp_policy!(
        result.workload,
        vec!["6",],
        "reason of permit workload should be '6'"
    );

    cmp_decision!(
        result.workload,
        Decision::Allow,
        "request result should be allowed for workload"
    );

    cmp_policy!(
        result.person,
        vec!["5"],
        "reason of permit person should be '5'"
    );

    cmp_decision!(
        result.person,
        Decision::Allow,
        "request result should be allowed for person"
    );

    assert!(result.decision, "request result should be allowed");
}

/// Check if we get error on mapping user to undefined entity
#[test]
async fn test_failed_user_mapping() {
    let mut raw_config = get_raw_config(POLICY_STORE_RAW_YAML);

    let entity_type = "MappedUserNotExist".to_string();
    raw_config.mapping_user = Some(entity_type.to_string());

    let config = crate::BootstrapConfig::from_raw_config(&raw_config)
        .expect("raw config should parse without errors");

    let cedarling = Cedarling::new(&config)
        .await
        .expect("could be created without error");

    let request = REQUEST.clone();

    let err = cedarling
        .authorize(request)
        .await
        .expect_err("request should be parsed with mapping error");

    match err {
        AuthorizeError::BuildEntity(BuildCedarlingEntityError::User(error)) => {
            assert_eq!(error.errors.len(), 2, "there should be 2 errors");

            let (token_name, err) = &error.errors[0];
            assert_eq!(token_name, "userinfo_token");
            assert!(
                matches!(err, BuildEntityError::EntityNotInSchema(ref err) if err == &entity_type),
                "expected EntityNotInSchema({}), got: {:?}",
                &entity_type,
                err,
            );

            let (token_name, err) = &error.errors[1];
            assert_eq!(token_name, "id_token");
            assert!(
                matches!(err, BuildEntityError::EntityNotInSchema(ref err) if err == &entity_type),
                "expected EntityNotInSchema({}), got: {:?}",
                &entity_type,
                err,
            );
        },
        _ => panic!("expected error BuildCedarlingEntityError::User"),
    }
}

/// Check if we get error on mapping workload to undefined entity
#[test]
async fn test_failed_workload_mapping() {
    let mut raw_config = get_raw_config(POLICY_STORE_RAW_YAML);

    let entity_type = "MappedWorkloadNotExist".to_string();
    raw_config.mapping_workload = Some(entity_type.clone());

    let config = crate::BootstrapConfig::from_raw_config(&raw_config)
        .expect("raw config should parse without errors");

    let cedarling = Cedarling::new(&config)
        .await
        .expect("could be created without error");

    let request = REQUEST.clone();

    let err = cedarling
        .authorize(request)
        .await
        .expect_err("request should be parsed with mapping error");

    match err {
        AuthorizeError::BuildEntity(BuildCedarlingEntityError::Workload(error)) => {
            assert_eq!(error.errors.len(), 2, "there should be 2 errors");

            // check for access token error
            let (token_name, err) = &error.errors[0];
            assert_eq!(token_name, "access_token");
            assert!(
                matches!(err, BuildEntityError::EntityNotInSchema(ref err) if err == &entity_type),
                "expected CouldNotFindEntity(\"{}\"), got: {:?}",
                &entity_type,
                err,
            );

            // check for id token error
            let (token_name, err) = &error.errors[1];
            assert_eq!(token_name, "id_token");
            assert!(
                matches!(err, BuildEntityError::EntityNotInSchema(ref err) if err == &entity_type),
                "expected CouldNotFindEntity(\"{}\"), got: {:?}",
                &entity_type,
                err,
            );
        },
        _ => panic!(
            "expected BuildEntity(BuildCedarlingEntityError::Workload(_))) error, got: {:?}",
            err
        ),
    }
}

/// Check if we get error on mapping id_token to undefined entity
#[test]
async fn test_failed_id_token_mapping() {
    let mut raw_config = get_raw_config(POLICY_STORE_RAW_YAML);
    raw_config.token_configs = serde_json::from_value(json!({
        "id_token": {
            "entity_type_name": "MappedIdTokenNotExist",
        },
    }))
    .expect("valid token configs");

    let config = crate::BootstrapConfig::from_raw_config(&raw_config)
        .expect("raw config should parse without errors");

    let cedarling = Cedarling::new(&config)
        .await
        .expect("could be created without error");

    let request = REQUEST.clone();

    let err = cedarling
        .authorize(request)
        .await
        .expect_err("request should be parsed with mapping error");

    match err {
        AuthorizeError::BuildEntity(BuildCedarlingEntityError::Token(BuildTokenEntityError {
            token_name,
            err,
        })) => {
            assert_eq!(token_name, "id_token");
            assert!(
                matches!(err, BuildEntityError::EntityNotInSchema(ref name) if name == "MappedIdTokenNotExist"),
                "expected EntityNotInSchema(\"MappedIdTokenNotExist\") got: {:?}",
                err
            );
        },
        _ => panic!(
            "expected BuildEntity(BuildCedarlingEntityError::IdToken(_)) error, got: {:?}",
            err
        ),
    }
}

/// Check if we get error on mapping access_token to undefined entity
#[test]
async fn test_failed_access_token_mapping() {
    let mut raw_config = get_raw_config(POLICY_STORE_RAW_YAML);

    raw_config.token_configs = serde_json::from_value(json!({
        "access_token": {
            "entity_type_name": "MappedAccess_tokenNotExist",
        },
    }))
    .expect("valid token configs");

    let config = crate::BootstrapConfig::from_raw_config(&raw_config)
        .expect("raw config should parse without errors");

    let cedarling = Cedarling::new(&config)
        .await
        .expect("could be created without error");

    let request = REQUEST.clone();

    let err = cedarling
        .authorize(request)
        .await
        .expect_err("request should be parsed with mapping error");

    match err {
        AuthorizeError::BuildEntity(BuildCedarlingEntityError::Token(BuildTokenEntityError {
            token_name,
            err,
        })) => {
            assert_eq!(token_name, "access_token");
            assert!(
                matches!(err, BuildEntityError::EntityNotInSchema(ref name) if name == "MappedAccess_tokenNotExist"),
                "expected EntityNotInSchema(\"MappedAccess_tokenNotExist\") got: {:?}",
                err
            );
        },
        _ => panic!("expected BuildEntity error, got: {:?}", err),
    }
}

/// Check if we get error on mapping userinfo_token to undefined entity
#[test]
async fn test_failed_userinfo_token_mapping() {
    let mut raw_config = get_raw_config(POLICY_STORE_RAW_YAML);

    raw_config.token_configs = serde_json::from_value(json!({
        "userinfo_token": {
            "entity_type_name": "MappedUserinfo_tokenNotExist",
        },
    }))
    .expect("valid token configs");

    let config = crate::BootstrapConfig::from_raw_config(&raw_config)
        .expect("raw config should parse without errors");

    let cedarling = Cedarling::new(&config)
        .await
        .expect("could be created without error");

    let request = REQUEST.clone();

    let err = cedarling
        .authorize(request)
        .await
        .expect_err("request should be parsed with mapping error");

    match err {
        AuthorizeError::BuildEntity(BuildCedarlingEntityError::Token(BuildTokenEntityError {
            token_name,
            err,
        })) => {
            assert_eq!(token_name, "userinfo_token");
            assert!(
                matches!(err, BuildEntityError::EntityNotInSchema(ref name) if name == "MappedUserinfo_tokenNotExist"),
                "expected EntityNotInSchema(\"MappedUserinfo_tokenNotExist\") got: {:?}",
                err
            );
        },
        _ => panic!("expected BuildEntity error, got: {:?}", err),
    }
}

/// Check if we get roles mapping from all tokens.
/// Because we specify mapping from each token in policy store
/// We use iss in JWT tokens to enable mapping for trusted issuer in policy store
#[test]
async fn test_role_many_tokens_mapping() {
    let raw_config = get_raw_config(POLICY_STORE_RAW_YAML);

    let config = crate::BootstrapConfig::from_raw_config(&raw_config)
        .expect("raw config should parse without errors");

    let cedarling = Cedarling::new(&config)
        .await
        .expect("could be created without error");

    let request = // deserialize `Request` from json
    Request::deserialize(serde_json::json!(
        {
            "tokens": {
                "access_token": generate_token_using_claims(json!({
                    "org_id": "some_long_id",
                    "jti": "some_jti",
                    "client_id": "some_client_id",
                    "iss": "https://test-casa.gluu.info",
                    "aud": "some_aud",
                    "role": "Guest",
                })),
                "id_token": generate_token_using_claims(json!({
                    "jti": "some_jti",
                    "iss": "https://test-casa.gluu.info",
                    "aud": "some_aud",
                    "sub": "some_sub",
                    "role": "User",
                })),
                "userinfo_token":  generate_token_using_claims(json!({
                    "jti": "some_jti",
                    "country": "US",
                    "sub": "some_sub",
                    "iss": "https://test-casa.gluu.info",
                    "client_id": "some_client_id",
                    "role": "Admin",
                })),
            },
            "action": "Jans::Action::\"Update\"",
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

    // HashSet of expected roles
    let mut expected_role_ids: HashSet<String> =
        HashSet::from_iter(["Guest", "User", "Admin"].into_iter().map(String::from));

    // iterate over roles that created and filter expected roles
    let roles_left = cedarling
        .build_entities(&request)
        .await
        .expect("should get authorize_entities_data without errors")
        .roles
        .into_iter()
        .map(|entity| entity.uid().id().escaped())
        // if role successfully removed from `expected_role_ids` we filter it
        .filter(|uid| !expected_role_ids.remove(uid.as_str()))
        .map(|v| v.to_string())
        .collect::<Vec<_>>();

    assert!(
        expected_role_ids.is_empty(),
        "HashSet `expected_role_ids` should be empty, not created roles: {expected_role_ids:?}"
    );

    assert!(
        roles_left.is_empty(),
        "list `roles_left` should be empty, additional created roles: {roles_left:?}"
    )
}
