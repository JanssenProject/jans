## cedarling ⚙️

### Installation

1. Ensure you have installed [Rust](https://www.rust-lang.org/tools/install) installed.
2. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/cedarling.git
   cd cedarling
   ```
3. Install dependencies:
   ```bash
   cargo build --release
   ```
4. The result of build process will be in `target/release` folder

### Notes

To execute example (`authz_run`)

```
cargo run
```

Path to local policy store:

```
policy-store/local.json
```

Path to input data:

```
cedar_files/input.json
```

The schema for demo was modified and placed in

```
schema/human/cedarling_demo_schema.schema
```

and policy was modified and placed in

```
cedar_files/policies_1.cedar
```

also local policy store was modified according to files above.
