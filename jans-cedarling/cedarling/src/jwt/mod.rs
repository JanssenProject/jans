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

/// The `token` module provides various types of JWT used in the system.
/// These tokens handle different aspects of authentication, authorization,
/// user identity, and transactions.
///
/// This module contains the following types of tokens:
/// - `AccessToken`: Used for authenticating and authorizing access to protected resources.
/// - `IdToken`: Contains user identity claims.
/// - `TransactionToken`: Used to track and verify specific transactions.
/// - `UserInfoToken`: Stores additional user details that might be retrieved after authentication.
pub mod token;
