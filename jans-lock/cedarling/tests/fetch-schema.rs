use cedar_policy::*;
use wasm_bindgen_test::*;

#[wasm_bindgen_test]
async fn test() {
	let schema = include_str!("../policy-store/schema.txt");
	let (schema, warnings) = Schema::from_str_natural(schema).unwrap();

	for warning in warnings {
		console_log!("Schema Parser generated Warning: {}", warning);
	}

	console_log!("Fetched Schema: {:?}", &schema)
}
