// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::*;
use cedar_policy::{EntityId, EntityTypeName, EntityUid};

impl EntityBuilder {
    /// Tries to build role entities using each given token. Will return an empty Vec
    /// if no entities were created.
    pub fn try_build_role_entities(
        &self,
        tokens: &DecodedTokens,
    ) -> Result<Vec<Entity>, BuildRoleEntityError> {
        let entity_name = &self.entity_names.role;
        let mut entities = Vec::new();

        // Get entity namespace and type
        let mut entity_name = entity_name.to_string();
        if let Some((namespace, _entity_type)) = self.schema.get_entity_type(&entity_name) {
            if !namespace.is_empty() {
                entity_name = [namespace.as_str(), &entity_name].join(CEDAR_NAMESPACE_SEPARATOR);
            }
        }

        for token in [
            tokens.userinfo.as_ref(),
            tokens.id.as_ref(),
            tokens.access.as_ref(),
        ]
        .into_iter()
        .flatten()
        {
            let role_claim = token.role_mapping();
            if let Some(claim) = token.get_claim(role_claim).as_ref() {
                match claim.value() {
                    // Case: the claim is a String
                    serde_json::Value::String(role) => {
                        let entity = build_entity(&entity_name, role)
                            .map_err(|e| BuildRoleEntityError::map_tkn_err(token, e))?;
                        entities.push(entity);
                    },

                    // Case: the claim is an Array
                    serde_json::Value::Array(vec) => {
                        for val in vec {
                            let role = match val.as_str() {
                                Some(role) => role,
                                None => {
                                    return Err(BuildRoleEntityError::map_tkn_err(
                                        token,
                                        BuildEntityError::json_type_err("str", val),
                                    ));
                                },
                            };

                            let entity = build_entity(&entity_name, role)
                                .map_err(|e| BuildRoleEntityError::map_tkn_err(token, e))?;
                            entities.push(entity);
                        }
                    },
                    _ => unimplemented!(),
                }
            }
        }

        return Ok(entities);
    }
}

fn build_entity(name: &str, id: &str) -> Result<Entity, BuildEntityError> {
    let name = EntityTypeName::from_str(&name).map_err(BuildEntityError::ParseEntityTypeName)?;
    let id = EntityId::from_str(id).expect("expected infallible");
    let uid = EntityUid::from_type_name_and_id(name, id);
    let entity = Entity::new(uid, HashMap::new(), HashSet::new())?;
    Ok(entity)
}

#[derive(Debug, thiserror::Error)]
pub enum BuildRoleEntityError {
    #[error("failed to build role entity from access token: {0}")]
    Access(#[source] BuildEntityError),
    #[error("failed to build role entity from id token: {0}")]
    Id(#[source] BuildEntityError),
    #[error("failed to build role entity from userinfo token: {0}")]
    Userinfo(#[source] BuildEntityError),
}

impl BuildRoleEntityError {
    pub fn map_tkn_err(token: &Token, err: BuildEntityError) -> Self {
        match token.kind {
            TokenKind::Access => BuildRoleEntityError::Access(err),
            TokenKind::Id => BuildRoleEntityError::Id(err),
            TokenKind::Userinfo => BuildRoleEntityError::Userinfo(err),
            TokenKind::Transaction => unimplemented!("transaction tokens are not yet supported"),
        }
    }
}

#[cfg(test)]
mod test {
    use super::super::*;
    use crate::common::cedar_schema::cedar_json::CedarSchemaJson;
    use crate::common::policy_store::TrustedIssuer;
    use crate::jwt::{Token, TokenClaims};
    use serde_json::json;
    use std::collections::HashMap;

    fn test_schema() -> CedarSchemaJson {
        serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": {
                "entityTypes": {
                    "Role": {},
                    "User": {
                        "memberOfTypes": ["Role"],
                        "shape": {
                            "type": "Record",
                            "attributes":  {},
                    }
                }}}
        }))
        .expect("should successfully create test schema")
    }

    fn test_build_entity_from_str_claim(tokens: DecodedTokens) {
        let schema = test_schema();
        let builder = EntityBuilder::new(schema, EntityNames::default(), false, false);
        let entity = builder
            .try_build_role_entities(&tokens)
            .expect("expected to build role entities");

        assert_eq!(entity.len(), 1);
        assert_eq!(entity[0].uid().to_string(), "Jans::Role::\"admin\"");
    }

    #[test]
    fn can_build_using_userinfo_tkn_vec_claim() {
        let iss = TrustedIssuer::default();
        let userinfo_token = Token::new_userinfo(
            TokenClaims::new(HashMap::from([(
                "role".to_string(),
                json!(["admin", "user"]),
            )])),
            Some(&iss),
        );
        let tokens = DecodedTokens {
            access: None,
            id: None,
            userinfo: Some(userinfo_token),
        };
        let schema = test_schema();
        let builder = EntityBuilder::new(schema, EntityNames::default(), false, false);
        let entity = builder
            .try_build_role_entities(&tokens)
            .expect("expected to build role entities");

        assert_eq!(entity.len(), 2);
        assert_eq!(entity[0].uid().to_string(), "Jans::Role::\"admin\"");
        assert_eq!(entity[1].uid().to_string(), "Jans::Role::\"user\"");
    }

    #[test]
    fn can_build_using_userinfo_tkn_string_claim() {
        let iss = TrustedIssuer::default();
        let userinfo_token = Token::new_userinfo(
            TokenClaims::new(HashMap::from([("role".to_string(), json!("admin"))])),
            Some(&iss),
        );
        let tokens = DecodedTokens {
            access: None,
            id: None,
            userinfo: Some(userinfo_token),
        };
        test_build_entity_from_str_claim(tokens);
    }

    #[test]
    fn can_build_using_id_tkn() {
        let iss = TrustedIssuer::default();
        let id_token = Token::new_id(
            TokenClaims::new(HashMap::from([("role".to_string(), json!("admin"))])),
            Some(&iss),
        );
        let tokens = DecodedTokens {
            access: None,
            id: Some(id_token),
            userinfo: None,
        };
        test_build_entity_from_str_claim(tokens);
    }

    #[test]
    fn can_build_using_access_tkn() {
        let iss = TrustedIssuer::default();
        let access_token = Token::new_access(
            TokenClaims::new(HashMap::from([("role".to_string(), json!("admin"))])),
            Some(&iss),
        );
        let tokens = DecodedTokens {
            access: Some(access_token),
            id: None,
            userinfo: None,
        };
        test_build_entity_from_str_claim(tokens);
    }
}
