#![cfg(not(target_arch = "wasm32"))]

use std::{
    collections::HashMap,
    sync::{
        Arc, LazyLock, Mutex,
        atomic::{AtomicUsize, Ordering},
    },
};
use tokio::runtime::Runtime;

use cedarling::{self as base, BootstrapConfig, BootstrapConfigRaw, LogStorage};

mod cedarling_interface;
use cedarling_interface::{Result, ResultInstance};

static BINDINGS_RUNTIME: LazyLock<BindingsRuntime> = LazyLock::new(|| {
    let rt = Runtime::new().expect("Failed to create Tokio runtime");
    BindingsRuntime {
        runtime: Arc::new(rt),
        instances: Mutex::new(HashMap::new()),
    }
});

// get unique instance id for each new Cedarling instance
fn get_instance_id() -> usize {
    static INSTANCE_ID: AtomicUsize = AtomicUsize::new(0);
    INSTANCE_ID.fetch_add(1, Ordering::SeqCst)
}

/// Get Cedarling instance by id
/// If the instance does not exist, return an error
macro_rules! get_instance {
    ($instance_id:expr) => {
        match BINDINGS_RUNTIME.get_instance($instance_id) {
            Some(instance) => instance,
            None => return Result::error("Instance not found"),
        }
    };
}

/// Get Cedarling instance by id
/// If the instance does not exist, return default value value
macro_rules! get_instance_or_return {
    ($instance_id:expr) => {
        match BINDINGS_RUNTIME.get_instance($instance_id) {
            Some(instance) => instance,
            None => return Default::default(),
        }
    };
}

/// Convert JSON string to request
macro_rules! from_json_str {
    ($json_str:expr) => {
        match serde_json::from_str(&$json_str) {
            Ok(request) => request,
            Err(e) => return Result::error(e),
        }
    };
}

/// Execute a future in the runtime and return the result
macro_rules! execute_in_runtime {
    ($future:expr ) => {
        match BINDINGS_RUNTIME.runtime.block_on($future) {
            Ok(response) => Result::success(response),
            Err(e) => Result::error(e),
        }
    };
}

struct BindingsRuntime {
    runtime: Arc<Runtime>,
    instances: Mutex<HashMap<usize, Arc<base::Cedarling>>>,
}

impl BindingsRuntime {
    fn add_instance(&self, instance: base::Cedarling) -> usize {
        let instance_id = get_instance_id();
        self.instances
            .lock()
            .unwrap()
            .insert(instance_id, Arc::new(instance));
        instance_id
    }

    fn drop_instance(&self, instance_id: usize) {
        self.instances.lock().unwrap().remove(&instance_id);
    }

    fn get_instance(&self, instance_id: usize) -> Option<Arc<base::Cedarling>> {
        self.instances.lock().unwrap().get(&instance_id).cloned()
    }
}

// From rust2go doc example:
// You can do it within lib.rs or other places, but it is not recommended in the same file the traits and structs defined.
impl cedarling_interface::G2RCall for cedarling_interface::G2RCallImpl {
    fn new_instance(boostrap_config_raw_json: String) -> ResultInstance {
        let bootstrap_config_raw =
            match serde_json::from_str::<BootstrapConfigRaw>(&boostrap_config_raw_json) {
                Ok(config) => config,
                Err(e) => return ResultInstance::error(e),
            };

        let bootstrap_config = match BootstrapConfig::from_raw_config(&bootstrap_config_raw) {
            Ok(config) => config,
            Err(e) => return ResultInstance::error(e),
        };

        let instance = match BINDINGS_RUNTIME
            .runtime
            .block_on(base::Cedarling::new(&bootstrap_config))
        {
            Ok(instance) => instance,
            Err(e) => return ResultInstance::error(e),
        };
        let instance_id = BINDINGS_RUNTIME.add_instance(instance);

        ResultInstance::new_instance(instance_id)
    }

    fn new_with_env_instance(boostrap_config_raw_json: String) -> ResultInstance {
        let bootstrap_config_raw =
            match serde_json::from_str::<Option<BootstrapConfigRaw>>(&boostrap_config_raw_json) {
                Ok(config) => config,
                Err(e) => return ResultInstance::error(e),
            };

        let instance = match BINDINGS_RUNTIME
            .runtime
            .block_on(base::Cedarling::new_with_env(bootstrap_config_raw))
        {
            Ok(instance) => instance,
            Err(e) => return ResultInstance::error(e),
        };
        let instance_id = BINDINGS_RUNTIME.add_instance(instance);

        ResultInstance::new_instance(instance_id)
    }

    fn drop_instance(instance_id: usize) {
        BINDINGS_RUNTIME.drop_instance(instance_id);
    }

    fn authorize(instance_id: usize, request_json: String) -> Result {
        let request = from_json_str!(request_json);
        let instance = get_instance!(instance_id);
        execute_in_runtime!(instance.authorize(request))
    }

    fn authorize_unsigned(instance_id: usize, request_json: String) -> Result {
        let request = from_json_str!(request_json);
        let instance = get_instance!(instance_id);
        execute_in_runtime!(instance.authorize_unsigned(request))
    }

    fn pop_logs(instance_id: usize) -> Vec<String> {
        let instance = get_instance_or_return!(instance_id);
        instance.pop_logs().iter().map(|v| v.to_string()).collect()
    }

    fn get_log_by_id(instance_id: usize, id: String) -> String {
        let instance = get_instance_or_return!(instance_id);
        instance
            .get_log_by_id(&id)
            .iter()
            .map(|v| v.to_string())
            .collect()
    }

    fn get_log_ids(instance_id: usize) -> Vec<String> {
        let instance = get_instance_or_return!(instance_id);
        instance
            .get_log_ids()
            .iter()
            .map(|v| v.to_string())
            .collect()
    }

    fn get_logs_by_tag(instance_id: usize, tag: String) -> Vec<String> {
        let instance = get_instance_or_return!(instance_id);
        instance
            .get_logs_by_tag(&tag)
            .iter()
            .map(|v| v.to_string())
            .collect()
    }

    fn get_logs_by_request_id(instance_id: usize, request_id: String) -> Vec<String> {
        let instance = get_instance_or_return!(instance_id);
        instance
            .get_logs_by_request_id(&request_id)
            .iter()
            .map(|v| v.to_string())
            .collect()
    }

    fn get_logs_by_request_id_and_tag(instance_id: usize, id: String, tag: String) -> Vec<String> {
        let instance = get_instance_or_return!(instance_id);
        instance
            .get_logs_by_request_id_and_tag(&id, &tag)
            .iter()
            .map(|v| v.to_string())
            .collect()
    }

    fn shut_down(instance_id: usize) {
        let instance = get_instance_or_return!(instance_id);
        BINDINGS_RUNTIME.runtime.block_on(instance.shut_down());
    }
}
