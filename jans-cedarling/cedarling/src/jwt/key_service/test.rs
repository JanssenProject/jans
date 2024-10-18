use std::{
    collections::HashMap,
    sync::{Arc, Mutex},
};

use bytes::Bytes;
use serde::Serialize;

use super::{HttpGet, KeyService};

const JWKS_RESP_1: &str = include_str!("./test-json-responses/test-jwks-response-1.json");
const JWKS_RESP_2: &str = include_str!("./test-json-responses/test-jwks-response-2.json");
// the `kid`s in JWKS_RESP
const KEY_ID_1: &str = "a50f6e70ef4b548a5fd9142eecd1fb8f54dce9ee";
const KEY_ID_2: &str = "73e25f9789119c7875d58087a78ac23f5ef2eda3";

struct MockHttpService {
    responses: HashMap<Box<str>, Bytes>,
}

impl MockHttpService {
    pub fn new() -> Self {
        Self {
            responses: HashMap::new(),
        }
    }

    pub fn set_response(&mut self, uri: &str, response: Bytes) {
        self.responses.insert(uri.into(), response);
    }
}

impl HttpGet for MockHttpService {
    fn get(&self, uri_str: &str) -> Result<Bytes, super::Error> {
        Ok(self.responses.get(uri_str).expect("unknown uri").clone())
    }
}

#[test]
fn key_service_can_retrieve_keys() {
    // setup mock data
    let issuer = "some_issuer.com";
    let jwks_uri = "https://www.googleapis.com/oauth2/v3/certs";
    let openid_conf_endpoint = format!("https://{}/.well-known/openid-configuration", issuer);

    // setup mock HttpService
    let mock_openid_conf_resp = MockOpenIdConfReponse {
        issuer: issuer.into(),
        jwks_uri: jwks_uri.into(),
        unexpected: 1123124, // a random number used to represent unexpected data
    };
    let mock_openid_conf_resp = serde_json::to_string(&mock_openid_conf_resp).unwrap();
    let mut http_service = MockHttpService::new();
    http_service.set_response(&openid_conf_endpoint, mock_openid_conf_resp.into());
    http_service.set_response(&jwks_uri, JWKS_RESP_1.into());
    let http_service = Arc::new(Mutex::new(http_service));

    // setup KeyService
    let key_service =
        KeyService::new_with_http_service(vec![&openid_conf_endpoint], http_service.clone())
            .unwrap();

    assert!(key_service.get_key(issuer, KEY_ID_1).is_some())
}

#[test]
fn key_service_can_update_keys() {
    // setup mock data
    let issuer = "some_issuer.com";
    let jwks_uri = "https://www.googleapis.com/oauth2/v3/certs";
    let openid_conf_endpoint = format!("https://{}/.well-known/openid-configuration", issuer);

    // setup mock HttpService
    let mock_openid_conf_resp = MockOpenIdConfReponse {
        issuer: issuer.into(),
        jwks_uri: jwks_uri.into(),
        unexpected: 1123124, // a random number used to represent unexpected data
    };
    let mock_openid_conf_resp = serde_json::to_string(&mock_openid_conf_resp).unwrap();
    let mut http_service = MockHttpService::new();
    http_service.set_response(&openid_conf_endpoint, mock_openid_conf_resp.into());
    http_service.set_response(&jwks_uri, JWKS_RESP_1.into());
    let http_service = Arc::new(Mutex::new(http_service));

    // setup KeyService
    let mut key_service =
        KeyService::new_with_http_service(vec![&openid_conf_endpoint], http_service.clone())
            .unwrap();

    // this should fail first
    assert!(key_service.get_key(issuer, KEY_ID_2).is_none());

    {
        let mut http_service = http_service.lock().unwrap();
        http_service.set_response(&jwks_uri, JWKS_RESP_2.into());
    }

    assert!(key_service
        .get_key_or_update_jwks(issuer, KEY_ID_2)
        .is_some());
}

#[derive(Serialize)]
struct MockOpenIdConfReponse {
    issuer: Box<str>,
    jwks_uri: Box<str>,
    unexpected: i32, // used to test if deserialzation still proceeds even if there's some
                     // unexpected data
}
