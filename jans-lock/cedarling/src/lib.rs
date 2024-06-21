use std::{borrow::Cow, collections::BTreeMap, str};

use cedar_policy::*;
use http::ResponseEx;
use serde_wasm_bindgen::from_value;
use wasm_bindgen::{prelude::*, throw_val};
use web_sys::*;

pub mod cedar;
pub mod http;
pub mod types;

static_toml::static_toml! {
	pub(crate) static CONFIG = include_toml!("config.toml");
}

pub async fn open_id_config(issuer_url: &'static str) -> &'static types::OpenIdConfiguration {
	static mut OPENID_CONFIGURATION_STORE: BTreeMap<&'static str, types::OpenIdConfiguration> = BTreeMap::new();

	unsafe {
		match OPENID_CONFIGURATION_STORE.get(issuer_url) {
			Some(config) => config,
			None => {
				// fetch OpenID Configuration
				let res = http::get(issuer_url, &[]).await.expect("Can't fetch OpenID Configuration");
				let config = res.into_json().await.expect("Can't parse OpenID Configuration");

				OPENID_CONFIGURATION_STORE.insert(issuer_url, config);
				OPENID_CONFIGURATION_STORE.get(issuer_url).unwrap_unchecked()
			}
		}
	}
}

#[wasm_bindgen(start)]
pub async fn start() {
	// TODO: setup panic hook, on production we should use wasm-bindgen::UnwrapThrowExt
	console_error_panic_hook::set_once();

	// load default OpenID Configuration
	let config = open_id_config(CONFIG.openid_config_url).await;

	// Get PolicyStore JSON
	let source = match CONFIG.policy_store.strategy {
		"local" => Cow::Borrowed(include_bytes!("../policy-store/default.json").as_slice()),
		"remote" => {
			let res = http::get(CONFIG.policy_store.uri, &[]).await.expect("Unable fetch Policy Store");
			let bytes = res.into_bytes().await.expect("Can't convert Policy Store response to String");
			Cow::Owned(bytes)
		}
		"authenticated" => {
			let client: types::OpenIdDynamicClient = {
				// OpenID dynamic client registration
				let client_req = types::OpenIdDynamicClientRequest {
					client_name: "Jans Cedarling",
					application_type: "web",
					redirect_uris: &[],
					token_endpoint_auth_method: "client_secret_basic",
					software_statement: CONFIG.lock_master.ssa_jwt,
					contacts: &["newton@gluu.org"],
				};

				// send
				let res = http::post(&config.registration_endpoint, http::PostBody::Json(client_req), &[])
					.await
					.expect("Unable to register client");
				res.into_json().await.expect("Unable to parse client registration response")
			};

			let grant: types::OpenIdGrantResponse = {
				// https://gluu.org/docs/gluu-server/4.0/admin-guide/oauth2/
				let token = if let Some(client_secret) = client.client_secret {
					format!("{}:{}", client.client_id, client_secret)
				} else {
					console::warn_1(&JsValue::from_str("Client Secret is not provided"));
					client.client_id
				};

				#[wasm_bindgen]
				extern "C" {
					#[wasm_bindgen(js_name = btoa)]
					pub fn js_btoa(input: &str) -> String;
				}

				let auth = format!("Basic {}", js_btoa(&token));

				let grant = types::OpenIdGrantRequest {
					scope: "cedarling",
					grant_type: "client_credentials",
				};

				// send
				let url = format!("{}/token", config.token_endpoint);
				let res = http::post(&url, http::PostBody::Form(&grant), &[("Authorization", &auth)]).await.expect("Unable to get Access Token");

				res.into_json().await.expect("Unable to parse Access Token Response")
			};

			let buffer = {
				let url = format!("{}/config?id={}", CONFIG.lock_master.url, CONFIG.lock_master.policy_store_id);
				let auth = format!("Bearer {}", grant.access_token);
				let res = http::get(&url, &[("Authorization", &auth)]).await.expect("Unable to fetch policies from remote location");
				Cow::Owned(res.into_bytes().await.expect("Unable to convert response to bytes"))
			};

			buffer
		}
		strategy => {
			let msg = format!("Unknown Policy Store Strategy: {}", strategy);
			throw_val(JsValue::from_str(&msg));
		}
	};

	// Decompress if necessary
	let source = match CONFIG.decompress_policy_store {
		true => Cow::Owned(miniz_oxide::inflate::decompress_to_vec_zlib(&source).unwrap()),
		false => source,
	};

	// Parse JSON
	let mut policy_store = serde_json::from_slice::<serde_json::Value>(&source).unwrap();
	let policy_store = policy_store.as_object_mut().expect("Expect top level policy store to be an object");

	// Load Schema
	let schema = {
		let schema = policy_store.remove("Schema").expect("Can't find Schema in policy store");
		Schema::from_json_value(schema).unwrap()
	};

	// Load PolicySet
	let policies = {
		let mut policies = policy_store.remove("PolicySet").expect("Can't find PolicySet in policy store");
		let policies = policies.as_array_mut().expect("Expect PolicySet to be an array");

		let iter = policies.drain(..).into_iter().map(|policy| Policy::from_json(None, policy).unwrap());
		PolicySet::from_policies(iter).unwrap()
	};

	// Load trusted issuers
	let trusted_issuers = {
		let mut issuers = policy_store.remove("TrustedIssuers").expect("Can't find TrustedIssuers in policy store");
		let issuers = issuers.as_array_mut().expect("Expect TrustedIssuers to be an array");

		issuers
			.drain(..)
			.into_iter()
			.map(|issuer| serde_json::from_value(issuer).unwrap())
			.collect::<Vec<types::TrustedIssuer>>()
	};

	// Store PolicyStore data
	cedar::schema(Some(schema));
	cedar::policies(Some(policies));
	cedar::trusted_issuers(Some(trusted_issuers));

	// check whether config updates are enabled
	if CONFIG.dynamic_configuration {
		let url = format!("{}/sse", CONFIG.lock_master.url);
		let sse = EventSource::new(&url).unwrap();
		let sse2 = sse.clone();

		// Setup SSE event listeners
		let onopen = Closure::once_into_js(move || {
			let onmessage = Closure::<dyn Fn(MessageEvent)>::new(move |ev: MessageEvent| {
				console::log_2(&JsValue::from_str("Cedarling Received message: "), &ev);
				unimplemented!("Dynamic Configuration Updates")
			})
			.into_js_value();
			sse2.set_onmessage(Some(onmessage.as_ref().unchecked_ref()));

			let onerror = Closure::<dyn Fn(JsValue)>::new(move |ev: JsValue| throw_val(ev)).into_js_value();
			sse2.set_onerror(Some(onerror.as_ref().unchecked_ref()));
		});

		sse.set_onopen(Some(onopen.as_ref().unchecked_ref()));
	}
}

#[wasm_bindgen]
pub async fn authz(req: JsValue) -> JsValue {
	let _request = from_value::<types::Tokens>(req).unwrap();
	JsValue::NULL
}
