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
mod built_entities;

use super::AuthorizeEntitiesData;
use crate::common::cedar_schema::cedar_json::CedarSchemaJson;
use crate::jwt::{Token, TokenClaimTypeError};
use crate::{AuthorizationConfig, EntityData};
use build_attrs::{BuildAttrError, ClaimAliasMap, build_entity_attrs_from_tkn};
use build_expr::*;
use build_resource_entity::{BuildCedarEntityError, JsonTypeError};
use build_role_entity::BuildRoleEntityError;
pub use build_token_entities::BuildTokenEntityError;
use build_user_entity::BuildUserEntityError;
use build_workload_entity::BuildWorkloadEntityError;
pub(crate) use built_entities::BuiltEntities;
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
}

impl EntityBuilder {
    pub fn new(
        schema: CedarSchemaJson,
        entity_names: EntityNames,
        build_workload: bool,
        build_user: bool,
    ) -> Self {
        Self {
            schema,
            entity_names,
            build_workload,
            build_user,
        }
    }

    pub fn build_token_entities(
        &self,
        tokens: &HashMap<String, Token>,
        resource: &EntityData,
    ) -> Result<AuthorizeEntitiesData, BuildCedarlingEntityError> {
        let mut built_entities = BuiltEntities::default();

        let mut token_entities = HashMap::new();
        for (tkn_name, tkn) in tokens.iter() {
            let entity_name = if let Some(entity_name) = self.entity_names.tokens.get(tkn_name) {
                entity_name
            } else {
                continue;
            };
            let entity = self.build_tkn_entity(entity_name, tkn, &built_entities)?;
            built_entities.insert(&entity);
            token_entities.insert(tkn_name.to_string(), entity);
        }

        let workload = if self.build_workload {
            let workload_entity = self.build_workload_entity(tokens, &built_entities)?;
            built_entities.insert(&workload_entity);
            Some(workload_entity)
        } else {
            None
        };

        let (user, roles) = if self.build_user {
            let roles = self.try_build_role_entities(tokens)?;
            let parents = roles
                .iter()
                .map(|role| {
                    built_entities.insert(role);
                    role.uid()
                })
                .collect::<HashSet<EntityUid>>();
            let user_entity = self.build_user_entity(tokens, parents, &built_entities)?;
            built_entities.insert(&user_entity);
            (Some(user_entity), roles)
        } else {
            (None, vec![])
        };

        let resource = self
            .build_cedar_entity(resource)
            .map_err(BuildCedarEntityError::BuildEntity)?;
        built_entities.insert(&resource);

        Ok(AuthorizeEntitiesData {
            workload,
            user,
            resource,
            roles,
            tokens: token_entities,
            build_entities: built_entities,
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
    built_entities: &BuiltEntities,
) -> Result<Entity, BuildEntityError> {
    // Get entity Id from the specified token claim
    let entity_id = token
        .get_claim(id_src_claim)
        .ok_or(BuildEntityError::MissingClaim(id_src_claim.to_string()))?
        .as_str()?
        .to_owned();

    // Get entity namespace and type
    let (entity_type_name, type_schema) = schema
        .get_entity_schema(entity_name, None)?
        .ok_or(BuildEntityError::EntityNotInSchema(entity_name.to_string()))?;
    let default_namespace = entity_type_name.namespace();

    // Build entity attributes
    let entity_attrs = build_entity_attrs_from_tkn(
        schema,
        type_schema,
        token,
        Some(default_namespace.as_str()),
        claim_aliases,
        built_entities,
    )?;

    // Build cedar entity
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
    Resource(#[from] BuildCedarEntityError),
    #[error(transparent)]
    Token(#[from] BuildTokenEntityError),
    #[error("failed to build unsigned principal entity: {0}")]
    UnverifiedPrincipal(BuildCedarEntityError),
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
    use crate::common::{cedar_schema::cedar_json::CedarSchemaJson, policy_store::TrustedIssuer};
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
            HashMap::from([
                ("client_id".to_string(), json!("workload-123")),
                ("name".to_string(), json!("somename")),
            ])
            .into(),
            Some(&iss),
        );
        let entity = build_entity(
            &schema,
            "Jans::Workload",
            &token,
            "client_id",
            Vec::new(),
            HashSet::new(),
            &BuiltEntities::default(),
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

    #[test]
    fn can_build_entity_with_token_ref() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": {
                "entityTypes": {
                    "Resource": {},
                    "Access_token": {},
                    "Id_token": {},
                    "Workload": {
                        "shape": {
                            "type": "Record",
                            "attributes":  {
                                "access_token": { "type": "Entity", "name": "Access_token" },
                                "id_token": { "type": "Entity", "name": "Id_token" },
                                "userinfo_token": { "type": "Entity", "name": "Custom::Userinfo_token" },
                                "custom_token": { "type": "Entity", "name": "Custom::Custom_token" },
                            },
                        }
                    }
                }
            },
            "Custom": {
                "entityTypes": {
                    "Custom_token": {},
                    "Userinfo_token": {},
                }
            }
        }))
        .expect("should successfully build schema");
        let iss = TrustedIssuer::default();
        let entity_builder = EntityBuilder::new(
            schema,
            EntityNames {
                tokens: HashMap::from([
                    ("access_token".to_string(), "Jans::Access_token".to_string()),
                    ("id_token".to_string(), "Jans::Id_token".to_string()),
                    (
                        "userinfo_token".to_string(),
                        "Custom::Userinfo_token".to_string(),
                    ),
                    (
                        "custom_token".to_string(),
                        "Custom::Custom_token".to_string(),
                    ),
                ]),
                ..Default::default()
            },
            true,
            false,
        );

        let tkn_names = ["access_token", "id_token", "userinfo_token", "custom_token"];

        let tokens = tkn_names
            .iter()
            .map(|tkn_name| {
                let token = Token::new(
                    tkn_name,
                    HashMap::from([
                        ("client_id".to_string(), json!(format!("{}_123", tkn_name))),
                        ("jti".to_string(), json!(format!("{}_123", tkn_name))),
                    ])
                    .into(),
                    Some(&iss),
                );
                (tkn_name.to_string(), token)
            })
            .collect::<HashMap<String, Token>>();

        let entities = entity_builder
            .build_token_entities(&tokens, &EntityData {
                resource_type: "Jans::Resource".to_string(),
                id: "res-123".to_string(),
                payload: HashMap::new(),
            })
            .expect("build entities");

        // Check if the token entities get created
        assert_eq!(
            entities
                .tokens
                .keys()
                .map(|x| x.to_string())
                .collect::<HashSet<String>>(),
            tkn_names
                .clone()
                .iter()
                .map(|x| x.to_string())
                .collect::<HashSet<String>>(),
            "should build all token entities",
        );
        let entity_names = [
            "Jans::Access_token",
            "Jans::Id_token",
            "Custom::Userinfo_token",
            "Custom::Custom_token",
        ];
        let entity_ids = tkn_names
            .iter()
            .map(|name| format!("{}_123", name))
            .collect::<Vec<String>>();
        for ((tkn_name, entity_name), entity_ids) in
            tkn_names.iter().zip(entity_names).zip(&entity_ids)
        {
            let entity = entities
                .tokens
                .get(*tkn_name)
                .expect(&format!("build {} entity", tkn_name));
            assert_eq!(
                entity.uid().type_name().to_string(),
                entity_name,
                "wrong type name for {}",
                tkn_name
            );
            assert_eq!(
                entity.uid().id().escaped(),
                entity_ids,
                "entity id for {}",
                tkn_name
            );
        }

        // Check if the tokens get added to the workload entity
        let workload_entity = entities.workload.expect("should built workload entity");
        assert_eq!(
            workload_entity.uid().type_name().to_string(),
            "Jans::Workload"
        );
        assert_eq!(workload_entity.uid().id().escaped(), "access_token_123");
        for ((tkn_name, entity_name), entity_id) in
            tkn_names.iter().zip(entity_names).zip(entity_ids)
        {
            assert_eq!(
                workload_entity.attr(tkn_name),
                Some(Ok(EvalResult::EntityUid(
                    EntityUid::from_str(&format!("{}::\"{}\"", entity_name, entity_id))
                        .expect("build entity uid")
                )))
            );
        }
    }

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
            HashMap::from([
                ("client_id".to_string(), json!("workload-123")),
                ("name".to_string(), json!("somename")),
            ])
            .into(),
            Some(&iss),
        );

        let err = build_entity(
            &schema,
            "Jans::Work:load",
            &token,
            "client_id",
            Vec::new(),
            HashSet::new(),
            &BuiltEntities::default(),
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
        let token = Token::new("access_token", HashMap::new().into(), Some(&iss));

        let err = build_entity(
            &schema,
            "Workload",
            &token,
            "client_id",
            Vec::new(),
            HashSet::new(),
            &BuiltEntities::default(),
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
            HashMap::from([("client_id".to_string(), json!(123))]).into(),
            Some(&iss),
        );
        let err = build_entity(
            &schema,
            "Workload",
            &token,
            "client_id",
            Vec::new(),
            HashSet::new(),
            &BuiltEntities::default(),
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
            HashMap::from([("client_id".to_string(), json!("client-123"))]).into(),
            Some(&iss),
        );

        let err = build_entity(
            &schema,
            "Workload",
            &token,
            "client_id",
            Vec::new(),
            HashSet::new(),
            &BuiltEntities::default(),
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
