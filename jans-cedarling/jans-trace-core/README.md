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
  takes JSON text or values and returns values; the host owns I/O, time, keys on disk and
  chain state;
- validate the full TRACE schema. It checks only what each function needs (JSON syntax,
  identity members, signature, receipt fields). Lock validates meaning: schema per event
  kind, key registry, producer-chain gaps and equivocation;
- choose sequence numbers or chain heads. Producers and Lock assign them; this crate
  hashes and checks what they supply.

## States

The API is a small set of types, each reached only by parsing or by the previous step.
Holding a value proves its checks passed, so later steps do not repeat them and cannot
fail on them.

```text
text -- UnsignedAssertion::parse --> UnsignedAssertion -- sign --> SignedAssertion
text -- SignedAssertion::parse --> SignedAssertion -- verify --> VerifiedAssertion
bulk body -- parse_batch --> one SignedAssertion (or its error) per record
VerifiedAssertion + ReceiptPosition + ReceivedAt --> ReceiptCommitment --> receipt hash
```

- `UnsignedAssertion`: strict JSON within the limits, an object with non-empty `producer`,
  `kid` and `record_id`, no top-level `signature`, and a signed form that will still fit
  the limits.
- `SignedAssertion`: the same identity rules, plus a top-level `signature` that is unpadded
  base64url of 64 bytes. It keeps the text it was parsed from (or the canonical text
  `sign` produced), its signing input and its `content_digest`. It cannot be serialized
  again, so the stored text is always the received text.
- `VerifiedAssertion`: only `SignedAssertion::verify` creates it. Receipts are built from it.
- `ReceivedAt`: only `YYYY-MM-DDTHH:MM:SS.sssZ` text for a real instant.
  `ReceiptPosition`: sequence 1 with the genesis sentinel, or sequence 2..=2^53−1 with a
  real predecessor. With both, a `ReceiptCommitment` always hashes.
- `ReceiptCommitment`: read-only once built. `for_assertion` takes the ids and digest from a
  `VerifiedAssertion` (ingest). `from_stored` rebuilds one from a `StoredReceipt` read back
  from the database (audit); it is the only way to build one from unverified values.

## Use cases

### Producer: Cedarling signs a decision record

At startup:

1. `signature::Ed25519Signer::from_pkcs8_pem(kid, pem_text)` loads the key that
   `openssl genpkey -algorithm ed25519` writes. The host reads the file; this takes text.
   HSM and KMS keys implement `signature::TraceSigner` instead.
2. `inputs::compute_bundle_hash(cjar_bytes)` on every policy-store (re)load gives
   `bundle_hash`: SHA-256 of the `.cjar` exactly as received.

For every record:

1. Build the record's JSON. Producer chain: `sequence_number` is the previous one plus 1,
   `prev_record_hash` is the previous record's `content_digest`, or `Digest::GENESIS` for
   the first record of a chain. For a token without `jti`, put
   `inputs::token_fingerprint(token_text)` in the record.
2. `assertion::UnsignedAssertion::parse(json_text, &limits)`. Use the limits Lock enforces: a
   record Lock would refuse for its size fails here, before it is signed.
3. `unsigned.sign(&signer)` returns a `SignedAssertion`. Its `as_str()` is the JCS form
   of the whole record, signature included; there is nothing to insert by hand. It fails
   only if the record's `kid` differs from the signer's or the signer fails.
4. Keep `signed.content_digest()` as the next `prev_record_hash`.
5. Send records with `assertion::batch_body(&records, &limits)`: their own text joined into
   the array Lock's bulk endpoint takes, refused if the batch is above Lock's limits. On
   retry, resend the same body; never re-sign.

### Lock: register a producer key

`key::PublicKey::from_jwk(jwk_text)` parses an OKP Ed25519 JWK (RFC 8037). It rejects
other key types, a padded or wrong-length `x`, a JWK with `d` (a private key) and weak
(small-order) keys. `PublicKey::thumbprint()` gives the RFC 7638 thumbprint as
`sha256:<hex>`. Parse once at registration and keep the `PublicKey`.

### Lock: ingest a record

1. `assertion::parse_batch(body_text, &limits)` splits the bulk request body and parses
   each record once; for the single-record endpoint use `SignedAssertion::parse`. Pass the
   body as received and do not parse it with another JSON library first: that library
   would merge duplicate members inside a record before the strict parser sees them, and
   the record's original text would be lost. A body error (not an array, broken JSON, too
   many records, too large) rejects the batch; every record has its own result.
   `identity()` gives `producer`, `kid` and `record_id` to look up the key.
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

Store `signed.as_str()`, the original text. Never re-serialize an assertion from a parsed
tree: canonical text cannot reproduce the original form.

### Lock: startup

`self_test::self_test()` runs known answers for JCS, SHA-256, Ed25519 and the receipt
hash on the machine and binary that actually run. If it fails, disable TRACE ingestion
(fail closed); the error names the broken algorithms.

### Tooling and audit

- Rebuild what a producer signed: `SignedAssertion::parse(..)?.signing_input()`.
- Canonicalize any JSON: `json::canonicalize_json(text, &limits)`.
- Re-verify a receipt chain: rebuild each stored receipt with
  `ReceiptCommitment::from_stored`, filling `StoredReceipt` with `ReceivedAt::from_str`,
  `ReceiptPosition::new`, `Digest::from_str` and the stored ids, then walk the chain from
  `ChainHead::empty(domain)` with `head = head.append(&receipt)?`. The empty head expects
  the genesis receipt, and every step checks the evidence domain, the next sequence and the
  predecessor hash. Compare the final `head.sequence()` and `head.hash()` with the head Lock
  stored, so a cut-off tail is also found. Insertion, deletion or rewriting of an entry
  breaks the walk. To continue later, save `evidence_domain_id()`, `sequence()` and
  `hash()` and rebuild with `ChainHead::resume`; this trusts the saved values, so keep them
  out of reach of whoever operates the audited chain. Without signed checkpoints this does not protect
  against an operator who rewrites the whole chain.

### Calling from other languages

Java: [`jans-trace-java`](../bindings/jans-trace-java/README.md).

Every JSON input is text (`&str`), so invalid UTF-8 cannot reach the core; bindings take a
string, and their toolkit does the UTF-8 conversion. Read an HTTP body as UTF-8 and pass
the text unchanged: do not parse it and serialize it again, and do not normalize Unicode.
A string that holds a lone surrogate (only code can produce one) cannot round-trip through
UTF-8, so such a record fails verification rather than passing silently.

## Example

Every variable is annotated with its type, so each step shows what goes in and what comes
out. In real code the annotations are optional.

```rust
use jans_trace_core::assertion::{
    AssertionIdentity, ParseSignedAssertionError, SignedAssertion, UnsignedAssertion,
    VerificationContext, VerifiedAssertion, batch_body, parse_batch,
};
use jans_trace_core::key::PublicKey;
use jans_trace_core::receipt::{ReceiptCommitment, ReceiptPosition, ReceivedAt};
use jans_trace_core::signature::Ed25519Signer;
use jans_trace_core::{Digest, Limits};

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let limits: Limits = Limits::default();

    // ---- Producer (Cedarling) ----

    // A fixed secret is for examples only; load real keys with `from_pkcs8_pem`.
    let signer: Ed25519Signer = Ed25519Signer::from_secret_bytes("key-2026-01", &[7; 32])?;

    // The core takes the record as JSON text, never as a parsed tree. How the producer
    // builds it does not matter, nor does formatting: the core canonicalizes before signing.
    let record_json: String = serde_json::to_string(&serde_json::json!({
        "producer": "cedarling-fleet-1",
        "kid": "key-2026-01",
        "record_id": "9f3e9e2a-6b0e-4b2c-9f6e-3a2f7b0c9d41",
        "producer_chain": { "sequence_number": 1, "prev_record_hash": Digest::GENESIS.to_string() },
        "trace": { "event_kind": "AUTHORIZATION_DECISION" },
    }))?;

    // Strict parse: identity members present, no `signature` yet, signed size within limits.
    let unsigned: UnsignedAssertion = UnsignedAssertion::parse(&record_json, &limits)?;

    // Sign: the result holds the canonical JSON of the whole record, signature included.
    let signed: SignedAssertion = unsigned.sign(&signer)?;

    // The bulk body Lock receives: the records' own text joined into an array.
    let body: String = batch_body(std::slice::from_ref(&signed), &limits)?;

    // Keep it as the next record's `prev_record_hash`.
    let content_digest: Digest = signed.content_digest();

    // ---- Lock ----

    // Split the request body and parse each record once; the identity selects the key.
    let mut records: Vec<Result<SignedAssertion, ParseSignedAssertionError>> =
        parse_batch(&body, &limits)?;
    let received: SignedAssertion = records.remove(0)?;
    let identity: &AssertionIdentity = received.identity();

    // Normally `PublicKey::from_jwk(registered_jwk_text)` at key registration.
    let public_key: PublicKey = PublicKey::from_bytes(&signer.public_key_bytes())?;

    let context: VerificationContext = VerificationContext {
        expected_producer: identity.producer().to_owned(),
        expected_kid: identity.kid().to_owned(),
        public_key,
    };
    let verified: VerifiedAssertion = received.verify(&context)?;
    assert_eq!(verified.content_digest(), content_digest);

    // Receipt: the first entry of an empty evidence domain links to the genesis sentinel.
    let received_at: ReceivedAt = ReceivedAt::from_unix_millis(1_789_676_132_123)?;
    let commitment: ReceiptCommitment = ReceiptCommitment::for_assertion(
        "domain-1".to_owned(),
        ReceiptPosition::GENESIS,
        received_at,
        &verified,
    );
    let chain_head: Digest = commitment.verify_link(&Digest::GENESIS)?;
    assert_ne!(chain_head, Digest::GENESIS);
    Ok(())
}
```

## API by module

Shared types live at the crate root: `Digest` (`sha256:<hex>`, strict `FromStr`,
`Digest::GENESIS`), `ParseDigestError`, `Limits`, `SizeLimitError`, `ErrorCode`.

| Module | Items | Who calls it |
|---|---|---|
| `assertion` | `UnsignedAssertion` (`parse`, `sign`), `SignedAssertion` (`parse`, `verify`, `as_str`, `content_digest`, `signing_input`), `parse_batch`, `batch_body`, `VerifiedAssertion`, `VerificationContext`, `AssertionIdentity` | producers parse, sign and build bulk bodies; Lock splits bulk bodies, parses and verifies; tooling reads signing input |
| `signature` | `TraceSigner`, `Ed25519Signer` (`from_pkcs8_pem`, `from_secret_bytes`, `public_key_bytes`), `SignatureValue` | producers |
| `key` | `PublicKey` (`from_jwk`, `from_bytes`, `to_bytes`, `thumbprint`) | Lock key registry, tooling |
| `receipt` | `ReceivedAt`, `ReceiptPosition`, `ReceiptCommitment` (`for_assertion`, `from_stored`, field accessors, `receipt_hash`, `verify_link`), `StoredReceipt`, `ChainHead` (`empty`, `resume`, `append`), `RECEIPT_PROFILE`, `ASSERTION_PROFILE` | Lock, auditors |
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
| Bulk body bytes | 8 MiB |
| Records per bulk body | 500 |

Limits are checked while parsing, so a hostile document is never fully built.

## Strict JSON

Rejected everywhere: lone surrogate escapes, a byte-order mark, trailing data, duplicate member
names at any depth, and every number outside ±(2^53−1), however it is written
(`9007199254740992`, `9007199254740993.0`, `1e30`). JCS writes numbers as IEEE-754
doubles, so a larger value could be signed as another one than a reader of the stored text
sees: `9007199254740993.0` would sign as `…992`, while Jackson keeps the integer
`9007199254740993` exact. This is stricter than JCS, which accepts any double; TRACE
records need no larger numbers, and producers that do send them as strings. Inside the
range a fraction is written as its nearest double, as every JSON reader reads it
(`1.00000000000000001` signs as `1`).

## Error codes

Every error has `code()`, which returns an `ErrorCode`; `as_str()` (and `Display`) give the
stable string below, which bindings carry to their exceptions. A release never renames or
reuses a code. `Display` explains the cause and never contains keys,
signatures or record content.

| Code | Meaning |
|---|---|
| `INVALID_JSON` | Not valid JSON (with line and column). |
| `DUPLICATE_MEMBER` | An object repeats a member name. |
| `NON_CANONICALIZABLE_JSON` | Valid JSON outside the profile, such as a number outside ±(2^53−1). |
| `SIZE_LIMIT_EXCEEDED` | Input breaks one of the `Limits`; `Display` names which. |
| `INVALID_ASSERTION_SHAPE` | Not an object, or `producer` / `kid` / `record_id` missing or empty. |
| `MISSING_SIGNATURE` | No top-level `signature`. |
| `SIGNATURE_ALREADY_PRESENT` | `UnsignedAssertion::parse` got a record that is already signed. |
| `INVALID_SIGNATURE_ENCODING` | `signature` is not unpadded base64url of 64 bytes. |
| `SIGNATURE_INVALID` | Ed25519 verification failed. |
| `PRODUCER_MISMATCH` | Signed `producer` differs from the expected one. |
| `KID_MISMATCH` | Signed `kid` differs from the selected key, or the record's `kid` from the signer's. |
| `INVALID_PUBLIC_KEY` | JWK or key bytes do not describe a usable Ed25519 public key. |
| `INVALID_PRIVATE_KEY` | Not an unencrypted PKCS#8 PEM Ed25519 key, or a raw secret that is not 32 bytes. Not in the core design. |
| `INVALID_DIGEST` | Text is not `sha256:` plus 64 lowercase hex digits. |
| `INVALID_RECEIPT` | `received_at` text or sequence and predecessor break the v1 profile, a clock reading is after year 9999, a stored receipt has an empty `producer_id` / `record_id`, or a saved chain head pairs sequence 0 and the sentinel wrongly. |
| `RECEIPT_LINK_MISMATCH` | Receipt does not name the expected predecessor; with `ChainHead::append`, also another evidence domain or a sequence that is not the next one. |
| `SIGNER_FAILURE` | The signer failed; carries no detail, so HSM data never leaks. |
| `SELF_TEST_FAILED` | A known answer differed. Not in the core design. |
| `INVALID_BATCH` | A bulk body is valid JSON but not an array. Not in the core design. |

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
