use cedarling::utils;
use wasm_bindgen_test::*;

#[wasm_bindgen_test]
async fn test() {
	let schema = utils::fetch_schema().await;
	console_log!("Fetched Schema: {:?}", &schema)
}
