use wasm_bindgen::prelude::*;

#[wasm_bindgen]
extern "C" {
	#[wasm_bindgen(js_name = btoa)]
	pub fn js_btoa(input: &str) -> String;
}

use wasm_bindgen_test::*;

#[wasm_bindgen_test]
async fn test() {
	let input = "Hello, World!";
	let output = js_btoa(input);
	assert_eq!(output, "SGVsbG8sIFdvcmxkIQ==");
}
