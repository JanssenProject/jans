use super::{decode, test_utils::*, JwtValidator, JwtValidatorConfig, JwtValidatorError};
use crate::jwt::new_key_service::NewKeyService;
use jsonwebtoken::Algorithm;
use serde_json::json;
use std::collections::HashSet;
use std::rc::Rc;
use test_utils::assert_eq;

#[test]
fn can_decode_jwt() {
    // Generate token
    let claims = json!({
        "sub": "1234567890",
        "name": "John Doe",
        "iat": 1516239022,
    });
    let keys = generate_keypair_hs256(Some("some_hs256_key")).expect("Should generate keys");
    let token =
        generate_token_using_claims(&claims, &keys).expect("Should generate token using keys");

    let result = decode(&token).expect("Should decode JWT");

    assert_eq!(result, claims);
}

#[test]
fn can_decode_and_validate_jwt() {
    // Generate token
    let keys = generate_keypair_hs256(Some("some_hs256_key")).expect("Should generate keys");
    let claims = json!({
        "sub": "1234567890",
        "name": "John Doe",
        "iat": 1516239022,
    });
    let token =
        generate_token_using_claims(&claims, &keys).expect("Should generate token using keys");

    // Prepare Key Service
    let jwks = generate_jwks(&vec![keys]);
    let key_service = NewKeyService::new_from_str(
        &json!({
            "test_idp": jwks.keys,
        })
        .to_string(),
    )
    .expect("Should create KeyService");

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
        .process_jwt(&token)
        .expect("Should successfully process JWT");

    assert_eq!(result, claims);
}

#[test]
fn errors_on_expired_token() {
    // Generate token
    let keys = generate_keypair_hs256(Some("some_hs256_key")).expect("Should generate keys");
    let claims = json!({
        "sub": "1234567890",
        "name": "John Doe",
        "iat": 1516239022,
        "exp": 0,
    });
    let token =
        generate_token_using_claims(&claims, &keys).expect("Should generate token using keys");

    // Prepare Key Service
    let jwks = generate_jwks(&vec![keys]);
    let key_service = NewKeyService::new_from_str(
        &json!({
            "test_idp": jwks.keys,
        })
        .to_string(),
    )
    .expect("Should create KeyService");

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

    let result = validator.process_jwt(&token);

    assert!(
        matches!(result, Err(JwtValidatorError::ExpiredToken)),
        "Expected validation to fail due to the token being expired."
    );
}

#[test]
fn errors_on_immature_token() {
    // Generate token
    let keys = generate_keypair_hs256(Some("some_hs256_key")).expect("Should generate keys");
    let claims = json!({
        "sub": "1234567890",
        "name": "John Doe",
        "iat": 1516239022,
        "nbf": u64::MAX,
    });
    let token =
        generate_token_using_claims(&claims, &keys).expect("Should generate token using keys");

    // Prepare Key Service
    let jwks = generate_jwks(&vec![keys]);
    let key_service = NewKeyService::new_from_str(
        &json!({
            "test_idp": jwks.keys,
        })
        .to_string(),
    )
    .expect("Should create KeyService");

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

    let result = validator.process_jwt(&token);

    assert!(
        matches!(result, Err(JwtValidatorError::ImmatureToken)),
        "Expected validation to fail due to the token being immature."
    );
}

#[test]
fn can_check_missing_claims() {
    // Generate token
    let keys = generate_keypair_hs256(Some("some_hs256_key")).expect("Should generate keys");
    let claims = json!({
        "sub": "1234567890",
        "name": "John Doe",
        "iat": 1516239022,
    });
    let token =
        generate_token_using_claims(&claims, &keys).expect("Should generate token using keys");

    // Prepare Key Service
    let jwks = generate_jwks(&vec![keys]);
    let key_service: Rc<NewKeyService> = NewKeyService::new_from_str(
        &json!({
            "test_idp": jwks.keys,
        })
        .to_string(),
    )
    .expect("Should create KeyService")
    .into();

    // Base case where all required claims are present
    let validator = JwtValidator::new(
        JwtValidatorConfig {
            sig_validation: true.into(),
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
        .process_jwt(&token)
        .expect("Should process JWT successfully");
    assert_eq!(result, claims);

    // Error case where `nbf` is missing from the token.
    let validator = JwtValidator::new(
        JwtValidatorConfig {
            sig_validation: true.into(),
            status_validation: false.into(),
            trusted_issuers: None.into(),
            algs_supported: HashSet::from([Algorithm::HS256]).into(),
            required_claims: HashSet::from(["sub", "name", "iat", "nbf"].map(|x| x.into())),
            validate_exp: true,
            validate_nbf: true,
        },
        key_service.clone(),
    );
    let result = validator.process_jwt(&token);
    assert!(
        matches!(
            result,
            Err(JwtValidatorError::MissingClaims(missing_claims))
            if missing_claims == Vec::from(["nbf"].map(|s| s.into()))
        ),
        "Expected an error due to missing `nbf` claim"
    );
}
