// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! `PostgreSQL` extension (`cedarling_pg`) for Cedarling-backed authorization.

mod authorized;
mod authz_bridge;
mod authz_cache;
mod catalog;
mod engine;
mod error;
mod extension_log;
mod guc_config;
mod resource;
mod row_authz;
mod status;
mod token_bundle;
mod token_sql;
mod trace;
mod validate;

#[cfg(feature = "pg_test")]
mod pg_test_rls_unsigned;

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
const _: fn() -> guc_config::CedarlingStrategy = guc_config::strategy;
const _: fn() -> i32 = guc_config::cache_size;
const _: fn() -> bool = guc_config::audit_fail_open;
const _: fn() -> i32 = guc_config::clock_skew_seconds;
const _: fn() -> Option<String> = guc_config::policy_version_utf8;
const _: fn(&error::CedarlingError) -> bool = error::CedarlingError::should_deny;
const _: fn(&error::CedarlingError) -> guc_config::CedarlingLogLevelGuc =
    error::CedarlingError::log_level;
const _: fn(&error::CedarlingError, &'static str) -> error::AuditLogEntry =
    error::CedarlingError::to_audit_entry;
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
const _: fn() -> pgrx::datum::JsonB = status::cedarling_status;
const _: fn() -> Option<pgrx::datum::JsonB> = trace::cedarling_last_trace;
const _: fn(Option<i32>) -> pgrx::datum::JsonB = trace::cedarling_recent_traces;
const _: fn(&str, &str) -> pgrx::datum::JsonB = trace::cedarling_explain;
const _: fn(pgrx::datum::JsonB, Option<&str>, Option<&str>) -> String =
    row_authz::cedarling_build_resource;
const _: fn(pgrx::datum::JsonB, &str, Option<pgrx::datum::JsonB>) -> bool =
    row_authz::cedarling_authorized_row;

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
            audit_fail_open, bootstrap_config_path_utf8, cache_size, cache_ttl_seconds,
            clock_skew_seconds, fail_mode, log_level, mode, policy_version_utf8, strategy,
            tokens_utf8, CedarlingFailMode, CedarlingLogLevelGuc, CedarlingMode, CedarlingStrategy,
        };

        assert_eq!(mode(), CedarlingMode::Enforcement, "default mode");
        assert_eq!(strategy(), CedarlingStrategy::Filter, "default strategy");
        assert_eq!(fail_mode(), CedarlingFailMode::Closed, "default fail_mode");
        assert_eq!(log_level(), CedarlingLogLevelGuc::Info, "default log_level");
        assert_eq!(cache_ttl_seconds(), 300, "default cache_ttl");
        assert_eq!(cache_size(), 8192, "default cache_size");
        assert!(audit_fail_open(), "default audit_fail_open");
        assert_eq!(tokens_utf8(), None, "default tokens unset");
        assert_eq!(
            bootstrap_config_path_utf8(),
            None,
            "default bootstrap_config unset"
        );
        assert_eq!(policy_version_utf8(), None, "default policy_version unset");
        assert_eq!(clock_skew_seconds(), 60, "default clock_skew_seconds");

        let show_mode = Spi::get_one::<String>("SHOW cedarling.mode")
            .expect("SPI should succeed for SHOW cedarling.mode");
        assert_eq!(
            show_mode,
            Some("enforcement".to_string()),
            "SHOW should match GUC registration"
        );

        let show_strategy =
            Spi::get_one::<String>("SHOW cedarling.strategy").expect("SHOW cedarling.strategy");
        assert_eq!(show_strategy, Some("filter".to_string()));
    }

    #[pg_test]
    fn test_strategy_guc_accepts_mask() {
        Spi::run("SET cedarling.strategy = 'mask'")
            .expect("SET cedarling.strategy = 'mask' should succeed");
        assert_eq!(
            crate::guc_config::strategy(),
            crate::guc_config::CedarlingStrategy::Mask
        );
        Spi::run("RESET cedarling.strategy").expect("RESET cedarling.strategy");
    }

    #[pg_test]
    fn test_mode_shadow_returns_true_even_when_denied() {
        // With no engine bootstrap and fail_mode=closed, enforcement would deny.
        // Shadow mode must always return true.
        Spi::run("SET LOCAL cedarling.mode = 'shadow'").expect("SET LOCAL mode shadow");
        assert!(
            crate::authorized::cedarling_authorized(
                r#"{"cedar_entity_mapping":{"entity_type":"T","id":"x"}}"#,
                Some(r#"[{"mapping":"M","payload":"p"}]"#),
                "T::Action::\"A\"",
            ),
            "shadow mode should always allow"
        );
        assert!(
            crate::authorized::cedarling_authorize_unsigned(
                None,
                r#"{"cedar_entity_mapping":{"entity_type":"T","id":"x"}}"#,
                "T::Action::\"A\"",
                "{}",
            ),
            "shadow mode should always allow (unsigned path)"
        );
        Spi::run("SET LOCAL cedarling.mode = 'enforcement'").expect("restore enforcement");
    }

    #[pg_test]
    fn test_catalog_schema_and_tables_exist() {
        let ns =
            Spi::get_one::<String>("SELECT nspname FROM pg_namespace WHERE nspname = 'cedarling'")
                .expect("SPI should succeed");
        assert_eq!(ns, Some("cedarling".to_string()));

        let mask_rules = Spi::get_one::<i64>(
            "SELECT count(*) FROM pg_class c \
             JOIN pg_namespace n ON n.oid = c.relnamespace \
             WHERE n.nspname = 'cedarling' AND c.relname = 'mask_rules'",
        )
        .expect("SPI should succeed");
        assert_eq!(mask_rules, Some(1), "cedarling.mask_rules should exist");

        let policy_history = Spi::get_one::<i64>(
            "SELECT count(*) FROM pg_class c \
             JOIN pg_namespace n ON n.oid = c.relnamespace \
             WHERE n.nspname = 'cedarling' AND c.relname = 'policy_history'",
        )
        .expect("SPI should succeed");
        assert_eq!(
            policy_history,
            Some(1),
            "cedarling.policy_history should exist"
        );
    }

    #[pg_test]
    fn test_mask_rules_insert_enforces_mask_type_check() {
        // The CHECK constraint should reject unknown mask_type values.
        let res = Spi::run(
            "INSERT INTO cedarling.mask_rules (table_name, column_name, mask_type) \
             VALUES ('t', 'c', 'bogus_type')",
        );
        assert!(
            res.is_err(),
            "mask_rules CHECK constraint must reject unknown mask_type"
        );

        Spi::run(
            "INSERT INTO cedarling.mask_rules (table_name, column_name, mask_type) \
             VALUES ('t', 'c', 'hash')",
        )
        .expect("valid mask_type 'hash' should be accepted");
        Spi::run("DELETE FROM cedarling.mask_rules WHERE table_name = 't'").expect("cleanup");
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

    #[pg_test]
    fn test_cedarling_status_returns_jsonb_with_required_keys() {
        use pgrx::datum::JsonB;
        let status: JsonB = crate::status::cedarling_status();
        let v = &status.0;
        assert!(
            v.get("total_requests").is_some(),
            "status missing total_requests"
        );
        assert!(v.get("allowed").is_some(), "status missing allowed");
        assert!(v.get("denied").is_some(), "status missing denied");
        assert!(v.get("errors").is_some(), "status missing errors");
        assert!(v.get("cache_hits").is_some(), "status missing cache_hits");
    }

    #[pg_test]
    fn test_cedarling_last_trace_api_does_not_panic() {
        // `cedarling_last_trace()` must not panic regardless of ring buffer state.
        let _ = crate::trace::cedarling_last_trace();
    }

    #[pg_test]
    fn test_cedarling_recent_traces_returns_array() {
        let result = crate::trace::cedarling_recent_traces(Some(5));
        assert!(
            result.0.is_array(),
            "cedarling_recent_traces should return a JSON array"
        );
    }

    #[pg_test]
    fn test_cedarling_explain_empty_action_returns_error_field() {
        let result = crate::trace::cedarling_explain(
            r#"{"cedar_entity_mapping":{"entity_type":"T","id":"x"}}"#,
            "",
        );
        let v = &result.0;
        assert!(
            v.get("error").is_some(),
            "empty action must yield an error field"
        );
        assert!(v["decision"].is_null(), "decision must be null on error");
    }

    #[pg_test]
    fn test_cedarling_authorized_row_fail_closed_without_engine() {
        use pgrx::datum::JsonB;
        use serde_json::json;
        let resource = JsonB(json!({
            "cedar_entity_mapping": {"entity_type": "T", "id": "x"}
        }));
        assert!(
            !crate::row_authz::cedarling_authorized_row(resource, "T::Action::\"A\"", None),
            "cedarling_authorized_row: without engine, fail-closed should deny"
        );
    }

    #[pg_test]
    fn test_cedarling_authorized_row_shadow_mode_always_allows() {
        use pgrx::datum::JsonB;
        use serde_json::json;
        Spi::run("SET LOCAL cedarling.mode = 'shadow'").expect("SET LOCAL mode shadow");
        let resource = JsonB(json!({
            "cedar_entity_mapping": {"entity_type": "T", "id": "x"}
        }));
        assert!(
            crate::row_authz::cedarling_authorized_row(resource, "T::Action::\"A\"", None),
            "cedarling_authorized_row: shadow mode must always allow"
        );
        Spi::run("SET LOCAL cedarling.mode = 'enforcement'").expect("restore enforcement");
    }

    #[pg_test]
    fn test_cedarling_explain_sql_returns_jsonb() {
        let result = Spi::get_one::<pgrx::datum::JsonB>(
            r#"SELECT cedarling_explain(
                '{"cedar_entity_mapping":{"entity_type":"T","id":"x"}}',
                'T::Action::"Read"'
            )"#,
        )
        .expect("SPI should succeed");
        assert!(
            result.is_some(),
            "cedarling_explain should return a non-null JSONB"
        );
        let v = result.unwrap().0;
        assert!(v.get("timestamp").is_some(), "trace must have timestamp");
        assert!(
            v.get("duration_ms").is_some(),
            "trace must have duration_ms"
        );
    }

    #[pg_test]
    fn test_clock_skew_guc_default_and_set() {
        assert_eq!(
            crate::guc_config::clock_skew_seconds(),
            60,
            "default clock_skew_seconds must be 60"
        );
        Spi::run("SET cedarling.clock_skew_seconds = 120").expect("SET clock_skew");
        assert_eq!(crate::guc_config::clock_skew_seconds(), 120);
        Spi::run("RESET cedarling.clock_skew_seconds").expect("RESET clock_skew");
    }

    /// RLS + `cedarling_authorize_unsigned` against the unsigned policy store (runs late by name so
    /// earlier `#[pg_test]` cases still see an uninitialized Cedarling engine in the same process).
    #[pg_test]
    fn test_zzz_rls_unsigned_policy_filters_select_under_row_security() {
        crate::pg_test_rls_unsigned::run_rls_unsigned_policy_filters_select_under_row_security();
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
