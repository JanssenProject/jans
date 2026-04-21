// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! `PostgreSQL` extension (`cedarling_pg`) for Cedarling-backed authorization.

mod guc_config;
mod validate;

use pgrx::prelude::*;

/// Compile-time anchor so the `cedarling` blocking API is linked; full integration comes later.
const _: usize = core::mem::size_of::<cedarling::blocking::Cedarling>();

::pgrx::pg_module_magic!(name, version);

#[pg_guard]
pub extern "C-unwind" fn _PG_init() {
    guc_config::register_gucs();
}

#[pg_extern]
fn hello_cedarling_pg() -> &'static str {
    "Hello, cedarling_pg"
}

#[cfg(any(test, feature = "pg_test"))]
#[pg_schema]
mod tests {
    use pgrx::prelude::*;

    #[pg_test]
    fn test_hello_cedarling_pg() {
        assert_eq!("Hello, cedarling_pg", crate::hello_cedarling_pg());
    }

    #[pg_test]
    fn test_gucs_defaults() {
        use crate::guc_config::{
            cache_ttl_seconds, fail_mode, log_level, mode, tokens_utf8, CedarlingFailMode,
            CedarlingLogLevelGuc, CedarlingMode,
        };

        assert_eq!(mode(), CedarlingMode::Enforcement, "default mode");
        assert_eq!(fail_mode(), CedarlingFailMode::Closed, "default fail_mode");
        assert_eq!(log_level(), CedarlingLogLevelGuc::Info, "default log_level");
        assert_eq!(cache_ttl_seconds(), 300, "default cache_ttl");
        assert_eq!(tokens_utf8(), None, "default tokens unset");

        let show_mode = Spi::get_one::<String>("SHOW cedarling.mode")
            .expect("SPI should succeed for SHOW cedarling.mode");
        assert_eq!(
            show_mode,
            Some("enforcement".to_string()),
            "SHOW should match GUC registration"
        );
    }

    #[pg_test]
    fn test_tokens_valid_json_accepted() {
        Spi::run(r#"SET cedarling.tokens = '{"access_token":"x"}'"#)
            .expect("valid JSON object should be accepted for cedarling.tokens");
        Spi::run("RESET cedarling.tokens").expect("RESET cedarling.tokens should succeed");
    }
}

#[cfg(feature = "pg_bench")]
#[pg_schema]
mod benches {
    use pgrx::prelude::*;
    use pgrx_bench::{black_box, Bencher};

    #[pg_bench]
    fn bench_hello_cedarling_pg(b: &mut Bencher) {
        b.iter(|| {
            black_box(crate::hello_cedarling_pg());
        });
    }
}

/// Required by `cargo pgrx test`.
#[cfg(test)]
pub mod pg_test {
    pub fn setup(_options: Vec<&str>) {
        // One-off initialization when the pg_test framework starts.
    }

    #[must_use]
    pub fn postgresql_conf_options() -> Vec<&'static str> {
        vec![]
    }
}
