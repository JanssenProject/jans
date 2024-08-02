use std::sync::OnceLock;

use serde_wasm_bindgen::{from_value, to_value};
use wasm_bindgen::prelude::*;

use crate::*;

pub mod statement;
pub mod token2entity;
pub mod types;

pub static APPLICATION_NAME: OnceLock<String> = OnceLock::new();
pub static REQUIRE_AUD_VALIDATION: OnceLock<bool> = OnceLock::new();

pub fn init(config: &startup::types::CedarlingConfig) {
	APPLICATION_NAME.set(config.application_name.clone()).unwrap_throw();
	REQUIRE_AUD_VALIDATION.set(config.require_aud_validation).unwrap_throw();
}

#[wasm_bindgen]
pub async fn authz(req: JsValue) -> JsValue {
	let input = from_value::<types::AuthzInput>(req).unwrap_throw();

	// generate extra parameters for cedar decision
	let (uids, mut entities) = token2entity::token2entities(&input);

	// append resource entity
	let r = serde_json::json!([input.resource]);
	entities = entities.add_entities_from_json_value(r, startup::SCHEMA.get()).unwrap_throw();

	// prepare request
	let action = input.action.parse().expect_throw("Unable to parse action");
	let resource = {
		#[derive(serde::Deserialize)]
		struct Extract {
			#[serde(rename = "type")]
			pub _type: String,
			pub id: String,
		}

		let uid = input.resource.get("uid").cloned().expect_throw("Can't find uid in Resource entity definition");
		let uid: Extract = serde_json::from_value(uid).expect_throw("Resource uid is not a valid cedar uid");
		let id = serde_json::json!({ "__entity": { "type": uid._type, "id": uid.id } });

		cedar_policy::EntityUid::from_json(id).unwrap_throw()
	};
	let context = cedar_policy::Context::from_json_value(input.context, None).expect_throw("Unable to generate context Object");

	// generate statement
	let statement = statement::parse(input.statement.as_deref().unwrap_or("User"));
	let input = (action, resource, context);

	// evaluate query
	let mut ctx = Default::default();
	let answer = statement::evaluate(statement, &uids, &entities, &input, &mut ctx);

	to_value(&serde_json::json!({ "decision": answer, "policies": ctx.policies })).unwrap_throw()
}
