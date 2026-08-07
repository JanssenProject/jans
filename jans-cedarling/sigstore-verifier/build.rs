// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Build script: validates embedded trust root certificates at compile time.
//!
//! If any embedded PEM file is corrupt or a CA certificate fails constraint
//! checks (`BasicConstraints` CA:true, `KeyUsage` `keyCertSign`), the build fails
//! immediately. This guarantees that `with_static_trust_root()` can `unwrap()`
//! safely at runtime.

use std::path::Path;

use base64::Engine;
use chrono::Utc;
use x509_parser::prelude::FromDer;

/// Fail the build if an embedded CA certificate expires within this many days.
/// Gives lead time to source, review, and merge updated trust-root PEMs before
/// the crate silently becomes un-buildable in production.
const EXPIRY_WARN_WINDOW_DAYS: i64 = 90;

fn main() {
    let trust_dir = Path::new("src/trust");

    let pem_files: &[(&str, bool)] = &[
        ("fulcio_root.pem", true),
        ("fulcio_intermediate.pem", true),
        ("rekor.pem", false),
        ("ctfe.pem", false),
        ("ctfe_2021.pem", false),
    ];

    for (filename, is_ca) in pem_files {
        let path = trust_dir.join(filename);
        let pem_bytes = std::fs::read(&path).unwrap_or_else(|e| {
            panic!("failed to read embedded trust file {}: {e}", path.display());
        });

        if *is_ca {
            validate_x509_cert(&pem_bytes, filename);
        } else {
            validate_public_key(&pem_bytes, filename);
        }
    }

    println!("cargo:rerun-if-changed=src/trust/");
}

fn validate_x509_cert(pem_bytes: &[u8], filename: &str) {
    let der = pem_to_der(pem_bytes, filename);
    let (_, cert) = x509_parser::certificate::X509Certificate::from_der(&der)
        .unwrap_or_else(|e| panic!("{filename}: X.509 DER parsing failed: {e}"));

    let tbs = &cert.tbs_certificate;

    let mut found_ca = false;
    let mut found_key_cert_sign = false;
    for ext in tbs.extensions() {
        use x509_parser::extensions::ParsedExtension;
        match ext.parsed_extension() {
            ParsedExtension::BasicConstraints(bc) => found_ca = bc.ca,
            ParsedExtension::KeyUsage(ku) => found_key_cert_sign = ku.key_cert_sign(),
            _ => {},
        }
    }
    assert!(
        found_ca,
        "{filename}: CA certificate missing BasicConstraints CA:true"
    );
    assert!(
        found_key_cert_sign,
        "{filename}: CA certificate missing KeyUsage keyCertSign"
    );
    println!("cargo:warning=validated CA cert: {filename} (CA:true, keyCertSign)");

    // Verify validity hasn't expired. Build fails if any cert is expired —
    // expired trust roots must be updated at the source before compilation.
    // Uses chrono::Utc::now() per project convention (SystemTime::now is
    // disallowed — may not work correctly in WASM).
    let not_after = tbs.validity.not_after.timestamp();
    let now = Utc::now().timestamp();
    assert!(
        not_after >= now,
        "{filename}: certificate expired at UNIX {not_after} (now: {now}). \
         Update the trust root PEM files from the Sigstore TUF repository."
    );

    // Fail early, well before actual expiry, so rotation happens on a planned
    // schedule instead of as an emergency once the cert has already expired.
    let seconds_left = not_after - now;
    let warn_window_seconds = EXPIRY_WARN_WINDOW_DAYS * 24 * 60 * 60;
    assert!(
        seconds_left >= warn_window_seconds,
        "{filename}: certificate expires in {} days (< {EXPIRY_WARN_WINDOW_DAYS}-day warning \
         window). Update the trust root PEM files from the Sigstore TUF repository.",
        seconds_left / (24 * 60 * 60)
    );
}

/// id-ecPublicKey (RFC 5480).
const OID_EC_PUBLIC_KEY: &str = "1.2.840.10045.2.1";
/// secp256r1 / prime256v1 named curve.
const OID_P256: &str = "1.2.840.10045.3.1.7";

fn validate_public_key(pem_bytes: &[u8], filename: &str) {
    let der = pem_to_der(pem_bytes, filename);
    let (_, spki) = x509_parser::x509::SubjectPublicKeyInfo::from_der(&der)
        .unwrap_or_else(|e| panic!("{filename}: SPKI DER parsing failed: {e}"));

    let algo_oid = spki.algorithm.algorithm.to_id_string();
    assert!(
        algo_oid == OID_EC_PUBLIC_KEY,
        "{filename}: expected id-ecPublicKey ({OID_EC_PUBLIC_KEY}), found {algo_oid}"
    );

    let curve_oid = spki
        .algorithm
        .parameters
        .as_ref()
        .and_then(|p| p.as_oid().ok())
        .unwrap_or_else(|| panic!("{filename}: EC key missing namedCurve parameter"))
        .to_id_string();
    assert!(
        curve_oid == OID_P256,
        "{filename}: expected P-256 curve ({OID_P256}), found {curve_oid}"
    );

    println!("cargo:warning=validated public key: {filename} (id-ecPublicKey, P-256)");
}

fn pem_to_der(pem_bytes: &[u8], filename: &str) -> Vec<u8> {
    let input = std::str::from_utf8(pem_bytes)
        .unwrap_or_else(|_| panic!("{filename}: invalid UTF-8 in PEM"));

    let mut in_body = false;
    let mut b64 = String::new();
    for line in input.lines() {
        if line.starts_with("-----BEGIN ") {
            in_body = true;
            continue;
        }
        if line.starts_with("-----END ") {
            break;
        }
        if in_body {
            b64.push_str(line.trim());
        }
    }

    base64::engine::general_purpose::STANDARD
        .decode(b64.as_bytes())
        .unwrap_or_else(|e| panic!("{filename}: base64 decode failed: {e}"))
}
