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

use serde::{Deserialize, Serialize};

type HealthCheckFn = Arc<dyn Fn() -> HealthStatus + Send + Sync>;

#[derive(Debug, Copy, Clone, PartialEq, Eq, Deserialize, Serialize)]
#[serde(rename_all = "lowercase")]
pub(crate) enum HealthStatus {
    Success,
    Failure,
}

impl std::fmt::Display for HealthStatus {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            HealthStatus::Success => write!(f, "success"),
            HealthStatus::Failure => write!(f, "failure"),
        }
    }
}

/// A registered health check with a name and callback.
struct RegisteredCheck {
    name: String,
    check: HealthCheckFn,
}

/// Thread-safe registry of health check callbacks.
///
/// Created during `LockService` initialization, shared with cedarling subsystems
/// so they can register their health status callbacks.
#[derive(Clone)]
pub(crate) struct HealthRegistry {
    checks: Arc<RwLock<Vec<RegisteredCheck>>>,
}

impl Default for HealthRegistry {
    fn default() -> Self {
        Self::new()
    }
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
    /// The callback should return [`HealthStatus::Success`] if the subsystem is healthy,
    /// or [`HealthStatus::Failure`] if not.
    pub(crate) fn register<F>(&self, name: &str, check: F)
    where
        F: Fn() -> HealthStatus + Send + Sync + 'static,
    {
        let mut checks = self
            .checks
            .write()
            .expect("health registry write lock poisoned");
        checks.retain(|c| c.name != name);
        checks.push(RegisteredCheck {
            name: name.to_string(),
            check: Arc::new(check),
        });
    }

    /// Collect health statuses from all registered checks.
    ///
    /// Returns a map of component name to status.
    pub(crate) fn collect(&self) -> HashMap<String, HealthStatus> {
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

#[cfg(test)]
mod test {
    use super::*;

    #[test]
    fn test_collect_empty() {
        let registry = HealthRegistry::new();
        let result = registry.collect();
        assert!(result.is_empty(), "expected empty map for empty registry");
    }

    #[test]
    fn test_register_and_collect() {
        let registry = HealthRegistry::new();
        registry.register("core", || HealthStatus::Success);
        let result = registry.collect();
        assert_eq!(result.len(), 1, "expected one entry");
        assert_eq!(
            result.get("core"),
            Some(&HealthStatus::Success),
            "expected Success status for core"
        );
    }

    #[test]
    fn test_register_multiple_distinct() {
        let registry = HealthRegistry::new();
        registry.register("core", || HealthStatus::Success);
        registry.register("data", || HealthStatus::Success);
        registry.register("policy_store", || HealthStatus::Failure);
        let result = registry.collect();
        assert_eq!(result.len(), 3, "expected three distinct entries");
        assert_eq!(result.get("core"), Some(&HealthStatus::Success));
        assert_eq!(result.get("data"), Some(&HealthStatus::Success));
        assert_eq!(result.get("policy_store"), Some(&HealthStatus::Failure));
    }

    #[test]
    fn test_register_same_name_twice() {
        let registry = HealthRegistry::new();
        registry.register("core", || HealthStatus::Success);
        registry.register("core", || HealthStatus::Failure);
        let result = registry.collect();
        // the last insertion wins
        assert_eq!(result.len(), 1, "expected only one entry after dedup");
        assert_eq!(
            result.get("core"),
            Some(&HealthStatus::Failure),
            "expected the second registration to take precedence"
        );
    }

    #[test]
    fn test_register_same_name_mixed_statuses() {
        let registry = HealthRegistry::new();
        registry.register("core", || HealthStatus::Failure);
        registry.register("data", || HealthStatus::Success);
        registry.register("core", || HealthStatus::Success);
        let result = registry.collect();

        assert_eq!(result.len(), 2, "expected two deduplicated entries");
        assert_eq!(result.get("core"), Some(&HealthStatus::Success));
        assert_eq!(result.get("data"), Some(&HealthStatus::Success));
    }
}
