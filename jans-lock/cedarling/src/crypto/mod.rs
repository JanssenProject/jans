use std::{collections::BTreeMap, str::FromStr, sync::OnceLock};
use wasm_bindgen::prelude::*;

use crate::{
	http::{self, ResponseEx},
	lock_master, startup,
};

pub mod decode;
pub mod types;

// Supported algorithms
pub static mut SUPPORTED_ALGORITHMS: Vec<jsonwebtoken::Algorithm> = Vec::new();

// Trust Store, iss -> { config: OAuthConfig, jwks: JsonWebKeySet }
pub static mut TRUST_STORE: BTreeMap<String, types::TrustStoreEntry> = BTreeMap::new();

pub fn init(config: &startup::types::CedarlingConfig, trusted_issuers: BTreeMap<String, types::TrustedIssuer>) {
	decode::JWT_VALIDATION_ENABLED.set(config.jwt_validation).expect_throw("JWT_VALIDATION_ENABLED already initialized");

	// Insert supported jwt signature algorithms
	let supported = config.supported_signature_algorithms.iter().map(|s| jsonwebtoken::Algorithm::from_str(s).unwrap_throw()).collect();
	unsafe {
		SUPPORTED_ALGORITHMS = supported;
	}

	// insert id into issuer for token creation, map to Vector for easy sequential iteration
	let issuers = trusted_issuers
		.into_iter()
		.map(|(name, mut issuer)| {
			issuer.name = Some(name);
			issuer
		})
		.collect();

	// Init trust store
	init_trust_store(config.trust_store_refresh_rate, issuers)
}

// A list of TrustedIssuers configured once during startup
static TRUSTED_ISSUERS: OnceLock<Vec<types::TrustedIssuer>> = OnceLock::new();

fn init_trust_store(refresh_rate: Option<i32>, trusted_issuers: Vec<types::TrustedIssuer>) {
	TRUSTED_ISSUERS.set(trusted_issuers).expect_throw("TRUSTED_ISSUERS already initialized");

	let refresh_trust_store = move || {
		wasm_bindgen_futures::spawn_local(async move {
			for issuer in TRUSTED_ISSUERS.get().expect_throw("TRUSTED_ISSUERS not initialized") {
				let req = http::get(&issuer.openid_configuration_endpoint, &[]).await;
				let res = req.expect_throw("Unable to get OpenID config for TrustedIssuer");

				let config = res
					.into_json::<lock_master::types::OAuthConfig>()
					.await
					.expect_throw("Unable to parse OpenID config from TrustedIssuer");

				// update JwkSet
				let req = http::get(&config.jwks_uri, &[]).await;
				let res = req.expect_throw("Unable to fetch jwks from trusted issuer");

				let iss = config.issuer.clone();

				let jwks = res.into_json().await.expect_throw("Unable to parse jwks from TrustedIssuer");
				let entry = types::TrustStoreEntry { jwks, issuer };

				let _ = unsafe { TRUST_STORE.insert(iss, entry) };
			}
		})
	};

	// initial update
	refresh_trust_store();

	#[wasm_bindgen]
	extern "C" {
		#[wasm_bindgen(js_name = setInterval)]
		pub fn set_interval(callback: &web_sys::js_sys::Function, interval: i32);
	}

	// setup refresh loop
	if let Some(refresh_rate) = refresh_rate {
		let callback: Closure<dyn Fn()> = Closure::new(refresh_trust_store);
		let function: web_sys::js_sys::Function = callback.into_js_value().dyn_into().unwrap_throw();

		set_interval(&function, refresh_rate);
	}
}
