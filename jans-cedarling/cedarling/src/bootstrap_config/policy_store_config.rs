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

    /// Read policy from a Cedar Archive (.cjar) file.
    ///
    /// The path points to a `.cjar` archive containing the policy store
    /// in the new directory structure format.
    CjarFile(PathBuf),

    /// Read policy from a Cedar Archive (.cjar) fetched from a URL.
    ///
    /// The string contains a URL where the `.cjar` archive can be downloaded.
    CjarUrl(String),

    /// Read policy from a directory structure.
    ///
    /// The path points to a directory containing the policy store
    /// in the new directory structure format (with manifest.json, policies/, etc.).
    Directory(PathBuf),
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
    /// Cedar Archive file (.cjar)
    CjarFile(String),
    /// Cedar Archive URL (.cjar)
    CjarUrl(String),
    /// Directory structure
    Directory(String),
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
                "cjar_file" => PolicyStoreSource::CjarFile(
                    raw.path
                        .filter(|p| !p.is_empty())
                        .unwrap_or_else(|| "policy-store.cjar".to_string())
                        .into(),
                ),
                "cjar_url" => PolicyStoreSource::CjarUrl(
                    raw.path.filter(|p| !p.is_empty()).unwrap_or_default(),
                ),
                "directory" => PolicyStoreSource::Directory(
                    raw.path
                        .filter(|p| !p.is_empty())
                        .unwrap_or_else(|| "policy-store".to_string())
                        .into(),
                ),
                _ => PolicyStoreSource::FileYaml("policy-store.yaml".into()),
            },
        }
    }
}
