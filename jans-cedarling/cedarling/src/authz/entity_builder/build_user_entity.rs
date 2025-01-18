// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::*;
use cedar_policy::Entity;
use std::collections::HashSet;

impl EntityBuilder {
    pub fn build_user_entity(
        &self,
        tokens: &HashMap<String, Token>,
        parents: HashSet<EntityUid>,
    ) -> Result<Entity, BuildUserEntityError> {
        let entity_name = self.entity_names.user.as_ref();
        let mut errors = vec![];

        let token_refs = [tokens.get("userinfo_token"), tokens.get("id_token")]
            .into_iter()
            .filter_map(|x| x);
        for token in token_refs {
            let user_id_claim = token.user_mapping();
            match build_entity(
                &self.schema,
                entity_name,
                token,
                user_id_claim,
                Vec::new(),
                parents.clone(),
                &HashMap::new(),
            ) {
                Ok(entity) => return Ok(entity),
                Err(err) => errors.push((token.name.clone(), err)),
            }
        }

        Err(BuildUserEntityError { errors })
    }
}

#[derive(Debug, thiserror::Error)]
pub struct BuildUserEntityError {
    pub errors: Vec<(String, BuildEntityError)>,
}

impl fmt::Display for BuildUserEntityError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        if self.errors.is_empty() {
            writeln!(
                f,
                "failed to create User Entity since no tokens were provided"
            )?;
        } else {
            writeln!(
                f,
                "failed to create User Entity due to the following errors:"
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
    use crate::common::cedar_schema::cedar_json::CedarSchemaJson;
    use crate::common::policy_store::{ClaimMappings, TokenEntityMetadata, TrustedIssuer};
    use crate::jwt::TokenClaims;
    use cedar_policy::EvalResult;
    use serde_json::json;
    use std::collections::HashMap;
    use test_utils::assert_eq;

    fn test_iss() -> TrustedIssuer {
        let token_entity_metadata = TokenEntityMetadata {
            claim_mapping: serde_json::from_value::<ClaimMappings>(json!({
                "email": {
                    "parser": "regex",
                    "type": "Jans::Email",
                    "regex_expression" : "^(?P<UID>[^@]+)@(?P<DOMAIN>.+)$",
                    "UID": {"attr": "uid", "type":"String"},
                    "DOMAIN": {"attr": "domain", "type":"String"},
                },
            }))
            .unwrap(),
            ..Default::default()
        };
        TrustedIssuer {
            tokens_metadata: HashMap::from([
                ("id_token".to_string(), token_entity_metadata.clone()),
                ("userinfo_token".to_string(), token_entity_metadata),
            ]),
            ..Default::default()
        }
    }

    fn test_schema() -> CedarSchemaJson {
        serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": {
                "commonTypes": {
                    "Email": {
                        "type": "Record",
                        "attributes": {
                            "uid": { "type": "String" },
                            "domain": { "type": "String" },
                        }
                    },
                    "Url": {
                        "type": "Record",
                        "attributes": {
                            "scheme": { "type": "String" },
                            "path": { "type": "String" },
                            "domain": { "type": "String" },
                        },
                    },
                },
                "entityTypes": {
                    "Role": {},
                    "User": {
                        "memberOf": ["Role"],
                        "shape": {
                            "type": "Record",
                            "attributes":  {
                                "email": { "type": "EntityOrCommon", "name": "Email" },
                                "sub": { "type": "String" },
                        },
                    }
                }
            }
        }}))
        .expect("should successfully create test schema")
    }

    fn test_successfully_building_user_entity(tokens: HashMap<String, Token<'_>>) {
        let schema = test_schema();
        let builder =
            EntityBuilder::new(schema, EntityNames::default(), false, true, HashMap::new());
        let entity = builder
            .build_user_entity(&tokens, HashSet::new())
            .expect("expected to build user entity");

        assert_eq!(entity.uid().to_string(), "Jans::User::\"user-123\"");

        assert_eq!(
            entity.attr("sub").unwrap().unwrap(),
            EvalResult::String("user-123".to_string()),
        );

        let email = entity
            .attr("email")
            .expect("entity must have an `email` attribute")
            .unwrap();
        if let EvalResult::Record(ref record) = email {
            assert_eq!(record.len(), 2);
            assert_eq!(
                record.get("uid").unwrap(),
                &EvalResult::String("test".to_string())
            );
            assert_eq!(
                record.get("domain").unwrap(),
                &EvalResult::String("email.com".to_string())
            );
        } else {
            panic!(
                "expected the attribute `email` to be a record, got: {:?}",
                email
            );
        }
    }

    #[test]
    fn can_build_using_userinfo_tkn() {
        let iss = test_iss();
        let userinfo_token = Token::new(
            "userinfo_token",
            TokenClaims::new(HashMap::from([
                ("email".to_string(), json!("test@email.com")),
                ("sub".to_string(), json!("user-123")),
                ("role".to_string(), json!(["admin", "user"])),
            ])),
            Some(&iss),
        );
        let tokens = HashMap::from([("userinfo_token".to_string(), userinfo_token)]);
        test_successfully_building_user_entity(tokens);
    }

    #[test]
    fn can_build_using_id_tkn() {
        let iss = test_iss();
        let id_token = Token::new(
            "id_token",
            TokenClaims::new(HashMap::from([
                ("email".to_string(), json!("test@email.com")),
                ("sub".to_string(), json!("user-123")),
                ("role".to_string(), json!(["admin", "user"])),
            ])),
            Some(&iss),
        );
        let tokens = HashMap::from([("id_token".to_string(), id_token)]);
        test_successfully_building_user_entity(tokens);
    }

    #[test]
    fn errors_when_token_has_missing_claim() {
        let iss = test_iss();
        let schema = test_schema();

        let id_token = Token::new("id_token", TokenClaims::new(HashMap::new()), Some(&iss));
        let userinfo_token = Token::new(
            "userinfo_token",
            TokenClaims::new(HashMap::new()),
            Some(&iss),
        );
        let tokens = HashMap::from([
            ("id_token".to_string(), id_token),
            ("userinfo_token".to_string(), userinfo_token),
        ]);

        let builder =
            EntityBuilder::new(schema, EntityNames::default(), false, true, HashMap::new());
        let err = builder
            .build_user_entity(&tokens, HashSet::new())
            .expect_err("expected to error while building the user entity");

        assert_eq!(err.errors.len(), 2);
        for (i, expected_tkn) in ["userinfo_token", "id_token"].iter().enumerate() {
            assert!(
                matches!(
                    err.errors[i],
                    (ref tkn_name, BuildEntityError::MissingClaim(ref claim_name))
                        if tkn_name == expected_tkn &&
                            claim_name == "sub"
                ),
                "expected an error due to missing the `sub` claim, got: {:?}",
                err.errors[i]
            );
        }
    }

    #[test]
    fn errors_when_tokens_unavailable() {
        let schema = test_schema();

        let tokens = HashMap::new();

        let builder =
            EntityBuilder::new(schema, EntityNames::default(), false, true, HashMap::new());
        let err = builder
            .build_user_entity(&tokens, HashSet::new())
            .expect_err("expected to error while building the user entity");

        assert_eq!(err.errors.len(), 0);
    }

    #[test]
    fn can_build_entity_with_roles() {
        let iss = test_iss();
        let userinfo_token = Token::new(
            "userinfo_token",
            TokenClaims::new(HashMap::from([
                ("sub".to_string(), json!("user-123")),
                ("email".to_string(), json!("someone@email.com")),
                ("role".to_string(), json!(["role1", "role2", "role3"])),
            ])),
            Some(&iss),
        );
        let tokens = HashMap::from([("userinfo_token".to_string(), userinfo_token)]);
        let schema = test_schema();
        let builder =
            EntityBuilder::new(schema, EntityNames::default(), false, true, HashMap::new());
        let roles = HashSet::from([
            "Role::\"role1\"".parse().unwrap(),
            "Role::\"role2\"".parse().unwrap(),
            "Role::\"role3\"".parse().unwrap(),
        ]);

        let user_entity = builder
            .build_user_entity(&tokens, roles.clone())
            .expect("expected to build user entity");

        let (_, _, parents) = user_entity.into_inner();
        assert_eq!(parents, roles,);
    }
}
