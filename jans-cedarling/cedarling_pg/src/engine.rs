// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Lazy, process-wide [`cedarling::blocking::Cedarling`] built from `cedarling.bootstrap_config`.

use std::sync::{Arc, Mutex, MutexGuard, PoisonError};

use cedarling::blocking::Cedarling;
use thiserror::Error;

use crate::guc_config;

/// One-time or cached initialization of the blocking Cedarling engine.
enum EngineSlot {
    Empty,
    Ready(Arc<Cedarling>),
    /// Cached hard failure (e.g. bad bootstrap file); operator must fix config and restart `PostgreSQL`.
    Failed(String),
}

static ENGINE: Mutex<EngineSlot> = Mutex::new(EngineSlot::Empty);

/// Errors while resolving the global Cedarling instance.
#[derive(Debug, Error)]
pub enum EngineError {
    #[error("cedarling.bootstrap_config is not set or is whitespace only")]
    BootstrapPathNotSet,
    #[error(transparent)]
    BootstrapLoad(#[from] cedarling::BootstrapConfigLoadingError),
    #[error(transparent)]
    CedarlingInit(#[from] cedarling::InitCedarlingError),
    #[error("Cedarling initialization failed earlier in this process: {0}")]
    InitPreviouslyFailed(String),
    #[error("internal Cedarling engine mutex was poisoned")]
    MutexPoisoned,
}

/// Loads Cedarling from a bootstrap file (YAML / JSON / TOML per Cedarling). For unit tests and tools.
pub fn try_init_cedarling_from_bootstrap_path(path: &str) -> Result<Arc<Cedarling>, EngineError> {
    let config = cedarling::BootstrapConfig::load_from_file(path)?;
    let instance = Cedarling::new(&config)?;
    Ok(Arc::new(instance))
}

/// Returns the process-wide Cedarling engine, initializing it on first call from [`guc_config::bootstrap_config_path_utf8`].
pub fn global_cedarling() -> Result<Arc<Cedarling>, EngineError> {
    let mut slot = lock_engine_slot()?;
    match &*slot {
        EngineSlot::Ready(arc) => return Ok(arc.clone()),
        EngineSlot::Failed(msg) => return Err(EngineError::InitPreviouslyFailed(msg.clone())),
        EngineSlot::Empty => {},
    }

    let path = guc_config::bootstrap_config_path_utf8().ok_or(EngineError::BootstrapPathNotSet)?;
    let trimmed = path.trim();
    if trimmed.is_empty() {
        return Err(EngineError::BootstrapPathNotSet);
    }

    match try_init_cedarling_from_bootstrap_path(trimmed) {
        Ok(arc) => {
            *slot = EngineSlot::Ready(arc.clone());
            Ok(arc)
        },
        Err(e) => {
            let msg = e.to_string();
            *slot = EngineSlot::Failed(msg.clone());
            Err(e)
        },
    }
}

fn lock_engine_slot() -> Result<MutexGuard<'static, EngineSlot>, EngineError> {
    ENGINE
        .lock()
        .map_err(|_: PoisonError<_>| EngineError::MutexPoisoned)
}
