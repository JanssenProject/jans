// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! `#[pg_test]` bodies for `cedarling_where` predicate pushdown.
//!
//! End-to-end: write a policy store + bootstrap → swap the Cedarling engine → create a probe
//! table with an entity_map registration → assert the lowered SQL fragment.

use std::fs;
use std::path::PathBuf;

use pgrx::prelude::*;

fn quoted_ident(name: &str) -> String {
    crate::authz::where_clause::quote_ident_safe(name)
}

struct WhereParityGuard {
    qtable: String,
    qrole: String,
}

impl WhereParityGuard {
    fn new(table: &str, role: &str) -> Self {
        Self {
            qtable: quoted_ident(table),
            qrole: quoted_ident(role),
        }
    }
}

impl Drop for WhereParityGuard {
    fn drop(&mut self) {
        Spi::run("RESET ROLE").ok();
        Spi::run("SET row_security = off").ok();
        Spi::run(&format!(
            "DO $$ BEGIN EXECUTE format('REVOKE {} FROM %I', session_user); END $$",
            self.qrole
        ))
        .ok();
        Spi::run(&format!("DROP TABLE IF EXISTS {} CASCADE", self.qtable)).ok();
        Spi::run(&format!("DROP ROLE IF EXISTS {}", self.qrole)).ok();
        Spi::run("RESET cedarling.bootstrap_config").ok();
        crate::authz::cache::global_cache().clear_all();
        crate::engine::reset_for_pg_tests();
    }
}

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
    let bootstrap_path =
        crate::test_support::write_bootstrap_yaml(&work, &policy_path, "cedarling_pg_where_test");
    let bootstrap_str = bootstrap_path.to_str().expect("utf8 bootstrap path");
    Spi::run("RESET cedarling.tokens").expect("RESET cedarling.tokens");
    assert!(
        crate::policy_sql::cedarling_use_policy(bootstrap_str),
        "cedarling_use_policy must succeed for the where-test policy store",
    );
    work
}

fn create_probe_table_and_register_mapping(table_name: &str) {
    let qname = quoted_ident(table_name);
    let _ = Spi::run(&format!("DROP TABLE IF EXISTS {qname}"));
    Spi::run(&format!(
        "CREATE TABLE {qname}(\
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
    let qname = quoted_ident(table_name);
    let _ = Spi::run(&format!("DROP TABLE IF EXISTS {qname}"));
}

/// Unconditional permit (`WhereOpen`) must lower to `"TRUE"` via `AlwaysTrue`.
pub fn run_unsigned_unconditional_permit_returns_true() {
    let work = setup_engine_with_where_policies("unconditional");
    let table = "cedarling_pg_where_unconditional";
    create_probe_table_and_register_mapping(table);

    let pred =
        crate::authz::where_clause::cedarling_where(table, "Jans::Action::\"WhereOpen\"", None);
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

    let pred =
        crate::authz::where_clause::cedarling_where(table, "Jans::Action::\"DoesNotExist\"", None);
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

    let pred =
        crate::authz::where_clause::cedarling_where(table, "Jans::Action::\"WhereEq\"", None);
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
    assert_ne!(
        pred, "TRUE",
        "structural lowering must not collapse to AlwaysTrue"
    );
    assert_ne!(
        pred, "FALSE",
        "matching policy must not collapse to AlwaysFalse"
    );

    drop_probe_table(table);
    let _ = fs::remove_dir_all(&work);
}

/// Method-call body (`resource.tags.contains(...)`) can't be lowered → `Partial` → `"TRUE"`.
/// Row-by-row authorization remains authoritative; a WARN diagnostic names the unhandled policy.
pub fn run_unsigned_unhandled_predicate_returns_partial_true() {
    let work = setup_engine_with_where_policies("unhandled");
    let table = "cedarling_pg_where_unhandled";
    create_probe_table_and_register_mapping(table);

    let pred = crate::authz::where_clause::cedarling_where(
        table,
        "Jans::Action::\"WhereUnhandled\"",
        None,
    );
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
    let qtable = quoted_ident(table);
    let qrole = quoted_ident(role);
    let _guard = WhereParityGuard::new(table, role);

    let _ = Spi::run(&format!("DROP TABLE IF EXISTS {qtable} CASCADE"));
    let _ = Spi::run(&format!("DROP ROLE IF EXISTS {qrole}"));

    Spi::run(&format!(
        "CREATE TABLE {qtable}(\
            id int PRIMARY KEY, \
            country text NOT NULL, \
            tags text[] NOT NULL DEFAULT '{{}}', \
            resource_json text NOT NULL\
        )"
    ))
    .expect("CREATE parity table");

    Spi::run(&format!(
        "INSERT INTO {qtable} (id, country, resource_json) VALUES \
           (1, 'US', '{{\"cedar_entity_mapping\":{{\"entity_type\":\"Jans::Issue\",\"id\":\"r1\"}},\"country\":\"US\",\"tags\":[]}}'), \
           (2, 'CA', '{{\"cedar_entity_mapping\":{{\"entity_type\":\"Jans::Issue\",\"id\":\"r2\"}},\"country\":\"CA\",\"tags\":[]}}'), \
           (3, 'US', '{{\"cedar_entity_mapping\":{{\"entity_type\":\"Jans::Issue\",\"id\":\"r3\"}},\"country\":\"US\",\"tags\":[]}}')"
    ))
    .expect("INSERT parity rows");

    Spi::run(&format!(
        "SELECT cedarling_register_entity_map('{table}'::regclass::oid, 'Jans::Issue', ARRAY['id'])"
    ))
    .expect("register entity map (parity)");

    let pred =
        crate::authz::where_clause::cedarling_where(table, "Jans::Action::\"WhereEq\"", None);
    assert_ne!(
        pred, "FALSE",
        "policy `WhereEq` must produce a permissive predicate (got FALSE — engine bootstrap failed?)"
    );
    let pushdown_count = Spi::get_one::<i64>(&format!(
        "SELECT count(*)::bigint FROM {qtable} WHERE {pred}"
    ))
    .expect("SPI pushdown count");

    Spi::run(&format!("ALTER TABLE {qtable} ENABLE ROW LEVEL SECURITY"))
        .expect("ENABLE RLS (parity)");
    Spi::run(&format!("ALTER TABLE {qtable} FORCE ROW LEVEL SECURITY"))
        .expect("FORCE RLS (parity)");
    Spi::run(&format!(
        "DROP POLICY IF EXISTS cedarling_pg_where_parity_pol ON {qtable}"
    ))
    .ok();
    Spi::run(&format!(
        "CREATE POLICY cedarling_pg_where_parity_pol ON {qtable} FOR SELECT USING (\
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
        "CREATE ROLE {qrole} NOSUPERUSER NOCREATEDB NOINHERIT NOLOGIN"
    ))
    .expect("CREATE ROLE (parity)");
    Spi::run(&format!("GRANT SELECT ON {qtable} TO {qrole}")).expect("GRANT SELECT (parity)");
    Spi::run(&format!(
        "DO $$ BEGIN EXECUTE format('GRANT {qrole} TO %I', session_user); END $$"
    ))
    .expect("GRANT membership (parity)");
    Spi::run(&format!("SET ROLE {qrole}")).expect("SET ROLE (parity)");
    Spi::run("SET row_security = on").expect("SET row_security (parity)");

    let rls_count = Spi::get_one::<i64>(&format!("SELECT count(*)::bigint FROM {qtable}"))
        .expect("SPI rls count");

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

/// `cedarling_explain` should include request/diagnostic/policy metadata on a successful call.
///
/// Uses `WhereOpen` which is an unconditional permit for `Jans::Issue` resources.
pub fn run_explain_includes_policy_hits_and_policies() {
    let work = setup_engine_with_where_policies("explain");
    let result = crate::observability::trace::cedarling_explain(
        r#"{
            "cedar_entity_mapping": { "entity_type": "Jans::Issue", "id": "issue-1" },
            "country": "US",
            "tags": ["vip"]
        }"#,
        r#"Jans::Action::"WhereOpen""#,
    );

    let v = &result.0;
    assert!(
        v.get("error").is_none(),
        "explain should succeed for WhereOpen; got: {v}"
    );
    assert!(
        v.get("request_id").and_then(|x| x.as_str()).is_some(),
        "successful explain must include request_id; got: {v}"
    );
    assert!(
        v.get("policy_hits")
            .and_then(|x| x.as_array())
            .is_some_and(|a| !a.is_empty()),
        "successful explain should include non-empty policy_hits; got: {v}"
    );
    assert!(
        v.get("policies")
            .and_then(|x| x.as_array())
            .is_some_and(|a| !a.is_empty()),
        "successful explain should include non-empty policies; got: {v}"
    );

    let _ = fs::remove_dir_all(&work);
}
