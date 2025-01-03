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

use crate::common::cedar_schema::new_cedar_json::CedarSchemaJson;
use crate::common::policy_store::{TokenKind, TrustedIssuer};
use crate::jwt::{Token, TokenClaimTypeError};
use crate::AuthorizationConfig;
use build_attrs::BuildAttrError;
use build_expr::*;
use build_resource_entity::JsonTypeError;
use cedar_policy::{Entity, EntityId, EntityTypeName, EntityUid};
use serde_json::Value;
use std::collections::{HashMap, HashSet};
use std::fmt;
use std::str::FromStr;

const CEDAR_NAMESPACE_SEPARATOR: &str = "::";
const DEFAULT_WORKLOAD_ENTITY_NAME: &str = "Workload";
const DEFAULT_USER_ENTITY_NAME: &str = "User";
const DEFAULT_ACCESS_TKN_ENTITY_NAME: &str = "Access_token";
const DEFAULT_ID_TKN_ENTITY_NAME: &str = "id_token";
const DEFAULT_USERINFO_TKN_ENTITY_NAME: &str = "Userinfo_token";

pub struct DecodedTokens<'a> {
    pub access: Option<Token<'a>>,
    pub id: Option<Token<'a>>,
    pub userinfo: Option<Token<'a>>,
}

impl<'a> DecodedTokens<'a> {
    /// Returns an iterator over non-None tokens.
    pub fn iter(&'a self) -> impl Iterator<Item = &'a Token<'a>> {
        [&self.access, &self.id, &self.userinfo]
            .into_iter()
            .filter_map(|token| token.as_ref())
    }
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
            user: DEFAULT_USER_ENTITY_NAME.to_string(),
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

    /// Builds a Cedar Entity using a JWT
    fn build_entity(
        &self,
        entity_name: &str,
        token: &Token,
        id_src_claim: &str,
        claim_aliases: Vec<(&str, &str)>,
        parents: HashSet<EntityUid>,
    ) -> Result<Entity, BuildEntityError> {
        // Get entity Id from the specified token claim
        let entity_id = token
            .get_claim(id_src_claim)
            .ok_or(BuildEntityError::MissingClaim(id_src_claim.to_string()))?
            .as_str()?
            .to_owned();

        // Get entity namespace and type
        let mut entity_name = entity_name.to_string();
        let (namespace, entity_type) = self
            .schema
            .get_entity_type(&entity_name)
            .ok_or(BuildEntityError::EntityNotInSchema(entity_name.to_string()))?;
        if !namespace.is_empty() {
            entity_name = [namespace.as_str(), &entity_name].join(CEDAR_NAMESPACE_SEPARATOR);
        }

        // Build entity attributes
        let entity_attrs = self.build_entity_attrs(entity_type, token, claim_aliases)?;

        // Build cedar entity
        let entity_type_name = EntityTypeName::from_str(&entity_name)
            .map_err(BuildEntityError::ParseEntityTypeName)?;
        let entity_id = EntityId::from_str(&entity_id).expect("expected infallible");
        let entity_uid = EntityUid::from_type_name_and_id(entity_type_name, entity_id);
        Ok(Entity::new(entity_uid, entity_attrs, parents)?)
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
    #[error("failed to build entity since a token was not provided")]
    TokenUnavailable,
    #[error("the given token is missing a `{0}` claim")]
    MissingClaim(String),
    #[error("the given data is missing a `{0}` entry")]
    MissingData(String),
    #[error(transparent)]
    TokenClaimTypeMismatch(#[from] TokenClaimTypeError),
    #[error(transparent)]
    JsonTypeError(#[from] JsonTypeError),
    #[error("the entity `{0}` is not defined in the schema")]
    EntityNotInSchema(String),
    #[error(transparent)]
    BuildExpression(#[from] BuildExprError),
    #[error(transparent)]
    BuildEntityAttr(#[from] BuildAttrError),
}

impl BuildEntityError {
    pub fn json_type_err(expected_type_name: &str, got_value: &Value) -> Self {
        Self::JsonTypeError(JsonTypeError::type_mismatch(expected_type_name, got_value))
    }
}
