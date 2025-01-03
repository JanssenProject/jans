// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedar_policy::{EntityAttrEvaluationError, ExpressionConstructionError, RestrictedExpression};
use serde_json::Value;

use super::*;
use crate::ResourceData;

impl EntityBuilder {
    pub fn build_resource_entity(
        &self,
        resource: &ResourceData,
    ) -> Result<Entity, BuildResourceEntityError> {
        // Get entity namespace and type

        // Build entity attributes from payload
        let mut entity_attrs = HashMap::new();
        for (key, val) in resource.payload.iter() {
            if let Some(expr) = value_to_restricted_expr(val)? {
                entity_attrs.insert(key.to_string(), expr);
            }
        }

        // Build cedar entity
        let entity_type_name = EntityTypeName::from_str(&resource.resource_type)
            .map_err(BuildEntityError::ParseEntityTypeName)?;
        let entity_id = EntityId::from_str(&resource.id).expect("expected infallible");
        let entity_uid = EntityUid::from_type_name_and_id(entity_type_name, entity_id);
        Ok(Entity::new(entity_uid, entity_attrs, HashSet::new())?)
    }
}

fn value_to_restricted_expr(
    value: &Value,
) -> Result<Option<RestrictedExpression>, BuildResourceEntityError> {
    let expr = match value {
        Value::Null => return Ok(None),
        Value::Bool(value) => RestrictedExpression::new_bool(*value),
        Value::Number(ref number) => RestrictedExpression::new_long(
            number
                .as_i64()
                .ok_or(JsonTypeError::type_mismatch("i64", &value))?,
        ),
        Value::String(str) => RestrictedExpression::new_string(str.to_string()),
        Value::Array(vec) => {
            let mut values = Vec::new();
            for val in vec.into_iter() {
                if let Some(expr) = value_to_restricted_expr(val)? {
                    values.push(expr);
                }
            }
            RestrictedExpression::new_set(values)
        },
        Value::Object(map) => {
            let mut fields = HashMap::new();
            for (key, val) in map.into_iter() {
                if let Some(expr) = value_to_restricted_expr(val)? {
                    fields.insert(key.to_string(), expr);
                }
            }
            RestrictedExpression::new_record(fields)?
        },
    };

    Ok(Some(expr))
}

#[derive(Debug, thiserror::Error)]
pub enum BuildResourceEntityError {
    #[error(transparent)]
    BuildEntity(#[from] BuildEntityError),
    #[error(transparent)]
    TypeMismatch(#[from] JsonTypeError),
    #[error(transparent)]
    ExpressionConstructExpression(#[from] ExpressionConstructionError),
    #[error(transparent)]
    EntityAttrEvaluationError(#[from] EntityAttrEvaluationError),
}

#[derive(Debug, thiserror::Error)]
#[error("JSON value type mismatch: expected '{expected_type}', but found '{actual_type}'")]
pub struct JsonTypeError {
    pub expected_type: String,
    pub actual_type: String,
}

impl JsonTypeError {
    /// Returns the JSON type name of the given value.
    pub fn value_type_name(value: &Value) -> String {
        match value {
            Value::Null => "null".to_string(),
            Value::Bool(_) => "bool".to_string(),
            Value::Number(_) => "number".to_string(),
            Value::String(_) => "string".to_string(),
            Value::Array(_) => "array".to_string(),
            Value::Object(_) => "object".to_string(),
        }
    }

    /// Constructs a `TypeMismatch` error with detailed information about the expected and actual types.
    pub fn type_mismatch(expected_type_name: &str, got_value: &Value) -> Self {
        let got_value_type_name = Self::value_type_name(got_value);

        Self {
            expected_type: expected_type_name.to_string(),
            actual_type: got_value_type_name,
        }
    }
}

#[cfg(test)]
mod test {
    use super::super::*;
    use super::*;
    use crate::common::cedar_schema::new_cedar_json::CedarSchemaJson;
    use cedar_policy::EvalResult;
    use serde_json::json;

    fn test_schema() -> CedarSchemaJson {
        serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": {
                "commonTypes": {
                    "Url": {
                        "type": "Record",
                        "attributes": {
                            "scheme": { "type": "String" },
                            "path": { "type": "String" },
                            "domain": { "type": "String" },
                        },
                    },
                },
            "entityTypes": {
                "Role": {},
                "HttpRequest": {
                    "shape": {
                        "type": "Record",
                        "attributes":  {
                            "url": { "type": "EntityOrCommon", "name": "Url" },
                    },
                }
            }}}
        }))
        .expect("should successfully create test schema")
    }

    #[test]
    fn can_build_resource_entity() {
        let schema = test_schema();
        let issuers = HashMap::from([("test_iss".into(), TrustedIssuer::default())]);
        let builder = EntityBuilder::new(issuers, schema, EntityNames::default());
        let resource_data = ResourceData {
            resource_type: "HttpRequest".to_string(),
            id: "request-123".to_string(),
            payload: HashMap::from([(
                "url".to_string(),
                json!({"scheme": "https", "domain": "test.com", "path": "/"}),
            )]),
        };
        let entity = builder
            .build_resource_entity(&resource_data)
            .expect("expected to build resource entity");

        let url = entity
            .attr("url")
            .expect("entity must have an `url` attribute")
            .unwrap();
        if let EvalResult::Record(ref record) = url {
            assert_eq!(record.len(), 3);
            assert_eq!(
                record
                    .get("scheme")
                    .expect("expected `url` to have a `scheme` attribute"),
                &EvalResult::String("https".to_string())
            );
            assert_eq!(
                record
                    .get("domain")
                    .expect("expected `url` to have a `domain` attribute"),
                &EvalResult::String("test.com".to_string())
            );
            assert_eq!(
                record
                    .get("path")
                    .expect("expected `url` to have a `path` attribute"),
                &EvalResult::String("/".to_string())
            );
        } else {
            panic!(
                "expected the attribute `url` to be a record, got: {:?}",
                url
            );
        }
    }
}
