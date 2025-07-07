// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use derive_more::Deref;
use serde::{Deserialize, Serialize};
use crate::BootstrapConfigRaw;

const DEFAULT_USER_ENTITY_NAME: &str = "Jans::User";
const DEFAULT_WORKLOAD_ENTITY_NAME: &str = "Jans::Workload";
const DEFAULT_ROLE_ENTITY_NAME: &str = "Jans::Role";
const DEFAULT_ISS_ENTITY_NAME: &str = "Jans::TrustedIssuer";
const DEFAULT_UNSIGNED_ROLE_ID_SRC: &str = "role";

pub(crate) const DEFAULT_ACCESS_TKN_ENTITY_NAME: &str = "Jans::Access_token";
pub(crate) const DEFAULT_ID_TKN_ENTITY_NAME: &str = "Jans::Id_token";
pub(crate) const DEFAULT_USERINFO_TKN_ENTITY_NAME: &str = "Jans::Userinfo_token";

/// Bootstrap Configurations for the JWT to Cedar entity mappings
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct EntityBuilderConfig {
    /// The names of the buildable Cedar entity type names
    pub entity_names: EntityNames,
    /// Toggles building the `Workload` entity
    pub build_workload: bool,
    /// Toggles building the `User` entity
    pub build_user: bool,
    /// The attribute to use when creating Role entities in the unsigned interface
    pub unsigned_role_id_src: UnsignedRoleIdSrc,
}

/// Raw entity builder config
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct EntityBuilderConfigRaw {
    /// Toggles building the `Workload` entity
    pub workload_authz: bool,
    /// Toggles building the `User` entity
    pub user_authz: bool,
    /// The attribute to use when creating Role entities in the unsigned interface
    pub unsigned_role_id_src: UnsignedRoleIdSrc,
    /// Mapping name of cedar schema User entity
    pub mapping_user: Option<String>,
    /// Mapping name of cedar schema Workload entity
    pub mapping_workload: Option<String>,
    /// Mapping name of cedar schema Role entity
    pub mapping_role: Option<String>,
    /// Mapping name of cedar schema Issuer entity
    pub mapping_iss: Option<String>,
}

/// Entity names
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct EntityNames {
    /// The entity type name of the `User` entity
    pub user: String,
    /// The entity type name of the `Workload` entity
    pub workload: String,
    /// The entity type name of the `Role` entity
    pub role: String,
    /// The entity type name of the `TrustedIssuer` entity
    pub iss: String,
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
            entity_names: EntityNames {
                user: DEFAULT_USER_ENTITY_NAME.to_string(),
                workload: DEFAULT_WORKLOAD_ENTITY_NAME.to_string(),
                role: DEFAULT_ROLE_ENTITY_NAME.to_string(),
                iss: DEFAULT_ISS_ENTITY_NAME.to_string(),
            },
            build_workload: true,
            build_user: true,
            unsigned_role_id_src: UnsignedRoleIdSrc(DEFAULT_UNSIGNED_ROLE_ID_SRC.to_string()),
        }
    }
}

impl EntityBuilderConfig {
    /// Enables building the `Workload` entity
    pub fn with_workload(mut self) -> Self {
        self.build_workload = true;
        self
    }

    /// Enables building the `User` entity
    pub fn with_user(mut self) -> Self {
        self.build_user = true;
        self
    }
}

impl From<EntityBuilderConfigRaw> for EntityBuilderConfig {
    fn from(raw: EntityBuilderConfigRaw) -> Self {
        let entity_names = EntityNames {
            user: raw.mapping_user.unwrap_or_else(|| DEFAULT_USER_ENTITY_NAME.to_string()),
            workload: raw.mapping_workload.unwrap_or_else(|| DEFAULT_WORKLOAD_ENTITY_NAME.to_string()),
            role: raw.mapping_role.unwrap_or_else(|| DEFAULT_ROLE_ENTITY_NAME.to_string()),
            iss: raw.mapping_iss.unwrap_or_else(|| DEFAULT_ISS_ENTITY_NAME.to_string()),
        };

        Self {
            entity_names,
            build_workload: raw.workload_authz,
            build_user: raw.user_authz,
            unsigned_role_id_src: raw.unsigned_role_id_src,
        }
    }
}

impl From<&BootstrapConfigRaw> for EntityBuilderConfig {
    fn from(raw: &BootstrapConfigRaw) -> Self {
        let raw_entity = EntityBuilderConfigRaw {
            workload_authz: raw.workload_authz.is_enabled(),
            user_authz: raw.user_authz.is_enabled(),
            unsigned_role_id_src: raw.unsigned_role_id_src.clone(),
            mapping_user: raw.mapping_user.clone(),
            mapping_workload: raw.mapping_workload.clone(),
            mapping_role: raw.mapping_role.clone(),
            mapping_iss: raw.mapping_iss.clone(),
        };
        EntityBuilderConfig::from(raw_entity)
    }
}
