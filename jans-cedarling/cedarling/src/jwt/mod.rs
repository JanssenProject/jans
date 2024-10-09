/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */
//! # JWT Engine
//! Part of Cedarling that main purpose is:
//! - validate JWT signature
//! - validate JWT status
//! - extract JWT claims

mod error;
mod service;
#[cfg(test)]
mod tests;

pub use error::DecodeJwtError;
pub(crate) use service::JwtService;
