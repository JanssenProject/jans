use std::{collections::HashMap, sync::OnceLock};

use crypto::{decode, TRUST_STORE};
use wasm_bindgen::throw_str;

use crate::*;
pub mod types;

pub static APPLICATION_NAME: OnceLock<String> = OnceLock::new();
pub static REQUIRE_AUD_VALIDATION: OnceLock<bool> = OnceLock::new();

pub(crate) fn init(config: &startup::types::CedarlingConfig) {
	if let Some(application_name) = config.application_name.as_ref() {
		APPLICATION_NAME.set(application_name.clone()).unwrap_throw();
	}

	REQUIRE_AUD_VALIDATION.set(config.require_aud_validation).unwrap_throw();
}

pub fn token2entities(input: &authz::types::AuthzInput) -> (cedar_policy::EntityUid, cedar_policy::Entities) {
	let mut entities = cedar_policy::Entities::empty();
	let schema = startup::SCHEMA.get();

	// create TrustedIssuer entities
	let trust_store = unsafe { std::ptr::addr_of!(TRUST_STORE).as_ref().unwrap_throw() };
	let issuers = trust_store.iter().map(|(_, entry)| (entry.issuer.get_entity()));
	entities = entities.add_entities(issuers, schema).unwrap_throw();

	// ensure iss can issue id_tokens according to trust store
	let mut id_token_valid = false;
	let id_token: types::IdToken = decode::decode_jwt(&input.id_token, decode::TokenType::IdToken);

	// create Client and Application Entities
	for access_token in &input.access_tokens {
		let access_token: types::AccessToken = decode::decode_jwt(access_token, decode::TokenType::AccessToken);

		// check if `aud` claim in id_token matches `client_id` in access token
		if id_token.aud == access_token.client_id {
			id_token_valid = true;
		}

		let client = access_token.get_client_entity();

		let application_name = APPLICATION_NAME.get().map(String::as_str);
		let application_entity = access_token.get_application_entity(application_name, client.uid());

		entities = match application_entity {
			Some(e) => entities.add_entities([e, client, access_token.get_token_entity()], schema).unwrap_throw(),
			None => entities.add_entities([client, access_token.get_token_entity()], schema).unwrap_throw(),
		};
	}

	// if REQUIRE_AUD_VALIDATION, skip id_token if `aud` isn't found in any access_token
	if !REQUIRE_AUD_VALIDATION.get().cloned().unwrap_or(false) {
		id_token_valid = true
	}

	// skip further entities' creation if id_token is not valid
	if !id_token_valid {
		throw_str("REQUIRE_AUD_VALIDATION = true: id_token lacks a corresponding access_token with a matching client_id")
	}

	// Join id_tokens and [userinfo_tokens]
	let userinfo_list = input
		.userinfo_tokens
		.iter()
		.filter_map(|userinfo_token| {
			// dispose any userinfo_tokens that don't match the id_tokens, sub and iss
			let userinfo: types::UserInfoToken = decode::decode_jwt(userinfo_token, decode::TokenType::UserInfoToken);
			(userinfo.sub != id_token.sub || userinfo.iss != id_token.iss).then_some(userinfo)
		})
		.collect::<Vec<_>>();

	// create Roles entities
	let mut roles = HashMap::new();
	for userinfo in &userinfo_list {
		roles.insert(&userinfo.sub, userinfo.get_role_entities());
	}

	// create User and id_token entity
	let userinfo = userinfo_list
		.iter()
		.find(|u| u.sub == id_token.sub)
		.expect_throw("Unable to find UserInfoToken with sub matching IdToken");
	let user_entity = userinfo.get_user_entity(&roles);
	let principal = user_entity.uid();
	entities = entities.add_entities([user_entity, id_token.get_token_entity()], schema).unwrap_throw();

	// create UserInfo entities
	entities = entities.add_entities(userinfo_list.iter().map(|i| i.get_token_entity(&roles)), schema).unwrap_throw();

	// Add Role entities and return Uid of User entity as principal
	(principal, entities.add_entities(roles.drain().flat_map(|(_, entity)| entity.into_iter()), schema).unwrap_throw())
}
