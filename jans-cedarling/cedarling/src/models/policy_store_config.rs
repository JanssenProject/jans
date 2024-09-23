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
pub enum PolicyStoreSource {
    /// Read policy from raw JSON
    Json(String),
}
