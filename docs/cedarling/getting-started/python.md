---
tags:
  - cedarling
  - python
  - getting-started
---

# Getting Started with Cedarling Python

- [Installation](#installation)
- [Usage](#usage)

## Installation

At the moment, the Cedarling Python bindings are not available via package managers like PyPI. To use them, you can either download a pre-compiled `cedarling_python` wheel from the [releases page](https://github.com/JanssenProject/jans/releases/latest) or [build it from the source](#building-from-source).

### Building from source

The recommended approach is to compile a Python wheel using [Maturin](https://github.com/PyO3/maturin), a tool for building and publishing Rust-based Python packages.

**1. Set up a virtual environment**

```sh
python -m venv venv
source venv/bin/activate
```

**2. Install `maturin`**

```sh
# for non-Linux systems
pip install maturin

# for Linux systems
pip install maturin[patchelf]
```

**3. Clone the [jans](https://github.com/JanssenProject/jans) repository and navigate to the python bindings directory.**

```sh
git clone https://github.com/JanssenProject/jans.git jans
cd jans/jans-cedarling/bindings/cedarling_python/
```

**Build the bindings**

You have two options from here:

**a. Build a wheel**

```sh
maturin build --release
```

This produces a `.whl` file in the `target/wheels/` directory.

**b. Install into your virtual environment directly**

```sh
maturin develop
```

## Including in projects

If you're using a dependency manager like [Poetry](https://python-poetry.org/), you can:

**Option 1: Add the wheel via CLI**

```
poetry add path/to/wheel.whl
```

**Option 2: Install it manually into Poetry's virtual environment**

```
poetry run pip install path/to/wheel.whl
```

**Option 3: Add it to `pyproject.toml` statically**

```
[tool.poetry.dependencies]
cedarling_python = {path = "path/to/wheel.whl"}
```

For other dependency managers, refer to their documentation on how to use local wheels.

## Usage

### Initialization

```py
# Load the bootstrap properties from the environment variable, using default values
# for unset properties
bootstrap_config = BootstrapConfig.from_env()

# Initialize Cedarling
cedarling = Cedarling(bootstrap_config)
```

See the python documentation for `BootstrapConfig` for other config loading options.

### Authorization

Cedarling provides two main interfaces for performing authorization checks: **Token-Based Authorization** and **Unsigned Authorization**. Both methods involve evaluating access requests based on various factors, including principals (entities), actions, resources, and context. The difference lies in how the Principals are provided.

- [**Token-Based Authorization**](#token-based-authorization) is the standard method where principals are extracted from JSON Web Tokens (JWTs), typically used in scenarios where you have existing user authentication and authorization data encapsulated in tokens.
- [**Unsigned Authorization**](#unsigned-authorization) allows you to pass principals directly, bypassing tokens entirely. This is useful when you need to authorize based on internal application data, or when tokens are not available.

#### Token-Based Authorization

To perform an authorization check, follow these steps:

**1. Prepare tokens**

```py
access_token = "<access_token>"
id_token = "<id_token>"
userinfo_token = "<userinfo_token>"
```

Your _principals_ will be built from these tokens.

**2. Define the resource**

```py
resource = EntityData(
  cedar_entity_mapping=CedarEntityMapping(
    entity_type="Jans::Application",
    id="app_id_001"
  ),
  name="App Name",
  url={
    "host": "example.com",
    "path": "/admin-dashboard",
    "protocol": "https"
  }
)
```

**3. Define the action**

```py
action = 'Jans::Action::"Read"'
```

**4. Define Context**

```py
context = {
  "current_time": int(time.time()),
  "device_health": ["Healthy"],
  "fraud_indicators": ["Allowed"],
  "geolocation": ["America"],
  "network": "127.0.0.1",
  "network_type": "Local",
  "operating_system": "Linux",
  "user_agent": "Linux"
}
```

**5. Build the request**

```py
request = Request(
  tokens={
    "access_token": access_token,
    "id_token": id_token,
    "userinfo_token": userinfo_token,
  },
  action=action,
  resource=resource,
  context=context
)
```

**6. Authorize**

```py
authorize_result = cedarling.authorize(request)
```

#### Unsigned Authorization

In unsigned authorization, you pass a set of Principals directly, without relying on tokens. This can be useful when the application needs to perform authorization based on internal data, or when token-based data is not available.

**1. Define the Principals**

```py
principals = [
  EntityData(
    cedar_entity_mapping=CedarEntityMapping(
      entity_type="Jans::Workload",
      id="some_workload_id"
    ),
    client_id="some_client_id",
  ),
  EntityData(
    cedar_entity_mapping=CedarEntityMapping(
      entity_type="Jans::User",
      id="random_user_id"
    ),
    roles=["admin", "manager"]
  ),
]
```

**2. Define the Resource**

This represents the _resource_ that the action will be performed on, such as a protected API endpoint or file.

```py
resource = EntityData(
  cedar_entity_mapping=CedarEntityMapping(
    entity_type="Jans::Application",
    id="app_id_001"
  ),
  name="App Name",
  url={
    "host": "example.com",
    "path": "/admin-dashboard",
    "protocol": "https"
  }
)
```

**3. Define the Action**

An _action_ represents what the principal is trying to do to the resource. For example, read, write, or delete operations.

```py
action = 'Jans::Action::"Write"'
```

**4. Define the Context**

The _context_ represents additional data that may affect the authorization decision, such as time, location, or user-agent.

```py
context = {
    "current_time": int(time.time()),
    "device_health": ["Healthy"],
    "location": "US",
    "network": "127.0.0.1",
    "operating_system": "Linux",
}
```

**5. Build the Request**

Now you'll construct the **_request_** by including the _principals_, _action_, and _context_.

```py
request = RequestUnsigned(
  principals=principals,
  action=action,
  resource=resource,
  context=context
)
```

**6. Perform Authorization**

Finally, call the `authorize` function to check whether the principals are allowed to perform the specified action on the resource.

```py
result = cedarling.authorize_unsigned(request);
```

### Logging

The logs could be retrieved using the `pop_logs` function.

```py
logs = cedarling.pop_logs()
print(logs)
```

---

## See Also

- [Cedarling TBAC quickstart](../cedarling-quick-start.md#implement-tbac-using-cedarling)
- [Cedarling Unsigned quickstart](../cedarling-quick-start.md#step-1-create-the-cedar-policy-and-schema)
- [Cedarling Sidecar Tutorial](../cedarling-sidecar-tutorial.md)
