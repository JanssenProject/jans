// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! This module contains tests for JSON logic functionality.  
//! Specifically, we test the result of `AuthorizeResult::new`, which is based on the evaluation of JSON logic for principals.

use std::collections::HashSet;

use crate::common::json_rules::ApplyRuleError;
use cedar_policy::Decision;
use serde_json::json;
use test_utils::assert_eq;
use uuid7::uuid4;

use crate::{AuthorizeResult, JsonRule};

fn get_result(
    workload: Option<bool>,
    person: Option<bool>,
    rule: &JsonRule,
) -> Result<AuthorizeResult, ApplyRuleError> {
    let workload_response = workload.map(|workload| {
        cedar_policy::Response::new(
            if workload {
                Decision::Allow
            } else {
                Decision::Deny
            },
            HashSet::new(),
            Vec::new(),
        )
    });

    let person_response = person.map(|person| {
        cedar_policy::Response::new(
            if person {
                Decision::Allow
            } else {
                Decision::Deny
            },
            HashSet::new(),
            Vec::new(),
        )
    });

    AuthorizeResult::new(
        rule,
        workload.is_some().then_some("Jans::Workload".into()),
        person.is_some().then_some("Jans::User".into()),
        workload_response,
        person_response,
        // just randomly generated UUID
        uuid4(),
    )
}

/// Test JSON Rule with `and` operator
#[test]
fn test_json_rule_and() {
    let rule = JsonRule::new(json!({
        "and" : [
            {"===": [{"var": "Jans::Workload"}, "ALLOW"]},
            {"===": [{"var": "Jans::User"}, "ALLOW"]}
        ]
    }))
    .unwrap();

    let result = get_result(Some(true), Some(true), &rule)
        .expect("should not fail when both workload and user are ALLOW");
    assert_eq!(
        result.decision, true,
        "Decision should be ALLOW for AND rule and both workload and user are ALLOW"
    );

    let result = get_result(Some(false), Some(false), &rule)
        .expect("should not fail when both workload and user are DENY");
    assert_eq!(
        result.decision, false,
        "Decision should be DENY for AND rule and both workload and user are DENY"
    );

    let result = get_result(Some(true), Some(false), &rule)
        .expect("should not fail when workload is ALLOW and user is DENY");
    assert_eq!(
        result.decision, false,
        "Decision should be DENY for AND rule when workload is ALLOW and user is DENY"
    );

    let result = get_result(Some(false), Some(true), &rule)
        .expect("should not fail when workload is DENY and user is ALLOW");
    assert_eq!(
        result.decision, false,
        "Decision should be DENY for AND rule when workload is DENY and user is ALLOW"
    );
}

/// Test JSON Rule with `or` operator
#[test]
fn test_json_rule_or() {
    let rule = JsonRule::new(json!({
        "or" : [
            {"===": [{"var": "Jans::Workload"}, "ALLOW"]},
            {"===": [{"var": "Jans::User"}, "ALLOW"]}
        ]
    }))
    .unwrap();

    let result = get_result(Some(true), Some(true), &rule)
        .expect("should not fail when both workload and user are ALLOW");
    assert_eq!(
        result.decision, true,
        "Decision should be ALLOW for OR rule and both workload and user are ALLOW"
    );

    let result = get_result(Some(false), Some(false), &rule)
        .expect("should not fail when both workload and user are DENY");
    assert_eq!(
        result.decision, false,
        "Decision should be DENY for OR rule when both workload and user are DENY"
    );

    let result = get_result(Some(true), Some(false), &rule)
        .expect("should not fail when workload is ALLOW and user is DENY");
    assert_eq!(
        result.decision, true,
        "Decision should be ALLOW for OR rule when workload is ALLOW and user is DENY"
    );

    let result = get_result(Some(false), Some(true), &rule)
        .expect("should not fail when workload is DENY and user is ALLOW");
    assert_eq!(
        result.decision, true,
        "Decision should be ALLOW for OR rule when workload is DENY and user is ALLOW"
    );
}

#[test]
fn test_json_rule_and_operator_with_empty_principal() {
    let rule = JsonRule::new(json!({
        "and" : [
            {"===": [{"var": "Jans::Workload"}, "ALLOW"]},
            {"===": [{"var": "Jans::User"}, "ALLOW"]}
        ]
    }))
    .unwrap();

    let result = get_result(None, Some(true), &rule)
        .expect("should not fail when workload is None and user is ALLOW");
    assert_eq!(
        result.decision, false,
        "Decision should be DENY for AND rule when one of the principals is missing"
    );
}

#[test]
fn test_json_rule_or_operator_with_empty_principal() {
    let rule = JsonRule::new(json!({
        "or" : [
            {"===": [{"var": "Jans::Workload"}, "ALLOW"]},
            {"===": [{"var": "Jans::User"}, "ALLOW"]}
        ]
    }))
    .unwrap();

    let result = get_result(None, Some(true), &rule)
        .expect("should not fail when workload is None and user is ALLOW");
    assert_eq!(
        result.decision, true,
        "Decision should be ALLOW  for OR rule when one of the principals is missing"
    );
}

/// Tests using the `==` operator. (Can throw Nan, when var not present)
#[test]
fn test_using_eq_operator() {
    let rule = JsonRule::new(json!({
        "or" : [
            {"==": [{"var": "Jans::Workload"}, "ALLOW"]},
            {"==": [{"var": "Jans::User"}, "ALLOW"]}
        ]
    }))
    .unwrap();

    let _ = get_result(None, Some(true), &rule)
        .expect_err("should throw an error when workload is None because throw Nan");

    // we should get error in this case, but looks like it is not throwing error because we use OR operator and first condition is true
    let _ = get_result(Some(true), None, &rule)
        .expect("we don't expect error because we use OR operator and first condition is true");
}

/// Test with only workload principal.
#[test]
fn test_with_only_person_principal() {
    let rule = JsonRule::new(json!({
        "or" : [
            {"===": [{"var": "Jans::Workload"}, "ALLOW"]},
        ]
    }))
    .unwrap();

    let result = get_result(Some(true), Some(true), &rule)
        .expect("should not fail when both workload and user are ALLOW");

    assert_eq!(
        result.decision, true,
        "Decision should be ALLOW for OR rule and both workload and user are ALLOW"
    );
}

/// test when compare with bool
#[test]
fn test_where_compare_with_bool() {
    let rule = JsonRule::new(json!({
        "or" : [
            {"===": [{"var": "Jans::Workload"}, true]},
        ]
    }))
    .unwrap();

    let result = get_result(Some(true), Some(true), &rule)
        .expect("should not fail when both workload and user are ALLOW");

    assert_eq!(
        result.decision, false,
        "Decision should be DENY because the comparison is with a boolean value, not a string"
    );
}

/// test when compare using operator `eq` with bool
#[test]
fn test_where_compare_op_eq_with_bool() {
    let rule = JsonRule::new(json!({
        "or" : [
            {"==": [{"var": "Jans::Workload"}, true]},
        ]
    }))
    .unwrap();

    let _ = get_result(Some(true), Some(true), &rule)
        .expect_err("should fail when comparing using `==` operator with bool");
}
