/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use std::{
    collections::{HashMap, HashSet},
    str::FromStr,
};

use crate::models::token_data::{GetTokenClaimValue, TokenClaim};
use cedar_policy::{EntityId, EntityTypeName, EntityUid, RestrictedExpression};

/// Meta information about an entity type.
/// Is used to store in `static` variable.
pub(crate) struct EntityMetadata<'a> {
    pub entity_type: &'a str,
    pub entity_id_data_key: &'a str,
    pub meta_attributes: Vec<EntityAttributeMetadata<'a>>,
}

impl<'a> EntityMetadata<'a> {
    pub fn new(
        entity_type: &'a str,
        entity_id_data_key: &'a str,
        meta_attributes: Vec<EntityAttributeMetadata<'a>>,
    ) -> Self {
        Self {
            entity_type,
            entity_id_data_key,
            meta_attributes,
        }
    }

    /// Create entity from token data.
    //
    // we also can create entity using the ['create_entity'] function.
    pub fn create_entity(
        &self,
        data: &TokenClaim,
        parents: HashSet<EntityUid>,
    ) -> Result<cedar_policy::Entity, CedarPolicyCreateTypeError> {
        create_entity(
            &self.entity_type,
            &self.entity_id_data_key,
            self.meta_attributes.as_slice(),
            data,
            parents,
        )
    }
}

/// Meta information about an attribute for cedar policy.
pub struct EntityAttributeMetadata<'a> {
    // The name of the attribute in the cedar policy
    pub attribute_name: &'a str,
    // The name of the token claim that contains the value.
    pub token_claims_key: &'a str,
    // The type of the cedar policy attribute.
    pub cedar_policy_type: CedarPolicyType,
}

/// Cedar policy type to store in the [`EntityAttributeMeta`]
pub(crate) enum CedarPolicyType {
    String,
    #[allow(dead_code)]
    Long,
    EntityUid {
        entity_type: &'static str,
    },
}

impl CedarPolicyType {
    // Get the cedar policy expression value for a given type.
    fn token_attribute_to_cedar_exp(
        &self,
        token_claim_key: &str,
        claim: &TokenClaim,
    ) -> Result<RestrictedExpression, CedarPolicyCreateTypeError> {
        let exp = match self {
            CedarPolicyType::String => claim.get_expression::<String>(token_claim_key)?,
            CedarPolicyType::Long => claim.get_expression::<i64>(token_claim_key)?,
            CedarPolicyType::EntityUid { entity_type } => {
                let uid = EntityUid::from_type_name_and_id(
                    EntityTypeName::from_str(entity_type).map_err(|err| {
                        CedarPolicyCreateTypeError::EntityTypeName(entity_type.to_string(), err)
                    })?,
                    EntityId::new(claim.get_value::<String>(token_claim_key)?),
                );
                RestrictedExpression::new_entity_uid(uid)
            },
        };
        Ok(exp)
    }
}

/// Create [`cedar_policy::Entity`]
pub fn create_entity(
    entity_type: &str,
    entity_id_data_key: &str,
    meta_attributes: &[EntityAttributeMetadata],
    data: &TokenClaim,
    parents: HashSet<EntityUid>,
) -> Result<cedar_policy::Entity, CedarPolicyCreateTypeError> {
    let uid = EntityUid::from_type_name_and_id(
        EntityTypeName::from_str(entity_type).map_err(|err| {
            CedarPolicyCreateTypeError::EntityTypeName(entity_type.to_string(), err)
        })?,
        EntityId::new(data.get_value::<String>(entity_id_data_key)?),
    );

    let attr_vec = meta_attributes
        .iter()
        .map(|attr| {
            let cedar_exp = attr
                .cedar_policy_type
                .token_attribute_to_cedar_exp(attr.token_claims_key, &data)?;
            Ok((attr.attribute_name.to_string(), cedar_exp))
        })
        .collect::<Result<Vec<(String, RestrictedExpression)>, CedarPolicyCreateTypeError>>()?;

    let attrs: HashMap<String, RestrictedExpression> = HashMap::from_iter(attr_vec);

    Ok(cedar_policy::Entity::new(uid, attrs, parents)
        .map_err(|err| CedarPolicyCreateTypeError::CreateEntity(entity_type.to_string(), err))?)
}

/// Describe errors on creating entity
#[derive(thiserror::Error, Debug)]
pub enum CedarPolicyCreateTypeError {
    #[error("could not parse entity type name: {0}, error: {1}")]
    EntityTypeName(String, cedar_policy::ParseErrors),
    #[error("could create entity with type name: {0}, error: {1}")]
    CreateEntity(String, cedar_policy::EntityAttrEvaluationError),

    #[error("could not get attribute value from token data error: {0}")]
    GetTokenClaimValue(#[from] GetTokenClaimValue),
}
