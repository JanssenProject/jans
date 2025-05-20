// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashSet;

use super::decode::*;
use super::key_service::DecodingKeyInfo;
use super::*;
use crate::common::policy_store::{TokenEntityMetadata, TrustedIssuer};
use jsonwebtoken::{self as jwt, Algorithm, DecodingKey, Validation};
use serde::Deserialize;
use serde_json::Value;

#[derive(Debug, PartialEq, Deserialize)]
pub struct ValidatedJwt<'a> {
    #[serde(flatten)]
    pub claims: Value,
    #[serde(skip)]
    pub trusted_iss: Option<&'a TrustedIssuer>,
}

/// This struct is a wrapper over [`jsonwebtoken::Validation`] which implements an
/// additional check for requiring custom JWT claims.
pub struct JwtValidator {
    validation: Validation,
    required_claims: HashSet<Box<str>>,
}

impl JwtValidator {
    pub fn new(
        iss: Option<String>,
        token_name: String,
        token_metadata: &TokenEntityMetadata,
        algorithm: Algorithm,
    ) -> (Self, ValidatorKey) {
        let mut validation = Validation::new(algorithm);
        validation.validate_exp = token_metadata.required_claims.contains("exp");
        validation.validate_nbf = token_metadata.required_claims.contains("nbf");

        // we will validate the missing claims in another function since the
        // jsonwebtoken crate does not support required custom claims
        // ... but this defaults to true so we need to set it to false.
        validation.required_spec_claims.clear();
        validation.validate_aud = false;

        let required_claims = token_metadata
            .required_claims
            .iter()
            .cloned()
            .map(|s| s.into_boxed_str())
            .collect();

        let key = ValidatorKey {
            iss,
            token_name,
            algorithm,
        };

        let validator = JwtValidator {
            validation,
            required_claims,
        };

        (validator, key)
    }

    pub fn validate_jwt(
        &self,
        jwt: &str,
        decoding_key: &DecodingKey,
    ) -> Result<ValidatedJwt, ValidateJwtError> {
        let validated_jwt =
            jwt::decode::<ValidatedJwt>(jwt, decoding_key, &self.validation)?.claims;

        // Custom implementation of requiring custom claims
        let missing_claims = self
            .required_claims
            .iter()
            .filter(|claim| validated_jwt.claims.get(claim.as_ref()).is_none())
            .cloned()
            .collect::<Vec<Box<str>>>();
        if !missing_claims.is_empty() {
            Err(ValidateJwtError::MissingClaims(missing_claims))?
        }

        return Ok(validated_jwt);
    }
}

impl DecodedJwt {
    pub fn iss(&self) -> Option<&str> {
        self.claims.inner.get("iss").and_then(|x| x.as_str())
    }

    pub fn decoding_key_info(&self) -> DecodingKeyInfo {
        DecodingKeyInfo {
            issuer: self.iss().map(|x| x.to_string()),
            kid: self.header.kid.clone(),
            algorithm: self.header.alg,
        }
    }
}

impl From<DecodedJwt> for ValidatedJwt<'_> {
    fn from(decoded_jwt: DecodedJwt) -> Self {
        Self {
            claims: decoded_jwt.claims.inner,
            trusted_iss: None,
        }
    }
}

#[derive(Debug, thiserror::Error)]
pub enum ValidateJwtError {
    #[error("failed to decode the JWT: {0}")]
    DecodeJwt(#[from] DecodeJwtError),
    #[error("failed to validate the JWT since no key was available")]
    MissingValidationKey,
    #[error("failed to validate the JWT from '{0:?}' since it's not a trusted issuer")]
    MissingValidator(Option<String>),
    #[error("failed to validate the JWT: {0}")]
    ValidateJwt(#[from] jwt::errors::Error),
    #[error("validation failed since the JWT is missing the following required claims: {0:#?}")]
    MissingClaims(Vec<Box<str>>),
}

#[cfg(test)]
mod test {
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

        assert!(matches!(err, ValidateJwtError::ValidateJwt(ref e)
            if *e.kind() == jsonwebtoken::errors::ErrorKind::ExpiredSignature
        ));
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
        tkn_entity_metadata.required_claims =
            HashSet::from(["sub", "name", "iat"].map(|x| x.into()));
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
}
