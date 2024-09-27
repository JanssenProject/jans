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
/// `AuthzConfig` is a Python wrapper around the Rust `cedarling::AuthzConfig` struct.
///
/// Class Definition
/// ----------------
///
/// .. class:: AuthzConfig(application_name=None)
///
///     The `AuthzConfig` class represents the configuration for the Authorization component.
///     It holds an optional `application_name` that can be set and retrieved.
///
///     :param application_name: Optional. A string representing the name of the application.
///
/// Methods
/// -------
///
/// .. method:: __init__(self, application_name=None)
///
///     Initializes a new instance of the `AuthzConfig` class.
///
///     :param application_name: Optional. The name of the application as a string. Defaults to `None`.
///
/// .. method:: application_name(self, application_name: str)
///
///     Sets the value of the `application_name` attribute.
///
///     :param application_name: The name of the application as a string.
///     :raises ValueError: If the application name is invalid or not provided.
///
/// Example
/// -------
///
/// ```python
///
///     # Creating a new AuthzConfig instance
///     config = AuthzConfig(application_name="MyApp")
///
///     # Setting the application name
///     config.application_name = "NewAppName"
///
///     # Getting the application name
///     print(config.application_name)
/// ```
///
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
