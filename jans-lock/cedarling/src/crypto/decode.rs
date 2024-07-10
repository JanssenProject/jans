use std::borrow::Cow;
use wasm_bindgen::prelude::*;

// JWT Validation settings
pub(crate) fn validation_options(jwt: &str) -> Result<(Option<jsonwebtoken::DecodingKey>, jsonwebtoken::Validation), Cow<'static, str>> {
	let trust_store = unsafe { super::TRUST_STORE.get().expect_throw("TRUST_STORE not initialized") };
	let supported = super::SUPPORTED_ALGORITHMS.get().expect_throw("SUPPORTED_ALGORITHMS not initialized").clone();

	let header = jsonwebtoken::decode_header(jwt).unwrap_throw();

	// extract JWK
	let jwk = if let Some(jwk) = header.jwk {
		Some(jwk)
	} else {
		match header.kid {
			Some(kid) => {
				let mut jwk = None;

				for (_, (_, jwks)) in trust_store {
					if let Some(found) = jwks.find(&kid) {
						jwk = Some(found);
						break;
					}
				}

				jwk.cloned()
			}
			None => None,
		}
	};

	// build decoding key
	let decoding_key = jwk.as_ref().map(jsonwebtoken::DecodingKey::from_jwk).map(Result::unwrap);

	// build validation
	let mut validation = jsonwebtoken::Validation::new(jsonwebtoken::Algorithm::HS256);
	let issuers = trust_store.values().map(|(issuer, _)| issuer).collect::<Vec<_>>();
	validation.set_issuer(&issuers);
	validation.algorithms = supported;

	Ok((decoding_key, validation))
}

pub(crate) fn decode_jwt<T: serde::de::DeserializeOwned>(jwt: &str) -> T {
	let (key, validation) = validation_options(jwt).unwrap_throw();
	let key = &key.expect_throw("Unable to extract DecodingKey from JWT");
	jsonwebtoken::decode(jwt, key, &validation).unwrap_throw().claims
}

#[wasm_bindgen]
extern "C" {
	#[wasm_bindgen(js_name = atob)]
	pub fn js_atob(input: &str) -> String;
}

pub(crate) fn get_issuer(jwt: &str) -> Option<String> {
	#[derive(serde::Deserialize)]
	struct IssuerExtract {
		iss: String,
	}

	let payload = jwt.split(".").nth(1)?;
	let decoded: IssuerExtract = serde_json::from_str::<IssuerExtract>(&js_atob(payload)).ok()?;

	Some(decoded.iss)
}
