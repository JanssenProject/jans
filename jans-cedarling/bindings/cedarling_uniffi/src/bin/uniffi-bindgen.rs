// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.
#[cfg(not(target_arch = "wasm32"))]

fn main() {
    uniffi::uniffi_bindgen_main()
}