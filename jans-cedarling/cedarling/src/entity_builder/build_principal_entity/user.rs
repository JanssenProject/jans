// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::*;
use cedar_policy::Entity;
use std::collections::HashSet;

const USER_ATTR_SRC_TKNS: &[&str] = &["userinfo_token", "id_token"];

impl EntityBuilder {
    pub fn build_user_entity(
        &self,
        tokens: &HashMap<String, Token>,
        tkn_principal_mappings: &TokenPrincipalMappings,
        built_entities: &BuiltEntities,
        roles: HashSet<EntityUid>,
    ) -> Result<Entity, BuildEntityError> {
        let type_name: &str = self.config.entity_names.user.as_ref();

        let id_srcs = UserIdSrcResolver::resolve(tokens);

        let attrs_srcs: Vec<_> = USER_ATTR_SRC_TKNS
            .iter()
            .filter_map(|name| {
                tokens
                    .get(*name)
                    .map(|tkn| (tkn.claims_value(), tkn.claim_mappings()))
            })
            .collect();
        if attrs_srcs.is_empty() {
            return Err(BuildEntityErrorKind::NoAvailableTokensToBuildEntity(
                USER_ATTR_SRC_TKNS.iter().map(|s| s.to_string()).collect(),
            )
            .while_building(type_name));
        }

        self.build_principal_entity(
            type_name,
            id_srcs,
            attrs_srcs,
            tkn_principal_mappings,
            built_entities,
            roles,
        )
    }
}

/// Resolves workload entity IDs from authentication tokens.
///
/// This struct provides a method to extract workload-related entity IDs from a set of
/// tokens. It looks for predefined claims within specific tokens to identify workloads.
pub struct UserIdSrcResolver;

impl UserIdSrcResolver {
    /// Resolves user entity IDs from the provided authentication tokens.
    ///
    /// This method scans the given `tokens` map and extracts entity IDs based on
    /// predefined rules. It prioritizes extracting the `user_id` from a token's
    /// metadata when available and falls back to predefined claims if necessary.
    ///
    /// ## Token Sources:
    /// The method checks the following tokens and claims in order:
    /// - `userinfo_token.sub`
    /// - `id_token.sub`
    pub fn resolve<'a>(tokens: &'a HashMap<String, Token>) -> Vec<EntityIdSrc<'a>> {
        const DEFAULT_USER_ID_SRCS: &[PrincipalIdSrc] = &[
            PrincipalIdSrc {
                token: "userinfo_token",
                claim: "sub",
            },
            PrincipalIdSrc {
                token: "id_token",
                claim: "sub",
            },
        ];

        let mut eid_srcs = Vec::with_capacity(DEFAULT_USER_ID_SRCS.len());

        for src in DEFAULT_USER_ID_SRCS.iter() {
            if let Some(token) = tokens.get(src.token) {
                // if a `user_id` is availble in the token's entity metadata
                let claim =
                    if let Some(claim) = token.get_metadata().and_then(|m| m.user_id.as_ref()) {
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
    fn test_build_user(
        tokens: &HashMap<String, Token>,
        builder: &EntityBuilder,
        tkn_principal_mappings: &TokenPrincipalMappings,
        expected: Value,
        roles: HashSet<EntityUid>,
        schema: Option<&Schema>,
    ) {
        let mut built_entities = BuiltEntities::from(&builder.iss_entities);
        built_entities.insert(
            &EntityUid::from_str("Jans::Userinfo_token::\"some_jti\"").expect("a valid EntityUid"),
        );
        built_entities.insert(
            &EntityUid::from_str("Jans::Id_token::\"some_jti\"").expect("a valid EntityUid"),
        );
        let entity = builder
            .build_user_entity(tokens, tkn_principal_mappings, &built_entities, roles)
            .expect("should build workload entity");

        assert_entity_eq(&entity, expected, schema);
    }

    #[test]
    fn can_build_user_with_id_tkn_and_schema() {
        let schema_src = r#"
            namespace Jans {
                entity TrustedIssuer;
                entity Role;
                entity Id_token;
                entity User in [Role] = {
                    iss: TrustedIssuer,
                    sub: String,
                    id_token: Id_token,
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
                principal: "Jans::User".into(),
                attr_name: "id_token".into(),
                expr: RestrictedExpression::new_entity_uid(
                    EntityUid::from_str("Jans::Id_token::\"some_jti\"")
                        .expect("should parse EntityUid"),
                ),
            }]
            .to_vec(),
        );
        let roles = HashSet::from(["Jans::Role::\"some_role\"".parse().expect("a valid uid")]);

        let id_token = Token::new(
            "id_token",
            HashMap::from([
                ("iss".to_string(), json!("https://test.jans.org/")),
                ("sub".to_string(), json!("some_sub")),
            ])
            .into(),
            Some(&iss),
        );

        let tokens = HashMap::from([("id_token".into(), id_token)]);

        test_build_user(
            &tokens,
            &builder,
            &tkn_principal_mappings,
            json!({
                "uid": {"type": "Jans::User", "id": "some_sub"},
                "attrs": {
                    "iss": {"__entity": {
                        "type": "Jans::TrustedIssuer",
                        "id": "some_iss",
                    }},
                    "sub": "some_sub",
                    "id_token": {"__entity": {
                        "type": "Jans::Id_token",
                        "id": "some_jti",
                    }},
                },
                "parents": [{"type": "Jans::Role", "id": "some_role"}],
            }),
            roles,
            Some(&schema),
        );
    }

    #[test]
    fn can_build_user_with_userinfo_tkn_and_schema() {
        let schema_src = r#"
            namespace Jans {
                entity TrustedIssuer;
                entity Role;
                entity Userinfo_token;
                entity User in [Role] = {
                    iss: TrustedIssuer,
                    sub: String,
                    userinfo_token: Userinfo_token,
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
                principal: "Jans::User".into(),
                attr_name: "userinfo_token".into(),
                expr: RestrictedExpression::new_entity_uid(
                    EntityUid::from_str("Jans::Userinfo_token::\"some_jti\"")
                        .expect("should parse EntityUid"),
                ),
            }]
            .to_vec(),
        );
        let roles = HashSet::from(["Jans::Role::\"some_role\"".parse().expect("a valid uid")]);

        let userinfo_token = Token::new(
            "userinfo_token",
            HashMap::from([
                ("iss".to_string(), json!("https://test.jans.org/")),
                ("sub".to_string(), json!("some_sub")),
            ])
            .into(),
            Some(&iss),
        );

        let tokens = HashMap::from([("userinfo_token".into(), userinfo_token)]);

        test_build_user(
            &tokens,
            &builder,
            &tkn_principal_mappings,
            json!({
                "uid": {"type": "Jans::User", "id": "some_sub"},
                "attrs": {
                    "iss": {"__entity": {
                        "type": "Jans::TrustedIssuer",
                        "id": "some_iss",
                    }},
                    "sub": "some_sub",
                    "userinfo_token": {"__entity": {
                        "type": "Jans::Userinfo_token",
                        "id": "some_jti",
                    }},
                },
                "parents": [{"type": "Jans::Role", "id": "some_role"}],
            }),
            roles,
            Some(&schema),
        );
    }

    #[test]
    fn can_build_user_from_joined_tkns_and_schema() {
        let schema_src = r#"
            namespace Jans {
                entity TrustedIssuer;
                entity Role;
                entity Id_token;
                entity Userinfo_token;
                entity User in [Role] = {
                    iss: TrustedIssuer,
                    sub: String,
                    from_id_tkn: String,
                    from_userinfo_tkn: String,
                    id_token: Id_token,
                    userinfo_token: Userinfo_token,
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
                principal: "Jans::User".into(),
                attr_name: "userinfo_token".into(),
                expr: RestrictedExpression::new_entity_uid(
                    EntityUid::from_str("Jans::Userinfo_token::\"some_jti\"")
                        .expect("should parse EntityUid"),
                ),
            }]
            .to_vec(),
        );
        let roles = HashSet::from(["Jans::Role::\"some_role\"".parse().expect("a valid uid")]);

        let id_token = Token::new(
            "id_token",
            HashMap::from([
                ("iss".to_string(), json!("https://test.jans.org/")),
                ("sub".to_string(), json!("id_tkn_sub")),
                ("from_id_tkn".to_string(), json!("from_id_tkn")),
            ])
            .into(),
            Some(&iss),
        );
        let userinfo_token = Token::new(
            "userinfo_token",
            HashMap::from([
                ("iss".to_string(), json!("https://test.jans.org/")),
                ("sub".to_string(), json!("userinfo_tkn_sub")),
                ("from_userinfo_tkn".to_string(), json!("from_userinfo_tkn")),
            ])
            .into(),
            Some(&iss),
        );

        let tokens = HashMap::from([
            ("id_token".into(), id_token),
            ("userinfo_token".into(), userinfo_token),
        ]);

        test_build_user(
            &tokens,
            &builder,
            &tkn_principal_mappings,
            json!({
                "uid": {"type": "Jans::User", "id": "userinfo_tkn_sub"},
                "attrs": {
                    "iss": {"__entity": {
                        "type": "Jans::TrustedIssuer",
                        "id": "some_iss",
                    }},
                    "sub": "id_tkn_sub",
                    "from_id_tkn": "from_id_tkn",
                    "from_userinfo_tkn": "from_userinfo_tkn",
                    "userinfo_token": {"__entity": {
                        "type": "Jans::Userinfo_token",
                        "id": "some_jti",
                    }},
                    "userinfo_token": {"__entity": {
                        "type": "Jans::Userinfo_token",
                        "id": "some_jti",
                    }},
                },
                "parents": [{"type": "Jans::Role", "id": "some_role"}],
            }),
            roles,
            Some(&schema),
        );
    }

    #[test]
    fn can_build_user_without_schema() {
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
                principal: "Jans::User".into(),
                attr_name: "id_token".into(),
                expr: RestrictedExpression::new_entity_uid(
                    EntityUid::from_str("Jans::Id_token::\"some_jti\"")
                        .expect("should parse EntityUid"),
                ),
            }]
            .to_vec(),
        );
        let roles = HashSet::from(["Jans::Role::\"some_role\"".parse().expect("a valid uid")]);

        let id_token = Token::new(
            "id_token",
            HashMap::from([
                ("iss".to_string(), json!("https://test.jans.org/")),
                ("sub".to_string(), json!("some_sub")),
            ])
            .into(),
            Some(&iss),
        );

        let tokens = HashMap::from([("id_token".into(), id_token)]);

        test_build_user(
            &tokens,
            &builder,
            &tkn_principal_mappings,
            json!({
                "uid": {"type": "Jans::User", "id": "some_sub"},
                "attrs": {
                    "iss": "https://test.jans.org/",
                    "sub": "some_sub",
                    "id_token": {"__entity": {
                        "type": "Jans::Id_token",
                        "id": "some_jti",
                    }},
                },
                "parents": [{"type": "Jans::Role", "id": "some_role"}],
            }),
            roles,
            None,
        );
    }
}
