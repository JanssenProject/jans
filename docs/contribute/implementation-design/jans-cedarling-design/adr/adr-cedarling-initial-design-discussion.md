---
tags:
- design
- ADR
- Architectural Decision Records
- cedarling
---

# Cedarling initial design discussion


## Project Structure

```
src/
├── lib.rs
├── init/
│   ├── mod.rs
│   ├── submodule.rs
├── authz/
│   ├── mod.rs
│   └── another_submodule.rs
├── jwt/
│   ├── mod.rs
│   └── submodule.rs
├── log/
│   ├── mod.rs
│   └── submodule.rs
├── lock/
│    ├── mod.rs
│    └── submodule.rs
├── model/
│   ├── user.rs
│   ├── client.rs
    ├── id_token.rs
│   └── acess_token.rs
├── test/
│    ├── mod.rs
├── services/
│    ├── mod.rs
```

## Best Practices for Documentation

Proper documentation in a large Rust project is crucial to ensure that the codebase is maintainable, understandable, and usable by other developers. Rust provides several tools and conventions that encourage good documentation practices, including rustdoc for generating API documentation from comments and other markdown-based approaches.

### 1. Use Rustdoc for API Documentation

- Document every public item: Use /// comments to generate documentation for all public structs, enums, functions, traits, and modules. These comments are extracted by rustdoc to create comprehensive API documentation.
- Keep documentation up to date: When code changes, update the associated documentation. Outdated documentation can be worse than no documentation.
- Add code examples: Include usage examples in the /// comments for functions, structs, and traits. Rustdoc examples can be automatically tested with cargo test, ensuring they remain valid over time.

```
/// A user struct that represents a user in the system.
///
/// # Examples
///
/// 
/// let user = User::new(1, "Alice".to_string());
/// 
pub struct User {
    pub id: u32,
    pub name: String,
}

impl User {
    /// Creates a new `User` with the given ID and name.
    pub fn new(id: u32, name: String) -> Self {
        User { id, name }
    }
}
```
**Why:** This ensures that documentation is closely tied to the code, easy to generate, and always current with the codebase.

### 2. Module-level Documentation

- **Provide overviews for each module**: In the `mod.rs` or the top of a module file, use `//!` comments to document the purpose of the module, its design, and how it interacts with other parts of the project. This is particularly useful for explaining the higher-level organization and structure of your code.

Example:

```rust
//! This module handles user authentication and authorization.
//! It includes functions for verifying credentials, managing tokens, and
//! enforcing access control policies.

pub mod auth;
pub mod permissions;
```

**Why**: It gives developers a high-level understanding of what the module is responsible for, making it easier to navigate the code.

### 3. Crate-level Documentation

- **Document the crate root**: In the `lib.rs` or `main.rs` file, include crate-level documentation using `//!`. This documentation should provide an overview of the entire project, its goals, its structure, and any important usage notes. For libraries, this is where you document how to use the crate.

Example:

```rust
//! # MyProject
//!
//! `MyProject` is a web application that manages user data securely.
//! It provides modules for user management, authentication, and database access.
//!
//! ## Quick Start
//!
//! Here’s an example of how to get started:
//! 
//! ```
//! let user = my_project::User::new(1, "Alice".to_string());
//! println!("User: {}", user.name);
//! ```
```

**Why**: This helps new users or contributors quickly understand the purpose and usage of the entire crate.

### 4. Consistent Markdown Conventions

- **Use markdown for formatting**: Rust documentation comments support markdown, so you can use it for headings, lists, bold/italic text, and code blocks. Use markdown to structure documentation clearly and consistently.
- **Use code fences**: For code examples, use triple backticks (```) to enclose code snippets. This ensures they are displayed properly when rendered by `rustdoc`.

### 5. Writing Style

- **Be concise but descriptive**: Documentation should explain what the code does, why it exists, and how to use it, but it doesn’t need to repeat what the code itself makes obvious.
- **Avoid technical jargon**: Keep explanations simple and avoid unnecessary jargon. The goal is clarity.
- **Document the *why***: Beyond describing what a function or module does, document the design decisions and why certain choices were made. This is particularly important in complex systems.

### 6. Use `cargo doc` and `cargo test` Regularly

- **Generate docs**: Run `cargo doc --open` to generate and review your project’s documentation. This ensures that your docs are formatted properly and are complete.
- **Test code examples**: Since Rust's doc examples are integrated with the testing framework, run `cargo test` to ensure that all examples in the documentation are accurate and functioning.

### 7. Examples and Tutorials

- **Create an `examples/` directory**: Rust supports the `examples/` directory, where you can include real-world examples that demonstrate how to use the various parts of your project. This is particularly helpful for large projects.
- **Link to examples in the documentation**: When writing the API documentation, link to specific examples for more detailed use cases.

Example project structure:

```
src/
examples/
    basic_usage.rs
    advanced_features.rs
```

**Why**: It allows users to learn by exploring real examples without cluttering the main codebase.

### 8. Documentation for Contributors

- **CONTRIBUTING.md**: Include a `CONTRIBUTING.md` file in the root of your project. This should describe the guidelines for contributing to the project, such as how to set up the development environment, coding conventions, and how to write and run tests.
- **Explain the project’s structure**: For large projects, it’s helpful to include documentation that explains the project’s overall architecture, key modules, and design patterns. This helps new contributors get up to speed quickly.

### 9. Use Doc Comments for Private Functions

- **Private functions and internal modules**: While `rustdoc` only generates documentation for public items, it's a good practice to document private functions and modules using `//` comments. This helps internal developers understand the purpose and usage of private code.

Example:

```rust
// This function handles the low-level logic for parsing user input.
fn parse_user_input(input: &str) -> Result<UserInput, ParseError> {
    // Implementation
}
```

**Why**: Proper internal documentation ensures that team members can work with the code more effectively and minimizes the learning curve for new developers.

### 10. Project-Level README

- **README.md**: Every large project should have a well-written `README.md` file that provides an overview of the project, how to install/use it, and key links (like API documentation, tutorials, etc.). This file should be concise but provide enough information for developers to understand the project’s purpose and start using it.

### 11. Use Rustdoc Lints (`deny(missing_docs)`)

In large Rust projects, it's useful to enforce documentation standards by adding the following at the top of your `lib.rs` or `main.rs`:

```rust
#![deny(missing_docs)]
```

This ensures that all public items are documented, and the compiler will emit warnings if you forget to document something.

## Multiple Crates

- The one thing we may want to split into a different crate is the "lock" functionality.
- A developer who is just using the cedarling for a single application may not care about Lock--which is only needed by an organization who has a few applications.
- Perhaps this would also give the developer the option to ship a smaller cedarling binary

## Use Traits for Abstraction and Extensibility

Trait-based design: Traits allow you to define shared behavior across different types. This is useful for defining abstract interfaces that can be implemented by multiple structs or enums.
Trait objects and generics: For large projects, using traits with generics or trait objects (dyn Trait) can help create flexible and extensible codebases.
**Example:**

```
pub trait Repository {
    fn get(&self, id: u32) -> Option<User>;
    fn save(&self, user: User);
}

pub struct PostgresRepository;

impl Repository for PostgresRepository {
    fn get(&self, id: u32) -> Option<User> {
        // Postgres-specific logic
    }

    fn save(&self, user: User) {
        // Postgres-specific logic
    }
}

pub struct InMemoryRepository;

impl Repository for InMemoryRepository {
    fn get(&self, id: u32) -> Option<User> {
        // In-memory logic
    }

    fn save(&self, user: User) {
        // In-memory logic
    }
}
```
**Why:** Traits allow for decoupling implementation details from high-level logic. This makes your code more extensible and easier to test by enabling dependency injection and mock implementations.

## Clarity in Error Handling

Custom error types: Use custom error types for clear and descriptive error handling. Create a centralized [error.rs](https://error.rs/) file to define common error types across the project.
Result and Option: Leverage Rust’s Result and Option types extensively to handle potential errors and absence of values in a type-safe way.
Error propagation: Use ? for concise error propagation and implement the std::error::Error trait on custom error types.

**Example custom error type:**

```
#[derive(Debug)]
pub enum MyError {
    NotFound,
    DatabaseError(String),
}

impl std::fmt::Display for MyError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            MyError::NotFound => write!(f, "Item not found"),
            MyError::DatabaseError(e) => write!(f, "Database error: {}", e),
        }
    }
}
```

impl std::error::Error for MyError {}

**Why:** Well-defined error types and clear handling make it easier to debug, test, and maintain the project.

## Organize Tests Effectively

**Unit tests:** Place unit tests inside the same module they are testing using a #[cfg(test)] block.
**Integration tests: **Use the root tests/ directory for integration tests that test the interaction between multiple modules or crates.
