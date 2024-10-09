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

/// Represents claims to be tested agains tokens
pub mod claims;

/// The `token` module provides validation for various types of JWT.
pub mod token;
