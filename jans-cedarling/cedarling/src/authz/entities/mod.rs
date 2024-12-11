/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

//! Module for creating cedar-policy entities

mod create;
mod trait_as_expression;

#[cfg(test)]
mod test_create;

use std::collections::HashSet;

use crate::common::cedar_schema::CedarSchemaJson;

use crate::authz::token_data::{AccessTokenData, IdTokenData, UserInfoTokenData};
use crate::common::policy_store::{
    AccessTokenEntityMetadata, ClaimMappings, PolicyStore, TokenKind, TrustedIssuer,
};
use crate::jwt;
use cedar_policy::EntityUid;
pub use create::CedarPolicyCreateTypeError;
use create::EntityParsedTypeName;
use create::{build_entity_uid, create_entity, parse_namespace_and_typename, EntityMetadata};

use super::request::ResourceData;
use super::token_data::TokenPayload;

pub(crate) type ProcessTokensResult<'a> =
    jwt::ProcessTokensResult<'a, AccessTokenData, IdTokenData, UserInfoTokenData>;

/// Access token entities
pub struct AccessTokenEntities {
    pub workload_entity: cedar_policy::Entity,
    pub access_token_entity: cedar_policy::Entity,
}

impl AccessTokenEntities {
    /// Map all values to vector of cedar-policy entities
    pub fn into_iter(self) -> impl Iterator<Item = cedar_policy::Entity> {
        vec![self.workload_entity, self.access_token_entity].into_iter()
    }

    pub fn iter(&self) -> impl Iterator<Item = &cedar_policy::Entity> {
        vec![&self.workload_entity, &self.access_token_entity].into_iter()
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
    policy_store: &PolicyStore,
    data: &AccessTokenData,
    meta: &AccessTokenEntityMetadata,
) -> Result<AccessTokenEntities, AccessTokenEntitiesError> {
    let schema = &policy_store.schema.json;
    let namespace = policy_store.namespace();
    let claim_mapping = &meta.entity_metadata.claim_mapping;

    let access_entity_meta = EntityMetadata::new(
        EntityParsedTypeName {
            typename: "Access_token",
            namespace,
        },
        meta.principal_identifier.as_deref().unwrap_or("jti"),
    );

    let workload_entity_meta = EntityMetadata::new(
        EntityParsedTypeName {
            typename: "Workload",
            namespace,
        },
        "client_id",
    );

    Ok(AccessTokenEntities {
        access_token_entity: access_entity_meta.create_entity(
            schema,
            data,
            HashSet::new(),
            claim_mapping,
        )?,
        workload_entity: workload_entity_meta.create_entity(
            schema,
            data,
            HashSet::new(),
            claim_mapping,
        )?,
    })
}

/// Create id_token entity
pub fn create_id_token_entity(
    policy_store: &PolicyStore,
    data: &IdTokenData,
    claim_mapping: &ClaimMappings,
) -> Result<cedar_policy::Entity, CedarPolicyCreateTypeError> {
    let schema = &policy_store.schema.json;
    let namespace = policy_store.namespace();

    EntityMetadata::new(
        EntityParsedTypeName {
            typename: "id_token",
            namespace,
        },
        "jti",
    )
    .create_entity(schema, data, HashSet::new(), claim_mapping)
}

/// Create user entity
pub fn create_user_entity(
    policy_store: &PolicyStore,
    tokens: &ProcessTokensResult,
    parents: HashSet<EntityUid>,
    trusted_issuer: &TrustedIssuer,
) -> Result<cedar_policy::Entity, CedarPolicyCreateTypeError> {
    let user_id_mapping = trusted_issuer.get_user_id_mapping().unwrap_or_default();

    let schema: &CedarSchemaJson = &policy_store.schema.json;
    let namespace = policy_store.namespace();

    // payload and claim mapping for getting user ID
    let (payload, claim_mapping): (&TokenPayload, &ClaimMappings) = match user_id_mapping.kind {
        TokenKind::Access => (
            &tokens.access_token,
            &trusted_issuer.access_tokens.entity_metadata.claim_mapping,
        ),
        TokenKind::Id => (&tokens.id_token, &trusted_issuer.id_tokens.claim_mapping),
        TokenKind::Userinfo => (
            &tokens.userinfo_token,
            &trusted_issuer.userinfo_tokens.claim_mapping,
        ),
        TokenKind::Transaction => return Err(CedarPolicyCreateTypeError::TransactionToken),
    };

    EntityMetadata::new(
        EntityParsedTypeName {
            typename: "User",
            namespace,
        },
        user_id_mapping.mapping_field,
    )
    .create_entity(schema, payload, parents, claim_mapping)
}

/// Create `Userinfo_token` entity
pub fn create_userinfo_token_entity(
    policy_store: &PolicyStore,
    data: &UserInfoTokenData,
    claim_mapping: &ClaimMappings,
) -> Result<cedar_policy::Entity, CedarPolicyCreateTypeError> {
    let schema = &policy_store.schema.json;
    let namespace = policy_store.namespace();

    EntityMetadata::new(
        EntityParsedTypeName {
            typename: "Userinfo_token",
            namespace,
        },
        "jti",
    )
    .create_entity(schema, data, HashSet::new(), claim_mapping)
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
        error: CedarPolicyCreateTypeError,
        token_kind: TokenKind,
    },
}

/// Create `Role` entity from based on `TrustedIssuer` or default value of `RoleMapping`
pub fn create_role_entities(
    policy_store: &PolicyStore,
    tokens: &ProcessTokensResult,
    trusted_issuer: &TrustedIssuer,
) -> Result<Vec<cedar_policy::Entity>, RoleEntityError> {
    // get role mapping or default value
    let role_mapping = trusted_issuer.get_role_mapping().unwrap_or_default();

    let parsed_typename = EntityParsedTypeName::new("Role", policy_store.namespace());
    let role_entity_type = parsed_typename.full_type_name();

    // map payload from token
    let token_data: &'_ TokenPayload = match role_mapping.kind {
        TokenKind::Access => &tokens.access_token,
        TokenKind::Id => &tokens.id_token,
        TokenKind::Userinfo => &tokens.userinfo_token,
        TokenKind::Transaction => {
            return Err(RoleEntityError::Create {
                error: CedarPolicyCreateTypeError::TransactionToken,
                token_kind: TokenKind::Transaction,
            })
        },
    };

    // we don't really need mapping for `role` but if user specify custom role with
    let token_mapping = match role_mapping.kind {
        TokenKind::Access => &trusted_issuer.access_tokens.entity_metadata.claim_mapping,
        TokenKind::Id => &trusted_issuer.id_tokens.claim_mapping,
        TokenKind::Userinfo => &trusted_issuer.userinfo_tokens.claim_mapping,
        TokenKind::Transaction => {
            return Err(RoleEntityError::Create {
                error: CedarPolicyCreateTypeError::TransactionToken,
                token_kind: TokenKind::Transaction,
            })
        },
    };

    // get payload of role id in JWT token data
    let Ok(payload) = token_data.get_payload(role_mapping.mapping_field) else {
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
                        .and_then(|name| build_entity_uid(role_entity_type.as_str(), name)
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

    let schema = &policy_store.schema.json;

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
                token_mapping,
            )
            .map_err(|err| RoleEntityError::Create {
                error: err,
                token_kind: role_mapping.kind,
            })
        })
        .collect::<Result<Vec<_>, _>>()
}
