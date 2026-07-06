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
//! ```rust,ignore
//! use sigstore_verifier::{SigstoreBlobVerifier, VerificationPolicy, IdentityMatch};
//!
//! let verifier = SigstoreBlobVerifier::with_static_trust_root();
//! let result = verifier.verify(
//!     b"my artifact bytes",
//!     bundle_json_bytes,
//!     &VerificationPolicy {
//!         cert_identity: IdentityMatch::Exact("https://github.com/example".into()),
//!         cert_issuer: "https://token.actions.githubusercontent.com".into(),
//!     },
//! )?;
//! println!("Signed by: {} ({})", result.subject_alternative_name, result.issuer);
//! # Ok::<(), sigstore_verifier::SigstoreVerificationError>(())
//! ```

// RustCrypto crates use generic-array which triggers this on 64-bit platforms.
// The casts are sound — P-256 keys are always 32-byte arrays.
#![allow(clippy::cast_possible_truncation)]
// The AGENTS.md style guide forbids Python-style doc sections.
#![allow(clippy::missing_errors_doc)]
// We use `&Option<T>` for bundle parsing convenience.
#![allow(clippy::ref_option)]
// The 9-step verify() is inherently long — it's one coherent algorithm.
#![allow(clippy::too_many_lines)]
// Pedantic lints that are antipatterns for this crate:
// format_collect — hex encoding of fixed-size digests is clearer with format!
#![allow(clippy::format_collect)]
// no_effect_underscore_binding — used for SCT field skip in parsing
#![allow(clippy::no_effect_underscore_binding)]
// used_underscore_binding — stub DSSE body verifier that will be completed later
#![allow(clippy::used_underscore_binding)]
// unnecessary_literal_unwrap — custom error construction is intentional
#![allow(clippy::unnecessary_literal_unwrap)]
// needless_pass_by_value — API design consumes trust root for clarity
#![allow(clippy::needless_pass_by_value)]

#[cfg(test)]
mod test_support;

pub mod bundle;
pub mod cert;
pub mod chain;
pub mod crypto;
pub mod error;
pub mod policy;
pub mod sct;
pub mod tlog;
pub mod trust_root;
pub mod verifier;

pub use bundle::{Bundle, TlogEntry};
pub use error::SigstoreVerificationError;
pub use policy::{IdentityMatch, VerificationPolicy};
pub use trust_root::SigstoreTrustRootRaw;
pub use verifier::{SigstoreBlobVerifier, VerifiedSignature};
