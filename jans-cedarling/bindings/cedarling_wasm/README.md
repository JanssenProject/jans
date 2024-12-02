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
import { Cedarling } from "cedarling_wasm";

let cedarling = Cedarling.new({
  "application_name": "TestApp",
  "policy_store_id": "asdasd123123",
});

cedarling.authorize();
```

You can now use cedarling's WASM functionality directly in your application.

## Running the example

A sample project demonstrating the usage is available in the `example/` directory.

To run the example:

```sh
cd ./example
npm install
npm run start
```
