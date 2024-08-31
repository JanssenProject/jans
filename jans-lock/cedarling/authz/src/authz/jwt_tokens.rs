use std::collections::{HashMap, HashSet};

use cedar_policy::Entity;
use cedar_policy::EntityAttrEvaluationError;
use cedar_policy::EntityUid;
use cedar_policy::RestrictedExpression;

use super::exp_parsers;

#[derive(thiserror::Error, Debug)]
pub enum EntityCreatingError {
	#[error("could not create entity uid from json: {0}")]
	CreateFromJson(String),
	#[error("create expression with email: {0}")]
	Email(#[from] exp_parsers::ParseEmailToExpError),
	#[error("could not create new entity: {0}")]
	NewEntity(#[from] EntityAttrEvaluationError),

	#[error("could not create new entity of trusted issuer: {0}")]
	TrustedIssuer(#[from] exp_parsers::TrustedIssuerEntityError),
}

#[derive(Default, Debug, Clone, PartialEq, serde::Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct IdToken {
	pub acr: String,
	pub amr: Vec<String>,
	pub aud: String,
	pub birthdate: String,
	pub email: String,
	pub exp: i64,
	pub iat: i64,
	pub iss: String,
	pub jti: String,
	pub name: String,
	#[serde(rename = "phone_number")]
	pub phone_number: String,
	pub sub: String,
	// next fields is unused for now
	// #[serde(rename = "at_hash")]
	// pub at_hash: String,
	// pub country: String,

	// #[serde(rename = "user_name")]
	// pub user_name: String,

	// pub inum: String,
	// pub sid: String,
	// #[serde(rename = "jansOpenIDConnectVersion")]
	// pub jans_open_idconnect_version: String,

	// #[serde(rename = "updated_at")]
	// pub updated_at: i64,
	// #[serde(rename = "auth_time")]
	// pub auth_time: i64,
	// pub nickname: String,

	// #[serde(rename = "given_name")]
	// pub given_name: String,
	// #[serde(rename = "middle_name")]
	// pub middle_name: String,
	// pub nonce: String,

	// #[serde(rename = "c_hash")]
	// pub c_hash: String,
	// #[serde(rename = "user_permission")]
	// pub user_permission: Vec<String>,

	// pub grant: String,
	// #[serde(rename = "family_name")]
	// pub family_name: String,
	// pub status: Status,
	// #[serde(rename = "jansAdminUIRole")]
	// pub jans_admin_uirole: Vec<String>,
}

#[derive(Default, Debug, Clone, PartialEq, serde::Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Status {
	#[serde(rename = "status_list")]
	pub status_list: StatusList,
}

#[derive(Default, Debug, Clone, PartialEq, serde::Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct StatusList {
	pub idx: i64,
	pub uri: String,
}

impl IdToken {
	pub fn entities(self) -> Result<Vec<Entity>, EntityCreatingError> {
		let id = serde_json::json!({ "__entity": { "type": "Jans::id_token", "id": self.jti } });
		let uid = EntityUid::from_json(id)
			.map_err(|err| EntityCreatingError::CreateFromJson(err.to_string()))?;

		let amr = self
			.amr
			.iter()
			.map(|v| RestrictedExpression::new_string(v.to_owned()));

		// TODO: add
		//         iss: TrustedIssuer,

		let mut attrs = HashMap::from([
			("acr".into(), RestrictedExpression::new_string(self.acr)),
			("amr".into(), RestrictedExpression::new_set(amr)),
			("aud".into(), RestrictedExpression::new_string(self.aud)),
			(
				"birthdate".into(),
				RestrictedExpression::new_string(self.birthdate),
			),
			("email".into(), exp_parsers::email_exp(&self.email)?),
			("exp".into(), RestrictedExpression::new_long(self.exp)),
			("iat".into(), RestrictedExpression::new_long(self.iat)),
			("jti".into(), RestrictedExpression::new_string(self.jti)),
			("name".into(), RestrictedExpression::new_string(self.name)),
			(
				"phone_number".into(),
				RestrictedExpression::new_string(self.phone_number),
			),
			("sub".into(), RestrictedExpression::new_string(self.sub)),
		]);

		let trusted_issuer_entity = exp_parsers::trusted_issuer_entity(&self.iss)?;

		attrs.insert(
			"iss".into(),
			RestrictedExpression::new_entity_uid(trusted_issuer_entity.uid()),
		);

		let id_token_entity = Entity::new(uid, attrs, HashSet::with_capacity(0))?;
		let result = vec![id_token_entity];
		Ok(result)
	}
}

#[derive(serde::Deserialize, Debug)]
pub struct UserInfoToken {
	pub aud: String,
	pub birthdate: String,
	pub email: String,
	pub iss: String,
	pub jti: String,
	pub name: String,
	#[serde(rename = "phone_number")]
	pub phone_number: String,
	pub sub: String,
	// id of user
	pub inum: String,
	// next fields is unused
	// pub country: String,
	// #[serde(rename = "user_name")]
	// pub user_name: String,
	// #[serde(rename = "given_name")]
	// pub given_name: String,
	// #[serde(rename = "middle_name")]
	// pub middle_name: String,

	// #[serde(rename = "client_id")]
	// pub client_id: String,
	// #[serde(rename = "updated_at")]
	// pub updated_at: i64,
	// pub nickname: String,
	// #[serde(rename = "user_permission")]
	// pub user_permission: Vec<String>,
	// #[serde(rename = "family_name")]
	// pub family_name: String,
	// #[serde(rename = "jansAdminUIRole")]
	// pub jans_admin_uirole: Vec<String>,
}

// Restricted expressions can contain only the following:
//   - bool, int, and string literals
//   - literal `EntityUid`s such as `User::"alice"`
//   - extension function calls, where the arguments must be other things
//       on this list
//   - set and record literals, where the values must be other things on
//       this list
#[allow(dead_code)]
//it can be usefull to dynamically fill the entities data from token
fn json_to_expression(value: serde_json::Value) -> Option<RestrictedExpression> {
	match value {
		serde_json::Value::Null => None,
		serde_json::Value::Bool(v) => Some(RestrictedExpression::new_bool(v)),
		serde_json::Value::Number(v) => {
			if let Option::Some(i) = v.as_i64() {
				Some(RestrictedExpression::new_long(i))
			} else if let Option::Some(f) = v.as_f64() {
				Some(RestrictedExpression::new_decimal(f.to_string()))
			} else {
				None
			}
		}
		serde_json::Value::String(v) => Some(RestrictedExpression::new_string(v)),
		serde_json::Value::Array(v) => Some(RestrictedExpression::new_set(
			v.into_iter()
				.filter_map(|v| json_to_expression(v))
				.collect::<Vec<RestrictedExpression>>(),
		)),
		serde_json::Value::Object(_) => None,
	}
}

pub(crate) struct UserInfoTokenEntityBox {
	pub entities: Vec<Entity>,
	pub user_entry_uid: EntityUid,
}

pub(crate) struct UserMissedInfo<'a> {
	pub username: String,
	pub roles: &'a [String],
}

impl UserInfoToken {
	pub(crate) fn entities(
		&self,
		user_info: UserMissedInfo,
	) -> Result<UserInfoTokenEntityBox, EntityCreatingError> {
		let mut entry_box = self.get_user_entities(user_info)?;
		entry_box
			.entities
			.extend(self.get_user_info_tokens_entity()?);
		Ok(entry_box)
	}

	fn get_user_info_tokens_entity(&self) -> Result<Vec<Entity>, EntityCreatingError> {
		let id = serde_json::json!({ "__entity": { "type": "Jans::Userinfo_token", "id": self.jti.to_owned() } });
		let uid = EntityUid::from_json(id)
			.map_err(|err| EntityCreatingError::CreateFromJson(err.to_string()))?;

		let trusted_issuer_entity = exp_parsers::trusted_issuer_entity(&self.iss)?;

		let parents = HashSet::new();
		let attrs = HashMap::from([
			(
				"aud".to_owned(),
				RestrictedExpression::new_string(self.aud.clone()),
			),
			(
				"birthdate".to_string(),
				RestrictedExpression::new_string(self.birthdate.clone()),
			),
			("email".to_string(), exp_parsers::email_exp(&self.email)?),
			(
				"iss".to_string(),
				RestrictedExpression::new_entity_uid(trusted_issuer_entity.uid()),
			),
			(
				"jti".to_string(),
				RestrictedExpression::new_string(self.jti.clone()),
			),
			(
				"name".to_string(),
				RestrictedExpression::new_string(self.name.clone()),
			),
			(
				"phone_number".to_string(),
				RestrictedExpression::new_string(self.phone_number.clone()),
			),
			(
				"sub".to_string(),
				RestrictedExpression::new_string(self.sub.clone()),
			),
		]);

		let token_entity = Entity::new(uid, attrs, parents)?;

		Ok(vec![token_entity])
	}

	fn get_user_entities(
		&self,
		user_info: UserMissedInfo,
	) -> Result<UserInfoTokenEntityBox, EntityCreatingError> {
		let id = serde_json::json!({ "__entity": { "type": "Jans::User", "id": self.sub } });
		let uid = EntityUid::from_json(id)
			.map_err(|err| EntityCreatingError::CreateFromJson(err.to_string()))?;

		let attrs = HashMap::from([
			(
				"sub".to_string(),
				RestrictedExpression::new_string(self.sub.clone()),
			),
			(
				"username".to_string(),
				RestrictedExpression::new_string(user_info.username),
			),
			("email".to_string(), exp_parsers::email_exp(&self.email)?),
			(
				"phone_number".to_string(),
				RestrictedExpression::new_string(self.phone_number.clone()),
			),
			(
				"role".to_string(),
				RestrictedExpression::new_set(
					user_info
						.roles
						.iter()
						.map(|r| RestrictedExpression::new_string(r.to_owned())),
				),
			),
		]);

		let roles_entities = exp_parsers::roles_entities(user_info.roles);

		let parents = HashSet::from_iter(roles_entities.iter().map(|e| e.uid()));
		let user_entity = Entity::new(uid, attrs, parents)?;
		let user_entry_uid = user_entity.uid();

		let mut entities = roles_entities;
		entities.push(user_entity);
		Ok(UserInfoTokenEntityBox {
			entities: entities,
			user_entry_uid,
		})
	}
}

#[derive(serde::Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
pub struct AccessToken {
	pub aud: String,
	pub exp: i64,
	pub iat: i64,
	pub iss: String,
	pub jti: String,
	pub scope: Vec<String>,
	#[serde(rename = "client_id")]
	pub client_id: String,
	pub username: String,
	// next fields don't used
	// pub sub: String,
	// pub code: String,
	// #[serde(rename = "token_type")]
	// pub token_type: String,
	// pub acr: String,
	// #[serde(rename = "x5t#S256")]
	// pub x5t_s256: String,
	// #[serde(rename = "auth_time")]
	// pub auth_time: i64,
	// pub status: Status,
}

pub struct AccessTokenEntityBox {
	pub entities: Vec<Entity>,
	pub client_entry_uid: EntityUid,
}

impl AccessToken {
	pub(crate) fn entities(
		&self,
		application_name: Option<&str>,
	) -> Result<Vec<Entity>, EntityCreatingError> {
		let mut box_entries = self.get_client_entity()?;
		box_entries
			.entities
			.extend(self.get_access_token_entities()?);

		if let Option::Some(name) = application_name {
			box_entries
				.entities
				.push(self.get_application_entity(name, box_entries.client_entry_uid)?);
		}

		Ok(box_entries.entities)
	}

	fn get_client_entity(&self) -> Result<AccessTokenEntityBox, EntityCreatingError> {
		let id = serde_json::json!({ "__entity": { "type": "Jans::Client", "id": self.aud } });
		let id = EntityUid::from_json(id)
			.map_err(|err| EntityCreatingError::CreateFromJson(err.to_string()))?;

		let trusted_issuer_entity = exp_parsers::trusted_issuer_entity(&self.iss)?;

		let parents = HashSet::new();
		let attrs = HashMap::from([
			(
				"client_id".to_string(),
				RestrictedExpression::new_string(self.client_id.clone()),
			),
			(
				"iss".to_string(),
				RestrictedExpression::new_entity_uid(trusted_issuer_entity.uid()),
			),
		]);

		let client_entity = Entity::new(id, attrs, parents)?;
		Ok(AccessTokenEntityBox {
			client_entry_uid: client_entity.uid(),
			entities: vec![client_entity],
		})
	}

	fn get_application_entity(
		&self,
		application_name: &str,
		client_uid: EntityUid,
	) -> Result<Entity, EntityCreatingError> {
		let id = serde_json::json!({ "__entity": { "type": "Jans::Application", "id": self.aud } });
		let id = EntityUid::from_json(id)
			.map_err(|err| EntityCreatingError::CreateFromJson(err.to_string()))?;

		let parents = HashSet::new();
		let attrs = HashMap::from([
			(
				"name".to_owned(),
				RestrictedExpression::new_string(application_name.to_string()),
			),
			(
				"client".to_owned(),
				RestrictedExpression::new_entity_uid(client_uid),
			),
		]);

		Ok(Entity::new(id, attrs, parents)?)
	}

	fn get_access_token_entities(&self) -> Result<Vec<Entity>, EntityCreatingError> {
		let id =
			serde_json::json!({ "__entity": { "type": "Jans::Access_token", "id": self.aud } });
		let id = EntityUid::from_json(id)
			.map_err(|err| EntityCreatingError::CreateFromJson(err.to_string()))?;

		let trusted_issuer_entity = exp_parsers::trusted_issuer_entity(&self.iss)?;

		let parents = HashSet::new();
		let attrs = HashMap::from([
			(
				"aud".to_owned(),
				RestrictedExpression::new_string(self.aud.to_owned()),
			),
			("exp".to_owned(), RestrictedExpression::new_long(self.exp)),
			("iat".to_owned(), RestrictedExpression::new_long(self.iat)),
			(
				"iss".to_owned(),
				RestrictedExpression::new_entity_uid(trusted_issuer_entity.uid()),
			),
			(
				"jti".to_owned(),
				RestrictedExpression::new_string(self.jti.to_owned()),
			),
			("iat".to_owned(), RestrictedExpression::new_long(self.iat)),
			(
				"scope".to_owned(),
				RestrictedExpression::new_set(
					self.scope
						.iter()
						.map(|s| RestrictedExpression::new_string(s.to_owned())),
				),
			),
		]);

		let access_token_entity = Entity::new(id, attrs, parents)?;
		Ok(vec![access_token_entity])
	}
}
