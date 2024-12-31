// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

mod build_attrs;
mod build_expr;
mod build_workload_entity;
mod mapping;

use crate::common::cedar_schema::new_cedar_json::CedarSchemaJson;
use crate::common::policy_store::{TokenKind, TrustedIssuer};
use crate::jwt::{Token, TokenClaimTypeError};
use crate::AuthorizationConfig;
use build_attrs::BuildAttrError;
use build_expr::*;
use cedar_policy::{Entity, EntityId, EntityTypeName, EntityUid, RestrictedExpression};
use std::collections::{HashMap, HashSet};
use std::fmt;
use std::str::FromStr;

const CEDAR_NAMESPACE_SEPARATOR: &str = "::";
const DEFAULT_WORKLOAD_ENTITY_NAME: &str = "Workload";
const DEFAULT_USER_ENTITY_NAME: &str = "User";
const DEFAULT_ACCESS_TKN_ENTITY_NAME: &str = "Access_token";
const DEFAULT_ID_TKN_ENTITY_NAME: &str = "id_token";
const DEFAULT_USERINFO_TKN_ENTITY_NAME: &str = "Userinfo_token";
const DEFAULT_ACCESS_TKN_WORKLOAD_CLAIM: &str = "client_id";
const DEFAULT_ID_TKN_WORKLOAD_CLAIM: &str = "aud";

pub struct DecodedTokens<'a> {
    pub access_token: Option<Token<'a>>,
    pub id_token: Option<Token<'a>>,
    pub userinfo_token: Option<Token<'a>>,
}

/// The names of the entities in the schema
pub struct EntityNames {
    user: String,
    workload: String,
    id_token: String,
    access_token: String,
    userinfo_token: String,
}

impl From<&AuthorizationConfig> for EntityNames {
    fn from(config: &AuthorizationConfig) -> Self {
        Self {
            user: config
                .mapping_user
                .clone()
                .unwrap_or(DEFAULT_USER_ENTITY_NAME.to_string()),
            workload: config
                .mapping_workload
                .clone()
                .unwrap_or(DEFAULT_WORKLOAD_ENTITY_NAME.to_string()),
            id_token: config
                .mapping_id_token
                .clone()
                .unwrap_or(DEFAULT_ID_TKN_ENTITY_NAME.to_string()),
            access_token: config
                .mapping_access_token
                .clone()
                .unwrap_or(DEFAULT_ACCESS_TKN_ENTITY_NAME.to_string()),
            userinfo_token: config
                .mapping_userinfo_token
                .clone()
                .unwrap_or(DEFAULT_USERINFO_TKN_ENTITY_NAME.to_string()),
        }
    }
}

impl Default for EntityNames {
    fn default() -> Self {
        Self {
            user: DEFAULT_USERINFO_TKN_ENTITY_NAME.to_string(),
            workload: DEFAULT_WORKLOAD_ENTITY_NAME.to_string(),
            id_token: DEFAULT_ID_TKN_ENTITY_NAME.to_string(),
            access_token: DEFAULT_ACCESS_TKN_ENTITY_NAME.to_string(),
            userinfo_token: DEFAULT_USERINFO_TKN_ENTITY_NAME.to_string(),
        }
    }
}

pub struct EntityBuilder {
    schema: CedarSchemaJson,
    issuers: HashMap<String, TrustedIssuer>,
    entity_names: EntityNames,
}

impl EntityBuilder {
    pub fn new(
        issuers: HashMap<String, TrustedIssuer>,
        schema: CedarSchemaJson,
        entity_names: EntityNames,
    ) -> Self {
        Self {
            schema,
            issuers,
            entity_names,
        }
    }
}

#[derive(Debug, thiserror::Error)]
pub enum BuildEntityError {
    #[error("failed to parse entity type name: {0}")]
    ParseEntityTypeName(#[source] cedar_policy::ParseErrors),
    #[error("failed to parse entity id: {0}")]
    ParseEntityId(#[source] cedar_policy::ParseErrors),
    #[error("failed to evaluate entity or tag: {0}")]
    AttrEvaluation(#[from] cedar_policy::EntityAttrEvaluationError),
    #[error("failed to build {0} entity since the following tokens were not provided: {1:?}")]
    TokenUnavailable(String, Vec<TokenKind>),
    #[error("the given token is missing a `{0}` claim")]
    MissingClaim(String),
    #[error(transparent)]
    TokenClaimTypeMismatch(#[from] TokenClaimTypeError),
    #[error("the entity `{0}` is not defined in the schema")]
    EntityNotInSchema(String),
    #[error(transparent)]
    BuildExpression(#[from] BuildExprError),
    #[error(transparent)]
    BuildEntityAttr(#[from] BuildAttrError),
}

fn build_entity(
    name: &str,
    id: &str,
    attrs: HashMap<String, RestrictedExpression>,
    parents: HashSet<EntityUid>,
) -> Result<Entity, BuildEntityError> {
    let name = EntityTypeName::from_str(name).map_err(BuildEntityError::ParseEntityTypeName)?;
    let id = EntityId::from_str(id).unwrap(); // this is safe to unwrap since it returns infallible
    let uid = EntityUid::from_type_name_and_id(name, id);
    Ok(Entity::new(uid, attrs, parents)?)
}
