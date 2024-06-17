use cedarling::utils;
use wasm_bindgen::UnwrapThrowExt;
use wasm_bindgen_test::*;

#[wasm_bindgen_test]
async fn test() {
	let data = utils::get("https://site-scraper.sokorototo.workers.dev/").await.unwrap_throw();
	let string = std::str::from_utf8(&data).unwrap_throw();
	assert_eq!(string, "site-scraper v0.2.0");
}
