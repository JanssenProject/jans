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
/// Tests decoding and validating JWT token claims with a mock server and key service.
///
/// This test ensures that `JwtService` can decode and validate claims from both access tokens
/// and ID tokens when provided with correct keys and expected algorithms (e.g., ES256). It also
/// verifies tokens against the issuer (`iss`) and audience (`aud`) claims, using a mock OpenID
/// configuration endpoint and JWKS (JSON Web Key Set).
///
/// The test workflow includes:
/// 1. Initializing a mock server to simulate OpenID and JWKS endpoints.
/// 2. Generating access and ID tokens using the ES256 algorithm.
/// 3. Setting up mock responses for the OpenID configuration and JWKS URIs.
/// 4. Using `JwtService` to decode and validate the tokens.
/// 5. Asserting that the decoded claims match the expected claims, and that the mock endpoints
///    are called exactly once.
fn can_decode_claims_with_validation() {
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
        sub: "some_sub".to_string(),
        aud: "some_aud".to_string(),
        email: "some_email@gmail.com".to_string(),
        ..Default::default()
    };

    // generate the access token and ID token using ES256 algorithm and encoding keys
    let access_token =
        generate_access_token_using_keys(&mut access_token_claims, &encoding_keys, false);
    let id_token = generate_id_token_using_keys(&mut id_token_claims, &encoding_keys, false);
    // TODO: add correct implementation for userinfo token
    let userinfo_token = generate_id_token_using_keys(&mut id_token_claims, &encoding_keys, false);

    // setup mock server responses for OpenID configuration and JWKS URIs
    let openid_config_response = json!({
        "issuer": server.url(),
        "jwks_uri": &format!("{}/jwks", server.url()),
        "unexpected": 123123, // a random number used to simulate unexpected data in the response
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
        supported_algs: vec![Algorithm::ES256],
    });

    // decode and validate both the access token and the ID token
    let (access_token_result, id_token_result, _userinfo_token_result) = jwt_service
        .decode_tokens::<AccessTokenClaims, IdTokenClaims, UserInfoTokenClaims>(
            &access_token,
            &id_token,
            &userinfo_token,
        )
        .expect("should decode token");
    jwks_uri_mock.assert();

    // assert that the decoded token claims match the expected claims
    assert_eq!(access_token_result, access_token_claims);
    assert_eq!(id_token_result, id_token_claims);

    // verify openid configuration endpoint was called once
    openid_conf_mock.assert();
}
