/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use cedarling::AuthorizeError as CedarlingAuthorizeError;
use pyo3::{create_exception, prelude::*};

// Define a base class for Authorization errors in Python
create_exception!(
    authorize_errors,
    AuthorizeError,
    pyo3::exceptions::PyException,
    "Exception raised by authorize_errors"
);

create_exception!(
    authorize_errors,
    LoggingError,
    AuthorizeError,
    "Error encountered while trying to write logs"
);

#[pyclass]
#[derive()]
pub struct ErrorPayload(CedarlingAuthorizeError);

#[pymethods]
impl ErrorPayload {
    fn __str__(&self) -> String {
        self.0.to_string()
    }
}

// macros to write logic with errors only once
macro_rules! errors_functions {
    ($($case_name:ident => $error_class:ident),*) => {
        // is used to map CedarlingAuthorizeError to python error
        pub fn authorize_error_to_py(err: CedarlingAuthorizeError) -> PyErr {
                match err {
                    $(CedarlingAuthorizeError::$case_name(_) => {
                        let err_args = ErrorPayload(err);
                        PyErr::new::<$error_class, _>(err_args)
                    },)*
                }
            }

        // is used to register errors in py module
        pub fn register_errors(m: &Bound<'_, PyModule>) -> PyResult<()> {
            $(
                m.add(stringify!($error_class), m.py().get_type_bound::<$error_class>())?;
            )*
            Ok(())
        }
        };
}

// We use macros to create the function `authorize_error_to_py`.
// This function is used to convert `cedarling::AuthorizeError` to a Python exception.
// For each possible case of `AuthorizeError`, we have created a corresponding Python exception that inherits from `cedarling::AuthorizeError`.
errors_functions! {
    Logging => LoggingError
}

pub fn authorize_errors_module(m: &Bound<'_, PyModule>) -> PyResult<()> {
    m.add("AuthorizeError", m.py().get_type_bound::<AuthorizeError>())?;
    register_errors(m)?;
    Ok(())
}
