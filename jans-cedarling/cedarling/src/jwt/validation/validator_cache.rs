// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::common::issuer_utils::IssClaim;
use crate::jwt::log_entry::JwtLogEntry;
use crate::jwt::{IssuerConfig, StatusListCache};
use crate::log::Logger;
use crate::{JwtConfig, LogLevel, LogWriter};

use super::JwtValidator;
use arc_swap::ArcSwap;
use jsonwebtoken::Algorithm;
use std::borrow::Cow;
use std::collections::HashMap;
use std::fmt::Display;
use std::hash::{DefaultHasher, Hash, Hasher};
use std::sync::Arc;

type CachedValidator = Arc<JwtValidator>;

/// Holds a collection of JWT validators keyed by a hash.
///
/// The internal storage maps a [`ValidatorKeyHash`] (a 64-bit hash) to a
/// vector of validator entries. Each entry is a tuple of:
/// - An owned version of the validator's identifying information
/// - The associated `JwtValidator`
///
/// If multiple validators share the same hash (i.e., hash collision),
/// we perform an additional full comparison via [`OwnedValidatorInfo::is_equal_to`].
///
/// This structure is thread-safe.
pub(crate) struct JwtValidatorCache {
    validators: ArcSwap<HashMap<ValidatorKeyHash, Vec<(OwnedValidatorInfo, CachedValidator)>>>,
}

impl Default for JwtValidatorCache {
    fn default() -> Self {
        Self {
            validators: ArcSwap::from_pointee(HashMap::new()),
        }
    }
}

impl JwtValidatorCache {
    /// Initializes the validators for the given [`IssuerConfig`] and the global settings
    /// from [`JwtConfig`].
    pub(crate) fn init_for_iss(
        &self,
        iss_config: &IssuerConfig,
        jwt_config: &JwtConfig,
        status_lists: &StatusListCache,
        logger: Option<&Logger>,
    ) {
        // The loader rejects a discovery document whose `issuer` differs from this.
        let iss = iss_config.policy.iss_claim();

        for (entity_type, tkn_metadata) in &iss_config.tokens {
            logger.log_any(JwtLogEntry::new(
                    format!(
                        "creating validators for token: {entity_type} under issuer: {iss} with algorithms: {:?}",
                        jwt_config.signature_algorithms_supported
                    ),
                    Some(LogLevel::DEBUG),
                ));

            for algorithm in jwt_config.signature_algorithms_supported.iter().copied() {
                let (validator, key) = JwtValidator::new_multi_issuer_tkn_validator(
                    Some(iss),
                    entity_type.as_str(),
                    tkn_metadata,
                    algorithm,
                    status_lists.clone(),
                    jwt_config.jwt_sig_validation,
                    jwt_config.jwt_status_validation,
                );

                self.insert(&key, validator);
            }
        }

        if jwt_config.jwt_status_validation {
            for algorithm in jwt_config.signature_algorithms_supported.iter().copied() {
                let status_list_uri = iss_config
                    .openid_config
                    .as_ref()
                    .and_then(|conf| conf.status_list_endpoint.as_ref())
                    .map(std::string::ToString::to_string);
                let (validator, key) = JwtValidator::new_status_list_tkn_validator(
                    Some(iss),
                    status_list_uri,
                    algorithm,
                    jwt_config.jwt_sig_validation,
                );

                self.insert(&key, validator);
            }
        }
    }

    /// Inserts a new validator into the store.
    ///
    /// If a validator with the same `ValidatorKeyHash` already exists, it is
    /// appended to the vector. Exact match resolution is deferred to lookup time.
    ///
    /// Uses an atomic CAS loop (`rcu`) so concurrent inserts from parallel
    /// issuer-loading tasks are never lost.
    fn insert(&self, validator_info: &ValidatorInfo<'_>, validator: JwtValidator) {
        let key = validator_info.key_hash();
        let owned_info = validator_info.owned();
        let cached = Arc::new(validator);

        self.validators.rcu(|old| {
            let mut new_map = (**old).clone();
            new_map
                .entry(key.clone())
                .or_default()
                .push((owned_info.clone(), Arc::clone(&cached)));
            new_map
        });
    }

    /// Retrieves a validator matching the given `ValidatorInfo`, if present.
    ///
    /// Performs a fast hash-based lookup. If multiple entries are found under
    /// the same hash (due to collisions), performs full field comparisons to
    /// find an exact match.
    pub(crate) fn get(&self, validator_info: &ValidatorInfo<'_>) -> Option<Arc<JwtValidator>> {
        let guard = self.validators.load();
        let validators = guard.get(&validator_info.key_hash())?;

        match validators.len() {
            0 => None,
            1 => Some(validators[0].1.clone()),
            _ => validators
                .iter()
                .find(|(info, _)| info.is_equal_to(validator_info))
                .map(|(_, validator)| validator.clone()),
        }
    }
}

/// Lightweight view of validator identity used for lookup and insertion.
///
/// Holds borrowed data and can be hashed to a `ValidatorKeyHash`.
#[derive(Hash, Clone)]
pub(crate) struct ValidatorInfo<'a> {
    /// Optional issuer string (typically from a JWT "iss" claim).
    pub iss: Option<&'a IssClaim>,
    /// The token name (e.g., audience or application-specific).
    pub token_kind: TokenKind<'a>,
    /// The algorithm used to sign the token.
    pub algorithm: Algorithm,
}

#[derive(Hash, Clone, PartialEq)]
pub(crate) enum TokenKind<'a> {
    /// A statuslist JWT.
    StatusList,
    /// A token that's provided by the user through the [`authorize_multi_issuer`] function.
    ///
    /// [`authorize_multi_issuer`]: crate::Cedarling::authorize_multi_issuer
    AuthorizeMultiIssuer(Cow<'a, str>),
    /// A custom (non-JWT) token processed by a [`CustomTokenProcessor`](crate::CustomTokenProcessor)
    AuthorizeCustom(Cow<'a, str>),
}

impl Display for TokenKind<'_> {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            TokenKind::StatusList => write!(f, "statuslist+jwt"),
            TokenKind::AuthorizeMultiIssuer(tkn_name) | TokenKind::AuthorizeCustom(tkn_name) => {
                write!(f, "{tkn_name}")
            },
        }
    }
}

/// Owned version of [`ValidatorInfo`] used to store entries inside `ValidatorStore`.
#[derive(Debug, Clone)]
pub struct OwnedValidatorInfo {
    iss: Option<IssClaim>,
    token_kind: OwnedTokenKind,
    algorithm: Algorithm,
}

#[derive(Debug, Clone, PartialEq)]
pub(crate) enum OwnedTokenKind {
    /// A statuslist JWT.
    StatusList,
    /// A token that's provided by the user through the [`authorize_multi_issuer`] function.
    ///
    /// [`authorize_multi_issuer`]: crate::Cedarling::authorize_multi_issuer
    AuthorizeMultiIssuer(String),
    /// A custom (non-JWT) token processed by a [`CustomTokenProcessor`](crate::CustomTokenProcessor).
    AuthorizeCustom(String),
}

impl From<&TokenKind<'_>> for OwnedTokenKind {
    fn from(tkn_kind: &TokenKind<'_>) -> Self {
        match tkn_kind {
            TokenKind::StatusList => Self::StatusList,
            TokenKind::AuthorizeMultiIssuer(tkn_name) => {
                Self::AuthorizeMultiIssuer(tkn_name.to_string())
            },
            TokenKind::AuthorizeCustom(tkn_name) => Self::AuthorizeCustom(tkn_name.to_string()),
        }
    }
}

impl OwnedTokenKind {
    /// Checks for full equality with a [`TokenKind`].
    ///
    /// Used to resolve hash collisions in the store.
    fn is_equal_to(&self, other: &TokenKind<'_>) -> bool {
        match (self, other) {
            (OwnedTokenKind::StatusList, TokenKind::StatusList) => true,
            (
                OwnedTokenKind::AuthorizeMultiIssuer(tkn_name_string),
                TokenKind::AuthorizeMultiIssuer(tkn_name_str),
            )
            | (
                OwnedTokenKind::AuthorizeCustom(tkn_name_string),
                TokenKind::AuthorizeCustom(tkn_name_str),
            ) => tkn_name_string.as_str() == tkn_name_str,
            _ => false,
        }
    }
}

/// Hash wrapper used as a key in the internal `HashMap`.
///
/// The hash is generated from the contents of a `ValidatorInfo`.
#[derive(Clone, Hash, Eq, PartialEq)]
struct ValidatorKeyHash(u64);

impl ValidatorInfo<'_> {
    /// Creates an [`OwnedValidatorInfo`] for storage.
    pub(crate) fn owned(&self) -> OwnedValidatorInfo {
        OwnedValidatorInfo {
            iss: self.iss.map(std::borrow::ToOwned::to_owned),
            token_kind: (&self.token_kind).into(),
            algorithm: self.algorithm,
        }
    }

    /// Computes a 64-bit hash of the validator info for use in the map key.
    fn key_hash(&self) -> ValidatorKeyHash {
        // PERF: we can use a faster hasher if this is too slow but this might be good
        // enough for now
        let mut state = DefaultHasher::new();
        self.hash(&mut state);
        ValidatorKeyHash(state.finish())
    }
}

impl OwnedValidatorInfo {
    /// Checks for full equality with a borrowed `ValidatorInfo`.
    ///
    /// Used to resolve hash collisions in the store.
    fn is_equal_to(&self, other: &ValidatorInfo<'_>) -> bool {
        if self.iss.as_ref() != other.iss {
            return false;
        }

        if !self.token_kind.is_equal_to(&other.token_kind) {
            return false;
        }

        if self.algorithm != other.algorithm {
            return false;
        }

        true
    }
}

#[cfg(test)]
mod test {
    use super::*;
    use crate::common::policy_store::TokenEntityMetadata;
    use crate::jwt::validation::JwtValidator;
    use std::collections::HashSet;

    #[test]
    fn test_insert_and_retrieve() {
        let store = JwtValidatorCache::default();
        let test_iss = IssClaim::new("https://example.com/issuer");
        let (validator, info) = JwtValidator::new_multi_issuer_tkn_validator(
            Some(&test_iss),
            "Jans::Access_token",
            &TokenEntityMetadata {
                token_id: "iss".into(),
                required_claims: HashSet::new(),
            },
            jsonwebtoken::Algorithm::HS256,
            StatusListCache::default(),
            true,
            false,
        );

        store.insert(&info, validator.clone());

        assert!(store.get(&info).is_some());
    }
}
