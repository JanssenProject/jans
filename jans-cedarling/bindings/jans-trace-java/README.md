# jans-trace-java

Java API of [`jans-trace-core`](../../jans-trace-core/README.md), the one implementation of
TRACE cryptography for Janssen. Lock uses it to parse, verify and fingerprint the records
producers send and to chain its receipts. Producers sign with the same Rust code, so a
record signed by Cedarling verifies byte-for-byte here.

The core README is the reference for what each operation checks: the [states](../../jans-trace-core/README.md#states),
[limits](../../jans-trace-core/README.md#limits), [strict JSON](../../jans-trace-core/README.md#strict-json),
[error codes](../../jans-trace-core/README.md#error-codes) and [profiles](../../jans-trace-core/README.md#profiles).
This page covers what is specific to Java.

## Requirements

- Java 11 or later, on Linux x86-64 or aarch64. The jar carries the native library of the
  platform it was built on.
- `org.jetbrains.kotlin:kotlin-stdlib` and `net.java.dev.jna:jna`, as plain dependencies. The
  versions match `cedarling-java`, so a host that loads both gets one copy of each.

## Build from source

Needs Rust, a JDK 11 or later, Maven and `make`.

```bash
cd jans-cedarling/bindings/jans-trace-java
make               # release build of jans_trace_uniffi, generated Kotlin, native library
mvn clean install  # compiles and tests the Java API, installs the jar
```

`make` writes the generated Kotlin to `src/main/kotlin` and the native library to
`src/main/resources/<platform>`; both are generated, so git ignores them. Run `make` again
after any change on the Rust side. Without it, `mvn` stops with a message that says so.

### Use the local build

`mvn install` produces `target/jans-trace-java-<version>.jar` and installs it into the local
Maven repository (`~/.m2`). `mvn clean package` builds and tests the same jar but leaves it
in `target/` only. The jar holds the Java API, the generated Kotlin and the native library,
built only for the platform of the build machine. It does not hold `kotlin-stdlib` and `jna`:
a project that uses the jar file directly puts those two on its class path itself.

Add the installed jar to a Maven project with:

```xml
<dependency>
    <groupId>io.jans</groupId>
    <artifactId>jans-trace-java</artifactId>
    <version>0.0.0-nightly</version>
</dependency>
```

## Example

A producer signs a record and Lock ingests it, end to end. A test compiles and runs this
example, so it always matches the API.

```java
import io.jans.trace.AssertionIdentity;
import io.jans.trace.Batch;
import io.jans.trace.BatchRecord;
import io.jans.trace.Digest;
import io.jans.trace.Ed25519Signer;
import io.jans.trace.JsonBody;
import io.jans.trace.Limits;
import io.jans.trace.PublicKey;
import io.jans.trace.ReceiptCommitment;
import io.jans.trace.ReceiptPosition;
import io.jans.trace.ReceivedAt;
import io.jans.trace.SelfTest;
import io.jans.trace.SignedAssertion;
import io.jans.trace.TraceException;
import io.jans.trace.UnsignedAssertion;
import io.jans.trace.VerificationContext;
import io.jans.trace.VerifiedAssertion;
import java.util.Collections;

public class TraceExample {

    public static void main(String[] args) throws TraceException {
        // Once at startup. If it throws, keep TRACE ingestion disabled (fail closed).
        SelfTest.run();

        Limits limits = new Limits();

        // ---- Producer ----

        // A fixed secret is for examples only; load real keys with Ed25519Signer.fromPkcs8Pem.
        Ed25519Signer signer = Ed25519Signer.fromSecretBytes("key-2026-01", new byte[32]);

        // The record as JSON text. Formatting does not matter: the record is canonicalized.
        String record = "{"
                + "\"producer\": \"cedarling-fleet-1\","
                + "\"kid\": \"key-2026-01\","
                + "\"record_id\": \"9f3e9e2a-6b0e-4b2c-9f6e-3a2f7b0c9d41\","
                + "\"producer_chain\": {\"sequence_number\": 1, \"prev_record_hash\": \""
                + Digest.genesis() + "\"},"
                + "\"trace\": {\"event_kind\": \"AUTHORIZATION_DECISION\"}"
                + "}";
        UnsignedAssertion unsigned = UnsignedAssertion.parse(new JsonBody(record), limits);
        SignedAssertion signed = unsigned.sign(signer);

        // What the producer sends to Lock's bulk endpoint.
        String body = Batch.body(Collections.singletonList(signed), limits);

        // ---- Lock ----

        // Parsed once at key registration, normally with PublicKey.fromJwk.
        PublicKey registeredKey = PublicKey.fromBytes(signer.publicKeyBytes());

        // The evidence domain's chain head, read inside the transaction that locks it.
        String domain = "domain-1";
        long headSequence = 0;
        Digest headHash = Digest.genesis();

        // A body error (not an array, too large, too many records) throws and rejects the batch.
        for (BatchRecord element : Batch.parse(new JsonBody(body), limits)) {
            try {
                SignedAssertion received = element.get();
                AssertionIdentity identity = received.identity();
                // Look the key up by identity.producer() and identity.kid(); pass the producer
                // and kid it is registered under.
                VerifiedAssertion verified = received.verify(
                        new VerificationContext(identity.producer(), identity.kid(), registeredKey));

                ReceiptPosition position = ReceiptPosition.of(headSequence + 1, headHash);
                ReceivedAt receivedAt = ReceivedAt.fromUnixMillis(System.currentTimeMillis());
                ReceiptCommitment receipt =
                        ReceiptCommitment.forAssertion(domain, position, receivedAt, verified);
                headHash = receipt.verifyLink(headHash);
                headSequence = position.sequence();
                // Store received.text() (never a re-serialized record), the receipt fields,
                // receipt.receiptHash() and the new head.
            } catch (TraceException e) {
                // This record's result; the other records are unaffected.
                System.err.println(e.code().name() + ": " + e.getMessage());
            }
        }
    }
}
```

## Lock, step by step

The order and the reasons are in the core README's [use cases](../../jans-trace-core/README.md#use-cases);
these are the Java calls.

| Step | Call |
|---|---|
| Startup | `SelfTest.run()`. If it throws `SelfTestException`, or the native library fails to load (an `Error` such as `UnsatisfiedLinkError` or `NoClassDefFoundError`), keep TRACE ingestion disabled. |
| Register a producer key | `PublicKey.fromJwk(new JsonBody(jwk))`; `thumbprint()` gives the RFC 7638 thumbprint. Keep the `PublicKey`. |
| Read a request | `new JsonBody(body)` with the body read as UTF-8, unchanged: no JSON library, no Unicode normalization. |
| Split a bulk body | `Batch.parse(body, limits)`: `ParseBatchException` rejects the whole body; each `BatchRecord` holds its record (`get()`) or its own rejection. The single-record endpoint uses `SignedAssertion.parse`. |
| Verify | `identity()` selects the key; `verify(new VerificationContext(producer, kid, key))` returns a `VerifiedAssertion`. |
| Producer chain | Lock's own check; `Digest.parse(prevRecordHash)` reads the stored form strictly. |
| Receipt | `ReceiptPosition.of(headSequence + 1, headHash)` (sequence 1 with `Digest.genesis()` for an empty domain), `ReceivedAt.fromUnixMillis(now)`, `ReceiptCommitment.forAssertion(..)`, then `verifyLink(headHash)` returns the new head. |
| Store | `SignedAssertion.text()`, the record as received. |
| Audit | `StoredReceipt.builder()` with `ReceivedAt.parse`, `ReceiptPosition.of` and `Digest.parse` on the stored values, `ReceiptCommitment.fromStored(..)`, then walk from `ChainHead.empty(domain)` with `head = head.append(receipt)` and compare the final `sequence()` and `hash()` with the stored head. `ChainHead.resume` continues a saved walk. |

## Errors

Every operation declares its own checked exception: `SignedAssertion.parse` throws
`ParseSignedAssertionException`, `verify` throws `VerifyException`, and so on. All of them
extend `TraceException`; catch it to handle every failure in one place.

- Decide by exception type: it tells which operation failed and limits the possible codes.
- `code()` returns the stable code, for the wire and for logs. Lock returns `code().name()`
  (such as `SIGNATURE_INVALID`) verbatim in per-record bulk results. The meaning of each code
  is in the [core table](../../jans-trace-core/README.md#error-codes).
- `getMessage()` explains the cause and never contains keys, signatures or record content.

Misuse is not a `TraceException`: a `null` argument throws `NullPointerException`, a negative
limit (`withMaxInputBytes(-1)`) throws `IllegalArgumentException`, and a `StoredReceipt` built
without every field throws `IllegalStateException`.

## Things to know

- Every type except `StoredReceipt.Builder` is immutable and safe to share between threads.
  Parse a `PublicKey` or build `Limits` once and reuse it.
- Values hold native memory, released when the Java object is garbage collected; there is
  nothing to close.
- Numbers are `long`, since Java has no unsigned types; the core rejects values outside each
  range (a negative sequence, a clock reading before 1970).
- `toString()` never prints secrets or record content: `Ed25519Signer` shows only its `kid`,
  `JsonBody` only its length.
- The API is the `io.jans.trace` package. `io.jans.trace.uniffi` is generated code used by it;
  of that package only `ErrorCode` appears in the API.
- `Profiles.assertionProfile()` and `Profiles.receiptProfile()` give the profile ids this
  library implements.

## Layout

| Path | What |
|---|---|
| [`../jans_trace_uniffi`](../jans_trace_uniffi/README.md) | Rust crate that exports the core through UniFFI. |
| `src/main/kotlin` | Kotlin generated by `make` (git-ignored). |
| `src/main/java/io/jans/trace` | The Java API: one class per core type, one exception per operation. |
| `src/test/java/io/jans/trace` | TestNG tests, with known answers computed outside this code. |
