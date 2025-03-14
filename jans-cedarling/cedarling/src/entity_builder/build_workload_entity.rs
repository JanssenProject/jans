// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::entity_id_getters::*;
use super::*;
use cedar_policy::Entity;
use derive_more::derive::Deref;
use std::collections::HashSet;

const WORKLOAD_ATTR_SRC_TKNS: &[&str] = &["access_token"];

impl EntityBuilder {
    // TODO: we need to set spiffe_id to be either `aud` or `client_id`
    pub fn build_workload_entity(
        &self,
        tokens: &HashMap<String, Token>,
        tkn_principal_mappings: &TokenPrincipalMappings,
        built_entities: &BuiltEntities,
    ) -> Result<Entity, BuildEntityError> {
        let workload_type_name = self.config.entity_names.workload.as_ref();

        let attrs_shape = self
            .schema
            .as_ref()
            .and_then(|s| s.get_entity_shape(workload_type_name));

        // Get Workload Entity ID
        let workload_id_srcs = WorkloadIdSrcs::resolve(tokens);
        let workload_id = get_first_valid_entity_id(&workload_id_srcs)
            .map_err(|e| e.while_building(workload_type_name))?;

        // Collect the tokens and mappings to be used for this entity
        let attrs_srcs: Vec<_> = WORKLOAD_ATTR_SRC_TKNS
            .iter()
            .filter_map(|name| {
                tokens
                    .get(*name)
                    .map(|tkn| (tkn.claims_value(), tkn.claim_mappings()))
            })
            .collect();
        if attrs_srcs.is_empty() {
            return Err(BuildEntityErrorKind::NoAvailableTokensToBuildEntity(
                WORKLOAD_ATTR_SRC_TKNS
                    .iter()
                    .map(|s| s.to_string())
                    .collect(),
            )
            .while_building(workload_type_name));
        }

        // Extract attributes from sources
        let (workload_attrs, errs): (Vec<_>, Vec<_>) = attrs_srcs
            .into_iter()
            .map(|(src, mappings)| build_entity_attrs(src, built_entities, attrs_shape, mappings))
            .partition(Result::is_ok);
        let mut workload_attrs: HashMap<String, RestrictedExpression> = if errs.is_empty() {
            // what should happen if claims have the same name but different values?
            workload_attrs
                .into_iter()
                .flatten()
                .fold(HashMap::new(), |mut acc, attrs| {
                    acc.extend(attrs);
                    acc
                })
        } else {
            let errs: Vec<_> = errs
                .into_iter()
                .flat_map(|e| e.unwrap_err().into_inner())
                .collect();
            return Err(BuildEntityErrorKind::from(errs).while_building(workload_type_name));
        };

        // Apply token mappings if the schema/shape is not present since that's the only
        // time it's really necessary
        if attrs_shape.is_none() {
            tkn_principal_mappings.apply(workload_type_name, &mut workload_attrs);
        }

        let parents = HashSet::default();
        let workload_entity =
            build_cedar_entity(workload_type_name, &workload_id, workload_attrs, parents)?;

        Ok(workload_entity)
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

        let mut eid_srcs = Vec::with_capacity(4);

        for src in DEFAULT_WORKLOAD_ID_SRCS.iter() {
            if let Some(token) = tokens.get(src.token) {
                // if a `workload_id` is availble in the token's entity metadata
                let claim = if let Some(claim) =
                    token.get_metadata().and_then(|m| m.workload_id.as_ref())
                {
                    eid_srcs.push(EntityIdSrc { token, claim });
                    Some(claim)
                } else {
                    None
                };

                // then we add the fallbacks in-case the token does not have the claims.
                if claim.map(|claim| claim == src.claim).unwrap_or(false) {
                    continue;
                }
                eid_srcs.push(EntityIdSrc {
                    token,
                    claim: src.claim,
                });
            }
        }

        Self(eid_srcs)
    }
}

#[cfg(test)]
mod test {
    use super::super::test::*;
    use super::super::*;
    use super::*;
    use crate::common::policy_store::TrustedIssuer;
    use cedar_policy::Schema;
    use serde_json::json;
    use std::collections::HashMap;

    #[track_caller]
    fn test_build_workload(
        token: Token,
        builder: &EntityBuilder,
        tkn_principal_mappings: &TokenPrincipalMappings,
        expected: Value,
        schema: Option<&Schema>,
    ) {
        let tokens = HashMap::from([(token.name.clone(), token)]);
        let mut built_entities = BuiltEntities::from(&builder.iss_entities);
        built_entities.insert(
            &EntityUid::from_str("Jans::Access_token::\"some_jti\"").expect("a valid EntityUid"),
        );
        let entity = builder
            .build_workload_entity(&tokens, &tkn_principal_mappings, &built_entities)
            .expect("should build workload entity");

        assert_entity_eq(&entity, expected, schema);
    }

    #[test]
    fn can_build_workload_with_aud_and_schema() {
        let schema_src = r#"
            namespace Jans {
                entity TrustedIssuer;
                entity Access_token;
                entity Workload = {
                    iss: TrustedIssuer,
                    aud?: String,
                    client_id?: String,
                    access_token?: Access_token,
                };
            }
        "#;
        let schema = Schema::from_str(schema_src).expect("build cedar Schema");
        let validator_schema =
            ValidatorSchema::from_str(schema_src).expect("build cedar ValidatorSchema");
        let iss = TrustedIssuer::default();
        let issuers = HashMap::from([("some_iss".into(), iss.clone())]);
        let builder = EntityBuilder::new(
            EntityBuilderConfig::default().with_workload(),
            &issuers,
            Some(&validator_schema),
        )
        .expect("should init entity builder");
        let tkn_principal_mappings = TokenPrincipalMappings::from(
            [TokenPrincipalMapping {
                principal: "Jans::Workload".into(),
                attr_name: "access_token".into(),
                expr: RestrictedExpression::new_entity_uid(
                    EntityUid::from_str("Jans::Access_token::\"some_jti\"".into())
                        .expect("should parse EntityUid"),
                ),
            }]
            .to_vec(),
        );

        let access_token = Token::new(
            "access_token",
            HashMap::from([
                ("iss".to_string(), json!("https://test.jans.org/")),
                ("aud".to_string(), json!("some_aud")),
                ("jti".to_string(), json!("some_jti")),
            ])
            .into(),
            Some(&iss),
        );

        test_build_workload(
            access_token,
            &builder,
            &tkn_principal_mappings,
            json!({
                "uid": {"type": "Jans::Workload", "id": "some_aud"},
                "attrs": {
                    "iss": {"__entity": {
                        "type": "Jans::TrustedIssuer",
                        "id": "some_iss",
                    }},
                    "aud": "some_aud",
                    "access_token": {"__entity": {
                        "type": "Jans::Access_token",
                        "id": "some_jti",
                    }},
                },
                "parents": [],
            }),
            Some(&schema),
        );
    }

    #[test]
    fn can_build_workload_with_client_id_and_schema() {
        let schema_src = r#"
            namespace Jans {
                entity TrustedIssuer;
                entity Access_token;
                entity Workload = {
                    iss: TrustedIssuer,
                    aud?: String,
                    client_id?: String,
                    access_token?: Access_token,
                };
            }
        "#;
        let schema = Schema::from_str(schema_src).expect("build cedar Schema");
        let validator_schema =
            ValidatorSchema::from_str(schema_src).expect("build cedar ValidatorSchema");
        let iss = TrustedIssuer::default();
        let issuers = HashMap::from([("some_iss".into(), iss.clone())]);
        let builder = EntityBuilder::new(
            EntityBuilderConfig::default().with_workload(),
            &issuers,
            Some(&validator_schema),
        )
        .expect("should init entity builder");
        let tkn_principal_mappings = TokenPrincipalMappings::from(
            [TokenPrincipalMapping {
                principal: "Jans::Workload".into(),
                attr_name: "access_token".into(),
                expr: RestrictedExpression::new_entity_uid(
                    EntityUid::from_str("Jans::Access_token::\"some_jti\"".into())
                        .expect("should parse EntityUid"),
                ),
            }]
            .to_vec(),
        );

        let access_token = Token::new(
            "access_token",
            HashMap::from([
                ("iss".to_string(), json!("https://test.jans.org/")),
                ("client_id".to_string(), json!("some_client_id")),
                ("jti".to_string(), json!("some_jti")),
            ])
            .into(),
            Some(&iss),
        );

        test_build_workload(
            access_token,
            &builder,
            &tkn_principal_mappings,
            json!({
                "uid": {"type": "Jans::Workload", "id": "some_client_id"},
                "attrs": {
                    "iss": {"__entity": {
                        "type": "Jans::TrustedIssuer",
                        "id": "some_iss",
                    }},
                    "client_id": "some_client_id",
                    "access_token": {"__entity": {
                        "type": "Jans::Access_token",
                        "id": "some_jti",
                    }},
                },
                "parents": [],
            }),
            Some(&schema),
        );
    }

    #[test]
    fn can_build_workload_without_schema() {
        let iss = TrustedIssuer::default();
        let issuers = HashMap::from([("some_iss".into(), iss.clone())]);
        let builder = EntityBuilder::new(
            EntityBuilderConfig::default().with_workload(),
            &issuers,
            None,
        )
        .expect("should init entity builder");
        let tkn_principal_mappings = TokenPrincipalMappings::from(
            [TokenPrincipalMapping {
                principal: "Jans::Workload".into(),
                attr_name: "access_token".into(),
                expr: RestrictedExpression::new_entity_uid(
                    EntityUid::from_str("Jans::Access_token::\"some_jti\"".into())
                        .expect("should parse EntityUid"),
                ),
            }]
            .to_vec(),
        );

        let access_token = Token::new(
            "access_token",
            HashMap::from([
                ("iss".to_string(), json!("https://test.jans.org/")),
                ("aud".to_string(), json!("some_aud")),
                ("jti".to_string(), json!("some_jti")),
            ])
            .into(),
            Some(&iss),
        );
        test_build_workload(
            access_token,
            &builder,
            &tkn_principal_mappings,
            json!({
                "uid": {"type": "Jans::Workload", "id": "some_aud"},
                "attrs": {
                    "iss": "https://test.jans.org/",
                    "aud": "some_aud",
                    "access_token": {"__entity": {
                        "type": "Jans::Access_token",
                        "id": "some_jti",
                    }},
                },
                "parents": [],
            }),
            None,
        );
    }
}
