# Cedarling WASM Bindings

This repository provides WebAssembly (WASM) bindings for Cedarling, making it easy to integrate into web projects. Follow the instructions below to build, install, and run the example.

## Building the WASM Bindings

To compile the WASM bindings, run the following command:

```sh
wasm-pack build
```

## Installing and Using the Bindings

Install the cedarling_wasm package in your project with:

```sh
npm install cedarling_wasm
```

Then, import and initialize `cedarling` in your JavaScript code:

```js
import * as cedarling from "cedarling_wasm";
cedarling.init();
```

You can now use cedarling's WASM functionality directly in your application.

## Running the example

A sample project demonstrating usage is available in the `example/` directory.

To run the example:

1. Install the required packages:

```sh
npm install
```

2. Start the example application:

```sh
npm run start
```
