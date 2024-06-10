use super::types::Tokens;

use cedar_policy::Schema;
use wasm_bindgen::{prelude::*, throw_str};
use wasm_bindgen_futures::*;
use web_sys::*;

#[wasm_bindgen]
extern "C" {
	#[wasm_bindgen(js_name = fetch)]
	fn fetch_with_request_and_init(input: &Request, init: &RequestInit) -> js_sys::Promise;
}

pub async fn get_str(url: &str, _: &Tokens) -> Option<String> {
	let mut opts = RequestInit::new();
	opts.method("GET");
	opts.mode(RequestMode::NoCors);

	let request = Request::new_with_str(url).unwrap();
	let resp_value = JsFuture::from(fetch_with_request_and_init(&request, &opts)).await.unwrap();

	let resp: Response = resp_value.dyn_into().unwrap();
	if !resp.ok() {
		return None;
	}

	// extract text
	let text = resp.text().unwrap();
	let text = JsFuture::from(text).await.unwrap();

	// Send the text response back to JS.
	text.as_string()
}

pub async fn fetch_schema() -> cedar_policy::Schema {
	let request = Request::new_with_str(crate::CONFIG.schema_url).unwrap();

	let mut opts = RequestInit::new();
	opts.method("GET");
	opts.mode(RequestMode::NoCors);

	request.headers().set("Accept", "application/vnd.github.v3.raw").unwrap();
	let response_value = JsFuture::from(fetch_with_request_and_init(&request, &opts)).await.unwrap();

	let response: Response = response_value.dyn_into().unwrap();
	if response.ok() {
		let buffer = JsFuture::from(response.array_buffer().unwrap()).await.unwrap();
		let buffer = js_sys::Uint8Array::new(&buffer);
		let buffer = buffer.to_vec();

		let (schema, warnings) = Schema::from_file_natural(buffer.as_slice()).unwrap();
		for warning in warnings {
			let msg = format!("Schema Parser generated Warning: {}", warning);
			let msg = JsValue::from_str(&msg);
			console::warn_1(&msg)
		}

		schema
	} else {
		let status_text = response.status_text();
		let error_message = format!("Failed to fetch Schema: {}", status_text);
		throw_str(&error_message)
	}
}
