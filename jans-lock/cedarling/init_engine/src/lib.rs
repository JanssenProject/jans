pub mod policy_store;
use policy_store::PolicyStoreEntry;

pub mod policy_store_config;

/// represent mapping tokens and role field
/// for CEDARLING_ROLE_MAPPING value in BootstrapConfig
/// for `id_token` default value `role`
/// for `userinfo_token` default value `role`
#[derive(Debug, derivative::Derivative, serde::Serialize, serde::Deserialize)]
#[derivative(Default, Clone)]
pub struct TokenMapper {
	#[derivative(Default(value = "Some(\"role\".into())"))]
	pub id_token: Option<String>,
	#[derivative(Default(value = "Some(\"role\".into())"))]
	pub userinfo_token: Option<String>,
	pub access_token: Option<String>,
}

/// Bootstrap properties of application [link](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties)
#[derive(Debug)]
pub struct BootstrapConfig {
	pub application_name: Option<String>,
	pub token_mapper: TokenMapper,
	pub policy_store: PolicyStoreEntry,
}

impl BootstrapConfig {
	pub fn get_jwt_decoder(&self) -> jwt::JWTDecoder {
		jwt::JWTDecoder::WithoutValidation
	}
}
