// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashMap;
use std::hash::Hash;
use std::sync::{Arc, RwLock};

use chrono::{DateTime, Duration, Utc};

use crate::LogLevel;
use crate::authz::metrics::MetricsCollector;
use crate::common::issuer_utils::IssClaim;
use crate::jwt::token::Token;
use crate::jwt::validation::TokenKind;
use crate::log::{BaseLogEntry, LogEntry, LogWriter, Logger};
use crate::sparkv::{Config, HashMapSparKV};

/// A dedicated cache for storing validated JWT tokens.
///
/// The cache uses a thread-safe key-value store with automatic expiration
/// based on token expiration claims and a configurable maximum TTL.
#[derive(Clone)]
pub(crate) struct TokenCache {
    cache: Option<Arc<RwLock<HashMapSparKV<Arc<Token>>>>>,
    /// How many times the tokens of each `iss` were invalidated. It is part of
    /// the key of that issuer's tokens, so a token validated before an
    /// invalidation is saved under a key no later lookup uses.
    issuer_epochs: Arc<RwLock<HashMap<IssClaim, u64>>>,
    max_ttl: usize,
    logger: Option<Logger>,
    metrics: Arc<MetricsCollector>,
}

#[cfg(test)]
impl Default for TokenCache {
    fn default() -> Self {
        // default parameters, is used only for testing

        use crate::authz::metrics::MetricsMode;
        use crate::log::TEST_LOGGER;
        Self::new(
            60 * 5,
            100,
            true,
            Some(TEST_LOGGER.clone()),
            Arc::new(MetricsCollector::new(MetricsMode::Local)),
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
            Arc::new(RwLock::new(HashMapSparKV::with_config(Config {
                max_ttl: Duration::seconds(i64::try_from(max_ttl).unwrap_or_default()),
                max_items: capacity,
                earliest_expiration_eviction,
                ..Default::default()
            })))
        });

        Self {
            cache,
            issuer_epochs: Arc::default(),
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

    /// Looks up a JWT of the trusted issuer whose `iss` is `iss`. The key holds
    /// the issuer's current epoch, so a token validated against a status list
    /// that changes before the token is saved is saved under a key that
    /// [`Self::invalidate_issuer`] already retired.
    pub(crate) fn lookup_jwt(&self, kind: &TokenKind, jwt: &str, iss: &IssClaim) -> Lookup<'_> {
        let epoch = self
            .issuer_epochs
            .read()
            .expect("issuer epochs lock shouldn't be poisoned")
            .get(iss)
            .copied()
            .unwrap_or_default();
        self.lookup(hash_jwt_token(kind, jwt, Some(epoch)), CachedKind::Jwt)
    }

    /// Looks up a custom token. Its kind already names the processor and the
    /// policy store generation it was processed for, so a swap retires its key.
    pub(crate) fn lookup_custom(&self, kind: &TokenKind, jwt: &str) -> Lookup<'_> {
        self.lookup(hash_jwt_token(kind, jwt, None), CachedKind::Custom)
    }

    fn lookup(&self, key: String, kind: CachedKind) -> Lookup<'_> {
        let found = self.cache.as_ref().and_then(|cache| {
            cache
                .read()
                .expect("token cache mutex shouldn't be poisoned")
                .get(&key)
                .map(std::borrow::ToOwned::to_owned)
        });
        if let Some(token) = found {
            self.metrics.record_cache_hit();
            Lookup::Hit(token)
        } else {
            self.metrics.record_cache_miss();
            Lookup::Miss(CacheMiss {
                cache: self,
                key,
                kind,
            })
        }
    }

    /// Saves a token under `key` with appropriate TTL.
    ///
    /// The TTL is determined as follows:
    /// 1. If the token has a valid `exp` claim, the entry TTL is the time until
    ///    expiration (clamped by `max_ttl` when it is > 0).
    /// 2. If the token has no `exp` claim and `max_ttl` is > 0, `max_ttl` is used.
    /// 3. If `max_ttl` is 0, the token cache is disabled and no token is cached.
    ///
    /// `expiration`, when given, takes precedence over an `exp` claim: a
    /// [`CustomTokenProcessor`](crate::CustomTokenProcessor) reports it through
    /// [`ProcessedTokenClaims::expiration`](crate::ProcessedTokenClaims), leaving
    /// the claims it returns untouched.
    fn save(
        &self,
        key: &str,
        kind: CachedKind,
        token: Arc<Token>,
        now: DateTime<Utc>,
        expiration: Option<i64>,
    ) {
        let Some(cache) = &self.cache else {
            return;
        };

        let exp = TokenCache::effective_exp(&token, expiration);

        if exp.is_some_and(|exp| exp <= now.timestamp()) {
            // token is expired, no need to save it
            return;
        }

        let Some(duration) = self.cache_duration(exp) else {
            // No expiration and no configured max TTL — do not cache.
            return;
        };

        // Extract issuer for indexing; custom-token entries additionally carry a
        // marker index so `clear_custom_tokens` can flush them without touching JWTs.
        let mut index_keys =
            vec![IndexKey::Iss(token.normalized_issuer().into_owned()).index_value()];
        if kind == CachedKind::Custom {
            index_keys.push(IndexKey::Custom.index_value());
        }

        let result = cache
            .write()
            .expect("token cache mutex shouldn't be poisoned")
            .set_with_ttl(key, token, Duration::seconds(duration), &index_keys);
        if let Err(err) = result {
            self.log_warn(format!("could not set token to token cache: {err}"));
        }
    }

    /// The expiration that governs this token: an explicit out-of-band value when
    /// present, otherwise the `exp` claim.
    fn effective_exp(token: &Arc<Token>, expiration: Option<i64>) -> Option<i64> {
        expiration.or_else(|| {
            token
                .claims
                .get_claim("exp")
                .and_then(|exp| super::parse_numeric_date(exp.value()))
        })
    }

    /// Extract cache duration, result is optional
    fn cache_duration(&self, exp: Option<i64>) -> Option<i64> {
        let max_ttl = i64::try_from(self.max_ttl).ok().filter(|ttl| *ttl > 0);
        let Some(exp) = exp else {
            // if no exp claim, use the configured max ttl if set
            return max_ttl;
        };
        // Anchor the remaining lifetime to insert time (`Utc::now()`), not the
        // request-start `now`: sparkv stamps the entry from insert, so using the
        // older `now` would over-extend the TTL and let the cache serve a token the
        // fresh path already rejects as expired. An elapsed `exp` is never cached.
        let duration = exp - Utc::now().timestamp();
        (duration > 0).then(|| max_ttl.map_or(duration, |ttl| duration.min(ttl)))
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

    /// Drop every cached custom-token entry, leaving JWT entries untouched.
    pub(crate) fn clear_custom_tokens(&self) {
        let Some(cache) = &self.cache else {
            return;
        };

        cache
            .write()
            .expect("token cache mutex shouldn't be poisoned")
            .remove_by_index(&IndexKey::Custom.index_value());
    }

    /// Retires the cached tokens of `iss`: bumps its epoch, so no later lookup
    /// finds them or a token validated before this call, and frees their entries.
    pub(crate) fn invalidate_issuer(&self, iss: &IssClaim) {
        *self
            .issuer_epochs
            .write()
            .expect("issuer epochs lock shouldn't be poisoned")
            .entry(iss.clone())
            .or_default() += 1;

        let Some(cache) = &self.cache else {
            return;
        };
        cache
            .write()
            .expect("token cache mutex shouldn't be poisoned")
            .remove_by_index(&IndexKey::Iss(iss.clone()).index_value());
    }
}

/// What [`TokenCache::lookup_jwt`] or [`TokenCache::lookup_custom`] found.
pub(crate) enum Lookup<'a> {
    Hit(Arc<Token>),
    Miss(CacheMiss<'a>),
}

/// A token that was not cached. It is saved through this miss, under the key
/// the lookup computed before the token was validated.
pub(crate) struct CacheMiss<'a> {
    cache: &'a TokenCache,
    key: String,
    kind: CachedKind,
}

/// Which lookup a [`CacheMiss`] came from; custom-token entries carry a marker
/// index so `clear_custom_tokens` can flush them without touching JWTs.
#[derive(Debug, Clone, Copy, PartialEq)]
enum CachedKind {
    Jwt,
    Custom,
}

impl CacheMiss<'_> {
    pub(crate) fn save(self, token: Arc<Token>, now: DateTime<Utc>) {
        self.cache.save(&self.key, self.kind, token, now, None);
    }

    /// Saves a token whose expiration is supplied out-of-band rather than read
    /// from an `exp` claim; `None` falls back to the claim.
    pub(crate) fn save_with_expiration(
        self,
        token: Arc<Token>,
        now: DateTime<Utc>,
        expiration: Option<i64>,
    ) {
        self.cache
            .save(&self.key, self.kind, token, now, expiration);
    }
}

/// Hash a string using `ahash` and return the hash value as a string.
///
/// Pre-hashing JWT keys reduces their size from potentially kilobytes down to
/// ~20 bytes, saving memory in the backing `HashMap` and making subsequent hash
/// computations cheaper during lookups.
fn hash_jwt_token(kind: &TokenKind, jwt: &str, issuer_epoch: Option<u64>) -> String {
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
    issuer_epoch.hash(&mut hasher);

    hasher.finish().to_string()
}

/// [`IndexKey`] is structure that is used for indexing [`TokenCache`]
//
// thiserror is used for usefull string conversation
#[derive(Debug, derive_more::Display)]
pub(crate) enum IndexKey {
    #[display("iss:{_0}")]
    Iss(IssClaim),
    /// Marker index attached to custom-token entries so `clear_custom_tokens` can flush
    /// them together (e.g. on a processor swap) without evicting JWT entries.
    #[display("custom")]
    Custom,
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
    use crate::authz::metrics::MetricsMode;
    use crate::common::policy_store::TrustedIssuer;
    use crate::jwt::TokenIssuer;
    use crate::jwt::token::TokenClaims;
    use serde_json::{Value, json};
    use std::collections::HashMap;

    fn token_cache(max_ttl: usize) -> TokenCache {
        TokenCache::new(
            max_ttl,
            100,
            true,
            None,
            Arc::new(MetricsCollector::new(MetricsMode::Local)),
        )
    }

    fn test_issuer() -> TokenIssuer {
        TokenIssuer::Jwt(Arc::new(TrustedIssuer::default()))
    }

    fn token(claims: HashMap<String, Value>) -> Arc<Token> {
        Arc::new(Token::new(TokenClaims::from(claims), test_issuer()))
    }

    fn token_with_exp(now: DateTime<Utc>, duration_secs: i64) -> Arc<Token> {
        token(HashMap::from([(
            "exp".to_string(),
            json!(now.timestamp() + duration_secs),
        )]))
    }

    fn access_token_kind() -> TokenKind<'static> {
        TokenKind::AuthorizeMultiIssuer("Acme::Access_Token".into())
    }

    fn iss(value: &str) -> IssClaim {
        IssClaim::new(value)
    }

    impl<'a> Lookup<'a> {
        fn hit(self) -> Option<Arc<Token>> {
            match self {
                Lookup::Hit(token) => Some(token),
                Lookup::Miss(_) => None,
            }
        }

        fn miss(self) -> Option<CacheMiss<'a>> {
            match self {
                Lookup::Hit(_) => None,
                Lookup::Miss(miss) => Some(miss),
            }
        }
    }

    /// A token validated before its issuer's cached tokens were invalidated, for
    /// example against a status list that a refresh then replaced, is saved
    /// under a retired key, so it is not served.
    #[test]
    fn token_validated_before_its_issuer_was_invalidated_is_not_served() {
        let now = Utc::now();
        let cache = token_cache(600);
        let kind = access_token_kind();
        let idp = iss("https://idp.example.com");

        let miss = cache
            .lookup_jwt(&kind, "jwt", &idp)
            .miss()
            .expect("nothing is cached yet");
        cache.invalidate_issuer(&idp);
        miss.save(token_with_exp(now, 3600), now);

        assert!(
            cache.lookup_jwt(&kind, "jwt", &idp).hit().is_none(),
            "a token validated before the invalidation must not be served"
        );

        cache
            .lookup_jwt(&kind, "jwt", &idp)
            .miss()
            .expect("the stale token is not served")
            .save(token_with_exp(now, 3600), now);
        assert!(
            cache.lookup_jwt(&kind, "jwt", &idp).hit().is_some(),
            "a token validated after the invalidation is served"
        );
    }

    /// Invalidating one issuer leaves the tokens of another, including one being
    /// validated meanwhile.
    #[test]
    fn invalidating_an_issuer_keeps_tokens_of_another() {
        let now = Utc::now();
        let cache = token_cache(600);
        let kind = access_token_kind();
        let acme = iss("https://acme.example.com");
        let other = iss("https://other.example.com");

        cache
            .lookup_jwt(&kind, "cached", &acme)
            .miss()
            .expect("nothing is cached yet")
            .save(token_with_exp(now, 3600), now);
        let in_flight = cache
            .lookup_jwt(&kind, "in-flight", &acme)
            .miss()
            .expect("nothing is cached yet");

        cache.invalidate_issuer(&other);
        in_flight.save(token_with_exp(now, 3600), now);

        assert!(
            cache.lookup_jwt(&kind, "cached", &acme).hit().is_some(),
            "a token of another issuer stays cached"
        );
        assert!(
            cache.lookup_jwt(&kind, "in-flight", &acme).hit().is_some(),
            "a token of another issuer validated meanwhile is cached"
        );
    }

    #[test]
    fn clear_custom_tokens_evicts_only_custom_entries() {
        let now = Utc::now();
        let cache = token_cache(600);
        let token = token_with_exp(now, 3600);

        let jwt_kind = TokenKind::AuthorizeMultiIssuer("Acme::Access_Token".into());
        let custom_kind = TokenKind::AuthorizeCustom {
            mapping: "Acme::ApiKey".into(),
            generation: 1,
            processor: 0,
        };

        let idp = iss("https://idp.example.com");
        cache
            .lookup_jwt(&jwt_kind, "jwt-payload", &idp)
            .miss()
            .expect("nothing is cached yet")
            .save(token.clone(), now);
        cache
            .lookup_custom(&custom_kind, "custom-payload")
            .miss()
            .expect("nothing is cached yet")
            .save(token, now);

        cache.clear_custom_tokens();

        assert!(
            cache
                .lookup_custom(&custom_kind, "custom-payload")
                .hit()
                .is_none(),
            "clear_custom_tokens should evict custom-token entries"
        );
        assert!(
            cache
                .lookup_jwt(&jwt_kind, "jwt-payload", &idp)
                .hit()
                .is_some(),
            "clear_custom_tokens should leave JWT entries cached"
        );
    }

    #[test]
    fn max_ttl_zero_disables_cache_for_token_with_exp() {
        let now = Utc::now();
        let cache = token_cache(0);
        let token = token_with_exp(now, 3600);

        let idp = iss("https://idp.example.com");
        cache
            .lookup_jwt(&TokenKind::StatusList, "jwt", &idp)
            .miss()
            .expect("a disabled cache finds nothing")
            .save(token, now);

        assert!(
            cache
                .lookup_jwt(&TokenKind::StatusList, "jwt", &idp)
                .hit()
                .is_none(),
            "max_ttl=0 should disable token cache even when the token has exp"
        );
    }

    #[test]
    fn max_ttl_zero_does_not_cache_token_without_exp() {
        let now = Utc::now();
        let cache = token_cache(0);
        let token = token(HashMap::new());

        let idp = iss("https://idp.example.com");
        cache
            .lookup_jwt(&TokenKind::StatusList, "jwt", &idp)
            .miss()
            .expect("a disabled cache finds nothing")
            .save(token, now);

        assert!(
            cache
                .lookup_jwt(&TokenKind::StatusList, "jwt", &idp)
                .hit()
                .is_none(),
            "max_ttl=0 should not cache tokens without exp"
        );
    }

    #[test]
    fn positive_max_ttl_caps_token_expiration() {
        let now = Utc::now();
        let cache = token_cache(5);
        let token = token_with_exp(now, 3600);

        assert_eq!(
            cache.cache_duration(TokenCache::effective_exp(&token, None)),
            Some(5),
            "positive max_ttl should cap the cache duration for tokens with exp"
        );
    }

    #[test]
    fn elapsed_expiration_is_not_given_max_ttl() {
        let cache = token_cache(300);

        assert_eq!(
            cache.cache_duration(Some(Utc::now().timestamp() - 1)),
            None,
            "an elapsed exp must not fall back to max_ttl"
        );
    }

    #[test]
    fn explicit_expiration_overrides_exp_claim() {
        let now = Utc::now();
        let cache = token_cache(0);
        let token = token_with_exp(now, 3600);

        assert_eq!(
            cache.cache_duration(TokenCache::effective_exp(
                &token,
                Some(now.timestamp() + 10)
            ),),
            Some(10),
            "an out-of-band expiration should win over the token's own exp claim"
        );
    }

    #[test]
    fn explicit_expiration_used_when_no_exp_claim() {
        let now = Utc::now();
        let cache = token_cache(0);
        let token = Arc::new(Token::new(TokenClaims::default(), test_issuer()));

        assert_eq!(
            cache.cache_duration(TokenCache::effective_exp(
                &token,
                Some(now.timestamp() + 30)
            ),),
            Some(30),
            "a claim-less token should still get a TTL from its out-of-band expiration"
        );
    }
}
