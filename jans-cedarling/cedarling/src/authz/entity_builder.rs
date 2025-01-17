// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

mod build_attrs;
mod build_expr;
mod build_resource_entity;
mod build_role_entity;
mod build_token_entities;
mod build_user_entity;
mod build_workload_entity;
mod mapping;

use super::AuthorizeEntitiesData;
use crate::common::cedar_schema::CEDAR_NAMESPACE_SEPARATOR;
use crate::common::cedar_schema::cedar_json::CedarSchemaJson;
use crate::jwt::{Token, TokenClaimTypeError};
use crate::{AuthorizationConfig, ResourceData};
use build_attrs::{BuildAttrError, ClaimAliasMap, build_entity_attrs_from_tkn};
use build_expr::*;
use build_resource_entity::{BuildResourceEntityError, JsonTypeError};
use build_role_entity::BuildRoleEntityError;
pub use build_token_entities::BuildTokenEntityError;
use build_user_entity::BuildUserEntityError;
use build_workload_entity::BuildWorkloadEntityError;
use cedar_policy::{Entity, EntityId, EntityUid};
use serde_json::Value;
use std::collections::{HashMap, HashSet};
use std::convert::Infallible;
use std::fmt;
use std::str::FromStr;

const DEFAULT_WORKLOAD_ENTITY_NAME: &str = "Jans::Workload";
const DEFAULT_USER_ENTITY_NAME: &str = "Jans::User";
const DEFAULT_ACCESS_TKN_ENTITY_NAME: &str = "Jans::Access_token";
const DEFAULT_ID_TKN_ENTITY_NAME: &str = "Jans::id_token";
const DEFAULT_USERINFO_TKN_ENTITY_NAME: &str = "Jans::Userinfo_token";
const DEFAULT_ROLE_ENTITY_NAME: &str = "Jans::Role";

/// The names of the entities in the schema
#[derive(Debug)]
pub struct EntityNames {
    user: String,
    workload: String,
    role: String,
    tokens: HashMap<String, String>,
}

impl From<&AuthorizationConfig> for EntityNames {
    fn from(config: &AuthorizationConfig) -> Self {
        Self {
            user: config
                .mapping_user
                .clone()
                .unwrap_or_else(|| DEFAULT_USER_ENTITY_NAME.to_string()),
            workload: config
                .mapping_workload
                .clone()
                .unwrap_or_else(|| DEFAULT_WORKLOAD_ENTITY_NAME.to_string()),
            role: config
                .mapping_role
                .clone()
                .unwrap_or_else(|| DEFAULT_ROLE_ENTITY_NAME.to_string()),
            tokens: config.mapping_tokens.clone().into(),
        }
    }
}

impl Default for EntityNames {
    fn default() -> Self {
        let tokens = HashMap::from([
            (
                "access_token".to_string(),
                DEFAULT_ACCESS_TKN_ENTITY_NAME.to_string(),
            ),
            (
                "id_token".to_string(),
                DEFAULT_ID_TKN_ENTITY_NAME.to_string(),
            ),
            (
                "userinfo_token".to_string(),
                DEFAULT_USERINFO_TKN_ENTITY_NAME.to_string(),
            ),
        ]);
        Self {
            user: DEFAULT_USER_ENTITY_NAME.to_string(),
            workload: DEFAULT_WORKLOAD_ENTITY_NAME.to_string(),
            role: DEFAULT_ROLE_ENTITY_NAME.to_string(),
            tokens,
        }
    }
}

pub struct EntityBuilder {
    schema: CedarSchemaJson,
    entity_names: EntityNames,
    build_workload: bool,
    build_user: bool,
    token_entity_mapping: HashMap<String, String>,
}

impl EntityBuilder {
    pub fn new(
        schema: CedarSchemaJson,
        entity_names: EntityNames,
        build_workload: bool,
        build_user: bool,
        token_entity_mapping: HashMap<String, String>,
    ) -> Self {
        Self {
            schema,
            entity_names,
            build_workload,
            build_user,
            token_entity_mapping,
        }
    }

    pub fn build_entities(
        &self,
        tokens: &HashMap<String, Token>,
        resource: &ResourceData,
    ) -> Result<AuthorizeEntitiesData, BuildCedarlingEntityError> {
        let mut token_entities = HashMap::new();
        for (tkn_name, tkn) in tokens.iter() {
            let entity_name = if let Some(entity_name) = self.entity_names.tokens.get(tkn_name) {
                entity_name
            } else {
                continue;
            };
            let entity = self.build_tkn_entity(&entity_name, tkn)?;
            token_entities.insert(entity_name.to_string(), entity);
        }

        let workload = if self.build_workload {
            Some(self.build_workload_entity(tokens)?)
        } else {
            None
        };

        let (user, roles) = if self.build_user {
            let roles = self.try_build_role_entities(tokens)?;
            let parents = roles
                .iter()
                .map(|role| role.uid())
                .collect::<HashSet<EntityUid>>();
            (Some(self.build_user_entity(tokens, parents)?), roles)
        } else {
            (None, vec![])
        };

        let resource = self.build_resource_entity(resource)?;

        Ok(AuthorizeEntitiesData {
            workload,
            user,
            resource,
            roles,
            tokens: token_entities,
        })
    }
}

/// Builds a Cedar Entity using a JWT
fn build_entity(
    schema: &CedarSchemaJson,
    entity_name: &str,
    token: &Token,
    id_src_claim: &str,
    claim_aliases: Vec<ClaimAliasMap>,
    parents: HashSet<EntityUid>,
) -> Result<Entity, BuildEntityError> {
    // Get entity Id from the specified token claim
    let entity_id = token
        .get_claim(id_src_claim)
        .ok_or(BuildEntityError::MissingClaim(id_src_claim.to_string()))?
        .as_str()?
        .to_owned();

    // Get entity namespace and type
    let entity_name = entity_name.to_string();
    let entity_type = schema
        .get_entity_from_full_name(&entity_name)?
        .ok_or(BuildEntityError::EntityNotInSchema(entity_name.to_string()))?;

    // Build entity attributes
    let entity_attrs = build_entity_attrs_from_tkn(schema, entity_type, token, claim_aliases)?;

    // Build cedar entity
    let entity_type_name = cedar_policy::EntityTypeName::from_str(&entity_name)
        .map_err(BuildEntityError::ParseEntityTypeName)?;
    let entity_id = EntityId::from_str(&entity_id).map_err(BuildEntityError::ParseEntityId)?;
    let entity_uid = EntityUid::from_type_name_and_id(entity_type_name, entity_id);
    Ok(Entity::new(entity_uid, entity_attrs, parents)?)
}

/// Errors encountered when building a Cedarling-specific entity
#[derive(Debug, thiserror::Error)]
pub enum BuildCedarlingEntityError {
    #[error(transparent)]
    Workload(#[from] BuildWorkloadEntityError),
    #[error(transparent)]
    User(#[from] BuildUserEntityError),
    #[error(transparent)]
    Role(#[from] BuildRoleEntityError),
    #[error("failed to build resource entity: {0}")]
    Resource(#[from] BuildResourceEntityError),
    #[error(transparent)]
    Token(#[from] BuildTokenEntityError),
}

#[derive(Debug, thiserror::Error)]
pub enum BuildEntityError {
    #[error("failed to parse entity type name: {0}")]
    ParseEntityTypeName(#[from] cedar_policy::ParseErrors),
    #[error("failed to parse entity id: {0}")]
    ParseEntityId(#[source] Infallible),
    #[error("failed to evaluate entity or tag: {0}")]
    AttrEvaluation(#[from] cedar_policy::EntityAttrEvaluationError),
    #[error("failed to build entity since a token was not provided")]
    TokenUnavailable,
    #[error("the given token is missing a `{0}` claim")]
    MissingClaim(String),
    #[error(transparent)]
    TokenClaimTypeMismatch(#[from] TokenClaimTypeError),
    #[error(transparent)]
    JsonTypeError(#[from] JsonTypeError),
    #[error("the entity `{0}` is not defined in the schema")]
    EntityNotInSchema(String),
    #[error("failed build restricted expression: {0}")]
    BuildExpression(#[from] BuildExprError),
    #[error("failed to build entity attribute: {0}")]
    BuildEntityAttr(#[from] BuildAttrError),
    #[error("got {0} token, expected: {1}")]
    InvalidToken(String, String),
}

impl BuildEntityError {
    pub fn json_type_err(expected_type_name: &str, got_value: &Value) -> Self {
        Self::JsonTypeError(JsonTypeError::type_mismatch(expected_type_name, got_value))
    }
}

#[cfg(test)]
mod test {
    use super::*;
    use crate::{
        common::{cedar_schema::cedar_json::CedarSchemaJson, policy_store::TrustedIssuer},
        jwt::TokenClaims,
    };
    use cedar_policy::EvalResult;
    use serde_json::json;
    use std::collections::HashMap;

    #[test]
    fn can_build_entity_using_jwt() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
        "Jans": {
            "entityTypes": {
                "Workload": {
                    "shape": {
                        "type": "Record",
                        "attributes":  {
                            "client_id": { "type": "String" },
                            "name": { "type": "String" },
                        },
                    }
                }
            }
        }}))
        .expect("should successfully build schema");
        let iss = TrustedIssuer::default();
        let token = Token::new(
            "access_token",
            TokenClaims::new(HashMap::from([
                ("client_id".to_string(), json!("workload-123")),
                ("name".to_string(), json!("somename")),
            ])),
            Some(&iss),
        );
        let entity = build_entity(
            &schema,
            "Jans::Workload",
            &token,
            "client_id",
            Vec::new(),
            HashSet::new(),
        )
        .expect("should successfully build entity");

        assert_eq!(entity.uid().to_string(), "Jans::Workload::\"workload-123\"");
        assert_eq!(
            entity
                .attr("client_id")
                .expect("expected workload entity to have a `client_id` attribute")
                .unwrap(),
            EvalResult::String("workload-123".to_string()),
        );
        assert_eq!(
            entity
                .attr("name")
                .expect("expected workload entity to have a `name` attribute")
                .unwrap(),
            EvalResult::String("somename".to_string()),
        );
    }

    // TODO: implement this test
    // #[test]
    // fn can_build_entity_with_token_ref() {
    //     let schema = serde_json::from_value::<CedarSchemaJson>(json!({
    //     "Jans": {
    //         "entityTypes": {
    //             "Access_token": {
    //                 "jti": { "type": "String" },
    //             },
    //             "Id_token": {
    //                 "jti": { "type": "String" },
    //             },
    //             "Userinfo_token": {
    //                 "jti": { "type": "String" },
    //             },
    //             "Workload": {
    //                 "shape": {
    //                     "type": "Record",
    //                     "attributes":  {
    //                         "access_token": { "type": "Entity", "name": "Access_token" },
    //                         "id_token": { "type": "Entity", "name": "Id_token" },
    //                         "userinfo_token": { "type": "Entity", "name": "Userinfo_token" },
    //                     },
    //                 }
    //             }
    //         }
    //     }}))
    //     .expect("should successfully build schema");
    //     let iss = TrustedIssuer::default();
    //     let token = NewToken::new(
    //         "access_token",
    //         TokenClaims::new(HashMap::from([
    //             ("client_id".to_string(), json!("workload-123")),
    //             ("access_token".to_string(), json!("token-123")),
    //             ("userinfo_token".to_string(), json!("token-321")),
    //         ])),
    //         Some(&iss),
    //     );
    //     let mut built_entities = HashMap::from([
    //         ("Jans::Id_token".to_string(), "token-123".into()),
    //         ("Jans::Userinfo_token".to_string(), "token-123".into()),
    //     ]);
    //     let entity = build_entity(
    //         &schema,
    //         "Workload",
    //         &token,
    //         "client_id",
    //         Vec::new(),
    //         HashSet::new(),
    //     )
    //     .expect("should successfully build entity");
    //
    //     assert_eq!(entity.uid().to_string(), "Jans::Workload::\"workload-123\"");
    //
    //     let access_tkn_uid = EntityUid::from_str("Jans::Access_token::\"token-123\"").unwrap();
    //     let access_tkn_attr = entity
    //         .attr("access_token")
    //         .expect("expected workload entity to have an `access_token` attribute")
    //         .unwrap();
    //     assert!(
    //         matches!(access_tkn_attr, EvalResult::EntityUid(ref uid) if uid == &access_tkn_uid),
    //         "the access_token attribute should be an entity uid from the given token's claim"
    //     );
    //
    //     let id_tkn_uid = EntityUid::from_str("Jans::Id_token::\"token-123\"").unwrap();
    //     let id_tkn_attr = entity
    //         .attr("id_token")
    //         .expect("expected workload entity to have an `id_token` attribute")
    //         .unwrap();
    //     assert!(
    //         matches!(id_tkn_attr, EvalResult::EntityUid(ref uid) if uid == &id_tkn_uid),
    //         "the id_token attribute should be an entity uid from the already built token entities"
    //     );
    //
    //     // This checks if the uid from the already built entities is prioritized over the uid from
    //     // the given token's claim
    //     let userinfo_tkn_uid = EntityUid::from_str("Jans::Userinfo_token::\"token-123\"").unwrap();
    //     let userinfo_tkn_attr = entity
    //         .attr("userinfo_token")
    //         .expect("expected workload entity to have an `userinfo_token` attribute")
    //         .unwrap();
    //     assert!(
    //         matches!(userinfo_tkn_attr, EvalResult::EntityUid(ref uid) if uid == &userinfo_tkn_uid),
    //         "the userinfo_token attribute should be an entity uid from the already built token entities"
    //     );
    // }

    #[test]
    fn errors_on_invalid_entity_type_name() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": { "entityTypes": { "Work:load": {
                "shape": {
                    "type": "Record",
                    "attributes":  {
                        "client_id": { "type": "String" },
                        "name": { "type": "String" },
                    },
                }
            }}}
        }))
        .expect("should successfully build schema");
        let iss = TrustedIssuer::default();
        let token = Token::new(
            "access_token",
            TokenClaims::new(HashMap::from([
                ("client_id".to_string(), json!("workload-123")),
                ("name".to_string(), json!("somename")),
            ])),
            Some(&iss),
        );

        let err = build_entity(
            &schema,
            "Jans::Work:load",
            &token,
            "client_id",
            Vec::new(),
            HashSet::new(),
        )
        .expect_err("should error while parsing entity type name");

        assert!(
            matches!(err, BuildEntityError::ParseEntityTypeName(_)),
            "expected ParseEntityTypeName error but got: {:?}",
            err
        );
    }

    #[test]
    fn errors_when_token_is_missing_entity_id_claim() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({}))
            .expect("should successfully build schema");
        let iss = TrustedIssuer::default();
        let token = Token::new("access_token", TokenClaims::new(HashMap::new()), Some(&iss));

        let err = build_entity(
            &schema,
            "Workload",
            &token,
            "client_id",
            Vec::new(),
            HashSet::new(),
        )
        .expect_err("should error while parsing entity type name");

        assert!(
            matches!(
                err,
                BuildEntityError::MissingClaim(ref claim_name)
                if claim_name =="client_id"
            ),
            "expected MissingClaim error but got: {}",
            err
        );
    }

    #[test]
    fn errors_token_claim_has_unexpected_type() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": { "entityTypes": { "Workload": {
                "shape": {
                    "type": "Record",
                    "attributes":  {
                        "client_id": { "type": "String" },
                    },
                }
            }}}
        }))
        .expect("should successfully build schema");
        let iss = TrustedIssuer::default();
        let token = Token::new(
            "access_token",
            TokenClaims::new(HashMap::from([("client_id".to_string(), json!(123))])),
            Some(&iss),
        );
        let err = build_entity(
            &schema,
            "Workload",
            &token,
            "client_id",
            Vec::new(),
            HashSet::new(),
        )
        .expect_err("should error due to unexpected json type");

        assert!(
            matches!(
                err,
                BuildEntityError::TokenClaimTypeMismatch(ref err)
                if err == &TokenClaimTypeError::type_mismatch("client_id", "String", &json!(123))
            ),
            "expected TokenClaimTypeMismatch error but got: {:?}",
            err
        );
    }

    #[test]
    fn errors_when_entity_not_in_schema() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({}))
            .expect("should successfully build schema");
        let iss = TrustedIssuer::default();
        let token = Token::new(
            "access_token",
            TokenClaims::new(HashMap::from([(
                "client_id".to_string(),
                json!("client-123"),
            )])),
            Some(&iss),
        );

        let err = build_entity(
            &schema,
            "Workload",
            &token,
            "client_id",
            Vec::new(),
            HashSet::new(),
        )
        .expect_err("should error due to entity not being in the schema");
        assert!(
            matches!(
                err,
                BuildEntityError::EntityNotInSchema(ref type_name)
                if type_name == "Workload"
            ),
            "expected EntityNotInSchema error but got: {:?}",
            err
        );
    }
}
