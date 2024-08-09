use std::{borrow::Cow, future::Future};

use wasm_bindgen::prelude::*;
use wasm_bindgen_futures::*;
use web_sys::*;

#[wasm_bindgen]
extern "C" {
	#[wasm_bindgen(js_name = fetch)]
	pub fn fetch_with_request_and_init(input: &Request, init: &RequestInit) -> js_sys::Promise;
}

pub async fn get(url: &str, headers: &[(&str, &str)]) -> Option<Response> {
	let mut opts = RequestInit::new();
	opts.method("GET");
	opts.mode(RequestMode::Cors);

	// insert headers
	let h = Headers::new().unwrap_throw();
	for (key, value) in headers {
		h.set(key, value).unwrap_throw();
	}

	opts.headers(&h);

	let req = Request::new_with_str(url).unwrap_throw();
	let res = JsFuture::from(fetch_with_request_and_init(&req, &opts)).await.unwrap_throw();

	// check if return value is response
	res.dyn_into::<Response>().ok()
}

#[allow(dead_code)]
pub enum PostBody<'a, T: serde::Serialize = ()> {
	None,
	Json(T),
	Form(T),
	String(Cow<'a, str>),
	Bytes(Vec<u8>),
}

pub async fn post<'a, T: serde::Serialize>(url: &str, body: PostBody<'a, T>, headers: &[(&'a str, &'a str)]) -> Option<Response> {
	let mut opts = RequestInit::new();
	let h = Headers::new().unwrap_throw();

	opts.method("POST");
	opts.mode(RequestMode::Cors);

	// Set the body
	match body {
		PostBody::None => {}
		PostBody::Form(form) => {
			let init = serde_wasm_bindgen::to_value(&form).unwrap_throw();
			let params = UrlSearchParams::new_with_str_sequence_sequence(&init).unwrap_throw();

			opts.body(Some(&JsValue::from(params)));
			h.set("Content-Type", "application/x-www-form-urlencoded").unwrap_throw();
		}
		PostBody::Json(json) => {
			let json = serde_json::to_string(&json).unwrap_throw();
			let json = JsValue::from_str(&json);

			opts.body(Some(&json));
			h.set("Content-Type", "application/json").unwrap_throw();
		}
		PostBody::String(string) => {
			opts.body(Some(&JsValue::from_str(&string)));
			h.set("Content-Type", "text/plain").unwrap_throw();
		}
		PostBody::Bytes(bytes) => {
			let array = js_sys::Uint8Array::new_with_length(bytes.len() as _);
			array.copy_from(&bytes);
			opts.body(Some(&array));
			h.set("Content-Type", "application/octet-stream").unwrap_throw();
		}
	}

	// set headers
	for (key, value) in headers {
		h.set(key, value).unwrap_throw();
	}
	opts.headers(&h);

	let request = Request::new_with_str(url).unwrap_throw();
	let res = JsFuture::from(fetch_with_request_and_init(&request, &opts)).await.unwrap_throw();

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
			let message = JsValue::from_str(format!("{}: {}", self.status_text(), text.as_string().unwrap_throw()).as_str());

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
			let message = JsValue::from_str(format!("{}: {}", self.status_text(), text.as_string().unwrap_throw()).as_str());

			console::error_1(&message);
			None
		}
	}
}
