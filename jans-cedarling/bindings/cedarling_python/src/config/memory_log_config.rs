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
/// A Python wrapper for the Rust `cedarling::LogTypeConfig`, used to configure memory-based logging.
///
/// Attributes
/// ----------
/// :param log_ttl: Optional TTL for log entries (in seconds), default is `60`.
///
/// Example
/// -------
/// ```
/// # Initialize with default TTL
/// config = MemoryLogConfig()              
/// # Initialize with custom TTL
/// config = MemoryLogConfig(log_ttl=120)   
/// print(config.log_ttl)                    # Accessing TTL
/// config.log_ttl = 300                     # Updating TTL
/// ```
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
