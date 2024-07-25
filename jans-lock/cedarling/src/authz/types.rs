#![allow(unused)]

use std::collections::{HashMap, HashSet};

use cedar_policy::*;
use serde::Deserialize;
use wasm_bindgen::{throw_str, UnwrapThrowExt};

use crate::crypto::TRUST_STORE;

#[derive(serde::Deserialize, Debug)]
pub struct AuthzInput {
	// generates entities
	pub id_token: String,
	pub userinfo_token: String,
	pub access_token: String,

	// extra parameters for cedar decision resolution
	pub action: String,
	pub resource: serde_json::Value,
	pub context: serde_json::Value,
}

#[derive(Debug)]
pub struct EntityUids {
	pub application: Option<EntityUid>,
	pub client: EntityUid,
	pub user: EntityUid,
}

#[derive(Debug)]
pub(super) enum Token<'a> {
	Access(&'a AccessToken),
	UserInfo(&'a UserInfoToken),
	Id(&'a IdToken),
}

fn space_separated_string_list<'de, D>(deserializer: D) -> Result<Vec<String>, D::Error>
where
	D: serde::Deserializer<'de>,
{
	let base = String::deserialize(deserializer)?;
	Ok(base.split(' ').map(str::to_string).collect())
}

#[derive(serde::Deserialize, Debug)]
pub struct AccessToken {
	pub jti: String,
	pub iss: String,

	active: bool,
	aud: String,
	pub client_id: String,

	#[serde(deserialize_with = "space_separated_string_list")]
	scope: Vec<String>,
}

impl AccessToken {
	pub fn get_token_entity(&self) -> Entity {
		// TODO: Implement
		unimplemented!()
	}

	pub fn get_client_entity(&self) -> Entity {
		if !self.active {
			throw_str("Attempted to extract a Client entity from an inactive access token")
		}

		let id = serde_json::json!({ "__entity": { "type": "Client", "id": self.aud } });
		let id = EntityUid::from_json(id).unwrap_throw();

		let parents = HashSet::new();
		let attrs = HashMap::from([
			("client_id".to_string(), RestrictedExpression::new_string(self.client_id.clone())),
			("iss".to_string(), RestrictedExpression::new_string(self.iss.clone())),
		]);

		Entity::new(id, attrs, parents).unwrap_throw()
	}

	pub fn get_application_entity(&self, application_name: Option<&str>, client_uid: EntityUid) -> Option<Entity> {
		application_name.map(|name| {
			if !self.active {
				throw_str("Attempted to extract a Application entity from an inactive access token")
			}

			let id = serde_json::json!({ "__entity": { "type": "Application", "id": self.aud } });
			let id = EntityUid::from_json(id).unwrap_throw();

			let parents = HashSet::new();
			let attrs = HashMap::from([
				("name".to_string(), RestrictedExpression::new_string(name.to_string())),
				("client".to_string(), RestrictedExpression::new_entity_uid(client_uid)),
			]);

			Entity::new(id, attrs, parents).unwrap_throw()
		})
	}
}

#[derive(serde::Deserialize, Debug)]
pub struct IdToken {
	pub jti: String,

	pub iss: String,
	pub aud: String,
	pub sub: String,
}

impl IdToken {
	pub fn get_token_entity(&self) -> Entity {
		// TODO: Implement
		unimplemented!()
	}
}

#[derive(serde::Deserialize, Debug)]
pub struct UserInfoToken {
	pub jti: String,
	pub iss: String,

	pub sub: String,
	pub aud: String,

	pub exp: i64,
	pub iat: i64,

	// TODO: Parse multiple strings from UserInfo token?
	#[serde(rename = "role")]
	pub roles: HashSet<String>,
	pub email: String,
	pub name: Option<String>,
	pub phone_number: Option<String>,
	pub birthdate: Option<String>,
}

impl UserInfoToken {
	pub fn get_role_entities(&self) -> Vec<Entity> {
		self.roles
			.iter()
			.map(|role| {
				let id = serde_json::json!({ "__entity": { "type": "Role", "id": role } });
				let uid = EntityUid::from_json(id).unwrap_throw();
				Entity::new_no_attrs(uid, HashSet::new())
			})
			.collect()
	}

	pub fn get_token_entity(&self, roles: &[Entity]) -> Entity {
		let id = serde_json::json!({ "__entity": { "type": "UserInfo", "id": self.sub } });
		let uid = EntityUid::from_json(id).unwrap_throw();

		// create email dict
		let mut iter = self.email.split('@');
		let record = [
			("id".to_string(), RestrictedExpression::new_string(iter.next().expect_throw("Invalid Email Address").into())),
			("domain".to_string(), RestrictedExpression::new_string(iter.next().expect_throw("Invalid Email Address").into())),
		];

		// acquire TrustedIssuer eid
		let entry = unsafe { TRUST_STORE.get(&self.iss) }.expect_throw("Unable to extract TrustedIssuer from UserInfo iss");
		let entity = entry.issuer.get_entity();

		// construct entity
		let mut attrs = HashMap::from([
			("aud".to_string(), RestrictedExpression::new_string(self.aud.clone())),
			("email".to_string(), RestrictedExpression::new_record(record).unwrap_throw()),
			("exp".to_string(), RestrictedExpression::new_long(self.exp)),
			("iat".to_string(), RestrictedExpression::new_long(self.iat)),
			("sub".to_string(), RestrictedExpression::new_string(self.sub.clone())),
			("iss".to_string(), RestrictedExpression::new_entity_uid(entity.uid())),
		]);

		if let Some(birthdate) = self.birthdate.clone() {
			attrs.insert("birthdate".to_string(), RestrictedExpression::new_string(birthdate));
		}

		if let Some(username) = self.name.clone() {
			attrs.insert("name".to_string(), RestrictedExpression::new_string(username));
		}

		if let Some(number) = self.phone_number.clone() {
			attrs.insert("phone_number".to_string(), RestrictedExpression::new_string(number));
		}

		let parents = HashSet::from_iter(roles.iter().map(|e| e.uid()));
		Entity::new(uid, attrs, parents).expect_throw("Unable to construct UserInfo entity from userinfo_token")
	}

	pub fn get_user_entity(&self, roles: &[Entity]) -> Entity {
		let entry = unsafe { TRUST_STORE.get(&self.iss) }.expect_throw("Can't get iss for User entity creation from userinfo_token");
		let id = serde_json::json!({ "__entity": { "type": entry.issuer.id_tokens.principal_identifier, "id": self.sub } });
		let uid = EntityUid::from_json(id).unwrap_throw();

		// create email dict
		let mut iter = self.email.split('@');
		let record = [
			("id".to_string(), RestrictedExpression::new_string(iter.next().expect_throw("Invalid Email Address").into())),
			("domain".to_string(), RestrictedExpression::new_string(iter.next().expect_throw("Invalid Email Address").into())),
		];

		// construct entity
		let mut attrs = HashMap::from([
			("sub".to_string(), RestrictedExpression::new_string(self.sub.clone())),
			("email".to_string(), RestrictedExpression::new_record(record).unwrap_throw()),
		]);

		if let Some(given_name) = self.name.clone() {
			attrs.insert("username".to_string(), RestrictedExpression::new_string(given_name));
		}

		if let Some(number) = self.phone_number.clone() {
			attrs.insert("phone_number".to_string(), RestrictedExpression::new_string(number));
		}

		let parents = HashSet::from_iter(roles.iter().map(|e| e.uid()));
		Entity::new(uid, attrs, parents).expect_throw("Unable to construct User entity from userinfo_token")
	}
}
