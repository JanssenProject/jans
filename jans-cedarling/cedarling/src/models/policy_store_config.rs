/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

/// PolicyStoreConfig - Configuration for the policy store.
/// represent the place where we going to read the policy
pub struct PolicyStoreConfig {
    /// Source - represent the place where we going to read the policy.
    pub source: PolicyStoreSource,
    /// `CEDARLING_POLICY_STORE_ID` in [bootstrap properties](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties) documentation.
    /// If None then we should have only one policy store in the `source`.
    pub store_id: Option<String>,
}

/// PolicyStoreSource - represent the place where we going to read the policy.
#[derive(Debug, Clone)]
pub enum PolicyStoreSource {
    /// Read policy from raw JSON
    Json(String),
}
