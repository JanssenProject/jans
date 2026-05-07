// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! `#[pg_test]` bodies for `cedarling_where` predicate pushdown.
//!
//! End-to-end: write a policy store + bootstrap → swap the Cedarling engine → create a probe
//! table with an entity_map registration → assert the lowered SQL fragment.

use std::fs;
use std::io::Write;
use std::path::{Path, PathBuf};

use pgrx::prelude::*;

/// Policy store for the predicate-pushdown matrix: `WhereOpen` (unconditional permit),
/// `WhereEq` (resource equality), `WhereUnhandled` (method-call body), plus no-match.
/// All policies use `PrincipalConstraint::Any` so unsigned matching keeps them in scope.
const POLICY_STORE_YAML: &str = r#"cedar_version: v4.0.0
policy_stores:
  cedarling_pg_where_test:
    cedar_version: v4.0.0
    name: "JansWhereTest"
    policies:
      where_open:
        description: "unconditional permit; AlwaysTrue lowering target"
        creation_date: "2025-01-01T00:00:00"
        policy_content:
          encoding: none
          content_type: cedar
          body: |-
            permit(principal, action in [Jans::Action::"WhereOpen"], resource);
      where_eq:
        description: "resource equality predicate; SQL Where lowering target"
        creation_date: "2025-01-01T00:00:00"
        policy_content:
          encoding: none
          content_type: cedar
          body: |-
            permit(principal, action in [Jans::Action::"WhereEq"], resource)
            when { resource.country == "US" };
      where_unhandled:
        description: "method-call body; unhandled-residual lowering target"
        creation_date: "2025-01-01T00:00:00"
        policy_content:
          encoding: none
          content_type: cedar
          body: |-
            permit(principal, action in [Jans::Action::"WhereUnhandled"], resource)
            when { resource.tags.contains("vip") };
    schema:
      encoding: none
      content_type: cedar
      body: |-
        namespace Jans {
        entity Issue = {
          "country": String,
          "tags": Set<String>,
        };
        entity Any;
        action "WhereOpen" appliesTo {
          principal: [Any],
          resource: [Issue],
          context: {}
        };
        action "WhereEq" appliesTo {
          principal: [Any],
          resource: [Issue],
          context: {}
        };
        action "WhereUnhandled" appliesTo {
          principal: [Any],
          resource: [Issue],
          context: {}
        };
        }
"#;

fn write_bootstrap(dir: &Path, policy_path: &Path) -> PathBuf {
    let bootstrap_path = dir.join("bootstrap.yaml");
    let policy_lit = policy_path.to_string_lossy();
    let contents = format!(
        "CEDARLING_APPLICATION_NAME: cedarling_pg_where_test\n\
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

fn temp_workdir(label: &str) -> PathBuf {
    let work = std::env::temp_dir().join(format!(
        "cedarling_pg_where_test_{label}_{}",
        std::process::id()
    ));
    let _ = fs::remove_dir_all(&work);
    fs::create_dir_all(&work).expect("temp work dir");
    work
}

/// Writes the policy store + bootstrap to a fresh temp dir and swaps the Cedarling engine.
fn setup_engine_with_where_policies(label: &str) -> PathBuf {
    let work = temp_workdir(label);
    let policy_path = work.join("policy-store.yaml");
    fs::write(&policy_path, POLICY_STORE_YAML).expect("write policy store");
    let bootstrap_path = write_bootstrap(&work, &policy_path);
    let bootstrap_str = bootstrap_path.to_str().expect("utf8 bootstrap path");
    Spi::run("RESET cedarling.tokens").expect("RESET cedarling.tokens");
    assert!(
        crate::policy_sql::cedarling_use_policy(bootstrap_str),
        "cedarling_use_policy must succeed for the where-test policy store",
    );
    work
}

fn create_probe_table_and_register_mapping(table_name: &str) {
    let _ = Spi::run(&format!("DROP TABLE IF EXISTS {table_name}"));
    Spi::run(&format!(
        "CREATE TABLE {table_name}(\
            id int PRIMARY KEY, \
            country text NOT NULL, \
            tags text[] NOT NULL DEFAULT '{{}}'\
        )"
    ))
    .expect("CREATE probe table");
    Spi::run(&format!(
        "SELECT cedarling_register_entity_map('{table_name}'::regclass::oid, 'Jans::Issue', ARRAY['id'])"
    ))
    .expect("register entity map for probe table");
}

fn drop_probe_table(table_name: &str) {
    let _ = Spi::run(&format!("DROP TABLE IF EXISTS {table_name}"));
}

/// Unconditional permit (`WhereOpen`) must lower to `"TRUE"` via `AlwaysTrue`.
pub fn run_unsigned_unconditional_permit_returns_true() {
    let work = setup_engine_with_where_policies("unconditional");
    let table = "cedarling_pg_where_unconditional";
    create_probe_table_and_register_mapping(table);

    let pred = crate::where_sql::cedarling_where(table, "Jans::Action::\"WhereOpen\"", None);
    assert_eq!(
        pred, "TRUE",
        "unsigned + unconditional permit must lower to AlwaysTrue (\"TRUE\")"
    );

    drop_probe_table(table);
    let _ = fs::remove_dir_all(&work);
}

/// No matching action → zero policies → `AlwaysFalse` → `"FALSE"`.
pub fn run_unsigned_no_matching_action_returns_false() {
    let work = setup_engine_with_where_policies("nomatch");
    let table = "cedarling_pg_where_nomatch";
    create_probe_table_and_register_mapping(table);

    let pred = crate::where_sql::cedarling_where(table, "Jans::Action::\"DoesNotExist\"", None);
    assert_eq!(
        pred, "FALSE",
        "no matching policy must lower to AlwaysFalse (\"FALSE\")"
    );

    drop_probe_table(table);
    let _ = fs::remove_dir_all(&work);
}

/// `resource.country == "US"` lowers to a SQL equality predicate with quoted column + literal.
pub fn run_unsigned_resource_predicate_lowers_to_sql() {
    let work = setup_engine_with_where_policies("eq");
    let table = "cedarling_pg_where_eq";
    create_probe_table_and_register_mapping(table);

    let pred = crate::where_sql::cedarling_where(table, "Jans::Action::\"WhereEq\"", None);
    assert!(
        pred.contains("\"country\""),
        "predicate must reference the quoted column name; got: {pred}"
    );
    assert!(
        pred.contains("'US'"),
        "predicate must contain the quoted literal 'US'; got: {pred}"
    );
    assert!(
        pred.contains('='),
        "predicate must contain a SQL equality comparator; got: {pred}"
    );
    assert_ne!(pred, "TRUE", "structural lowering must not collapse to AlwaysTrue");
    assert_ne!(pred, "FALSE", "matching policy must not collapse to AlwaysFalse");

    drop_probe_table(table);
    let _ = fs::remove_dir_all(&work);
}

/// Method-call body (`resource.tags.contains(...)`) can't be lowered → `Partial` → `"TRUE"`.
/// Row-by-row authorization remains authoritative; a WARN diagnostic names the unhandled policy.
pub fn run_unsigned_unhandled_predicate_returns_partial_true() {
    let work = setup_engine_with_where_policies("unhandled");
    let table = "cedarling_pg_where_unhandled";
    create_probe_table_and_register_mapping(table);

    let pred = crate::where_sql::cedarling_where(table, "Jans::Action::\"WhereUnhandled\"", None);
    assert_eq!(
        pred, "TRUE",
        "unhandled (method-call) policy must surface a permissive Partial \"TRUE\" fragment"
    );

    drop_probe_table(table);
    let _ = fs::remove_dir_all(&work);
}

/// Parity: `SELECT count(*) FROM t WHERE <cedarling_where(...)>` must equal
/// `SELECT count(*) FROM t` under RLS with per-row `cedarling_authorize_unsigned`.
/// Dataset: 3 rows (2 US, 1 CA); `WhereEq` permits `country == "US"` → both counts = 2.
pub fn run_unsigned_predicate_matches_rls_count_parity() {
    let work = setup_engine_with_where_policies("parity");
    let table = "cedarling_pg_where_parity";
    let role = "cedarling_pg_where_parity_role";

    let _ = Spi::run(&format!("DROP TABLE IF EXISTS {table} CASCADE"));
    let _ = Spi::run(&format!("DROP ROLE IF EXISTS {role}"));

    Spi::run(&format!(
        "CREATE TABLE {table}(\
            id int PRIMARY KEY, \
            country text NOT NULL, \
            tags text[] NOT NULL DEFAULT '{{}}', \
            resource_json text NOT NULL\
        )"
    ))
    .expect("CREATE parity table");

    Spi::run(&format!(
        "INSERT INTO {table} (id, country, resource_json) VALUES \
           (1, 'US', '{{\"cedar_entity_mapping\":{{\"entity_type\":\"Jans::Issue\",\"id\":\"r1\"}},\"country\":\"US\",\"tags\":[]}}'), \
           (2, 'CA', '{{\"cedar_entity_mapping\":{{\"entity_type\":\"Jans::Issue\",\"id\":\"r2\"}},\"country\":\"CA\",\"tags\":[]}}'), \
           (3, 'US', '{{\"cedar_entity_mapping\":{{\"entity_type\":\"Jans::Issue\",\"id\":\"r3\"}},\"country\":\"US\",\"tags\":[]}}')"
    ))
    .expect("INSERT parity rows");

    Spi::run(&format!(
        "SELECT cedarling_register_entity_map('{table}'::regclass::oid, 'Jans::Issue', ARRAY['id'])"
    ))
    .expect("register entity map (parity)");

    let pred = crate::where_sql::cedarling_where(table, "Jans::Action::\"WhereEq\"", None);
    assert_ne!(
        pred, "FALSE",
        "policy `WhereEq` must produce a permissive predicate (got FALSE — engine bootstrap failed?)"
    );
    let pushdown_count = Spi::get_one::<i64>(&format!(
        "SELECT count(*)::bigint FROM {table} WHERE {pred}"
    ))
    .expect("SPI pushdown count");

    Spi::run(&format!("ALTER TABLE {table} ENABLE ROW LEVEL SECURITY"))
        .expect("ENABLE RLS (parity)");
    Spi::run(&format!("ALTER TABLE {table} FORCE ROW LEVEL SECURITY"))
        .expect("FORCE RLS (parity)");
    Spi::run(&format!("DROP POLICY IF EXISTS {table}_pol ON {table}")).ok();
    Spi::run(&format!(
        "CREATE POLICY {table}_pol ON {table} FOR SELECT USING (\
            cedarling_authorize_unsigned(\
                NULL, \
                resource_json, \
                'Jans::Action::\"WhereEq\"', \
                '{{}}'\
            )\
        )"
    ))
    .expect("CREATE RLS POLICY (parity)");

    Spi::run(&format!(
        "CREATE ROLE {role} NOSUPERUSER NOCREATEDB NOINHERIT NOLOGIN"
    ))
    .expect("CREATE ROLE (parity)");
    Spi::run(&format!("GRANT SELECT ON {table} TO {role}")).expect("GRANT SELECT (parity)");
    Spi::run(&format!(
        "DO $$ BEGIN EXECUTE format('GRANT {role} TO %I', session_user); END $$"
    ))
    .expect("GRANT membership (parity)");
    Spi::run(&format!("SET ROLE {role}")).expect("SET ROLE (parity)");
    Spi::run("SET row_security = on").expect("SET row_security (parity)");

    let rls_count =
        Spi::get_one::<i64>(&format!("SELECT count(*)::bigint FROM {table}")).expect("SPI rls count");

    Spi::run("RESET ROLE").expect("RESET ROLE (parity)");
    let _ = Spi::run("SET row_security = off");
    Spi::run(&format!(
        "DO $$ BEGIN EXECUTE format('REVOKE {role} FROM %I', session_user); END $$"
    ))
    .expect("REVOKE membership (parity)");
    Spi::run(&format!("DROP TABLE IF EXISTS {table} CASCADE")).expect("DROP TABLE (parity)");
    Spi::run(&format!("DROP ROLE IF EXISTS {role}")).expect("DROP ROLE (parity)");

    assert_eq!(
        pushdown_count,
        Some(2),
        "pushdown count must include only US rows (2 of 3); got {pushdown_count:?} for predicate `{pred}`"
    );
    assert_eq!(
        rls_count,
        Some(2),
        "RLS count must include only US rows (2 of 3); got {rls_count:?}"
    );
    assert_eq!(
        pushdown_count, rls_count,
        "RLS parity: pushdown count must equal RLS count (predicate `{pred}`)"
    );

    let _ = fs::remove_dir_all(&work);
}
