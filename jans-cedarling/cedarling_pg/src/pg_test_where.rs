// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! `#[pg_test]` bodies for `cedarling_where` predicate pushdown.
//!
//! These tests exercise the SPI-touching path of `authz::where_clause::cedarling_where`
//! end-to-end: write a policy store + bootstrap to a temp dir, swap the
//! Cedarling engine via `cedarling_use_policy`, create a probe table with an
//! `entity_map` registration, then assert the lowered SQL fragment that
//! `cedarling_where` returns for representative policy shapes.
//!
//! Kept out of `lib.rs` to respect line-count limits (mirrors the
//! `pg_test_rls_unsigned.rs` precedent for `cedarling_authorize_unsigned`).

use std::fs;
use std::io::Write;
use std::path::{Path, PathBuf};

use pgrx::prelude::*;

/// Inline policy store covering the four shapes the e2e matrix asserts:
///   - `WhereOpen`       — unconditional permit, lowers to `AlwaysTrue` (`"TRUE"`).
///   - `WhereEq`         — `resource.country == "US"`, lowers to a SQL `=` predicate.
///   - `WhereUnhandled`  — `resource.tags.contains("vip")`, can't be lowered;
///                          must surface as `Partial { fragment: "TRUE", ... }`.
///   - `<no match>`      — driven by an action absent from the store.
///
/// All policies leave `principal` unconstrained (`PrincipalConstraint::Any`)
/// so unsigned matching (no tokens, `principal_types = {}`) keeps them in
/// the candidate set; otherwise unsigned filtering would drop them and the
/// `WhereEq`/`WhereUnhandled` cases would silently degrade to `AlwaysFalse`.
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

/// Writes the `where_*` policy store + bootstrap to a fresh temp dir and
/// swaps the process-wide Cedarling engine to it. Returns the temp dir so
/// callers can clean up after asserting.
fn setup_engine_with_where_policies(label: &str) -> PathBuf {
    let work = temp_workdir(label);
    let policy_path = work.join("policy-store.yaml");
    fs::write(&policy_path, POLICY_STORE_YAML).expect("write policy store");
    let bootstrap_path = write_bootstrap(&work, &policy_path);
    let bootstrap_str = bootstrap_path.to_str().expect("utf8 bootstrap path");
    // Reset any leftover token GUC from earlier `#[pg_test]` cases so the
    // unsigned fallback path is exercised (the gap-1 fix the matrix covers).
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

/// `WhereOpen` is an unconditional permit; lowering must collapse to
/// `SqlPredicate::AlwaysTrue` and `cedarling_where` must return `"TRUE"`.
/// Also exercises the gap-1 unsigned fallback: tokens=None must NOT be a
/// hard fail-closed.
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

/// An action absent from the policy store yields zero matching policies →
/// `SqlPredicate::AlwaysFalse` → `"FALSE"`. Confirms fail-closed default
/// when no permit applies (rather than e.g. a permissive `"TRUE"`).
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

/// `resource.country == "US"` is a structural shape `lower_atom` recognises;
/// the predicate must reference the quoted column and quoted literal so it
/// can be embedded in a SQL WHERE clause without further escaping.
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

/// `resource.tags.contains("vip")` is a method-call body outside the
/// supported lowering shapes; it must surface as a `Partial` residual,
/// causing `cedarling_where` to return the permissive `"TRUE"` fragment so
/// the optimizer keeps every row and row-by-row authorization remains
/// authoritative. (Plus a `WARN` diagnostic listing the unhandled policy id
/// is emitted; we don't capture log output here, just the contract.)
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

/// End-to-end parity assertion called out in the gap-closure plan:
///
///   `SELECT count(*) FROM t WHERE <cedarling_where(...)>` (pushdown, superuser, RLS bypassed)
///   ==
///   `SELECT count(*) FROM t` (under RLS via per-row `cedarling_authorize_unsigned`)
///
/// The plan's literal `(cedarling_where(...))::bool = true` expression only
/// works for the `'TRUE'` / `'FALSE'` outputs (the always-true / always-false
/// branches); for the structural Where case (e.g. `"country" = 'US'`) the
/// predicate has to be **interpolated** into the WHERE clause, since a
/// `text` like `("country" = 'US')` isn't a valid `bool` cast input. We
/// fetch the predicate string in Rust, format `SELECT count(*) FROM t
/// WHERE <pred>`, and compare to the RLS path that runs the same Cedar
/// policy per row through `cedarling_authorize_unsigned`. Mirrors the role-
/// switching pattern from `pg_test_rls_unsigned`.
///
/// Dataset: 3 rows, 2 with `country='US'`, 1 with `country='CA'`. Policy
/// `WhereEq` permits when `resource.country == "US"`. Both paths must
/// therefore observe a count of 2.
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

    // Per-row resource_json mirrors the row's `country` column, so per-row
    // Cedar evaluation under RLS will see the same data the column-level
    // pushdown filters on. `tags: []` satisfies the schema's `Set<String>`
    // attribute even though `WhereEq` doesn't reference it.
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

    // 1) Pushdown predicate path (superuser; RLS is irrelevant here yet).
    //    Interpolate the lowered fragment directly into the WHERE clause so
    //    structural predicates (e.g. `"country" = 'US'`) and the literal
    //    branches (`TRUE`/`FALSE`) work uniformly.
    let pred = crate::where_sql::cedarling_where(table, "Jans::Action::\"WhereEq\"", None);
    assert_ne!(
        pred, "FALSE",
        "policy `WhereEq` must produce a permissive predicate (got FALSE — engine bootstrap failed?)"
    );
    let pushdown_count = Spi::get_one::<i64>(&format!(
        "SELECT count(*)::bigint FROM {table} WHERE {pred}"
    ))
    .expect("SPI pushdown count");

    // 2) RLS path. Use `cedarling_authorize_unsigned(NULL, resource_json,
    //    action, '{}')` so the per-row Cedar evaluation matches the
    //    unsigned matching path that drove the predicate lowering. With
    //    `principal_json = NULL`, only `PrincipalConstraint::Any` policies
    //    apply — the same filter `get_matching_policies_unsigned` ran.
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

    // Cleanup *before* asserting so a failed assertion doesn't leave the
    // session stuck in `SET ROLE` / RLS state for subsequent `#[pg_test]`s.
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
