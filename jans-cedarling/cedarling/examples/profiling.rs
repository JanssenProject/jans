// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedarling::*;
use std::collections::{HashMap, HashSet};
use std::fs::File;

#[tokio::main(flavor = "current_thread")]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let cedarling = init_cedarling().await;
    let (access_token, id_token, userinfo_token) = get_tokens();

    // init profiler guard
    let guard = pprof::ProfilerGuardBuilder::default()
        .frequency(1000)
        .blocklist(&["libc", "libgcc", "pthread", "vdso"])
        .build()
        .unwrap();

    // calls Cedarling::authorize
    call_authorize(&cedarling, &access_token, &id_token, &userinfo_token).await;

    // write output to file
    if let Ok(report) = guard.report().build() {
        let file = File::create("../cedarling_profiling_flamegraph.svg").unwrap();
        let mut options = pprof::flamegraph::Options::default();
        options.image_width = Some(3000);
        report.flamegraph_with_options(file, &mut options).unwrap();
    };

    Ok(())
}

static POLICY_STORE_RAW: &str = include_str!("../../test_files/policy-store_ok.yaml");

async fn init_cedarling() -> Cedarling {
    let cedarling = Cedarling::new(&BootstrapConfig {
        application_name: "test_app".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::Off,
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
            token_validation_settings: HashMap::from_iter(
                ["access_token", "id_token", "userinfo_token", "custom_token"]
                    .iter()
                    .map(|tkn| (tkn.to_string(), TokenValidationConfig::default())),
            ),
        }
        .allow_all_algorithms(),
        authorization_config: AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: true,
            user_workload_operator: WorkloadBoolOp::And,
            decision_log_default_jwt_id: "jti".to_string(),
            decision_log_user_claims: vec!["client_id".to_string(), "username".to_string()],
            decision_log_workload_claims: vec!["org_id".to_string()],
            id_token_trust_mode: IdTokenTrustMode::None,
            mapping_tokens: HashMap::from([
                ("access_token".to_string(), "Jans::Access_token".to_string()),
                ("id_token".to_string(), "Jans::id_token".to_string()),
                (
                    "userinfo_token".to_string(),
                    "Jans::Userinfo_token".to_string(),
                ),
            ])
            .into(),
            ..Default::default()
        },
    })
    .await
    .expect("should initialize cedarling");

    cedarling
}

fn get_tokens() -> (String, String, String) {
    let access_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY29kZSI6ImJmMTkzNGY2LTM5MDUtNDIwYS04Mjk5LTZiMmUzZmZkZGQ2ZSIsImlzcyI6Imh0dHBzOi8vYWRtaW4tdWktdGVzdC5nbHV1Lm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhdWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhY3IiOiJiYXNpYyIsIng1dCNTMjU2IjoiIiwic2NvcGUiOlsib3BlbmlkIiwicHJvZmlsZSJdLCJvcmdfaWQiOiJzb21lX2xvbmdfaWQiLCJhdXRoX3RpbWUiOjE3MjQ4MzA3NDYsImV4cCI6MTcyNDk0NTk3OCwiaWF0IjoxNzI0ODMyMjU5LCJqdGkiOiJhY2Nlc3NfdGtuX2p0aSIsIm5hbWUiOiJEZWZhdWx0IEFkbWluIFVzZXIiLCJzdGF0dXMiOnsic3RhdHVzX2xpc3QiOnsiaWR4IjoyMDEsInVyaSI6Imh0dHBzOi8vYWRtaW4tdWktdGVzdC5nbHV1Lm9yZy9qYW5zLWF1dGgvcmVzdHYxL3N0YXR1c19saXN0In19fQ.D6q28qP-rZ3LayPsVlvUzXCwHtl7g3VTntMQvG_f3mM".to_string();
    let id_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY29kZSI6ImJmMTkzNGY2LTM5MDUtNDIwYS04Mjk5LTZiMmUzZmZkZGQ2ZSIsImlzcyI6Imh0dHBzOi8vYWRtaW4tdWktdGVzdC5nbHV1Lm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhdWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhY3IiOiJiYXNpYyIsImFtciI6InB3ZCIsInVzZXJuYW1lIjoiYWRtaW5AZ2x1dS5vcmciLCJjb3VudHJ5IjoidXNhIiwieDV0I1MyNTYiOiIiLCJzY29wZSI6WyJvcGVuaWQiLCJwcm9maWxlIl0sIm9yZ19pZCI6InNvbWVfbG9uZ19pZCIsImF1dGhfdGltZSI6MTcyNDgzMDc0NiwiZXhwIjoxNzI0OTQ1OTc4LCJpYXQiOjE3MjQ4MzIyNTksImp0aSI6ImlkX3Rrbl9qdGkiLCJuYW1lIjoiRGVmYXVsdCBBZG1pbiBVc2VyIiwic3RhdHVzIjp7InN0YXR1c19saXN0Ijp7ImlkeCI6MjAxLCJ1cmkiOiJodHRwczovL2FkbWluLXVpLXRlc3QuZ2x1dS5vcmcvamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdCJ9fX0.xVRNRN7RW3Y2n4bzW0k93zbe5Tn0htQS6JiVq9NP0NE".to_string();
    let userinfo_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL2FkbWluLXVpLXRlc3QuZ2x1dS5vcmciLCJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY2xpZW50X2lkIjoiNWI0NDg3YzQtOGRiMS00MDlkLWE2NTMtZjkwN2I4MDk0MDM5IiwiYXVkIjoiNWI0NDg3YzQtOGRiMS00MDlkLWE2NTMtZjkwN2I4MDk0MDM5IiwidXNlcm5hbWUiOiJhZG1pbkBnbHV1Lm9yZyIsIm5hbWUiOiJEZWZhdWx0IEFkbWluIFVzZXIiLCJlbWFpbCI6ImFkbWluQGdsdXUub3JnIiwiY291bnRyeSI6IlVTIiwianRpIjoidXNyaW5mb190a25fanRpIn0.NoR53vPZFpfb4vFk85JH9RPx7CHsaJMZwrH3fnB-N60".to_string();

    (access_token, id_token, userinfo_token)
}

async fn call_authorize(
    cedarling: &Cedarling,
    access_token: &str,
    id_token: &str,
    userinfo_token: &str,
) {
    let _result = cedarling
        .authorize(Request {
            tokens: HashMap::from([
                ("access_token".to_string(), access_token.to_string()),
                ("id_token".to_string(), id_token.to_string()),
                ("userinfo_token".to_string(), userinfo_token.to_string()),
            ]),
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
        })
        .await;
}
