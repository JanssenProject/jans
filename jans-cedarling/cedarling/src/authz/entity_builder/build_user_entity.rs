// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::*;
use cedar_policy::Entity;
use derive_more::derive::Deref;
use std::collections::HashSet;

const DEFAULT_USER_ATTR_SRCS: &[&str] = &["userinfo_token", "id_token"];

impl EntityBuilder {
    pub fn build_user_entity(
        &self,
        tokens: &HashMap<String, Token>,
        tkn_principal_mappings: &TokenPrincipalMappings,
    ) -> Result<(Entity, Vec<Entity>), BuildEntityError> {
        let user_type_name: &str = self.entity_names.user.as_ref();

        // Get User Entity ID
        let user_id_srcs = UserIdSrcs::resolve(tokens);
        let user_id = get_first_valid_entity_id(&*user_id_srcs)
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
        let user_attr_srcs = DEFAULT_USER_ATTR_SRCS
            .iter()
            .filter_map(|src| tokens.get(*src).map(|tkn| tkn.into()))
            .collect::<Vec<_>>();
        let user_attrs = build_entity_attrs(user_attr_srcs);

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

#[derive(Debug, thiserror::Error)]
pub struct BuildUserEntityError {
    pub errors: Vec<(String, BuildEntityError)>,
}

impl fmt::Display for BuildUserEntityError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        if self.errors.is_empty() {
            writeln!(
                f,
                "failed to create User Entity since no tokens were provided"
            )?;
        } else {
            writeln!(
                f,
                "failed to create User Entity due to the following errors:"
            )?;
            for (token_kind, error) in &self.errors {
                writeln!(f, "- TokenKind {:?}: {}", token_kind, error)?;
            }
        }
        Ok(())
    }
}
