// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use chrono::{DateTime, Duration, Utc};
use sparkv::{Config, SparKV};
use std::hash::Hash;
use std::sync::{Arc, RwLock};

use crate::authz::metrics::MetricsCollector;
use crate::common::issuer_utils::IssClaim;
use crate::jwt::token::Token;
use crate::jwt::validation::TokenKind;
use crate::log::{BaseLogEntry, LogEntry, LogWriter, Logger};
use crate::LogLevel;

/// A dedicated cache for storing validated JWT tokens.
///
/// The cache uses a thread-safe key-value store with automatic expiration
/// based on token expiration claims and a configurable maximum TTL.
#[derive(Clone)]
pub(crate) struct TokenCache {
    cache: Option<Arc<RwLock<SparKV<Arc<Token>>>>>,
    max_ttl: usize,
    logger: Option<Logger>,
    metrics: Arc<MetricsCollector>,
}

#[cfg(test)]
impl Default for TokenCache {
    fn default() -> Self {
        // default parameters, is used only for testing

        use crate::log::TEST_LOGGER;
        Self::new(
            60 * 5,
            100,
            true,
            Some(TEST_LOGGER.clone()),
            Arc::new(MetricsCollector::new(0)),
        )
    }
}

impl TokenCache {
    /// Creates a new `TokenCache` with the specified maximum TTL in seconds.
    ///
    /// `max_ttl` semantics:
    /// - `> 0`: caps each entry's TTL at `max_ttl`. Also used as the TTL when
    ///   the token has no `exp` claim.
    /// - `0`: disables the token cache entirely.
    pub(crate) fn new(
        max_ttl: usize,
        capacity: usize,
        earliest_expiration_eviction: bool,
        logger: Option<Logger>,
        metrics: Arc<MetricsCollector>,
    ) -> Self {
        let cache = (max_ttl > 0).then(|| {
            Arc::new(RwLock::new(SparKV::with_config(Config {
                max_ttl: Duration::seconds(i64::try_from(max_ttl).unwrap_or_default()),
                max_items: capacity,
                earliest_expiration_eviction,
                ..Default::default()
            })))
        });

        Self {
            cache,
            max_ttl,
            logger,
            metrics,
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
        let Some(cache) = &self.cache else {
            self.metrics.record_cache_miss();
            return None;
        };

        let key = hash_jwt_token(kind, jwt);
        let result = cache
            .read()
            .expect("token cache mutex shouldn't be poisoned")
            .get(&key)
            .map(std::borrow::ToOwned::to_owned);
        if result.is_some() {
            self.metrics.record_cache_hit();
        } else {
            self.metrics.record_cache_miss();
        }
        result
    }

    /// Saves a token to the cache with appropriate TTL.
    ///
    /// The TTL is determined as follows:
    /// 1. If the token has a valid `exp` claim, the entry TTL is the time until
    ///    expiration (clamped by `max_ttl` when it is > 0).
    /// 2. If the token has no `exp` claim and `max_ttl` is > 0, `max_ttl` is used.
    /// 3. If `max_ttl` is 0, the token cache is disabled and no token is cached.
    pub(crate) fn save(&self, kind: &TokenKind, jwt: &str, token: Arc<Token>, now: DateTime<Utc>) {
        let Some(cache) = &self.cache else {
            return;
        };

        if TokenCache::check_token_expired(&token, now) {
            // token is expired, no need to save it
            return;
        }

        let Some(duration) = self.cache_duration(&token, now) else {
            // No `exp` claim and no configured max TTL — do not cache.
            return;
        };

        let key = hash_jwt_token(kind, jwt);

        // Extract issuer for indexing
        let index_keys = token
            .extract_normalized_issuer()
            .map(|iss| vec![IndexKey::Iss(iss).index_value()])
            .unwrap_or_default();

        let result = cache
            .write()
            .expect("token cache mutex shouldn't be poisoned")
            .set_with_ttl(&key, token, Duration::seconds(duration), &index_keys);
        if let Err(err) = result {
            self.log_warn(format!("could not set token to token cache: {err}"));
        }
    }

    /// Check if token is expired
    /// true - means is expired
    fn check_token_expired(token: &Arc<Token>, now: DateTime<Utc>) -> bool {
        token
            .claims
            .get_claim("exp")
            .and_then(|exp| exp.value().as_i64())
            .is_some_and(|exp| exp <= now.timestamp())
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
                    if let Ok(max_ttl_i64) = i64::try_from(self.max_ttl) {
                        Some(if self.max_ttl > 0 && duration > max_ttl_i64 {
                            max_ttl_i64
                        } else {
                            duration
                        })
                    } else {
                        // fallback to duration if max_ttl conversion fails
                        Some(duration)
                    }
                } else {
                    None
                }
            })
            .or({
                // if no exp claim, use the configured max ttl if set
                if self.max_ttl > 0 {
                    i64::try_from(self.max_ttl).ok()
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
        let Some(cache) = &self.cache else {
            return;
        };

        let cleared = cache
            .write()
            .expect("token cache mutex shouldn't be poisoned")
            .clear_expired();
        if cleared > 0 {
            self.metrics.record_cache_eviction(cleared);
        }
    }

    /// Remove tokens from cache by index key
    pub(crate) fn invalidate_by_index(&self, index_key: &IndexKey) {
        let Some(cache) = &self.cache else {
            return;
        };

        cache
            .write()
            .expect("token cache mutex shouldn't be poisoned")
            .remove_by_index(&index_key.index_value());
    }
}

/// Hash a string using `ahash` and return the hash value as a string
/// This is used to create a key for caching tokens
/// The hash value is used instead of the original string to have shorter keys for [`SparKV`] which utilizes `BTree`.
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

/// [`IndexKey`] is structure that is used for indexing [`TokenCache`]
//
// thiserror is used for usefull string conversation
#[derive(Debug, derive_more::Display)]
pub(crate) enum IndexKey {
    #[display("iss:{_0}")]
    Iss(IssClaim),
}

impl IndexKey {
    // We use special method to have string representation of index value
    // because in future it may change, for example applying hash function
    fn index_value(&self) -> String {
        self.to_string()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::jwt::token::TokenClaims;
    use serde_json::{json, Value};
    use std::collections::HashMap;

    fn token_cache(max_ttl: usize) -> TokenCache {
        TokenCache::new(max_ttl, 100, true, None, Arc::new(MetricsCollector::new(0)))
    }

    fn token(claims: HashMap<String, Value>) -> Arc<Token> {
        Arc::new(Token::new("test", TokenClaims::from(claims), None))
    }

    fn token_with_exp(now: DateTime<Utc>, duration_secs: i64) -> Arc<Token> {
        token(HashMap::from([(
            "exp".to_string(),
            json!(now.timestamp() + duration_secs),
        )]))
    }

    #[test]
    fn max_ttl_zero_disables_cache_for_token_with_exp() {
        let now = Utc::now();
        let cache = token_cache(0);
        let token = token_with_exp(now, 3600);

        cache.save(&TokenKind::StatusList, "jwt", token, now);

        assert!(
            cache.find(&TokenKind::StatusList, "jwt").is_none(),
            "max_ttl=0 should disable token cache even when the token has exp"
        );
    }

    #[test]
    fn max_ttl_zero_does_not_cache_token_without_exp() {
        let now = Utc::now();
        let cache = token_cache(0);
        let token = token(HashMap::new());

        cache.save(&TokenKind::StatusList, "jwt", token, now);

        assert!(
            cache.find(&TokenKind::StatusList, "jwt").is_none(),
            "max_ttl=0 should not cache tokens without exp"
        );
    }

    #[test]
    fn positive_max_ttl_caps_token_expiration() {
        let now = Utc::now();
        let cache = token_cache(5);
        let token = token_with_exp(now, 3600);

        assert_eq!(
            cache.cache_duration(&token, now),
            Some(5),
            "positive max_ttl should cap the cache duration for tokens with exp"
        );
    }
}
