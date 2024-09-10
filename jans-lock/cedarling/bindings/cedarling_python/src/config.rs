use pyo3::{exceptions::PyValueError, prelude::*};

use crate::policy_store::PolicyStore;
// The config structs should match those in the authz module.
// The key idea is to keep authz independent from the Python bindings.
// Therefore, we map the Python binding structs to pure Rust equivalents.

#[pyclass]
#[derive(Debug, Clone)]
pub struct TokenMapper {
	#[pyo3(get, set)]
	pub id_token: Option<String>,
	#[pyo3(get, set)]
	pub userinfo_token: Option<String>,
	#[pyo3(get, set)]
	pub access_token: Option<String>,
}

#[pymethods]
impl TokenMapper {
	#[new]
	#[pyo3(signature = (id_token=None, userinfo_token=None, access_token=None))]
	fn new(
		id_token: Option<String>,
		userinfo_token: Option<String>,
		access_token: Option<String>,
	) -> Self {
		let default_val = TokenMapper::default();

		TokenMapper {
			id_token: id_token.or(default_val.id_token),
			userinfo_token: userinfo_token.or(default_val.userinfo_token),
			access_token: access_token.or(default_val.access_token),
		}
	}
}

impl Default for TokenMapper {
	fn default() -> Self {
		let role: cedarling::TokenMapper = <_>::default();
		Self {
			id_token: role.id_token,
			userinfo_token: role.userinfo_token,
			access_token: role.access_token,
		}
	}
}

impl From<TokenMapper> for cedarling::TokenMapper {
	fn from(val: TokenMapper) -> Self {
		cedarling::TokenMapper {
			id_token: val.id_token,
			userinfo_token: val.userinfo_token,
			access_token: val.access_token,
		}
	}
}

#[pyclass]
#[derive(Default, Clone)]
pub struct BootstrapConfig {
	#[pyo3(get, set)]
	pub application_name: Option<String>,
	#[pyo3(get, set)]
	pub token_mapper: TokenMapper,
	#[pyo3(get, set)]
	pub policy_store: Option<PolicyStore>,
}

#[allow(non_snake_case)]
#[pymethods]
impl BootstrapConfig {
	#[new]
	#[pyo3(signature = (application_name=None, token_mapper=None,policy_store=None))]
	fn new(
		application_name: Option<String>,
		token_mapper: Option<TokenMapper>,
		policy_store: Option<PolicyStore>,
	) -> Self {
		BootstrapConfig {
			application_name,
			token_mapper: token_mapper.unwrap_or_default(),
			policy_store,
		}
	}
}

impl TryInto<cedarling::BootstrapConfig> for BootstrapConfig {
	type Error = PyErr;

	fn try_into(self) -> Result<cedarling::BootstrapConfig, Self::Error> {
		Ok(cedarling::BootstrapConfig {
			application_name: self.application_name,
			token_mapper: self.token_mapper.into(),
			policy_store: self.policy_store.map(|store| store.inner).ok_or(
				PyValueError::new_err("in BootstrapConfig field policy_store is None"),
			)?,
		})
	}
}
