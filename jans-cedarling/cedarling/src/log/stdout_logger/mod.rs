// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

// conditionally compile logger for native platform and WASM

#[cfg(not(target_arch = "wasm32"))]
mod native_logger;
#[cfg(not(target_arch = "wasm32"))]
pub(crate) use native_logger::*;

#[cfg(target_arch = "wasm32")]
mod wasm_logger;
#[cfg(target_arch = "wasm32")]
pub(crate) use wasm_logger::*;
