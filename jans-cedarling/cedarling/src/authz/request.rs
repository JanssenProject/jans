// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashMap;
use std::str::FromStr;

use cedar_policy::{EntityId, EntityTypeName, EntityUid, ParseErrors};

/// Box to store authorization data
#[derive(Debug, Clone, serde::Deserialize)]
pub struct Request {
    /// Contains the JWTs that will be used for the AuthZ request
    pub tokens: Tokens,
    /// cedar_policy action
    pub action: String,
    /// cedar_policy resource data
    pub resource: ResourceData,
    /// context to be used in cedar_policy
    pub context: serde_json::Value,
}

/// Contains the JWTs that will be used for the AuthZ request
#[derive(Debug, Clone, serde::Deserialize)]
pub struct Tokens {
    /// Access token raw value
    #[serde(default)]
    pub access_token: Option<String>,
    /// Id Token raw value
    #[serde(default)]
    pub id_token: Option<String>,
    /// Userinfo Token raw value
    #[serde(default)]
    pub userinfo_token: Option<String>,
}

/// Cedar policy resource data
/// fields represent EntityUid
#[derive(serde::Deserialize, Debug, Clone)]
pub struct ResourceData {
    /// entity type name
    #[serde(rename = "type")]
    pub resource_type: String,
    /// entity id
    pub id: String,

    /// entity attributes
    #[serde(flatten)]
    pub payload: HashMap<String, serde_json::Value>,
}

impl ResourceData {
    pub(crate) fn entity_uid(&self) -> Result<EntityUid, ParseErrors> {
        Ok(EntityUid::from_type_name_and_id(
            EntityTypeName::from_str(&self.resource_type)?,
            EntityId::new(&self.id),
        ))
    }
}
