use std::{borrow::Cow, sync::OnceLock};

use cedar_policy::*;
use wasm_bindgen::{prelude::*, throw_str};
use web_sys::*;

use crate::{crypto, http, lock_master};
use http::ResponseEx;

pub(crate) mod types;

pub(crate) static SCHEMA: OnceLock<Schema> = OnceLock::new();
pub(crate) static POLICY_SET: OnceLock<PolicySet> = OnceLock::new();

pub(crate) async fn init(config: &types::CedarlingConfig) {
	init_policy_store(&config).await;
}

pub(crate) async fn init_policy_store(config: &types::CedarlingConfig) {
	// Get PolicyStore JSON
	let source = match &config.policy_store {
		types::PolicyStoreConfig::Local => Cow::Borrowed(include_bytes!(concat!(env!("CARGO_MANIFEST_DIR"), "/policy-store/default.json")).as_slice()),
		types::PolicyStoreConfig::Remote { url } => {
			let res = http::get(url, &[]).await.expect_throw("Unable fetch Policy Store");
			let bytes = res.into_bytes().await.expect_throw("Can't convert Policy Store response to String");
			Cow::Owned(bytes)
		}
		policy_store_config => lock_master::init(policy_store_config).await,
	};

	// Decompress if necessary
	let source = match config.decompress_policy_store {
		true => Cow::Owned(miniz_oxide::inflate::decompress_to_vec_zlib(&source).unwrap_throw()),
		false => source,
	};

	// Parse JSON
	let policy_store = serde_json::from_slice::<serde_json::Value>(&source).unwrap_throw();
	let mut policy_store = match policy_store {
		serde_json::Value::Object(map) => map,
		p => {
			let error = format!("Expected top-level policy store to be an Object. Found: {:?}", p);
			throw_str(&error)
		}
	};

	// Load Schema
	let schema = {
		let schema = policy_store.remove("Schema").expect_throw("Can't find Schema in policy store");
		Schema::from_json_value(schema).unwrap_throw()
	};

	// Load PolicySet
	let policies = {
		let mut policies = policy_store.remove("PolicySet").expect_throw("Can't find PolicySet in policy store");
		let policies = policies.as_array_mut().expect_throw("expect_throw PolicySet to be an array");

		let iter = policies.drain(..).map(|policy| Policy::from_json(None, policy).unwrap_throw());
		PolicySet::from_policies(iter).unwrap_throw()
	};

	// Persist PolicyStore data
	SCHEMA.set(schema).expect_throw("SCHEMA has already been initialized");
	POLICY_SET.set(policies).expect_throw("POLICY_SET has already been initialized");
	crypto::init(config, policy_store);
}
