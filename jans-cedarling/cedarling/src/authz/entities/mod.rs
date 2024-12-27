// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Module for creating cedar-policy entities

mod create;
mod trait_as_expression;
mod user;
mod workload;

#[cfg(test)]
mod test_create;

use std::collections::HashSet;

use cedar_policy::{Entity, EntityUid};
use create::{
    build_entity_uid, create_entity, parse_namespace_and_typename, EntityMetadata,
    EntityParsedTypeName,
};
pub use create::{CreateCedarEntityError, CEDAR_POLICY_SEPARATOR};
pub use user::*;
pub use workload::*;

use super::request::ResourceData;
use super::AuthorizeError;
use crate::common::cedar_schema::CedarSchemaJson;
use crate::common::policy_store::{ClaimMappings, PolicyStore, TokenKind};
use crate::jwt::Token;
use crate::AuthorizationConfig;

const DEFAULT_ACCESS_TKN_ENTITY_TYPE_NAME: &str = "Access_token";
const DEFAULT_ID_TKN_ENTITY_TYPE_NAME: &str = "id_token";
const DEFAULT_USERINFO_TKN_ENTITY_TYPE_NAME: &str = "Userinfo_token";
const DEFAULT_TKN_PRINCIPAL_IDENTIFIER: &str = "jti";

pub struct DecodedTokens<'a> {
    pub access_token: Option<Token<'a>>,
    pub id_token: Option<Token<'a>>,
    pub userinfo_token: Option<Token<'a>>,
}

impl DecodedTokens<'_> {
    pub fn iter(&self) -> impl Iterator<Item = &Token> {
        [
            self.access_token.as_ref(),
            self.id_token.as_ref(),
            self.userinfo_token.as_ref(),
        ]
        .into_iter()
        .flatten()
    }
}

pub struct TokenEntities {
    pub access: Option<Entity>,
    pub id: Option<Entity>,
    pub userinfo: Option<Entity>,
}

pub fn create_token_entities(
    conf: &AuthorizationConfig,
    policy_store: &PolicyStore,
    tokens: &DecodedTokens,
) -> Result<TokenEntities, AuthorizeError> {
    let schema = &policy_store.schema.json;
    let namespace = policy_store.namespace();

    // create access token entity
    let access = if let Some(token) = tokens.access_token.as_ref() {
        let type_name = conf
            .mapping_access_token
            .as_deref()
            .unwrap_or(DEFAULT_ACCESS_TKN_ENTITY_TYPE_NAME);
        Some(
            create_token_entity(token, schema, namespace, type_name)
                .map_err(AuthorizeError::CreateAccessTokenEntity)?,
        )
    } else {
        None
    };

    // create id token entity
    let id = if let Some(token) = tokens.id_token.as_ref() {
        let type_name = conf
            .mapping_id_token
            .as_deref()
            .unwrap_or(DEFAULT_ID_TKN_ENTITY_TYPE_NAME);
        Some(
            create_token_entity(token, schema, namespace, type_name)
                .map_err(AuthorizeError::CreateIdTokenEntity)?,
        )
    } else {
        None
    };

    // create userinfo token entity
    let userinfo = if let Some(token) = tokens.userinfo_token.as_ref() {
        let type_name = conf
            .mapping_userinfo_token
            .as_deref()
            .unwrap_or(DEFAULT_USERINFO_TKN_ENTITY_TYPE_NAME);
        Some(
            create_token_entity(token, schema, namespace, type_name)
                .map_err(AuthorizeError::CreateUserinfoTokenEntity)?,
        )
    } else {
        None
    };

    Ok(TokenEntities {
        access,
        id,
        userinfo,
    })
}

fn create_token_entity(
    token: &Token,
    schema: &CedarSchemaJson,
    namespace: &str,
    type_name: &str,
) -> Result<Entity, CreateCedarEntityError> {
    let claim_mapping = token.claim_mapping();
    let tkn_metadata = EntityMetadata::new(
        EntityParsedTypeName {
            type_name,
            namespace,
        },
        token
            .metadata()
            .principal_identifier
            .as_deref()
            .unwrap_or(DEFAULT_TKN_PRINCIPAL_IDENTIFIER),
    );
    tkn_metadata.create_entity(schema, token, HashSet::new(), claim_mapping)
}

/// Describe errors on creating resource entity
#[derive(thiserror::Error, Debug)]
pub enum ResourceEntityError {
    #[error("could not create resource entity: {0}")]
    Create(#[from] CreateCedarEntityError),
}

/// Create entity from [`ResourceData`]
pub fn create_resource_entity(
    resource: ResourceData,
    schema: &CedarSchemaJson,
) -> Result<cedar_policy::Entity, ResourceEntityError> {
    let entity_uid = resource.entity_uid().map_err(|err| {
        CreateCedarEntityError::EntityTypeName(resource.resource_type.clone(), err)
    })?;

    let (typename, namespace) = parse_namespace_and_typename(&resource.resource_type);

    Ok(create_entity(
        entity_uid,
        &EntityParsedTypeName::new(typename, namespace.as_str()),
        schema,
        &resource.payload.into(),
        HashSet::new(),
        // we no need mapping for resource because user put json structure and it should be correct
        &ClaimMappings::default(),
    )?)
}

/// Describe errors on creating role entity
#[derive(thiserror::Error, Debug)]
pub enum RoleEntityError {
    #[error("could not create Jans::Role entity from {token_kind} token: {error}")]
    Create {
        error: CreateCedarEntityError,
        token_kind: TokenKind,
    },

    /// Indicates that the creation of the Role Entity failed due to the absence of available tokens.
    #[error("Role Entity creation failed: no available token to build the entity from")]
    UnavailableToken,
}

/// Create `Role` entites from based on `TrustedIssuer` role mapping for each token or default value of `RoleMapping`
pub fn create_role_entities(
    policy_store: &PolicyStore,
    tokens: &DecodedTokens,
) -> Result<Vec<cedar_policy::Entity>, RoleEntityError> {
    let mut role_entities = Vec::new();

    for token in tokens.iter() {
        let mut entities = extract_roles_from_token(policy_store, token)?;
        role_entities.append(&mut entities);
    }

    Ok(role_entities)
}

/// Extract `Role` entites based on single `RoleMapping`
fn extract_roles_from_token(
    policy_store: &PolicyStore,
    token: &Token,
) -> Result<Vec<cedar_policy::Entity>, RoleEntityError> {
    let parsed_typename = EntityParsedTypeName::new("Role", policy_store.namespace());
    let role_entity_type = parsed_typename.full_type_name();

    // get payload of role id in JWT token data
    let Some(payload) = token.get_claim(token.role_mapping()) else {
        // if key not found we return empty vector
        return Ok(Vec::new());
    };

    // it can be 2 scenario when field is array or field is string
    let entity_uid_vec: Vec<EntityUid> = if let Ok(payload_str) = payload.as_str() {
        // case if it string
        let entity_uid =
            build_entity_uid(role_entity_type.as_str(), payload_str).map_err(|err| {
                RoleEntityError::Create {
                    error: err,
                    token_kind: token.kind,
                }
            })?;
        vec![entity_uid]
    } else {
        // case if it array of string
        match payload
            // get as array
            .as_array()
        {
            Ok(payload_vec) => {
                payload_vec
                    .iter()
                    .map(|payload_el| {
                        // get each element of array as `str`
                        payload_el.as_str().map_err(|err| RoleEntityError::Create {
                            error: err.into(),
                            token_kind: token.kind,
                        })
                        // build entity uid 
                        .and_then(|name| build_entity_uid(role_entity_type.as_str(), name)
                        .map_err(|err| RoleEntityError::Create {
                            error: err,
                            token_kind: token.kind,
                        }))
                    })
                    .collect::<Result<Vec<_>, _>>()?
            },
            Err(err) => {
                // Handle the case where the payload is neither a string nor an array
                return Err(RoleEntityError::Create {
                    error: err.into(),
                    token_kind: token.kind,
                });
            },
        }
    };

    let schema = &policy_store.schema.json;

    // create role entity for each entity uid
    entity_uid_vec
        .into_iter()
        .map(|entity_uid| {
            create_entity(
                entity_uid,
                &parsed_typename,
                schema,
                token.claims(),
                HashSet::new(),
                token.claim_mapping(),
            )
            .map_err(|err| RoleEntityError::Create {
                error: err,
                token_kind: token.kind,
            })
        })
        .collect::<Result<Vec<_>, _>>()
}
