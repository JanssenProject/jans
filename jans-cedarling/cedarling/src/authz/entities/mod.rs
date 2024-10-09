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

use crate::{
    models::{cedar_schema::CedarSchemaJson, token_data::TokenPayload},
    ResourceData,
};
use create::{create_entity, parse_namespace_and_typename, CedarPolicyCreateTypeError};

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
    pub fn entities(self) -> Vec<cedar_policy::Entity> {
        vec![self.workload_entity, self.access_token_entity]
    }
}

/// Create all entities from AccessToken
pub fn create_access_token_entities(
    schema: &CedarSchemaJson,
    data: &TokenPayload,
) -> Result<AccessTokenEntities, AccessTokenEntitiesError> {
    Ok(AccessTokenEntities {
        access_token_entity: meta::AccessTokenMeta.create_entity(schema, data)?,
        workload_entity: meta::WorkloadEntityMeta.create_entity(schema, data)?,
    })
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
