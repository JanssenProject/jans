// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use serde::{Deserialize, Serialize};
use std::path::PathBuf;

/// `PolicyStoreConfig` - Configuration for the policy store.
///
/// Defines where the policy will be retrieved from.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct PolicyStoreConfig {
    /// Specifies the source from which the policy will be read.
    pub source: PolicyStoreSource,
}

/// Raw policy store config
pub struct PolicyStoreConfigRaw {
    /// Source
    pub source: String,
    /// Path
    pub path: Option<String>,
}

/// `PolicyStoreSource` represents the source from which policies will be retrieved.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
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
    LockServer(String),

    /// Read policy from a JSON File.
    FileJson(PathBuf),

    /// Read policy from a YAML File.
    FileYaml(PathBuf),
}

/// Raw policy store source
pub enum PolicyStoreSourceRaw {
    /// JSON
    Json(String),
    /// YAML
    Yaml(String),
    /// Lock server
    LockServer(String),
    /// File JSON
    FileJson(String),
    /// File YAML
    FileYaml(String),
}

impl From<PolicyStoreConfigRaw> for PolicyStoreConfig {
    fn from(raw: PolicyStoreConfigRaw) -> Self {
        Self {
            source: match raw.source.as_str() {
                "json" => PolicyStoreSource::Json(raw.path.unwrap_or_default()),
                "yaml" => PolicyStoreSource::Yaml(raw.path.unwrap_or_default()),
                "lock_server" => PolicyStoreSource::LockServer(raw.path.unwrap_or_default()),
                "file_json" => PolicyStoreSource::FileJson(raw.path.unwrap_or_default().into()),
                "file_yaml" => PolicyStoreSource::FileYaml(raw.path.unwrap_or_default().into()),
                _ => PolicyStoreSource::FileYaml("policy-store.yaml".into()),
            },
        }
    }
}
