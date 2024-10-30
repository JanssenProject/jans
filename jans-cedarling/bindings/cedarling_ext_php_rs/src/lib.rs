#![cfg_attr(windows, feature(abi_vectorcall))]
use ext_php_rs::prelude::*;
use cedarling::{
    BootstrapConfig, Cedarling, JwtConfig, LogConfig, LogTypeConfig, PolicyStoreConfig,
    PolicyStoreSource, Request, ResourceData,
};
use std::collections::HashMap;

static POLICY_STORE_RAW: &str = include_str!("policy-store_ok.json");

#[php_function]

//cedarling_authorize_test() function is exported as PHP extension library 

pub fn cedarling_authorize_test(acc_tok_str: &str,payload_str: &str) -> String {
    let cedarling = match Cedarling::new(BootstrapConfig {
        application_name: "test_app".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::StdOut,
        },
        policy_store_config: PolicyStoreConfig {
            source: PolicyStoreSource::Json(POLICY_STORE_RAW.to_string()),
            store_id: None,
        },
        jwt_config: JwtConfig::Disabled,
    }) {
        Ok(cedarling_instance) => cedarling_instance, // success case
        Err(e) => {
            eprintln!("Failed to initialize Cedarling: {:?}", e); 
            // Return a default error message or a specific String on failure
            return format!("Hello, {}! (Failed to initialize Cedarling)", payload_str); 
        }
    };
    let id_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhY3IiOiJiYXNpYyIsImFtciI6IjEwIiwiYXVkIjoiNWI0NDg3YzQtOGRiMS00MDlkLWE2NTMtZjkwN2I4MDk0MDM5IiwiZXhwIjoxNzI0ODM1ODU5LCJpYXQiOjE3MjQ4MzIyNTksInN1YiI6ImJvRzhkZmM1TUtUbjM3bzdnc2RDZXlxTDhMcFdRdGdvTzQxbTFLWndkcTAiLCJpc3MiOiJodHRwczovL2FkbWluLXVpLXRlc3QuZ2x1dS5vcmciLCJqdGkiOiJzazNUNDBOWVNZdWs1c2FIWk5wa1p3Iiwibm9uY2UiOiJjMzg3MmFmOS1hMGY1LTRjM2YtYTFhZi1mOWQwZTg4NDZlODEiLCJzaWQiOiI2YTdmZTUwYS1kODEwLTQ1NGQtYmU1ZC01NDlkMjk1OTVhMDkiLCJqYW5zT3BlbklEQ29ubmVjdFZlcnNpb24iOiJvcGVuaWRjb25uZWN0LTEuMCIsImNfaGFzaCI6InBHb0s2WV9SS2NXSGtVZWNNOXV3NlEiLCJhdXRoX3RpbWUiOjE3MjQ4MzA3NDYsImdyYW50IjoiYXV0aG9yaXphdGlvbl9jb2RlIiwic3RhdHVzIjp7InN0YXR1c19saXN0Ijp7ImlkeCI6MjAyLCJ1cmkiOiJodHRwczovL2FkbWluLXVpLXRlc3QuZ2x1dS5vcmcvamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdCJ9fX0.8BwLLGkFpWGx8wGpvVmNk_Ao8nZrP_WT-zoo-MY4zqY".to_string();
    
    let access_token = (*acc_tok_str).to_string();
    
    let result = cedarling.authorize(Request {
        access_token,
        id_token,
        action: "Jans::Action::\"Update\"".to_string(),
        context: serde_json::json!({}),
        resource: ResourceData {
            id: "random_id".to_string(),
            resource_type: "Jans::Issue".to_string(),
            payload: HashMap::from_iter([(
                "org_id".to_string(),
                serde_json::Value::String((*payload_str).to_string()),
            )]),
        },
    });
	//
	//
    match result {
        Ok(auth_result) => format!("Hello, {}! Authorization success result: {}!", payload_str, auth_result.is_allowed().to_string() ),
        Err(e) => format!("Hello, {}! Authorization failed: {:?}", payload_str, e),
    }
    
    //
}


#[php_module]
pub fn get_module(module: ModuleBuilder) -> ModuleBuilder {
    module
}
