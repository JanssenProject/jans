use wasm_bindgen::prelude::*;

pub(crate) mod http;

mod authz;
mod sse;
mod startup;

static_toml::static_toml! {
	pub(crate) static CONFIG = include_toml!("config.toml");
}

#[wasm_bindgen(start)]
pub(crate) async fn start() {
	// TODO: setup panic hook, on production we should use wasm-bindgen::UnwrapThrowExt
	console_error_panic_hook::set_once();

	// load policy store
	let config = startup::open_id_config(CONFIG.openid_config_url).await;
	let policy_store = startup::get_policy_store(config).await;

	// Persist Policy Store Data
	startup::persist_policy_store_data(policy_store);

	// Enable Dynamic Updates via Server Sent Events
	sse::install();
}
