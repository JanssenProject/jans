# jans-trace-core

The one implementation of TRACE cryptography for Janssen. Cedarling (and other
producers) sign decision records with it; Lock verifies them, fingerprints them and
chains its receipts with it. Because both sides call the same code, a record signed in
Rust verifies byte-for-byte in Lock, with no drift in JSON canonicalization, number
formatting, Unicode handling or base64url details.

Normative design: [`jans-trace-core` MVP design][core]. Producer and Lock behavior:
[Cedarling TRACE MVP][cedarling], [Lock TRACE MVP][lock].

## What it does and does not do

It does:

- strict JSON parsing with size limits, RFC 8785 canonicalization (JCS);
- Ed25519 signatures over `JCS(assertion without its top-level "signature")`;
- `content_digest`, `bundle_hash`, token fingerprints, key thumbprints, all as
  `sha256:<64 lowercase hex>`;
- the Lock receipt-chain hash and its link check;
- a startup self-test.

It does not:

- read files, open sockets, read the clock, use randomness or keep state. Every function
  takes bytes or values and returns values; the host owns I/O, time, keys on disk and
  chain state;
- validate the full TRACE schema. It checks only what each function needs (bytes,
  identity members, signature, receipt fields). Lock validates meaning: schema per event
  kind, key registry, producer-chain gaps and equivocation;
- choose sequence numbers or chain heads. Producers and Lock assign them; this crate
  hashes and checks what they supply.

## Use cases

### Producer: Cedarling signs a decision record

At startup:

1. `signature::Ed25519Signer::from_pkcs8_pem(kid, pem_bytes)` loads the key that
   `openssl genpkey -algorithm ed25519` writes. The host reads the file; this takes bytes.
   HSM and KMS keys implement `signature::TraceSigner` instead.
2. `inputs::compute_bundle_hash(cjar_bytes)` on every policy-store (re)load gives
   `bundle_hash`: SHA-256 of the `.cjar` exactly as received.

For every record:

1. Build the unsigned assertion. Producer chain: `sequence_number` is the previous one
   plus 1, `prev_record_hash` is the previous record's `content_digest`, or
   `Digest::GENESIS` for the first record of a chain. For a token without `jti`, put
   `inputs::token_fingerprint(token_bytes)` in the record.
2. `assertion::sign_assertion(unsigned, &signer)` returns the signature. It fails if the
   record already has a `signature` or its `kid` differs from the signer's.
3. Insert it as the top-level `"signature"` member. The crate never edits your JSON.
4. `assertion::compute_content_digest(signed, &limits)` gives the record's fingerprint;
   keep it as the next `prev_record_hash`.
5. Send the signed bytes. On retry, resend the same bytes; never re-sign.

### Lock: register a producer key

`key::PublicKey::from_jwk(jwk_bytes)` parses an OKP Ed25519 JWK (RFC 8037). It rejects
other key types, a padded or wrong-length `x`, a JWK with `d` (a private key) and weak
(small-order) keys. `PublicKey::thumbprint()` gives the RFC 7638 thumbprint as
`sha256:<hex>`. Parse once at registration and keep the `PublicKey`.

### Lock: ingest a record

1. `assertion::parse_assertion_identity(bytes, &limits)` reads `producer`, `kid` and
   `record_id` from strictly parsed bytes, to look up the key. Do not read them with
   another JSON parser first: strict parsing rejects duplicate members, so after it
   succeeds any reader sees the same values.
2. `assertion::verify_assertion(bytes, &context, &limits)` checks that the signed
   `producer` and `kid` equal the ones used for the key lookup and verifies the
   signature. It returns the identity and `content_digest`.
3. Producer-chain checks are Lock's: compare `prev_record_hash` (parse it with
   `str::parse::<Digest>()`) with the stored `content_digest` of the previous position.
4. Receipt chain, inside the same database transaction that locks the chain head:
   `receipt::format_received_at(unix_millis)` formats Lock's clock reading, then
   `receipt::verify_receipt_link(&head_hash, &commitment)` validates the commitment,
   checks that it names the current head and returns its hash, the new head. The first
   receipt of an evidence domain uses `receipt_sequence = 1` and `Digest::GENESIS`.

Store and return the original bytes. Never re-serialize an assertion from a parsed tree:
canonical bytes cannot reproduce the original form.

### Lock: startup

`self_test::self_test()` runs known answers for JCS, SHA-256, Ed25519 and the receipt
hash on the machine and binary that actually run. If it fails, disable TRACE ingestion
(fail closed); the error names the broken algorithms.

### Tooling and audit

- Rebuild what a producer signed: `assertion::assertion_signing_input(bytes, &limits)`.
- Canonicalize any JSON: `json::canonicalize_json(bytes, &limits)`.
- Re-verify a receipt chain: walk it from `Digest::GENESIS`, calling
  `receipt::verify_receipt_link(&previous_hash, &receipt)` for each entry. Insertion,
  deletion or rewriting of an entry breaks the walk. Without signed checkpoints this does
  not protect against an operator who rewrites the whole chain.

## Example

```rust
use jans_trace_core::assertion::{
    VerificationContext, compute_content_digest, parse_assertion_identity, sign_assertion,
    verify_assertion,
};
use jans_trace_core::key::PublicKey;
use jans_trace_core::receipt::{
    RECEIPT_PROFILE, ReceiptCommitment, format_received_at, verify_receipt_link,
};
use jans_trace_core::signature::Ed25519Signer;
use jans_trace_core::{Digest, Limits};

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let limits = Limits::default();

    // Producer. A fixed secret is for examples only; load real keys with `from_pkcs8_pem`.
    let signer = Ed25519Signer::from_secret_bytes("key-2026-01", &[7; 32]);
    let mut record = serde_json::json!({
        "producer": "cedarling-fleet-1",
        "kid": "key-2026-01",
        "record_id": "9f3e9e2a-6b0e-4b2c-9f6e-3a2f7b0c9d41",
        "producer_chain": { "sequence_number": 1, "prev_record_hash": Digest::GENESIS.to_string() },
        "trace": { "event_kind": "AUTHORIZATION_DECISION" },
    });
    let signature = sign_assertion(&serde_json::to_vec(&record)?, &signer)?;
    record["signature"] = signature.to_string().into();
    let signed = serde_json::to_vec(&record)?;
    let content_digest = compute_content_digest(&signed, &limits)?;

    // Lock. The key normally comes from `PublicKey::from_jwk` at registration.
    let key = PublicKey::from_bytes(&signer.public_key_bytes())?;
    let identity = parse_assertion_identity(&signed, &limits)?;
    let context = VerificationContext {
        expected_producer: &identity.producer,
        expected_kid: &identity.kid,
        public_key: &key,
    };
    let verified = verify_assertion(&signed, &context, &limits)?;
    assert_eq!(verified.content_digest, content_digest);

    let received_at = format_received_at(1_789_676_132_123)?;
    let receipt = ReceiptCommitment {
        receipt_profile: RECEIPT_PROFILE,
        evidence_domain_id: "domain-1",
        receipt_sequence: 1,
        received_at: &received_at,
        producer_id: &verified.producer,
        record_id: &verified.record_id,
        content_digest: &verified.content_digest,
        prev_receipt_hash: &Digest::GENESIS,
    };
    let chain_head = verify_receipt_link(&Digest::GENESIS, &receipt)?;
    assert_ne!(chain_head, Digest::GENESIS);
    Ok(())
}
```

## API by module

Shared types live at the crate root: `Digest` (`sha256:<hex>`, strict `FromStr`,
`Digest::GENESIS`), `ParseDigestError`, `Limits`, `SizeLimitError`.

| Module | Items | Who calls it |
|---|---|---|
| `assertion` | `sign_assertion`, `compute_content_digest`, `assertion_signing_input`, `parse_assertion_identity`, `verify_assertion`, `VerificationContext`, `AssertionIdentity`, `VerifiedAssertion` | producers sign and digest; Lock reads identity and verifies; tooling rebuilds signing input |
| `signature` | `TraceSigner`, `Ed25519Signer` (`from_pkcs8_pem`, `from_secret_bytes`, `public_key_bytes`), `SignatureValue` | producers |
| `key` | `PublicKey` (`from_jwk`, `from_bytes`, `to_bytes`, `thumbprint`) | Lock key registry, tooling |
| `receipt` | `ReceiptCommitment`, `compute_receipt_hash`, `verify_receipt_link`, `format_received_at`, `RECEIPT_PROFILE`, `ASSERTION_PROFILE` | Lock, auditors |
| `inputs` | `compute_bundle_hash`, `token_fingerprint` | producers (PEPs too), Lock later |
| `json` | `canonicalize_json`, `JsonError` | tooling; `JsonError` appears inside other errors |
| `self_test` | `self_test` | Lock at startup |

Each function has its own error type with only the failures it can return, next to the
function in its module.

## Limits

Functions that read untrusted bytes take `&Limits`. `sign_assertion` does not: its input
is the producer's own record. `Limits::default()` holds generous MVP values; the caller
can change each one with `with_max_*`. Keep Lock's own API limits equal or lower.

| Limit | Default |
|---|---|
| Input bytes | 256 KiB |
| Nesting depth | 32 |
| Members per object | 1024 |
| Elements per array | 1024 |
| Bytes per string | 64 KiB |
| Canonical output bytes | 512 KiB |

Limits are checked while parsing, so a hostile document is never fully built.

## Strict JSON

Rejected everywhere: invalid UTF-8, lone surrogate escapes, a byte-order mark, trailing data, duplicate member
names at any depth, and integers outside ±(2^53−1). JCS writes numbers as IEEE-754
doubles, so a larger integer would silently change between signer and verifier.

## Error codes

Every error has `code()`, a stable string that bindings map to exceptions. A release
never renames or reuses a code. `Display` explains the cause and never contains keys,
signatures or record content.

| Code | Meaning |
|---|---|
| `INVALID_JSON` | Not valid JSON (with line and column). |
| `DUPLICATE_MEMBER` | An object repeats a member name. |
| `NON_CANONICALIZABLE_JSON` | Valid JSON that JCS cannot represent exactly, such as a large integer. |
| `SIZE_LIMIT_EXCEEDED` | Input breaks one of the `Limits`; `Display` names which. |
| `INVALID_ASSERTION_SHAPE` | Not an object, or `producer` / `kid` / `record_id` missing or empty. |
| `MISSING_SIGNATURE` | No top-level `signature`. |
| `SIGNATURE_ALREADY_PRESENT` | `sign_assertion` got a record that is already signed. |
| `INVALID_SIGNATURE_ENCODING` | `signature` is not unpadded base64url of 64 bytes. |
| `SIGNATURE_INVALID` | Ed25519 verification failed. |
| `PRODUCER_MISMATCH` | Signed `producer` differs from the expected one. |
| `KID_MISMATCH` | Signed `kid` differs from the selected key (or from the signer's). |
| `INVALID_PUBLIC_KEY` | JWK or key bytes do not describe a usable Ed25519 public key. |
| `INVALID_PRIVATE_KEY` | Not an unencrypted PKCS#8 PEM Ed25519 key. Not in the core design. |
| `INVALID_DIGEST` | Text is not `sha256:` plus 64 lowercase hex digits. |
| `INVALID_RECEIPT` | Receipt breaks the v1 profile (profile, sequence, `received_at`, genesis), or `format_received_at` got a time after year 9999. |
| `RECEIPT_LINK_MISMATCH` | Receipt does not name the expected predecessor. |
| `SIGNER_FAILURE` | The signer failed; carries no detail, so HSM data never leaks. |
| `SELF_TEST_FAILED` | A known answer differed. Not in the core design. |

## Profiles

Outputs are fixed by profile identifiers, not by crate version: no release changes the
output for an existing profile.

```text
TRACE assertion profile: tag:jans.io,2026:trace-v1                (ASSERTION_PROFILE)
Lock receipt profile:    tag:jans.io,2026:lock-trace-receipt-v1   (RECEIPT_PROFILE)
```

[core]: https://github.com/nynymike/lock-server-design/blob/main/jans-trace-core-mvp-design.md
[cedarling]: https://github.com/nynymike/lock-server-design/blob/main/cedarling-trace-design-MVP.md
[lock]: https://github.com/nynymike/lock-server-design/blob/main/Lock-Server-TRACE-MVP-Design.md
