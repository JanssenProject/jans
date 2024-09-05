use pyo3::exceptions::PyValueError;
use pyo3::prelude::*;
use pyo3::types::{PyAny, PyString, PyStringMethods};
use serde_pyobject::from_pyobject;

use authz;
#[derive(Debug, Clone)]
#[pyclass]
pub struct PolicyStore {
	pub(crate) inner: authz::PolicyStoreEntry,
}

#[pymethods]
impl PolicyStore {
	#[new]
	fn new(input: Bound<'_, PyAny>) -> PyResult<Self> {
		let store: authz::PolicyStoreEntry = from_pyobject(input)?;
		Ok(PolicyStore { inner: store })
	}

	#[staticmethod]
	pub fn from_raw_json(raw_json: &Bound<'_, PyString>) -> PyResult<Self> {
		let json_str = raw_json
			.to_str()
			.map_err(|_| PyValueError::new_err("Failed to convert Python string to Rust string"))?;

		let store = authz::PolicyStoreConfig::LocalJson(json_str.to_owned())
			.get_policy()
			.map_err(|err| PyValueError::new_err(err.to_string()))?;

		Ok(PolicyStore { inner: store })
	}
}
