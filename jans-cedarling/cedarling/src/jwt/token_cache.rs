// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use chrono::{DateTime, Duration, Utc};
use sparkv::SparKV;
use std::hash::Hash;
use std::sync::{Arc, RwLock};

use crate::jwt::token::Token;
use crate::jwt::validation::TokenKind;

/// A dedicated cache for storing validated JWT tokens.
///
/// The cache uses a thread-safe key-value store with automatic expiration
/// based on token expiration claims and a configurable maximum TTL.
pub struct TokenCache {
    cache: Arc<RwLock<SparKV<Arc<Token>>>>,
    max_ttl: usize,
}

impl TokenCache {
    /// Creates a new `TokenCache` with the specified maximum TTL in seconds.
    ///
    /// If `max_ttl` is set to 0, tokens will use their natural expiration or default TTL.
    pub fn new(max_ttl: usize) -> Self {
        Self {
            cache: Arc::new(RwLock::new(SparKV::new())),
            max_ttl,
        }
    }

    /// Attempts to find a token in the cache.
    ///
    /// Returns `Some(Arc<Token>)` if the token is found in cache and not expired,
    /// otherwise returns `None`.
    pub fn find(&self, kind: &TokenKind, jwt: &str) -> Option<Arc<Token>> {
        let key = hash_jwt_token(kind, jwt);
        self.cache
            .read()
            .expect("token cache mutex shouldn't be poisoned")
            .get(&key)
            .map(|v| v.to_owned())
    }

    /// Saves a token to the cache with appropriate TTL.
    ///
    /// The TTL is calculated based on:
    /// 1. The token's expiration claim (if present and valid)
    /// 2. The configured maximum TTL (if set)
    /// 3. Default TTL (5 minutes) if neither applies
    pub fn save(&self, kind: &TokenKind, jwt: &str, token: Arc<Token>, now: DateTime<Utc>) {
        let key = hash_jwt_token(kind, jwt);

        let cache_duration_opt = token
            .claims
            .get_claim("exp")
            .and_then(|exp| exp.value().as_i64())
            .and_then(|exp| {
                // calculate duration until token expiration
                let duration = exp - now.timestamp();
                if duration > 0 {
                    // if duration bigger than configured max ttl, use the max ttl
                    Some(
                        if self.max_ttl > 0 && duration > self.max_ttl as i64 {
                            self.max_ttl as i64
                        } else {
                            duration
                        },
                    )
                } else {
                    None
                }
            })
            .or({
                // if no exp claim, use the configured max ttl if set
                if self.max_ttl > 0 {
                    Some(self.max_ttl as i64)
                } else {
                    None
                }
            });

        if let Some(duration) = cache_duration_opt {
            let _ = self
                .cache
                .write()
                .expect("token cache mutex shouldn't be poisoned")
                .set_with_ttl(&key, token, Duration::seconds(duration), &[]);
        } else {
            // set with SparkKV default TTL (5 minutes)
            let _ = self
                .cache
                .write()
                .expect("token cache mutex shouldn't be poisoned")
                .set(&key, token, &[]);
        }
    }

    /// Clears expired tokens from the cache.
    ///
    /// This should be called periodically to prevent memory leaks from
    /// accumulated expired tokens.
    pub fn clear_expired(&self) {
        self.cache
            .write()
            .expect("token cache mutex shouldn't be poisoned")
            .clear_expired();
    }
}

/// Hash a string using `ahash` and return the hash value as a string
/// This is used to create a key for caching tokens
/// The hash value is used instead of the original string to have shorter keys for SparKV which utilizes BTree.
fn hash_jwt_token(kind: &TokenKind, jwt: &str) -> String {
    use core::hash::BuildHasher;
    use std::hash::Hasher;
    use std::sync::LazyLock;
    static HASHER_KEYS: LazyLock<(u64, u64, u64, u64)> = LazyLock::new(|| {
        (
            rand::random(),
            rand::random(),
            rand::random(),
            rand::random(),
        )
    });

    let mut hasher =
        ahash::RandomState::with_seeds(HASHER_KEYS.0, HASHER_KEYS.1, HASHER_KEYS.2, HASHER_KEYS.3)
            .build_hasher();

    kind.hash(&mut hasher);
    jwt.hash(&mut hasher);

    hasher.finish().to_string()
}