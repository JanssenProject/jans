#[cfg(test)]
mod tests {
	use super::super::*;
	const POLICY_JSON: &str = include_str!("../../../demo/policy-store/local.json");
	const INPUT_JSON: &str = include_str!("../../../demo/input.json");

	#[test]
	fn authz_ok_authorized() {
		let token_mapper = TokenMapper {
			access_token: Some("role".to_string()),
			..Default::default()
		};

		let authz = Authz::new(BootstrapConfig {
			application_name: Some("Demo_App".to_owned()),
			policy_store: PolicyStoreConfig::JsonRaw(POLICY_JSON.to_string())
				.get_policy()
				.unwrap(),
			token_mapper,
		})
		.unwrap();

		let request = AuthzRequest::parse_raw(INPUT_JSON).unwrap();
		assert!(
			authz.is_authorized(request).unwrap(),
			"should be authorized (true)"
		);
	}

	#[test]
	fn authz_ok_not_authorized() {
		let token_mapper = TokenMapper {
			access_token: None, // the do not map the role from the token
			id_token: None,
			userinfo_token: None,
		};

		let authz = Authz::new(BootstrapConfig {
			application_name: Some("Demo_App".to_owned()),
			policy_store: PolicyStoreConfig::JsonRaw(POLICY_JSON.to_string())
				.get_policy()
				.unwrap(),
			token_mapper,
		})
		.unwrap();

		let request = AuthzRequest::parse_raw(INPUT_JSON).unwrap();
		assert!(
			!authz.is_authorized(request).unwrap(),
			"should be not authorized (false)"
		);
	}

	fn get_authz() -> Authz {
		let token_mapper = TokenMapper {
			access_token: None, // the do not map the role from the token
			id_token: None,
			userinfo_token: None,
		};

		let authz = Authz::new(BootstrapConfig {
			application_name: Some("Demo_App".to_owned()),
			policy_store: PolicyStoreConfig::JsonRaw(POLICY_JSON.to_string())
				.get_policy()
				.unwrap(),
			token_mapper,
		})
		.unwrap();
		authz
	}

	#[test]
	fn authz_err_input_json_parse() {
		let err = get_authz()
			.handle_raw_input("{\"invalid_json\"")
			.unwrap_err();

		assert!(
			matches!(err, HandleError::InputJsonParse(_)),
			"Expected InputJsonParse error, got: {:?}",
			err
		);
	}

	#[test]
	fn authz_err_decode_access_token() {
		let mut request = AuthzRequest::parse_raw(INPUT_JSON).unwrap();
		request.access_token = "invalid_token".to_string();

		let err = get_authz().handle(request).unwrap_err();
		assert!(
			matches!(err, HandleError::DecodeTokens(_)),
			"Expected DecodeTokens error, got: {:?}",
			err
		);
	}

	#[test]
	fn authz_err_decode_id_token() {
		let mut request = AuthzRequest::parse_raw(INPUT_JSON).unwrap();
		request.id_token = "invalid_token".to_string();

		let err = get_authz().handle(request).unwrap_err();
		assert!(
			matches!(err, HandleError::DecodeTokens(_)),
			"Expected DecodeTokens error, got: {:?}",
			err
		);
	}

	#[test]
	fn authz_err_decode_userinfo_token() {
		let mut request = AuthzRequest::parse_raw(INPUT_JSON).unwrap();
		request.userinfo_token = "invalid_token".to_string();

		let err = get_authz().handle(request).unwrap_err();
		assert!(
			matches!(err, HandleError::DecodeTokens(_)),
			"Expected DecodeTokens error, got: {:?}",
			err
		);
	}

	#[test]
	fn authz_err_action() {
		let mut request = AuthzRequest::parse_raw(INPUT_JSON).unwrap();
		request.extra.action = "invalid_action".to_string();

		let err = get_authz().handle(request).unwrap_err();
		assert!(
			matches!(err, HandleError::Action(_)),
			"Expected Action error, got: {:?}",
			err
		);
	}

	#[test]
	fn authz_err_resource() {
		let mut request = AuthzRequest::parse_raw(INPUT_JSON).unwrap();
		request.extra.resource = ResourceData {
			_type: "invalid:::".to_string(),
			id: "invalid".to_string(),
		};

		let err = get_authz().handle(request).unwrap_err();
		assert!(
			matches!(err, HandleError::Resource(_)),
			"Expected Resource error, got: {:?}",
			err
		);
	}

	#[test]
	fn authz_err_input_entities() {
		let mut request = AuthzRequest::parse_raw(INPUT_JSON).unwrap();
		//token with wrong iss
		request.id_token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjQ3OTkzNDQ3ZmZjZDZmMTViODAwZDg1Njc2NWU2YjI5In0.eyJhdF9oYXNoIjoiYjVDS21YdVBVdElRMW9VZFN4YkdQUSIsInN1YiI6ImJvRzhkZmM1TUtUbjM3bzdnc2RDZXlxTDhMcFdRdGdvTzQxbTFLWndkcTAiLCJjb3VudHJ5IjoiSFUiLCJiaXJ0aGRhdGUiOiIyMDAwLTAxLTAxIiwidXNlcl9uYW1lIjoiYWRtaW4iLCJhbXIiOlsiMTAiXSwiaXNzIjoiIiwiaW51bSI6IjhkMWNkZTZhLTE0NDctNDc2Ni1iM2M4LTE2NjYzZTEzYjQ1OCIsInNpZCI6ImNiMDNkZWU3LWIyYTktNGVhZC04Mjg3LWU3OGFhNWFiYjIyNSIsImphbnNPcGVuSURDb25uZWN0VmVyc2lvbiI6Im9wZW5pZGNvbm5lY3QtMS4wIiwiYWNyIjoiYmFzaWMiLCJ1cGRhdGVkX2F0IjoxNzI1MDE4OTAyLCJhdXRoX3RpbWUiOjE3MjUwMTg5MzEsIm5pY2tuYW1lIjoiQWRtaW4iLCJleHAiOjE3MjUwMjI1MzIsImlhdCI6MTcyNTAxODkzMiwianRpIjoia01HZGhVbDFSYXFqOVlsaTRXSzlYQSIsImVtYWlsIjoiYWRtaW5AYWRtaW4tdWktdGVzdC5nbHV1Lm9yZyIsImdpdmVuX25hbWUiOiJBZG1pbiIsIm1pZGRsZV9uYW1lIjoiQWRtaW4iLCJub25jZSI6IjQwZmU3ZGQ0LWM5YmYtNGFlOS1iMWFkLTk2MDQwYjRhZGUxMCIsImF1ZCI6IjMzZDhjMDIwLTVjOTEtNGZhNi04MDQxLTQ4NGVhYWUzOTkyNiIsImNfaGFzaCI6IlIxeDlYWk0zQ0FQU09DOC1XRjBGeEEiLCJuYW1lIjoiRGVmYXVsdCBBZG1pbiBVc2VyIiwidXNlcl9wZXJtaXNzaW9uIjpbIkNhc2FBZG1pbiJdLCJwaG9uZV9udW1iZXIiOiIrOTE3ODM3Njc5MzQwIiwiZ3JhbnQiOiJhdXRob3JpemF0aW9uX2NvZGUiLCJmYW1pbHlfbmFtZSI6IlVzZXIiLCJzdGF0dXMiOnsic3RhdHVzX2xpc3QiOnsiaWR4IjoyMDA0LCJ1cmkiOiJodHRwczovL2FkbWluLXVpLXRlc3QuZ2x1dS5vcmcvamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdCJ9fSwiamFuc0FkbWluVUlSb2xlIjpbImFwaS1hZG1pbiJdLCJyb2xlIjpbImlkX3Rva2VuX3JvbGUiLCJBZG1pbiJdfQ.Bjyn8s8DH0GCxypGZOH2Cf_-LJcaYVVBFFzke1RErh9XBM_XAFLydD_E9k8JyefS2RvNCYWsvRq_BEyKwOEfow".to_string();

		let err = get_authz().handle(request).unwrap_err();
		assert!(
			matches!(err, HandleError::AuthzInputEntities(_)),
			"Expected AuthzInputEntities error, got: {:?}",
			err
		);
	}

	#[test]
	fn authz_err_context() {
		let mut request = AuthzRequest::parse_raw(INPUT_JSON).unwrap();
		request.extra.context = serde_json::json!({"field_not_covered_by_schema": true});
		let err = get_authz().handle(request).unwrap_err();
		assert!(
			matches!(err, HandleError::Context(_)),
			"Expected Context error, got: {:?}",
			err
		);
	}
}
