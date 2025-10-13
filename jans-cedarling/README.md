# Cedarling

The Cedarling is a performant local authorization service that runs the Rust Cedar Engine.
Cedar policies and schema are loaded at startup from a locally cached "Policy Store".
In simple terms, the Cedarling returns the answer: should the application allow this action on this resource given these JWT tokens.
"Fit for purpose" policies help developers build a better user experience.
For example, why display form fields that a user is not authorized to see?
The Cedarling is a more productive and flexible way to handle authorization.

## Rust Cedarling

Cedarling is written in the Rust programming language (folder `cedarling`). And you can import it into your project as a dependency.

You can install Rust toolchain by following the official [rust installation guide](https://www.rust-lang.org/tools/install).

## Examples of rust Cedarling

Rust examples of using Cedarling contains in the folder `cedarling/examples`.

### Example of initialization logger

File with example is `log_init.rs`.
We support 4 types of loggers according to the [bootstrap properties](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties).

To run example you need execute next command according to the logger:

- `off` - This log type is do nothing. It means that all logs will be ignored.

```bash
cargo run -p cedarling --example log_init -- off
```

- `stdout`- This log type writes all logs to `stdout`. Without storing or additional handling log messages.
  [Standart streams](https://www.gnu.org/software/libc/manual/html_node/Standard-Streams.html).

```bash
cargo run -p cedarling --example log_init -- stdout
```

- `memory` - This log type holds all logs in database (in memory) with eviction policy.

```bash
cargo run -p cedarling --example log_init -- memory 60
```

60 - ttl (time to live log entry) in seconds.
But actually the example execute very fast, so we no need to wait.

- `lock` - This log type will send logs to the server (corporate feature). (in development)

```bash
cargo run -p cedarling --example log_init -- lock
```

### Authorization Evaluation Examples

#### Running Without JWT validation

To evaluate authorization without validating JWT tokens, use the following command:

```bash
cargo run -p cedarling --example authorize_without_jwt_validation
```

#### Running with JWT validation

To include JWT validation in the authorization evaluation, use this command:

```bash
cargo run -p cedarling --example authorize_with_jwt_validation
```

#### Lock Server Integration with SSA JWT

To run the lock server integration example with Software Statement Assertion (SSA) JWT validation:

```bash
cargo run -p cedarling --example lock_integration
```

This example demonstrates how Cedarling integrates with the Lock Server using SSA JWT for secure Dynamic Client Registration (DCR).

## SSA JWT Validation

Cedarling supports Software Statement Assertion (SSA) JWT validation for secure integration with the Lock Server. SSA JWTs provide an additional layer of security by ensuring only properly authorized software can register and obtain access tokens.

### Configuration

To enable SSA JWT validation, configure the following bootstrap properties:

```json
{
  "CEDARLING_LOCK": "enabled",
  "CEDARLING_LOCK_SERVER_CONFIGURATION_URI": "{Lock_Server_URL}/.well-known/lock-server-configuration",
  "CEDARLING_LOCK_SSA_JWT": "your_ssa_jwt_token_here"
}
```

### SSA JWT Structure

The SSA JWT must contain the following claims:

**Claims defined by RFC 7591:**

```json
{
  "software_id": "string",
  "grant_types": ["array"],
  "iss": "string",
  "exp": "number",
  "iat": "number",
  "jti": "string"
}
```

**Additional custom claims required by Cedarling:**

```json
{
  "org_id": "string",
  "software_roles": ["array"]
}
```

**Note:** While RFC 7591 defines `software_id`, `grant_types`, and standard JWT claims (`iss`, `exp`, `iat`, `jti`), the `org_id` and `software_roles` claims are custom requirements specific to the Cedarling implementation and are not part of the RFC 7591 specification.

### Validation Process

1. **Structure Validation**: Verify all required claims are present and have correct types
2. **JWKS Fetching**: Retrieve JSON Web Key Set from the identity provider
3. **Key Resolution**: Find the appropriate key using the JWT's `kid` header
4. **Signature Validation**: Verify the JWT signature using the resolved key
5. **Claims Validation**: Validate expiration and other standard JWT claims

## Unit tests

To run all unit tests in the project you need execute next commands:

```bash
cargo test --workspace
```

## Code coverage

We use `cargo-llvm-cov` for code coverage.

To install run:

```bash
cargo install cargo-llvm-cov
```

Get coverage report in console with:

```bash
cargo llvm-cov
```

Get coverage report in html format with:

```bash
cargo llvm-cov --html --open
```

Getting coverage report in `lcov.info`:

```bash
cargo llvm-cov --workspace --lcov --output-path lcov.info
```

Using `lcov.info` you can configure your IDE to display line-by-line code coverage.

## Autogenerated docs

To generate autogenerated docs run:

```bash
cargo doc -p cedarling --no-deps --open
```

## Python Cedarling

The python bindings for `Cedarling` is located in the `bindings/cedarling_python` folder.

Or you can find readme by clicking [here](bindings/cedarling_python/README.md).

## Configuration

For complete configuration documentation, see [cedarling-properties.md](../docs/cedarling/cedarling-properties.md).

## Benchmarks

Benchmarks have been written with the help of the [`criterion`](https://crates.io/crates/criterion) crate.

You can run the benchmarks using:

```sh
cargo bench -p cedarling
```

After executing benchmarks you can run python script for pretty formatting result:

```sh
python3 scripts/check_benchmarks.py
```

## Profiling

Profiling is done using the [`pprof`](https://crates.io/crates/pprof) crate. We have an example you can run with

```sh
cargo run --example profiling
```

that outputs an SVG named `cedarling_profiling_flamegraph`.
