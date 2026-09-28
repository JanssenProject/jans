// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Policy store metadata types for identification, versioning, and integrity validation.

use chrono::{DateTime, Utc};

/// Metadata for a policy store.
///
/// Contains identification, versioning, and descriptive information about a policy store.
#[derive(Debug, Clone, PartialEq)]
pub(crate) struct PolicyStoreMetadata {
    /// The version of the Cedar policy language used in this policy store
    pub(crate) cedar_version: String,
    /// Policy store configuration
    pub(crate) policy_store: PolicyStoreInfo,
}

/// Core information about a policy store.
#[derive(Debug, Clone, PartialEq)]
pub(crate) struct PolicyStoreInfo {
    /// Unique identifier for the policy store (hex hash)
    pub id: String,
    /// Human-readable name for the policy store
    pub name: String,
    /// Optional description of the policy store
    pub description: Option<String>,
    /// Semantic version of the policy store content
    pub version: String,
    /// ISO 8601 timestamp when created
    pub created_date: Option<DateTime<Utc>>,
    /// ISO 8601 timestamp when last modified
    pub updated_date: Option<DateTime<Utc>>,
}
