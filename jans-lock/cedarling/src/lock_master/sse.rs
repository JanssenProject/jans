use wasm_bindgen::{prelude::*, throw_val};
use web_sys::*;

pub fn init(enable_dynamic_configuration: bool, lock_sse_uri: &str) {
	// Setup dynamic SSE updates for LockMaster
	if enable_dynamic_configuration {
		let sse = EventSource::new(&lock_sse_uri).unwrap_throw();
		let sse2 = sse.clone();

		let onopen = Closure::once_into_js(move || {
			let onmessage = Closure::<dyn Fn(MessageEvent)>::new(move |ev: MessageEvent| {
				console::log_2(&JsValue::from_str("Cedarling Received message: "), &ev);
				unimplemented!("Dynamic Configuration Updates")
			})
			.into_js_value();
			sse2.set_onmessage(Some(onmessage.unchecked_ref()));

			let onerror = Closure::<dyn Fn(JsValue)>::new(move |ev: JsValue| throw_val(ev)).into_js_value();
			sse2.set_onerror(Some(onerror.unchecked_ref()));
		});

		sse.set_onopen(Some(onopen.unchecked_ref()));
	}
}
