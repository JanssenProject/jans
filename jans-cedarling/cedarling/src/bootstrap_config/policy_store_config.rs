// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

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
    /// The string contains a URI where the policy store can be retrieved.
    LockMaster {
        /// Software Statement Assertion JWT used to dynamically register to the OpenID provider
        ssa_jwt: Option<String>,
        /// URI where Cedarling can get a JSON containing metadata about Lock Master,
        /// i.e. `.well-known/lock-master-configuration`.
        config_uri: String,
        /// The ID of the policy store to be used.
        policy_store_id: String,
        /// Client's JSON Web Key Set RFC7517 document value, which contains
        /// the client's public keys.
        jwks: Option<String>,
    },

    /// Read policy from a JSON File.
    FileJson(Box<Path>),

    /// Read policy from a YAML File.
    FileYaml(Box<Path>),

    /// Fetch the policies from a URI
    Uri(String),
}
