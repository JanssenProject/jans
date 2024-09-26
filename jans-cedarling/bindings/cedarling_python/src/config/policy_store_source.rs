use pyo3::exceptions::PyValueError;
use pyo3::prelude::*;

/// PolicyStoreSource
/// =================
///
/// `PolicyStoreSource` is a Python wrapper around the Rust `cedarling::PolicyStoreSource` struct. It represents the source from which policies are read. Currently, the supported source for reading policies is a JSON string.
///
/// Class Definition
/// ----------------
///
/// .. class:: PolicyStoreSource(json=None)
///
///     The `PolicyStoreSource` class is used to specify the source from which the policy data is loaded. At present, it supports reading policies from a JSON string.
///
///     :param json: Optional. A JSON-formatted string that represents the policy data.
///
/// Methods
/// -------
///
/// .. method:: __init__(self, json=None)
///
///     Initializes a new instance of the `PolicyStoreSource` class with a JSON string. If no JSON string is provided, a `ValueError` is raised.
///
///     :param json: A JSON-formatted string. If not provided, raises a `ValueError`.
///     :raises ValueError: If the `json` parameter is not specified.
///
/// Example
/// -------
///
/// ```python
///
///     # Creating a new PolicyStoreSource instance with a JSON string
///     json_string = '{...}'
///     config = PolicyStoreSource(json=json_string)
///
///     # Attempting to create a PolicyStoreSource without a JSON string (raises ValueError)
///     try:
///         invalid_config = PolicyStoreSource()
///     except ValueError as e:
///         print(f"Error: {e}")
/// ```
///
#[derive(Debug, Clone)]
#[pyclass]
pub struct PolicyStoreSource {
    inner: cedarling::PolicyStoreSource,
}

#[pymethods]
impl PolicyStoreSource {
    #[new]
    #[pyo3(signature = (json=None))]
    // signature will be extended when will be extended rust `PolicyStoreSource` enum
    fn new(json: Option<String>) -> PyResult<Self> {
        if let Some(json_val) = json {
            Ok(Self {
                inner: cedarling::PolicyStoreSource::Json(json_val),
            })
        } else {
            Err(PyValueError::new_err("value not specified"))
        }
    }
}

impl From<PolicyStoreSource> for cedarling::PolicyStoreSource {
    fn from(value: PolicyStoreSource) -> Self {
        value.inner
    }
}
