// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Sigstore/Cosign offline blob verification library.
//!
//! Pure Rust, WASM-compatible. No network calls during [`SigstoreBlobVerifier::verify`].
//!
//! # Quick start
//!
//! ```rust,no_run
//! use sigstore_verifier::{SigstoreBlobVerifier, VerificationPolicy, IdentityMatch};
//!
//! // Read bundle JSON from a file, HTTP response, etc.
//! let bundle_json_bytes: Vec<u8> = std::fs::read("my-bundle.sigstore.json")?;
//!
//! let verifier = SigstoreBlobVerifier::with_static_trust_root();
//! let result = verifier.verify(
//!     b"my artifact bytes",
//!     &bundle_json_bytes,
//!     &VerificationPolicy {
//!         cert_identity: IdentityMatch::Exact("https://github.com/example".into()),
//!         cert_issuer: "https://token.actions.githubusercontent.com".into(),
//!     },
//! )?;
//! println!("Signed by: {} ({})", result.subject_alternative_name, result.issuer);
//! # Ok::<(), Box<dyn std::error::Error>>(())
//! ```

#![allow(clippy::missing_errors_doc)]
#[cfg(test)]
mod test_support;

pub(crate) mod bundle;
pub(crate) mod cert;
pub(crate) mod chain;
pub(crate) mod crypto;
pub mod error;
pub(crate) mod hex;
pub(crate) mod merkle;
pub mod policy;
pub(crate) mod sct;
pub(crate) mod tlog;
pub mod trust_root;
pub mod verifier;

pub use error::SigstoreVerificationError;
pub use policy::{IdentityMatch, VerificationPolicy};
pub use trust_root::SigstoreTrustRootRaw;
pub use verifier::{SigstoreBlobVerifier, VerifiedSignature};
