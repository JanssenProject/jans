// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::CEDAR_NAMESPACE_SEPARATOR;
use crate::common::cedar_schema::cedar_json::CedarSchemaJson;
use crate::common::cedar_schema::cedar_json::attribute::Attribute;
use cedar_policy::{
    EntityId, EntityTypeName, EntityUid, ExpressionConstructionError, ParseErrors,
    RestrictedExpression,
};
use serde_json::Value;
use smol_str::{SmolStr, ToSmolStr};
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

    /// Builds a [`RestrictedExpression`] while checking the schema
    pub fn build_expr(
        &self,
        attr_src: &HashMap<String, Value>,
        src_key: &str,
        schema: &CedarSchemaJson,
        built_entities: &HashMap<SmolStr, SmolStr>,
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
                    if let Some(expr) = kind.build_expr(attr_src, name, schema, built_entities)? {
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
                        .ok_or(KeyedJsonTypeError::type_mismatch(src_key, "array", claim))?;

                    let mut values = Vec::new();
                    for (i, val) in claim.iter().enumerate() {
                        let claim_name = i.to_string();
                        if let Some(expr) = element.build_expr(
                            &HashMap::from([(claim_name.clone(), val.clone())]),
                            &claim_name,
                            schema,
                            built_entities,
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
                // Check if the entity is in the schema
                let mut name = name.to_string();
                if let Some((namespace, _)) = schema.get_entity_from_base_name(&name) {
                    if !namespace.is_empty() {
                        name = [namespace, name.as_str()].join(CEDAR_NAMESPACE_SEPARATOR);
                    }
                } else if *required {
                    return Err(BuildExprError::EntityNotInSchema(name.to_string()));
                } else {
                    return Ok(None);
                }

                // Get the entity id
                let entity_type_name = EntityTypeName::from_str(&name)
                    .map_err(|e| BuildExprError::ParseTypeName(name.to_string(), e))?;
                let entity_id = if let Some(entity_id) =
                    built_entities.get(&entity_type_name.to_smolstr())
                {
                    entity_id
                } else if let Some(entity_id) = built_entities.get(entity_type_name.basename()) {
                    entity_id
                } else if let Some(claim) = attr_src.get(src_key) {
                    claim
                        .as_str()
                        .ok_or(KeyedJsonTypeError::type_mismatch(src_key, "string", claim))?
                } else if *required {
                    return Err(BuildExprError::EntityReference(src_key.to_string(), name));
                } else {
                    return Ok(None);
                };

                let type_name = cedar_policy::EntityTypeName::from_str(&name)
                    .map_err(|e| BuildExprError::ParseTypeName(name.clone(), e))?;
                let type_id = EntityId::new(entity_id);
                let uid = EntityUid::from_type_name_and_id(type_name, type_id);
                let expr = RestrictedExpression::new_entity_uid(uid);
                Ok(Some(expr))
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
                    attr.build_expr(attr_src, src_key, schema, built_entities)
                } else if schema.get_entity_from_base_name(name).is_some() {
                    let attr = Attribute::Entity {
                        required: *required,
                        name: name.to_string(),
                    };
                    attr.build_expr(attr_src, src_key, schema, built_entities)
                } else if let Some(attr) = str_to_primitive_type(*required, name) {
                    attr.build_expr(attr_src, src_key, schema, built_entities)
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
    #[error(
        "failed to build entity reference for '{0}' with type `{1}` because no entity of this type has been created yet"
    )]
    EntityReference(String, String),
    #[error(transparent)]
    TypeMismatch(#[from] KeyedJsonTypeError),
    #[error(transparent)]
    ConstructionError(#[from] ExpressionConstructionError),
    #[error("the type of `{0}` could not be determined")]
    UnkownType(String),
    #[error("the entity type `{0}` is not in the schema")]
    EntityNotInSchema(String),
    #[error("failed to parse entity type name `{0}`: {1}")]
    ParseTypeName(String, ParseErrors),
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

#[cfg(test)]
mod test {
    use crate::{
        authz::entity_builder::BuildExprError,
        common::cedar_schema::cedar_json::{CedarSchemaJson, attribute::Attribute},
    };
    use serde_json::json;
    use std::collections::HashMap;

    #[test]
    fn can_build_string_expr() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": { "entityTypes": { "Test": {
                "shape": {
                    "type": "Record",
                    "attributes":  {
                        "attr1": { "type": "String" },
                    },
                }
            }}}
        }))
        .expect("should successfully build schema");
        let attr = Attribute::string();
        let src = HashMap::from([("src_key".to_string(), json!("attr-val"))]);
        let expr = attr
            .build_expr(&src, "src_key", &schema, &HashMap::new())
            .expect("should not error");
        assert!(expr.is_some(), "a restricted expression should be built")
    }

    #[test]
    fn can_build_long_expr() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": { "entityTypes": { "Test": {
                "shape": {
                    "type": "Record",
                    "attributes":  {
                        "attr1": { "type": "Long" },
                    },
                }
            }}}
        }))
        .expect("should successfully build schema");
        let attr = Attribute::long();
        let src = HashMap::from([("src_key".to_string(), json!(123))]);
        let expr = attr
            .build_expr(&src, "src_key", &schema, &HashMap::new())
            .expect("should not error");
        assert!(expr.is_some(), "a restricted expression should be built")
    }

    #[test]
    fn can_build_boolean_expr() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": { "entityTypes": { "Test": {
                "shape": {
                    "type": "Record",
                    "attributes":  {
                        "attr1": { "type": "Boolean" },
                    },
                }
            }}}
        }))
        .expect("should successfully build schema");
        let attr = Attribute::boolean();
        let src = HashMap::from([("src_key".to_string(), json!(true))]);
        let expr = attr
            .build_expr(&src, "src_key", &schema, &HashMap::new())
            .expect("should not error");
        assert!(expr.is_some(), "a restricted expression should be built")
    }

    #[test]
    fn can_build_record_expr() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": { "entityTypes": { "Test": {
                "shape": {
                    "type": "Record",
                    "attributes":  {
                        "outer_attr": {
                            "type": "Record",
                            "attributes": {
                                "inner_attr": { "type": "String" }
                            },
                        },
                    },
                }
            }}}
        }))
        .expect("should successfully build schema");
        let attr = Attribute::record(HashMap::from([(
            "inner_attr".to_string(),
            Attribute::string(),
        )]));
        let src = HashMap::from([("inner_attr".to_string(), json!("test"))]);
        let expr = attr
            .build_expr(&src, "src_key", &schema, &HashMap::new())
            .expect("should not error");
        assert!(expr.is_some(), "a restricted expression should be built")
    }

    #[test]
    fn can_build_set_expr() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": { "entityTypes": { "Test": {
                "shape": {
                    "type": "Record",
                    "attributes":  {
                        "attr1": {
                            "type": "Set",
                            "element": {
                              "type": "String",
                            }
                        },
                    },
                }
            }}}
        }))
        .expect("should successfully build schema");
        let attr = Attribute::set(Attribute::string());
        let src = HashMap::from([("src_key".to_string(), json!(["admin", "user"]))]);
        let expr = attr
            .build_expr(&src, "src_key", &schema, &HashMap::new())
            .expect("should not error");
        assert!(expr.is_some(), "a restricted expression should be built")
    }

    #[test]
    fn errors_when_expected_set_has_different_types() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": { "entityTypes": { "Test": {
                "shape": {
                    "type": "Record",
                    "attributes":  {
                        "attr1": {
                            "type": "Set",
                            "element": {
                              "type": "String",
                            }
                        },
                    },
                }
            }}}
        }))
        .expect("should successfully build schema");
        let attr = Attribute::set(Attribute::string());
        let src = HashMap::from([("src_key".to_string(), json!(["admin", 123]))]);
        let err = attr
            .build_expr(&src, "src_key", &schema, &HashMap::new())
            .expect_err("should error");
        assert!(
            matches!(err, BuildExprError::TypeMismatch(_)),
            "should error due to type mismatch but got: {:?}",
            err
        );
    }

    #[test]
    fn can_build_entity_expr() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": { "entityTypes": {
                "OtherEntity": {},
                "Test": {
                    "shape": {
                        "type": "Record",
                        "attributes":  {
                            "attr1": {
                                "type": "Entity",
                                "name": "OtherEntity",
                            },
                        },
                    }
                }
            }}
        }))
        .expect("should successfully build schema");
        let attr = Attribute::entity("OtherEntity");
        let src = HashMap::from([("src_key".to_string(), json!("test"))]);
        let expr = attr
            .build_expr(&src, "src_key", &schema, &HashMap::new())
            .expect("should not error");
        assert!(expr.is_some(), "a restricted expression should be built");
    }

    #[test]
    fn can_build_entity_expr_from_entity_or_common() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": { "entityTypes": {
                "OtherEntity": {},
                "Test": {
                    "shape": {
                        "type": "Record",
                        "attributes":  {
                            "attr1": {
                                "type": "EntityOrCommon",
                                "name": "OtherEntity",
                            },
                        },
                    }
                }
            }}
        }))
        .expect("should successfully build schema");
        let attr = Attribute::entity("OtherEntity");
        let src = HashMap::from([("src_key".to_string(), json!("test"))]);
        let expr = attr
            .build_expr(&src, "src_key", &schema, &HashMap::new())
            .expect("should not error");
        assert!(expr.is_some(), "a restricted expression should be built");
    }

    #[test]
    fn errors_when_entity_isnt_in_schema() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": { "entityTypes": { "Test": {
                "shape": {
                    "type": "Record",
                    "attributes":  {
                        "attr1": {
                            "type": "Entity",
                            "name": "OtherEntity",
                        },
                    },
                }
            }}}
        }))
        .expect("should successfully build schema");
        let attr = Attribute::entity("OtherEntity");
        let src = HashMap::from([("src_key".to_string(), json!("test"))]);
        let err = attr
            .build_expr(&src, "src_key", &schema, &HashMap::new())
            .expect_err("should error");
        assert!(
            matches!(
                err,
                BuildExprError::EntityNotInSchema(ref entity_name)
                    if entity_name == "OtherEntity"
            ),
            "should error due to type mismatch but got: {:?}",
            err
        );
    }

    #[test]
    fn can_build_ip_addr_extension_expr() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": { "entityTypes": { "Test": {
                "shape": {
                    "type": "Record",
                    "attributes":  {
                        "attr1": { "type": "Extension", "name": "ipaddr" },
                    },
                }
            }}}
        }))
        .expect("should successfully build schema");
        let attr = Attribute::string();
        let src = HashMap::from([("src_key".to_string(), json!("0.0.0.0"))]);
        let expr = attr
            .build_expr(&src, "src_key", &schema, &HashMap::new())
            .expect("should not error");
        assert!(expr.is_some(), "a restricted expression should be built")
    }

    #[test]
    fn can_build_decimal_extension_expr() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": { "entityTypes": { "Test": {
                "shape": {
                    "type": "Record",
                    "attributes":  {
                        "attr1": { "type": "Extension", "name": "decimal" },
                    },
                }
            }}}
        }))
        .expect("should successfully build schema");
        let attr = Attribute::string();
        let src = HashMap::from([("src_key".to_string(), json!("1.1"))]);
        let expr = attr
            .build_expr(&src, "src_key", &schema, &HashMap::new())
            .expect("should not error");
        assert!(expr.is_some(), "a restricted expression should be built")
    }

    #[test]
    fn can_skip_non_required_expr() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": { "entityTypes": { "Workload": {
                "shape": {
                    "type": "Record",
                    "attributes":  {
                        "client_id": { "type": "String" },
                    },
                }
            }}}
        }))
        .expect("should successfully build schema");
        let attr = Attribute::String { required: false };
        let src = HashMap::new();
        let expr = attr
            .build_expr(&src, "client_id", &schema, &HashMap::new())
            .expect("should not error");
        assert!(expr.is_none(), "a restricted expression shouldn't built")
    }

    #[test]
    fn errors_on_type_mismatch() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": { "entityTypes": { "Test": {
                "shape": {
                    "type": "Record",
                    "attributes":  {
                        "attr1": { "type": "String" },
                    },
                }
            }}}
        }))
        .expect("should successfully build schema");
        let attr = Attribute::string();
        let src = HashMap::from([("src_key".to_string(), json!(123))]);
        let err = attr
            .build_expr(&src, "src_key", &schema, &HashMap::new())
            .expect_err("should error");
        assert!(
            matches!(err, BuildExprError::TypeMismatch(_)),
            "should error due to type mismatch but got: {:?}",
            err
        );
    }
}
