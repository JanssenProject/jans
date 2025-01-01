// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::common::cedar_schema::new_cedar_json::attribute::Attribute;
use crate::common::cedar_schema::new_cedar_json::CedarSchemaJson;
use crate::jwt::TokenClaimTypeError;
use cedar_policy::{
    EntityId, EntityTypeName, EntityUid, ExpressionConstructionError, RestrictedExpression,
};
use serde_json::Value;
use std::collections::HashMap;
use std::str::FromStr;

impl Attribute {
    pub fn kind_str(&self) -> &str {
        match self {
            Attribute::String { .. } => "String",
            Attribute::Long { .. } => "Long",
            Attribute::Boolean { .. } => "Boolean",
            Attribute::Record { .. } => "Record",
            Attribute::Set { .. } => "Set",
            Attribute::Entity { .. } => "Entity",
            Attribute::Extension { .. } => "Extension",
            Attribute::EntityOrCommon { .. } => "EntityOrCommon",
        }
    }

    /// Builds a [`RestrictedExpression`] using a token's claim
    pub fn build_expr(
        &self,
        claims: &HashMap<String, Value>,
        claim_name: &str,
        schema: &CedarSchemaJson,
    ) -> Result<Option<RestrictedExpression>, BuildExprError> {
        match self {
            // Handle String attributes
            Attribute::String { required } => {
                if let Some(claim) = claims.get(claim_name) {
                    let claim = claim
                        .as_str()
                        .ok_or(TokenClaimTypeError::type_mismatch(
                            claim_name, "string", claim,
                        ))?
                        .to_string();
                    Ok(Some(RestrictedExpression::new_string(claim)))
                } else if *required {
                    Err(BuildExprError::MissingClaim(claim_name.to_string()))
                } else {
                    Ok(None)
                }
            },

            // Handle Long attributes
            Attribute::Long { required } => {
                if let Some(claim) = claims.get(claim_name) {
                    let claim = claim.as_i64().ok_or(TokenClaimTypeError::type_mismatch(
                        claim_name, "number", claim,
                    ))?;
                    Ok(Some(RestrictedExpression::new_long(claim)))
                } else if *required {
                    Err(BuildExprError::MissingClaim(claim_name.to_string()))
                } else {
                    Ok(None)
                }
            },

            // Handle Boolean attributes
            Attribute::Boolean { required } => {
                if let Some(claim) = claims.get(claim_name) {
                    let claim = claim.as_bool().ok_or(TokenClaimTypeError::type_mismatch(
                        claim_name, "bool", claim,
                    ))?;
                    Ok(Some(RestrictedExpression::new_bool(claim)))
                } else if *required {
                    Err(BuildExprError::MissingClaim(claim_name.to_string()))
                } else {
                    Ok(None)
                }
            },

            // Handle Record attributes
            Attribute::Record { attrs, required } => {
                let mut fields = HashMap::new();
                for (name, kind) in attrs.iter() {
                    if let Some(expr) = kind.build_expr(claims, name, schema)? {
                        fields.insert(name.to_string(), expr);
                    } else if *required {
                        return Err(BuildExprError::MissingClaim(name.clone()));
                    }
                }
                Ok(Some(RestrictedExpression::new_record(fields)?))
            },

            // Handle Set attributes
            Attribute::Set { required, element } => {
                if let Some(claim) = claims.get(claim_name) {
                    let claim = claim.as_array().ok_or(TokenClaimTypeError::type_mismatch(
                        claim_name, "Array", claim,
                    ))?;

                    let mut values = Vec::new();
                    for (i, val) in claim.iter().enumerate() {
                        let claim_name = i.to_string();
                        if let Some(expr) = element.build_expr(
                            &HashMap::from([(claim_name.clone(), val.clone())]),
                            &claim_name,
                            schema,
                        )? {
                            values.push(expr);
                        }
                    }
                    Ok(Some(RestrictedExpression::new_set(values)))
                } else if *required {
                    Err(BuildExprError::MissingClaim(claim_name.to_string()))
                } else {
                    Ok(None)
                }
            },

            // Handle Entity attributes
            Attribute::Entity { required, name } => {
                if let Some(claim) = claims.get(claim_name) {
                    let claim = claim.as_str().ok_or(TokenClaimTypeError::type_mismatch(
                        claim_name, "string", claim,
                    ))?;
                    let type_name = EntityTypeName::from_str(name).unwrap();
                    let type_id = EntityId::new(claim);
                    let uid = EntityUid::from_type_name_and_id(type_name, type_id);
                    Ok(Some(RestrictedExpression::new_entity_uid(uid)))
                } else if *required {
                    Err(BuildExprError::MissingClaim(claim_name.to_string()))
                } else {
                    Ok(None)
                }
            },

            // Handle Extension attributes
            Attribute::Extension { required, name } => {
                if let Some(claim) = claims.get(claim_name) {
                    let claim = claim.as_str().ok_or(TokenClaimTypeError::type_mismatch(
                        claim_name, "string", claim,
                    ))?;
                    let expr = match name.as_str() {
                        "ipaddr" => RestrictedExpression::new_ip(claim),
                        "decimal" => RestrictedExpression::new_decimal(claim),
                        name => RestrictedExpression::new_unknown(name),
                    };
                    Ok(Some(expr))
                } else if *required {
                    Err(BuildExprError::MissingClaim(claim_name.to_string()))
                } else {
                    Ok(None)
                }
            },

            // Handle EntityOrCommon attributes
            Attribute::EntityOrCommon { required, name } => {
                if let Some((_namespace_name, attr)) = schema.get_common_type(name) {
                    attr.build_expr(claims, claim_name, schema)
                } else if schema.get_entity_type(name).is_some() {
                    let attr = Attribute::Entity {
                        required: *required,
                        name: name.to_string(),
                    };
                    attr.build_expr(claims, claim_name, schema)
                } else if let Some(attr) = str_to_primitive_type(*required, name) {
                    attr.build_expr(claims, claim_name, schema)
                } else if *required {
                    Err(BuildExprError::UnkownType(name.to_string()))
                } else {
                    Ok(None)
                }
            },
        }
    }
}

fn str_to_primitive_type(required: bool, name: &str) -> Option<Attribute> {
    let primitive_type = match name {
        "String" => Attribute::String { required },
        "Long" => Attribute::Long { required },
        "Boolean" => Attribute::Boolean { required },
        _ => return None,
    };
    Some(primitive_type)
}

/// Errors when building a [`RestrictedExpression`]
#[derive(Debug, thiserror::Error)]
pub enum BuildExprError {
    #[error("the given token is missing the claim: {0}")]
    MissingClaim(String),
    #[error(transparent)]
    TypeMismatch(#[from] TokenClaimTypeError),
    #[error(transparent)]
    ConstructionError(#[from] ExpressionConstructionError),
    #[error(
        "failed to build restricted expression for `{0}` since the type could not be determined"
    )]
    UnkownType(String),
}
