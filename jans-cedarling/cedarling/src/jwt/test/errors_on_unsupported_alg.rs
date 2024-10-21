/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::{
    super::{
        decoding_strategy::{DecodingStrategy, KeyService},
        JwtService,
    },
    *,
};
use jsonwebtoken::Algorithm;
use serde_json::json;

#[test]
/// Tests if JWT validation fails due to unsupported algorithm.
///
/// This test verifies that the `JwtService` correctly fails validation when the token
/// is signed with an unsupported algorithm. the service is configured to support `HS256`,
/// but the token is signed with `ES256`.
fn errors_on_unsuppored_alg() {
    // initialize mock server to simulate OpenID configuration and JWKS responses
    let mut server = mockito::Server::new();

    // generate keys and setup the encoding keys and JWKS (JSON Web Key Set)
    let (encoding_keys, jwks) = generate_keys();

    // setup claims for access token and ID token
    let mut access_token_claims = AccessTokenClaims {
        iss: server.url(),
        aud: "some_aud".to_string(),
        sub: "some_sub".to_string(),
        scopes: "some_scope".to_string(),
        ..Default::default()
    };
    let mut id_token_claims = IdTokenClaims {
        iss: server.url(),
        aud: "some_aud".to_string(),
        sub: "some_sub".to_string(),
        email: "some_email@gmail.com".to_string(),
        ..Default::default()
    };

    // generate the access token and ID token using ES256 algorithm and encoding keys
    let access_token =
        generate_access_token_using_keys(&mut access_token_claims, &encoding_keys, false);
    let id_token = generate_id_token_using_keys(&mut id_token_claims, &encoding_keys, false);

    // setup mock server responses for OpenID configuration and JWKS URIs
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
        .with_body(jwks)
        .create();

    // setup KeyService with the mock OpenID configuration endpoint
    let openid_conf_endpoint = format!("{}/.well-known/openid-configuration", server.url());
    let key_service =
        KeyService::new(vec![&openid_conf_endpoint]).expect("should create key service");

    // initialize JwtService with validation enabled and ES256 as the supported algorithm
    let jwt_service = JwtService::new(DecodingStrategy::WithValidation {
        key_service,
        supported_algs: vec![Algorithm::HS256],
    });

    // assert that the validation fails due to the tokens being signed with an
    // unsupported algorithm
    assert!(jwt_service
        .decode_tokens::<AccessTokenClaims, IdTokenClaims>(&access_token, &id_token)
        .is_err());
    jwks_uri_mock.assert();

    // check if the openid_conf_endpoint got called exactly once
    openid_conf_mock.assert();
}
