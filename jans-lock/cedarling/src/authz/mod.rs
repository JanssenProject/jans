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
	if let Some(application_name) = config.application_name.as_ref() {
		APPLICATION_NAME.set(application_name.clone()).unwrap_throw();
	}

	REQUIRE_AUD_VALIDATION.set(config.require_aud_validation).unwrap_throw();
}

#[wasm_bindgen]
pub async fn authz(req: JsValue) -> JsValue {
	let input = from_value::<types::AuthzInput>(req).unwrap_throw();

	// generate extra parameters for cedar decision
	let (uids, mut entities) = token2entity::token2entities(&input);
	let policies = startup::POLICY_SET.get().expect_throw("POLICY_SET not initialized");

	// append resource entity
	let r = serde_json::json!([input.resource]);
	entities = entities.add_entities_from_json_value(r, startup::SCHEMA.get()).unwrap_throw();

	// generate request
	let action = input.action.parse().expect_throw("Unable to parse action");
	let resource = {
		#[derive(serde::Deserialize)]
		struct Extract {
			#[serde(rename = "type")]
			pub _type: String,
			pub id: String,
		}

		let uid = input.resource.get("uid").cloned().expect_throw("Can't find uid in Resource entity definition");
		let uid: Extract = serde_json::from_value(uid).expect_throw("Resource uid is not a valid cedar UID");
		let id = serde_json::json!({ "__entity": { "type": uid._type, "id": uid.id } });

		cedar_policy::EntityUid::from_json(id).unwrap_throw()
	};
	let context = cedar_policy::Context::from_json_value(input.context, None).expect_throw("Unable to generate context Object");

	let answer = match input.statement {
		Some(s) => {
			let statement = statement::parse(s.as_str());
			let input = (action, resource, context);

			statement::evaluate(statement, &uids, &entities, &input, &mut Default::default())
		}
		None => {
			// defaults to checking user access
			let user_decision = cedar_policy::Request::new(Some(uids.user), Some(action), Some(resource), context, startup::SCHEMA.get()).unwrap_throw();

			// create authorizer
			let authorizer = cedar_policy::Authorizer::new();
			authorizer.is_authorized(&user_decision, policies, &entities).decision()
		}
	};

	to_value(&answer).unwrap_throw()
}
