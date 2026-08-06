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
#[cfg(any(test, feature = "pg_test"))]
mod test_fixtures;
#[cfg(any(test, feature = "pg_test"))]
mod test_support;
mod sync_mutex;

#[cfg(feature = "pg_test")]
mod authorized {
    pub(crate) use crate::functions::authorized::*;
}
#[cfg(feature = "pg_test")]
mod row_authz {
    pub(crate) use crate::functions::authorized_row::*;
}

#[cfg(feature = "pg_test")]
mod policy_sql {
    pub(crate) use crate::policy::versions::*;
}

#[cfg(feature = "pg_test")]
mod schema_sql {
    pub(crate) use crate::policy::schema::*;
}

#[cfg(feature = "pg_test")]
mod mask_sql {
    pub(crate) use crate::mask::{
        cedarling_mask_row, cedarling_set_mask_config, cedarling_test_masking,
    };
}

use pgrx::prelude::*;

::pgrx::pg_module_magic!(name, version);

#[pg_guard]
pub extern "C-unwind" fn _PG_init() {
    guc_config::register_gucs();
}

#[pg_extern(immutable, parallel_safe)]
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
    fn test_policy_segment_changes_with_policy_version() {
        Spi::run("SET cedarling.policy_version = 'v1'").expect("set policy version v1");
        let a = crate::authz::cache::policy_segment_from_bootstrap_path();
        Spi::run("SET cedarling.policy_version = 'v2'").expect("set policy version v2");
        let b = crate::authz::cache::policy_segment_from_bootstrap_path();
        assert_ne!(
            a, b,
            "cache policy segment should include cedarling.policy_version"
        );
        Spi::run("RESET cedarling.policy_version").expect("reset policy version");
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
    fn test_policy_use_and_rollback_update_history_and_version() {
        use std::fs;

        let work =
            std::env::temp_dir().join(format!("cedarling_pg_policy_test_{}", std::process::id()));
        let _ = fs::remove_dir_all(&work);
        fs::create_dir_all(&work).expect("temp work dir");

        let p1 = work.join("policy-1.yaml");
        let p2 = work.join("policy-2.yaml");
        fs::write(&p1, crate::test_support::POLICY_STORE_UNSIGNED_YAML.as_bytes())
            .expect("write policy 1");
        fs::write(&p2, crate::test_support::POLICY_STORE_UNSIGNED_YAML.as_bytes())
            .expect("write policy 2");
        let b1 = crate::test_support::write_bootstrap_yaml(&work, &p1, "cedarling_pg_policy_test");
        let b2 = crate::test_support::write_bootstrap_yaml(&work, &p2, "cedarling_pg_policy_test");

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
    fn test_policy_history_trimmed_to_size_guc() {
        use std::fs;

        let work = std::env::temp_dir()
            .join(format!("cedarling_pg_trim_test_{}", std::process::id()));
        let _ = fs::remove_dir_all(&work);
        fs::create_dir_all(&work).expect("temp work dir");

        let policy_path = work.join("policy.yaml");
        fs::write(
            &policy_path,
            crate::test_support::POLICY_STORE_UNSIGNED_YAML.as_bytes(),
        )
        .expect("write policy");
        let bootstrap_path =
            crate::test_support::write_bootstrap_yaml(&work, &policy_path, "cedarling_pg_trim_test");
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
    fn test_policy_versions_registry_roundtrip() {
        use std::fs;

        let work = std::env::temp_dir()
            .join(format!("cedarling_pg_versions_test_{}", std::process::id()));
        let _ = fs::remove_dir_all(&work);
        fs::create_dir_all(&work).expect("temp work dir");

        let policy_path = work.join("policy.yaml");
        fs::write(
            &policy_path,
            crate::test_support::POLICY_STORE_UNSIGNED_YAML.as_bytes(),
        )
        .expect("write policy");
        let bootstrap_path = crate::test_support::write_bootstrap_yaml(
            &work,
            &policy_path,
            "cedarling_pg_versions_test",
        );

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
    fn test_cedarling_authorized_row_fail_closed_without_engine() {
        use pgrx::datum::JsonB;
        use serde_json::json;
        let resource = JsonB(json!({
            "cedar_entity_mapping": {"entity_type": "T", "id": "x"}
        }));
        assert!(
            !crate::row_authz::cedarling_authorized_row(resource, Some("T::Action::\"A\""), None),
            "cedarling_authorized_row: without engine, fail-closed should deny"
        );
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
        assert_eq!(
            default_id,
            Some(r#"["10","20"]"#.to_string()),
            "default composite PK id should encode id columns in PK order as JSON array"
        );

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
        assert_eq!(
            mapped_id,
            Some(r#"["20","10"]"#.to_string()),
            "registered id_columns [id2,id1] should reverse composite id component order"
        );

        Spi::run("DROP TABLE IF EXISTS cedarling_phase2_pk").expect("drop test table");
    }

    #[pg_test]
    fn test_mask_config_and_mask_row_roundtrip() {
        use pgrx::datum::JsonB;
        use serde_json::json;
        assert!(crate::mask_sql::cedarling_set_mask_config(
            "test_tbl", "email", "redact", None
        ));
        let row = JsonB(json!({"email":"alice@example.org","name":"alice"}));
        let masked = crate::mask_sql::cedarling_mask_row(row, "test_tbl");
        assert_eq!(masked.0["email"], "***REDACTED***");
        assert_eq!(masked.0["name"], "alice");
        let _ = Spi::run("DELETE FROM cedarling.mask_rules WHERE table_name = 'test_tbl'");
    }

    #[pg_test]
    fn test_mask_plan_returns_registered_rules() {
        let table = "test_mask_plan_tbl";
        let _ = Spi::run(&format!(
            "DELETE FROM cedarling.mask_rules WHERE table_name = '{table}'"
        ));

        // Empty plan: action defaults to 'Read'; rules array is empty.
        let empty = Spi::get_one_with_args::<pgrx::datum::JsonB>(
            "SELECT cedarling_mask_plan($1, NULL)",
            &[table.into()],
        )
        .expect("SPI empty plan")
        .expect("empty plan jsonb not NULL");
        assert_eq!(empty.0["table_name"], serde_json::json!(table));
        assert_eq!(empty.0["action"], serde_json::json!("Read"));
        assert_eq!(empty.0["rules"], serde_json::json!([]));

        // After registering a rule, cedarling_mask_plan must echo it back.
        assert!(crate::mask_sql::cedarling_set_mask_config(
            table, "email", "redact", None
        ));
        let plan = Spi::get_one_with_args::<pgrx::datum::JsonB>(
            "SELECT cedarling_mask_plan($1, $2)",
            &[table.into(), "Write".into()],
        )
        .expect("SPI populated plan")
        .expect("plan jsonb not NULL");
        assert_eq!(
            plan.0["action"],
            serde_json::json!("Write"),
            "explicit action must be echoed back, not overridden by the 'Read' default"
        );
        let rules = plan.0["rules"]
            .as_array()
            .expect("rules must be an array");
        assert_eq!(rules.len(), 1, "expected exactly one rule, got {rules:?}");
        assert_eq!(rules[0]["column"], serde_json::json!("email"));
        assert_eq!(rules[0]["mask_type"], serde_json::json!("redact"));

        let _ = Spi::run(&format!(
            "DELETE FROM cedarling.mask_rules WHERE table_name = '{table}'"
        ));
    }

    #[pg_test]
    fn test_mask_hash_salt_guc_and_hash_masking() {
        // Without salt → sentinel returned
        Spi::run("RESET cedarling.mask_hash_salt").expect("reset mask_hash_salt");
        let no_salt = crate::mask_sql::cedarling_test_masking(Some("value"), None, "hash", None);
        assert_eq!(
            no_salt.as_deref(),
            Some("[HASH_SALT_REQUIRED]"),
            "hash without salt must return sentinel"
        );

        // With salt → 64-char hex
        Spi::run("SET cedarling.mask_hash_salt = 'test-salt-value'")
            .expect("set mask_hash_salt");
        let with_salt = crate::mask_sql::cedarling_test_masking(Some("value"), None, "hash", None);
        let hex = with_salt.expect("hash with salt must return Some");
        assert_eq!(hex.len(), 64, "SHA-256 hex must be 64 chars");
        assert!(hex.chars().all(|c| c.is_ascii_hexdigit()), "must be valid lowercase hex");

        // Deterministic
        let same = crate::mask_sql::cedarling_test_masking(Some("value"), None, "hash", None)
            .expect("deterministic hash");
        assert_eq!(hex, same, "hash must be deterministic for same (salt, value)");

        Spi::run("RESET cedarling.mask_hash_salt").expect("reset mask_hash_salt after test");
    }

    #[pg_test]
    fn test_mask_strategy_on_deny_returns_true() {
        // When cedarling.strategy = mask, a deny should surface as true so RLS still
        // includes the row. Actual column masking is the caller's responsibility
        // (pair with cedarling_mask_row in the SELECT list).
        use pgrx::datum::JsonB;
        use serde_json::json;

        Spi::run("SET LOCAL cedarling.strategy = 'mask'").expect("set strategy mask");

        let resource = JsonB(json!({
            "cedar_entity_mapping": {"entity_type": "T", "id": "x"},
            "secret_col": "top-secret"
        }));
        let result = crate::row_authz::cedarling_authorized_row(resource, Some("T::Action::\"A\""), None);
        // With no engine and fail-closed, decision=false. With strategy=mask, function returns true.
        assert!(result, "strategy=mask on deny must return true");

        Spi::run("RESET cedarling.strategy").expect("reset strategy");
    }

    #[pg_test]
    fn test_mask_condition_sql_errors_fail_closed() {
        Spi::run("DELETE FROM cedarling.mask_rules WHERE table_name = 'test_mask_condition'")
            .expect("cleanup before test");
        Spi::run(
            r#"INSERT INTO cedarling.mask_rules(
                   table_name, column_name, mask_type, mask_value, condition_sql, data_type
               ) VALUES (
                   'test_mask_condition',
                   'pii_value',
                   'redact',
                   NULL,
                   $$current_setting('cedarling.role', true) <> 'admin'$$,
                   NULL
               )"#,
        )
        .expect("insert conditional mask rule");

        Spi::run(
            r#"
            DO $cedarling_test$
            DECLARE
              caught boolean := false;
            BEGIN
              BEGIN
                PERFORM cedarling_mask_row(
                  '{"pii_value":"secret-value"}'::jsonb,
                  'test_mask_condition'
                );
              EXCEPTION
                WHEN OTHERS THEN
                  IF SQLERRM LIKE 'cedarling_mask_row:%conditional mask rules%' THEN
                    caught := true;
                  ELSE
                    RAISE;
                  END IF;
              END;
              IF NOT caught THEN
                RAISE EXCEPTION 'expected cedarling_mask_row to reject conditional mask rule';
              END IF;
            END;
            $cedarling_test$;
            "#,
        )
        .expect("cedarling_mask_row should abort on non-empty condition_sql");

        Spi::run("DELETE FROM cedarling.mask_rules WHERE table_name = 'test_mask_condition'")
            .expect("cleanup after test");
    }

    #[pg_test]
    fn test_validate_schema() {
        use std::fs;

        let pid = std::process::id();

        // Case 1: unknown table returns ok=false with an error field.
        {
            let report = crate::schema_sql::cedarling_validate_schema(
                "cedarling_nonexistent_table_xyzzy_abc123",
                "/nonexistent/path/schema.cedarschema",
            );
            let v = &report.0;
            assert_eq!(v["ok"], false, "unknown table must return ok=false, got: {v}");
            assert!(v["error"].as_str().is_some(), "error field must be present for unknown table");
        }

        // Case 2: extra column in table not in schema → missing_in_schema is populated.
        {
            let work = std::env::temp_dir().join(format!("cedarling_pg_schema_a_{pid}"));
            let _ = fs::remove_dir_all(&work);
            fs::create_dir_all(&work).expect("tmp dir a");
            let schema_path = work.join("schema.cedarschema");
            fs::write(&schema_path, "entity User { name: String, age: Long }\n")
                .expect("write schema a");
            Spi::run("DROP TABLE IF EXISTS cedarling_schema_a").ok();
            Spi::run("CREATE TABLE cedarling_schema_a(name text, age int, extra text)")
                .expect("create table a");
            let report = crate::schema_sql::cedarling_validate_schema(
                "cedarling_schema_a",
                schema_path.to_str().expect("utf8"),
            );
            assert!(
                report.0["missing_in_schema"].as_array().is_some(),
                "extra column must appear in missing_in_schema; got: {}",
                report.0
            );
            Spi::run("DROP TABLE IF EXISTS cedarling_schema_a").ok();
            let _ = fs::remove_dir_all(&work);
        }

        // Case 3: type mismatch (age int4 vs String) in strict mode.
        {
            let work = std::env::temp_dir().join(format!("cedarling_pg_schema_b_{pid}"));
            let _ = fs::remove_dir_all(&work);
            fs::create_dir_all(&work).expect("tmp dir b");
            let schema_path = work.join("schema.cedarschema");
            fs::write(
                &schema_path,
                "namespace Jans { entity Probe = { \"name\": String, \"age\": String }; }\n",
            )
            .expect("write schema b");
            Spi::run("DROP TABLE IF EXISTS cedarling_schema_b").ok();
            Spi::run("CREATE TABLE cedarling_schema_b(name text, age int4)")
                .expect("create table b");
            Spi::run(
                "SELECT cedarling_register_entity_map('cedarling_schema_b'::regclass::oid, \
                 'Jans::Probe', ARRAY['name'])",
            )
            .expect("register entity map b");
            let report = crate::schema_sql::cedarling_validate_schema(
                "cedarling_schema_b",
                schema_path.to_str().expect("utf8"),
            );
            let mismatches = report.0["type_mismatches"].as_array().cloned().unwrap_or_default();
            assert!(
                mismatches.iter().any(|m| m.get("column").and_then(|v| v.as_str()) == Some("age")),
                "strict mode must report int4 vs String mismatch; got: {}",
                report.0
            );
            Spi::run("DROP TABLE IF EXISTS cedarling_schema_b").ok();
            let _ = fs::remove_dir_all(&work);
        }

        // Case 4: matching shapes → ok=true, all arrays empty.
        {
            let work = std::env::temp_dir().join(format!("cedarling_pg_schema_c_{pid}"));
            let _ = fs::remove_dir_all(&work);
            fs::create_dir_all(&work).expect("tmp dir c");
            let schema_path = work.join("schema.cedarschema");
            fs::write(
                &schema_path,
                "namespace Jans { entity MatchProbe = { \"name\": String, \"score\": Long }; }\n",
            )
            .expect("write schema c");
            Spi::run("DROP TABLE IF EXISTS cedarling_schema_c").ok();
            Spi::run("CREATE TABLE cedarling_schema_c(name text, score int8)")
                .expect("create table c");
            Spi::run(
                "SELECT cedarling_register_entity_map('cedarling_schema_c'::regclass::oid, \
                 'Jans::MatchProbe', ARRAY['name'])",
            )
            .expect("register entity map c");
            let report = crate::schema_sql::cedarling_validate_schema(
                "cedarling_schema_c",
                schema_path.to_str().expect("utf8"),
            );
            let v = &report.0;
            assert_eq!(v["ok"], true, "matching shapes must return ok=true; got: {v}");
            assert!(v["missing_in_table"].as_array().is_some_and(|a| a.is_empty()));
            assert!(v["missing_in_schema"].as_array().is_some_and(|a| a.is_empty()));
            assert!(v["type_mismatches"].as_array().is_some_and(|a| a.is_empty()));
            Spi::run("DROP TABLE IF EXISTS cedarling_schema_c").ok();
            let _ = fs::remove_dir_all(&work);
        }

        // Case 5: OID overload produces the same result as name overload.
        {
            let work = std::env::temp_dir().join(format!("cedarling_pg_schema_d_{pid}"));
            let _ = fs::remove_dir_all(&work);
            fs::create_dir_all(&work).expect("tmp dir d");
            let schema_path = work.join("schema.cedarschema");
            fs::write(
                &schema_path,
                "namespace Jans { entity OidProbe = { \"name\": String }; }\n",
            )
            .expect("write schema d");
            Spi::run("DROP TABLE IF EXISTS cedarling_schema_d").ok();
            Spi::run("CREATE TABLE cedarling_schema_d(name text)").expect("create table d");
            Spi::run(
                "SELECT cedarling_register_entity_map('cedarling_schema_d'::regclass::oid, \
                 'Jans::OidProbe', ARRAY['name'])",
            )
            .expect("register entity map d");
            let schema_str = schema_path.to_str().expect("utf8").to_string();
            let report = Spi::get_one_with_args::<pgrx::datum::JsonB>(
                "SELECT cedarling_validate_schema('cedarling_schema_d'::regclass::oid, $1)",
                &[schema_str.into()],
            )
            .expect("SPI")
            .expect("non-null");
            assert_eq!(report.0["ok"], true, "OID overload must succeed; got: {}", report.0);
            Spi::run("DROP TABLE IF EXISTS cedarling_schema_d").ok();
            let _ = fs::remove_dir_all(&work);
        }

        // Case 6: strict=off → no type mismatches for known type mismatches.
        {
            let work = std::env::temp_dir().join(format!("cedarling_pg_schema_e_{pid}"));
            let _ = fs::remove_dir_all(&work);
            fs::create_dir_all(&work).expect("tmp dir e");
            let schema_path = work.join("schema.cedarschema");
            fs::write(
                &schema_path,
                "namespace Jans { entity Probe = { \"name\": String, \"age\": String }; }\n",
            )
            .expect("write schema e");
            Spi::run("DROP TABLE IF EXISTS cedarling_schema_e").ok();
            Spi::run("CREATE TABLE cedarling_schema_e(name text, age int4)")
                .expect("create table e");
            Spi::run("SET LOCAL cedarling.schema_validate_strict = 'off'")
                .expect("disable strict");
            let report = crate::schema_sql::cedarling_validate_schema(
                "cedarling_schema_e",
                schema_path.to_str().expect("utf8"),
            );
            let mismatches = report.0["type_mismatches"].as_array().cloned().unwrap_or_default();
            assert!(mismatches.is_empty(), "lexical fallback must not emit type mismatches");
            Spi::run("SET LOCAL cedarling.schema_validate_strict = 'on'").ok();
            Spi::run("DROP TABLE IF EXISTS cedarling_schema_e").ok();
            let _ = fs::remove_dir_all(&work);
        }
    }

    #[pg_test]
    fn test_rls_unsigned_policy_filters_select_under_row_security() {
        crate::functions::pg_test_rls_unsigned::run_rls_unsigned_policy_filters_select_under_row_security();
    }

    #[pg_test]
    fn test_signed_authorized_allow_then_deny() {
        crate::functions::pg_test_authorized_signed::run_signed_authorized_allow_then_deny();
    }

    #[pg_test]
    fn test_authorize_unsigned_batch_three_allow_items() {
        crate::functions::pg_test_authorize_batch::run_unsigned_batch_three_allow_items();
    }

    #[pg_test]
    fn test_authorize_unsigned_batch_mixed_decisions_preserve_order() {
        crate::functions::pg_test_authorize_batch::run_unsigned_batch_mixed_decisions_preserve_order();
    }

    #[pg_test]
    fn test_authorize_unsigned_batch_empty_items_synthesizes_fail_closed_row() {
        crate::functions::pg_test_authorize_batch::run_unsigned_batch_empty_items_synthesizes_fail_closed_row();
    }

    #[pg_test]
    fn test_authorize_multi_issuer_batch_empty_tokens_synthesizes_fail_closed_rows() {
        crate::functions::pg_test_authorize_batch::run_multi_issuer_batch_empty_tokens_synthesizes_fail_closed_rows();
    }

    #[pg_test]
    fn test_authorize_unsigned_batch_malformed_json_synthesizes_sentinel_row() {
        crate::functions::pg_test_authorize_batch::run_unsigned_batch_malformed_json_synthesizes_sentinel_row();
    }

    #[pg_test]
    fn test_authorize_unsigned_batch_observability_records_requests_and_traces() {
        crate::functions::pg_test_authorize_batch::run_unsigned_batch_observability_records_requests_and_traces();
    }

    #[pg_test]
    fn test_authorize_unsigned_batch_failure_bumps_requests_and_errors() {
        crate::functions::pg_test_authorize_batch::run_unsigned_batch_failure_bumps_requests_and_errors();
    }

    #[pg_test]
    fn test_authorize_unsigned_batch_clean_policy_deny() {
        crate::functions::pg_test_authorize_batch::run_unsigned_batch_clean_policy_deny();
    }

    #[pg_test]
    fn test_authorize_unsigned_batch_shadow_mode_returns_true_records_raw() {
        crate::functions::pg_test_authorize_batch::run_unsigned_batch_shadow_mode_returns_true_records_raw();
    }

    #[pg_test]
    fn test_authorize_unsigned_batch_fail_open_synthesizes_true_rows() {
        crate::functions::pg_test_authorize_batch::run_unsigned_batch_fail_open_synthesizes_true_rows();
    }

    /// Asserts `cedarling.context` and `cedarling.tokens` check hooks
    /// (`guc_config.rs:261-287`) reject invalid input at SET time. The validator
    /// logic is unit-tested in `validate.rs`; this is the only test that proves
    /// the hooks are actually wired through `define_string_guc_with_hooks` and
    /// that the SET surfaces as a SQL error to the caller (not a silent accept).
    ///
    /// Negative cases are wrapped in a PL/pgSQL `DO` block so the
    /// `invalid_parameter_value` raised by the check hook is caught at the
    /// subtransaction boundary — a bare `Spi::run("SET ...")` would propagate
    /// the ERROR and abort the test's outer transaction before we could inspect
    /// the result.
    #[pg_test]
    fn test_guc_check_hooks_reject_invalid_input_at_set_time() {
        // Negative — context must be a JSON object; an array is rejected.
        // If the SET unexpectedly succeeds, the inner RAISE (custom SQLSTATE
        // outside the WHEN clause) propagates and surfaces as a test failure.
        Spi::run(
            r"DO $$
            BEGIN
                SET cedarling.context = '[]';
                RAISE EXCEPTION 'expected SET cedarling.context = ''[]'' to fail at SET time but it succeeded';
            EXCEPTION
                WHEN invalid_parameter_value THEN
                    NULL;
            END $$;",
        )
        .expect("invalid context SET should be rejected by the check hook with SQLSTATE 22023");

        // Negative — malformed JSON is rejected.
        Spi::run(
            r"DO $$
            BEGIN
                SET cedarling.tokens = '{';
                RAISE EXCEPTION 'expected SET cedarling.tokens = ''{{'' to fail at SET time but it succeeded';
            EXCEPTION
                WHEN invalid_parameter_value THEN
                    NULL;
            END $$;",
        )
        .expect("invalid tokens SET should be rejected by the check hook with SQLSTATE 22023");

        // Positive — valid object is accepted, valid JSON tokens are accepted.
        Spi::run("SET cedarling.context = '{}'")
            .expect("SET cedarling.context = '{}' should succeed");
        Spi::run("SET cedarling.tokens = '{}'")
            .expect("SET cedarling.tokens = '{}' should succeed");
        Spi::run("RESET cedarling.context").ok();
        Spi::run("RESET cedarling.tokens").ok();
    }

    #[pg_test]
    fn test_diff_policies_structural_lines_and_io_error() {
        use std::fs;

        let work = std::env::temp_dir()
            .join(format!("cedarling_pg_diff_policies_test_{}", std::process::id()));
        let _ = fs::remove_dir_all(&work);
        fs::create_dir_all(&work).expect("temp work dir");

        // Old policy set: one permit, conditional on x == 1.
        let old = work.join("old.cedar");
        fs::write(
            &old,
            r#"@id("p1") permit(principal, action, resource) when { resource.x == 1 };"#,
        )
        .expect("write old");

        // New policy set: same id but different body (modified), plus a brand new forbid (added).
        let new = work.join("new.cedar");
        fs::write(
            &new,
            r#"@id("p1") permit(principal, action, resource) when { resource.x == 2 };
               @id("p2") forbid(principal, action, resource);"#,
        )
        .expect("write new");

        let old_s = old.to_str().expect("old path utf8");
        let new_s = new.to_str().expect("new path utf8");

        // Structural mode (default) — diff by policy id.
        Spi::run("RESET cedarling.diff_mode").expect("RESET diff_mode");
        let structural = Spi::get_one_with_args::<pgrx::datum::JsonB>(
            "SELECT cedarling_diff_policies($1, $2)",
            &[old_s.into(), new_s.into()],
        )
        .expect("SPI structural diff")
        .expect("structural diff jsonb not NULL");
        assert_eq!(structural.0["ok"], serde_json::json!(true));
        assert!(
            structural.0["modified"].as_array().is_some_and(|a| !a.is_empty()),
            "structural diff should surface modified p1: {:?}",
            structural.0
        );
        assert!(
            structural.0["added"].as_array().is_some_and(|a| !a.is_empty()),
            "structural diff should surface added p2: {:?}",
            structural.0
        );

        // Lines mode — same fixtures, dispatch through the legacy line-oriented path.
        Spi::run("SET cedarling.diff_mode = 'lines'").expect("SET diff_mode = lines");
        let lines = Spi::get_one_with_args::<pgrx::datum::JsonB>(
            "SELECT cedarling_diff_policies($1, $2)",
            &[old_s.into(), new_s.into()],
        )
        .expect("SPI lines diff")
        .expect("lines diff jsonb not NULL");
        assert_eq!(lines.0["ok"], serde_json::json!(true));
        assert!(
            lines.0["added"].as_array().is_some_and(|a| !a.is_empty())
                || lines.0["removed"].as_array().is_some_and(|a| !a.is_empty()),
            "lines diff should surface at least one added/removed line: {:?}",
            lines.0
        );
        Spi::run("RESET cedarling.diff_mode").expect("RESET diff_mode");

        // File I/O error branch — non-existent path should fall into the error arm at
        // `versions.rs:223-233` (ok=false, error populated, arrays empty).
        let err = Spi::get_one_with_args::<pgrx::datum::JsonB>(
            "SELECT cedarling_diff_policies($1, $2)",
            &[
                "/cedarling_pg_no_such_dir/old.cedar".into(),
                new_s.into(),
            ],
        )
        .expect("SPI error case")
        .expect("error jsonb not NULL");
        assert_eq!(err.0["ok"], serde_json::json!(false));
        assert!(
            err.0["error"].as_str().is_some_and(|s| !s.is_empty()),
            "error branch must populate an `error` string: {:?}",
            err.0
        );

        let _ = fs::remove_dir_all(&work);
    }

    #[pg_test]
    fn test_jwt_row_uses_cedarling_tokens_guc() {
        crate::functions::pg_test_authorized_signed::run_jwt_row_uses_cedarling_tokens_guc();
    }

    #[pg_test]
    fn test_unsigned_unconditional_permit_returns_true() {
        crate::authz::pg_test_where::run_unsigned_unconditional_permit_returns_true();
    }

    #[pg_test]
    fn test_unsigned_no_matching_action_returns_false() {
        crate::authz::pg_test_where::run_unsigned_no_matching_action_returns_false();
    }

    #[pg_test]
    fn test_unsigned_resource_predicate_lowers_to_sql() {
        crate::authz::pg_test_where::run_unsigned_resource_predicate_lowers_to_sql();
    }

    #[pg_test]
    fn test_unsigned_unhandled_predicate_returns_partial_deny() {
        crate::authz::pg_test_where::run_unsigned_unhandled_predicate_returns_partial_deny();
    }

    #[pg_test]
    fn test_unsigned_unhandled_predicate_returns_partial_permit() {
        crate::authz::pg_test_where::run_unsigned_unhandled_predicate_returns_partial_permit();
    }

    #[pg_test]
    fn test_unsigned_predicate_matches_rls_count_parity() {
        crate::authz::pg_test_where::run_unsigned_predicate_matches_rls_count_parity();
    }

    #[pg_test]
    fn test_explain_includes_policy_hits_and_policies() {
        crate::authz::pg_test_where::run_explain_includes_policy_hits_and_policies();
    }
}

/// Required by `cargo pgrx test`.
#[cfg(test)]
pub mod pg_test {
    pub fn setup(_options: Vec<&str>) {
        #[cfg(feature = "pg_test")]
        {
            let _ = crate::engine::reset_for_pg_tests();
            crate::authz::cache::global_cache().clear_all();
        }
    }

    #[must_use]
    pub fn postgresql_conf_options() -> Vec<&'static str> {
        vec![]
    }
}
