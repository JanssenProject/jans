# AGENTS.md - Jans Cedarling Codebase Guidelines

## Build/Lint/Test Commands

**Build:**
- `cargo build --workspace` - Build all workspace members
- `cargo build -p cedarling` - Build main cedarling crate
- `cargo build --release` - Build with optimizations

**Lint:**
- `cargo clippy --workspace` - Run clippy linter
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

**Types:**
- Use typed-builder for complex struct construction
- Leverage serde for serialization/deserialization
- Use smol_str for string optimization where appropriate
- Prefer strong typing over stringly-typed APIs

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
- For error checking, use `assert!(matches!(...), "explicit comment")` instead of `assert!(result.is_err())`
- Use `expect_err("explicit comment")` instead of `panic()`
- Always include explicit comments explaining what error is expected

**File Headers:**
- Each Rust file must contain the Apache 2.0 license header:
```
// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.
```

## Python Bindings Validation

**When working with Python bindings:**
- After making changes to `bindings/cedarling_python`, always validate the `.pyi` files in `bindings/cedarling_python/cedarling_python`
- Check if type hints need adjustment to match Rust interface changes
- **Before running tests, ensure Python virtual environment is activated if available**
- Run Python tests to ensure bindings work correctly: `cd bindings/cedarling_python && python -m pytest` , if it's not installed install by using `pip install pytest`.
- Use `mypy` to validate type annotations: `mypy cedarling_python`, if it's not installed install by using `pip install mypy`.
