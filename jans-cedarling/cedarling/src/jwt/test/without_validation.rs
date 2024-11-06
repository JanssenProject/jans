/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use crate::jwt::JwtServiceConfig;

use super::{super::JwtService, *};
use serde_json::json;

#[test]
/// Tests decoding JWT token claims without validation.
///
/// This test verifies the ability of the `JwtService` to decode claims from both
/// access tokens and ID tokens without performing validation on the token's signature or claims.
/// The decoded claims are compared to the expected claims to ensure correctness.
fn can_decode_claims_without_validation() {
    // initialize JwtService with validation disabled
    let jwt_service = JwtService::new_with_config(JwtServiceConfig::WithoutValidation {
        trusted_idps: Vec::new(),
    });

    // generate keys and setup the encoding keys and JWKS (JSON Web Key Set)
    let (encoding_keys, _jwks) = generate_keys();

    // setup claims for access token and ID token
    let access_token_claims = json!({
        "iss": "https://accounts.google.com".to_string(),
        "aud": "some_other_aud".to_string(),
        "sub": "some_sub".to_string(),
        "scopes": "some_scope".to_string(),
        "exp": Timestamp::now(),
        "iat": Timestamp::one_hour_before_now(), // expired token
    });
    let id_token_claims = json!({
        "iss": "https://accounts.facebook.com".to_string(),
        "sub": "some_sub".to_string(),
        "aud": "some_aud".to_string(),
        "email": "some_email@gmail.com".to_string(),
        "exp": Timestamp::now(),
        "iat": Timestamp::one_hour_before_now(), // expired token
    });
    let userinfo_token_claims = json!({
        "sub": "another_sub".to_string(),
        "client_id": "some_client_id".to_string(),
        "name": "ferris".to_string(),
        "email": "ferris@gluu.com".to_string(),
    });

    // generate the signed token strings
    let tokens = generate_tokens_using_claims(GenerateTokensArgs {
        access_token_claims: access_token_claims.clone(),
        id_token_claims: id_token_claims.clone(),
        userinfo_token_claims: userinfo_token_claims.clone(),
        encoding_keys,
    });

    // decode and validate both the access token and the ID token
    let result = jwt_service
        .decode_tokens::<serde_json::Value, serde_json::Value, serde_json::Value>(
            &tokens.access_token,
            &tokens.id_token,
            &tokens.userinfo_token,
        )
        .expect("should decode token");

    // assert that the decoded token claims match the expected claims
    assert_eq!(result.access_token, access_token_claims);
    assert_eq!(result.id_token, id_token_claims);
    assert_eq!(result.userinfo_token, userinfo_token_claims);
}
