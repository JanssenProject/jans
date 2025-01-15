// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use test_utils::{SortedJson, assert_eq};
use tokio::test;

use super::utils::*;

static POLICY_STORE_RAW_YAML: &str = include_str!("../../../test_files/agama-store_2.yaml");

/// Test loading policy store with mappings JWT payload to custom `cedar-entities` types in schema
#[test]
async fn check_mapping_tokens_data() {
    let cedarling = get_cedarling(PolicyStoreSource::Yaml(POLICY_STORE_RAW_YAML.to_string())).await;

    // deserialize `Request` from json
    // JWT tokens payload from using `tarp` with `https://test-casa.gluu.info/.well-known/openid-configuration`
    let request = Request::deserialize(serde_json::json!(
        {
            "tokens": {
                "access_token": generate_token_using_claims(json!({
                    "sub": "J3BmtnPPB8BjMbScWmR8cjT9gWCCTHKfSf0dkbOvhGg",
                    "code": "697da80d-16ad-41f8-ad8e-c71f881c5473",
                    "iss": "https://test-casa.gluu.info",
                    "token_type": "Bearer",
                    "client_id": "95bd63d2-85ed-40ad-bd03-3c18af797ca4",
                    "aud": "95bd63d2-85ed-40ad-bd03-3c18af797ca4",
                    "acr": "simple_password_auth",
                    "x5t#S256": "",
                    "nbf": 1730494543,
                    "scope": [
                        "role",
                        "openid"
                    ],
                    "auth_time": 1730494542,
                    "exp": 1730574245,
                    "iat": 1730494543,
                    "jti": "qpCu52Z0S8ynfZ7ufCXQow",
                    "username": "John Smith",
                    "status": {
                        "status_list": {
                            "idx": 1003,
                            "uri": "https://test-casa.gluu.info/jans-auth/restv1/status_list"
                        }
                    }
                })),
                "id_token": generate_token_using_claims(json!({
                    "at_hash": "zajL-IEPbJ7XprbAgi5LAg",
                    "sub": "J3BmtnPPB8BjMbScWmR8cjT9gWCCTHKfSf0dkbOvhGg",
                    "amr": [],
                    "iss": "https://test-casa.gluu.info",
                    "nonce": "b9b6df51-a04a-475a-9141-3fe589c2aab8",
                    "sid": "71eedb3a-7c18-420c-9fea-37d6532990e6",
                    "jansOpenIDConnectVersion": "openidconnect-1.0",
                    "aud": "95bd63d2-85ed-40ad-bd03-3c18af797ca4",
                    "acr": "simple_password_auth",
                    "c_hash": "pQi9rYqmSCVc3tK--2AgiA",
                    "nbf": 1730494543,
                    "auth_time": 1730494542,
                    "exp": 1730498143,
                    "grant": "authorization_code",
                    "iat": 1730494543,
                    "jti": "v2SWGfAEQGWZ1mPDSJPvbg",
                    "status": {
                        "status_list": {
                            "idx": 1004,
                            "uri": "https://test-casa.gluu.info/jans-auth/restv1/status_list"
                        }
                    }
                })),
                "userinfo_token":  generate_token_using_claims(json!({
                    "sub": "J3BmtnPPB8BjMbScWmR8cjT9gWCCTHKfSf0dkbOvhGg",
                    "aud": "95bd63d2-85ed-40ad-bd03-3c18af797ca4",
                    "role": [
                        "Manager",
                        "Support"
                    ],
                    "iss": "https://test-casa.gluu.info",
                    "jti": "qOxklMYfSfqdYgXl01j9wA",
                    "client_id": "95bd63d2-85ed-40ad-bd03-3c18af797ca4",
                    "email":"user@example.com",
                })),
            },
            "action": "Test::Action::\"Search\"",
            "resource": {
                "id": "SomeID",
                "type": "Test::Application",
                "app_id":"1234",
                "name": "some_app",
                "url":{
                    "host": "test-casa.gluu.info",
                    "path":"/",
                    "protocol":"https",
                }
            },
            "context": {
                "current_time":1731812031,
                "device_health": [],
                "fraud_indicators": [],
                "geolocation": [],
                "network": "vpn",
                "network_type": "vpn",
                "operating_system": "linux",
                "user_agent": "Smith",
            },
        }
    ))
    .expect("Request should be deserialized from json");

    let entities = cedarling
        .authorize_entities_data(&request)
        .await
        // log err to be human readable
        .inspect_err(|err| println!("Error: {}", err.to_string()))
        .expect("request should be parsed without errors");

    // check value of resource entity
    let expected_resource = json!({"uid":{"type":"Test::Application","id":"SomeID"},"attrs":{"url":{"host":"test-casa.gluu.info","path":"/","protocol":"https"},"app_id":"1234","name":"some_app"},"parents":[]}).sorted();
    assert_eq!(
        expected_resource,
        entities.resource.to_json_value().unwrap().sorted(),
        "derived resource_entity is not equal to the expected"
    );

    // check value of user entity
    // managed mapping of email
    let expected_user = json!({"attrs":{"email":{"domain":"example.com", "uid":"user"},"role":["Manager","Support"],"sub":"J3BmtnPPB8BjMbScWmR8cjT9gWCCTHKfSf0dkbOvhGg"},"parents":[{"id":"Manager","type":"Test::Role"},{"id":"Support","type":"Test::Role"}],"uid":{"id":"J3BmtnPPB8BjMbScWmR8cjT9gWCCTHKfSf0dkbOvhGg","type":"Test::User"}}).sorted();
    assert_eq!(
        expected_user,
        entities.user.unwrap().to_json_value().unwrap().sorted(),
        "derived user_entity is not equal to the expected"
    );
}
