// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::*;
use crate::jwt::Token;
use cedar_policy::Entity;
use std::collections::HashSet;

impl EntityBuilder {
    pub fn build_workload_entity(
        &self,
        tokens: &DecodedTokens,
    ) -> Result<Entity, BuildWorkloadEntityError> {
        let mut errors = vec![];

        let try_build_entity = |claim_name: &str,
                                token: &Token,
                                claim_aliases: Vec<(&str, &str)>| {
            let mut entity_name = self.entity_names.workload.clone();
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

        if let Some(token) = tokens.access_token.as_ref() {
            match try_build_entity(DEFAULT_ACCESS_TKN_WORKLOAD_CLAIM, token, vec![]) {
                Ok(entity) => return Ok(entity),
                Err(err) => errors.push((TokenKind::Access, err)),
            }
        } else if let Some(token) = tokens.id_token.as_ref() {
            match try_build_entity(
                DEFAULT_ID_TKN_WORKLOAD_CLAIM,
                token,
                vec![("aud", "client_id")],
            ) {
                Ok(entity) => return Ok(entity),
                Err(err) => errors.push((TokenKind::Id, err)),
            }
        }

        Err(BuildWorkloadEntityError { errors })
    }
}

#[derive(Debug, thiserror::Error)]
pub struct BuildWorkloadEntityError {
    pub errors: Vec<(TokenKind, BuildEntityError)>,
}

impl fmt::Display for BuildWorkloadEntityError {
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
    use super::super::*;
    use crate::authz::entity_builder::BuildEntityError;
    use crate::common::cedar_schema::new_cedar_json::CedarSchemaJson;
    use crate::common::policy_store::{
        ClaimMappings, TokenEntityMetadata, TokenKind, TrustedIssuer,
    };
    use crate::jwt::{Token, TokenClaims};
    use cedar_policy::EvalResult;
    use serde_json::json;
    use std::collections::HashMap;
    use test_utils::assert_eq;

    #[test]
    fn can_build_using_access_tkn() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": { "entityTypes": { "Workload": {
                "shape": {
                    "type": "Record",
                    "attributes":  {
                        "client_id": { "type": "String" },
                        "name": { "type": "String" },
                    },
                }
            }}}
        }))
        .unwrap();
        let iss = TrustedIssuer::default();
        let issuers = HashMap::from([("test_iss".into(), iss.clone())]);
        let builder = EntityBuilder::new(issuers, schema, EntityNames::default());
        let access_token = Token::new_access(
            TokenClaims::new(HashMap::from([
                ("client_id".to_string(), json!("workload-123")),
                ("name".to_string(), json!("somename")),
            ])),
            Some(&iss),
        );
        let tokens = DecodedTokens {
            access_token: Some(access_token),
            id_token: None,
            userinfo_token: None,
        };
        let entity = builder.build_workload_entity(&tokens).unwrap();
        assert_eq!(entity.uid().to_string(), "Jans::Workload::\"workload-123\"");
        assert_eq!(
            entity.attr("client_id").unwrap().unwrap(),
            EvalResult::String("workload-123".to_string()),
        );
        assert_eq!(
            entity.attr("name").unwrap().unwrap(),
            EvalResult::String("somename".to_string()),
        );
    }

    #[test]
    fn can_build_using_id_tkn() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": { "entityTypes": { "Workload": {
                "shape": {
                    "type": "Record",
                    "attributes":  {
                        "client_id": { "type": "String" },
                        "name": { "type": "String" },
                    },
                }
            }}}
        }))
        .unwrap();
        let iss = TrustedIssuer::default();
        let issuers = HashMap::from([("test_iss".into(), iss.clone())]);
        let builder = EntityBuilder::new(issuers, schema, EntityNames::default());
        let id_token = Token::new_id(
            TokenClaims::new(HashMap::from([
                ("aud".to_string(), json!("workload-123")),
                ("name".to_string(), json!("somename")),
            ])),
            Some(&iss),
        );
        let tokens = DecodedTokens {
            access_token: None,
            id_token: Some(id_token),
            userinfo_token: None,
        };
        let entity = builder.build_workload_entity(&tokens).unwrap();
        assert_eq!(entity.uid().to_string(), "Jans::Workload::\"workload-123\"");
        assert_eq!(
            entity.attr("client_id").unwrap().unwrap(),
            EvalResult::String("workload-123".to_string()),
        );
        assert_eq!(
            entity.attr("name").unwrap().unwrap(),
            EvalResult::String("somename".to_string()),
        );
    }

    #[test]
    fn can_build_expression_with_regex_mapping() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
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
                    "Workload": {
                        "shape": {
                            "type": "Record",
                            "attributes":  {
                                "client_id": { "type": "String" },
                                "email": { "type": "EntityOrCommon", "name": "Email" },
                                "url": { "type": "EntityOrCommon", "name": "Url" },
                            },
                        }
                    }
            }}
        }))
        .unwrap();
        let iss = TrustedIssuer {
            access_tokens: TokenEntityMetadata {
                claim_mapping: serde_json::from_value::<ClaimMappings>(json!({
                    "email": {
                        "parser": "regex",
                        "type": "Jans::Email",
                        "regex_expression" : "^(?P<UID>[^@]+)@(?P<DOMAIN>.+)$",
                        "UID": {"attr": "uid", "type":"String"},
                        "DOMAIN": {"attr": "domain", "type":"String"},
                    },
                    "url": {
                        "parser": "regex",
                        "type": "Jans::Url",
                        "regex_expression": r#"^(?P<SCHEME>[a-zA-Z][a-zA-Z0-9+.-]*):\/\/(?P<DOMAIN>[^\/]+)(?P<PATH>\/.*)?$"#,
                        "SCHEME": {"attr": "scheme", "type": "String"},
                        "DOMAIN": {"attr": "domain", "type": "String"},
                        "PATH": {"attr": "path", "type": "String"}
                    }
                }))
                .unwrap(),
                ..Default::default()
            },
            ..Default::default()
        };
        let issuers = HashMap::from([("test_iss".into(), iss.clone())]);
        let builder = EntityBuilder::new(issuers, schema, EntityNames::default());
        let access_token = Token::new_access(
            TokenClaims::new(HashMap::from([
                ("client_id".to_string(), json!("workload-123")),
                ("email".to_string(), json!("test@example.com")),
                ("url".to_string(), json!("https://test.com/example")),
            ])),
            Some(&iss),
        );
        let tokens = DecodedTokens {
            access_token: Some(access_token),
            id_token: None,
            userinfo_token: None,
        };
        let entity = builder.build_workload_entity(&tokens).unwrap();

        assert_eq!(entity.uid().to_string(), "Jans::Workload::\"workload-123\"");

        assert_eq!(
            entity.attr("client_id").unwrap().unwrap(),
            EvalResult::String("workload-123".to_string()),
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
                &EvalResult::String("example.com".to_string())
            );
        } else {
            panic!(
                "expected the attribute `email` to be a record, got: {:?}",
                email
            );
        }

        let url = entity
            .attr("url")
            .expect("entity must have a `url` attribute")
            .unwrap();
        if let EvalResult::Record(ref record) = url {
            assert_eq!(record.len(), 3);
            assert_eq!(
                record.get("scheme").unwrap(),
                &EvalResult::String("https".to_string())
            );
            assert_eq!(
                record.get("domain").unwrap(),
                &EvalResult::String("test.com".to_string())
            );
            assert_eq!(
                record.get("path").unwrap(),
                &EvalResult::String("/example".to_string())
            );
        } else {
            panic!(
                "expected the attribute `url` to be a record, got: {:?}",
                email
            );
        }
    }

    #[test]
    fn errors_when_token_has_missing_claim() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": { "entityTypes": { "Workload": {
                "shape": {
                    "type": "Record",
                    "attributes":  {
                        "client_id": { "type": "String" },
                        "name": { "type": "String" },
                    },
                }
            }}}
        }))
        .unwrap();
        let iss = TrustedIssuer::default();
        let issuers = HashMap::from([("test_iss".into(), iss.clone())]);
        let builder = EntityBuilder::new(issuers, schema, EntityNames::default());
        let access_token = Token::new_access(TokenClaims::new(HashMap::new()), Some(&iss));
        let tokens = DecodedTokens {
            access_token: Some(access_token),
            id_token: None,
            userinfo_token: None,
        };
        let err = builder.build_workload_entity(&tokens).unwrap_err();

        assert_eq!(err.errors.len(), 1);
        assert!(matches!(
            err.errors[0],
            (ref tkn_kind, BuildEntityError::MissingClaim(ref claim_name))
                if tkn_kind == &TokenKind::Access &&
                    claim_name == "client_id"
        ));
    }

    #[test]
    fn errors_when_tokens_unavailable() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": { "entityTypes": { "Workload": {
                "shape": {
                    "type": "Record",
                    "attributes":  {
                        "client_id": { "type": "String" },
                        "name": { "type": "String" },
                    },
                }
            }}}
        }))
        .unwrap();
        let builder = EntityBuilder::new(HashMap::new(), schema, EntityNames::default());
        let tokens = DecodedTokens {
            access_token: None,
            id_token: None,
            userinfo_token: None,
        };
        let err = builder.build_workload_entity(&tokens).unwrap_err();

        assert_eq!(err.errors.len(), 0);
    }
}
