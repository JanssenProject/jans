/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

//! Module for creating cedar-policy entities

mod create;
mod trait_as_expression;
mod user;
mod workload;

#[cfg(test)]
mod test_create;

use super::request::ResourceData;
use crate::common::cedar_schema::CedarSchemaJson;
use crate::common::policy_store::{ClaimMappings, PolicyStore, TokenKind};
use crate::jwt::Token;
use cedar_policy::EntityUid;
use create::EntityParsedTypeName;
pub use create::CEDAR_POLICY_SEPARATOR;
use create::{build_entity_uid, create_entity, parse_namespace_and_typename, EntityMetadata};
use std::collections::HashSet;

pub use create::CreateCedarEntityError;
pub use user::*;
pub use workload::*;

const DEFAULT_TKN_PRINCIPAL_IDENTIFIER: &str = "jti";

pub struct DecodedTokens<'a> {
    pub access_token: Option<Token<'a>>,
    pub id_token: Option<Token<'a>>,
    pub userinfo_token: Option<Token<'a>>,
}

/// Create access_token entity
pub fn create_access_token(
    entity_mapping: Option<&str>,
    policy_store: &PolicyStore,
    token: &Token,
) -> Result<cedar_policy::Entity, CreateCedarEntityError> {
    let schema = &policy_store.schema.json;
    let namespace = policy_store.namespace();
    let claim_mapping = token.claim_mapping();

    let access_entity_meta = EntityMetadata::new(
        EntityParsedTypeName {
            typename: entity_mapping.unwrap_or("Access_token"),
            namespace,
        },
        token
            .metadata()
            .principal_identifier
            .as_deref()
            .unwrap_or(DEFAULT_TKN_PRINCIPAL_IDENTIFIER),
    );

    access_entity_meta.create_entity(schema, token, HashSet::new(), claim_mapping)
}

/// Create id_token entity
pub fn create_id_token_entity(
    entity_mapping: Option<&str>,
    policy_store: &PolicyStore,
    token: &Token,
) -> Result<cedar_policy::Entity, CreateCedarEntityError> {
    let schema = &policy_store.schema.json;
    let namespace = policy_store.namespace();
    let claim_mapping = token.claim_mapping();

    EntityMetadata::new(
        EntityParsedTypeName {
            typename: entity_mapping.unwrap_or("id_token"),
            namespace,
        },
        "jti",
    )
    .create_entity(schema, token, HashSet::new(), claim_mapping)
}

/// Create `Userinfo_token` entity
pub fn create_userinfo_token_entity(
    entity_mapping: Option<&str>,
    policy_store: &PolicyStore,
    token: &Token,
) -> Result<cedar_policy::Entity, CreateCedarEntityError> {
    let schema = &policy_store.schema.json;
    let namespace = policy_store.namespace();
    let claim_mapping = token.claim_mapping();

    EntityMetadata::new(
        EntityParsedTypeName {
            typename: entity_mapping.unwrap_or("Userinfo_token"),
            namespace,
        },
        "jti",
    )
    .create_entity(schema, token, HashSet::new(), claim_mapping)
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

/// Create `Role` entity from based on `TrustedIssuer` or default value of `RoleMapping`
pub fn create_role_entities(
    policy_store: &PolicyStore,
    tokens: &DecodedTokens,
) -> Result<Vec<cedar_policy::Entity>, RoleEntityError> {
    let mut token: Option<&Token> = None;

    if let Some(tkn) = tokens.access_token.as_ref() {
        let role_mapping = tkn.role_mapping();
        if tkn.has_claim(role_mapping) {
            token = Some(tkn);
        }
    }

    if token.is_none() {
        if let Some(tkn) = tokens.id_token.as_ref() {
            let role_mapping = tkn.role_mapping();
            if tkn.has_claim(role_mapping) {
                token = Some(tkn);
            }
        }
    }

    if token.is_none() {
        if let Some(tkn) = tokens.userinfo_token.as_ref() {
            let role_mapping = tkn.role_mapping();
            if tkn.has_claim(role_mapping) {
                token = Some(tkn);
            }
        }
    }

    // (token_kind, token_data, role_mapping, token_mapping)
    let token = match token {
        Some(token) => token,
        None => return Ok(Vec::new()),
    };

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
