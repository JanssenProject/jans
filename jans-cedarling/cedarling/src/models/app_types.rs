/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

//! Module that contains structures used as configuration internally in the application
//! It is usefull to use it with DI container
use uuid7::{uuid4, Uuid};

/// Value is used as ID for application
/// represents a unique ID for application
/// generated one on startup
#[derive(Debug, Clone, Copy)]
pub(crate) struct PdpID(pub Uuid);

impl PdpID {
    pub fn new() -> Self {
        PdpID(uuid4())
    }
}

/// Name of application from configuration
#[derive(Debug, Clone)]
pub(crate) struct ApplicationName(pub String);
