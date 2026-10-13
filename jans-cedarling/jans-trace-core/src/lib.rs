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

mod assertion;
mod canonical;
mod digest;
mod inputs;
mod json;
mod key;
mod limits;
mod signature;
mod verify;

pub use assertion::{
    ContentDigestError, SignError, SigningInputError, assertion_signing_input,
    compute_content_digest, sign_assertion,
};
pub use canonical::{CanonicalizeError, canonicalize_json};
pub use digest::{Digest, ParseDigestError};
pub use inputs::{compute_bundle_hash, token_fingerprint};
pub use json::JsonError;
pub use key::{JwkError, PublicKey, PublicKeyError};
pub use limits::{Limits, SizeLimitError};
pub use signature::{
    Ed25519Signer, ParseSignatureError, PrivateKeyError, SignatureValue, SignerError, TraceSigner,
};
pub use verify::{
    AssertionIdentity, IdentityError, VerificationContext, VerifiedAssertion, VerifyError,
    parse_assertion_identity, verify_assertion,
};
