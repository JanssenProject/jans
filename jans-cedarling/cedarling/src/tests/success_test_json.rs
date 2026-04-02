// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::utils::cedarling_util::get_cedarling_with_callback;
use super::utils::test_helpers::create_test_unsigned_request;
use super::utils::*;
use crate::{EntityData, JsonRule};
use serde_json::json;
use tokio::test;

/// Test success scenario with authorization using `authorize_unsigned`.
#[test]
async fn success_test_json() {
    // Use policy store that has UpdateForTestPrincipals and TestPrincipal types
    static POLICY_STORE_RAW_JSON: &str = include_str!("../../../test_files/policy-store_ok_2.yaml");

    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_JSON.to_string()),
        |config| {
            config.authorization_config.principal_bool_operator = JsonRule::new(json!({
                "===": [{"var": "Jans::TestPrincipal1"}, "ALLOW"]
            }))
            .expect("principal bool operator");
        },
    )
    .await;

    let resource = EntityData::deserialize(json!({
        "cedar_entity_mapping": {
            "entity_type": "Jans::Issue",
            "id": "random_id"
        },
        "org_id": "some_long_id",
        "country": "US"
    }))
    .expect("resource entity should deserialize");

    let principal = EntityData::deserialize(json!({
        "cedar_entity_mapping": {
            "entity_type": "Jans::TestPrincipal1",
            "id": "1"
        },
        "is_ok": true
    }))
    .expect("principal entity should deserialize");

    let request = create_test_unsigned_request(
        "Jans::Action::\"UpdateForTestPrincipals\"",
        vec![principal],
        resource,
    );

    let result = cedarling
        .authorize_unsigned(request)
        .await
        .expect("request should be parsed without errors");

    assert!(
        result.decision,
        "request result should be allowed: {:?}",
        result.principals
    );
}
