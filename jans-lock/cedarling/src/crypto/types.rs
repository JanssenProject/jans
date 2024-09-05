use std::{
	collections::{HashMap, HashSet},
	str::FromStr,
};

use cedar_policy::*;
use wasm_bindgen::UnwrapThrowExt;

#[derive(Debug, Clone, Copy)]
#[allow(clippy::enum_variant_names)]
pub enum TokenType {
	IdToken,
	AccessToken,
	UserInfoToken,
}

#[derive(Debug)]
pub struct TrustStoreEntry {
	pub jwks: jsonwebtoken::jwk::JwkSet,
	pub issuer: &'static TrustedIssuer,
}

#[derive(serde::Deserialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct TrustedIssuer {
	pub name: Option<String>,
	pub openid_configuration_endpoint: String,

	pub access_tokens: AccessTokenConfig,
	pub id_tokens: IdTokenConfig,
	pub userinfo_tokens: UserInfoTokenConfig,
}

impl TrustedIssuer {
	pub fn get_entity(&self) -> Entity {
		let id = serde_json::json!({ "__entity": { "type": "TrustedIssuer", "id": self.name } });
		let id = EntityUid::from_json(id).unwrap_throw();

		// parse openid_configuration_endpoint
		let url = web_sys::Url::new(&self.openid_configuration_endpoint).unwrap_throw();
		let record = [
			("protocol".to_string(), RestrictedExpression::from_str(&url.protocol()).unwrap_throw()),
			("host".to_string(), RestrictedExpression::from_str(&url.host()).unwrap_throw()),
			("path".to_string(), RestrictedExpression::from_str(&url.pathname()).unwrap_throw()),
		];

		// construct entity
		let attrs = HashMap::from([("issuer_entity_id".to_string(), RestrictedExpression::new_record(record).unwrap_throw())]);
		let parents = HashSet::new();

		Entity::new(id, attrs, parents).unwrap_throw()
	}
}

#[derive(serde::Deserialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct AccessTokenConfig {
	pub trusted: bool,
}

#[derive(serde::Deserialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct IdTokenConfig {
	pub trusted: bool,
	pub principal_identifier: Option<String>,
}

#[derive(serde::Deserialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct UserInfoTokenConfig {
	pub trusted: bool,
	pub role_mapping: Option<String>,
}
