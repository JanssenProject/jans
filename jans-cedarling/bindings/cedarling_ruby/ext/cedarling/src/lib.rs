extern crate cedarling;

use magnus::{function, prelude::*, Ruby};

// TODO this must go away fairly soon
fn hello(subject: String) -> String {
    format!("Hello from Rust, {subject}!")
}

static POLICY_STORE_RAW: &str = include_str!("../../../../../cedarling/src/init/test_files/policy-store_ok.json");

fn bootstrap(ruby : &Ruby) -> Result<String,magnus::Error> {
    let bootstrap_config = cedarling::BootstrapConfig {
        application_name: "test_app".to_string(),
        log_config: cedarling::LogConfig {
            log_type: cedarling::LogTypeConfig::StdOut,
        },
        policy_store_config: cedarling::PolicyStoreConfig {
            source: cedarling::PolicyStoreSource::Json(POLICY_STORE_RAW.to_string()),
            store_id: None,
        },
        jwt_config: cedarling::JwtConfig::Disabled,
    };

    // construct something a little more complicated
    use std::io::Write;
    let mut buf : Vec<u8> = vec![];
    writeln!(buf, "app name: {}", bootstrap_config.application_name).unwrap();
    match String::from_utf8(buf) {
        Ok(string_value) => Ok(string_value),
        Err(_err) => Err(magnus::Error::new(ruby.exception_runtime_error(), "cannot convert from utf8"))
    }
}

#[magnus::init]
fn init(ruby: &Ruby) -> Result<(), magnus::Error> {
    let module = ruby.define_module("Cedarling")?;
    module.define_singleton_method("hello", function!(hello, 1))?;
    module.define_singleton_method("bootstrap", function!(bootstrap, 0))?;
    Ok(())
}
