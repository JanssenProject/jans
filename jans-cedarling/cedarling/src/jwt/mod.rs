/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

//! # `JwtEngine`
//!
//! The `JwtEngine` is responsible for handling JSON Web Tokens (JWTs) and includes functionality for:
//! - Validating JWT signatures
//! - Validating JWT status
//! - Extracting claims from JWTs
//!
//! ## Modules
//!
//! - `decoding_strategy`: Defines strategies for decoding JWTs.
//! - `error`: Contains error types and handling for JWT operations.
//! - `jwt_service`: Provides the primary service for JWT management.
//! - `token`: Manages token representations and their manipulations.
//! - `traits`: Defines traits for extensibility and common behaviors related to JWTs.
//! - `test`: Contains unit tests for the `JwtEngine` functionality.

mod decoding_strategy;
mod error;
mod jwt_service;
#[cfg(test)]
mod test;
mod token;
mod traits;

pub use error::*;
pub(crate) use jwt_service::*;
