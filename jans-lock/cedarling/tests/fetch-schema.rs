use cedar_policy::*;
use wasm_bindgen_test::*;

#[wasm_bindgen_test]
async fn test() {
	let mut policy_store = serde_json::from_str::<serde_json::Value>(include_str!("../policy-store/default.json")).unwrap();
	let policy_store = policy_store.as_object_mut().expect("Expect top level policy store to be an object");

	let _schema = {
		let schema = policy_store.remove("Schema").expect("Can't find Schema in policy store");
		Schema::from_json_value(schema).unwrap()
	};

	console_log!("Loaded Schema: {:?}", _schema);
}
