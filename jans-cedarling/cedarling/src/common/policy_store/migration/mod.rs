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

use crate::common::policy_store::formats::current;

/// The doc of the current spec version, the only one that converts into
/// runtime types.
pub(super) type CurrentDoc = current::PolicyStoreDoc;

/// One migration step, from a spec version's doc to the next version's.
///
/// Written by hand for each step: how fields map between two versions is a
/// decision, not something to derive.
pub(super) trait Upgrade {
    type Next;

    fn upgrade(self) -> Self::Next;
}

/// Brings the doc of any supported spec version up to [`CurrentDoc`] by
/// applying each [`Upgrade`] step in turn.
pub(super) trait IntoCurrent {
    fn into_current(self) -> CurrentDoc;
}

/// The chain ends at the current version.
///
/// This impl and the blanket one below would overlap if [`CurrentDoc`] ever
/// implemented [`Upgrade`]. It cannot: the current version is the newest, so
/// there is no version to upgrade it to. Each bump moves both the `Upgrade`
/// impl and this one forward together.
impl IntoCurrent for CurrentDoc {
    fn into_current(self) -> CurrentDoc {
        self
    }
}

impl<T> IntoCurrent for T
where
    T: Upgrade,
    T::Next: IntoCurrent,
{
    fn into_current(self) -> CurrentDoc {
        self.upgrade().into_current()
    }
}
