// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashMap;

use serde::{Deserialize, Serialize};
use serde_json::Value;

#[derive(Debug, thiserror::Error)]
pub(super) enum MappingValidationError {
    #[error("missing required field")]
    MissingField,
}

#[derive(Debug, Deserialize)]
pub(super) struct CedarlingLogEntry {
    pub timestamp: String,
    pub log_kind: String,
    pub decision: String,
    pub action: String,
    pub level: Option<String>,
    // Cedarling emits principal as an array of entity strings
    #[serde(default)]
    pub principal: Vec<String>,
    pub resource: String,
    pub application_id: String,
    pub pdp_id: String,
    // Catch everything else for context_information
    #[serde(flatten)]
    pub extra: HashMap<String, Value>,
}

/// Serializes into the lock server's expected format
#[derive(Debug, Serialize)]
pub(super) struct LockServerLogEntry {
    pub creation_date: String,
    pub event_time: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub service: Option<String>,
    pub node_name: String,
    pub event_type: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub severity_level: Option<String>,
    pub action: String,
    pub decision_result: String,
    pub requested_resource: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub principal_id: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_id: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_information: Option<Value>,
}

impl TryFrom<CedarlingLogEntry> for LockServerLogEntry {
    type Error = MappingValidationError;

    fn try_from(value: CedarlingLogEntry) -> Result<Self, Self::Error> {
        if value.log_kind.is_empty()
            || value.decision.is_empty()
            || value.action.is_empty()
            || value.resource.is_empty()
        {
            return Err(MappingValidationError::MissingField);
        }

        let mut extra = value.extra;
        let client_id = extra
            .remove("lock_client_id")
            .and_then(|v| v.as_str().map(String::from));
        let context_information = if extra.is_empty() {
            None
        } else {
            Some(Value::Object(extra.into_iter().collect()))
        };

        Ok(Self {
            creation_date: value.timestamp.clone(),
            event_time: value.timestamp,
            service: Some(value.application_id),
            node_name: value.pdp_id,
            event_type: value.log_kind,
            severity_level: value.level,
            action: value.action,
            decision_result: value.decision,
            requested_resource: value.resource,
            principal_id: if value.principal.is_empty() {
                None
            } else {
                Some(value.principal.join(", "))
            },
            client_id,
            context_information,
        })
    }
}
