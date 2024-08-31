use std::collections::BTreeMap;
use std::str::FromStr;

use cedar_policy::{Entity, EntityId, EntityTypeName, EntityUid, ParseErrors};

use super::jwt_tokens::{AccessToken, EntityCreatingError, IdToken, UserInfoToken, UserMissedInfo};

#[derive(serde::Deserialize, Debug)]
pub struct AuthzInputRaw {
	// generates entities
	pub id_token: String,
	pub userinfo_token: String,
	pub access_token: String,

	#[serde(flatten)]
	pub extra: CedarParams,
}

impl AuthzInputRaw {
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

#[derive(serde::Deserialize, Debug)]
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

#[derive(thiserror::Error, Debug)]
pub enum DecodeTokensError {
	#[error("could not decode id_token: {0}")]
	IdToken(jwt::DecodeError),
	#[error("could not decode userinfo_token: {0}")]
	UserInfoToken(jwt::DecodeError),
	#[error("could not decode access_token: {0}")]
	AccessToken(jwt::DecodeError),
}

impl AuthzInputRaw {
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
	pub fn entities(
		self,
		application_name: Option<&str>,
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

		let id_token_entities = self
			.id_token
			.entities()
			.map_err(AuthzInputEntitiesError::IdTokenEntity)?;

		let user_info_entities = self
			.userinfo_token
			.entities(UserMissedInfo {
				roles: &[], //TODO: add roles after adding to jwts
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
		.into_iter()
		.map(|(_k, v)| v)
		.collect()
}
