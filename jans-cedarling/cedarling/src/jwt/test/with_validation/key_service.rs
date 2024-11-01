//! This module contains negative tests for the validation involving
//! [`jwt::decoding_strategy::KeyService`]
//!
//! ## Tests Included
//!
//! - Test for missing keys

use super::super::*;
use crate::common::policy_store::TrustedIssuer;
use crate::jwt::{self, JwtService};
use jsonwebtoken::Algorithm;
use serde_json::json;

#[test]
/// Tests if [`JwtService::decode_tokens`] fails if the `kid` of the tokens are not found
/// in the JWKS
fn errors_when_no_key_found() {
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
    let access_token = generate_token_using_claims(
        &access_token_claims,
        "some_key_id_not_in_the_jwks", // we set an non-existing `kid` for the access_token
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

    assert!(
        matches!(
            decode_result,
            Err(jwt::Error::InvalidAccessToken(
                jwt::decoding_strategy::Error::KeyService(jwt::decoding_strategy::key_service::Error::KeyNotFound(ref e))
            )) if **e == *"some_key_id_not_in_the_jwks",
        ),
        "Expected decoding to fail due to not being able to find a key to validate `access_token`: {:?}",
        decode_result
    );
    // key service should fetch the jwks again when it cant find the `kid` for the access_token
    let jwks_uri_mock = jwks_uri_mock.expect(2);
    jwks_uri_mock.assert();

    // assert that there aren't any additional calls to the openid_config_uri
    openid_conf_mock.assert();
}

#[test]
#[should_panic]
/// Tests if [`JwtService::new_with_config`] panics if the JWKS cant be fetched
/// from the `jwks_uri`.
///
/// TODO: change this to check for an error instead of a panic once returning a `Result` for
/// the initialization for JwtService becomes supported
fn panics_when_cant_fetch_jwks_uri() {
    // initialize mock server to simulate OpenID configuration and JWKS responses
    let mut server = mockito::Server::new();

    // setup mock server responses for OpenID configuration and JWKS URIs
    //
    // NOTE: we don't add a response for the `jwks_uri` so that we can simulate the endpoint being
    // unreachable
    let openid_config_response = json!({
        "issuer": server.url(),
        "jwks_uri": &format!("{}/jwks", server.url()),
        "unexpected": 123123, // a random number used to simulate having unexpected fields in the response
    });
    let _openid_conf_mock = server
        .mock("GET", "/.well-known/openid-configuration")
        .with_status(200)
        .with_header("content-type", "application/json")
        .with_body(openid_config_response.to_string())
        .create();

    // initialize JwtService with validation enabled and ES256 as the supported algorithm
    let _jwt_service = JwtService::new_with_config(crate::jwt::JwtServiceConfig::WithValidation {
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
}

#[test]
#[should_panic]
/// Tests if [`JwtService::new_with_config`] panics if the openid_configuration
/// cant be fetched from the `openid_configuration_endpoint`
///
/// TODO: change this to check for an error instead of a panic once returning a `Result` for
/// the initialization for JwtService becomes supported
fn panics_when_cant_fetch_openid_configuration() {
    // initialize mock server to simulate OpenID configuration and JWKS responses
    let mut server = mockito::Server::new();

    let _openid_conf_mock = server
        .mock("GET", "/.well-known/openid-configuration")
        .with_status(500)
        .create();

    // initialize JwtService with validation enabled and ES256 as the supported algorithm
    let _jwt_service = JwtService::new_with_config(crate::jwt::JwtServiceConfig::WithValidation {
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
}
