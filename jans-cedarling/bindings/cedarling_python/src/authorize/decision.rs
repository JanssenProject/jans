/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */
use cedarling::bindings::cedar_policy;
use pyo3::prelude::*;

/// Decision
/// ========
///
/// Represents the decision result of a Cedar policy authorization.
///
/// Methods
/// -------
/// value() -> str
///     Returns the string value of the decision.
/// __str__() -> str
///     Returns the string representation of the decision.
/// __repr__() -> str
///     Returns the detailed type representation of the decision.
/// __eq__(other: Decision) -> bool
///     Compares two `Decision` objects for equality.
#[derive(Debug, Clone)]
#[pyclass]
pub struct Decision {
    inner: cedarling::bindings::Decision,
}

#[pymethods]
impl Decision {
    /// get the value of the decision
    #[getter]
    fn value(&self) -> String {
        self.inner.to_string()
    }

    /// string representation of the decision
    fn __str__(&self) -> String {
        self.value()
    }

    /// type string representation of the decision
    fn __repr__(&self) -> String {
        format!("Decision({})", self.value())
    }

    /// equality operator
    fn __eq__(&self, other: &Decision) -> bool {
        self.inner == other.inner
    }
}

impl From<cedar_policy::Decision> for Decision {
    fn from(value: cedar_policy::Decision) -> Self {
        Self {
            inner: value.into(),
        }
    }
}
