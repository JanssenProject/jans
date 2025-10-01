---
tags:
  - cedarling
  - javascript
  - WASM
  - browser app
  - getting-started
---


# Getting Started with Cedarling in a JavaScript app

This guide combines the JavaScript usage instructions with the WebAssembly (WASM) build and API reference for Cedarling.


## Installation

### Using the package manager

You can easily install Cedarling using WASM.

```sh
npm i @janssenproject/cedarling_wasm
```

Alternatively, see [here](#build-from-source), if you want to build Cedarling from the source.


### Build from Source


#### Requirements

Rust 1.63 or Greater. Ensure that you have `Rust` version 1.63 or higher installed. 
You can check your current version of Rust using the command below.

```bash title="Command"
rustc --version
```

Installed `wasm-pack` via `Cargo`. You can install it with the following command:

```bash title="Command"
cargo install wasm-pack
```

Ensure that Clang is installed with support for WebAssembly targets. You can check the installation and available targets with:

```bash title="Command"
clang -print-targets
```

Check `clang` version

```bash title="Command"
clang --version
```

#### Building

Clone the Janssen server repository from the GitHub and change the directory to the `cedarling_wasm` directory:

```bash title="Command"
cd /path/to/jans/jans-cedarling/bindings/cedarling_wasm
```

Build the WebAssembly package in release mode after you've reached the `cedarling_wasm` directory. `wasm-pack` automatically optimizes the WebAssembly binary file using `wasm-opt` for better performance.

```bash title="Command"
wasm-pack build --release --target web
```

To view the WebAssembly project in action, you can run a local server. One way to do this is by using the following command:

```bash title="Command"
python3 -m http.server
```



## Usage

!!! info "Sample Apps"

    You can find usage examples at the following locations in the Janssen server
    repository:

    - A [sample app](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/bindings/cedarling_wasm/index.html) that demonstrates basic usage.
    - A fully featured [Cedarling browser](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/bindings/cedarling_wasm/cedarling_app.html) app where you can test and validate your configuration.

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

## Defined API

```
/**
 * Create a new instance of the Cedarling application.
 * This function can take as config parameter the eather `Map` other `Object`
 */
export function init(config: any): Promise<Cedarling>;
/**
 * A WASM wrapper for the Rust `cedarling::AuthorizeResult` struct.
 * Represents the result of an authorization request.
 */
export class AuthorizeResult {

  /**
   * Convert `AuthorizeResult` to json string value
   */
  json_string(): string;
  principal(principal: string): AuthorizeResultResponse | undefined;
  /**
   * Result of authorization where principal is `Jans::Workload`
   */
  get workload(): AuthorizeResultResponse | undefined;
  /**
   * Result of authorization where principal is `Jans::Workload`
   */
  set workload(value: AuthorizeResultResponse | null | undefined);
  /**
   * Result of authorization where principal is `Jans::User`
   */
  get person(): AuthorizeResultResponse | undefined;
  /**
   * Result of authorization where principal is `Jans::User`
   */
  set person(value: AuthorizeResultResponse | null | undefined);
  /**
   * Result of authorization
   * true means `ALLOW`
   * false means `Deny`
   *
   * this field is [`bool`] type to be compatible with [authzen Access Evaluation Decision](https://openid.github.io/authzen/#section-6.2.1).
   */
  decision: boolean;
  /**
   * Request ID of the authorization request
   */
  request_id: string;
}
/**
 * A WASM wrapper for the Rust `cedar_policy::Response` struct.
 * Represents the result of an authorization request.
 */
export class AuthorizeResultResponse {

  /**
   * Authorization decision
   */
  readonly decision: boolean;
  /**
   * Diagnostics providing more information on how this decision was reached
   */
  readonly diagnostics: Diagnostics;
}
/**
 * The instance of the Cedarling application.
 */
export class Cedarling {

  /**
   * Create a new instance of the Cedarling application.
   * Assume that config is `Object`
   */
  static new(config: object): Promise<Cedarling>;
  /**
   * Create a new instance of the Cedarling application.
   * Assume that config is `Map`
   */
  static new_from_map(config: Map<any, any>): Promise<Cedarling>;
  /**
   * Authorize request
   * makes authorization decision based on the [`Request`]
   */
  authorize(request: any): Promise<AuthorizeResult>;
  /**
   * Authorize request for unsigned principals.
   * makes authorization decision based on the [`RequestUnsigned`]
   */
  authorize_unsigned(request: any): Promise<AuthorizeResult>;
  /**
   * Get logs and remove them from the storage.
   * Returns `Array` of `Map`
   */
  pop_logs(): Array<any>;
  /**
   * Get specific log entry.
   * Returns `Map` with values or `null`.
   */
  get_log_by_id(id: string): any;
  /**
   * Returns a list of all log ids.
   * Returns `Array` of `String`
   */
  get_log_ids(): Array<any>;
  /**
   * Get logs by tag, like `log_kind` or `log level`.
   * Tag can be `log_kind`, `log_level`.
   */
  get_logs_by_tag(tag: string): any[];
  /**
   * Get logs by request_id.
   * Return log entries that match the given request_id.
   */
  get_logs_by_request_id(request_id: string): any[];
  /**
   * Get log by request_id and tag, like composite key `request_id` + `log_kind`.
   * Tag can be `log_kind`, `log_level`.
   * Return log entries that match the given request_id and tag.
   */
  get_logs_by_request_id_and_tag(request_id: string, tag: string): any[];
}
/**
 * Diagnostics
 * ===========
 *
 * Provides detailed information about how a policy decision was made, including policies that contributed to the decision and any errors encountered during evaluation.
 */
export class Diagnostics {

  /**
   * `PolicyId`s of the policies that contributed to the decision.
   * If no policies applied to the request, this set will be empty.
   *
   * The ids should be treated as unordered,
   */
  readonly reason: string[];
  /**
   * Errors that occurred during authorization. The errors should be
   * treated as unordered, since policies may be evaluated in any order.
   */
  readonly errors: PolicyEvaluationError[];
}
export class JsJsonLogic {
  apply(logic: any, data: any): any;
}
/**
 * PolicyEvaluationError
 * =====================
 *
 * Represents an error that occurred when evaluating a Cedar policy.
 */
export class PolicyEvaluationError {

  /**
   * Id of the policy with an error
   */
  readonly id: string;
  /**
   * Underlying evaluation error string representation
   */
  readonly error: string;
}
```

---

## See Also

- [Cedarling TBAC quickstart](../cedarling-quick-start.md#implement-tbac-using-cedarling)
- [Cedarling Unsigned quickstart](../cedarling-quick-start.md#step-1-create-the-cedar-policy-and-schema)
- [Cedarling Sidecar Tutorial](../cedarling-sidecar-tutorial.md)