use std::{borrow::Cow, ptr::addr_of, sync::OnceLock};
use wasm_bindgen::prelude::*;

use super::types;

pub(super) static JWT_VALIDATION_ENABLED: OnceLock<bool> = OnceLock::new();

// extracts JWT Validation settings
fn validation_options(jwt: &str, _type: types::TokenType) -> Result<(Option<jsonwebtoken::DecodingKey>, jsonwebtoken::Validation), Cow<'static, str>> {
	let trust_store = unsafe { addr_of!(super::TRUST_STORE).as_ref().expect_throw("TRUST_STORE not initialized") };
	let header = jsonwebtoken::decode_header(jwt).unwrap_throw();

	// extract JWK from iss and trust_store
	let iss = get_issuer(jwt).expect_throw("JWT must have an iss field");
	let kid = header.kid.expect_throw("JWT must have a kid for validation purposes");
	let entry = trust_store.get(&iss).expect_throw("Unknown issuer found on JWT");
	let jwk = entry.jwks.find(&kid);

	// ensure issuer can issue TokenType
	let can_issue = match _type {
		types::TokenType::IdToken => entry.issuer.id_tokens.trusted,
		types::TokenType::AccessToken => entry.issuer.access_tokens.trusted,
		types::TokenType::UserInfoToken => entry.issuer.userinfo_tokens.trusted,
	};

	if !can_issue {
		let msg = format!("Trusted Issuer: {} cannot issue tokens of type: {:?}", iss, _type);
		return Err(Cow::Owned(msg));
	}

	// TODO: verify jti against status_list

	// build decoding key
	let decoding_key = jwk.map(jsonwebtoken::DecodingKey::from_jwk).map(Result::unwrap);

	// build validation
	let mut validation = jsonwebtoken::Validation::new(jsonwebtoken::Algorithm::HS256);
	let issuers = trust_store.keys().collect::<Vec<_>>();
	validation.set_issuer(&issuers);
	validation.algorithms = unsafe { super::SUPPORTED_ALGORITHMS.clone() };

	Ok((decoding_key, validation))
}

pub fn decode_jwt<T: serde::de::DeserializeOwned>(jwt: &str, _type: types::TokenType) -> T {
	// secure by default
	match JWT_VALIDATION_ENABLED.get().cloned().unwrap_or(true) {
		true => {
			let (key, validation) = validation_options(jwt, _type).unwrap_throw();
			let key = key.expect_throw("Unable to extract DecodingKey from JWT");
			jsonwebtoken::decode(jwt, &key, &validation).unwrap_throw().claims
		}
		false => {
			let payload = jwt.split('.').nth(1).expect_throw("Malformed JWT provided");
			serde_json::from_str(&js_atob(payload)).expect_throw("Unable to parse JWT as valid base64 encoded JSON")
		}
	}
}

#[wasm_bindgen]
extern "C" {
	#[wasm_bindgen(js_name = atob)]
	pub fn js_atob(input: &str) -> String;
}

pub fn get_issuer(jwt: &str) -> Option<String> {
	#[derive(serde::Deserialize)]
	struct IssuerExtract {
		iss: String,
	}

	let payload = jwt.split('.').nth(1)?;
	let decoded: IssuerExtract = serde_json::from_str::<IssuerExtract>(&js_atob(payload)).ok()?;

	Some(decoded.iss)
}
