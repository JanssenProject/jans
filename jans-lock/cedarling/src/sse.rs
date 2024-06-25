use wasm_bindgen::{prelude::*, throw_val};
use web_sys::*;

use super::CONFIG;

pub(crate) fn install() {
	if CONFIG.dynamic_configuration {
		let url = format!("{}/sse", CONFIG.lock_master.url);
		let sse = EventSource::new(&url).unwrap();
		let sse2 = sse.clone();

		// Setup SSE event listeners
		let onopen = Closure::once_into_js(move || {
			let onmessage = Closure::<dyn Fn(MessageEvent)>::new(move |ev: MessageEvent| {
				console::log_2(&JsValue::from_str("Cedarling Received message: "), &ev);
				unimplemented!("Dynamic Configuration Updates")
			})
			.into_js_value();
			sse2.set_onmessage(Some(onmessage.as_ref().unchecked_ref()));

			let onerror = Closure::<dyn Fn(JsValue)>::new(move |ev: JsValue| throw_val(ev)).into_js_value();
			sse2.set_onerror(Some(onerror.as_ref().unchecked_ref()));
		});

		sse.set_onopen(Some(onopen.as_ref().unchecked_ref()));
	}
}
