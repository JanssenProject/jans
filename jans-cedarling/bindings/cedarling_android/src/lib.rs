
use cedarling::{self as core, BootstrapConfig, BootstrapConfigRaw, Request, Tokens};
use serde_json::Error;
mod request_wrapper;
use crate::request_wrapper::RequestWrapper;
use once_cell::sync::Lazy;
use tokio::runtime::Runtime;
use serde_json::Value;
uniffi::setup_scaffolding!();


static TOKIO_RUNTIME: Lazy<Runtime> =
    Lazy::new(|| Runtime::new().expect("Failed to create Tokio runtime"));

#[derive(Debug, thiserror::Error, uniffi::Enum)]
pub enum CedarlingError {
    #[error("Initialization Error: {error_msg}")]
    InitializationFailed { error_msg: String},
}

#[derive(Debug, thiserror::Error, uniffi::Enum)]
pub enum AuthorizeError {
    #[error("Authorization Error: {error_msg}")]
    AuthorizationFailed { error_msg: String},
}

#[derive( uniffi::Object)]
pub struct Cedarling {
    inner: core::Cedarling,
}


#[derive(Debug, serde::Serialize, uniffi::Object)]
pub struct AuthorizeResult {
    inner: core::AuthorizeResult,
}

//compatible to read in IOS
#[derive(Debug, serde::Serialize, uniffi::Record)]
pub struct AuthorizeResultWithJsonString {
    inner: String,
}

impl AuthorizeResultWithJsonString {
    pub fn new(json_value: Value) -> Self {
        let json_string = serde_json::to_string(&json_value).unwrap_or_else(|_| "null".to_string());
        Self { inner: json_string }
    }

    pub fn to_json_value(&self) -> Value {
        serde_json::from_str(&self.inner).unwrap_or(Value::Null)
    }
}

#[uniffi::export]
impl Cedarling {
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
    //
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

            let result = self
            .inner
            .authorize(request.inner)
            .await
            .map_err(|e| AuthorizeError::AuthorizationFailed{ error_msg: String::from(e.to_string()) })?;

            Ok(AuthorizeResult { inner: result })
        })
    }

    #[uniffi::method]
    pub fn authorize_string_result(
        &self,
        access_token: String,
        id_token: String,
        userinfo_token: String,
        action: String,
        resource_type: String,
        resource_id: String,
        payload: String,
        context: String,
    ) -> Result<AuthorizeResultWithJsonString, AuthorizeError> {

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

            let result: cedarling::AuthorizeResult = self
            .inner
            .authorize(request.inner)
            .await
            .map_err(|e| AuthorizeError::AuthorizationFailed{ error_msg: String::from(e.to_string()) })?;

            Ok(AuthorizeResultWithJsonString::new(serde_json::to_value(result).unwrap()))
        })
    }
    
}

#[test]
fn test_authorize_success() {
    // Mock a Cedarling instance with a valid core::Cedarling
    let cedarling = Cedarling::load_from_file(String::from("/Users/arnabdutta/Projects/jans2/jans-cedarling/bindings/cedarling_android/src/bootstrap.json")); // Replace with actual mock logic

    let result = cedarling.unwrap().authorize_string_result(
        "eyJraWQiOiJjb25uZWN0X2Y5YTAwN2EyLTZkMGItNDkyYS05MGNkLWYwYzliMWMyYjVkYl9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJxenhuMVNjcmI5bFd0R3hWZWRNQ2t5LVFsX0lMc3BaYVFBNmZ5dVlrdHcwIiwiY29kZSI6IjNlMmEyMDEyLTA5OWMtNDY0Zi04OTBiLTQ0ODE2MGMyYWIyNSIsImlzcyI6Imh0dHBzOi8vYWNjb3VudC5nbHV1Lm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiJkN2Y3MWJlYS1jMzhkLTRjYWYtYTFiYS1lNDNjNzRhMTFhNjIiLCJhdWQiOiJkN2Y3MWJlYS1jMzhkLTRjYWYtYTFiYS1lNDNjNzRhMTFhNjIiLCJhY3IiOiJzaW1wbGVfcGFzc3dvcmRfYXV0aCIsIng1dCNTMjU2IjoiIiwibmJmIjoxNzMxOTUzMDMwLCJzY29wZSI6WyJyb2xlIiwib3BlbmlkIiwicHJvZmlsZSIsImVtYWlsIl0sImF1dGhfdGltZSI6MTczMTk1MzAyNywiZXhwIjoxNzMyMTIxNDYwLCJpYXQiOjE3MzE5NTMwMzAsImp0aSI6InVaVWgxaERVUW82UEZrQlBud3BHemciLCJ1c2VybmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsInN0YXR1cyI6eyJzdGF0dXNfbGlzdCI6eyJpZHgiOjMwNiwidXJpIjoiaHR0cHM6Ly9qYW5zLnRlc3QvamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdCJ9fX0.Pt-Y7F-hfde_WP7ZYwyvvSS11rKYQWGZXTzjH_aJKC5VPxzOjAXqI3Igr6gJLsP1aOd9WJvOPchflZYArctopXMWClbX_TxpmADqyCMsz78r4P450TaMKj-WKEa9cL5KtgnFa0fmhZ1ZWolkDTQ_M00Xr4EIvv4zf-92Wu5fOrdjmsIGFot0jt-12WxQlJFfs5qVZ9P-cDjxvQSrO1wbyKfHQ_txkl1GDATXsw5SIpC5wct92vjAVm5CJNuv_PE8dHAY-KfPTxOuDYBuWI5uA2Yjd1WUFyicbJgcmYzUSVt03xZ0kQX9dxKExwU2YnpDorfwebaAPO7G114Bkw208g".to_string(),
        "eyJraWQiOiJjb25uZWN0X2Y5YTAwN2EyLTZkMGItNDkyYS05MGNkLWYwYzliMWMyYjVkYl9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdF9oYXNoIjoiYnhhQ1QwWlFYYnY0c2J6alNEck5pQSIsInN1YiI6InF6eG4xU2NyYjlsV3RHeFZlZE1Da3ktUWxfSUxzcFphUUE2Znl1WWt0dzAiLCJhbXIiOltdLCJpc3MiOiJodHRwczovL2FjY291bnQuZ2x1dS5vcmciLCJub25jZSI6IjI1YjJiMTZiLTMyYTItNDJkNi04YThlLWU1ZmE5YWI4ODhjMCIsInNpZCI6IjZkNDQzNzM0LWI3YTItNGVkOC05ZDNhLTE2MDZkMmY5OTI0NCIsImphbnNPcGVuSURDb25uZWN0VmVyc2lvbiI6Im9wZW5pZGNvbm5lY3QtMS4wIiwiYXVkIjoiZDdmNzFiZWEtYzM4ZC00Y2FmLWExYmEtZTQzYzc0YTExYTYyIiwiYWNyIjoic2ltcGxlX3Bhc3N3b3JkX2F1dGgiLCJjX2hhc2giOiJWOGg0c085Tnp1TEthd1BPLTNETkxBIiwibmJmIjoxNzMxOTUzMDMwLCJhdXRoX3RpbWUiOjE3MzE5NTMwMjcsImV4cCI6MTczMTk1NjYzMCwiZ3JhbnQiOiJhdXRob3JpemF0aW9uX2NvZGUiLCJpYXQiOjE3MzE5NTMwMzAsImp0aSI6ImlqTFpPMW9vUnlXcmdJbjdjSWROeUEiLCJzdGF0dXMiOnsic3RhdHVzX2xpc3QiOnsiaWR4IjozMDcsInVyaSI6Imh0dHBzOi8vamFucy50ZXN0L2phbnMtYXV0aC9yZXN0djEvc3RhdHVzX2xpc3QifX19.Nw7MRaJ5LtDak_LdEjrICgVOxDwd1p1I8WxD7IYw0_mKlIJ-J_78rGPski9p3L5ZNCpXiHtVbnhc4lJdmbh-y6mrD3_EY_AmjK50xpuf6YuUuNVtFENCSkj_irPLkIDG65HeZherWsvH0hUn4FVGv8Sw9fjny9Doi-HGHnKg9Qvphqre1U8hCphCVLQlzXAXmBkbPOC8tDwId5yigBKXP50cdqDcT-bjXf9leIdGgq0jxb57kYaFSElprLN9nUygM4RNCn9mtmo1l4IsdTlvvUb3OMAMQkRLfMkiKBjjeSF3819mYRLb3AUBaFH16ZdHFBzTSB6oA22TYpUqOLihMg".to_string(),
        "eyJraWQiOiJjb25uZWN0X2Y5YTAwN2EyLTZkMGItNDkyYS05MGNkLWYwYzliMWMyYjVkYl9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.eyJzdWIiOiJxenhuMVNjcmI5bFd0R3hWZWRNQ2t5LVFsX0lMc3BaYVFBNmZ5dVlrdHcwIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsInJvbGUiOlsiQ2FzYUFkbWluIl0sImlzcyI6Imh0dHBzOi8vYWNjb3VudC5nbHV1Lm9yZyIsImdpdmVuX25hbWUiOiJBZG1pbiIsIm1pZGRsZV9uYW1lIjoiQWRtaW4iLCJpbnVtIjoiYTZhNzAzMDEtYWY0OS00OTAxLTk2ODctMGJjZGNmNGUzNGZhIiwiY2xpZW50X2lkIjoiZDdmNzFiZWEtYzM4ZC00Y2FmLWExYmEtZTQzYzc0YTExYTYyIiwiYXVkIjoiZDdmNzFiZWEtYzM4ZC00Y2FmLWExYmEtZTQzYzc0YTExYTYyIiwidXBkYXRlZF9hdCI6MTczMTY5ODEzNSwibmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsIm5pY2tuYW1lIjoiQWRtaW4iLCJmYW1pbHlfbmFtZSI6IlVzZXIiLCJqdGkiOiJPSW4zZzFTUFNEU0tBWUR6RU5Wb3VnIiwiZW1haWwiOiJhZG1pbkBqYW5zLnRlc3QiLCJqYW5zQWRtaW5VSVJvbGUiOlsiYXBpLWFkbWluIl0sICJ1c2VyX25hbWUiOiAiYWRtaW4ifQ.".to_string(),
        r#"Jans::Action::"Update""#.to_string(),
        "Jans::Issue".to_string(),
        "some_id".to_string(),
        r#"
        {
            "org_id": "some_long_id",
            "country": "US",
            "sub": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0"
        }
        "#.to_string(),
        "{}".to_string(),
    );
    print!("{:?}", result);
    
    assert!(result.is_ok());
}