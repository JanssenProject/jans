/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use pyo3::prelude::*;

/// Request
/// =======
///
/// Python wrapper for the Rust `cedarling::Request` struct.
/// Stores authorization data
///
/// Attributes
/// ----------
/// :param access_token: A string containing the access token.
///
/// Example
/// -------
/// ```
/// req = Request(access_token="your_token")
/// ```
#[derive(Debug, Clone)]
#[pyclass(get_all, set_all)]
pub struct Request {
    /// Access token raw value
    access_token: String,
}

#[pymethods]
impl Request {
    #[new]
    fn new(access_token: String) -> Request {
        Request { access_token }
    }
}

impl<'a> From<&'a Request> for cedarling::Request<'a> {
    fn from(value: &'a Request) -> Self {
        cedarling::Request {
            access_token: value.access_token.as_str(),
        }
    }
}
