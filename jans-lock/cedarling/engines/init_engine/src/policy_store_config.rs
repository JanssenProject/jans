use std::{
	fs::File,
	path::{Path, PathBuf},
};

use crate::policy_store::PolicyStore;

#[derive(Debug)]
pub enum PolicyStoreConfig {
	JsonRaw(String),
	File(PathBuf),
	RemoteURI(String),
}

#[derive(thiserror::Error, Debug)]
pub enum GetPolicyError {
	#[error("could not parse policy from json: {0}")]
	ParseJson(#[from] serde_json::Error),
	#[error("could not read policy file: {0}")]
	ReadFile(#[from] std::io::Error),
	#[error("could make http request: {0}")]
	HttpRequest(String),
}

impl PolicyStoreConfig {
	pub fn get_policy(self) -> Result<PolicyStore, GetPolicyError> {
		match self {
			Self::JsonRaw(policy_raw_json) => {
				Self::get_local_policy_raw_json(policy_raw_json.as_str())
			}
			Self::File(path) => Self::get_local_policy_file(path.as_path()),
			Self::RemoteURI(uri) => Self::get_policy_remote(uri.as_str()),
		}
	}

	pub fn get_local_policy_raw_json(policy_raw_json: &str) -> Result<PolicyStore, GetPolicyError> {
		let policy: PolicyStore = serde_json::from_str(policy_raw_json)?;
		Ok(policy)
	}

	pub fn get_local_policy_file(path: &Path) -> Result<PolicyStore, GetPolicyError> {
		let policy: PolicyStore = serde_json::from_reader(File::open(path)?)?;
		Ok(policy)
	}

	pub fn get_policy_remote(uri: &str) -> Result<PolicyStore, GetPolicyError> {
		let policy: PolicyStore = ehttp::fetch_blocking(&ehttp::Request::get(uri))
			.map_err(GetPolicyError::HttpRequest)?
			.json()?;

		Ok(policy)
	}
}
