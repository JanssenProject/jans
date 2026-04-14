// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::BootstrapConfigRaw;
use serde::{Deserialize, Serialize};

pub(crate) const DEFAULT_ACCESS_TKN_ENTITY_NAME: &str = "Jans::Access_token";
pub(crate) const DEFAULT_ID_TKN_ENTITY_NAME: &str = "Jans::Id_token";
pub(crate) const DEFAULT_USERINFO_TKN_ENTITY_NAME: &str = "Jans::Userinfo_token";
pub(crate) const DEFAULT_ENTITY_TYPE_NAME: &str = "Token";

/// Bootstrap Configurations for the JWT to Cedar entity mappings
#[derive(Debug, Clone, PartialEq, Default, Serialize, Deserialize)]
pub struct EntityBuilderConfig {}

impl From<&BootstrapConfigRaw> for EntityBuilderConfig {
    fn from(_raw: &BootstrapConfigRaw) -> Self {
        Self {}
    }
}
