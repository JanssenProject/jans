// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! # Data Store Module
//!
//! Provides key-value storage for pushed data with TTL support, capacity management,
//! and thread-safe concurrent access.

mod api;
mod config;
mod entry;
mod error;
mod store;

pub use api::{DataApi, DataStoreStats};
pub use config::{ConfigValidationError, DataStoreConfig};
pub use entry::{CedarType, DataEntry};
pub use error::DataError;
pub(crate) use store::DataStore;
