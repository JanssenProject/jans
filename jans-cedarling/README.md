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

## Benchmarks

Benchmarks have been written with the help of the [`criterion`](https://crates.io/crates/criterion) crate.

You can run the benchmarks using:

```sh
cargo bench -p cedarling
```

## Profiling

Profiling is done using the [`pprof`](https://crates.io/crates/pprof) crate. We have an example you can run with

```sh
cargo run --example profiling
```

that outputs an SVG named `cedarling_profiling_flamegraph`.
