use std::collections::{BTreeSet, HashMap, HashSet};

use cedar_policy::*;
use wasm_bindgen::{throw_str, UnwrapThrowExt};

use crate::crypto::TRUST_STORE;

#[derive(serde::Deserialize, Debug)]
pub struct AuthzInput {
	pub statement: Option<String>,

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

#[derive(serde::Deserialize, Debug)]
pub struct AccessToken {
	pub jti: String,
	pub iss: String,
	active: bool,

	aud: String,
	scope: Option<String>,
	pub client_id: String,

	exp: i64,
	iat: i64,
	nbf: i64,
}

impl AccessToken {
	pub fn get_token_entity(self) -> Entity {
		if !self.active {
			throw_str("Attempted to extract an AccessToken entity from an inactive access token")
		}

		let id = serde_json::json!({ "__entity": { "type": "AccessToken", "id": self.jti } });
		let id = EntityUid::from_json(id).unwrap_throw();

		let entry = unsafe { TRUST_STORE.get(&self.iss) }.expect_throw("Unable to extract TrustedIssuer from UserInfo iss");
		let issuer = entry.issuer.get_entity();

		let mut attrs = HashMap::from([
			("aud".into(), RestrictedExpression::new_string(self.aud)),
			("iss".into(), RestrictedExpression::new_entity_uid(issuer.uid())),
			("jti".into(), RestrictedExpression::new_string(self.jti)),
			("active".into(), RestrictedExpression::new_bool(self.active)),
			("exp".into(), RestrictedExpression::new_long(self.exp)),
			("iat".into(), RestrictedExpression::new_long(self.iat)),
			("nbf".into(), RestrictedExpression::new_long(self.nbf)),
		]);

		if let Some(scope) = self.scope {
			attrs.insert("scope".into(), RestrictedExpression::new_string(scope));
		}

		Entity::new(id, attrs, Default::default()).unwrap_throw()
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

	pub iat: i64,
	pub exp: i64,

	pub acr: Option<String>,
	pub azp: Option<String>,
	#[serde(default)]
	pub amr: BTreeSet<String>,
}

impl IdToken {
	pub fn get_token_entity(self) -> Entity {
		let id = serde_json::json!({ "__entity": { "type": "IdToken", "id": self.jti } });
		let uid = EntityUid::from_json(id).unwrap_throw();

		let entry = unsafe { TRUST_STORE.get(&self.iss) }.expect_throw("Unable to extract TrustedIssuer from UserInfo iss");
		let issuer = entry.issuer.get_entity();

		let amr = self.amr.into_iter().map(RestrictedExpression::new_string);

		let mut attrs = HashMap::from([
			("jti".into(), RestrictedExpression::new_string(self.jti)),
			("iss".into(), RestrictedExpression::new_entity_uid(issuer.uid())),
			("aud".into(), RestrictedExpression::new_string(self.aud)),
			("sub".into(), RestrictedExpression::new_string(self.sub)),
			("iat".into(), RestrictedExpression::new_long(self.iat)),
			("exp".into(), RestrictedExpression::new_long(self.exp)),
			("amr".into(), RestrictedExpression::new_set(amr)),
		]);

		// optional member
		if let Some(azp) = self.azp {
			let _ = attrs.insert("azp".into(), RestrictedExpression::new_string(azp));
		}

		if let Some(acr) = self.acr {
			let _ = attrs.insert("acr".into(), RestrictedExpression::new_string(acr));
		}

		Entity::new(uid, attrs, HashSet::with_capacity(0)).unwrap_throw()
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
		let entry = unsafe { TRUST_STORE.get(&self.iss) }.expect_throw("Can't get iss for User entity creation from userinfo_token");
		let role_mapping = entry.issuer.userinfo_tokens.role_mapping.as_deref().unwrap_or("Role");

		self.roles
			.iter()
			.map(|role| {
				let id = serde_json::json!({ "__entity": { "type": role_mapping, "id": role } });
				let uid = EntityUid::from_json(id).unwrap_throw();
				Entity::new_no_attrs(uid, HashSet::new())
			})
			.collect()
	}

	pub fn get_token_entity(self, roles: &[Entity]) -> Entity {
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
		let identifier = entry.issuer.id_tokens.principal_identifier.as_deref().unwrap_or("User");
		let id = serde_json::json!({ "__entity": { "type": identifier, "id": self.sub } });
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
