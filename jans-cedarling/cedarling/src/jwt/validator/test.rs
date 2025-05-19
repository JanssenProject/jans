// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashSet;
use std::sync::Arc;

use jsonwebtoken::Algorithm;
use serde_json::json;
use test_utils::assert_eq;

use super::super::test_utils::*;
use super::{JwtValidator, JwtValidatorConfig};
use crate::jwt::key_service::KeyService;
use crate::jwt::validator::{ValidateJwtError, ValidatedJwt};

#[track_caller]
fn prepare_key_service() -> (KeyService, String, KeyPair) {
    let iss = "https://127.0.0.1".to_string();
    let keys = generate_keypair_hs256(Some("some_hs256_key")).expect("Should generate keys");
    let jwks = generate_jwks(&vec![keys.clone()]);
    let mut key_service = KeyService::new();
    key_service
        .insert_keys_from_str(
            &json!({
                &iss: jwks.keys,
            })
            .to_string(),
        )
        .expect("insert JWKS using string");
    (key_service, iss, keys)
}

#[test]
fn can_decode_jwt_without_sig_validation() {
    let (_key_service, iss, keys) = prepare_key_service();

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

    let validator = JwtValidator::new(
        JwtValidatorConfig {
            sig_validation: false,
            status_validation: false.into(),
            trusted_issuers: None.into(),
            algs_supported: HashSet::from([Algorithm::HS256]).into(),
            required_claims: HashSet::new(),
            validate_exp: true,
            validate_nbf: true,
        },
        KeyService::default().into(),
    );

    let result = validator.validate_jwt(&token).expect("should validate JWT");

    let expected = ValidatedJwt {
        claims,
        trusted_iss: None,
    };

    assert_eq!(result, expected);
}

#[test]
fn can_decode_and_validate_jwt() {
    let (key_service, iss, keys) = prepare_key_service();

    let claims = json!({
        "iss": iss,
        "sub": "1234567890",
        "name": "John Doe",
        "iat": 1516239022,
    });
    let token =
        generate_token_using_claims(&claims, &keys).expect("Should generate token using keys");

    let validator = JwtValidator::new(
        JwtValidatorConfig {
            sig_validation: true.into(),
            status_validation: false.into(),
            trusted_issuers: None.into(),
            algs_supported: HashSet::from([Algorithm::HS256]).into(),
            required_claims: HashSet::new(),
            validate_exp: true,
            validate_nbf: true,
        },
        key_service.into(),
    );

    let result = validator
        .validate_jwt(&token)
        .expect("Should successfully process JWT");

    let expected = ValidatedJwt {
        claims,
        trusted_iss: None,
    };

    assert_eq!(result, expected);
}

#[test]
fn errors_on_expired_token() {
    let (key_service, iss, keys) = prepare_key_service();

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

    let validator = JwtValidator::new(
        JwtValidatorConfig {
            sig_validation: true,
            status_validation: false.into(),
            trusted_issuers: None.into(),
            algs_supported: HashSet::from([Algorithm::HS256]).into(),
            required_claims: HashSet::new(),
            validate_exp: true,
            validate_nbf: true,
        },
        key_service.into(),
    );

    let err = validator
        .validate_jwt(&token)
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
    let (key_service, iss, keys) = prepare_key_service();

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

    let validator = JwtValidator::new(
        JwtValidatorConfig {
            sig_validation: true,
            status_validation: false.into(),
            trusted_issuers: None.into(),
            algs_supported: HashSet::from([Algorithm::HS256]).into(),
            required_claims: HashSet::new(),
            validate_exp: true,
            validate_nbf: true,
        },
        key_service.into(),
    );

    let err = validator
        .validate_jwt(&token)
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
    let (key_service, iss, keys) = prepare_key_service();

    // Generate token
    let claims = json!({
        "iss": iss,
        "sub": "1234567890",
        "name": "John Doe",
        "iat": 1516239022,
    });
    let token =
        generate_token_using_claims(&claims, &keys).expect("Should generate token using keys");

    let key_service = Arc::new(key_service);

    // Base case where all required claims are present
    let validator = JwtValidator::new(
        JwtValidatorConfig {
            sig_validation: true,
            status_validation: false.into(),
            trusted_issuers: None.into(),
            algs_supported: HashSet::from([Algorithm::HS256]).into(),
            required_claims: HashSet::from(["sub", "name", "iat"].map(|x| x.into())),
            validate_exp: true,
            validate_nbf: true,
        },
        key_service.clone(),
    );
    let result = validator
        .validate_jwt(&token)
        .expect("Should process JWT successfully");

    let expected = ValidatedJwt {
        claims,
        trusted_iss: None,
    };

    assert_eq!(result, expected);

    // Error case where `nbf` is missing from the token.
    let validator = JwtValidator::new(
        JwtValidatorConfig {
            sig_validation: true,
            status_validation: false.into(),
            trusted_issuers: None.into(),
            algs_supported: HashSet::from([Algorithm::HS256]).into(),
            required_claims: HashSet::from(["sub", "name", "iat", "nbf"].map(|x| x.into())),
            validate_exp: true,
            validate_nbf: true,
        },
        key_service.clone(),
    );
    let err = validator
        .validate_jwt(&token)
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
