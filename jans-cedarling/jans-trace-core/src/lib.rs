// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

#![doc = include_str!("../README.md")]
#![forbid(unsafe_code)]
// Each error type documents its own variants; `# Errors` sections would repeat them (AGENTS.md
// avoids Python-style doc sections). Same choice as `cedarling/src/lib.rs`.
#![allow(clippy::missing_errors_doc)]

pub mod assertion;
mod digest;
pub mod inputs;
pub mod json;
pub mod key;
mod limits;
pub mod receipt;
pub mod self_test;
pub mod signature;

// Shared by every module, so they live at the root instead of `digest::Digest`.
pub use digest::{Digest, ParseDigestError};
pub use limits::{Limits, SizeLimitError};
