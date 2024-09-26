/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use pyo3::prelude::*;

/// MemoryLogConfig
/// ===============
///
/// `MemoryLogConfig` is a Python wrapper around the Rust `cedarling::LogTypeConfig` struct.
///  It is used to configure memory-based logging and is part of the overall logging configuration within the `cedarling` system.
///  The configuration takes parameters from `cedarling::MemoryLogConfig`.
///
/// Class Definition
/// ----------------
///
/// .. class:: MemoryLogConfig(log_ttl=60)
///
///     The `MemoryLogConfig` class allows you to configure memory logging settings, particularly the time-to-live (TTL) for log entries.
///
///     :param log_ttl: Optional. The maximum time to live (in seconds) of log entries. Defaults to `60` seconds (1 minute).
///
/// Attributes
/// ----------
///
/// .. attribute:: log_ttl
///
///     The time-to-live (TTL) for log entries in memory, measured in seconds. This represents the `CEDARLING_LOG_TTL` setting from the `bootstrap properties` as defined in the `cedarling` documentation.
///
///     :type: int
///
/// Methods
/// -------
///
/// .. method:: __init__(self, log_ttl=60)
///
///     Initializes a new instance of the `MemoryLogConfig` class.
///
///     :param log_ttl: Optional. The time-to-live (in seconds) for log entries. Defaults to `60` seconds (1 minute).
///
/// Example
/// -------
///
/// ```python
///
///     # Creating a new MemoryLogConfig instance with the default TTL
///     config = MemoryLogConfig()
///
///     # Creating a new MemoryLogConfig instance with a custom TTL
///     config = MemoryLogConfig(log_ttl=120)
///
///     # Accessing the log_ttl attribute
///     print(config.log_ttl)
///
///     # Setting a new TTL value
///     config.log_ttl = 300
/// ```
///
#[derive(Debug, Clone)]
#[pyclass(get_all, set_all)]
pub struct MemoryLogConfig {
    log_ttl: u64,
}

#[pymethods]
impl MemoryLogConfig {
    /// log_ttl - represent`CEDARLING_LOG_TTL` in [bootstrap properties](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties) documentation.
    /// The maximum time to live (in seconds) of the log entries.
    /// The default value is 1 minute.
    #[new]
    #[pyo3(signature = (log_ttl=60))]
    fn new(log_ttl: u64) -> PyResult<Self> {
        Ok(MemoryLogConfig { log_ttl })
    }
}

impl From<MemoryLogConfig> for cedarling::LogTypeConfig {
    fn from(value: MemoryLogConfig) -> Self {
        Self::Memory(cedarling::MemoryLogConfig {
            log_ttl: value.log_ttl,
        })
    }
}
