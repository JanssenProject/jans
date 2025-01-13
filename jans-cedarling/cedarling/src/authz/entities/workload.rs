// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashSet;
use std::fmt;

use super::{CreateCedarEntityError, DecodedTokens, EntityMetadata, EntityParsedTypeName};
use crate::common::policy_store::{PolicyStore, TokenKind};
use crate::jwt::Token;

/// Create workload entity
pub fn create_workload_entity(
    entity_mapping: Option<&str>,
    policy_store: &PolicyStore,
    tokens: &DecodedTokens,
) -> Result<cedar_policy::Entity, CreateWorkloadEntityError> {
    let namespace = policy_store.namespace();
    let schema = &policy_store.schema.json;
    let mut errors = Vec::new();

    // helper closure to attempt entity creation from a token
    let try_create_entity = |token_kind: TokenKind, token: Option<&Token>, key: &str| {
        if let Some(token) = token {
            let claim_mapping = token.claim_mapping();
            let entity_metadta = EntityMetadata::new(
                EntityParsedTypeName {
                    type_name: entity_mapping.unwrap_or("Workload"),
                    namespace,
                },
                key,
            );
            entity_metadta
                .create_entity(schema, token, HashSet::new(), claim_mapping)
                .map_err(|e| (token_kind, e))
        } else {
            Err((token_kind, CreateCedarEntityError::UnavailableToken))
        }
    };

    // attempt entity creation for each token type
    for (token_kind, token, key) in [
        (TokenKind::Access, tokens.access_token.as_ref(), "client_id"),
        (TokenKind::Id, tokens.id_token.as_ref(), "aud"),
    ] {
        match try_create_entity(token_kind, token, key) {
            Ok(entity) => return Ok(entity),
            Err(e) => errors.push(e),
        }
    }

    Err(CreateWorkloadEntityError { errors })
}

#[derive(Debug, thiserror::Error)]
pub struct CreateWorkloadEntityError {
    pub errors: Vec<(TokenKind, CreateCedarEntityError)>,
}

impl fmt::Display for CreateWorkloadEntityError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        if self.errors.is_empty() {
            writeln!(
                f,
                "Failed to create Workload Entity since no tokens were provided"
            )?;
        } else {
            writeln!(
                f,
                "Failed to create Workload Entity due to the following errors:"
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
    use std::collections::{HashMap, HashSet};
    use std::path::Path;

    use cedar_policy::{Entity, RestrictedExpression};
    use serde_json::json;
    use test_utils::assert_eq;
    use tokio::test;

    use super::create_workload_entity;
    use crate::authz::entities::DecodedTokens;
    use crate::common::policy_store::TokenKind;
    use crate::init::policy_store::load_policy_store;
    use crate::jwt::Token;
    use crate::{CreateCedarEntityError, PolicyStoreConfig, PolicyStoreSource};

    #[test]
    async fn can_create_from_id_token() {
        let entity_mapping = None;
        let policy_store = load_policy_store(&PolicyStoreConfig {
            source: PolicyStoreSource::FileYaml(
                Path::new("../test_files/policy-store_ok_2.yaml").into(),
            ),
        })
        .await
        .expect("Should load policy store")
        .store;

        let tokens = DecodedTokens {
            access_token: None,
            id_token: Some(Token::new_id(
                HashMap::from([
                    ("aud".to_string(), json!("workload-1")),
                    ("org_id".to_string(), json!("some-org-123")),
                ])
                .into(),
                None,
            )),
            userinfo_token: None,
        };
        let result = create_workload_entity(entity_mapping, &policy_store, &tokens)
            .expect("expected to create workload entity");
        assert_eq!(
            result,
            Entity::new(
                "Jans::Workload::\"workload-1\""
                    .parse()
                    .expect("expected to create workload UID"),
                HashMap::from([(
                    "org_id".to_string(),
                    RestrictedExpression::new_string("some-org-123".to_string())
                )]),
                HashSet::new(),
            )
            .expect("should create expected workload entity")
        )
    }

    #[test]
    async fn can_create_from_access_token() {
        let entity_mapping = None;
        let policy_store = load_policy_store(&PolicyStoreConfig {
            source: PolicyStoreSource::FileYaml(
                Path::new("../test_files/policy-store_ok_2.yaml").into(),
            ),
        })
        .await
        .expect("Should load policy store")
        .store;

        let tokens = DecodedTokens {
            access_token: Some(Token::new_access(
                HashMap::from([
                    ("client_id".to_string(), json!("workload-1")),
                    ("org_id".to_string(), json!("some-org-123")),
                ])
                .into(),
                None,
            )),
            id_token: None,
            userinfo_token: None,
        };
        let result = create_workload_entity(entity_mapping, &policy_store, &tokens)
            .expect("expected to create workload entity");
        assert_eq!(
            result,
            Entity::new(
                "Jans::Workload::\"workload-1\""
                    .parse()
                    .expect("expected to create workload UID"),
                HashMap::from([(
                    "org_id".to_string(),
                    RestrictedExpression::new_string("some-org-123".to_string())
                )]),
                HashSet::new(),
            )
            .expect("should create expected workload entity")
        )
    }

    #[test]
    async fn errors_when_tokens_have_missing_claims() {
        let entity_mapping = None;
        let policy_store = load_policy_store(&PolicyStoreConfig {
            source: PolicyStoreSource::FileYaml(
                Path::new("../test_files/policy-store_ok_2.yaml").into(),
            ),
        })
        .await
        .expect("Should load policy store")
        .store;

        let tokens = DecodedTokens {
            access_token: Some(Token::new_access(HashMap::from([]).into(), None)),
            id_token: Some(Token::new_id(HashMap::from([]).into(), None)),
            userinfo_token: Some(Token::new_userinfo(HashMap::from([]).into(), None)),
        };

        let result = create_workload_entity(entity_mapping, &policy_store, &tokens)
            .expect_err("expected to error while creating workload entity");

        for (tkn_kind, err) in result.errors.iter() {
            match tkn_kind {
                TokenKind::Access => assert!(
                    matches!(err, CreateCedarEntityError::MissingClaim(ref claim) if claim == "client_id"),
                    "expected error MissingClaim(\"client_id\")"
                ),
                TokenKind::Id => assert!(
                    matches!(err, CreateCedarEntityError::MissingClaim(ref claim) if claim == "aud"),
                    "expected error MissingClaim(\"aud\")"
                ),
                _ => (), // we don't create workload tokens using other tokens
            }
        }
    }

    #[test]
    async fn errors_when_tokens_unavailable() {
        let entity_mapping = None;
        let policy_store = load_policy_store(&PolicyStoreConfig {
            source: PolicyStoreSource::FileYaml(
                Path::new("../test_files/policy-store_ok_2.yaml").into(),
            ),
        })
        .await
        .expect("Should load policy store")
        .store;

        // we can only create the workload from the access_token and id_token
        let tokens = DecodedTokens {
            access_token: None,
            id_token: None,
            userinfo_token: None,
        };

        let result = create_workload_entity(entity_mapping, &policy_store, &tokens)
            .expect_err("expected to error while creating workload entity");

        assert_eq!(result.errors.len(), 2);
        for (_tkn_kind, err) in result.errors.iter() {
            assert!(
                matches!(err, CreateCedarEntityError::UnavailableToken),
                "expected error UnavailableToken, got: {:?}",
                err
            );
        }
    }
}
