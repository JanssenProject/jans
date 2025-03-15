// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::{
    PartitionResult,
    schema::{EntityRefAttrSrc, EntityRefSetSrc, ExpectedClaimType, TknClaimAttrSrc},
};
use crate::common::cedar_schema::cedar_json::attribute::Attribute;
use cedar_policy::{EntityUid, RestrictedExpression};
use serde_json::Value;
use smol_str::SmolStr;
use std::{collections::HashMap, fmt::Display, str::FromStr};
use thiserror::Error;

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
}

#[derive(Debug, Error)]
#[error("type mismatch for key '{key}'. expected: '{expected_type}', but found: '{actual_type}'")]
pub struct KeyedJsonTypeError {
    pub key: String,
    pub expected_type: String,
    pub actual_type: String,
}

impl TknClaimAttrSrc {
    pub fn build_expr(
        &self,
        src: &Value,
    ) -> Result<Option<RestrictedExpression>, BuildExprErrorVec> {
        build_expr_from_value(&self.expected_type, src)
    }
}

impl EntityRefAttrSrc {
    pub fn build_expr(&self, id: &str) -> Result<RestrictedExpression, BuildExprError> {
        let uid = EntityUid::from_str(&format!("{}::\"{}\"", &self.0, id))?;
        Ok(RestrictedExpression::new_entity_uid(uid))
    }
}

impl EntityRefSetSrc {
    pub fn build_expr(&self, ids: &[SmolStr]) -> Result<RestrictedExpression, BuildExprErrorVec> {
        let (uids, errs): (Vec<_>, Vec<_>) = ids
            .iter()
            .map(|id| {
                EntityUid::from_str(&format!("{}::\"{}\"", &self.0, id))
                    .map(RestrictedExpression::new_entity_uid)
            })
            .partition_result();

        if !errs.is_empty() {
            return Err(BuildExprErrorVec::from(
                errs.into_iter()
                    .map(|e| BuildExprError::ParseUid(e))
                    .collect::<Vec<_>>(),
            ));
        }

        Ok(RestrictedExpression::new_set(uids))
    }
}

fn build_expr_from_value(
    claim_type: &ExpectedClaimType,
    src: &Value,
) -> Result<Option<RestrictedExpression>, BuildExprErrorVec> {
    match claim_type {
        ExpectedClaimType::Null => Ok(None),
        ExpectedClaimType::Bool => {
            let val = src.as_bool().ok_or_else(|| TypeMismatchError {
                expected: "bool".to_string(),
                actual: TypeMismatchError::value_type_name(src).to_string(),
            })?;
            Ok(Some(RestrictedExpression::new_bool(val)))
        },
        ExpectedClaimType::Number => {
            let val = src.as_i64().ok_or_else(|| TypeMismatchError {
                expected: "number".to_string(),
                actual: TypeMismatchError::value_type_name(src).to_string(),
            })?;
            Ok(Some(RestrictedExpression::new_long(val)))
        },
        ExpectedClaimType::String => {
            let val = src.as_str().ok_or_else(|| TypeMismatchError {
                expected: "string".to_string(),
                actual: TypeMismatchError::value_type_name(src).to_string(),
            })?;
            Ok(Some(RestrictedExpression::new_string(val.to_string())))
        },
        ExpectedClaimType::Array(expected_claim_type) => {
            let src = src.as_array().ok_or_else(|| TypeMismatchError {
                expected: "array".to_string(),
                actual: TypeMismatchError::value_type_name(src).to_string(),
            })?;

            let (vals, errs): (Vec<_>, Vec<_>) = src
                .iter()
                .map(|src| build_expr_from_value(expected_claim_type, src))
                .filter_map(|expr| expr.transpose())
                .partition_result();

            if !errs.is_empty() {
                return Err(BuildExprErrorVec(
                    errs.into_iter().map(|e| e.into_inner()).flatten().collect(),
                ))?;
            }

            Ok(Some(RestrictedExpression::new_set(vals)))
        },
        ExpectedClaimType::Object(expected_obj) => {
            let src = src.as_object().ok_or_else(|| TypeMismatchError {
                expected: "object".to_string(),
                actual: TypeMismatchError::value_type_name(src).to_string(),
            })?;

            let (fields, errs): (Vec<_>, Vec<_>) = expected_obj
                .iter()
                .filter_map(|(name, ty)| {
                    src.get(name.as_str()).and_then(|src| {
                        build_expr_from_value(ty, src)
                            .transpose()
                            .map(|res| res.map(|res| (name.to_string(), res)))
                    })
                })
                .partition_result();

            let fields: HashMap<String, RestrictedExpression> = if errs.is_empty() {
                fields.into_iter().collect()
            } else {
                return Err(BuildExprErrorVec(
                    errs.into_iter().map(|e| e.into_inner()).flatten().collect(),
                ))?;
            };

            Ok(Some(RestrictedExpression::new_record(fields)?))
        },
        ExpectedClaimType::Extension(name) => {
            let val = src.as_str().unwrap();
            let expr = match name.as_str() {
                "decimal" => Some(RestrictedExpression::new_decimal(val)),
                "ipaddr" => Some(RestrictedExpression::new_ip(val)),
                _ => Some(RestrictedExpression::new_unknown(val)),
            };
            Ok(expr)
        },
    }
}

#[derive(Debug, Error)]
pub struct BuildExprErrorVec(Vec<BuildExprError>);

impl BuildExprErrorVec {
    pub fn into_inner(self) -> Vec<BuildExprError> {
        self.0
    }
}

impl From<Vec<BuildExprError>> for BuildExprErrorVec {
    fn from(errs: Vec<BuildExprError>) -> Self {
        Self(errs)
    }
}

#[derive(Debug, Error)]
pub enum BuildExprError {
    #[error(transparent)]
    TypeMismatch(#[from] TypeMismatchError),
    #[error(transparent)]
    ConstructExpr(#[from] cedar_policy::ExpressionConstructionError),
    #[error("failed to parse uid: {0}")]
    ParseUid(#[from] cedar_policy::ParseErrors),
}

#[derive(Debug, Error)]
#[error("expected a {expected}, but found: '{actual}'")]
pub struct TypeMismatchError {
    pub expected: String,
    pub actual: String,
}

impl Display for BuildExprErrorVec {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "{:?}", self.0.iter().map(|e| e.to_string()))
    }
}

impl From<BuildExprError> for BuildExprErrorVec {
    fn from(err: BuildExprError) -> Self {
        Self(Vec::from([err]))
    }
}

impl From<TypeMismatchError> for BuildExprErrorVec {
    fn from(err: TypeMismatchError) -> Self {
        Self(Vec::from([BuildExprError::TypeMismatch(err)]))
    }
}

impl From<cedar_policy::ExpressionConstructionError> for BuildExprErrorVec {
    fn from(err: cedar_policy::ExpressionConstructionError) -> Self {
        Self(Vec::from([BuildExprError::ConstructExpr(err)]))
    }
}

impl TypeMismatchError {
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
}
