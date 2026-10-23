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

use std::convert::Infallible;

use crate::common::policy_store::formats::{PolicyStoreWarning, current};

/// The doc of the current spec version, the only one that converts into
/// runtime types.
pub(super) type CurrentDoc = current::PolicyStoreDoc;

/// A store that cannot be expressed in a later spec version, by the step that
/// failed. Each step owns its error type, so dropping a version removes its
/// step module and its variant here. The message names the step, so a store
/// migrated through several of them shows where it stopped.
#[derive(Debug, thiserror::Error)]
pub enum MigrationError {
    #[error("spec version 1 to 2: {0}")]
    V1ToV2(#[from] v1_to_v2::V1ToV2Error),
}

/// A step that cannot fail converts with no variant here.
impl From<Infallible> for MigrationError {
    fn from(never: Infallible) -> Self {
        match never {}
    }
}

/// One migration step, from a spec version's doc to the next version's.
///
/// Written by hand for each step: how fields map between two versions is a
/// decision, not something to derive. A step reports what it had to change in
/// behavior through `warnings`, and fails when the store cannot be expressed.
/// Both use the step's own types, so they leave with the step.
pub(super) trait Upgrade {
    type Next;
    type Error: Into<MigrationError>;
    type Warning: Into<PolicyStoreWarning>;

    fn upgrade(self, warnings: &mut Vec<Self::Warning>) -> Result<Self::Next, Self::Error>;
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
        let mut step_warnings = Vec::new();
        let next = self.upgrade(&mut step_warnings).map_err(Into::into)?;
        warnings.extend(step_warnings.into_iter().map(Into::into));
        next.into_current(warnings)
    }
}
