// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! # Data Store Module
//!
//! Provides key-value storage for pushed data with TTL support, capacity management,
//! and thread-safe concurrent access.
//!
//! # Overview
//!
//! The data module enables applications to push external data into Cedarling's
//! evaluation context. This data becomes available in Cedar policies through
//! the `context.data` namespace.
//!
//! # Components
//!
//! - [`DataStore`] - Thread-safe key-value store with TTL support
//! - [`DataApi`] - Trait defining the public data access interface
//! - [`DataEntry`] - Entry structure with value and metadata
//! - [`DataStoreStats`] - Statistics about store state
//! - [`DataStoreConfig`] - Configuration for storage limits and TTL
//!
//! # Example
//!
//! ```ignore
//! use cedarling::data::{DataApi, DataStore, DataStoreConfig};
//! use serde_json::json;
//! use std::time::Duration;
//!
//! // Create store with default configuration
//! let store = DataStore::new(DataStoreConfig::default())?;
//!
//! // Push data with 5-minute TTL
//! store.push_data("user_context", json!({
//!     "department": "engineering",
//!     "clearance_level": 3
//! }), Some(Duration::from_secs(300)))?;
//!
//! // Retrieve and use data
//! if let Some(ctx) = store.get_data("user_context")? {
//!     println!("User context: {}", ctx);
//! }
//!
//! // Check store statistics
//! let stats = store.get_stats()?;
//! println!("Entries: {}", stats.entry_count);
//! ```

mod api;
mod config;
mod entry;
mod error;
mod store;

pub use api::{DataApi, DataStoreStats};
pub use config::{ConfigValidationError, DataStoreConfig};
pub use entry::{CedarType, DataEntry};
pub use error::DataError;
pub use store::DataStore;
