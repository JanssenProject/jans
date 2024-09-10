use std::io::Write;

use authz_engine::{jwt_engine, Authz, PolicyStoreConfig};
use simplelog::*;

fn main() -> Result<(), Box<dyn std::error::Error>> {
	let _ = SimpleLogger::init(LevelFilter::Debug, Config::default());

	if let Result::Err(err) = real_demo_case() {
		println!("got error in demo case: {}", err);
	};
	Ok(())
}

fn real_demo_case() -> Result<(), Box<dyn std::error::Error>> {
	println!("start real_demo_case");

	let policy_json = include_str!("../../demo/policy-store/local.json");
	let input_json = include_str!("../../demo/input.json");

	let token_mapper = authz_engine::TokenMapper {
		..Default::default()
	};

	let authz = Authz::new(authz_engine::BootstrapConfig {
		application_name: Some("Demo_App".to_owned()),
		policy_store: PolicyStoreConfig::JsonRaw(policy_json.to_owned()).get_policy()?,
		token_mapper,
	})?;

	// only show entities for debug
	{
		let q = authz_engine::AuthzRequest::parse_raw(input_json)?;
		let decoded_input = q.decode_tokens(&jwt_engine::JWTDecoder::new_without_validation())?;
		let entites_box = authz.get_entities(decoded_input.jwt)?;

		let stdout = std::io::stdout();
		let mut handle = stdout.lock();

		if let Err(e) = entites_box.entities.write_to_json(&mut handle) {
			eprintln!("Error writing to JSON: {:?}", e);
		} else {
			let _ = handle.write("\n".as_bytes());
		}
	}

	let v = authz.handle_raw_input(input_json)?;
	let decision = v.decision();
	println!("decision: {decision:#?}");
	Ok(())
}
