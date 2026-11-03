# jans_trace_uniffi

UniFFI export of [`jans-trace-core`](../../jans-trace-core/README.md) for server-side Java.
It is built as a `cdylib`; `uniffi-bindgen` generates Kotlin from it, and
[`jans-trace-java`](../jans-trace-java/README.md) compiles that Kotlin under a Java API. Java
code uses the Java API, never this crate's generated types directly.

The crate adapts types. Every check, error message and error code comes from the core, except
one check: a negative `Limits` value, which Java can pass and the core's `usize` cannot hold
(`InvalidLimitError`). The tests are the Java ones in `jans-trace-java`; they exercise this
crate through the generated code, the way Lock calls it.

## Build

```bash
cd jans-cedarling/bindings/jans-trace-java
make
```

`make` builds this crate in release mode, generates the Kotlin into
`jans-trace-java/src/main/kotlin` and copies the native library into
`jans-trace-java/src/main/resources`. The Kotlin package is set in [`uniffi.toml`](uniffi.toml).

## Why Kotlin

UniFFI generates Kotlin, Swift, Python and Ruby. The Kotlin runs on any JVM through JNA,
so it serves Java 11, which Lock runs on. The only pure-Java generator,
`uniffi-bindgen-java`, needs Java 22 or later.

## Rules at the boundary

Each rule exists because the obvious Rust signature would be unusable or unsafe from Java.

- **Numbers are `i64`.** UniFFI maps `u64` to Kotlin `ULong`, whose methods Java cannot
  call. The core takes and returns `i64` for sequences and clock readings and checks the
  range itself; `Limits` converts to `usize` here and refuses a negative value
  (`InvalidLimitError`, the only error type this crate defines).
- **Bytes are `Vec<u8>`.** `&[u8]` becomes a `java.nio.ByteBuffer`; `Vec<u8>` becomes `byte[]`.
  A raw secret is wrapped in `Zeroizing` as soon as it arrives.
- **JSON is `JsonBody`,** a record with one string, so JSON text cannot be passed where an id
  or a key is expected.
- **Fallible constructors have names** (`parse`, `of`, `from_jwk`). Kotlin turns a
  constructor named `new` into a primary constructor without `@Throws`, and Java cannot catch
  an undeclared checked exception.
- **Constants are functions** (`Digest::genesis`, `assertion_profile`), because UniFFI exports
  no constants.
- **Errors are objects.** Each operation keeps its own error type, declared in
  [`error_code.rs`](src/error_code.rs), carrying the core's `ErrorCode` and `Display` text.
  `ErrorCode` itself is the core enum, exported with `#[uniffi::remote(Enum)]`; the compiler
  rejects the list there if it misses a core variant. In Kotlin an object error's `message`
  is `null`; its text is `toString()`.
- **`parse_batch` returns `BatchRecord` objects,** because UniFFI has no `Result` type for a
  list element.

## Not exported

`TraceSigner`, the trait for HSM and KMS signers, is post-MVP. Producers sign with
`Ed25519Signer`.
