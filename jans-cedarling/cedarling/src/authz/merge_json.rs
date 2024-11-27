use serde_json::Value;

#[derive(Debug, thiserror::Error)]
pub enum MergeError {
    #[error("Failed to merge JSON objects due to conflicting keys: {0}")]
    KeyConflict(String),
}

pub fn merge_json_values(mut base: Value, other: Value) -> Result<Value, MergeError> {
    if let (Some(base_map), Some(additional_map)) = (base.as_object_mut(), other.as_object()) {
        for (key, value) in additional_map {
            if base_map.contains_key(key) {
                return Err(MergeError::KeyConflict(key.clone()));
            }
            base_map.insert(key.clone(), value.clone());
        }
    }
    Ok(base)
}

#[cfg(test)]
mod test {
    use serde_json::json;

    use crate::authz::merge_json::MergeError;

    use super::merge_json_values;

    #[test]
    fn can_merge_json_objects() {
        let obj1 = json!({ "a": 1, "b": 2 });
        let obj2 = json!({ "c": 3, "d": 4 });
        let expected = json!({"a": 1, "b": 2, "c": 3, "d": 4});

        let result = merge_json_values(obj1, obj2).expect("Should merge JSON objects");

        assert_eq!(result, expected);
    }

    #[test]
    fn errors_on_same_keys() {
        // Test for only two objects
        let obj1 = json!({ "a": 1, "b": 2 });
        let obj2 = json!({ "b": 3, "c": 4 });
        let result = merge_json_values(obj1, obj2);

        assert!(
            matches!(result, Err(MergeError::KeyConflict(key)) if key.as_str() == "b"),
            "Expected an error due to conflicting keys"
        );
    }
}
