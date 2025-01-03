// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::*;
use cedar_policy::{EntityId, EntityTypeName, EntityUid};

const DEFAULT_ROLE_ENTITY_NAME: &str = "Role";

impl EntityBuilder {
    pub fn build_role_entities(
        &self,
        tokens: &DecodedTokens,
    ) -> Result<Vec<Entity>, BuildRoleEntityError> {
        let entity_name = DEFAULT_ROLE_ENTITY_NAME;
        let mut errors = Vec::new();
        let mut entities = Vec::new();

        // Get entity namespace and type
        let mut entity_name = entity_name.to_string();
        if let Some((namespace, _entity_type)) = self.schema.get_entity_type(&entity_name) {
            if !namespace.is_empty() {
                entity_name = [namespace.as_str(), &entity_name].join(CEDAR_NAMESPACE_SEPARATOR);
            }
        }

        for token in [
            tokens.userinfo.as_ref(),
            tokens.id.as_ref(),
            tokens.access.as_ref(),
        ]
        .into_iter()
        .flatten()
        {
            let role_claim = token.role_mapping();
            if let Some(claim) = token.get_claim(role_claim).as_ref() {
                match claim.value() {
                    serde_json::Value::String(role) => {
                        match build_entity(&entity_name, role) {
                            Ok(entity) => {
                                entities.push(entity);
                            },
                            Err(err) => {
                                errors.push((token.kind, err));
                            },
                        };
                    },
                    serde_json::Value::Array(vec) => {
                        for val in vec {
                            let role = match val.as_str() {
                                Some(role) => role,
                                None => {
                                    errors.push((
                                        token.kind,
                                        BuildEntityError::json_type_err("str", val),
                                    ));
                                    continue;
                                },
                            };

                            match build_entity(&entity_name, role) {
                                Ok(entity) => {
                                    entities.push(entity);
                                },
                                Err(err) => {
                                    errors.push((token.kind, err));
                                },
                            };
                        }
                    },
                    _ => unimplemented!(),
                }
            } else {
                errors.push((
                    token.kind,
                    BuildEntityError::MissingClaim(role_claim.to_string()),
                ));
            }
        }

        if entities.is_empty() {
            Err(BuildRoleEntityError { errors })
        } else {
            return Ok(entities);
        }
    }
}

fn build_entity(name: &str, id: &str) -> Result<Entity, BuildEntityError> {
    let name = EntityTypeName::from_str(&name).map_err(BuildEntityError::ParseEntityTypeName)?;
    let id = EntityId::from_str(id).expect("expected infallible");
    let uid = EntityUid::from_type_name_and_id(name, id);
    let entity = Entity::new(uid, HashMap::new(), HashSet::new())?;
    Ok(entity)
}

#[derive(Debug, thiserror::Error)]
pub struct BuildRoleEntityError {
    pub errors: Vec<(TokenKind, BuildEntityError)>,
}

impl fmt::Display for BuildRoleEntityError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        if self.errors.is_empty() {
            writeln!(
                f,
                "Failed to create Role Entity since no tokens were provided"
            )?;
        } else {
            writeln!(
                f,
                "Failed to create Role Entity due to the following errors:"
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
    use super::super::*;
    use crate::common::cedar_schema::new_cedar_json::CedarSchemaJson;
    use crate::common::policy_store::TrustedIssuer;
    use crate::jwt::{Token, TokenClaims};
    use serde_json::json;
    use std::collections::HashMap;

    fn test_schema() -> CedarSchemaJson {
        serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": {
                "entityTypes": {
                    "Role": {},
                    "User": {
                        "memberOfTypes": ["Role"],
                        "shape": {
                            "type": "Record",
                            "attributes":  {},
                    }
                }}}
        }))
        .expect("should successfully create test schema")
    }

    fn test_build_entity_from_str_claim(tokens: DecodedTokens, iss: TrustedIssuer) {
        let schema = test_schema();
        let issuers = HashMap::from([("test_iss".into(), iss.clone())]);
        let builder = EntityBuilder::new(issuers, schema, EntityNames::default());
        let entity = builder
            .build_role_entities(&tokens)
            .expect("expected to build role entities");

        assert_eq!(entity.len(), 1);
        assert_eq!(entity[0].uid().to_string(), "Jans::Role::\"admin\"");
    }

    #[test]
    fn can_build_using_userinfo_tkn_vec_claim() {
        let iss = TrustedIssuer::default();
        let userinfo_token = Token::new_userinfo(
            TokenClaims::new(HashMap::from([(
                "role".to_string(),
                json!(["admin", "user"]),
            )])),
            Some(&iss),
        );
        let tokens = DecodedTokens {
            access: None,
            id: None,
            userinfo: Some(userinfo_token),
        };
        let schema = test_schema();
        let issuers = HashMap::from([("test_iss".into(), iss.clone())]);
        let builder = EntityBuilder::new(issuers, schema, EntityNames::default());
        let entity = builder
            .build_role_entities(&tokens)
            .expect("expected to build role entities");

        assert_eq!(entity.len(), 2);
        assert_eq!(entity[0].uid().to_string(), "Jans::Role::\"admin\"");
        assert_eq!(entity[1].uid().to_string(), "Jans::Role::\"user\"");
    }

    #[test]
    fn can_build_using_userinfo_tkn_string_claim() {
        let iss = TrustedIssuer::default();
        let userinfo_token = Token::new_userinfo(
            TokenClaims::new(HashMap::from([("role".to_string(), json!("admin"))])),
            Some(&iss),
        );
        let tokens = DecodedTokens {
            access: None,
            id: None,
            userinfo: Some(userinfo_token),
        };
        test_build_entity_from_str_claim(tokens, iss.clone());
    }

    #[test]
    fn can_build_using_id_tkn() {
        let iss = TrustedIssuer::default();
        let id_token = Token::new_id(
            TokenClaims::new(HashMap::from([("role".to_string(), json!("admin"))])),
            Some(&iss),
        );
        let tokens = DecodedTokens {
            access: None,
            id: Some(id_token),
            userinfo: None,
        };
        test_build_entity_from_str_claim(tokens, iss.clone());
    }

    #[test]
    fn can_build_using_access_tkn() {
        let iss = TrustedIssuer::default();
        let access_token = Token::new_access(
            TokenClaims::new(HashMap::from([("role".to_string(), json!("admin"))])),
            Some(&iss),
        );
        let tokens = DecodedTokens {
            access: Some(access_token),
            id: None,
            userinfo: None,
        };
        test_build_entity_from_str_claim(tokens, iss.clone());
    }

    #[test]
    fn errors_when_token_is_missing_role_claim() {
        let iss = TrustedIssuer::default();
        let schema = test_schema();

        let access_token = Token::new_access(TokenClaims::new(HashMap::new()), Some(&iss));
        let id_token = Token::new_id(TokenClaims::new(HashMap::new()), Some(&iss));
        let userinfo_token = Token::new_userinfo(TokenClaims::new(HashMap::new()), Some(&iss));
        let tokens = DecodedTokens {
            access: Some(access_token),
            id: Some(id_token),
            userinfo: Some(userinfo_token),
        };

        let issuers = HashMap::from([("test_iss".into(), iss.clone())]);
        let builder = EntityBuilder::new(issuers, schema, EntityNames::default());
        let err = builder
            .build_role_entities(&tokens)
            .expect_err("expected to error while building the role entity");

        assert_eq!(err.errors.len(), 3);
        for (i, expected_kind) in [TokenKind::Userinfo, TokenKind::Id, TokenKind::Access]
            .iter()
            .enumerate()
        {
            assert!(
                matches!(
                    err.errors[i],
                    (ref tkn_kind, BuildEntityError::MissingClaim(ref claim_name))
                        if tkn_kind == expected_kind &&
                            claim_name == "role"
                ),
                "expected an error due to missing the `role` claim, got: {:?}",
                err.errors[i]
            );
        }
    }

    #[test]
    fn errors_when_tokens_unavailable() {
        let iss = TrustedIssuer::default();
        let schema = test_schema();

        let tokens = DecodedTokens {
            access: None,
            id: None,
            userinfo: None,
        };

        let issuers = HashMap::from([("test_iss".into(), iss.clone())]);
        let builder = EntityBuilder::new(issuers, schema, EntityNames::default());
        let err = builder
            .build_role_entities(&tokens)
            .expect_err("expected to error while building the role entity");

        assert_eq!(err.errors.len(), 0);
    }
}
