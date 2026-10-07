# Cedarling WASM

This module builds Cedarling's Rust implementation for WebAssembly. This guide
covers building and testing the generated binding and running its local browser
examples; JavaScript package consumers should use the npm package guide below.

## Choose the right guide

- For a JavaScript or TypeScript application, use the
  [npm package guide](./js/README.md).
- For package generation, artifact qualification, and release maintenance, use
  the [maintainer guide](./js/docs/maintainer.md).
- This guide covers direct development with the wasm-pack-generated binding.

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
import { BOOTSTRAP_CONFIG, REQUEST_UNSIGNED } from "/example_data.js"; // Import js objects: bootstrap config and request
import initWasm, { init } from "/pkg/cedarling_wasm.js";

async function main() {
  await initWasm(); // Initialize the WebAssembly module

  let instance = await init(BOOTSTRAP_CONFIG);
  // authorize calls take the request as a JSON string: it crosses the
  // JS/WASM boundary as one string copy parsed by serde_json
  let result = await instance.authorizeUnsigned(JSON.stringify(REQUEST_UNSIGNED));
  console.log("result:", result);
}
main().catch(console.error);
```

When importing the generated glue directly from `pkg/`, call `initWasm()` before
using the binding. The npm package's automatic entries handle this initialization.

## API contract

`pkg/cedarling_wasm.d.ts` is the authoritative direct WebAssembly API after a
successful build. Callable JavaScript APIs use camelCase, including
`jsonString()`. Cedarling bootstrap properties, request JSON keys, and returned
data fields retain their canonical names, including snake_case fields such as
`request_id`.

For portable npm consumption across browser, Node.js ESM, Node.js CommonJS,
edge, and manual bundler paths, use `@janssenproject/cedarling_wasm` and its
consumer guide instead of importing files under `pkg/`.
