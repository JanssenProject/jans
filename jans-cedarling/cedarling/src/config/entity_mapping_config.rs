// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use derive_more::derive::Deref;
use serde::{Deserialize, Serialize};

/// Config specific to entity mapping
#[derive(Debug, Default, Deserialize, Serialize, PartialEq)]
pub struct EntityMappingConfig {
    /// Mapping name of cedar schema TrustedIssuer entity
    #[serde(alias = "CEDARLING_MAPPING_TRUSTED_ISSUER", default)]
    pub mapping_iss: MappingTrustedIssuer,

    /// Name of Cedar User schema entity
    #[serde(alias = "CEDARLING_MAPPING_USER", default)]
    pub mapping_user: MappingUser,

    /// Name of Cedar Workload schema entity
    #[serde(alias = "CEDARLING_MAPPING_WORKLOAD", default)]
    pub mapping_workload: MappingWorkload,

    /// Name of Cedar Role schema entity
    #[serde(alias = "CEDARLING_MAPPING_ROLE", default)]
    pub mapping_role: MappingRole,
}

/// Mapping name of cedar schema TrustedIssuer entity
#[derive(Debug, Deref, Deserialize, Serialize, PartialEq)]
pub struct MappingTrustedIssuer(pub String);

impl Default for MappingTrustedIssuer {
    /// Defaults to `"Jans::TrustedIssuer"`
    fn default() -> Self {
        Self("Jans::TrustedIssuer".to_string())
    }
}

/// Name of Cedar User schema entity
#[derive(Debug, Deref, Deserialize, Serialize, PartialEq)]
pub struct MappingUser(pub String);

impl Default for MappingUser {
    /// Defaults to `"Jans::User"`
    fn default() -> Self {
        Self("Jans::User".to_string())
    }
}

/// Name of Cedar Workload schema entity
#[derive(Debug, Deref, Deserialize, Serialize, PartialEq)]
pub struct MappingWorkload(pub String);

impl Default for MappingWorkload {
    /// Defaults to `"Jans::Workload"`
    fn default() -> Self {
        Self("Jans::Workload".to_string())
    }
}

/// Name of Cedar Role schema entity
#[derive(Debug, Deref, Deserialize, Serialize, PartialEq)]
pub struct MappingRole(pub String);

impl Default for MappingRole {
    /// Defaults to `"Jans::Role"`
    fn default() -> Self {
        Self("Jans::Role".to_string())
    }
}
