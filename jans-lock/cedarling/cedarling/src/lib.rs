/// This library is entry point for the authorization engine.
/// it is used to initialize the authorization engine and to authorize requests.
///
pub use authz_engine;
pub use init_engine;
pub use jwt_engine;

use authz_engine::{Authz, AuthzNewError, AuthzRequest};
pub use authz_engine::{CedarParams, ResourceData};
pub use init_engine::policy_store::PolicyStore;
pub use init_engine::policy_store_config::{GetPolicyError, PolicyStoreConfig};
pub use init_engine::{BootstrapConfig, TokenMapper};

pub type InitError = AuthzNewError;
pub type Instance = Authz;
pub type Request = AuthzRequest;

pub fn init(config: BootstrapConfig) -> Result<Instance, InitError> {
	Authz::new(config)
}
