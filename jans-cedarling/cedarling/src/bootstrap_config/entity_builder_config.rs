// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use derive_more::derive::Deref;
use serde::{Deserialize, Serialize};

use super::BootstrapConfigRaw;

const DEFAULT_WORKLOAD_ENTITY_NAME: &str = "Jans::Workload";
const DEFAULT_USER_ENTITY_NAME: &str = "Jans::User";
const DEFAULT_ISS_ENTITY_NAME: &str = "Jans::TrustedIssuer";
const DEFAULT_ROLE_ENTITY_NAME: &str = "Jans::Role";

pub(crate) const DEFAULT_ACCESS_TKN_ENTITY_NAME: &str = "Jans::Access_token";
pub(crate) const DEFAULT_ID_TKN_ENTITY_NAME: &str = "Jans::Id_token";
pub(crate) const DEFAULT_USERINFO_TKN_ENTITY_NAME: &str = "Jans::Userinfo_token";

/// Bootstrap Configurations for the JWT to Cedar entity mappings
#[derive(Debug, PartialEq, Clone, Default)]
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

#[derive(Debug, PartialEq, Clone, Deref, Serialize)]
/// The attribute that will be used to create the Role entity when using the
/// [`authorize_unsigned`] interface.
///
///
/// This can be set using the `CEDARLING_UNSIGNED_ROLE_ID_SRC` bootstrap property
///
/// [`authorize_unsigned`]: crate::Cedarling::authorize_unsigned
pub struct UnsignedRoleIdSrc(String);

const DEFAULT_UNSIGNED_ROLE_ID_SRC: &str = "role";

impl Default for UnsignedRoleIdSrc {
    fn default() -> Self {
        Self(DEFAULT_UNSIGNED_ROLE_ID_SRC.to_string())
    }
}

impl<'de> Deserialize<'de> for UnsignedRoleIdSrc {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let role_str = String::deserialize(deserializer)?.trim().to_string();
        let src = if role_str.is_empty() {
            Self(DEFAULT_UNSIGNED_ROLE_ID_SRC.to_string())
        } else {
            Self(role_str)
        };

        Ok(src)
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

/// The names of the entities in the schema
///
/// Note that the entity names for the tokens can be found in the trusted issuer
/// struct under their respective token entity metadata. The entity names here
/// only belong to the entity names that could be set using the bootstrap
/// properties
#[derive(Debug, PartialEq, Clone)]
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

impl Default for EntityNames {
    fn default() -> Self {
        Self {
            user: DEFAULT_USER_ENTITY_NAME.to_string(),
            workload: DEFAULT_WORKLOAD_ENTITY_NAME.to_string(),
            role: DEFAULT_ROLE_ENTITY_NAME.to_string(),
            iss: DEFAULT_ISS_ENTITY_NAME.to_string(),
        }
    }
}

impl From<&BootstrapConfigRaw> for EntityBuilderConfig {
    fn from(config: &BootstrapConfigRaw) -> Self {
        let entity_names = EntityNames {
            user: config
                .mapping_user
                .clone()
                .unwrap_or_else(|| DEFAULT_USER_ENTITY_NAME.to_string())
                .clone(),
            workload: config
                .mapping_workload
                .clone()
                .unwrap_or_else(|| DEFAULT_WORKLOAD_ENTITY_NAME.to_string()),
            role: config
                .mapping_role
                .clone()
                .unwrap_or_else(|| DEFAULT_ROLE_ENTITY_NAME.to_string()),
            iss: config
                .mapping_iss
                .clone()
                .unwrap_or_else(|| DEFAULT_ISS_ENTITY_NAME.to_string()),
        };
        Self {
            entity_names,
            build_workload: config.workload_authz.into(),
            build_user: config.user_authz.into(),
            unsigned_role_id_src: config.unsigned_role_id_src.clone(),
        }
    }
}
