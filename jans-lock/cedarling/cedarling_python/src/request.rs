use pyo3::exceptions::PyValueError;
use pyo3::prelude::*;
use pyo3::types::PyAny;
use serde_pyobject::from_pyobject;

#[derive(Debug, Clone)]
#[pyclass]
pub struct Resource {
	#[pyo3(get, set)]
	pub _type: String,
	#[pyo3(get, set)]
	pub id: String,
}

#[pymethods]
impl Resource {
	#[new]
	fn new(_type: String, id: String) -> PyResult<Self> {
		Ok(Resource { _type, id })
	}
}

#[derive(Debug, Clone)]
#[pyclass]
pub struct Request {
	#[pyo3(get, set)]
	pub id_token: Option<String>,
	#[pyo3(get, set)]
	pub userinfo_token: Option<String>,
	#[pyo3(get, set)]
	pub access_token: Option<String>,

	#[pyo3(get, set)]
	pub action: Option<String>,
	#[pyo3(get, set)]
	pub resource: Option<Resource>,
	pub context: Option<serde_json::Value>,
}

#[pymethods]
impl Request {
	#[new]
	#[pyo3(signature = (id_token=None, userinfo_token=None, access_token=None, action=None, resource=None, context=None))]
	fn new(
		id_token: Option<String>,
		userinfo_token: Option<String>,
		access_token: Option<String>,
		action: Option<String>,
		resource: Option<Resource>,
		context: Option<Bound<'_, PyAny>>,
	) -> PyResult<Self> {
		let context = match context {
			Some(v) => {
				let ctx: serde_json::Value = from_pyobject(v).map_err(|err| {
					PyValueError::new_err(format!("could not parse context field:{err}"))
				})?;
				Some(ctx)
			}
			None => None,
		};

		Ok(Request {
			id_token,
			userinfo_token,
			access_token,
			action,
			resource,
			context,
		})
	}

	#[setter]
	fn context(&mut self, context: Bound<'_, PyAny>) -> PyResult<()> {
		self.context = from_pyobject(context)?;

		Ok(())
	}
}

impl TryInto<authz_engine::AuthzRequest> for Request {
	type Error = PyErr;

	fn try_into(self) -> Result<authz_engine::AuthzRequest, Self::Error> {
		Ok(authz_engine::AuthzRequest {
			id_token: self
				.id_token
				.ok_or(PyValueError::new_err("in Request value id_token is None"))?,
			userinfo_token: self.userinfo_token.ok_or(PyValueError::new_err(
				"in Request value userinfo_token is None",
			))?,
			access_token: self.access_token.ok_or(PyValueError::new_err(
				"in Request value access_token is None",
			))?,

			extra: authz_engine::CedarParams {
				action: self
					.action
					.ok_or(PyValueError::new_err("in Request value action is None"))?,
				resource: self
					.resource
					.map(|r| authz_engine::ResourceData {
						_type: r._type,
						id: r.id,
					})
					.ok_or(PyValueError::new_err("in Request value resource is None"))?,
				context: self
					.context
					.ok_or(PyValueError::new_err("in Request value context is None"))?,
			},
		})
	}
}
