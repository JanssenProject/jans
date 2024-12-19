# Cedarling WASM

This module is designed to build cedarling for browser wasm.

## Building

For building we use [`wasm-pack`](https://developer.mozilla.org/en-US/docs/WebAssembly/Rust_to_Wasm) for install you can use command `cargo install wasm-pack`

Build cedarling:

```bash
wasm-pack build --release --target web
```

Result files will be in `pkg` folder.

## Run browser example

To run example using `index.html` you need execute following steps:

1. Build wasm cedarling.
2. Run webserver using `python3 -m http.server` or any other.
3. Visit [localhost](http://localhost:8000/).
