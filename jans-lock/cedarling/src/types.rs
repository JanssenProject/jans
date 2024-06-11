use wasm_bindgen::UnwrapThrowExt;

#[derive(serde::Deserialize, serde::Serialize, Debug, Default)]
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
		let principal = value.principal.parse().unwrap_throw();
		let action = value.action.parse().unwrap_throw();
		let resource = value.resource.parse().unwrap_throw();

		cedar_policy::Request::new(Some(principal), Some(action), Some(resource), cedar_policy::Context::empty(), None).unwrap_throw()
	}
}
