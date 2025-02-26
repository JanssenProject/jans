// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::*;
use derive_more::derive::Deref;
use url::Url;

const REGISTERED_ISS_CLAIM_NAME: &str = "iss";
const DEFAULT_ISS_ATTR_NAME: &str = "iss";

impl EntityBuilder {
    pub fn build_tkn_entity(
        &self,
        tkn_type_name: &str,
        token: &Token,
        tkn_principal_mappings: &mut TokenPrincipalMappings,
    ) -> Result<Entity, BuildEntityError> {
        // Build entity attributes
        let mut attrs = build_entity_attrs(vec![token.claims_value().into()]);

        // Overwrite the iss attribute with an entity reference to the attribute
        // if available
        if let Some(claim) = token.get_claim(REGISTERED_ISS_CLAIM_NAME) {
            let iss_claim_value = claim.as_str().map_err(|e| {
                BuildEntityErrorKind::from(e).while_building(&self.entity_names.iss)
            })?;
            let iss_url = Url::parse(iss_claim_value).map_err(|e| {
                BuildEntityErrorKind::from(e).while_building(&self.entity_names.iss)
            })?;
            if let Some(entity) = self.iss_entities.get(&iss_url.origin()) {
                let entity_ref = RestrictedExpression::new_entity_uid(entity.uid());
                attrs.insert(DEFAULT_ISS_ATTR_NAME.into(), entity_ref);
            }
        }

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
        let default_srcs: &[TokenIdSrc; 1] = &[TokenIdSrc { claim: "jti" }];

        Self(default_srcs.iter().fold(Vec::new(), |mut acc, &src| {
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
        }))
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
    use super::super::*;
    use crate::common::policy_store::{ClaimMappings, TokenEntityMetadata, TrustedIssuer};
    use serde_json::json;
    use std::collections::HashMap;
    use test_utils::assert_eq;

    fn test_issuers() -> HashMap<String, TrustedIssuer> {
        let token_entity_metadata_builder = TokenEntityMetadata::builder().claim_mapping(
            serde_json::from_value::<ClaimMappings>(json!({
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
            tokens_metadata: HashMap::from([
                (
                    "access_token".to_string(),
                    token_entity_metadata_builder
                        .clone()
                        .entity_type_name("Jans::Access_token".into())
                        .build(),
                ),
                (
                    "id_token".to_string(),
                    token_entity_metadata_builder
                        .clone()
                        .entity_type_name("Jans::id_token".into())
                        .build(),
                ),
                (
                    "userinfo_token".to_string(),
                    token_entity_metadata_builder
                        .clone()
                        .entity_type_name("Jans::Userinfo_token".into())
                        .build(),
                ),
            ]),
            ..Default::default()
        };
        let issuers = HashMap::from([("test_iss".into(), iss.clone())]);
        issuers
    }

    fn test_build_entity(tkn_entity_type_name: &str, token: Token, builder: &EntityBuilder) {
        let entity = builder
            .build_tkn_entity(
                tkn_entity_type_name,
                &token,
                &mut TokenPrincipalMappings::default(),
            )
            .expect("expected to successfully build token entity");

        let entity_json = entity
            .to_json_value()
            .expect("should serialize entity to json");
        assert_eq!(
            entity_json,
            json!({
                "uid": {
                    "type": tkn_entity_type_name,
                    "id": "tkn-123",
                },
                "attrs": {
                    "jti": "tkn-123",
                    "iss": { "__entity": {
                        "type": "Jans::TrustedIssuer",
                        "id": "test_iss",
                    }}
                },
                "parents": [],
            })
        );
    }

    #[test]
    fn can_build_access_tkn_entity() {
        let issuers = test_issuers();
        let builder = EntityBuilder::new(EntityNames::default(), false, false, &issuers);
        let access_token = Token::new(
            "access_token",
            HashMap::from([
                ("jti".to_string(), json!("tkn-123")),
                ("iss".to_string(), json!("https://test.jans.org/")),
            ])
            .into(),
            Some(&issuers.get("test_iss").unwrap()),
        );
        test_build_entity("Jans::Access_token", access_token, &builder);
    }

    #[test]
    fn can_build_id_tkn_entity() {
        let issuers = test_issuers();
        let builder = EntityBuilder::new(EntityNames::default(), false, false, &issuers);
        let id_token = Token::new(
            "id_token",
            HashMap::from([
                ("jti".to_string(), json!("tkn-123")),
                (
                    "iss".to_string(),
                    json!("https://test.jans.org/.well-known/openid-configuration"),
                ),
            ])
            .into(),
            Some(&issuers.get("test_iss").unwrap()),
        );
        test_build_entity("Jans::id_token", id_token, &builder);
    }

    #[test]
    fn can_build_userinfo_tkn_entity() {
        let issuers = test_issuers();
        let builder = EntityBuilder::new(EntityNames::default(), false, false, &issuers);
        let userinfo_token = Token::new(
            "iss",
            HashMap::from([
                ("jti".to_string(), json!("tkn-123")),
                (
                    "iss".to_string(),
                    json!("https://test.jans.org/.well-known/openid-configuration"),
                ),
            ])
            .into(),
            Some(&issuers.get("test_iss").unwrap()),
        );
        test_build_entity("Jans::Userinfo_token", userinfo_token, &builder);
    }
}
