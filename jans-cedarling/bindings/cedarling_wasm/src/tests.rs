// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

// allow dead code to avoid highlight test functions (by linter) that is used only using WASM
#![allow(dead_code)]

use std::sync::LazyLock;

use crate::*;

use cedarling::{ResourceData, Tokens};
use serde::Deserialize;
use serde_json::json;
use test_utils::token_claims::generate_token_using_claims;
use wasm_bindgen_test::*;

wasm_bindgen_test_configure!(run_in_browser);

// Reuse json policy store file from python example.
// Because for `BootstrapConfigRaw` we need to use JSON
static POLICY_STORE_RAW_YAML: &str =
    include_str!("../../../bindings/cedarling_python/example_files/policy-store.json");

static BOOTSTRAP_CONFIG: LazyLock<serde_json::Value> = LazyLock::new(|| {
    json!({
        "CEDARLING_APPLICATION_NAME": "My App",
        "CEDARLING_LOCAL_POLICY_STORE": POLICY_STORE_RAW_YAML,
        "CEDARLING_LOG_TYPE": "std_out",
        "CEDARLING_LOG_LEVEL": "INFO",
        "CEDARLING_USER_AUTHZ": "enabled",
        "CEDARLING_WORKLOAD_AUTHZ": "enabled",
        "CEDARLING_USER_WORKLOAD_BOOLEAN_OPERATION": "AND",
        "CEDARLING_ID_TOKEN_TRUST_MODE": "strict",

    })
});

/// test init with map value using `Cedarling::new_from_map`
#[wasm_bindgen_test]
async fn test_cedarling_new_from_map() {
    let bootstrap_config_json = BOOTSTRAP_CONFIG.clone();
    let conf_map_js_value = serde_wasm_bindgen::to_value(&bootstrap_config_json)
        .expect("serde json value should be converted to JsValue");
    console_log!("conf_map_js_value: {conf_map_js_value:?}");

    let conf_js_map: Map = conf_map_js_value.unchecked_into();
    console_log!("conf_js_map: {conf_js_map:?}");
    let _instance = Cedarling::new_from_map(conf_js_map.clone())
        .await
        .inspect(|_| console_log!("Cedarling::new_from_map initialized successfully"))
        .expect("Cedarling::new_from_map should be initialized");
}

/// test init with map value using `init`
#[wasm_bindgen_test]
async fn test_init_conf_as_map() {
    let bootstrap_config_json = BOOTSTRAP_CONFIG.clone();
    let conf_map_js_value = serde_wasm_bindgen::to_value(&bootstrap_config_json)
        .expect("serde json value should be converted to JsValue");
    console_log!("conf_map_js_value: {conf_map_js_value:?}");

    let _instance = init(conf_map_js_value)
        .await
        .inspect(|_| console_log!("init initialized successfully"))
        .expect("init function should be initialized with js map");
}

/// test init with object value using `Cedarling::new`
#[wasm_bindgen_test]
async fn test_cedarling_new_from_object() {
    let bootstrap_config_json = BOOTSTRAP_CONFIG.clone();
    let conf_map_js_value = serde_wasm_bindgen::to_value(&bootstrap_config_json)
        .expect("serde json value should be converted to JsValue");

    let conf_object =
        Object::from_entries(&conf_map_js_value).expect("map value should be converted to object");

    let _instance = Cedarling::new(&conf_object)
        .await
        .expect("Cedarling::new_from_map should be initialized");
}

/// test init with object value using `init`
#[wasm_bindgen_test]
async fn test_init_conf_as_object() {
    let bootstrap_config_json = BOOTSTRAP_CONFIG.clone();
    let conf_map_js_value = serde_wasm_bindgen::to_value(&bootstrap_config_json)
        .expect("serde json value should be converted to JsValue");

    let conf_object =
        Object::from_entries(&conf_map_js_value).expect("map value should be converted to object");

    let _instance = init(conf_object.into())
        .await
        .expect("init function should be initialized with js map");
}

/// Test execution of cedarling.
/// Policy store and tokens data is used from python example.
///
/// Policies used:
///    @444da5d85403f35ea76519ed1a18a33989f855bf1cf8
///    permit(
///        principal is Jans::Workload,
///        action in [Jans::Action::"Read"],
///        resource is Jans::Application
///    )when{
///        resource.name == "Some Application"
///    };
///    
///    @840da5d85403f35ea76519ed1a18a33989f855bf1cf8
///    permit(
///        principal is Jans::User,
///        action in [Jans::Action::"Read"],
///        resource is Jans::Application
///    )when{
///        resource.name == "Some Application"
///    };
///
#[wasm_bindgen_test]
async fn test_run_cedarling() {
    let bootstrap_config_json = BOOTSTRAP_CONFIG.clone();
    let conf_map_js_value = serde_wasm_bindgen::to_value(&bootstrap_config_json)
        .expect("serde json value should be converted to JsValue");

    let conf_object =
        Object::from_entries(&conf_map_js_value).expect("map value should be converted to object");

    let instance = init(conf_object.into())
        .await
        .expect("init function should be initialized with js map");

    let request = Request {
        tokens: Tokens {
            access_token: Some(generate_token_using_claims(json!({
              "sub": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0",
              "code": "3e2a2012-099c-464f-890b-448160c2ab25",
              "iss": "https://account.gluu.org",
              "token_type": "Bearer",
              "client_id": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62",
              "aud": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62",
              "acr": "simple_password_auth",
              "x5t#S256": "",
              "nbf": 1731953030,
              "scope": [
                "role",
                "openid",
                "profile",
                "email"
              ],
              "auth_time": 1731953027,
              "exp": 1732121460,
              "iat": 1731953030,
              "jti": "uZUh1hDUQo6PFkBPnwpGzg",
              "username": "Default Admin User",
              "status": {
                "status_list": {
                  "idx": 306,
                  "uri": "https://jans.test/jans-auth/restv1/status_list"
                }
              }
            }))),
            id_token: Some(generate_token_using_claims(json!({
              "at_hash": "bxaCT0ZQXbv4sbzjSDrNiA",
              "sub": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0",
              "amr": [],
              "iss": "https://account.gluu.org",
              "nonce": "25b2b16b-32a2-42d6-8a8e-e5fa9ab888c0",
              "sid": "6d443734-b7a2-4ed8-9d3a-1606d2f99244",
              "jansOpenIDConnectVersion": "openidconnect-1.0",
              "aud": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62",
              "acr": "simple_password_auth",
              "c_hash": "V8h4sO9NzuLKawPO-3DNLA",
              "nbf": 1731953030,
              "auth_time": 1731953027,
              "exp": 1731956630,
              "grant": "authorization_code",
              "iat": 1731953030,
              "jti": "ijLZO1ooRyWrgIn7cIdNyA",
              "status": {
                "status_list": {
                  "idx": 307,
                  "uri": "https://jans.test/jans-auth/restv1/status_list"
                }
              }
            }))),
            userinfo_token: Some(generate_token_using_claims(json!({
              "sub": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0",
              "email_verified": true,
              "role": [
                "CasaAdmin"
              ],
              "iss": "https://account.gluu.org",
              "given_name": "Admin",
              "middle_name": "Admin",
              "inum": "a6a70301-af49-4901-9687-0bcdcf4e34fa",
              "client_id": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62",
              "aud": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62",
              "updated_at": 1731698135,
              "name": "Default Admin User",
              "nickname": "Admin",
              "family_name": "User",
              "jti": "OIn3g1SPSDSKAYDzENVoug",
              "email": "admin@jans.test",
              "jansAdminUIRole": [
                "api-admin"
              ]
            }))),
        },
        context: json!({
            "current_time": 1735349685, // unix time
            "device_health": ["Healthy"],
            "fraud_indicators": ["Allowed"],
            "geolocation": ["America"],
            "network": "127.0.0.1",
            "network_type": "Local",
            "operating_system": "Linux",
            "user_agent": "Linux"
        }),
        action: "Jans::Action::\"Read\"".to_string(),
        resource: ResourceData::deserialize(json!({
            "type": "Jans::Application",
            "id": "some_id",
            "app_id": "application_id",
            "name": "Some Application",
            "url": {
                "host": "jans.test",
                "path": "/protected-endpoint",
                "protocol": "http"
            }
        }))
        .expect("ResourceData should be deserialized correctly"),
    };

    let js_request =
        serde_wasm_bindgen::to_value(&request).expect("Request should be converted to JsObject");

    let result = instance
        .authorize(js_request)
        .await
        .expect("authorize request should be executed");

    assert!(result.decision, "decision should be allowed")
}

/// Test memory log interface.
/// In this scenario we check that memory log interface return some data
#[wasm_bindgen_test]
async fn test_memory_log_interface() {
    let bootstrap_config_json = json!({
        "CEDARLING_APPLICATION_NAME": "My App",
        "CEDARLING_LOCAL_POLICY_STORE": POLICY_STORE_RAW_YAML,
        "CEDARLING_LOG_TYPE": "memory",
        "CEDARLING_LOG_TTL": 120,
        "CEDARLING_LOG_LEVEL": "INFO",
        "CEDARLING_USER_AUTHZ": "enabled",
        "CEDARLING_WORKLOAD_AUTHZ": "enabled",
        "CEDARLING_USER_WORKLOAD_BOOLEAN_OPERATION": "AND",
        "CEDARLING_ID_TOKEN_TRUST_MODE": "strict",

    });

    let conf_map_js_value = serde_wasm_bindgen::to_value(&bootstrap_config_json)
        .expect("serde json value should be converted to JsValue");

    let conf_object =
        Object::from_entries(&conf_map_js_value).expect("map value should be converted to object");

    let instance = init(conf_object.into())
        .await
        .expect("init function should be initialized with js map");

    let request = Request {
        tokens: Tokens {
            access_token: Some(generate_token_using_claims(json!({
              "sub": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0",
              "code": "3e2a2012-099c-464f-890b-448160c2ab25",
              "iss": "https://account.gluu.org",
              "token_type": "Bearer",
              "client_id": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62",
              "aud": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62",
              "acr": "simple_password_auth",
              "x5t#S256": "",
              "nbf": 1731953030,
              "scope": [
                "role",
                "openid",
                "profile",
                "email"
              ],
              "auth_time": 1731953027,
              "exp": 1732121460,
              "iat": 1731953030,
              "jti": "uZUh1hDUQo6PFkBPnwpGzg",
              "username": "Default Admin User",
              "status": {
                "status_list": {
                  "idx": 306,
                  "uri": "https://jans.test/jans-auth/restv1/status_list"
                }
              }
            }))),
            id_token: Some(generate_token_using_claims(json!({
              "at_hash": "bxaCT0ZQXbv4sbzjSDrNiA",
              "sub": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0",
              "amr": [],
              "iss": "https://account.gluu.org",
              "nonce": "25b2b16b-32a2-42d6-8a8e-e5fa9ab888c0",
              "sid": "6d443734-b7a2-4ed8-9d3a-1606d2f99244",
              "jansOpenIDConnectVersion": "openidconnect-1.0",
              "aud": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62",
              "acr": "simple_password_auth",
              "c_hash": "V8h4sO9NzuLKawPO-3DNLA",
              "nbf": 1731953030,
              "auth_time": 1731953027,
              "exp": 1731956630,
              "grant": "authorization_code",
              "iat": 1731953030,
              "jti": "ijLZO1ooRyWrgIn7cIdNyA",
              "status": {
                "status_list": {
                  "idx": 307,
                  "uri": "https://jans.test/jans-auth/restv1/status_list"
                }
              }
            }))),
            userinfo_token: Some(generate_token_using_claims(json!({
              "sub": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0",
              "email_verified": true,
              "role": [
                "CasaAdmin"
              ],
              "iss": "https://account.gluu.org",
              "given_name": "Admin",
              "middle_name": "Admin",
              "inum": "a6a70301-af49-4901-9687-0bcdcf4e34fa",
              "client_id": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62",
              "aud": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62",
              "updated_at": 1731698135,
              "name": "Default Admin User",
              "nickname": "Admin",
              "family_name": "User",
              "jti": "OIn3g1SPSDSKAYDzENVoug",
              "email": "admin@jans.test",
              "jansAdminUIRole": [
                "api-admin"
              ]
            }))),
        },
        context: json!({
            "current_time": 1735349685, // unix time
            "device_health": ["Healthy"],
            "fraud_indicators": ["Allowed"],
            "geolocation": ["America"],
            "network": "127.0.0.1",
            "network_type": "Local",
            "operating_system": "Linux",
            "user_agent": "Linux"
        }),
        action: "Jans::Action::\"Read\"".to_string(),
        resource: ResourceData::deserialize(json!({
            "type": "Jans::Application",
            "id": "some_id",
            "app_id": "application_id",
            "name": "Some Application",
            "url": {
                "host": "jans.test",
                "path": "/protected-endpoint",
                "protocol": "http"
            }
        }))
        .expect("ResourceData should be deserialized correctly"),
    };

    let js_request =
        serde_wasm_bindgen::to_value(&request).expect("Request should be converted to JsObject");

    let _result = instance
        .authorize(js_request)
        .await
        .expect("authorize request should be executed");

    let js_log_ids = instance.get_log_ids();
    let logs_count = js_log_ids.length();

    for js_log_id in js_log_ids {
        let log_id_str = js_log_id.as_string().expect("js_log_id should be string");

        let log_val = instance
            .get_log_by_id(log_id_str.as_str())
            .expect("get_log_by_id should not throw error");

        assert_ne!(log_val, JsValue::NULL, "log result should be not null")
    }

    let pop_logs_result = instance.pop_logs().expect("pop_logs not throw error");
    assert_eq!(
        logs_count,
        pop_logs_result.length(),
        "length of ids and logs should be the same"
    );

    let pop_logs_result2 = instance.pop_logs().expect("pop_logs not throw error");
    assert_eq!(
        pop_logs_result2.length(),
        0,
        "logs should be removed from storage, storage should be empty"
    );
}
