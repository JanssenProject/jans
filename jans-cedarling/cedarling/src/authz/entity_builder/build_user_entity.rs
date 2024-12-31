// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::*;
use crate::jwt::Token;
use cedar_policy::Entity;
use std::collections::HashSet;

impl EntityBuilder {
    pub fn build_user_entity(
        &self,
        tokens: &DecodedTokens,
    ) -> Result<Entity, BuildUserEntityError> {
        let mut errors = vec![];

        let try_build_entity = |claim_name: &str,
                                token: &Token,
                                claim_aliases: Vec<(&str, &str)>| {
            let mut entity_name = self.entity_names.user.clone();
            let entity_id = token
                .get_claim(claim_name)
                .ok_or(BuildEntityError::MissingClaim(claim_name.to_string()))?
                .as_str()?
                .to_owned();

            // get entity namespace and type
            let (namespace, entity_type) = self
                .schema
                .get_entity_type(&entity_name)
                .ok_or(BuildEntityError::EntityNotInSchema(entity_name.to_string()))?;
            if !namespace.is_empty() {
                entity_name = [namespace.as_str(), &entity_name].join(CEDAR_NAMESPACE_SEPARATOR);
            }

            let entity_attrs = self.build_entity_attrs(entity_type, token, claim_aliases)?;

            build_entity(&entity_name, &entity_id, entity_attrs, HashSet::new())
        };

        if let Some(token) = tokens.userinfo_token.as_ref() {
            let claim_name = token.user_mapping();
            match try_build_entity(claim_name, token, vec![]) {
                Ok(entity) => return Ok(entity),
                Err(err) => errors.push((TokenKind::Userinfo, err)),
            }
        }

        if let Some(token) = tokens.id_token.as_ref() {
            let claim_name = token.user_mapping();
            match try_build_entity(claim_name, token, vec![]) {
                Ok(entity) => return Ok(entity),
                Err(err) => errors.push((TokenKind::Id, err)),
            }
        }

        Err(BuildUserEntityError { errors })
    }
}

#[derive(Debug, thiserror::Error)]
pub struct BuildUserEntityError {
    pub errors: Vec<(TokenKind, BuildEntityError)>,
}

impl fmt::Display for BuildUserEntityError {
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
    use super::super::*;
    use crate::common::cedar_schema::new_cedar_json::CedarSchemaJson;
    use crate::common::policy_store::{ClaimMappings, TokenEntityMetadata, TrustedIssuer};
    use crate::jwt::{Token, TokenClaims};
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
            id_tokens: token_entity_metadata.clone(),
            userinfo_tokens: token_entity_metadata,
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
                        },
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
                    "memberOfTypes": ["Role"],
                    "shape": {
                        "type": "Record",
                        "attributes":  {
                            "email": { "type": "EntityOrCommon", "name": "Email" },
                            "sub": { "type": "String" },
                            "role": { "type": "Set", "element": { "type": "String" }},
                    },
                }
            }}}
        }))
        .expect("should successfully create test schema")
    }

    fn test_successfully_building_user_entity(tokens: DecodedTokens, iss: TrustedIssuer) {
        let schema = test_schema();
        let issuers = HashMap::from([("test_iss".into(), iss.clone())]);
        let builder = EntityBuilder::new(issuers, schema, EntityNames::default());
        let entity = builder
            .build_user_entity(&tokens)
            .expect("expected to build user entity");

        assert_eq!(entity.uid().to_string(), "Jans::User::\"user-123\"");

        assert_eq!(
            entity.attr("sub").unwrap().unwrap(),
            EvalResult::String("user-123".to_string()),
        );

        let role = entity
            .attr("role")
            .expect("expected role attribute to be present")
            .unwrap();
        if let EvalResult::Set(set) = role {
            assert_eq!(set.len(), 2);
            assert!(set.contains(&EvalResult::String("admin".to_string())));
            assert!(set.contains(&EvalResult::String("user".to_string())));
        } else {
            panic!("expected role attribute to be of kind EvalResult::Set, got: {role:?}");
        }

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
        let userinfo_token = Token::new_userinfo(
            TokenClaims::new(HashMap::from([
                ("email".to_string(), json!("test@email.com")),
                ("sub".to_string(), json!("user-123")),
                ("role".to_string(), json!(["admin", "user"])),
            ])),
            Some(&iss),
        );
        let tokens = DecodedTokens {
            access_token: None,
            id_token: None,
            userinfo_token: Some(userinfo_token),
        };
        test_successfully_building_user_entity(tokens, iss.clone());
    }

    #[test]
    fn can_build_using_id_tkn() {
        let iss = test_iss();
        let id_token = Token::new_id(
            TokenClaims::new(HashMap::from([
                ("email".to_string(), json!("test@email.com")),
                ("sub".to_string(), json!("user-123")),
                ("role".to_string(), json!(["admin", "user"])),
            ])),
            Some(&iss),
        );
        let tokens = DecodedTokens {
            access_token: None,
            id_token: Some(id_token),
            userinfo_token: None,
        };
        test_successfully_building_user_entity(tokens, iss.clone());
    }

    #[test]
    fn errors_when_token_has_missing_claim() {
        let iss = test_iss();
        let schema = test_schema();

        let id_token = Token::new_id(TokenClaims::new(HashMap::new()), Some(&iss));
        let userinfo_token = Token::new_userinfo(TokenClaims::new(HashMap::new()), Some(&iss));
        let tokens = DecodedTokens {
            access_token: None,
            id_token: Some(id_token),
            userinfo_token: Some(userinfo_token),
        };

        let issuers = HashMap::from([("test_iss".into(), iss.clone())]);
        let builder = EntityBuilder::new(issuers, schema, EntityNames::default());
        let err = builder
            .build_user_entity(&tokens)
            .expect_err("expected to error while building the workload entity");

        assert_eq!(err.errors.len(), 2);
        for (i, expected_kind) in [TokenKind::Userinfo, TokenKind::Id].iter().enumerate() {
            assert!(
                matches!(
                    err.errors[i],
                    (ref tkn_kind, BuildEntityError::MissingClaim(ref claim_name))
                        if tkn_kind == expected_kind &&
                            claim_name == "sub"
                ),
                "expected an error due to missing the `sub` claim"
            );
        }
    }

    #[test]
    fn errors_when_tokens_unavailable() {
        let iss = test_iss();
        let schema = test_schema();

        let tokens = DecodedTokens {
            access_token: None,
            id_token: None,
            userinfo_token: None,
        };

        let issuers = HashMap::from([("test_iss".into(), iss.clone())]);
        let builder = EntityBuilder::new(issuers, schema, EntityNames::default());
        let err = builder
            .build_user_entity(&tokens)
            .expect_err("expected to error while building the workload entity");

        assert_eq!(err.errors.len(), 0);
    }
}
