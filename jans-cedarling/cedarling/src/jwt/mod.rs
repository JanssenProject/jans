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
mod jwt_service_config;
#[cfg(test)]
mod test;
mod token;
mod traits;

pub(crate) use decoding_strategy::string_to_alg;
pub use decoding_strategy::ParseAlgorithmError;
pub use error::*;
use jsonwebtoken as jwt;
pub(crate) use jwt::Algorithm;
pub(crate) use jwt_service::*;
pub(crate) use jwt_service_config::JwtServiceConfig;
pub(crate) use traits::GetKey;
