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

use super::trait_as_expression::AsExpression;
use crate::jwt::{TokenClaim, TokenClaimTypeError, TokenData};
use crate::{
    common::{
        cedar_schema::{
            cedar_json::{
                CedarSchemaEntityShape, CedarSchemaRecord, CedarType, GetCedarTypeError,
                SchemaDefinedType,
            },
            CedarSchemaJson,
        },
        policy_store::ClaimMappings,
    },
    jwt::Token,
};
use cedar_policy::{EntityId, EntityTypeName, EntityUid, RestrictedExpression};

pub const CEDAR_POLICY_SEPARATOR: &str = "::";

/// Meta information about an entity type.
/// Is used to store in `static` variable.
pub(crate) struct EntityMetadata<'a> {
    pub entity_type: EntityParsedTypeName<'a>,
    pub entity_id_data_key: &'a str,
}

impl<'a> EntityMetadata<'a> {
    /// create new instance of EntityMetadata.
    pub fn new(entity_type: EntityParsedTypeName<'a>, entity_id_data_key: &'a str) -> Self {
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
        token: &Token,
        parents: HashSet<EntityUid>,
        claim_mapping: &ClaimMappings,
    ) -> Result<cedar_policy::Entity, CedarPolicyCreateTypeError> {
        let entity_uid = build_entity_uid(
            self.entity_type.full_type_name().as_str(),
            token
                .get_claim(self.entity_id_data_key)
                .ok_or(CedarPolicyCreateTypeError::MissingClaim(
                    self.entity_id_data_key.to_string(),
                ))?
                .as_str()?,
        )?;

        create_entity(
            entity_uid,
            &self.entity_type,
            schema,
            token.data(),
            parents,
            claim_mapping,
        )
    }
}

/// build [`EntityUid`] based on input parameters
pub(crate) fn build_entity_uid(
    entity_type: &str,
    entity_id: &str,
) -> Result<EntityUid, CedarPolicyCreateTypeError> {
    let entity_uid = EntityUid::from_type_name_and_id(
        EntityTypeName::from_str(entity_type).map_err(|err| {
            CedarPolicyCreateTypeError::EntityTypeName(entity_type.to_string(), err)
        })?,
        EntityId::new(entity_id),
    );

    Ok(entity_uid)
}

/// Parsed result of entity type name and namespace.
/// Analog to the internal cedar_policy type `InternalName`
pub(crate) struct EntityParsedTypeName<'a> {
    pub typename: &'a str,
    pub namespace: &'a str,
}
impl<'a> EntityParsedTypeName<'a> {
    pub fn new(typename: &'a str, namespace: &'a str) -> Self {
        EntityParsedTypeName {
            typename,
            namespace,
        }
    }

    pub fn full_type_name(&self) -> String {
        if self.namespace.is_empty() {
            self.typename.to_string()
        } else {
            [self.namespace, self.typename].join(CEDAR_POLICY_SEPARATOR)
        }
    }
}

/// Parse entity type name and namespace from entity type string.
/// return (typename, namespace)
pub fn parse_namespace_and_typename(raw_entity_type: &str) -> (&str, String) {
    let mut raw_path: Vec<&str> = raw_entity_type.split(CEDAR_POLICY_SEPARATOR).collect();
    let typename = raw_path.pop().unwrap_or_default();
    let namespace = raw_path.join(CEDAR_POLICY_SEPARATOR);
    (typename, namespace)
}

/// fetch the schema record for a given entity type from the cedar schema json
fn fetch_schema_record<'a>(
    entity_info: &EntityParsedTypeName,
    schema: &'a CedarSchemaJson,
) -> Result<&'a CedarSchemaEntityShape, CedarPolicyCreateTypeError> {
    let entity_shape = schema
        .entity_schema(entity_info.namespace, entity_info.typename)
        .ok_or(CedarPolicyCreateTypeError::CouldNotFindEntity(
            entity_info.typename.to_string(),
        ))?;

    // just to check if the entity is a record to be sure
    // if shape not empty
    if let Some(entity_record) = &entity_shape.shape {
        if !entity_record.is_record() {
            return Err(CedarPolicyCreateTypeError::NotRecord(
                entity_info.typename.to_string(),
            ));
        };
    }

    Ok(entity_shape)
}

/// get mapping of the entity attributes
fn entity_meta_attributes(
    schema_record: &CedarSchemaRecord,
) -> Result<Vec<EntityAttributeMetadata>, GetCedarTypeError> {
    schema_record
        .attributes
        .iter()
        .map(|(attribute_name, attribute)| {
            attribute
                .get_type()
                .map(|attr_type| EntityAttributeMetadata {
                    attribute_name: attribute_name.as_str(),
                    cedar_policy_type: attr_type,
                    is_required: attribute.is_required(),
                })
        })
        .collect::<Result<Vec<_>, _>>()
}

/// Build attributes for the entity
fn build_entity_attributes(
    schema: &CedarSchemaJson,
    parsed_typename: &EntityParsedTypeName,
    tkn_data: &TokenData,
    claim_mapping: &ClaimMappings,
) -> Result<HashMap<String, RestrictedExpression>, CedarPolicyCreateTypeError> {
    // fetch the schema entity shape from the json-schema.
    let schema_shape = fetch_schema_record(parsed_typename, schema)?;

    if let Some(schema_record) = &schema_shape.shape {
        let attr_vec = entity_meta_attributes(schema_record)?
            .into_iter()
            .filter_map(|attr: EntityAttributeMetadata| {
                let attr_name = attr.attribute_name;
                let cedar_exp_result = token_attribute_to_cedar_exp(
                    &attr,
                    tkn_data,
                    parsed_typename,
                    schema,
                    claim_mapping,
                );
                match (cedar_exp_result, attr.is_required) {
                    (Ok(cedar_exp), _) => Some(Ok((attr_name.to_string(), cedar_exp))),
                    (
                        Err(CedarPolicyCreateTypeError::MissingClaim(_)),
                        false,
                        // when the attribute is not required and not found in token data we skip it
                    ) => None,
                    (Err(err), _) => Some(Err(err)),
                }
            })
            .collect::<Result<Vec<(String, RestrictedExpression)>, CedarPolicyCreateTypeError>>()?;
        Ok(HashMap::from_iter(attr_vec))
    } else {
        Ok(HashMap::new())
    }
}

/// Create entity from token payload data.
pub fn create_entity(
    entity_uid: EntityUid,
    parsed_typename: &EntityParsedTypeName,
    schema: &CedarSchemaJson,
    tkn_data: &TokenData,
    parents: HashSet<EntityUid>,
    claim_mapping: &ClaimMappings,
) -> Result<cedar_policy::Entity, CedarPolicyCreateTypeError> {
    let attrs = build_entity_attributes(schema, parsed_typename, tkn_data, claim_mapping)?;

    let entity_uid_string = entity_uid.to_string();
    cedar_policy::Entity::new(entity_uid, attrs, parents)
        .map_err(|err| CedarPolicyCreateTypeError::CreateEntity(entity_uid_string, err))
}

/// Meta information about an attribute for cedar policy.
pub struct EntityAttributeMetadata<'a> {
    // The name of the attribute in the cedar policy
    // mapped one-to-one with the attribute in the token data.
    pub attribute_name: &'a str,
    // The type of the cedar policy attribute.
    pub cedar_policy_type: CedarType,
    // if this attribute is required
    pub is_required: bool,
}

/// Get the cedar policy expression value for a given type.
fn token_attribute_to_cedar_exp(
    attribute_metadata: &EntityAttributeMetadata,
    tkn_data: &TokenData,
    entity_typename: &EntityParsedTypeName,
    schema: &CedarSchemaJson,
    claim_mapping: &ClaimMappings,
) -> Result<RestrictedExpression, CedarPolicyCreateTypeError> {
    let token_claim_key = attribute_metadata.attribute_name;

    let token_claim_value =
        tkn_data
            .get_claim(token_claim_key)
            .ok_or(CedarPolicyCreateTypeError::MissingClaim(
                token_claim_key.to_string(),
            ))?;

    get_expression(
        &attribute_metadata.cedar_policy_type,
        &token_claim_value,
        entity_typename,
        schema,
        claim_mapping,
    )
}

/// Build [`RestrictedExpression`] based on input parameters.
fn get_expression(
    cedar_type: &CedarType,
    claim: &TokenClaim,
    base_entity_typename: &EntityParsedTypeName,
    schema: &CedarSchemaJson,
    claim_mapping: &ClaimMappings,
) -> Result<RestrictedExpression, CedarPolicyCreateTypeError> {
    match cedar_type {
        CedarType::String => Ok(claim.as_str()?.to_string().to_expression()),
        CedarType::Long => Ok(claim.as_i64()?.to_expression()),
        CedarType::Boolean => Ok(claim.as_bool()?.to_expression()),
        CedarType::TypeName(cedar_typename) => {
            match schema.find_type(cedar_typename, base_entity_typename.namespace) {
                Some(SchemaDefinedType::Entity(_)) => {
                    get_entity_expression(cedar_typename, base_entity_typename, claim)
                },
                Some(SchemaDefinedType::CommonType(record)) => {
                    let record_typename =
                        EntityParsedTypeName::new(cedar_typename, base_entity_typename.namespace);

                    get_record_expression(record, &record_typename, claim, schema, claim_mapping)
                        .map_err(|err| {
                            CedarPolicyCreateTypeError::CreateRecord(
                                record_typename.full_type_name(),
                                Box::new(err),
                            )
                        })
                },
                None => Err(CedarPolicyCreateTypeError::FindType(
                    EntityParsedTypeName::new(cedar_typename, base_entity_typename.namespace)
                        .full_type_name(),
                )),
            }
        },
        CedarType::Set(cedar_type) => {
            let vec_of_expression = claim
                .as_array()?
                .into_iter()
                .map(|payload| {
                    get_expression(
                        cedar_type,
                        &payload,
                        base_entity_typename,
                        schema,
                        claim_mapping,
                    )
                })
                .collect::<Result<Vec<_>, _>>()?;

            Ok(RestrictedExpression::new_set(vec_of_expression))
        },
    }
}

/// Create [`RestrictedExpression`] with entity UID as token_claim_value
fn get_entity_expression(
    cedar_typename: &str,
    base_entity_typename: &EntityParsedTypeName<'_>,
    token_claim: &TokenClaim,
) -> Result<RestrictedExpression, CedarPolicyCreateTypeError> {
    let restricted_expression = {
        let entity_full_type_name =
            EntityParsedTypeName::new(cedar_typename, base_entity_typename.namespace)
                .full_type_name();

        let uid = EntityUid::from_type_name_and_id(
            EntityTypeName::from_str(entity_full_type_name.as_str()).map_err(|err| {
                CedarPolicyCreateTypeError::EntityTypeName(entity_full_type_name.to_string(), err)
            })?,
            EntityId::new(token_claim.as_str()?),
        );
        RestrictedExpression::new_entity_uid(uid)
    };
    Ok(restricted_expression)
}

/// Build [`RestrictedExpression`] based on token_claim_value.
/// It tries to find mapping and apply it to `token_claim` json value.
fn get_record_expression(
    record: &CedarSchemaRecord,
    cedar_record_type: &EntityParsedTypeName<'_>,
    token_claim: &TokenClaim,
    schema: &CedarSchemaJson,
    claim_mapping: &ClaimMappings,
) -> Result<RestrictedExpression, CedarPolicyCreateTypeError> {
    // map json value of `token_claim` to TokenPayload object (HashMap)
    let mapped_claim: TokenData =
        match claim_mapping.get_mapping(token_claim.key(), &cedar_record_type.full_type_name()) {
            Some(m) => m.apply_mapping(token_claim.value()).into(),
            // if we do not have mapping, and value is json object, return TokenPayload based on it.
            // if value is not json object, return empty value
            None => {
                if let Some(map) = token_claim.value().as_object() {
                    TokenData::from_json_map(map.to_owned())
                } else {
                    TokenData::default()
                }
            },
        };

    let mut record_restricted_exps = Vec::new();

    for (attribute_key, entity_attribute) in record.attributes.iter() {
        let attribute_type = entity_attribute.get_type()?;

        let mapped_claim_value = mapped_claim.get_claim(attribute_key).ok_or(
            CedarPolicyCreateTypeError::MissingClaim(attribute_key.to_string()),
        )?;

        let exp = get_expression(
            &attribute_type,
            &mapped_claim_value,
            cedar_record_type,
            schema,
            claim_mapping,
        )
        .map_err(|err| {
            CedarPolicyCreateTypeError::BuildAttribute(
                cedar_record_type.full_type_name(),
                attribute_key.to_string(),
                Box::new(err),
            )
        })?;

        record_restricted_exps.push((attribute_key.to_string(), exp));
    }

    let restricted_expression =
        RestrictedExpression::new_record(record_restricted_exps.into_iter())
            .map_err(CedarPolicyCreateTypeError::CreateRecordFromIter)?;
    Ok(restricted_expression)
}

/// Describe errors on creating entity
#[derive(thiserror::Error, Debug)]
pub enum CedarPolicyCreateTypeError {
    /// Could not parse entity type
    #[error("could not parse entity type name: {0}, error: {1}")]
    EntityTypeName(String, cedar_policy::ParseErrors),

    /// Could find entity type in the `cedar-policy` schema
    #[error("could find entity type: {0} in the schema")]
    CouldNotFindEntity(String),

    /// Type in the schema is not record
    #[error("type: {0} in the schema is not record")]
    NotRecord(String),

    /// Could create entity
    #[error("could create entity with uid: {0}, error: {1}")]
    CreateEntity(String, cedar_policy::EntityAttrEvaluationError),

    /// Could not get attribute value from payload
    #[error("could not get attribute value from payload: {0}")]
    GetTokenClaim(#[from] TokenClaimTypeError),

    /// Could not retrieve attribute from cedar-policy schema
    #[error("could not retrieve attribute from cedar-policy schema: {0}")]
    GetCedarType(#[from] GetCedarTypeError),

    /// Error on cedar-policy type attribute
    #[error("err build cedar-policy type: {0}, mapped JWT attribute `{1}`: {2}")]
    BuildAttribute(String, String, Box<CedarPolicyCreateTypeError>),

    /// Error on creating `cedar-policy` record, in schema it is named as type
    #[error("could not create `cedar-policy` record/type {0} : {1}")]
    CreateRecord(String, Box<CedarPolicyCreateTypeError>),

    /// Wrapped error on [`RestrictedExpression::new_record`]
    // this error probably newer happen
    #[error("could not build expression from list of expressions: {0}")]
    CreateRecordFromIter(cedar_policy::ExpressionConstructionError),

    /// Cause when cannot find record/type in json schema.
    #[error("could find record/type: {0}")]
    FindType(String),

    /// Error when using the transaction token. Its usage is currently not implemented.
    #[error("transaction token not implemented")]
    TransactionToken,

    /// Indicates that the creation of an Entity failed due to the absence of available tokens.
    #[error("{0} Entity creation failed: no available token to build the entity from")]
    UnavailableToken(String),

    /// Missing claim
    #[error("{0} Entity creation failed: no available token to build the entity from")]
    MissingClaim(String),
}
