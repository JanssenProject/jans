// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::*;

#[test]
fn test_sparkv_config() {
    let config: Config = Config::new();
    assert_eq!(config.max_items, 10_000);
    assert_eq!(config.max_item_size, 500_000);
    assert_eq!(config.max_ttl, Duration::seconds(60 * 60));
}

#[test]
fn test_sparkv_new_with_config() {
    let config: Config = Config::new();
    let sparkv = SparKV::<String>::with_config(config);
    assert_eq!(sparkv.config, config);
}

#[test]
fn test_len_is_empty() {
    let mut sparkv = SparKV::<String>::new();
    assert_eq!(sparkv.len(), 0);
    assert!(sparkv.is_empty());

    _ = sparkv.set("keyA", "value".to_string(), &[]);
    assert_eq!(sparkv.len(), 1);
    assert!(!sparkv.is_empty());
}

#[test]
fn test_set_get() {
    let mut sparkv = SparKV::<String>::new();
    _ = sparkv.set("keyA", "value".into(), &[]);
    assert_eq!(sparkv.get("keyA"), Some(&String::from("value")));
    assert_eq!(sparkv.expiries.len(), 1);

    // Overwrite the value
    _ = sparkv.set("keyA", "value2".into(), &[]);
    assert_eq!(sparkv.get("keyA"), Some(&String::from("value2")));
    assert_eq!(sparkv.expiries.len(), 2);

    assert!(sparkv.get("non-existent").is_none());
}

#[test]
fn test_get_item() {
    let mut sparkv = SparKV::new();
    let item = KvEntry::new("keyARaw", "value99", Duration::seconds(1));
    sparkv.data.insert(item.key.clone(), item);
    let get_result = sparkv.get_item("keyARaw");
    let unwrapped = get_result.unwrap();

    assert!(get_result.is_some());
    assert_eq!(unwrapped.key, "keyARaw");
    assert_eq!(unwrapped.value, "value99");

    assert!(sparkv.get_item("non-existent").is_none());
}

#[test]
fn test_get_item_return_none_if_expired() {
    let mut sparkv = SparKV::new();
    _ = sparkv.set_with_ttl("key", "value", Duration::microseconds(40), &[]);
    assert_eq!(sparkv.get("key"), Some(&"value"));

    std::thread::sleep(std::time::Duration::from_micros(80));
    assert_eq!(sparkv.get("key"), None);
}

#[test]
fn test_set_should_fail_if_capacity_exceeded() {
    let mut config: Config = Config::new();
    config.max_items = 2;

    let mut sparkv = SparKV::<String>::with_config(config);
    let mut set_result = sparkv.set("keyA", "value".to_string(), &[]);
    assert!(set_result.is_ok());
    assert_eq!(sparkv.get("keyA"), Some(&String::from("value")));

    set_result = sparkv.set("keyB", "value2".to_string(), &[]);
    assert!(set_result.is_ok());

    set_result = sparkv.set("keyC", "value3".to_string(), &[]);
    assert!(set_result.is_err());
    assert_eq!(set_result.unwrap_err(), Error::CapacityExceeded);
    assert!(sparkv.get("keyC").is_none());

    // Overwrite existing key should not err
    set_result = sparkv.set("keyB", "newValue1234".to_string(), &[]);
    assert!(set_result.is_ok());
    assert_eq!(sparkv.get("keyB"), Some(&String::from("newValue1234")));
}

#[test]
fn memsize_item_capacity_exceeded() {
    let value: String = "jay".into();

    let mut config: Config = Config::new();
    config.max_item_size = std::mem::size_of_val(&value) / 2;
    let mut sparkv = SparKV::<String>::with_config(config);

    let error = sparkv.set("blue", value, &[]);
    assert_eq!(error, Err(crate::Error::ItemSizeExceeded));
}

#[test]
fn custom_item_capacity_exceeded() {
    let mut config: Config = Config::new();
    config.max_item_size = 20;
    let mut sparkv = SparKV::<&str>::with_config_and_sizer(config, Some(|s| s.len()));

    assert_eq!(Ok(()), sparkv.set("short", "value", &[]));
    assert_eq!(
        Err(crate::Error::ItemSizeExceeded),
        sparkv.set("long", "This is a value that exceeds 20 characters", &[])
    );
}

#[test]
fn test_set_with_ttl() {
    let mut sparkv = SparKV::<String>::new();
    _ = sparkv.set("longest", "value".into(), &[]);
    _ = sparkv.set_with_ttl("longer", "value".into(), Duration::seconds(2), &[]);
    _ = sparkv.set_with_ttl("shorter", "value".into(), Duration::seconds(1), &[]);

    assert_eq!(sparkv.get("longer"), Some(&String::from("value")));
    assert_eq!(sparkv.get("shorter"), Some(&String::from("value")));
    assert!(
        sparkv.get_item("longer").unwrap().expired_at
            > sparkv.get_item("shorter").unwrap().expired_at
    );
    assert!(
        sparkv.get_item("longest").unwrap().expired_at
            > sparkv.get_item("longer").unwrap().expired_at
    );
}

#[test]
fn test_ensure_max_ttl() {
    let mut config: Config = Config::new();
    config.max_ttl = Duration::seconds(3600);
    config.default_ttl = Duration::seconds(5000);
    let mut sparkv = SparKV::<String>::with_config(config);

    let set_result_long_def =
        sparkv.set("default is longer than max", "should fail".to_string(), &[]);
    assert!(set_result_long_def.is_err());
    assert_eq!(set_result_long_def.unwrap_err(), Error::TTLTooLong);

    let set_result_ok = sparkv.set_with_ttl("shorter", "ok".into(), Duration::seconds(3599), &[]);
    assert!(set_result_ok.is_ok());

    let set_result_ok_2 = sparkv.set_with_ttl("exact", "ok".into(), Duration::seconds(3600), &[]);
    assert!(set_result_ok_2.is_ok());

    let set_result_not_ok =
        sparkv.set_with_ttl("not", "not ok".into(), Duration::seconds(33601), &[]);
    assert!(set_result_not_ok.is_err());
    assert_eq!(set_result_not_ok.unwrap_err(), Error::TTLTooLong);
}

#[test]
fn test_delete() {
    let mut sparkv = SparKV::<String>::new();
    _ = sparkv.set("keyA", "value".to_string(), &[]);
    assert_eq!(sparkv.get("keyA"), Some(&String::from("value")));
    assert_eq!(sparkv.expiries.len(), 1);

    let deleted_value = sparkv.pop("keyA");
    assert_eq!(deleted_value, Some(String::from("value")));
    assert!(sparkv.get("keyA").is_none());
    assert_eq!(sparkv.expiries.len(), 1); // it does not delete
}

#[test]
fn test_clear_expired() {
    let mut config: Config = Config::new();
    config.auto_clear_expired = false;
    let mut sparkv = SparKV::with_config(config);
    _ = sparkv.set_with_ttl("not-yet-expired", "v", Duration::seconds(90), &[]);
    _ = sparkv.set_with_ttl("expiring", "value", Duration::milliseconds(1), &[]);
    _ = sparkv.set_with_ttl("not-expired", "value", Duration::seconds(60), &[]);
    std::thread::sleep(std::time::Duration::from_millis(2));
    assert_eq!(sparkv.len(), 3);

    let cleared_count = sparkv.clear_expired();
    assert_eq!(cleared_count, 1);
    assert_eq!(sparkv.len(), 2);

    assert_eq!(sparkv.clear_expired(), 0);
}

#[test]
fn test_clear_expired_with_overwritten_key() {
    let mut config: Config = Config::new();
    config.auto_clear_expired = false;
    let mut sparkv = SparKV::with_config(config);
    _ = sparkv.set_with_ttl("no-longer", "value", Duration::milliseconds(1), &[]);
    _ = sparkv.set_with_ttl("no-longer", "v", Duration::seconds(90), &[]);
    _ = sparkv.set_with_ttl("not-expired", "value", Duration::seconds(60), &[]);
    std::thread::sleep(std::time::Duration::from_millis(2));
    assert_eq!(sparkv.expiries.len(), 3); // overwriting key does not update expiries
    assert_eq!(sparkv.len(), 2);

    let cleared_count = sparkv.clear_expired();
    assert_eq!(cleared_count, 0); // no longer expiring
    assert_eq!(sparkv.expiries.len(), 2); // should have cleared the expiries
    assert_eq!(sparkv.len(), 2); // but not actually deleting
}

#[test]
fn test_capacity_disabled() {
    let mut config: Config = Config::new();
    config.max_items = 0; // disabled
    let mut sparkv = SparKV::<String>::with_config(config);

    // Should be able to add unlimited items
    for i in 0..1000 {
        sparkv
            .set(&format!("key{}", i), format!("value{}", i), &[])
            .unwrap();
    }
    assert_eq!(sparkv.len(), 1000);
}

#[test]
fn test_item_size_disabled() {
    let mut config: Config = Config::new();
    config.max_item_size = 0; // disabled
    let mut sparkv = SparKV::<String>::with_config(config);

    // Should be able to add any size
    sparkv.set("huge", "a".repeat(1_000_000), &[]).unwrap();
    assert_eq!(sparkv.len(), 1);
}

#[test]
fn test_clear_expired_with_auto_clear_expired_enabled() {
    let mut config: Config = Config::new();
    config.auto_clear_expired = true; // explicitly setting it to true
    let mut sparkv = SparKV::<String>::with_config(config);
    _ = sparkv.set_with_ttl("no-longer", "value".into(), Duration::milliseconds(1), &[]);
    _ = sparkv.set_with_ttl("no-longer", "v".into(), Duration::seconds(90), &[]);
    std::thread::sleep(std::time::Duration::from_millis(2));
    _ = sparkv.set_with_ttl("not-expired", "value".into(), Duration::seconds(60), &[]);
    assert_eq!(sparkv.expiries.len(), 2); // diff from above, because of auto clear
    assert_eq!(sparkv.len(), 2);

    // auto clear 2
    _ = sparkv.set_with_ttl("new-", "value".into(), Duration::seconds(60), &[]);
    assert_eq!(sparkv.expiries.len(), 3); // should have cleared the expiries
    assert_eq!(sparkv.len(), 3); // but not actually deleting
}

#[test]
fn iterator() {
    let mut sparkv = SparKV::<String>::new();
    sparkv.set("this", "town".into(), &[]).unwrap();
    sparkv.set("woo", "oooo".into(), &[]).unwrap();
    sparkv.set("is", "coming".into(), &[]).unwrap();
    sparkv.set("like", "a".into(), &[]).unwrap();
    sparkv.set("ghost", "town".into(), &[]).unwrap();
    sparkv.set("oh", "yeah".into(), &[]).unwrap();

    let iter = sparkv.iter();
    assert!(!sparkv.is_empty(), "sparkv should be not empty");
    assert_eq!(sparkv.get("ghost").unwrap(), "town");

    let (keys, values): (Vec<_>, Vec<_>) = iter.unzip();
    assert_eq!(keys, vec!["ghost", "is", "like", "oh", "this", "woo"]);
    assert_eq!(values, vec!["town", "coming", "a", "yeah", "town", "oooo"]);
}

#[test]
fn drain() {
    let mut sparkv = SparKV::<String>::new();
    sparkv.set("this", "town".into(), &[]).unwrap();
    sparkv.set("woo", "oooo".into(), &[]).unwrap();
    sparkv.set("is", "coming".into(), &[]).unwrap();
    sparkv.set("like", "a".into(), &[]).unwrap();
    sparkv.set("ghost", "town".into(), &[]).unwrap();
    sparkv.set("oh", "yeah".into(), &[]).unwrap();

    let iter = sparkv.drain();
    assert!(sparkv.is_empty(), "sparkv should be empty");

    let (keys, values): (Vec<_>, Vec<_>) = iter.unzip();
    assert_eq!(keys, vec!["ghost", "is", "like", "oh", "this", "woo"]);
    assert_eq!(values, vec!["town", "coming", "a", "yeah", "town", "oooo"]);
}

#[test]
fn test_get_oldest_key_by_expiration() {
    let mut sparkv = SparKV::<String>::new();

    // Add items with different TTLs
    sparkv
        .set_with_ttl("short", "short_value".into(), Duration::seconds(1), &[])
        .unwrap();
    sparkv
        .set_with_ttl("medium", "medium_value".into(), Duration::seconds(5), &[])
        .unwrap();
    sparkv
        .set_with_ttl("long", "long_value".into(), Duration::seconds(10), &[])
        .unwrap();

    // The oldest (earliest to expire) should be "short"
    let oldest = sparkv.get_oldest_key_by_expiration();
    assert!(oldest.is_some());
    assert_eq!(oldest.unwrap().key, "short");
}

#[test]
fn test_get_keys() {
    let mut sparkv = SparKV::<String>::new();
    assert_eq!(sparkv.get_keys(), Vec::<String>::new());

    sparkv.set("key1", "value1".into(), &[]).unwrap();
    sparkv.set("key2", "value2".into(), &[]).unwrap();
    sparkv.set("key3", "value3".into(), &[]).unwrap();

    let mut keys = sparkv.get_keys();
    keys.sort(); // BTreeMap returns keys in sorted order

    assert_eq!(keys, vec!["key1", "key2", "key3"]);
}

#[test]
fn test_contains_key() {
    let mut sparkv = SparKV::<String>::new();

    assert!(!sparkv.contains_key("key1"));

    sparkv.set("key1", "value1".into(), &[]).unwrap();
    assert!(sparkv.contains_key("key1"));
    assert!(!sparkv.contains_key("key2"));

    sparkv.set("key2", "value2".into(), &[]).unwrap();
    assert!(sparkv.contains_key("key2"));

    sparkv.pop("key1");
    assert!(!sparkv.contains_key("key1"));
    assert!(sparkv.contains_key("key2"));
}

#[test]
fn test_add_additional_index() {
    let mut sparkv = SparKV::<String>::new();

    // Add item with initial index
    sparkv
        .set("key1", "value1".into(), &["index1".to_string()])
        .unwrap();

    // Add additional index
    sparkv.add_additional_index("key1", "index2");

    // Get by both indexes
    let index1_results: Vec<_> = sparkv.get_by_index_key("index1").collect();
    let index2_results: Vec<_> = sparkv.get_by_index_key("index2").collect();

    assert_eq!(index1_results, vec![&"value1".to_string()]);
    assert_eq!(index2_results, vec![&"value1".to_string()]);

    // Try to add index for non-existent key
    sparkv.add_additional_index("non_existent", "index3");
    let index3_results: Vec<_> = sparkv.get_by_index_key("index3").collect();
    assert_eq!(index3_results, Vec::<&String>::new());
}

#[test]
fn test_get_by_index_key() {
    let mut sparkv = SparKV::<String>::new();

    // Add items with same index
    sparkv
        .set("key1", "value1".into(), &["common_index".to_string()])
        .unwrap();
    sparkv
        .set("key2", "value2".into(), &["common_index".to_string()])
        .unwrap();
    sparkv
        .set("key3", "value3".into(), &["common_index".to_string()])
        .unwrap();

    // Add item with different index
    sparkv
        .set("key4", "value4".into(), &["other_index".to_string()])
        .unwrap();

    // Get by common index
    let common_results: Vec<_> = sparkv.get_by_index_key("common_index").collect();
    assert_eq!(common_results.len(), 3);
    assert!(common_results.contains(&&"value1".to_string()));
    assert!(common_results.contains(&&"value2".to_string()));
    assert!(common_results.contains(&&"value3".to_string()));

    // Get by other index
    let other_results: Vec<_> = sparkv.get_by_index_key("other_index").collect();
    assert_eq!(other_results, vec![&"value4".to_string()]);

    // Get by non-existent index
    let non_existent_results: Vec<_> = sparkv.get_by_index_key("non_existent").collect();
    assert_eq!(non_existent_results, Vec::<&String>::new());
}

#[test]
fn test_remove_by_index() {
    let mut sparkv = SparKV::<String>::new();

    // Add items with same index
    sparkv
        .set("key1", "value1".into(), &["index_to_remove".to_string()])
        .unwrap();
    sparkv
        .set("key2", "value2".into(), &["index_to_remove".to_string()])
        .unwrap();
    sparkv
        .set("key3", "value3".into(), &["other_index".to_string()])
        .unwrap();
    sparkv
        .set("key4", "value4".into(), &["index_to_remove".to_string()])
        .unwrap();

    assert_eq!(sparkv.len(), 4);

    // Remove by index
    let removed_count = sparkv.remove_by_index("index_to_remove");
    assert_eq!(removed_count, 3);
    assert_eq!(sparkv.len(), 1);

    // Verify remaining item
    assert!(sparkv.contains_key("key3"));
    assert_eq!(sparkv.get("key3"), Some(&"value3".to_string()));

    // Remove by non-existent index
    let removed_count = sparkv.remove_by_index("non_existent");
    assert_eq!(removed_count, 0);
    assert_eq!(sparkv.len(), 1);
}

#[test]
fn test_clear() {
    let mut sparkv = SparKV::<String>::new();

    // Add items
    sparkv
        .set("key1", "value1".into(), &["index1".to_string()])
        .unwrap();
    sparkv
        .set("key2", "value2".into(), &["index2".to_string()])
        .unwrap();
    sparkv
        .set("key3", "value3".into(), &["index3".to_string()])
        .unwrap();

    assert_eq!(sparkv.len(), 3);
    assert!(!sparkv.is_empty());

    // Clear all
    sparkv.clear();

    assert_eq!(sparkv.len(), 0);
    assert!(sparkv.is_empty());

    // Verify indexes are also cleared
    let index_results: Vec<_> = sparkv.get_by_index_key("index1").collect();
    assert_eq!(index_results, Vec::<&String>::new());
}

#[test]
fn test_default_implementation() {
    let sparkv: SparKV<String> = SparKV::default();
    assert_eq!(sparkv.len(), 0);
    assert!(sparkv.is_empty());

    // Verify it has default config
    assert_eq!(sparkv.config.max_items, 10_000);
    assert_eq!(sparkv.config.max_item_size, 500_000);
}

#[test]
fn test_set_with_index_keys() {
    let mut sparkv = SparKV::<String>::new();

    // Set with multiple index keys
    let index_keys = vec![
        "index1".to_string(),
        "index2".to_string(),
        "index3".to_string(),
    ];
    sparkv.set("key1", "value1".into(), &index_keys).unwrap();

    // Verify all indexes work
    let index1_results: Vec<_> = sparkv.get_by_index_key("index1").collect();
    let index2_results: Vec<_> = sparkv.get_by_index_key("index2").collect();
    let index3_results: Vec<_> = sparkv.get_by_index_key("index3").collect();

    assert_eq!(index1_results, vec![&"value1".to_string()]);
    assert_eq!(index2_results, vec![&"value1".to_string()]);
    assert_eq!(index3_results, vec![&"value1".to_string()]);
}

#[test]
fn test_earliest_expiration_eviction() {
    let mut config: Config = Config::new();
    config.max_items = 2;
    config.earliest_expiration_eviction = true;

    let mut sparkv = SparKV::<String>::with_config(config);

    // Add first item with short TTL
    sparkv
        .set_with_ttl("short", "short_value".into(), Duration::seconds(1), &[])
        .unwrap();

    // Add second item with longer TTL
    sparkv
        .set_with_ttl("long", "long_value".into(), Duration::seconds(10), &[])
        .unwrap();

    assert_eq!(sparkv.len(), 2);

    // Try to add third item - should trigger eviction of earliest expiring item
    sparkv
        .set_with_ttl("new", "new_value".into(), Duration::seconds(5), &[])
        .unwrap();

    // "short" should be evicted, "long" and "new" should remain
    assert_eq!(sparkv.len(), 2);
    assert!(!sparkv.contains_key("short"));
    assert!(sparkv.contains_key("long"));
    assert!(sparkv.contains_key("new"));
}

#[test]
fn test_auto_clear_with_expired_entry() {
    let mut config: Config = Config::new();
    config.auto_clear_expired = true;
    let mut sparkv = SparKV::<String>::with_config(config);

    sparkv
        .set_with_ttl("expired", "v".into(), Duration::milliseconds(1), &[])
        .unwrap();

    std::thread::sleep(std::time::Duration::from_millis(5));

    // This should clear the expired entry without panicking/recursing
    sparkv.set("other", "x".into(), &[]).unwrap();

    assert!(!sparkv.contains_key("expired"));
    assert!(sparkv.contains_key("other"));
}
