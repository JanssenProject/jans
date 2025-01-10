// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::entities::DecodedTokens;
use crate::{
    common::policy_store::TokenKind,
    jwt::{Token, TokenClaimTypeError},
};

pub fn enforce_id_tkn_trust_mode(tokens: &DecodedTokens) -> Result<(), IdTokenTrustModeError> {
    let access_tkn = tokens
        .access_token
        .as_ref()
        .ok_or(IdTokenTrustModeError::MissingAccessToken)?;
    let id_tkn = tokens
        .id_token
        .as_ref()
        .ok_or(IdTokenTrustModeError::MissingIdToken)?;

    let access_tkn_client_id = get_tkn_claim_as_str(access_tkn, "client_id")?;
    let id_tkn_aud = get_tkn_claim_as_str(id_tkn, "aud")?;

    if access_tkn_client_id != id_tkn_aud {
        return Err(IdTokenTrustModeError::ClientIdIdTokenAudMismatch);
    }

    let userinfo_tkn = match tokens.userinfo_token.as_ref() {
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
    let claim = token
        .get_claim(claim_name)
        .ok_or(IdTokenTrustModeError::MissingRequiredClaim(
            claim_name.to_string(),
            token.kind,
        ))?;
    let claim_str = claim
        .as_str()
        .map_err(|e| IdTokenTrustModeError::TokenClaimTypeError(token.kind, e))?;
    Ok(claim_str.into())
}

#[derive(Debug, thiserror::Error)]
pub enum IdTokenTrustModeError {
    #[error("the access token's `client_id` does not match with the id token's `aud`")]
    ClientIdIdTokenAudMismatch,
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
    use super::{enforce_id_tkn_trust_mode, IdTokenTrustModeError};
    use crate::authz::entities::DecodedTokens;
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
            access_token: Some(access_token),
            id_token: Some(id_token),
            userinfo_token: None,
        };
        enforce_id_tkn_trust_mode(&tokens).expect("should not error");
    }

    #[test]
    fn errors_when_missing_access_tkn() {
        let id_token = Token::new_id(
            TokenClaims::new(HashMap::from([("aud".to_string(), json!("some-id-123"))])),
            None,
        );
        let tokens = DecodedTokens {
            access_token: None,
            id_token: Some(id_token),
            userinfo_token: None,
        };
        let err = enforce_id_tkn_trust_mode(&tokens).expect_err("should error");
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
            access_token: Some(access_token),
            id_token: Some(id_token),
            userinfo_token: None,
        };
        let err = enforce_id_tkn_trust_mode(&tokens).expect_err("should error");
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
            access_token: Some(access_token),
            id_token: None,
            userinfo_token: None,
        };
        let err = enforce_id_tkn_trust_mode(&tokens).expect_err("should error");
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
            access_token: Some(access_token),
            id_token: Some(id_token),
            userinfo_token: None,
        };
        let err = enforce_id_tkn_trust_mode(&tokens).expect_err("should error");
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
            access_token: Some(access_token),
            id_token: Some(id_token),
            userinfo_token: None,
        };
        let err = enforce_id_tkn_trust_mode(&tokens).expect_err("should error");
        assert!(
            matches!(err, IdTokenTrustModeError::ClientIdIdTokenAudMismatch),
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
            access_token: Some(access_token),
            id_token: Some(id_token),
            userinfo_token: Some(userinfo_token),
        };
        enforce_id_tkn_trust_mode(&tokens).expect("should not error");
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
            access_token: Some(access_token),
            id_token: Some(id_token),
            userinfo_token: Some(userinfo_token),
        };
        let err = enforce_id_tkn_trust_mode(&tokens).expect_err("should error");
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
            access_token: Some(access_token),
            id_token: Some(id_token),
            userinfo_token: Some(userinfo_token),
        };
        let err = enforce_id_tkn_trust_mode(&tokens).expect_err("should error");
        assert!(
            matches!(err, IdTokenTrustModeError::SubMismatchIdTokenUserinfo),
            "expected error due to the id_token's `aud` not matching with the userinfo_token's `aud`, got: {:?}",
            err
        )
    }
}
