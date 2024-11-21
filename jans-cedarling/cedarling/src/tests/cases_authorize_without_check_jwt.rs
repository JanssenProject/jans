/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::utils::*;
use crate::{cmp_decision, cmp_policy}; // macros is defined in the cedarling\src\tests\utils\cedarling_util.rs
use test_utils::assert_eq;

static POLICY_STORE_RAW_YAML: &str = include_str!("../../../test_files/policy-store_ok_2.yaml");

/// Success test case where all check a successful
/// role field in the `userinfo_token` because we search here by default
/// role field is string.
///
/// we check here that field are parsed from JWT tokens
/// and correctly executed using correct cedar-policy id
#[test]
fn success_test_role_string() {
    let cedarling = get_cedarling(PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()));

    // deserialize `Request` from json
    let request = Request::deserialize(serde_json::json!(
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
    .expect("Request should be deserialized from json");

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

/// forbid test case where all check of role is forbid
/// role field in the `userinfo_token` because we search here by default
/// role field is string and is "Guest".
///
/// we check here that field are parsed from JWT tokens
/// and correctly executed using correct cedar-policy id
#[test]
fn forbid_test_role_guest() {
    let cedarling = get_cedarling(PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()));

    // deserialize `Request` from json
    let request = Request::deserialize(serde_json::json!(
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
                    "role": "Guest",
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
    .expect("Request should be deserialized from json");

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
        Decision::Deny,
        "request result should be not allowed for person with role Guest"
    );

    cmp_policy!(
        result.person,
        vec!["4"],
        "reason of permit person should be '2' and '4'"
    );

    assert!(!result.is_allowed(), "request result should be not allowed");
}

/// Success test case where all check a successful
/// role field in the `userinfo_token` because we search here by default
/// role field is array of string.
///
/// we check here that field are parsed from JWT tokens
/// and correctly executed using correct cedar-policy id
#[test]
fn success_test_role_array() {
    let cedarling = get_cedarling(PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()));

    // deserialize `Request` from json
    let request = Request::deserialize(serde_json::json!(
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
                    "role": ["Admin"],
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
    .expect("Request should be deserialized from json");

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

/// Success test case where all check a successful
/// role field is not present
///
/// we check here that field are parsed from JWT tokens
/// and correctly executed using correct cedar-policy id
/// if role field is not present, just ignore role check
#[test]
fn success_test_no_role() {
    let cedarling = get_cedarling(PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()));

    // deserialize `Request` from json
    let request = Request::deserialize(serde_json::json!(
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
                    // comment role field (removed)
                    // "role": ["Admin"],
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
    .expect("Request should be deserialized from json");

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

    assert!(
        result.is_allowed(),
        "request result should be allowed, because workload and user allowed"
    );
}

/// Success test case where all check a successful
///
/// we check here that field for `Jans::User` is present in `id_token`
/// it is `country` field of `Jans::User` and role field is present
#[test]
fn success_test_user_data_in_id_token() {
    let cedarling = get_cedarling(PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()));

    // deserialize `Request` from json
    let request = Request::deserialize(serde_json::json!(
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
                    "country": "US",
                  })),
            "userinfo_token":  generate_token_using_claims(json!({
                    "jti": "some_jti",
                    "sub": "some_sub",
                    "iss": "some_iss",
                    "client_id": "some_client_id",
                    "role": ["Admin"],
                    "country": "US",
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
    .expect("Request should be deserialized from json");

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

// check all forbid
#[test]
fn all_forbid() {
    let cedarling = get_cedarling(PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()));

    // deserialize `Request` from json
    let request = Request::deserialize(serde_json::json!(
        {
            "access_token": generate_token_using_claims(json!({
                  // org_id different from resource
                    "org_id": "some_long_id_2",
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
                  // country different from resource
                    "country": "UK",
                    "sub": "some_sub",
                    "iss": "some_iss",
                    "jti": "some_jti",
                    "client_id": "some_client_id",
                    // role not Admin
                    "role": ["Guest"],
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
    .expect("Request should be deserialized from json");

    let result = cedarling
        .authorize(request)
        .expect("request should be parsed without errors");

    cmp_decision!(
        result.workload,
        Decision::Deny,
        "request result should be forbidden for workload"
    );

    cmp_policy!(
        result.workload,
        Vec::new() as Vec<String>,
        "reason of permit workload should be empty"
    );

    cmp_decision!(
        result.person,
        Decision::Deny,
        "request result should be forbidden for person with role Guest"
    );

    cmp_policy!(
        result.person,
        vec!["4"],
        "reason of forbid person should empty, no forbid rule"
    );

    assert!(!result.is_allowed(), "request result should be not allowed");
}

// check only workload permit and other not
#[test]
fn only_workload_permit() {
    let cedarling = get_cedarling(PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()));

    // deserialize `Request` from json
    let request = Request::deserialize(serde_json::json!(
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
                  // country different from resource
                    "country": "UK",
                    "sub": "some_sub",
                    "iss": "some_iss",
                    "client_id": "some_client_id",
                    // role not Admin
                    "role": ["Guest"],
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
    .expect("Request should be deserialized from json");

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
        Decision::Deny,
        "request result should be forbidden for person"
    );

    cmp_decision!(
        result.person,
        Decision::Deny,
        "request result should be forbidden for person"
    );

    cmp_policy!(
        result.person,
        vec!["4"],
        "reason of forbid person should empty, no forbid rule"
    );

    assert!(!result.is_allowed(), "request result should be not allowed");
}

// check only person permit and other not
#[test]
fn only_person_permit() {
    let cedarling = get_cedarling(PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()));

    // deserialize `Request` from json
    let request = Request::deserialize(serde_json::json!(
        {
            "access_token": generate_token_using_claims(json!({
                  // org_id different from resource
                    "org_id": "some_long_id_2",
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
                  // country different from resource
                    "country": "US",
                    "sub": "some_sub",
                    "iss": "some_iss",
                    "client_id": "some_client_id",
                    // role not present, commented line
                    // "role": ["Guest"],
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
    .expect("Request should be deserialized from json");

    let result = cedarling
        .authorize(request)
        .expect("request should be parsed without errors");

    cmp_decision!(
        result.workload,
        Decision::Deny,
        "request result should be forbidden for workload"
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
        "reason of forbid person should '2'"
    );

    assert!(!result.is_allowed(), "request result should be not allowed");
}

// check only user role permit and other not
#[test]
fn only_user_role_permit() {
    let cedarling = get_cedarling(PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()));

    // deserialize `Request` from json
    let request = Request::deserialize(serde_json::json!(
        {
            "access_token": generate_token_using_claims(json!({
                  // org_id different from resource
                    "org_id": "some_long_id_2",
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
                  // country different from resource
                    "country": "UK",
                    "sub": "some_sub",
                    "iss": "some_iss",
                    "client_id": "some_client_id",
                    "role": ["Admin"],
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
    .expect("Request should be deserialized from json");

    let result = cedarling
        .authorize(request)
        .expect("request should be parsed without errors");

    cmp_decision!(
        result.workload,
        Decision::Deny,
        "request result should be forbidden for workload"
    );

    cmp_policy!(
        result.workload,
        Vec::new() as Vec<String>,
        "reason of permit workload should be empty"
    );

    cmp_decision!(
        result.person,
        Decision::Allow,
        "request result should be forbidden for person"
    );

    cmp_policy!(
        result.person,
        vec!["3"],
        "reason of forbid person '3', permit for role Admin"
    );

    assert!(!result.is_allowed(), "request result should be not allowed");
}

// check only workload and person permit and role not
#[test]
fn only_workload_and_person_permit() {
    let cedarling = get_cedarling(PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()));

    // deserialize `Request` from json
    let request = Request::deserialize(serde_json::json!(
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
                    // role vector is empty
                    "role": [],
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
    .expect("Request should be deserialized from json");

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
        "reason of permit person should '2'"
    );

    assert!(result.is_allowed(), "request result should be allowed");
}

// check only workload and role permit and user not
#[test]
fn only_workload_and_role_permit() {
    let cedarling = get_cedarling(PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()));

    // deserialize `Request` from json
    let request = Request::deserialize(serde_json::json!(
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
                  // country different from resource
                    "country": "UK",
                    "sub": "some_sub",
                    "iss": "some_iss",
                    "client_id": "some_client_id",
                    "role": ["Admin"],
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
    .expect("Request should be deserialized from json");

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
        "request result should be not allowed for person"
    );

    cmp_policy!(
        result.person,
        vec!["3"],
        "reason of forbid person should be none, but we have permit for role"
    );

    assert!(result.is_allowed(), "request result should be allowed");
}
