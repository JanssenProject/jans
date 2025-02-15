// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::*;
use cedar_policy::Entity;
use std::collections::HashSet;

// Default claims to use for the Workload Entity's ID.
const DEFAULT_ACCESS_TKN_WORKLOAD_CLAIM: &str = "client_id";
const DEFAULT_ID_TKN_WORKLOAD_CLAIM: &str = "aud";

// TODO: make a bootstrap property to control which tokens to use
// to create this entity
const DEFAULT_WORKLOAD_ENTITY_TKN_SRCS: [&str; 2] = ["access_token", "id_token"];

impl EntityBuilder {
    pub fn build_workload_entity(
        &self,
        tokens: &HashMap<String, Token>,
        built_entities: &BuiltEntities,
    ) -> Result<Entity, BuildWorkloadEntityError> {
        let entity_name = self.entity_names.workload.as_ref();
        let mut errors = vec![];

        for ((workload_id_claim, claim_aliases), token_name) in [
            (DEFAULT_ACCESS_TKN_WORKLOAD_CLAIM, Vec::new()),
            (DEFAULT_ID_TKN_WORKLOAD_CLAIM, vec![ClaimAliasMap::new(
                "aud",
                "client_id",
            )]),
        ]
        .into_iter()
        .zip(DEFAULT_WORKLOAD_ENTITY_TKN_SRCS)
        {
            if let Some(token) = tokens.get(token_name) {
                println!("token: {}\n", token.name);

                match build_entity(
                    &self.schema,
                    entity_name,
                    token,
                    workload_id_claim,
                    claim_aliases,
                    HashSet::new(),
                    built_entities,
                ) {
                    Ok(entity) => {
                        return Ok(entity);
                    },
                    Err(err) => errors.push((token.name.clone(), err)),
                }
            }
        }

        Err(BuildWorkloadEntityError { errors })
    }
}

#[derive(Debug, thiserror::Error)]
pub struct BuildWorkloadEntityError {
    pub errors: Vec<(String, BuildEntityError)>,
}

impl fmt::Display for BuildWorkloadEntityError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        if self.errors.is_empty() {
            writeln!(
                f,
                "failed to create Workload Entity since no tokens were provided"
            )?;
        } else {
            writeln!(
                f,
                "failed to create Workload Entity due to the following errors:"
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
    use crate::common::cedar_schema::cedar_json::CedarSchemaJson;
    use crate::common::policy_store::{ClaimMappings, TokenEntityMetadata, TrustedIssuer};
    use cedar_policy::EvalResult;
    use serde_json::json;
    use std::collections::HashMap;

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
        let builder = EntityBuilder::new(schema, EntityNames::default(), true, false);
        let access_token = Token::new(
            "access_token",
            HashMap::from([
                ("client_id".to_string(), json!("workload-123")),
                ("name".to_string(), json!("somename")),
            ])
            .into(),
            Some(&iss),
        );
        let tokens = HashMap::from([("access_token".to_string(), access_token)]);
        let entity = builder
            .build_workload_entity(&tokens, &BuiltEntities::default())
            .expect("expeted to successfully build workload entity");
        assert_eq!(entity.uid().to_string(), "Jans::Workload::\"workload-123\"");
        assert_eq!(
            entity
                .attr("client_id")
                .expect("expected workload entity to have a `client_id` attribute")
                .unwrap(),
            EvalResult::String("workload-123".to_string()),
        );
        assert_eq!(
            entity
                .attr("name")
                .expect("expected workload entity to have a `name` attribute")
                .unwrap(),
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
        let builder = EntityBuilder::new(schema, EntityNames::default(), true, false);
        let id_token = Token::new(
            "id_token",
            HashMap::from([
                ("aud".to_string(), json!("workload-123")),
                ("name".to_string(), json!("somename")),
            ])
            .into(),
            Some(&iss),
        );
        let tokens = HashMap::from([("id_token".to_string(), id_token)]);
        let entity = builder
            .build_workload_entity(&tokens, &BuiltEntities::default())
            .expect("expected to successfully build workload entity");
        assert_eq!(entity.uid().to_string(), "Jans::Workload::\"workload-123\"");
        assert_eq!(
            entity
                .attr("client_id")
                .expect("expected workload entity to have a `client_id` attribute")
                .unwrap(),
            EvalResult::String("workload-123".to_string()),
        );
        assert_eq!(
            entity
                .attr("name")
                .expect("expected workload entity to have a `name` attribute")
                .unwrap(),
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
            tokens_metadata: HashMap::from([("access_token".to_string(), TokenEntityMetadata {
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
            })]),
            ..Default::default()
        };
        let builder = EntityBuilder::new(schema, EntityNames::default(), true, false);
        let access_token = Token::new(
            "access_token",
            HashMap::from([
                ("client_id".to_string(), json!("workload-123")),
                ("email".to_string(), json!("test@example.com")),
                ("url".to_string(), json!("https://test.com/example")),
            ])
            .into(),
            Some(&iss),
        );
        let tokens = HashMap::from([("access_token".to_string(), access_token)]);
        let entity = builder
            .build_workload_entity(&tokens, &BuiltEntities::default())
            .expect("expected to successfully build workload entity");

        assert_eq!(entity.uid().to_string(), "Jans::Workload::\"workload-123\"");

        assert_eq!(
            entity
                .attr("client_id")
                .expect("expected to workload entity to have a `client_id` attribute")
                .unwrap(),
            EvalResult::String("workload-123".to_string()),
        );

        let email = entity
            .attr("email")
            .expect("expected workload entity to have an `email` attribute")
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
    fn can_build_entity_with_entity_ref() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": {
                "entityTypes": {
                    "TrustedIss": {},
                    "Workload": {
                        "shape": {
                            "type": "Record",
                            "attributes":  {
                                "client_id": { "type": "String" },
                                "iss": { "type": "EntityOrCommon", "name": "TrustedIss" },
                            },
                        }
                    }
            }}
        }))
        .unwrap();
        let iss = TrustedIssuer::default();
        let builder = EntityBuilder::new(schema, EntityNames::default(), true, false);
        let access_token = Token::new(
            "access_token",
            HashMap::from([
                ("client_id".to_string(), json!("workload-123")),
                (
                    "iss".to_string(),
                    json!("https://test.com/.well-known/openid-configuration"),
                ),
            ])
            .into(),
            Some(&iss),
        );
        let tokens = HashMap::from([("access_token".to_string(), access_token)]);
        let entity = builder
            .build_workload_entity(&tokens, &BuiltEntities::default())
            .expect("expected to successfully build workload entity");

        assert_eq!(entity.uid().to_string(), "Jans::Workload::\"workload-123\"");

        assert_eq!(
            entity
                .attr("client_id")
                .expect("expected to workload entity to have a `client_id` attribute")
                .unwrap(),
            EvalResult::String("workload-123".to_string()),
        );

        let iss = entity
            .attr("iss")
            .expect("entity must have a `iss` attribute")
            .unwrap();
        if let EvalResult::EntityUid(uid) = iss {
            assert_eq!(uid.type_name().namespace(), "Jans");
            assert_eq!(uid.type_name().basename(), "TrustedIss");
            assert_eq!(
                uid.id().escaped(),
                "https://test.com/.well-known/openid-configuration"
            );
        } else {
            panic!(
                "expected the attribute `iss` to be an EntityUid, got: {:?}",
                iss
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
        let builder = EntityBuilder::new(schema, EntityNames::default(), true, false);
        let access_token = Token::new("access_token", HashMap::new().into(), Some(&iss));
        let id_token = Token::new("id_token", HashMap::new().into(), Some(&iss));
        let tokens = HashMap::from([
            ("access_token".to_string(), access_token),
            ("id_token".to_string(), id_token),
        ]);
        let err = builder
            .build_workload_entity(&tokens, &BuiltEntities::default())
            .expect_err("expected to error while building the workload entity");

        assert_eq!(err.errors.len(), 2);
        assert!(
            matches!(
                err.errors[0],
                (ref tkn_name, BuildEntityError::MissingClaim(ref claim_name))
                    if tkn_name == "access_token" &&
                        claim_name == "client_id"
            ),
            "expected an error due to missing the `client_id` claim"
        );
        assert!(
            matches!(
                err.errors[1],
                (ref tkn_name, BuildEntityError::MissingClaim(ref claim_name))
                    if tkn_name == "id_token" &&
                        claim_name == "aud"
            ),
            "expected an error due to missing the `aud` claim"
        );
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
        let builder = EntityBuilder::new(schema, EntityNames::default(), true, false);
        let tokens = HashMap::new();
        let err = builder
            .build_workload_entity(&tokens, &BuiltEntities::default())
            .unwrap_err();

        assert_eq!(err.errors.len(), 0);
    }
}
