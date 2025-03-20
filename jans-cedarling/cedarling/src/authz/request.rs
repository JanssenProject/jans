// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashMap;

use serde::{Deserialize, Deserializer, Serialize, de};
use serde_json::Value;

/// Box to store authorization data
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Request {
    /// Contains the JWTs that will be used for the AuthZ request
    #[serde(default, deserialize_with = "deserialize_tokens")]
    pub tokens: HashMap<String, String>,
    /// cedar_policy action
    pub action: String,
    /// cedar_policy resource data
    pub resource: ResourceData,
    /// context to be used in cedar_policy
    pub context: Value,
}

/// Custom parser for an Option<String> which returns `None` if the string is empty.
fn deserialize_tokens<'de, D>(deserializer: D) -> Result<HashMap<String, String>, D::Error>
where
    D: Deserializer<'de>,
{
    let tokens = HashMap::<String, Value>::deserialize(deserializer)?;
    let (tokens, errs): (Vec<_>, Vec<_>) = tokens
        .into_iter()
        .filter_map(|(tkn_name, val)| match val {
            Value::Null => None,
            Value::String(token) => Some(Ok((tkn_name, token))),
            val => Some(Err((tkn_name, value_to_str(&val)))),
        })
        .partition(Result::is_ok);

    let tokens: HashMap<String, String> = if errs.is_empty() {
        tokens.into_iter().flatten().collect()
    } else {
        let err_msgs = errs
            .into_iter()
            .map(|e| e.unwrap_err())
            .map(|(tkn_name, got_type)| {
                format!(
                    "expected `{}` to be 'string' or 'null' but got '{}'",
                    tkn_name, got_type
                )
            })
            .collect::<Vec<_>>();
        return Err(de::Error::custom(format!(
            "failed to deserialize input tokens: {:?}",
            err_msgs
        )));
    };

    Ok(tokens)
}

fn value_to_str(value: &Value) -> &'static str {
    match value {
        Value::Null => "null",
        Value::Bool(_) => "bool",
        Value::Number(_) => "number",
        Value::String(_) => "string",
        Value::Array(_) => "array",
        Value::Object(_) => "object",
    }
}

/// Cedar policy resource data
/// fields represent EntityUid
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct ResourceData {
    /// entity type name
    #[serde(rename = "type")]
    pub resource_type: String,
    /// entity id
    pub id: String,
    /// entity attributes
    #[serde(flatten)]
    pub payload: HashMap<String, Value>,
}
