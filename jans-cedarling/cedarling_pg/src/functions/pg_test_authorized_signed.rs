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

    let policy_path = work.join("policy-store.yaml");
    fs::write(&policy_path, POLICY_MULTI_ISSUER.as_bytes()).expect("write policy store");

    let bootstrap_path = crate::test_support::write_bootstrap_yaml(
        &work,
        &policy_path,
        "cedarling_pg_signed_pg_test",
    );
    let bootstrap_str = bootstrap_path.to_str().expect("bootstrap path utf-8");
    let escaped = bootstrap_str.replace('\'', "''");
    Spi::run(&format!("SET cedarling.bootstrap_config = '{escaped}'"))
        .expect("SET cedarling.bootstrap_config");

    // Dolphin access token carrying location=miami so basic_token_query_1.1 matches.
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
    let token_bundle = json!([
        {"mapping": "Dolphin::Access_Token", "payload": dolphin_access_token},
        {"mapping": "Dolphin::Dolphin_Token", "payload": dolphin_user_token},
    ])
    .to_string();

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
