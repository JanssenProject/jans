use std::collections::BTreeSet;

#[derive(serde::Deserialize, Debug)]
pub(crate) struct CedarlingConfig {
	// policy store
	pub(crate) policy_store: PolicyStoreConfig,
	pub(crate) decompress_policy_store: bool,
	pub(crate) trust_store_refresh_rate: Option<i32>,

	// authz (set supported_signature_algorithms to None to disable signature validation)
	pub(crate) supported_signature_algorithms: Option<BTreeSet<String>>,
}

#[derive(serde::Deserialize, Debug)]
#[serde(tag = "strategy")]
#[serde(rename_all = "kebab-case")]
pub(crate) enum PolicyStoreConfig {
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
