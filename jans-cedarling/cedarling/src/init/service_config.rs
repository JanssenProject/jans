/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::policy_store::PolicyStoreLoadError;
use crate::common::policy_store::PolicyStoreWithID;
use crate::http::{HttpClient, InitializeHttpClientError};

/// Configuration that holds validated information from bootstrap config.
///
/// Plain data struct — callers (currently only `Cedarling::new`) build it
/// directly from a freshly-loaded policy store plus a configured HTTP client.
/// Refresh-worker seed data (initial body hash, initial cache validators)
/// is intentionally **not** carried here: those values are consumed inline at
/// the worker-spawn site rather than smuggled through service initialization.
#[derive(Clone)]
pub(crate) struct ServiceConfig {
    pub policy_store: PolicyStoreWithID,
    pub http_client: HttpClient,
}

#[derive(thiserror::Error, Debug)]
pub enum ServiceConfigError {
    /// Error that may occur during loading the policy store.
    #[error("Could not load policy: {0}")]
    PolicyStore(#[from] PolicyStoreLoadError),
    #[error(transparent)]
    InitHttpClient(#[from] InitializeHttpClientError),
}

// Note: this branch deliberately drops the `ServiceConfig::new(bootstrap)`
// constructor from `main`. The refresh-worker seed plumbing requires
// `Cedarling::new` to keep the `LoadedPolicyStore` (with `body_hash` and
// `validators`) in lexical scope after the load, so the load now happens at
// the call site in `lib.rs::perform_bootstrap_load` and the result is
// destructured there. `strict_schema_validation` is forwarded at that call
// site (see `lib.rs`).
