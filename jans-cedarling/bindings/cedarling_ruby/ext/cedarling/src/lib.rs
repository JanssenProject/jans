/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

extern crate cedarling;

// #[derive(Debug)]
#[magnus::wrap(class = "Engine")]
// silence "warning: field 0 is never read"
// because this goes back to ruby and accesses will come from there.
#[allow(dead_code)]
struct Engine(cedarling::Cedarling);

// TODO TtlTooLong is thrown by sparkv if this exceeds max.
// But max should be immediately thrown out here, not over there in sparkv.
const MAX_LOG_TTL : u64 = 60*60;

fn bootstrap(ruby : &magnus::Ruby, application_name : String, policy_store_text : String, log_ttl : u64) -> Result<Engine,magnus::Error> {
    if log_ttl > MAX_LOG_TTL {
        let err = magnus::Error::new(ruby.exception_runtime_error(), format!("{log_ttl} exceeds max of {MAX_LOG_TTL}"));
        return Err(err)
    }

    // build config
    use cedarling::MemoryLogConfig;
    let bootstrap_config = cedarling::BootstrapConfig {
        application_name: application_name,
        log_config: cedarling::LogConfig {
            log_type: cedarling::LogTypeConfig::Memory(MemoryLogConfig{log_ttl}),
        },
        policy_store_config: cedarling::PolicyStoreConfig {
            source: cedarling::PolicyStoreSource::Json(policy_store_text),
            store_id: None,
        },
        jwt_config: cedarling::JwtConfig::Disabled,
    };

    // construct the actual cedarling instance
    // It will need to store the top-level rust structure in a ruby object.
    // gc from ruby will be interesting. May have to use mem::forget and a manual cleanup later.
    // https://github.com/matsadler/magnus#wrapping-rust-types-in-ruby-objects
    // TODO will magnus take care of `drop` for this?
    match cedarling::Cedarling::new(bootstrap_config) {
        Ok(instance) => Ok(Engine(instance)),
        // TODO might need a custom ruby exception for this.
        // in the meantime RuntimeError will do
        Err(err) => Err(magnus::Error::new(ruby.exception_runtime_error(), format!("{err:?}")))
    }
}

#[magnus::init]
fn init(ruby: &magnus::Ruby) -> Result<(), magnus::Error> {
    use magnus::{function, prelude::*};

    let module = ruby.define_module("Cedarling")?;
    module.define_singleton_method("bootstrap", function!(bootstrap, 3))?;
    module.const_set("MAX_LOG_TTL", MAX_LOG_TTL)?;

    // definition of the Engine class, which is a facade for cedarling::Cedarling
    let _class = ruby.define_class("Engine", ruby.class_object())?;

    Ok(())
}
