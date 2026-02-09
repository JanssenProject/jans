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
    "Exception raised by data_errors_ctx"
);

create_exception!(
    data_errors_ctx,
    InvalidKey,
    DataErrorCtx,
    "Invalid key provided (key is empty)"
);

create_exception!(
    data_errors_ctx,
    KeyNotFound,
    DataErrorCtx,
    "Key not found in store"
);

create_exception!(
    data_errors_ctx,
    StorageLimitExceeded,
    DataErrorCtx,
    "Storage limit exceeded"
);

create_exception!(
    data_errors_ctx,
    TTLExceeded,
    DataErrorCtx,
    "TTL exceeds maximum allowed"
);

create_exception!(
    data_errors_ctx,
    ValueTooLarge,
    DataErrorCtx,
    "Value size exceeds maximum allowed size"
);

create_exception!(
    data_errors_ctx,
    SerializationError,
    DataErrorCtx,
    "Serialization error"
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
