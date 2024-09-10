use pyo3::exceptions::PyValueError;
use pyo3::prelude::*;
use pyo3::types::{PyAny, PyString, PyStringMethods};
use serde_pyobject::from_pyobject;

#[derive(Debug, Clone)]
#[pyclass]
pub struct PolicyStore {
	pub(crate) inner: init_engine::policy_store::PolicyStore,
}

fn get_store_from_config(config: authz::PolicyStoreConfig) -> PyResult<PolicyStore> {
	let store = config
		.get_policy()
		.map_err(|err| PyValueError::new_err(err.to_string()))?;
	Ok(PolicyStore { inner: store })
}

fn to_rust_string(py_str: &Bound<'_, PyString>) -> PyResult<String> {
	py_str
		.to_str()
		.map(|s| s.to_owned())
		.map_err(|_| PyValueError::new_err("Failed to convert Python string to Rust string"))
}

#[pymethods]
impl PolicyStore {
	#[new]
	fn new(input: Bound<'_, PyAny>) -> PyResult<Self> {
		let store: init_engine::policy_store::PolicyStore = from_pyobject(input)?;
		Ok(PolicyStore { inner: store })
	}

	#[staticmethod]
	pub fn from_raw_json(raw_json: &Bound<'_, PyString>) -> PyResult<Self> {
		let rust_str = to_rust_string(raw_json)?;
		get_store_from_config(authz::PolicyStoreConfig::JsonRaw(rust_str))
	}

	#[staticmethod]
	pub fn from_filepath(filepath: &Bound<'_, PyString>) -> PyResult<Self> {
		let rust_str = to_rust_string(filepath)?;
		get_store_from_config(authz::PolicyStoreConfig::File(rust_str.into()))
	}

	#[staticmethod]
	pub fn from_remote_uri(uri: &Bound<'_, PyString>) -> PyResult<Self> {
		let rust_str = to_rust_string(uri)?;
		get_store_from_config(authz::PolicyStoreConfig::RemoteURI(rust_str.into()))
	}
}
