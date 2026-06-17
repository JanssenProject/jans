// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Process-local mutex helpers. On poison we fail closed and never use
//! `PoisonError::into_inner` (see REVIEW.md §13.5).

use std::sync::{Mutex, MutexGuard};

/// A mutex was poisoned (a panic occurred while the lock was held).
#[derive(Debug, Clone, Copy)]
pub(crate) struct MutexPoisoned;

pub(crate) fn lock<T>(mutex: &Mutex<T>) -> Result<MutexGuard<'_, T>, MutexPoisoned> {
    mutex.lock().map_err(|_| MutexPoisoned)
}
