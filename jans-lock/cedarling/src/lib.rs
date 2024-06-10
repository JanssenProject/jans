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
pub async fn init(auth: JsValue, entities: Option<String>, policies: Option<String>) {
	let tokens = from_value::<types::Tokens>(auth).unwrap();
	let schema = utils::fetch_schema().await;
	cedar::schema(Some(schema));

	if let Some(entities) = entities {
		let entities = utils::get_str(&entities, &tokens).await.expect_throw("Can't fetch entities from URL");
		let entities = Entities::from_json_str(&entities, cedar::schema(None)).unwrap();
		cedar::entities(Some(entities));
	}

	if let Some(policies) = policies {
		let policies = utils::get_str(&policies, &tokens).await.expect_throw("Can't fetch policies from URL");
		let policies: PolicySet = policies.parse().unwrap();
		cedar::policies(Some(policies));
	}

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
	let request = from_value::<types::Request>(req).unwrap();

	let request = cedar_policy::Request::from(request);
	let response = cedar::authorizer().is_authorized(&request, cedar::policies(None), cedar::entities(None));

	to_value(&response.decision()).unwrap()
}
