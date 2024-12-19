/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::{CreateCedarEntityError, DecodedTokens, EntityMetadata, EntityParsedTypeName};
use crate::common::cedar_schema::CedarSchemaJson;
use crate::common::policy_store::{PolicyStore, TokenKind};
use cedar_policy::EntityUid;
use std::collections::HashSet;
use std::fmt;

/// Create user entity
pub fn create_user_entity(
    entity_mapping: Option<&str>,
    policy_store: &PolicyStore,
    tokens: &DecodedTokens,
    parents: HashSet<EntityUid>,
) -> Result<cedar_policy::Entity, CreateUserEntityError> {
    let schema: &CedarSchemaJson = &policy_store.schema.json;
    let namespace = policy_store.namespace();
    let mut errors = Vec::new();

    if let Some(token) = tokens.id_token.as_ref() {
        match EntityMetadata::new(
            EntityParsedTypeName {
                typename: entity_mapping.unwrap_or("User"),
                namespace,
            },
            token.user_mapping(),
        )
        .create_entity(schema, token, parents.clone(), token.claim_mapping())
        {
            Ok(entity) => return Ok(entity),
            Err(e) => errors.push((TokenKind::Id, e)),
        }
    } else {
        errors.push((TokenKind::Id, CreateCedarEntityError::UnavailableToken));
    }

    if let Some(token) = tokens.access_token.as_ref() {
        match EntityMetadata::new(
            EntityParsedTypeName {
                typename: entity_mapping.unwrap_or("User"),
                namespace,
            },
            token.user_mapping(),
        )
        .create_entity(schema, token, parents.clone(), token.claim_mapping())
        {
            Ok(entity) => return Ok(entity),
            Err(e) => errors.push((TokenKind::Access, e)),
        }
    } else {
        errors.push((TokenKind::Access, CreateCedarEntityError::UnavailableToken));
    }

    if let Some(token) = tokens.userinfo_token.as_ref() {
        match EntityMetadata::new(
            EntityParsedTypeName {
                typename: entity_mapping.unwrap_or("User"),
                namespace,
            },
            token.user_mapping(),
        )
        .create_entity(schema, token, parents.clone(), token.claim_mapping())
        {
            Ok(entity) => return Ok(entity),
            Err(e) => errors.push((TokenKind::Userinfo, e)),
        }
    } else {
        errors.push((
            TokenKind::Userinfo,
            CreateCedarEntityError::UnavailableToken,
        ));
    }

    Err(CreateUserEntityError { errors })
}

#[derive(Debug, thiserror::Error)]
pub struct CreateUserEntityError {
    pub errors: Vec<(TokenKind, CreateCedarEntityError)>,
}

impl fmt::Display for CreateUserEntityError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        if self.errors.is_empty() {
            writeln!(
                f,
                "Failed to create User Entity since no tokens were provided"
            )?;
        } else {
            writeln!(
                f,
                "Failed to create User Entity due to the following errors:"
            )?;
            for (token_kind, error) in &self.errors {
                writeln!(f, "- TokenKind {:?}: {}", token_kind, error)?;
            }
        }
        Ok(())
    }
}

#[cfg(test)]
mod test {
    use super::create_user_entity;
    use crate::{
        authz::entities::DecodedTokens, common::policy_store::TokenKind,
        init::policy_store::load_policy_store, jwt::Token, CreateCedarEntityError,
        PolicyStoreConfig, PolicyStoreSource,
    };
    use cedar_policy::{Entity, RestrictedExpression};
    use serde_json::json;
    use std::{
        collections::{HashMap, HashSet},
        path::Path,
    };
    use test_utils::assert_eq;

    #[test]
    fn can_create_from_id_token() {
        let entity_mapping = None;
        let policy_store = load_policy_store(&PolicyStoreConfig {
            source: PolicyStoreSource::FileYaml(
                Path::new("../test_files/policy-store_ok_2.yaml").into(),
            ),
        })
        .expect("Should load policy store")
        .store;

        let tokens = DecodedTokens {
            access_token: None,
            id_token: Some(Token::new_id(
                HashMap::from([
                    ("sub".to_string(), json!("user-1")),
                    ("country".to_string(), json!("US")),
                ])
                .into(),
                None,
            )),
            userinfo_token: None,
        };
        let result = create_user_entity(entity_mapping, &policy_store, &tokens, HashSet::new())
            .expect("expected to create user entity");
        assert_eq!(
            result,
            Entity::new(
                "Jans::User::\"user-1\""
                    .parse()
                    .expect("expected to create user UID"),
                HashMap::from([(
                    "country".to_string(),
                    RestrictedExpression::new_string("US".to_string())
                )]),
                HashSet::new(),
            )
            .expect("should create expected user entity")
        )
    }

    #[test]
    fn can_create_from_access_token() {
        let entity_mapping = None;
        let policy_store = load_policy_store(&PolicyStoreConfig {
            source: PolicyStoreSource::FileYaml(
                Path::new("../test_files/policy-store_ok_2.yaml").into(),
            ),
        })
        .expect("Should load policy store")
        .store;

        let tokens = DecodedTokens {
            access_token: Some(Token::new_access(
                HashMap::from([
                    ("sub".to_string(), json!("user-1")),
                    ("country".to_string(), json!("US")),
                ])
                .into(),
                None,
            )),
            id_token: None,
            userinfo_token: None,
        };
        let result = create_user_entity(entity_mapping, &policy_store, &tokens, HashSet::new())
            .expect("expected to create user entity");
        assert_eq!(
            result,
            Entity::new(
                "Jans::User::\"user-1\""
                    .parse()
                    .expect("expected to create user UID"),
                HashMap::from([(
                    "country".to_string(),
                    RestrictedExpression::new_string("US".to_string())
                )]),
                HashSet::new(),
            )
            .expect("should create expected user entity")
        )
    }

    #[test]
    fn can_create_from_userinfo_token() {
        let entity_mapping = None;
        let policy_store = load_policy_store(&PolicyStoreConfig {
            source: PolicyStoreSource::FileYaml(
                Path::new("../test_files/policy-store_ok_2.yaml").into(),
            ),
        })
        .expect("Should load policy store")
        .store;

        let tokens = DecodedTokens {
            id_token: None,
            access_token: None,
            userinfo_token: Some(Token::new_userinfo(
                HashMap::from([
                    ("sub".to_string(), json!("user-1")),
                    ("country".to_string(), json!("US")),
                ])
                .into(),
                None,
            )),
        };
        let result = create_user_entity(entity_mapping, &policy_store, &tokens, HashSet::new())
            .expect("expected to create user entity");
        assert_eq!(
            result,
            Entity::new(
                "Jans::User::\"user-1\""
                    .parse()
                    .expect("expected to create user UID"),
                HashMap::from([(
                    "country".to_string(),
                    RestrictedExpression::new_string("US".to_string())
                )]),
                HashSet::new(),
            )
            .expect("should create expected user entity")
        )
    }

    #[test]
    fn errors_when_tokens_have_missing_claims() {
        let entity_mapping = None;
        let policy_store = load_policy_store(&PolicyStoreConfig {
            source: PolicyStoreSource::FileYaml(
                Path::new("../test_files/policy-store_ok_2.yaml").into(),
            ),
        })
        .expect("Should load policy store")
        .store;

        let tokens = DecodedTokens {
            access_token: Some(Token::new_access(HashMap::from([]).into(), None)),
            id_token: Some(Token::new_id(HashMap::from([]).into(), None)),
            userinfo_token: Some(Token::new_userinfo(HashMap::from([]).into(), None)),
        };

        let result = create_user_entity(entity_mapping, &policy_store, &tokens, HashSet::new())
            .expect_err("expected to error while creating user entity");

        for (tkn_kind, err) in result.errors.iter() {
            match tkn_kind {
                TokenKind::Access => assert!(
                    matches!(err, CreateCedarEntityError::MissingClaim(ref claim) if claim == "sub"),
                    "expected error MissingClaim(\"sub\")"
                ),
                TokenKind::Id => assert!(
                    matches!(err, CreateCedarEntityError::MissingClaim(ref claim) if claim == "sub"),
                    "expected error MissingClaim(\"sub\")"
                ),
                TokenKind::Userinfo => assert!(
                    matches!(err, CreateCedarEntityError::MissingClaim(ref claim) if claim == "sub"),
                    "expected error MissingClaim(\"sub\")"
                ),
                TokenKind::Transaction => (), // we don't support these yet
            }
        }
    }

    #[test]
    fn errors_when_tokens_unavailable() {
        let entity_mapping = None;
        let policy_store = load_policy_store(&PolicyStoreConfig {
            source: PolicyStoreSource::FileYaml(
                Path::new("../test_files/policy-store_ok_2.yaml").into(),
            ),
        })
        .expect("Should load policy store")
        .store;

        let tokens = DecodedTokens {
            access_token: None,
            id_token: None,
            userinfo_token: None,
        };

        let result = create_user_entity(entity_mapping, &policy_store, &tokens, HashSet::new())
            .expect_err("expected to error while creating user entity");

        assert_eq!(result.errors.len(), 3);
        for (_tkn_kind, err) in result.errors.iter() {
            assert!(
                matches!(err, CreateCedarEntityError::UnavailableToken),
                "expected error UnavailableToken, got: {:?}",
                err
            );
        }
    }
}
