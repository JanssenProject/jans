// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! `#[pg_test]` body for RLS + `cedarling_authorize_unsigned` (kept out of `lib.rs` for line limits).

use std::fs;
use std::path::PathBuf;

use pgrx::prelude::*;

const POLICY_UNSIGNED: &str = crate::test_support::POLICY_STORE_UNSIGNED_YAML;

fn temp_policy_workdir() -> PathBuf {
    let work =
        std::env::temp_dir().join(format!("cedarling_pg_rls_pg_test_{}", std::process::id()));
    let _ = fs::remove_dir_all(&work);
    fs::create_dir_all(&work).expect("temp work dir");
    work
}

fn ddl_create_table_policy() {
    Spi::run(
        r"CREATE TABLE cedarling_pg_rls_demo (
            id int PRIMARY KEY,
            label text NOT NULL,
            principal_json text NOT NULL,
            resource_json text NOT NULL
        )",
    )
    .expect("CREATE TABLE");
    Spi::run(
        r#"INSERT INTO cedarling_pg_rls_demo (id, label, principal_json, resource_json) VALUES
        (1, 'allowed_principal',
         '{"cedar_entity_mapping":{"entity_type":"Jans::TestPrincipal1","id":"id1"},"is_ok": true}',
         '{"cedar_entity_mapping":{"entity_type":"Jans::Issue","id":"random_id"},"org_id":"o1","country":"US"}'),
        (2, 'denied_principal',
         '{"cedar_entity_mapping":{"entity_type":"Jans::TestPrincipal1","id":"id2"},"is_ok": false}',
         '{"cedar_entity_mapping":{"entity_type":"Jans::Issue","id":"random_id"},"org_id":"o1","country":"US"}')"#,
    )
    .expect("INSERT");
    Spi::run("ALTER TABLE cedarling_pg_rls_demo ENABLE ROW LEVEL SECURITY")
        .expect("ENABLE ROW LEVEL SECURITY");
    Spi::run("ALTER TABLE cedarling_pg_rls_demo FORCE ROW LEVEL SECURITY")
        .expect("FORCE ROW LEVEL SECURITY");
    Spi::run("DROP POLICY IF EXISTS cedarling_pg_rls_demo_pol ON cedarling_pg_rls_demo").ok();
    Spi::run(
        r#"CREATE POLICY cedarling_pg_rls_demo_pol ON cedarling_pg_rls_demo FOR SELECT USING (
            cedarling_authorize_unsigned(
                principal_json,
                resource_json,
                'Jans::Action::"UpdateForTestPrincipals"',
                '{}'
            )
        )"#,
    )
    .expect("CREATE POLICY");
}

fn ddl_cleanup() {
    Spi::run("RESET ROLE").ok();
    Spi::run("SET row_security = off").ok();
    Spi::run(
        "DO $$ BEGIN EXECUTE format('REVOKE cedarling_pg_rls_demo FROM %I', session_user); END $$;",
    )
    .ok();
    Spi::run("DROP TABLE IF EXISTS cedarling_pg_rls_demo CASCADE").ok();
    Spi::run("DROP ROLE IF EXISTS cedarling_pg_rls_demo").ok();
    Spi::run("RESET cedarling.bootstrap_config").ok();
    crate::authz::cache::global_cache().clear_all();
    crate::engine::reset_for_pg_tests();
}

struct RlsPgTestGuard {
    work: PathBuf,
}

impl Drop for RlsPgTestGuard {
    fn drop(&mut self) {
        ddl_cleanup();
        let _ = fs::remove_dir_all(&self.work);
    }
}

/// RLS + `cedarling_authorize_unsigned` against `test_files/policy-store_no_trusted_issuers.yaml`.
pub fn run_rls_unsigned_policy_filters_select_under_row_security() {
    let work = temp_policy_workdir();
    let _guard = RlsPgTestGuard { work: work.clone() };
    let policy_path = work.join("policy-store.yaml");
    fs::write(&policy_path, POLICY_UNSIGNED.as_bytes()).expect("write policy store");
    let bootstrap_path = crate::test_support::write_bootstrap_yaml(
        &work,
        &policy_path,
        "cedarling_pg_rls_pg_test",
    );
    let bootstrap_str = bootstrap_path.to_str().expect("bootstrap path utf-8");
    let escaped = bootstrap_str.replace('\'', "''");

    Spi::run("DROP TABLE IF EXISTS cedarling_pg_rls_demo CASCADE").ok();
    Spi::run("DROP ROLE IF EXISTS cedarling_pg_rls_demo").ok();

    Spi::run(&format!("SET cedarling.bootstrap_config = '{escaped}'"))
        .expect("SET cedarling.bootstrap_config for RLS pg_test");

    ddl_create_table_policy();

    let super_count = Spi::get_one::<i64>("SELECT count(*)::bigint FROM cedarling_pg_rls_demo")
        .expect("SPI count superuser");
    assert_eq!(
        super_count,
        Some(2),
        "superuser session should bypass RLS and see both physical rows"
    );

    Spi::run(r"CREATE ROLE cedarling_pg_rls_demo NOSUPERUSER NOCREATEDB NOINHERIT NOLOGIN")
        .expect("CREATE ROLE");
    Spi::run("GRANT SELECT ON cedarling_pg_rls_demo TO cedarling_pg_rls_demo")
        .expect("GRANT SELECT");
    Spi::run(
        "DO $$ BEGIN EXECUTE format('GRANT cedarling_pg_rls_demo TO %I', session_user); END $$;",
    )
    .expect("GRANT membership to session user");
    Spi::run("SET ROLE cedarling_pg_rls_demo").expect("SET ROLE");
    Spi::run("SET row_security = on").expect("SET row_security");

    let rls_count = Spi::get_one::<i64>("SELECT count(*)::bigint FROM cedarling_pg_rls_demo")
        .expect("SPI count under RLS");
    assert_eq!(
        rls_count,
        Some(1),
        "non-superuser subject to RLS should see only Cedar-allowed rows"
    );
}
