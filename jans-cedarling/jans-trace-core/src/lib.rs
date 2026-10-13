// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Deterministic TRACE cryptography shared by producers (Cedarling, embedded PEPs) and Lock.
//!
//! One implementation of strict JSON, RFC 8785 canonicalization, Ed25519 signatures and
//! SHA-256 content digests, so a record signed in Rust verifies byte-for-byte anywhere.
//! The crate performs no I/O and holds no state.
//!
//! Each function has its own error type listing only the failures it can produce; every
//! error exposes `code()`, the stable identifier bindings rely on.

#![forbid(unsafe_code)]
// Each error type documents its own variants; `# Errors` sections would repeat them (AGENTS.md
// avoids Python-style doc sections). Same choice as `cedarling/src/lib.rs`.
#![allow(clippy::missing_errors_doc)]

mod canonical;
mod digest;
mod json;
mod limits;
mod signature;

pub use canonical::{CanonicalizeError, canonicalize_json};
pub use digest::{Digest, InvalidDigest};
pub use json::JsonError;
pub use limits::{Limits, SizeLimitExceeded};
pub use signature::{
    Ed25519Signer, InvalidSignatureEncoding, SignatureValue, SignerError, TraceSigner,
};
