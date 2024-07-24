use std::sync::OnceLock;

use serde_wasm_bindgen::{from_value, to_value};
use wasm_bindgen::prelude::*;

use crate::*;

pub mod token2entity;
pub mod types;

pub static APPLICATION_NAME: OnceLock<String> = OnceLock::new();
pub static REQUIRE_AUD_VALIDATION: OnceLock<bool> = OnceLock::new();

pub fn init(config: &startup::types::CedarlingConfig) {
	if let Some(application_name) = config.application_name.as_ref() {
		APPLICATION_NAME.set(application_name.clone()).unwrap_throw();
	}

	REQUIRE_AUD_VALIDATION.set(config.require_aud_validation).unwrap_throw();
}

#[wasm_bindgen]
pub async fn authz(req: JsValue) -> JsValue {
	let input = from_value::<types::AuthzInput>(req).unwrap_throw();

	// generate extra parameters for cedar decision
	let (principal, entities) = token2entity::token2entities(&input);
	let policies = startup::POLICY_SET.get().expect_throw("POLICY_SET not initialized");

	// generate request
	let action = input.action.parse().expect_throw("Unable to parse action");
	let resource = token2entity::json2entity(input.resource);
	let context = cedar_policy::Context::from_json_value(input.context, None).expect_throw("Unable to generate context Object");

	let request = cedar_policy::Request::new(Some(principal), Some(action), Some(resource.uid()), context, startup::SCHEMA.get()).unwrap_throw();

	// create authorizer
	let authorizer = cedar_policy::Authorizer::new();
	let answer = authorizer.is_authorized(&request, policies, &entities);

	to_value(&answer.decision()).unwrap_throw()
}
