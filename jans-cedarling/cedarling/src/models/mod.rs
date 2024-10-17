/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */
//! # Models
//! This package provides the core data models for the *Cedarling* application,
//! defining the structures and types essential for its functionality.

pub(crate) mod app_types;
pub mod authorize_result;
pub mod bootstrap_config;
pub(crate) mod cedar_schema;
pub mod jwt_config;
pub mod log_config;
pub mod log_entry;
pub mod policy_store;
pub mod policy_store_config;
pub mod request;
pub mod token_data;

/// # Configuration module.
/// Reimport all entities that we need to configure application
pub mod config {
    use super::*;

    pub use bootstrap_config::*;
    pub use jwt_config::*;
    pub use log_config::*;
    pub use policy_store_config::*;
}
