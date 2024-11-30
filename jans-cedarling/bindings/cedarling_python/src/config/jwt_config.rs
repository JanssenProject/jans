/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use jsonwebtoken::Algorithm;
use pyo3::{exceptions::PyValueError, prelude::*};
use std::{collections::HashSet, str::FromStr};

/// JwtConfig
/// =========
///
/// A Python wrapper for the Rust `cedarling::JwtConfig` struct.
/// Manages JWT validation settings in the `Cedarling` application, specifying supported signature algorithms.
///
/// Attributes
/// ----------
/// :param enabled: Enables JWT validation.
/// :param signature_algorithms: List of supported JWT signature algorithms.
///
/// Example
/// -------
/// ```
/// # Initialize with JWT validation enabled
/// config = JwtConfig(enabled=True, signature_algorithms=["RS256", "HS256"])
/// ```
#[derive(Debug, Clone)]
#[pyclass(get_all, set_all)]
pub struct JwtConfig {
    enabled: bool,
    /// `CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED` in [bootstrap properties](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties) documentation.
    signature_algorithms: Option<Vec<String>>,
}

#[pymethods]
impl JwtConfig {
    #[new]
    #[pyo3(signature = (enabled, signature_algorithms=None))]
    fn new(enabled: bool, signature_algorithms: Option<Vec<String>>) -> PyResult<Self> {
        Ok(JwtConfig {
            enabled,
            signature_algorithms,
        })
    }
}

impl TryFrom<JwtConfig> for cedarling::JwtConfig {
    type Error = PyErr;

    fn try_from(value: JwtConfig) -> Result<Self, Self::Error> {
        let cedarling_config = if value.enabled {
            let str_algs = value.signature_algorithms.ok_or(PyValueError::new_err(
                "Expected signature_algorithms for JwtConfig, but got: None",
            ))?;
            let mut signature_algorithms = HashSet::new();
            for alg in str_algs.iter() {
                let alg = Algorithm::from_str(alg).map_err(|_| {
                    PyValueError::new_err(format!("Unsupported algorithm: {}", alg))
                })?;
                signature_algorithms.insert(alg);
            }
            Self::Enabled {
                signature_algorithms,
            }
        } else {
            Self::Disabled
        };
        Ok(cedarling_config)
    }
}
