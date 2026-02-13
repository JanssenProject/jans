# cedarling_python 🐍

This project uses `maturin` to create a Python library from Rust code. Follow the steps below to install and build the library.

## Prerequisites

1. (Optional) Install build tools (for Linux users)

   Install the `build-essential` package or its equivalent packages using your distribution's package manager.

   Ubuntu/Debian:

   ```
   sudo apt install build-essential
   ```

   Arch Linux:

   ```
   sudo pacman -S base-devel
   ```

   RHEL/CentOS/Fedora:

   ```
   sudo yum install gcc gcc-c++ make openssl-dev
   ```

1. Ensure Rust is installed:
   Verify Rust installation by running:

   ```
   cargo --version
   ```

   If Rust is not installed, you can install it from [here](https://www.rust-lang.org/tools/install).

1. Set up a virtual environment:

   - Install `venv` for your platform by following the [instructions here](https://virtualenv.pypa.io/en/latest/installation.html).
   - Create a virtual environment:

     ```bash
     python3 -m venv venv
     ```

1. Activate the virtual environment:
   Follow the [instructions here](https://packaging.python.org/guides/installing-using-pip-and-virtual-environments/#activate-a-virtual-environment) to activate the virtual environment.

   Example for Linux/macOS:

   ```bash
   source venv/bin/activate
   ```

   Example for Windows:

   ```bash
   .\venv\Scripts\activate
   ```

1. Install `maturin`

   ```
   pip install maturin
   ```

   For Linux, install patchelf dependency:

   ```
   pip install maturin[patchelf]
   ```

1. Clone the repository:

   ```
   git clone https://github.com/JanssenProject/jans.git
   ```

1. Navigate to the `cedarling_python` folder:

   ```
   cd jans/jans-cedarling/bindings/cedarling_python
   ```

1. Build and install the package:
   Build the Rust crate and install it into the virtual environment:

   ```
   maturin develop --release
   ```

1. Verify installation:
   Check that the library is installed by listing the installed Python packages:

   ```
   pip list
   ```

   You should see `cedarling_python` listed among the available packages.

1. Read documentation
   After installing the package you can read the documentation from python using the following command:

   ```bash
   python -m pydoc cedarling_python
   ```

## Running a Python Script

To verify that the library works correctly, you can run the provided `example.py` script. Make sure the virtual environment is activated before running the script:

```bash
CEDARLING_POLICY_STORE_LOCAL_FN=example_files/policy-store.json python example.py
```

## Configuration

### Policy Store Sources

Policy store sources can be configured via a YAML/JSON file or environment variables. Here are examples for each source type:

```python
from cedarling_python import BootstrapConfig, Cedarling

# From a local JSON/YAML file
bootstrap_config = BootstrapConfig.load_from_file("/path/to/bootstrap-config.yaml")
instance = Cedarling(bootstrap_config)

# From a local directory (new format)
# In your bootstrap-config.yaml:
# CEDARLING_POLICY_STORE_LOCAL_FN: "/path/to/policy-store/"
bootstrap_config = BootstrapConfig.load_from_file("/path/to/bootstrap-config.yaml")
instance = Cedarling(bootstrap_config)

# From a local .cjar archive
# In your bootstrap-config.yaml:
# CEDARLING_POLICY_STORE_LOCAL_FN: "/path/to/policy-store.cjar"
bootstrap_config = BootstrapConfig.load_from_file("/path/to/bootstrap-config.yaml")
instance = Cedarling(bootstrap_config)

# From a URL (.cjar or Lock Server)
# In your bootstrap-config.yaml:
# CEDARLING_POLICY_STORE_URI: "https://example.com/policy-store.cjar"
bootstrap_config = BootstrapConfig.load_from_file("/path/to/bootstrap-config.yaml")
instance = Cedarling(bootstrap_config)

# Using environment variables instead of a file
import os
os.environ["CEDARLING_POLICY_STORE_LOCAL_FN"] = "/path/to/policy-store.json"
bootstrap_config = BootstrapConfig.from_env()
instance = Cedarling(bootstrap_config)
```

For a complete working example showing the full instantiation flow, see [`example.py`](example.py).

For details on the directory-based format and .cjar archives, see [Policy Store Formats](../../../docs/cedarling/reference/cedarling-policy-store.md#policy-store-formats).

### ID Token Trust Mode

The `CEDARLING_ID_TOKEN_TRUST_MODE` property controls how ID tokens are validated:

- **`strict`** (default): Enforces strict validation rules
  - ID token `aud` must match access token `client_id`
  - If userinfo token is present, its `sub` must match the ID token `sub`
- **`never`**: Disables ID token validation (useful for testing)
- **`always`**: Always validates ID tokens when present
- **`ifpresent`**: Validates ID tokens only if they are provided

### Testing Configuration

For testing scenarios, you may want to disable JWT validation. You can set environment variables:

```bash
export CEDARLING_JWT_SIG_VALIDATION="disabled"
export CEDARLING_JWT_STATUS_VALIDATION="disabled"
export CEDARLING_ID_TOKEN_TRUST_MODE="never"
```

Or configure in your Python code:

```python
import os
os.environ['CEDARLING_ID_TOKEN_TRUST_MODE'] = 'never'
os.environ['CEDARLING_JWT_SIG_VALIDATION'] = 'disabled'
```

For complete configuration documentation, see [cedarling-properties.md](../../../docs/cedarling/cedarling-properties.md).

## Context Data API

The Context Data API allows you to push external data into the Cedarling evaluation context, making it available in Cedar policies through the `context.data` namespace.

### Push Data

Store data with an optional TTL (Time To Live):

```python
from cedarling_python import Cedarling, BootstrapConfig

config = BootstrapConfig.load_from_file("bootstrap-config.yaml")
instance = Cedarling(config)

# Push data without TTL (uses default from config)
instance.push_data_ctx("user:123", {"role": ["admin", "editor"], "country": "US"})

# Push data with TTL (5 minutes = 300 seconds)
instance.push_data_ctx("config:app", {"setting": "value"}, ttl_secs=300)

# Push different data types
instance.push_data_ctx("key1", "string_value")
instance.push_data_ctx("key2", 42)
instance.push_data_ctx("key3", [1, 2, 3])
instance.push_data_ctx("key4", {"nested": "data"})
```

### Get Data

Retrieve stored data:

```python
# Get data by key
value = instance.get_data_ctx("user:123")
if value is not None:
    print(f"User roles: {value['role']}")
```

### Get Data Entry with Metadata

Get a data entry with full metadata including creation time, expiration, access count, and type:

```python
entry = instance.get_data_entry_ctx("user:123")
if entry is not None:
    print(f"Key: {entry.key}")
    print(f"Created at: {entry.created_at}")
    print(f"Access count: {entry.access_count}")
    print(f"Data type: {entry.data_type}")
    print(f"Value: {entry.value()}")
```

### Remove Data

Remove a specific entry:

```python
# Remove data by key
removed = instance.remove_data_ctx("user:123")
if removed:
    print("Entry was removed")
else:
    print("Entry did not exist")
```

### Clear All Data

Remove all entries from the data store:

```python
instance.clear_data_ctx()
```

### List All Data

List all entries with their metadata:

```python
entries = instance.list_data_ctx()
for entry in entries:
    print(f"Key: {entry.key}, Type: {entry.data_type}, Created: {entry.created_at}")
```

### Get Statistics

Get statistics about the data store:

```python
stats = instance.get_stats_ctx()
print(f"Entries: {stats.entry_count}/{stats.max_entries}")
print(f"Total size: {stats.total_size_bytes} bytes")
print(f"Capacity usage: {stats.capacity_usage_percent}%")
```

### Error Handling

The Context Data API methods raise specific exceptions for different error conditions:

```python
from cedarling_python import data_errors

try:
    instance.push_data_ctx("", {"data": "value"})  # Empty key
except data_errors.InvalidKey:
    print("Invalid key provided")

try:
    value = instance.get_data_ctx("nonexistent")
except data_errors.KeyNotFound:
    print("Key not found")
```

Available exceptions:
- `InvalidKey`: The provided key is invalid (e.g., empty)
- `KeyNotFound`: The requested key does not exist
- `StorageLimitExceeded`: The data store has reached its capacity limit
- `TTLExceeded`: The requested TTL exceeds the maximum allowed TTL
- `ValueTooLarge`: The value exceeds the maximum entry size
- `SerializationError`: Failed to serialize/deserialize the value

### Using Data in Cedar Policies

Data pushed via the Context Data API is automatically available in Cedar policies under the `context.data` namespace:

```cedar
permit(
    principal,
    action == Action::"read",
    resource
) when {
    context.data["user:123"].role.contains("admin")
};
```

The data is injected into the evaluation context before policy evaluation, allowing policies to make decisions based on dynamically pushed data.

## Building the Python Library

If you only want to build the library without installing it in the Python environment, follow these steps:

1. Complete the [prerequisites](#Prerequisites)

1. Navigate to the `cedarling_python` folder:

   ```bash
   cd jans/jans-cedarling/bindings/cedarling_python
   ```

1. Build the crate:
   To build the library:

   ```bash
   maturin build --release
   ```

## Python types definitions

The python types definitions are available in the `PYTHON_TYPES.md` file. Or by clicking [here](PYTHON_TYPES.md).
Also after installing the library you can get same information using:

```bash
python -m pydoc cedarling_python
```

## Testing the Python bindings

We use `pytest` and `tox` to create reproduceable environments for testing.

### Run test with `pytest`

To run the tests, with `pytest`:

1. Make sure that you have installed the `cedarling_python` package in your virtual environment or system.
1. Install `pytest`:

   ```bash
   pip install pytest
   ```

1. Make sure that you are in the `jans/jans-cedarling/bindings/cedarling_python/` folder.
1. Run the following command:

   ```bash
   pytest
   ```

   Or run `pytest` without capturing the output:

   ```bash
   pytest -s
   ```

1. See the results in the terminal.

### Run test with `tox`

1. Ensure that you installed rust compiler and toolchain. You can install it by following the official [rust installation guide](https://www.rust-lang.org/tools/install).

1. Ensure tox is installed:
   You can install tox in your environment using pip:

   ```bash
   pip install tox
   ```

1. Make sure that you are in the `jans/jans-cedarling/bindings/cedarling_python/` folder.
1. Run the following command:

   ```bash
   tox
   ```

1. See the results in the terminal.
