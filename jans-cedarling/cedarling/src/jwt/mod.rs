/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

//! # `JwtEngine`
//!
//! The `JwtEngine` is responsible for
//! - validating JWT signatures
//! - validating JWT status
//! - extracting JWT claims

mod decoding_strategy;
mod error;
mod jwt_service;
#[cfg(test)]
mod test;
mod traits;

pub use error::*;
pub(crate) use jwt_service::*;
