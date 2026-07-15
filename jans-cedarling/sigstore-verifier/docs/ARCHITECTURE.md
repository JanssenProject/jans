# sigstore-verifier — architecture and status

Crate structure, module dependencies, and implementation status.

For the verification algorithm and corner-case test matrix, see
[`cosign-keyless-verification-algorithm.md`](./cosign-keyless-verification-algorithm.md).

---

## Crate structure

```
sigstore-verifier/
├── Cargo.toml                  # Pure Rust, WASM-compatible deps
├── build.rs                    # Compile-time trust root validation
├── docs/
│   ├── ARCHITECTURE.md                          # This file
│   └── cosign-keyless-verification-algorithm.md # Algorithm + corner cases
├── src/
│   ├── lib.rs                  # Crate root, re-exports, lint config
│   ├── error.rs                # SigstoreVerificationError (11 variants)
│   ├── bundle.rs               # JSON types + format detection
│   ├── crypto.rs               # ECDSA P-256 verify (prehash + raw)
│   ├── cert.rs                 # X.509 parsing + validation
│   ├── chain.rs                # Chain validation
│   ├── sct.rs                  # SCT verification
│   ├── tlog.rs                 # SET + body consistency
│   ├── verifier.rs             # 9-step orchestrator
│   ├── policy.rs               # Identity matching
│   ├── trust_root.rs           # Trust material management
│   ├── test_support.rs         # rcgen-based synthetic cert factory (cfg(test))
│   └── trust/
│       ├── fulcio_root.pem            # Fulcio root CA (sigstore.dev)
│       ├── fulcio_intermediate.pem    # Fulcio intermediate CA
│       ├── rekor.pem                  # Rekor public key (rekor.sigstore.dev)
│       ├── ctfe.pem                   # CTFE key (ctfe.sigstore.dev/2022)
│       └── ctfe_2021.pem              # CTFE key (ctfe.sigstore.dev, archived)
```

## Module dependency diagram

```
                    +----------+
                    | lib.rs   | (re-exports pub API)
                    +----+-----+
                         |
            +------------+----------------+
            |            |                |
       +----v----+  +----v----+     +-----v------+
       |verifier |  | policy  |     | trust_root |
       +----+----+  +---------+     +------------+
            |
    +-------+-------+--------+--------+--------+
    |       |       |        |        |        |
+---v--+ +-v----+ +-v---+ +--v----+ +-v--+ +--v---+
|bundle| |crypto| |cert | |chain  | |sct | |tlog  |
+------+ +------+ +--+--+ +---+---+ +--+-+ +--+---+
                    |         |        |      |
                    +---------+--------+------+
                              |
                         +----v----+
                         | crypto  |   (cert/chain/sct/tlog verify via crypto)
                         +---------+

All modules depend on error.rs.
```

## Data flow

```
                  +--------------+
                  |  Caller       |
                  +--+-------+---+
                     |       |
          artifact   |       |  bundle JSON
          bytes      |       |  + policy
                     |       |
                +----v-------v----+
                | SigstoreBlob   |
                | Verifier       |
                +-------+--------+
                        |
           +------------+--------------+
           |            |              |
      +----v----+  +----v----+   +-----v------+
      |Fulcio   |  |Rekor    |   |CTFE        |
      |roots    |  |pubkeys  |   |pubkeys     |
      |(chain)  |  |(SET)    |   |(SCT)       |
      +---------+  +---------+   +------------+
           |            |              |
           +------------+--------------+
                        |
                 +------v------+
                 |VerifiedSig  |
                 |{san, issuer,|
                 | verified_at}|
                 +-------------+
```

---

## Implementation status

### Working (implemented + tested)

| Module | Notes |
|---|---|
| `error.rs` | 11-variant error enum |
| `bundle.rs` | Sigstore bundle v0.1–v0.3 + legacy RekorBundle; MessageSignature + DSSE |
| `crypto.rs` | ECDSA P-256 prehash + raw verify (RustCrypto, no RNG) |
| `cert.rs` | X.509 parse via x509-parser; pubkey/SAN/issuer-ext/validity/SCT-bytes/SPKI; CA & leaf constraints |
| `chain.rs` | DN-based path building leaf→…→trusted root (selects issuer from bundle + trust-root intermediate pool); per-link ECDSA (P-256 **and P-384** — Fulcio CAs are P-384/SHA-384, digest+curve per cert); pathLen; timestamp-anchored validity |
| `sct.rs` | RFC 6962 SCT list parse; precert TBS reconstruction (SCT ext removed); `issuer_key_hash` = SHA-256(issuer SPKI); verify vs CTFE keys |
| `tlog.rs` | SET verify (RFC 8785); hashedrekord + DSSE body consistency (CVE-2022-36056); signed-checkpoint verify (note format, keyhint = SHA-256(Rekor SPKI)[..4], ECDSA) |
| `merkle.rs` | Offline RFC 6962 Merkle inclusion-proof verification (Trillian fold) |
| `policy.rs` | Exact + auto-anchored regex SAN; exact issuer |
| `trust_root.rs` | PEM→DER; `with_static_trust_root()`; `build.rs` compile-time validation |
| `verifier.rs` | 10-step orchestrator, SET-first ordering; messageDigest consistency; offline inclusion proof when present |

### Incomplete / stubbed

| Area | Status |
|---|---|
| **DSSE artifact binding** (`verifier.rs`) | PAE signature + tlog envelope/payload-hash checked, but the in-toto statement `subject.digest` is not compared to the artifact hash. Envelope proven signed, not bound to *this* artifact. |
| **Legacy `RekorBundle`** | Not supported by `verify()`: the legacy format carries no certificate, so verification stops at cert extraction. Parse-only. |
| **Algorithm enforcement** | Chain links dispatch on the cert's signatureAlgorithm OID + issuer key size (P-256/P-384), else `UnsupportedAlgorithm`. Leaf artifact signature + SET + SCT are still P-256-only (correct for production, but unrecognised curves there give a key-parse error rather than `UnsupportedAlgorithm`). |
| **Clock-skew / min-time policy** | No bound on `integratedTime` (=0 or far-future accepted). |
| **Multiple-SAN policy** | `.any()` accepts if any SAN matches; spec recommends REJECT on mixed match. |

### Tests

- Unit + e2e: 49 lib tests pass. Synthetic certs/keys via `rcgen` (pure Rust,
  WASM-safe) in `test_support.rs`. Negative tests assert exact error variant.
- **End-to-end** (`verifier.rs::e2e_tests`): drives the public `verify()` over a
  fully-assembled v0.3 bundle — real cert chain + genuinely embedded SCT + Rekor
  SET + hashedrekord tlog + MessageSignature. Positive plus negatives (wrong
  identity, tampered artifact, forged SET). Trust root built via `::new()` from
  generated keys.
- **Real-bundle parity** (`tests/real_bundle.rs`): verifies a genuine
  public-good Sigstore v0.3 bundle (sigstore-conformance `happy-path-v0.3` over
  `a.txt`) against `with_static_trust_root()` — offline. Exercises the real
  Fulcio P-384 chain, a real embedded SCT vs the real CTFE key, a real Rekor SET,
  and a real Merkle inclusion proof + signed checkpoint. Committed negative
  fixtures: corrupted inclusion proof, invalid checkpoint signature, wrong
  checkpoint root hash, messageDigest mismatch — all rejected.
- **Conformance scan** (`tests/conformance_scan.rs`, opt-in via
  `SIGSTORE_CONFORMANCE_DIR`): runs the verifier across the whole
  sigstore-conformance `bundle-verify` corpus. Current in-scope result: all
  hashedrekord/messageSignature positives pass, **all negatives rejected, zero
  false-accepts**. Out-of-scope cases (DSSE/intoto, managed key, custom trusted
  root, Rekor v2 / TSA / checkpoint-cosigning) are skipped.

### Build

- `cargo build` — native + `wasm32-unknown-unknown` pass.
- `cargo clippy` — clean.
- Trust root embedded via `include_bytes!`, validated at compile time.

---

## Roadmap

**Priority (correctness):**

1. ~~Real SCT precert reconstruction + `issuer_key_hash`~~ — **done** (`sct.rs`), unit-tested with synthetic CTFE keys.
2. ~~Generated-chain e2e + real public-good bundle parity~~ — **done** (`e2e_tests`, `tests/real_bundle.rs`). SCT now validated against a real Fulcio cert.
3. ~~Offline Merkle inclusion proof + signed checkpoint~~ — **done** (`merkle.rs`, `tlog::verify_checkpoint`). Validated against the full sigstore-conformance corpus (zero false-accepts).
4. DSSE in-toto subject binding (if DSSE stays in scope).

**Conformance:**

5. ~~Bundle-provided intermediates + DN path building~~ — **done** (`chain.rs`, `verifier.rs`).
6. Clock-skew / min-time policy bound (optional replay guard).

**Later:**

7. TSA (RFC 3161) timestamp verification (Rekor v2).

---

## Dependency compatibility (why custom, not sigstore-rs)

| Operation | sigstore-rs dep | WASM? | Our alternative |
|---|---|---|---|
| SHA-256/384/512 | `sha2` | Yes | Same |
| ECDSA P-256 | `p256` + `ecdsa` | Yes | Same (leaf/Rekor/CTFE + artifact sig) |
| ECDSA P-384 | `p384` | Yes | Chain links (Fulcio root + intermediate) |
| Cert chain | `rustls-webpki` + `aws-lc-rs` | No | Custom over `x509-parser` + `p256` |
| X.509 parsing | `x509-cert` | Yes | `x509-parser` |
| JSON canon. | `serde_json_canonicalizer` | Yes | Same |
| Bundle JSON | `serde_json` | Yes | Same |
| TUF trust root | `tough` (native) | No | Caller-provided (no TUF in crate) |
| SCT verify | `aws-lc-rs` | No | Pure Rust via `p256` |
| SET verify | TODO in sigstore-rs | Yes | Pure Rust |

---

## Crate-specific references

- [p256](https://crates.io/crates/p256) — ECDSA P-256 (RustCrypto, pure Rust)
- [x509-parser](https://crates.io/crates/x509-parser) — X.509 parsing (zero-copy)
- [serde_json_canonicalizer](https://crates.io/crates/serde_json_canonicalizer) — RFC 8785 JCS
- [regex-lite](https://crates.io/crates/regex-lite) — lightweight regex for WASM
- [rcgen](https://crates.io/crates/rcgen) — synthetic cert generator for tests
- [CVE-2022-36056](https://nvd.nist.gov/vuln/detail/CVE-2022-36056) — Rekor entry inconsistency attack

Spec and protocol references live in
[`cosign-keyless-verification-algorithm.md`](./cosign-keyless-verification-algorithm.md).
