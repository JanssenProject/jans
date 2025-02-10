# Cedarling WASM

This module is designed to build cedarling for browser wasm.

## Building

For building we use [`wasm-pack`](https://developer.mozilla.org/en-US/docs/WebAssembly/Rust_to_Wasm) for install you can use command `cargo install wasm-pack`

Build cedarling in release:

```bash
wasm-pack build --release --target web
```

Build cedarling in dev mode

```bash
wasm-pack build --target web --dev
```

Result files will be in `pkg` folder.

## Testing

For WASM testing we use `wasm-pack` and it allows to make test in `node`, `chrome`, `firefox`, `safari`. You just need specify appropriate flag.

Example for firefox.

```bash
wasm-pack test --firefox
```

## Run browser example

To run example using `index.html` you need execute following steps:

1. Build wasm cedarling.
2. Run webserver using `python3 -m http.server` or any other.
3. Visit example app [localhost](http://localhost:8000/), on this app you will get log in browser console.
    - Also you can try use cedarling with web app using [cedarling_app](http://localhost:8000/cedarling_app.html), using custom bootstrap properties and request.

## WASM Usage

After building WASM bindings in folder `pkg` you can find where you can find `cedarling_wasm.js` and `cedarling_wasm.d.ts` where is defined interface for application.

In `index.html` described simple usage of `cedarling wasm` API:

```js
        import { BOOTSTRAP_CONFIG, REQUEST } from "/example_data.js" // Import js objects: bootstrap config and request
        import initWasm, { init } from "/pkg/cedarling_wasm.js";

        async function main() {
            await initWasm(); // Initialize the WebAssembly module

            let instance = await init(BOOTSTRAP_CONFIG);
            let result = await instance.authorize(REQUEST);
            console.log("result:", result);
        }
        main().catch(console.error);
```

Before using any function from library you need initialize WASM runtime by calling `initWasm` function.

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
