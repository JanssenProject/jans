// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::Config;
use crate::SparKV;

#[cfg(test)]
fn json_value_size(value: &serde_json::Value) -> usize {
    std::mem::size_of::<serde_json::Value>()
        + match value {
            serde_json::Value::Null => 0,
            serde_json::Value::Bool(_) => 0,
            serde_json::Value::Number(_) => 0, // Incorrect if arbitrary_precision is enabled. oh well
            serde_json::Value::String(s) => s.capacity(),
            serde_json::Value::Array(a) => {
                a.iter().map(json_value_size).sum::<usize>()
                    + a.capacity() * std::mem::size_of::<serde_json::Value>()
            },
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

fn second_json() -> serde_json::Value {
    serde_json::json!({
        "name" : "second_json",
        "compile_kind": 0,
        "config": 5533035641051825893usize,
        "declared_features": ["bstr", "bytes", "default", "inline", "serde", "text", "unicode", "unicode-segmentation"],
        "deps": [],
        "features": ["default", "text"],
        "local": [
            {
                "CheckDepInfo": {
                    "checksum": false,
                    "dep_info": "debug/.fingerprint/utterly-different-0a6664d898c8f8a5/dep-lib-utterly-different"
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
fn simple_serde_json() {
    let config: Config = Config::new();
    let mut sparkv =
        SparKV::<serde_json::Value>::with_config_and_sizer(config, Some(json_value_size));
    let json = first_json();
    sparkv.set("first", json.clone(), &[]).unwrap();
    let stored_first = sparkv.get("first").unwrap();
    assert_eq!(&json, stored_first);
}

#[test]
fn type_serde_json() {
    let config: Config = Config::new();
    let mut sparkv =
        SparKV::<serde_json::Value>::with_config_and_sizer(config, Some(json_value_size));
    let json = first_json();
    sparkv.set("first", json.clone(), &[]).unwrap();

    // now make sure it's actually stored as the value, not as a String
    let kv = sparkv.get_item("first").unwrap();
    use std::any::{Any, TypeId};
    assert_eq!(kv.value.type_id(), TypeId::of::<serde_json::Value>());
}

#[test]
fn fails_size_calculator() {
    // create this first, so we know what item size is too large
    let json = first_json();

    let mut config: Config = Config::new();
    // set item size to something smaller than item
    config.max_item_size = json_value_size(&json) / 2;
    let mut sparkv =
        SparKV::<serde_json::Value>::with_config_and_sizer(config, Some(json_value_size));

    let should_be_error = sparkv.set("first", json.clone(), &[]);
    assert_eq!(should_be_error, Err(crate::Error::ItemSizeExceeded));
}

#[test]
fn two_json_items() {
    let mut sparkv = SparKV::<serde_json::Value>::new();
    sparkv.set("first", first_json(), &[]).unwrap();
    sparkv.set("second", second_json(), &[]).unwrap();

    let fj = sparkv.get("first").unwrap();
    assert_eq!(
        fj.pointer("/name").unwrap(),
        &serde_json::Value::String("first_json".into())
    );

    let sj = sparkv.get("second").unwrap();
    assert_eq!(
        sj.pointer("/name").unwrap(),
        &serde_json::Value::String("second_json".into())
    );
}

#[test]
fn drain_all_json_items() {
    let mut sparkv = SparKV::<serde_json::Value>::new();
    sparkv.set("first", first_json(), &[]).unwrap();
    sparkv.set("second", second_json(), &[]).unwrap();

    let all_items = sparkv.drain();
    let all_values = all_items.map(|(_, v)| v).collect::<Vec<_>>();
    assert_eq!(all_values, vec![first_json(), second_json()]);

    assert!(sparkv.is_empty(), "sparkv not empty");
}

#[test]
fn rc_json_items() {
    use std::rc::Rc;
    let mut sparkv = SparKV::<Rc<serde_json::Value>>::new();
    sparkv.set("first", Rc::new(first_json()), &[]).unwrap();
    sparkv.set("second", Rc::new(second_json()), &[]).unwrap();

    let all_items = sparkv.drain();
    let all_values = all_items.map(|(_, v)| v).collect::<Vec<_>>();
    assert_eq!(all_values, vec![
        Rc::new(first_json()),
        Rc::new(second_json())
    ]);

    assert!(sparkv.is_empty(), "sparkv not empty");
}
