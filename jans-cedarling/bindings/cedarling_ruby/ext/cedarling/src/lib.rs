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

/// Helper method to retrieve the Cedarling::Error class.
fn cedarling_ruby_exception(ruby : &magnus::Ruby, msg : String) -> magnus::Error {
    // srsly? Is eval the only way to do this?
    let cedarling_error_class = match ruby.eval::<magnus::Value>("Cedarling::Error") {
        Ok(cedarling_error_class) => cedarling_error_class,
        Err(err) => {
            return magnus::Error::new(
                ruby.exception_runtime_error(),
                format!("Could not find class Cedarling::Error: {err}. Original error was {msg}"))
        },
    };
    let cedarling_exception = match magnus::ExceptionClass::from_value(cedarling_error_class) {
        Some(cedarling_exception) => cedarling_exception,
        None => {
            return magnus::Error::new(
                ruby.exception_runtime_error(),
                format!("Could not find class Cedarling::Error. Original error was {msg}"))
        },
    };
    magnus::Error::new(cedarling_exception, msg)
}

/**
    Helper function to convert a `magnus::Value` ruby object into a
    `cedarling::PolicyStoreSource`. See instantiate_engine for details.
*/
fn policy_store_source_of_hash(ruby : &magnus::Ruby, policy_store_value : Option<magnus::Value>)
-> Result<cedarling::PolicyStoreSource,magnus::Error> {    // convert policy
    use magnus::prelude::*;

    let policy_store_value = policy_store_value
        .ok_or(cedarling_ruby_exception(ruby, format!("policy_store: hash required wth either json: or yaml: key")))?;

    if policy_store_value.is_kind_of(ruby.class_hash()) {
        use magnus::r_hash::RHash;
        let ruby_hash = RHash::from_value(policy_store_value).ok_or(cedarling_ruby_exception(ruby, format!("Failed to convert policy_store: to a hash")))?;

        if let Ok(json) = ruby_hash.lookup::<_, String>(ruby.to_symbol("json")) {
            Ok(cedarling::PolicyStoreSource::Json(json.clone()))
        } else if let Ok(yaml) = ruby_hash.lookup::<_, String>(ruby.to_symbol("yaml")) {
            Ok(cedarling::PolicyStoreSource::Yaml(yaml.clone()))
        } else {
            Err(cedarling_ruby_exception(ruby, format!("policy_store: neither json: nor yaml: specified")))
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
    use magnus::scan_args::{get_kwargs, scan_args};

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
        Err(cedarling_ruby_exception(ruby, format!("log_ttl: {log_ttl} exceeds max of {MAX_LOG_TTL}")))?
    }

    if log_ttl <= 0 {
        Err(cedarling_ruby_exception(ruby, format!("log_ttl: must be at minimum 1")))?
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
        Err(err) => Err(cedarling_ruby_exception(ruby, format!("Cannot create cedarling: {err}")))
    }
}

/// Wrapper to facilitate working with the `cedarling::Request` object
/// but without unnecessarily attaching ruby-specific functionality to it.
#[magnus::wrap(class="AuthorizeRequest")]
#[derive(serde::Deserialize, Debug, PartialEq)]
struct AuthorizeRequest(cedarling::Request);

impl std::fmt::Display for AuthorizeRequest {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f,"{:?}", self.0)
    }
}

/// Wrapper for `cedarling::AuthorizeResult` for ruby.
#[magnus::wrap(class="AuthorizeResult")]
struct AuthorizeResult(cedarling::AuthorizeResult);

impl std::fmt::Display for AuthorizeResult {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f,"workload: {:#?}\nperson: {:#?}\nrole: {:#?}", self.0.workload, self.0.person, self.0.role)
    }
}

/// Wrapper for to facilitate working with `cedar_policy::Response`.
/// Primarily used to provide diagnostics.
struct Response<'a>(&'a cedar_policy::Response);

impl serde::Serialize for Response<'_> {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where S: serde::Serializer {
        use serde::ser::SerializeMap;

        let mut map = serializer.serialize_map(Some(3))?;

        map.serialize_entry("allow", &(self.0.decision() == cedar_policy::Decision::Allow))?;

        let diags = self.0.diagnostics();
        let policy_ids = diags.reason().map(|d| d.to_string()).collect::<Vec<_>>();
        map.serialize_entry("policy_ids", &policy_ids)?;

        let errors = diags.errors().map(|d| d.to_string()).collect::<Vec<_>>();
        if !errors.is_empty() {map.serialize_entry("errors", &errors)?;}

        map.end()
    }
}

impl serde::Serialize for AuthorizeResult {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where S: serde::Serializer {
        use serde::ser::SerializeMap;

        let mut map = serializer.serialize_map(Some(3))?;

        if let Some(response) = &self.0.workload {
            map.serialize_entry("workload", &(Response(&response)))?;
        }

        if let Some(response) = &self.0.person {
            map.serialize_entry("person", &(Response(&response)))?;
        }

        if let Some(response) = &self.0.role {
            map.serialize_entry("role", &(Response(&response)))?;
        }

        map.end()
    }
}

impl AuthorizeResult {
    fn to_h(&self) -> Result<magnus::Value, magnus::Error> {
        Ok(serde_magnus::serialize(&self)?)
    }
}

/**
    Perform an authorize request.

    Ruby example:

    cedarling = Cedarling.new(...)
    cedarling.authorize(
        access_token:,
        id_token:,
        userinfo_token:,
        action: %q(Jans::Action::"Update"),
        resource: {type: 'Jans::Issue', id: 'random_id', org_id: 'some_long_id', country: 'US'},
        context: {}
    )
*/
fn authorize(ruby : &magnus::Ruby, engine : &Engine, request : magnus::Value)
-> Result<AuthorizeResult, magnus::Error> {
    let Engine(cedarling) = engine;
    let AuthorizeRequest(request) = serde_magnus::deserialize(request)?;
    match cedarling.authorize(request) {
        Ok(cedarling_auth_result) => Ok(AuthorizeResult(cedarling_auth_result)),
        Err(err) => Err(cedarling_ruby_exception(ruby, format!("Error while authorizing: {}\n {:#?}\n\n", err, err)))
    }
}

/// Create ruby classes and their methods.
#[magnus::init]
fn init(ruby: &magnus::Ruby) -> Result<(), magnus::Error> {
    use magnus::{function, method, prelude::*};

    let module = ruby.define_module("Cedarling")?;
    module.define_singleton_method("version_core", function!(version_core, 0))?;
    module.define_singleton_method("new", function!(instantiate_engine, -1))?;
    module.const_set("MAX_LOG_TTL", MAX_LOG_TTL)?;
    // define Cedarling::Error exception base class
    module.define_error("Error", ruby.exception_runtime_error())?;

    // definition of the Engine class, which is a facade for cedarling::Cedarling
    let class = ruby.define_class("Engine", ruby.class_object())?;
    class.define_method("authorize", method!(authorize, 1))?;

    let authorize_request_class = ruby.define_class("AuthorizeRequest", ruby.class_object())?;
    authorize_request_class.define_method("to_s", method!(AuthorizeRequest::to_string, 0))?;
    authorize_request_class.define_method("inspect", method!(AuthorizeRequest::to_string, 0))?;

    let authorize_result_class = ruby.define_class("AuthorizeResult", ruby.class_object())?;
    authorize_result_class.define_method("allowed?", method!(|inst : &AuthorizeResult| inst.0.is_allowed(), 0))?;
    authorize_result_class.define_method("to_s", method!(AuthorizeResult::to_string, 0))?;
    authorize_result_class.define_method("inspect", method!(|inst : &AuthorizeResult| format!("{}", inst), 0))?;
    authorize_result_class.define_method("to_h", method!(AuthorizeResult::to_h, 0))?;

    Ok(())
}
