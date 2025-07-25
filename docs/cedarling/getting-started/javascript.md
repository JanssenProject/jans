---
tags:
  - cedarling
  - javascript
  - getting-started
---

# Getting Started with Cedarling JavaScript

## Installation

You can easily install Cedarling using WASM.

```sh
npm i @janssenproject/cedarling_wasm
```

Alternatively see [here](../cedarling-wasm.md), if you want to build Cedarling from the source.

## Usage

### Initialization

Since Cedarling is a WASM module, you need to initialize it first.

```js
import initWasm, { init } from "@janssenproject/cedarling_wasm";


// initialize the WASM binary
await initWasm();

let cedarling = init(
  "CEDARLING_APPLICATION_NAME": "My App",
  // make sure to update this with your own policy store
  "CEDARLING_POLICY_STORE_URI": "https://raw.githubusercontent.com/...",
  "CEDARLING_LOG_TYPE": "std_out",
  "CEDARLING_LOG_LEVEL": "DEBUG",
  "CEDARLING_USER_AUTHZ": "enabled",
  "CEDARLING_WORKLOAD_AUTHZ": "disabled",
  "CEDARLING_JWT_SIG_VALIDATION": "disabled",
  "CEDARLING_ID_TOKEN_TRUST_MODE": "never",
);
```

### Authorization

Cedarling provides two main interfaces for performing authorization checks: **Token-Based Authorization** and **Unsigned Authorization**. Both methods involve evaluating access requests based on various factors, including principals (entities), actions, resources, and context. The difference lies in how the Principals are provided.

- [**Token-Based Authorization**](#token-based-authorization) is the standard method where principals are extracted from JSON Web Tokens (JWTs), typically used in scenarios where you have existing user authentication and authorization data encapsulated in tokens.
- [**Unsigned Authorization**](#unsigned-authorization) allows you to pass principals directly, bypassing tokens entirely. This is useful when you need to authorize based on internal application data, or when tokens are not available.

#### Token-Based Authorization

To perform an authorization check, follow these steps:

**1. Prepare tokens**

```js
const access_token = "<access_token>";
const id_token = "<id_token>";
const userinfo_token = "<userinfo_token>";
```

Your _principals_ will be built from these tokens.

**2. Define the resource**

This represents the _resource_ that the action will be performed on, such as a protected API endpoint or file.

```js
const resource = {
  cedar_entity_mapping: {
    entity_type: "Jans::Application",
    id: "app_id_001",
  },
  name: "App Name",
  url: {
    host: "example.com",
    path: "/admin-dashboard",
    protocol: "https",
  },
};
```

**3. Define the action**

An _action_ represents what the principal is trying to do to the resource. For example, read, write, or delete operations.

```js
const action = 'Jans::Action::"Read"';
```

**4. Define Context**

The _context_ represents additional data that may affect the authorization decision, such as time, location, or user-agent.

```js
const context = {
  current_time: Math.floor(Date.now() / 1000),
  device_health: ["Healthy"],
  fraud_indicators: ["Allowed"],
  geolocation: ["America"],
  network: "127.0.0.1",
  network_type: "Local",
  operating_system: "Linux",
  user_agent: "Linux",
};
```

**5. Build the request**

Now you'll construct the **_request_** by including the _principals_, _action_, and _context_.

```js
const request = {
  tokens: {
    access_token: access_token,
    id_token: id_token,
    userinfo_token: userinfo_token,
  },
  action: action,
  resource: resource,
  context: context,
};
```

**6. Perform Authorization**

Finally, call the `authorize` function to check whether the principals are allowed to perform the specified action on the resource.

```js
const authorize_result = await cedarling.authorize(request);
```

#### Unsigned Authorization

In unsigned authorization, you pass a set of Principals directly, without relying on tokens. This can be useful when the application needs to perform authorization based on internal data, or when token-based data is not available.

**1. Define the Principals**

```js
const principals = [
  {
    cedar_entity_mapping: {
      entity_type: "Jans::Workload",
      id: "some_workload_id",
    },
    client_id: "some_client_id",
  },
  {
    cedar_entity_mapping: {
      entity_type: "Jans::User",
      id: "random_user_id",
    },
    roles: ["admin", "manager"],
  },
];
```

**2. Define the Resource**

This represents the _resource_ that the action will be performed on, such as a protected API endpoint or file.

```js
const resource = {
  cedar_entity_mapping: {
    entity_type: "Jans::Application",
    id: "app_id_001",
  },
  name: "App Name",
  url: {
    host: "example.com",
    path: "/admin-dashboard",
    protocol: "https",
  },
};
```

**3. Define the Action**

An _action_ represents what the principal is trying to do to the resource. For example, read, write, or delete operations.

```js
const action = 'Jans::Action::"Write"';
```

**4. Define the Context**

The _context_ represents additional data that may affect the authorization decision, such as time, location, or user-agent.

```js
const context = {
  current_time: Math.floor(Date.now() / 1000),
  device_health: ["Healthy"],
  location: "US",
  network: "127.0.0.1",
  operating_system: "Linux",
};
```

**5. Build the Request**

Now you'll construct the **_request_** by including the _principals_, _action_, and _context_.

```js
const request = {
  principals: principals,
  action: action,
  resource: resource,
  context: context,
};
```

**6. Perform Authorization**

Finally, call the `authorize` function to check whether the principals are allowed to perform the specified action on the resource.

```js
const result = await cedarling.authorize_unsigned(request);
```

### Logging

The logs could be retrieved using the `pop_logs` function.

```js
const logs = cedarling.pop_logs();
console.log(logs);
```

---

## See Also

- [Cedarling TBAC quickstart](../cedarling-quick-start-tbac.md)
- [Cedarling Unsigned quickstart](../cedarling-quick-start-unsigned.md)
- [Cedarling Sidecar Tutorial](../cedarling-sidecar-tutorial.md)
- [Cedarling WASM](../cedarling-wasm.md)
