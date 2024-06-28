use serde_wasm_bindgen::from_value;
use wasm_bindgen::prelude::*;

#[derive(serde::Deserialize, serde::Serialize, Debug, Default)]
pub struct Tokens {
	// generates entities
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

#[wasm_bindgen]
pub async fn authz(req: JsValue) -> JsValue {
	// TODO: Stub
	let _request = from_value::<Tokens>(req).unwrap();
	JsValue::NULL
}
