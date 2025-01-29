// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::entity_builder::DecodedTokens;
use crate::{
    common::policy_store::TokenKind,
    jwt::{Token, TokenClaimTypeError},
};

/// Enforces the trust mode setting set by the `CEDARLING_ID_TOKEN_TRUST_MODE`
/// bootstrap property.
///
/// # Trust Modes
///
/// There are currently two trust modes:
/// - None
/// - Strict
///
/// # Strict Mode
///
/// Strict mode requires the following:
/// - `id_token.aud` == `access_token.client_id`
/// - if a Userinfo token is present:
///     - `userinfo_token.aud` == `access_token.client_id`
///     - `userinfo_token.sub` == `id_token.sub`
pub fn validate_id_tkn_trust_mode(tokens: &DecodedTokens) -> Result<(), IdTokenTrustModeError> {
    let access_tkn = tokens
        .access
        .as_ref()
        .ok_or(IdTokenTrustModeError::MissingAccessToken)?;
    let id_tkn = tokens
        .id
        .as_ref()
        .ok_or(IdTokenTrustModeError::MissingIdToken)?;

    let access_tkn_client_id = get_tkn_claim_as_str(access_tkn, "client_id")?;
    let id_tkn_aud = get_tkn_claim_as_str(id_tkn, "aud")?;

    if access_tkn_client_id != id_tkn_aud {
        return Err(IdTokenTrustModeError::AccessTokenClientIdMismatch);
    }

    let userinfo_tkn = match tokens.userinfo.as_ref() {
        Some(token) => token,
        None => return Ok(()),
    };
    let userinfo_tkn_aud = get_tkn_claim_as_str(userinfo_tkn, "aud")?;

    if userinfo_tkn_aud != id_tkn_aud {
        return Err(IdTokenTrustModeError::SubMismatchIdTokenUserinfo);
    }
    if userinfo_tkn_aud != access_tkn_client_id {
        return Err(IdTokenTrustModeError::ClientIdUserinfoAudMismatch);
    }

    Ok(())
}

fn get_tkn_claim_as_str(
    token: &Token,
    claim_name: &str,
) -> Result<Box<str>, IdTokenTrustModeError> {
    token
        .get_claim(claim_name)
        .ok_or_else(|| {
            IdTokenTrustModeError::MissingRequiredClaim(claim_name.to_string(), token.kind)
        })
        .and_then(|claim| {
            claim
                .as_str()
                .map(|s| s.into())
                .map_err(|e| IdTokenTrustModeError::TokenClaimTypeError(token.kind, e))
        })
}

#[derive(Debug, thiserror::Error)]
pub enum IdTokenTrustModeError {
    #[error("the access token's `client_id` does not match with the id token's `aud`")]
    AccessTokenClientIdMismatch,
    #[error("an access token is required when using strict mode")]
    MissingAccessToken,
    #[error("an id token is required when using strict mode")]
    MissingIdToken,
    #[error("the id token's `sub` does not match with the userinfo token's `sub`")]
    SubMismatchIdTokenUserinfo,
    #[error("the access token's `client_id` does not match with the userinfo token's `aud`")]
    ClientIdUserinfoAudMismatch,
    #[error("missing a required claim `{0}` from `{1}` token")]
    MissingRequiredClaim(String, TokenKind),
    #[error("invalid claim type in {0} token: {1}")]
    TokenClaimTypeError(TokenKind, TokenClaimTypeError),
}

#[cfg(test)]
mod test {
    use super::{IdTokenTrustModeError, validate_id_tkn_trust_mode};
    use crate::authz::entity_builder::DecodedTokens;
    use crate::common::policy_store::TokenKind;
    use crate::jwt::{Token, TokenClaims};
    use serde_json::json;
    use std::collections::HashMap;

    #[test]
    fn success_without_userinfo_tkn() {
        let access_token = Token::new_access(
            TokenClaims::new(HashMap::from([(
                "client_id".to_string(),
                json!("some-id-123"),
            )])),
            None,
        );
        let id_token = Token::new_id(
            TokenClaims::new(HashMap::from([("aud".to_string(), json!("some-id-123"))])),
            None,
        );
        let tokens = DecodedTokens {
            access: Some(access_token.into()),
            id: Some(id_token.into()),
            userinfo: None,
        };
        validate_id_tkn_trust_mode(&tokens).expect("should not error");
    }

    #[test]
    fn errors_when_missing_access_tkn() {
        let id_token = Token::new_id(
            TokenClaims::new(HashMap::from([("aud".to_string(), json!("some-id-123"))])),
            None,
        );
        let tokens = DecodedTokens {
            access: None,
            id: Some(id_token.into()),
            userinfo: None,
        };
        let err = validate_id_tkn_trust_mode(&tokens).expect_err("should error");
        assert!(
            matches!(err, IdTokenTrustModeError::MissingAccessToken),
            "expected error due to missing access token, got: {:?}",
            err
        )
    }

    #[test]
    fn errors_when_access_tkn_missing_required_claim() {
        let access_token = Token::new_access(TokenClaims::new(HashMap::new()), None);
        let id_token = Token::new_id(
            TokenClaims::new(HashMap::from([("aud".to_string(), json!("some-id-123"))])),
            None,
        );
        let tokens = DecodedTokens {
            access: Some(access_token.into()),
            id: Some(id_token.into()),
            userinfo: None,
        };
        let err = validate_id_tkn_trust_mode(&tokens).expect_err("should error");
        assert!(
            matches!(
                err,
                IdTokenTrustModeError::MissingRequiredClaim(ref claim_name, tkn_kind)
                    if claim_name == "client_id" &&
                        tkn_kind == TokenKind::Access
            ),
            "expected error due to access token missing a required claim, got: {:?}",
            err
        )
    }

    #[test]
    fn errors_when_missing_id_tkn() {
        let access_token = Token::new_id(
            TokenClaims::new(HashMap::from([(
                "client_id".to_string(),
                json!("some-id-123"),
            )])),
            None,
        );
        let tokens = DecodedTokens {
            access: Some(access_token.into()),
            id: None,
            userinfo: None,
        };
        let err = validate_id_tkn_trust_mode(&tokens).expect_err("should error");
        assert!(
            matches!(err, IdTokenTrustModeError::MissingIdToken),
            "expected error due to missing id token, got: {:?}",
            err
        )
    }

    #[test]
    fn errors_when_id_tkn_missing_required_claim() {
        let access_token = Token::new_access(
            TokenClaims::new(HashMap::from([(
                "client_id".to_string(),
                json!("some-id-123"),
            )])),
            None,
        );
        let id_token = Token::new_id(TokenClaims::new(HashMap::new()), None);
        let tokens = DecodedTokens {
            access: Some(access_token.into()),
            id: Some(id_token.into()),
            userinfo: None,
        };
        let err = validate_id_tkn_trust_mode(&tokens).expect_err("should error");
        assert!(
            matches!(
                err,
                IdTokenTrustModeError::MissingRequiredClaim(ref claim_name, tkn_kind)
                    if claim_name == "aud" &&
                        tkn_kind == TokenKind::Id
            ),
            "expected error due to id token missing a required claim, got: {:?}",
            err
        )
    }

    #[test]
    fn errors_when_access_tkn_client_id_id_tkn_aud_mismatch() {
        let access_token = Token::new_access(
            TokenClaims::new(HashMap::from([(
                "client_id".to_string(),
                json!("some-id-123"),
            )])),
            None,
        );
        let id_token = Token::new_id(
            TokenClaims::new(HashMap::from([(
                "aud".to_string(),
                json!("another-id-123"),
            )])),
            None,
        );
        let tokens = DecodedTokens {
            access: Some(access_token.into()),
            id: Some(id_token.into()),
            userinfo: None,
        };
        let err = validate_id_tkn_trust_mode(&tokens).expect_err("should error");
        assert!(
            matches!(err, IdTokenTrustModeError::AccessTokenClientIdMismatch),
            "expected error due to the access_token's `client_id` not matching with the id_token's `aud`, got: {:?}",
            err
        )
    }

    #[test]
    fn success_with_userinfo_tkn() {
        let access_token = Token::new_access(
            TokenClaims::new(HashMap::from([(
                "client_id".to_string(),
                json!("some-id-123"),
            )])),
            None,
        );
        let id_token = Token::new_id(
            TokenClaims::new(HashMap::from([("aud".to_string(), json!("some-id-123"))])),
            None,
        );
        let userinfo_token = Token::new_userinfo(
            TokenClaims::new(HashMap::from([("aud".to_string(), json!("some-id-123"))])),
            None,
        );
        let tokens = DecodedTokens {
            access: Some(access_token.into()),
            id: Some(id_token.into()),
            userinfo: Some(userinfo_token.into()),
        };
        validate_id_tkn_trust_mode(&tokens).expect("should not error");
    }

    #[test]
    fn errors_when_userinfo_tkn_missing_required_claim() {
        let access_token = Token::new_access(
            TokenClaims::new(HashMap::from([(
                "client_id".to_string(),
                json!("some-id-123"),
            )])),
            None,
        );
        let id_token = Token::new_id(
            TokenClaims::new(HashMap::from([("aud".to_string(), json!("some-id-123"))])),
            None,
        );
        let userinfo_token = Token::new_userinfo(TokenClaims::new(HashMap::new()), None);
        let tokens = DecodedTokens {
            access: Some(access_token.into()),
            id: Some(id_token.into()),
            userinfo: Some(userinfo_token.into()),
        };
        let err = validate_id_tkn_trust_mode(&tokens).expect_err("should error");
        assert!(
            matches!(
                err,
                IdTokenTrustModeError::MissingRequiredClaim(ref claim_name, tkn_kind)
                    if claim_name == "aud" &&
                        tkn_kind == TokenKind::Userinfo
            ),
            "expected error due to id token missing a required claim, got: {:?}",
            err
        )
    }

    #[test]
    fn errors_when_access_tkn_client_id_userinfo_tkn_aud_mismatch() {
        let access_token = Token::new_access(
            TokenClaims::new(HashMap::from([(
                "client_id".to_string(),
                json!("some-id-123"),
            )])),
            None,
        );
        let id_token = Token::new_id(
            TokenClaims::new(HashMap::from([("aud".to_string(), json!("some-id-123"))])),
            None,
        );
        let userinfo_token = Token::new_userinfo(
            TokenClaims::new(HashMap::from([(
                "aud".to_string(),
                json!("another-id-123"),
            )])),
            None,
        );
        let tokens = DecodedTokens {
            access: Some(access_token.into()),
            id: Some(id_token.into()),
            userinfo: Some(userinfo_token.into()),
        };
        let err = validate_id_tkn_trust_mode(&tokens).expect_err("should error");
        assert!(
            matches!(err, IdTokenTrustModeError::SubMismatchIdTokenUserinfo),
            "expected error due to the id_token's `aud` not matching with the userinfo_token's `aud`, got: {:?}",
            err
        )
    }
}
