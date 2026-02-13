// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashSet;
use std::sync::atomic::{AtomicUsize, Ordering};

/// Tracks loading progress of trusted issuers
#[derive(Debug)]
pub(super) struct TrustedIssuerLoadingState {
    total_issuers: usize,
    loaded_count: AtomicUsize,
    /// Stores issuer IDs that failed to load
    failed_issuers: std::sync::RwLock<HashSet<String>>,
}

impl TrustedIssuerLoadingState {
    /// Create a new loading state tracker
    pub(super) fn new(total_issuers: usize) -> Self {
        Self {
            total_issuers,
            loaded_count: AtomicUsize::new(0),
            failed_issuers: std::sync::RwLock::new(HashSet::new()),
        }
    }

    /// Record that an issuer has been successfully loaded
    pub(super) fn add_trusted_issuer_loaded(&self) {
        self.loaded_count.fetch_add(1, Ordering::Release);
    }

    /// Record that an issuer has failed to load
    pub(super) fn add_trusted_issuer_failed(&self, issuer_id: String) {
        self.loaded_count.fetch_add(1, Ordering::Release); // Still counts toward total processed
        let mut failed = self
            .failed_issuers
            .write()
            .expect("TrustedIssuerLoadingState RwLock poisoned");
        failed.insert(issuer_id);
    }

    /// Get the number of issuers that have been processed (successful or failed)
    pub(super) fn processed_count(&self) -> usize {
        self.loaded_count.load(Ordering::Acquire)
    }

    /// Get the total number of issuers to load
    pub(super) fn total_count(&self) -> usize {
        self.total_issuers
    }

    /// Get the percentage of issuers that have been loaded (0.0 to 100.0)
    #[allow(clippy::cast_precision_loss)]
    pub(super) fn percent_handled(&self) -> f32 {
        if self.total_issuers == 0 {
            100.0
        } else {
            (self.processed_count() as f32 / self.total_count() as f32) * 100.0
        }
    }

    /// Get the issuer IDs that failed to load
    pub(super) fn failed_issuers(&self) -> HashSet<String> {
        self.failed_issuers
            .read()
            .expect("TrustedIssuerLoadingState RwLock poisoned")
            .clone()
    }
}
