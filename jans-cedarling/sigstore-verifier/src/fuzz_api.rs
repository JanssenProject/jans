// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Fuzzing-only entry points into otherwise `pub(crate)` parsers.
//!
//! Only compiled behind the `fuzzing` feature, which the out-of-tree
//! `fuzz/` crate (a separate cargo-fuzz project, not a workspace member)
//! enables on its `sigstore-verifier` dependency. Not part of the crate's
//! real public API — see `fuzz/README.md` for how these are used.

/// Fuzz entry point for the X.509 certificate DER parser.
pub fn cert_from_der(der: &[u8]) {
    let _ = crate::cert::Cert::from_der(der);
}

/// Fuzz entry point for Sigstore bundle JSON deserialization.
pub fn bundle_from_json(json: &[u8]) {
    let _ = crate::bundle::Bundle::from_json(json);
}

/// Fuzz entry point for the RFC 6962 SCT list (TLS-encoded) parser.
pub fn sct_parse_list(ext_value: &[u8]) {
    let _ = crate::sct::parse_sct_list(ext_value);
}
