#[derive(serde::Deserialize, Debug)]
pub struct LockMasterConfig {
	// TODO: Discuss, implement audit features
	#[allow(unused)]
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
	// Might not support DCR
	pub registration_endpoint: Option<String>,
	pub token_endpoint: String,
	pub jwks_uri: String,
}

#[derive(serde::Deserialize, Debug)]
pub struct OAuthDynamicClient {
	pub client_id: String,
	pub client_secret: Option<String>,
}

fn space_separated<'a, S>(scopes: &'a [&'a str], s: S) -> Result<S::Ok, S::Error>
where
	S: serde::Serializer,
{
	let mut iter = scopes.iter();

	// is scopes is empty, serialize ""
	match iter.next() {
		Some(first) => {
			let res = iter.fold(first.to_string(), |mut acc, ntx| {
				acc.push(' ');
				acc.push_str(ntx);
				acc
			});

			s.serialize_str(&res)
		}
		None => s.serialize_str(""),
	}
}

#[derive(serde::Serialize, Debug)]
pub struct OAuthGrantRequest<'a> {
	#[serde(serialize_with = "space_separated")]
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
