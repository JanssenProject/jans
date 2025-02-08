use cedarling::{self as core, Request};
use serde_json::Value;
use std::collections::HashMap;

// Enum representing request errors
#[derive(Debug, thiserror::Error, uniffi::Enum)]
pub enum RequestError {
    #[error("Authorization request creation error: {error_msg}")]
    RequestParsingFailed { error_msg: String },
}

#[derive(Debug, serde::Deserialize, uniffi::Object)]
pub struct RequestWrapper {
    pub inner: core::Request,
}

#[uniffi::export]
impl RequestWrapper {
    /// Constructor for `RequestWrapper`
    #[uniffi::constructor]
    pub fn new(
        tokens: HashMap<String, String>,
        action: String,
        resource_type: String,
        resource_id: String,
        payload: String,
        context: String,
    ) -> Result<Self, RequestError> {
        // Validate tokens: ensure all values are non-empty after trimming
        let mut validated_tokens = HashMap::new();
        for (key, value) in tokens.iter() {
            let trimmed_value = value.trim();
            if trimmed_value.is_empty() {
                return Err(RequestError::RequestParsingFailed {
                    error_msg: format!("Token '{}' must not be empty.", key),
                });
            }
            validated_tokens.insert(key.clone(), trimmed_value.to_string());
        }
        // Parse context
        let parsed_context: Value =
            serde_json::from_str(&context).map_err(|e| RequestError::RequestParsingFailed {
                error_msg: format!("Invalid JSON for context: {}", e.to_string()),
            })?;
        // Parse payload
        let parsed_payload: HashMap<String, Value> =
            serde_json::from_str(&payload).map_err(|e| RequestError::RequestParsingFailed {
                error_msg: format!("Invalid JSON for payload: {}", e.to_string()),
            })?;

        let resource: core::ResourceData = core::ResourceData {
            resource_type,
            id: resource_id,
            payload: parsed_payload,
        };

        let inner = Request {
            tokens: validated_tokens,
            action,
            resource,
            context: parsed_context,
        };

        Ok(RequestWrapper { inner })
    }
}
