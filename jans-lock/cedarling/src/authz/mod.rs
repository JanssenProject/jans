use serde_wasm_bindgen::from_value;
use wasm_bindgen::prelude::*;

use crate::startup;

mod token2entity;
mod types;

#[wasm_bindgen]
pub async fn authz(req: JsValue) -> JsValue {
	let _request = from_value::<types::Tokens>(req).unwrap();
	JsValue::NULL
}

pub(crate) fn init(_: &startup::types::CedarlingConfig) {}
