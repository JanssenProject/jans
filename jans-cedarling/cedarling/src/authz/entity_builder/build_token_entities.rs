// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::*;

const DEFAULT_TKN_PRINCIPAL_IDENTIFIER: &str = "jti";

impl EntityBuilder {
    pub fn build_access_tkn_entity(&self, token: &Token) -> Result<Entity, BuildTokenEntityError> {
        let entity_name = self.entity_names.access_token.as_ref();
        self.build_tkn_entity(entity_name, token)
    }

    pub fn build_id_tkn_entity(&self, token: &Token) -> Result<Entity, BuildTokenEntityError> {
        let entity_name = self.entity_names.id_token.as_ref();
        self.build_tkn_entity(entity_name, token)
    }

    pub fn build_userinfo_tkn_entity(
        &self,
        token: &Token,
    ) -> Result<Entity, BuildTokenEntityError> {
        let entity_name = self.entity_names.userinfo_token.as_ref();
        self.build_tkn_entity(entity_name, token)
    }

    fn build_tkn_entity(
        &self,
        entity_name: &str,
        token: &Token,
    ) -> Result<Entity, BuildTokenEntityError> {
        let id_src_claim = token
            .metadata()
            .principal_identifier
            .as_deref()
            .unwrap_or(DEFAULT_TKN_PRINCIPAL_IDENTIFIER);
        self.build_entity(entity_name, token, id_src_claim, vec![], HashSet::new())
            .map_err(|err| BuildTokenEntityError {
                token_kind: token.kind,
                err,
            })
    }
}

#[derive(Debug, thiserror::Error)]
#[error("failed to create {token_kind} token entity: {err}")]
pub struct BuildTokenEntityError {
    pub token_kind: TokenKind,
    pub err: BuildEntityError,
}

impl BuildTokenEntityError {
    pub fn access_tkn_unavailable() -> Self {
        Self {
            token_kind: TokenKind::Access,
            err: BuildEntityError::TokenUnavailable,
        }
    }

    pub fn id_tkn_unavailable() -> Self {
        Self {
            token_kind: TokenKind::Id,
            err: BuildEntityError::TokenUnavailable,
        }
    }

    pub fn userinfo_tkn_unavailable() -> Self {
        Self {
            token_kind: TokenKind::Userinfo,
            err: BuildEntityError::TokenUnavailable,
        }
    }
}

#[cfg(test)]
mod test {
    use super::super::*;
    use crate::common::cedar_schema::cedar_json::CedarSchemaJson;
    use crate::common::policy_store::{ClaimMappings, TokenEntityMetadata, TrustedIssuer};
    use crate::jwt::{Token, TokenClaims};
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
        let iss = TrustedIssuer {
            access_tokens: TokenEntityMetadata {
                claim_mapping: serde_json::from_value::<ClaimMappings>(json!({
                    "url": {
                        "parser": "regex",
                        "type": "Jans::Url",
                        "regex_expression": r#"^(?P<SCHEME>[a-zA-Z][a-zA-Z0-9+.-]*):\/\/(?P<DOMAIN>[^\/]+)(?P<PATH>\/.*)?$"#,
                        "SCHEME": {"attr": "scheme", "type": "String"},
                        "DOMAIN": {"attr": "domain", "type": "String"},
                        "PATH": {"attr": "path", "type": "String"}
                    }
                }))
                .unwrap(),
                ..Default::default()
            },
            ..Default::default()
        };
        let issuers = HashMap::from([("test_iss".into(), iss.clone())]);
        issuers
    }

    #[test]
    fn can_build_access_tkn_entity() {
        let schema = test_schema();
        let issuers = test_issusers();
        let builder = EntityBuilder::new(schema, EntityNames::default(), false, false);
        let access_token = Token::new_access(
            TokenClaims::new(HashMap::from([
                ("jti".to_string(), json!("tkn-123")),
                (
                    "trusted_issuer".to_string(),
                    json!("https://some-iss.com/.well-known/openid-configuration"),
                ),
            ])),
            Some(&issuers.get("test_iss").unwrap()),
        );
        let entity = builder
            .build_access_tkn_entity(&access_token)
            .expect("expected to successfully build token entity");

        assert_eq!(entity.uid().to_string(), "Jans::Access_token::\"tkn-123\"");

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
}
