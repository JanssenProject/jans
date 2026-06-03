/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::policy_store::{PolicyStoreLoadError, load_policy_store};
use crate::bootstrap_config;
use crate::common::policy_store::PolicyStoreWithID;
use crate::http::{HttpClient, InitializeHttpClientError};
use bootstrap_config::BootstrapConfig;

/// Configuration that hold validated infomation from bootstrap config
#[derive(Clone)]
pub(crate) struct ServiceConfig {
    pub policy_store: PolicyStoreWithID,
    pub http_client: HttpClient,
    /// Hash of the policy-store body bytes captured during initial load — `Some`
    /// only for URL-based sources (Lock Server, `.cjar` URL). Seeded into the
    /// refresh worker so the first tick can short-circuit when the upstream
    /// returns a byte-identical body. `None` for local sources (the refresh
    /// worker doesn't spawn there).
    pub initial_body_hash: Option<u64>,
    /// Cache validators (`ETag`, `Last-Modified`, `max-age` / `Expires`)
    /// captured from the initial bootstrap response. Seeded into the refresh
    /// worker so the very first periodic conditional GET can return `304 Not
    /// Modified` without downloading any body bytes. Empty for non-URL sources.
    pub initial_validators: crate::http::cache_headers::CacheHeadersState,
}

#[derive(thiserror::Error, Debug)]
pub enum ServiceConfigError {
    /// Error that may occur during loading the policy store.
    #[error("Could not load policy: {0}")]
    PolicyStore(#[from] PolicyStoreLoadError),
    #[error(transparent)]
    InitHttpClient(#[from] InitializeHttpClientError),
}

impl ServiceConfig {
    pub(crate) async fn new(bootstrap: &BootstrapConfig) -> Result<Self, ServiceConfigError> {
        let http_client = HttpClient::new(bootstrap.http_client_config)?;
        let loaded = load_policy_store(&bootstrap.policy_store_config, &http_client).await?;

        Ok(Self {
            policy_store: loaded.store,
            http_client,
            initial_body_hash: loaded.body_hash,
            initial_validators: loaded.validators,
        })
    }
}
