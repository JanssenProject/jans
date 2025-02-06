use cedarling::{self as core, Request, Tokens};
use std::collections::HashMap;
use serde_json::Value;

#[derive(Debug, serde::Deserialize, uniffi::Object)]
pub struct RequestWrapper {
    pub inner: core::Request,
}

#[uniffi::export]
impl RequestWrapper {
    /// Constructor for `RequestWrapper`
    #[uniffi::constructor]
    pub fn new (
        access_token: String,
        id_token: String,
        userinfo_token: String,
        action: String,
        resource_type: String,
        resource_id: String,
        payload: String,
        context: String,
    ) -> Self {
        let parsed_context: Value = serde_json::from_str(&context).expect("Invalid JSON");
        let parsed_payload: HashMap<String, Value> = serde_json::from_str(&payload).expect("Invalid JSON input");

        let resource:  core::ResourceData = core::ResourceData {
            resource_type,
            id: resource_id,
            payload: parsed_payload,
        };

        let access_token = if access_token.is_empty() { None } else { Some(access_token) };
        let id_token = if id_token.is_empty() { None } else { Some(id_token) };
        let userinfo_token = if userinfo_token.is_empty() { None } else { Some(userinfo_token) };

        let tokens = HashMap::from([
            ("access_token".to_string(), access_token.unwrap()),
            ("id_token".to_string(), id_token.unwrap()),
            ("userinfo_token".to_string(), userinfo_token.unwrap()),
        ]);

        let inner = Request {
            tokens,
            action,
            resource,
            context: parsed_context,
        };

        RequestWrapper { inner }
    }
}