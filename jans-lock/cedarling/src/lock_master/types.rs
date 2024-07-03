#[derive(serde::Deserialize, Debug)]
pub(crate) struct LockMasterConfig {
	pub(crate) audit_uri: String,
	pub(crate) config_uri: String,
	pub(crate) lock_sse_uri: String,
	pub(crate) oauth_as_well_known: String,
}

#[derive(serde::Serialize, Debug)]
pub(crate) struct OAuthDynamicClientRequest<'a> {
	pub(crate) client_name: &'a str,
	pub(crate) grant_types: &'a [&'a str],
	pub(crate) application_type: &'static str,
	pub(crate) redirect_uris: &'a [&'a str],
	pub(crate) token_endpoint_auth_method: &'static str,
	pub(crate) software_statement: &'a str,
	pub(crate) contacts: &'a [&'static str],
}

#[derive(serde::Deserialize, Debug)]
pub struct OAuthConfig {
	pub(crate) issuer: String,
	pub(crate) authorization_endpoint: String,
	pub(crate) registration_endpoint: String,
	pub(crate) token_endpoint: String,
	pub(crate) jwks_uri: String,
}

#[derive(serde::Deserialize, Debug)]
pub(crate) struct OAuthDynamicClient {
	pub(crate) client_id: String,
	pub(crate) client_secret: Option<String>,
}

#[derive(serde::Serialize, Debug)]
pub(crate) struct OAuthGrantRequest<'a> {
	pub(crate) scope: &'a [&'a str],
	pub(crate) grant_type: &'a str,
}

#[derive(serde::Deserialize, Debug)]
pub(crate) struct OAuthGrantResponse {
	pub(crate) access_token: String,
	pub(crate) id_token: String,
	pub(crate) refresh_token: Option<String>,
	pub(crate) token_type: String,
}
