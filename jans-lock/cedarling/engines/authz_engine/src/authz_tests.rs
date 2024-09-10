#[cfg(test)]
mod tests {
	use super::super::*;

	#[test]
	fn authz_ok_authorized() {
		let policy_json = include_str!("../../../demo/policy-store/local.json");
		let input_json = include_str!("../../../demo/input.json");

		let token_mapper = TokenMapper {
			access_token: Some("role".to_string()),
			..Default::default()
		};

		let authz = Authz::new(BootstrapConfig {
			application_name: Some("Demo_App".to_owned()),
			policy_store: PolicyStoreConfig::JsonRaw(policy_json.to_string())
				.get_policy()
				.unwrap(),
			token_mapper,
		})
		.unwrap();

		let request = AuthzRequest::parse_raw(input_json).unwrap();
		assert!(
			authz.is_authorized(request).unwrap(),
			"should be authorized (true)"
		);
	}

	#[test]
	fn authz_ok_not_authorized() {
		let policy_json = include_str!("../../../demo/policy-store/local.json");
		let input_json = include_str!("../../../demo/input.json");

		let token_mapper = TokenMapper {
			access_token: None, // the do not map the role from the token
			id_token: None,
			userinfo_token: None,
		};

		let authz = Authz::new(BootstrapConfig {
			application_name: Some("Demo_App".to_owned()),
			policy_store: PolicyStoreConfig::JsonRaw(policy_json.to_string())
				.get_policy()
				.unwrap(),
			token_mapper,
		})
		.unwrap();

		let request = AuthzRequest::parse_raw(input_json).unwrap();
		assert!(
			!authz.is_authorized(request).unwrap(),
			"should be not authorized (false)"
		);
	}
}
