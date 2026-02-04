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
    data_errors,
    DataError,
    pyo3::exceptions::PyException,
    "Exception raised by data_errors"
);

create_exception!(
    data_errors,
    InvalidKey,
    DataError,
    "Invalid key provided (key is empty)"
);

create_exception!(
    data_errors,
    KeyNotFound,
    DataError,
    "Key not found in store"
);

create_exception!(
    data_errors,
    StorageLimitExceeded,
    DataError,
    "Storage limit exceeded"
);

create_exception!(
    data_errors,
    TTLExceeded,
    DataError,
    "TTL exceeds maximum allowed"
);

create_exception!(
    data_errors,
    ValueTooLarge,
    DataError,
    "Value size exceeds maximum allowed size"
);

create_exception!(
    data_errors,
    SerializationError,
    DataError,
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
#[allow(clippy::boxed_local)]
pub fn data_error_to_py(err: Box<CedarlingDataError>) -> PyErr {
    let err_args = ErrorPayload(*err);
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
    m.add("DataError", m.py().get_type::<DataError>())?;
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
