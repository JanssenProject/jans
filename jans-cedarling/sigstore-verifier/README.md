# sigstore-verifier

Pure Rust, WASM-compatible offline verification of Sigstore/Cosign bundles.
No network calls during `verify()` — all trust material is embedded at compile time
or provided by the caller.

## Quick start

```rust
use sigstore_verifier::{SigstoreBlobVerifier, VerificationPolicy, IdentityMatch};

// Read bundle JSON from a file, HTTP response, etc.
let bundle_json_bytes: Vec<u8> = std::fs::read("my-bundle.sigstore.json")?;

let verifier = SigstoreBlobVerifier::with_static_trust_root();
let result = verifier.verify(
    b"my artifact bytes",
    &bundle_json_bytes,
    &VerificationPolicy {
        cert_identity: IdentityMatch::Exact("https://github.com/example".into()),
        cert_issuer: "https://token.actions.githubusercontent.com".into(),
    },
)?;
println!("Signed by: {} ({})", result.subject_alternative_name, result.issuer);
```

## What it verifies

The 10-step pipeline verifies every link in the Sigstore chain:

1. Parse Sigstore bundle (v0.1–v0.3, MessageSignature + DSSE with in-toto subject binding)
2. Extract X.509 signing certificate
3. Verify the signed entry timestamp (SET) from Rekor transparency log
4. Validate certificate chain against Fulcio trust roots
5. Verify the signed certificate timestamp (SCT) from CTFE
6. Check certificate validity window against `integratedTime`
7. Match OIDC identity (SAN + issuer) against caller policy
8. Verify artifact signature against the leaf certificate's public key
9. Ensure Rekor body consistency (CVE-2022-36056 guard)
10. Verify offline Merkle inclusion proof + signed checkpoint

## Trust roots

Two ways to provide trust material:

```rust
// Embedded at compile time — production Fulcio/Rekor/CTFE keys
let verifier = SigstoreBlobVerifier::with_static_trust_root();

// Custom trust roots provided by the caller
let verifier = SigstoreBlobVerifier::new(SigstoreTrustRootRaw {
    fulcio_roots:        vec![fulcio_root_pem],
    fulcio_intermediate: fulcio_intermediate_pem,
    rekor_keys:          vec![rekor_pem],
    ctfe_keys:           vec![ctfe_pem],
})
.expect("invalid trust material");
```

## WASM

The crate compiles to `wasm32-unknown-unknown` with no special feature flags.
All dependencies are pure Rust — no native C libraries, no `openssl`, no `ring` in the library profile.

## Scope

This crate performs **offline** verification against caller-provided or compile-time
trust material. Not implemented (on purpose):

- TUF / `trusted_root.json` — trust material is PEM
- Rekor v2 / proof-only bundles — SET (`inclusionPromise`) is always required
- RSA / Ed25519 — ECDSA P-256 and P-384 only
- Managed keys (`verificationMaterial.publicKey`) — certificate-based bundles only

For the full verification algorithm and corner-case matrix, see
[`docs/cosign-keyless-verification-algorithm.md`](docs/cosign-keyless-verification-algorithm.md).

For architecture, module layout, and implementation status, see
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## License

Apache-2.0
