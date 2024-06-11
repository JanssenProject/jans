use std::{borrow::Cow, str};

use cedar_policy::*;
use serde_wasm_bindgen::{from_value, to_value};
use wasm_bindgen::{prelude::*, throw_val};
use web_sys::*;

pub mod cedar;
pub mod types;
pub mod utils;

#[wasm_bindgen(start)]
pub fn start() {
	// setup panic hook
	console_error_panic_hook::set_once();
}

static_toml::static_toml! {
	pub(crate) static CONFIG = include_toml!("config.toml");
}

#[wasm_bindgen]
pub async fn init(tokens: JsValue, entities: Option<String>) {
	let tokens = from_value::<types::Tokens>(tokens).unwrap_throw();
	let schema = utils::fetch_schema().await;
	cedar::schema(Some(schema));

	if let Some(entities) = entities {
		let entities = utils::get(&entities).await.expect_throw("Can't fetch entities from URL");
		let entities = Entities::from_json_str(str::from_utf8(&entities).unwrap_throw(), cedar::schema(None)).unwrap_throw();
		cedar::entities(Some(entities));
	}

	// Load Policy Store
	let vector;
	let mut raw = if CONFIG.policy_store.use_static_store {
		include_bytes!("../policies.store")
	} else {
		vector = utils::get(&CONFIG.policy_store.remote_uri).await.expect_throw("Can't fetch policies from remote location");
		vector.as_slice()
	};

	let decompressed = if CONFIG.policy_store.use_brotli_decompression {
		let mut buffer = Vec::with_capacity(raw.len());
		brotli::BrotliDecompress(&mut raw, &mut buffer).expect_throw("Unable to Decompress Policy Store!");
		Cow::Owned(buffer)
	} else {
		Cow::Borrowed(raw)
	};

	let policies: PolicySet = str::from_utf8(&decompressed).unwrap_throw().parse().unwrap_throw();
	cedar::policies(Some(policies));

	// register sse
	let sse = EventSource::new(&tokens.sse_url).unwrap_throw();
	let sse2 = sse.clone();

	// Setup event listeners
	let onopen = Closure::once_into_js(move || {
		let onmessage = Closure::<dyn Fn(MessageEvent)>::new(move |ev: MessageEvent| {
			console::log_2(&JsValue::from_str("Received message: "), &ev);
		})
		.into_js_value();
		sse2.set_onmessage(Some(onmessage.as_ref().unchecked_ref()));

		let onerror = Closure::<dyn Fn(JsValue)>::new(move |ev: JsValue| throw_val(ev)).into_js_value();
		sse2.set_onerror(Some(onerror.as_ref().unchecked_ref()));
	});

	sse.set_onopen(Some(onopen.as_ref().unchecked_ref()));
}

#[wasm_bindgen]
pub async fn authz(req: JsValue) -> JsValue {
	let request = from_value::<types::Request>(req).unwrap_throw();

	let request = cedar_policy::Request::from(request);
	let response = cedar::authorizer().is_authorized(&request, cedar::policies(None), cedar::entities(None));

	to_value(&response.decision()).unwrap_throw()
}
