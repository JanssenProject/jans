// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::*;

const DEFAULT_TKN_PRINCIPAL_IDENTIFIER: &str = "jti";

impl EntityBuilder {
    pub fn build_tkn_entity(
        &self,
        entity_name: &str,
        token: &Token,
        built_entities: &BuiltEntities,
    ) -> Result<Entity, BuildTokenEntityError> {
        let id_src_claim = token
            .get_metadata()
            .and_then(|x| x.principal_identifier.as_deref())
            .unwrap_or(DEFAULT_TKN_PRINCIPAL_IDENTIFIER);
        let tkn_entity = build_entity(
            &self.schema,
            entity_name,
            token,
            id_src_claim,
            Vec::new(),
            HashSet::new(),
            built_entities,
        )
        .map_err(|err| BuildTokenEntityError {
            token_name: token.name.clone(),
            err,
        })?;
        Ok(tkn_entity)
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
    use crate::common::cedar_schema::cedar_json::CedarSchemaJson;
    use crate::common::policy_store::{ClaimMappings, TokenEntityMetadata, TrustedIssuer};
    use cedar_policy::EvalResult;
    use serde_json::json;
    use std::collections::HashMap;
    use test_utils::assert_eq;

    fn test_schema() -> CedarSchemaJson {
        serde_json::from_value::<CedarSchemaJson>(json!({
        "Jans": {
            "commonTypes": {
                "Url": {
                    "type": "Record",
                    "attributes": {
                        "scheme": { "type": "String" },
                        "path": { "type": "String" },
                        "domain": { "type": "String" },
                    },
                },
            },
            "entityTypes": {
                "Access_token": {
                    "shape": {
                        "type": "Record",
                        "attributes":  {
                            "jti": { "type": "String" },
                            "trusted_issuer": { "type": "EntityOrCommon", "name": "TrustedIssuer" },
                        },
                    }
                },
                "id_token": {
                    "shape": {
                        "type": "Record",
                        "attributes":  {
                            "jti": { "type": "String" },
                            "trusted_issuer": { "type": "EntityOrCommon", "name": "TrustedIssuer" },
                        },
                    }
                },
                "Userinfo_token": {
                    "shape": {
                        "type": "Record",
                        "attributes":  {
                            "jti": { "type": "String" },
                            "trusted_issuer": { "type": "EntityOrCommon", "name": "TrustedIssuer" },
                        },
                    }
                },
                "TrustedIssuer": {
                    "shape": {
                        "type": "Record",
                        "attributes":  {
                            "issuer_entity_id": { "type": "EntityOrCommon", "name": "Url" }
                        },
                    }
                }
            }
        }}))
        .expect("should deserialize schema")
    }

    fn test_issusers() -> HashMap<String, TrustedIssuer> {
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
                .unwrap()
        );
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
            .build_tkn_entity(tkn_entity_type_name, &token, &BuiltEntities::default())
            .expect("expected to successfully build token entity");

        assert_eq!(
            entity.uid().to_string(),
            format!("{}::\"tkn-123\"", tkn_entity_type_name)
        );

        assert_eq!(
            entity
                .attr("jti")
                .expect("expected entity to have a `jti` attribute")
                .unwrap(),
            EvalResult::String("tkn-123".to_string()),
        );

        let trusted_iss = entity
            .attr("trusted_issuer")
            .expect("expected entity to have a `trusted_issuer` attribute")
            .unwrap();
        if let EvalResult::EntityUid(ref uid) = trusted_iss {
            assert_eq!(uid.type_name().basename(), "TrustedIssuer");
            assert_eq!(
                uid.id().escaped(),
                "https://some-iss.com/.well-known/openid-configuration"
            );
        } else {
            panic!(
                "expected the attribute `trusted_issuer` to be an EntityUid, got: {:?}",
                trusted_iss
            );
        }
    }

    #[test]
    fn can_build_access_tkn_entity() {
        let schema = test_schema();
        let issuers = test_issusers();
        let builder = EntityBuilder::new(schema, EntityNames::default(), false, false);
        let access_token = Token::new(
            "access_token",
            HashMap::from([
                ("jti".to_string(), json!("tkn-123")),
                (
                    "trusted_issuer".to_string(),
                    json!("https://some-iss.com/.well-known/openid-configuration"),
                ),
            ])
            .into(),
            Some(&issuers.get("test_iss").unwrap()),
        );
        test_build_entity("Jans::Access_token", access_token, &builder);
    }

    #[test]
    fn can_build_id_tkn_entity() {
        let schema = test_schema();
        let issuers = test_issusers();
        let builder = EntityBuilder::new(schema, EntityNames::default(), false, false);
        let id_token = Token::new(
            "id_token",
            HashMap::from([
                ("jti".to_string(), json!("tkn-123")),
                (
                    "trusted_issuer".to_string(),
                    json!("https://some-iss.com/.well-known/openid-configuration"),
                ),
            ])
            .into(),
            Some(&issuers.get("test_iss").unwrap()),
        );
        test_build_entity("Jans::id_token", id_token, &builder);
    }

    #[test]
    fn can_build_userinfo_tkn_entity() {
        let schema = test_schema();
        let issuers = test_issusers();
        let builder = EntityBuilder::new(schema, EntityNames::default(), false, false);
        let userinfo_token = Token::new(
            "userinfo_token",
            HashMap::from([
                ("jti".to_string(), json!("tkn-123")),
                (
                    "trusted_issuer".to_string(),
                    json!("https://some-iss.com/.well-known/openid-configuration"),
                ),
            ])
            .into(),
            Some(&issuers.get("test_iss").unwrap()),
        );
        test_build_entity("Jans::Userinfo_token", userinfo_token, &builder);
    }
}
