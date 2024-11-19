/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

extern crate cedarling;

// this env var is set by build.rs
const CEDARLING_VERSION: &str = env!("CEDARLING_VERSION");

/// Return the current version of cedarling - not the current version of the gem.
fn version_core() -> Result<String,magnus::Error> {
    Ok(CEDARLING_VERSION.into())
}

#[magnus::init]
fn init(ruby: &magnus::Ruby) -> Result<(), magnus::Error> {
    use magnus::{function, prelude::*};

    let module = ruby.define_module("Cedarling")?;
    module.define_singleton_method("version_core", function!(version_core, 0))?;

    Ok(())
}
