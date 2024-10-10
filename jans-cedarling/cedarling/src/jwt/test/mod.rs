mod mock_key_service;
mod utils;

use jsonwebkey as jwk;
use jsonwebtoken as jwt;
use std::sync::Arc;

use crate::{jwt::JwtService, JwtConfig};
use mock_key_service::*;
use utils::*;

#[test]
/// Tests the ability to decode claims from a JWT token without validation.
fn can_decode_claims_unvalidated() {
    // Initialize JwtService with validation disabled
    let config = JwtConfig::Disabled;
    let service = JwtService::new(config, None);

    // Generate an expired token using ES256
    let (token, _public_key, claims) = generate_token(jwt::Algorithm::ES256, true);

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
    let algorithm = jwt::Algorithm::ES256;
    let (private_keys, jwks) = generate_keys(jwk::Algorithm::ES256);

    // Generate a token using ES256
    let (token, claims) = generate_token_using_keys(algorithm, private_keys, false);

    // Setup key service
    let key_service = MockKeyService::new_from_str(&jwks);

    // Setup JWT service
    let config = JwtConfig::Disabled;
    let jwt_service = JwtService::new(config, Some(Arc::new(key_service)));

    // validate token
    let result = jwt_service
        .decode::<Claims>(&token)
        .expect("should decode token");

    assert_eq!(claims, result);
}
