/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

mod sort_json;
pub mod token_claims;

use mockito::{Mock, Server};
pub use pretty_assertions::*;
use serde_json::json;
pub use sort_json::SortedJson;

use crate::token_claims::{KeyPair, generate_jwks, generate_keypair_hs256};

pub struct MockServer {
    pub keys: KeyPair,
    pub oidc_endpoint: Mock,
    pub jwks_endpoint: Mock,
    pub base_idp_url: String,
    // we need to store server to avoid drop
    #[allow(dead_code)]
    mock_server: Server,
}

pub fn gen_mock_server() -> MockServer {
    // only when we define `mockito::ServerOpts` it different
    // by default it gets random port
    let mut mock_server = mockito::Server::new_with_opts(mockito::ServerOpts {
        ..Default::default()
    });

    // Setup OpenId config endpoint
    let oidc = json!({
        "issuer": mock_server.url(),
        "jwks_uri": &format!("{}/jwks", mock_server.url()),
    });
    let oidc_endpoint = mock_server
        .mock("GET", "/.well-known/openid-configuration")
        .with_status(200)
        .with_header("content-type", "application/json")
        .with_body(oidc.to_string())
        .expect_at_least(1)
        .create();

    // Setup JWKS endpoint
    let keys = generate_keypair_hs256(Some("some_hs256_key"));
    let jwks_endpoint = mock_server
        .mock("GET", "/jwks")
        .with_status(200)
        .with_header("content-type", "application/json")
        .with_body(json!({"keys": generate_jwks(&vec![keys.clone()]).keys}).to_string())
        .expect_at_least(1)
        .create();

    MockServer {
        keys,
        oidc_endpoint,
        jwks_endpoint,
        base_idp_url: mock_server.url(),
        mock_server,
    }
}
