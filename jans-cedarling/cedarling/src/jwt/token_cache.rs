// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use chrono::{DateTime, Duration, Utc};
use sparkv::{Config, SparKV};
use std::hash::Hash;
use std::sync::{Arc, RwLock};

use crate::LogLevel;
use crate::jwt::token::Token;
use crate::jwt::validation::TokenKind;
use crate::log::{BaseLogEntry, LogEntry, LogWriter, Logger};

/// A dedicated cache for storing validated JWT tokens.
///
/// The cache uses a thread-safe key-value store with automatic expiration
/// based on token expiration claims and a configurable maximum TTL.
#[derive(Clone)]
pub(crate) struct TokenCache {
    cache: Arc<RwLock<SparKV<Arc<Token>>>>,
    max_ttl: usize,
    logger: Option<Logger>,
}

#[cfg(test)]
impl Default for TokenCache {
    fn default() -> Self {
        // default parameters, is used only for testing

        use crate::log::TEST_LOGGER;
        Self::new(60 * 5, 100, true, Some(TEST_LOGGER.clone()))
    }
}

impl TokenCache {
    /// Creates a new `TokenCache` with the specified maximum TTL in seconds.
    ///
    /// If `max_ttl` is set to 0, tokens will use their natural expiration or default TTL.
    pub(crate) fn new(
        max_ttl: usize,
        capacity: usize,
        earliest_expiration_eviction: bool,
        logger: Option<Logger>,
    ) -> Self {
        Self {
            cache: Arc::new(RwLock::new(SparKV::with_config(Config {
                max_ttl: Duration::seconds(max_ttl as i64),
                max_items: capacity,
                earliest_expiration_eviction,
                ..Default::default()
            }))),
            max_ttl,
            logger,
        }
    }

    fn log_warn(&self, msg: String) {
        if let Some(logger) = &self.logger {
            logger.log_any(
                LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                    LogLevel::WARN,
                    None,
                ))
                .set_message(msg),
            );
        }
    }

    /// Attempts to find a token in the cache.
    ///
    /// Returns `Some(Arc<Token>)` if the token is found in cache and not expired,
    /// otherwise returns `None`.
    pub(crate) fn find(&self, kind: &TokenKind, jwt: &str) -> Option<Arc<Token>> {
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
    pub(crate) fn save(&self, kind: &TokenKind, jwt: &str, token: Arc<Token>, now: DateTime<Utc>) {
        if self.check_token_expired(&token, now) {
            // token is expired, no need to save it
            return;
        }

        let key = hash_jwt_token(kind, jwt);

        // Extract issuer for indexing
        let index_keys = token
            .extract_normalized_issuer()
            .map(|iss| vec![IndexKey::Iss(iss).index_value()])
            .unwrap_or_default();

        let result = if let Some(duration) = self.cache_duration(&token, now) {
            self.cache
                .write()
                .expect("token cache mutex shouldn't be poisoned")
                .set_with_ttl(&key, token, Duration::seconds(duration), &index_keys)
        } else {
            // set with SparkKV default TTL (5 minutes)
            self.cache
                .write()
                .expect("token cache mutex shouldn't be poisoned")
                .set(&key, token, &index_keys)
        };
        if let Err(err) = result {
            self.log_warn(format!("could not set token to token cache: {}", err));
        }
    }

    /// Check if token is expired
    /// true - means is expired
    fn check_token_expired(&self, token: &Arc<Token>, now: DateTime<Utc>) -> bool {
        token
            .claims
            .get_claim("exp")
            .and_then(|exp| exp.value().as_i64())
            .map(|exp| exp <= now.timestamp())
            .unwrap_or(false)
    }

    /// Extract cache duration, result is optional
    fn cache_duration(&self, token: &Arc<Token>, now: DateTime<Utc>) -> Option<i64> {
        token
            .claims
            .get_claim("exp")
            .and_then(|exp| exp.value().as_i64())
            .and_then(|exp| {
                // calculate duration until token expiration
                let duration = exp - now.timestamp();
                if duration > 0 {
                    // if duration bigger than configured max ttl, use the max ttl
                    Some(if self.max_ttl > 0 && duration > self.max_ttl as i64 {
                        self.max_ttl as i64
                    } else {
                        duration
                    })
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
            })
    }

    /// Clears expired tokens from the cache.
    ///
    /// This should be called periodically to prevent memory leaks from
    /// accumulated expired tokens.
    pub(crate) fn clear_expired(&self) {
        self.cache
            .write()
            .expect("token cache mutex shouldn't be poisoned")
            .clear_expired();
    }

    /// Remove tokens from cache by index key
    pub(crate) fn invalidate_by_index(&self, index_key: IndexKey) {
        self.cache
            .write()
            .expect("token cache mutex shouldn't be poisoned")
            .remove_by_index(&index_key.index_value());
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

/// IndexKey is structure that is used for indexing [TokenCache]
//
// thiserror is used for usefull string conversation
#[derive(Debug, derive_more::Display)]
pub(crate) enum IndexKey {
    #[display("iss:{_0}")]
    Iss(String),
}

impl IndexKey {
    // We use special method to have string representation of index value
    // because in future it may change, for example applying hash function
    fn index_value(&self) -> String {
        self.to_string()
    }
}
