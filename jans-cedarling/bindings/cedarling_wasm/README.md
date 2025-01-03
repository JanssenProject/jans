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
3. Visit [localhost](http://localhost:8000/).
