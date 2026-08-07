#![no_main]

use std::sync::LazyLock;

use libfuzzer_sys::fuzz_target;
use sigstore_verifier::{IdentityMatch, SigstoreBlobVerifier, VerificationPolicy};

// Built once per process: the embedded public-good trust root plus a fixed
// (never-matching) identity policy. Only `bundle_json` varies per input —
// the policy check happens last, after parsing/crypto, so a fixed policy
// still lets the fuzzer reach every earlier verification step.
static VERIFIER: LazyLock<SigstoreBlobVerifier> =
    LazyLock::new(SigstoreBlobVerifier::with_static_trust_root);
static POLICY: LazyLock<VerificationPolicy> = LazyLock::new(|| VerificationPolicy {
    cert_identity: IdentityMatch::Exact("https://example.com/fuzz".into()),
    cert_issuer: "https://example.com/fuzz".into(),
});

fuzz_target!(|data: &[u8]| {
    let _ = VERIFIER.verify(b"fuzz-artifact", data, &POLICY);
});
