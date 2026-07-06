# sigstore-verifier -- implementation plan and architecture

## Implementation plan

### Phase 1: Core verifier (platform-agnostic) -- DONE

- [x] `error.rs` -- `SigstoreVerificationError` enum (11 variants)
- [x] `bundle.rs` -- bundle JSON deserialization types
  - Sigstore protobuf bundle (v0.1-v0.3 JSON)
  - Legacy cosign RekorBundle
  - `MessageSignature` + `DsseEnvelope` content types
- [x] `crypto.rs` -- ECDSA P-256 signature verification via `p256::ecdsa::VerifyingKey`
  - `verify_ecdsa_p256_prehashed` for pre-computed SHA-256 digests (SET, chain, SCT, bundle sig)
  - `verify_ecdsa_p256` for raw message verification (uncommon)
- [x] `cert.rs` -- X.509 cert parsing from DER/PEM via `x509-parser`
  - Trust root certs: BasicConstraints CA:true, KeyUsage keyCertSign
  - Leaf cert: pubkey, SAN, OIDC issuer ext, validity, SCT
  - Leaf cert constraints: CA:false, EKU id-kp-codeSigning
  - TBS DER and signature value extraction for chain validation
- [x] `chain.rs` -- certificate chain validation
  - Path building leaf -> root, timestamp-anchored on `integratedTime`
  - BasicConstraints/KeyUsage/EKU pre-validated by `cert.rs`
  - pathLen constraint checking
  - Each link: SHA-256(TBS) -> ECDSA verify against parent's signature_value
- [x] `sct.rs` -- SCT extraction from x.509 extension + signature verification
  - RFC 6962 section 3.2 DigitallySigned structure
  - PreCert TBS reconstruction (SCT extension removal)
- [x] `tlog.rs` -- Rekor SET verification (RFC 8785) + body consistency (CVE-2022-36056)
  - SET payload: body as base64 STRING (matching Rekor's wire format)
  - Hashedrekord body: cert/signature/artifact hash consistency
  - DSSE body: envelopeHash, payloadHash, signature, verifier cert
- [x] `policy.rs` -- `VerificationPolicy` with auto-anchored regex
- [x] `verifier.rs` -- `SigstoreBlobVerifier` orchestrating the 9-step flow
- [x] `trust_root.rs` -- `SigstoreTrustRootRaw` + PEM-to-DER conversion
  - `with_static_trust_root()` with production keys from Sigstore TUF repo
  - `build.rs` validates all embedded PEM files at compile time

### Phase 2: Tests -- DONE

- [x] Synthetic tests with `rcgen` (`test_support.rs`)
  - 7 cert tests: field extraction, leaf validation, CA recognition, EKU, validity
  - 6 chain tests: valid leaf-to-root, intermediate chain, self-signed rejection, expiry
  - 7 crypto tests: prehash verification, raw messages, wrong key, tampered message
  - 5 tlog tests: SET verification, SET rejection, hashedrekord consistency
  - 3 trust_root tests: static keys parse, root/intermediate are valid CAs
  - 7 policy tests: exact match, regex match, anchored regex, missing issuer
- [x] All assertions use `expect`/`expect_err` with descriptive messages
- [x] Negative tests verify exact error variant via `matches!(err, Variant { .. })`

### Phase 3: Polish (future)

- [ ] Bundle format version negotiation (v0.1, v0.2, v0.3)
- [ ] DSSE / in-toto envelope full support
- [ ] TSA (RFC 3161) timestamp verification
- [ ] Integration tests with real `cosign sign-blob` bundles
- [ ] Full DER-based PreCert reconstruction for SCT verification

---

## Architecture

### Crate structure

```
sigstore-verifier/
├── Cargo.toml                  # Pure Rust, WASM-compatible deps
├── build.rs                    # Compile-time trust root validation
├── docs/
│   └── ARCHITECTURE.md         # This file
├── src/
│   ├── lib.rs                  # Crate root, re-exports, lint config
│   ├── error.rs                # SigstoreVerificationError
│   ├── bundle.rs               # JSON types + format detection
│   ├── crypto.rs               # ECDSA P-256 verify (prehash + raw)
│   ├── cert.rs                 # X.509 parsing + validation
│   ├── chain.rs                # Chain validation
│   ├── sct.rs                  # SCT verification
│   ├── tlog.rs                 # SET + body consistency
│   ├── verifier.rs             # 9-step orchestrator
│   ├── policy.rs               # Identity matching
│   ├── trust_root.rs           # Trust material management
│   ├── test_support.rs         # rcgen-based synthetic cert factory
│   └── trust/
│       ├── fulcio_root.pem            # Fulcio root CA (sigstore.dev, valid until 2031)
│       ├── fulcio_intermediate.pem    # Fulcio intermediate CA
│       ├── rekor.pem                   # Rekor public key (rekor.sigstore.dev)
│       ├── ctfe.pem                   # CTFE key (ctfe.sigstore.dev/2022)
│       └── ctfe_2021.pem              # CTFE key (ctfe.sigstore.dev/test, archived)
└── tests/
    └── test_utils/
        └── synthetic.rs         # rcgen-based cert factory (TBD)
```

### Module dependency diagram

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
+---v--+ +-v--+ +--v---+ +---v---+ +-v--+ +--v---+
|bundle| |crypto| |cert | |chain  | |sct | |tlog  |
+------+ +-----+ +--+---+ +---+---+ +----+ +------+
                    |         |
                    |    +----v----+
                    +----> crypto  |
                         +---------+

All modules depend on error.rs
```

### 9-step verification algorithm

```
+-----------------------------------------------------------+
|                    SigstoreBlobVerifier::verify()           |
+-----------------------------------------------------------+
|                                                            |
|  1. Parse bundle JSON                                      |
|     +- Detect format: Sigstore (has mediaType) vs Legacy   |
|     +- Extract: cert (base64 DER), signature, content type |
|     +- Extract: tlog entry with SET, canonicalizedBody     |
|                                                            |
|  2. Parse X.509 certificate                                |
|     +- From DER -> pubkey (SEC1), SANs, OIDC issuer ext    |
|     +- Validity: not_before, not_after                     |
|     +- SCT extension bytes                                 |
|     +- Constraints: CA:false, EKU codeSigning              |
|     +- Extract TBS DER and signature_value                 |
|                                                            |
|  3. * SET verification *                                   |
|     +- Construct RekorPayload {body, integratedTime,        |
|     |   logIndex, logID}                                   |
|     +- RFC 8785 canonicalize (serde_json_canonicalizer)    |
|     +- ECDSA verify_prehash on SHA-256(canonical)          |
|     +- integratedTime is now TRUSTED                       |
|                                                            |
|  4. Cert chain validation (anchored on integratedTime)     |
|     +- Build path: leaf -> [intermediates] -> trusted root |
|     +- Verify each link: SHA-256(child_tbs) -> ECDSA       |
|     +- Check BasicConstraints CA:true on roots/intermeds   |
|     +- Check pathLen constraints                           |
|     +- Check validity for ALL certs in chain               |
|                                                            |
|  5. SCT verification                                       |
|     +- Parse SCT from cert extension                       |
|     +- Build DigitallySigned TLS structure (RFC 6962 3.2)  |
|     +- ECDSA verify_prehash on SHA-256(digitally_signed)   |
|                                                            |
|  6. Cert validity window                                   |
|     +- not_before <= integratedTime                        |
|     +- integratedTime <= not_after                          |
|                                                            |
|  7. OIDC identity check                                    |
|     +- SAN matches policy.identity (Exact or Regex)        |
|     +- Issuer ext (OID 1.3.6.1.4.1.57264.1.8) matches     |
|                                                            |
|  8. Signature verification                                 |
|     +- MessageSignature: verify_prehash over SHA-256(artf) |
|     +- DSSE: verify_prehash over SHA-256(PAE(type,payload))|
|                                                            |
|  9. Rekor entry consistency (CVE-2022-36056)               |
|     +- Decode canonicalizedBody                            |
|     +- Check: cert in body == bundle cert (DER in PEM)     |
|     +- Check: signature in body == bundle signature        |
|     +- Check: artifact hash in body == SHA-256(artifact)   |
|     +- DSSE: envelopeHash, payloadHash, verifier checks    |
|                                                            |
|  Return VerifiedSignature { san, issuer, verified_at }     |
+-----------------------------------------------------------+
```

### Data flow diagram

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
      |(CA pool)|  |(SET)    |   |(SCT)       |
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

## Reference documentation

### Sigstore Core Specs
- [Sigstore Client Spec](https://github.com/sigstore/architecture-docs/blob/main/client-spec.md) -- section 4 Verification algorithm
- [Rekor Spec V1](https://github.com/sigstore/architecture-docs/blob/main/rekor-spec.md) -- section 9.5 SET, section 6 Entry types
- [Fulcio OID Info](https://github.com/sigstore/fulcio/blob/main/docs/oid-info.md) -- OID 1.3.6.1.4.1.57264.1.8 Issuer V2
- [Cosign Signature Spec](https://github.com/sigstore/cosign/blob/main/specs/SIGNATURE_SPEC.md) -- Bundle format

### Reference Implementations
- [sigstore-rs](https://github.com/sigstore/sigstore-rs) -- Rust reference (not WASM-compatible; uses aws-lc-rs)
- [sigstore-js @sigstore/verify](https://github.com/sigstore/sigstore-js/tree/main/packages/verify/src) -- JS reference
- [sigstore-go](https://pkg.go.dev/github.com/sigstore/sigstore-go/pkg/verify) -- Go reference
- [cosign](https://github.com/sigstore/cosign) -- CLI reference implementation

### SET Verification (Critical Path)
- [RFC 8785](https://datatracker.ietf.org/doc/html/rfc8785) -- JSON Canonicalization Scheme
- [Cosign VerifySET Go impl](https://github.com/sigstore/cosign/blob/main/pkg/cosign/verify.go#L1608) -- Line 1608
- [Cosign SET entry construction](https://github.com/sigstore/cosign/blob/main/pkg/cosign/bundle/rekor.go#L24) -- RekorPayload struct

### Certificate Chain (RFC 5280)
- [RFC 5280 section 6](https://datatracker.ietf.org/doc/html/rfc5280#section-6) -- Certification path validation
- [RFC 6962 section 3.2](https://datatracker.ietf.org/doc/html/rfc6962#section-3.2) -- SCT structure
- [EKU: id-kp-codeSigning](https://oidref.com/1.3.6.1.5.5.7.3.3) -- OID 1.3.6.1.5.5.7.3.3

### Rust Crates Used
- [p256](https://crates.io/crates/p256) -- ECDSA P-256 (RustCrypto, pure Rust)
- [x509-parser](https://crates.io/crates/x509-parser) -- X.509 parsing (zero-copy, pure Rust)
- [serde_json_canonicalizer](https://crates.io/crates/serde_json_canonicalizer) -- RFC 8785 JSON canonicalization
- [regex-lite](https://crates.io/crates/regex-lite) -- Lightweight regex for WASM
- [rcgen](https://crates.io/crates/rcgen) -- Synthetic cert generator for tests

### Security
- [CVE-2022-36056](https://nvd.nist.gov/vuln/detail/CVE-2022-36056) -- Rekor entry inconsistency attack
- [Sigstore Threat Model](https://docs.sigstore.dev/threat-model/)
- [Braun et al. (2013)](https://research.tue.nl/en/publications/how-to-avoid-the-breakdown-of-public-key-infrastructures-forward-) -- Hybrid cert model

---

## Dependency Compatibility

| Operation | sigstore-rs dep | WASM? | Our alternative |
|---|---|---|---|
| SHA-256 | `sha2` | Yes | Same |
| ECDSA P-256 | `p256` + `ecdsa` | Yes | Same |
| Cert chain | `rustls-webpki` + `aws-lc-rs` | No | Custom over `x509-parser` + `p256` |
| X.509 parsing | `x509-cert` | Yes | `x509-parser` |
| JSON canon. | `serde_json_canonicalizer` | Yes | Same |
| Bundle JSON | `serde_json` | Yes | Same |
| TUF trust root | `tough` (native) | No | Caller-provided |
| SCT verify | `aws-lc-rs` | No | Pure Rust via `p256` |
| SET verify | TODO in sigstore-rs | Yes | Pure Rust |

---

## Current status

- **Build:** cargo build passes (native + WASM)
- **Clippy:** cargo clippy clean (0 warnings)
- **Tests:** 37/37 pass
- **Trust root:** Static keys embedded via include_bytes, validated at compile time
- **Prehash fix:** ECDSA verify uses PrehashVerifier::verify_prehash (no double-hash)
- **Integration tests:** Pending
