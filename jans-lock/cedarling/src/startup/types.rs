use std::collections::BTreeSet;

#[derive(serde::Deserialize, serde::Serialize, Debug)]
#[serde(rename_all = "camelCase")]
pub struct CedarlingConfig {
	pub application_name: Option<String>,
	#[serde(default)]
	pub require_aud_validation: bool,

	// policy store
	pub policy_store: PolicyStoreConfig,
	pub decompress_policy_store: bool,
	pub trust_store_refresh_rate: Option<i32>,

	// authz (set supported_signature_algorithms to None to disable signature validation)
	#[serde(default)]
	pub supported_signature_algorithms: BTreeSet<String>,
}

#[derive(serde::Deserialize, serde::Serialize, Debug)]
#[serde(tag = "strategy")]
#[serde(rename_all = "kebab-case")]
#[serde(rename_all_fields = "camelCase")]
pub enum PolicyStoreConfig {
	Local,
	Remote {
		url: String,
	},
	LockMaster {
		url: String,
		application_name: String,
		policy_store_id: String,
		enable_dynamic_configuration: bool,
		ssa_jwt: String,
	},
}
