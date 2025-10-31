// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashSet;

use crate::common::policy_store::{TokenEntityMetadata, TrustedIssuer};
use crate::jwt::decode::*;
use crate::jwt::key_service::DecodingKeyInfo;
use crate::jwt::*;
use jsonwebtoken::{self as jwt, Algorithm, DecodingKey, Validation};
use serde::{Deserialize, Serialize};
use serde_json::Value;

#[derive(Debug, PartialEq, Deserialize, Clone)]
pub struct ValidatedJwt {
    #[serde(flatten)]
    pub claims: Value,
    #[serde(skip)]
    pub trusted_iss: Option<Arc<TrustedIssuer>>,
}

impl ValidatedJwt {
    /// Gets the value of the status list claim in the [`referenced token`]
    ///
    /// [`referenced token`]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-10.html#name-referenced-token
    pub fn get_ref_status(&self) -> Result<Option<RefJwtStatusList>, serde_json::Error> {
        let Some(status) = self.claims.get("status") else {
            return Ok(None);
        };

        let status_list = serde_json::from_value::<RefJwtStatusListClaim>(status.clone())?;

        Ok(Some(status_list.status_list))
    }
}

/// Struct for deserializing the status list of the [`referenced token`]
///
/// [`referenced token`]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-10.html#name-referenced-token
#[derive(Debug, Deserialize, PartialEq)]
pub struct RefJwtStatusListClaim {
    status_list: RefJwtStatusList,
}

/// The value of the status list claim in the [`referenced token`]
///
/// [`referenced token`]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-10.html#name-referenced-token
#[derive(Debug, Deserialize, PartialEq, Serialize)]
pub struct RefJwtStatusList {
    pub idx: usize,
    pub uri: String,
    /// Maximum amount of time, in seconds, that the Status List Token can be cached
    /// before a fresh copy SHOULD be retrieved.
    #[serde(default)]
    pub ttl: Option<u64>,
}

/// This struct is a wrapper over [`jsonwebtoken::Validation`] which implements an
/// additional check for requiring custom JWT claims.
#[derive(Debug, Clone)]
pub struct JwtValidator {
    pub(crate) validation: Validation,
    required_claims: HashSet<Box<str>>,
    validate_signature: bool,
    validate_status_list: bool,
    status_list_cache: StatusListCache,
}

impl JwtValidator {
    /// Creates a new validator for the tokens passed through [`crate::Cedarling::authorize`]
    pub fn new_input_tkn_validator<'a>(
        iss: Option<&'a str>,
        tkn_name: &'a str,
        token_metadata: &TokenEntityMetadata,
        algorithm: Algorithm,
        status_lists: StatusListCache,
        validate_signature: bool,
        validate_status_list: bool,
    ) -> (Self, ValidatorInfo<'a>) {
        let token_kind = TokenKind::AuthzRequestInput(tkn_name);

        let mut validation = Validation::new(algorithm);
        if let Some(iss) = iss {
            validation.set_issuer(&[iss])
        }
        validation.validate_exp = token_metadata.required_claims.contains("exp");
        validation.validate_nbf = token_metadata.required_claims.contains("nbf");

        // we will validate the missing claims in another function since the
        // jsonwebtoken crate does not support required custom claims
        // ... but this defaults to true so we need to set it to false.
        validation.required_spec_claims.clear();
        validation.validate_aud = false;

        if !validate_signature {
            validation.insecure_disable_signature_validation();
        }

        let required_claims = token_metadata
            .required_claims
            .iter()
            .cloned()
            .map(|s| s.into_boxed_str())
            .collect();

        let key = ValidatorInfo {
            iss,
            token_kind,
            algorithm,
        };

        let validator = JwtValidator {
            validation,
            required_claims,
            validate_signature,
            validate_status_list,
            status_list_cache: status_lists,
        };

        (validator, key)
    }

    /// Creates a new validator for status list tokens
    pub fn new_status_list_tkn_validator(
        iss: Option<&'_ str>,
        status_list_uri: Option<String>,
        algorithm: Algorithm,
        validate_signature: bool,
    ) -> (Self, ValidatorInfo<'_>) {
        let token_kind = TokenKind::StatusList;

        let mut validation = Validation::new(algorithm);
        validation.validate_exp = true;
        validation.validate_nbf = true;

        // we will validate the missing claims in another function since the
        // jsonwebtoken crate does not support required custom claims
        // ... but this defaults to true so we need to set it to false.
        validation.required_spec_claims.clear();
        validation.validate_aud = false;
        validation.sub = status_list_uri;

        if let Some(iss) = iss {
            validation.set_issuer(&[iss])
        };

        let required_claims = ["sub", "iat", "status_list"]
            .into_iter()
            .map(|s| s.into())
            .collect();

        let key = ValidatorInfo {
            iss,
            token_kind,
            algorithm,
        };

        let validator = JwtValidator {
            validation,
            required_claims,
            validate_signature,
            validate_status_list: false,
            status_list_cache: StatusListCache::default(),
        };

        (validator, key)
    }

    /// Validates JWT by checking:
    /// - The JWT's Signature
    /// - If the claims are valid (e.g. the JWT isn't expired)
    /// - If the status of the JWT isn't [`invalid`] or [`suspended`].
    ///
    /// [`invalid`]: JwtStatus::Invalid
    /// [`suspended`]: JwtStatus::Suspended
    pub fn validate_jwt(
        &self,
        jwt: &str,
        decoding_key: Option<&DecodingKey>,
    ) -> Result<ValidatedJwt, ValidateJwtError> {
        let validated_jwt = match decoding_key {
            Some(decoding_key) => {
                jwt::decode::<ValidatedJwt>(jwt, decoding_key, &self.validation)?.claims
            },
            None => {
                if self.validate_signature {
                    return Err(ValidateJwtError::MissingValidationKey);
                } else {
                    decode_jwt(jwt)?.try_into()?
                }
            },
        };

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

        if self.validate_status_list {
            // Check if the JWT has a status claim
            let Some(ref_status_list) = validated_jwt.get_ref_status()? else {
                // status validation is not required if the JWT does not
                // have a status claim
                return Ok(validated_jwt);
            };

            let jwt_status = {
                self.status_list_cache
                    .status_lists
                    .read()
                    .expect("obtain status list read lock")
                    .get(&ref_status_list.uri)
                    .ok_or(ValidateJwtError::MissingStatusList)?
                    .get_status(ref_status_list.idx)?
            };

            if !jwt_status.is_valid() {
                return Err(ValidateJwtError::RejectJwtStatus(jwt_status));
            }
        }

        Ok(validated_jwt)
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

impl TryFrom<DecodedJwt> for ValidatedJwt {
    type Error = serde_json::Error;

    fn try_from(decoded_jwt: DecodedJwt) -> Result<Self, Self::Error> {
        Ok(Self {
            claims: decoded_jwt.claims.inner,
            trusted_iss: None,
        })
    }
}

#[derive(Debug, thiserror::Error)]
pub enum ValidateJwtError {
    #[error("failed to decode the JWT: {0}")]
    DecodeJwt(#[from] DecodeJwtError),
    #[error("failed to validate the JWT since no key was available")]
    MissingValidationKey,
    #[error(
        "failed to validate JWT {0:?}: no validator was initialized. this may be due to an untrusted issuer or an unsupported algorithm"
    )]
    MissingValidator(OwnedValidatorInfo),
    #[error("failed to validate the JWT: {0}")]
    ValidateJwt(#[from] jwt::errors::Error),
    #[error("validation failed since the JWT is missing the following required claims: {0:#?}")]
    MissingClaims(Vec<Box<str>>),
    #[error("failed to get the status for the JWT: {0}")]
    GetJwtStatus(#[from] JwtStatusError),
    #[error("the token is rejected because it's status is: {0}")]
    RejectJwtStatus(JwtStatus),
    #[error("there isn't a status list available for the token")]
    MissingStatusList,
    #[error("failed to deserialize the JWT's status claim: {0}")]
    DeserializeStatusClaim(#[from] serde_json::Error),
}

#[cfg(test)]
mod test {
    use std::collections::{HashMap, HashSet};
    use std::sync::LazyLock;

    use crate::common::policy_store::{ClaimMappings, TokenEntityMetadata};
    use crate::jwt::status_list::{JwtStatus, StatusBitSize, StatusList};
    use crate::jwt::validation::{JwtValidator, ValidateJwtError, ValidatedJwt};
    use crate::jwt::{StatusListCache, test_utils::*};
    use jsonwebtoken::Algorithm;
    use serde_json::json;
    use test_utils::assert_eq;

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
        let iss = "127.0.0.1";

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

        let (validator, _) = JwtValidator::new_input_tkn_validator(
            Some(iss),
            "access_token".into(),
            &TEST_TKN_ENTITY_METADATA,
            Algorithm::HS256,
            StatusListCache::default(),
            false,
            false,
        );

        let result = validator
            .validate_jwt(&token, Some(&decoding_key))
            .expect("should validate JWT");

        let expected = ValidatedJwt {
            claims,
            trusted_iss: None,
        };

        assert_eq!(result, expected);
    }

    #[test]
    fn decoding_errors_if_token_is_expired_when_without_sig_validation() {
        let iss = "127.0.0.1";
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
        let (validator, _) = JwtValidator::new_input_tkn_validator(
            Some(iss),
            "access_token".into(),
            &TEST_TKN_ENTITY_METADATA,
            Algorithm::HS256,
            StatusListCache::default(),
            false,
            false,
        );

        let err = validator
            .validate_jwt(&token, Some(&decoding_key))
            .expect_err("should error due to expired JWT");

        assert!(matches!(err, ValidateJwtError::ValidateJwt(ref e)
            if *e.kind() == jsonwebtoken::errors::ErrorKind::ExpiredSignature
        ));
    }

    #[test]
    fn can_decode_and_validate_jwt() {
        let iss = "127.0.0.1";
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

        let (validator, _) = JwtValidator::new_input_tkn_validator(
            Some(iss),
            "access_token".into(),
            &TEST_TKN_ENTITY_METADATA,
            Algorithm::HS256,
            StatusListCache::default(),
            true,
            false,
        );

        let result = validator
            .validate_jwt(&token, Some(&decoding_key))
            .expect("Should successfully process JWT");

        let expected = ValidatedJwt {
            claims,
            trusted_iss: None,
        };

        assert_eq!(result, expected);
    }

    #[test]
    fn errors_on_expired_token() {
        let iss = "127.0.0.1";
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
        let (validator, _) = JwtValidator::new_input_tkn_validator(
            Some(iss),
            "access_token".into(),
            &TEST_TKN_ENTITY_METADATA,
            Algorithm::HS256,
            StatusListCache::default(),
            true,
            false,
        );

        let err = validator
            .validate_jwt(&token, Some(&decoding_key))
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
        let iss = "127.0.0.1";
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

        let (validator, _) = JwtValidator::new_input_tkn_validator(
            Some(iss),
            "access_token".into(),
            &TEST_TKN_ENTITY_METADATA,
            Algorithm::HS256,
            StatusListCache::default(),
            true,
            false,
        );

        let err = validator
            .validate_jwt(&token, Some(&decoding_key))
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
        let iss = "127.0.0.1";
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
        let (validator, _) = JwtValidator::new_input_tkn_validator(
            Some(iss),
            "access_token".into(),
            &tkn_entity_metadata,
            Algorithm::HS256,
            StatusListCache::default(),
            true,
            false,
        );

        let result = validator
            .validate_jwt(&token, Some(&decoding_key))
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
        let (validator, _) = JwtValidator::new_input_tkn_validator(
            Some(iss),
            "access_token".into(),
            &tkn_entity_metadata,
            Algorithm::HS256,
            StatusListCache::default(),
            true,
            false,
        );

        let err = validator
            .validate_jwt(&token, Some(&decoding_key))
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

    #[tokio::test]
    async fn reject_invalid_token_from_status_list() {
        let bit_size = StatusBitSize::try_from(1u8).unwrap();
        let status_list = [0b1111_1111];

        let mut server = MockServer::new_with_defaults().await.unwrap();
        server.generate_status_list_endpoint(bit_size, &status_list, None);
        let iss = server.issuer();
        let decoding_key = server.jwt_decoding_key().unwrap();
        let mut claims = json!({
            "iss": iss,
            "sub": "1234567890",
            "name": "John Doe",
            "iat": 0,
            "nbf": 10,
            "exp": u64::MAX,
        });
        let token = server
            .generate_token_with_hs256sig(&mut claims, Some(0))
            .unwrap();

        let status_lists: StatusListCache = HashMap::from([(
            server.status_list_endpoint().unwrap().to_string(),
            StatusList {
                bit_size,
                list: status_list.to_vec(),
            },
        )])
        .into();

        let (validator, _) = JwtValidator::new_input_tkn_validator(
            Some(&iss),
            "access_token".into(),
            &TEST_TKN_ENTITY_METADATA,
            Algorithm::HS256,
            status_lists,
            true,
            true,
        );

        let err = validator
            .validate_jwt(&token, Some(&decoding_key))
            .expect_err("should error because the status of the token is JwtStatus::Invalid");

        assert!(
            matches!(
                err,
                ValidateJwtError::RejectJwtStatus(ref status)
                    if *status == JwtStatus::Invalid
            ),
            "GOT {:?}: {}",
            err,
            err
        );
    }

    #[tokio::test]
    async fn reject_suspended_token_from_status_list() {
        let bit_size = StatusBitSize::try_from(1u8).unwrap();
        let status_list = [0b1111_1111];

        let mut server = MockServer::new_with_defaults().await.unwrap();
        server.generate_status_list_endpoint(bit_size, &status_list, None);
        let iss = server.issuer();
        let decoding_key = server.jwt_decoding_key().unwrap();
        let mut claims = json!({
            "iss": iss,
            "sub": "1234567890",
            "name": "John Doe",
            "iat": 0,
            "nbf": 10,
            "exp": u64::MAX,
        });
        let token = server
            .generate_token_with_hs256sig(&mut claims, Some(0))
            .unwrap();

        let status_lists: StatusListCache = HashMap::from([(
            server.status_list_endpoint().unwrap().to_string(),
            StatusList {
                bit_size: 2u8.try_into().unwrap(),
                list: vec![0b1010_1010],
            },
        )])
        .into();

        let (validator, _) = JwtValidator::new_input_tkn_validator(
            Some(&iss),
            "access_token".into(),
            &TEST_TKN_ENTITY_METADATA,
            Algorithm::HS256,
            status_lists,
            true,
            true,
        );

        let err = validator
            .validate_jwt(&token, Some(&decoding_key))
            .expect_err("should error because the status of the token is JwtStatus::Suspended");

        assert!(
            matches!(
                err,
                ValidateJwtError::RejectJwtStatus(ref status)
                    if *status == JwtStatus::Suspended
            ),
            "GOT {:?}: {}",
            err,
            err
        );
    }
}
