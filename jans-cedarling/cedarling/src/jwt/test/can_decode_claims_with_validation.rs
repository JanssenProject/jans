/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::{super::*, *};
use crate::jwt::decoding_strategy::KeyService;
use crate::jwt::JsonWebToken;
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

    // setup token claims
    let access_token_claims = AccessTokenClaims {
        iss: server.url(),
        aud: "some_aud".to_string(),
        sub: "some_sub".to_string(),
        scopes: "some_scope".to_string(),
        iat: Timestamp::now(),
        exp: Timestamp::one_hour_after_now(),
    };
    let id_token_claims = IdTokenClaims {
        iss: server.url(),
        aud: "some_aud".to_string(),
        sub: "some_sub".to_string(),
        email: "some_email@gmail.com".to_string(),
        iat: Timestamp::now(),
        exp: Timestamp::one_hour_after_now(),
    };
    let userinfo_token_claims = UserinfoTokenClaims {
        iss: server.url(),
        aud: "some_aud".to_string(),
        sub: "some_sub".to_string(),
        client_id: "some_aud".to_string(),
        name: "ferris".to_string(),
        email: "ferris@gluu.com".to_string(),
    };

    // generate the signed token strings
    let access_token = generate_token_using_claims(
        &access_token_claims,
        &encoding_keys[0].0,
        &encoding_keys[0].1,
    );
    let id_token =
        generate_token_using_claims(&id_token_claims, &encoding_keys[1].0, &encoding_keys[1].1);
    let userinfo_token = generate_token_using_claims(
        &userinfo_token_claims,
        &encoding_keys[0].0,
        &encoding_keys[0].1,
    );

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

    // decode and validate the tokens
    let (access_token_result, id_token_result, userinfo_token_result) = jwt_service
        .decode_tokens(&access_token, &id_token, &userinfo_token)
        .expect("should decode token");
    jwks_uri_mock.assert();

    // assert that the decoded token claims match the expected claims
    assert_eq!(access_token_result.claims(), access_token_claims.into());
    assert_eq!(id_token_result.claims(), id_token_claims.into());
    assert_eq!(userinfo_token_result.claims(), userinfo_token_claims.into());

    // verify openid configuration endpoint was called once
    openid_conf_mock.assert();
}
