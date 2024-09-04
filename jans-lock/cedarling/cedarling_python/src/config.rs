use pyo3::prelude::*;

use authz;

// The config structs should match those in the authz module.
// The key idea is to keep authz independent of the Python bindings.
// Therefore, we map the Python binding structs to pure Rust equivalents.

// TODO: investigate how to create macros for automatic `Into` implementation

#[pyclass]
#[derive(Debug, Clone)]
pub struct RoleMapping {
	#[pyo3(get, set)]
	pub id_token: Option<String>,
	#[pyo3(get, set)]
	pub userinfo_token: Option<String>,
	#[pyo3(get, set)]
	pub access_token: Option<String>,
}

#[pymethods]
impl RoleMapping {
	#[new]
	#[pyo3(signature = (id_token=None, userinfo_token=None, access_token=None))]
	fn new(
		id_token: Option<String>,
		userinfo_token: Option<String>,
		access_token: Option<String>,
	) -> Self {
		let default_val = RoleMapping::default();

		RoleMapping {
			id_token: id_token.or(default_val.id_token),
			userinfo_token: userinfo_token.or(default_val.userinfo_token),
			access_token: access_token.or(default_val.access_token),
		}
	}
}

impl Default for RoleMapping {
	fn default() -> Self {
		let role: authz::RoleMapping = <_>::default();
		Self {
			id_token: role.id_token,
			userinfo_token: role.userinfo_token,
			access_token: role.access_token,
		}
	}
}

impl Into<authz::RoleMapping> for RoleMapping {
	fn into(self) -> authz::RoleMapping {
		authz::RoleMapping {
			id_token: self.id_token,
			userinfo_token: self.userinfo_token,
			access_token: self.access_token,
		}
	}
}

#[allow(non_snake_case)]
#[pyclass]
#[derive(Default, Clone)]
pub struct BootstrapConfig {
	#[pyo3(get, set)]
	pub CEDARLING_APPLICATION_NAME: Option<String>,
	#[pyo3(get, set)]
	pub CEDARLING_ROLE_MAPPING: RoleMapping,
}

#[allow(non_snake_case)]
#[pymethods]
impl BootstrapConfig {
	#[new]
	#[pyo3(signature = (CEDARLING_APPLICATION_NAME=None, CEDARLING_ROLE_MAPPING=None))]
	fn new(
		CEDARLING_APPLICATION_NAME: Option<String>,
		CEDARLING_ROLE_MAPPING: Option<RoleMapping>,
	) -> Self {
		BootstrapConfig {
			CEDARLING_APPLICATION_NAME,
			CEDARLING_ROLE_MAPPING: CEDARLING_ROLE_MAPPING.unwrap_or_default(),
		}
	}
}

impl Into<authz::BootstrapConfig> for BootstrapConfig {
	fn into(self) -> authz::BootstrapConfig {
		authz::BootstrapConfig {
			CEDARLING_APPLICATION_NAME: self.CEDARLING_APPLICATION_NAME,
			CEDARLING_ROLE_MAPPING: self.CEDARLING_ROLE_MAPPING.into(),
		}
	}
}

impl From<&BootstrapConfig> for authz::BootstrapConfig {
	fn from(value: &BootstrapConfig) -> Self {
		authz::BootstrapConfig {
			CEDARLING_APPLICATION_NAME: value.CEDARLING_APPLICATION_NAME.to_owned(),
			CEDARLING_ROLE_MAPPING: value.CEDARLING_ROLE_MAPPING.to_owned().into(),
		}
	}
}
