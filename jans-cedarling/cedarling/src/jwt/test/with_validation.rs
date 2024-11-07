/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

//! This test module includes tests for when validation is off.

mod access_token;
mod id_token;
mod key_service;
mod userinfo_token;

use super::*;
use crate::common::policy_store::TrustedIssuer;
use crate::jwt::{self, JwtService, JwtServiceError, TrustedIssuerAndOpenIdConfig};
use jsonwebtoken::Algorithm;
use serde_json::json;

#[test]
/// Tests if [`JwtService::decode_tokens`] can successfully decode tokens valid claims
fn can_decode_claims_with_validation() {
    // initialize mock server to simulate OpenID configuration and JWKS responses
    let mut server = mockito::Server::new();

    // generate keys and setup the encoding keys and JWKS (JSON Web Key Set)
    let (encoding_keys, jwks) = generate_keys();

    // Valid access_token claims
    let access_token_claims = json!({
        "iss": server.url(),
        "aud": "some_aud".to_string(),
        "sub": "some_sub".to_string(),
        "scopes": "some_scope".to_string(),
        "iat": Timestamp::now(),
        "exp": Timestamp::one_hour_after_now(),
    });
    // Valid id_token token claims
    let id_token_claims = json!({
        "iss": server.url(),
        "aud": "some_aud".to_string(),
        "sub": "some_sub".to_string(),
        "email": "some_email@gmail.com".to_string(),
        "iat": Timestamp::now(),
        "exp": Timestamp::one_hour_after_now(),
    });
    // Valid userinfo_token claims
    let userinfo_token_claims = json!({
        "iss": server.url(),
        "aud": "some_aud".to_string(),
        "sub": "some_sub".to_string(),
        "client_id": "some_aud".to_string(),
        "name": "ferris".to_string(),
        "email": "ferris@gluu.com".to_string(),
        "iat": Timestamp::now(),
        "exp": Timestamp::one_hour_after_now(),
    });

    // generate the signed token strings
    let access_token = generate_token_using_claims(&access_token_claims, &encoding_keys[0]);
    let id_token = generate_token_using_claims(&id_token_claims, &encoding_keys[1]);
    let userinfo_token = generate_token_using_claims(&userinfo_token_claims, &encoding_keys[0]);

    // setup mock server responses for OpenID configuration and JWKS URIs
    let openid_config_response = json!({
        "issuer": server.url(),
        "jwks_uri": &format!("{}/jwks", server.url()),
        "unexpected": 123123, // a random number used to simulate having unexpected fields in the response
    });
    let openid_conf_mock = server
        .mock("GET", "/.well-known/openid-configuration")
        .with_status(200)
        .with_header("content-type", "application/json")
        .with_body(openid_config_response.to_string())
        .expect_at_least(1)
        .expect_at_most(2)
        .create();
    let jwks_uri_mock = server
        .mock("GET", "/jwks")
        .with_status(200)
        .with_header("content-type", "application/json")
        .with_body(jwks)
        .expect_at_least(1)
        .expect_at_most(2)
        .create();

    let trusted_idp = TrustedIssuerAndOpenIdConfig::fetch(
        TrustedIssuer {
            name: "some_idp".to_string(),
            description: "some_desc".to_string(),
            openid_configuration_endpoint: format!(
                "{}/.well-known/openid-configuration",
                server.url()
            ),
            token_metadata: None,
        },
        &reqwest::blocking::Client::new(),
    )
    .expect("openid config should be fetched successfully");

    // initialize JwtService with validation enabled and ES256 as the supported algorithm
    let jwt_service = JwtService::new_with_config(crate::jwt::JwtServiceConfig::WithValidation {
        supported_algs: vec![Algorithm::ES256],
        trusted_idps: vec![trusted_idp],
    });

    // key service should fetch the jwks_uri on init
    openid_conf_mock.assert();
    // key service should fetch the jwks on init
    jwks_uri_mock.assert();

    // decode and validate the tokens
    let result = jwt_service
        .decode_tokens::<serde_json::Value, serde_json::Value, serde_json::Value>(
            &access_token,
            &id_token,
            &userinfo_token,
        )
        .expect("should decode token");

    // assert that the decoded token claims match the input claims
    assert_eq!(
        result.access_token, access_token_claims,
        "decoded access_token claims did not match the input claims"
    );
    assert_eq!(
        result.id_token, id_token_claims,
        "decoded id_token claims did not match the input claims"
    );
    assert_eq!(
        result.userinfo_token, userinfo_token_claims,
        "decoded id_token claims did not match the input claims"
    );

    // assert that there aren't any additional calls to the mock server
    openid_conf_mock.assert();
    jwks_uri_mock.assert();
}

#[test]
/// Tests if JWT validation fails due to unsupported algorithm.
///
/// This test verifies that the `JwtService` correctly fails validation when the token
/// is signed with an unsupported algorithm. the service is configured to support `HS256`,
/// but the token is signed with `ES256`.
fn errors_on_unsupported_alg() {
    // initialize mock server to simulate OpenID configuration and JWKS responses
    let mut server = mockito::Server::new();

    // generate keys and setup the encoding keys and JWKS (JSON Web Key Set)
    let (encoding_keys, jwks) = generate_keys();

    // setup claims for access token and ID token
    let access_token_claims = json!({
        "iss": server.url(),
        "aud": "some_aud".to_string(),
        "sub": "some_sub".to_string(),
        "scopes": "some_scope".to_string(),
        "iat": Timestamp::now(),
        "exp": Timestamp::one_hour_after_now(),
    });
    let id_token_claims = json!({
        "iss": server.url(),
        "sub": "some_sub".to_string(),
        "aud": "some_aud".to_string(),
        "email": "some_email@gmail.com".to_string(),
        "iat": Timestamp::now(),
        "exp": Timestamp::one_hour_after_now(),
    });
    let userinfo_token_claims = json!({
        "sub": "some_sub".to_string(),
        "client_id": "some_client_id".to_string(),
        "name": "ferris".to_string(),
        "email": "ferris@gluu.com".to_string(),
    });

    // generate the signed token strings
    let access_token = generate_token_using_claims(&access_token_claims, &encoding_keys[0]);
    let id_token = generate_token_using_claims(&id_token_claims, &encoding_keys[1]);
    let userinfo_token = generate_token_using_claims(&userinfo_token_claims, &encoding_keys[0]);

    // setup mock server responses for OpenID configuration and JWKS URIs
    let openid_config_response = json!({
        "issuer": server.url(),
        "jwks_uri": &format!("{}/jwks", server.url()),
    });
    let openid_conf_mock = server
        .mock("GET", "/.well-known/openid-configuration")
        .with_status(200)
        .with_header("content-type", "application/json")
        .with_body(openid_config_response.to_string())
        .expect_at_least(1)
        .expect_at_most(2)
        .create();
    let jwks_uri_mock = server
        .mock("GET", "/jwks")
        .with_status(200)
        .with_header("content-type", "application/json")
        .with_body(jwks)
        .expect_at_least(1)
        .expect_at_most(2)
        .create();

    let trusted_idp = TrustedIssuerAndOpenIdConfig::fetch(
        TrustedIssuer {
            name: "some_idp".to_string(),
            description: "some_desc".to_string(),
            openid_configuration_endpoint: format!(
                "{}/.well-known/openid-configuration",
                server.url()
            ),
            token_metadata: None,
        },
        &reqwest::blocking::Client::new(),
    )
    .expect("openid config should be fetched successfully");

    // initialize JwtService with validation enabled and ES256 as the supported algorithm
    let jwt_service = JwtService::new_with_config(crate::jwt::JwtServiceConfig::WithValidation {
        supported_algs: vec![Algorithm::HS256],
        trusted_idps: vec![trusted_idp],
    });

    // assert that the validation fails due to the tokens being signed with an
    // unsupported algorithm
    let validation_result = jwt_service
        .decode_tokens::<serde_json::Value, serde_json::Value, serde_json::Value>(
            &access_token,
            &id_token,
            &userinfo_token,
        );
    assert!(
        matches!(
            validation_result,
            Err(JwtServiceError::InvalidAccessToken(
                jwt::decoding_strategy::JwtDecodingError::TokenSignedWithUnsupportedAlgorithm(
                    jsonwebtoken::Algorithm::ES256
                )
            ))
        ),
        "Validation expected to failed due to unsupported algorithm"
    );
    jwks_uri_mock.assert();

    // check if the openid_conf_endpoint got called exactly once
    openid_conf_mock.assert();
}
