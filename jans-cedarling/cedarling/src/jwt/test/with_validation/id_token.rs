/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

//! This module contains negative tests for the `id_token` validation.
//!
//! ## Tests Included
//! - Tests for errors when the `id_token` has an invalid signature.
//! - Tests for errors when the `id_token` is expired.
//! - Tests for errors when the `nbf` has not passed yet.

use std::collections::HashSet;

use super::super::*;
use crate::common::policy_store::TrustedIssuer;
use crate::jwt::decoding_strategy::JwtDecodingError;
use crate::jwt::{self, HttpClient, JwtService, TrustedIssuerAndOpenIdConfig};
use jsonwebtoken::Algorithm;
use serde_json::json;

#[test]
/// Tests that [`JwtService::decode_tokens`] returns an error when the `id_token`
/// has an invalid signature
fn errors_on_invalid_signature() {
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

    // generate the signed access_token
    let access_token = generate_token_using_claims(&access_token_claims, &encoding_keys[0])
        .expect("Should generate access_token");

    // generate id_token with invalid signature
    let id_token = generate_token_using_claims(&id_token_claims, &encoding_keys[1])
        .expect("Should generate id_token");
    let id_token = invalidate_token(id_token);

    // generate signed userinfo_token
    let userinfo_token = generate_token_using_claims(&userinfo_token_claims, &encoding_keys[0])
        .expect("Should generate userinfo_token");

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
            ..Default::default()
        },
        &HttpClient::new().expect("should create http client"),
    )
    .expect("openid config should be fetched successfully");

    // initialize JwtService with validation enabled and ES256 as the supported algorithm
    let jwt_service = JwtService::new_with_config(crate::jwt::JwtServiceConfig::WithValidation {
        supported_algs: HashSet::from([Algorithm::ES256, Algorithm::HS256]),
        trusted_idps: vec![trusted_idp],
    });

    // key service should fetch the jwks_uri on init
    openid_conf_mock.assert();
    // key service should fetch the jwks on init
    jwks_uri_mock.assert();

    // decode and validate the tokens
    let decode_result = jwt_service
        .decode_tokens::<serde_json::Value, serde_json::Value, serde_json::Value>(
            &access_token,
            &id_token,
            &userinfo_token,
        );

    assert!(
        matches!(
            decode_result,
            Err(jwt::JwtServiceError::InvalidIdToken(
                JwtDecodingError::Validation(ref e)
            )) if *e.kind() == jsonwebtoken::errors::ErrorKind::InvalidSignature,
        ),
        "Expected decoding to fail due to `id_token` having an invalid signature: {:?}",
        decode_result
    );

    // assert that there aren't any additional calls to the mock server
    openid_conf_mock.assert();
    jwks_uri_mock.assert();
}

#[test]
/// Tests that [`JwtService::decode_tokens`] returns an error when the `id_token` is expired
fn errors_on_expired_token() {
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
    // Invalid id_token token claims (expired)
    let id_token_claims = json!({
        "iss": server.url(),
        "aud": "some_aud".to_string(),
        "sub": "some_sub".to_string(),
        "email": "some_email@gmail.com".to_string(),
        "iat": Timestamp::now(),
        "exp": Timestamp::one_hour_before_now(),
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
    let tokens = generate_tokens_using_claims(GenerateTokensArgs {
        access_token_claims,
        id_token_claims,
        userinfo_token_claims,
        encoding_keys,
    });

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
            ..Default::default()
        },
        &HttpClient::new().expect("should create http client"),
    )
    .expect("openid config should be fetched successfully");

    // initialize JwtService with validation enabled and ES256 as the supported algorithm
    let jwt_service = JwtService::new_with_config(crate::jwt::JwtServiceConfig::WithValidation {
        supported_algs: HashSet::from([Algorithm::ES256, Algorithm::HS256]),
        trusted_idps: vec![trusted_idp],
    });

    // key service should fetch the jwks_uri on init
    openid_conf_mock.assert();
    // key service should fetch the jwks on init
    jwks_uri_mock.assert();

    // decode and validate the tokens
    let decode_result = jwt_service
        .decode_tokens::<serde_json::Value, serde_json::Value, serde_json::Value>(
            &tokens.access_token,
            &tokens.id_token,
            &tokens.userinfo_token,
        );

    assert!(
        matches!(
            decode_result,
            Err(jwt::JwtServiceError::InvalidIdToken(
                JwtDecodingError::Validation(ref e)
            )) if matches!(e.kind(), jsonwebtoken::errors::ErrorKind::ExpiredSignature),
        ),
        "Expected decoding to fail due to `id_token` being expired: {:?}",
        decode_result
    );

    // assert that there aren't any additional calls to the mock server
    openid_conf_mock.assert();
    jwks_uri_mock.assert();
}

#[test]
/// Tests that [`JwtService::decode_tokens`] returns an error when the `id_token` is used
/// before the `nbf` timestamp
fn errors_on_token_used_before_nbf() {
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
    // Invalid id_token claims (nbf has not yet passed)
    let id_token_claims = json!({
        "iss": server.url(),
        "aud": "some_aud".to_string(),
        "sub": "some_sub".to_string(),
        "email": "some_email@gmail.com".to_string(),
        "iat": Timestamp::now(),
        "exp": Timestamp::one_hour_after_now()*2,
        "nbf": Timestamp::one_hour_after_now(),
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
    let tokens = generate_tokens_using_claims(GenerateTokensArgs {
        access_token_claims,
        id_token_claims,
        userinfo_token_claims,
        encoding_keys,
    });

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
            ..Default::default()
        },
        &HttpClient::new().expect("should create http client"),
    )
    .expect("openid config should be fetched successfully");

    // initialize JwtService with validation enabled and ES256 as the supported algorithm
    let jwt_service = JwtService::new_with_config(crate::jwt::JwtServiceConfig::WithValidation {
        supported_algs: HashSet::from([Algorithm::ES256, Algorithm::HS256]),
        trusted_idps: vec![trusted_idp],
    });

    // key service should fetch the jwks_uri on init
    openid_conf_mock.assert();
    // key service should fetch the jwks on init
    jwks_uri_mock.assert();

    // decode and validate the tokens
    let decode_result = jwt_service
        .decode_tokens::<serde_json::Value, serde_json::Value, serde_json::Value>(
            &tokens.access_token,
            &tokens.id_token,
            &tokens.userinfo_token,
        );

    assert!(
        matches!(
            decode_result,
            Err(jwt::JwtServiceError::InvalidIdToken(
                JwtDecodingError::Validation(ref e)
            )) if matches!(e.kind(), jsonwebtoken::errors::ErrorKind::ImmatureSignature),
        ),
        "Expected decoding to fail due to `id_token` being used before the `nbf` timestamp: {:?}",
        decode_result
    );

    // assert that there aren't any additional calls to the mock server
    openid_conf_mock.assert();
    jwks_uri_mock.assert();
}
