use serde_wasm_bindgen::{from_value, to_value};
use wasm_bindgen::prelude::*;

pub mod authz;
mod crypto;
pub mod http;
mod lock_master;
mod startup;

#[wasm_bindgen(start)]
pub async fn start() {
	// Extract information on unexpected panics
	console_error_panic_hook::set_once();

	let msg = JsValue::from_str(concat!("Jans Cedarling; v", env!("CARGO_PKG_VERSION")));
	web_sys::console::log_1(&msg);
}

#[wasm_bindgen]
pub async fn init(config: JsValue) {
	let config = from_value::<startup::types::CedarlingConfig>(config).unwrap();

	// Startup sequence
	startup::init(&config).await;

	// Initialize authz module
	authz::init(&config);
}

#[wasm_bindgen_test::wasm_bindgen_test]
async fn remote() {
	let config = startup::types::CedarlingConfig {
		application_name: Some("test#docs".into()),
		require_aud_validation: false,
		jwt_validation: false,
		policy_store: startup::types::PolicyStoreConfig::Local {
			id: "fc2fee0253af46f3dce320484c42444ae0b24f7ec84a".into(),
		},
		decompress_policy_store: false,
		trust_store_refresh_rate: Some(5000),
		supported_signature_algorithms: std::collections::BTreeSet::from_iter(["HS256".into(), "HS384".into(), "RS256".into()]),
	};

	let config = to_value(&config).unwrap_throw();
	web_sys::console::log_1(&config);

	init(config).await;
}
