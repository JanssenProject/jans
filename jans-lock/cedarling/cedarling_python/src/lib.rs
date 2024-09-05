use pyo3::exceptions::PyValueError;
use pyo3::prelude::*;
use pyo3::{types::PyAny, Bound};
use serde_pyobject::from_pyobject;

mod config;
use config::{BootstrapConfig, TokenMapper};

#[pyclass]
pub struct Authz {
	inner: authz::Authz,
}

#[pymethods]
impl Authz {
	#[new]
	fn new(bootstrap_config: BootstrapConfig) -> PyResult<Self> {
		Ok(Authz {
			inner: authz::Authz::new(authz::AuthzConfig {
				bootstrap_config: bootstrap_config.into(),
				..Default::default()
			})
			.map_err(|err| PyValueError::new_err(err.to_string()))?,
		})
	}

	pub fn is_authorized(&self, input: Bound<'_, PyAny>) -> PyResult<bool> {
		let authz_input: authz::AuthzInputRaw = from_pyobject(input)?;
		Ok(self
			.inner
			.is_authorized(authz_input)
			.map_err(|err| PyValueError::new_err(err.to_string()))?)
	}
}

#[pymodule]
fn cedarling_python(m: &Bound<'_, PyModule>) -> PyResult<()> {
	m.add_class::<BootstrapConfig>()?;
	m.add_class::<TokenMapper>()?;
	m.add_class::<Authz>()?;

	Ok(())
}
