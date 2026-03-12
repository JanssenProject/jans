// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashSet;

/// Tracks loading progress of trusted issuers
#[derive(Debug)]
pub(super) struct TrustedIssuerLoadingState {
    total_issuers: usize,
    /// Stores issuer IDs that failed to load
    failed_issuers: std::sync::RwLock<HashSet<String>>,
}

impl TrustedIssuerLoadingState {
    /// Create a new loading state tracker
    pub(super) fn new(total_issuers: usize) -> Self {
        Self {
            total_issuers,
            failed_issuers: std::sync::RwLock::new(HashSet::new()),
        }
    }

    /// Record that an issuer has failed to load
    pub(super) fn add_trusted_issuer_failed(&self, issuer_id: String) {
        let mut failed = self
            .failed_issuers
            .write()
            .expect("TrustedIssuerLoadingState RwLock poisoned");
        failed.insert(issuer_id);
    }

    /// Get the total number of issuers to load
    pub(super) fn total_issuers(&self) -> usize {
        self.total_issuers
    }

    /// Get the issuer IDs that failed to load
    pub(super) fn failed_issuers(&self) -> HashSet<String> {
        self.failed_issuers
            .read()
            .expect("TrustedIssuerLoadingState RwLock poisoned")
            .clone()
    }
}
