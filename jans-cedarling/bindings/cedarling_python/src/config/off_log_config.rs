/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use pyo3::prelude::*;

/// DisabledLoggingConfig
/// ============
///
/// `DisabledLoggingConfig` is a Python wrapper around the Rust `cedarling::LogTypeConfig` struct.
/// This configuration represents the "Off" log setting, where the logger is effectively disabled, and all log entries are ignored.
///
/// Class Definition
/// ----------------
///
/// .. class:: DisabledLoggingConfig()
///
///     The `DisabledLoggingConfig` class is used when logging is turned off. This configuration disables logging, meaning that no logs are captured or stored.
///
///     This configuration is invariant, meaning once created, it remains constant and cannot be modified.
///
/// Methods
/// -------
///
/// .. method:: __init__(self)
///
///     Initializes a new instance of the `DisabledLoggingConfig` class. This effectively disables logging.
///
/// Example
/// -------
///
/// .. code-block:: python
///
///     # Creating a new DisabledLoggingConfig instance to disable logging
///     config = DisabledLoggingConfig()
///
#[derive(Debug, Clone)]
#[pyclass]
pub struct DisabledLoggingConfig;

#[pymethods]
impl DisabledLoggingConfig {
    #[new]
    fn new() -> DisabledLoggingConfig {
        DisabledLoggingConfig
    }
}

impl From<DisabledLoggingConfig> for cedarling::LogTypeConfig {
    fn from(_value: DisabledLoggingConfig) -> Self {
        Self::Off
    }
}
