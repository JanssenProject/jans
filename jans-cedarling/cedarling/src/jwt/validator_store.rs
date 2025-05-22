// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::validation::JwtValidator;
use jsonwebtoken::Algorithm;
use std::collections::HashMap;
use std::fmt::Display;
use std::hash::{DefaultHasher, Hash, Hasher};

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
pub struct ValidatorStore(HashMap<ValidatorKeyHash, Vec<(OwnedValidatorInfo, JwtValidator)>>);

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
struct OwnedValidatorInfo {
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

impl ValidatorStore {
    /// Inserts a new validator into the store.
    ///
    /// If a validator with the same `ValidatorKeyHash` already exists, it is
    /// appended to the vector. Exact match resolution is deferred to lookup time.
    pub fn insert(&mut self, validator_info: ValidatorInfo<'_>, validator: JwtValidator) {
        let key = validator_info.key_hash();
        self.0
            .entry(key)
            .or_default()
            .push((validator_info.as_owned(), validator));
    }

    /// Retrieves a validator matching the given `ValidatorInfo`, if present.
    ///
    /// Performs a fast hash-based lookup. If multiple entries are found under
    /// the same hash (due to collisions), performs full field comparisons to
    /// find an exact match.
    pub fn get(&self, validator_info: &ValidatorInfo<'_>) -> Option<&JwtValidator> {
        let validators = self.0.get(&validator_info.key_hash())?;

        match validators.len() {
            0 => None,
            1 => Some(&validators[0].1),
            _ => validators
                .iter()
                .find(|(info, _)| info.is_equal_to(validator_info))
                .map(|(_, validator)| validator),
        }
    }
}

impl ValidatorInfo<'_> {
    /// Converts the borrowed validator info into an owned version for storage.
    fn as_owned(&self) -> OwnedValidatorInfo {
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

        if self.token_kind.is_equal_to(&other.token_kind) {
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
        let mut store = ValidatorStore::default();
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
            None,
        );

        store.insert(info, validator.clone());

        assert_eq!(store.get(&info), Some(&validator));
    }
}
