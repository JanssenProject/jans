/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use cedarling::ResourceData;
use pyo3::exceptions::{PyTypeError, PyValueError};
use pyo3::prelude::*;
use pyo3::types::PyDict;

#[pyclass(get_all)]
pub struct Request<'a> {
    /// Access token raw value
    pub access_token: String,
    /// cedar_policy action
    pub action: String,
    /// cedar_policy resource data
    pub resource: ResourceData,
    /// context to be used in cedar_policy
    pub context: &'a PyDict,
}

// #[pymethods]
// impl BootstrapConfig {
//     #[new]
//     #[pyo3(signature = (application_name=None, log_config=None, policy_store_config=None, jwt_config=None))]
//     fn new(
//         application_name: Option<String>,
//         log_config: Option<PyObject>,
//         policy_store_config: Option<PolicyStoreConfig>,
//         jwt_config: Option<JwtConfig>,
//     ) -> PyResult<Self> {
//         let log_config = match log_config {
//             Some(python_value) => Some(extract_log_config(&python_value)?),
//             None => None,
//         };

//         Ok(Self {
//             application_name,
//             policy_store_config,
//             log_config,
//             jwt_config,
//         })
//     }

//     #[setter]
//     fn log_config(&mut self, value: PyObject) -> PyResult<()> {
//         self.log_config = Some(extract_log_config(&value)?);
//         Ok(())
//     }
// }
