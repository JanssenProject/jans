/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use cedarling::{
    AuthorizationConfig, BootstrapConfig, Cedarling, JwtConfig, LogConfig, LogTypeConfig,
    PolicyStoreConfig, PolicyStoreSource, Request, ResourceData, WorkloadBoolOp,
};
use std::collections::HashMap;

static POLICY_STORE_RAW: &str = include_str!("../../test_files/policy-store_ok.yaml");

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let cedarling = Cedarling::new(&BootstrapConfig {
        application_name: "test_app".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::StdOut,
        },
        policy_store_config: PolicyStoreConfig {
            source: PolicyStoreSource::Yaml(POLICY_STORE_RAW.to_string()),
        },
        jwt_config: JwtConfig::new_without_validation(),
        authorization_config: AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: true,
            user_workload_operator: WorkloadBoolOp::And,
            decision_log_default_jwt_id: "jti".to_string(),
            decision_log_user_claims: vec!["client_id".to_string(), "username".to_string()],
            decision_log_workload_claims: vec!["org_id".to_string()],
            ..Default::default()
        },
    })?;

    // the following tokens are expired
    // access_token claims:
    // {
    //   "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
    //   "code": "bf1934f6-3905-420a-8299-6b2e3ffddd6e",
    //   "iss": "https://admin-ui-test.gluu.org",
    //   "token_type": "Bearer",
    //   "client_id": "5b4487c4-8db1-409d-a653-f907b8094039",
    //   "aud": "5b4487c4-8db1-409d-a653-f907b8094039",
    //   "acr": "basic",
    //   "x5t#S256": "",
    //   "scope": [
    //     "openid",
    //     "profile"
    //   ],
    //   "org_id": "some_long_id",
    //   "auth_time": 1724830746,
    //   "exp": 1724945978, -> Aug 29, 2024 23:39:38 GMT+0800
    //   "iat": 1724832259, -> Aug 28, 2024 16:0419 GMT+0800
    //   "jti": "access_tkn_jti",
    //   "username": "admin@gluu.org",
    //   "name": "Default Admin User",
    //   "status": {
    //     "status_list": {
    //       "idx": 201,
    //       "uri": "https://admin-ui-test.gluu.org/jans-auth/restv1/status_list"
    //     }
    //   }
    // }
    let access_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY29kZSI6ImJmMTkzNGY2LTM5MDUtNDIwYS04Mjk5LTZiMmUzZmZkZGQ2ZSIsImlzcyI6Imh0dHBzOi8vYWRtaW4tdWktdGVzdC5nbHV1Lm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhdWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhY3IiOiJiYXNpYyIsIng1dCNTMjU2IjoiIiwic2NvcGUiOlsib3BlbmlkIiwicHJvZmlsZSJdLCJvcmdfaWQiOiJzb21lX2xvbmdfaWQiLCJhdXRoX3RpbWUiOjE3MjQ4MzA3NDYsImV4cCI6MTcyNDk0NTk3OCwiaWF0IjoxNzI0ODMyMjU5LCJqdGkiOiJhY2Nlc3NfdGtuX2p0aSIsIm5hbWUiOiJEZWZhdWx0IEFkbWluIFVzZXIiLCJzdGF0dXMiOnsic3RhdHVzX2xpc3QiOnsiaWR4IjoyMDEsInVyaSI6Imh0dHBzOi8vYWRtaW4tdWktdGVzdC5nbHV1Lm9yZy9qYW5zLWF1dGgvcmVzdHYxL3N0YXR1c19saXN0In19fQ.D6q28qP-rZ3LayPsVlvUzXCwHtl7g3VTntMQvG_f3mM".to_string();

    // id_token claims:
    // {
    //   "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
    //   "code": "bf1934f6-3905-420a-8299-6b2e3ffddd6e",
    //   "iss": "https://admin-ui-test.gluu.org",
    //   "token_type": "Bearer",
    //   "client_id": "5b4487c4-8db1-409d-a653-f907b8094039",
    //   "aud": "5b4487c4-8db1-409d-a653-f907b8094039",
    //   "acr": "basic",
    //   "amr": "pwd",
    //   "username": "admin@gluu.org",
    //   "country": "US",
    //   "x5t#S256": "",
    //   "scope": [
    //     "openid",
    //     "profile"
    //   ],
    //   "org_id": "some_long_id",
    //   "auth_time": 1724830746,
    //   "exp": 1724945978, -> Aug 29, 2024 23:39:38 GMT+0800
    //   "iat": 1724832259, -> Aug 28, 2024 16:0419 GMT+0800
    //   "jti": "id_tkn_jti",
    //   "name": "Default Admin User",
    //   "status": {
    //     "status_list": {
    //       "idx": 201,
    //       "uri": "https://admin-ui-test.gluu.org/jans-auth/restv1/status_list"
    //     }
    //   }
    // }
    let id_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY29kZSI6ImJmMTkzNGY2LTM5MDUtNDIwYS04Mjk5LTZiMmUzZmZkZGQ2ZSIsImlzcyI6Imh0dHBzOi8vYWRtaW4tdWktdGVzdC5nbHV1Lm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhdWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhY3IiOiJiYXNpYyIsImFtciI6InB3ZCIsInVzZXJuYW1lIjoiYWRtaW5AZ2x1dS5vcmciLCJjb3VudHJ5IjoidXNhIiwieDV0I1MyNTYiOiIiLCJzY29wZSI6WyJvcGVuaWQiLCJwcm9maWxlIl0sIm9yZ19pZCI6InNvbWVfbG9uZ19pZCIsImF1dGhfdGltZSI6MTcyNDgzMDc0NiwiZXhwIjoxNzI0OTQ1OTc4LCJpYXQiOjE3MjQ4MzIyNTksImp0aSI6ImlkX3Rrbl9qdGkiLCJuYW1lIjoiRGVmYXVsdCBBZG1pbiBVc2VyIiwic3RhdHVzIjp7InN0YXR1c19saXN0Ijp7ImlkeCI6MjAxLCJ1cmkiOiJodHRwczovL2FkbWluLXVpLXRlc3QuZ2x1dS5vcmcvamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdCJ9fX0.xVRNRN7RW3Y2n4bzW0k93zbe5Tn0htQS6JiVq9NP0NE".to_string();

    // userinfo_token claims:
    // {
    //   "iss": "https://admin-ui-test.gluu.org",
    //   "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
    //   "client_id": "5b4487c4-8db1-409d-a653-f907b8094039",
    //   "aud": "5b4487c4-8db1-409d-a653-f907b8094039",
    //   "ueyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL2FkbWluLXVpLXRlc3QuZ2x1dS5vcmciLCJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY2xpZW50X2lkIjoiNWI0NDg3YzQtOGRiMS00MDlkLWE2NTMtZjkwN2I4MDk0MDM5IiwiYXVkIjoiNWI0NDg3YzQtOGRiMS00MDlkLWE2NTMtZjkwN2I4MDk0MDM5IiwibmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsImVtYWlsIjoiYWRtaW5AZ2x1dS5vcmciLCJjb3VudHJ5IjoiVVMiLCJqdGkiOiJ1c3JpbmZvX3Rrbl9qdGkifQ.sUrKttbzHktPYtSelizTJZJr_8mqkVBpStA5fjNJU9ksername": "admin@gluu.org",
    //   "name": "Default Admin User",
    //   "email": "admin@gluu.org",
    //   "country": "US",
    //   "jti": "usrinfo_tkn_jti"
    // }
    let userinfo_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL2FkbWluLXVpLXRlc3QuZ2x1dS5vcmciLCJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY2xpZW50X2lkIjoiNWI0NDg3YzQtOGRiMS00MDlkLWE2NTMtZjkwN2I4MDk0MDM5IiwiYXVkIjoiNWI0NDg3YzQtOGRiMS00MDlkLWE2NTMtZjkwN2I4MDk0MDM5IiwidXNlcm5hbWUiOiJhZG1pbkBnbHV1Lm9yZyIsIm5hbWUiOiJEZWZhdWx0IEFkbWluIFVzZXIiLCJlbWFpbCI6ImFkbWluQGdsdXUub3JnIiwiY291bnRyeSI6IlVTIiwianRpIjoidXNyaW5mb190a25fanRpIn0.NoR53vPZFpfb4vFk85JH9RPx7CHsaJMZwrH3fnB-N60".to_string();

    let result = cedarling.authorize(Request {
        access_token: Some(access_token),
        id_token: Some(id_token),
        userinfo_token: Some(userinfo_token),
        action: "Jans::Action::\"Update\"".to_string(),
        context: serde_json::json!({}),
        resource: ResourceData {
            id: "random_id".to_string(),
            resource_type: "Jans::Issue".to_string(),
            payload: HashMap::from_iter([
                (
                    "org_id".to_string(),
                    serde_json::Value::String("some_long_id".to_string()),
                ),
                (
                    "country".to_string(),
                    serde_json::Value::String("US".to_string()),
                ),
            ]),
        },
    });

    match result {
        Ok(result) => {
            println!("\n\nis allowed: {}", result.is_allowed());
        },
        Err(e) => eprintln!("Error while authorizing: {}\n {:?}\n\n", e, e),
    }

    Ok(())
}
