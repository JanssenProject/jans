// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::BootstrapConfigRaw;
use derive_more::Deref;
use serde::{Deserialize, Serialize};

const DEFAULT_ROLE_ENTITY_NAME: &str = "Jans::Role";
const DEFAULT_UNSIGNED_ROLE_ID_SRC: &str = "role";

pub(crate) const DEFAULT_ACCESS_TKN_ENTITY_NAME: &str = "Jans::Access_token";
pub(crate) const DEFAULT_ID_TKN_ENTITY_NAME: &str = "Jans::Id_token";
pub(crate) const DEFAULT_USERINFO_TKN_ENTITY_NAME: &str = "Jans::Userinfo_token";
pub(crate) const DEFAULT_ENTITY_TYPE_NAME: &str = "Token";

/// Bootstrap Configurations for the JWT to Cedar entity mappings
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct EntityBuilderConfig {
    /// The name of the Cedar Role entity type
    pub role_entity_name: String,
    /// The attribute to use when creating Role entities in the unsigned interface
    pub unsigned_role_id_src: UnsignedRoleIdSrc,
}

/// Unsigned role ID source
#[derive(Debug, Clone, PartialEq, Deref, Serialize, Deserialize)]
pub struct UnsignedRoleIdSrc(pub String);

impl Default for UnsignedRoleIdSrc {
    fn default() -> Self {
        Self(DEFAULT_UNSIGNED_ROLE_ID_SRC.to_string())
    }
}

impl Default for EntityBuilderConfig {
    fn default() -> Self {
        Self {
            role_entity_name: DEFAULT_ROLE_ENTITY_NAME.to_string(),
            unsigned_role_id_src: UnsignedRoleIdSrc(DEFAULT_UNSIGNED_ROLE_ID_SRC.to_string()),
        }
    }
}

impl From<&BootstrapConfigRaw> for EntityBuilderConfig {
    fn from(raw: &BootstrapConfigRaw) -> Self {
        Self {
            role_entity_name: raw
                .mapping_role
                .clone()
                .unwrap_or_else(|| DEFAULT_ROLE_ENTITY_NAME.to_string()),
            unsigned_role_id_src: raw.unsigned_role_id_src.clone(),
        }
    }
}
