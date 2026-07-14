// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! `#[pg_test]` bodies for the batch authorize surface:
//! `cedarling_authorize_unsigned_batch` and `cedarling_authorize_multi_issuer_batch`.
//!
//! Bootstraps a real engine against the unsigned policy store used by the RLS
//! test, sends the batch request as JSON via SPI, and asserts per-item
//! decisions + `batch_id` on the returned rows.

use std::fs;
use std::path::PathBuf;

use pgrx::prelude::*;
use serde_json::json;

const POLICY_UNSIGNED: &str = crate::test_support::POLICY_STORE_UNSIGNED_YAML;

fn temp_policy_workdir(prefix: &str) -> PathBuf {
    let work =
        std::env::temp_dir().join(format!("cedarling_pg_batch_{prefix}_{}", std::process::id()));
    let _ = fs::remove_dir_all(&work);
    fs::create_dir_all(&work).expect("temp work dir");
    work
}

struct BatchPgTestGuard {
    work: PathBuf,
}

impl Drop for BatchPgTestGuard {
    fn drop(&mut self) {
        Spi::run("RESET cedarling.bootstrap_config").ok();
        crate::authz::cache::global_cache().clear_all();
        crate::engine::reset_for_pg_tests();
        let _ = fs::remove_dir_all(&self.work);
    }
}

fn bootstrap_engine_with_unsigned_policy(work: &std::path::Path, app_name: &str) {
    let policy_path = work.join("policy-store.yaml");
    fs::write(&policy_path, POLICY_UNSIGNED.as_bytes()).expect("write policy store");
    let bootstrap_path = crate::test_support::write_bootstrap_yaml(work, &policy_path, app_name);
    let bootstrap_str = bootstrap_path.to_str().expect("bootstrap path utf-8");
    let escaped = bootstrap_str.replace('\'', "''");
    Spi::run(&format!("SET cedarling.bootstrap_config = '{escaped}'"))
        .expect("SET cedarling.bootstrap_config");
}

/// N=3 items, same allowing `is_ok=true` principal — expect 3 rows all decision=true
/// and every row carrying the same non-empty `batch_id`.
pub fn run_unsigned_batch_three_allow_items() {
    let work = temp_policy_workdir("unsigned_allow");
    let _guard = BatchPgTestGuard { work: work.clone() };
    bootstrap_engine_with_unsigned_policy(&work, "cedarling_pg_batch_unsigned");

    let make_item = |id: &str| {
        json!({
            "resource": {
                "cedar_entity_mapping": {"entity_type": "Jans::Issue", "id": id},
                "org_id": "o1",
                "country": "US",
            },
            "action": "Jans::Action::\"UpdateForTestPrincipals\"",
            "context": {},
        })
    };
    let request = json!({
        "principal": {
            "cedar_entity_mapping": {"entity_type": "Jans::TestPrincipal1", "id": "id1"},
            "is_ok": true,
        },
        "items": [make_item("a"), make_item("b"), make_item("c")],
    })
    .to_string();

    let row_count = Spi::get_one_with_args::<i64>(
        "SELECT count(*)::bigint FROM cedarling_authorize_unsigned_batch($1)",
        &[request.as_str().into()],
    )
    .expect("SPI count")
    .expect("count is not NULL");
    assert_eq!(row_count, 3, "N=3 items must yield 3 rows");

    let allow_count = Spi::get_one_with_args::<i64>(
        "SELECT count(*)::bigint FROM cedarling_authorize_unsigned_batch($1) WHERE decision",
        &[request.as_str().into()],
    )
    .expect("SPI allow count")
    .expect("allow count is not NULL");
    assert_eq!(allow_count, 3, "all 3 items must Allow with is_ok=true");

    let batch_id_count = Spi::get_one_with_args::<i64>(
        "SELECT count(DISTINCT batch_id)::bigint FROM cedarling_authorize_unsigned_batch($1) \
         WHERE batch_id <> ''",
        &[request.as_str().into()],
    )
    .expect("SPI distinct batch_id count")
    .expect("distinct count is not NULL");
    assert_eq!(
        batch_id_count, 1,
        "every row in one batch must share the same non-empty batch_id"
    );
}

/// Empty `items` — batch-level validation error → empty result set, engine
/// diagnostic logged upstream. Under fail-closed enforcement this is the
/// analog of the single-item function returning `false`.
pub fn run_unsigned_batch_empty_items_returns_no_rows() {
    let work = temp_policy_workdir("unsigned_empty");
    let _guard = BatchPgTestGuard { work: work.clone() };
    bootstrap_engine_with_unsigned_policy(&work, "cedarling_pg_batch_unsigned_empty");

    let request = json!({
        "principal": {
            "cedar_entity_mapping": {"entity_type": "Jans::TestPrincipal1", "id": "id1"},
            "is_ok": true,
        },
        "items": [],
    })
    .to_string();

    let row_count = Spi::get_one_with_args::<i64>(
        "SELECT count(*)::bigint FROM cedarling_authorize_unsigned_batch($1)",
        &[request.as_str().into()],
    )
    .expect("SPI count")
    .expect("count is not NULL");
    assert_eq!(
        row_count, 0,
        "empty items must produce an empty result set (batch-level validation error)"
    );
}

/// Multi-issuer batch with empty `tokens` — batch-level validation error →
/// empty result set. Exercises the multi-issuer FFI shape end-to-end without
/// requiring a full JWT-signing harness (that flow is covered by core lib
/// tests and other bindings).
pub fn run_multi_issuer_batch_empty_tokens_returns_no_rows() {
    let work = temp_policy_workdir("multi_empty");
    let _guard = BatchPgTestGuard { work: work.clone() };
    bootstrap_engine_with_unsigned_policy(&work, "cedarling_pg_batch_multi_empty");

    let request = json!({
        "tokens": [],
        "items": [{
            "resource": {"cedar_entity_mapping": {"entity_type": "Jans::Issue", "id": "a"}},
            "action": "Jans::Action::\"Update\"",
            "context": {},
        }],
    })
    .to_string();

    let row_count = Spi::get_one_with_args::<i64>(
        "SELECT count(*)::bigint FROM cedarling_authorize_multi_issuer_batch($1)",
        &[request.as_str().into()],
    )
    .expect("SPI count")
    .expect("count is not NULL");
    assert_eq!(
        row_count, 0,
        "empty tokens must produce an empty result set (batch-level validation error)"
    );
}
