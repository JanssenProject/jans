#[derive(serde::Deserialize, Debug)]
pub struct AuthzInput {
	// generates entities
	pub id_token: String,
	pub userinfo_token: String,
	pub access_token: String,

	// extra parameters for cedar decision resolution
	pub action: String,
	pub resource: serde_json::Value,
	pub context: serde_json::Value,
}
