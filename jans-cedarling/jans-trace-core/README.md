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

## States

The API is a small set of types, each reached only by parsing or by the previous step.
Holding a value proves its checks passed, so later steps do not repeat them and cannot
fail on them.

```text
bytes -- UnsignedAssertion::parse --> UnsignedAssertion -- sign --> SignedAssertion
bytes -- SignedAssertion::parse --> SignedAssertion -- verify --> VerifiedAssertion
VerifiedAssertion + ReceiptPosition + ReceivedAt --> ReceiptCommitment --> receipt hash
```

- `UnsignedAssertion`: strict JSON within the limits, an object with non-empty `producer`,
  `kid` and `record_id`, no top-level `signature`, and a signed form that will still fit
  the limits.
- `SignedAssertion`: the same identity rules, plus a top-level `signature` that is unpadded
  base64url of 64 bytes. It keeps the bytes it was parsed from (or the canonical bytes
  `sign` produced), its signing input and its `content_digest`. It cannot be serialized
  again, so the stored bytes are always the received bytes.
- `VerifiedAssertion`: only `SignedAssertion::verify` creates it. Receipts are built from it.
- `ReceivedAt`: only `YYYY-MM-DDTHH:MM:SS.sssZ` text for a real instant.
  `ReceiptPosition`: sequence 1 with the genesis sentinel, or sequence 2..=2^53−1 with a
  real predecessor. With both, a `ReceiptCommitment` always hashes.

## Use cases

### Producer: Cedarling signs a decision record

At startup:

1. `signature::Ed25519Signer::from_pkcs8_pem(kid, pem_bytes)` loads the key that
   `openssl genpkey -algorithm ed25519` writes. The host reads the file; this takes bytes.
   HSM and KMS keys implement `signature::TraceSigner` instead.
2. `inputs::compute_bundle_hash(cjar_bytes)` on every policy-store (re)load gives
   `bundle_hash`: SHA-256 of the `.cjar` exactly as received.

For every record:

1. Build the record's JSON. Producer chain: `sequence_number` is the previous one plus 1,
   `prev_record_hash` is the previous record's `content_digest`, or `Digest::GENESIS` for
   the first record of a chain. For a token without `jti`, put
   `inputs::token_fingerprint(token_bytes)` in the record.
2. `assertion::UnsignedAssertion::parse(bytes, &limits)`. Use the limits Lock enforces: a
   record Lock would refuse for its size fails here, before it is signed.
3. `unsigned.sign(&signer)` returns a `SignedAssertion`. Its `as_bytes()` is the JCS form
   of the whole record, signature included; there is nothing to insert by hand. It fails
   only if the record's `kid` differs from the signer's or the signer fails.
4. Keep `signed.content_digest()` as the next `prev_record_hash`.
5. Send `signed.as_bytes()`. On retry, resend the same bytes; never re-sign.

### Lock: register a producer key

`key::PublicKey::from_jwk(jwk_bytes)` parses an OKP Ed25519 JWK (RFC 8037). It rejects
other key types, a padded or wrong-length `x`, a JWK with `d` (a private key) and weak
(small-order) keys. `PublicKey::thumbprint()` gives the RFC 7638 thumbprint as
`sha256:<hex>`. Parse once at registration and keep the `PublicKey`.

### Lock: ingest a record

1. `assertion::SignedAssertion::parse(body_bytes, &limits)` parses the request bytes once.
   `identity()` gives `producer`, `kid` and `record_id` to look up the key. Do not read
   them with another JSON parser first: strict parsing rejects duplicate members, so after
   it succeeds any reader sees the same values.
2. `signed.verify(&VerificationContext { expected_producer, expected_kid, public_key })`
   with the registered key and the producer and `kid` it is registered under. It returns
   a `VerifiedAssertion`.
3. Producer-chain checks are Lock's: compare `prev_record_hash` (parse it with
   `str::parse::<Digest>()`) with the stored `content_digest` of the previous position.
4. Receipt chain, inside the database transaction that locks the chain head:
   `receipt::ReceivedAt::from_unix_millis(now)`, `ReceiptPosition::GENESIS` for an empty
   evidence domain or `ReceiptPosition::new(head_sequence + 1, head_hash)`, then
   `ReceiptCommitment::for_assertion(domain, position, received_at, &verified)`.
   `commitment.verify_link(&head_hash)` checks that it names the current head (or
   `Digest::GENESIS`) and returns its hash, the new head.

Store `signed.as_bytes()`, the original bytes. Never re-serialize an assertion from a
parsed tree: canonical bytes cannot reproduce the original form.

### Lock: startup

`self_test::self_test()` runs known answers for JCS, SHA-256, Ed25519 and the receipt
hash on the machine and binary that actually run. If it fails, disable TRACE ingestion
(fail closed); the error names the broken algorithms.

### Tooling and audit

- Rebuild what a producer signed: `SignedAssertion::parse(..)?.signing_input()`.
- Canonicalize any JSON: `json::canonicalize_json(bytes, &limits)`.
- Re-verify a receipt chain: rebuild each stored receipt with `ReceivedAt::from_str`,
  `ReceiptPosition::new` and the stored fields, then walk the chain from
  `Digest::GENESIS` with `commitment.verify_link(&previous_hash)`. Insertion, deletion or
  rewriting of an entry breaks the walk. Without signed checkpoints this does not protect
  against an operator who rewrites the whole chain.

### Calling from other languages

Every input is bytes: UTF-8 JSON without a byte-order mark. Pass the bytes as received;
converting them to a string and back can change them (a platform default charset, a
replaced lone surrogate, Unicode normalization), and the signature would no longer match.

## Example

```rust
use jans_trace_core::Digest;
use jans_trace_core::Limits;
use jans_trace_core::assertion::{SignedAssertion, UnsignedAssertion, VerificationContext};
use jans_trace_core::key::PublicKey;
use jans_trace_core::receipt::{ReceiptCommitment, ReceiptPosition, ReceivedAt};
use jans_trace_core::signature::Ed25519Signer;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let limits = Limits::default();

    // Producer. A fixed secret is for examples only; load real keys with `from_pkcs8_pem`.
    let signer = Ed25519Signer::from_secret_bytes("key-2026-01", &[7; 32]);
    let record = serde_json::json!({
        "producer": "cedarling-fleet-1",
        "kid": "key-2026-01",
        "record_id": "9f3e9e2a-6b0e-4b2c-9f6e-3a2f7b0c9d41",
        "producer_chain": { "sequence_number": 1, "prev_record_hash": Digest::GENESIS.to_string() },
        "trace": { "event_kind": "AUTHORIZATION_DECISION" },
    });
    let signed = UnsignedAssertion::parse(&serde_json::to_vec(&record)?, &limits)?.sign(&signer)?;
    let wire_bytes = signed.as_bytes();

    // Lock. The key normally comes from `PublicKey::from_jwk` at registration.
    let received = SignedAssertion::parse(wire_bytes, &limits)?;
    let verified = received.verify(&VerificationContext {
        expected_producer: received.identity().producer().to_owned(),
        expected_kid: received.identity().kid().to_owned(),
        public_key: PublicKey::from_bytes(&signer.public_key_bytes())?,
    })?;
    assert_eq!(verified.content_digest(), signed.content_digest());

    let commitment = ReceiptCommitment::for_assertion(
        "domain-1".to_owned(),
        ReceiptPosition::GENESIS,
        ReceivedAt::from_unix_millis(1_789_676_132_123)?,
        &verified,
    );
    let chain_head = commitment.verify_link(&Digest::GENESIS)?;
    assert_ne!(chain_head, Digest::GENESIS);
    Ok(())
}
```

## API by module

Shared types live at the crate root: `Digest` (`sha256:<hex>`, strict `FromStr`,
`Digest::GENESIS`), `ParseDigestError`, `Limits`, `SizeLimitError`.

| Module | Items | Who calls it |
|---|---|---|
| `assertion` | `UnsignedAssertion` (`parse`, `sign`), `SignedAssertion` (`parse`, `verify`, `as_bytes`, `content_digest`, `signing_input`), `VerifiedAssertion`, `VerificationContext`, `AssertionIdentity` | producers parse and sign; Lock parses and verifies; tooling reads signing input |
| `signature` | `TraceSigner`, `Ed25519Signer` (`from_pkcs8_pem`, `from_secret_bytes`, `public_key_bytes`), `SignatureValue` | producers |
| `key` | `PublicKey` (`from_jwk`, `from_bytes`, `to_bytes`, `thumbprint`) | Lock key registry, tooling |
| `receipt` | `ReceivedAt`, `ReceiptPosition`, `ReceiptCommitment` (`for_assertion`, `receipt_hash`, `verify_link`), `RECEIPT_PROFILE`, `ASSERTION_PROFILE` | Lock, auditors |
| `inputs` | `compute_bundle_hash`, `token_fingerprint` | producers (PEPs too), Lock later |
| `json` | `canonicalize_json`, `JsonError` | tooling; `JsonError` appears inside other errors |
| `self_test` | `self_test` | Lock at startup |

Each operation has its own error type with only the failures it can return, next to it in
its module.

## Limits

Every parse takes `&Limits`. `UnsignedAssertion::parse` also checks that the signed record
it will produce fits them, so producers find size problems before Lock does. `Limits::default()` holds generous MVP values; the caller
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
| `SIGNATURE_ALREADY_PRESENT` | `UnsignedAssertion::parse` got a record that is already signed. |
| `INVALID_SIGNATURE_ENCODING` | `signature` is not unpadded base64url of 64 bytes. |
| `SIGNATURE_INVALID` | Ed25519 verification failed. |
| `PRODUCER_MISMATCH` | Signed `producer` differs from the expected one. |
| `KID_MISMATCH` | Signed `kid` differs from the selected key, or the record's `kid` from the signer's. |
| `INVALID_PUBLIC_KEY` | JWK or key bytes do not describe a usable Ed25519 public key. |
| `INVALID_PRIVATE_KEY` | Not an unencrypted PKCS#8 PEM Ed25519 key. Not in the core design. |
| `INVALID_DIGEST` | Text is not `sha256:` plus 64 lowercase hex digits. |
| `INVALID_RECEIPT` | `received_at` text or sequence and predecessor break the v1 profile, or a clock reading is after year 9999. |
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
