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
const WORKLOAD_ATTR_SRC_CLAIMS: &[&str] =
    &["iss", "aud", "client_id", "name", "rp_id", "spiffe_id"];

impl EntityBuilder {
    pub fn build_workload_entity(
        &self,
        tokens: &HashMap<String, Token>,
        tkn_principal_mappings: &TokenPrincipalMappings,
    ) -> Result<(Entity, Option<Entity>), BuildEntityError> {
        let workload_type_name = self.entity_names.workload.as_ref();

        // Get Workload Entity ID
        let workload_id_srcs = WorkloadIdSrcs::resolve(tokens);
        let workload_id = get_first_valid_entity_id(&workload_id_srcs)
            .map_err(|e| e.while_building(workload_type_name))?;

        // Get Workload Entity attributes
        let workload_attrs = build_entity_attrs(EntityAttrsSrc::new(
            tokens,
            WORKLOAD_ATTR_SRC_TKNS,
            WORKLOAD_ATTR_SRC_CLAIMS,
        ))
        .map_err(|e| e.while_building(workload_type_name))?;

        // Insert token references in the entity attributes
        let mut workload_attrs =
            add_token_references(workload_type_name, workload_attrs, tkn_principal_mappings);

        let iss_entity = workload_id_srcs
            .iter()
            .find_map(|src| {
                self.replace_iss_with_entity(src.token, &mut workload_attrs)
                    .transpose()
            })
            .transpose()
            .map_err(|e| e.while_building(workload_type_name))?;

        let workload_entity = build_cedar_entity(
            workload_type_name,
            &workload_id,
            workload_attrs,
            HashSet::new(),
        )?;

        Ok((workload_entity, iss_entity))
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
                    if let Some(token) = tokens.get(src.token) {
                        let claim = token
                            .get_metadata()
                            .and_then(|m| m.workload_id.as_ref())
                            .map(|claim| {
                                acc.push(EntityIdSrc { token, claim });
                                claim
                            });

                        if matches!(claim, Some(ref claim) if *claim != src.claim) {
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

#[cfg(test)]
mod test {
    use super::super::*;
    use super::*;
    use crate::authz::entity_builder::test::cedarling_schema;
    use crate::common::policy_store::TrustedIssuer;
    use cedar_policy::Entities;
    use serde_json::json;
    use std::collections::HashMap;

    #[test]
    fn can_build_using_access_tkn_aud() {
        let iss = TrustedIssuer::default();
        let issuers = HashMap::from([("some_iss".into(), iss.clone())]);
        let builder = EntityBuilder::new(EntityNames::default(), true, false, &issuers)
            .expect("should init entity builder");
        let access_token = Token::new(
            "access_token",
            HashMap::from([
                ("iss".to_string(), json!("https://test.jans.org/")),
                ("aud".to_string(), json!("some_aud")),
                ("jti".to_string(), json!("some_jti")),
                ("name".to_string(), json!("some_name")),
                ("rp_id".to_string(), json!("some_rp_id")),
                ("spiffe_id".to_string(), json!("some_spiffe_id")),
                ("exp".to_string(), json!(123)),
            ])
            .into(),
            Some(&iss),
        );
        let tokens = HashMap::from([("access_token".to_string(), access_token)]);
        let token_principal_mappings = TokenPrincipalMappings::from(
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
        let (entity, _) = builder
            .build_workload_entity(&tokens, &token_principal_mappings)
            .expect("should build workload entity");

        // Check if the entity has the correct attributes
        assert_eq!(
            entity
                .clone()
                .to_json_value()
                .expect("should serialize entity to JSON"),
            json!({
                "uid": {"type": "Jans::Workload", "id": "some_aud"},
                "attrs": {
                    "iss": {"__entity": {
                        "type": "Jans::TrustedIssuer",
                        "id": "some_iss",
                    }},
                    "aud": "some_aud",
                    "name": "some_name",
                    "rp_id": "some_rp_id",
                    "spiffe_id": "some_spiffe_id",
                    "access_token": {"__entity": {
                        "type": "Jans::Access_token",
                        "id": "some_jti",
                    }},
                },
                "parents": [],
            }),
            "workload entity should have the correct attrs"
        );

        Entities::from_entities([entity], Some(cedarling_schema()))
            .expect("workload entity should conform to the schema");
    }

    #[test]
    fn can_build_using_access_tkn_client_id() {
        let iss = TrustedIssuer::default();
        let issuers = HashMap::from([("some_iss".into(), iss.clone())]);
        let builder = EntityBuilder::new(EntityNames::default(), true, false, &issuers)
            .expect("should init entity builder");
        let access_token = Token::new(
            "access_token",
            HashMap::from([
                ("iss".to_string(), json!("https://test.jans.org/")),
                ("jti".to_string(), json!("some_jti")),
                ("client_id".to_string(), json!("some_client_id")),
                ("name".to_string(), json!("some_name")),
                ("rp_id".to_string(), json!("some_rp_id")),
                ("spiffe_id".to_string(), json!("some_spiffe_id")),
                ("exp".to_string(), json!(123)),
            ])
            .into(),
            Some(&iss),
        );
        let tokens = HashMap::from([("access_token".to_string(), access_token)]);
        let token_principal_mappings = TokenPrincipalMappings::from(
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
        let (entity, _) = builder
            .build_workload_entity(&tokens, &token_principal_mappings)
            .expect("should build workload entity");

        assert_eq!(
            entity
                .to_json_value()
                .expect("should serialize entity to JSON"),
            json!({
                "uid": {"type": "Jans::Workload", "id": "some_client_id"},
                "attrs": {
                    "iss": {"__entity": {
                        "type": "Jans::TrustedIssuer",
                        "id": "some_iss",
                    }},
                    "client_id": "some_client_id",
                    "name": "some_name",
                    "rp_id": "some_rp_id",
                    "spiffe_id": "some_spiffe_id",
                    "access_token": {"__entity": {
                        "type": "Jans::Access_token",
                        "id": "some_jti",
                    }},
                },
                "parents": [],
            }),
            "workload entity should have the correct attrs"
        );

        Entities::from_entities([entity], Some(cedarling_schema()))
            .expect("workload entity should conform to the schema");
    }
}
