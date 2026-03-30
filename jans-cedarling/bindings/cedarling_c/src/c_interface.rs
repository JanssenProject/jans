// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashMap;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::{Arc, LazyLock, Mutex};
use tokio::runtime::Runtime;

use cedarling::{
    self as base, BootstrapConfig, BootstrapConfigRaw, DataApi, LogStorage,
    TrustedIssuerLoadingInfo,
};

use crate::types::*;

/// Global runtime for async operations
static CEDARLING_RUNTIME: LazyLock<Result<CedarlingRuntime, String>> = LazyLock::new(|| {
    Runtime::new()
        .map(|rt| CedarlingRuntime {
            runtime: Arc::new(rt),
            instances: Mutex::new(HashMap::new()),
        })
        .map_err(|e| format!("Internal runtime initialization failure: {}", e))
});

fn get_instance_id() -> u64 {
    static INSTANCE_ID: AtomicU64 = AtomicU64::new(1);
    INSTANCE_ID.fetch_add(1, Ordering::Relaxed)
}

/// Runtime manager for Cedarling instances
struct CedarlingRuntime {
    runtime: Arc<Runtime>,
    instances: Mutex<HashMap<u64, Arc<base::Cedarling>>>,
}

impl CedarlingRuntime {
    fn add_instance(&self, instance: base::Cedarling) -> u64 {
        let instance_id = get_instance_id();
        match self.instances.lock() {
            Ok(mut guard) => {
                guard.insert(instance_id, Arc::new(instance));
            },
            Err(e) => {
                set_last_error(&format!(
                    "Internal lock failure while adding instance: {}",
                    e
                ));
                return 0;
            },
        }
        instance_id
    }

    fn drop_instance(&self, instance_id: u64) -> bool {
        match self.instances.lock() {
            Ok(mut guard) => guard.remove(&instance_id).is_some(),
            Err(e) => {
                set_last_error(&format!(
                    "Internal lock failure while dropping instance: {}",
                    e
                ));
                false
            },
        }
    }

    fn get_instance(
        &self,
        instance_id: u64,
    ) -> Result<Option<Arc<base::Cedarling>>, CedarlingErrorCode> {
        match self.instances.lock() {
            Ok(guard) => Ok(guard.get(&instance_id).cloned()),
            Err(e) => {
                set_last_error(&format!(
                    "Internal lock failure while getting instance: {}",
                    e
                ));
                Err(CedarlingErrorCode::Internal)
            },
        }
    }
}

fn runtime_ref() -> Result<&'static CedarlingRuntime, CedarlingErrorCode> {
    match &*CEDARLING_RUNTIME {
        Ok(runtime) => Ok(runtime),
        Err(msg) => {
            set_last_error(msg);
            Err(CedarlingErrorCode::Internal)
        },
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

    let runtime = match runtime_ref() {
        Ok(runtime) => runtime,
        Err(_) => {
            return CedarlingInstanceResult::error(
                CedarlingErrorCode::Internal,
                "Internal runtime initialization failure",
            );
        },
    };

    let instance = match runtime
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

    let instance_id = runtime.add_instance(instance);
    if instance_id == 0 {
        return CedarlingInstanceResult::error(
            CedarlingErrorCode::Internal,
            "Internal lock failure while adding instance",
        );
    }

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

    let runtime = match runtime_ref() {
        Ok(runtime) => runtime,
        Err(_) => {
            return CedarlingInstanceResult::error(
                CedarlingErrorCode::Internal,
                "Internal runtime initialization failure",
            );
        },
    };

    let instance = match runtime
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

    let instance_id = runtime.add_instance(instance);
    CedarlingInstanceResult::success(instance_id)
}

/// Drop a Cedarling Instance
pub fn drop_instance(instance_id: u64) -> bool {
    clear_last_error();
    match runtime_ref() {
        Ok(runtime) => runtime.drop_instance(instance_id),
        Err(_) => false,
    }
}

/// Authorize an unsigned request
pub fn authorize_unsigned(instance_id: u64, request_json: &str) -> CedarlingResult {
    clear_last_error();

    let runtime = match runtime_ref() {
        Ok(runtime) => runtime,
        Err(_) => {
            return CedarlingResult::error(
                CedarlingErrorCode::Internal,
                "Internal runtime initialization failure",
            );
        },
    };

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            let error_msg = "Instance not found";
            set_last_error(error_msg);
            return CedarlingResult::error(CedarlingErrorCode::InstanceNotFound, error_msg);
        },
        Err(code) => {
            return CedarlingResult::error(code, "Internal lock failure");
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

    match runtime
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

/// Authorize a multi-issuer request
pub fn authorize_multi_issuer(instance_id: u64, request_json: &str) -> CedarlingResult {
    clear_last_error();

    let runtime = match runtime_ref() {
        Ok(runtime) => runtime,
        Err(_) => {
            return CedarlingResult::error(
                CedarlingErrorCode::Internal,
                "Internal runtime initialization failure",
            );
        },
    };

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            let error_msg = "Instance not found";
            set_last_error(error_msg);
            return CedarlingResult::error(CedarlingErrorCode::InstanceNotFound, error_msg);
        },
        Err(code) => {
            return CedarlingResult::error(code, "Internal lock failure");
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

    match runtime
        .runtime
        .block_on(instance.authorize_multi_issuer(request))
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

// Context Data API functions

/// Push context data
pub fn context_push(instance_id: u64, key: &str, value_json: &str) -> CedarlingResult {
    clear_last_error();

    let runtime = match runtime_ref() {
        Ok(runtime) => runtime,
        Err(_) => {
            return CedarlingResult::error(
                CedarlingErrorCode::Internal,
                "Internal runtime initialization failure",
            );
        },
    };

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            let error_msg = "Instance not found";
            set_last_error(error_msg);
            return CedarlingResult::error(CedarlingErrorCode::InstanceNotFound, error_msg);
        },
        Err(code) => {
            return CedarlingResult::error(code, "Internal lock failure");
        },
    };

    let value: serde_json::Value = match serde_json::from_str(value_json) {
        Ok(v) => v,
        Err(e) => {
            let error_msg = format!("Failed to parse value JSON: {}", e);
            set_last_error(&error_msg);
            return CedarlingResult::error(CedarlingErrorCode::JsonError, &error_msg);
        },
    };

    match instance.push_data_ctx(key, value, None) {
        Ok(()) => CedarlingResult::success("{}".to_string()),
        Err(e) => {
            let error_msg = format!("Failed to push data: {}", e);
            set_last_error(&error_msg);
            CedarlingResult::error(CedarlingErrorCode::Internal, &error_msg)
        },
    }
}

/// Get context data by key
pub fn context_get(instance_id: u64, key: &str) -> CedarlingResult {
    clear_last_error();

    let runtime = match runtime_ref() {
        Ok(runtime) => runtime,
        Err(_) => {
            return CedarlingResult::error(
                CedarlingErrorCode::Internal,
                "Internal runtime initialization failure",
            );
        },
    };

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            let error_msg = "Instance not found";
            set_last_error(error_msg);
            return CedarlingResult::error(CedarlingErrorCode::InstanceNotFound, error_msg);
        },
        Err(code) => {
            return CedarlingResult::error(code, "Internal lock failure");
        },
    };

    match instance.get_data_ctx(key) {
        Ok(Some(value)) => match serde_json::to_string(&value) {
            Ok(json) => CedarlingResult::success(json),
            Err(e) => {
                let error_msg = format!("Failed to serialize value: {}", e);
                set_last_error(&error_msg);
                CedarlingResult::error(CedarlingErrorCode::Internal, &error_msg)
            },
        },
        Ok(None) => CedarlingResult::success("null".to_string()),
        Err(e) => {
            let error_msg = format!("Failed to get data: {}", e);
            set_last_error(&error_msg);
            CedarlingResult::error(CedarlingErrorCode::Internal, &error_msg)
        },
    }
}

/// Remove context data by key
pub fn context_remove(instance_id: u64, key: &str) -> CedarlingResult {
    clear_last_error();

    let runtime = match runtime_ref() {
        Ok(runtime) => runtime,
        Err(_) => {
            return CedarlingResult::error(
                CedarlingErrorCode::Internal,
                "Internal runtime initialization failure",
            );
        },
    };

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            let error_msg = "Instance not found";
            set_last_error(error_msg);
            return CedarlingResult::error(CedarlingErrorCode::InstanceNotFound, error_msg);
        },
        Err(code) => {
            return CedarlingResult::error(code, "Internal lock failure");
        },
    };

    match instance.remove_data_ctx(key) {
        Ok(removed) => {
            let result = serde_json::json!({"removed": removed});
            CedarlingResult::success(result.to_string())
        },
        Err(e) => {
            let error_msg = format!("Failed to remove data: {}", e);
            set_last_error(&error_msg);
            CedarlingResult::error(CedarlingErrorCode::Internal, &error_msg)
        },
    }
}

/// Clear all context data
pub fn context_clear(instance_id: u64) -> CedarlingResult {
    clear_last_error();

    let runtime = match runtime_ref() {
        Ok(runtime) => runtime,
        Err(_) => {
            return CedarlingResult::error(
                CedarlingErrorCode::Internal,
                "Internal runtime initialization failure",
            );
        },
    };

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            let error_msg = "Instance not found";
            set_last_error(error_msg);
            return CedarlingResult::error(CedarlingErrorCode::InstanceNotFound, error_msg);
        },
        Err(code) => {
            return CedarlingResult::error(code, "Internal lock failure");
        },
    };

    match instance.clear_data_ctx() {
        Ok(()) => CedarlingResult::success("{}".to_string()),
        Err(e) => {
            let error_msg = format!("Failed to clear data: {}", e);
            set_last_error(&error_msg);
            CedarlingResult::error(CedarlingErrorCode::Internal, &error_msg)
        },
    }
}

/// List all context entries
pub fn context_list(instance_id: u64) -> CedarlingResult {
    clear_last_error();

    let runtime = match runtime_ref() {
        Ok(runtime) => runtime,
        Err(_) => {
            return CedarlingResult::error(
                CedarlingErrorCode::Internal,
                "Internal runtime initialization failure",
            );
        },
    };

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            let error_msg = "Instance not found";
            set_last_error(error_msg);
            return CedarlingResult::error(CedarlingErrorCode::InstanceNotFound, error_msg);
        },
        Err(code) => {
            return CedarlingResult::error(code, "Internal lock failure");
        },
    };

    match instance.list_data_ctx() {
        Ok(entries) => match serde_json::to_string(&entries) {
            Ok(json) => CedarlingResult::success(json),
            Err(e) => {
                let error_msg = format!("Failed to serialize entries: {}", e);
                set_last_error(&error_msg);
                CedarlingResult::error(CedarlingErrorCode::Internal, &error_msg)
            },
        },
        Err(e) => {
            let error_msg = format!("Failed to list data: {}", e);
            set_last_error(&error_msg);
            CedarlingResult::error(CedarlingErrorCode::Internal, &error_msg)
        },
    }
}

/// Get context stats
pub fn context_stats(instance_id: u64) -> CedarlingResult {
    clear_last_error();

    let runtime = match runtime_ref() {
        Ok(runtime) => runtime,
        Err(_) => {
            return CedarlingResult::error(
                CedarlingErrorCode::Internal,
                "Internal runtime initialization failure",
            );
        },
    };

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            let error_msg = "Instance not found";
            set_last_error(error_msg);
            return CedarlingResult::error(CedarlingErrorCode::InstanceNotFound, error_msg);
        },
        Err(code) => {
            return CedarlingResult::error(code, "Internal lock failure");
        },
    };

    match instance.get_stats_ctx() {
        Ok(stats) => match serde_json::to_string(&stats) {
            Ok(json) => CedarlingResult::success(json),
            Err(e) => {
                let error_msg = format!("Failed to serialize stats: {}", e);
                set_last_error(&error_msg);
                CedarlingResult::error(CedarlingErrorCode::Internal, &error_msg)
            },
        },
        Err(e) => {
            let error_msg = format!("Failed to get stats: {}", e);
            set_last_error(&error_msg);
            CedarlingResult::error(CedarlingErrorCode::Internal, &error_msg)
        },
    }
}

/// Pop all logs from the instance
pub fn pop_logs(instance_id: u64) -> Result<CedarlingStringArray, CedarlingErrorCode> {
    clear_last_error();

    let runtime = match runtime_ref() {
        Ok(runtime) => runtime,
        Err(code) => return Err(code),
    };

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            let error_msg = "Instance not found";
            set_last_error(error_msg);
            return Err(CedarlingErrorCode::InstanceNotFound);
        },
        Err(code) => return Err(code),
    };

    let logs = instance.pop_logs();

    let log_strings: Vec<String> = logs.iter().map(|log| log.to_string()).collect();

    CedarlingStringArray::try_new(log_strings)
}

/// Get a log by ID
pub fn get_log_by_id(instance_id: u64, log_id: &str) -> CedarlingResult {
    clear_last_error();

    let runtime = match runtime_ref() {
        Ok(runtime) => runtime,
        Err(_) => {
            return CedarlingResult::error(
                CedarlingErrorCode::Internal,
                "Internal runtime initialization failure",
            );
        },
    };

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            let error_msg = "Instance not found";
            set_last_error(error_msg);
            return CedarlingResult::error(CedarlingErrorCode::InstanceNotFound, error_msg);
        },
        Err(code) => {
            return CedarlingResult::error(code, "Internal lock failure");
        },
    };

    let log = instance.get_log_by_id(log_id);

    let log_string = log.iter().map(|l| l.to_string()).collect::<String>();

    CedarlingResult::success(log_string)
}

/// Get all log IDs
pub fn get_log_ids(instance_id: u64) -> Result<CedarlingStringArray, CedarlingErrorCode> {
    clear_last_error();

    let runtime = match runtime_ref() {
        Ok(runtime) => runtime,
        Err(code) => return Err(code),
    };

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            let error_msg = "Instance not found";
            set_last_error(error_msg);
            return Err(CedarlingErrorCode::InstanceNotFound);
        },
        Err(code) => return Err(code),
    };

    let logs = instance.get_log_ids();

    let log_strings: Vec<String> = logs.iter().map(|log| log.to_string()).collect();

    CedarlingStringArray::try_new(log_strings)
}

/// Get logs by tag
pub fn get_logs_by_tag(
    instance_id: u64,
    tag: &str,
) -> Result<CedarlingStringArray, CedarlingErrorCode> {
    clear_last_error();
    let runtime = match runtime_ref() {
        Ok(runtime) => runtime,
        Err(code) => return Err(code),
    };

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            let error_msg = "Instance not found";
            set_last_error(error_msg);
            return Err(CedarlingErrorCode::InstanceNotFound);
        },
        Err(code) => return Err(code),
    };
    let logs = instance.get_logs_by_tag(tag);

    let log_strings: Vec<String> = logs.iter().map(|log| log.to_string()).collect();

    CedarlingStringArray::try_new(log_strings)
}

/// Get logs by request ID
pub fn get_logs_by_request_id(
    instance_id: u64,
    request_id: &str,
) -> Result<CedarlingStringArray, CedarlingErrorCode> {
    clear_last_error();
    let runtime = match runtime_ref() {
        Ok(runtime) => runtime,
        Err(code) => return Err(code),
    };

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            let error_msg = "Instance not found";
            set_last_error(error_msg);
            return Err(CedarlingErrorCode::InstanceNotFound);
        },
        Err(code) => return Err(code),
    };
    let logs = instance.get_logs_by_request_id(request_id);

    let log_strings: Vec<String> = logs.iter().map(|log| log.to_string()).collect();

    CedarlingStringArray::try_new(log_strings)
}

/// Get logs by request ID and tag
pub fn get_logs_by_request_id_and_tag(
    instance_id: u64,
    request_id: &str,
    tag: &str,
) -> Result<CedarlingStringArray, CedarlingErrorCode> {
    clear_last_error();
    let runtime = match runtime_ref() {
        Ok(runtime) => runtime,
        Err(code) => return Err(code),
    };
    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            let error_msg = "Instance not found";
            set_last_error(error_msg);
            return Err(CedarlingErrorCode::InstanceNotFound);
        },
        Err(code) => return Err(code),
    };
    let logs = instance.get_logs_by_request_id_and_tag(request_id, tag);

    let log_strings: Vec<String> = logs.iter().map(|log| log.to_string()).collect();

    CedarlingStringArray::try_new(log_strings)
}

/// Check whether a trusted issuer was loaded by issuer identifier
pub fn is_trusted_issuer_loaded_by_name(instance_id: u64, issuer_id: &str) -> bool {
    clear_last_error();
    let runtime = match runtime_ref() {
        Ok(runtime) => runtime,
        Err(_) => {
            return false;
        },
    };
    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            set_last_error("Instance not found");
            return false;
        },
        Err(_) => {
            // last_error already set by get_instance
            return false;
        },
    };
    instance.is_trusted_issuer_loaded_by_name(issuer_id)
}

/// Check whether a trusted issuer was loaded by `iss` claim
pub fn is_trusted_issuer_loaded_by_iss(instance_id: u64, iss_claim: &str) -> bool {
    clear_last_error();
    let runtime = match runtime_ref() {
        Ok(runtime) => runtime,
        Err(_) => {
            return false;
        },
    };
    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            set_last_error("Instance not found");
            return false;
        },
        Err(_) => {
            // last_error already set by get_instance
            return false;
        },
    };
    instance.is_trusted_issuer_loaded_by_iss(iss_claim)
}

/// Get total trusted issuers discovered
pub fn total_issuers(instance_id: u64) -> usize {
    clear_last_error();
    let runtime = match runtime_ref() {
        Ok(runtime) => runtime,
        Err(_) => {
            return 0;
        },
    };
    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            set_last_error("Instance not found");
            return 0;
        },
        Err(_) => {
            // last_error already set by get_instance
            return 0;
        },
    };
    instance.total_issuers()
}

/// Get number of trusted issuers loaded successfully
pub fn loaded_trusted_issuers_count(instance_id: u64) -> usize {
    clear_last_error();
    let runtime = match runtime_ref() {
        Ok(runtime) => runtime,
        Err(_) => {
            return 0;
        },
    };
    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            set_last_error("Instance not found");
            return 0;
        },
        Err(_) => {
            // last_error already set by get_instance
            return 0;
        },
    };
    instance.loaded_trusted_issuers_count()
}

/// Get trusted issuer IDs loaded successfully
pub fn loaded_trusted_issuer_ids(
    instance_id: u64,
) -> Result<CedarlingStringArray, CedarlingErrorCode> {
    clear_last_error();
    let runtime = match runtime_ref() {
        Ok(runtime) => runtime,
        Err(code) => return Err(code),
    };
    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            let error_msg = "Instance not found";
            set_last_error(error_msg);
            return Err(CedarlingErrorCode::InstanceNotFound);
        },
        Err(code) => return Err(code),
    };
    let ids: Vec<String> = instance.loaded_trusted_issuer_ids().into_iter().collect();
    CedarlingStringArray::try_new(ids)
}

/// Get trusted issuer IDs that failed to load
pub fn failed_trusted_issuer_ids(
    instance_id: u64,
) -> Result<CedarlingStringArray, CedarlingErrorCode> {
    clear_last_error();
    let runtime = match runtime_ref() {
        Ok(runtime) => runtime,
        Err(code) => return Err(code),
    };

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            let error_msg = "Instance not found";
            set_last_error(error_msg);
            return Err(CedarlingErrorCode::InstanceNotFound);
        },
        Err(code) => return Err(code),
    };
    let ids: Vec<String> = instance.failed_trusted_issuer_ids().into_iter().collect();
    CedarlingStringArray::try_new(ids)
}

/// Shutdown an instance
pub fn shutdown_instance(instance_id: u64) -> CedarlingErrorCode {
    clear_last_error();

    let runtime = match runtime_ref() {
        Ok(runtime) => runtime,
        Err(_) => {
            return CedarlingErrorCode::Internal;
        },
    };

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            set_last_error("Instance not found");
            return CedarlingErrorCode::InstanceNotFound;
        },
        Err(code) => {
            return code;
        },
    };

    runtime.runtime.block_on(instance.shut_down());

    CedarlingErrorCode::Success
}
