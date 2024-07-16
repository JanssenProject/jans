use serde_wasm_bindgen::from_value;
use wasm_bindgen::prelude::*;

use crate::{startup, token2entity::token2entities};

pub mod types;

#[wasm_bindgen]
pub async fn authz(req: JsValue) -> JsValue {
	let input = from_value::<types::AuthzInput>(req).unwrap_throw();

	// generate request
	let context = cedar_policy::Context::from_json_value(input.context, None).expect_throw("Unable to generate context Object");

	// generate extra parameters for cedar decision
	let entities = token2entities(&input);
	let policies = startup::POLICY_SET.get().expect_throw("POLICY_SET not initialized");

	JsValue::NULL
}
