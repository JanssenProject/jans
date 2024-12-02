mod utils;

use cedarling::{self as core, BootstrapConfig, BootstrapConfigRaw, Request};
// use std::collections::HashSet;
//
// use cedarling::*;
use wasm_bindgen::prelude::*;

#[wasm_bindgen]
pub struct Cedarling {
    cedarling: core::Cedarling,
}

#[wasm_bindgen]
impl Cedarling {
    pub fn new(config: JsValue) -> Result<Self, JsError> {
        let config = serde_wasm_bindgen::from_value::<BootstrapConfigRaw>(config.clone())?;
        let config =
            BootstrapConfig::from_raw_config(&config).map_err(|e| JsError::new(&e.to_string()))?;
        let cedarling = core::Cedarling::new(config).map_err(|e| JsError::new(&e.to_string()))?;

        Ok(Self { cedarling })
    }

    pub fn authorize(&self, request: JsValue) -> Result<JsValue, JsError> {
        let request = serde_wasm_bindgen::from_value::<Request>(request)
            .map_err(|e| JsError::new(&e.to_string()))?;

        let result = self
            .cedarling
            .authorize(request)
            .map_err(|e| JsError::new(&e.to_string()))?;

        Ok(serde_wasm_bindgen::to_value(&result)?)
    }
}

#[wasm_bindgen]
extern "C" {
    fn alert(s: &str);
}

#[wasm_bindgen]
pub fn init() {
    alert("Hello, cedarling_wasm!");
}
