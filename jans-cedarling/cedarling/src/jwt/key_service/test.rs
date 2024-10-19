/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use serde_json::json;

use super::KeyService;

const JWKS_RESP_1: &str = include_str!("./test-json-responses/test-jwks-response-1.json");
const JWKS_RESP_2: &str = include_str!("./test-json-responses/test-jwks-response-2.json");
// the `kid`s in JWKS_RESP
const KEY_ID_1: &str = "a50f6e70ef4b548a5fd9142eecd1fb8f54dce9ee";
const KEY_ID_2: &str = "73e25f9789119c7875d58087a78ac23f5ef2eda3";

#[test]
fn key_service_can_retrieve_keys() {
    // init server
    let mut server = mockito::Server::new();

    // setup server responses
    let openid_config_response = json!({
        "issuer": server.url(),
        "jwks_uri": &format!("{}/jwks", server.url()),
        "unexpected": 123123, // a random number used to represent unexpected data
    });
    let openid_conf_mock = server
        .mock("GET", "/.well-known/openid-configuration")
        .with_status(200)
        .with_header("content-type", "application/json")
        .with_body(openid_config_response.to_string())
        .create();
    let jwks_uri_mock = server
        .mock("GET", "/jwks")
        .with_status(200)
        .with_header("content-type", "application/json")
        .with_body(JWKS_RESP_1)
        .create();

    // setup KeyService
    let openid_conf_endpoint = format!("{}/.well-known/openid-configuration", server.url());
    let key_service =
        KeyService::new(vec![&openid_conf_endpoint]).expect("should create key service");
    openid_conf_mock.assert();
    jwks_uri_mock.assert();

    assert!(key_service.get_key(KEY_ID_1).is_ok());
}

#[test]
fn key_service_can_update_keys() {
    // init server
    let mut server = mockito::Server::new();

    // setup server responses
    let openid_config_response = json!({
        "issuer": server.url(),
        "jwks_uri": &format!("{}/jwks", server.url()),
        "unexpected": 123123, // a random number used to represent unexpected data
    });
    let openid_conf_mock = server
        .mock("GET", "/.well-known/openid-configuration")
        .with_status(200)
        .with_header("content-type", "application/json")
        .with_body(openid_config_response.to_string())
        .create();
    let jwks_uri_mock = server
        .mock("GET", "/jwks")
        .with_status(200)
        .with_header("content-type", "application/json")
        .with_body(JWKS_RESP_1)
        .expect(2)
        .create();

    // setup KeyService
    let openid_conf_endpoint = format!("{}/.well-known/openid-configuration", server.url());
    let key_service =
        KeyService::new(vec![&openid_conf_endpoint]).expect("should create key service");
    openid_conf_mock.assert();

    // this should fail first
    assert!(key_service.get_key(KEY_ID_2).is_err());
    jwks_uri_mock.assert();

    // update the mock keystore
    let jwks_uri_mock = server
        .mock("GET", "/jwks")
        .with_status(200)
        .with_header("content-type", "application/json")
        .with_body(JWKS_RESP_2)
        .create();

    assert!(key_service.get_key(KEY_ID_2).is_ok());
    jwks_uri_mock.assert();
}
