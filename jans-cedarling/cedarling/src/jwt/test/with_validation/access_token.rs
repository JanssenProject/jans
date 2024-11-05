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
//!   - Tests for errors when the `exp` (Expiration) claim is missing.
//!
//! - **Invalid Signature**:
//!   - Tests for errors when the `access_token` has an invalid signature.
//!   - Tests for errors when the `access_token` is expired.
//!   - Tests for errors when the `nbf` has not passed yet.

use super::super::*;
use crate::common::policy_store::TrustedIssuer;
use crate::jwt::decoding_strategy::JwtDecodingError;
use crate::jwt::{self, JwtService, TrustedIssuerAndOpenIdConfig};
use jsonwebtoken::Algorithm;
use serde_json::json;

#[test]
/// Tests that [`JwtService::decode_tokens`] returns an error when the `access_token`
/// is missing an `iss` claim.
fn errors_on_missing_iss() {
    test_missing_claim("iss");
}

#[test]
/// Tests that [`JwtService::decode_tokens`] returns an error when the `access_token`
/// is missing an `aud` claim.
fn errors_on_missing_aud() {
    test_missing_claim("aud");
}

#[test]
/// Tests that [`JwtService::decode_tokens`] returns an error when the `access_token`
/// is missing a `sub` claim.
fn errors_on_missing_sub() {
    test_missing_claim("sub");
}

#[test]
/// Tests that [`JwtService::decode_tokens`] returns an error when the `access_token`
/// is missing an `exp` claim.
fn errors_on_missing_exp() {
    test_missing_claim("exp");
}

fn test_missing_claim(missing_claim: &'static str) {
    // initialize mock server to simulate OpenID configuration and JWKS responses
    let mut server = mockito::Server::new();

    // generate keys and setup the encoding keys and JWKS (JSON Web Key Set)
    let (encoding_keys, jwks) = generate_keys();

    // Invalid access_token claims (missing `iss` field)
    let mut access_token_claims = json!({
        "scopes": "some_scope".to_string(),
        "iat": Timestamp::now(),
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
        supported_algs: vec![Algorithm::ES256],
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

    if let Err(ref e) = decode_result {
        println!("err: {}", e.to_string());
    }

    // the jsonwebtoken crate checks for missing claims differently depending on
    // the claim so we need to split these asserts into two, unfortunately.
    //
    // - the first case is triggered when deserializing the token onto a struct
    // - the second case is triggered when checking if the claim in the token is equal to the
    //   expected value
    if ["iss", "aud"].contains(&missing_claim) {
        let err_string = format!("missing field `{}`", missing_claim);
        assert!(
            matches!(
                decode_result,
                Err(jwt::JwtServiceError::InvalidAccessToken(
                    JwtDecodingError::Validation(ref e)
                )) if matches!(e.kind(), jsonwebtoken::errors::ErrorKind::Json(json_err)
                    if json_err.to_string().contains(&err_string))
            ),
            "Expected decoding to fail due to `access_token` missing a required claim: {:?}",
            decode_result
        );
    // for missing `exp` and `sub`
    } else {
        assert!(
            matches!(
                decode_result,
                Err(jwt::JwtServiceError::InvalidAccessToken(
                    jwt::decoding_strategy::JwtDecodingError::Validation(ref e)
                )) if matches!(
                    e.kind(),
                    jsonwebtoken::errors::ErrorKind::MissingRequiredClaim(req_claim) if req_claim == missing_claim,
                )
            ),
            "Expected decoding to fail due to `access_token` missing a required claim: {:?}",
            decode_result
        );
    }

    // assert that there aren't any additional calls to the mock server
    openid_conf_mock.assert();
    jwks_uri_mock.assert();
}

#[test]
/// Tests that [`JwtService::decode_tokens`] returns an error when the `access_token`
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
    let access_token = generate_token_using_claims(&access_token_claims, &encoding_keys[0]);
    let access_token = invalidate_token(access_token);

    // generate the signed id_token
    let id_token = generate_token_using_claims(&id_token_claims, &encoding_keys[1]);

    // generate the signed userinfo_token
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
        supported_algs: vec![Algorithm::ES256],
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
            Err(jwt::JwtServiceError::InvalidAccessToken(
                jwt::decoding_strategy::JwtDecodingError::Validation(ref e)
            )) if *e.kind() == jsonwebtoken::errors::ErrorKind::InvalidSignature,
        ),
        "Expected decoding to fail due to `access_token` having an invalid signature: {:?}",
        decode_result
    );

    // assert that there aren't any additional calls to the mock server
    openid_conf_mock.assert();
    jwks_uri_mock.assert();
}

#[test]
/// Tests that [`JwtService::decode_tokens`] returns an error when the `access_token` is expired
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
        supported_algs: vec![Algorithm::ES256],
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
            Err(jwt::JwtServiceError::InvalidAccessToken(
                JwtDecodingError::Validation(ref e)
            )) if *e.kind() == jsonwebtoken::errors::ErrorKind::ExpiredSignature,
        ),
        "Expected decoding to fail due to `access_token` being expired: {:?}",
        decode_result
    );

    // assert that there aren't any additional calls to the mock server
    openid_conf_mock.assert();
    jwks_uri_mock.assert();
}

#[test]
/// Tests that [`JwtService::decode_tokens`] returns an error when the `access_token` is used
/// before the `nbf` timestamp
fn errors_on_token_used_before_nbf() {
    // initialize mock server to simulate OpenID configuration and JWKS responses
    let mut server = mockito::Server::new();

    // generate keys and setup the encoding keys and JWKS (JSON Web Key Set)
    let (encoding_keys, jwks) = generate_keys();

    // Invalid access_token claims (nbf has not yet passed)
    let access_token_claims = json!({
        "iss": server.url(),
        "aud": "some_aud".to_string(),
        "sub": "some_sub".to_string(),
        "scopes": "some_scope".to_string(),
        "iat": Timestamp::now(),
        "exp": Timestamp::one_hour_before_now()*2,
        "nbf": Timestamp::one_hour_after_now(),
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
    let decode_result = jwt_service
        .decode_tokens::<serde_json::Value, serde_json::Value, serde_json::Value>(
            &access_token,
            &id_token,
            &userinfo_token,
        );

    assert!(
        matches!(
            decode_result,
            Err(jwt::JwtServiceError::InvalidAccessToken(
                JwtDecodingError::Validation(ref e)
            )) if *e.kind() == jsonwebtoken::errors::ErrorKind::ImmatureSignature,
        ),
        "Expected decoding to fail due to `access_token` being used before the `nbf` timestamp: {:?}",
        decode_result
    );

    // assert that there aren't any additional calls to the mock server
    openid_conf_mock.assert();
    jwks_uri_mock.assert();
}
