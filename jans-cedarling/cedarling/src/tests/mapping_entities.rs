/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

//! In this test cases we check mapping entities using Bootstrap properties:
//! CEDARLING_MAPPING_USER
//! CEDARLING_MAPPING_WORKLOAD
//! CEDARLING_MAPPING_ID_TOKEN
//! CEDARLING_MAPPING_ACCESS_TOKEN
//! CEDARLING_MAPPING_USERINFO_TOKEN

use super::utils::*;
use crate::{cmp_decision, cmp_policy, CedarPolicyCreateTypeError};
use crate::{AuthorizeError, Cedarling};
use cedarling_util::get_raw_config;
use std::sync::LazyLock;
use test_utils::assert_eq;

static POLICY_STORE_RAW_YAML: &str =
    include_str!("../../../test_files/policy-store_entity_mapping.yaml");

/// static [`Request`] value that will be used in tests
static REQUEST: LazyLock<Request> = LazyLock::new(|| {
    // deserialize `Request` from json
    Request::deserialize(serde_json::json!(
        {
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
fn test_default_mapping() {
    let raw_config = get_raw_config(POLICY_STORE_RAW_YAML);
    let config = crate::BootstrapConfig::from_raw_config(&raw_config)
        .expect("raw config should parse without errors");
    let cedarling = Cedarling::new(&config).expect("could be created without error");

    let request = REQUEST.clone();

    let result = cedarling
        .authorize(request)
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

    assert!(result.is_allowed(), "request result should be allowed");
}

/// Validate mapping entities.
/// This function validates the mapping of users and workloads using the defined `cedar` schema.
/// For other entities, currently, it is not possible to automatically validate the mapping.
///
/// TODO: Add validation for `IdToken`, `Access_token`, and `Userinfo_token` once they are added to the context.
///
/// Note: Verified that the mapped entity types are present in the logs.
#[test]
fn test_custom_mapping() {
    let mut raw_config = get_raw_config(POLICY_STORE_RAW_YAML);

    raw_config.mapping_user = Some("MappedUser".to_string());
    raw_config.mapping_workload = Some("MappedWorkload".to_string());
    raw_config.mapping_id_token = Some("MappedIdToken".to_string());
    raw_config.mapping_access_token = Some("MappedAccess_token".to_string());
    raw_config.mapping_userinfo_token = Some("MappedUserinfo_token".to_string());

    let config = crate::BootstrapConfig::from_raw_config(&raw_config)
        .expect("raw config should parse without errors");
    let cedarling = Cedarling::new(&config).expect("could be created without error");

    let mut request = REQUEST.clone();
    request.action = "Jans::Action::\"UpdateMappedWorkloadAndUser\"".to_string();

    let result = cedarling
        .authorize(request)
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

    assert!(result.is_allowed(), "request result should be allowed");
}

/// Check if we get error on mapping user to undefined entity
#[test]
fn test_failed_user_mapping() {
    let mut raw_config = get_raw_config(POLICY_STORE_RAW_YAML);

    raw_config.mapping_user = Some("MappedUserNotExist".to_string());

    let config = crate::BootstrapConfig::from_raw_config(&raw_config)
        .expect("raw config should parse without errors");

    let cedarling = Cedarling::new(&config).expect("could be created without error");

    let request = REQUEST.clone();

    let err = cedarling
        .authorize(request)
        .expect_err("request should be parsed with mapping error");

    assert!(
        matches!(
            err,
            AuthorizeError::CreateUserEntity(CedarPolicyCreateTypeError::CouldNotFindEntity(_))
        ),
        "should be error CouldNotFindEntity, got: {:?}",
        err
    );
}

/// Check if we get error on mapping workload to undefined entity
#[test]
fn test_failed_workload_mapping() {
    let mut raw_config = get_raw_config(POLICY_STORE_RAW_YAML);

    raw_config.mapping_workload = Some("MappedWorkloadNotExist".to_string());

    let config = crate::BootstrapConfig::from_raw_config(&raw_config)
        .expect("raw config should parse without errors");

    let cedarling = Cedarling::new(&config).expect("could be created without error");

    let request = REQUEST.clone();

    let err = cedarling
        .authorize(request)
        .expect_err("request should be parsed with mapping error");

    assert!(
        matches!(
            err,
            AuthorizeError::CreateWorkloadEntity(CedarPolicyCreateTypeError::CouldNotFindEntity(_))
        ),
        "should be error CouldNotFindEntity"
    );
}

/// Check if we get error on mapping id_token to undefined entity
#[test]
fn test_failed_id_token_mapping() {
    let mut raw_config = get_raw_config(POLICY_STORE_RAW_YAML);

    raw_config.mapping_id_token = Some("MappedIdTokenNotExist".to_string());

    let config = crate::BootstrapConfig::from_raw_config(&raw_config)
        .expect("raw config should parse without errors");

    let cedarling = Cedarling::new(&config).expect("could be created without error");

    let request = REQUEST.clone();

    let err = cedarling
        .authorize(request)
        .expect_err("request should be parsed with mapping error");

    assert!(
        matches!(
            err,
            AuthorizeError::CreateIdTokenEntity(CedarPolicyCreateTypeError::CouldNotFindEntity(_))
        ),
        "should be error CouldNotFindEntity, got: {err:?}"
    );
}

/// Check if we get error on mapping access_token to undefined entity
#[test]
fn test_failed_access_token_mapping() {
    let mut raw_config = get_raw_config(POLICY_STORE_RAW_YAML);

    raw_config.mapping_access_token = Some("MappedAccess_tokenNotExist".to_string());

    let config = crate::BootstrapConfig::from_raw_config(&raw_config)
        .expect("raw config should parse without errors");

    let cedarling = Cedarling::new(&config).expect("could be created without error");

    let request = REQUEST.clone();

    let err = cedarling
        .authorize(request)
        .expect_err("request should be parsed with mapping error");

    assert!(
        matches!(
            err,
            AuthorizeError::CreateAccessTokenEntity(
                CedarPolicyCreateTypeError::CouldNotFindEntity(_)
            )
        ),
        "should be error CouldNotFindEntity"
    );
}

/// Check if we get error on mapping userinfo_token to undefined entity
#[test]
fn test_failed_userinfo_token_mapping() {
    let mut raw_config = get_raw_config(POLICY_STORE_RAW_YAML);

    raw_config.mapping_userinfo_token = Some("MappedUserinfo_tokenNotExist".to_string());

    let config = crate::BootstrapConfig::from_raw_config(&raw_config)
        .expect("raw config should parse without errors");

    let cedarling = Cedarling::new(&config).expect("could be created without error");

    let request = REQUEST.clone();

    let err = cedarling
        .authorize(request)
        .expect_err("request should be parsed with mapping error");

    assert!(
        matches!(
            err,
            AuthorizeError::CreateUserinfoTokenEntity(
                CedarPolicyCreateTypeError::CouldNotFindEntity(_)
            )
        ),
        "should be error CouldNotFindEntity"
    );
}
