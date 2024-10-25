/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use cedarling::{
    BootstrapConfig, Cedarling, JwtConfig, LogConfig, LogTypeConfig, PolicyStoreConfig,
    PolicyStoreSource, Request, ResourceData,
};
use std::collections::HashMap;

// The human-readable policy and schema file is located in next folder:
// `test_files\policy-store_ok`
static POLICY_STORE_RAW: &str = include_str!("../../test_files/policy-store_ok.json");

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let cedarling = Cedarling::new(BootstrapConfig {
        application_name: "test_app".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::StdOut,
        },
        policy_store_config: PolicyStoreConfig {
            source: PolicyStoreSource::Json(POLICY_STORE_RAW.to_string()),
            store_id: None,
        },
        jwt_config: JwtConfig::Disabled,
    })?;

    // JSON payload of access token
    //   {
    //     "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
    //     "code": "bf1934f6-3905-420a-8299-6b2e3ffddd6e",
    //     "iss": "https://admin-ui-test.gluu.org",
    //     "token_type": "Bearer",
    //     "client_id": "5b4487c4-8db1-409d-a653-f907b8094039",
    //     "aud": "5b4487c4-8db1-409d-a653-f907b8094039",
    //     "acr": "basic",
    //     "x5t#S256": "",
    //     "scope": [
    //       "openid",
    //       "profile"
    //     ],
    //     "org_id": "some_long_id",
    //     "auth_time": 1724830746,
    //     "exp": 1724945978,
    //     "iat": 1724832259,
    //     "jti": "lxTmCVRFTxOjJgvEEpozMQ",
    //     "name": "Default Admin User",
    //     "status": {
    //       "status_list": {
    //         "idx": 201,
    //         "uri": "https://admin-ui-test.gluu.org/jans-auth/restv1/status_list"
    //       }
    //     }
    //   }
    let access_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY29kZSI6ImJmMTkzNGY2LTM5MDUtNDIwYS04Mjk5LTZiMmUzZmZkZGQ2ZSIsImlzcyI6Imh0dHBzOi8vYWRtaW4tdWktdGVzdC5nbHV1Lm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhdWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhY3IiOiJiYXNpYyIsIng1dCNTMjU2IjoiIiwic2NvcGUiOlsib3BlbmlkIiwicHJvZmlsZSJdLCJvcmdfaWQiOiJzb21lX2xvbmdfaWQiLCJhdXRoX3RpbWUiOjE3MjQ4MzA3NDYsImV4cCI6MTcyNDk0NTk3OCwiaWF0IjoxNzI0ODMyMjU5LCJqdGkiOiJseFRtQ1ZSRlR4T2pKZ3ZFRXBvek1RIiwibmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsInN0YXR1cyI6eyJzdGF0dXNfbGlzdCI6eyJpZHgiOjIwMSwidXJpIjoiaHR0cHM6Ly9hZG1pbi11aS10ZXN0LmdsdXUub3JnL2phbnMtYXV0aC9yZXN0djEvc3RhdHVzX2xpc3QifX19._eQT-DsfE_kgdhA0YOyFxxPEMNw44iwoelWa5iU1n9s".to_string();

    // JSON payload of id token
    //   {
    //     "acr": "basic",
    //     "amr": "10",
    //     "aud": "5b4487c4-8db1-409d-a653-f907b8094039",
    //     "exp": 1724835859,
    //     "iat": 1724832259,
    //     "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
    //     "iss": "https://admin-ui-test.gluu.org",
    //     "jti": "sk3T40NYSYuk5saHZNpkZw",
    //     "nonce": "c3872af9-a0f5-4c3f-a1af-f9d0e8846e81",
    //     "sid": "6a7fe50a-d810-454d-be5d-549d29595a09",
    //     "jansOpenIDConnectVersion": "openidconnect-1.0",
    //     "c_hash": "pGoK6Y_RKcWHkUecM9uw6Q",
    //     "auth_time": 1724830746,
    //     "grant": "authorization_code",
    //     "status": {
    //       "status_list": {
    //         "idx": 202,
    //         "uri": "https://admin-ui-test.gluu.org/jans-auth/restv1/status_list"
    //       }
    //     }
    //   }
    let id_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhY3IiOiJiYXNpYyIsImFtciI6IjEwIiwiYXVkIjoiNWI0NDg3YzQtOGRiMS00MDlkLWE2NTMtZjkwN2I4MDk0MDM5IiwiZXhwIjoxNzI0ODM1ODU5LCJpYXQiOjE3MjQ4MzIyNTksInN1YiI6ImJvRzhkZmM1TUtUbjM3bzdnc2RDZXlxTDhMcFdRdGdvTzQxbTFLWndkcTAiLCJpc3MiOiJodHRwczovL2FkbWluLXVpLXRlc3QuZ2x1dS5vcmciLCJqdGkiOiJzazNUNDBOWVNZdWs1c2FIWk5wa1p3Iiwibm9uY2UiOiJjMzg3MmFmOS1hMGY1LTRjM2YtYTFhZi1mOWQwZTg4NDZlODEiLCJzaWQiOiI2YTdmZTUwYS1kODEwLTQ1NGQtYmU1ZC01NDlkMjk1OTVhMDkiLCJqYW5zT3BlbklEQ29ubmVjdFZlcnNpb24iOiJvcGVuaWRjb25uZWN0LTEuMCIsImNfaGFzaCI6InBHb0s2WV9SS2NXSGtVZWNNOXV3NlEiLCJhdXRoX3RpbWUiOjE3MjQ4MzA3NDYsImdyYW50IjoiYXV0aG9yaXphdGlvbl9jb2RlIiwic3RhdHVzIjp7InN0YXR1c19saXN0Ijp7ImlkeCI6MjAyLCJ1cmkiOiJodHRwczovL2FkbWluLXVpLXRlc3QuZ2x1dS5vcmcvamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdCJ9fX0.8BwLLGkFpWGx8wGpvVmNk_Ao8nZrP_WT-zoo-MY4zqY".to_string();

    // JSON payload of userinfo token
    //   {
    //     "country": "US",
    //     "email": "user@example.com",
    //     "username": "UserNameExample",
    //     "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
    //     "iss": "https://admin-ui-test.gluu.org",
    //     "given_name": "Admin",
    //     "middle_name": "Admin",
    //     "inum": "8d1cde6a-1447-4766-b3c8-16663e13b458",
    //     "client_id": "5b4487c4-8db1-409d-a653-f907b8094039",
    //     "aud": "5b4487c4-8db1-409d-a653-f907b8094039",
    //     "updated_at": 1724778591,
    //     "name": "Default Admin User",
    //     "nickname": "Admin",
    //     "family_name": "User",
    //     "jti": "faiYvaYIT0cDAT7Fow0pQw",
    //     "jansAdminUIRole": [
    //       "api-admin"
    //     ],
    //     "exp": 1724945978
    //   }
    let userinfo_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb3VudHJ5IjoiVVMiLCJlbWFpbCI6InVzZXJAZXhhbXBsZS5jb20iLCJ1c2VybmFtZSI6IlVzZXJOYW1lRXhhbXBsZSIsInN1YiI6ImJvRzhkZmM1TUtUbjM3bzdnc2RDZXlxTDhMcFdRdGdvTzQxbTFLWndkcTAiLCJpc3MiOiJodHRwczovL2FkbWluLXVpLXRlc3QuZ2x1dS5vcmciLCJnaXZlbl9uYW1lIjoiQWRtaW4iLCJtaWRkbGVfbmFtZSI6IkFkbWluIiwiaW51bSI6IjhkMWNkZTZhLTE0NDctNDc2Ni1iM2M4LTE2NjYzZTEzYjQ1OCIsImNsaWVudF9pZCI6IjViNDQ4N2M0LThkYjEtNDA5ZC1hNjUzLWY5MDdiODA5NDAzOSIsImF1ZCI6IjViNDQ4N2M0LThkYjEtNDA5ZC1hNjUzLWY5MDdiODA5NDAzOSIsInVwZGF0ZWRfYXQiOjE3MjQ3Nzg1OTEsIm5hbWUiOiJEZWZhdWx0IEFkbWluIFVzZXIiLCJuaWNrbmFtZSI6IkFkbWluIiwiZmFtaWx5X25hbWUiOiJVc2VyIiwianRpIjoiZmFpWXZhWUlUMGNEQVQ3Rm93MHBRdyIsImphbnNBZG1pblVJUm9sZSI6WyJhcGktYWRtaW4iXSwiZXhwIjoxNzI0OTQ1OTc4fQ.3LTc8YLvEeb7ONZp_FKA7yPP7S6e_VTzwhvAWUJrL4M".to_string();

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
        Err(e) => eprintln!("Error while authorizing: {}\n {:?}\n\n", e.to_string(), e),
    }

    Ok(())
}
