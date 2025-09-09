// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashMap;

use super::errors::IdTokenTrustModeError;
use crate::jwt::{Token, TokenClaimTypeError};

/// Enforces the trust mode setting set by the `CEDARLING_ID_TOKEN_TRUST_MODE`
/// bootstrap property.
///
/// # Trust Modes
///
/// There are currently two trust modes:
/// - Never
/// - Strict
///
/// # Strict Mode
///
/// Strict mode requires the following:
/// - `id_token.aud` contains `access_token.client_id`
/// - if a Userinfo token is present:
///     - `userinfo_token.aud` == `access_token.client_id`
///     - `userinfo_token.sub` == `id_token.sub`
pub fn validate_id_tkn_trust_mode(
    tokens: &HashMap<String, Token>,
) -> Result<(), IdTokenTrustModeError> {
    let access_tkn = tokens
        .get("access_token")
        .ok_or(IdTokenTrustModeError::MissingAccessToken)?;
    let id_tkn = tokens
        .get("id_token")
        .ok_or(IdTokenTrustModeError::MissingIdToken)?;

    let access_tkn_client_id = get_tkn_claim_as_str(access_tkn, "client_id")?;

    if !aud_claim_contains_value(id_tkn, &access_tkn_client_id)? {
        return Err(IdTokenTrustModeError::AccessTokenClientIdMismatch);
    }

    let userinfo_tkn = match tokens.get("userinfo_token") {
        Some(token) => token,
        None => return Ok(()),
    };

    if !aud_claim_contains_value(userinfo_tkn, &access_tkn_client_id)? {
        return Err(IdTokenTrustModeError::ClientIdUserinfoAudMismatch);
    }

    Ok(())
}

fn aud_claim_contains_value(
    token: &Token,
    expected_value: &str,
) -> Result<bool, IdTokenTrustModeError> {
    token
        .get_claim("aud")
        .ok_or_else(|| {
            IdTokenTrustModeError::MissingRequiredClaim("aud".to_string(), token.name.clone())
        })
        .and_then(|claim| {
            match claim.value() {
                serde_json::Value::String(s) => {
                    // String aud claim - direct comparison
                    Ok(s == expected_value)
                },
                serde_json::Value::Array(arr) => {
                    // Array aud claim - check if it contains the expected value
                    Ok(arr
                        .iter()
                        .filter_map(|item| item.as_str())
                        .any(|s| s == expected_value))
                },
                _ => Err(IdTokenTrustModeError::TokenClaimTypeError(
                    token.name.clone(),
                    TokenClaimTypeError::type_mismatch("aud", "string or array", claim.value()),
                )),
            }
        })
}

/// Gets a claim value as a string (for non-aud claims)
fn get_tkn_claim_as_str(
    token: &Token,
    claim_name: &str,
) -> Result<Box<str>, IdTokenTrustModeError> {
    token
        .get_claim(claim_name)
        .ok_or_else(|| {
            IdTokenTrustModeError::MissingRequiredClaim(claim_name.to_string(), token.name.clone())
        })
        .and_then(|claim| {
            claim
                .as_str()
                .map(|s| s.into())
                .map_err(|e| IdTokenTrustModeError::TokenClaimTypeError(token.name.clone(), e))
        })
}

#[cfg(test)]
mod test {
    use super::{IdTokenTrustModeError, validate_id_tkn_trust_mode};
    use crate::jwt::Token;
    use serde_json::json;
    use std::collections::HashMap;

    #[test]
    fn success_without_userinfo_tkn() {
        let access_token = Token::new(
            "access_token",
            serde_json::from_value(json!({"client_id": "some-id-123"}))
                .expect("valid token claims"),
            None,
        );
        let id_token = Token::new(
            "id_token",
            serde_json::from_value(json!({"aud": ["some-id-123"]})).expect("valid token claims"),
            None,
        );
        let tokens = HashMap::from([
            ("access_token".to_string(), access_token),
            ("id_token".to_string(), id_token),
        ]);
        validate_id_tkn_trust_mode(&tokens).expect("should not error");
    }

    #[test]
    fn errors_when_missing_access_tkn() {
        let id_token = Token::new(
            "id_token",
            serde_json::from_value(json!({"aud": "some-id-123"})).expect("valid token claims"),
            None,
        );
        let tokens = HashMap::from([("id_token".to_string(), id_token)]);
        let err = validate_id_tkn_trust_mode(&tokens).expect_err("should error");
        assert!(
            matches!(err, IdTokenTrustModeError::MissingAccessToken),
            "expected error due to missing access token, got: {:?}",
            err
        )
    }

    #[test]
    fn errors_when_access_tkn_missing_required_claim() {
        let access_token = Token::new(
            "access_token",
            serde_json::from_value(json!({})).expect("valid token claims"),
            None,
        );
        let id_token = Token::new(
            "id_token",
            serde_json::from_value(json!({"aud": "some-id-123"})).expect("valid token claims"),
            None,
        );
        let tokens = HashMap::from([
            ("access_token".to_string(), access_token),
            ("id_token".to_string(), id_token),
        ]);
        let err = validate_id_tkn_trust_mode(&tokens).expect_err("should error");
        assert!(
            matches!(
                err,
                IdTokenTrustModeError::MissingRequiredClaim(ref claim_name, ref tkn_name)
                    if claim_name == "client_id" &&
                        tkn_name == "access_token"
            ),
            "expected error due to access token missing a required claim, got: {:?}",
            err
        )
    }

    #[test]
    fn errors_when_missing_id_tkn() {
        let access_token = Token::new(
            "id_token",
            serde_json::from_value(json!({"client_id": "some-id-123"}))
                .expect("valid token claims"),
            None,
        );
        let tokens = HashMap::from([("access_token".to_string(), access_token)]);
        let err = validate_id_tkn_trust_mode(&tokens).expect_err("should error");
        assert!(
            matches!(err, IdTokenTrustModeError::MissingIdToken),
            "expected error due to missing id token, got: {:?}",
            err
        )
    }

    #[test]
    fn errors_when_id_tkn_missing_required_claim() {
        let access_token = Token::new(
            "access_token",
            serde_json::from_value(json!({"client_id": "some-id-123"}))
                .expect("valid token claims"),
            None,
        );
        let id_token = Token::new(
            "id_token",
            serde_json::from_value(json!({"client_id": "some-id-123"}))
                .expect("valid token claims"),
            None,
        );
        let tokens = HashMap::from([
            ("access_token".to_string(), access_token),
            ("id_token".to_string(), id_token),
        ]);
        let err = validate_id_tkn_trust_mode(&tokens).expect_err("should error");
        assert!(
            matches!(
                err,
                IdTokenTrustModeError::MissingRequiredClaim(ref claim_name, ref tkn_name)
                    if claim_name == "aud" &&
                        tkn_name == "id_token"
            ),
            "expected error due to id token missing a required claim, got: {:?}",
            err
        )
    }

    #[test]
    fn errors_when_access_tkn_client_id_id_tkn_aud_mismatch() {
        let access_token = Token::new(
            "access_token",
            serde_json::from_value(json!({"client_id": "some-id-123"}))
                .expect("valid token claims"),
            None,
        );
        let id_token = Token::new(
            "id_token",
            serde_json::from_value(json!({"aud": ["another-id-123"]})).expect("valid token claims"),
            None,
        );
        let tokens = HashMap::from([
            ("access_token".to_string(), access_token),
            ("id_token".to_string(), id_token),
        ]);
        let err = validate_id_tkn_trust_mode(&tokens).expect_err("should error");
        assert!(
            matches!(err, IdTokenTrustModeError::AccessTokenClientIdMismatch),
            "expected error due to the access_token's `client_id` not matching with the id_token's `aud`, got: {:?}",
            err
        )
    }

    #[test]
    fn success_with_userinfo_tkn() {
        let access_token = Token::new(
            "access_token",
            serde_json::from_value(json!({"client_id": "some-id-123"}))
                .expect("valid token claims"),
            None,
        );
        let id_token = Token::new(
            "id_token",
            serde_json::from_value(json!({"aud": ["some-id-123"]})).expect("valid token claims"),
            None,
        );
        let userinfo_token = Token::new(
            "userinfo_token",
            serde_json::from_value(json!({"aud": "some-id-123"})).expect("valid token claims"),
            None,
        );
        let tokens = HashMap::from([
            ("access_token".to_string(), access_token),
            ("id_token".to_string(), id_token),
            ("userinfo_token".to_string(), userinfo_token),
        ]);
        validate_id_tkn_trust_mode(&tokens).expect("should not error");
    }

    #[test]
    fn errors_when_userinfo_tkn_missing_required_claim() {
        let access_token = Token::new(
            "access_token",
            serde_json::from_value(json!({"client_id": "some-id-123"}))
                .expect("valid token claims"),
            None,
        );
        let id_token = Token::new(
            "id_token",
            serde_json::from_value(json!({"aud": ["some-id-123"]})).expect("valid token claims"),
            None,
        );
        let userinfo_token = Token::new(
            "userinfo_token",
            serde_json::from_value(json!({})).expect("valid token claims"),
            None,
        );
        let tokens = HashMap::from([
            ("access_token".to_string(), access_token),
            ("id_token".to_string(), id_token),
            ("userinfo_token".to_string(), userinfo_token),
        ]);
        let err = validate_id_tkn_trust_mode(&tokens).expect_err("should error");
        assert!(
            matches!(
                err,
                IdTokenTrustModeError::MissingRequiredClaim(ref claim_name, ref tkn_name)
                    if claim_name == "aud" &&
                        tkn_name == "userinfo_token"
            ),
            "expected error due to id token missing a required claim, got: {:?}",
            err
        )
    }

    #[test]
    fn errors_when_userinfo_tkn_aud_does_not_contain_client_id() {
        let access_token = Token::new(
            "access_token",
            serde_json::from_value(json!({"client_id": "some-id-123"}))
                .expect("valid token claims"),
            None,
        );
        let id_token = Token::new(
            "id_token",
            serde_json::from_value(json!({"aud": ["some-id-123"]})).expect("valid token claims"),
            None,
        );
        let userinfo_token = Token::new(
            "userinfo_token",
            serde_json::from_value(json!({"aud": "another-id-123"})).expect("valid token claims"),
            None,
        );
        let tokens = HashMap::from([
            ("access_token".to_string(), access_token),
            ("id_token".to_string(), id_token),
            ("userinfo_token".to_string(), userinfo_token),
        ]);
        let err = validate_id_tkn_trust_mode(&tokens).expect_err("should error");
        assert!(
            matches!(err, IdTokenTrustModeError::ClientIdUserinfoAudMismatch),
            "expected error due to userinfo token aud not containing client_id, got: {:?}",
            err
        )
    }
}
