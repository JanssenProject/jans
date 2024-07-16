use std::collections::BTreeSet;

#[derive(serde::Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
pub(crate) struct CedarlingConfig {
	pub(crate) application_name: Option<String>,
	pub(crate) require_aud_validation: bool,

	// policy store
	pub(crate) policy_store: PolicyStoreConfig,
	pub(crate) decompress_policy_store: bool,
	pub(crate) trust_store_refresh_rate: Option<i32>,

	// authz (set supported_signature_algorithms to None to disable signature validation)
	#[serde(default)]
	pub(crate) supported_signature_algorithms: BTreeSet<String>,
}

#[derive(serde::Deserialize, Debug)]
#[serde(tag = "strategy")]
#[serde(rename_all = "kebab-case")]
#[serde(rename_all_fields = "camelCase")]
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
