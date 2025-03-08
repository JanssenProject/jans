// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::entity_id_getters::*;
use super::*;
use derive_more::derive::Deref;

impl EntityBuilder {
    pub fn build_tkn_entity(
        &self,
        tkn_type_name: &str,
        token: &Token,
        tkn_principal_mappings: &mut TokenPrincipalMappings,
    ) -> Result<(Entity, Option<Entity>), BuildEntityError> {
        // Build entity attributes
        let mut attrs = build_entity_attrs(EntityAttrsSrc::from(token))
            .map_err(|e| e.while_building(tkn_type_name))?;

        let iss_entity = self
            .replace_iss_with_entity(token, &mut attrs)
            .map_err(|e| e.while_building(tkn_type_name))?;

        let entity_id = get_first_valid_entity_id(&TokenIdSrcs::resolve(token))
            .map_err(|e| e.while_building(tkn_type_name))?;

        let uid = EntityUid::from_str(&format!("{}::\"{}\"", tkn_type_name, entity_id))
            .map_err(|e| BuildEntityErrorKind::from(e).while_building(tkn_type_name))?;
        let tkn_entity = Entity::new(uid, attrs, HashSet::new())
            .map_err(|e| BuildEntityErrorKind::from(e).while_building(tkn_type_name))?;

        // Record the principal mappings for later use
        if let Some(metadata) = token.get_metadata() {
            let expr = RestrictedExpression::new_entity_uid(tkn_entity.uid());
            for principal in metadata.principal_mapping.iter() {
                tkn_principal_mappings.insert(TokenPrincipalMapping {
                    principal: principal.clone(),
                    attr_name: token.name.clone(),
                    expr: expr.clone(),
                });
            }
        }

        Ok((tkn_entity, iss_entity))
    }
}

#[derive(Deref)]
struct TokenIdSrcs<'a>(Vec<EntityIdSrc<'a>>);

#[derive(Clone, Copy)]
struct TokenIdSrc<'a> {
    claim: &'a str,
}

impl<'a> TokenIdSrcs<'a> {
    fn resolve(token: &'a Token) -> Self {
        const DEFAULT_TKN_ID_SRCS: &[TokenIdSrc] = &[TokenIdSrc { claim: "jti" }];

        let mut eid_srcs = Vec::with_capacity(2);

        for src in DEFAULT_TKN_ID_SRCS.iter() {
            // if a `token_id` is availble in the token's entity metadata
            let claim = if let Some(claim) = token.get_metadata().and_then(|m| m.user_id.as_ref()) {
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

        Self(
            DEFAULT_TKN_ID_SRCS
                .iter()
                .fold(Vec::new(), |mut acc, &src| {
                    if let Some(claim) = token.get_metadata().map(|m| &m.token_id) {
                        acc.push(EntityIdSrc { token, claim });
                        if claim != src.claim {
                            acc.push(EntityIdSrc {
                                token,
                                claim: src.claim,
                            });
                        }
                    } else {
                        acc.push(EntityIdSrc {
                            token,
                            claim: src.claim,
                        });
                    };

                    acc
                }),
        )
    }
}

#[derive(Debug, thiserror::Error)]
#[error("failed to create token entity, `{token_name}`: {err}")]
pub struct BuildTokenEntityError {
    pub token_name: String,
    pub err: BuildEntityError,
}

#[cfg(test)]
mod test {
    use super::super::test::*;
    use super::super::*;
    use crate::common::policy_store::TrustedIssuer;
    use cedar_policy::Entities;
    use serde_json::json;
    use std::collections::HashMap;

    #[test]
    fn can_build_access_tkn_entity() {
        let iss = TrustedIssuer::default();
        let issuers = HashMap::from([("some_iss".into(), iss.clone())]);
        let builder = EntityBuilder::new(EntityBuilderConfig::default(), &issuers)
            .expect("should init entity builder");
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
        let (entity, _) = builder
            .build_tkn_entity(
                "Jans::Access_token",
                &access_token,
                &mut TokenPrincipalMappings::default(),
            )
            .expect("should build access_token entity");

        // Check if the entity has the correct attributes
        assert_eq!(
            entity
                .clone()
                .to_json_value()
                .expect("should serialize entity to JSON"),
            json!({
                "uid": {"type": "Jans::Access_token", "id": "some_jti"},
                "attrs": {
                    "iss": {"__entity": {
                        "type": "Jans::TrustedIssuer",
                        "id": "some_iss",
                    }},
                    "aud": "some_aud",
                    "jti": "some_jti",
                },
                "parents": [],
            }),
            "Access_token entity should have the correct attrs"
        );

        // Check if the entity conforms to the schema
        Entities::from_entities([entity], Some(cedarling_schema()))
            .expect("Access_token entity should conform to the schema");
    }

    #[test]
    fn can_build_id_tkn_entity() {
        let iss = TrustedIssuer::default();
        let issuers = HashMap::from([("some_iss".into(), iss.clone())]);
        let builder = EntityBuilder::new(EntityBuilderConfig::default(), &issuers)
            .expect("should init entity builder");
        let access_token = Token::new(
            "access_token",
            HashMap::from([
                ("iss".to_string(), json!("https://test.jans.org/")),
                ("sub".to_string(), json!("some_sub")),
                ("jti".to_string(), json!("some_jti")),
            ])
            .into(),
            Some(&iss),
        );
        let (entity, _) = builder
            .build_tkn_entity(
                "Jans::Id_token",
                &access_token,
                &mut TokenPrincipalMappings::default(),
            )
            .expect("should build id_token entity");

        // Check if the entity has the correct attributes
        assert_entity_eq(
            &entity,
            json!({
                "uid": {"type": "Jans::Id_token", "id": "some_jti"},
                "attrs": {
                    "iss": {"__entity": {
                        "type": "Jans::TrustedIssuer",
                        "id": "some_iss",
                    }},
                    "sub": "some_sub",
                    "jti": "some_jti",
                },
                "parents": [],
            }),
            Some(cedarling_schema()),
        );
    }

    #[test]
    fn can_build_userinfo_tkn_entity() {
        let iss = TrustedIssuer::default();
        let issuers = HashMap::from([("some_iss".into(), iss.clone())]);
        let builder = EntityBuilder::new(EntityBuilderConfig::default(), &issuers)
            .expect("should init entity builder");
        let access_token = Token::new(
            "userinfo_token",
            HashMap::from([
                ("iss".to_string(), json!("https://test.jans.org/")),
                ("sub".to_string(), json!("some_sub")),
                ("jti".to_string(), json!("some_jti")),
            ])
            .into(),
            Some(&iss),
        );
        let (entity, _) = builder
            .build_tkn_entity(
                "Jans::Userinfo_token",
                &access_token,
                &mut TokenPrincipalMappings::default(),
            )
            .expect("should build userinfo_token entity");

        // Check if the entity has the correct attributes
        assert_eq!(
            entity
                .clone()
                .to_json_value()
                .expect("should serialize entity to JSON"),
            json!({
                "uid": {"type": "Jans::Userinfo_token", "id": "some_jti"},
                "attrs": {
                    "iss": {"__entity": {
                        "type": "Jans::TrustedIssuer",
                        "id": "some_iss",
                    }},
                    "sub": "some_sub",
                    "jti": "some_jti",
                },
                "parents": [],
            }),
            "Userinfo_token entity should have the correct attrs"
        );

        assert_entity_eq(
            &entity,
            json!({
                "uid": {"type": "Jans::Userinfo_token", "id": "some_jti"},
                "attrs": {
                    "iss": {"__entity": {
                        "type": "Jans::TrustedIssuer",
                        "id": "some_iss",
                    }},
                    "sub": "some_sub",
                    "jti": "some_jti",
                },
                "parents": [],
            }),
            Some(cedarling_schema()),
        );
    }

    #[test]
    fn builds_str_iss_if_entity_is_unavailable() {
        let builder = EntityBuilder::new(EntityBuilderConfig::default(), &HashMap::new())
            .expect("should init entity builder");
        let access_token = Token::new(
            "userinfo_token",
            HashMap::from([
                ("iss".to_string(), json!("https://test.jans.org/")),
                ("jti".to_string(), json!("some_jti")),
            ])
            .into(),
            None,
        );
        let (entity, _) = builder
            .build_tkn_entity(
                "Jans::Access_token",
                &access_token,
                &mut TokenPrincipalMappings::default(),
            )
            .expect("should build access_token entity");

        assert_entity_eq(
            &entity,
            json!({
                "uid": {"type": "Jans::Access_token", "id": "some_jti"},
                "attrs": {
                    "iss": "https://test.jans.org/",
                    "jti": "some_jti",
                },
                "parents": [],
            }),
            None,
        );
    }
}
