// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Shared health check registry for cedarling subsystems.
//!
//! Subsystems register health check callbacks when they are initialized.
//! The `HealthTicker` collects statuses from all registered checks on each tick.

use std::collections::HashMap;
use std::sync::{Arc, RwLock};

type HealthCheckFn = Arc<dyn Fn() -> String + Send + Sync>;

/// A registered health check with a name and callback.
struct RegisteredCheck {
    name: String,
    check: HealthCheckFn,
}

/// Thread-safe registry of health check callbacks.
///
/// Created during `LockService` initialization, shared with cedarling subsystems
/// so they can register their health status callbacks.
#[derive(Clone, Default)]
pub(crate) struct HealthRegistry {
    checks: Arc<RwLock<Vec<RegisteredCheck>>>,
}

impl std::fmt::Debug for HealthRegistry {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("HealthRegistry").finish_non_exhaustive()
    }
}

impl HealthRegistry {
    pub(crate) fn new() -> Self {
        Self {
            checks: Arc::new(RwLock::new(Vec::new())),
        }
    }

    /// Register a health check callback.
    ///
    /// The callback should return `"success"` if the subsystem is healthy,
    /// or `"failure"` (or another descriptive error string) if not.
    pub(crate) fn register<F>(&self, name: &str, check: F)
    where
        F: Fn() -> String + Send + Sync + 'static,
    {
        let mut checks = self
            .checks
            .write()
            .expect("health registry write lock poisoned");
        checks.push(RegisteredCheck {
            name: name.to_string(),
            check: Arc::new(check),
        });
    }

    /// Collect health statuses from all registered checks.
    ///
    /// Returns a map of component name to status string.
    pub(crate) fn collect(&self) -> HashMap<String, String> {
        let checks = self
            .checks
            .read()
            .expect("health registry read lock poisoned");
        checks
            .iter()
            .map(|c| (c.name.clone(), (c.check)()))
            .collect()
    }
}
