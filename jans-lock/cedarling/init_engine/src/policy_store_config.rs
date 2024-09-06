use crate::policy_store::PolicyStoreEntry;

#[derive(serde::Deserialize, serde::Serialize, Debug)]
#[serde(tag = "strategy")]
#[serde(rename_all = "kebab-case")]
#[serde(rename_all_fields = "camelCase")]
pub enum PolicyStoreConfig {
	LocalJson(String),
	RemoteURI(String),
}

#[derive(thiserror::Error, Debug)]
pub enum GetPolicyError {
	#[error("could not parse policy from json: {0}")]
	ParseJson(#[from] serde_json::Error),
}

impl PolicyStoreConfig {
	pub fn get_policy(self) -> Result<PolicyStoreEntry, GetPolicyError> {
		match self {
			Self::LocalJson(policy_raw_json) => Self::get_local_policy(policy_raw_json.as_str()),
			Self::RemoteURI(_uri) => todo!(),
		}
	}

	pub fn get_local_policy(policy_raw_json: &str) -> Result<PolicyStoreEntry, GetPolicyError> {
		let policy: PolicyStoreEntry = serde_json::from_str(policy_raw_json)?;
		Ok(policy)
	}
}
