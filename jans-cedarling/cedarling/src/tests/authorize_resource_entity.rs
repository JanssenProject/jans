// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! In this module we test authorize different action
//! where not all principals can be applied
//!
//! all case scenario should have `result.decision == true`
//! because we have checked different scenarios in `cases_authorize_without_check_jwt.rs`

use std::sync::LazyLock;

use cedar_policy::EntityUid;
use test_utils::{SortedJson, assert_eq};
use tokio::test;

use super::utils::*;
use crate::{IdTokenTrustMode, JsonRule};

static POLICY_STORE_RAW_YAML: &str = include_str!("../../../test_files/policy-store_ok_2.yaml");

static OPERATOR_WORKLOAD: LazyLock<JsonRule> =
    LazyLock::new(|| JsonRule::new(json!({"===": [{"var": "Jans::Workload"}, "ALLOW"]})).unwrap());

pub(crate) static AUTH_REQUEST_BASE: LazyLock<Request> = LazyLock::new(|| {
    Request::deserialize(serde_json::json!(
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
            "action": "Jans::Action::\"UpdateForWorkload\"",
            // we use resource from default entities
            // all attributes should be retrieved there
            "resource": {
                "cedar_entity_mapping": {
                    "entity_type": "Jans::Issue",
                    "id": "SomeNotRandomID1234"
                },
            },
            "context": {},
        }
    ))
    .expect("Request should be deserialized from json")
});

/// Check resource entity defined in default entities.
#[test]
async fn check_default_authorize_resource_entity() {
    let cedarling = get_cedarling_with_authorization_conf(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string()),
        crate::AuthorizationConfig {
            use_user_principal: false,
            use_workload_principal: true,
            principal_bool_operator: OPERATOR_WORKLOAD.to_owned(),
            id_token_trust_mode: IdTokenTrustMode::Never,
            ..Default::default()
        },
        {
            let mut config = crate::EntityBuilderConfig::default().with_workload();
            config.build_user = false;
            config
        },
    )
    .await;

    let request = AUTH_REQUEST_BASE.clone();

    let auth_entities = cedarling
        .build_entities(&request)
        .await
        .expect("entities should be build without errors");

    let default_resource_entity_value1 = json!({
      "uid": {
        "type": "Jans::Issue",
        "id": "SomeNotRandomID1234"
      },
      "attrs": {
        "org_id": "some_long_id",
        "country": "US"
      },
      "parents": [
        {
          "type": "Jans::BaseIssue",
          "id": "SomeNotRandomID12345"
        }
      ]
    });

    // check if resource same as in default entities
    assert_eq!(
        auth_entities.resource.to_json_value().unwrap().sorted(),
        default_resource_entity_value1.clone().sorted(),
        "result resource entity should be the same as in default entities"
    );

    let euid1 = EntityUid::from_json(
        serde_json::json!({ "__entity": { "type": "Jans::Issue", "id": "SomeNotRandomID1234" } }),
    )
    .unwrap();

    let entity1 = auth_entities.default_entities.get(&euid1).unwrap();
    assert_eq!(
        entity1.to_json_value().unwrap().sorted(),
        default_resource_entity_value1.sorted(),
        "default entity should be the same as resource"
    );

    let default_resource_entity_value2 = json!({
      "uid": {
        "type": "Jans::BaseIssue",
        "id": "SomeNotRandomID12345"
      },
      "attrs": {
        "org_id": "some_long_id",
        "country": "US"
      },
      "parents": []
    });

    let euid2 = EntityUid::from_json(
        serde_json::json!({ "__entity": { "type": "Jans::BaseIssue", "id": "SomeNotRandomID12345" } }),
    )
    .unwrap();

    let entity2 = auth_entities.default_entities.get(&euid2).unwrap();
    assert_eq!(
        entity2.to_json_value().unwrap().sorted(),
        default_resource_entity_value2.sorted(),
        "default entity should be the same as resource"
    );
}
