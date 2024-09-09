# cedarling_python 🐍

This project uses `maturin` to create a Python library from Rust code. Follow the steps below to install and build the library.

### Prerequisites

1. **Install `maturin`:**
   We recommend using `pipx` to install `maturin`. First, ensure `pipx` is installed by following the [official instructions](https://pipx.pypa.io/stable/).

   To install `maturin` using `pipx`:

   ```bash
   pipx install maturin
   ```

1. **Ensure Rust is installed:**
   Verify Rust installation by running:
   ```bash
   cargo --version
   ```
   If Rust is not installed, you can install it from [here](https://www.rust-lang.org/tools/install).

---

## Installing the Python Library

Follow these steps to install the Python package in a virtual environment.

1. **Clone the repository**:
   Ensure the repository is loaded into your directory.

1. **Set up a virtual environment**:
   - Install `venv` for your platform by following the [instructions here](https://virtualenv.pypa.io/en/latest/installation.html).
   - Create a virtual environment:
     ```bash
     python3 -m venv venv
     ```
1. **Activate the virtual environment**:
   Follow the [instructions here](https://packaging.python.org/guides/installing-using-pip-and-virtual-environments/#activate-a-virtual-environment) to activate the virtual environment.

   Example for Linux/macOS:

   ```bash
   source venv/bin/activate
   ```

   Example for Windows:

   ```bash
   .\venv\Scripts\activate
   ```

1. **Navigate to the `cedarling_python` folder**:

   ```bash
   cd cedarling_python
   ```

1. **Install `maturin` dependencies** (optional, for **windows** not work)::

   ```bash
   pip install maturin[patchelf]
   ```

1. **Build and install the package**:
   Build the Rust crate and install it into the virtual environment:

   ```bash
   maturin develop --release
   ```

1. **Verify installation**:
   Check that the library is installed by listing the installed Python packages:
   ```bash
   pip list
   ```

> If you want to install the package globally (not in a virtual environment), you can skip steps 2 and 3.

---

## Running a Python Script

To verify that the library works correctly, you can run the provided `example.py` script. Make sure the virtual environment is activated before running the script:

```bash
python example.py
```

---

## Only Building the Library

If you only want to build the library without installing it in the Python environment, follow these steps:

1. **Activate the virtual environment**:
   (Refer to steps 2 and 3 above if needed.)

1. **Navigate to the `cedarling_python` folder**:

   ```bash
   cd cedarling_python
   ```

1. **Install `maturin` dependencies** (optional, for **windows** not work):

   ```bash
   pip install maturin[patchelf]
   ```

1. **Build the crate**:
   To build the library:
   ```bash
   maturin build --release
   ```

___


## Classes and Methods in python library

### `PolicyStore`

Represents a store for policies used in authorization. It can be created from a JSON string, a file, or a remote URI.

- **from_raw_json(raw_json: str)**: Creates a `PolicyStore` from a raw JSON string.
- **from_filepath(filepath: str)**: Creates a `PolicyStore` from a file path.
- **from_remote_uri(uri: str)**: Creates a `PolicyStore` from a remote URI.

### `TokenMapper`

Maps tokens used for authorization. Can be initialized with optional tokens: `id_token`, `userinfo_token`, `access_token`.

### `BootstrapConfig`

Configuration class that contains the application name, `TokenMapper`, and `PolicyStore`.

### `Authz`

The main authorization engine that evaluates requests.

- **is_authorized(request: Request) -> bool**: Evaluates if the provided request is authorized.

### `Request`

Represents an authorization request with tokens and contextual information.

- **id_token**: The ID token.
- **userinfo_token**: The user info token.
- **access_token**: The access token.
- **action**: The action to be performed.
- **resource**: The resource to be accessed.
- **context**: Additional context for the request.

### `Resource`

Represents the resource involved in the authorization request.

- **_type**: The type of the resource (e.g., `"Jans::Application"`).
- **id**: The ID of the resource.

### Error Handling

Most functions return Python exceptions, such as `PyValueError`, if something goes wrong during the authorization process.

## Example

Ilustrate how to use the library.

```
from cedarling_python import Authz, BootstrapConfig, TokenMapper, PolicyStore, Request, Resource

# for example we store it in the variable, 
# but local policy store better to store in the file
LOCAL_POLICY_STORE='''
{
  "policies": {
    "b34...0540598c": "dsaf..s="
  },
  "trustedIssuers": { },
  "schema": "bm...n0K"
}
'''

# example with raw json
store = PolicyStore.from_raw_json(LOCAL_POLICY_STORE)

# example with reading from file
store = PolicyStore.from_filepath("../demo/policy-store/local.json")

# example with loading from remote uri
store = PolicyStore.from_remote_uri(
    "https://raw.githubusercontent.com/JanssenProject/jans/main/jans-lock/cedarling/demo/policy-store/local.json")


# none means default mapping
# in this example, we extract the "role" claim from the userinfo token
mapper = TokenMapper(id_token=None, userinfo_token="role", access_token=None)
config = BootstrapConfig(application_name="DemoApp",token_mapper=mapper,policy_store=store)

# also fields support setters and getter
config.policy_store = store

authz = Authz(config)

# Create a new Request instance
req = Request()

# Fields can also be provided during the initialization of the Request object.
req.access_token = "ey...aw"
req.id_token = "eyJ...WMQ"
req.userinfo_token = "eyJ...zg"
req.action ='Jans::Action::"Execute"'
req.resource = Resource(_type="Jans::Application", id="Support Portal")
req.context = {}

result = authz.is_authorized(req)

print(f"authorization result: {result}")

```