/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

//! Module for creating cedar-policy entities

mod create;
mod meta;
mod trait_as_expression;

#[cfg(test)]
mod test_create;

use std::collections::HashSet;

use crate::common::cedar_schema::CedarSchemaJson;

use crate::authz::token_data::{AccessTokenData, IdTokenData, UserInfoTokenData};
use crate::common::policy_store::TokenKind;
use crate::jwt;
use cedar_policy::EntityUid;
pub use create::CedarPolicyCreateTypeError;
use create::{build_entity_uid, create_entity, parse_namespace_and_typename};

use super::request::ResourceData;
use super::token_data::TokenPayload;

pub(crate) type DecodeTokensResult<'a> =
    jwt::DecodeTokensResult<'a, AccessTokenData, IdTokenData, UserInfoTokenData>;

/// Access token entities
pub(crate) struct AccessTokenEntities {
    pub workload_entity: cedar_policy::Entity,
    pub access_token_entity: cedar_policy::Entity,
}

impl AccessTokenEntities {
    /// Map all values to vector of cedar-policy entities
    pub fn into_iter(self) -> impl Iterator<Item = cedar_policy::Entity> {
        vec![self.workload_entity, self.access_token_entity].into_iter()
    }
}

/// Describe errors on creating entites for AccessToken
#[derive(thiserror::Error, Debug)]
pub enum AccessTokenEntitiesError {
    #[error("could not create entity from access_token: {0}")]
    Create(#[from] CedarPolicyCreateTypeError),
}

/// Create all entities from AccessToken
pub fn create_access_token_entities(
    schema: &CedarSchemaJson,
    data: &AccessTokenData,
) -> Result<AccessTokenEntities, AccessTokenEntitiesError> {
    Ok(AccessTokenEntities {
        access_token_entity: meta::AccessTokenMeta.create_entity(schema, data, HashSet::new())?,
        workload_entity: meta::WorkloadEntityMeta.create_entity(schema, data, HashSet::new())?,
    })
}

/// Create id_token entity
pub fn create_id_token_entity(
    schema: &CedarSchemaJson,
    data: &IdTokenData,
) -> Result<cedar_policy::Entity, CedarPolicyCreateTypeError> {
    meta::IdToken.create_entity(schema, data, HashSet::new())
}

/// Create user entity
pub fn create_user_entity(
    schema: &CedarSchemaJson,
    id_token_data: &IdTokenData,
    userinfo_token_data: &UserInfoTokenData,
    parents: HashSet<EntityUid>,
) -> Result<cedar_policy::Entity, CedarPolicyCreateTypeError> {
    const SUB_KEY: &str = "sub";

    // if 'sub' is not the same we discard the userinfo token
    let payload =
        if id_token_data.get_json_value(SUB_KEY) == userinfo_token_data.get_json_value(SUB_KEY) {
            &id_token_data.merge(userinfo_token_data)
        } else {
            id_token_data
        };

    meta::User.create_entity(schema, payload, parents)
}

/// Describe errors on creating resource entity
#[derive(thiserror::Error, Debug)]
pub enum ResourceEntityError {
    #[error("could not create resource entity: {0}")]
    Create(#[from] CedarPolicyCreateTypeError),
}

/// Create entity from [`ResourceData`]
pub fn create_resource_entity(
    resource: ResourceData,
    schema: &CedarSchemaJson,
) -> Result<cedar_policy::Entity, ResourceEntityError> {
    let entity_uid = resource.entity_uid().map_err(|err| {
        CedarPolicyCreateTypeError::EntityTypeName(resource.resource_type.clone(), err)
    })?;

    let parsed_typename = parse_namespace_and_typename(&resource.resource_type);

    Ok(create_entity(
        entity_uid,
        &parsed_typename,
        schema,
        &resource.payload.into(),
        HashSet::new(),
    )?)
}

/// Describe errors on creating role entity
#[derive(thiserror::Error, Debug)]
pub enum RoleEntityError {
    #[error("could not create Jans::Role entity from {token_kind} token: {error}")]
    Create {
        error: CedarPolicyCreateTypeError,
        token_kind: TokenKind,
    },
}

/// Create `Role` entity from based on `TrustedIssuer` or default value of `RoleMapping`
pub fn create_role_entities(
    schema: &CedarSchemaJson,
    tokens: &DecodeTokensResult,
) -> Result<Vec<cedar_policy::Entity>, RoleEntityError> {
    // get role mapping or default value
    let role_mapping = tokens
        .trusted_issuer
        .map(|trusted_issuer| trusted_issuer.get_role_mapping().unwrap_or_default())
        .unwrap_or_default();

    let role_entity_type: &str = "Jans::Role";
    let parsed_typename = parse_namespace_and_typename(role_entity_type);

    // map payload from token
    let token_data: &'_ TokenPayload = match role_mapping.kind {
        TokenKind::Access => &tokens.access_token,
        TokenKind::Id => &tokens.id_token,
        TokenKind::Userinfo => &tokens.userinfo_token,
        TokenKind::Transaction => unimplemented!(),
    };

    // get payload of role id in JWT token data
    let Ok(payload) = token_data.get_payload(role_mapping.role_mapping_field) else {
        // if key not found we return empty vector
        return Ok(Vec::new());
    };

    // it can be 2 scenario when field is array or field is string
    let entity_uid_vec: Vec<EntityUid> = if let Ok(payload_str) = payload.as_str() {
        // case if it string
        let entity_uid = build_entity_uid(role_entity_type, payload_str).map_err(|err| {
            RoleEntityError::Create {
                error: err,
                token_kind: role_mapping.kind,
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
                            token_kind: role_mapping.kind,
                        })
                        // build entity uid 
                        .and_then(|name| build_entity_uid(role_entity_type, name)
                        .map_err(|err| RoleEntityError::Create {
                            error: err,
                            token_kind: role_mapping.kind,
                        }))
                    })
                    .collect::<Result<Vec<_>, _>>()?
            },
            Err(err) => {
                // Handle the case where the payload is neither a string nor an array
                return Err(RoleEntityError::Create {
                    error: err.into(),
                    token_kind: role_mapping.kind,
                });
            },
        }
    };

    // create role entity for each entity uid
    entity_uid_vec
        .into_iter()
        .map(|entity_uid| {
            create_entity(
                entity_uid,
                &parsed_typename,
                schema,
                token_data,
                HashSet::new(),
            )
            .map_err(|err| RoleEntityError::Create {
                error: err,
                token_kind: role_mapping.kind,
            })
        })
        .collect::<Result<Vec<_>, _>>()
}
