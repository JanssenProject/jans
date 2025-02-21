// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::built_entities::BuiltEntities;
use crate::common::cedar_schema::cedar_json::CedarSchemaJson;
use crate::common::cedar_schema::cedar_json::attribute::Attribute;
use cedar_policy::{
    EntityId, EntityTypeName, EntityUid, ExpressionConstructionError, ParseErrors,
    RestrictedExpression,
};
use serde_json::Value;
use std::collections::HashMap;

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
        src_key: &str,
        attr_claim_value: Option<&Value>,
        default_namespace: Option<&str>,
        schema: &CedarSchemaJson,
        built_entities: &BuiltEntities,
    ) -> Result<Option<RestrictedExpression>, BuildExprError> {
        match self {
            // Handle String attributes
            Attribute::String { required } => {
                let Some(attr_claim_value) = attr_claim_value else {
                    if *required {
                        return Err(BuildExprError::MissingSource(src_key.to_string()));
                    } else {
                        return Ok(None);
                    }
                };

                let claim = attr_claim_value
                    .as_str()
                    .ok_or(KeyedJsonTypeError::type_mismatch(
                        src_key,
                        "string",
                        attr_claim_value,
                    ))?
                    .to_string();
                Ok(Some(RestrictedExpression::new_string(claim)))
            },

            // Handle Long attributes
            Attribute::Long { required } => {
                let Some(attr_claim_value) = attr_claim_value else {
                    if *required {
                        return Err(BuildExprError::MissingSource(src_key.to_string()));
                    } else {
                        return Ok(None);
                    }
                };

                let claim = attr_claim_value
                    .as_i64()
                    .ok_or(KeyedJsonTypeError::type_mismatch(
                        src_key,
                        "number",
                        attr_claim_value,
                    ))?;
                Ok(Some(RestrictedExpression::new_long(claim)))
            },

            // Handle Boolean attributes
            Attribute::Boolean { required } => {
                let Some(attr_claim_value) = attr_claim_value else {
                    if *required {
                        return Err(BuildExprError::MissingSource(src_key.to_string()));
                    } else {
                        return Ok(None);
                    }
                };

                let claim = attr_claim_value
                    .as_bool()
                    .ok_or(KeyedJsonTypeError::type_mismatch(
                        src_key,
                        "bool",
                        attr_claim_value,
                    ))?;
                Ok(Some(RestrictedExpression::new_bool(claim)))
            },

            // Handle Record attributes
            Attribute::Record { attrs, required } => {
                let Some(attr_claim_value) = attr_claim_value else {
                    if *required {
                        return Err(BuildExprError::MissingSource(src_key.to_string()));
                    } else {
                        return Ok(None);
                    }
                };

                let attr_claim_object =
                    attr_claim_value
                        .as_object()
                        .ok_or(KeyedJsonTypeError::type_mismatch(
                            src_key,
                            "object",
                            attr_claim_value,
                        ))?;

                let mut fields = HashMap::new();
                for (key, kind) in attrs.iter() {
                    if let Some(obj_value) = attr_claim_object.get(key) {
                        if let Some(expr) = kind.build_expr(
                            key,
                            Some(obj_value),
                            default_namespace,
                            schema,
                            built_entities,
                        )? {
                            fields.insert(key.to_string(), expr);
                        }
                    };
                }

                if fields.is_empty() && !required {
                    Ok(None)
                } else {
                    Ok(Some(RestrictedExpression::new_record(fields)?))
                }
            },

            // Handle Set attributes
            Attribute::Set { required, element } => {
                let Some(attr_claim_value) = attr_claim_value else {
                    if *required {
                        return Err(BuildExprError::MissingSource(src_key.to_string()));
                    } else {
                        return Ok(None);
                    }
                };

                let claim =
                    attr_claim_value
                        .as_array()
                        .ok_or(KeyedJsonTypeError::type_mismatch(
                            src_key,
                            "array",
                            attr_claim_value,
                        ))?;

                let mut values = Vec::new();
                for (i, val) in claim.iter().enumerate() {
                    let attr_claim_value = Some(val);

                    let claim_name = i.to_string();
                    if let Some(expr) = element.build_expr(
                        &claim_name,
                        attr_claim_value,
                        default_namespace,
                        schema,
                        built_entities,
                    )? {
                        values.push(expr);
                    }
                }
                Ok(Some(RestrictedExpression::new_set(values)))
            },

            // Handle Entity attributes
            Attribute::Entity { required, name } => {
                // Check if the entity is in the schema
                if let Some((type_name, _type_schema)) = schema
                    .get_entity_schema(name, default_namespace)
                    .map_err(|e| BuildExprError::ParseTypeName(name.clone(), e))?
                {
                    build_entity(
                        src_key,
                        attr_claim_value,
                        built_entities,
                        type_name,
                        *required,
                    )
                } else if *required {
                    Err(BuildExprError::EntityNotInSchema(name.to_string()))
                } else {
                    Ok(None)
                }
            },

            // Handle Extension attributes
            Attribute::Extension { required, name } => {
                let Some(attr_claim_value) = attr_claim_value else {
                    if *required {
                        return Err(BuildExprError::MissingSource(src_key.to_string()));
                    } else {
                        return Ok(None);
                    }
                };

                let claim = attr_claim_value
                    .as_str()
                    .ok_or(KeyedJsonTypeError::type_mismatch(
                        src_key,
                        "string",
                        attr_claim_value,
                    ))?;
                let expr = match name.as_str() {
                    "ipaddr" => RestrictedExpression::new_ip(claim),
                    "decimal" => RestrictedExpression::new_decimal(claim),
                    name => RestrictedExpression::new_unknown(name),
                };
                Ok(Some(expr))
            },

            // Handle EntityOrCommon attributes
            Attribute::EntityOrCommon { required, name } => {
                // Check if we work with an entity, and if it is we handle it.
                // Because it can have special case when we map entity from `built_entities`.
                if let Some((type_name, _type_schema)) = schema
                    .get_entity_schema(name, default_namespace)
                    .map_err(|e| BuildExprError::ParseTypeName(name.clone(), e))?
                {
                    return build_entity(
                        src_key,
                        attr_claim_value,
                        built_entities,
                        type_name,
                        *required,
                    );
                };

                let Some(attr_claim_value) = attr_claim_value else {
                    if *required {
                        return Err(BuildExprError::MissingSource(src_key.to_string()));
                    } else {
                        return Ok(None);
                    }
                };

                if let Some((entity_type_name, attr)) = schema
                    .get_common_type(name, default_namespace)
                    .map_err(|e| BuildExprError::ParseTypeName(name.clone(), e))?
                {
                    // It is common type, so we can build the expression directly.
                    attr.build_expr(
                        src_key,
                        Some(attr_claim_value),
                        Some(entity_type_name.namespace().as_str()),
                        schema,
                        built_entities,
                    )
                } else if let Some(attr) = str_to_primitive_type(*required, name) {
                    attr.build_expr(
                        src_key,
                        Some(attr_claim_value),
                        default_namespace,
                        schema,
                        built_entities,
                    )
                } else if *required {
                    Err(BuildExprError::UnkownType(name.to_string()))
                } else {
                    Ok(None)
                }
            },
        }
    }
}

fn build_entity(
    src_key: &str,
    attr_claim_value: Option<&Value>,
    built_entities: &BuiltEntities,
    entity_type_name: EntityTypeName,
    is_required: bool,
) -> Result<Option<RestrictedExpression>, BuildExprError> {
    let entity_id = if let Some(entity_id) = built_entities.get(&entity_type_name) {
        entity_id
    } else if let Some(entity_id) = attr_claim_value.and_then(|v| v.as_str()) {
        entity_id
    } else if is_required {
        return Err(KeyedJsonTypeError::type_mismatch_optional(
            src_key,
            "string",
            attr_claim_value,
        )
        .into());
    } else {
        return Ok(None);
    };

    let type_id = EntityId::new(entity_id);
    let uid = EntityUid::from_type_name_and_id(entity_type_name, type_id);
    let expr = RestrictedExpression::new_entity_uid(uid);
    Ok(Some(expr))
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
    MissingBuiltEntityRef(String, String),
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
    pub fn value_type_name(value: Option<&Value>) -> &'static str {
        match value {
            Some(Value::Null) => "null",
            Some(Value::Bool(_)) => "bool",
            Some(Value::Number(_)) => "number",
            Some(Value::String(_)) => "string",
            Some(Value::Array(_)) => "array",
            Some(Value::Object(_)) => "object",
            None => "null",
        }
    }

    /// Constructs a `TypeMismatch` error with detailed information about the expected and actual types.
    pub fn type_mismatch(key: &str, expected_type_name: &str, got_value: &Value) -> Self {
        Self::type_mismatch_optional(key, expected_type_name, Some(got_value))
    }

    /// Constructs a `TypeMismatch` error with detailed information about the expected and actual types.
    pub fn type_mismatch_optional(
        key: &str,
        expected_type_name: &str,
        got_value: Option<&Value>,
    ) -> Self {
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
        authz::entity_builder::{BuildExprError, built_entities::BuiltEntities},
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
        let srs_key = "src_key";

        let expr = attr
            .build_expr(
                srs_key,
                src.get(srs_key),
                None,
                &schema,
                &BuiltEntities::default(),
            )
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
        let srs_key = "src_key";

        let expr = attr
            .build_expr(
                srs_key,
                src.get(srs_key),
                None,
                &schema,
                &BuiltEntities::default(),
            )
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
        let srs_key = "src_key";

        let expr = attr
            .build_expr(
                srs_key,
                src.get(srs_key),
                Some("Jans"),
                &schema,
                &BuiltEntities::default(),
            )
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
        let srs_key = "inner_attr";

        let expr = attr
            .build_expr(
                srs_key,
                Some(&json!(src)),
                Some("Jans"),
                &schema,
                &BuiltEntities::default(),
            )
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
        let src = HashMap::from([("attr1".to_string(), json!(["admin", "user"]))]);
        let srs_key = "attr1";

        let expr = attr
            .build_expr(
                srs_key,
                src.get(srs_key),
                Some("Jans"),
                &schema,
                &BuiltEntities::default(),
            )
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
        let srs_key = "src_key";

        let err = attr
            .build_expr(
                srs_key,
                src.get(srs_key),
                Some("Jans"),
                &schema,
                &BuiltEntities::default(),
            )
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
        let srs_key = "src_key";

        let expr = attr
            .build_expr(
                srs_key,
                src.get(srs_key),
                Some("Jans"),
                &schema,
                &BuiltEntities::default(),
            )
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
        let srs_key = "src_key";

        let expr = attr
            .build_expr(
                srs_key,
                src.get(srs_key),
                Some("Jans"),
                &schema,
                &BuiltEntities::default(),
            )
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
        let srs_key = "src_key";

        let err = attr
            .build_expr(
                srs_key,
                src.get(srs_key),
                Some("Jans"),
                &schema,
                &BuiltEntities::default(),
            )
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
        let srs_key = "src_key";

        let expr = attr
            .build_expr(
                srs_key,
                src.get(srs_key),
                Some("Jans"),
                &schema,
                &BuiltEntities::default(),
            )
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
        let srs_key = "src_key";

        let expr = attr
            .build_expr(
                srs_key,
                src.get(srs_key),
                Some("Jans"),
                &schema,
                &BuiltEntities::default(),
            )
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
        let src: HashMap<String, serde_json::Value> = HashMap::new();
        let srs_key = "client_id";

        let expr = attr
            .build_expr(
                srs_key,
                src.get(srs_key),
                Some("Jans"),
                &schema,
                &BuiltEntities::default(),
            )
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
        let srs_key = "src_key";

        let err = attr
            .build_expr(
                srs_key,
                src.get(srs_key),
                Some("Jans"),
                &schema,
                &BuiltEntities::default(),
            )
            .expect_err("should error");
        assert!(
            matches!(err, BuildExprError::TypeMismatch(_)),
            "should error due to type mismatch but got: {:?}",
            err
        );
    }
}
