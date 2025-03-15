// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedar_policy::{ExpressionConstructionError, RestrictedExpression};
use serde_json::Value;
use std::collections::HashMap;
use std::net::IpAddr;
use std::str::FromStr;

use crate::common::PartitionResult;

/// Converts a [`Value`] to a [`RestrictedExpression`]
pub fn value_to_expr(
    value: &Value,
) -> Result<Option<RestrictedExpression>, Vec<ExpressionConstructionError>> {
    let expr = match value {
        Value::Null => return Ok(None),
        Value::Bool(val) => RestrictedExpression::new_bool(*val),
        Value::Number(val) => {
            if let Some(int) = val.as_i64() {
                RestrictedExpression::new_long(int)
            } else if let Some(float) = val.as_f64() {
                RestrictedExpression::new_decimal(float.to_string())
            } else {
                return Ok(None);
            }
        },
        Value::String(val) => {
            if IpAddr::from_str(val).is_ok() {
                RestrictedExpression::new_ip(val)
            } else {
                RestrictedExpression::new_string(val.to_string())
            }
        },
        Value::Array(values) => {
            let (values, errors): (Vec<_>, Vec<_>) =
                values.iter().map(value_to_expr).partition_result();

            if !errors.is_empty() {
                return Err(errors.into_iter().flatten().collect());
            }

            let values: Vec<_> = values.into_iter().flatten().collect();
            RestrictedExpression::new_set(values)
        },
        Value::Object(map) => {
            let (fields, errs): (Vec<_>, Vec<_>) = map
                .iter()
                .map(|(key, val)| value_to_expr(val).map(|expr| (key.to_string(), expr)))
                .partition_result();

            if !errs.is_empty() {
                return Err(errs.into_iter().flatten().collect());
            }

            let fields = fields
                .into_iter()
                .filter_map(|(key, val)| val.map(|val| (key, val)))
                .collect::<HashMap<String, RestrictedExpression>>();
            RestrictedExpression::new_record(fields).map_err(|e| vec![e])?
        },
    };
    Ok(Some(expr))
}

#[cfg(test)]
mod test {
    use super::*;
    use cedar_policy::Entity;
    use cedar_policy::EntityUid;
    use cedar_policy::EvalResult;
    use serde_json::Value;
    use serde_json::json;
    use std::collections::HashMap;
    use std::collections::HashSet;

    #[test]
    pub fn test_value_to_expr() {
        let attrs = HashMap::from_iter(
            [
                ("test_null", value_to_expr(&json!(Value::Null)).unwrap()),
                ("test_bool", value_to_expr(&json!(true)).unwrap()),
                ("test_long", value_to_expr(&json!(521)).unwrap()),
                ("test_decimal", value_to_expr(&json!(12.5)).unwrap()),
                ("test_str", value_to_expr(&json!("some str")).unwrap()),
                ("test_set", value_to_expr(&json!(["a", 1])).unwrap()),
                (
                    "test_record",
                    value_to_expr(&json!({"a": 1, "b": "b"})).unwrap(),
                ),
            ]
            .into_iter()
            .flat_map(|(key, expr)| expr.map(|expr| (key.to_string(), expr))),
        );

        let entity = Entity::new(
            EntityUid::from_str("Test::\"test\"").expect("should parse EntityUid"),
            attrs,
            HashSet::new(),
        )
        .expect("should create entity");

        assert!(matches!(
            entity.attr("test_bool").expect("entity should have a `test_bool` attribute").expect("should be a valid value"),
            EvalResult::Bool(ref val)
                if *val == true,
        ));

        assert!(matches!(
            entity.attr("test_long").expect("entity should have a `test_long` attribute").expect("should be a valid value"),
            EvalResult::Long(ref val)
                if *val == 521,
        ));

        assert!(matches!(
            entity.attr("test_decimal").expect("entity should have a `test_decimal` attribute").expect("should be a valid value"),
            EvalResult::ExtensionValue(ref val)
                if *val == "decimal(\"12.5\")",
        ));

        assert!(matches!(
            entity.attr("test_str").expect("entity should have a `test_str` attribute").expect("should be a valid value"),
            EvalResult::String(ref val)
                if *val == "some str",
        ));

        assert!(matches!(
            entity.attr("test_set").expect("entity should have a `test_set` attribute").expect("should be a valid value"),
            EvalResult::Set(set)
                if
                    set.len() == 2 &&
                    set.contains(&EvalResult::String("a".into())) &&
                    set.contains(&EvalResult::Long(1))
        ));

        assert!(matches!(
            entity.attr("test_record").expect("entity should have a `test_record` attribute").expect("should be a valid value"),
            EvalResult::Record(record)
                if
                    record.len() == 2 &&
                    record.get("a") == Some(&EvalResult::Long(1)) &&
                    record.get("b") == Some(&EvalResult::String("b".into()))
        ));
    }
}
