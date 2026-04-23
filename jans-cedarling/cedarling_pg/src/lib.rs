// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! `PostgreSQL` extension (`cedarling_pg`) for Cedarling-backed authorization.

mod authorized;
mod authz_bridge;
mod authz_cache;
mod engine;
mod extension_log;
mod guc_config;
mod resource;
mod token_bundle;
mod token_sql;
mod validate;

use pgrx::prelude::*;

/// Compile-time anchor so the `cedarling` blocking API is linked; full integration comes later.
const _: usize = core::mem::size_of::<cedarling::blocking::Cedarling>();

// Keep resource parsers reachable for `dead_code` when the cdylib is built without `cfg(test)`.
const _: fn(&str) -> Result<cedarling::EntityData, resource::ResourceEntityDataError> =
    resource::resource_entity_data_from_json_str;
const _: fn(serde_json::Value) -> Result<cedarling::EntityData, resource::ResourceEntityDataError> =
    resource::resource_entity_data_from_json_value;
const _: fn(&str) -> Result<std::sync::Arc<cedarling::blocking::Cedarling>, engine::EngineError> =
    engine::try_init_cedarling_from_bootstrap_path;
const _: fn() -> Result<std::sync::Arc<cedarling::blocking::Cedarling>, engine::EngineError> =
    engine::global_cedarling;
const _: fn() -> guc_config::CedarlingMode = guc_config::mode;
const _: fn() -> guc_config::CedarlingLogLevelGuc = guc_config::log_level;
const _: fn(&str) -> Result<Vec<cedarling::TokenInput>, token_bundle::TokenBundleError> =
    token_bundle::parse_token_inputs_from_json;
const _: fn(
    &cedarling::blocking::Cedarling,
    &str,
    &str,
    &str,
) -> Result<bool, authz_bridge::AuthorizeBridgeError> =
    authz_bridge::authorize_multi_issuer_decision;
#[allow(clippy::type_complexity)]
const _: fn(
    Option<&str>,
    &str,
    &str,
    &str,
) -> Result<cedarling::RequestUnsigned, authz_bridge::UnsignedBridgeError> =
    authz_bridge::unsigned_request_from_json_parts;
const _: fn(
    &cedarling::blocking::Cedarling,
    cedarling::RequestUnsigned,
) -> Result<bool, authz_bridge::UnsignedBridgeError> =
    authz_bridge::authorize_unsigned_decision_for_request;
#[allow(clippy::type_complexity)]
const _: fn(
    &cedarling::blocking::Cedarling,
    Option<&str>,
    &str,
    &str,
    &str,
) -> Result<bool, authz_bridge::UnsignedBridgeError> = authz_bridge::authorize_unsigned_decision;
const _: fn(&str, Option<&str>, &str) -> bool = authorized::cedarling_authorized;
#[allow(clippy::type_complexity)]
const _: fn(Option<&str>, &str, &str, &str) -> bool = authorized::cedarling_authorize_unsigned;

::pgrx::pg_module_magic!(name, version);

#[pg_guard]
pub extern "C-unwind" fn _PG_init() {
    guc_config::register_gucs();
}

#[pg_extern]
fn hello_cedarling_pg() -> &'static str {
    "Hello, cedarling_pg"
}

/// Slow `PostgreSQL`/`pgrx` integration tests (`#[cfg(feature = "pg_test")]`). Enable with
/// `--features pg16,pg_test`. Workspace alias: `cargo cedarling-pg-sqltest`.
#[cfg(feature = "pg_test")]
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
            bootstrap_config_path_utf8, cache_ttl_seconds, fail_mode, log_level, mode, tokens_utf8,
            CedarlingFailMode, CedarlingLogLevelGuc, CedarlingMode,
        };

        assert_eq!(mode(), CedarlingMode::Enforcement, "default mode");
        assert_eq!(fail_mode(), CedarlingFailMode::Closed, "default fail_mode");
        assert_eq!(log_level(), CedarlingLogLevelGuc::Info, "default log_level");
        assert_eq!(cache_ttl_seconds(), 300, "default cache_ttl");
        assert_eq!(tokens_utf8(), None, "default tokens unset");
        assert_eq!(
            bootstrap_config_path_utf8(),
            None,
            "default bootstrap_config unset"
        );

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

    #[pg_test]
    fn test_token_sql_helpers_roundtrip() {
        use pgrx::datum::JsonB;
        use serde_json::json;

        crate::token_sql::cedarling_clear_tokens().expect("clear tokens should succeed");
        assert!(
            crate::token_sql::cedarling_current_tokens().is_none(),
            "after clear, current tokens should be NULL"
        );

        let payload = json!({"access_token": "t1", "id_token": "t2"});
        crate::token_sql::cedarling_set_tokens(JsonB(payload.clone()))
            .expect("cedarling_set_tokens should succeed");

        let current =
            crate::token_sql::cedarling_current_tokens().expect("current tokens expected");
        assert_eq!(
            current.0, payload,
            "cedarling_current_tokens should round-trip set value"
        );

        crate::token_sql::cedarling_clear_tokens().expect("second clear should succeed");
        assert!(
            crate::token_sql::cedarling_current_tokens().is_none(),
            "after second clear, current tokens should be NULL"
        );
    }

    #[pg_test]
    fn test_cedarling_authorized_fail_closed_without_engine() {
        assert!(
            !crate::authorized::cedarling_authorized(
                r#"{"cedar_entity_mapping":{"entity_type":"T","id":"x"}}"#,
                Some(r#"[{"mapping":"M","payload":"p"}]"#),
                "T::Action::\"A\"",
            ),
            "without bootstrap/engine, fail-closed should deny"
        );
    }

    #[pg_test]
    fn test_cedarling_authorized_fail_open_without_engine() {
        Spi::run("SET LOCAL cedarling.fail_mode = 'open'").expect("SET fail_mode open");
        assert!(
            crate::authorized::cedarling_authorized(
                r#"{"cedar_entity_mapping":{"entity_type":"T","id":"x"}}"#,
                Some(r#"[{"mapping":"M","payload":"p"}]"#),
                "T::Action::\"A\"",
            ),
            "fail-open should allow when engine or authz errors are treated as pass"
        );
        Spi::run("SET LOCAL cedarling.fail_mode = 'closed'").expect("restore fail_mode closed");
    }

    #[pg_test]
    fn test_cedarling_authorized_empty_action_fail_closed() {
        assert!(
            !crate::authorized::cedarling_authorized(
                r#"{"cedar_entity_mapping":{"entity_type":"T","id":"x"}}"#,
                Some(r#"[{"mapping":"M","payload":"p"}]"#),
                "   ",
            ),
            "empty action should deny under fail-closed"
        );
    }

    #[pg_test]
    fn test_cedarling_authorize_unsigned_fail_closed_without_engine() {
        assert!(
            !crate::authorized::cedarling_authorize_unsigned(
                None,
                r#"{"cedar_entity_mapping":{"entity_type":"T","id":"x"}}"#,
                "T::Action::\"A\"",
                "{}",
            ),
            "unsigned: without engine, fail-closed should deny"
        );
    }

    #[pg_test]
    fn test_cedarling_authorize_unsigned_fail_open_without_engine() {
        Spi::run("SET LOCAL cedarling.fail_mode = 'open'").expect("SET fail_mode open");
        assert!(
            crate::authorized::cedarling_authorize_unsigned(
                None,
                r#"{"cedar_entity_mapping":{"entity_type":"T","id":"x"}}"#,
                "T::Action::\"A\"",
                "{}",
            ),
            "unsigned: fail-open should pass on engine/authz errors"
        );
        Spi::run("SET LOCAL cedarling.fail_mode = 'closed'").expect("restore fail_mode closed");
    }

    #[pg_test]
    fn test_cedarling_authorize_unsigned_empty_resource_fail_closed() {
        assert!(
            !crate::authorized::cedarling_authorize_unsigned(None, "   ", "T::Action::\"A\"", "{}",),
            "unsigned: empty resource should deny under fail-closed"
        );
    }

    #[pg_test]
    fn test_cedarling_authorize_unsigned_invalid_context_fail_closed() {
        assert!(
            !crate::authorized::cedarling_authorize_unsigned(
                None,
                r#"{"cedar_entity_mapping":{"entity_type":"T","id":"x"}}"#,
                "T::Action::\"A\"",
                "[]",
            ),
            "unsigned: non-object context JSON should deny under fail-closed"
        );
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
