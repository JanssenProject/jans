/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

//! Module for creating cedar-policy entities

mod create;
mod meta;
#[cfg(test)]
mod test_create;

use crate::common::cedar_schema::CedarSchemaJson;

use crate::authz::token_data::{AccessTokenData, IdTokenData, UserInfoTokenData};
pub use create::CedarPolicyCreateTypeError;
use create::{create_entity, parse_namespace_and_typename};

use super::request::ResourceData;

/// Describe errors on creating entites for AccessToken
#[derive(thiserror::Error, Debug)]
pub enum AccessTokenEntitiesError {
    #[error("could not create entity from access_token: {0}")]
    Create(#[from] CedarPolicyCreateTypeError),
}

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

/// Create all entities from AccessToken
pub fn create_access_token_entities(
    schema: &CedarSchemaJson,
    data: &AccessTokenData,
) -> Result<AccessTokenEntities, AccessTokenEntitiesError> {
    Ok(AccessTokenEntities {
        access_token_entity: meta::AccessTokenMeta.create_entity(schema, &data.0)?,
        workload_entity: meta::WorkloadEntityMeta.create_entity(schema, &data.0)?,
    })
}

/// Create id_token entity
pub fn id_token_entity(
    schema: &CedarSchemaJson,
    data: &IdTokenData,
) -> Result<cedar_policy::Entity, CedarPolicyCreateTypeError> {
    meta::IdToken.create_entity(schema, &data.0)
}

/// Create user entity
pub fn user_entity(
    schema: &CedarSchemaJson,
    id_token_data: &IdTokenData,
    userinfo_token_data: &UserInfoTokenData,
) -> Result<cedar_policy::Entity, CedarPolicyCreateTypeError> {
    const SUB_KEY: &str = "sub";

    // if 'sub' is not the same we discard the userinfo token
    let data = if id_token_data.0.payload.get(SUB_KEY) == userinfo_token_data.0.payload.get(SUB_KEY)
    {
        &id_token_data.0.extend(userinfo_token_data.0.clone())
    } else {
        &id_token_data.0
    };

    meta::User.create_entity(schema, data)
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
    // fetch the schema record from the json-schema.
    let schema_record = schema
        .entity_schema_record(&parsed_typename.namespace(), parsed_typename.typename)
        .ok_or(CedarPolicyCreateTypeError::CouldNotFindEntity(
            entity_uid.to_string(),
        ))?;

    Ok(create_entity(
        entity_uid,
        &parsed_typename.namespace(),
        schema_record,
        &resource.payload.into(),
    )?)
}
