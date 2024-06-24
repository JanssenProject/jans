use serde_wasm_bindgen::from_value;
use wasm_bindgen::prelude::*;

use super::types;

#[wasm_bindgen]
pub async fn authz(req: JsValue) -> JsValue {
	// TODO: Stub
	let _request = from_value::<types::Tokens>(req).unwrap();
	JsValue::NULL
}
