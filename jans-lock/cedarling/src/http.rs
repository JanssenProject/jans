use std::{borrow::Cow, future::Future};

use wasm_bindgen::prelude::*;
use wasm_bindgen_futures::*;
use web_sys::*;

#[wasm_bindgen]
extern "C" {
	#[wasm_bindgen(js_name = fetch)]
	pub fn fetch_with_request_and_init(input: &Request, init: &RequestInit) -> js_sys::Promise;
}

pub(crate) async fn get(url: &str, headers: &[(&str, &str)]) -> Option<Response> {
	let mut opts = RequestInit::new();
	opts.method("GET");
	opts.mode(RequestMode::NoCors);

	// insert headers
	let h = Headers::new().unwrap();
	for (key, value) in headers {
		h.set(key, value).unwrap();
	}

	opts.headers(&h);

	let req = Request::new_with_str(url).unwrap();
	let res = JsFuture::from(fetch_with_request_and_init(&req, &opts)).await.unwrap();

	// check if return value is response
	res.dyn_into::<Response>().ok()
}

#[allow(dead_code)]
pub(crate) enum PostBody<'a, T: serde::Serialize = ()> {
	None,
	Json(T),
	Form(T),
	String(Cow<'a, str>),
	Bytes(Vec<u8>),
}

pub(crate) async fn post<'a, T: serde::Serialize>(url: &str, body: PostBody<'a, T>, headers: &[(&'a str, &'a str)]) -> Option<Response> {
	let mut opts = RequestInit::new();
	let h = Headers::new().unwrap();

	opts.method("POST");	
	opts.mode(RequestMode::NoCors);

	// Set the body
	match body {
		PostBody::None => {}
		PostBody::Form(form) => {
			let init = serde_wasm_bindgen::to_value(&form).unwrap();
			let params = UrlSearchParams::new_with_str_sequence_sequence(&init).unwrap();

			opts.body(Some(&JsValue::from(params)));
			h.set("Content-Type", "application/x-www-form-urlencoded").unwrap();
		}
		PostBody::Json(json) => {
			let json = serde_json::to_string(&json).unwrap();
			let json = JsValue::from_str(&json);

			opts.body(Some(&json));
			h.set("Content-Type", "application/json").unwrap();
		}
		PostBody::String(string) => {
			opts.body(Some(&JsValue::from_str(&string)));
			h.set("Content-Type", "text/plain").unwrap();
		}
		PostBody::Bytes(bytes) => {
			let array = js_sys::Uint8Array::new_with_length(bytes.len() as _);
			array.copy_from(&bytes);
			opts.body(Some(&array));
			h.set("Content-Type", "application/octet-stream").unwrap();
		}
	}

	// set headers
	for (key, value) in headers {
		h.set(key, value).unwrap();
	}
	opts.headers(&h);

	let request = Request::new_with_str(url).unwrap();
	let res = JsFuture::from(fetch_with_request_and_init(&request, &opts)).await.unwrap();

	// check if return value is response
	res.dyn_into::<Response>().ok()
}

pub trait ResponseEx {
	fn into_string(self) -> impl Future<Output = Option<String>>;
	fn into_bytes(self) -> impl Future<Output = Option<Vec<u8>>>;
	fn into_json<T: serde::de::DeserializeOwned>(self) -> impl Future<Output = Option<T>>;
}

impl ResponseEx for Response {
	async fn into_json<T: serde::de::DeserializeOwned>(self) -> Option<T> {
		let text = self.into_string().await?;
		serde_json::from_str(&text).ok()
	}

	async fn into_string(self) -> Option<String> {
		if self.ok() {
			let text = JsFuture::from(self.text().ok()?).await.ok()?;
			text.as_string()
		} else {
			let text = JsFuture::from(self.text().ok()?).await.ok()?;
			let message = JsValue::from_str(format!("{}: {}", self.status_text(), text.as_string().unwrap()).as_str());

			console::error_1(&message);
			None
		}
	}

	async fn into_bytes(self) -> Option<Vec<u8>> {
		if self.ok() {
			let array = JsFuture::from(self.array_buffer().ok()?).await.ok()?;
			let array = js_sys::Uint8Array::new(&array);
			Some(array.to_vec())
		} else {
			let text = JsFuture::from(self.text().ok()?).await.ok()?;
			let message = JsValue::from_str(format!("{}: {}", self.status_text(), text.as_string().unwrap()).as_str());

			console::error_1(&message);
			None
		}
	}
}
