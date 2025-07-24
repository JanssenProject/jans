/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2025, Gluu, Inc.
 */

use std::collections::HashMap;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::{Arc, LazyLock, Mutex};
use tokio::runtime::Runtime;

use cedarling::{self as base, BootstrapConfig, BootstrapConfigRaw, LogStorage};

use crate::types::*;

/// Global runtime for async operations
static CEDARLING_RUNTIME: LazyLock<CedarlingRuntime> = LazyLock::new(|| {
    let rt = Runtime::new().expect("Failed to create tokio runtime");

    CedarlingRuntime {
        runtime: Arc::new(rt),
        instances: Mutex::new(HashMap::new()),
    }
});

fn get_instance_id() -> u64 {
    static INSTANCE_ID: AtomicU64 = AtomicU64::new(1);
    INSTANCE_ID.fetch_add(1, Ordering::SeqCst)
}

/// Runtime manager for Cedarling instances
struct CedarlingRuntime {
    runtime: Arc<Runtime>,
    instances: Mutex<HashMap<u64, Arc<base::Cedarling>>>,
}

impl CedarlingRuntime {
    fn add_instance(&self, instance: base::Cedarling) -> u64 {
        let instance_id = get_instance_id();
        self.instances
            .lock()
            .unwrap()
            .insert(instance_id, Arc::new(instance));
        instance_id
    }

    fn drop_instance(&self, instance_id: u64) -> bool {
        self.instances
            .lock()
            .unwrap()
            .remove(&instance_id)
            .is_some()
    }

    fn get_instance(&self, instance_id: u64) -> Option<Arc<base::Cedarling>> {
        self.instances.lock().unwrap().get(&instance_id).cloned()
    }
}

/// Create a new cedarling instance
pub fn create_instance(config_json: &str) -> CedarlingInstanceResult {
    clear_last_error();
    let bootstrap_config_raw = match serde_json::from_str::<BootstrapConfigRaw>(config_json) {
        Ok(config) => config,
        Err(e) => {
            let error_msg = format!("Failed to parse bootstrap config: {}", e);
            set_last_error(&error_msg);

            return CedarlingInstanceResult::error(CedarlingErrorCode::JsonError, &error_msg);
        },
    };

    let bootstrap_config = match BootstrapConfig::from_raw_config(&bootstrap_config_raw) {
        Ok(config) => config,
        Err(e) => {
            let error_msg = format!("Invalid bootstrap config: {}", e);
            set_last_error(&error_msg);

            return CedarlingInstanceResult::error(
                CedarlingErrorCode::ConfigurationError,
                &error_msg,
            );
        },
    };

    let instance = match CEDARLING_RUNTIME
        .runtime
        .block_on(base::Cedarling::new(&bootstrap_config))
    {
        Ok(instance) => instance,
        Err(e) => {
            let error_msg = format!("Failed to create Cedarling instance: {}", e);
            set_last_error(&error_msg);

            return CedarlingInstanceResult::error(CedarlingErrorCode::Internal, &error_msg);
        },
    };

    let instance_id = CEDARLING_RUNTIME.add_instance(instance);

    CedarlingInstanceResult::success(instance_id)
}

/// Create a new Cedarling instance with environment variables
pub fn create_instance_with_env(config_json: Option<&str>) -> CedarlingInstanceResult {
    clear_last_error();

    let bootstrap_config_raw = match config_json {
        Some(json) => match serde_json::from_str::<Option<BootstrapConfigRaw>>(json) {
            Ok(config) => config,
            Err(e) => {
                let error_msg = format!("Failed to parse bootstrap config: {}", e);
                set_last_error(&error_msg);

                return CedarlingInstanceResult::error(CedarlingErrorCode::JsonError, &error_msg);
            },
        },
        None => None,
    };

    let instance = match CEDARLING_RUNTIME
        .runtime
        .block_on(base::Cedarling::new_with_env(bootstrap_config_raw))
    {
        Ok(instance) => instance,
        Err(e) => {
            let error_msg = format!("Failed to create Cedarling instance: {}", e);
            set_last_error(&error_msg);

            return CedarlingInstanceResult::error(CedarlingErrorCode::Internal, &error_msg);
        },
    };

    let instance_id = CEDARLING_RUNTIME.add_instance(instance);
    CedarlingInstanceResult::success(instance_id)
}

/// Drop a Cedarling Insatnce
pub fn drop_instance(instance_id: u64) -> bool {
    clear_last_error();
    CEDARLING_RUNTIME.drop_instance(instance_id)
}

/// Authorize a Request
pub fn authorize(instance_id: u64, request_json: &str) -> CedarlingResult {
    clear_last_error();

    let instance = match CEDARLING_RUNTIME.get_instance(instance_id) {
        Some(instance) => instance,
        None => {
            let error_msg = "Instance not found";
            set_last_error(error_msg);

            return CedarlingResult::error(CedarlingErrorCode::InstanceNotFound, error_msg);
        },
    };

    let request = match serde_json::from_str(request_json) {
        Ok(request) => request,
        Err(e) => {
            let error_msg = format!("Failed to parse request JSON: {}", e);
            set_last_error(&error_msg);

            return CedarlingResult::error(CedarlingErrorCode::JsonError, &error_msg);
        },
    };

    match CEDARLING_RUNTIME
        .runtime
        .block_on(instance.authorize(request))
    {
        Ok(response) => match serde_json::to_string(&response) {
            Ok(json) => CedarlingResult::success(json),
            Err(e) => {
                let error_msg = format!("Failed to serialize response: {}", e);
                set_last_error(&error_msg);
                CedarlingResult::error(CedarlingErrorCode::Internal, &error_msg)
            },
        },
        Err(e) => {
            let error_msg = format!("Authorization failed: {}", e);
            set_last_error(&error_msg);
            CedarlingResult::error(CedarlingErrorCode::AuthorizationError, &error_msg)
        },
    }
}

/// Authorize an unsigned request
pub fn authorize_unsigned(instance_id: u64, request_json: &str) -> CedarlingResult {
    clear_last_error();

    let instance = match CEDARLING_RUNTIME.get_instance(instance_id) {
        Some(instance) => instance,
        None => {
            let error_msg = "Instance not found";
            set_last_error(error_msg);

            return CedarlingResult::error(CedarlingErrorCode::InstanceNotFound, error_msg);
        },
    };

    let request = match serde_json::from_str(request_json) {
        Ok(request) => request,
        Err(e) => {
            let error_msg = format!("Failed to parse request JSON: {}", e);
            set_last_error(&error_msg);

            return CedarlingResult::error(CedarlingErrorCode::JsonError, &error_msg);
        },
    };

    match CEDARLING_RUNTIME
        .runtime
        .block_on(instance.authorize_unsigned(request))
    {
        Ok(response) => match serde_json::to_string(&response) {
            Ok(json) => CedarlingResult::success(json),
            Err(e) => {
                let error_msg = format!("Failed to serialize response: {}", e);
                set_last_error(&error_msg);
                CedarlingResult::error(CedarlingErrorCode::Internal, &error_msg)
            },
        },
        Err(e) => {
            let error_msg = format!("Authorization failed: {}", e);
            set_last_error(&error_msg);
            CedarlingResult::error(CedarlingErrorCode::AuthorizationError, &error_msg)
        },
    }
}

/// Pop all logs from the instance
pub fn pop_logs(instance_id: u64) -> CedarlingStringArray {
    clear_last_error();

    let instance = match CEDARLING_RUNTIME.get_instance(instance_id) {
        Some(instance) => instance,
        None => {
            set_last_error("Instance not found");
            return CedarlingStringArray {
                items: std::ptr::null_mut(),
                count: 0,
            };
        },
    };

    let logs = instance.pop_logs();

    let log_strings: Vec<String> = logs.iter().map(|log| log.to_string()).collect();

    CedarlingStringArray::new(log_strings)
}

/// Get a log by ID
pub fn get_log_by_id(instance_id: u64, log_id: &str) -> CedarlingResult {
    clear_last_error();

    let instance = match CEDARLING_RUNTIME.get_instance(instance_id) {
        Some(instance) => instance,
        None => {
            let error_msg = "Instance not found";
            set_last_error(error_msg);

            return CedarlingResult::error(CedarlingErrorCode::InstanceNotFound, error_msg);
        },
    };

    let log = instance.get_log_by_id(log_id);

    let log_string = log.iter().map(|l| l.to_string()).collect::<String>();

    CedarlingResult::success(log_string)
}

/// Get all log IDs
pub fn get_log_ids(instance_id: u64) -> CedarlingStringArray {
    clear_last_error();

    let instance = match CEDARLING_RUNTIME.get_instance(instance_id) {
        Some(instance) => instance,
        None => {
            set_last_error("Instance not found");
            return CedarlingStringArray {
                items: std::ptr::null_mut(),
                count: 0,
            };
        },
    };

    let logs = instance.get_log_ids();

    let log_strings: Vec<String> = logs.iter().map(|log| log.to_string()).collect();

    CedarlingStringArray::new(log_strings)
}

/// Get logs by tag
pub fn get_logs_by_tag(instance_id: u64, tag: &str) -> CedarlingStringArray {
    clear_last_error();
    let instance = match CEDARLING_RUNTIME.get_instance(instance_id) {
        Some(instance) => instance,
        None => {
            set_last_error("Instance not found");
            return CedarlingStringArray {
                items: std::ptr::null_mut(),
                count: 0,
            };
        },
    };
    let logs = instance.get_logs_by_tag(tag);

    let log_strings: Vec<String> = logs.iter().map(|log| log.to_string()).collect();

    CedarlingStringArray::new(log_strings)
}

/// Get logs by request ID
pub fn get_logs_by_request_id(instance_id: u64, request_id: &str) -> CedarlingStringArray {
    clear_last_error();
    let instance = match CEDARLING_RUNTIME.get_instance(instance_id) {
        Some(instance) => instance,
        None => {
            set_last_error("Instance not found");
            return CedarlingStringArray {
                items: std::ptr::null_mut(),
                count: 0,
            };
        },
    };
    let logs = instance.get_logs_by_request_id(request_id);

    let log_strings: Vec<String> = logs.iter().map(|log| log.to_string()).collect();

    CedarlingStringArray::new(log_strings)
}

/// Get logs by request ID and tag
pub fn get_logs_by_request_id_and_tag(
    instance_id: u64,
    request_id: &str,
    tag: &str,
) -> CedarlingStringArray {
    clear_last_error();
    let instance = match CEDARLING_RUNTIME.get_instance(instance_id) {
        Some(instance) => instance,
        None => {
            set_last_error("Instance not found");
            return CedarlingStringArray {
                items: std::ptr::null_mut(),
                count: 0,
            };
        },
    };
    let logs = instance.get_logs_by_request_id_and_tag(request_id, tag);

    let log_strings: Vec<String> = logs.iter().map(|log| log.to_string()).collect();

    CedarlingStringArray::new(log_strings)
}

/// Shutdown an instance
pub fn shutdown_instance(instance_id: u64) -> CedarlingErrorCode {
    clear_last_error();

    let instance = match CEDARLING_RUNTIME.get_instance(instance_id) {
        Some(instance) => instance,
        None => {
            set_last_error("Instance not found");

            return CedarlingErrorCode::InstanceNotFound;
        },
    };

    CEDARLING_RUNTIME.runtime.block_on(instance.shut_down());

    CedarlingErrorCode::Success
}
