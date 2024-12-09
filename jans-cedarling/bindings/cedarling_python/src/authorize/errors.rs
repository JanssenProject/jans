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
    DecodeTokens,
    AuthorizeError,
    "Error encountered while decoding JWT token data"
);

create_exception!(
    authorize_errors,
    ProcessTokens,
    AuthorizeError,
    "Error encountered while processing JWT token data"
);

create_exception!(
    authorize_errors,
    AccessTokenEntitiesError,
    AuthorizeError,
    "Error encountered while creating access token entities"
);

create_exception!(
    authorize_errors,
    CreateIdTokenEntityError,
    AuthorizeError,
    "Error encountered while creating id token entities"
);

create_exception!(
    authorize_errors,
    CreateUserinfoTokenEntityError,
    AuthorizeError,
    "Error encountered while creating Userinfo_token entity"
);

create_exception!(
    authorize_errors,
    CreateUserEntityError,
    AuthorizeError,
    "Error encountered while creating User entity"
);

create_exception!(
    authorize_errors,
    ResourceEntityError,
    AuthorizeError,
    "Error encountered while creating resource entity"
);

create_exception!(
    authorize_errors,
    RoleEntityError,
    AuthorizeError,
    "Error encountered while creating role entity"
);

create_exception!(
    authorize_errors,
    ActionError,
    AuthorizeError,
    "Error encountered while parsing Action to EntityUid"
);

create_exception!(
    authorize_errors,
    CreateContextError,
    AuthorizeError,
    "Error encountered while validating context according to the schema"
);

create_exception!(
    authorize_errors,
    CreateRequestWorkloadEntityError,
    AuthorizeError,
    "Error encountered while creating cedar_policy::Request for workload entity principal"
);

create_exception!(
    authorize_errors,
    CreateRequestUserEntityError,
    AuthorizeError,
    "Error encountered while creating cedar_policy::Request for user entity principal"
);

create_exception!(
    authorize_errors,
    EntitiesError,
    AuthorizeError,
    "Error encountered while collecting all entities"
);

create_exception!(
    authorize_errors,
    EntitiesToJsonError,
    AuthorizeError,
    "Error encountered while parsing all entities to json for logging"
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
    ProcessTokens => ProcessTokens,
    AccessTokenEntities => AccessTokenEntitiesError,
    CreateIdTokenEntity => CreateIdTokenEntityError,
    CreateUserinfoTokenEntity => CreateUserinfoTokenEntityError,
    CreateUserEntity => CreateUserEntityError,
    ResourceEntity => ResourceEntityError,
    RoleEntity => RoleEntityError,
    Action => ActionError,
    CreateContext => CreateContextError,
    CreateRequestWorkloadEntity => CreateRequestWorkloadEntityError,
    CreateRequestUserEntity => CreateRequestUserEntityError,
    Entities => EntitiesError,
    EntitiesToJson => EntitiesToJsonError
}

pub fn authorize_errors_module(m: &Bound<'_, PyModule>) -> PyResult<()> {
    m.add("AuthorizeError", m.py().get_type_bound::<AuthorizeError>())?;
    register_errors(m)?;
    Ok(())
}
