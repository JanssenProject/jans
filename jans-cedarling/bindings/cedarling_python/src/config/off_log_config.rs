/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use pyo3::prelude::*;

/// DisabledLoggingConfig
/// ======================
///
/// A Python wrapper for the Rust `cedarling::LogTypeConfig` struct.
/// This class configures logging to be disabled, meaning no log entries are captured.
///
/// Attributes
/// ----------
/// - `None`: This class has no attributes.
///
/// Example
/// -------
/// ```python
/// # Disable logging
/// config = DisabledLoggingConfig()
/// ```
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
