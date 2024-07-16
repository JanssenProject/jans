#[derive(serde::Deserialize, Debug)]
pub struct AuthzInput {
	// generates entities
	pub id_token: String,
	pub userinfo_tokens: Vec<String>,
	pub access_tokens: Vec<String>,

	// extra parameters for cedar decision resolution
	pub action: String,
	pub resource: String,
	pub context: serde_json::Value,
}
