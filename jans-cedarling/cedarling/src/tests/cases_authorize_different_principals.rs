/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

//! In this module we test authorize different action
//! where not all principals can be applied
//!
//! all case scenario should have `result.is_allowed() == true`
//! because we have checked different scenarios in `cases_authorize_without_check_jwt.rs`

use super::utils::*;
use crate::{cmp_decision, cmp_policy}; // macros is defined in the cedarling\src\tests\utils\cedarling_util.rs
use lazy_static::lazy_static;
use test_utils::assert_eq;

static POLICY_STORE_RAW_YAML: &str = include_str!("../../../test_files/policy-store_ok_2.yaml");

lazy_static! {
    pub(crate) static ref AuthRequestBase: Request = Request::deserialize(serde_json::json!(
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

/// Check if action executes for next principals: Workload, User, Role
#[test]
fn success_test_for_all_principals() {
    let cedarling = get_cedarling(PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()));

    let mut request = AuthRequestBase.clone();
    request.action = "Jans::Action::\"Update\"".to_string();

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
        vec!["2"],
        "reason of permit person should be '2'"
    );

    cmp_decision!(
        result.role,
        Decision::Allow,
        "request result should be allowed for role"
    );

    cmp_policy!(
        result.role,
        vec!["3"],
        "reason of permit role should be '3'"
    );

    assert!(result.is_allowed(), "request result should be allowed");
}

/// Check if action executes for next principals: Workload
#[test]
fn success_test_for_principal_workload() {
    let cedarling = get_cedarling(PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()));

    let mut request = AuthRequestBase.clone();
    request.action = "Jans::Action::\"UpdateForWorkload\"".to_string();

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

    assert!(result.person.is_none(), "result for person should be none");

    assert!(result.role.is_none(), "result for role should be none");

    assert!(result.is_allowed(), "request result should be allowed");
}

/// Check if action executes for next principals: User
#[test]
fn success_test_for_principal_user() {
    let cedarling = get_cedarling(PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()));

    let mut request = AuthRequestBase.clone();
    request.action = "Jans::Action::\"UpdateForUser\"".to_string();

    let result = cedarling
        .authorize(request)
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

    assert!(result.role.is_none(), "result for role should be none");

    assert!(result.is_allowed(), "request result should be allowed");
}

/// Check if action executes for next principals: Role
#[test]
fn success_test_for_principal_role() {
    let cedarling = get_cedarling(PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()));

    let mut request = AuthRequestBase.clone();
    request.action = "Jans::Action::\"UpdateForRole\"".to_string();

    let result = cedarling
        .authorize(request)
        .expect("request should be parsed without errors");

    cmp_decision!(
        result.role,
        Decision::Allow,
        "request result should be allowed for role"
    );
    cmp_policy!(
        result.role,
        vec!["3"],
        "reason of permit person should be '3'"
    );

    assert!(
        result.workload.is_none(),
        "result for workload should be none"
    );

    assert!(result.person.is_none(), "result for person should be none");

    assert!(result.is_allowed(), "request result should be allowed");
}

/// Check if action executes for next principals: Person AND Role
#[test]
fn success_test_for_principal_person_role() {
    let cedarling = get_cedarling(PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()));

    let mut request = AuthRequestBase.clone();
    request.action = "Jans::Action::\"UpdateForUserAndRole\"".to_string();

    let result = cedarling
        .authorize(request)
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

    cmp_decision!(
        result.role,
        Decision::Allow,
        "request result should be allowed for role"
    );
    cmp_policy!(
        result.role,
        vec!["3"],
        "reason of permit person should be '3'"
    );

    assert!(
        result.workload.is_none(),
        "result for workload should be none"
    );

    assert!(result.is_allowed(), "request result should be allowed");
}

/// Check if action executes for next principals: Person AND Role
#[test]
fn success_test_for_principal_workload_role() {
    let cedarling = get_cedarling(PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()));

    let mut request = AuthRequestBase.clone();
    request.action = "Jans::Action::\"UpdateForWorkloadAndRole\"".to_string();

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
        result.role,
        Decision::Allow,
        "request result should be allowed for role"
    );
    cmp_policy!(
        result.role,
        vec!["3"],
        "reason of permit person should be '3'"
    );

    assert!(result.person.is_none(), "result for person should be none");

    assert!(result.is_allowed(), "request result should be allowed");
}

/// Check if action executes when principal can't be applied
#[test]
fn test_where_principal_cant_be_applied() {
    let cedarling = get_cedarling(PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()));

    let mut request = AuthRequestBase.clone();
    request.action = "Jans::Action::\"NoApplies\"".to_string();

    let result = cedarling
        .authorize(request)
        .expect("request should be parsed without errors");

    assert!(
        result.workload.is_none(),
        "result for workload should be none"
    );

    assert!(result.person.is_none(), "result for person should be none");

    assert!(result.role.is_none(), "result for role should be none");

    assert!(!result.is_allowed(), "request result should be not allowed");
}
