#[derive(serde::Deserialize, serde::Serialize, Debug)]
pub struct Tokens {
	#[serde(rename = "access_token")]
	pub(crate) access: String,
	#[serde(rename = "id_token")]
	pub(crate) id: String,
	#[serde(rename = "userinfo_token")]
	pub(crate) user_info: String,
	#[serde(rename = "tx_token")]
	pub(crate) tx: String,
	#[serde(rename = "sse")]
	pub(crate) sse_url: String,
}

#[derive(serde::Deserialize, serde::Serialize)]
pub struct Request {
	pub principal: String,
	pub action: String,
	pub resource: String,
}

impl From<Request> for cedar_policy::Request {
	fn from(value: Request) -> cedar_policy::Request {
		let principal = value.principal.parse().unwrap();
		let action = value.action.parse().unwrap();
		let resource = value.resource.parse().unwrap();

		cedar_policy::Request::new(Some(principal), Some(action), Some(resource), cedar_policy::Context::empty(), None).unwrap()
	}
}
