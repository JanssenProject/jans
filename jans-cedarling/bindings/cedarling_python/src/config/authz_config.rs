/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use pyo3::exceptions::PyValueError;
use pyo3::prelude::*;

/// AuthzConfig
/// ============
///
/// A Python wrapper for the Rust `cedarling::AuthzConfig` struct.
/// Represents the authorization configuration with an `application_name`.
///
/// Attributes
/// ----------
/// :param application_name: Optional. Name of the application.
///
/// Example
/// -------
/// ```python
/// config = AuthzConfig(application_name="MyApp")
/// print(config.application_name)
/// ```
#[derive(Debug, Clone)]
#[pyclass(get_all, set_all)]
pub struct AuthzConfig {
    application_name: Option<String>,
}

#[pymethods]
impl AuthzConfig {
    #[new]
    #[pyo3(signature = (application_name=None))]
    fn new(application_name: Option<String>) -> PyResult<Self> {
        Ok(AuthzConfig { application_name })
    }
}

impl TryInto<cedarling::AuthzConfig> for AuthzConfig {
    type Error = PyErr;

    fn try_into(self) -> Result<cedarling::AuthzConfig, Self::Error> {
        Ok(cedarling::AuthzConfig {
            application_name: self.application_name.ok_or(PyValueError::new_err(
                "Expected application_name for AuthzConfig, but got: None",
            ))?,
        })
    }
}
