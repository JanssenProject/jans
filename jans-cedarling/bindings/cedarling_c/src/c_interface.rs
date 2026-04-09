// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashMap;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::{LazyLock, Mutex};
use std::time::Duration;

use cedarling::{
    BootstrapConfig, BootstrapConfigRaw, DataApi, DataError, LogStorage, TrustedIssuerLoadingInfo,
    blocking,
};

use crate::types::*;

/// Map core [`DataError`] to the closest [`CedarlingErrorCode`] for C callers.
fn cedarling_result_from_data_error(e: DataError) -> CedarlingResult {
    let msg = e.to_string();
    let code = match &e {
        DataError::InvalidKey => CedarlingErrorCode::InvalidArgument,
        DataError::KeyNotFound { .. } => CedarlingErrorCode::KeyNotFound,
        DataError::StorageLimitExceeded { .. } => CedarlingErrorCode::Internal,
        DataError::TTLExceeded { .. } => CedarlingErrorCode::InvalidArgument,
        DataError::ValueTooLarge { .. } => CedarlingErrorCode::InvalidArgument,
        DataError::SerializationError(_) => CedarlingErrorCode::Internal,
    };
    CedarlingResult::error(code, &msg)
}

/// Global registry of per-thread blocking [`cedarling::blocking::Cedarling`] instances.
static CEDARLING_RUNTIME: LazyLock<CedarlingRuntime> = LazyLock::new(|| CedarlingRuntime {
    instances: Mutex::new(HashMap::new()),
});

fn get_instance_id() -> u64 {
    static INSTANCE_ID: AtomicU64 = AtomicU64::new(1);
    INSTANCE_ID.fetch_add(1, Ordering::Relaxed)
}

/// Registry for blocking Cedarling instances, keyed by unique instance ID.
struct CedarlingRuntime {
    instances: Mutex<HashMap<u64, blocking::Cedarling>>,
}

/// Lock acquisition failure when accessing the instance registry (`CedarlingErrorCode` + detail).
pub(crate) type InstanceRegistryLockError = (CedarlingErrorCode, String);

impl CedarlingRuntime {
    fn add_instance(&self, instance: blocking::Cedarling) -> Result<u64, String> {
        let instance_id = get_instance_id();
        match self.instances.lock() {
            Ok(mut guard) => {
                guard.insert(instance_id, instance);
                Ok(instance_id)
            },
            Err(e) => Err(format!(
                "Internal lock failure while adding instance: {}",
                e
            )),
        }
    }

    fn drop_instance(&self, instance_id: u64) -> CedarlingErrorCode {
        match self.instances.lock() {
            Ok(mut guard) => {
                if guard.remove(&instance_id).is_some() {
                    CedarlingErrorCode::Success
                } else {
                    set_last_error("Instance not found");
                    CedarlingErrorCode::InstanceNotFound
                }
            },
            Err(e) => {
                set_last_error(&format!(
                    "Internal lock failure while dropping instance: {}",
                    e
                ));
                CedarlingErrorCode::Internal
            },
        }
    }

    fn get_instance(
        &self,
        instance_id: u64,
    ) -> Result<Option<blocking::Cedarling>, InstanceRegistryLockError> {
        match self.instances.lock() {
            Ok(guard) => Ok(guard.get(&instance_id).cloned()),
            Err(e) => Err((
                CedarlingErrorCode::Internal,
                format!("Internal lock failure while getting instance: {}", e),
            )),
        }
    }

    fn shutdown_and_clear_all_instances(&self) -> CedarlingErrorCode {
        let instances = match self.instances.lock() {
            Ok(mut guard) => {
                let instances: Vec<blocking::Cedarling> = guard.values().cloned().collect();
                guard.clear();
                instances
            },
            Err(e) => {
                set_last_error(&format!(
                    "Internal lock failure while clearing instances: {}",
                    e
                ));
                return CedarlingErrorCode::Internal;
            },
        };

        for instance in instances {
            instance.shut_down();
        }

        CedarlingErrorCode::Success
    }
}

fn runtime_ref() -> &'static CedarlingRuntime {
    &CEDARLING_RUNTIME
}

/// Force runtime initialization so startup failures surface at `cedarling_init`.
pub fn initialize_runtime() -> CedarlingErrorCode {
    clear_last_error();
    #[allow(clippy::explicit_auto_deref)]
    let _ = &*CEDARLING_RUNTIME; // explicit deref forces LazyLock initialization
    CedarlingErrorCode::Success
}

/// Shutdown and remove all runtime instances.
pub fn cleanup_runtime() -> CedarlingErrorCode {
    clear_last_error();
    runtime_ref().shutdown_and_clear_all_instances()
}

/// Create a new cedarling instance
pub fn create_instance(config_json: &str) -> CedarlingInstanceResult {
    clear_last_error();
    let bootstrap_config_raw = match serde_json::from_str::<BootstrapConfigRaw>(config_json) {
        Ok(config) => config,
        Err(e) => {
            let error_msg = format!("Failed to parse bootstrap config: {}", e);
            return CedarlingInstanceResult::error(CedarlingErrorCode::JsonError, &error_msg);
        },
    };

    let bootstrap_config = match BootstrapConfig::from_raw_config(&bootstrap_config_raw) {
        Ok(config) => config,
        Err(e) => {
            let error_msg = format!("Invalid bootstrap config: {}", e);
            return CedarlingInstanceResult::error(
                CedarlingErrorCode::ConfigurationError,
                &error_msg,
            );
        },
    };

    let runtime = runtime_ref();

    let instance = match blocking::Cedarling::new(&bootstrap_config) {
        Ok(instance) => instance,
        Err(e) => {
            let error_msg = format!("Failed to create Cedarling instance: {}", e);
            return CedarlingInstanceResult::error(CedarlingErrorCode::Internal, &error_msg);
        },
    };

    let instance_id = match runtime.add_instance(instance) {
        Ok(id) => id,
        Err(msg) => {
            return CedarlingInstanceResult::error(CedarlingErrorCode::Internal, &msg);
        },
    };

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
                return CedarlingInstanceResult::error(CedarlingErrorCode::JsonError, &error_msg);
            },
        },
        None => None,
    };

    let runtime = runtime_ref();

    let instance = match blocking::Cedarling::new_with_env(bootstrap_config_raw) {
        Ok(instance) => instance,
        Err(e) => {
            let error_msg = format!("Failed to create Cedarling instance: {}", e);
            return CedarlingInstanceResult::error(CedarlingErrorCode::Internal, &error_msg);
        },
    };

    let instance_id = match runtime.add_instance(instance) {
        Ok(id) => id,
        Err(msg) => {
            return CedarlingInstanceResult::error(CedarlingErrorCode::Internal, &msg);
        },
    };

    CedarlingInstanceResult::success(instance_id)
}

/// Drop a Cedarling Instance
pub fn drop_instance(instance_id: u64) -> CedarlingErrorCode {
    clear_last_error();
    runtime_ref().drop_instance(instance_id)
}

/// Authorize an unsigned request
pub fn authorize_unsigned(instance_id: u64, request_json: &str) -> CedarlingResult {
    clear_last_error();

    let runtime = runtime_ref();

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            return CedarlingResult::error(
                CedarlingErrorCode::InstanceNotFound,
                "Instance not found",
            );
        },
        Err((code, ref msg)) => {
            return CedarlingResult::error(code, msg.as_str());
        },
    };

    let request = match serde_json::from_str(request_json) {
        Ok(request) => request,
        Err(e) => {
            let error_msg = format!("Failed to parse request JSON: {}", e);
            return CedarlingResult::error(CedarlingErrorCode::JsonError, &error_msg);
        },
    };

    match instance.authorize_unsigned(request) {
        Ok(response) => match serde_json::to_string(&response) {
            Ok(json) => CedarlingResult::success(json),
            Err(e) => {
                let error_msg = format!("Failed to serialize response: {}", e);
                CedarlingResult::error(CedarlingErrorCode::Internal, &error_msg)
            },
        },
        Err(e) => {
            let error_msg = format!("Authorization failed: {}", e);
            CedarlingResult::error(CedarlingErrorCode::AuthorizationError, &error_msg)
        },
    }
}

/// Authorize a multi-issuer request
pub fn authorize_multi_issuer(instance_id: u64, request_json: &str) -> CedarlingResult {
    clear_last_error();

    let runtime = runtime_ref();

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            return CedarlingResult::error(
                CedarlingErrorCode::InstanceNotFound,
                "Instance not found",
            );
        },
        Err((code, ref msg)) => {
            return CedarlingResult::error(code, msg.as_str());
        },
    };

    let request = match serde_json::from_str(request_json) {
        Ok(request) => request,
        Err(e) => {
            let error_msg = format!("Failed to parse request JSON: {}", e);
            return CedarlingResult::error(CedarlingErrorCode::JsonError, &error_msg);
        },
    };

    match instance.authorize_multi_issuer(request) {
        Ok(response) => match serde_json::to_string(&response) {
            Ok(json) => CedarlingResult::success(json),
            Err(e) => {
                let error_msg = format!("Failed to serialize response: {}", e);
                CedarlingResult::error(CedarlingErrorCode::Internal, &error_msg)
            },
        },
        Err(e) => {
            let error_msg = format!("Authorization failed: {}", e);
            CedarlingResult::error(CedarlingErrorCode::AuthorizationError, &error_msg)
        },
    }
}

// Context Data API functions

/// Push context data
pub fn context_push(
    instance_id: u64,
    key: &str,
    value_json: &str,
    ttl_secs: i64,
) -> CedarlingResult {
    clear_last_error();

    let runtime = runtime_ref();

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            return CedarlingResult::error(
                CedarlingErrorCode::InstanceNotFound,
                "Instance not found",
            );
        },
        Err((code, ref msg)) => {
            return CedarlingResult::error(code, msg.as_str());
        },
    };

    let value: serde_json::Value = match serde_json::from_str(value_json) {
        Ok(v) => v,
        Err(e) => {
            let error_msg = format!("Failed to parse value JSON: {}", e);
            return CedarlingResult::error(CedarlingErrorCode::JsonError, &error_msg);
        },
    };

    let ttl = if ttl_secs > 0 {
        Some(Duration::from_secs(ttl_secs as u64))
    } else {
        None
    };

    match instance.push_data_ctx(key, value, ttl) {
        Ok(()) => CedarlingResult::success("{}".to_string()),
        Err(e) => cedarling_result_from_data_error(e),
    }
}

/// Get context data by key
pub fn context_get(instance_id: u64, key: &str) -> CedarlingResult {
    clear_last_error();

    let runtime = runtime_ref();

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            return CedarlingResult::error(
                CedarlingErrorCode::InstanceNotFound,
                "Instance not found",
            );
        },
        Err((code, ref msg)) => {
            return CedarlingResult::error(code, msg.as_str());
        },
    };

    match instance.get_data_ctx(key) {
        Ok(Some(value)) => match serde_json::to_string(&value) {
            Ok(json) => CedarlingResult::success(json),
            Err(e) => {
                let error_msg = format!("Failed to serialize value: {}", e);
                CedarlingResult::error(CedarlingErrorCode::Internal, &error_msg)
            },
        },
        Ok(None) => CedarlingResult::success("null".to_string()),
        Err(e) => cedarling_result_from_data_error(e),
    }
}

/// Get a single context [`cedarling::context_data_api::DataEntry`] (value plus metadata) by key.
pub fn context_get_entry(instance_id: u64, key: &str) -> CedarlingResult {
    clear_last_error();

    let runtime = runtime_ref();

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            return CedarlingResult::error(
                CedarlingErrorCode::InstanceNotFound,
                "Instance not found",
            );
        },
        Err((code, ref msg)) => {
            return CedarlingResult::error(code, msg.as_str());
        },
    };

    match instance.get_data_entry_ctx(key) {
        Ok(Some(entry)) => match serde_json::to_string(&entry) {
            Ok(json) => CedarlingResult::success(json),
            Err(e) => {
                let error_msg = format!("Failed to serialize data entry: {}", e);
                CedarlingResult::error(CedarlingErrorCode::Internal, &error_msg)
            },
        },
        Ok(None) => CedarlingResult::success("null".to_string()),
        Err(e) => cedarling_result_from_data_error(e),
    }
}

/// Remove context data by key
pub fn context_remove(instance_id: u64, key: &str) -> CedarlingResult {
    clear_last_error();

    let runtime = runtime_ref();

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            return CedarlingResult::error(
                CedarlingErrorCode::InstanceNotFound,
                "Instance not found",
            );
        },
        Err((code, ref msg)) => {
            return CedarlingResult::error(code, msg.as_str());
        },
    };

    match instance.remove_data_ctx(key) {
        Ok(removed) => {
            let result = serde_json::json!({"removed": removed});
            CedarlingResult::success(result.to_string())
        },
        Err(e) => cedarling_result_from_data_error(e),
    }
}

/// Clear all context data
pub fn context_clear(instance_id: u64) -> CedarlingResult {
    clear_last_error();

    let runtime = runtime_ref();

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            return CedarlingResult::error(
                CedarlingErrorCode::InstanceNotFound,
                "Instance not found",
            );
        },
        Err((code, ref msg)) => {
            return CedarlingResult::error(code, msg.as_str());
        },
    };

    match instance.clear_data_ctx() {
        Ok(()) => CedarlingResult::success("{}".to_string()),
        Err(e) => cedarling_result_from_data_error(e),
    }
}

/// List all context entries
pub fn context_list(instance_id: u64) -> CedarlingResult {
    clear_last_error();

    let runtime = runtime_ref();

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            return CedarlingResult::error(
                CedarlingErrorCode::InstanceNotFound,
                "Instance not found",
            );
        },
        Err((code, ref msg)) => {
            return CedarlingResult::error(code, msg.as_str());
        },
    };

    match instance.list_data_ctx() {
        Ok(entries) => match serde_json::to_string(&entries) {
            Ok(json) => CedarlingResult::success(json),
            Err(e) => {
                let error_msg = format!("Failed to serialize entries: {}", e);
                CedarlingResult::error(CedarlingErrorCode::Internal, &error_msg)
            },
        },
        Err(e) => cedarling_result_from_data_error(e),
    }
}

/// Get context stats
pub fn context_stats(instance_id: u64) -> CedarlingResult {
    clear_last_error();

    let runtime = runtime_ref();

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            return CedarlingResult::error(
                CedarlingErrorCode::InstanceNotFound,
                "Instance not found",
            );
        },
        Err((code, ref msg)) => {
            return CedarlingResult::error(code, msg.as_str());
        },
    };

    match instance.get_stats_ctx() {
        Ok(stats) => match serde_json::to_string(&stats) {
            Ok(json) => CedarlingResult::success(json),
            Err(e) => {
                let error_msg = format!("Failed to serialize stats: {}", e);
                CedarlingResult::error(CedarlingErrorCode::Internal, &error_msg)
            },
        },
        Err(e) => cedarling_result_from_data_error(e),
    }
}

/// Pop all logs from the instance
pub fn pop_logs(instance_id: u64) -> Result<CedarlingStringArray, CedarlingErrorCode> {
    clear_last_error();

    let runtime = runtime_ref();

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            set_last_error("Instance not found");
            return Err(CedarlingErrorCode::InstanceNotFound);
        },
        Err((code, ref msg)) => {
            set_last_error(msg);
            return Err(code);
        },
    };

    let logs = instance.pop_logs();

    let log_strings: Vec<String> = logs.iter().map(|log| log.to_string()).collect();

    CedarlingStringArray::try_new(log_strings)
}

/// Get a log by ID
pub fn get_log_by_id(instance_id: u64, log_id: &str) -> CedarlingResult {
    clear_last_error();

    let runtime = runtime_ref();

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            return CedarlingResult::error(
                CedarlingErrorCode::InstanceNotFound,
                "Instance not found",
            );
        },
        Err((code, ref msg)) => {
            return CedarlingResult::error(code, msg.as_str());
        },
    };

    match instance.get_log_by_id(log_id) {
        Some(log) => CedarlingResult::success(log.to_string()),
        None => CedarlingResult::error(CedarlingErrorCode::KeyNotFound, "Log not found"),
    }
}

/// Get all log IDs
pub fn get_log_ids(instance_id: u64) -> Result<CedarlingStringArray, CedarlingErrorCode> {
    clear_last_error();

    let runtime = runtime_ref();

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            set_last_error("Instance not found");
            return Err(CedarlingErrorCode::InstanceNotFound);
        },
        Err((code, ref msg)) => {
            set_last_error(msg);
            return Err(code);
        },
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
    let runtime = runtime_ref();

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            set_last_error("Instance not found");
            return Err(CedarlingErrorCode::InstanceNotFound);
        },
        Err((code, ref msg)) => {
            set_last_error(msg);
            return Err(code);
        },
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
    let runtime = runtime_ref();

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            set_last_error("Instance not found");
            return Err(CedarlingErrorCode::InstanceNotFound);
        },
        Err((code, ref msg)) => {
            set_last_error(msg);
            return Err(code);
        },
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
    let runtime = runtime_ref();
    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            set_last_error("Instance not found");
            return Err(CedarlingErrorCode::InstanceNotFound);
        },
        Err((code, ref msg)) => {
            set_last_error(msg);
            return Err(code);
        },
    };
    let logs = instance.get_logs_by_request_id_and_tag(request_id, tag);

    let log_strings: Vec<String> = logs.iter().map(|log| log.to_string()).collect();

    CedarlingStringArray::try_new(log_strings)
}

/// Check whether a trusted issuer was loaded by issuer identifier
pub fn is_trusted_issuer_loaded_by_name(
    instance_id: u64,
    issuer_id: &str,
) -> Result<bool, CedarlingErrorCode> {
    clear_last_error();
    let runtime = runtime_ref();
    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            set_last_error("Instance not found");
            return Err(CedarlingErrorCode::InstanceNotFound);
        },
        Err((code, ref msg)) => {
            set_last_error(msg);
            return Err(code);
        },
    };
    Ok(instance.is_trusted_issuer_loaded_by_name(issuer_id))
}

/// Check whether a trusted issuer was loaded by `iss` claim
pub fn is_trusted_issuer_loaded_by_iss(
    instance_id: u64,
    iss_claim: &str,
) -> Result<bool, CedarlingErrorCode> {
    clear_last_error();
    let runtime = runtime_ref();
    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            set_last_error("Instance not found");
            return Err(CedarlingErrorCode::InstanceNotFound);
        },
        Err((code, ref msg)) => {
            set_last_error(msg);
            return Err(code);
        },
    };
    Ok(instance.is_trusted_issuer_loaded_by_iss(iss_claim))
}

/// Get total trusted issuers discovered
pub fn total_issuers(instance_id: u64) -> Result<usize, CedarlingErrorCode> {
    clear_last_error();
    let runtime = runtime_ref();
    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            set_last_error("Instance not found");
            return Err(CedarlingErrorCode::InstanceNotFound);
        },
        Err((code, ref msg)) => {
            set_last_error(msg);
            return Err(code);
        },
    };
    Ok(instance.total_issuers())
}

/// Get number of trusted issuers loaded successfully
pub fn loaded_trusted_issuers_count(instance_id: u64) -> Result<usize, CedarlingErrorCode> {
    clear_last_error();
    let runtime = runtime_ref();
    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            set_last_error("Instance not found");
            return Err(CedarlingErrorCode::InstanceNotFound);
        },
        Err((code, ref msg)) => {
            set_last_error(msg);
            return Err(code);
        },
    };
    Ok(instance.loaded_trusted_issuers_count())
}

/// Get trusted issuer IDs loaded successfully
pub fn loaded_trusted_issuer_ids(
    instance_id: u64,
) -> Result<CedarlingStringArray, CedarlingErrorCode> {
    clear_last_error();
    let runtime = runtime_ref();
    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            set_last_error("Instance not found");
            return Err(CedarlingErrorCode::InstanceNotFound);
        },
        Err((code, ref msg)) => {
            set_last_error(msg);
            return Err(code);
        },
    };
    let ids: Vec<String> = instance.loaded_trusted_issuer_ids().into_iter().collect();
    CedarlingStringArray::try_new(ids)
}

/// Get trusted issuer IDs that failed to load
pub fn failed_trusted_issuer_ids(
    instance_id: u64,
) -> Result<CedarlingStringArray, CedarlingErrorCode> {
    clear_last_error();
    let runtime = runtime_ref();

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            set_last_error("Instance not found");
            return Err(CedarlingErrorCode::InstanceNotFound);
        },
        Err((code, ref msg)) => {
            set_last_error(msg);
            return Err(code);
        },
    };
    let ids: Vec<String> = instance.failed_trusted_issuer_ids().into_iter().collect();
    CedarlingStringArray::try_new(ids)
}

/// Shutdown an instance
pub fn shutdown_instance(instance_id: u64) -> CedarlingErrorCode {
    clear_last_error();

    let runtime = runtime_ref();

    let instance = match runtime.get_instance(instance_id) {
        Ok(Some(instance)) => instance,
        Ok(None) => {
            set_last_error("Instance not found");
            return CedarlingErrorCode::InstanceNotFound;
        },
        Err((code, ref msg)) => {
            set_last_error(msg);
            return code;
        },
    };

    instance.shut_down();

    // Remove from the runtime map so further API calls fail with InstanceNotFound.
    match runtime.drop_instance(instance_id) {
        CedarlingErrorCode::Success => CedarlingErrorCode::Success,
        CedarlingErrorCode::InstanceNotFound => {
            // drop_instance sets last_error internally; clear it since the
            // instance was already shut down and this is still a success.
            clear_last_error();
            CedarlingErrorCode::Success
        },
        code => code,
    }
}
