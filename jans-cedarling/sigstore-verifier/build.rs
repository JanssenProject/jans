// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Build script: validates embedded trust root certificates at compile time.
//!
//! If any embedded PEM file is corrupt or a CA certificate fails constraint
//! checks (BasicConstraints CA:true, KeyUsage keyCertSign), the build fails
//! immediately. This guarantees that `with_static_trust_root()` can `unwrap()`
//! safely at runtime.

#![allow(clippy::pedantic)]

use std::path::Path;

use chrono::Utc;
use x509_parser::prelude::FromDer;

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
            panic!("failed to read embedded trust file {path:?}: {e}");
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
            _ => {}
        }
    }
    if !found_ca {
        panic!("{filename}: CA certificate missing BasicConstraints CA:true");
    }
    if !found_key_cert_sign {
        panic!("{filename}: CA certificate missing KeyUsage keyCertSign");
    }
    println!("cargo:warning=validated CA cert: {filename} (CA:true, keyCertSign)");

    // Verify validity hasn't expired. Build fails if any cert is expired —
    // expired trust roots must be updated at the source before compilation.
    // Uses chrono::Utc::now() per project convention (SystemTime::now is
    // disallowed — may not work correctly in WASM).
    let not_after = tbs.validity.not_after.timestamp();
    let now = Utc::now().timestamp();
    if not_after < now {
        panic!(
            "{filename}: certificate expired at UNIX {not_after} (now: {now}). \
             Update the trust root PEM files from the Sigstore TUF repository."
        );
    }
}

fn validate_public_key(pem_bytes: &[u8], filename: &str) {
    let der = pem_to_der(pem_bytes, filename);
    let (_, spki) = x509_parser::x509::SubjectPublicKeyInfo::from_der(&der)
        .unwrap_or_else(|e| panic!("{filename}: SPKI DER parsing failed: {e}"));

    let algo_oid = &spki.algorithm.algorithm;
    println!(
        "cargo:warning=validated public key: {filename} (algorithm: {algo_oid})"
    );
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

    use base64::Engine;
    base64::engine::general_purpose::STANDARD
        .decode(b64.as_bytes())
        .unwrap_or_else(|e| panic!("{filename}: base64 decode failed: {e}"))
}
