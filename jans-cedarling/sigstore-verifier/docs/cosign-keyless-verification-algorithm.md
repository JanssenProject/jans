# Cosign Keyless Verification — sigstore-verifier reference

Design reference and test checklist for the `sigstore-verifier` crate: an
offline, pure-Rust, WASM-compatible Sigstore/Cosign **blob** verifier
(equivalent to `cosign verify-blob`).

Scope of this crate:

- **Blob verification only** (no OCI container/image signing).
- **Offline only** — no network calls during `verify()`. SET-based, no online
  Merkle inclusion proofs.
- **ECDSA P-256 only** — Fulcio root, leaf certs, Rekor key, CTFE keys all use
  P-256 in production. RSA/Ed25519 out of scope.
- **MessageSignature** is the primary payload (what `cosign sign-blob`
  produces). **DSSE** is phase 2.
- **Trust root is caller-provided** — this crate is not a TUF client. The caller
  embeds keys at compile time (`with_static_trust_root()`) or passes raw bytes
  to `SigstoreBlobVerifier::new()`.

> **Sources:** [Sigstore Client Spec](https://github.com/sigstore/architecture-docs/blob/main/client-spec.md),
> [Cosign Signature Spec](https://github.com/sigstore/cosign/blob/main/specs/SIGNATURE_SPEC.md),
> [Fulcio OIDC Docs](https://github.com/sigstore/fulcio/blob/main/docs/oidc.md),
> [RFC 6962 (SCT)](https://www.rfc-editor.org/rfc/rfc6962), [RFC 8785 (JCS)](https://www.rfc-editor.org/rfc/rfc8785).

---

## How keyless signing works (context)

- Signer authenticates via **OIDC** (GitHub Actions, Google, GitLab, …).
- **Fulcio** (CA) issues a **short-lived X.509 cert** (~10 min) binding the OIDC
  identity to an ephemeral key pair.
- Signature + certificate are recorded in **Rekor** (transparency log), which
  returns a Signed Entry Timestamp (SET) and `integratedTime`.
- All material needed to verify is packaged in a **bundle** distributed with the
  artifact. The verifier never contacts Fulcio or Rekor.

The verifier needs only: the artifact, its bundle, and the trust root
(Fulcio roots + intermediates, Rekor key, CTFE keys).

---

## The bundle

Distributed alongside the artifact (convention: `{artifact}.sigstore.json`).

```json
{
  "mediaType": "application/vnd.dev.sigstore.bundle.v0.3+json",
  "verificationMaterial": {
    "certificate": { "rawBytes": "<base64 DER X.509 leaf cert>" },
    "tlogEntries": [{
      "logIndex": "5179",
      "logId": { "keyId": "<base64 SHA-256 of Rekor pubkey>" },
      "integratedTime": "1624396085",
      "canonicalizedBody": "<base64 Rekor log entry body>",
      "inclusionPromise": { "signedEntryTimestamp": "<base64 SET>" },
      "inclusionProof": { "...": "Merkle proof — online only, we ignore it" }
    }]
  },
  "messageSignature": {
    "messageDigest": { "algorithm": "SHA2_256", "digest": "<base64>" },
    "signature": "<base64 signature over SHA-256(artifact)>"
  }
}
```

### Payload formats

| Format | Signature is over | Status |
|---|---|---|
| **MessageSignature** | `SHA-256(artifact_bytes)` (prehash) | ✓ primary |
| **DSSE** (in-toto) | PAE bytes; statement subject digest compared to artifact | phase 2 |

Supported media types: `bundle+json;version=0.1`, `;version=0.2`,
`bundle.v0.3+json`. Also the legacy cosign `RekorBundle` format.

---

## Verification algorithm (as implemented)

> **Ordering note — do not "fix" to match textbook order.**
> This crate verifies the **SET first** so `integratedTime` is authenticated
> *before* it is used as the trusted timestamp for chain validation and the
> validity-window check. The sigstore-rs 7-step ordering validates the chain
> against a not-yet-authenticated timestamp; ours is deliberately stronger.
> Reordering steps 3↔4 reintroduces that weakness.

```
1. Parse bundle JSON → mediaType, leaf cert, signature, tlog entry, SET
2. Parse X.509 leaf → pubkey, SAN, OIDC issuer ext, validity, SCT, constraints
3. SET verification → verify Rekor's signature over the canonical entry
     (RFC 8785). AUTHENTICATES integratedTime. After this, it is TRUSTED.
4. Cert chain validation → leaf → intermediates → Fulcio root,
     anchored on the verified integratedTime (not wall clock).
5. SCT verification → against CTFE keys (RFC 6962 precert reconstruction).
6. Cert validity window → not_before ≤ integratedTime ≤ not_after.
7. OIDC identity check → SAN match + issuer exact match vs policy.
8. Artifact signature → verify against the leaf pubkey:
     MessageSignature: over SHA-256(artifact)
     DSSE:             over PAE(payloadType, payload)
9. Rekor body consistency → logged cert/sig/hash all match the bundle
     (CVE-2022-36056).
```

### Timestamp anchoring (steps 4 & 6)

Fulcio certs live ~10 min, so they are almost always expired by wall-clock time.
That is expected. Validity is checked against the **verified `integratedTime`**,
which proves the cert was valid *when signing happened*. RFC 5280 §6 path
validation with the timestamp as "current time" (hybrid model).

### SET verification detail (step 3)

Rekor signs the RFC 8785 (JCS) canonicalization of
`{ body: <base64 string>, integratedTime, logIndex, logID: <hex> }`, then
SHA-256, then ECDSA over the Rekor key. `body` is the base64 **string**, not the
decoded JSON object. The SET is an *inclusion promise* — verifiable offline.

### DSSE PAE (step 8, phase 2)

```
PAE(payloadType, payload) = "DSSEv1 " + len(payloadType) + " " + payloadType
                                      + " " + len(payload) + " " + payload
```

For DSSE the crate must also compare the in-toto statement's
`subject[].digest.sha256` to `SHA-256(artifact)` — otherwise the envelope is
proven signed but not bound to *this* artifact. (Not yet implemented.)

---

## Corner-case test matrix

Each row is a required negative test (positive counterpart implied).

### Certificate

| Case | Step | Expect |
|---|---|---|
| `not_after` < integratedTime (expired at signing) | 6 | REJECT |
| `not_before` > integratedTime (not yet valid) | 6 | REJECT |
| Chain incomplete (missing intermediate) | 4 | REJECT |
| Signed by unknown CA / self-signed leaf | 4 | REJECT |
| Leaf marked CA:true | 4 | REJECT |
| Leaf missing code-signing EKU | 4 | REJECT |
| No SCT extension | 5 | REJECT |
| SCT signed by unknown CTFE key | 5 | REJECT |

### Rekor

| Case | Step | Expect |
|---|---|---|
| Missing tlog entry | 3 | REJECT |
| Body doesn't parse | 9 | REJECT |
| Logged cert ≠ bundle cert | 9 | REJECT |
| Logged signature ≠ bundle signature | 9 | REJECT |
| Logged artifact hash ≠ computed hash | 9 | REJECT (CVE-2022-36056) |
| Missing SET | 3 | REJECT |
| SET signature invalid | 3 | REJECT |
| SET signed by unknown Rekor key | 3 | REJECT |
| `integratedTime` = 0 or far-future skew | 6 | REJECT (bound TBD) |

### Signature

| Case | Step | Expect |
|---|---|---|
| Unsupported algorithm (non-P256) | 8 | REJECT (`UnsupportedAlgorithm`) |
| Zero-length signature | 8 | REJECT |
| Signature over wrong artifact | 8 | REJECT |
| DSSE PAE mismatch | 8 | REJECT |
| DSSE statement subject digest ≠ artifact hash | 8 | REJECT |

### Identity / policy

| Case | Step | Expect |
|---|---|---|
| Issuer in cert ≠ policy issuer | 7 | REJECT |
| Regex identity doesn't match SAN | 7 | REJECT |
| Anchored regex: `evil.com` vs `not-evil.com.attacker.io` | 7 | REJECT |
| Empty issuer / empty SAN | 7 | REJECT |
| Multiple SANs, one matches one doesn't | 7 | REJECT (spec-recommended) |

### Bundle format

| Case | Step | Expect |
|---|---|---|
| Unknown mediaType | 1 | REJECT |
| Malformed JSON | 1 | REJECT |
| Both `messageSignature` and `dsseEnvelope` present | 1 | REJECT (ambiguous) |
| Neither present | 1 | REJECT |
| `inclusionProof` present | 9 | Ignore (online-only path, out of scope) |

---

## Trust root

Caller supplies (no TUF client in this crate):

```
fulcio_root_certs         — Fulcio root CA (PEM)          → chain anchor (step 4)
fulcio_intermediate_certs — Fulcio intermediates (PEM)    → chain (step 4)
rekor_keys                — Rekor public key(s) (PEM)     → SET (step 3)
ctfe_keys                 — CTFE public key(s) (PEM)      → SCT (step 5)
```

Multiple entries per field support key rotation — the verifier tries all and
accepts any that validates. `with_static_trust_root()` embeds the public-good
Sigstore keys via `include_bytes!`; `build.rs` validates them at compile time
(CA constraints + non-expiry), so the constructor cannot fail at runtime.

Public-good production values are ECDSA P-256. If Fulcio/Rekor rotate keys, the
embedded PEMs must be refreshed from the Sigstore TUF repo at build time — this
crate does not fetch them.

---

## Usage

```
artifact_bytes + bundle_json
        │
        ▼
SigstoreBlobVerifier::with_static_trust_root()   // or ::new(trust_root_raw)
        │
        ▼
verifier.verify(&artifact_bytes, &bundle_json, &policy)?   // 9 steps, offline
        │
        ├─ Ok(VerifiedSignature { subject_alternative_name, issuer, verified_at })
        │       └─► artifact trusted → proceed
        │
        └─ Err(SigstoreVerificationError) → reject artifact

policy = VerificationPolicy {
    cert_identity: IdentityMatch::Exact | Regex,   // mandatory
    cert_issuer:   "<OIDC issuer>",                // exact, mandatory
}
```

Identity is mandatory — there is no "accept any signer" mode. The caller must
always state whom it trusts.

---

## Security model

| Threat | Mitigation | Step |
|---|---|---|
| Fulcio issues cert for wrong identity | OIDC validated by Fulcio; SCT gives CT-log accountability | 5, 7 |
| Rekor serves tampered entries | SET verified against pre-distributed Rekor key | 3 |
| Bundle reused for a different artifact | Rekor body consistency check | 9 (CVE-2022-36056) |
| Expired short-lived cert presented | Timestamp-anchored path validation (hybrid model) | 4, 6 |
| Cert from wrong OIDC issuer | Issuer extension `1.3.6.1.4.1.57264.1.8` checked | 7 |
| Trust-root key compromise | Trust root distributed via TUF (caller's responsibility) | — |

Trust model: Fulcio and Rekor are trusted parties; the transparency log makes
misbehavior *detectable*. Sigstore defines no revocation mechanism — key
validity is bounded by TUF metadata at distribution time.

---

## Out of scope (this crate)

- **TSA / RFC 3161 timestamping.** Rekor v2 adds a signed TSA timestamp; v1
  `integratedTime` comes from Rekor's clock and is mutable. We trust
  `integratedTime` via the SET (offline). Revisit for Rekor v2.
- **Online verification** (Merkle inclusion proofs, signed tree heads).
- **OCI containers/images.** Blob only.
- **Signing.** Verify only.
- **RSA / Ed25519.** P-256 only.

---

## OIDC issuer cheat sheet

| Issuer | `cert_issuer` |
|---|---|
| GitHub Actions | `https://token.actions.githubusercontent.com` |
| GitHub (user) | `https://github.com/login/oauth` |
| GitLab CI | `https://gitlab.com` |
| Google | `https://accounts.google.com` |

---

## Historical: why not `sigstore-rs`

`sigstore-rs` cannot compile to `wasm32-unknown-unknown` — it depends on
`aws-lc-rs` (C) for cert-chain and SCT, `tough` (native TUF), `webbrowser`
(OAuth), and `oci-client`. This crate is a minimal custom verifier built on pure
RustCrypto (`p256`, `sha2`, `x509-parser`) precisely to reach WASM. sigstore-rs
also left SET verification as a TODO — a production offline verifier must
implement it (step 3 here).
