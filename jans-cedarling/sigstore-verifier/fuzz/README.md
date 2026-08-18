# Fuzzing sigstore-verifier

Coverage-guided fuzzing (via [cargo-fuzz](https://github.com/rust-fuzz/cargo-fuzz) /
libFuzzer) for the crate's hand-rolled parsers: X.509 DER (`cert.rs`), Sigstore
bundle JSON (`bundle.rs`), the RFC 6962 SCT list parser (`sct.rs`), and the full
`verify()` pipeline end to end. All targets only check for **panics** (index
out of bounds, arithmetic overflow, `unwrap`/`expect` on attacker-controlled
data) — the crate is safe Rust, so there's no memory-unsafety to find, but a
malformed Sigstore bundle or certificate is exactly the kind of input an
attacker controls, and any of these functions panicking on it is a real
(denial-of-service-class) bug.

This directory is **not** a member of the parent `jans` Cargo workspace
(`fuzz/Cargo.toml` has its own `[workspace]` table) and is not wired into any
CI job — it's set up for you to run locally, on demand.

## Prerequisites

cargo-fuzz needs a **nightly** toolchain (it instruments the build with
sanitizer coverage, which requires nightly-only rustc flags). Nothing in this
repo pins or auto-switches you to nightly — there's no `rust-toolchain.toml`
in `fuzz/`, on purpose, so your default toolchain is never silently changed.
Every command below passes `+nightly` explicitly instead.

One-time setup:

```sh
rustup toolchain install nightly
cargo install cargo-fuzz
```

## Running a target

From `sigstore-verifier/` (the crate root, not `fuzz/` — cargo-fuzz finds the
`fuzz/` subdirectory automatically):

```sh
cargo +nightly fuzz run cert_from_der
cargo +nightly fuzz run bundle_from_json
cargo +nightly fuzz run sct_parse_list
cargo +nightly fuzz run verify_full
```

Each runs until you stop it (Ctrl-C) or it finds a crash. For a bounded local
run instead of "forever":

```sh
# stop after 5 minutes
cargo +nightly fuzz run bundle_from_json -- -max_total_time=300

# stop after 1,000,000 executions
cargo +nightly fuzz run bundle_from_json -- -runs=1000000
```

By default a run is single-threaded. `cargo fuzz run` has its own `--jobs`
flag (goes *before* the `--`, not after) to spawn that many parallel
libFuzzer worker processes against the same shared corpus — use it to load
every core for a bounded run:

```sh
# all cores, 5 minutes each
cargo +nightly fuzz run cert_from_der --jobs "$(nproc)" -- -max_total_time=300
```

Everything after the bare `--` is passed straight through to libFuzzer — see
`cargo +nightly fuzz run <target> -- -help=1` for the full flag list (corpus
minimization, dictionaries, etc).

## Running everything unattended: `run_fuzz.py`

`fuzz/run_fuzz.py` automates the loop above across all targets: run each one
in 5-minute chunks (all cores), stop a target once its corpus stops growing
(2 consecutive flat chunks by default), then run `cargo fuzz cmin` to shrink
the corpus down to the inputs that actually contribute distinct coverage. By
default there's **no time cap** — a target just keeps going, chunk after
chunk, until it plateaus, however long that takes; pass `--max-seconds` if
you want a hard stop instead. A crash stops that target immediately, skips
minimization for it, and is called out in the summary — see "Reproducing /
fixing a crash" above.

```sh
python3 fuzz/run_fuzz.py                      # all targets, run each to plateau (no cap)
python3 fuzz/run_fuzz.py --targets sct_parse_list
python3 fuzz/run_fuzz.py --max-seconds 3600    # cap each target at 1h
python3 fuzz/run_fuzz.py --chunk-seconds 60 --plateau-chunks 3
python3 fuzz/run_fuzz.py --skip-minimize
```

It only ever shells out to `cargo +nightly fuzz ...` — same as running the
commands above by hand, just looped and time-boxed. No plateau-detection
flag exists in libFuzzer itself; this script's stop condition is a simple
corpus-size diff between chunks, not something cargo-fuzz provides natively.

## What each target exercises

| Target | Entry point | What it fuzzes |
|---|---|---|
| `cert_from_der` | `Cert::from_der` | X.509 DER parsing: TBS, SPKI/curve detection, extensions (SAN, EKU, Fulcio OIDC-issuer, SCT list) |
| `bundle_from_json` | `Bundle::from_json` | Sigstore bundle JSON → struct deserialization + media-type validation |
| `sct_parse_list` | `sct::parse_sct_list` | The hand-rolled RFC 6962 TLS-encoded `SignedCertificateTimestampList` parser — the highest-risk hand-written parser in the crate |
| `verify_full` | `SigstoreBlobVerifier::verify` | The whole 10-step pipeline against a fixed public-good trust root + fixed artifact bytes, with `bundle_json` as the only fuzzed input |

These entry points aren't part of the crate's real public API — they're
`pub(crate)` functions the `fuzz` crate can't normally reach from outside.
`src/fuzz_api.rs` (gated behind the `fuzzing` Cargo feature, which only
`fuzz/Cargo.toml` enables) exposes thin `pub` wrappers around them. A normal
build of the crate (`cargo build`, `cargo test`, the WASM target, etc.) never
enables `fuzzing` and never compiles `fuzz_api.rs`.

## Seed corpus

`corpus/bundle_from_json/` and `corpus/verify_full/` are pre-seeded with
**symlinks** into `../../tests/fixtures/*.sigstore.json` (the crate's existing
test fixtures) — no duplicated file content, and they can't drift out of sync
with the fixtures the unit tests already exercise. Starting a fuzz run from
real, structurally-valid bundles lets the mutator find deep parser/logic
branches far faster than starting from nothing.

`corpus/cert_from_der/` and `corpus/sct_parse_list/` start empty — there's no
existing raw-DER-cert or raw-SCT-extension-value fixture in the repo to link
to. libFuzzer works fine from an empty corpus (it bootstraps its own), but
seeding it helps. To seed `cert_from_der` with a real cert's DER bytes,
convert one of the PEM files under `src/trust/` once:

```sh
openssl x509 -in src/trust/fulcio_intermediate.pem -outform DER \
  -out fuzz/corpus/cert_from_der/fulcio_intermediate.der
```

Whatever the fuzzer finds gets saved back into `corpus/<target>/` automatically
as new coverage is discovered — that directory is the corpus, not just a seed;
it grows over time and is safe to commit if you want to keep the accumulated
coverage.

### Optional: seeding from sigstore-conformance

`tests/conformance_scan.rs` already knows how to run against a checkout of
[`sigstore-conformance`](https://github.com/sigstore/sigstore-conformance)'s
`test/assets/bundle-verify` — the project's own curated set of bundle test
vectors (many more pass/fail cases, across bundle versions, than our 5 local
fixtures). If you already have that checked out for
`SIGSTORE_CONFORMANCE_DIR`, the same directory is a much richer corpus seed
for `bundle_from_json` and `verify_full`:

```sh
find "$SIGSTORE_CONFORMANCE_DIR" -name '*.json' -exec \
  ln -s {} fuzz/corpus/bundle_from_json/ \;
find "$SIGSTORE_CONFORMANCE_DIR" -name '*.json' -exec \
  ln -s {} fuzz/corpus/verify_full/ \;
```

Not checked out by default and not required — the repo-local fixtures above
are enough to get a fuzz run started.

## Reproducing / fixing a crash

A crash writes a minimized-ish reproducer to `fuzz/artifacts/<target>/`.
Replay it directly (no fuzzing, just runs that one input):

```sh
cargo +nightly fuzz run bundle_from_json fuzz/artifacts/bundle_from_json/crash-<hash>
```

To shrink it further:

```sh
cargo +nightly fuzz tmin bundle_from_json fuzz/artifacts/bundle_from_json/crash-<hash>
```

Once fixed, consider adding the minimized input (or a hand-written case
derived from it) as a regular `#[test]` in the crate — fuzz finds it once,
the test suite keeps it fixed.

## Cleaning up

```sh
cargo +nightly fuzz clean       # removes fuzz/target build artifacts
rm -rf fuzz/artifacts/<target>  # discard crash reproducers once fixed/filed
```
