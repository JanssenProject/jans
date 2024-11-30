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

use std::collections::HashSet;

use super::*;
use crate::common::policy_store::TrustedIssuer;
use crate::jwt::{self, HttpClient, JwtService, JwtServiceError, TrustedIssuerAndOpenIdConfig};
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
    let tokens = generate_tokens_using_claims(GenerateTokensArgs {
        access_token_claims: access_token_claims.clone(),
        id_token_claims: id_token_claims.clone(),
        userinfo_token_claims: userinfo_token_claims.clone(),
        encoding_keys: encoding_keys.clone(),
    });

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
    let result = jwt_service
        .decode_tokens::<serde_json::Value, serde_json::Value, serde_json::Value>(
            &tokens.access_token,
            &tokens.id_token,
            &tokens.userinfo_token,
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
        supported_algs: HashSet::from([Algorithm::HS256]),
        trusted_idps: vec![trusted_idp],
    });

    // assert that the validation fails due to the tokens being signed with an
    // unsupported algorithm
    let validation_result = jwt_service
        .decode_tokens::<serde_json::Value, serde_json::Value, serde_json::Value>(
            &tokens.access_token,
            &tokens.id_token,
            &tokens.userinfo_token,
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

#[test]
/// Tests if receiving keys using unsupported algorithms from a JWKS gets handled gracefully
fn can_gracefully_handle_unsupported_algorithms_from_jwks() {
    /// A struct used for manually editing the JSON Web Key Set (JWKS) in the mock response
    #[derive(serde::Deserialize, serde::Serialize)]
    struct Jwks {
        keys: Vec<serde_json::Value>,
    }

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
        "email": "some_email@gluu.org".to_string(),
        "iat": Timestamp::now(),
        "exp": Timestamp::one_hour_after_now(),
    });
    // Valid userinfo_token claims
    let userinfo_token_claims = json!({
        "iss": server.url(),
        "aud": "some_aud".to_string(),
        "sub": "some_sub".to_string(),
        "client_id": "some_aud".to_string(),
        "name": "admin".to_string(),
        "email": "some_email@gluu.org".to_string(),
        "iat": Timestamp::now(),
        "exp": Timestamp::one_hour_after_now(),
    });

    // generate the signed token strings
    let access_token = generate_token_using_claims(&access_token_claims, &encoding_keys[1])
        .expect("should generate access_token");
    let id_token = generate_token_using_claims(&id_token_claims, &encoding_keys[1])
        .expect("should generate id_token");
    let userinfo_token = generate_token_using_claims(&userinfo_token_claims, &encoding_keys[1])
        .expect("should generate userinfo_token");

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
        .expect_at_least(1)
        .expect_at_most(2)
        .create();

    // we insert an unknown variant here to simulate getting a key with an unknown algorithm from
    // the trusted issuer
    let mut jwks = serde_json::from_str::<Jwks>(&jwks).unwrap();
    jwks.keys.push(json!({
        "kty": "EC",
        "use": "sig",
        "key_ops_type": [],
        "crv": "P-521",
        "kid": "connect_190362b7-efca-4674-9cb7-21b428cb682a_sig_es512",
        "x5c": [
            "MIICBjCCAWegAwIBAgIhALe16fd76pin3igeUTiLhGW01wkEMVzBsmGdXVtYpeZuMAoGCCqGSM49BAMEMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjQxMDE5MTg1NzMyWhcNMjQxMDIxMTk1NzMxWjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQAf4TdXH7umWW64g1w8+UZ0NhyRm6rWsRGL7E+bvS2cY+K6UPThM7/xy9nTs73Pw8OT26oUhBz1oM9Jhs0Qy/veXMAvgHuUeIT6CBV3aHr4osWFAnGwoh0pjd1NOU3TN+ms1ttcD1qyJcZxLOhvFr3VZ7/7p5gSOaY1MwEEG2Ka/itQTujJzAlMCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADAKBggqhkjOPQQDBAOBjAAwgYgCQgGBq8DEjIF1SwqFos+2mHA6XFO+pZfx9HESd8dUZxN3yA5yf1oFxhUCbviQeOCeATAITuEfSIIL8hAQ4uzQc7JYhgJCAfB8/JGumVAnU/3lx2aHVl8hpSXn/f2107VN4ld46dwy3r48Ioo8dfjN2dH0BOKNg2ddYPiORfrpI9Y/WF7vI4UT"
        ],
        "x": "f4TdXH7umWW64g1w8-UZ0NhyRm6rWsRGL7E-bvS2cY-K6UPThM7_xy9nTs73Pw8OT26oUhBz1oM9Jhs0Qy_veXM",
        "y": "vgHuUeIT6CBV3aHr4osWFAnGwoh0pjd1NOU3TN-ms1ttcD1qyJcZxLOhvFr3VZ7_7p5gSOaY1MwEEG2Ka_itQTs",
        "exp": 2729540651438u64,
        "alg": "ES512"
    }));
    let jwks = serde_json::to_string(&jwks).unwrap();
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
        supported_algs: HashSet::from([Algorithm::HS256]),
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
