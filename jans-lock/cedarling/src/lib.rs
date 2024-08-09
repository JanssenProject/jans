use serde_wasm_bindgen::from_value;
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
	let mut config = from_value::<startup::types::CedarlingConfig>(config).expect_throw("Unable to parse startup config");

	// Startup sequence
	startup::init(&mut config).await;

	// Initialize authz module
	authz::init(&config);
}
