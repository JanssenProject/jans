/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::{super::*, *};

#[test]
/// Tests decoding JWT token claims without validation.
///
/// This test verifies the ability of the `JwtService` to decode claims from both
/// access tokens and ID tokens without performing validation on the token's signature or claims.
/// The decoded claims are compared to the expected claims to ensure correctness.
fn can_decode_claims_without_validation() {
    // initialize JwtService with validation disabled
    let jwt_service = JwtService::new(DecodingStrategy::WithoutValidation);

    // generate keys and setup the encoding keys and JWKS (JSON Web Key Set)
    let (encoding_keys, _jwks) = generate_keys();

    // setup claims for access token and ID token
    let access_token_claims = AccessTokenClaims {
        iss: "https://accounts.google.com".to_string(),
        aud: "some_other_aud".to_string(),
        sub: "some_sub".to_string(),
        scopes: "some_scope".to_string(),
        exp: Timestamp::now(),
        iat: Timestamp::one_hour_before_now(), // expired token
    };
    let id_token_claims = IdTokenClaims {
        aud: "some_aud".to_string(),
        iss: "https://accounts.facebook.com".to_string(),
        sub: "some_sub".to_string(),
        email: "some_email@gmail.com".to_string(),
        exp: Timestamp::now(),
        iat: Timestamp::one_hour_before_now(), // expired token
    };
    let userinfo_token_claims = UserinfoTokenClaims {
        aud: "some_other_aud".to_string(),
        iss: "https://accounts.youtube.com".to_string(),
        sub: "another_sub".to_string(),
        client_id: "some_client_id".to_string(),
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
        generate_token_using_claims(&id_token_claims, &encoding_keys[0].0, &encoding_keys[0].1);
    let userinfo_token = generate_token_using_claims(
        &userinfo_token_claims,
        &encoding_keys[0].0,
        &encoding_keys[0].1,
    );

    // decode and validate both the access token and the ID token
    let (access_token_result, id_token_result, userinfo_token_result) = jwt_service
        .decode_tokens(&access_token, &id_token, &userinfo_token)
        .expect("should decode token");

    // assert that the decoded token claims match the expected claims
    assert_eq!(access_token_result.claims(), access_token_claims.into());
    assert_eq!(id_token_result.claims(), id_token_claims.into());
    assert_eq!(userinfo_token_result.claims(), userinfo_token_claims.into());
}
