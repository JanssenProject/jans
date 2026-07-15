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

use p256::ecdsa::{Signature, SigningKey, VerifyingKey, signature::Signer};
use p256::pkcs8::DecodePrivateKey;
use rcgen::{
    BasicConstraints, CertificateParams, CustomExtension, DnType, ExtendedKeyUsagePurpose, IsCa,
    Issuer, KeyPair, KeyUsagePurpose, PKCS_ECDSA_P256_SHA256, SanType, SerialNumber, date_time_ymd,
};
use sha2::{Digest, Sha256};

/// OID of the Fulcio v2 OIDC issuer extension (1.3.6.1.4.1.57264.1.8).
pub const FULCIO_ISSUER_OID: &[u64] = &[1, 3, 6, 1, 4, 1, 57264, 1, 8];

/// OID of the CT precertificate SCTs extension (1.3.6.1.4.1.11129.2.4.2).
pub const SCT_LIST_OID: &[u64] = &[1, 3, 6, 1, 4, 1, 11129, 2, 4, 2];

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

/// Assemble leaf `CertificateParams` from `opts`, optionally embedding an SCT
/// list extension with the given raw extension-value bytes and a fixed serial.
fn leaf_params(opts: &LeafOpts, sct_ext_value: Option<&[u8]>, serial: Option<u64>) -> CertificateParams {
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
    if let Some(sct) = sct_ext_value {
        params
            .custom_extensions
            .push(CustomExtension::from_oid_content(SCT_LIST_OID, sct.to_vec()));
    }
    if let Some(s) = serial {
        params.serial_number = Some(SerialNumber::from(s));
    }
    params.not_before = date_time_ymd(opts.not_before_ymd.0, opts.not_before_ymd.1, opts.not_before_ymd.2);
    params.not_after = date_time_ymd(opts.not_after_ymd.0, opts.not_after_ymd.1, opts.not_after_ymd.2);
    params
}

/// Build a leaf certificate signed by `issuer` per `opts`.
pub fn make_leaf(issuer: &Ca, opts: &LeafOpts) -> Leaf {
    let key = keypair();
    let params = leaf_params(opts, None, None);
    let der = params.signed_by(&key, &issuer.issuer()).expect("sign leaf").der().to_vec();
    Leaf { der }
}

/// Like [`make_leaf`] but embeds a placeholder SCT list extension
/// (OID 1.3.6.1.4.1.11129.2.4.2). The content is never read — SCT verification
/// removes the whole extension to reconstruct the precertificate TBS, so tests
/// splice the real SCT into the parsed `Cert` afterwards.
pub fn make_leaf_with_sct_placeholder(issuer: &Ca, opts: &LeafOpts) -> Leaf {
    let key = keypair();
    let params = leaf_params(opts, Some(&[0x04, 0x02, 0xDE, 0xAD]), None);
    let der = params.signed_by(&key, &issuer.issuer()).expect("sign leaf").der().to_vec();
    Leaf { der }
}

/// Build a leaf with a *genuinely embedded* valid SCT, signed by `ctfe_sk`.
///
/// Two-pass issuance: the precertificate TBS is independent of the SCT
/// extension's content (removal drops the whole extension), so pass 1 computes
/// the precert (fixed serial + key), signs the RFC 6962 `DigitallySigned`
/// input, and pass 2 re-issues the same cert with the real SCT embedded.
pub fn make_leaf_with_real_sct(
    issuer: &Ca,
    opts: &LeafOpts,
    ctfe_sk: &SigningKey,
    log_id: &[u8; 32],
    timestamp: u64,
) -> (Leaf, SigningKey) {
    let key = keypair();
    // Bridge the rcgen subject key into a p256 SigningKey for artifact signing.
    let leaf_sk = SigningKey::from_pkcs8_der(&key.serialize_der())
        .expect("rcgen P-256 key must load as p256 SigningKey");
    let serial = 0x0102_0304_0506_0708u64;

    // Pass 1: placeholder SCT → recover the precertificate TBS.
    let der1 = leaf_params(opts, Some(&[0x04, 0x02, 0xDE, 0xAD]), Some(serial))
        .signed_by(&key, &issuer.issuer())
        .expect("sign leaf pass 1")
        .der()
        .to_vec();
    let cert1 = crate::cert::Cert::from_der(&der1).expect("parse leaf pass 1");
    let precert = crate::sct::remove_sct_extension(&cert1.tbs_der).expect("precert reconstruct");

    let issuer_cert = crate::cert::Cert::from_der(&issuer.der).expect("parse issuer");
    let issuer_key_hash: [u8; 32] = Sha256::digest(&issuer_cert.spki_der).into();
    let signed_data = digitally_signed_input(0, timestamp, &issuer_key_hash, &precert);
    let sig: Signature = ctfe_sk.sign(&signed_data);

    let sct_body = serialized_sct(0, log_id, timestamp, sig.to_der().as_bytes());
    let ext_value = sct_extension_value(&sct_body);

    // Pass 2: same key + serial + opts, real SCT embedded.
    let der2 = leaf_params(opts, Some(&ext_value), Some(serial))
        .signed_by(&key, &issuer.issuer())
        .expect("sign leaf pass 2")
        .der()
        .to_vec();
    (Leaf { der: der2 }, leaf_sk)
}

/// Mirror of `sct::build_digitally_signed_data` for test SCT construction:
/// `version || 0 || timestamp_be(8) || precert_entry(2) || issuer_key_hash(32)
///  || tbs_len_u24 || tbs || ext_len(2)=0`.
fn digitally_signed_input(
    version: u8,
    timestamp: u64,
    issuer_key_hash: &[u8; 32],
    precert_tbs: &[u8],
) -> Vec<u8> {
    let mut d = Vec::new();
    d.push(version);
    d.push(0); // certificate_timestamp
    d.extend_from_slice(&timestamp.to_be_bytes());
    d.extend_from_slice(&1u16.to_be_bytes()); // precert_entry
    d.extend_from_slice(issuer_key_hash);
    let len = precert_tbs.len();
    d.push((len >> 16) as u8);
    d.push((len >> 8) as u8);
    d.push(len as u8);
    d.extend_from_slice(precert_tbs);
    d.extend_from_slice(&0u16.to_be_bytes()); // no CT extensions
    d
}

/// Encode a P-256 verifying key as a PEM `PUBLIC KEY` (`SubjectPublicKeyInfo`).
pub fn ec_pub_pem(vk: &VerifyingKey) -> String {
    // Fixed SPKI prefix for an uncompressed P-256 point, then `04 || X || Y`.
    const P256_SPKI_PREFIX: &[u8] = &[
        0x30, 0x59, 0x30, 0x13, 0x06, 0x07, 0x2a, 0x86, 0x48, 0xce, 0x3d, 0x02, 0x01, 0x06, 0x08,
        0x2a, 0x86, 0x48, 0xce, 0x3d, 0x03, 0x01, 0x07, 0x03, 0x42, 0x00,
    ];
    let point = vk.to_encoded_point(false);
    let mut der = P256_SPKI_PREFIX.to_vec();
    der.extend_from_slice(point.as_bytes());

    let b64 = base64::Engine::encode(&base64::engine::general_purpose::STANDARD, &der);
    let mut out = String::from("-----BEGIN PUBLIC KEY-----\n");
    for chunk in b64.as_bytes().chunks(64) {
        out.push_str(std::str::from_utf8(chunk).unwrap());
        out.push('\n');
    }
    out.push_str("-----END PUBLIC KEY-----\n");
    out
}

/// Build one RFC 6962 `SerializedSCT` body (no outer length prefix):
/// `version(1) || logID(32) || timestamp_be(8) || ext_len_be(2)=0
///  || {hash=sha256, sig=ecdsa} || sig_len_be(2) || sig`.
pub fn serialized_sct(version: u8, log_id: &[u8; 32], timestamp: u64, sig_der: &[u8]) -> Vec<u8> {
    let mut b = Vec::new();
    b.push(version);
    b.extend_from_slice(log_id);
    b.extend_from_slice(&timestamp.to_be_bytes());
    b.extend_from_slice(&0u16.to_be_bytes()); // no CT extensions
    b.push(4); // hash algorithm: sha256
    b.push(3); // signature algorithm: ecdsa
    b.extend_from_slice(&(sig_der.len() as u16).to_be_bytes());
    b.extend_from_slice(sig_der);
    b
}

/// Wrap a single `SerializedSCT` into the x.509 SCT extension value:
/// `OCTET STRING { SCTList }` where `SCTList = total_len_be(2) || sct_len_be(2) || sct`.
pub fn sct_extension_value(sct_body: &[u8]) -> Vec<u8> {
    let mut list = Vec::new();
    let entry_len = 2 + sct_body.len();
    list.extend_from_slice(&(entry_len as u16).to_be_bytes()); // SCTList total length
    list.extend_from_slice(&(sct_body.len() as u16).to_be_bytes()); // this SCT length
    list.extend_from_slice(sct_body);

    // DER OCTET STRING (short-form length is enough for test sizes).
    let mut out = vec![0x04, list.len() as u8];
    out.extend_from_slice(&list);
    out
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
