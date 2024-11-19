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

// #[derive(Debug)]
#[magnus::wrap(class = "Engine")]
// silence "warning: field 0 is never read"
// because this goes back to ruby and accesses will come from there.
#[allow(dead_code)]
struct Engine(cedarling::Cedarling);

// TODO TtlTooLong is thrown by sparkv if this exceeds max.
// But max should be immediately thrown out here, not over there in sparkv.
const MAX_LOG_TTL : u64 = 60*60;

/**
    Helper function to convert a `magnus::Value` ruby object into a
    `cedarling::PolicyStoreSource`. See instantiate_engine for details.
*/
fn policy_store_source_of_hash(ruby : &magnus::Ruby, policy_store_value : Option<magnus::Value>)
-> Result<cedarling::PolicyStoreSource,magnus::Error> {    // convert policy
    use magnus::prelude::*;

    let policy_store_value = policy_store_value
        .ok_or(magnus::Error::new(ruby.exception_runtime_error(), "policy_store: hash required wth either json: or yaml: key"))?;

    if policy_store_value.is_kind_of(ruby.class_hash()) {
        use magnus::r_hash::RHash;
        let ruby_hash = RHash::from_value(policy_store_value).ok_or_else(|| {
            magnus::Error::new(
                ruby.exception_runtime_error(),
                format!("Failed to convert policy_store: to a hash"))
        })?;

        if let Ok(json) = ruby_hash.lookup::<_, String>(ruby.to_symbol("json")) {
            Ok(cedarling::PolicyStoreSource::Json(json.clone()))
        } else if let Ok(yaml) = ruby_hash.lookup::<_, String>(ruby.to_symbol("yaml")) {
            Ok(cedarling::PolicyStoreSource::Yaml(yaml.clone()))
        } else {
            Err(magnus::Error::new(ruby.exception_runtime_error(), "policy_store: neither json: nor yaml: specified"))
        }
    } else {
        // Just convert to a string, and for now assume it's yaml
        Ok(cedarling::PolicyStoreSource::Yaml(policy_store_value.to_string()))
    }
}

/**
    Implement `Cedarling::new`.

    Minimal example:
    ```
    Cedarling.new(policy_store: policy_store)
    ```

    Fully specified
    ```
    Cedarling.new(
      signature_algorithms: %w[HS256 RS256],
      policy: {yaml: policy_store},
      name: 'rubyling'
      log_ttl_ms: 300
    )
    ```

    The value of the `policy_store:` key is either plain ruby `String`, where it
    is assumed it will contain yaml, or it is a ruby `Hash`, which is assumed
    to be one of:
    ```
    {yaml: policy_store_in_yaml}
    ```
    or
    ```
    {json: policy_store_in_json}
    ```

    `name` can be whatever you want

    `log_ttl` is the number of milliseconds the cedarling logs are kept. This is
    a range between 1 and 3600. See sparkv for details.
*/
fn instantiate_engine(ruby : &magnus::Ruby, args: &[magnus::Value])
-> Result<Engine,magnus::Error> {
    use magnus::{
        scan_args::{get_kwargs, scan_args},
    };

    let args = scan_args::<(), (), (), (), _, ()>(args)?;
    let kwargs = get_kwargs::<_, (), (Option<magnus::Value>, Option<String>, Option<u64>, Option<Vec<String>>), ()>(
        args.keywords,
        &[],
        &["policy_store", "name", "log_ttl", "signature_algorithms"],
    )?;

    let (policy_store, application_name, log_ttl, signature_algorithms) = kwargs.optional;
    let application_name = application_name.unwrap_or("ruby".into());
    let log_ttl = log_ttl.unwrap_or(MAX_LOG_TTL);

    let jwt_config = match signature_algorithms {
        Some(signature_algorithms) => cedarling::JwtConfig::Enabled{signature_algorithms},
        None => cedarling::JwtConfig::Disabled,
    };

    if log_ttl > MAX_LOG_TTL {
        let err = magnus::Error::new(ruby.exception_runtime_error(), format!("log_ttl: {log_ttl} exceeds max of {MAX_LOG_TTL}"));
        return Err(err)
    }

    if log_ttl <= 0 {
        let err = magnus::Error::new(ruby.exception_runtime_error(), format!("log_ttl: must be at minimum 1"));
        return Err(err)
    }

    let policy_store_source = policy_store_source_of_hash(ruby, policy_store)?;

    // build config
    let bootstrap_config = cedarling::BootstrapConfig {
        application_name,
        log_config: cedarling::LogConfig {
            log_type: cedarling::LogTypeConfig::Memory(cedarling::MemoryLogConfig{log_ttl}),
        },
        policy_store_config: cedarling::PolicyStoreConfig {
            source: policy_store_source,
        },
        jwt_config: jwt_config,
    };

    // construct the actual cedarling instance
    match cedarling::Cedarling::new(bootstrap_config) {
        Ok(instance) => Ok(Engine(instance)),
        // TODO might need a custom ruby exception for this.
        // in the meantime RuntimeError will do
        Err(err) => Err(magnus::Error::new(ruby.exception_runtime_error(), format!("{err:?}")))
    }
}

impl Engine {
    pub fn authorize( &self, access_token : String, id_token : String, userinfo_token : String )
    -> Result<String, magnus::Error> {
        let Engine(cedarling) = self;
        let result = cedarling.authorize(cedarling::Request {
            access_token,
            id_token,
            userinfo_token,
            action: "Jans::Action::\"Update\"".to_string(),
            context: serde_json::json!({}),
            resource: cedarling::ResourceData {
                id: "random_id".to_string(),
                resource_type: "Jans::Issue".to_string(),
                payload: std::collections::HashMap::from_iter([
                    (
                        "org_id".to_string(),
                        serde_json::Value::String("some_long_id".to_string()),
                    ),
                    (
                        "country".to_string(),
                        serde_json::Value::String("US".to_string()),
                    ),
                ]),
            },
        });

        match result {
            Ok(result) => Ok(format!("is allowed: {}", result.is_allowed())),
            Err(e) => Ok(format!("Error while authorizing: {}\n {:?}\n\n", e, e).into()),
        }
    }
}

/// Create ruby classes and their methods.
#[magnus::init]
fn init(ruby: &magnus::Ruby) -> Result<(), magnus::Error> {
    use magnus::{function, prelude::*};

    let module = ruby.define_module("Cedarling")?;
    module.define_singleton_method("version_core", function!(version_core, 0))?;
    module.define_singleton_method("new", function!(instantiate_engine, -1))?;
    module.const_set("MAX_LOG_TTL", MAX_LOG_TTL)?;

    // definition of the Engine class, which is a facade for cedarling::Cedarling
    let _class = ruby.define_class("Engine", ruby.class_object())?;

    Ok(())
}
