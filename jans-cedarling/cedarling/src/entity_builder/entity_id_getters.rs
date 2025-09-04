// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::BuildEntityErrorKind;
use crate::jwt::Token;
use serde_json::Value;
use smol_str::{SmolStr, ToSmolStr};
use std::fmt::Display;

pub enum EntityIdSrc<'a> {
    Token { token: &'a Token, claim: &'a str },
    String(&'a str),
}

pub fn get_first_valid_entity_id(id_srcs: &[EntityIdSrc]) -> Result<SmolStr, BuildEntityErrorKind> {
    let mut errors = Vec::new();

    for src in id_srcs.iter() {
        match src {
            EntityIdSrc::Token {
                token,
                claim: claim_name,
            } => {
                let claim = match token.get_claim_val(claim_name) {
                    Some(claim) => claim,
                    None => {
                        errors.push(GetEntityIdError {
                            token: token.name.clone(),
                            claim: claim_name.to_string(),
                            reason: GetEntityIdErrorReason::MissingClaim,
                        });
                        continue;
                    },
                };

                let claim = claim.to_string();
                let id = claim.trim_matches('"');

                if id.is_empty() {
                    errors.push(GetEntityIdError {
                        token: token.name.clone(),
                        claim: claim_name.to_string(),
                        reason: GetEntityIdErrorReason::EmptyString,
                    });
                    continue;
                }

                return Ok(id.to_smolstr());
            },
            EntityIdSrc::String(eid) => return Ok(eid.to_smolstr()),
        }
    }

    Err(BuildEntityErrorKind::MissingEntityId(errors.into()))
}

pub fn collect_all_valid_entity_ids(id_srcs: &[EntityIdSrc]) -> Vec<SmolStr> {
    id_srcs
        .iter()
        .filter_map(|src| match src {
            EntityIdSrc::Token { token, claim } => token.get_claim_val(claim).cloned(),
            EntityIdSrc::String(eid) => id_str_src_to_value(eid),
        })
        .flat_map(claim_to_ids)
        .collect()
}

fn id_str_src_to_value(eid: &str) -> Option<Value> {
    let eid = eid.trim().to_string();
    if eid.is_empty() {
        None
    } else {
        let eid =
            serde_json::from_str::<Value>(&eid).expect("Strings should always be a valid JSON");
        Some(eid)
    }
}

fn claim_to_ids(claim: Value) -> Vec<SmolStr> {
    let mut ids = Vec::with_capacity(1 + claim.as_array().map(|v| v.len()).unwrap_or_default());
    match claim {
        serde_json::Value::Number(number) => {
            ids.push(number.to_smolstr());
        },
        serde_json::Value::String(string) => {
            ids.push(string.trim_matches('"').into());
        },
        serde_json::Value::Array(values) => {
            for value in values {
                ids.append(&mut claim_to_ids(value));
            }
        },
        _ => {},
    }
    ids
}

#[derive(Debug, thiserror::Error, PartialEq)]
#[error("failed to use {claim} from {token} since {reason}")]
pub struct GetEntityIdError {
    token: String,
    claim: String,
    reason: GetEntityIdErrorReason,
}

#[derive(Debug, thiserror::Error, PartialEq)]
pub enum GetEntityIdErrorReason {
    #[error("the claim cannot be an empty string")]
    EmptyString,
    #[error("the claim was not present in the token")]
    MissingClaim,
}

#[derive(Debug)]
pub struct GetEntityIdErrors(Vec<GetEntityIdError>);

impl From<Vec<GetEntityIdError>> for GetEntityIdErrors {
    fn from(errors: Vec<GetEntityIdError>) -> Self {
        Self(errors)
    }
}

impl Display for GetEntityIdErrors {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "{:?}", self.0.iter().map(|e| e.to_string()))
    }
}

#[cfg(test)]
impl GetEntityIdErrors {
    pub fn contains(&self, err: &GetEntityIdError) -> bool {
        self.0.contains(err)
    }

    pub fn len(&self) -> usize {
        self.0.len()
    }
}

#[cfg(test)]
mod test {
    use super::*;
    use serde_json::json;
    use std::collections::HashMap;

    #[test]
    fn can_get_first_valid_eid() {
        let expected_aud = "some_aud";
        let expected_client_id = "some_client_id";

        let token = Token::new(
            "test_token",
            HashMap::from([("aud".into(), json!(expected_aud))]).into(),
            None,
        );

        // Using the token's aud
        let id = get_first_valid_entity_id(&[EntityIdSrc::Token {
            token: &token,
            claim: "aud",
        }])
        .expect("should get entity id from token's aud");
        assert_eq!(id, expected_aud);

        // Using the token's aud if the client_id is not present
        let id = get_first_valid_entity_id(&[
            EntityIdSrc::Token {
                token: &token,
                claim: "client_id",
            },
            EntityIdSrc::Token {
                token: &token,
                claim: "aud",
            },
        ])
        .expect("should get entity id from token's aud");
        assert_eq!(id, expected_aud);

        let token = Token::new(
            "test_token",
            HashMap::from([
                ("aud".into(), json!(expected_aud)),
                ("client_id".into(), json!(expected_client_id)),
            ])
            .into(),
            None,
        );

        // Using the first valid id even if others are also valid
        let id = get_first_valid_entity_id(&[
            EntityIdSrc::Token {
                token: &token,
                claim: "client_id",
            },
            EntityIdSrc::Token {
                token: &token,
                claim: "aud",
            },
        ])
        .expect("should get entity id from token's client_id");
        assert_eq!(id, expected_client_id);

        let token = Token::new(
            "test_token",
            HashMap::from([("empty".into(), json!(""))]).into(),
            None,
        );

        // Errors when no valid ids found
        let err = get_first_valid_entity_id(&[
            EntityIdSrc::Token {
                token: &token,
                claim: "empty",
            },
            EntityIdSrc::Token {
                token: &token,
                claim: "missing",
            },
        ])
        .expect_err("should error while getting id");
        let expected_errs = vec![
            GetEntityIdError {
                token: "test_token".into(),
                claim: "empty".into(),
                reason: GetEntityIdErrorReason::EmptyString,
            },
            GetEntityIdError {
                token: "test_token".into(),
                claim: "missing".into(),
                reason: GetEntityIdErrorReason::MissingClaim,
            },
        ];
        assert!(
            matches!(
                err,
                BuildEntityErrorKind::MissingEntityId(GetEntityIdErrors(ref errs))
                    if *errs == expected_errs
            ),
            "expected: {:?}\nbut got: {:?}",
            BuildEntityErrorKind::MissingEntityId(GetEntityIdErrors(expected_errs)),
            err,
        );
    }

    #[test]
    fn test_collect_all_entity_ids() {
        let token1 = Token::new(
            "tkn1",
            HashMap::from([("role".into(), json!("role1"))]).into(),
            None,
        );
        let token2 = Token::new(
            "tkn1",
            HashMap::from([("role".into(), json!("role2"))]).into(),
            None,
        );

        // Collecting one valid id
        let ids = collect_all_valid_entity_ids(&[EntityIdSrc::Token {
            token: &token1,
            claim: "role",
        }]);
        assert_eq!(ids, vec!["role1"]);

        // Collecting multiple valid ids
        let ids = collect_all_valid_entity_ids(&[
            EntityIdSrc::Token {
                token: &token1,
                claim: "role",
            },
            EntityIdSrc::Token {
                token: &token2,
                claim: "role",
            },
        ]);
        assert_eq!(ids, vec!["role1", "role2"]);

        // Ignore invalid ids
        let ids = collect_all_valid_entity_ids(&[
            EntityIdSrc::Token {
                token: &token1,
                claim: "role",
            },
            EntityIdSrc::Token {
                token: &token1,
                claim: "missing",
            },
            EntityIdSrc::Token {
                token: &token1,
                claim: "",
            },
            EntityIdSrc::Token {
                token: &token2,
                claim: "role",
            },
        ]);
        assert_eq!(ids, vec!["role1", "role2"]);
    }
}
