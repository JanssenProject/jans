# Custom Lints for Cedarling

This directory contains custom Rust lints specifically designed for the Cedarling project to enforce best practices and catch common performance issues.

## Overview

This custom linter package detects inefficient usage patterns in Cedar policy code, specifically:

- **BAD_STRING_CONCATENATION**: Detects `EntityUid::from_str(&format!(...))` patterns which are explicitly warned against in Cedar documentation for performance reasons.

## Usage

### Running custom lints

```bash
# Build the custom lint library
cargo build

# Run the linter in project directory
cargo dylint --all --path custom-lints
```

## Development

### Creating New Custom Lints

1. **Add a new lint declaration** in `src/lib.rs`:

````rust
dylint_linting::declare_late_lint! {
    /// ### What it does
    /// Brief description of what the lint checks for.
    ///
    /// ### Why is this bad?
    /// Explain why this pattern should be avoided.
    ///
    /// ### Example
    /// ```rust
    /// // Bad example
    /// ```
    ///
    /// Use instead:
    ///
    /// ```rust
    /// // Good example
    /// ```
    pub YOUR_LINT_NAME,
    Warn,
    "lint description"
}
````

2. **Implement the lint logic** in either `LateLintPass` or `EarlyLintPass` implementation

3. **Add UI tests** in the `ui/` directory:
    - Create test cases in `ui/<your_lint_name>.rs`
    - Update expected output in `ui/<your_lint_name>.stderr`

4. **Run tests** to verify your implementation:

```bash
cargo test
```

## Resources

Helpful resources for writing lints:

- [Dylint Documentation](https://github.com/trailofbits/dylint#quick-start)
- [Rustc Dev Guide](https://rustc-dev-guide.rust-lang.org/)
- [Adding a new lint](https://github.com/rust-lang/rust-clippy/blob/master/book/src/development/adding_lints.md)
- [Common tools for writing lints](https://github.com/rust-lang/rust-clippy/blob/master/book/src/development/common_tools_writing_lints.md)
- [Crate `rustc_hir`](https://doc.rust-lang.org/nightly/nightly-rustc/rustc_hir/index.html)
- [Struct `rustc_lint::LateContext`](https://doc.rust-lang.org/nightly/nightly-rustc/rustc_lint/struct.LateContext.html)

### Key Concepts

- **Late Lint Pass**: Runs after type checking, allowing analysis of typed expressions
- **AST vs HIR**: Abstract Syntax Tree vs High-Level Intermediate Representation
- **Span**: Location information in source code
- **Diagnostic**: Error/warning messages with suggestions
