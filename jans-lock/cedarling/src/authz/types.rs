use serde::Deserialize;

fn space_separated_string_list<'de, D>(deserializer: D) -> Result<Vec<String>, D::Error>
where
	D: serde::Deserializer<'de>,
{
	let base = String::deserialize(deserializer)?;
	Ok(base.split(" ").map(str::to_string).collect())
}

#[derive(serde::Deserialize, Debug)]
pub struct AuthzInput {
	// generates entities
	#[serde(rename = "id_token")]
	pub(crate) id: String,
	#[serde(rename = "userinfo_token")]
	pub(crate) user_info: String,
	#[serde(rename = "access_token")]
	pub(crate) access: String,

	#[serde(rename = "tx_token")]
	pub(crate) tx: Option<String>,
}

#[derive(serde::Deserialize, Debug)]
pub struct AccessToken {
	active: bool,
	#[serde(deserialize_with = "space_separated_string_list")]
	scope: Vec<String>,
	client_id: String,
}

#[derive(serde::Deserialize, Debug)]
pub struct TxnToken {
	purp: String,
	aud: Vec<String>,
	txn: String,
	azd: serde_json::Value,
	rctx: serde_json::Value,
}

#[derive(serde::Deserialize, Debug)]
pub struct IdToken {
	iss: String,
	aud: String,
	sub: String,
	nonce: String
}
