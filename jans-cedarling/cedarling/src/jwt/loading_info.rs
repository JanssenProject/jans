// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashSet;

/// Provides information about the loading status of trusted issuers.
///
/// This trait is implemented by services that can report on the progress of
/// loading trusted issuers, including which issuers are loaded, which have failed,
/// and overall loading statistics.
pub trait TrustedIssuerLoadingInfo {
    /// Returns `true` if the trusted issuer with the given ID (policy store key) is loaded.
    ///
    /// # Arguments
    ///
    /// * `issuer_id` - The policy store key identifier for the trusted issuer.
    #[must_use]
    fn is_trusted_issuer_loaded_by_name(&self, issuer_id: &str) -> bool;

    /// Returns `true` if the trusted issuer with the given iss claim is loaded.
    ///
    /// # Arguments
    ///
    /// * `iss_claim` - The iss claim value from a JWT token.
    #[must_use]
    fn is_trusted_issuer_loaded_by_iss(&self, iss_claim: &str) -> bool;

    /// Returns the number of trusted issuers that have been loaded.
    #[must_use]
    fn loaded_trusted_issuers_count(&self) -> usize;

    /// Returns the percentage (0.0 to 100.0) of trusted issuers loaded.
    #[must_use]
    fn percent_loaded_trusted_issuers(&self) -> f32;

    /// Returns all issuer IDs in the index of loaded trusted issuers.
    ///
    /// This returns a set of unique issuer IDs that have been successfully loaded.
    #[must_use]
    fn loaded_trusted_issuer_ids(&self) -> HashSet<String>;

    /// Returns issuer IDs that failed to load.
    ///
    /// This returns a set of unique issuer IDs that encountered errors during loading.
    /// Failed issuers are still counted toward the total processing count.
    #[must_use]
    fn failed_trusted_issuer_ids(&self) -> HashSet<String>;
}
