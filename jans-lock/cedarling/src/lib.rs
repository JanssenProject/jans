use std::{
	collections::BTreeMap,
	io::{Cursor, Read},
	str,
};

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

	// Get PolicyStore zip
	let mut zip = match CONFIG.policy_store.strategy {
		"local" => None,
		"remote" => {
			let res = http::get(CONFIG.policy_store.uri, &[]).await.expect("Can't fetch Policy Store zip");
			let bytes = res.into_bytes().await.expect("Can't convert Policy Store response to bytes");
			let bytes = Cursor::new(bytes);

			Some(zip::ZipArchive::new(bytes).expect("Can't open Policy Store zip"))
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
					window().unwrap().btoa(&format!("{}:{}", client.client_id, client_secret)).unwrap()
				} else {
					console::warn_1(&JsValue::from_str("Client Secret is not provided"));
					window().unwrap().btoa(&client.client_id).unwrap()
				};
				let auth = format!("Basic {}", token);

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
				Cursor::new(res.into_bytes().await.expect("Unable to convert response to bytes"))
			};

			Some(zip::ZipArchive::new(buffer).expect("Can't open Policy Store zip"))
		}
		strategy => {
			let msg = format!("Unknown Policy Store Strategy: {}", strategy);
			throw_val(JsValue::from_str(&msg));
		}
	};

	// Load Schema
	let _schema = match zip {
		Some(ref mut zip) => {
			let file = zip.by_name("schema.txt").expect("Can't find `schema.txt` in Policy Store");
			let (schema, warnings) = Schema::from_file_natural(file).unwrap();

			for warning in warnings {
				let msg = format!("Schema Parser generated Warning: {}", warning);
				let msg = JsValue::from_str(&msg);
				console::warn_1(&msg)
			}

			cedar::schema(Some(schema))
		}
		None => {
			let schema = include_str!("../policy-store/schema.txt");
			let (schema, warnings) = Schema::from_str_natural(schema).unwrap();

			for warning in warnings {
				let msg = format!("Schema Parser generated Warning: {}", warning);
				let msg = JsValue::from_str(&msg);
				console::warn_1(&msg)
			}

			cedar::schema(Some(schema))
		}
	};

	// Load PolicySet
	let _policies = match zip {
		Some(ref mut zip) => {
			let mut file = zip.by_name("policies.txt").expect("Can't find `policies.txt` in Policy Store");
			let mut string = String::new();
			file.read_to_string(&mut string).unwrap_throw();

			cedar::policies(Some(string.parse().unwrap_throw()))
		}
		None => {
			let policies = include_str!("../policy-store/policies.txt");
			cedar::policies(Some(policies.parse().unwrap_throw()))
		}
	};

	// Load trusted issuers
	let _issuers = match zip {
		Some(ref mut zip) => {
			let file = zip.by_name("trusted-issuers.json").expect("Can't find `trusted-issuers.json` in Policy Store");
			let issuers = serde_json::from_reader(file).expect("Can't parse `trusted-issuers.json` in Policy Store");

			cedar::trusted_issuers(Some(issuers))
		}
		None => {
			let issuers = include_str!("../policy-store/trusted-issuers.json");
			let issuers = serde_json::from_str(issuers).expect("Can't parse `trusted-issuers.json` in Policy Store");

			cedar::trusted_issuers(Some(issuers))
		}
	};

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
