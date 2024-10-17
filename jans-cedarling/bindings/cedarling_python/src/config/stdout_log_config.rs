/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use pyo3::prelude::*;

/// StdOutLogConfig
/// ================
///
/// A Python wrapper for the Rust `cedarling::LogTypeConfig` struct.
/// Represents the configuration for logging to the standard output stream.
///
/// Attributes
/// ----------
/// This configuration is constant and cannot be modified.
///
/// Example
/// -------
/// ```
/// # Create an instance for logging to standard output
/// config = StdOutLogConfig()
/// ```
#[derive(Debug, Clone)]
#[pyclass]
pub struct StdOutLogConfig;

#[pymethods]
impl StdOutLogConfig {
    #[new]
    fn new() -> StdOutLogConfig {
        StdOutLogConfig
    }
}

impl From<StdOutLogConfig> for cedarling::LogTypeConfig {
    fn from(_value: StdOutLogConfig) -> Self {
        Self::StdOut
    }
}
