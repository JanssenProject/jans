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

    let access_token = acc_tok_str;
    
    let result = cedarling.authorize(Request {
        access_token,
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
