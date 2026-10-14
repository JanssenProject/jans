// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! `UniFFI` bindings of `jans-trace-core` for server-side Java.

#![forbid(unsafe_code)]

uniffi::setup_scaffolding!();

mod digest;
mod inputs;
mod json;
mod key;
mod limits;
mod self_test;
mod signature;
