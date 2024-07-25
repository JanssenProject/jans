use base64::Engine;
use std::collections::{BTreeMap, BTreeSet};
use wasm_bindgen::{JsValue, UnwrapThrowExt};

use crate::*;

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
	Local {
		id: String,
	},
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

#[derive(Debug, serde::Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PolicyStoreEntry {
	#[serde(deserialize_with = "parse_schema")]
	pub schema: cedar_policy::Schema,
	pub trusted_issuers: Vec<crypto::types::TrustedIssuer>,
	#[serde(deserialize_with = "parse_policies")]
	pub policies: cedar_policy::PolicySet,
	pub default_entities: Option<serde_json::Value>,
}

fn parse_schema<'de, D>(deserializer: D) -> Result<cedar_policy::Schema, D::Error>
where
	D: serde::Deserializer<'de>,
{
	let source = <String as serde::Deserialize>::deserialize(deserializer)?;
	let decoded = base64::engine::general_purpose::STANDARD.decode(&source).expect_throw("Unable to parse Schema source as valid base64");

	let (schema, warnings) = cedar_policy::Schema::from_file_natural(decoded.as_slice()).expect_throw("Unable to parse Schema in Human Readable cedar format");
	for warning in warnings {
		let msg = format!("Schema Parser generated warning: {:?}", warning);
		web_sys::console::warn_1(&JsValue::from_str(&msg));
	}

	Ok(schema)
}

fn parse_policies<'de, D>(deserializer: D) -> Result<cedar_policy::PolicySet, D::Error>
where
	D: serde::Deserializer<'de>,
{
	let policies = <BTreeMap<String, String> as serde::Deserialize>::deserialize(deserializer)?;
	let policies = policies
		.into_iter()
		.map(|(id, s)| (id, base64::engine::general_purpose::STANDARD.decode(&s).expect_throw("Unable to parse Policy source as valid base64")))
		.map(|(id, b)| (id, String::from_utf8(b).unwrap_throw()))
		.map(|(id, s)| cedar_policy::Policy::parse(Some(id), s).unwrap_throw());

	Ok(cedar_policy::PolicySet::from_policies(policies).unwrap_throw())
}
