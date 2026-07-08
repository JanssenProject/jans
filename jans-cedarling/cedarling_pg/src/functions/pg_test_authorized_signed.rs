// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! `#[pg_test]` body for the JWT / multi-issuer authorize path (`cedarling_authorized`).
//!
//! Boots a real Cedarling engine against `test_files/policy-store-multi-issuer-basic.yaml`,
//! generates a Dolphin token bundle, and asserts both an allow and a deny outcome through
//! the SQL surface. Closes the "signed path is never exercised against a real engine"
//! coverage gap (`cedarling_authorize_multi_issuer` is otherwise only reached via
//! Rust-level smoke tests in `authz/bridge.rs`).

use std::fs;
use std::path::PathBuf;

use pgrx::prelude::*;
use serde_json::json;
use test_utils::token_claims::generate_token_using_claims;

const POLICY_MULTI_ISSUER: &str = include_str!(concat!(
    env!("CARGO_MANIFEST_DIR"),
    "/../test_files/policy-store-multi-issuer-basic.yaml"
));

fn temp_policy_workdir() -> PathBuf {
    let work = std::env::temp_dir()
        .join(format!("cedarling_pg_signed_pg_test_{}", std::process::id()));
    let _ = fs::remove_dir_all(&work);
    fs::create_dir_all(&work).expect("temp work dir");
    work
}

struct SignedPgTestGuard {
    work: PathBuf,
}

impl Drop for SignedPgTestGuard {
    fn drop(&mut self) {
        Spi::run("RESET cedarling.bootstrap_config").ok();
        crate::authz::cache::global_cache().clear_all();
        crate::engine::reset_for_pg_tests();
        let _ = fs::remove_dir_all(&self.work);
    }
}

/// Writes the multi-issuer policy store + bootstrap YAML into `work`, then points
/// `cedarling.bootstrap_config` at it. Subsequent engine calls in the test will load this
/// store on first use.
fn bootstrap_engine_with_multi_issuer_policy(work: &std::path::Path, app_name: &str) {
    let policy_path = work.join("policy-store.yaml");
    fs::write(&policy_path, POLICY_MULTI_ISSUER.as_bytes()).expect("write policy store");

    let bootstrap_path = crate::test_support::write_bootstrap_yaml(work, &policy_path, app_name);
    let bootstrap_str = bootstrap_path.to_str().expect("bootstrap path utf-8");
    let escaped = bootstrap_str.replace('\'', "''");
    Spi::run(&format!("SET cedarling.bootstrap_config = '{escaped}'"))
        .expect("SET cedarling.bootstrap_config");
}

/// Builds a Dolphin token bundle matching `basic_token_query_1.1_default_entity`: the
/// dolphin access token carries `location=["miami", "orlando"]` so the `getTag("location")
/// .contains("miami")` clause in the permit policy is satisfied.
fn build_dolphin_token_bundle() -> String {
    let dolphin_access_token = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_123",
        "jti": "dolphin123",
        "client_id": "dolphin_client_123",
        "aud": "dolphin_audience",
        "location": ["miami", "orlando"],
        "scope": ["read", "write"],
        "exp": 2_000_000_000,
        "iat": 1_516_239_022,
    }));
    let dolphin_user_token = generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "sub": "dolphin_user_123",
        "jti": "dolphin_user_123",
        "client_id": "dolphin_client_123",
        "aud": "dolphin_audience",
        "exp": 2_000_000_000,
        "iat": 1_516_239_022,
    }));
    json!([
        {"mapping": "Dolphin::Access_Token", "payload": dolphin_access_token},
        {"mapping": "Dolphin::Dolphin_Token", "payload": dolphin_user_token},
    ])
    .to_string()
}

/// Real-engine signed authorization through the SQL surface.
///
/// Asserts both directions:
///   * `1694c954f8a3` resource → `Acme::Action::"GetFood"` with the Dolphin token bundle
///     matches `basic_token_query_1.1_default_entity` → allow.
///   * `not-permitted` resource → same action + tokens → no policy applies → Cedar default
///     deny.
pub fn run_signed_authorized_allow_then_deny() {
    let work = temp_policy_workdir();
    let _guard = SignedPgTestGuard { work: work.clone() };

    bootstrap_engine_with_multi_issuer_policy(&work, "cedarling_pg_signed_pg_test");
    let token_bundle = build_dolphin_token_bundle();

    let allow_resource = json!({
        "cedar_entity_mapping": {
            "entity_type": "Acme::Resource",
            "id": "1694c954f8a3",
        },
        "name": "Approved Dolphin Foods",
    })
    .to_string();

    let allow_decision = Spi::get_one_with_args::<bool>(
        "SELECT cedarling_authorized($1, $2, $3)",
        &[
            allow_resource.as_str().into(),
            token_bundle.as_str().into(),
            "Acme::Action::\"GetFood\"".into(),
        ],
    )
    .expect("SPI: allow case")
    .expect("allow decision is not NULL");
    assert!(
        allow_decision,
        "expected ALLOW: Dolphin access (location=miami) + dolphin token against \
         Acme::Resource::\"1694c954f8a3\" should match basic_token_query_1.1"
    );

    // Same tokens, same action, different resource id with no parent relationship to
    // either permit's `resource in ...` target → no permit matches → Cedar default deny.
    let deny_resource = json!({
        "cedar_entity_mapping": {
            "entity_type": "Acme::Resource",
            "id": "not-permitted",
        },
        "name": "Unauthorized Resource",
    })
    .to_string();

    let deny_decision = Spi::get_one_with_args::<bool>(
        "SELECT cedarling_authorized($1, $2, $3)",
        &[
            deny_resource.as_str().into(),
            token_bundle.as_str().into(),
            "Acme::Action::\"GetFood\"".into(),
        ],
    )
    .expect("SPI: deny case")
    .expect("deny decision is not NULL");
    assert!(
        !deny_decision,
        "expected DENY: no permit matches Acme::Resource::\"not-permitted\""
    );
}

/// End-to-end JWT-via-GUC integration: the production RLS pattern is
///
/// ```sql
/// SELECT cedarling_set_tokens('[...]'::jsonb);
/// CREATE POLICY rls ON t FOR SELECT USING (cedarling_authorized_row_jwt(t, 'Read'));
/// SELECT * FROM t;
/// ```
///
/// `cedarling_authorized_row_jwt` (`authorized_row.rs:301-310`) passes `None` for
/// `token_bundle`, so `resolve_token_bundle()` (`authorized.rs:345-351`) MUST read from
/// the `cedarling.tokens` GUC. The other signed test passes tokens as an explicit
/// argument, which bypasses the GUC entirely.
///
/// This test exercises the full chain:
///   `cedarling_set_tokens` → GUC write → row materialization →
///   `cedarling_authorized_row_jwt` → `resolve_token_bundle` (GUC read) →
///   `cedarling_authorized` → engine
///
/// Asserts both an allow and a deny outcome differ only by the row materialized from the
/// probe table.
pub fn run_jwt_row_uses_cedarling_tokens_guc() {
    let work = temp_policy_workdir();
    let _guard = SignedPgTestGuard { work: work.clone() };

    bootstrap_engine_with_multi_issuer_policy(&work, "cedarling_pg_jwt_guc_pg_test");
    let token_bundle = build_dolphin_token_bundle();

    // Probe table carries its own `cedar_entity_mapping` column so row materialization
    // skips the entity_map lookup (`build_resource_json_from_row` at `resource/row.rs:35`).
    Spi::run("DROP TABLE IF EXISTS cedarling_pg_jwt_guc_probe CASCADE").ok();
    Spi::run(
        r"CREATE TABLE cedarling_pg_jwt_guc_probe (
            id int PRIMARY KEY,
            cedar_entity_mapping jsonb NOT NULL,
            name text NOT NULL
        )",
    )
    .expect("CREATE TABLE");
    Spi::run(
        r#"INSERT INTO cedarling_pg_jwt_guc_probe(id, cedar_entity_mapping, name) VALUES
            (1, '{"entity_type":"Acme::Resource","id":"1694c954f8a3"}'::jsonb, 'Approved Dolphin Foods'),
            (2, '{"entity_type":"Acme::Resource","id":"not-permitted"}'::jsonb, 'Unauthorized')"#,
    )
    .expect("INSERT probe rows");

    // Write the GUC via the production SQL surface — this is the path that real callers
    // use (it goes through cedarling_set_tokens → set_config('cedarling.tokens', ...)
    // → check hook → GUC store).
    let escaped_bundle = token_bundle.replace('\'', "''");
    Spi::run(&format!(
        "SELECT cedarling_set_tokens('{escaped_bundle}'::jsonb)"
    ))
    .expect("cedarling_set_tokens");

    let allow = Spi::get_one::<bool>(
        "SELECT cedarling_authorized_row_jwt(t, 'Acme::Action::\"GetFood\"') \
         FROM cedarling_pg_jwt_guc_probe t WHERE id = 1",
    )
    .expect("SPI allow case")
    .expect("allow decision is not NULL");
    assert!(
        allow,
        "expected ALLOW: cedarling.tokens GUC carries the Dolphin bundle that permits \
         Acme::Resource::\"1694c954f8a3\" via basic_token_query_1.1_default_entity"
    );

    let deny = Spi::get_one::<bool>(
        "SELECT cedarling_authorized_row_jwt(t, 'Acme::Action::\"GetFood\"') \
         FROM cedarling_pg_jwt_guc_probe t WHERE id = 2",
    )
    .expect("SPI deny case")
    .expect("deny decision is not NULL");
    assert!(
        !deny,
        "expected DENY: same tokens, same action; only the row's entity id differs and \
         no permit matches Acme::Resource::\"not-permitted\""
    );

    Spi::run("DROP TABLE IF EXISTS cedarling_pg_jwt_guc_probe CASCADE").ok();
}
