// allow dead code to avoid highlight test functions (by linter) that is used only using WASM
#![allow(dead_code)]

use std::sync::LazyLock;

use crate::*;

use wasm_bindgen_test::*;

wasm_bindgen_test_configure!(run_in_browser);

// Reuse json policy store file from python example.
// Because for `BootstrapConfigRaw` we need to use JSON
static POLICY_STORE_RAW_YAML: &str =
    include_str!("../../../bindings/cedarling_python/example_files/policy-store.json");

static BOOTSTRAP_CONFIG: LazyLock<serde_json::Value> = LazyLock::new(|| {
    serde_json::json!({
        "CEDARLING_APPLICATION_NAME": "My App",
        "CEDARLING_LOCAL_POLICY_STORE": POLICY_STORE_RAW_YAML,
        "CEDARLING_LOG_TYPE": "std_out",
        "CEDARLING_LOG_LEVEL": "INFO",
        "CEDARLING_USER_AUTHZ": "enabled",
        "CEDARLING_WORKLOAD_AUTHZ": "enabled",
        "CEDARLING_USER_WORKLOAD_BOOLEAN_OPERATION": "AND",
        "CEDARLING_ID_TOKEN_TRUST_MODE": "strict",

    })
});

/// test init with map value using `Cedarling::new_from_map`
#[wasm_bindgen_test]
async fn test_cedarling_new_from_map() {
    let bootstrap_config_json = BOOTSTRAP_CONFIG.clone();
    let conf_map_js_value = serde_wasm_bindgen::to_value(&bootstrap_config_json)
        .expect("serde json value should be converted to JsValue");
    console_log!("conf_map_js_value: {conf_map_js_value:?}");

    let conf_js_map: Map = conf_map_js_value.unchecked_into();
    console_log!("conf_js_map: {conf_js_map:?}");
    let _instance = Cedarling::new_from_map(conf_js_map.clone())
        .await
        .inspect(|_| console_log!("Cedarling::new_from_map initialized successfully"))
        .expect("Cedarling::new_from_map should be initialized");
}

/// test init with map value using `init`
#[wasm_bindgen_test]
async fn test_init_conf_as_map() {
    let bootstrap_config_json = BOOTSTRAP_CONFIG.clone();
    let conf_map_js_value = serde_wasm_bindgen::to_value(&bootstrap_config_json)
        .expect("serde json value should be converted to JsValue");
    console_log!("conf_map_js_value: {conf_map_js_value:?}");

    let _instance = init(conf_map_js_value)
        .await
        .inspect(|_| console_log!("init initialized successfully"))
        .expect("init function should be initialized with js map");
}

/// test init with object value using `Cedarling::new`
#[wasm_bindgen_test]
async fn test_cedarling_new_from_object() {
    let bootstrap_config_json = BOOTSTRAP_CONFIG.clone();
    let conf_map_js_value = serde_wasm_bindgen::to_value(&bootstrap_config_json)
        .expect("serde json value should be converted to JsValue");

    let conf_object =
        Object::from_entries(&conf_map_js_value).expect("map value should be converted to object");

    let _instance = Cedarling::new(&conf_object)
        .await
        .expect("Cedarling::new_from_map should be initialized");
}

/// test init with object value using `init`
#[wasm_bindgen_test]
async fn test_init_conf_as_object() {
    let bootstrap_config_json = BOOTSTRAP_CONFIG.clone();
    let conf_map_js_value = serde_wasm_bindgen::to_value(&bootstrap_config_json)
        .expect("serde json value should be converted to JsValue");

    let conf_object =
        Object::from_entries(&conf_map_js_value).expect("map value should be converted to object");

    let _instance = init(conf_object.into())
        .await
        .expect("init function should be initialized with js map");
}
