/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::{super::decoding_strategy::DecodingStrategy, super::JwtService, *};

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

    // generate the access token and ID token using ES256 algorithm and encoding keys
    let access_token =
        generate_access_token_using_keys(&mut access_token_claims, &encoding_keys, true);
    let id_token = generate_id_token_using_keys(&mut id_token_claims, &encoding_keys, true);

    // decode and validate both the access token and the ID token
    let (access_token_result, id_token_result) = jwt_service
        .decode_tokens::<AccessTokenClaims, IdTokenClaims>(&access_token, &id_token)
        .expect("should decode token");

    // assert that the decoded token claims match the expected claims
    assert_eq!(access_token_result, access_token_claims);
    assert_eq!(id_token_result, id_token_claims);
}
