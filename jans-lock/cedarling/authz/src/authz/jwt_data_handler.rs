use std::collections::{BTreeMap, HashMap};
use std::str::FromStr;

use cedar_policy::{Entity, EntityId, EntityTypeName, EntityUid, ParseErrors};

use super::jwt_tokens::{AccessToken, EntityCreatingError, IdToken, UserInfoToken, UserMissedInfo};
use init_engine::TokenMapper;

#[derive(serde::Deserialize, Debug)]
pub struct AuthzRequest {
	// generates entities
	pub id_token: String,
	pub userinfo_token: String,
	pub access_token: String,

	#[serde(flatten)]
	pub extra: CedarParams,
}

impl AuthzRequest {
	pub fn parse_raw(data: &str) -> Result<Self, serde_json::error::Error> {
		let input = serde_json::from_str(data)?;
		Ok(input)
	}
}

#[derive(serde::Deserialize, Debug)]
pub struct CedarParams {
	// extra parameters for cedar decision resolution
	pub action: String,
	pub resource: ResourceData,
	pub context: serde_json::Value,
}

#[derive(serde::Deserialize, Debug, Clone)]
pub struct ResourceData {
	#[serde(rename = "type")]
	pub _type: String,
	pub id: String,
}

impl ResourceData {
	pub(crate) fn entity_uid(&self) -> Result<EntityUid, ParseErrors> {
		Ok(EntityUid::from_type_name_and_id(
			EntityTypeName::from_str(&self._type)?,
			EntityId::new(&self.id),
		))
	}
}

#[allow(clippy::enum_variant_names)]
#[derive(thiserror::Error, Debug)]
pub enum DecodeTokensError {
	#[error("could not decode id_token: {0}")]
	IdToken(jwt::DecodeError),
	#[error("could not decode userinfo_token: {0}")]
	UserInfoToken(jwt::DecodeError),
	#[error("could not decode access_token: {0}")]
	AccessToken(jwt::DecodeError),
}

impl AuthzRequest {
	pub fn decode_tokens(self, decoder: &jwt::JWTDecoder) -> Result<AuthzInput, DecodeTokensError> {
		let id_token: IdToken = decoder
			.decode(&self.id_token)
			.map_err(DecodeTokensError::IdToken)?;

		let userinfo_token: UserInfoToken = decoder
			.decode(&self.userinfo_token)
			.map_err(DecodeTokensError::UserInfoToken)?;

		let access_token: AccessToken = decoder
			.decode(&self.access_token)
			.map_err(DecodeTokensError::AccessToken)?;

		Ok(AuthzInput {
			jwt: JWTData {
				id_token,
				userinfo_token,
				access_token,
			},
			chedar_params: self.extra,
		})
	}
}

#[derive(Debug)]
pub struct JWTData {
	pub id_token: IdToken,
	pub userinfo_token: UserInfoToken,
	pub access_token: AccessToken,
}

#[derive(Debug)]
pub struct AuthzInput {
	// jwt tokens
	pub jwt: JWTData,

	pub chedar_params: CedarParams,
}

#[allow(clippy::enum_variant_names)]
#[derive(thiserror::Error, Debug)]
pub enum AuthzInputEntitiesError {
	#[error("could not get id token entity from id_token: {0}")]
	IdTokenEntity(EntityCreatingError),

	#[error("could not get user entity from userinfo_token: {0}")]
	UserEntity(EntityCreatingError),
	#[error("could not get user token entity from userinfo_token: {0}")]
	UserTokenEntity(EntityCreatingError),

	#[error("could not get access token entity from access_token: {0}")]
	AccessTokenEntity(EntityCreatingError),
	#[error("could not get application entity from access_token: {0}")]
	ApplicationEntity(EntityCreatingError),
}

pub struct JWTDataEntities {
	pub entities: Vec<Entity>,
	pub user_entity_uid: EntityUid,
}

impl JWTData {
	fn roles(&self, token_mapper: &TokenMapper) -> Vec<String> {
		// if serde_json::Value is string  return Vec with string
		// if serde_json::Value is array of string return Vec with strings
		fn parse_roles(
			key_val: &Option<String>,
			dict: &HashMap<String, serde_json::Value>,
		) -> Vec<String> {
			key_val
				.as_deref()
				.and_then(|key| dict.get(key))
				.and_then(|value| match value {
					serde_json::Value::String(role) => Some(vec![role.clone()]),
					serde_json::Value::Array(array) => Some(
						array
							.iter()
							.filter_map(|value| {
								if let serde_json::Value::String(role) = value {
									Some(role.clone())
								} else {
									log::warn!("could not parse role value in json array");
									None
								}
							})
							.collect(),
					),
					serde_json::Value::Null => None,
					_ => {
						log::warn!(
							"could not parse role, it should be an array of strings or a string"
						);
						None
					}
				})
				.unwrap_or_default()
		}

		let mut result = Vec::new();
		result.extend(parse_roles(&token_mapper.id_token, &self.id_token.extra));

		result.extend(parse_roles(
			&token_mapper.userinfo_token,
			&self.userinfo_token.extra,
		));

		result.extend(parse_roles(
			&token_mapper.access_token,
			&self.access_token.extra,
		));

		result
	}

	pub fn entities(
		self,
		application_name: Option<&str>,
		token_mapping: &TokenMapper,
	) -> Result<JWTDataEntities, AuthzInputEntitiesError> {
		// TODO: implement check of token correctness
		// // check if `aud` claim in id_token matches `client_id` in access token
		// if id_token.aud != access_token.client_id && super::REQUIRE_AUD_VALIDATION.get().cloned().unwrap_or(false) {
		// 	throw_str("id_token was not issued for this client: (id_token.aud != access_token.client_id)")
		// }

		// // check if both tokens were issued by the same issuer
		// if id_token.iss != access_token.iss {
		// 	throw_str("access_token and id_token weren't issued by the same issuer: (access_token.iss != id_token.iss)")
		// }
		// if userinfo.sub != id_token.sub || userinfo.iss != id_token.iss {
		// 	throw_str("userinfo token invalid: either sub or iss doesn't match id_token")
		// }

		let roles = self.roles(token_mapping);

		let id_token_entities = self
			.id_token
			.entities()
			.map_err(AuthzInputEntitiesError::IdTokenEntity)?;

		let user_info_entities = self
			.userinfo_token
			.entities(UserMissedInfo {
				roles: &roles,
				// according to doc
				// User: Created based on the joined id_token and userinfo token. sub is the entity identifier
				// but username only has in access_token
				username: self.access_token.username.clone(),
			})
			.map_err(AuthzInputEntitiesError::UserTokenEntity)?;

		let access_token_entities = self
			.access_token
			.entities(application_name)
			.map_err(AuthzInputEntitiesError::AccessTokenEntity)?;

		let mut list = id_token_entities;
		list.extend(user_info_entities.entities);
		list.extend(access_token_entities);

		Ok(JWTDataEntities {
			entities: deduplicate_entities(list),
			user_entity_uid: user_info_entities.user_entry_uid,
		})
	}
}

fn deduplicate_entities(list: Vec<Entity>) -> Vec<Entity> {
	// use Btree to not implement hash
	BTreeMap::from_iter(list.into_iter().map(|e| (e.uid(), e)))
		.into_values()
		.collect()
}
