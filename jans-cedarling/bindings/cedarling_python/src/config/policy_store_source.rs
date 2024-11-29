use pyo3::exceptions::PyValueError;
use pyo3::prelude::*;

/// PolicyStoreSource
/// =================
///
/// A Python wrapper for the Rust `cedarling::PolicyStoreSource` struct.
/// This class specifies the source for reading policy data, currently supporting
/// JSON strings.
///
/// Attributes
/// ----------
/// :param json: Optional JSON string for policy data.
///
/// Example
/// -------
/// ```
/// # Initialize with a JSON string
/// config = PolicyStoreSource(json='{...}')
/// ```
#[derive(Debug, Clone)]
#[pyclass]
pub struct PolicyStoreSource {
    inner: cedarling::PolicyStoreSource,
}

#[pymethods]
impl PolicyStoreSource {
    #[new]
    #[pyo3(signature = (json=None, yaml=None))]
    // signature will be extended when will be extended rust `PolicyStoreSource` enum
    fn new(json: Option<String>, yaml: Option<String>) -> PyResult<Self> {
        let inner = if let Some(json_val) = json {
            cedarling::PolicyStoreSource::Json(json_val)
        } else if let Some(yaml_val) = yaml {
            cedarling::PolicyStoreSource::Yaml(yaml_val)
        } else {
            return Err(PyValueError::new_err("value not specified"));
        };

        Ok(Self { inner })
    }
}

impl From<PolicyStoreSource> for cedarling::PolicyStoreSource {
    fn from(value: PolicyStoreSource) -> Self {
        value.inner
    }
}
