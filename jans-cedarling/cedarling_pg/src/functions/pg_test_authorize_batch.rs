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
        Spi::run("RESET cedarling.mode").ok();
        Spi::run("RESET cedarling.fail_mode").ok();
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
pub(crate) fn run_unsigned_batch_three_allow_items() {
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

/// Ordering: N=3 items where item[1] carries a malformed action UID
/// (synthesizes a fail-closed Deny) sandwiched between two allowing items.
/// Verifies each row's `decision` reflects the corresponding input item,
/// not a uniform result set — this is the SQL-surface analog of the
/// mixed-decision ordering tests in Python / WASM / UniFFI / Java / C / Go.
pub(crate) fn run_unsigned_batch_mixed_decisions_preserve_order() {
    let work = temp_policy_workdir("unsigned_mixed");
    let _guard = BatchPgTestGuard { work: work.clone() };
    bootstrap_engine_with_unsigned_policy(&work, "cedarling_pg_batch_unsigned_mixed");

    let ok_action = "Jans::Action::\"UpdateForTestPrincipals\"";
    let make_item = |id: &str, action: &str| {
        json!({
            "resource": {
                "cedar_entity_mapping": {"entity_type": "Jans::Issue", "id": id},
                "org_id": "o1",
                "country": "US",
            },
            "action": action,
            "context": {},
        })
    };
    let request = json!({
        "principal": {
            "cedar_entity_mapping": {"entity_type": "Jans::TestPrincipal1", "id": "id1"},
            "is_ok": true,
        },
        "items": [
            make_item("a", ok_action),
            make_item("b", "this is not a valid uid"),
            make_item("c", ok_action),
        ],
    })
    .to_string();

    let row_count = Spi::get_one_with_args::<i64>(
        "SELECT count(*)::bigint FROM cedarling_authorize_unsigned_batch($1)",
        &[request.as_str().into()],
    )
    .expect("SPI count")
    .expect("count is not NULL");
    assert_eq!(row_count, 3, "N=3 items must yield 3 rows");

    // Read decisions in item_index order and assert positional agreement.
    let decisions: Vec<(i32, bool)> = Spi::connect(|client| {
        client
            .select(
                "SELECT item_index, decision FROM cedarling_authorize_unsigned_batch($1) \
                 ORDER BY item_index",
                None,
                &[request.as_str().into()],
            )
            .expect("SPI select")
            .map(|row| {
                let idx: i32 = row.get::<i32>(1).expect("item_index col").expect("non-null");
                let decision: bool = row.get::<bool>(2).expect("decision col").expect("non-null");
                (idx, decision)
            })
            .collect()
    });
    assert_eq!(decisions.len(), 3);
    assert_eq!(decisions[0], (0, true), "item 0 must allow");
    assert_eq!(
        decisions[1],
        (1, false),
        "item 1 with bad action must fail closed"
    );
    assert_eq!(decisions[2], (2, true), "item 2 must allow");
}

/// Empty `items` → single sentinel row at `item_index = -1` with fail-closed
/// `decision = false`. Guards `bool_and(decision)` from collapsing to `NULL`.
pub(crate) fn run_unsigned_batch_empty_items_synthesizes_fail_closed_row() {
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

    let rows: Vec<(i32, bool, String)> = Spi::connect(|client| {
        client
            .select(
                "SELECT item_index, decision, batch_id \
                 FROM cedarling_authorize_unsigned_batch($1) ORDER BY item_index",
                None,
                &[request.as_str().into()],
            )
            .expect("SPI select")
            .map(|row| {
                let idx: i32 = row.get::<i32>(1).expect("item_index col").expect("non-null");
                let decision: bool =
                    row.get::<bool>(2).expect("decision col").expect("non-null");
                let batch_id: String =
                    row.get::<String>(3).expect("batch_id col").expect("non-null");
                (idx, decision, batch_id)
            })
            .collect()
    });
    assert_eq!(
        rows,
        vec![(-1, false, String::new())],
        "empty items must synthesize a single fail-closed sentinel row at -1"
    );
}

/// Multi-issuer with empty `tokens` + N=1 item → one fail-closed row per
/// input item (parity with N single-item calls), not an empty result set.
pub(crate) fn run_multi_issuer_batch_empty_tokens_synthesizes_fail_closed_rows() {
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

    let rows: Vec<(i32, bool, String)> = Spi::connect(|client| {
        client
            .select(
                "SELECT item_index, decision, batch_id \
                 FROM cedarling_authorize_multi_issuer_batch($1) ORDER BY item_index",
                None,
                &[request.as_str().into()],
            )
            .expect("SPI select")
            .map(|row| {
                let idx: i32 = row.get::<i32>(1).expect("item_index col").expect("non-null");
                let decision: bool =
                    row.get::<bool>(2).expect("decision col").expect("non-null");
                let batch_id: String =
                    row.get::<String>(3).expect("batch_id col").expect("non-null");
                (idx, decision, batch_id)
            })
            .collect()
    });
    assert_eq!(
        rows,
        vec![(0, false, String::new())],
        "empty tokens with N=1 item must synthesize one fail-closed row per input item"
    );
}

/// Unparseable JSON (no item count recoverable) → single sentinel row at
/// `item_index = -1` with fail-closed `decision = false`.
pub(crate) fn run_unsigned_batch_malformed_json_synthesizes_sentinel_row() {
    let work = temp_policy_workdir("unsigned_bad_json");
    let _guard = BatchPgTestGuard { work: work.clone() };
    bootstrap_engine_with_unsigned_policy(&work, "cedarling_pg_batch_unsigned_bad_json");

    let request = "{ not valid json";

    let rows: Vec<(i32, bool, String)> = Spi::connect(|client| {
        client
            .select(
                "SELECT item_index, decision, batch_id \
                 FROM cedarling_authorize_unsigned_batch($1) ORDER BY item_index",
                None,
                &[request.into()],
            )
            .expect("SPI select")
            .map(|row| {
                let idx: i32 = row.get::<i32>(1).expect("item_index col").expect("non-null");
                let decision: bool =
                    row.get::<bool>(2).expect("decision col").expect("non-null");
                let batch_id: String =
                    row.get::<String>(3).expect("batch_id col").expect("non-null");
                (idx, decision, batch_id)
            })
            .collect()
    });
    assert_eq!(
        rows,
        vec![(-1, false, String::new())],
        "malformed JSON must synthesize a single fail-closed sentinel row at -1"
    );
}

/// One batch call bumps `cedarling_status.total_requests` by 1 and appends
/// one per-item trace to the ring buffer, each stamped with the shared
/// `batch_id`. Guards the observability contract (record_request +
/// per-item record_decision + per-item push_trace).
pub(crate) fn run_unsigned_batch_observability_records_requests_and_traces() {
    let work = temp_policy_workdir("unsigned_obs");
    let _guard = BatchPgTestGuard { work: work.clone() };
    bootstrap_engine_with_unsigned_policy(&work, "cedarling_pg_batch_unsigned_obs");

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

    let before = current_total_requests();
    let allowed_before = current_allowed();

    let _ = Spi::run_with_args(
        "SELECT * FROM cedarling_authorize_unsigned_batch($1)",
        &[request.as_str().into()],
    );

    assert_eq!(
        current_total_requests() - before,
        1,
        "one batch SQL call must bump total_requests exactly once"
    );
    assert_eq!(
        current_allowed() - allowed_before,
        3,
        "three Allow items must bump allowed by 3 (one record_decision per item)"
    );

    let traces_json = crate::observability::trace::cedarling_recent_traces(Some(10));
    let arr = traces_json.0.as_array().cloned().unwrap_or_default();
    let batch_traces: Vec<&serde_json::Value> = arr
        .iter()
        .filter(|t| t.get("batch_id").is_some())
        .collect();
    assert_eq!(
        batch_traces.len(),
        3,
        "one per-item trace must be recorded per batch item"
    );
    let first_bid = batch_traces[0]
        .get("batch_id")
        .and_then(|v| v.as_str())
        .expect("batch_id string");
    assert!(!first_bid.is_empty(), "batch_id on trace must be non-empty");
    for t in &batch_traces {
        assert_eq!(
            t.get("batch_id").and_then(|v| v.as_str()),
            Some(first_bid),
            "all per-item traces from one batch must share the same batch_id"
        );
        assert_eq!(t.get("decision").and_then(|v| v.as_str()), Some("allow"));
    }
}

/// Batch-level failure path bumps `total_requests` AND `errors` — closes the
/// errors/requests skew the current code had before this fix.
pub(crate) fn run_unsigned_batch_failure_bumps_requests_and_errors() {
    let work = temp_policy_workdir("unsigned_fail_obs");
    let _guard = BatchPgTestGuard { work: work.clone() };
    bootstrap_engine_with_unsigned_policy(&work, "cedarling_pg_batch_unsigned_fail_obs");

    let requests_before = current_total_requests();
    let errors_before = current_errors();

    let _ = Spi::run_with_args(
        "SELECT * FROM cedarling_authorize_unsigned_batch($1)",
        &[("{ not valid json").into()],
    );

    assert_eq!(
        current_total_requests() - requests_before,
        1,
        "batch failure must still bump total_requests once (matches single-item)"
    );
    assert!(
        current_errors() > errors_before,
        "batch failure must bump the error counter"
    );
}

fn current_total_requests() -> i64 {
    read_status_field("total_requests")
}

fn current_allowed() -> i64 {
    read_status_field("allowed")
}

fn current_errors() -> i64 {
    read_status_field("errors")
}

fn read_status_field(field: &str) -> i64 {
    let sql = format!("SELECT (cedarling_status()->>'{field}')::bigint");
    Spi::get_one::<i64>(&sql)
        .expect("SPI status")
        .expect("status field non-null")
}

/// Clean policy Deny (no error involved): `principal.is_ok = false` causes the
/// `UpdateForTestPrincipals` permit's `when { principal.is_ok }` clause to fail,
/// so Cedar returns Deny with no `error_category`. `decision = false` in
/// enforcement mode; the underlying trace records `decision = "deny"`.
pub(crate) fn run_unsigned_batch_clean_policy_deny() {
    let work = temp_policy_workdir("unsigned_policy_deny");
    let _guard = BatchPgTestGuard { work: work.clone() };
    bootstrap_engine_with_unsigned_policy(&work, "cedarling_pg_batch_unsigned_policy_deny");

    let request = json!({
        "principal": {
            "cedar_entity_mapping": {"entity_type": "Jans::TestPrincipal1", "id": "id1"},
            "is_ok": false,
        },
        "items": [{
            "resource": {
                "cedar_entity_mapping": {"entity_type": "Jans::Issue", "id": "a"},
                "org_id": "o1",
                "country": "US",
            },
            "action": "Jans::Action::\"UpdateForTestPrincipals\"",
            "context": {},
        }],
    })
    .to_string();

    let rows: Vec<(i32, bool, String)> = Spi::connect(|client| {
        client
            .select(
                "SELECT item_index, decision, batch_id \
                 FROM cedarling_authorize_unsigned_batch($1) ORDER BY item_index",
                None,
                &[request.as_str().into()],
            )
            .expect("SPI select")
            .map(|row| {
                let idx: i32 = row.get::<i32>(1).expect("item_index").expect("non-null");
                let decision: bool = row.get::<bool>(2).expect("decision").expect("non-null");
                let batch_id: String =
                    row.get::<String>(3).expect("batch_id").expect("non-null");
                (idx, decision, batch_id)
            })
            .collect()
    });
    assert_eq!(rows.len(), 1);
    assert_eq!(rows[0].0, 0);
    assert!(!rows[0].1, "policy-Deny item must return decision = false");
    assert!(
        !rows[0].2.is_empty(),
        "clean-Deny row must still carry a real batch_id (engine ran successfully)"
    );

    let traces = crate::observability::trace::cedarling_recent_traces(Some(5));
    let arr = traces.0.as_array().cloned().unwrap_or_default();
    let batch_trace = arr
        .iter()
        .find(|t| t.get("batch_id").is_some())
        .expect("per-item batch trace must be present");
    assert_eq!(batch_trace.get("decision").and_then(|v| v.as_str()), Some("deny"));
    assert!(
        batch_trace.get("error_category").is_none(),
        "clean policy-Deny must not tag error_category (it is not an error)"
    );
}

/// Shadow mode: raw decisions are `deny` for `is_ok=false`, but every returned
/// row is `true` (shadow lets RLS through). Traces still record the raw `deny`
/// via the pre-flip `record_decision`.
pub(crate) fn run_unsigned_batch_shadow_mode_returns_true_records_raw() {
    let work = temp_policy_workdir("unsigned_shadow");
    let _guard = BatchPgTestGuard { work: work.clone() };
    bootstrap_engine_with_unsigned_policy(&work, "cedarling_pg_batch_unsigned_shadow");
    Spi::run("SET cedarling.mode = 'shadow'").expect("SET mode");

    let request = json!({
        "principal": {
            "cedar_entity_mapping": {"entity_type": "Jans::TestPrincipal1", "id": "id1"},
            "is_ok": false,
        },
        "items": [{
            "resource": {
                "cedar_entity_mapping": {"entity_type": "Jans::Issue", "id": "a"},
                "org_id": "o1",
                "country": "US",
            },
            "action": "Jans::Action::\"UpdateForTestPrincipals\"",
            "context": {},
        }],
    })
    .to_string();

    let denied_before = read_status_field("denied");
    let all_true = Spi::get_one_with_args::<bool>(
        "SELECT bool_and(decision) FROM cedarling_authorize_unsigned_batch($1)",
        &[request.as_str().into()],
    )
    .expect("SPI bool_and")
    .expect("bool_and non-null");
    assert!(
        all_true,
        "shadow mode must return decision = true even for policy-Deny outcomes"
    );
    assert_eq!(
        read_status_field("denied") - denied_before,
        1,
        "shadow must still bump `denied` via raw record_decision (raw was deny)"
    );

    let traces = crate::observability::trace::cedarling_recent_traces(Some(5));
    let arr = traces.0.as_array().cloned().unwrap_or_default();
    let batch_trace = arr
        .iter()
        .find(|t| t.get("batch_id").is_some())
        .expect("per-item batch trace");
    assert_eq!(
        batch_trace.get("decision").and_then(|v| v.as_str()),
        Some("deny"),
        "trace must record the RAW pre-shadow decision"
    );
    assert_eq!(
        batch_trace.get("shadow").and_then(|v| v.as_bool()),
        Some(true),
        "trace must be tagged shadow"
    );
}

/// Fail-open on batch-level failure: synthesized rows flip to `decision = true`
/// (matches what N single-item calls would return in fail-open mode).
pub(crate) fn run_unsigned_batch_fail_open_synthesizes_true_rows() {
    let work = temp_policy_workdir("unsigned_fail_open");
    let _guard = BatchPgTestGuard { work: work.clone() };
    bootstrap_engine_with_unsigned_policy(&work, "cedarling_pg_batch_unsigned_fail_open");
    Spi::run("SET cedarling.fail_mode = 'open'").expect("SET fail_mode");

    let malformed = "{ not valid json";
    let rows: Vec<(i32, bool, String)> = Spi::connect(|client| {
        client
            .select(
                "SELECT item_index, decision, batch_id \
                 FROM cedarling_authorize_unsigned_batch($1) ORDER BY item_index",
                None,
                &[malformed.into()],
            )
            .expect("SPI select")
            .map(|row| {
                let idx: i32 = row.get::<i32>(1).expect("item_index").expect("non-null");
                let decision: bool = row.get::<bool>(2).expect("decision").expect("non-null");
                let batch_id: String =
                    row.get::<String>(3).expect("batch_id").expect("non-null");
                (idx, decision, batch_id)
            })
            .collect()
    });
    assert_eq!(
        rows,
        vec![(-1, true, String::new())],
        "fail_mode=open must flip the sentinel row's decision to true"
    );
}
