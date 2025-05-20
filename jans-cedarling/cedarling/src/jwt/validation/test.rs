// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashSet;
use std::sync::LazyLock;

use jsonwebtoken::Algorithm;
use serde_json::json;
use test_utils::assert_eq;

use super::super::test_utils::*;
use crate::common::policy_store::{ClaimMappings, TokenEntityMetadata};
use crate::jwt::validation::{JwtValidator, ValidateJwtError, ValidatedJwt};

#[track_caller]
fn generate_keys() -> KeyPair {
    let keys = generate_keypair_hs256(Some("some_hs256_key")).expect("Should generate keys");
    keys
}

static TEST_TKN_ENTITY_METADATA: LazyLock<TokenEntityMetadata> =
    LazyLock::new(|| TokenEntityMetadata {
        trusted: true,
        entity_type_name: "Jans::AccessToken".into(),
        principal_mapping: HashSet::new(),
        token_id: "jti".into(),
        user_id: None,
        role_mapping: None,
        workload_id: None,
        claim_mapping: ClaimMappings::default(),
        required_claims: HashSet::from(["exp".into(), "nbf".into()]),
    });

#[test]
fn can_decode_jwt_without_sig_validation() {
    let keys = generate_keys();
    let iss = "127.0.0.1".to_string();

    // Generate token
    let claims = json!({
        "iss": iss,
        "sub": "1234567890",
        "name": "John Doe",
        "iat": 1516239022,
        "exp": u64::MAX,
        "nbf": u64::MIN,
    });
    let token =
        generate_token_using_claims(&claims, &keys).expect("Should generate token using keys");
    let decoding_key = keys.decoding_key().unwrap();

    let (validator, _) = JwtValidator::new(
        Some(iss),
        "access_token".into(),
        &TEST_TKN_ENTITY_METADATA,
        Algorithm::HS256,
    );

    let result = validator
        .validate_jwt(&token, &decoding_key)
        .expect("should validate JWT");

    let expected = ValidatedJwt {
        claims,
        trusted_iss: None,
    };

    assert_eq!(result, expected);
}

#[test]
fn decoding_errors_if_token_is_expired_when_without_sig_validation() {
    let iss = "127.0.0.1".to_string();
    let keys = generate_keys();

    // Generate token
    let claims = json!({
        "iss": iss,
        "sub": "1234567890",
        "name": "John Doe",
        "iat": 1516239022,
        "exp": 0,
    });
    let token =
        generate_token_using_claims(&claims, &keys).expect("Should generate token using keys");
    let decoding_key = keys.decoding_key().unwrap();

    let mut tkn_entity_metadata = TEST_TKN_ENTITY_METADATA.clone();
    tkn_entity_metadata.required_claims = HashSet::from(["exp".into()]);
    let (validator, _) = JwtValidator::new(
        Some(iss),
        "access_token".into(),
        &TEST_TKN_ENTITY_METADATA,
        Algorithm::HS256,
    );

    let err = validator
        .validate_jwt(&token, &decoding_key)
        .expect_err("should error due to expired JWT");
}

#[test]
fn can_decode_and_validate_jwt() {
    let iss = "127.0.0.1".to_string();
    let keys = generate_keys();

    let claims = json!({
        "iss": iss,
        "sub": "1234567890",
        "name": "John Doe",
        "iat": 0,
        "nbf": 10,
        "exp": u64::MAX,
    });
    let token =
        generate_token_using_claims(&claims, &keys).expect("Should generate token using keys");
    let decoding_key = keys.decoding_key().unwrap();

    let (validator, _) = JwtValidator::new(
        Some(iss),
        "access_token".into(),
        &TEST_TKN_ENTITY_METADATA,
        Algorithm::HS256,
    );

    let result = validator
        .validate_jwt(&token, &decoding_key)
        .expect("Should successfully process JWT");

    let expected = ValidatedJwt {
        claims,
        trusted_iss: None,
    };

    assert_eq!(result, expected);
}

#[test]
fn errors_on_expired_token() {
    let iss = "127.0.0.1".to_string();
    let keys = generate_keys();

    // Generate token
    let claims = json!({
        "iss": iss,
        "sub": "1234567890",
        "name": "John Doe",
        "iat": 1516239022,
        "exp": 0,
    });
    let token =
        generate_token_using_claims(&claims, &keys).expect("Should generate token using keys");
    let decoding_key = keys.decoding_key().unwrap();

    let mut tkn_entity_metadata = TEST_TKN_ENTITY_METADATA.clone();
    tkn_entity_metadata.required_claims = HashSet::from(["exp".into(), "nbf".into()]);
    let (validator, _) = JwtValidator::new(
        Some(iss),
        "access_token".into(),
        &TEST_TKN_ENTITY_METADATA,
        Algorithm::HS256,
    );

    let err = validator
        .validate_jwt(&token, &decoding_key)
        .expect_err("should error when validating JWT");

    assert!(
        matches!(
            err,
            ValidateJwtError::ValidateJwt(ref e)
                if *e.kind() == jsonwebtoken::errors::ErrorKind::ExpiredSignature
        ),
        "expected validation to fail due to the token being expired."
    );
}

#[test]
fn errors_on_immature_token() {
    let iss = "127.0.0.1".to_string();
    let keys = generate_keys();

    // Generate token
    let claims = json!({
        "iss": iss,
        "sub": "1234567890",
        "name": "John Doe",
        "iat": 1516239022,
        "nbf": u64::MAX,
    });
    let token =
        generate_token_using_claims(&claims, &keys).expect("Should generate token using keys");
    let decoding_key = keys.decoding_key().unwrap();

    let (validator, _) = JwtValidator::new(
        Some(iss),
        "access_token".into(),
        &TEST_TKN_ENTITY_METADATA,
        Algorithm::HS256,
    );

    let err = validator
        .validate_jwt(&token, &decoding_key)
        .expect_err("should error when validating JWT");

    assert!(
        matches!(
            err,
            ValidateJwtError::ValidateJwt(ref e)
                if *e.kind() == jsonwebtoken::errors::ErrorKind::ImmatureSignature
        ),
        "expected validation to fail due to the token being immature."
    );
}

#[test]
fn can_check_missing_claims() {
    let iss = "127.0.0.1".to_string();
    let keys = generate_keys();

    // Generate token
    let claims = json!({
        "iss": iss,
        "sub": "1234567890",
        "name": "John Doe",
        "iat": 1516239022,
    });
    let token =
        generate_token_using_claims(&claims, &keys).expect("Should generate token using keys");
    let decoding_key = keys.decoding_key().unwrap();

    // Base case where all required claims are present
    let mut tkn_entity_metadata = TEST_TKN_ENTITY_METADATA.clone();
    tkn_entity_metadata.required_claims = HashSet::from(["sub", "name", "iat"].map(|x| x.into()));
    let (validator, _) = JwtValidator::new(
        Some(iss.clone()),
        "access_token".into(),
        &tkn_entity_metadata,
        Algorithm::HS256,
    );

    let result = validator
        .validate_jwt(&token, &decoding_key)
        .expect("Should process JWT successfully");

    let expected = ValidatedJwt {
        claims,
        trusted_iss: None,
    };

    assert_eq!(result, expected);

    // Error case where `nbf` is missing from the token.
    let mut tkn_entity_metadata = TEST_TKN_ENTITY_METADATA.clone();
    tkn_entity_metadata.required_claims =
        HashSet::from(["sub", "name", "iat", "nbf"].map(|x| x.into()));
    let (validator, _) = JwtValidator::new(
        Some(iss.clone()),
        "access_token".into(),
        &tkn_entity_metadata,
        Algorithm::HS256,
    );

    let err = validator
        .validate_jwt(&token, &decoding_key)
        .expect_err("expected an error while validating the JWT");

    assert!(
        matches!(
        err,
        ValidateJwtError::MissingClaims(missing_claims)
            if missing_claims == ["nbf"].map(|s| s.into())
        ),
        "expected an error due to missing `nbf` claim"
    );
}
