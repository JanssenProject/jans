use pyo3::exceptions::PyValueError;
use pyo3::prelude::*;
use pyo3::Bound;

mod config;
use config::{BootstrapConfig, TokenMapper};

mod policy_store;
use policy_store::PolicyStore;

mod request;
use request::{Request, Resource};

#[pyclass]
pub struct Authz {
	inner: cedarling::Instance,
}

#[pymethods]
impl Authz {
	#[new]
	fn new(bootstrap_config: BootstrapConfig) -> PyResult<Self> {
		Ok(Authz {
			inner: cedarling::init(bootstrap_config.try_into()?)
				.map_err(|err| PyValueError::new_err(err.to_string()))?,
		})
	}

	pub fn is_authorized(&self, request: Request) -> PyResult<bool> {
		let authz_input: cedarling::Request = request.try_into()?;
		self.inner
			.is_authorized(authz_input)
			.map_err(|err| PyValueError::new_err(err.to_string()))
	}
}

#[pyfunction]
fn init(bootstrap_config: BootstrapConfig) -> PyResult<Authz> {
	Ok(Authz {
		inner: cedarling::init(bootstrap_config.try_into()?)
			.map_err(|err| PyValueError::new_err(err.to_string()))?,
	})
}

#[pymodule]
fn cedarling_python(m: &Bound<'_, PyModule>) -> PyResult<()> {
	m.add_class::<BootstrapConfig>()?;
	m.add_class::<TokenMapper>()?;
	m.add_class::<Authz>()?;
	m.add_class::<PolicyStore>()?;
	m.add_class::<Request>()?;
	m.add_class::<Resource>()?;
	m.add_function(wrap_pyfunction!(init, m)?)?;

	Ok(())
}
