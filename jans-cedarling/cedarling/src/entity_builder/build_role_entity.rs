// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::entity_id_getters::*;
use super::*;
use cedar_policy::Entity;
use derive_more::derive::Deref;
use std::collections::HashSet;

impl EntityBuilder {
    pub fn build_role_entities(
        &self,
        tokens: &HashMap<String, Token>,
    ) -> Result<Vec<Entity>, BuildEntityError> {
        // Build Role entities
        let role_id_srcs = RoleIdSrcs::resolve(tokens);
        let mut role_entities = Vec::with_capacity(2);
        let role_ids = collect_all_valid_entity_ids(&role_id_srcs);
        for id in role_ids {
            let role_entity = build_cedar_entity(
                &self.config.entity_names.role,
                &id,
                HashMap::new(),
                HashSet::new(),
            )?;
            role_entities.push(role_entity);
        }

        Ok(role_entities)
    }
}

#[derive(Deref)]
struct RoleIdSrcs<'a>(Vec<EntityIdSrc<'a>>);

#[derive(Clone, Copy)]
struct RoleIdSrc<'a> {
    token: &'a str,
    claim: &'a str,
}

impl<'a> RoleIdSrcs<'a> {
    fn resolve(tokens: &'a HashMap<String, Token>) -> Self {
        const DEFAULT_ROLE_ID_SRCS: &[RoleIdSrc] = &[
            RoleIdSrc {
                token: "userinfo_token",
                claim: "role",
            },
            RoleIdSrc {
                token: "id_token",
                claim: "role",
            },
        ];

        Self(
            DEFAULT_ROLE_ID_SRCS
                .iter()
                .filter_map(|src| {
                    tokens.get(src.token).map(|token| {
                        let claim = token
                            .get_metadata()
                            .and_then(|m| m.role_mapping.as_deref())
                            .unwrap_or(src.claim);
                        EntityIdSrc { token, claim }
                    })
                })
                .collect::<Vec<_>>(),
        )
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

    #[test]
    fn can_build_role_entity_from_str() {
        let schema_src = r#"
            namespace Jans {
                entity Role;
            }
        "#;
        let schema = Schema::from_str(schema_src).expect("build cedar Schema");
        let validator_schema =
            ValidatorSchema::from_str(schema_src).expect("build cedar ValidatorSchema");
        let iss = TrustedIssuer::default();
        let builder = EntityBuilder::new(
            EntityBuilderConfig::default().build_workload(),
            &HashMap::new(),
            Some(&validator_schema),
        )
        .expect("should init entity builder");

        let id_token = Token::new(
            "id_token",
            HashMap::from([("role".to_string(), json!("some_role"))]).into(),
            Some(&iss),
        );
        let tokens = HashMap::from([("id_token".into(), id_token)]);

        let token_entities = builder
            .build_role_entities(&tokens)
            .expect("successfully build role entity");

        assert_eq!(token_entities.len(), 1, "one token entity");

        assert_entity_eq(
            &token_entities[0],
            json!({
                "uid": {"type": "Jans::Role", "id": "some_role"},
                "attrs": {},
                "parents": [],
            }),
            Some(&schema),
        );
    }

    #[test]
    fn can_build_role_entities_from_vec() {
        let schema_src = r#"
            namespace Jans {
                entity Role;
            }
        "#;
        let schema = Schema::from_str(schema_src).expect("build cedar Schema");
        let validator_schema =
            ValidatorSchema::from_str(schema_src).expect("build cedar ValidatorSchema");
        let iss = TrustedIssuer::default();
        let builder = EntityBuilder::new(
            EntityBuilderConfig::default().build_workload(),
            &HashMap::new(),
            Some(&validator_schema),
        )
        .expect("should init entity builder");

        let id_token = Token::new(
            "id_token",
            HashMap::from([("role".to_string(), json!(["some_role", "another_role"]))]).into(),
            Some(&iss),
        );
        let tokens = HashMap::from([("id_token".into(), id_token)]);

        let token_entities = builder
            .build_role_entities(&tokens)
            .expect("successfully build role entity");

        assert_eq!(token_entities.len(), 2, "two token entities");

        assert_entity_eq(
            &token_entities[0],
            json!({
                "uid": {"type": "Jans::Role", "id": "some_role"},
                "attrs": {},
                "parents": [],
            }),
            Some(&schema),
        );

        assert_entity_eq(
            &token_entities[1],
            json!({
                "uid": {"type": "Jans::Role", "id": "another_role"},
                "attrs": {},
                "parents": [],
            }),
            Some(&schema),
        );
    }

    #[test]
    fn builds_role_entities_from_multiple_tokens() {
        let schema_src = r#"
            namespace Jans {
                entity Role;
            }
        "#;
        let schema = Schema::from_str(schema_src).expect("build cedar Schema");
        let validator_schema =
            ValidatorSchema::from_str(schema_src).expect("build cedar ValidatorSchema");
        let iss = TrustedIssuer::default();
        let builder = EntityBuilder::new(
            EntityBuilderConfig::default().build_workload(),
            &HashMap::new(),
            Some(&validator_schema),
        )
        .expect("should init entity builder");

        let id_token = Token::new(
            "id_token",
            HashMap::from([("role".to_string(), json!("some_role"))]).into(),
            Some(&iss),
        );
        let userinfo_token = Token::new(
            "userinfo_token",
            HashMap::from([("role".to_string(), json!("another_role"))]).into(),
            Some(&iss),
        );
        let tokens = HashMap::from([
            ("id_token".into(), id_token),
            ("userinfo_token".into(), userinfo_token),
        ]);

        let token_entities = builder
            .build_role_entities(&tokens)
            .expect("successfully build role entity");

        assert_eq!(token_entities.len(), 2, "two token entities");

        assert_entity_eq(
            &token_entities[0],
            json!({
                "uid": {"type": "Jans::Role", "id": "another_role"},
                "attrs": {},
                "parents": [],
            }),
            Some(&schema),
        );

        assert_entity_eq(
            &token_entities[1],
            json!({
                "uid": {"type": "Jans::Role", "id": "some_role"},
                "attrs": {},
                "parents": [],
            }),
            Some(&schema),
        );
    }
}
