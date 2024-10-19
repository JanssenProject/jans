/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

mod utils;

use super::{decoding_strategy::DecodingStrategy, JwtService};
use crate::jwt::key_service::KeyService;
use jsonwebtoken::Algorithm;
use serde_json::json;
use utils::*;

#[test]
/// Tests decoding JWT token claims without validation.
///
/// This test verifies the ability of the `JwtService` to decode claims from both
/// access tokens and ID tokens without performing validation on the token's signature or claims.
/// The decoded claims are compared to the expected claims to ensure correctness.
fn decode_claims_without_validation() {
    // Initialize JwtService with validation disabled
    let service = JwtService::new(DecodingStrategy::WithoutValidation);
    let (encoding_keys, _jwks) = generate_keys();

    // setup claims
    let mut access_token_claims = AccessTokenClaims {
        iss: "https://accounts.google.com".to_string(),
        aud: "some_other_aud".to_string(),
        sub: "some_sub".to_string(),
        scopes: "some_scope".to_string(),
        ..Default::default()
    };
    let mut id_token_claims = IdTokenClaims {
        iss: "https://accounts.facebook.com".to_string(),
        sub: "some_sub".to_string(),
        aud: "some_aud".to_string(),
        email: "some_email@gmail.com".to_string(),
        ..Default::default()
    };

    // Generate tokens with ES256
    let access_token =
        generate_access_token_using_keys(&mut access_token_claims, &encoding_keys, true);
    let id_token = generate_id_token_using_keys(&mut id_token_claims, &encoding_keys, true);

    // Decode Tokens without validation
    let (access_token_result, id_token_result) = service
        .decode_tokens::<AccessTokenClaims, IdTokenClaims>(&access_token, &id_token)
        .expect("should decode token");

    assert_eq!(access_token_result, access_token_claims);
    assert_eq!(id_token_result, id_token_claims);
}

#[test]
/// Tests decoding JWT token claims with validation.
///
/// This test ensures that `JwtService` can decode and validate claims from access tokens
/// and ID tokens when provided with the correct keys and expected algorithms. The tokens
/// are verified against the issuer (`iss`) and audience (`aud`).
fn decode_claims_with_validation() {
    // init mock server
    let mut server = mockito::Server::new();

    // Initialize JwtService with validation enabled
    let (encoding_keys, jwks) = generate_keys();

    // setup claims
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

    // Generate tokens with ES256
    let access_token =
        generate_access_token_using_keys(&mut access_token_claims, &encoding_keys, false);
    let id_token = generate_id_token_using_keys(&mut id_token_claims, &encoding_keys, false);

    // setup mock server responses
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

    // Setup key service
    let openid_conf_endpoint = format!("{}/.well-known/openid-configuration", server.url());
    let key_service =
        KeyService::new(vec![&openid_conf_endpoint]).expect("should create key service");

    // Setup JWT service
    let service = JwtService::new(DecodingStrategy::WithValidation {
        key_service,
        supported_algs: vec![Algorithm::ES256],
    });

    // Decode Tokens with validation
    let (access_token_result, id_token_result) = service
        .decode_tokens::<AccessTokenClaims, IdTokenClaims>(&access_token, &id_token)
        .expect("should decode token");

    assert_eq!(access_token_result, access_token_claims);
    assert_eq!(id_token_result, id_token_claims);

    // check if the paths in the mock server got called
    openid_conf_mock.assert();
    jwks_uri_mock.assert();
}

#[test]
#[should_panic]
/// Tests JWT validation failure due to mismatched audience (`aud`).
///
/// This test ensures that JWT validation will fail when the `aud` (audience) claim in the ID token
/// does not match the expected audience. It is expected to panic if the audience is incorrect.
fn should_not_validate_diff_aud() {
    // init mock server
    let mut server = mockito::Server::new();

    // Initialize JwtService with validation enabled
    let (encoding_keys, jwks) = generate_keys();

    // setup claims
    let mut access_token_claims = AccessTokenClaims {
        iss: "https://accounts.google.com".to_string(),
        aud: "some_aud".to_string(),
        sub: "some_sub".to_string(),
        scopes: "some_scope".to_string(),
        ..Default::default()
    };
    let mut id_token_claims = IdTokenClaims {
        iss: "https://accounts.google.com".to_string(),
        aud: "some_other_aud".to_string(),
        sub: "some_sub".to_string(),
        email: "some_email@gmail.com".to_string(),
        ..Default::default()
    };

    // Generate tokens with ES256
    let access_token =
        generate_access_token_using_keys(&mut access_token_claims, &encoding_keys, false);
    let id_token = generate_id_token_using_keys(&mut id_token_claims, &encoding_keys, false);

    // setup mock server responses
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

    // Setup key service
    let openid_conf_endpoint = format!("{}/.well-known/openid-configuration", server.url());
    let key_service =
        KeyService::new(vec![&openid_conf_endpoint]).expect("should create key service");

    // Setup JWT service
    let service = JwtService::new(DecodingStrategy::WithValidation {
        key_service,
        supported_algs: vec![Algorithm::ES256],
    });

    // Decode Tokens with validation
    service
        .decode_tokens::<AccessTokenClaims, IdTokenClaims>(&access_token, &id_token)
        .expect("should decode token");

    // check if the paths in the mock server got called
    openid_conf_mock.assert();
    jwks_uri_mock.assert();
}

#[test]
#[should_panic]
/// Tests JWT validation failure due to unsupported algorithm.
///
/// This test ensures that JWT validation will panic when the token is signed
/// with an unsupported algorithm. Here, the service is set to support `HS256`,
/// but the token is signed with `ES256`.
fn should_panic_on_unsuppored_alg() {
    // init mock server
    let mut server = mockito::Server::new();

    // Initialize JwtService with validation enabled
    let (encoding_keys, jwks) = generate_keys();

    // setup claims
    let mut access_token_claims = AccessTokenClaims {
        iss: "https://accounts.google.com".to_string(),
        aud: "some_aud".to_string(),
        sub: "some_sub".to_string(),
        scopes: "some_scope".to_string(),
        ..Default::default()
    };
    let mut id_token_claims = IdTokenClaims {
        iss: "https://accounts.google.com".to_string(),
        aud: "some_aud".to_string(),
        sub: "some_sub".to_string(),
        email: "some_email@gmail.com".to_string(),
        ..Default::default()
    };

    // Generate tokens with ES256
    let access_token =
        generate_access_token_using_keys(&mut access_token_claims, &encoding_keys, false);
    let id_token = generate_id_token_using_keys(&mut id_token_claims, &encoding_keys, false);

    // setup mock server responses
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

    // Setup key service
    let openid_conf_endpoint = format!("{}/.well-known/openid-configuration", server.url());
    let key_service =
        KeyService::new(vec![&openid_conf_endpoint]).expect("should create key service");

    // Setup JWT service
    let service = JwtService::new(DecodingStrategy::WithValidation {
        key_service,
        supported_algs: vec![Algorithm::HS256],
    });

    // Decode Tokens with validation
    service
        .decode_tokens::<AccessTokenClaims, IdTokenClaims>(&access_token, &id_token)
        .expect("should decode token");

    // check if the paths in the mock server got called
    openid_conf_mock.assert();
    jwks_uri_mock.assert();
}
