// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! `PostgreSQL` extension (`cedarling_pg`) for Cedarling-backed authorization.

mod authz;
mod catalog;
mod engine;
mod functions;
mod guc_config;
mod mask;
mod observability;
mod policy;
mod resource;
mod tokens;
mod validate;

// Transparent aliases so existing paths (crate::authorized::*, crate::row_authz::*,
// crate::error::*) remain valid while the real implementations live in functions/.
mod authorized {
    pub(crate) use crate::functions::authorized::*;
}
mod row_authz {
    pub(crate) use crate::functions::authorized_row::*;
    pub(crate) use crate::functions::build_resource::*;
}
mod error {
    pub(crate) use crate::functions::error::*;
}

mod authz_bridge {
    pub(crate) use crate::authz::bridge::*;
}

mod authz_cache {
    pub(crate) use crate::authz::cache::*;
}

mod where_sql {
    pub(crate) use crate::authz::where_clause::*;
}

mod token_bundle {
    pub(crate) use crate::tokens::bundle::*;
}

#[allow(unused_imports)]
mod token_sql {
    pub(crate) use crate::tokens::sql::*;
}

mod extension_log {
    pub(crate) use crate::observability::log::*;
}

mod trace {
    pub(crate) use crate::observability::trace::*;
}

mod status {
    pub(crate) use crate::observability::status::*;
}

mod policy_sql {
    pub(crate) use crate::policy::versions::*;
}

mod schema_sql {
    pub(crate) use crate::policy::schema::*;
}

mod mask_sql {
    pub(crate) use crate::mask::*;
}


use pgrx::prelude::*;

const _: usize = core::mem::size_of::<cedarling::blocking::Cedarling>();

// Keep resource parsers reachable for `dead_code` when the cdylib is built without `cfg(test)`.
const _: fn(&str) -> Result<cedarling::EntityData, resource::ResourceEntityDataError> =
    resource::resource_entity_data_from_json_str;
const _: fn(serde_json::Value) -> Result<cedarling::EntityData, resource::ResourceEntityDataError> =
    resource::resource_entity_data_from_json_value;
const _: fn(pgrx::datum::AnyElement) -> Result<String, resource::row::RowBuildError> =
    resource::row::build_resource_json_from_row;
const _: fn(pg_sys::Oid, &str, Vec<String>) -> bool = resource::schema_map::cedarling_register_entity_map;
const _: fn(&str) -> Result<std::sync::Arc<cedarling::blocking::Cedarling>, engine::EngineError> =
    engine::try_init_cedarling_from_bootstrap_path;
const _: fn() -> Result<std::sync::Arc<cedarling::blocking::Cedarling>, engine::EngineError> =
    engine::global_cedarling;
const _: fn() -> guc_config::CedarlingMode = guc_config::mode;
const _: fn() -> guc_config::CedarlingLogLevelGuc = guc_config::log_level;
const _: fn() -> guc_config::CedarlingStrategy = guc_config::strategy;
const _: fn() -> i32 = guc_config::cache_size;
const _: fn() -> bool = guc_config::audit_fail_open;
const _: fn() -> i32 = guc_config::trace_buffer_size;
const _: fn() -> i32 = guc_config::policy_history_size;
const _: fn() -> bool = guc_config::schema_validate_strict;
const _: fn() -> Option<String> = guc_config::context_utf8;
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
    Option<&str>,
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
const _: fn(&str, &str) -> bool = policy_sql::cedarling_register_policy_version;
const _: fn(&str) -> bool = policy_sql::cedarling_use_policy;
const _: fn() -> bool = policy_sql::cedarling_rollback_policy;
const _: fn(&str, &str) -> pgrx::datum::JsonB = policy_sql::cedarling_diff_policies;
const _: fn(&str, Option<&str>) -> pgrx::datum::JsonB = mask_sql::cedarling_mask_plan;
const _: fn(pgrx::datum::JsonB, &str, Option<&str>) -> pgrx::datum::JsonB =
    mask_sql::cedarling_mask_row;
const _: fn() -> Option<pgrx::datum::JsonB> = mask_sql::cedarling_get_masked_row;
const _: fn(&str, &str, &str, Option<&str>) -> bool = mask_sql::cedarling_set_mask_config;
const _: fn(Option<&str>, Option<&str>, &str, Option<&str>) -> Option<String> =
    mask_sql::cedarling_test_masking;
const _: fn(&str, &str) -> pgrx::datum::JsonB = schema_sql::cedarling_validate_schema;
const _: fn(pg_sys::Oid, &str) -> pgrx::datum::JsonB = schema_sql::cedarling_validate_schema_by_oid;
const _: fn(&str, &str, Option<&str>) -> String = where_sql::cedarling_where;
const _: fn() -> Option<pgrx::datum::JsonB> = trace::cedarling_last_trace;
const _: fn(Option<i32>) -> pgrx::datum::JsonB = trace::cedarling_recent_traces;
const _: fn(&str, &str) -> pgrx::datum::JsonB = trace::cedarling_explain;
const _: fn(pgrx::datum::JsonB, Option<&str>, Option<&str>) -> String =
    row_authz::cedarling_build_resource;
const _: fn(pgrx::datum::AnyElement) -> String = row_authz::cedarling_build_resource_anyelement;
const _: fn(pgrx::datum::JsonB, &str, Option<pgrx::datum::JsonB>) -> bool =
    row_authz::cedarling_authorized_row;
const _: fn(pgrx::datum::AnyElement, &str, Option<pgrx::datum::JsonB>) -> bool =
    row_authz::cedarling_authorized_row_from_anyelement;
const _: fn(pgrx::datum::AnyElement, Option<&str>) -> bool = row_authz::cedarling_authorized_row_jwt;

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
            audit_fail_open, bootstrap_config_path_utf8, cache_size, cache_ttl_seconds, diff_mode,
            fail_mode, log_level, mode, policy_history_size, policy_version_utf8,
            schema_validate_strict, strategy,
            trace_buffer_size, tokens_utf8, CedarlingDiffMode, CedarlingFailMode,
            CedarlingLogLevelGuc, CedarlingMode, CedarlingStrategy,
        };

        assert_eq!(mode(), CedarlingMode::Enforcement, "default mode");
        assert_eq!(strategy(), CedarlingStrategy::Filter, "default strategy");
        assert_eq!(fail_mode(), CedarlingFailMode::Closed, "default fail_mode");
        assert_eq!(log_level(), CedarlingLogLevelGuc::Info, "default log_level");
        assert_eq!(cache_ttl_seconds(), 300, "default cache_ttl");
        assert_eq!(cache_size(), 8192, "default cache_size");
        assert!(audit_fail_open(), "default audit_fail_open");
        assert_eq!(trace_buffer_size(), 1024, "default trace_buffer_size");
        assert_eq!(policy_history_size(), 16, "default policy_history_size");
        assert!(schema_validate_strict(), "default schema_validate_strict");
        assert_eq!(tokens_utf8(), None, "default tokens unset");
        assert_eq!(crate::guc_config::context_utf8(), None, "default context unset");
        assert_eq!(
            bootstrap_config_path_utf8(),
            None,
            "default bootstrap_config unset"
        );
        assert_eq!(policy_version_utf8(), None, "default policy_version unset");
        assert_eq!(diff_mode(), CedarlingDiffMode::Structural, "default diff_mode");

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

        let show_trace = Spi::get_one::<String>("SHOW cedarling.trace_buffer_size")
            .expect("SHOW cedarling.trace_buffer_size");
        assert_eq!(show_trace, Some("1024".to_string()));

        let show_schema_strict = Spi::get_one::<String>("SHOW cedarling.schema_validate_strict")
            .expect("SHOW cedarling.schema_validate_strict");
        assert_eq!(show_schema_strict, Some("on".to_string()));
    }

    #[pg_test]
    fn test_context_guc_accepts_json_object() {
        Spi::run(r#"SET cedarling.context = '{"tenant":"acme","ip":"127.0.0.1"}'"#)
            .expect("cedarling.context should accept valid object JSON");
        assert_eq!(
            crate::guc_config::context_utf8(),
            Some(r#"{"tenant":"acme","ip":"127.0.0.1"}"#.to_string())
        );
        Spi::run("RESET cedarling.context").expect("RESET cedarling.context");
    }

    #[pg_test]
    fn test_policy_segment_changes_with_policy_version() {
        Spi::run("SET cedarling.policy_version = 'v1'").expect("set policy version v1");
        let a = crate::authz_cache::policy_segment_from_bootstrap_path();
        Spi::run("SET cedarling.policy_version = 'v2'").expect("set policy version v2");
        let b = crate::authz_cache::policy_segment_from_bootstrap_path();
        assert_ne!(
            a, b,
            "cache policy segment should include cedarling.policy_version"
        );
        Spi::run("RESET cedarling.policy_version").expect("reset policy version");
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
        let ns = Spi::get_one::<String>(
            "SELECT nspname::text FROM pg_namespace WHERE nspname = 'cedarling'",
        )
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

        let entity_map = Spi::get_one::<i64>(
            "SELECT count(*) FROM pg_class c \
             JOIN pg_namespace n ON n.oid = c.relnamespace \
             WHERE n.nspname = 'cedarling' AND c.relname = 'entity_map'",
        )
        .expect("SPI should succeed");
        assert_eq!(entity_map, Some(1), "cedarling.entity_map should exist");

        let policy_versions = Spi::get_one::<i64>(
            "SELECT count(*) FROM pg_class c \
             JOIN pg_namespace n ON n.oid = c.relnamespace \
             WHERE n.nspname = 'cedarling' AND c.relname = 'policy_versions'",
        )
        .expect("SPI should succeed");
        assert_eq!(policy_versions, Some(1), "cedarling.policy_versions should exist");
    }

    #[pg_test]
    fn test_mask_rules_insert_enforces_mask_type_check() {
        // The CHECK constraint should reject unknown mask_type values. Use a DO block so the
        // expected check_violation is not raised to the #[pg_test] SQL wrapper as FATAL ERROR.
        Spi::run(
            r#"DO $do$
               BEGIN
                 INSERT INTO cedarling.mask_rules (table_name, column_name, mask_type)
                 VALUES ('t', 'c', 'bogus_type');
                 RAISE EXCEPTION 'mask_rules CHECK should have rejected bogus_type';
               EXCEPTION
                 WHEN check_violation THEN
                   NULL;
               END
               $do$"#,
        )
        .expect("DO block should catch check_violation for bogus mask_type");

        Spi::run(
            "INSERT INTO cedarling.mask_rules (table_name, column_name, mask_type) \
             VALUES ('t', 'c', 'hash')",
        )
        .expect("valid mask_type 'hash' should be accepted");
        Spi::run("DELETE FROM cedarling.mask_rules WHERE table_name = 't'").expect("cleanup");
    }

    #[pg_test]
    fn test_policy_use_and_rollback_update_history_and_version() {
        use std::fs;
        use std::io::Write;

        const POLICY_UNSIGNED: &str = include_str!(concat!(
            env!("CARGO_MANIFEST_DIR"),
            "/../test_files/policy-store_no_trusted_issuers.yaml"
        ));

        fn write_bootstrap(
            dir: &std::path::Path,
            policy_path: &std::path::Path,
        ) -> std::path::PathBuf {
            let bootstrap_path = dir.join("bootstrap.yaml");
            let policy_lit = policy_path.to_string_lossy();
            let contents = format!(
                "CEDARLING_APPLICATION_NAME: cedarling_pg_policy_test\n\
                 CEDARLING_POLICY_STORE_URI: ''\n\
                 CEDARLING_LOG_TYPE: memory\n\
                 CEDARLING_LOG_LEVEL: DEBUG\n\
                 CEDARLING_LOG_TTL: 60\n\
                 CEDARLING_LOCAL_JWKS: null\n\
                 CEDARLING_POLICY_STORE_LOCAL: null\n\
                 CEDARLING_POLICY_STORE_LOCAL_FN: {policy_lit}\n\
                 CEDARLING_JWT_SIG_VALIDATION: disabled\n\
                 CEDARLING_JWT_STATUS_VALIDATION: disabled\n\
                 CEDARLING_LOCK: disabled\n\
                 CEDARLING_LOCK_SERVER_CONFIGURATION_URI: null\n\
                 CEDARLING_LOCK_DYNAMIC_CONFIGURATION: disabled\n\
                 CEDARLING_LOCK_HEALTH_INTERVAL: 0\n\
                 CEDARLING_LOCK_TELEMETRY_INTERVAL: 0\n\
                 CEDARLING_LOCK_LISTEN_SSE: disabled\n"
            );
            let mut f = fs::File::create(&bootstrap_path).expect("bootstrap file");
            f.write_all(contents.as_bytes()).expect("write bootstrap");
            bootstrap_path
        }

        let work =
            std::env::temp_dir().join(format!("cedarling_pg_policy_test_{}", std::process::id()));
        let _ = fs::remove_dir_all(&work);
        fs::create_dir_all(&work).expect("temp work dir");

        let p1 = work.join("policy-1.yaml");
        let p2 = work.join("policy-2.yaml");
        fs::write(&p1, POLICY_UNSIGNED.as_bytes()).expect("write policy 1");
        fs::write(&p2, POLICY_UNSIGNED.as_bytes()).expect("write policy 2");
        let b1 = write_bootstrap(&work, &p1);
        let b2 = write_bootstrap(&work, &p2);

        let b1s = b1.to_str().expect("utf8 bootstrap 1");
        let b2s = b2.to_str().expect("utf8 bootstrap 2");
        assert!(crate::policy_sql::cedarling_use_policy(b1s));
        assert!(crate::policy_sql::cedarling_use_policy(b2s));
        assert!(crate::policy_sql::cedarling_rollback_policy());

        let pv =
            Spi::get_one::<String>("SHOW cedarling.policy_version").expect("SHOW policy_version");
        assert_eq!(pv, Some(b1s.to_string()));

        let history_count = Spi::get_one::<i64>(
            "SELECT count(*) FROM cedarling.policy_history WHERE operation IN ('use','rollback')",
        )
        .expect("policy history count");
        assert!(
            history_count.unwrap_or_default() >= 3,
            "expected use/use/rollback entries in cedarling.policy_history"
        );

        let _ = fs::remove_dir_all(&work);
    }

    #[pg_test]
    fn test_policy_diff_structural_detects_added_policy() {
        use std::fs;

        let work =
            std::env::temp_dir().join(format!("cedarling_pg_diff_test_{}", std::process::id()));
        let _ = fs::remove_dir_all(&work);
        fs::create_dir_all(&work).expect("temp work dir");
        let old = work.join("old.cedar");
        let new = work.join("new.cedar");
        fs::write(&old, "permit(principal, action, resource);\n").expect("write old");
        fs::write(
            &new,
            "permit(principal, action, resource);\nforbid(principal, action, resource);\n",
        )
        .expect("write new");

        Spi::run("RESET cedarling.diff_mode").expect("reset diff_mode");
        let diff = crate::policy_sql::cedarling_diff_policies(
            old.to_str().expect("old path utf8"),
            new.to_str().expect("new path utf8"),
        );
        let v = diff.0;
        assert_eq!(v["ok"], true);
        let added = v["added"].as_array().cloned().unwrap_or_default();
        assert!(!added.is_empty(), "structural diff must report added forbid policy");
        assert!(
            added.iter().any(|e| e.get("effect").and_then(|e| e.as_str()) == Some("forbid")),
            "added entry must carry effect=forbid"
        );
        assert!(added.iter().all(|e| e.get("id").is_some()), "each added entry must have an id");
        assert!(v["removed"].as_array().is_some_and(|a| a.is_empty()), "no policies removed");

        let _ = fs::remove_dir_all(&work);
    }

    #[pg_test]
    fn test_policy_history_trimmed_to_size_guc() {
        use std::fs;
        use std::io::Write;

        const POLICY_UNSIGNED: &str = include_str!(concat!(
            env!("CARGO_MANIFEST_DIR"),
            "/../test_files/policy-store_no_trusted_issuers.yaml"
        ));

        let work = std::env::temp_dir()
            .join(format!("cedarling_pg_trim_test_{}", std::process::id()));
        let _ = fs::remove_dir_all(&work);
        fs::create_dir_all(&work).expect("temp work dir");

        let policy_path = work.join("policy.yaml");
        fs::write(&policy_path, POLICY_UNSIGNED.as_bytes()).expect("write policy");
        let policy_lit = policy_path.to_string_lossy();
        let bootstrap_contents = format!(
            "CEDARLING_APPLICATION_NAME: cedarling_pg_trim_test\n\
             CEDARLING_POLICY_STORE_URI: ''\n\
             CEDARLING_LOG_TYPE: memory\n\
             CEDARLING_LOG_LEVEL: DEBUG\n\
             CEDARLING_LOG_TTL: 60\n\
             CEDARLING_LOCAL_JWKS: null\n\
             CEDARLING_POLICY_STORE_LOCAL: null\n\
             CEDARLING_POLICY_STORE_LOCAL_FN: {policy_lit}\n\
             CEDARLING_JWT_SIG_VALIDATION: disabled\n\
             CEDARLING_JWT_STATUS_VALIDATION: disabled\n\
             CEDARLING_LOCK: disabled\n\
             CEDARLING_LOCK_SERVER_CONFIGURATION_URI: null\n\
             CEDARLING_LOCK_DYNAMIC_CONFIGURATION: disabled\n\
             CEDARLING_LOCK_HEALTH_INTERVAL: 0\n\
             CEDARLING_LOCK_TELEMETRY_INTERVAL: 0\n\
             CEDARLING_LOCK_LISTEN_SSE: disabled\n"
        );
        let bootstrap_path = work.join("bootstrap.yaml");
        let mut f = fs::File::create(&bootstrap_path).expect("bootstrap file");
        f.write_all(bootstrap_contents.as_bytes()).expect("write bootstrap");
        let bs = bootstrap_path.to_str().expect("utf8 bootstrap");

        // Limit history to 2 rows for this session.
        Spi::run("SET LOCAL cedarling.policy_history_size = 2").expect("set policy_history_size");
        // Delete existing history so counts are predictable.
        Spi::run("DELETE FROM cedarling.policy_history").expect("clear history");

        // Perform 5 use operations — only the 2 most recent should survive.
        for _ in 0..5 {
            crate::policy_sql::cedarling_use_policy(bs);
        }

        let count = Spi::get_one::<i64>("SELECT count(*) FROM cedarling.policy_history")
            .expect("count history")
            .unwrap_or_default();
        assert_eq!(
            count, 2,
            "history must be trimmed to policy_history_size=2, got {count} rows"
        );

        Spi::run("RESET cedarling.policy_history_size").expect("reset policy_history_size");
        let _ = fs::remove_dir_all(&work);
    }

    #[pg_test]
    fn test_policy_diff_lines_mode_returns_string_lines() {
        use std::fs;

        let work =
            std::env::temp_dir().join(format!("cedarling_pg_diff_lines_{}", std::process::id()));
        let _ = fs::remove_dir_all(&work);
        fs::create_dir_all(&work).expect("temp work dir");
        let old = work.join("old.cedar");
        let new = work.join("new.cedar");
        fs::write(&old, "permit(principal, action, resource);\n").expect("write old");
        fs::write(
            &new,
            "permit(principal, action, resource);\nforbid(principal, action, resource);\n",
        )
        .expect("write new");

        Spi::run("SET LOCAL cedarling.diff_mode = 'lines'").expect("set diff_mode lines");
        let diff = crate::policy_sql::cedarling_diff_policies(
            old.to_str().expect("old path utf8"),
            new.to_str().expect("new path utf8"),
        );
        let v = diff.0;
        assert_eq!(v["ok"], true);
        let added = v["added"].as_array().cloned().unwrap_or_default();
        assert!(
            added.contains(&serde_json::json!("forbid(principal, action, resource);")),
            "lines mode must report added line as a string"
        );
        Spi::run("RESET cedarling.diff_mode").expect("reset diff_mode");
        let _ = fs::remove_dir_all(&work);
    }

    #[pg_test]
    fn test_policy_versions_registry_roundtrip() {
        use std::fs;
        use std::io::Write;

        let work = std::env::temp_dir()
            .join(format!("cedarling_pg_versions_test_{}", std::process::id()));
        let _ = fs::remove_dir_all(&work);
        fs::create_dir_all(&work).expect("temp work dir");

        let bootstrap_path = work.join("bootstrap.yaml");
        let policy_path = work.join("policy.yaml");
        const POLICY_UNSIGNED: &str = include_str!(concat!(
            env!("CARGO_MANIFEST_DIR"),
            "/../test_files/policy-store_no_trusted_issuers.yaml"
        ));
        fs::write(&policy_path, POLICY_UNSIGNED.as_bytes()).expect("write policy");
        let policy_lit = policy_path.to_string_lossy();
        let bootstrap_contents = format!(
            "CEDARLING_APPLICATION_NAME: cedarling_pg_versions_test\n\
             CEDARLING_POLICY_STORE_URI: ''\n\
             CEDARLING_LOG_TYPE: memory\n\
             CEDARLING_LOG_LEVEL: DEBUG\n\
             CEDARLING_LOG_TTL: 60\n\
             CEDARLING_LOCAL_JWKS: null\n\
             CEDARLING_POLICY_STORE_LOCAL: null\n\
             CEDARLING_POLICY_STORE_LOCAL_FN: {policy_lit}\n\
             CEDARLING_JWT_SIG_VALIDATION: disabled\n\
             CEDARLING_JWT_STATUS_VALIDATION: disabled\n\
             CEDARLING_LOCK: disabled\n\
             CEDARLING_LOCK_SERVER_CONFIGURATION_URI: null\n\
             CEDARLING_LOCK_DYNAMIC_CONFIGURATION: disabled\n\
             CEDARLING_LOCK_HEALTH_INTERVAL: 0\n\
             CEDARLING_LOCK_TELEMETRY_INTERVAL: 0\n\
             CEDARLING_LOCK_LISTEN_SSE: disabled\n"
        );
        let mut f = fs::File::create(&bootstrap_path).expect("bootstrap file");
        f.write_all(bootstrap_contents.as_bytes()).expect("write bootstrap");

        let bootstrap_str = bootstrap_path.to_str().expect("utf8 bootstrap");

        // Register a named version and use it by name.
        assert!(
            crate::policy_sql::cedarling_register_policy_version("v_test_reg", bootstrap_str),
            "cedarling_register_policy_version must return true"
        );

        let stored = Spi::get_one_with_args::<String>(
            "SELECT bootstrap_path FROM cedarling.policy_versions WHERE name = $1",
            &["v_test_reg".into()],
        )
        .expect("SPI query")
        .expect("row must exist");
        assert_eq!(stored, bootstrap_str, "stored bootstrap_path must match");

        // cedarling_use_policy resolves the name and loads the engine.
        assert!(
            crate::policy_sql::cedarling_use_policy("v_test_reg"),
            "cedarling_use_policy by name must succeed"
        );
        let pv = Spi::get_one::<String>("SHOW cedarling.policy_version")
            .expect("SHOW policy_version")
            .expect("policy_version must be set");
        assert_eq!(
            pv, "v_test_reg",
            "policy_version GUC must reflect the resolved registry name"
        );

        Spi::run("DELETE FROM cedarling.policy_versions WHERE name = 'v_test_reg'")
            .expect("cleanup policy_versions");
        let _ = fs::remove_dir_all(&work);
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
    fn test_fail_closed_without_engine() {
        let res = r#"{"cedar_entity_mapping":{"entity_type":"T","id":"x"}}"#;
        let tok = Some(r#"[{"mapping":"M","payload":"p"}]"#);
        let act = "T::Action::\"A\"";
        assert!(!crate::authorized::cedarling_authorized(res, tok, act));
        assert!(!crate::authorized::cedarling_authorize_unsigned(None, res, act, "{}"));
    }

    #[pg_test]
    fn test_fail_open_without_engine() {
        Spi::run("SET LOCAL cedarling.fail_mode = 'open'").expect("SET fail_mode open");
        let res = r#"{"cedar_entity_mapping":{"entity_type":"T","id":"x"}}"#;
        let tok = Some(r#"[{"mapping":"M","payload":"p"}]"#);
        let act = "T::Action::\"A\"";
        assert!(crate::authorized::cedarling_authorized(res, tok, act));
        assert!(crate::authorized::cedarling_authorize_unsigned(None, res, act, "{}"));
        Spi::run("SET LOCAL cedarling.fail_mode = 'closed'").expect("restore fail_mode closed");
    }

    #[pg_test]
    fn test_input_validation_fail_closed() {
        // Empty action, empty resource, and non-object context all deny under fail-closed.
        let res = r#"{"cedar_entity_mapping":{"entity_type":"T","id":"x"}}"#;
        let tok = Some(r#"[{"mapping":"M","payload":"p"}]"#);
        assert!(!crate::authorized::cedarling_authorized(res, tok, "   "));
        assert!(!crate::authorized::cedarling_authorize_unsigned(None, "   ", "T::Action::\"A\"", "{}"));
        assert!(!crate::authorized::cedarling_authorize_unsigned(None, res, "T::Action::\"A\"", "[]"));
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
    fn test_anyelement_build_resource_and_jwt_row_helper_exist() {
        Spi::run(
            "CREATE TEMP TABLE cedarling_pg_row_probe (id int4 NOT NULL, owner text NOT NULL)",
        )
        .expect("create temp probe table");
        Spi::run("INSERT INTO cedarling_pg_row_probe(id, owner) VALUES (42, 'alice')")
            .expect("insert probe row");

        let built = Spi::get_one::<String>(
            r#"SELECT cedarling_build_resource_row(t) FROM cedarling_pg_row_probe t"#,
        )
        .expect("SPI call should succeed");
        let built = built.expect("resource string expected");
        assert!(
            built.contains("cedar_entity_mapping"),
            "anyelement builder should inject cedar_entity_mapping"
        );

        let allowed = Spi::get_one::<bool>(
            r#"SELECT cedarling_authorized_row_jwt(t, 'Read') FROM cedarling_pg_row_probe t"#,
        )
        .expect("SPI jwt row helper should succeed");
        assert_eq!(allowed, Some(false));

        Spi::run("DROP TABLE IF EXISTS cedarling_pg_row_probe").expect("drop probe table");
    }

    #[pg_test]
    fn test_entity_map_override_and_composite_pk_id() {
        Spi::run("DROP TABLE IF EXISTS cedarling_phase2_pk").expect("drop test table");
        Spi::run(
            "CREATE TABLE cedarling_phase2_pk(
                id1 int4 NOT NULL,
                id2 int4 NOT NULL,
                owner text NOT NULL,
                PRIMARY KEY(id1, id2)
             )",
        )
        .expect("create test table");
        Spi::run("INSERT INTO cedarling_phase2_pk(id1, id2, owner) VALUES (10, 20, 'alice')")
            .expect("insert test row");

        let default_id = Spi::get_one::<String>(
            r#"SELECT cedarling_build_resource_row(t)::jsonb #>> '{cedar_entity_mapping,id}'
               FROM cedarling_phase2_pk t
               LIMIT 1"#,
        )
        .expect("default mapping query");
        assert_eq!(default_id, Some("10-20".to_string()));

        let registered = Spi::get_one::<bool>(
            r#"SELECT cedarling_register_entity_map(
                   'cedarling_phase2_pk'::regclass::oid,
                   'Jans::CompositeThing',
                   ARRAY['id2','id1']
               )"#,
        )
        .expect("register entity map query");
        assert_eq!(registered, Some(true));

        let mapped_entity_type = Spi::get_one::<String>(
            r#"SELECT cedarling_build_resource_row(t)::jsonb #>> '{cedar_entity_mapping,entity_type}'
               FROM cedarling_phase2_pk t
               LIMIT 1"#,
        )
        .expect("mapped entity type query");
        assert_eq!(mapped_entity_type, Some("Jans::CompositeThing".to_string()));

        let mapped_id = Spi::get_one::<String>(
            r#"SELECT cedarling_build_resource_row(t)::jsonb #>> '{cedar_entity_mapping,id}'
               FROM cedarling_phase2_pk t
               LIMIT 1"#,
        )
        .expect("mapped id query");
        assert_eq!(mapped_id, Some("20-10".to_string()));

        Spi::run("DROP TABLE IF EXISTS cedarling_phase2_pk").expect("drop test table");
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
    fn test_mask_config_and_mask_row_roundtrip() {
        use pgrx::datum::JsonB;
        use serde_json::json;
        assert!(crate::mask_sql::cedarling_set_mask_config(
            "test_tbl", "email", "redact", None
        ));
        let row = JsonB(json!({"email":"alice@example.org","name":"alice"}));
        let masked = crate::mask_sql::cedarling_mask_row(row, "test_tbl", Some("Read"));
        assert_eq!(masked.0["email"], "***REDACTED***");
        let last = crate::mask_sql::cedarling_get_masked_row().expect("last masked row");
        assert_eq!(last.0["email"], "***REDACTED***");
        let _ = Spi::run("DELETE FROM cedarling.mask_rules WHERE table_name = 'test_tbl'");
    }

    #[pg_test]
    fn test_cedarling_where_returns_false_without_tokens() {
        Spi::run("RESET cedarling.tokens").expect("RESET tokens");
        let pred = crate::where_sql::cedarling_where("some_table", "Jans::Action::\"Read\"", None);
        assert_eq!(pred, "FALSE");
    }

    #[pg_test]
    fn test_cedarling_where_unsigned_unconditional_permit_returns_true() {
        crate::authz::pg_test_where::run_unsigned_unconditional_permit_returns_true();
    }

    #[pg_test]
    fn test_cedarling_where_unsigned_no_matching_action_returns_false() {
        crate::authz::pg_test_where::run_unsigned_no_matching_action_returns_false();
    }

    #[pg_test]
    fn test_cedarling_where_unsigned_resource_predicate_lowers_to_sql() {
        crate::authz::pg_test_where::run_unsigned_resource_predicate_lowers_to_sql();
    }

    #[pg_test]
    fn test_cedarling_where_unsigned_unhandled_predicate_returns_partial_true() {
        crate::authz::pg_test_where::run_unsigned_unhandled_predicate_returns_partial_true();
    }

    #[pg_test]
    fn test_cedarling_where_unsigned_predicate_matches_rls_count_parity() {
        crate::authz::pg_test_where::run_unsigned_predicate_matches_rls_count_parity();
    }

    #[pg_test]
    fn test_validate_schema_reports_columns_not_in_schema() {
        use std::fs;
        let work =
            std::env::temp_dir().join(format!("cedarling_pg_schema_test_{}", std::process::id()));
        let _ = fs::remove_dir_all(&work);
        fs::create_dir_all(&work).expect("temp dir");
        let schema_path = work.join("schema.cedarschema");
        fs::write(&schema_path, "entity User { name: String, age: Long }\n").expect("write schema");

        Spi::run("DROP TABLE IF EXISTS cedarling_schema_probe")
            .expect("drop cedarling_schema_probe");
        Spi::run("CREATE TABLE cedarling_schema_probe(name text, age int, extra text)")
            .expect("create cedarling_schema_probe");

        let report = crate::schema_sql::cedarling_validate_schema(
            "cedarling_schema_probe",
            schema_path.to_str().expect("utf8 schema path"),
        );
        assert!(
            report.0["missing_in_schema"].as_array().is_some(),
            "validate_schema should return missing_in_schema array"
        );
        Spi::run("DROP TABLE IF EXISTS cedarling_schema_probe").expect("drop probe table");
        let _ = fs::remove_dir_all(&work);
    }

    #[pg_test]
    fn test_validate_schema_reports_type_mismatch_strict_mode() {
        use std::fs;
        let work = std::env::temp_dir()
            .join(format!("cedarling_pg_schema_mismatch_{}", std::process::id()));
        let _ = fs::remove_dir_all(&work);
        fs::create_dir_all(&work).expect("temp dir");
        let schema_path = work.join("schema.cedarschema");
        fs::write(
            &schema_path,
            r#"
            namespace Jans {
                entity Probe = {
                    "name": String,
                    "age": String
                };
            }
            "#,
        )
        .expect("write schema");

        Spi::run("DROP TABLE IF EXISTS cedarling_schema_mismatch_probe")
            .expect("drop cedarling_schema_mismatch_probe");
        Spi::run("CREATE TABLE cedarling_schema_mismatch_probe(name text, age int4)")
            .expect("create cedarling_schema_mismatch_probe");
        Spi::run(
            "SELECT cedarling_register_entity_map(
                'cedarling_schema_mismatch_probe'::regclass::oid,
                'Jans::Probe',
                ARRAY['name']
            )",
        )
        .expect("register schema mismatch probe mapping");

        let report = crate::schema_sql::cedarling_validate_schema(
            "cedarling_schema_mismatch_probe",
            schema_path.to_str().expect("utf8 schema path"),
        );
        let mismatches = report.0["type_mismatches"]
            .as_array()
            .cloned()
            .unwrap_or_default();
        assert!(
            mismatches
                .iter()
                .any(|m| m.get("column").and_then(|v| v.as_str()) == Some("age")),
            "strict mode must report type mismatch for age int4 vs String, got report: {}",
            report.0
        );

        Spi::run("DROP TABLE IF EXISTS cedarling_schema_mismatch_probe").expect("drop probe table");
        let _ = fs::remove_dir_all(&work);
    }

    #[pg_test]
    fn test_validate_schema_matching_shapes_returns_ok_true() {
        use std::fs;
        let work = std::env::temp_dir()
            .join(format!("cedarling_pg_schema_match_{}", std::process::id()));
        let _ = fs::remove_dir_all(&work);
        fs::create_dir_all(&work).expect("temp dir");
        let schema_path = work.join("schema.cedarschema");
        fs::write(
            &schema_path,
            r#"
            namespace Jans {
                entity MatchProbe = {
                    "name": String,
                    "score": Long
                };
            }
            "#,
        )
        .expect("write schema");

        Spi::run("DROP TABLE IF EXISTS cedarling_schema_match_probe")
            .expect("drop cedarling_schema_match_probe");
        Spi::run("CREATE TABLE cedarling_schema_match_probe(name text, score int8)")
            .expect("create cedarling_schema_match_probe");
        Spi::run(
            "SELECT cedarling_register_entity_map(
                'cedarling_schema_match_probe'::regclass::oid,
                'Jans::MatchProbe',
                ARRAY['name']
            )",
        )
        .expect("register schema match probe mapping");

        let report = crate::schema_sql::cedarling_validate_schema(
            "cedarling_schema_match_probe",
            schema_path.to_str().expect("utf8 schema path"),
        );
        let v = &report.0;
        assert_eq!(v["ok"], true, "matching shapes must return ok=true, got: {v}");
        assert!(
            v["missing_in_table"].as_array().is_some_and(|a| a.is_empty()),
            "missing_in_table must be empty"
        );
        assert!(
            v["missing_in_schema"].as_array().is_some_and(|a| a.is_empty()),
            "missing_in_schema must be empty"
        );
        assert!(
            v["type_mismatches"].as_array().is_some_and(|a| a.is_empty()),
            "type_mismatches must be empty"
        );

        Spi::run("DROP TABLE IF EXISTS cedarling_schema_match_probe").expect("drop probe table");
        let _ = fs::remove_dir_all(&work);
    }

    #[pg_test]
    fn test_validate_schema_unknown_table_returns_ok_false() {
        // The table OID lookup fails before schema parsing, so schema content doesn't matter.
        let report = crate::schema_sql::cedarling_validate_schema(
            "cedarling_nonexistent_table_xyzzy_abc123",
            "/nonexistent/path/schema.cedarschema",
        );
        let v = &report.0;
        assert_eq!(v["ok"], false, "unknown table must return ok=false, got: {v}");
        assert!(
            v["error"].as_str().is_some(),
            "error field must be present for unknown table"
        );
    }

    #[pg_test]
    fn test_validate_schema_by_oid_accepts_table_oid() {
        use std::fs;
        let work = std::env::temp_dir()
            .join(format!("cedarling_pg_schema_byoid_{}", std::process::id()));
        let _ = fs::remove_dir_all(&work);
        fs::create_dir_all(&work).expect("temp dir");
        let schema_path = work.join("schema.cedarschema");
        fs::write(
            &schema_path,
            r#"
            namespace Jans {
                entity OidProbe = {
                    "name": String
                };
            }
            "#,
        )
        .expect("write schema");

        Spi::run("DROP TABLE IF EXISTS cedarling_schema_byoid_probe")
            .expect("drop cedarling_schema_byoid_probe");
        Spi::run("CREATE TABLE cedarling_schema_byoid_probe(name text)")
            .expect("create cedarling_schema_byoid_probe");
        Spi::run(
            "SELECT cedarling_register_entity_map(
                'cedarling_schema_byoid_probe'::regclass::oid,
                'Jans::OidProbe',
                ARRAY['name']
            )",
        )
        .expect("register byoid probe mapping");

        let schema_str = schema_path.to_str().expect("utf8 schema path").to_string();
        let report = Spi::get_one_with_args::<pgrx::datum::JsonB>(
            "SELECT cedarling_validate_schema('cedarling_schema_byoid_probe'::regclass::oid, $1)",
            &[schema_str.into()],
        )
        .expect("SPI call should succeed")
        .expect("result must be non-null");
        assert_eq!(
            report.0["ok"], true,
            "OID overload must succeed for matching shape, got: {}",
            report.0
        );

        Spi::run("DROP TABLE IF EXISTS cedarling_schema_byoid_probe").expect("drop probe table");
        let _ = fs::remove_dir_all(&work);
    }

    #[pg_test]
    fn test_validate_schema_lexical_fallback_when_strict_disabled() {
        use std::fs;
        let work = std::env::temp_dir()
            .join(format!("cedarling_pg_schema_lexical_{}", std::process::id()));
        let _ = fs::remove_dir_all(&work);
        fs::create_dir_all(&work).expect("temp dir");
        let schema_path = work.join("schema.cedarschema");
        fs::write(
            &schema_path,
            r#"
            namespace Jans {
                entity Probe = {
                    "name": String,
                    "age": String
                };
            }
            "#,
        )
        .expect("write schema");

        Spi::run("DROP TABLE IF EXISTS cedarling_schema_lexical_probe")
            .expect("drop cedarling_schema_lexical_probe");
        Spi::run("CREATE TABLE cedarling_schema_lexical_probe(name text, age int4)")
            .expect("create cedarling_schema_lexical_probe");

        Spi::run("SET LOCAL cedarling.schema_validate_strict = 'off'")
            .expect("disable strict schema validation");
        let report = crate::schema_sql::cedarling_validate_schema(
            "cedarling_schema_lexical_probe",
            schema_path.to_str().expect("utf8 schema path"),
        );
        let mismatches = report.0["type_mismatches"]
            .as_array()
            .cloned()
            .unwrap_or_default();
        assert!(
            mismatches.is_empty(),
            "lexical fallback should not emit type mismatches"
        );
        Spi::run("SET LOCAL cedarling.schema_validate_strict = 'on'")
            .expect("restore strict schema validation");

        Spi::run("DROP TABLE IF EXISTS cedarling_schema_lexical_probe").expect("drop probe table");
        let _ = fs::remove_dir_all(&work);
    }

    /// RLS + `cedarling_authorize_unsigned` against the unsigned policy store (runs late by name so
    /// earlier `#[pg_test]` cases still see an uninitialized Cedarling engine in the same process).
    #[pg_test]
    fn test_zzz_rls_unsigned_policy_filters_select_under_row_security() {
        crate::functions::pg_test_rls_unsigned::run_rls_unsigned_policy_filters_select_under_row_security();
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
