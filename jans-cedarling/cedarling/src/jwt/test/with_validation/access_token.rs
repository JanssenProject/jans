/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

//! This module contains negative tests for the `access_token` validation.
//!
//! ## Tests Included
//!
//! - **Missing Claims**:
//!   - Tests for errors when the `iss` (Issuer) claim is missing.
//!   - Tests for errors when the `aud` (Audience) claim is missing.
//!   - Tests for errors when the `sub` (Subject) claim is missing.
//!   - Tests for errors when the `iat` (Issued At) claim is missing.
//!   - Tests for errors when the `exp` (Expiration) claim is missing.
//!
//! - **Invalid Signature**:
//!   - Tests for errors when the `access_token` has an invalid signature.
//!   - Tests for errors when the `access_token` is expired.

use super::super::*;
use crate::common::policy_store::TrustedIssuer;
use crate::jwt::JwtService;
use jsonwebtoken::Algorithm;
use serde_json::json;

#[test]
/// Tests that [`JwtService::decode_claims`] returns an error when the `access_token`
/// is missing an `iss` claim.
fn errors_on_missing_iss() {
    test_missing_claim("iss");
}

#[test]
/// Tests that [`JwtService::decode_claims`] returns an error when the `access_token`
/// is missing an `aud` claim.
fn errors_on_missing_aud() {
    test_missing_claim("aud");
}

#[test]
/// Tests that [`JwtService::decode_claims`] returns an error when the `access_token`
/// is missing a `sub` claim.
fn errors_on_missing_sub() {
    test_missing_claim("sub");
}

#[test]
/// Tests that [`JwtService::decode_claims`] returns an error when the `access_token`
/// is missing an `iat` claim.
fn errors_on_missing_iat() {
    test_missing_claim("iat");
}

#[test]
/// Tests that [`JwtService::decode_claims`] returns an error when the `access_token`
/// is missing an `exp` claim.
fn errors_on_missing_exp() {
    test_missing_claim("exp");
}

fn test_missing_claim(missing_claim: &str) {
    // initialize mock server to simulate OpenID configuration and JWKS responses
    let mut server = mockito::Server::new();

    // generate keys and setup the encoding keys and JWKS (JSON Web Key Set)
    let (encoding_keys, jwks) = generate_keys();

    // Invalid access_token claims (missing `iss` field)
    let mut access_token_claims = json!({
        "scopes": "some_scope".to_string(),
    });

    // add claims incrementally if they're not set to missing
    if missing_claim != "iss" {
        access_token_claims["iss"] = serde_json::Value::String(server.url());
    }
    if missing_claim != "aud" {
        access_token_claims["aud"] = serde_json::Value::String("some_aud".to_string());
    }
    if missing_claim != "sub" {
        access_token_claims["sub"] = serde_json::Value::String("some_sub".to_string());
    }
    if missing_claim != "iat" {
        access_token_claims["iat"] = serde_json::Value::Number(Timestamp::now().into());
    }
    if missing_claim != "exp" {
        access_token_claims["exp"] =
            serde_json::Value::Number(Timestamp::one_hour_after_now().into());
    }

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
        "unexpected": 123123, // a random number used to simulate having unexpected fields in the response
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

    // initialize JwtService with validation enabled and ES256 as the supported algorithm
    let jwt_service = JwtService::new_with_config(crate::jwt::JwtServiceConfig::WithValidation {
        supported_algs: vec![Algorithm::ES256],
        trusted_idps: vec![TrustedIssuer {
            name: "some_idp".to_string(),
            description: "some_desc".to_string(),
            openid_configuration_endpoint: format!(
                "{}/.well-known/openid-configuration",
                server.url()
            ),
            token_metadata: None,
        }],
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

    // assert that decoding resulted in an error due to missing claims
    // TODO: make this error more specific
    assert!(
        decode_result.is_err(),
        "Expected an error due to missing `{}` from `access_token` during token decoding: {:?}",
        missing_claim,
        decode_result.err()
    );

    // assert that there aren't any additional calls to the mock server
    openid_conf_mock.assert();
    jwks_uri_mock.assert();
}

#[test]
/// Tests that [`JwtService::decode_claims`] returns an error when the `access_token`
/// has an invalid signature
fn errors_on_invalid_signature() {
    // initialize mock server to simulate OpenID configuration and JWKS responses
    let mut server = mockito::Server::new();

    // generate keys and setup the encoding keys and JWKS (JSON Web Key Set)
    let (encoding_keys, jwks) = generate_keys();

    // Invalid access_token claims (missing `iss` field)
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
    let mut access_token = generate_token_using_claims(
        &access_token_claims,
        &encoding_keys[0].0,
        &encoding_keys[0].1,
    );
    // invalidate the token's signature by changing it
    let mut token_parts: Vec<&str> = access_token.split('.').collect();
    if token_parts.len() == 3 {
        token_parts[2] = "invalid_signature";
        access_token = token_parts.join(".");
    }

    // generate the signed id_token
    let id_token =
        generate_token_using_claims(&id_token_claims, &encoding_keys[1].0, &encoding_keys[1].1);

    // generate the signed userinfo_token
    let userinfo_token = generate_token_using_claims(
        &userinfo_token_claims,
        &encoding_keys[0].0,
        &encoding_keys[0].1,
    );

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
        .create();
    let jwks_uri_mock = server
        .mock("GET", "/jwks")
        .with_status(200)
        .with_header("content-type", "application/json")
        .with_body(jwks)
        .create();

    // initialize JwtService with validation enabled and ES256 as the supported algorithm
    let jwt_service = JwtService::new_with_config(crate::jwt::JwtServiceConfig::WithValidation {
        supported_algs: vec![Algorithm::ES256],
        trusted_idps: vec![TrustedIssuer {
            name: "some_idp".to_string(),
            description: "some_desc".to_string(),
            openid_configuration_endpoint: format!(
                "{}/.well-known/openid-configuration",
                server.url()
            ),
            token_metadata: None,
        }],
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

    // assert that decoding resulted in an error due to missing claims
    // TODO: make this error more specific
    assert!(
        decode_result.is_err(),
        "Expected an error due to invalid signature from `access_token` during token decoding: {:?}",
        decode_result.err()
    );

    // assert that there aren't any additional calls to the mock server
    openid_conf_mock.assert();
    jwks_uri_mock.assert();
}

#[test]
/// Tests that [`JwtService::decode_claims`] returns an error when the `access_token` is expired
fn errors_on_expired_token() {
    // initialize mock server to simulate OpenID configuration and JWKS responses
    let mut server = mockito::Server::new();

    // generate keys and setup the encoding keys and JWKS (JSON Web Key Set)
    let (encoding_keys, jwks) = generate_keys();

    // Invalid access_token claims (expired)
    let access_token_claims = json!({
        "iss": server.url(),
        "aud": "some_aud".to_string(),
        "sub": "some_sub".to_string(),
        "scopes": "some_scope".to_string(),
        "iat": Timestamp::now(),
        "exp": Timestamp::one_hour_before_now(),
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
        "unexpected": 123123, // a random number used to simulate having unexpected fields in the response
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

    // initialize JwtService with validation enabled and ES256 as the supported algorithm
    let jwt_service = JwtService::new_with_config(crate::jwt::JwtServiceConfig::WithValidation {
        supported_algs: vec![Algorithm::ES256],
        trusted_idps: vec![TrustedIssuer {
            name: "some_idp".to_string(),
            description: "some_desc".to_string(),
            openid_configuration_endpoint: format!(
                "{}/.well-known/openid-configuration",
                server.url()
            ),
            token_metadata: None,
        }],
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

    // assert that decoding resulted in an error due to missing claims
    // TODO: make this error more specific
    assert!(
        decode_result.is_err(),
        "Expected an error due to expired `access_token` during token decoding: {:?}",
        decode_result.err()
    );

    // assert that there aren't any additional calls to the mock server
    openid_conf_mock.assert();
    jwks_uri_mock.assert();
}
