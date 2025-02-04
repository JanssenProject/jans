use cedarling::{self as core, BootstrapConfig, BootstrapConfigRaw, LogStorage};
mod request_wrapper;
use crate::request_wrapper::RequestWrapper;
use once_cell::sync::Lazy;
use serde_json::Value;
use tokio::runtime::Runtime;
uniffi::setup_scaffolding!();

// Lazy-initialized global Tokio runtime
static TOKIO_RUNTIME: Lazy<Runtime> =
    Lazy::new(|| Runtime::new().expect("Failed to create Tokio runtime"));

// Enum representing initialization errors
#[derive(Debug, thiserror::Error, uniffi::Enum)]
pub enum CedarlingError {
    #[error("Initialization Error: {error_msg}")]
    InitializationFailed { error_msg: String },
}

// Enum representing authorization errors
#[derive(Debug, thiserror::Error, uniffi::Enum)]
pub enum AuthorizeError {
    #[error("Authorization Error: {error_msg}")]
    AuthorizationFailed { error_msg: String },
}

// Enum representing logging errors
#[derive(Debug, thiserror::Error, uniffi::Enum)]
pub enum LogError {
    #[error("Logging Error: {error_msg}")]
    LoggingFailed { error_msg: String },
}

// Wrapper struct for core Cedarling instance
#[derive(uniffi::Object)]
pub struct Cedarling {
    inner: core::Cedarling,
}

// Struct to hold authorization result, compatible with iOS serialization
#[derive(Debug, serde::Serialize, uniffi::Record)]
pub struct AuthorizeResult {
    json_workload: String,
    json_person: String,
    decision: bool,
}

impl AuthorizeResult {
    // Constructor to create a new AuthorizeResult instance
    pub fn new(json_workload: &Value, json_person: &Value, decision: bool) -> Self {
        let json_workload_string =
            serde_json::to_string(&json_workload).unwrap_or_else(|_| "null".to_string());
        let json_person_string =
            serde_json::to_string(&json_person).unwrap_or_else(|_| "null".to_string());
        Self {
            json_workload: json_workload_string,
            json_person: json_person_string,
            decision,
        }
    }
    // Convert workload string back to JSON Value
    pub fn to_workload_json_value(&self) -> Value {
        serde_json::from_str(&self.json_workload).unwrap_or(Value::Null)
    }
    // Convert person string back to JSON Value
    pub fn to_person_json_value(&self) -> Value {
        serde_json::from_str(&self.json_person).unwrap_or(Value::Null)
    }
}

#[uniffi::export]
impl Cedarling {
    // Loads Cedarling instance from a JSON configuration string
    #[uniffi::constructor]
    pub fn load_from_json(config: String) -> Result<Self, CedarlingError> {
        TOKIO_RUNTIME
        //.stack_size(REQUIRED_STACK_SPACE)
        .block_on(async {
            // Parse the JSON string into `BootstrapConfigRaw`
            let config: BootstrapConfigRaw = serde_json::from_str(&config).map_err(|e| {
                CedarlingError::InitializationFailed {
                    error_msg: e.to_string(),
                }
            })?;

            // Convert to `BootstrapConfig`
            let config = BootstrapConfig::from_raw_config(&config).map_err(|e| {
                CedarlingError::InitializationFailed {
                    error_msg: e.to_string(),
                }
            })?;

            // Create a new `Cedarling` instance
            let cedarling = core::Cedarling::new(&config).await.map_err(|e| {
                CedarlingError::InitializationFailed {
                    error_msg: e.to_string(),
                }
            })?;

            Ok(Self { inner: cedarling })
        })
    }

    // Loads Cedarling instance from a configuration file
    #[uniffi::constructor]
    pub fn load_from_file(path: String) -> Result<Self, CedarlingError> {
        TOKIO_RUNTIME.block_on(async {
            let config: BootstrapConfig = cedarling::BootstrapConfig::load_from_file(&path)
                .map_err(|e| CedarlingError::InitializationFailed {
                    error_msg: String::from(
                        ["Failed to read the file:", e.to_string().as_str()].join(" "),
                    ),
                })?;

            let cedarling = core::Cedarling::new(&config).await.map_err(|e| {
                CedarlingError::InitializationFailed {
                    error_msg: String::from(e.to_string()),
                }
            })?;

            Ok(Self { inner: cedarling })
        })
    }

    // Handles authorization and returns a structured result
    #[uniffi::method]
    pub fn authorize(
        &self,
        access_token: String,
        id_token: String,
        userinfo_token: String,
        action: String,
        resource_type: String,
        resource_id: String,
        payload: String,
        context: String,
    ) -> Result<AuthorizeResult, AuthorizeError> {
        // Run the async operation within the Tokio runtime
        TOKIO_RUNTIME.block_on(async {
            let request = RequestWrapper::new(
                access_token,
                id_token,
                userinfo_token,
                action,
                resource_type,
                resource_id,
                payload,
                context,
            );

            let result: cedarling::AuthorizeResult =
                self.inner.authorize(request.inner).await.map_err(|e| {
                    AuthorizeError::AuthorizationFailed {
                        error_msg: String::from(e.to_string()),
                    }
                })?;
            let resVal = serde_json::to_value(result.clone()).unwrap();
            Ok(AuthorizeResult::new(
                resVal.get("workload").unwrap(),
                resVal.get("person").unwrap(),
                result.decision,
            ))
        })
    }

    // Retrieves logs and serializes them as JSON strings
    #[uniffi::method]
    pub fn pop_logs(&self) -> Result<Vec<String>, LogError> {
        //let result = Array::new();
        let mut result = Vec::new();

        for log in self.inner.pop_logs() {
            let log_str = serde_json::to_string(&log).map_err(|e| LogError::LoggingFailed {
                error_msg: e.to_string(),
            })?;
            result.push(log_str);
        }
        Ok(result)
    }
}

#[test]
fn test_authorize_success() {
    //reading bootstra.json and instantiate cedarling
    let cedarling = Cedarling::load_from_file(String::from(
        "../../bindings/cedarling_native_apps/test_files/bootstrap.json", 
    )); 
    //tokens
    let access_token ="eyJraWQiOiJjb25uZWN0X2Y5YTAwN2EyLTZkMGItNDkyYS05MGNkLWYwYzliMWMyYjVkYl9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJxenhuMVNjcmI5bFd0R3hWZWRNQ2t5LVFsX0lMc3BaYVFBNmZ5dVlrdHcwIiwiY29kZSI6IjNlMmEyMDEyLTA5OWMtNDY0Zi04OTBiLTQ0ODE2MGMyYWIyNSIsImlzcyI6Imh0dHBzOi8vYWNjb3VudC5nbHV1Lm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiJkN2Y3MWJlYS1jMzhkLTRjYWYtYTFiYS1lNDNjNzRhMTFhNjIiLCJhdWQiOiJkN2Y3MWJlYS1jMzhkLTRjYWYtYTFiYS1lNDNjNzRhMTFhNjIiLCJhY3IiOiJzaW1wbGVfcGFzc3dvcmRfYXV0aCIsIng1dCNTMjU2IjoiIiwibmJmIjoxNzMxOTUzMDMwLCJzY29wZSI6WyJyb2xlIiwib3BlbmlkIiwicHJvZmlsZSIsImVtYWlsIl0sImF1dGhfdGltZSI6MTczMTk1MzAyNywiZXhwIjoxNzMyMTIxNDYwLCJpYXQiOjE3MzE5NTMwMzAsImp0aSI6InVaVWgxaERVUW82UEZrQlBud3BHemciLCJ1c2VybmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsInN0YXR1cyI6eyJzdGF0dXNfbGlzdCI6eyJpZHgiOjMwNiwidXJpIjoiaHR0cHM6Ly9qYW5zLnRlc3QvamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdCJ9fX0.Pt-Y7F-hfde_WP7ZYwyvvSS11rKYQWGZXTzjH_aJKC5VPxzOjAXqI3Igr6gJLsP1aOd9WJvOPchflZYArctopXMWClbX_TxpmADqyCMsz78r4P450TaMKj-WKEa9cL5KtgnFa0fmhZ1ZWolkDTQ_M00Xr4EIvv4zf-92Wu5fOrdjmsIGFot0jt-12WxQlJFfs5qVZ9P-cDjxvQSrO1wbyKfHQ_txkl1GDATXsw5SIpC5wct92vjAVm5CJNuv_PE8dHAY-KfPTxOuDYBuWI5uA2Yjd1WUFyicbJgcmYzUSVt03xZ0kQX9dxKExwU2YnpDorfwebaAPO7G114Bkw208g";
    let id_token = "eyJraWQiOiJjb25uZWN0X2Y5YTAwN2EyLTZkMGItNDkyYS05MGNkLWYwYzliMWMyYjVkYl9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdF9oYXNoIjoiYnhhQ1QwWlFYYnY0c2J6alNEck5pQSIsInN1YiI6InF6eG4xU2NyYjlsV3RHeFZlZE1Da3ktUWxfSUxzcFphUUE2Znl1WWt0dzAiLCJhbXIiOltdLCJpc3MiOiJodHRwczovL2FjY291bnQuZ2x1dS5vcmciLCJub25jZSI6IjI1YjJiMTZiLTMyYTItNDJkNi04YThlLWU1ZmE5YWI4ODhjMCIsInNpZCI6IjZkNDQzNzM0LWI3YTItNGVkOC05ZDNhLTE2MDZkMmY5OTI0NCIsImphbnNPcGVuSURDb25uZWN0VmVyc2lvbiI6Im9wZW5pZGNvbm5lY3QtMS4wIiwiYXVkIjoiZDdmNzFiZWEtYzM4ZC00Y2FmLWExYmEtZTQzYzc0YTExYTYyIiwiYWNyIjoic2ltcGxlX3Bhc3N3b3JkX2F1dGgiLCJjX2hhc2giOiJWOGg0c085Tnp1TEthd1BPLTNETkxBIiwibmJmIjoxNzMxOTUzMDMwLCJhdXRoX3RpbWUiOjE3MzE5NTMwMjcsImV4cCI6MTczMTk1NjYzMCwiZ3JhbnQiOiJhdXRob3JpemF0aW9uX2NvZGUiLCJpYXQiOjE3MzE5NTMwMzAsImp0aSI6ImlqTFpPMW9vUnlXcmdJbjdjSWROeUEiLCJzdGF0dXMiOnsic3RhdHVzX2xpc3QiOnsiaWR4IjozMDcsInVyaSI6Imh0dHBzOi8vamFucy50ZXN0L2phbnMtYXV0aC9yZXN0djEvc3RhdHVzX2xpc3QifX19.Nw7MRaJ5LtDak_LdEjrICgVOxDwd1p1I8WxD7IYw0_mKlIJ-J_78rGPski9p3L5ZNCpXiHtVbnhc4lJdmbh-y6mrD3_EY_AmjK50xpuf6YuUuNVtFENCSkj_irPLkIDG65HeZherWsvH0hUn4FVGv8Sw9fjny9Doi-HGHnKg9Qvphqre1U8hCphCVLQlzXAXmBkbPOC8tDwId5yigBKXP50cdqDcT-bjXf9leIdGgq0jxb57kYaFSElprLN9nUygM4RNCn9mtmo1l4IsdTlvvUb3OMAMQkRLfMkiKBjjeSF3819mYRLb3AUBaFH16ZdHFBzTSB6oA22TYpUqOLihMg";
    let userinfo_token = "eyJraWQiOiJjb25uZWN0X2Y5YTAwN2EyLTZkMGItNDkyYS05MGNkLWYwYzliMWMyYjVkYl9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.eyJzdWIiOiJxenhuMVNjcmI5bFd0R3hWZWRNQ2t5LVFsX0lMc3BaYVFBNmZ5dVlrdHcwIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsInJvbGUiOlsiQ2FzYUFkbWluIl0sImlzcyI6Imh0dHBzOi8vYWNjb3VudC5nbHV1Lm9yZyIsImdpdmVuX25hbWUiOiJBZG1pbiIsIm1pZGRsZV9uYW1lIjoiQWRtaW4iLCJpbnVtIjoiYTZhNzAzMDEtYWY0OS00OTAxLTk2ODctMGJjZGNmNGUzNGZhIiwiY2xpZW50X2lkIjoiZDdmNzFiZWEtYzM4ZC00Y2FmLWExYmEtZTQzYzc0YTExYTYyIiwiYXVkIjoiZDdmNzFiZWEtYzM4ZC00Y2FmLWExYmEtZTQzYzc0YTExYTYyIiwidXBkYXRlZF9hdCI6MTczMTY5ODEzNSwibmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsIm5pY2tuYW1lIjoiQWRtaW4iLCJmYW1pbHlfbmFtZSI6IlVzZXIiLCJqdGkiOiJPSW4zZzFTUFNEU0tBWUR6RU5Wb3VnIiwiZW1haWwiOiJhZG1pbkBqYW5zLnRlc3QiLCJqYW5zQWRtaW5VSVJvbGUiOlsiYXBpLWFkbWluIl0sICJ1c2VyX25hbWUiOiAiYWRtaW4iLCJleHAiOiAxNzI0OTQ1OTc4LAogICJpYXQiOiAxNzI0ODMyMjU5LCAiYWNyIjogInBhc3MiLCAiYW1yIjogWyJwYXNzIl19.";
    //execute authz
    let result = cedarling.unwrap().authorize(
        access_token.to_string(),
        id_token.to_string(),
        userinfo_token.to_string(),
        r#"Jans::Action::"Update""#.to_string(),
        "Jans::Issue".to_string(),
        "some_id".to_string(),
        r#"
        {
            "app_id": "admin_ui_id",
                  "id": "admin_ui_id",
                  "name": "My App",
                  "permission": "view_clients",
                  "sub": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0",
                  "type": "Jans::Issue"
        }
        "#
        .to_string(),
        "{}".to_string(),
    );
    print!("{:?}", result);

    assert!(result.is_ok());
}
