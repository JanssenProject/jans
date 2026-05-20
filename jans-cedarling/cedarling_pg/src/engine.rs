// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Lazy, process-wide [`cedarling::blocking::Cedarling`] built from `cedarling.bootstrap_config`.

use std::sync::{Arc, Mutex, MutexGuard};

use cedarling::blocking::Cedarling;
use thiserror::Error;

use crate::guc_config;

struct EngineInner {
    current: Arc<Cedarling>,
    path: String,
    previous: Option<(Arc<Cedarling>, String)>,
}

enum EngineSlot {
    Empty,
    Ready(EngineInner),
}

static ENGINE: Mutex<EngineSlot> = Mutex::new(EngineSlot::Empty);

#[derive(Debug, Error)]
pub enum EngineError {
    #[error("cedarling.bootstrap_config is not set or is whitespace only")]
    BootstrapPathNotSet,
    #[error(transparent)]
    BootstrapLoad(#[from] cedarling::BootstrapConfigLoadingError),
    #[error(transparent)]
    CedarlingInit(#[from] cedarling::InitCedarlingError),
    #[error("internal Cedarling engine mutex was poisoned")]
    MutexPoisoned,
}

/// Loads Cedarling from a bootstrap file. Used by the global slot and by `use_policy`.
pub fn try_init_cedarling_from_bootstrap_path(path: &str) -> Result<Arc<Cedarling>, EngineError> {
    let config = cedarling::BootstrapConfig::load_from_file(path)?;
    let instance = Cedarling::new(&config)?;
    Ok(Arc::new(instance))
}

/// Returns the process-wide Cedarling engine, initializing it on first call from
/// [`guc_config::bootstrap_config_path_utf8`]. On failure the slot stays `Empty` so the
/// next call re-attempts initialization (no failure is cached).
pub fn global_cedarling() -> Result<Arc<Cedarling>, EngineError> {
    let mut slot = lock_engine_slot()?;
    if let EngineSlot::Ready(inner) = &*slot {
        return Ok(inner.current.clone());
    }

    let path = guc_config::bootstrap_config_path_utf8().ok_or(EngineError::BootstrapPathNotSet)?;
    let trimmed = path.trim();
    if trimmed.is_empty() {
        return Err(EngineError::BootstrapPathNotSet);
    }

    let arc = try_init_cedarling_from_bootstrap_path(trimmed)?;
    *slot = EngineSlot::Ready(EngineInner {
        current: arc.clone(),
        path: trimmed.to_string(),
        previous: None,
    });
    Ok(arc)
}

/// Atomically swap in a new engine loaded from `new_path`.
///
/// The new engine is built *before* the lock is acquired so the lock hold time is minimal.
/// Returns the previous bootstrap path (for audit logging), or `None` if there was none.
pub fn use_policy(new_path: &str) -> Result<Option<String>, EngineError> {
    let new_arc = try_init_cedarling_from_bootstrap_path(new_path)?;
    let mut slot = lock_engine_slot()?;
    let old_path = match &*slot {
        EngineSlot::Ready(inner) => Some(inner.path.clone()),
        EngineSlot::Empty => None,
    };
    let old_inner = match std::mem::replace(&mut *slot, EngineSlot::Empty) {
        EngineSlot::Ready(inner) => Some((inner.current, inner.path)),
        EngineSlot::Empty => None,
    };
    *slot = EngineSlot::Ready(EngineInner {
        current: new_arc,
        path: new_path.to_string(),
        previous: old_inner,
    });
    Ok(old_path)
}

/// Revert to the previous engine.
///
/// Returns `Some((rolled_back_to_path, rolled_back_from_path))` on success,
/// or `None` if there is no previous engine stored.
pub fn rollback_policy() -> Result<Option<(String, String)>, EngineError> {
    let mut slot = lock_engine_slot()?;
    let has_previous = matches!(&*slot, EngineSlot::Ready(inner) if inner.previous.is_some());
    if !has_previous {
        return Ok(None);
    }
    if let EngineSlot::Ready(inner) = std::mem::replace(&mut *slot, EngineSlot::Empty) {
        let old_path = inner.path.clone();
        if let Some((prev_engine, prev_path)) = inner.previous {
            let new_path = prev_path.clone();
            *slot = EngineSlot::Ready(EngineInner {
                current: prev_engine,
                path: prev_path,
                previous: None,
            });
            return Ok(Some((new_path, old_path)));
        }
    }
    Ok(None)
}

/// Returns the loaded engine without triggering initialization. Returns `None` if uninitialized, failed, or mutex poisoned.
#[cfg(not(test))]
pub fn peek_cedarling() -> Option<std::sync::Arc<cedarling::blocking::Cedarling>> {
    ENGINE.lock().ok().and_then(|slot| match &*slot {
        EngineSlot::Ready(inner) => Some(inner.current.clone()),
        EngineSlot::Empty => None,
    })
}

fn lock_engine_slot() -> Result<MutexGuard<'static, EngineSlot>, EngineError> {
    crate::sync_mutex::lock(&ENGINE).map_err(|_| EngineError::MutexPoisoned)
}

/// Clears the process-wide engine slot so later `#[pg_test]` cases start without a loaded engine.
#[cfg(feature = "pg_test")]
pub(crate) fn reset_for_pg_tests() {
    if let Ok(mut slot) = crate::sync_mutex::lock(&ENGINE) {
        *slot = EngineSlot::Empty;
    }
}
