use serde_json::Value;
use std::mem::size_of;

pub fn calculate_memory_usage(value: &Value) -> usize {
    let mut total = size_of::<Value>(); // Root stack size (32 bytes)
    add_heap_usage(value, &mut total);
    total
}

fn add_heap_usage(value: &Value, total: &mut usize) {
    match value {
        Value::String(s) => {
            // Use capacity for String (includes unused allocated space)
            *total += size_of::<String>();
            *total += s.capacity();
        },
        Value::Array(arr) => {
            // Use capacity for Vec (includes unused allocated space)
            *total += size_of::<Vec<Value>>() + // Vec header (24 bytes)
                    arr.capacity() * size_of::<Value>(); // Elements

            for item in arr {
                add_heap_usage(item, total);
            }
        },
        Value::Object(map) => {
            // BTreeMap: capacity is not accessible, so use length
            for (key, value) in map {
                // Use capacity for String keys
                *total += size_of::<String>() + // String header (24 bytes)
                        key.capacity(); // Key data

                // Value storage in map node
                *total += size_of::<Value>(); // Value in node

                add_heap_usage(value, total);
            }
        },
        // next values are alocated on the stack and size taken into account
        Value::Number(_) => {},
        Value::Null => {},
        Value::Bool(_) => {},
    }
}
