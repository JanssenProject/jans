#[derive(serde::Deserialize, serde::Serialize, Debug, Default)]
pub struct Tokens {
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
