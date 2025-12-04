// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! In this file we define functions for serde `default` macro.

use crate::JwtConfig;

pub fn default_jti() -> String {
    "jti".to_string()
}

pub fn default_true() -> bool {
    true
}

pub fn default_token_cache_capacity() -> usize {
    JwtConfig::default().token_cache_capacity
}
