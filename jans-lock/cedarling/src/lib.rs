use serde_wasm_bindgen::from_value;
use wasm_bindgen::prelude::*;

pub mod http;

mod authz;
mod startup;
mod lock_master;
mod crypto;

#[wasm_bindgen(start)]
pub(crate) async fn start() {
	// Extract information on unexpected panics
	console_error_panic_hook::set_once();
}

#[wasm_bindgen]
pub async fn init(config: JsValue) {
	let config = from_value::<startup::types::CedarlingConfig>(config).unwrap_throw();

	// Startup sequence
	startup::init(&config).await;

	// Initialize authz module
	authz::init(&config);
}
