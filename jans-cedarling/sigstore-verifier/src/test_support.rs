// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Shared test helpers — synthetic certs/keys via `rcgen` (pure Rust, WASM-safe,
//! no network). Compiled only under `cfg(test)`.
//!
//! These build Fulcio-like CA and leaf certificates with full control over
//! SAN, OIDC-issuer extension, EKU, `BasicConstraints`, and validity window so
//! each verification step can be exercised in isolation.

#![cfg(test)]

use rcgen::{
    BasicConstraints, CertificateParams, CustomExtension, DnType, ExtendedKeyUsagePurpose, IsCa,
    Issuer, KeyPair, KeyUsagePurpose, PKCS_ECDSA_P256_SHA256, SanType, date_time_ymd,
};

/// OID of the Fulcio v2 OIDC issuer extension (1.3.6.1.4.1.57264.1.8).
pub const FULCIO_ISSUER_OID: &[u64] = &[1, 3, 6, 1, 4, 1, 57264, 1, 8];

/// Generate a fresh ECDSA P-256 key pair.
pub fn keypair() -> KeyPair {
    KeyPair::generate_for(&PKCS_ECDSA_P256_SHA256).expect("keygen")
}

/// DER-encode a short `UTF8String` (tag 0x0C) exactly as `cert.rs`'s issuer
/// extension parser expects: `0x0C <len> <bytes>`.
fn der_utf8string(s: &str) -> Vec<u8> {
    let bytes = s.as_bytes();
    assert!(bytes.len() < 128, "test issuer string must be short-form DER");
    let mut v = vec![0x0C, bytes.len() as u8];
    v.extend_from_slice(bytes);
    v
}

/// A synthetic CA (root or intermediate) plus the material needed to sign children.
pub struct Ca {
    pub params: CertificateParams,
    pub key: KeyPair,
    /// Self-signed (root) or issued (intermediate) DER.
    pub der: Vec<u8>,
}

impl Ca {
    /// An [`Issuer`] view usable to sign child certificates.
    pub fn issuer(&self) -> Issuer<'_, &KeyPair> {
        Issuer::from_params(&self.params, &self.key)
    }
}

/// Build a self-signed root CA valid 2020-01-01 .. 2030-01-01.
pub fn make_root(common_name: &str) -> Ca {
    let key = keypair();
    let mut params = CertificateParams::default();
    params.distinguished_name.push(DnType::CommonName, common_name);
    params.is_ca = IsCa::Ca(BasicConstraints::Unconstrained);
    params.key_usages = vec![KeyUsagePurpose::KeyCertSign, KeyUsagePurpose::CrlSign];
    params.not_before = date_time_ymd(2020, 1, 1);
    params.not_after = date_time_ymd(2030, 1, 1);
    let der = params.self_signed(&key).expect("self-sign root").der().to_vec();
    Ca { params, key, der }
}

/// Build an intermediate CA signed by `issuer`, with an optional `pathLen`.
pub fn make_intermediate(common_name: &str, path_len: Option<u8>, issuer: &Ca) -> Ca {
    let key = keypair();
    let mut params = CertificateParams::default();
    params.distinguished_name.push(DnType::CommonName, common_name);
    params.is_ca = IsCa::Ca(match path_len {
        Some(n) => BasicConstraints::Constrained(n),
        None => BasicConstraints::Unconstrained,
    });
    params.key_usages = vec![KeyUsagePurpose::KeyCertSign, KeyUsagePurpose::CrlSign];
    params.not_before = date_time_ymd(2020, 1, 1);
    params.not_after = date_time_ymd(2030, 1, 1);
    let der = params
        .signed_by(&key, &issuer.issuer())
        .expect("sign intermediate")
        .der()
        .to_vec();
    Ca { params, key, der }
}

/// Options for a synthetic leaf certificate.
pub struct LeafOpts<'a> {
    pub san_uri: Option<&'a str>,
    pub oidc_issuer: Option<&'a str>,
    pub code_signing_eku: bool,
    pub is_ca: bool,
    pub not_before_ymd: (i32, u8, u8),
    pub not_after_ymd: (i32, u8, u8),
}

impl Default for LeafOpts<'_> {
    fn default() -> Self {
        Self {
            san_uri: Some("https://github.com/acme/app/.github/workflows/release.yml@refs/tags/v1"),
            oidc_issuer: Some("https://token.actions.githubusercontent.com"),
            code_signing_eku: true,
            is_ca: false,
            not_before_ymd: (2021, 1, 1),
            not_after_ymd: (2025, 1, 1),
        }
    }
}

/// A synthetic leaf certificate.
pub struct Leaf {
    pub der: Vec<u8>,
}

/// Build a leaf certificate signed by `issuer` per `opts`.
pub fn make_leaf(issuer: &Ca, opts: &LeafOpts) -> Leaf {
    let key = keypair();
    let mut params = CertificateParams::default();
    params.distinguished_name.push(DnType::CommonName, "test-leaf");
    if let Some(uri) = opts.san_uri {
        params.subject_alt_names = vec![SanType::URI(uri.try_into().expect("ia5 uri"))];
    }
    params.is_ca = if opts.is_ca {
        IsCa::Ca(BasicConstraints::Unconstrained)
    } else {
        IsCa::ExplicitNoCa
    };
    if opts.is_ca {
        params.key_usages = vec![KeyUsagePurpose::KeyCertSign];
    }
    if opts.code_signing_eku {
        params.extended_key_usages = vec![ExtendedKeyUsagePurpose::CodeSigning];
    }
    if let Some(iss) = opts.oidc_issuer {
        params
            .custom_extensions
            .push(CustomExtension::from_oid_content(FULCIO_ISSUER_OID, der_utf8string(iss)));
    }
    params.not_before = date_time_ymd(opts.not_before_ymd.0, opts.not_before_ymd.1, opts.not_before_ymd.2);
    params.not_after = date_time_ymd(opts.not_after_ymd.0, opts.not_after_ymd.1, opts.not_after_ymd.2);
    let der = params.signed_by(&key, &issuer.issuer()).expect("sign leaf").der().to_vec();
    Leaf { der }
}

/// Wrap DER bytes in a PEM `CERTIFICATE` block (as Rekor stores them).
pub fn der_to_pem(der: &[u8]) -> String {
    let b64 = base64::Engine::encode(&base64::engine::general_purpose::STANDARD, der);
    let mut out = String::from("-----BEGIN CERTIFICATE-----\n");
    for chunk in b64.as_bytes().chunks(64) {
        out.push_str(std::str::from_utf8(chunk).unwrap());
        out.push('\n');
    }
    out.push_str("-----END CERTIFICATE-----\n");
    out
}
