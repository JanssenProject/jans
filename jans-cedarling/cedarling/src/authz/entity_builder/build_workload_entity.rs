// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::*;
use cedar_policy::Entity;
use derive_more::derive::Deref;
use std::collections::HashSet;

const DEFAULT_WORKLOAD_ATTR_SRCS: [&str; 1] = ["access_token"];

impl EntityBuilder {
    pub fn build_workload_entity(
        &self,
        tokens: &HashMap<String, Token>,
        // tkn_principal_mappings: &HashMap<String, Vec<(String, RestrictedExpression)>>,
        tkn_principal_mappings: &TokenPrincipalMappings,
    ) -> Result<Entity, BuildEntityError> {
        let workload_type_name = self.entity_names.workload.as_ref();

        // Get Workload Entity ID
        let workload_id_srcs = WorkloadIdSrcs::resolve(tokens);
        let workload_id = get_first_valid_entity_id(&workload_id_srcs)
            .map_err(|e| e.while_building(workload_type_name))?;

        // Get Workload Entity attributes
        let workload_attr_srcs = DEFAULT_WORKLOAD_ATTR_SRCS
            .iter()
            .filter_map(|src| tokens.get(*src).map(|tkn| tkn.into()))
            .collect::<Vec<_>>();
        let workload_attrs = build_entity_attrs(workload_attr_srcs);

        // Insert token references in the entity attributes
        let workload_attrs =
            add_token_references(workload_type_name, workload_attrs, tkn_principal_mappings);

        let workload_entity = build_cedar_entity(
            workload_type_name,
            &workload_id,
            workload_attrs,
            HashSet::new(),
        )?;

        Ok(workload_entity)
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

#[derive(Deref)]
struct WorkloadIdSrcs<'a>(Vec<EntityIdSrc<'a>>);

#[derive(Clone, Copy)]
struct WorkloadIdSrc<'a> {
    token: &'a str,
    claim: &'a str,
}

impl<'a> WorkloadIdSrcs<'a> {
    fn resolve(tokens: &'a HashMap<String, Token>) -> Self {
        const DEFAULT_WORKLOAD_ID_SRCS: &[WorkloadIdSrc] = &[
            WorkloadIdSrc {
                token: "access_token",
                claim: "aud",
            },
            WorkloadIdSrc {
                token: "access_token",
                claim: "client_id",
            },
            WorkloadIdSrc {
                token: "id_token",
                claim: "aud",
            },
        ];

        Self(
            DEFAULT_WORKLOAD_ID_SRCS
                .iter()
                .fold(Vec::new(), |mut acc, &src| {
                    if let Some((token, claim)) = tokens.get(src.token).and_then(|tkn| {
                        tkn.get_metadata()
                            .and_then(|m| m.workload_id.as_ref().map(|claim| (tkn, claim)))
                    }) {
                        acc.push(EntityIdSrc { token, claim });
                        if claim != src.claim {
                            acc.push(EntityIdSrc {
                                token,
                                claim: src.claim,
                            });
                        }
                    }

                    acc
                }),
        )
    }
}

// TODO: move these tests to the top level
#[cfg(test)]
mod test_get_id {
    use super::*;
    use crate::authz::entity_builder::build_workload_entity::GetEntityIdError;
    use serde_json::json;
    use std::collections::HashMap;

    const TEST_ID_SRCS: &[WorkloadIdSrc] = &[
        WorkloadIdSrc {
            token: "access_token",
            claim: "aud",
        },
        WorkloadIdSrc {
            token: "access_token",
            claim: "client_id",
        },
        WorkloadIdSrc {
            token: "id_token",
            claim: "aud",
        },
    ];

    #[test]
    fn can_get_id() {
        let expected_id = "some_workload_id";

        // Using access token's aud
        let tokens = HashMap::<String, Token>::from([(
            "access_token".into(),
            Token::new(
                "access_token",
                HashMap::from([("aud".into(), json!(expected_id))]).into(),
                None,
            ),
        )]);
        let workload_id_srcs = TEST_ID_SRCS
            .iter()
            .filter_map(|src| {
                tokens.get(src.token).map(|token| {
                    let claim = token
                        .get_metadata()
                        .and_then(|m| m.user_id.as_deref())
                        .unwrap_or(src.claim);
                    EntityIdSrc { token, claim }
                })
            })
            .collect::<Vec<_>>();
        let id = get_first_valid_entity_id(&workload_id_srcs)
            .expect("should get workload id from access_token's aud");
        assert_eq!(id, expected_id);

        // Using access token's client_id
        let tokens = HashMap::<String, Token>::from([(
            "access_token".into(),
            Token::new(
                "access_token",
                HashMap::from([("client_id".into(), json!(expected_id))]).into(),
                None,
            ),
        )]);
        let workload_id_srcs = TEST_ID_SRCS
            .iter()
            .filter_map(|src| {
                tokens.get(src.token).map(|token| {
                    let claim = token
                        .get_metadata()
                        .and_then(|m| m.user_id.as_deref())
                        .unwrap_or(src.claim);
                    EntityIdSrc { token, claim }
                })
            })
            .collect::<Vec<_>>();
        let id = get_first_valid_entity_id(&workload_id_srcs)
            .expect("should get workload id from access_token's aud");
        assert_eq!(id, expected_id);

        // Using id token's aud
        let tokens = HashMap::<String, Token>::from([(
            "id_token".into(),
            Token::new(
                "id_token",
                HashMap::from([("aud".into(), json!(expected_id))]).into(),
                None,
            ),
        )]);
        let workload_id_srcs = TEST_ID_SRCS
            .iter()
            .filter_map(|src| {
                tokens.get(src.token).map(|token| {
                    let claim = token
                        .get_metadata()
                        .and_then(|m| m.user_id.as_deref())
                        .unwrap_or(src.claim);
                    EntityIdSrc { token, claim }
                })
            })
            .collect::<Vec<_>>();
        let id = get_first_valid_entity_id(&workload_id_srcs)
            .expect("should get workload id from access_token's aud");
        assert_eq!(id, expected_id);
    }

    #[test]
    fn errors_on_missing_claim() {
        let token_names = vec!["access_token", "id_token"];
        let tokens = HashMap::<String, Token>::from_iter(token_names.iter().map(|name| {
            (
                name.to_string(),
                Token::new(*name, HashMap::new().into(), None),
            )
        }));
        let workload_id_srcs = TEST_ID_SRCS
            .iter()
            .filter_map(|src| {
                tokens.get(src.token).map(|token| {
                    let claim = token
                        .get_metadata()
                        .and_then(|m| m.user_id.as_deref())
                        .unwrap_or(src.claim);
                    EntityIdSrc { token, claim }
                })
            })
            .collect::<Vec<_>>();
        let err = get_first_valid_entity_id(&workload_id_srcs)
            .expect_err("should error while getting workload id");
        if let BuildEntityErrorKind::MissingEntityId(errs) = err {
            assert_eq!(errs.len(), 3);
            for (token, claim) in TEST_ID_SRCS.iter().map(|src| (src.token, src.claim)) {
                let expected_err = &GetEntityIdError {
                    token: token.into(),
                    claim: claim.into(),
                    reason: GetEntityIdErrorReason::MissingClaim,
                };
                assert!(
                    errs.contains(expected_err),
                    "errors {:#?} do not contain {:#?}",
                    errs,
                    expected_err
                );
            }
        } else {
            panic!("error should be of kind MissingEntityId: {}", err);
        }
    }

    #[test]
    fn errors_on_empty_string() {
        let token_names = vec!["access_token", "id_token"];
        let tokens = HashMap::<String, Token>::from_iter(token_names.iter().map(|name| {
            (
                name.to_string(),
                Token::new(
                    *name,
                    HashMap::from([("aud".into(), json!("")), ("client_id".into(), json!(""))])
                        .into(),
                    None,
                ),
            )
        }));
        let workload_id_srcs = TEST_ID_SRCS
            .iter()
            .filter_map(|src| {
                tokens.get(src.token).map(|token| {
                    let claim = token
                        .get_metadata()
                        .and_then(|m| m.user_id.as_deref())
                        .unwrap_or(src.claim);
                    EntityIdSrc { token, claim }
                })
            })
            .collect::<Vec<_>>();
        let err = get_first_valid_entity_id(&workload_id_srcs)
            .expect_err("should error while getting workload id");
        if let BuildEntityErrorKind::MissingEntityId(errs) = err {
            for (token, claim) in TEST_ID_SRCS.iter().map(|src| (src.token, src.claim)) {
                assert_eq!(errs.len(), 3);
                let expected_err = &GetEntityIdError {
                    token: token.into(),
                    claim: claim.into(),
                    reason: GetEntityIdErrorReason::EmptyString,
                };
                assert!(
                    errs.contains(expected_err),
                    "errors {:#?} do not contain {:#?}",
                    errs,
                    expected_err
                );
            }
        } else {
            panic!("error should be of kind MissingEntityId: {}", err);
        }
    }
}

#[cfg(test)]
mod test {
    use super::super::*;
    use super::*;
    use crate::common::policy_store::{ClaimMappings, TokenEntityMetadata, TrustedIssuer};
    use cedar_policy::EvalResult;
    use serde_json::json;
    use std::collections::HashMap;

    #[test]
    fn can_build_using_access_tkn_aud() {
        let iss = TrustedIssuer::default();
        let builder = EntityBuilder::new(EntityNames::default(), true, false, &HashMap::new());
        let access_token = Token::new(
            "access_token",
            HashMap::from([
                ("jti".to_string(), json!("some_jti")),
                ("aud".to_string(), json!("some_aud")),
                ("name".to_string(), json!("some_name")),
            ])
            .into(),
            Some(&iss),
        );
        let tokens = HashMap::from([("access_token".to_string(), access_token)]);
        let entity = builder
            .build_workload_entity(&tokens, &TokenPrincipalMappings::default())
            .expect("should build workload entity")
            .to_json_value()
            .expect("should serialize entity to JSON");

        assert_eq!(
            entity,
            json!({
                "uid": {"type": "Jans::Workload", "id": "some_aud"},
                "attrs": {
                    "jti": "some_jti",
                    "aud": "some_aud",
                    "name": "some_name",
                },
                "parents": [],
            })
        );
    }

    #[test]
    fn can_build_using_access_tkn_client_id() {
        let iss = TrustedIssuer::default();
        let builder = EntityBuilder::new(EntityNames::default(), true, false, &HashMap::new());
        let access_token = Token::new(
            "access_token",
            HashMap::from([
                ("jti".to_string(), json!("some_jti")),
                ("client_id".to_string(), json!("some_client_id")),
                ("name".to_string(), json!("some_name")),
            ])
            .into(),
            Some(&iss),
        );
        let tokens = HashMap::from([("access_token".to_string(), access_token)]);
        let entity = builder
            .build_workload_entity(&tokens, &TokenPrincipalMappings::default())
            .expect("should build workload entity")
            .to_json_value()
            .expect("should serialize entity to JSON");

        assert_eq!(
            entity,
            json!({
                "uid": {"type": "Jans::Workload", "id": "some_client_id"},
                "attrs": {
                    "jti": "some_jti",
                    "client_id": "some_client_id",
                    "name": "some_name",
                },
                "parents": [],
            })
        );
    }

    #[test]
    fn can_build_expression_with_regex_mapping() {
        let token_entity_metadata_builder = TokenEntityMetadata::builder().claim_mapping(serde_json::from_value::<ClaimMappings>(json!({
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
                .unwrap());
        let iss = TrustedIssuer {
            tokens_metadata: HashMap::from([(
                "access_token".to_string(),
                token_entity_metadata_builder
                    .entity_type_name("Jans::Access_token".into())
                    .workload_id(Some("client_id".into()))
                    .build(),
            )]),
            ..Default::default()
        };
        let builder = EntityBuilder::new(EntityNames::default(), true, false, &HashMap::new());
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
            .build_workload_entity(&tokens, &TokenPrincipalMappings::default())
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
}
