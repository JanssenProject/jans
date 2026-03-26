// This software is available under the Apache-2.0 license.
//
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use jsonwebtoken::Algorithm;
use serde::{Deserialize, Serialize};
use std::{collections::HashSet, num::NonZeroUsize};

/// The set of Bootstrap properties related to JWT validation.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct JwtConfig {
    /// A Json Web Key Store (JWKS) with public keys.
    ///
    /// If this is used, Cedarling will no longer try to fetch JWK Stores from
    /// a trustede identity provider and stick to using the local JWKS.
    pub jwks: Option<String>,
    /// Check the signature for all the Json Web Tokens.
    ///
    /// This Requires the `iss` claim to be present in all the tokens and
    /// and the scheme must be `https`.
    ///
    /// This setting overrides the `iss` validation settings in the following:
    ///
    /// - `access_token_config`
    /// - `id_token_config`
    /// - `userinfo_token_config`
    pub jwt_sig_validation: bool,
    /// Whether to check the status of the JWT.
    ///
    /// On startup, the Cedarling will fetch and retreive the latest Status List
    /// JWT from the `.well-known/openid-configuration` via the `status_list_endpoint`
    /// claim and cache it.
    ///
    /// See the [`IETF Draft`] for more info.
    ///
    /// [`IETF Draft`]: https://datatracker.ietf.org/doc/draft-ietf-oauth-status-list/
    pub jwt_status_validation: bool,
    /// Only tokens signed with algorithms in this list can be valid.
    pub signature_algorithms_supported: HashSet<Algorithm>,
    /// Maximum TTL (in seconds) for cached tokens.
    /// Zero means no TTL limit is applied.
    ///
    /// It is recommended to keep this value within a few minutes to prevent the
    /// cache from growing excessively.
    pub token_cache_max_ttl_secs: usize,
    /// Maximum number of tokens the cache can store.
    pub token_cache_capacity: usize,
    /// Enables eviction policy based on the earliest expiration time.
    ///
    /// When the cache reaches its capacity, the entry with the nearest
    /// expiration timestamp will be removed to make room for a new one.
    pub token_cache_earliest_expiration_eviction: bool,
    /// Configuration for loading trusted issuers.
    pub trusted_issuer_loader: TrustedIssuerLoaderConfig,
}

impl Default for JwtConfig {
    /// Cedarling will use the strictest validation options by default.
    fn default() -> Self {
        let config = Self {
            jwks: None,
            jwt_sig_validation: true,
            jwt_status_validation: true,
            signature_algorithms_supported: HashSet::new(),
            token_cache_capacity: 100,
            token_cache_earliest_expiration_eviction: true,
            token_cache_max_ttl_secs: 60 * 5, // 5min
            trusted_issuer_loader: TrustedIssuerLoaderConfig::default(),
        };
        config.allow_all_algorithms()
    }
}

impl JwtConfig {
    /// Creates a new `JwtConfig` instance with validation turned off for all tokens.
    #[must_use]
    pub fn new_without_validation() -> Self {
        Self {
            jwks: None,
            jwt_sig_validation: false,
            jwt_status_validation: false,
            signature_algorithms_supported: HashSet::new(),
            ..Default::default()
        }
        .allow_all_algorithms()
    }

    pub(crate) fn supported_algorithms() -> HashSet<Algorithm> {
        HashSet::from_iter([
            Algorithm::HS256,
            Algorithm::HS384,
            Algorithm::HS512,
            Algorithm::ES256,
            Algorithm::ES384,
            Algorithm::RS256,
            Algorithm::RS384,
            Algorithm::RS512,
            Algorithm::PS256,
            Algorithm::PS384,
            Algorithm::PS512,
            Algorithm::EdDSA,
        ])
    }

    /// Adds all supported algorithms to `signature_algorithms_supported`.
    #[must_use]
    pub fn allow_all_algorithms(mut self) -> Self {
        self.signature_algorithms_supported = Self::supported_algorithms();
        self
    }
}

/// Raw representation of trusted issuer loader type from environment variable.
/// Is used in [`BootstrapConfigRaw`].
#[derive(Debug, Default, Clone, PartialEq, Serialize, Deserialize)]
pub enum TrustedIssuerLoaderTypeRaw {
    /// Synchronous loading
    #[default]
    #[serde(rename = "SYNC")]
    Sync,
    /// Asynchronous loading
    #[serde(rename = "ASYNC")]
    Async,
}

impl TrustedIssuerLoaderTypeRaw {
    /// Converts raw representation to `TrustedIssuerLoaderConfig`.
    pub(crate) fn to_config(&self, workers: WorkersCount) -> TrustedIssuerLoaderConfig {
        match self {
            TrustedIssuerLoaderTypeRaw::Sync => TrustedIssuerLoaderConfig::Sync { workers },
            TrustedIssuerLoaderTypeRaw::Async => TrustedIssuerLoaderConfig::Async { workers },
        }
    }
}

/// Config structure that define how trusted issuers will be loaded.
///
/// Default is `Sync` with 1 worker.
#[derive(Debug, Copy, Clone, PartialEq, Serialize, Deserialize)]
pub enum TrustedIssuerLoaderConfig {
    /// Synchronous loading, on start program.
    /// The Cedarling will load all entities on start in "blocking mode" (you need to wait).
    Sync {
        /// Workers count
        workers: WorkersCount,
    },
    /// Asynchronous loading, on start program.
    /// The Cedarling will load all entities on the background.
    /// You need specify workers count.
    Async {
        /// Workers count
        workers: WorkersCount,
    },
}

impl Default for TrustedIssuerLoaderConfig {
    fn default() -> Self {
        Self::Sync {
            workers: WorkersCount::MIN,
        }
    }
}

/// A wrapper around `NonZeroUsize` that enforces a maximum value.
/// This is used to ensure that the number of workers for loading trusted issuers does not exceed a reasonable limit.
/// The maximum value is defined based on the target architecture to prevent excessive resource usage.
///
/// On non-WebAssembly targets, the maximum is set to 1000, while on WebAssembly targets,
/// it is set to 4 to align with typical browser limits on concurrent HTTP connections.
#[derive(
    Debug, Copy, Clone, PartialEq, Eq, PartialOrd, Ord, derive_more::Deref, serde::Serialize,
)]
pub struct WorkersCount(NonZeroUsize);

#[cfg(not(target_arch = "wasm32"))]
impl WorkersCount {
    /// Maximum number of workers is 1000 to prevent excessive resource usage.
    pub const MAX: WorkersCount = WorkersCount(NonZeroUsize::new(1000).unwrap());

    /// For native architecture default value is 10. Should cover most of cases.
    pub const DEFAULT: WorkersCount = WorkersCount(NonZeroUsize::new(10).unwrap());
}

#[cfg(target_arch = "wasm32")]
impl WorkersCount {
    /// On WebAssembly targets, we set workers limit 6.
    ///
    /// Most of web browsers have limit of 6 concurrent http connections, so we respect that.
    pub const MAX: WorkersCount = WorkersCount(NonZeroUsize::new(6).unwrap());

    /// For WASM architecture default value is 2
    pub const DEFAULT: WorkersCount = WorkersCount(NonZeroUsize::new(2).unwrap());
}

impl WorkersCount {
    /// Minimum number of workers is 1.
    pub const MIN: WorkersCount = WorkersCount(NonZeroUsize::MIN);
}

impl WorkersCount {
    /// Creates a new `NonZeroUsizeLimited` instance, ensuring the value is non-zero and does not exceed the defined maximum.
    #[must_use]
    pub fn new(value: usize) -> Self {
        let value = NonZeroUsize::new(value)
            .unwrap_or(NonZeroUsize::MIN)
            .min(Self::MAX.0);

        Self(value)
    }
}

impl Default for WorkersCount {
    fn default() -> Self {
        Self::DEFAULT
    }
}

impl<'de> serde::Deserialize<'de> for WorkersCount {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = usize::deserialize(deserializer)?;
        Ok(Self::new(value))
    }
}

impl PartialEq<usize> for WorkersCount {
    fn eq(&self, other: &usize) -> bool {
        self.0.get() == *other
    }
}
