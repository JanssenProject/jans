---
tags:
  - cedarling
  - wasm
---

# WASM for Cedarling

Cedarling provides a binding for JavaScript programs via the `wasm-pack` tool. 
This allows browser developers to use the cedarling crate in their code directly.

## Requirements

* Rust 1.63 or Greater. Ensure that you have `Rust` version 1.63 or higher installed. You can check your 
  current version of Rust by running the following command in your terminal:
```bash   
rustc --version
```

* Installed `wasm-pack` via `Cargo`. 
You can install it with the following command:
```bash
cargo install wasm-pack
```

* Ensure that Clang is installed with support for WebAssembly targets. 
You can check the installation and available targets with:
```bash 
clang -print-targets
```
Check `clang` version
```bash 
clang --version
```

## Building

  * Clone the Janssen server repository from the GitHub and change the directory 
  to the `cedarling_wasm` directory:
  ```bash title="Command"
  cd /path/to/jans/jans-cedarling/bindings/cedarling_wasm
  ```

  * Build the WebAssembly package in release mode after you've reached 
the `cedarling_wasm` directory. `wasm-pack` automatically optimizes the 
WebAssembly binary file using `wasm-opt` for better performance.
```bash title="Command"
wasm-pack build --release --target web
```

  * To view the WebAssembly project in action, you can run a local server. 
One way to do this is by using the following command:
```bash title="Command"
python3 -m http.server
```

## Including in projects

!!! info "Sample Apps"

    You can find usage examples in the following locations in the Janssen server
    repository:

    - `jans/jans-cedarling/bindings/cedarling_wasm/index.html`: A simple example demonstrating basic usage.
    - `jans/jans-cedarling/bindings/cedarling_wasm/cedarling_app.html`: A fully featured `Cedarling` browser app where you can test and validate your configuration.




For using result files in the browser project, you need to make the 
result `pkg` directory accessible for loading in the browser so that you can 
later import the corresponding file from the browser.

```html title="Example code snippet"
   <script type="module">
        import initWasm, { init } from "/pkg/cedarling_wasm.js";

        async function main() {
            await initWasm(); // Initialize the WebAssembly module

            // init cedarling with `BOOTSTRAP` config
            let instance = await init({
               "CEDARLING_APPLICATION_NAME": "My App",
                "CEDARLING_POLICY_STORE_URI": "https://example.com/policy-store.json",
                "CEDARLING_LOG_TYPE": "memory",
                "CEDARLING_LOG_LEVEL": "INFO",
                "CEDARLING_LOG_TTL": 120,
                "CEDARLING_DECISION_LOG_USER_CLAIMS ": ["aud", "sub", "email", "username"],
                "CEDARLING_DECISION_LOG_WORKLOAD_CLAIMS ": ["aud", "client_id", "rp_id"],
                "CEDARLING_USER_AUTHZ": "enabled",
                "CEDARLING_WORKLOAD_AUTHZ": "enabled",
                "CEDARLING_PRINCIPAL_BOOLEAN_OPERATION": {
                    "and" : [
                        {"===": [{"var": "Jans::Workload"}, "ALLOW"]},
                        {"===": [{"var": "Jans::User"}, "ALLOW"]}
                    ]
                },
            });
            // make authorize request
            let result = await instance.authorize({
                "tokens": {
                    "access_token": "...",
                    "id_token": "...",
                    "userinfo_token": "...",
                },
                "action": 'Jans::Action::"Read"',
                "resource": {
                    "type": "Jans::Application",
                    "id": "some_id",
                    "app_id": "application_id",
                    "name": "Some Application",
                    "url": {
                        "host": "jans.test",
                        "path": "/protected-endpoint",
                        "protocol": "http"
                    }
                },
                "context": {
                    "current_time": Math.floor(Date.now() / 1000),
                    "device_health": ["Healthy"],
                    "fraud_indicators": ["Allowed"],
                    "geolocation": ["America"],
                    "network": "127.0.0.1",
                    "network_type": "Local",
                    "operating_system": "Linux",
                    "user_agent": "Linux"
                },
            });
            console.log("result:", result);
        }
        main().catch(console.error);
    </script>

```

## Defined API

```ts
/**
 * Create a new instance of the Cedarling application.
 * This function can take as config parameter the eather `Map` other `Object`
 */
export function init(config: any): Promise<Cedarling>;

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
}

/**
 * A WASM wrapper for the Rust `cedarling::AuthorizeResult` struct.
 * Represents the result of an authorization request.
 */
export class AuthorizeResult {
  /**
   * Convert `AuthorizeResult` to json string value
   */
  json_string(): string;
  /**
   * Result of authorization where principal is `Jans::Workload`
   */
  workload?: AuthorizeResultResponse;
  /**
   * Result of authorization where principal is `Jans::User`
   */
  person?: AuthorizeResultResponse;
  /**
   * Result of authorization
   * true means `ALLOW`
   * false means `Deny`
   *
   * this field is [`bool`] type to be compatible with [authzen Access Evaluation Decision](https://openid.github.io/authzen/#section-6.2.1).
   */
  decision: boolean;
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
  readonly reason: (string)[];
  /**
   * Errors that occurred during authorization. The errors should be
   * treated as unordered, since policies may be evaluated in any order.
   */
  readonly errors: (PolicyEvaluationError)[];
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
