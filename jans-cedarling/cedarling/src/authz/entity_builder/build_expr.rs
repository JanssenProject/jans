// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::common::cedar_schema::cedar_json::attribute::Attribute;
use crate::common::cedar_schema::cedar_json::CedarSchemaJson;
use cedar_policy::{
    EntityId, EntityTypeName, EntityUid, ExpressionConstructionError, RestrictedExpression,
};
use serde_json::Value;
use std::collections::HashMap;
use std::str::FromStr;

use super::CEDAR_NAMESPACE_SEPARATOR;

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

    /// Builds a [`RestrictedExpression`] while checking the schema
    pub fn build_expr(
        &self,
        attr_src: &HashMap<String, Value>,
        src_key: &str,
        schema: &CedarSchemaJson,
    ) -> Result<Option<RestrictedExpression>, BuildExprError> {
        match self {
            // Handle String attributes
            Attribute::String { required } => {
                if let Some(claim) = attr_src.get(src_key) {
                    let claim = claim
                        .as_str()
                        .ok_or(KeyedJsonTypeError::type_mismatch(src_key, "string", claim))?
                        .to_string();
                    Ok(Some(RestrictedExpression::new_string(claim)))
                } else if *required {
                    Err(BuildExprError::MissingSource(src_key.to_string()))
                } else {
                    Ok(None)
                }
            },

            // Handle Long attributes
            Attribute::Long { required } => {
                if let Some(claim) = attr_src.get(src_key) {
                    let claim = claim
                        .as_i64()
                        .ok_or(KeyedJsonTypeError::type_mismatch(src_key, "number", claim))?;
                    Ok(Some(RestrictedExpression::new_long(claim)))
                } else if *required {
                    Err(BuildExprError::MissingSource(src_key.to_string()))
                } else {
                    Ok(None)
                }
            },

            // Handle Boolean attributes
            Attribute::Boolean { required } => {
                if let Some(claim) = attr_src.get(src_key) {
                    let claim = claim
                        .as_bool()
                        .ok_or(KeyedJsonTypeError::type_mismatch(src_key, "bool", claim))?;
                    Ok(Some(RestrictedExpression::new_bool(claim)))
                } else if *required {
                    Err(BuildExprError::MissingSource(src_key.to_string()))
                } else {
                    Ok(None)
                }
            },

            // Handle Record attributes
            Attribute::Record { attrs, required } => {
                let mut fields = HashMap::new();
                for (name, kind) in attrs.iter() {
                    if let Some(expr) = kind.build_expr(attr_src, name, schema)? {
                        fields.insert(name.to_string(), expr);
                    }
                }

                if fields.is_empty() && !required {
                    Ok(None)
                } else {
                    Ok(Some(RestrictedExpression::new_record(fields)?))
                }
            },

            // Handle Set attributes
            Attribute::Set { required, element } => {
                if let Some(claim) = attr_src.get(src_key) {
                    let claim = claim
                        .as_array()
                        .ok_or(KeyedJsonTypeError::type_mismatch(src_key, "Array", claim))?;

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
                    Err(BuildExprError::MissingSource(src_key.to_string()))
                } else {
                    Ok(None)
                }
            },

            // Handle Entity attributes
            Attribute::Entity { required, name } => {
                if let Some(claim) = attr_src.get(src_key) {
                    let claim = claim
                        .as_str()
                        .ok_or(KeyedJsonTypeError::type_mismatch(src_key, "string", claim))?;

                    let mut name = name.to_string();
                    if let Some((namespace, _)) = schema.get_entity_from_base_name(&name) {
                        if !namespace.is_empty() {
                            name = [namespace, name.as_str()].join(CEDAR_NAMESPACE_SEPARATOR);
                        }
                    }

                    let type_name = EntityTypeName::from_str(&name).unwrap();
                    let type_id = EntityId::new(claim);
                    let uid = EntityUid::from_type_name_and_id(type_name, type_id);
                    Ok(Some(RestrictedExpression::new_entity_uid(uid)))
                } else if *required {
                    Err(BuildExprError::MissingSource(src_key.to_string()))
                } else {
                    Ok(None)
                }
            },

            // Handle Extension attributes
            Attribute::Extension { required, name } => {
                if let Some(claim) = attr_src.get(src_key) {
                    let claim = claim
                        .as_str()
                        .ok_or(KeyedJsonTypeError::type_mismatch(src_key, "string", claim))?;
                    let expr = match name.as_str() {
                        "ipaddr" => RestrictedExpression::new_ip(claim),
                        "decimal" => RestrictedExpression::new_decimal(claim),
                        name => RestrictedExpression::new_unknown(name),
                    };
                    Ok(Some(expr))
                } else if *required {
                    Err(BuildExprError::MissingSource(src_key.to_string()))
                } else {
                    Ok(None)
                }
            },

            // Handle EntityOrCommon attributes
            Attribute::EntityOrCommon { required, name } => {
                if let Some((_namespace_name, attr)) = schema.get_common_type(name) {
                    attr.build_expr(attr_src, src_key, schema)
                } else if schema.get_entity_from_base_name(name).is_some() {
                    let attr = Attribute::Entity {
                        required: *required,
                        name: name.to_string(),
                    };
                    attr.build_expr(attr_src, src_key, schema)
                } else if let Some(attr) = str_to_primitive_type(*required, name) {
                    attr.build_expr(attr_src, src_key, schema)
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
    #[error("the given attribute source data is missing the key: {0}")]
    MissingSource(String),
    #[error(transparent)]
    TypeMismatch(#[from] KeyedJsonTypeError),
    #[error(transparent)]
    ConstructionError(#[from] ExpressionConstructionError),
    #[error(
        "failed to build restricted expression for `{0}` since the type could not be determined"
    )]
    UnkownType(String),
}

#[derive(Debug, thiserror::Error)]
#[error("type mismatch for key '{key}'. expected: '{expected_type}', but found: '{actual_type}'")]
pub struct KeyedJsonTypeError {
    pub key: String,
    pub expected_type: String,
    pub actual_type: String,
}

impl KeyedJsonTypeError {
    /// Returns the JSON type name of the given value.
    pub fn value_type_name(value: &Value) -> &'static str {
        match value {
            Value::Null => "null",
            Value::Bool(_) => "bool",
            Value::Number(_) => "number",
            Value::String(_) => "string",
            Value::Array(_) => "array",
            Value::Object(_) => "object",
        }
    }

    /// Constructs a `TypeMismatch` error with detailed information about the expected and actual types.
    pub fn type_mismatch(key: &str, expected_type_name: &str, got_value: &Value) -> Self {
        let got_value_type_name = Self::value_type_name(got_value).to_string();

        Self {
            key: key.to_string(),
            expected_type: expected_type_name.to_string(),
            actual_type: got_value_type_name,
        }
    }
}
