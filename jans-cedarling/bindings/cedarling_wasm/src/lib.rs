#![cfg(
    target_arch = "wasm32",
    target_vendor = "unknown",
    target_os = "unknown"
)]

use wasm_bindgen::prelude::*;

#[wasm_bindgen]
extern "C" {
    pub fn alert(s: &str);
}

#[wasm_bindgen]
pub fn greet(name: &str) {
    alert(&format!("Hello, {}!", name));
}
