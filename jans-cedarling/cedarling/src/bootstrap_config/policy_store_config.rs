/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use std::path::Path;

/// `PolicyStoreConfig` - Configuration for the policy store.
///
/// Defines where the policy will be retrieved from.
#[derive(Debug, PartialEq)]
pub struct PolicyStoreConfig {
    /// Specifies the source from which the policy will be read.
    pub source: PolicyStoreSource,
}

/// `PolicyStoreSource` represents the source from which policies will be retrieved.
#[derive(Debug, Clone, PartialEq)]
pub enum PolicyStoreSource {
    /// Read the policy directly from a raw JSON string.
    ///
    /// The string contains the raw JSON data representing the policy.
    Json(String),

    /// Read the policy directly from a raw YAML string.
    ///
    /// The string contains the raw YAML data representing the policy.
    /// Mostly used only for testing purposes.
    Yaml(String),

    /// Fetch the policies from the Lock Master service using a specified identifier.
    ///
    /// The string contains the identifier of the policy store, which is set in the
    /// `CEDARLING_POLICY_STORE_ID` bootstrap configuration.
    LockMaster(String),

    /// Read policy from a JSON File.
    FileJson(Box<Path>),

    /// Read policy from a YAML File.
    FileYaml(Box<Path>),
}
