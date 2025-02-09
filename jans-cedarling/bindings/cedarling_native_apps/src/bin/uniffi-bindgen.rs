// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::env;

fn main() {
    env::set_var("RUST_MIN_STACK", "67108864"); // Example: 64 MB stack
    uniffi::uniffi_bindgen_main()
}