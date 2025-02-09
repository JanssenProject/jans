// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

// allow dead code to avoid highlight test functions (by linter)
#![allow(dead_code)]

use crate::Cedarling;
use std::collections::HashMap;
use test_utils::token_claims::generate_token_using_claims;
use serde_json::json;

#[test]
fn test_authorize_success() {
    //reading bootstra.json and instantiate cedarling
    let cedarling = Cedarling::load_from_file(String::from(
        "../../bindings/cedarling_native_apps/test_files/bootstrap.json",
    ))
    .expect("Error in initializing Cedarling");

    //execute authz
    let result = cedarling.authorize(
        HashMap::from([
            (
                "access_token".to_string(),
                generate_token_using_claims(json!({
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
                  })),
            ),
            (
                "id_token".to_string(),
                generate_token_using_claims(json!({
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
                  })),
            ),
            (
                "userinfo_token".to_string(),
                generate_token_using_claims(json!({
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
                    ],
                    "user_name": "admin",
                    "exp": 1724945978,
                    "iat": 1724832259,
                    "acr": "pass",
                    "amr": [
                      "pass"
                    ]
                  })),
            ),
        ]),
        r#"Jans::Action::"Update""#.to_string(),
        "Jans::Issue".to_string(),
        "some_id".to_string(),
        r#"
                {
                    "app_id": "admin_ui_id",
                        "id": "admin_ui_id",
                        "name": "My App",
                        "permission": "view_clients",
                        "sub": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0",
                        "type": "Jans::Issue"
                }
            "#
        .to_string(),
        "{}".to_string(),
    );

    assert!(result.is_ok());
}
