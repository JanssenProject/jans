/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

mod mock_key_service;
mod utils;

use super::{decoding_strategy::DecodingStrategy, JwtService};
use jsonwebtoken::Algorithm;
use mock_key_service::*;
use std::sync::Arc;
use utils::*;

#[test]
/// Tests the ability to decode claims from a JWT token without validation.
fn can_decode_claims_unvalidated() {
    // Initialize JwtService with validation disabled
    let service = JwtService::new(DecodingStrategy::WithoutValidation);

    // Generate an expired token using ES256
    let (token, _public_key, claims) = generate_token(true);

    // Call decode
    let result = service
        .decode::<Claims>(&token)
        .expect("should decode token");

    assert_eq!(claims, result)
}

#[test]
/// Tests the ability to decode claims from a JWT token with validation.
fn can_decode_claims_validated() {
    // Initialize JwtService with validation enabled
    let (private_keys, jwks) = generate_keys();
    println!("{}", jwks);

    // Generate a token using ES256
    let (token, claims) = generate_token_using_keys(private_keys, false);

    // Setup key service
    let key_service = MockKeyService::new_from_str(&jwks);

    // Setup JWT service
    let jwt_service = JwtService::new(DecodingStrategy::WithValidation {
        key_service: Arc::new(key_service),
        supported_algs: vec![Algorithm::ES256],
    });

    // validate token
    let result = jwt_service
        .decode::<Claims>(&token)
        .expect("should decode token");

    assert_eq!(claims, result);
}
