// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Opt-in scanner: run the verifier against a checkout of the
//! sigstore-conformance `bundle-verify` assets and report pass/fail vs the
//! expected outcome (dir name ending in `_fail` means failure is expected).
//!
//! Inert unless `SIGSTORE_CONFORMANCE_DIR` points at
//! `<sigstore-conformance>/test/assets/bundle-verify`. Cases needing features
//! we don't implement (managed key, custom trusted root) are skipped.
//!
//! ```text
//! SIGSTORE_CONFORMANCE_DIR=/path/to/bundle-verify cargo test --test conformance_scan -- --nocapture
//! ```

use std::fs;
use std::path::Path;

use sigstore_verifier::{IdentityMatch, SigstoreBlobVerifier, VerificationPolicy};

const DEFAULT_IDENTITY: &str = "https://github.com/sigstore-conformance/extremely-dangerous-public-oidc-beacon/.github/workflows/extremely-dangerous-oidc-beacon.yml@refs/heads/main";
const DEFAULT_ISSUER: &str = "https://token.actions.githubusercontent.com";

#[test]
fn scan_conformance_bundle_verify() {
    let Ok(root) = std::env::var("SIGSTORE_CONFORMANCE_DIR") else {
        eprintln!("SIGSTORE_CONFORMANCE_DIR unset — skipping conformance scan");
        return;
    };
    let root = Path::new(&root);
    let default_artifact = fs::read(root.join("a.txt")).ok();

    let verifier = SigstoreBlobVerifier::with_static_trust_root();

    let mut expected_pass = Vec::new(); // (name, ok, detail)
    let mut expected_fail = Vec::new();
    let mut skipped = Vec::new();

    let mut dirs: Vec<_> = fs::read_dir(root)
        .expect("read conformance dir")
        .filter_map(Result::ok)
        .map(|e| e.path())
        .filter(|p| p.is_dir())
        .collect();
    dirs.sort();

    for dir in dirs {
        let name = dir.file_name().unwrap().to_string_lossy().to_string();
        let bundle_path = dir.join("bundle.sigstore.json");
        if !bundle_path.exists() {
            continue;
        }
        // Skip cases that need features we don't implement.
        if dir.join("key.pub").exists() {
            skipped.push(format!("{name} (managed key)"));
            continue;
        }
        if dir.join("trusted_root.json").exists() {
            skipped.push(format!("{name} (custom trusted root)"));
            continue;
        }

        let bundle = fs::read(&bundle_path).unwrap();
        let artifact = dir
            .join("artifact")
            .exists()
            .then(|| fs::read(dir.join("artifact")).unwrap())
            .or_else(|| default_artifact.clone())
            .unwrap_or_default();
        let identity = read_trim(&dir.join("identity")).unwrap_or_else(|| DEFAULT_IDENTITY.into());
        let issuer = read_trim(&dir.join("issuer")).unwrap_or_else(|| DEFAULT_ISSUER.into());

        let policy = VerificationPolicy {
            cert_identity: IdentityMatch::Exact(identity),
            cert_issuer: issuer,
        };
        let res = verifier.verify(&artifact, &bundle, &policy);
        let detail = match &res {
            Ok(_) => "OK".to_string(),
            Err(e) => format!("{e}"),
        };

        if name.ends_with("_fail") {
            expected_fail.push((name, res.is_err(), detail));
        } else {
            expected_pass.push((name, res.is_ok(), detail));
        }
    }

    println!("\n=== EXPECTED-PASS cases ===");
    let mut pass_gaps = 0;
    for (name, ok, detail) in &expected_pass {
        let mark = if *ok { "PASS" } else { "**GAP**" };
        if !ok {
            pass_gaps += 1;
        }
        println!("[{mark}] {name}: {detail}");
    }

    println!("\n=== EXPECTED-FAIL cases ===");
    let mut fail_gaps = 0;
    for (name, correctly_failed, detail) in &expected_fail {
        let mark = if *correctly_failed { "rejected" } else { "**FALSE-ACCEPT**" };
        if !correctly_failed {
            fail_gaps += 1;
        }
        println!("[{mark}] {name}: {detail}");
    }

    println!("\n=== SKIPPED (out of scope) ===");
    for s in &skipped {
        println!("  {s}");
    }

    println!(
        "\n=== SUMMARY: {} pass-cases ({} gaps), {} fail-cases ({} false-accepts), {} skipped ===",
        expected_pass.len(),
        pass_gaps,
        expected_fail.len(),
        fail_gaps,
        skipped.len()
    );

    // A false-accept (expected-fail that we accepted) is a security bug — never allow.
    assert_eq!(fail_gaps, 0, "verifier accepted bundle(s) that must be rejected");
}

fn read_trim(p: &Path) -> Option<String> {
    fs::read_to_string(p).ok().map(|s| s.trim().to_string())
}
