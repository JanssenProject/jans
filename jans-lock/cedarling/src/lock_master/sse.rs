use std::{
	collections::{BTreeMap, HashSet},
	sync::OnceLock,
};

use wasm_bindgen::{prelude::*, throw_val};
use web_sys::*;

use super::types;

// Stores a status list referencing each JWT's `jti` claim
pub(crate) static mut STATUS_LISTS: OnceLock<BTreeMap<String, (u8, HashSet<String>)>> = OnceLock::new();

pub fn init(enable_dynamic_configuration: bool, lock_sse_uri: &str) {
	unsafe {
		STATUS_LISTS.set(Default::default()).expect_throw("STATUS_LIST already set");
	}

	// Setup dynamic SSE updates for LockMaster
	if enable_dynamic_configuration {
		let sse = EventSource::new(&lock_sse_uri).unwrap_throw();
		let sse2 = sse.clone();

		let onopen = Closure::once_into_js(move || {
			// called for each successful message
			let onmessage = Closure::<dyn Fn(MessageEvent)>::new(move |ev: MessageEvent| {
				let data = ev.data();

				let json = data.as_string().expect_throw("Unable to convert event data to string");
				let event: types::SseUpdate = serde_json::from_str(&json).unwrap_throw();

				match event {
					types::SseUpdate::StatusListUpdate { bits, status_list } => {
						let status_lists = unsafe { STATUS_LISTS.get_mut().unwrap_throw() };
						status_lists.entry(status_list).and_modify(|(status, _)| *status = bits).or_default();
					}
				}
			})
			.into_js_value();
			sse2.set_onmessage(Some(onmessage.unchecked_ref()));

			let onerror = Closure::<dyn Fn(JsValue)>::new(move |ev: JsValue| throw_val(ev)).into_js_value();
			sse2.set_onerror(Some(onerror.unchecked_ref()));
		});

		sse.set_onopen(Some(onopen.unchecked_ref()));
	}
}
