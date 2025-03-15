// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashMap;

/// Box to store authorization data
#[derive(Debug, Clone, serde::Serialize, serde::Deserialize)]
pub struct Request {
    /// Contains the JWTs that will be used for the AuthZ request
    #[serde(default)]
    pub tokens: HashMap<String, String>,
    /// cedar_policy action
    pub action: String,
    /// cedar_policy resource data
    pub resource: EntityData,
    /// context to be used in cedar_policy
    pub context: serde_json::Value,
}

/// Box to store authorization data, with any additional principals
#[derive(Debug, Clone, serde::Serialize, serde::Deserialize)]
pub struct RequestUnverified {
    /// Contains the JWTs that will be used for the AuthZ request
    pub principals: Vec<EntityData>,
    /// cedar_policy action
    pub action: String,
    /// cedar_policy resource data
    pub resource: EntityData,
    /// context to be used in cedar_policy
    pub context: serde_json::Value,
}

/// Cedar policy entity data
/// fields represent EntityUid
#[derive(serde::Serialize, serde::Deserialize, Debug, Clone)]
pub struct EntityData {
    /// entity type name
    #[serde(rename = "type")]
    pub resource_type: String,
    /// entity id
    pub id: String,

    /// entity attributes
    #[serde(flatten)]
    pub payload: HashMap<String, serde_json::Value>,
}
