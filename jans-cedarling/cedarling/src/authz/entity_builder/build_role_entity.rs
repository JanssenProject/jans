// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::*;
use cedar_policy::{EntityId, EntityTypeName, EntityUid, ParseErrors};
use serde::Deserialize;

// TODO: make a bootstrap property to control which tokens to use
// when creating token entities
const DEFAULT_ROLE_ENTITY_TKN_SRCS: [&str; 3] = ["userinfo_token", "id_token", "access_token"];

#[derive(Debug, serde::Deserialize)]
#[serde(untagged)]
enum UnifyClaims {
    Single(String),
    Multiple(Vec<String>),
}

impl UnifyClaims {
    fn iter<'a>(&'a self) -> Box<dyn std::iter::Iterator<Item = &'a String> + 'a> {
        match self {
            Self::Single(ref v) => Box::new(std::iter::once(v)),
            Self::Multiple(ref vs) => Box::new(vs.iter()),
        }
    }
}

impl EntityBuilder {
    /// Tries to build role entities using each given token. Will return an empty Vec
    /// if no entities were created.
    pub fn try_build_role_entities(
        &self,
        tokens: &HashMap<String, Token>,
    ) -> Result<Vec<Entity>, BuildRoleEntityError> {
        // Check if the entity is in the schema
        let entity_name = self.entity_names.role.to_string();
        self.schema
            .get_entity_schema(&entity_name, None)
            .map_err(|e| BuildRoleEntityError::ParseTypeName(entity_name.clone(), e))?
            .ok_or(BuildRoleEntityError::EntityNotInSchema(entity_name.clone()))?;

        let mut entities = HashMap::new();

        let token_refs = DEFAULT_ROLE_ENTITY_TKN_SRCS
            .iter()
            .filter_map(|src| tokens.get(*src));

        for token in token_refs {
            let role_claim = token.role_mapping();
            if let Some(claim) = token.get_claim(role_claim).as_ref() {
                let unified_claims = UnifyClaims::deserialize(claim.value());
                let claim_role_name_iter = match unified_claims {
                    Ok(ref unified_claims) => unified_claims.iter(),
                    Err(_) => {
                        return Err(BuildRoleEntityError::map_tkn_err(
                            token,
                            BuildEntityError::TokenClaimTypeMismatch(
                                TokenClaimTypeError::type_mismatch(
                                    role_claim,
                                    "String or Array",
                                    claim.value(),
                                ),
                            ),
                        ));
                    },
                };

                for claim_role_name in claim_role_name_iter {
                    if !entities.contains_key(claim_role_name) {
                        let entity = build_entity(&entity_name, claim_role_name)
                            .map_err(|e| BuildRoleEntityError::map_tkn_err(token, e))?;
                        entities.insert(claim_role_name.clone(), entity);
                    }
                }
            }
        }

        Ok(entities.into_values().collect())
    }
}

fn build_entity(name: &str, id: &str) -> Result<Entity, BuildEntityError> {
    let name = EntityTypeName::from_str(name).map_err(BuildEntityError::ParseEntityTypeName)?;
    let id = EntityId::from_str(id).map_err(BuildEntityError::ParseEntityId)?;
    let uid = EntityUid::from_type_name_and_id(name, id);
    let entity = Entity::new(uid, HashMap::new(), HashSet::new())?;
    Ok(entity)
}

#[derive(Debug, thiserror::Error)]
pub enum BuildRoleEntityError {
    #[error("failed to build role entity from `{0}`: {1}")]
    Token(String, BuildEntityError),
    #[error("failed to parse entity type name `{0}`: {1}")]
    ParseTypeName(String, ParseErrors),
    #[error("the entity `{0}` is not in the schema")]
    EntityNotInSchema(String),
}

impl BuildRoleEntityError {
    pub fn map_tkn_err(token: &Token, err: BuildEntityError) -> Self {
        Self::Token(token.name.clone(), err)
    }
}

#[cfg(test)]
mod test {
    use super::super::*;
    use crate::common::cedar_schema::cedar_json::CedarSchemaJson;
    use crate::common::policy_store::TrustedIssuer;
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

    fn test_build_entity_from_str_claim(tokens: &HashMap<String, Token>) {
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
        let userinfo_token = Token::new(
            "userinfo_token",
            HashMap::from([("role".to_string(), json!(["admin", "user"]))]).into(),
            Some(&iss),
        );
        let tokens = HashMap::from([("userinfo_token".to_string(), userinfo_token)]);
        let schema = test_schema();
        let builder = EntityBuilder::new(schema, EntityNames::default(), false, false);
        let entity = builder
            .try_build_role_entities(&tokens)
            .expect("expected to build role entities");

        assert_eq!(entity.len(), 2);
        let entity_uids = entity
            .iter()
            .map(|e| e.uid().to_string())
            .collect::<HashSet<String>>();
        assert_eq!(
            entity_uids,
            HashSet::from(["Jans::Role::\"admin\"", "Jans::Role::\"user\""].map(|s| s.to_string()))
        );
    }

    #[test]
    fn can_build_using_userinfo_tkn_string_claim() {
        let iss = TrustedIssuer::default();
        let userinfo_token = Token::new(
            "userinfo_token",
            HashMap::from([("role".to_string(), json!("admin"))]).into(),
            Some(&iss),
        );
        let tokens = HashMap::from([("userinfo_token".to_string(), userinfo_token)]);
        test_build_entity_from_str_claim(&tokens);
    }

    #[test]
    fn can_build_using_id_tkn() {
        let iss = TrustedIssuer::default();
        let id_token = Token::new(
            "id_token",
            HashMap::from([("role".to_string(), json!("admin"))]).into(),
            Some(&iss),
        );
        let tokens = HashMap::from([("id_token".to_string(), id_token)]);
        test_build_entity_from_str_claim(&tokens);
    }

    #[test]
    fn can_build_using_access_tkn() {
        let iss = TrustedIssuer::default();
        let access_token = Token::new(
            "access_token",
            HashMap::from([("role".to_string(), json!("admin"))]).into(),
            Some(&iss),
        );
        let tokens = HashMap::from([("access_token".to_string(), access_token)]);
        test_build_entity_from_str_claim(&tokens);
    }

    #[test]
    fn ignores_duplicate_roles() {
        let iss = TrustedIssuer::default();
        let access_token = Token::new(
            "access_token",
            HashMap::from([("role".to_string(), json!("admin"))]).into(),
            Some(&iss),
        );
        let id_token = Token::new(
            "id_token",
            HashMap::from([("role".to_string(), json!("admin"))]).into(),
            Some(&iss),
        );
        let userinfo_token = Token::new(
            "userinfo_token",
            HashMap::from([("role".to_string(), json!("admin"))]).into(),
            Some(&iss),
        );
        let tokens = HashMap::from([
            ("access_token".to_string(), access_token),
            ("id_token".to_string(), id_token),
            ("userinfo_token".to_string(), userinfo_token),
        ]);
        test_build_entity_from_str_claim(&tokens);
    }

    #[test]
    fn can_create_multiple_different_roles_from_different_tokens() {
        let iss = TrustedIssuer::default();
        let schema = test_schema();
        let access_token = Token::new(
            "access_token",
            HashMap::from([("role".to_string(), json!("role1"))]).into(),
            Some(&iss),
        );
        let id_token = Token::new(
            "id_token",
            HashMap::from([("role".to_string(), json!("role2"))]).into(),
            Some(&iss),
        );
        let userinfo_token = Token::new(
            "userinfo_token",
            HashMap::from([("role".to_string(), json!(["role3", "role4"]))]).into(),
            Some(&iss),
        );
        let tokens = HashMap::from([
            ("access_token".to_string(), access_token),
            ("id_token".to_string(), id_token),
            ("userinfo_token".to_string(), userinfo_token),
        ]);
        let builder = EntityBuilder::new(schema, EntityNames::default(), false, false);
        let entities = builder
            .try_build_role_entities(&tokens)
            .expect("expected to build role entities");

        let entities = entities
            .iter()
            .map(|e| e.uid().to_string())
            .collect::<HashSet<String>>();
        let expected_entities = (1..=4)
            .map(|x| format!("Jans::Role::\"role{}\"", x))
            .collect::<HashSet<String>>();
        assert_eq!(entities, expected_entities);
    }
}
