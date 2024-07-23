use std::{
	collections::{HashMap, HashSet},
	sync::OnceLock,
};

use crypto::{decode, TRUST_STORE};
use wasm_bindgen::throw_str;

use crate::*;
pub mod types;

pub static APPLICATION_NAME: OnceLock<String> = OnceLock::new();
pub static REQUIRE_AUD_VALIDATION: OnceLock<bool> = OnceLock::new();

pub fn init(config: &startup::types::CedarlingConfig) {
	if let Some(application_name) = config.application_name.as_ref() {
		APPLICATION_NAME.set(application_name.clone()).unwrap_throw();
	}

	REQUIRE_AUD_VALIDATION.set(config.require_aud_validation).unwrap_throw();
}

#[derive(Debug, serde::Deserialize)]
struct EntityExtractor {
	#[serde(rename = "type")]
	type_name: String,
	id: String,
	#[serde(flatten)]
	attributes: Vec<(String, serde_json::Value)>,
}

fn converter(_value: serde_json::Value) -> cedar_policy::RestrictedExpression {
	match _value {
		serde_json::Value::Bool(b) => cedar_policy::RestrictedExpression::new_bool(b),
		serde_json::Value::Number(n) => {
			if let Some(long) = n.as_i64() {
				cedar_policy::RestrictedExpression::new_long(long)
			} else if let Some(float) = n.as_f64() {
				cedar_policy::RestrictedExpression::new_decimal(float.to_string())
			} else if let Some(ulong) = n.as_u64() {
				cedar_policy::RestrictedExpression::new_long(ulong as _)
			} else {
				throw_str("Unknown number format encountered")
			}
		}
		serde_json::Value::String(s) => cedar_policy::RestrictedExpression::new_string(s),
		serde_json::Value::Array(a) => cedar_policy::RestrictedExpression::new_set(a.into_iter().map(converter)),
		serde_json::Value::Object(o) => cedar_policy::RestrictedExpression::new_record(o.into_iter().map(|(n, v)| (n, converter(v)))).unwrap_throw(),

		// NULL not accepted
		serde_json::Value::Null => throw_str("Encountered null in JSON to Entity converter"),
	}
}

pub fn json2entity(value: serde_json::Value) -> cedar_policy::Entity {
	let EntityExtractor { type_name, id, attributes } = serde_json::from_value(value).expect_throw("Expected Resource to contain 'type' and 'id' fields");

	let id = serde_json::json!({ "__entity": { "type": type_name, "id": id } });
	let id = cedar_policy::EntityUid::from_json(id).unwrap_throw();

	let parents = HashSet::new();
	let attrs = HashMap::from_iter(attributes.into_iter().map(|(n, value)| (n, converter(value))));

	cedar_policy::Entity::new(id, attrs, parents).unwrap_throw()
}

pub fn token2entities(input: &authz::types::AuthzInput) -> (cedar_policy::EntityUid, cedar_policy::Entities) {
	// TODO: enable returning Client as Principal
	let principal;
	let mut entities = cedar_policy::Entities::empty();
	let schema = startup::SCHEMA.get();

	// extract tokens
	let id_token: types::IdToken = decode::decode_jwt(&input.id_token, decode::TokenType::IdToken);
	let access_token: types::AccessToken = decode::decode_jwt(&input.access_token, decode::TokenType::AccessToken);

	// check if `aud` claim in id_token matches `client_id` in access token
	if !(id_token.aud == access_token.client_id) && !REQUIRE_AUD_VALIDATION.get().cloned().unwrap_or(false) {
		throw_str("id_token was not issued for this client: (id_token.aud != access_token.client_id)")
	}

	// check if both tokens were issued by the same issuer
	if id_token.iss != access_token.iss {
		throw_str("access_token and id_token weren't issued by the same issuer: (access_token.iss != id_token.iss)")
	}

	// create TrustedIssuer entities
	let trust_store = unsafe { std::ptr::addr_of!(TRUST_STORE).as_ref().unwrap_throw() };
	let issuers = trust_store.iter().map(|(_, entry)| (entry.issuer.get_entity()));
	entities = entities.add_entities(issuers, schema).unwrap_throw();

	// create Client, Application and access_token Entities
	let client = access_token.get_client_entity();

	let application_name = APPLICATION_NAME.get().map(String::as_str);
	let application_entity = access_token.get_application_entity(application_name, client.uid());

	entities = match application_entity {
		Some(a) => entities.add_entities([a, client, access_token.get_token_entity()], schema).unwrap_throw(),
		None => entities.add_entities([client, access_token.get_token_entity()], schema).unwrap_throw(),
	};

	// extract UserInfo
	let userinfo: types::UserInfoToken = decode::decode_jwt(&input.userinfo_token, decode::TokenType::UserInfoToken);
	if userinfo.sub != id_token.sub || userinfo.iss != id_token.iss {
		throw_str("userinfo token invalid: either sub or iss doesn't match id_token")
	}

	// create Roles entities
	let roles = userinfo.get_role_entities();

	// create User, userinfo_token and id_token entities
	let user_entity = userinfo.get_user_entity(&roles);
	principal = user_entity.uid();
	entities = entities
		.add_entities([user_entity, userinfo.get_token_entity(&roles), id_token.get_token_entity()], schema)
		.unwrap_throw();

	// Add Role entities and return Uid of User entity as principal
	(principal, entities.add_entities(roles.into_iter(), schema).unwrap_throw())
}
