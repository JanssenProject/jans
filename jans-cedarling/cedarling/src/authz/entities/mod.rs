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

use crate::common::cedar_schema::CedarSchemaJson;
use crate::common::policy_store::{
    AccessTokenEntityMetadata, ClaimMappings, PolicyStore, TokenKind,
};
use crate::jwt::Token;
use cedar_policy::EntityUid;
pub use create::CedarPolicyCreateTypeError;
use create::EntityParsedTypeName;
use create::{build_entity_uid, create_entity, parse_namespace_and_typename, EntityMetadata};
use std::collections::HashSet;

use super::request::ResourceData;

pub struct DecodedTokens<'a> {
    pub access_token: Option<Token<'a>>,
    pub id_token: Option<Token<'a>>,
    pub userinfo_token: Option<Token<'a>>,
}

/// Create workload entity
pub fn create_workload(
    entity_mapping: Option<&str>,
    policy_store: &PolicyStore,
    tokens: &DecodedTokens,
) -> Result<cedar_policy::Entity, CedarPolicyCreateTypeError> {
    let namespace = policy_store.namespace();

    // try to build the Workload entity using the access_token
    // or id_token whichever is available.
    let (token, workload_entity_meta, claim_mapping) =
        if let Some(access_tkn) = tokens.access_token.as_ref() {
            let meta = access_tkn.iss.unwrap_or_default().tokens_metadata();
            let claim_mapping = &meta.access_tokens.entity_metadata.claim_mapping;
            let workload_entity_meta = EntityMetadata::new(
                EntityParsedTypeName {
                    typename: entity_mapping.unwrap_or("Workload"),
                    namespace,
                },
                "client_id",
            );
            (access_tkn, workload_entity_meta, claim_mapping)
        } else if let Some(id_tkn) = tokens.id_token.as_ref() {
            let meta = id_tkn.iss.unwrap_or_default().tokens_metadata();
            let claim_mapping = &meta.id_tokens.claim_mapping;
            let workload_entity_meta = EntityMetadata::new(
                EntityParsedTypeName {
                    typename: entity_mapping.unwrap_or("Workload"),
                    namespace,
                },
                "aud",
            );
            (id_tkn, workload_entity_meta, claim_mapping)
        } else {
            Err(CedarPolicyCreateTypeError::UnavailableToken(
                "Workload".to_string(),
            ))?
        };

    let schema = &policy_store.schema.json;
    workload_entity_meta.create_entity(schema, token, HashSet::new(), claim_mapping)
}

/// Create access_token entity
pub fn create_access_token(
    entity_mapping: Option<&str>,
    policy_store: &PolicyStore,
    data: &Token,
    meta: &AccessTokenEntityMetadata,
) -> Result<cedar_policy::Entity, CedarPolicyCreateTypeError> {
    let schema = &policy_store.schema.json;
    let namespace = policy_store.namespace();
    let claim_mapping = &meta.entity_metadata.claim_mapping;

    let access_entity_meta = EntityMetadata::new(
        EntityParsedTypeName {
            typename: entity_mapping.unwrap_or("Access_token"),
            namespace,
        },
        meta.principal_identifier.as_deref().unwrap_or("jti"),
    );

    access_entity_meta.create_entity(schema, data, HashSet::new(), claim_mapping)
}

/// Create id_token entity
pub fn create_id_token_entity(
    entity_mapping: Option<&str>,
    policy_store: &PolicyStore,
    data: &Token,
    claim_mapping: &ClaimMappings,
) -> Result<cedar_policy::Entity, CedarPolicyCreateTypeError> {
    let schema = &policy_store.schema.json;
    let namespace = policy_store.namespace();

    EntityMetadata::new(
        EntityParsedTypeName {
            typename: entity_mapping.unwrap_or("id_token"),
            namespace,
        },
        "jti",
    )
    .create_entity(schema, data, HashSet::new(), claim_mapping)
}

/// Create user entity
pub fn create_user_entity(
    entity_mapping: Option<&str>,
    policy_store: &PolicyStore,
    tokens: &DecodedTokens,
    parents: HashSet<EntityUid>,
) -> Result<cedar_policy::Entity, CedarPolicyCreateTypeError> {
    let schema: &CedarSchemaJson = &policy_store.schema.json;
    let namespace = policy_store.namespace();

    // We attempt to create the User entity using the first available token that also
    // has an assigned claim to get the User Entity's id from.

    let mut data: Option<(&Token, &String, &ClaimMappings)> = None;

    if let Some(access_tkn) = tokens.access_token.as_ref() {
        let iss = access_tkn.iss.unwrap_or_default();
        if let Some(user_mapping) = iss.get_user_mapping(TokenKind::Access) {
            data = Some((
                access_tkn,
                user_mapping,
                &iss.access_tokens.entity_metadata.claim_mapping,
            ));
        }
    }

    if data.is_none() {
        if let Some(id_tkn) = tokens.id_token.as_ref() {
            let iss = id_tkn.iss.unwrap_or_default();
            if let Some(user_mapping) = iss.get_user_mapping(TokenKind::Id) {
                data = Some((
                    id_tkn,
                    user_mapping,
                    &iss.access_tokens.entity_metadata.claim_mapping,
                ));
            }
        }
    }

    if data.is_none() {
        if let Some(id_tkn) = tokens.id_token.as_ref() {
            let iss = id_tkn.iss.unwrap_or_default();
            if let Some(user_mapping) = iss.get_user_mapping(TokenKind::Userinfo) {
                data = Some((id_tkn, user_mapping, &iss.id_tokens.claim_mapping));
            }
        }
    }

    if data.is_none() {
        if let Some(userinfo_tkn) = tokens.userinfo_token.as_ref() {
            let iss = userinfo_tkn.iss.unwrap_or_default();
            if let Some(user_mapping) = iss.get_user_mapping(TokenKind::Userinfo) {
                data = Some((
                    userinfo_tkn,
                    user_mapping,
                    &iss.userinfo_tokens.claim_mapping,
                ));
            }
        }
    }

    let (token, user_mapping, claim_mapping) = data.ok_or(
        CedarPolicyCreateTypeError::UnavailableToken("User".to_string()),
    )?;

    EntityMetadata::new(
        EntityParsedTypeName {
            typename: entity_mapping.unwrap_or("User"),
            namespace,
        },
        user_mapping,
    )
    .create_entity(schema, token, parents, claim_mapping)
}

/// Create `Userinfo_token` entity
pub fn create_userinfo_token_entity(
    entity_mapping: Option<&str>,
    policy_store: &PolicyStore,
    data: &Token,
    claim_mapping: &ClaimMappings,
) -> Result<cedar_policy::Entity, CedarPolicyCreateTypeError> {
    let schema = &policy_store.schema.json;
    let namespace = policy_store.namespace();

    EntityMetadata::new(
        EntityParsedTypeName {
            typename: entity_mapping.unwrap_or("Userinfo_token"),
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

    /// Indicates that the creation of the Role Entity failed due to the absence of available tokens.
    #[error("Role Entity creation failed: no available token to build the entity from")]
    UnavailableToken,
}

/// Create `Role` entity from based on `TrustedIssuer` or default value of `RoleMapping`
pub fn create_role_entities(
    policy_store: &PolicyStore,
    tokens: &DecodedTokens,
) -> Result<Vec<cedar_policy::Entity>, RoleEntityError> {
    let (token_data, role_mapping, token_mapping) =
        if let Some(token) = tokens.access_token.as_ref() {
            let iss = token.iss.unwrap_or_default();
            let role_mapping = iss.get_role_mapping().unwrap_or_default();
            let token_mapping = &iss.access_tokens.entity_metadata.claim_mapping;
            (token, role_mapping, token_mapping)
        } else if let Some(token) = tokens.id_token.as_ref() {
            let iss = token.iss.unwrap_or_default();
            let role_mapping = iss.get_role_mapping().unwrap_or_default();
            let token_mapping = &iss.id_tokens.claim_mapping;
            (token, role_mapping, token_mapping)
        } else if let Some(token) = tokens.userinfo_token.as_ref() {
            let iss = token.iss.unwrap_or_default();
            let role_mapping = iss.get_role_mapping().unwrap_or_default();
            let token_mapping = &iss.userinfo_tokens.claim_mapping;
            (token, role_mapping, token_mapping)
        } else {
            Err(RoleEntityError::UnavailableToken)?
        };

    let parsed_typename = EntityParsedTypeName::new("Role", policy_store.namespace());
    let role_entity_type = parsed_typename.full_type_name();

    // get payload of role id in JWT token data
    let Ok(payload) = token_data.get_claim(role_mapping.mapping_field) else {
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
