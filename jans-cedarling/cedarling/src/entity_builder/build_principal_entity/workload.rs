// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::*;
use cedar_policy::Entity;
use std::collections::HashSet;

const WORKLOAD_ATTR_SRC_TKNS: &[&str] = &["access_token"];

impl EntityBuilder {
    // TODO: we might need to add support for setting the 'spiffe_id' attribute to be
    // either the 'aud' or 'client_id' claim eventually
    pub fn build_workload_entity(
        &self,
        tokens: &HashMap<String, Token>,
        tkn_principal_mappings: &TokenPrincipalMappings,
        built_entities: &BuiltEntities,
    ) -> Result<Entity, BuildEntityError> {
        let type_name: &str = self.config.entity_names.workload.as_ref();

        let id_srcs = WorkloadIdSrcResolver::resolve(tokens);

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
            .while_building(type_name));
        }

        self.build_principal_entity(
            type_name,
            id_srcs,
            attrs_srcs,
            tkn_principal_mappings,
            built_entities,
            HashSet::default(),
        )
    }
}

/// Resolves workload entity IDs from authentication tokens.
///
/// This struct provides a method to extract workload-related entity IDs from a set of
/// tokens. It looks for predefined claims within specific tokens to identify workloads.
pub struct WorkloadIdSrcResolver;

impl WorkloadIdSrcResolver {
    /// Resolves workload entity IDs from the provided authentication tokens.
    ///
    /// This method scans the given `tokens` map and extracts entity IDs based on
    /// predefined rules. It prioritizes extracting the `workload_id` from a token's
    /// metadata when available and falls back to predefined claims if necessary.
    ///
    /// ## Token Sources:
    /// The method checks the following tokens and claims in order:
    /// - `access_token.aud`
    /// - `access_token.client_id`
    /// - `id_token.aud`
    pub fn resolve<'a>(tokens: &'a HashMap<String, Token>) -> Vec<EntityIdSrc<'a>> {
        const DEFAULT_WORKLOAD_ID_SRCS: &[PrincipalIdSrc] = &[
            PrincipalIdSrc {
                token: "access_token",
                claim: "aud",
            },
            PrincipalIdSrc {
                token: "access_token",
                claim: "client_id",
            },
            PrincipalIdSrc {
                token: "id_token",
                claim: "aud",
            },
        ];

        let mut eid_srcs = Vec::with_capacity(DEFAULT_WORKLOAD_ID_SRCS.len());

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

        eid_srcs
    }
}

#[cfg(test)]
mod test {
    use super::*;
    use crate::common::policy_store::TrustedIssuer;
    use crate::entity_builder::test::*;
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
