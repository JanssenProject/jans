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
        built_entities: &BuiltEntities,
        parents: HashSet<EntityUid>,
    ) -> Result<Entity, BuildEntityError> {
        let id = get_first_valid_entity_id(&TokenIdSrcs::resolve(token))
            .map_err(|e| e.while_building(tkn_type_name))?;

        let tkn_entity = self.build_entity(
            tkn_type_name,
            &id,
            parents,
            token.claims_value(),
            built_entities,
            token.claim_mappings(),
        )?;

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

        Ok(tkn_entity)
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
    use cedar_policy::Schema;
    use serde_json::json;
    use std::collections::HashMap;

    #[test]
    fn can_build_tkn_entity_with_schema() {
        let schema_src = r#"
        namespace Jans {
            entity TrustedIssuer;
            entity Role;
            entity Access_token in [Role] = {
                iss: TrustedIssuer,
                aud?: String,
                jti: String,
            };
        }
        "#;
        let schema = Schema::from_str(schema_src).expect("build Schema");
        let validator_schema =
            ValidatorSchema::from_str(schema_src).expect("build ValidatorSchema");
        let iss = TrustedIssuer::default();
        let issuers = HashMap::from([("some_iss".into(), iss.clone())]);
        let builder = EntityBuilder::new(
            EntityBuilderConfig::default(),
            &issuers,
            Some(&validator_schema),
        )
        .expect("should init entity builder");
        let access_token = Token::new(
            "access_token",
            HashMap::from([
                ("iss".to_string(), json!("https://test.jans.org/")),
                ("jti".to_string(), json!("some_jti")),
                ("not_in_schema".to_string(), json!("not_in_schema")),
            ])
            .into(),
            Some(&iss),
        );
        let parents = HashSet::from(["Jans::Role::\"some_role\""
            .parse()
            .expect("a valid entity UID")]);
        let entity = builder
            .build_tkn_entity(
                "Jans::Access_token",
                &access_token,
                &mut TokenPrincipalMappings::default(),
                &BuiltEntities::from(&builder.iss_entities),
                parents,
            )
            .expect("should build access_token entity");

        assert_entity_eq(
            &entity,
            json!({
                "uid": {"type": "Jans::Access_token", "id": "some_jti"},
                "attrs": {
                    "iss": {"__entity": {
                        "type": "Jans::TrustedIssuer",
                        "id": "some_iss",
                    }},
                    "jti": "some_jti",
                },
                "parents": [{"type": "Jans::Role", "id": "some_role"}],
            }),
            Some(&schema),
        );
    }

    #[test]
    fn can_build_tkn_entity_without_schema() {
        let iss = TrustedIssuer::default();
        let issuers = HashMap::from([("some_iss".into(), iss.clone())]);
        let builder = EntityBuilder::new(EntityBuilderConfig::default(), &issuers, None)
            .expect("should init entity builder");
        let access_token = Token::new(
            "access_token",
            HashMap::from([
                ("iss".to_string(), json!("https://test.jans.org/")),
                ("jti".to_string(), json!("some_jti")),
                ("not_in_schema".to_string(), json!("not_in_schema")),
            ])
            .into(),
            Some(&iss),
        );
        let parents = HashSet::from(["Jans::Role::\"some_role\""
            .parse()
            .expect("a valid entity UID")]);
        let entity = builder
            .build_tkn_entity(
                "Jans::Access_token",
                &access_token,
                &mut TokenPrincipalMappings::default(),
                &BuiltEntities::from(&builder.iss_entities),
                parents,
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
                "parents": [{"type": "Jans::Role", "id": "some_role"}],
            }),
            None,
        );
    }
}
