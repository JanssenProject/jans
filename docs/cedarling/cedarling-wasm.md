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

* Clang with WebAssembly (WASM) Target Support
Ensure that Clang is installed with support for WebAssembly targets. 
This is necessary for compiling C/C++ code to WebAssembly. 
You can check the installation and available targets with:
```bash 
clang --version
```
## Building


**Install wasm-pack**

* To get started with WebAssembly in Rust, first install `wasm-pack` by running the following command:

```bash title="Command"
cargo install wasm-pack
```


  **Cloning the Jans Monorepo and Changing Directory**

  1. Clone the Jans Monorepo
  First, clone the Jans monorepository from GitHub using the following command:
  ```bash title="Command"
  git clone https://github.com/JanssenProject/jans.git
  ```
  This will create a local copy of the repository on your system.

2. Navigate to the jans-cedarling Directory
  After cloning the repository, change the directory to the jans-cedarling folder by running:
  ```bash title="Command"
  cd /path/to/jans/jans-cedarling
  ```
Make sure to replace `/path/to/` with the actual path where the `jans` repository was cloned.


**Add WebAssembly Dependencies**

* Navigate to `/jans/jans-cedarling/test_utils`
* Open the `Cargo.toml` file in the project directory and add the necessary dependencies 
for WebAssembly. Specifically, we will use `wasm-bindgen`, which provides an 
interface between Rust and JavaScript.

* Edit `Cargo.toml` to look like this:

```
[package]
name = "test_utils"
version = "0.0.0-nightly"
edition = "2021"

[dependencies]
pretty_assertions = "1"
serde_json = { workspace = true }
jsonwebtoken = { workspace = true }
jsonwebkey = { workspace = true, features = ["generate", "jwt-convert"] }
serde = { workspace = true }


[lib]
crate-type = ["cdylib", "rlib"]
```


**Build the WebAssembly Project**

* Once the dependencies are set up, build the WebAssembly package in release mode 
by running the following command:
```bash title="Command"
wasm-pack build --release --target web
```

* wasm-pack automatically optimizes the WebAssembly binary file using 
wasm-opt for better performance.


## Including in projects


For using result files in browser project you need make result `pkg` folder 
accessible for loading in the browser so that you can later import the 
corresponding file from the browser.

Create a new `pkg/index.html` file and add the below code.

Here is example of code snippet:

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>WebAssembly Cedarling Example</title>
</head>
<body>
    <h1>WebAssembly Cedarling Project</h1>
    <p>This is a simple example to run WebAssembly in the browser.</p>

    <!-- Include the script to load WebAssembly -->
    <script type="module">
        import initWasm, { init } from "/pkg/cedarling_wasm.js";

        async function main() {
            await initWasm(); // Initialize the WebAssembly module

            // Initialize Cedarling with `BOOTSTRAP` config
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
                "CEDARLING_USER_WORKLOAD_BOOLEAN_OPERATION": "AND",
            });

            // Make authorize request
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
</body>
</html>

```



## Run the Server

* To view the WebAssembly project in action, you can run a local server. 
One way to do this is by using `http-server`, a simple, zero-config static 
file server. You can install and run it using:

```bash title="Command"
npx http-server .
```



## Usage

Before usage make sure that you have completed `Building` steps.
You can find usage examples in the following locations:

- `jans-cedarling/bindings/cedarling_wasm/index.html`: A simple example demonstrating basic usage.
- `jans-cedarling/bindings/cedarling_wasm/cedarling_app.html`: A fully featured `Cedarling` browser app where you can test and validate your configuration.

### Defined API

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
