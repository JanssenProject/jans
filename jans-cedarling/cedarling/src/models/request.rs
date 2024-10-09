/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use std::{collections::HashMap, str::FromStr};

use cedar_policy::{EntityId, EntityTypeName, EntityUid, ParseErrors};

/// Box to store authorization data
#[derive(Debug, serde::Deserialize)]
pub struct Request<'a> {
    /// Access token raw value
    pub access_token: &'a str,
    // pub id_token:  &'a str,
    // pub userinfo_token:  &'a str,
    /// cedar_policy action
    pub action: String,
    /// cedar_policy resource data
    pub resource: ResourceData,
    /// context to be used in cedar_policy
    pub context: serde_json::Value,
}

/// Cedar policy resource data
/// field represent EntityUid
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
