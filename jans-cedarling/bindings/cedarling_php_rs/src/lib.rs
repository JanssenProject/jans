#![cfg_attr(windows, feature(abi_vectorcall))]

use ext_php_rs::prelude::*;
use cedarling::{
    BootstrapConfig, Cedarling as RustCedarling, JwtConfig, LogConfig, LogTypeConfig, PolicyStoreConfig,
    PolicyStoreSource, Request, ResourceData,
};
use std::collections::HashMap;

static POLICY_STORE_RAW: &str = include_str!("policy-store_ok.json");

#[php_class]
pub struct Cedarling {
    cedarling: RustCedarling, // Wrap the Rust Cedarling instance
}

#[php_impl]
impl Cedarling {
    // Define the __construct method that PHP can use to instantiate the object
    #[php_method]
    pub fn __construct() -> PhpResult<Self> {
        // Initialize the Cedarling instance with the BootstrapConfig
        let cedarling = RustCedarling::new(BootstrapConfig {
            application_name: "test_app".to_string(),
            log_config: LogConfig {
                log_type: LogTypeConfig::StdOut,
            },
            policy_store_config: PolicyStoreConfig {
                source: PolicyStoreSource::Json(POLICY_STORE_RAW.to_string()),
                store_id: None,
            },
            jwt_config: JwtConfig::Disabled,
        }).map_err(|e| format!("Failed to initialize Cedarling: {:?}", e))?;

        Ok(Cedarling { cedarling })
    }

    // PHP-exposed authorization method
    pub fn authz(
        &mut self,
        access_token: &str,
        id_token: &str,
        org_id: &str,
    ) -> PhpResult<String> {
        // Perform the authorization logic
        let result = self.cedarling.authorize(Request {
            access_token: access_token.to_string(),
            id_token: id_token.to_string(),
            action: "Jans::Action::\"Update\"".to_string(),
            context: serde_json::json!({}),
            resource: ResourceData {
                id: "random_id".to_string(),
                resource_type: "Jans::Issue".to_string(),
                payload: HashMap::from_iter([(
                    "org_id".to_string(),
                    serde_json::Value::String(org_id.to_string()),
                )]),
            },
        });

        // Return the result of authorization to PHP
        match result {
            Ok(auth_result) => Ok(format!("Authorization success: {}", auth_result.is_allowed())),
            Err(e) => Err(format!("Authorization failed: {:?}", e).into()),
        }
    }
}

#[php_module]
pub fn get_module(module: ModuleBuilder) -> ModuleBuilder {
    module
}

