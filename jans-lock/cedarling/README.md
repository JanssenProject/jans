## cedarling ⚙️

### Installation

1. Ensure you have installed [Rust](https://www.rust-lang.org/tools/install) installed.
2. Clone the repository:
   ```bash
   git clone https://github.com/JanssenProject/jans.git
   cd jans/jans-lock/cedarling/
   ```
3. Install dependencies and build:
   ```bash
   cargo build --release
   ```
4. The result of build process will be in `target/release` folder

### Notes

To execute example (`cedarling_run`)

```
cargo run
```

Path to local policy store:

```
demo\policy-store\local.json
```

Path to input data:

```
demo\input.json
```

The schema for demo was modified and placed in

```
schema/human/cedarling_demo_schema.schema
```

and policy was modified and placed in

```
demo\policies_1.cedar
```

also local policy store was modified according to files above.

# Python binding

To build the python binding you need move to the `cedarling_python` folder and follow steps written in `Readme.md`

# Unit tests of rust code

For tests we use standart unit test framework for rust.  
To run tests you need to install [Rust](https://www.rust-lang.org/tools/install). And then execute:
```
cargo test
```

# Code coverage of rust code

To generate code coverage we use `cargo-llvm-cov`. To install it run:
```
cargo install cargo-llvm-cov
```

You can run code coverage by running:
### Simple table
```
cargo llvm-cov > coverage.txt
```
the result will be in `coverage.txt` file.

### HTML results
```
cargo llvm-cov --html --open
```
the result will be opened in browser.

### Generate `lcov.info` file
```
cargo llvm-cov --workspace --lcov --output-path lcov.info
```
the result will be in `lcov.info` file.  
With `lcov.info` you can use IDE tools like [coverage gutters](https://marketplace.visualstudio.com/items?itemName=ryanluker.vscode-coverage-gutters) to watch code coverage.
