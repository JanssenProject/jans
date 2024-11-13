/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::utils::*;

/// Test scenario with load agama policy store
#[test]
fn load_agama_policy_store_json() {
    let agama_policy_store = serde_json::json!({
        "cedar_version": "v4.0.0",
        "policy_stores": {
            "fbcd361d3243f4808d5e6f89ca81eaf4ca03900b407e": {
                "name": "JansTest",
                "policies": {
                    "840da5d85403f35ea76519ed1a18a33989f855bf1cf8": {
                        "cedar_version": "v4.0.0",
                        "description": "simple policy example for principal workload",
                        "creation_date": "2024-09-20T17:22:39.996050",
                        "policy_content": "cGVybWl0KAogICAgcHJpbmNpcGFsIGlzIEphbnM6Oldvcmtsb2FkLAogICAgYWN0aW9uIGluIFtKYW5zOjpBY3Rpb246OiJVcGRhdGUiXSwKICAgIHJlc291cmNlIGlzIEphbnM6Oklzc3VlCil3aGVuewogICAgcHJpbmNpcGFsLm9yZ19pZCA9PSByZXNvdXJjZS5vcmdfaWQKfTs="
                    },
                    "444da5d85403f35ea76519ed1a18a33989f855bf1cf8": {
                        "cedar_version": "v4.0.0",
                        "description": "simple policy example for principal user",
                        "creation_date": "2024-09-20T17:22:39.996050",
                        "policy_content": "cGVybWl0KAogICAgcHJpbmNpcGFsIGlzIEphbnM6OlVzZXIsCiAgICBhY3Rpb24gaW4gW0phbnM6OkFjdGlvbjo6IlVwZGF0ZSJdLAogICAgcmVzb3VyY2UgaXMgSmFuczo6SXNzdWUKKXdoZW57CiAgICBwcmluY2lwYWwuY291bnRyeSA9PSByZXNvdXJjZS5jb3VudHJ5Cn07"
                    }
                },
                "schema": "eyJKYW5zIjp7ImNvbW1vblR5cGVzIjp7IlVybCI6eyJ0eXBlIjoiUmVjb3JkIiwiYXR0cmlidXRlcyI6eyJob3N0Ijp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifSwicGF0aCI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIn0sInByb3RvY29sIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifX19fSwiZW50aXR5VHlwZXMiOnsiVHJ1c3RlZElzc3VlciI6eyJzaGFwZSI6eyJ0eXBlIjoiUmVjb3JkIiwiYXR0cmlidXRlcyI6eyJpc3N1ZXJfZW50aXR5X2lkIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJVcmwifX19fSwiV29ya2xvYWQiOnsic2hhcGUiOnsidHlwZSI6IlJlY29yZCIsImF0dHJpYnV0ZXMiOnsiY2xpZW50X2lkIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifSwiaXNzIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJUcnVzdGVkSXNzdWVyIn0sIm5hbWUiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9LCJvcmdfaWQiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9fX19LCJVc2VyIjp7Im1lbWJlck9mVHlwZXMiOlsiUm9sZSJdLCJzaGFwZSI6eyJ0eXBlIjoiUmVjb3JkIiwiYXR0cmlidXRlcyI6eyJjb3VudHJ5Ijp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifSwiZW1haWwiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9LCJzdWIiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9LCJ1c2VybmFtZSI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiU3RyaW5nIn19fX0sIkFjY2Vzc190b2tlbiI6eyJzaGFwZSI6eyJ0eXBlIjoiUmVjb3JkIiwiYXR0cmlidXRlcyI6eyJhdWQiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9LCJleHAiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IkxvbmcifSwiaWF0Ijp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJMb25nIn0sImlzcyI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiVHJ1c3RlZElzc3VlciJ9LCJqdGkiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9fX19LCJpZF90b2tlbiI6eyJzaGFwZSI6eyJ0eXBlIjoiUmVjb3JkIiwiYXR0cmlidXRlcyI6eyJhY3IiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9LCJhbXIiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9LCJhdWQiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9LCJleHAiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IkxvbmcifSwiaWF0Ijp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJMb25nIn0sImlzcyI6eyJ0eXBlIjoiRW50aXR5T3JDb21tb24iLCJuYW1lIjoiVHJ1c3RlZElzc3VlciJ9LCJqdGkiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9LCJzdWIiOnsidHlwZSI6IkVudGl0eU9yQ29tbW9uIiwibmFtZSI6IlN0cmluZyJ9fX19LCJJc3N1ZSI6eyJzaGFwZSI6eyJ0eXBlIjoiUmVjb3JkIiwiYXR0cmlidXRlcyI6eyJjb3VudHJ5Ijp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifSwib3JnX2lkIjp7InR5cGUiOiJFbnRpdHlPckNvbW1vbiIsIm5hbWUiOiJTdHJpbmcifX19fSwiUm9sZSI6e319LCJhY3Rpb25zIjp7IlVwZGF0ZSI6eyJhcHBsaWVzVG8iOnsicmVzb3VyY2VUeXBlcyI6WyJJc3N1ZSJdLCJwcmluY2lwYWxUeXBlcyI6WyJXb3JrbG9hZCIsIlVzZXIiLCJSb2xlIl19fX19fQ==",
                "identity_source": {}
            }
        }
    });

    let cedarling = get_cedarling(PolicyStoreSource::Json(agama_policy_store.to_string()));

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
