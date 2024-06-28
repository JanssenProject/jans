use cedarling::open_id_config;
use wasm_bindgen_test::*;

#[wasm_bindgen_test]
async fn test() {
	let config = open_id_config("https://account.gluu.org/.well-known/openid-configuration").await;
	console_log!("Fetched config: {:?}", &config);
}
