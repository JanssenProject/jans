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

static POLICY_STORE_RAW: &str = include_str!("../src/init/test_files/policy-store_ok.json");

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

    let access_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY29kZSI6ImJmMTkzNGY2LTM5MDUtNDIwYS04Mjk5LTZiMmUzZmZkZGQ2ZSIsImlzcyI6Imh0dHBzOi8vYWRtaW4tdWktdGVzdC5nbHV1Lm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhdWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhY3IiOiJiYXNpYyIsIng1dCNTMjU2IjoiIiwic2NvcGUiOlsib3BlbmlkIiwicHJvZmlsZSJdLCJvcmdfaWQiOiJzb21lX2xvbmdfaWQiLCJhdXRoX3RpbWUiOjE3MjQ4MzA3NDYsImV4cCI6MTcyNDk0NTk3OCwiaWF0IjoxNzI0ODMyMjU5LCJqdGkiOiJseFRtQ1ZSRlR4T2pKZ3ZFRXBvek1RIiwibmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsInN0YXR1cyI6eyJzdGF0dXNfbGlzdCI6eyJpZHgiOjIwMSwidXJpIjoiaHR0cHM6Ly9hZG1pbi11aS10ZXN0LmdsdXUub3JnL2phbnMtYXV0aC9yZXN0djEvc3RhdHVzX2xpc3QifX19._eQT-DsfE_kgdhA0YOyFxxPEMNw44iwoelWa5iU1n9s";
    let id_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhY3IiOiJiYXNpYyIsImFtciI6IjEwIiwiYXVkIjoiNWI0NDg3YzQtOGRiMS00MDlkLWE2NTMtZjkwN2I4MDk0MDM5IiwiZXhwIjoxNzI0ODM1ODU5LCJpYXQiOjE3MjQ4MzIyNTksInN1YiI6ImJvRzhkZmM1TUtUbjM3bzdnc2RDZXlxTDhMcFdRdGdvTzQxbTFLWndkcTAiLCJpc3MiOiJodHRwczovL2FkbWluLXVpLXRlc3QuZ2x1dS5vcmciLCJqdGkiOiJzazNUNDBOWVNZdWs1c2FIWk5wa1p3Iiwibm9uY2UiOiJjMzg3MmFmOS1hMGY1LTRjM2YtYTFhZi1mOWQwZTg4NDZlODEiLCJzaWQiOiI2YTdmZTUwYS1kODEwLTQ1NGQtYmU1ZC01NDlkMjk1OTVhMDkiLCJqYW5zT3BlbklEQ29ubmVjdFZlcnNpb24iOiJvcGVuaWRjb25uZWN0LTEuMCIsImNfaGFzaCI6InBHb0s2WV9SS2NXSGtVZWNNOXV3NlEiLCJhdXRoX3RpbWUiOjE3MjQ4MzA3NDYsImdyYW50IjoiYXV0aG9yaXphdGlvbl9jb2RlIiwic3RhdHVzIjp7InN0YXR1c19saXN0Ijp7ImlkeCI6MjAyLCJ1cmkiOiJodHRwczovL2FkbWluLXVpLXRlc3QuZ2x1dS5vcmcvamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdCJ9fX0.8BwLLGkFpWGx8wGpvVmNk_Ao8nZrP_WT-zoo-MY4zqY";
    let userinfo_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb3VudHJ5IjoiVVMiLCJlbWFpbCI6InVzZXJAZXhhbXBsZS5jb20iLCJ1c2VybmFtZSI6IlVzZXJOYW1lRXhhbXBsZSIsInN1YiI6ImJvRzhkZmM1TUtUbjM3bzdnc2RDZXlxTDhMcFdRdGdvTzQxbTFLWndkcTAiLCJpc3MiOiJodHRwczovL2FkbWluLXVpLXRlc3QuZ2x1dS5vcmciLCJnaXZlbl9uYW1lIjoiQWRtaW4iLCJtaWRkbGVfbmFtZSI6IkFkbWluIiwiaW51bSI6IjhkMWNkZTZhLTE0NDctNDc2Ni1iM2M4LTE2NjYzZTEzYjQ1OCIsImNsaWVudF9pZCI6IjViNDQ4N2M0LThkYjEtNDA5ZC1hNjUzLWY5MDdiODA5NDAzOSIsImF1ZCI6IjViNDQ4N2M0LThkYjEtNDA5ZC1hNjUzLWY5MDdiODA5NDAzOSIsInVwZGF0ZWRfYXQiOjE3MjQ3Nzg1OTEsIm5hbWUiOiJEZWZhdWx0IEFkbWluIFVzZXIiLCJuaWNrbmFtZSI6IkFkbWluIiwiZmFtaWx5X25hbWUiOiJVc2VyIiwianRpIjoiZmFpWXZhWUlUMGNEQVQ3Rm93MHBRdyIsImphbnNBZG1pblVJUm9sZSI6WyJhcGktYWRtaW4iXX0.2KPAIkn93CngqGLbSeFvEVwBxPUc8TryKrfdntz9zO4";

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
