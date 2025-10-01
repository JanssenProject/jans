/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use serde_json::{Map, Value};

// Recursively sort the fields of JSON objects
// we need deterministic ordering of the JSON objects to be able to compare them
fn sort_json(value: &mut Value) {
    if let Value::Object(map) = value {
        // in serde_json, maps are opdered if used feature "preserve_order", it is enabled by default
        let mut sorted_map = Map::new();

        // Sort the map by key and recursively sort nested values
        let mut entries: Vec<_> = map.iter_mut().collect();
        entries.sort_by_key(|(k, _)| k.to_owned());

        for (key, val) in entries {
            sort_json(val); // Recursively sort nested JSON objects
            sorted_map.insert(key.to_owned(), val.clone());
        }

        *map = sorted_map;
    } else if let Value::Array(arr) = value {
        arr.sort_by_key(|a| a.to_string()); // Sort the array elements

        for item in arr.iter_mut() {
            sort_json(item); // Sort each element in the array
        }
    }
}

/// Trait to sort JSON objects
pub trait SortedJson {
    fn sorted(self) -> Self;
}

impl SortedJson for Value {
    fn sorted(self) -> Self {
        let mut value = self;
        sort_json(&mut value);
        value
    }
}
