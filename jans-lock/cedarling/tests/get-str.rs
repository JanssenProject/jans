use cedarling::http::{self, ResponseEx};
use wasm_bindgen_test::*;

#[wasm_bindgen_test]
async fn test() {
	let data = http::get("https://site-scraper.sokorototo.workers.dev/", &[]).await.unwrap();
	let string = data.into_string().await.unwrap();
	assert_eq!(string, "site-scraper v0.2.0");
}
