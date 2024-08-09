## cedarling ⚙️

The `cedarling` is an embeddable Webassembly Component that runs a local Cedar Engine, enabling fine grained and responsive Policy Management on the Web (or any JS environment). The `cedarling` allows for dynamic updates to it's internal Policy Store via Server Sent events, enabling sub-second Access Management.

### Setup 1️⃣

Make sure you have [the Rust Toolchain](https://rustup.rs/), [`wasm-bindgen`](https://rustwasm.github.io/wasm-bindgen/reference/cli.html), [`clang`](https://clang.llvm.org/) and the `wasm32-unknown-unknown` Rust target triple installed. General setup follows the following flow:

```bash
# clang installation differs by OS and distribution

$ rustup default stable # Install Rust stable
$ rustup target add wasm32-unknown-unknown # Install wasm target

$ cargo install -f wasm-bindgen-cli # Install wasm-bindgen CLI
```

### Building ⚒️

To build the `cedarling`, run the following commands, the appropriate Javascript bindings will be generated in the `out` directory.


```bash
$ cargo build --release --target wasm32-unknown-unknown

# For use in Node, Deno or the Edge
$ wasm-bindgen --out-dir out ./target/wasm32-unknown-unknown/release/cedarling.wasm

$ ls out
	out
	├── cedarling_bg.js
	├── cedarling_bg.wasm
	├── cedarling_bg.wasm.d.ts
	├── cedarling.d.ts
	└── cedarling.js
```

To run the `cedarling`, you'll need to include the `cedarling.js`, `cedarling_bg.js` and `cedarling.wasm`. The other files in `out` are Typescript type definitions.

### Special Instructions for the Web 🌍

The Web requires the WASM binary to be loaded from the network, and thus requires some manual initialization. Luckily, it's not a complicated process. To build:

```bash
$ wasm-bindgen --out-dir out ./target/wasm32-unknown-unknown/release/cedarling.wasm --target web
```

To instantiate:

```js
import setup, { authz, init } from "cedarling.js";

// The default export is the initialization function, here renamed to `setup`
await setup();

// ... use cedarling functions here
```

### Testing 🧪

To test, you'll need the `wasm-pack` test utility. To install `wasm-pack` refer [here](https://rustwasm.github.io/wasm-pack/installer/), or run:

```bash
$ curl https://rustwasm.github.io/wasm-pack/installer/init.sh -sSf | sh
```

The tests simulate a JS environment, therefore there's no need to write any actual JS tests. To run tests, run:

```bash
$ wasm-pack test --node
$ wasm-pack test --chrome # --firefox or --safari
```

### Usage 🔧

From within your JS project, you'll need to import the exported `cedarling` functions from the `cedarling.js` file.

```js
// Cedarling Initialization Flow
// INFO: The cedarling must be initialized once and no more than once

import { init } from "cedarling.js"

const config = {
	policyStore: {
		// can be "local", "remote" or "lock-server",
		// each strategy requires different parameters, see below
		strategy: "local",
	},
	// if policy-store.json is compressed using deflate-zlib
	decompressPolicyStore: false,
	// [OPTIONAL] How often, in milliseconds, will the cedarling refresh it's TrustStore. The cedarling won't refresh it's trust store if not omitted
	trustStoreRefreshRate: 2000,
	// Set of jwt algorithms that the cedarling will allow, empty if omitted
	supportedAlgorithms: ["HS256", "HS384", "RS256"]
};

/// > config.policyStore options <

// the "local" strategy is a fallback option. the cedarling will use a statically embedded policy store, located in `/policy-store/default.json`
const local = {
	strategy: "local"
};

// the "remote" strategy is only slightly more complex than "local", with the only difference being you provide a http `url` from which a simple GET request is used to acquire the Policy Store
const remote = {
	strategy: "remote",
	url: "https://raw.githubusercontent.com/JanssenProject/jans/main/jans-lock/cedarling/policy-store/default.json"
}

// the "lock-server" strategy is a more complicated, authenticated strategy employing OAuth.
const lockMaster = {
	strategy: "lock-server",
	// `url` a http URL to a Jans Lock Master instance
	url: "https://lock.master.gluu.cloud",
	// `policyStoreId` acquire a specific Policy Store from the Lock Master
	policyStoreId: "#83J5KF9U2KAKtO2J",
	// `enableDynamicConfiguration` if the cedarling should subscribe to Policy Updates via the Lock Master's SSE endpoint
	enableDynamicConfiguration: true,
	// `ssaJwt`: Software Statement used by the cedarling during OAuth Dynamic Client registration
	ssaJwt: "..."
}

/// END > config.policyStore options <

// To initialize the cedarling, run init(config)
init(config);
```

---

```js
// Cedarling Authorization Flow

import { init, authz } from "cedarling.js"

// Ensure the cedarling is initialized before calling authz
init(..);

// 🚧 (WIP)
```

### Lock Master SSE Interface 🚧

> A specification for Policy Updates is in incubation