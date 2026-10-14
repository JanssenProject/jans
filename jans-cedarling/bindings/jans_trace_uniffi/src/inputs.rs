// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::sync::Arc;

use crate::digest::Digest;

/// `bundle_hash` of a policy store: SHA-256 of the `.cjar` exactly as received.
#[uniffi::export]
#[allow(
    clippy::needless_pass_by_value,
    reason = "UniFFI maps `Vec<u8>` to `byte[]`; `&[u8]` would become a `java.nio.ByteBuffer`"
)]
pub fn compute_bundle_hash(archive: Vec<u8>) -> Arc<Digest> {
    Arc::new(Digest(jans_trace_core::inputs::compute_bundle_hash(
        &archive,
    )))
}

/// Identity of a token without `jti`: SHA-256 of the UTF-8 text of the token exactly as
/// received.
#[uniffi::export]
pub fn token_fingerprint(token: &str) -> Arc<Digest> {
    Arc::new(Digest(jans_trace_core::inputs::token_fingerprint(token)))
}
