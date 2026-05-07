// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Policy management: version registry, schema validation, and policy diffs.

use thiserror::Error;

pub(crate) mod schema;
pub(crate) mod versions;

pub(crate) use schema::SchemaError;

/// Module-level error for policy operations (loading, registry SPI, rollback).
#[derive(Debug, Error)]
pub(crate) enum PolicyError {
    #[error("SPI error: {0}")]
    Spi(#[from] pgrx::spi::Error),
    #[error("policy load failed: {0}")]
    #[allow(dead_code)]
    Load(String),
    #[error("registry error: {0}")]
    #[allow(dead_code)]
    Registry(String),
}
