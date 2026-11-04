# AGENTS.md - Jans Cedarling Codebase Guidelines

## Build/Lint/Test Commands

**Build:**
- `cargo build --workspace` - Build all workspace members
- `cargo build -p cedarling` - Build main cedarling crate
- `cargo build --release` - Build with optimizations

**Lint:**
- `cargo clippy --workspace` - Run clippy linter
- `cargo clippy -p cedarling --all-targets` - Lint tests and benches too
- `cargo clippy -p cedarling --target wasm32-unknown-unknown` - Lint the WASM build, where native-only code is dead
- `cargo fmt --check` - Check formatting compliance
- See clippy.toml for project-specific lint rules

**Test:**
- `cargo test --workspace` - Run all workspace tests
- `cargo test -p cedarling` - Run main crate tests
- `cargo test -p cedarling --test authorize_unsigned` - Run specific test file
- `cargo test -- --nocapture` - Show test output
- `cargo llvm-cov` - Generate code coverage report

**Benchmarks:**
- `cargo bench -p cedarling` - Run benchmarks
- `python3 scripts/check_benchmarks.py` - Format benchmark results

**Documentation:**
- `cargo doc -p cedarling --no-deps --open` - Generate and open docs

## Code Style Guidelines

**Formatting:**
- Use rustfmt with project's rustfmt.toml settings
- Max line width: 100 characters
- Imports layout: HorizontalVertical with StdExternalCrate grouping
- Use 4-space indentation (no tabs)

**Imports:**
- Group imports: std/external crates first, then internal modules
- Use imports_granularity = "Module"
- Follow existing patterns in codebase

**Naming:**
- Use snake_case for variables, functions, modules
- Use PascalCase for types, traits, enums
- Use SCREAMING_SNAKE_CASE for constants
- Follow Rust naming conventions

**Error Handling:**
- Use thiserror for custom error types
- Prefer Result<T, E> over panics
- Use derive_more for error derivation when needed
- Include context in error messages
- Give each fallible function its own error type with only the variants it can return, so callers match exactly what can happen instead of a shared catch-all enum
- Never use runtime validation that can panic (e.g. `assert!`, `expect`, `unwrap`) for input validation in production code. Use `Result` and propagate errors through the type system instead. The only exceptions are invariants that are statically guaranteed by the type system or explicitly agreed with the user.

**Types:**
- Use typed-builder for complex struct construction
- Leverage serde for serialization/deserialization
- Use smol_str for string optimization where appropriate
- Prefer strong typing over stringly-typed APIs
- Make invalid states unrepresentable: use enums, newtypes, and the type system to encode constraints at compile time. Avoid runtime checks where a more precise type can guarantee correctness statically.
- Replace several related `bool` fields or flags with one enum, so contradictory combinations cannot exist. An enum that names a lifecycle stage ends in `State` (`GenesisState { Pending, Accepted, Lost }`); an enum of plain values does not (`Outcome { Allow, Deny }`)
- Do not use tuples for grouped values in signatures, return types, or test helpers: use a struct with named fields (`BulkRequest { body, records }`, not `(String, Vec<Pending>)`). Only std APIs that require pairs, such as `HashMap::from([(k, v)])`, take a tuple
- Do not add `#[non_exhaustive]` to new public types: a new variant or field should break the bindings' build, so they are updated, rather than fall into a wildcard arm
- Keep derived data with its source: compute it once where the source is built and store it next to it (as `PolicyStoreWithID` holds `bundle_hash`), so the two are replaced together and cannot disagree

**Design Principles:**

These principles are mandatory. Apply them to every new or changed type, function and module, not only to the kind of code an example mentions. Check a design against them before writing it and the result before finishing; when existing code you touch breaks one, say so and fix it or name it as a follow-up. Do not leave a violation in new code silently.

- Single Responsibility: each type or function has one reason to change. If describing it needs "and" (validates and logs, builds and records), split it
- Separation of Concerns: computing a result and acting on it (logging, metrics, I/O, storing outcomes) are separate steps, usually in separate functions
- High cohesion, low coupling: a module depends only on what it uses. A lower layer never imports types from a layer that calls it; it returns its own result and the caller maps it to the caller's types
- Dependency Inversion: depend on a trait, not a concrete service, so the lower layer does not know the implementation and tests can pass a test double
- Law of Demeter (least knowledge): pass a function the values it uses, not the object that holds them; avoid call chains that reach through several objects or modules (`a.b.c.d`)
- Information Expert: the code that has the information does the work. A side effect that needs context (request id, input position, caller-facing name) happens where that context already is, instead of threading the context into a helper that only uses it for the side effect
- Functional core, imperative shell: keep transformations pure (input in, `Result` out) and do side effects at the edge that calls them, so the core is tested without loggers, mocks or I/O
- Encapsulated invariants: when correctness depends on how a caller uses a type (call order, matching an index with its item, calling `finish` once), the type owns that data and enforces it, instead of a documented precondition every caller must keep
- Parse, don't validate (correct by construction): check a condition once where the value is created, and return a type that carries the guarantee, so later code never checks it again
- Hard to misuse: the obvious call is the correct one. If a caller can pass arguments in the wrong order, skip a step or call methods out of sequence, change the API (owning iterator, single entry function, typestate) instead of adding a doc warning
- DRY: a check or rule repeated at every call site moves into the callee. Do not merge code that only looks alike but changes for different reasons

**Visibility:**
- Use the most restrictive visibility level possible: private (no modifier) better than `pub(super)`, `pub(super)` better than `pub(crate)`, `pub(crate)` better than `pub`.
- `pub` items must always be exported in the root `lib.rs` to be truly public.
- Be extremely careful about what you make public; prefer to expose minimal API surface.

**Documentation:**
- Use standard Rust docstrings without Python-style sections
- Document public API items
- Keep documentation concise, focus on "why" not "what"
- Include examples for complex functionality
- **Preferred style:** Use standard Rust docstrings without Python-style sections
- **Good:** `/// Creates a new TokenCache with the specified maximum TTL in seconds. /// If max_ttl is set to 0, tokens will use their natural expiration or default TTL.`
- **Avoid:** Python-style sections like `# Arguments`, `# Returns`
- Rust's documentation generator automatically handles parameter documentation from function signatures
- Only add explanatory comments when necessary
- The function signature already tells us the "what" - focus on the "why"

**Testing:**
- Place integration tests in src/tests/ directory
- Use test_utils crate for shared test helpers
- Follow existing test patterns in authorize_*.rs files
- Include both positive and negative test cases
- For success checking, use `result.expect("descriptive message")` instead of `assert!(result.is_ok())`
- For error checking, use `result.expect_err("descriptive message")` instead of `assert!(result.is_err())`
- For pattern matching errors, use `assert!(matches!(...), "descriptive message")`
- All assertions must include a descriptive message explaining what is being tested
- Use `expect_err("explicit comment")` instead of `panic()` when expecting errors
- A negative test asserts the specific error variant (and its key fields), not just that some error occurred
- Before relying on a new test, check that it fails without the change it covers

**Async and Performance:**
- Blocking I/O and CPU-heavy work (signing, hashing large inputs) inside async code run through `tokio::task::spawn_blocking`, never directly on the runtime
- Where `spawn_blocking` is not available (WASM runs on one thread) and the work must stay on the runtime, split it into bounded steps and yield between them (for example `tokio::task::yield_now().await` every N items), so authorization and other tasks keep running while it completes
- Data read by many items, records, or retries is shared, not copied: `Arc<T>`, `Arc<[T]>`, or `bytes::Bytes`, created once and only when the feature that needs it is enabled
- Do not build Cedar identifiers by parsing formatted strings such as `EntityUid::from_str(&format!(...))`: the Cedar documentation warns against this for performance reasons. Use `EntityUid::from_type_name_and_id` instead
- The authorization path never waits on side work such as logging or telemetry export: hand it off without blocking (for example `try_send`) and report what was dropped

**File Headers:**
- Each Rust file must contain the Apache 2.0 license header:
```
// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.
```

## Default Config Sample

- `cedarling/config/default_config.yaml` is a user-facing sample. Keep it consistent with the real defaults in code (e.g. `JwtConfig::default`, `BootstrapConfigRaw` defaults) and with `docs/cedarling/reference/cedarling-properties.md`.
- When changing any default value, default behavior, or adding/removing a `CEDARLING_*` bootstrap property: update this YAML in the same change so the sample never contradicts the documented or compiled-in defaults.

## Python Bindings Validation

**When working with Python bindings:**
- After making changes to `bindings/cedarling_python`, always validate the `.pyi` files in `bindings/cedarling_python/cedarling_python`
- Check if type hints need adjustment to match Rust interface changes
- **Before running tests, ensure Python virtual environment is activated if available**
- To build updated python bindings execute `cd bindings/cedarling_python && maturin develop` if you have error during execution read readme file `bindings/cedarling_python/README.md`
- Run Python tests to ensure bindings work correctly: `cd bindings/cedarling_python && python -m pytest` , if it's not installed install by using `pip install pytest`.
- Use `mypy` to validate type annotations: `mypy cedarling_python`, if it's not installed install by using `pip install mypy`.
