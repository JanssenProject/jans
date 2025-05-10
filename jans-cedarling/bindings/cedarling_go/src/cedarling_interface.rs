/// Result structure to hold json values of success or error
/// If string is empty then it means no error or success
#[derive(rust2go::R2G, Clone)]
pub struct Result {
    pub value: String,
    pub error: String,
}

impl Result {
    pub fn success<S: serde::Serialize>(value: S) -> Self {
        let value = match serde_json::to_string(&value) {
            Ok(value) => value,
            Err(err) => return Self::error(format!("Bindings serialization failed: {err}")), // Handle serialization errors)
        };
        Result {
            value,
            error: "".to_string(),
        }
    }

    pub fn error<E: std::fmt::Display>(err: E) -> Self {
        Result {
            value: "".to_string(),
            error: err.to_string(),
        }
    }
}

#[derive(rust2go::R2G, Clone)]
pub struct ResultInstance {
    pub instance_id: usize,
    pub error: String,
}

impl ResultInstance {
    pub fn new_instance(instance_id: usize) -> Self {
        ResultInstance {
            instance_id,
            error: "".to_string(),
        }
    }

    pub fn error<E: std::fmt::Display>(err: E) -> Self {
        ResultInstance {
            instance_id: 0,
            error: err.to_string(),
        }
    }
}

// implementation to this trait should work like RPC call
#[rust2go::g2r]
pub trait G2RCall {
    fn new_instance(boostrap_config_raw_json: String) -> ResultInstance;
    fn new_with_env_instance(boostrap_config_raw_json: String) -> ResultInstance;
    fn drop_instance(instance_id: usize);

    fn authorize(instance_id: usize, request_json: String) -> Result;
    fn authorize_unsigned(instance_id: usize, request_json: String) -> Result;

    fn pop_logs(instance_id: usize) -> Vec<String>;
    fn get_log_by_id(instance_id: usize, id: String) -> String;
    fn get_log_ids(instance_id: usize) -> Vec<String>;
    fn get_logs_by_tag(instance_id: usize, tag: String) -> Vec<String>;
    fn get_logs_by_request_id(instance_id: usize, request_id: String) -> Vec<String>;
    fn get_logs_by_request_id_and_tag(instance_id: usize, id: String, tag: String) -> Vec<String>;
    fn shut_down(instance_id: usize);
}
