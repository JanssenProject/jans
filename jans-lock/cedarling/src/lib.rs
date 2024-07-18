use serde_wasm_bindgen::from_value;
use wasm_bindgen::prelude::*;

pub mod http;

mod authz;
mod crypto;
mod lock_master;
mod startup;
mod token2entity;

#[wasm_bindgen(start)]
pub async fn start() {
	// Extract information on unexpected panics
	console_error_panic_hook::set_once();
}

#[wasm_bindgen]
pub async fn init(config: JsValue) {
	let config = from_value::<startup::types::CedarlingConfig>(config).unwrap_throw();

	// Startup sequence
	startup::init(&config).await;

	// Initialize authz module
	token2entity::init(&config);
}
