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

use std::collections::HashSet;

use crate::models::token_data::TokenClaim;
use create::CedarPolicyCreateTypeError;

/// Describe errors on creating entites for AccessToken
#[derive(thiserror::Error, Debug)]
pub enum AccessTokenEntitiesError {
    #[error("could not create entity from access_token: {0}")]
    Create(#[from] CedarPolicyCreateTypeError),
}

/// Create all entities from AccessToken
pub fn create_access_token_entities(
    data: &TokenClaim,
) -> Result<Vec<cedar_policy::Entity>, AccessTokenEntitiesError> {
    let mut entities = Vec::new();
    entities.push(meta::WorkloadEntityMeta.create_entity(data, HashSet::new())?);
    Ok(entities)
}
