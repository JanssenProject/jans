use std::{borrow::Cow, collections::BTreeMap, sync::OnceLock};

use cedar_policy::*;
use wasm_bindgen::prelude::*;
use web_sys::*;

use crate::{crypto, http, lock_master};
use http::ResponseEx;

pub mod types;

pub static SCHEMA: OnceLock<Schema> = OnceLock::new();
pub static POLICY_SET: OnceLock<PolicySet> = OnceLock::new();
pub static DEFAULT_ENTITIES: OnceLock<serde_json::Value> = OnceLock::new();

pub async fn init(config: &mut types::CedarlingConfig) {
	let trusted_issuers = init_policy_store(config).await;
	crypto::init(config, trusted_issuers);
}

pub async fn init_policy_store(config: &mut types::CedarlingConfig) -> BTreeMap<String, crypto::types::TrustedIssuer> {
	// Get PolicyStore JSON
	let mut policy_store: types::PolicyStoreEntry = match &mut config.policy_store {
		#[cfg(feature = "direct_startup_strategy")]
		types::PolicyStoreConfig::Direct { value } => serde_json::from_value(value.take()).unwrap_throw(),
		types::PolicyStoreConfig::Local { id } => {
			let data = include_bytes!(concat!(env!("CARGO_MANIFEST_DIR"), "/policy-store/default.json")).as_slice();
			let decompressed = match config.decompress_policy_store {
				true => Cow::Owned(miniz_oxide::inflate::decompress_to_vec_zlib(data).unwrap_throw()),
				false => Cow::Borrowed(data),
			};

			let source = serde_json::from_slice::<serde_json::Value>(&decompressed).unwrap_throw();
			let entry = source.get(id.as_str()).cloned().expect_throw("Can't find PolicyStore with given id in Local Store");

			serde_json::from_value(entry).unwrap_throw()
		}
		types::PolicyStoreConfig::Remote { url } => {
			let res = http::get(url, &[]).await.expect_throw("Unable fetch Policy Store");
			let bytes = res.into_bytes().await.expect_throw("Can't convert Policy Store response to String");

			// decompress if necessary
			let bytes = match config.decompress_policy_store {
				true => Cow::Owned(miniz_oxide::inflate::decompress_to_vec_zlib(&bytes).unwrap_throw()),
				false => Cow::Owned(bytes),
			};

			serde_json::from_slice(&bytes).unwrap_throw()
		}
		policy_store_config => lock_master::init(policy_store_config, &config.application_name, config.decompress_policy_store).await,
	};

	// Persist PolicyStore data
	SCHEMA.set(policy_store.schema).expect_throw("SCHEMA already initialized");
	POLICY_SET.set(policy_store.policies).expect_throw("POLICY_SET already initialized");

	if let Some(entities) = policy_store.default_entities.take() {
		DEFAULT_ENTITIES.set(entities).expect_throw("DEFAULT_ENTITIES already initialized");
	}

	policy_store.trusted_issuers
}
