use std::{borrow::Cow, collections::BTreeMap};

use cedar_policy::*;
use wasm_bindgen::{prelude::*, throw_str};
use web_sys::*;

use super::{http, CONFIG};
use http::ResponseEx;

mod statics;
mod types;

pub async fn open_id_config(issuer_url: &'static str) -> &'static types::OpenIdConfiguration {
	static mut OPENID_CONFIGURATION_STORE: BTreeMap<&'static str, types::OpenIdConfiguration> = BTreeMap::new();

	unsafe {
		match OPENID_CONFIGURATION_STORE.get(issuer_url) {
			Some(config) => config,
			None => {
				// fetch OpenID Configuration
				let res = http::get(issuer_url, &[]).await.expect_throw("Can't fetch OpenID Configuration");
				let config = res.into_json().await.expect_throw("Can't parse OpenID Configuration");

				OPENID_CONFIGURATION_STORE.insert(issuer_url, config);
				OPENID_CONFIGURATION_STORE.get(issuer_url).unwrap_unchecked()
			}
		}
	}
}

pub async fn get_policy_store(config: &types::OpenIdConfiguration) -> serde_json::Map<String, serde_json::Value> {
	// Get PolicyStore JSON
	let source = match CONFIG.policy_store.strategy {
		"local" => Cow::Borrowed(include_bytes!(concat!(env!("CARGO_MANIFEST_DIR"), "/policy-store/default.json")).as_slice()),
		"remote" => {
			let res = http::get(CONFIG.policy_store.uri, &[]).await.expect_throw("Unable fetch Policy Store");
			let bytes = res.into_bytes().await.expect_throw("Can't convert Policy Store response to String");
			Cow::Owned(bytes)
		}
		"lock-master" => {
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
					.expect_throw("Unable to register client");
				res.into_json().await.expect_throw("Unable to parse client registration response")
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
				let headers = [("Authorization", auth.as_str())];
				let res = http::post(&url, http::PostBody::Form(&grant), &headers).await.expect_throw("Unable to get Access Token");

				res.into_json().await.expect_throw("Unable to parse Access Token Response")
			};

			let buffer = {
				let url = format!("{}/config?id={}", CONFIG.lock_master.url, CONFIG.lock_master.policy_store_id);
				let auth = format!("Bearer {}", grant.access_token);
				let res = http::get(&url, &[("Authorization", &auth)]).await.expect_throw("Unable to fetch policies from remote location");
				Cow::Owned(res.into_bytes().await.expect_throw("Unable to convert response to bytes"))
			};

			buffer
		}
		strategy => {
			let msg = format!("Unknown Policy Store Strategy: {}", strategy);
			throw_str(&msg)
		}
	};

	// Decompress if necessary
	let source = match CONFIG.decompress_policy_store {
		true => Cow::Owned(miniz_oxide::inflate::decompress_to_vec_zlib(&source).unwrap_throw()),
		false => source,
	};

	// Parse JSON
	let policy_store = serde_json::from_slice::<serde_json::Value>(&source).unwrap_throw();
	match policy_store {
		serde_json::Value::Object(map) => map,
		p => {
			let error = format!("Expected top-level policy store to be an Object. Found: {:?}", p);
			throw_str(&error)
		}
	}
}

pub fn persist_policy_store_data(mut policy_store: serde_json::Map<String, serde_json::Value>) {
	// Load Schema
	let schema = {
		let schema = policy_store.remove("Schema").expect_throw("Can't find Schema in policy store");
		Schema::from_json_value(schema).unwrap_throw()
	};

	// Load PolicySet
	let policies = {
		let mut policies = policy_store.remove("PolicySet").expect_throw("Can't find PolicySet in policy store");
		let policies = policies.as_array_mut().expect_throw("expect_throw PolicySet to be an array");

		let iter = policies.drain(..).into_iter().map(|policy| Policy::from_json(None, policy).unwrap_throw());
		PolicySet::from_policies(iter).unwrap_throw()
	};

	// Load trusted issuers
	let trusted_issuers = {
		let mut issuers = policy_store.remove("TrustedIssuers").expect_throw("Can't find TrustedIssuers in policy store");
		let issuers = issuers.as_array_mut().expect_throw("expect_throw TrustedIssuers to be an array");

		issuers
			.drain(..)
			.into_iter()
			.map(|issuer| serde_json::from_value(issuer).unwrap_throw())
			.collect::<Vec<types::TrustedIssuer>>()
	};

	// Persist PolicyStore data
	statics::schema(Some(schema));
	statics::policies(Some(policies));
	statics::trusted_issuers(Some(trusted_issuers));
}
