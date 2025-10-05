// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Policy store metadata types for identification, versioning, and integrity validation.

use chrono::{DateTime, Utc};
use serde::{Deserialize, Deserializer, Serialize, Serializer};
use std::collections::HashMap;

/// Helper module for serializing Optional DateTime
mod datetime_option {
    use chrono::{DateTime, Utc};
    use serde::{Deserialize, Deserializer, Serializer};

    pub fn serialize<S>(date: &Option<DateTime<Utc>>, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: Serializer,
    {
        match date {
            Some(dt) => serializer.serialize_some(&dt.to_rfc3339()),
            None => serializer.serialize_none(),
        }
    }

    pub fn deserialize<'de, D>(deserializer: D) -> Result<Option<DateTime<Utc>>, D::Error>
    where
        D: Deserializer<'de>,
    {
        let opt: Option<String> = Option::deserialize(deserializer)?;
        match opt {
            Some(s) => DateTime::parse_from_rfc3339(&s)
                .map(|dt| Some(dt.with_timezone(&Utc)))
                .map_err(serde::de::Error::custom),
            None => Ok(None),
        }
    }
}

/// Helper module for serializing DateTime
mod datetime {
    use chrono::{DateTime, Utc};
    use serde::{Deserialize, Deserializer, Serializer};

    pub fn serialize<S>(date: &DateTime<Utc>, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: Serializer,
    {
        serializer.serialize_str(&date.to_rfc3339())
    }

    pub fn deserialize<'de, D>(deserializer: D) -> Result<DateTime<Utc>, D::Error>
    where
        D: Deserializer<'de>,
    {
        let s: String = String::deserialize(deserializer)?;
        DateTime::parse_from_rfc3339(&s)
            .map(|dt| dt.with_timezone(&Utc))
            .map_err(serde::de::Error::custom)
    }
}

/// Metadata for a policy store.
///
/// Contains identification, versioning, and descriptive information about a policy store.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct PolicyStoreMetadata {
    /// The version of the Cedar policy language used in this policy store
    pub cedar_version: String,
    /// Policy store configuration
    pub policy_store: PolicyStoreInfo,
}

/// Core information about a policy store.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct PolicyStoreInfo {
    /// Unique identifier for the policy store (hex hash)
    #[serde(default)]
    pub id: String,
    /// Human-readable name for the policy store
    pub name: String,
    /// Optional description of the policy store
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Semantic version of the policy store content
    #[serde(default)]
    pub version: String,
    /// ISO 8601 timestamp when created
    #[serde(skip_serializing_if = "Option::is_none", with = "datetime_option")]
    pub created_date: Option<DateTime<Utc>>,
    /// ISO 8601 timestamp when last modified
    #[serde(skip_serializing_if = "Option::is_none", with = "datetime_option")]
    pub updated_date: Option<DateTime<Utc>>,
}

/// Manifest file for policy store integrity validation.
///
/// Contains checksums and metadata for all files in the policy store.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct PolicyStoreManifest {
    /// Reference to the policy store ID this manifest belongs to
    pub policy_store_id: String,
    /// ISO 8601 timestamp when the manifest was generated
    #[serde(with = "datetime")]
    pub generated_date: DateTime<Utc>,
    /// Map of file paths to their metadata
    pub files: HashMap<String, FileInfo>,
}

/// Information about a file in the policy store manifest.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct FileInfo {
    /// File size in bytes
    pub size: u64,
    /// SHA-256 checksum of the file content (format: "sha256:<hex>")
    pub checksum: String,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_policy_store_metadata_serialization() {
        let metadata = PolicyStoreMetadata {
            cedar_version: "4.4.0".to_string(),
            policy_store: PolicyStoreInfo {
                id: "abc123".to_string(),
                name: "test_store".to_string(),
                description: Some("A test policy store".to_string()),
                version: "1.0.0".to_string(),
                created_date: Some(Utc::now()),
                updated_date: Some(Utc::now()),
            },
        };

        // Test serialization
        let json = serde_json::to_string(&metadata).unwrap();
        assert!(json.contains("cedar_version"));
        assert!(json.contains("4.4.0"));

        // Test deserialization
        let deserialized: PolicyStoreMetadata = serde_json::from_str(&json).unwrap();
        assert_eq!(deserialized.cedar_version, metadata.cedar_version);
        assert_eq!(deserialized.policy_store.name, metadata.policy_store.name);
    }

    #[test]
    fn test_policy_store_manifest_serialization() {
        let mut files = HashMap::new();
        files.insert("metadata.json".to_string(), FileInfo {
            size: 245,
            checksum: "sha256:abc123".to_string(),
        });

        let manifest = PolicyStoreManifest {
            policy_store_id: "test123".to_string(),
            generated_date: Utc::now(),
            files,
        };

        // Test serialization
        let json = serde_json::to_string(&manifest).unwrap();
        assert!(json.contains("policy_store_id"));
        assert!(json.contains("test123"));

        // Test deserialization
        let deserialized: PolicyStoreManifest = serde_json::from_str(&json).unwrap();
        assert_eq!(deserialized.policy_store_id, manifest.policy_store_id);
        assert_eq!(deserialized.files.len(), 1);
    }
}
