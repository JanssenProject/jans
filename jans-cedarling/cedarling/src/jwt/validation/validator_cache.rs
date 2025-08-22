// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::jwt::log_entry::JwtLogEntry;
use crate::jwt::{IssuerConfig, StatusListCache};
use crate::common::issuer_utils::normalize_issuer;
use crate::log::Logger;
use crate::{JwtConfig, LogLevel, LogWriter};

use super::*;
use jsonwebtoken::Algorithm;
use std::collections::HashMap;
use std::fmt::Display;
use std::hash::{DefaultHasher, Hash, Hasher};
use std::sync::{Arc, RwLock};

pub type CachedValidator = Arc<RwLock<JwtValidator>>;

/// Holds a collection of JWT validators keyed by a hash.
///
/// The internal storage maps a [`ValidatorKeyHash`] (a 64-bit hash) to a
/// vector of validator entries. Each entry is a tuple of:
/// - An owned version of the validator's identifying information
/// - The associated `JwtValidator`
///
/// If multiple validators share the same hash (i.e., hash collision),
/// we perform an additional full comparison via [`OwnedValidatorInfo::is_equal_to`].
#[derive(Default)]
pub struct JwtValidatorCache {
    validators: HashMap<ValidatorKeyHash, Vec<(OwnedValidatorInfo, CachedValidator)>>,
}

impl JwtValidatorCache {
    /// Initializes the validators for the given [`IssuerConfig`] and the global settings
    /// from [`JwtConfig`].
    pub fn init_for_iss(
        &mut self,
        iss_config: &IssuerConfig,
        jwt_config: &JwtConfig,
        status_lists: &StatusListCache,
        logger: Option<Logger>,
    ) {
        let iss = iss_config
            .openid_config
            .as_ref()
            .map(|oidc| normalize_issuer(&oidc.issuer))
            .unwrap_or_else(|| {
                normalize_issuer(&iss_config
                    .policy
                    .oidc_endpoint
                    .origin()
                    .ascii_serialization())
            });

        for (token_name, tkn_metadata) in iss_config.policy.token_metadata.iter() {
            if !tkn_metadata.trusted {
                logger.log_any(JwtLogEntry::new(
                    format!(
                        "skipping metadata for '{}' from '{}' since `trusted == false`",
                        token_name, iss_config.issuer_id,
                    ),
                    Some(LogLevel::INFO),
                ));
                continue;
            }

            for algorithm in jwt_config.signature_algorithms_supported.iter().copied() {
                let (validator, key) = JwtValidator::new_input_tkn_validator(
                    Some(&iss),
                    token_name,
                    tkn_metadata,
                    algorithm,
                    status_lists.clone(),
                    jwt_config.jwt_sig_validation,
                    jwt_config.jwt_status_validation,
                );

                self.insert(key, validator);
            }
        }

        if jwt_config.jwt_status_validation {
            for algorithm in jwt_config.signature_algorithms_supported.iter().copied() {
                let status_list_uri = iss_config
                    .openid_config
                    .as_ref()
                    .and_then(|conf| conf.status_list_endpoint.as_ref())
                    .map(|uri| uri.to_string());
                let (validator, key) = JwtValidator::new_status_list_tkn_validator(
                    Some(&iss),
                    status_list_uri,
                    algorithm,
                    jwt_config.jwt_sig_validation,
                );

                self.insert(key, validator);
            }
        }
    }

    /// Inserts a new validator into the store.
    ///
    /// If a validator with the same `ValidatorKeyHash` already exists, it is
    /// appended to the vector. Exact match resolution is deferred to lookup time.
    fn insert(&mut self, validator_info: ValidatorInfo<'_>, validator: JwtValidator) {
        let key = validator_info.key_hash();
        self.validators
            .entry(key)
            .or_default()
            .push((validator_info.owned(), Arc::new(RwLock::new(validator))));
    }

    /// Retrieves a validator matching the given `ValidatorInfo`, if present.
    ///
    /// Performs a fast hash-based lookup. If multiple entries are found under
    /// the same hash (due to collisions), performs full field comparisons to
    /// find an exact match.
    pub fn get(&self, validator_info: &ValidatorInfo<'_>) -> Option<Arc<RwLock<JwtValidator>>> {
        let validators = self.validators.get(&validator_info.key_hash())?;

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
#[derive(Hash, Clone, Copy)]
pub struct ValidatorInfo<'a> {
    /// Optional issuer string (typically from a JWT "iss" claim).
    pub iss: Option<&'a str>,
    /// The token name (e.g., audience or application-specific).
    pub token_kind: TokenKind<'a>,
    /// The algorithm used to sign the token.
    pub algorithm: Algorithm,
}

#[derive(Hash, Clone, Copy, PartialEq)]
pub enum TokenKind<'a> {
    /// A token that's provided by the user through the [`authorize`] function.
    ///
    /// [`authorize`]: crate::Cedarling::authorize
    AuthzRequestInput(&'a str),
    /// A statuslist JWT.
    StatusList,
}

impl Display for TokenKind<'_> {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            TokenKind::AuthzRequestInput(tkn_name) => write!(f, "{tkn_name}"),
            TokenKind::StatusList => write!(f, "statuslist+jwt"),
        }
    }
}

/// Owned version of [`ValidatorInfo`] used to store entries inside `ValidatorStore`.
#[derive(Debug)]
pub struct OwnedValidatorInfo {
    iss: Option<String>,
    token_kind: OwnedTokenKind,
    algorithm: Algorithm,
}

#[derive(Debug, PartialEq)]
pub enum OwnedTokenKind {
    /// A token that's provided by the user through the [`authorize`] function.
    ///
    /// [`authorize`]: crate::Cedarling::authorize
    AuthzRequestInput(String),
    /// A statuslist JWT.
    StatusList,
}

impl From<TokenKind<'_>> for OwnedTokenKind {
    fn from(tkn_kind: TokenKind<'_>) -> Self {
        match tkn_kind {
            TokenKind::AuthzRequestInput(tkn_name) => Self::AuthzRequestInput(tkn_name.to_string()),
            TokenKind::StatusList => Self::StatusList,
        }
    }
}

impl OwnedTokenKind {
    /// Checks for full equality with a [`TokenKind`].
    ///
    /// Used to resolve hash collisions in the store.
    fn is_equal_to(&self, other: &TokenKind<'_>) -> bool {
        match (self, other) {
            (
                OwnedTokenKind::AuthzRequestInput(tkn_name_string),
                TokenKind::AuthzRequestInput(tkn_name_str),
            ) => tkn_name_string.as_str() == *tkn_name_str,
            (OwnedTokenKind::StatusList, TokenKind::StatusList) => true,
            _ => false,
        }
    }
}

/// Hash wrapper used as a key in the internal `HashMap`.
///
/// The hash is generated from the contents of a `ValidatorInfo`.
#[derive(Hash, Eq, PartialEq)]
struct ValidatorKeyHash(u64);

impl ValidatorInfo<'_> {
    /// Creates an [`OwnedValidatorInfo`] for storage.
    pub fn owned(&self) -> OwnedValidatorInfo {
        OwnedValidatorInfo {
            iss: self.iss.map(|s| s.to_string()),
            token_kind: self.token_kind.into(),
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
        if self.iss.as_deref() != other.iss {
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
    use crate::common::policy_store::{ClaimMappings, TokenEntityMetadata};
    use crate::jwt::validation::JwtValidator;
    use std::collections::HashSet;

    #[test]
    fn test_insert_and_retrieve() {
        let mut store = JwtValidatorCache::default();
        let (validator, info) = JwtValidator::new_input_tkn_validator(
            Some("test"),
            "access_tkn".into(),
            &TokenEntityMetadata {
                trusted: true,
                entity_type_name: "AccessToken".into(),
                principal_mapping: HashSet::default(),
                token_id: "iss".into(),
                user_id: None,
                role_mapping: None,
                workload_id: Some("aud".into()),
                claim_mapping: ClaimMappings::default(),
                required_claims: HashSet::new(),
            },
            jsonwebtoken::Algorithm::HS256,
            StatusListCache::default(),
            true,
            false,
        );

        store.insert(info, validator.clone());

        assert!(store.get(&info).is_some());
    }
}
