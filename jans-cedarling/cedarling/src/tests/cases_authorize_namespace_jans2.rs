// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashMap;

use cedarling_util::get_config;
use test_utils::assert_eq;
use tokio::test;

use super::utils::*;
use crate::{Cedarling, cmp_decision, cmp_policy}; // macros is defined in the cedarling\src\tests\utils\cedarling_util.rs

static POLICY_STORE_RAW_YAML: &str =
    include_str!("../../../test_files/policy-store_ok_namespace_Jans2.yaml");

/// We check if application support non standart namespace
/// In previous we hardcoded creating entities in namespace `Jans`
/// in `POLICY_STORE_RAW_YAML` is used namespace `Jans2`
#[test]
async fn test_namespace_jans2() {
    let policy_src = PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string());
    let mut bs_config = get_config(policy_src);
    bs_config.authorization_config.mapping_user = Some("Jans2::User".to_string());
    bs_config.authorization_config.mapping_workload = Some("Jans2::Workload".to_string());
    bs_config.authorization_config.mapping_role = Some("Jans2::Role".to_string());
    bs_config.authorization_config.mapping_tokens = HashMap::from([
        (
            "access_token".to_string(),
            "Jans2::Access_token".to_string(),
        ),
        ("id_token".to_string(), "Jans2::id_token".to_string()),
        (
            "userinfo_token".to_string(),
            "Jans2::Userinfo_token".to_string(),
        ),
    ])
    .into();
    let cedarling = Cedarling::new(&bs_config)
        .await
        .expect("bootstrap config should initialize correctly");

    // deserialize `Request` from json
    let request = Request::deserialize(serde_json::json!(
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
                    "country": "US",
                    "sub": "some_sub",
                    "iss": "some_iss",
                    "jti": "some_jti",
                    "client_id": "some_client_id",
                    "role": ["Admin"],
                })),
            },
            "action": "Jans2::Action::\"Update\"",
            "resource": {
                "id": "random_id",
                "type": "Jans2::Issue",
                "org_id": "some_long_id",
                "country": "US"
            },
            "context": {},
        }
    ))
    .expect("Request should be deserialized from json");

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
        vec!["2"],
        "reason of permit person should be '2'"
    );

    assert!(result.decision, "request result should be allowed");
}
