use std::sync::OnceLock;

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
