#[derive(serde::Serialize, Debug)]
pub(crate) struct OpenIdDynamicClientRequest<'a> {
	pub(crate) client_name: &'static str,
	pub(crate) application_type: &'static str,
	pub(crate) redirect_uris: &'a [&'static str], // basically a list of callback urls
	pub(crate) token_endpoint_auth_method: &'static str,
	pub(crate) software_statement: &'a str,
	pub(crate) contacts: &'a [&'static str],
}

#[derive(serde::Deserialize, Debug)]
pub(crate) struct TrustedIssuer {
	pub(crate) name: String,
	pub(crate) description: String,
	pub(crate) openid_configuration_endpoint: String,
}

#[derive(serde::Deserialize, Debug)]
pub struct OpenIdConfiguration {
	pub(crate) issuer: String,
	pub(crate) authorization_endpoint: String,
	pub(crate) registration_endpoint: String,
	pub(crate) token_endpoint: String,
	pub(crate) jwks_uri: String,
}

#[derive(serde::Deserialize, Debug)]
pub(crate) struct OpenIdDynamicClient {
	pub(crate) client_id: String,
	pub(crate) client_secret: Option<String>,
}

#[derive(serde::Serialize, Debug)]
pub(crate) struct OpenIdGrantRequest<'a> {
	pub(crate) scope: &'a str,
	pub(crate) grant_type: &'a str,
}

#[derive(serde::Deserialize, Debug)]
pub(crate) struct OpenIdGrantResponse {
	pub(crate) access_token: String,
}
