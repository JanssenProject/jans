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

#[cfg(test)]
mod tests {
	use super::*;

	const POLICY_RAW_JSON: &str = include_str!("../../../demo/policy-store/local.json");
	const POLICY_RAW_JSON_POISONED: &str =
		include_str!("../../../demo/policy-store/local_poisoned.json");
	const POLICY_RAW_JSON_SCHEMA_BASE64_POISONED: &str =
		include_str!("../../../demo/policy-store/local_schema_base64_poisoned.json");
	const POLICY_RAW_JSON_SCHEMA_STRUCT_POISONED: &str =
		include_str!("../../../demo/policy-store/local_schema_schema_struct_poisoned.json");
	const POLICY_RAW_JSON_POLICY_BASE64_POISONED: &str =
		include_str!("../../../demo/policy-store/local_policy_base64_poisoned.json");
	const POLICY_RAW_JSON_POLICY_STRUCT_POISONED: &str =
		include_str!("../../../demo/policy-store/local_policy_struct_poisoned.json");

	fn check_policy_store(policy: &PolicyStore) {
		// policies and schema we don`t check because it is parsed and validated during the parsing json
		let google = policy
			.trusted_issuers
			.get("Google")
			.expect("Google issuer not found");

		// just check if we read fields correctly
		assert_eq!(
			google.openid_configuration_endpoint,
			"https://accounts.google.com/.well-known/openid-configuration"
		);
		assert!(google.access_tokens.trusted);
		assert!(google.id_tokens.trusted);
		assert!(google.userinfo_tokens.trusted);

		assert_eq!(
			google.id_tokens.principal_identifier,
			Some("email".to_string())
		);
		assert_eq!(
			google.userinfo_tokens.role_mapping,
			Some("role".to_string())
		)
	}

	#[test]
	fn get_local_policy_raw_json() {
		let policy = PolicyStoreConfig::get_local_policy_raw_json(POLICY_RAW_JSON).unwrap();
		check_policy_store(&policy);
	}

	#[test]
	fn get_local_policy_raw_json_fail() {
		let err = PolicyStoreConfig::get_local_policy_raw_json(POLICY_RAW_JSON_POISONED)
			.expect_err("should be error");
		assert!(
			matches!(err, GetPolicyError::ParseJson(_)),
			"Expected ParseJson error, got: {:?}",
			err
		);
	}

	#[test]
	fn get_local_policy_raw_json_schema_base64_fail() {
		let err =
			PolicyStoreConfig::get_local_policy_raw_json(POLICY_RAW_JSON_SCHEMA_BASE64_POISONED)
				.expect_err("should be error");
		assert!(
			matches!(err, GetPolicyError::ParseJson(_)),
			"Expected ParseJson error, got: {:?}",
			err
		);

		assert!(err
			.to_string()
			.contains("unable to parse Schema source as valid base64"))
	}

	#[test]
	fn get_local_policy_raw_json_schema_struct_fail() {
		let err =
			PolicyStoreConfig::get_local_policy_raw_json(POLICY_RAW_JSON_SCHEMA_STRUCT_POISONED)
				.expect_err("should be error");
		assert!(
			matches!(err, GetPolicyError::ParseJson(_)),
			"Expected ParseJson error, got: {:?}",
			err
		);

		assert!(err
			.to_string()
			.contains("unable to parse Schema in Human Readable cedar format"))
	}

	#[test]
	fn get_local_policy_raw_json_policy_base64_fail() {
		let err =
			PolicyStoreConfig::get_local_policy_raw_json(POLICY_RAW_JSON_POLICY_BASE64_POISONED)
				.expect_err("should be error");
		assert!(
			matches!(err, GetPolicyError::ParseJson(_)),
			"Expected ParseJson error, got: {:?}",
			err
		);

		assert!(err
			.to_string()
			.contains("unable to parse Policy source as valid base64"))
	}

	#[test]
	fn get_local_policy_raw_json_policy_struct_fail() {
		let err =
			PolicyStoreConfig::get_local_policy_raw_json(POLICY_RAW_JSON_POLICY_STRUCT_POISONED)
				.expect_err("should be error");
		assert!(
			matches!(err, GetPolicyError::ParseJson(_)),
			"Expected ParseJson error, got: {:?}",
			err
		);

		assert!(err
			.to_string()
			.contains("unable to parse Policy from string"))
	}

	#[test]
	fn get_local_policy_file_json() {
		let policy = PolicyStoreConfig::get_local_policy_file(
			PathBuf::from("../../demo/policy-store/local.json".to_string()).as_path(),
		)
		.unwrap();
		check_policy_store(&policy);
	}

	#[test]
	fn get_local_policy_file_parce_json_fail() {
		let err = PolicyStoreConfig::get_local_policy_file(
			PathBuf::from("../../demo/policy-store/local_poisoned.json".to_string()).as_path(),
		)
		.expect_err("should be error");

		assert!(
			matches!(err, GetPolicyError::ParseJson(_)),
			"Expected ParseJson error, got: {:?}",
			err
		);
	}

	#[test]
	fn get_local_policy_file_path_fail() {
		let err = PolicyStoreConfig::get_local_policy_file(
			PathBuf::from("../../demo/policy-store/not_existent_file_path.json".to_string())
				.as_path(),
		)
		.expect_err("should be error");

		assert!(
			matches!(err, GetPolicyError::ReadFile(_)),
			"Expected ReadFile error, got: {:?}",
			err
		);
	}
}
