/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use pyo3::prelude::*;

/// StdOutLogConfig
/// ============
///
/// `StdOutLogConfig` is a Python wrapper around the Rust `cedarling::LogTypeConfig` struct.
/// This configuration represents the "StdOutLogConfig" log setting, where the logger writes log information to std output stream.
///
/// Class Definition
/// ----------------
///
/// .. class:: StdOutLogConfig()
///
///     The `StdOutLogConfig` class is used when we want to write log information to std output stream.
///
///     This configuration is invariant, meaning once created, it remains constant and cannot be modified.
///
/// Methods
/// -------
///
/// .. method:: __init__(self)
///
///     Initializes a new instance of the `StdOutLogConfig` class. This allows logger write logger to std output stream.
///
/// Example
/// -------
///
/// ```python
///
///     # Creating a new StdOutLogConfig instance to write log information to std output stream.
///     config = StdOutLogConfig()
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
