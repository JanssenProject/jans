/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::utils::*;

/// Test success scenario wiht authorization
//  test duplicate code of example file `authorize.rs` (authorization without JWT validation)
#[test]
fn success_test_json() {
    // The human-readable policy and schema file is located in next folder:
    // `test_files\policy-store_ok`
    // Is used to check that the JSON policy is loaded correctly
    static POLICY_STORE_RAW_JSON: &str = include_str!("../../../test_files/policy-store_ok.json");

    let cedarling = get_cedarling(PolicyStoreSource::Json(POLICY_STORE_RAW_JSON.to_string()));

    // deserialize `Request` from json
    let request = Request::deserialize(serde_json::json!(
        {
            "access_token": generate_token_using_claims(json!({
                    "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
                    "code": "bf1934f6-3905-420a-8299-6b2e3ffddd6e",
                    "iss": "https://admin-ui-test.gluu.org",
                    "token_type": "Bearer",
                    "client_id": "5b4487c4-8db1-409d-a653-f907b8094039",
                    "aud": "5b4487c4-8db1-409d-a653-f907b8094039",
                    "acr": "basic",
                    "x5t#S256": "",
                    "scope": [
                      "openid",
                      "profile"
                    ],
                    "org_id": "some_long_id",
                    "auth_time": 1724830746,
                    "exp": 1724945978,
                    "iat": 1724832259,
                    "jti": "lxTmCVRFTxOjJgvEEpozMQ",
                    "name": "Default Admin User",
                    "status": {
                      "status_list": {
                        "idx": 201,
                        "uri": "https://admin-ui-test.gluu.org/jans-auth/restv1/status_list"
                      }
                    }
                  })),
            "id_token": generate_token_using_claims(json!({
                    "acr": "basic",
                    "amr": "10",
                    "aud": "5b4487c4-8db1-409d-a653-f907b8094039",
                    "exp": 1724835859,
                    "iat": 1724832259,
                    "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
                    "iss": "https://admin-ui-test.gluu.org",
                    "jti": "sk3T40NYSYuk5saHZNpkZw",
                    "nonce": "c3872af9-a0f5-4c3f-a1af-f9d0e8846e81",
                    "sid": "6a7fe50a-d810-454d-be5d-549d29595a09",
                    "jansOpenIDConnectVersion": "openidconnect-1.0",
                    "c_hash": "pGoK6Y_RKcWHkUecM9uw6Q",
                    "auth_time": 1724830746,
                    "grant": "authorization_code",
                    "status": {
                      "status_list": {
                        "idx": 202,
                        "uri": "https://admin-ui-test.gluu.org/jans-auth/restv1/status_list"
                      }
                    },
                    "role":"Admin"
                  })),
            "userinfo_token":  generate_token_using_claims(json!({
                    "country": "US",
                    "email": "user@example.com",
                    "username": "UserNameExample",
                    "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
                    "iss": "https://admin-ui-test.gluu.org",
                    "given_name": "Admin",
                    "middle_name": "Admin",
                    "inum": "8d1cde6a-1447-4766-b3c8-16663e13b458",
                    "client_id": "5b4487c4-8db1-409d-a653-f907b8094039",
                    "aud": "5b4487c4-8db1-409d-a653-f907b8094039",
                    "updated_at": 1724778591,
                    "name": "Default Admin User",
                    "nickname": "Admin",
                    "family_name": "User",
                    "jti": "faiYvaYIT0cDAT7Fow0pQw",
                    "jansAdminUIRole": [
                        "api-admin"
                    ],
                    "exp": 1724945978
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

    assert!(result.is_allowed(), "request result should be allowed");
}
