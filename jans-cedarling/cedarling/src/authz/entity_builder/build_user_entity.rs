// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::entity_id_getters::*;
use super::*;
use cedar_policy::Entity;
use derive_more::derive::Deref;
use std::collections::HashSet;

const USER_ATTR_SRC_TKNS: &[&str] = &["userinfo_token", "id_token"];
const USER_ATTR_SRC_CLAIMS: &[&str] = &[
    "sub",
    "role",
    "email",
    "phone_number",
    "username",
    "birthdate",
    "country",
];

impl EntityBuilder {
    pub fn build_user_entity(
        &self,
        tokens: &HashMap<String, Token>,
        tkn_principal_mappings: &TokenPrincipalMappings,
    ) -> Result<(Entity, Vec<Entity>), BuildEntityError> {
        let user_type_name: &str = self.entity_names.user.as_ref();

        // Get User Entity ID
        let user_id_srcs = UserIdSrcs::resolve(tokens);
        let user_id = get_first_valid_entity_id(&user_id_srcs)
            .map_err(|e| e.while_building(user_type_name))?;

        // Build Role entities
        let role_id_srcs = RoleIdSrcs::resolve(tokens);
        let mut user_parents = HashSet::new();
        let mut role_entities = Vec::with_capacity(2);
        let role_ids = collect_all_valid_entity_ids(&role_id_srcs);
        for id in role_ids {
            let role_entity =
                build_cedar_entity(&self.entity_names.role, &id, HashMap::new(), HashSet::new())?;
            user_parents.insert(role_entity.uid());
            role_entities.push(role_entity);
        }

        // Get User Entity attributes
        let user_attrs = build_entity_attrs(EntityAttrsSrc::new(
            tokens,
            USER_ATTR_SRC_TKNS,
            USER_ATTR_SRC_CLAIMS,
        ))
        .map_err(|e| e.while_building(user_type_name))?;

        // Insert token references in the entity attributes
        let user_attrs = add_token_references(user_type_name, user_attrs, tkn_principal_mappings);

        let user_entity = build_cedar_entity(user_type_name, &user_id, user_attrs, user_parents)?;

        Ok((user_entity, role_entities))
    }
}

#[derive(Deref)]
struct UserIdSrcs<'a>(Vec<EntityIdSrc<'a>>);

#[derive(Clone, Copy)]
struct UserIdSrc<'a> {
    token: &'a str,
    claim: &'a str,
}

impl<'a> UserIdSrcs<'a> {
    fn resolve(tokens: &'a HashMap<String, Token>) -> Self {
        const DEFAULT_USER_ID_SRCS: &[UserIdSrc] = &[
            UserIdSrc {
                token: "userinfo_token",
                claim: "sub",
            },
            UserIdSrc {
                token: "id_token",
                claim: "sub",
            },
        ];

        Self(
            DEFAULT_USER_ID_SRCS
                .iter()
                .fold(Vec::new(), |mut acc, &src| {
                    if let Some((token, claim)) = tokens.get(src.token).and_then(|tkn| {
                        tkn.get_metadata()
                            .and_then(|m| m.user_id.as_ref().map(|claim| (tkn, claim)))
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
    use super::super::*;
    use super::*;
    use crate::authz::entity_builder::test::{assert_entity_eq, cedarling_schema};
    use crate::common::policy_store::{ClaimMappings, TrustedIssuer};
    use serde_json::json;
    use std::collections::HashMap;

    #[test]
    fn can_build_user_entity() {
        let mut iss = TrustedIssuer::default();
        let claim_mappings = ClaimMappings::builder().email("email").build();
        for token in ["id_token", "userinfo_token"].iter() {
            let metadata = iss
                .tokens_metadata
                .get_mut(*token)
                .expect("should have token metadata");
            metadata.claim_mapping = claim_mappings.clone();
        }
        let issuers = HashMap::from([("some_iss".into(), iss.clone())]);
        let builder = EntityBuilder::new(EntityNames::default(), true, false, &issuers)
            .expect("should init entity builder");
        let id_token = Token::new(
            "id_token",
            HashMap::from([
                ("iss".to_string(), json!("https://test.jans.org/")),
                ("sub".to_string(), json!("some_sub")),
                ("username".to_string(), json!("some_username")),
                ("role".to_string(), json!("role1")),
                ("email".to_string(), json!("email@email.com")),
                ("exp".to_string(), json!(123)),
            ])
            .into(),
            Some(&iss),
        );
        let userinfo_token = Token::new(
            "userinfo_token",
            HashMap::from([
                ("iss".to_string(), json!("https://test.jans.org/")),
                ("sub".to_string(), json!("some_sub")),
                ("role".to_string(), json!(["role2", "role3"])),
                ("phone_number".to_string(), json!("1234567890")),
                ("exp".to_string(), json!(123)),
            ])
            .into(),
            Some(&iss),
        );
        let tokens = HashMap::from([
            ("id_token".to_string(), id_token),
            ("userinfo_token".to_string(), userinfo_token),
        ]);
        let token_principal_mappings = TokenPrincipalMappings::from(
            [
                TokenPrincipalMapping {
                    principal: "Jans::User".into(),
                    attr_name: "id_token".into(),
                    expr: RestrictedExpression::new_entity_uid(
                        EntityUid::from_str("Jans::Id_token::\"some_jti\"".into())
                            .expect("should parse id_token EntityUid"),
                    ),
                },
                TokenPrincipalMapping {
                    principal: "Jans::User".into(),
                    attr_name: "userinfo_token".into(),
                    expr: RestrictedExpression::new_entity_uid(
                        EntityUid::from_str("Jans::Userinfo_token::\"some_jti\"".into())
                            .expect("should parse Userinfo_token EntityUid"),
                    ),
                },
            ]
            .to_vec(),
        );
        let (entity, _) = builder
            .build_user_entity(&tokens, &token_principal_mappings)
            .expect("should build user entity");

        assert_entity_eq(
            &entity,
            json!({
                "uid": {"type": "Jans::User", "id": "some_sub"},
                "attrs": {
                    "sub": "some_sub",
                    "email": {
                        "domain": "email.com",
                        "uid": "email",
                    },
                    "phone_number": "1234567890",
                    "role": ["role1", "role2", "role3"],
                    "username": "some_username",
                    "id_token": {"__entity": {
                        "type": "Jans::Id_token",
                        "id": "some_jti",
                    }},
                    "userinfo_token": {"__entity": {
                        "type": "Jans::Userinfo_token",
                        "id": "some_jti",
                    }},
                },
                "parents": [
                    {"type": "Jans::Role", "id": "role1"},
                    {"type": "Jans::Role", "id": "role2"},
                    {"type": "Jans::Role", "id": "role3"},
                ],
            }),
            Some(cedarling_schema()),
        );
    }
}
