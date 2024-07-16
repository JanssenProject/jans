#![allow(unused)]

#[derive(serde::Deserialize, Debug)]
pub struct LockMasterConfig {
	pub audit_uri: String,
	pub config_uri: String,
	pub lock_sse_uri: String,
	pub oauth_as_well_known: String,
}

#[derive(serde::Serialize, Debug)]
pub struct OAuthDynamicClientRequest<'a> {
	pub client_name: &'a str,
	pub grant_types: &'a [&'a str],
	pub application_type: &'a str,
	pub redirect_uris: &'a [&'a str],
	pub token_endpoint_auth_method: &'a str,
	pub software_statement: &'a str,
	pub contacts: &'a [&'a str],
}

#[derive(serde::Deserialize, Debug)]
pub struct OAuthConfig {
	pub issuer: String,
	pub authorization_endpoint: String,
	pub registration_endpoint: String,
	pub token_endpoint: String,
	pub jwks_uri: String,
}

#[derive(serde::Deserialize, Debug)]
pub struct OAuthDynamicClient {
	pub client_id: String,
	pub client_secret: Option<String>,
}

#[derive(serde::Serialize, Debug)]
pub struct OAuthGrantRequest<'a> {
	pub scope: &'a [&'a str],
	pub grant_type: &'a str,
}

#[derive(serde::Deserialize, Debug)]
pub struct OAuthGrantResponse {
	pub access_token: String,
}

#[derive(serde::Deserialize, Debug)]
#[non_exhaustive]
pub enum SseUpdate {
	StatusListUpdate { bits: u8, status_list: String },
}
