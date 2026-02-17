/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use cedarling::DataError as CedarlingDataError;
use pyo3::{create_exception, prelude::*};

// Define a base class for Data errors in Python
create_exception!(
    data_errors_ctx,
    DataErrorCtx,
    pyo3::exceptions::PyException,
    "Base exception for errors encountered during data operations in Cedarling context storage."
);

create_exception!(
    data_errors_ctx,
    InvalidKey,
    DataErrorCtx,
    "Raised when an invalid (e.g., empty) key is provided to the context data store. This typically means the key argument was missing or empty."
);

create_exception!(
    data_errors_ctx,
    KeyNotFound,
    DataErrorCtx,
    "Raised when a requested key is not found in the context data store. This usually means the key does not exist or has expired."
);

create_exception!(
    data_errors_ctx,
    StorageLimitExceeded,
    DataErrorCtx,
    "Raised when an operation would exceed the maximum allowed storage size for the context data store."
);

create_exception!(
    data_errors_ctx,
    TTLExceeded,
    DataErrorCtx,
    "Raised when a requested time-to-live (TTL) value exceeds the maximum allowed by the context data store."
);

create_exception!(
    data_errors_ctx,
    ValueTooLarge,
    DataErrorCtx,
    "Raised when a value is too large to be stored in the context data store, exceeding the allowed size limit."
);

create_exception!(
    data_errors_ctx,
    SerializationError,
    DataErrorCtx,
    "Raised when there is a failure serializing or deserializing data for storage or retrieval in the context data store."
);

#[pyclass]
pub struct ErrorPayload(CedarlingDataError);

#[pymethods]
impl ErrorPayload {
    fn __str__(&self) -> String {
        self.0.to_string()
    }
}

// Convert CedarlingDataError to Python exception
pub fn data_error_to_py(err: CedarlingDataError) -> PyErr {
    let err_args = ErrorPayload(err);
    match err_args.0 {
        CedarlingDataError::InvalidKey => PyErr::new::<InvalidKey, _>(err_args),
        CedarlingDataError::KeyNotFound { .. } => PyErr::new::<KeyNotFound, _>(err_args),
        CedarlingDataError::StorageLimitExceeded { .. } => {
            PyErr::new::<StorageLimitExceeded, _>(err_args)
        },
        CedarlingDataError::TTLExceeded { .. } => PyErr::new::<TTLExceeded, _>(err_args),
        CedarlingDataError::ValueTooLarge { .. } => PyErr::new::<ValueTooLarge, _>(err_args),
        CedarlingDataError::SerializationError(_) => PyErr::new::<SerializationError, _>(err_args),
    }
}

pub fn data_errors_module(m: &Bound<'_, PyModule>) -> PyResult<()> {
    m.add("DataErrorCtx", m.py().get_type::<DataErrorCtx>())?;
    m.add("InvalidKey", m.py().get_type::<InvalidKey>())?;
    m.add("KeyNotFound", m.py().get_type::<KeyNotFound>())?;
    m.add(
        "StorageLimitExceeded",
        m.py().get_type::<StorageLimitExceeded>(),
    )?;
    m.add("TTLExceeded", m.py().get_type::<TTLExceeded>())?;
    m.add("ValueTooLarge", m.py().get_type::<ValueTooLarge>())?;
    m.add(
        "SerializationError",
        m.py().get_type::<SerializationError>(),
    )?;
    Ok(())
}
