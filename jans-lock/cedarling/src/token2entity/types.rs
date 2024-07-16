#![allow(unused)]

use std::collections::{HashMap, HashSet};

use cedar_policy::*;
use serde::Deserialize;
use wasm_bindgen::{throw_str, UnwrapThrowExt};

fn space_separated_string_list<'de, D>(deserializer: D) -> Result<Vec<String>, D::Error>
where
	D: serde::Deserializer<'de>,
{
	let base = String::deserialize(deserializer)?;
	Ok(base.split(" ").map(str::to_string).collect())
}

#[derive(Debug)]
pub(super) enum Token<'a> {
	Access(&'a AccessToken),
	UserInfo(&'a UserInfoToken),
	Id(&'a IdToken),
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
	pub nonce: String,
}

#[derive(serde::Deserialize, Debug)]
pub struct UserInfoToken {
	pub jti: String,
	pub iss: String,

	pub sub: String,
	pub email: String,
	pub role: String,

	pub phone_number: Option<String>,
	pub given_name: Option<String>,
}

impl UserInfoToken {
	pub fn get_role_entity(&self) -> Entity {
		let id = serde_json::json!({ "__entity": { "type": "Role", "id": self.role } });
		let uid = EntityUid::from_json(id).unwrap_throw();

		Entity::new_no_attrs(uid, HashSet::new())
	}

	pub fn get_user_entity(&self, roles: &HashMap<&str, Entity>) -> Entity {
		let id = serde_json::json!({ "__entity": { "type": "User", "id": self.sub } });
		let uid = EntityUid::from_json(id).unwrap_throw();

		// create email dict
		let mut iter = self.email.split("@");
		let record = [
			("id".to_string(), RestrictedExpression::new_string(iter.next().expect_throw("Invalid Email Address").into())),
			("domain".to_string(), RestrictedExpression::new_string(iter.next().expect_throw("Invalid Email Address").into())),
		];

		// construct entity
		let mut attrs = HashMap::from([
			("sub".to_string(), RestrictedExpression::new_string(self.sub.clone())),
			("email".to_string(), RestrictedExpression::new_record(record).unwrap_throw()),
		]);

		if let Some(given_name) = self.given_name.clone() {
			attrs.insert("username".to_string(), RestrictedExpression::new_string(given_name));
		}

		if let Some(number) = self.phone_number.clone() {
			attrs.insert("phone_number".to_string(), RestrictedExpression::new_string(number));
		}

		match roles.get(self.role.as_str()) {
			Some(e) => {
				let parents = HashSet::from_iter(std::iter::once(e.uid()));
				Entity::new(uid, attrs, parents)
			}
			None => Entity::new(uid, attrs, HashSet::with_capacity(0)),
		}
		.expect_throw("Unable to construct User entity from userinfo_token")
	}
}
