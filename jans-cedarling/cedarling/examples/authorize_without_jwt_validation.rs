// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedarling::*;
use std::collections::{HashMap, HashSet};

static POLICY_STORE_RAW: &str = include_str!("../../test_files/policy-store_ok.yaml");

#[tokio::main(flavor = "current_thread")]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let cedarling = Cedarling::new(&BootstrapConfig {
        application_name: "test_app".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::StdOut,
            log_level: LogLevel::INFO,
        },
        policy_store_config: PolicyStoreConfig {
            source: PolicyStoreSource::Yaml(POLICY_STORE_RAW.to_string()),
        },
        jwt_config: JwtConfig {
            jwks: None,
            jwt_sig_validation: false,
            jwt_status_validation: false,
            signature_algorithms_supported: HashSet::new(),
        }
        .allow_all_algorithms(),
        authorization_config: AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: true,

            decision_log_default_jwt_id: "jti".to_string(),
            decision_log_user_claims: vec!["client_id".to_string(), "username".to_string()],
            decision_log_workload_claims: vec!["org_id".to_string()],
            id_token_trust_mode: IdTokenTrustMode::None,
            principal_bool_operator: JsonRule::new(serde_json::json!({
                "and" : [
                    {"===": [{"var": "Jans::Workload"}, "ALLOW"]},
                    {"===": [{"var": "Jans::User"}, "ALLOW"]}
                ]
            }))
            .unwrap(),
        },
        entity_builder_config: EntityBuilderConfig::default().with_user().with_workload(),
    })
    .await?;

    // the following tokens are expired
    // access_token claims:
    // {
    //   "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
    //   "code": "bf1934f6-3905-420a-8299-6b2e3ffddd6e",
    //   "iss": "https://test.jans.org",
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
    let access_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY29kZSI6ImJmMTkzNGY2LTM5MDUtNDIwYS04Mjk5LTZiMmUzZmZkZGQ2ZSIsImlzcyI6Imh0dHBzOi8vdGVzdC5qYW5zLm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhdWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhY3IiOiJiYXNpYyIsIng1dCNTMjU2IjoiIiwic2NvcGUiOlsib3BlbmlkIiwicHJvZmlsZSJdLCJvcmdfaWQiOiJzb21lX2xvbmdfaWQiLCJhdXRoX3RpbWUiOjE3MjQ4MzA3NDYsImV4cCI6MTcyNDk0NTk3OCwiaWF0IjoxNzI0ODMyMjU5LCJqdGkiOiJhY2Nlc3NfdGtuX2p0aSIsIm5hbWUiOiJEZWZhdWx0IEFkbWluIFVzZXIiLCJzdGF0dXMiOnsic3RhdHVzX2xpc3QiOnsiaWR4IjoyMDEsInVyaSI6Imh0dHBzOi8vYWRtaW4tdWktdGVzdC5nbHV1Lm9yZy9qYW5zLWF1dGgvcmVzdHYxL3N0YXR1c19saXN0In19fQ.TecxgeZvKQsc2scQ3rCxFIT0JYiI3uLOw1TB4Zw18pQ".to_string();

    // id_token claims:
    // {
    //   "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
    //   "code": "bf1934f6-3905-420a-8299-6b2e3ffddd6e",
    //   "iss": "https://test.jans.org",
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
    let id_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY29kZSI6ImJmMTkzNGY2LTM5MDUtNDIwYS04Mjk5LTZiMmUzZmZkZGQ2ZSIsImlzcyI6Imh0dHBzOi8vdGVzdC5qYW5zLm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhdWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhY3IiOiJiYXNpYyIsImFtciI6InB3ZCIsInVzZXJuYW1lIjoiYWRtaW5AZ2x1dS5vcmciLCJjb3VudHJ5IjoiVVMiLCJ4NXQjUzI1NiI6IiIsInNjb3BlIjpbIm9wZW5pZCIsInByb2ZpbGUiXSwib3JnX2lkIjoic29tZV9sb25nX2lkIiwiYXV0aF90aW1lIjoxNzI0ODMwNzQ2LCJleHAiOjE3MjQ5NDU5NzgsImlhdCI6MTcyNDgzMjI1OSwianRpIjoiaWRfdGtuX2p0aSIsIm5hbWUiOiJEZWZhdWx0IEFkbWluIFVzZXIiLCJzdGF0dXMiOnsic3RhdHVzX2xpc3QiOnsiaWR4IjoyMDEsInVyaSI6Imh0dHBzOi8vYWRtaW4tdWktdGVzdC5nbHV1Lm9yZy9qYW5zLWF1dGgvcmVzdHYxL3N0YXR1c19saXN0In19fQ.S7K8amV8-PINCTaGzrxorHbLz5d8QBeCxT1ZcwV6g4Y".to_string();

    // userinfo_token claims:
    // {
    //   "iss": "https://test.jans.org",
    //   "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
    //   "client_id": "5b4487c4-8db1-409d-a653-f907b8094039",
    //   "aud": "5b4487c4-8db1-409d-a653-f907b8094039",
    //   "ueyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL2FkbWluLXVpLXRlc3QuZ2x1dS5vcmciLCJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY2xpZW50X2lkIjoiNWI0NDg3YzQtOGRiMS00MDlkLWE2NTMtZjkwN2I4MDk0MDM5IiwiYXVkIjoiNWI0NDg3YzQtOGRiMS00MDlkLWE2NTMtZjkwN2I4MDk0MDM5IiwibmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsImVtYWlsIjoiYWRtaW5AZ2x1dS5vcmciLCJjb3VudHJ5IjoiVVMiLCJqdGkiOiJ1c3JpbmZvX3Rrbl9qdGkifQ.sUrKttbzHktPYtSelizTJZJr_8mqkVBpStA5fjNJU9ksername": "admin@gluu.org",
    //   "name": "Default Admin User",
    //   "email": "admin@gluu.org",
    //   "country": "US",
    //   "jti": "usrinfo_tkn_jti"
    // }
    let userinfo_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL3Rlc3QuamFucy5vcmciLCJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY2xpZW50X2lkIjoiNWI0NDg3YzQtOGRiMS00MDlkLWE2NTMtZjkwN2I4MDk0MDM5IiwiYXVkIjoiNWI0NDg3YzQtOGRiMS00MDlkLWE2NTMtZjkwN2I4MDk0MDM5IiwidXNlcm5hbWUiOiJhZG1pbkBnbHV1Lm9yZyIsIm5hbWUiOiJEZWZhdWx0IEFkbWluIFVzZXIiLCJlbWFpbCI6ImFkbWluQGdsdXUub3JnIiwiY291bnRyeSI6IlVTIiwianRpIjoidXNyaW5mb190a25fanRpIn0.HVVu5lx0PkPSGOwYDZTOnZFDiHmqhCLERKtsirkvYZs".to_string();

    let result = cedarling
        .authorize(Request {
            tokens: HashMap::from([
                ("access_token".to_string(), access_token.clone()),
                ("id_token".to_string(), id_token.clone()),
                ("userinfo_token".to_string(), userinfo_token.clone()),
                ("custom_token".to_string(), access_token.clone()),
            ]),
            action: "Jans::Action::\"Update\"".to_string(),
            context: serde_json::json!({}),
            resource: EntityData {
                id: "random_id".to_string(),
                entity_type: "Jans::Issue".to_string(),
                attributes: HashMap::from_iter([
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
        })
        .await;

    match result {
        Ok(result) => {
            println!("\n\nis allowed: {}", result.decision);
        },
        Err(e) => eprintln!("Error while authorizing: {}\n {:?}\n\n", e, e),
    }

    Ok(())
}
