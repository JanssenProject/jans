use serde_json;
use crate::Config;
use crate::SparKV;

#[cfg(test)]
fn json_value_size(value : &serde_json::Value) -> usize {
    std::mem::size_of::<serde_json::Value>()
        + match value {
            serde_json::Value::Null => 0,
            serde_json::Value::Bool(_) => 0,
            serde_json::Value::Number(_) => 0, // Incorrect if arbitrary_precision is enabled. oh well
            serde_json::Value::String(s) => s.capacity(),
            serde_json::Value::Array(a) =>
                a.iter().map(json_value_size).sum::<usize>()
                + a.capacity() * std::mem::size_of::<serde_json::Value>(),
            serde_json::Value::Object(o) => o
                .iter()
                .map(|(k, v)| {
                    std::mem::size_of::<String>()
                        + k.capacity()
                        + json_value_size(v)
                        + std::mem::size_of::<usize>() * 3 // As a crude approximation, I pretend each map entry has 3 words of overhead
                })
                .sum(),
        }
}

fn first_json() -> serde_json::Value {
    serde_json::json!({
        "name" : "first_json",
        "compile_kind": 0,
        "config": 3355035640151825893usize,
        "declared_features": ["bstr", "bytes", "default", "inline", "serde", "text", "unicode", "unicode-segmentation"],
        "deps": [],
        "features": ["default", "text"],
        "local": [
            {
                "CheckDepInfo": {
                    "checksum": false,
                    "dep_info": "debug/.fingerprint/similar-056a66f4ad898c88/dep-lib-similar"
                }
            }
        ],
        "metadata": 943206097653546126i64,
        "path": 7620609427446831929u64,
        "profile": 10243973527296709326usize,
        "rustc": 11594289678289209806usize,
        "rustflags": [
            "-C",
            "link-arg=-fuse-ld=/usr/bin/mold"
        ],
        "target": 15605724903113465739u64
    })
}

#[test]
fn fails_size_calculator() {
    // create this first, so we know what item size is too large
    let json = first_json();

    let mut config: Config = Config::new();
    // set item size to something smaller than item
    config.max_item_size = json_value_size(&json) / 2;
    let mut sparkv = SparKV::<serde_json::Value>::with_config_and_sizer(config, Some(json_value_size));

    let should_be_error = sparkv.set("first", json.clone());
    assert_eq!(should_be_error, Err(crate::Error::ItemSizeExceeded));
}
