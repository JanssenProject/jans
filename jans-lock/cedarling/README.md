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

# Python binding

To build the python binding you need move to the `cedarling_python` folder and follow steps written in `Readme.md`

---

---

# Cedarling ⚙️

## Overview

`cedarling` is a component of the Janssen Project that manages policy evaluation and authorization using the Cedar policy language. This project includes a Rust core and Python bindings for integration.

## Installation

### Prerequisites

- Ensure [Rust](https://www.rust-lang.org/tools/install) is installed on your system.

### Steps

1. **Clone the repository:**

   ```bash
   git clone https://github.com/JanssenProject/jans.git
   cd jans/jans-lock/cedarling/
   ```

2. **Build the project:**
   Install dependencies and build the project in release mode:

   ```bash
   cargo build --release
   ```

3. **Locate the build output:**
   After building, the compiled binaries will be located in the `target/release` folder.

## Usage

To run the example executable (`authz_run`), use:

```bash
cargo run
```

### Configuration Files:

- **Policy Store:** Path to the local policy store:  
  `demo/policy-store/local.json`
- **Input Data:** Path to the input data file:  
  `demo/input.json`

- **Demo Schema:** The schema for the demo (modified version) is located at:  
  `schema/human/cedarling_demo_schema.schema`

- **Policies:** The modified policies are available at:  
  `demo/policies_1.cedar`

> Note: The local policy store is customized according to the schema and policies above.

### Python Binding

To build the Python bindings for `cedarling`, navigate to the `cedarling_python` folder and follow the instructions in its `README.md`.
