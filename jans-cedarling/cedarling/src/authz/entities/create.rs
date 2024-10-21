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

use crate::authz::token_data::{GetTokenClaimValue, TokenPayload};
use crate::common::cedar_schema::{
    cedar_json::{CedarSchemaRecord, PrimitiveTypeKind},
    CedarSchemaJson,
};
use cedar_policy::{EntityId, EntityTypeName, EntityUid, RestrictedExpression};

const CEDAR_POLICY_SEPARATOR: &str = "::";

/// Meta information about an entity type.
/// Is used to store in `static` variable.
pub(crate) struct EntityMetadata<'a> {
    pub entity_type: &'a str,
    pub entity_id_data_key: &'a str,
}

impl<'a> EntityMetadata<'a> {
    /// create new instance of EntityMetadata.
    pub fn new(entity_type: &'a str, entity_id_data_key: &'a str) -> Self {
        Self {
            entity_type,
            entity_id_data_key,
        }
    }

    /// Create entity from token data.
    //
    // we also can create entity using the ['create_entity'] function.
    pub fn create_entity(
        &'a self,
        schema: &'a CedarSchemaJson,
        data: &'a TokenPayload,
    ) -> Result<cedar_policy::Entity, CedarPolicyCreateTypeError> {
        let entity_uid = EntityUid::from_type_name_and_id(
            EntityTypeName::from_str(self.entity_type).map_err(|err| {
                CedarPolicyCreateTypeError::EntityTypeName(self.entity_type.to_string(), err)
            })?,
            EntityId::new(data.get_value::<String>(self.entity_id_data_key)?),
        );

        let parsed_typename = parse_namespace_and_typename(self.entity_type);
        // fetch the schema record from the json-schema.
        let entity_schema_record = fetch_schema_record(
            &parsed_typename.namespace(),
            parsed_typename.typename,
            schema,
        )?;

        create_entity(
            entity_uid,
            &parsed_typename.namespace(),
            entity_schema_record,
            data,
        )
    }
}

/// Parsed result of entity type name and namespace.
/// Analog to the internal cedar_policy type `InternalName`
pub(crate) struct EntityParsedTypeName<'a> {
    pub typename: &'a str,
    path: Vec<&'a str>,
}
impl<'a> EntityParsedTypeName<'a> {
    pub fn namespace(&self) -> String {
        self.path.join(CEDAR_POLICY_SEPARATOR)
    }
}

/// Parse entity type name and namespace from entity type string.
pub fn parse_namespace_and_typename(entity_type: &str) -> EntityParsedTypeName {
    let mut raw_path: Vec<&str> = entity_type.split(CEDAR_POLICY_SEPARATOR).collect();
    let typename = raw_path.pop().unwrap_or_default();
    EntityParsedTypeName {
        typename,
        path: raw_path,
    }
}

/// fetch the schema record for a given entity type from the cedar schema json
fn fetch_schema_record<'a>(
    entity_namespace: &str,
    entity_typename: &str,
    schema: &'a CedarSchemaJson,
) -> Result<&'a CedarSchemaRecord, CedarPolicyCreateTypeError> {
    let entity_record = schema
        .entity_schema_record(entity_namespace, entity_typename)
        .ok_or(CedarPolicyCreateTypeError::CouldNotFindEntity(
            entity_typename.to_string(),
        ))?;

    // just to check if the entity is a record to be sure
    if !entity_record.is_record() {
        return Err(CedarPolicyCreateTypeError::NotRecord(
            entity_typename.to_string(),
        ));
    };
    Ok(entity_record)
}

/// get mapping of the entity attributes
fn entity_meta_attributes(
    schema_record: &CedarSchemaRecord,
) -> impl Iterator<Item = EntityAttributeMetadata> {
    schema_record
        .attributes
        .iter()
        .filter_map(|(attribute_name, attribute)| {
            attribute
                .get_type()
                .map(|attr_type| EntityAttributeMetadata {
                    attribute_name: attribute_name.as_str(),
                    cedar_policy_type: attr_type,
                    is_required: attribute.is_required(),
                })
        })
}

/// Create entity from token payload data.
pub fn create_entity<'a>(
    entity_uid: EntityUid,
    entity_namespace: &str,
    schema_record: &'a CedarSchemaRecord,
    data: &'a TokenPayload,
) -> Result<cedar_policy::Entity, CedarPolicyCreateTypeError> {
    let attr_vec = entity_meta_attributes(schema_record)
        .filter_map(|attr: EntityAttributeMetadata<'a>| {
            let attr_name = attr.attribute_name;
            let cedar_exp_result = token_attribute_to_cedar_exp(&attr, data, entity_namespace);
            match (cedar_exp_result, attr.is_required) {
                (Ok(cedar_exp), _) => Some(Ok((attr_name.to_string(), cedar_exp))),
                (
                    Err(CedarPolicyCreateTypeError::GetTokenClaimValue(
                        GetTokenClaimValue::KeyNotFound(_),
                    )),
                    false,
                    // when the attribute is not required and not found in token data we skip it
                ) => None,
                (Err(err), _) => Some(Err(err)),
            }
        })
        .collect::<Result<Vec<(String, RestrictedExpression)>, CedarPolicyCreateTypeError>>()?;

    let attrs: HashMap<String, RestrictedExpression> = HashMap::from_iter(attr_vec);

    cedar_policy::Entity::new(entity_uid.clone(), attrs, HashSet::new())
        .map_err(|err| CedarPolicyCreateTypeError::CreateEntity(entity_uid.to_string(), err))
}

/// Meta information about an attribute for cedar policy.
pub struct EntityAttributeMetadata<'a> {
    // The name of the attribute in the cedar policy
    // mapped one-to-one with the attribute in the token data.
    pub attribute_name: &'a str,
    // The type of the cedar policy attribute.
    pub cedar_policy_type: PrimitiveTypeKind,
    // if this attribute is required
    pub is_required: bool,
}

/// Get the cedar policy expression value for a given type.
fn token_attribute_to_cedar_exp(
    attribute_metadata: &EntityAttributeMetadata,
    claim: &TokenPayload,
    entity_namespace: &str,
) -> Result<RestrictedExpression, CedarPolicyCreateTypeError> {
    let token_claim_key = attribute_metadata.attribute_name;
    let exp = match &attribute_metadata.cedar_policy_type {
        PrimitiveTypeKind::String => claim.get_expression::<String>(token_claim_key)?,
        PrimitiveTypeKind::Long => claim.get_expression::<i64>(token_claim_key)?,
        PrimitiveTypeKind::Boolean => claim.get_expression::<bool>(token_claim_key)?,
        PrimitiveTypeKind::TypeName(entity_type_name) => {
            // We need concat typename of entity attribute with the namespace of entity
            let entity_type = if !entity_namespace.is_empty() {
                format!("{entity_namespace}{CEDAR_POLICY_SEPARATOR}{entity_type_name}")
            } else {
                entity_type_name.to_string()
            };
            let uid = EntityUid::from_type_name_and_id(
                EntityTypeName::from_str(entity_type.as_str()).map_err(|err| {
                    CedarPolicyCreateTypeError::EntityTypeName(entity_type.to_string(), err)
                })?,
                EntityId::new(claim.get_value::<String>(token_claim_key)?),
            );
            RestrictedExpression::new_entity_uid(uid)
        },
    };
    Ok(exp)
}

/// Describe errors on creating entity
#[derive(thiserror::Error, Debug)]
pub enum CedarPolicyCreateTypeError {
    #[error("could not parse entity type name: {0}, error: {1}")]
    EntityTypeName(String, cedar_policy::ParseErrors),

    #[error("could find entity type: {0} in the schema")]
    CouldNotFindEntity(String),
    #[error("type: {0} in the schema is not record")]
    NotRecord(String),

    #[error("could create entity with uid: {0}, error: {1}")]
    CreateEntity(String, cedar_policy::EntityAttrEvaluationError),

    #[error("could not get attribute value from token data error: {0}")]
    GetTokenClaimValue(#[from] GetTokenClaimValue),
}
