/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use cedarling::{
    AuthorizationConfig, BootstrapConfig, Cedarling, JwtConfig, LogConfig, LogTypeConfig,
    NewJwtConfig, PolicyStoreConfig, PolicyStoreSource, Request, ResourceData, WorkloadBoolOp,
};
use std::collections::HashMap;

static POLICY_STORE_RAW: &str = include_str!("../../test_files/policy-store_ok.yaml");

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let cedarling = Cedarling::new(BootstrapConfig {
        application_name: "test_app".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::StdOut,
        },
        policy_store_config: PolicyStoreConfig {
            source: PolicyStoreSource::Yaml(POLICY_STORE_RAW.to_string()),
        },
        jwt_config: JwtConfig::Disabled,
        new_jwt_config: NewJwtConfig::new_without_validation(),
        authorization_config: AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: true,
            user_workload_operator: WorkloadBoolOp::And,
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
    //   "jti": "lxTmCVRFTxOjJgvEEpozMQ",
    //   "name": "Default Admin User",
    //   "status": {
    //     "status_list": {
    //       "idx": 201,
    //       "uri": "https://admin-ui-test.gluu.org/jans-auth/restv1/status_list"
    //     }
    //   }
    // }
    let access_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY29kZSI6ImJmMTkzNGY2LTM5MDUtNDIwYS04Mjk5LTZiMmUzZmZkZGQ2ZSIsImlzcyI6Imh0dHBzOi8vYWRtaW4tdWktdGVzdC5nbHV1Lm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhdWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhY3IiOiJiYXNpYyIsIng1dCNTMjU2IjoiIiwic2NvcGUiOlsib3BlbmlkIiwicHJvZmlsZSJdLCJvcmdfaWQiOiJzb21lX2xvbmdfaWQiLCJhdXRoX3RpbWUiOjE3MjQ4MzA3NDYsImV4cCI6MTcyNDk0NTk3OCwiaWF0IjoxNzI0ODMyMjU5LCJqdGkiOiJseFRtQ1ZSRlR4T2pKZ3ZFRXBvek1RIiwibmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsInN0YXR1cyI6eyJzdGF0dXNfbGlzdCI6eyJpZHgiOjIwMSwidXJpIjoiaHR0cHM6Ly9hZG1pbi11aS10ZXN0LmdsdXUub3JnL2phbnMtYXV0aC9yZXN0djEvc3RhdHVzX2xpc3QifX19._eQT-DsfE_kgdhA0YOyFxxPEMNw44iwoelWa5iU1n9s".to_string();

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
    //   "country": "usa",
    //   "x5t#S256": "",
    //   "scope": [
    //     "openid",
    //     "profile"
    //   ],
    //   "org_id": "some_long_id",
    //   "auth_time": 1724830746,
    //   "exp": 1724945978, -> Aug 29, 2024 23:39:38 GMT+0800
    //   "iat": 1724832259, -> Aug 28, 2024 16:0419 GMT+0800
    //   "jti": "lxTmCVRFTxOjJgvEEpozMQ",
    //   "name": "Default Admin User",
    //   "status": {
    //     "status_list": {
    //       "idx": 201,
    //       "uri": "https://admin-ui-test.gluu.org/jans-auth/restv1/status_list"
    //     }
    //   }
    // }
    let id_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY29kZSI6ImJmMTkzNGY2LTM5MDUtNDIwYS04Mjk5LTZiMmUzZmZkZGQ2ZSIsImlzcyI6Imh0dHBzOi8vYWRtaW4tdWktdGVzdC5nbHV1Lm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhdWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhY3IiOiJiYXNpYyIsImFtciI6InB3ZCIsInVzZXJuYW1lIjoiYWRtaW5AZ2x1dS5vcmciLCJjb3VudHJ5IjoidXNhIiwieDV0I1MyNTYiOiIiLCJzY29wZSI6WyJvcGVuaWQiLCJwcm9maWxlIl0sIm9yZ19pZCI6InNvbWVfbG9uZ19pZCIsImF1dGhfdGltZSI6MTcyNDgzMDc0NiwiZXhwIjoxNzI0OTQ1OTc4LCJpYXQiOjE3MjQ4MzIyNTksImp0aSI6Imx4VG1DVlJGVHhPakpndkVFcG96TVEiLCJuYW1lIjoiRGVmYXVsdCBBZG1pbiBVc2VyIiwic3RhdHVzIjp7InN0YXR1c19saXN0Ijp7ImlkeCI6MjAxLCJ1cmkiOiJodHRwczovL2FkbWluLXVpLXRlc3QuZ2x1dS5vcmcvamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdCJ9fX0.i492ZEtxVSK1wwy0YUHnPGKDNSTlJJymhW71ShqBJD0".to_string();

    // userinfo_token claims:
    // {
    //   "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
    //   "client_id": "5b4487c4-8db1-409d-a653-f907b8094039",
    //   "aud": "5b4487c4-8db1-409d-a653-f907b8094039",
    //   "name": "Default Admin User",
    //   "email": "admin@gluu.org"
    // }
    let userinfo_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY2xpZW50X2lkIjoiNWI0NDg3YzQtOGRiMS00MDlkLWE2NTMtZjkwN2I4MDk0MDM5IiwiYXVkIjoiNWI0NDg3YzQtOGRiMS00MDlkLWE2NTMtZjkwN2I4MDk0MDM5IiwibmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsImVtYWlsIjoiYWRtaW5AZ2x1dS5vcmcifQ.MNebnjubvPtn9eq5j4RvWOTw7NBkjqt2Z8hTyFSJz0w".to_string();

    // TODO: make access_token, id_token, and userinfo_token optional since they are not needed
    // when validation is off
    let result = cedarling.authorize(Request {
        access_token,
        id_token,
        userinfo_token,
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
