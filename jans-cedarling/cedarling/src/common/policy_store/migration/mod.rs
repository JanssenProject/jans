// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! In-memory migration between adjacent policy store formats.
//!
//! Each `vN_to_vM` step implements [`Upgrade`] for the older version's doc and
//! depends only on its two version modules. [`IntoCurrent`] chains the steps,
//! so every supported version reaches the current one without a hand-written
//! path per version, and a missing step fails to compile where that version
//! is dispatched.

pub(crate) mod v0_to_v1;
pub(crate) mod v1_to_v2;

use crate::common::policy_store::InvalidConfigurationEndpoint;
use crate::common::policy_store::formats::{PolicyStoreWarning, current};

/// The doc of the current spec version, the only one that converts into
/// runtime types.
pub(super) type CurrentDoc = current::PolicyStoreDoc;

/// A store that cannot be expressed in the next spec version.
#[derive(Debug, thiserror::Error)]
pub enum MigrationError {
    #[error(
        "issuer '{name}' in '{file}' must be a single Cedar identifier to migrate: it is the \
         namespace of its TrustedIssuer entity and the prefix of its context.tokens keys"
    )]
    InvalidIssuerName { file: String, name: String },
    #[error("'{entity_type}' in '{file}' is not a valid Cedar entity type: {reason}")]
    InvalidEntityType {
        file: String,
        entity_type: String,
        reason: String,
    },
    #[error("invalid issuer in '{file}': {source}")]
    InvalidConfigurationEndpoint {
        file: String,
        source: InvalidConfigurationEndpoint,
    },
    #[error("entity type '{entity_type}' is declared in both '{file_a}' and '{file_b}'")]
    DuplicateEntityType {
        entity_type: String,
        file_a: String,
        file_b: String,
    },
}

/// One migration step, from a spec version's doc to the next version's.
///
/// Written by hand for each step: how fields map between two versions is a
/// decision, not something to derive. A step reports what it had to change in
/// behavior through `warnings`, and fails when the store cannot be expressed.
pub(super) trait Upgrade {
    type Next;

    fn upgrade(self, warnings: &mut Vec<PolicyStoreWarning>) -> Result<Self::Next, MigrationError>;
}

/// Brings the doc of any supported spec version up to [`CurrentDoc`] by
/// applying each [`Upgrade`] step in turn.
pub(super) trait IntoCurrent {
    fn into_current(
        self,
        warnings: &mut Vec<PolicyStoreWarning>,
    ) -> Result<CurrentDoc, MigrationError>;
}

/// The chain ends at the current version.
///
/// This impl and the blanket one below would overlap if [`CurrentDoc`] ever
/// implemented [`Upgrade`]. It cannot: the current version is the newest, so
/// there is no version to upgrade it to. Each bump moves both the `Upgrade`
/// impl and this one forward together.
impl IntoCurrent for CurrentDoc {
    fn into_current(
        self,
        _warnings: &mut Vec<PolicyStoreWarning>,
    ) -> Result<CurrentDoc, MigrationError> {
        Ok(self)
    }
}

impl<T> IntoCurrent for T
where
    T: Upgrade,
    T::Next: IntoCurrent,
{
    fn into_current(
        self,
        warnings: &mut Vec<PolicyStoreWarning>,
    ) -> Result<CurrentDoc, MigrationError> {
        self.upgrade(warnings)?.into_current(warnings)
    }
}
