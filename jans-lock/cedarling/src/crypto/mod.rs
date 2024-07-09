use std::{collections::BTreeMap, rc::Rc, str::FromStr, sync::OnceLock};
use wasm_bindgen::prelude::*;
use web_sys::Window;

use crate::{
	http::{self, ResponseEx},
	lock_master, startup,
};

pub(crate) mod decode;
mod types;

// Supported algorithms
pub(crate) static SUPPORTED_ALGORITHMS: OnceLock<Vec<jsonwebtoken::Algorithm>> = OnceLock::new();

pub(crate) fn init(config: &startup::types::CedarlingConfig, mut policy_store: serde_json::Map<String, serde_json::Value>) {
	// insert supported jwt signature algorithms
	let supported = config.supported_signature_algorithms.iter().map(|s| jsonwebtoken::Algorithm::from_str(s).unwrap_throw()).collect();
	SUPPORTED_ALGORITHMS.set(supported).expect_throw("SUPPORTED_ALGORITHMS is already initialized");

	// Load trusted issuers
	let trusted_issuers = {
		let mut issuers = policy_store.remove("TrustedIssuers").expect_throw("Can't find TrustedIssuers in policy store");
		let issuers = issuers.as_array_mut().expect_throw("expect_throw TrustedIssuers to be an array");
		issuers.drain(..).map(|issuer| serde_json::from_value(issuer).unwrap_throw()).collect::<Vec<types::TrustedIssuer>>()
	};

	// Init trust store
	init_trust_store(trusted_issuers.into(), config.trust_store_refresh_rate)
}

// Trust Store
pub(crate) static mut TRUST_STORE: OnceLock<BTreeMap<types::TrustedIssuer, (String, jsonwebtoken::jwk::JwkSet)>> = OnceLock::new();

fn init_trust_store(trusted_issuers: Rc<[types::TrustedIssuer]>, refresh_rate: Option<i32>) {
	let refresh_trust_store = move || {
		let issuers = trusted_issuers.clone();

		wasm_bindgen_futures::spawn_local(async move {
			for issuer in issuers.as_ref() {
				let req = http::get(&issuer.openid_configuration_endpoint, &[]).await;
				let res = req.expect_throw("Unable to get OpenID config for TrustedIssuer");

				let config = res
					.into_json::<lock_master::types::OAuthConfig>()
					.await
					.expect_throw("Unable to parse OpenID config from TrustedIssuer");

				// update JWKS
				let req = http::get(&config.jwks_uri, &[]).await;
				let res = req.expect_throw("Unable to fetch JWKS from trusted issuer");

				unsafe {
					let jwks = res.into_json().await.expect_throw("Unable to parse JWKS from TrustedIssuer");

					let store = TRUST_STORE.get_mut().unwrap_throw();
					let _ = store.insert(issuer.clone(), (config.issuer, jwks));
				}
			}
		})
	};

	// initial update
	refresh_trust_store();

	// setup refresh loop
	if let Some(refresh_rate) = refresh_rate {
		let callback: Closure<dyn Fn()> = Closure::new(refresh_trust_store);

		let global = web_sys::js_sys::global();
		let window = global.unchecked_ref::<Window>();

		window
			.set_interval_with_callback_and_timeout_and_arguments_0(callback.into_js_value().unchecked_ref(), refresh_rate)
			.unwrap_throw();
	}
}
