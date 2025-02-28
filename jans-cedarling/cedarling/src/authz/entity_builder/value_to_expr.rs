// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedar_policy::RestrictedExpression;
use serde_json::Value;
use std::net::IpAddr;
use std::str::FromStr;

/// Converts a [`Value`] to a [`RestrictedExpression`]
pub fn value_to_expr(value: &Value) -> Option<RestrictedExpression> {
    let expr = match value {
        Value::Null => return None,
        Value::Bool(val) => RestrictedExpression::new_bool(*val),
        Value::Number(val) => {
            if let Some(int) = val.as_i64() {
                RestrictedExpression::new_long(int)
            } else if let Some(float) = val.as_f64() {
                RestrictedExpression::new_decimal(float.to_string())
            } else {
                return None;
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
            let exprs = values.iter().filter_map(value_to_expr).collect::<Vec<_>>();
            RestrictedExpression::new_set(exprs)
        },
        Value::Object(map) => {
            let fields = map
                .iter()
                .filter_map(|(key, val)| value_to_expr(val).map(|expr| (key.to_string(), expr)))
                .collect::<Vec<_>>();
            // TODO: handle error
            RestrictedExpression::new_record(fields).expect("there shouldn't be duplicate keys")
        },
    };
    Some(expr)
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
                ("test_null", value_to_expr(&json!(Value::Null))),
                ("test_bool", value_to_expr(&json!(true))),
                ("test_long", value_to_expr(&json!(521))),
                ("test_decimal", value_to_expr(&json!(12.5))),
                ("test_str", value_to_expr(&json!("some str"))),
                ("test_set", value_to_expr(&json!(["a", 1]))),
                ("test_record", value_to_expr(&json!({"a": 1, "b": "b"}))),
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
