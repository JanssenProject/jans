# Cedarling JavaScript SDK maintainer guide

This guide covers source builds, package qualification, and the intentionally
thin wrapper in jans-cedarling/bindings/cedarling_js. The package
[README](../README.md) is the consumer guide.

## Prerequisites and build order

Source development requires Node.js 22, 24, or 26, npm, a Rust toolchain with
the wasm32-unknown-unknown target, and wasm-pack 0.14.0. Build the read-only
sibling generated package before installing SDK dependencies:

~~~bash
cd jans-cedarling/bindings/cedarling_wasm
wasm-pack build --release --locked --target web --scope janssenproject

cd ../cedarling_js
npm ci --ignore-scripts
npm run check
~~~

The generated package is a development-only input. Do not edit, publish, or
make it a runtime dependency from this package.

## Architecture boundaries

The JavaScript SDK is a stable wrapper around the generated binding, not an
alternate Cedarling configuration model.

- `init(properties)` forwards the original raw CEDARLING property object to the
  generated binding without defaults, renames, snapshots, or property validation.
- `initFromArchiveBytes(properties, bytes)` forwards the same original values to
  the generated archive initializer. Applications retrieve and authorize archive
  downloads themselves.
- Public methods use JavaScript camel case while preserving Cedarling inputs,
  result fields, and behavior.
- Generated classes and free methods never cross the public interface. The
  wrapper copies generated result wrappers into plain data and releases each one.
- Errors reject normally. Do not add custom result wrappers, error classes,
  error-code catalogues, property facades, policy-source loaders, or request reshaping.
- `shutDown()` waits for active authorization calls, invokes the generated shutdown operation,
  then releases the generated client even if shutdown fails.

Raw Cedarling property compatibility belongs to core. The JavaScript package
owns only its method names, plain result shapes, resource lifecycle, and private
runtime loading.

## Project structure

~~~text
cedarling_js/
├── src/
│   ├── index.ts            Browser package-root entry
│   ├── node.ts             Node ESM/CommonJS package-root entry
│   ├── edge.ts             Explicit ESM edge entry
│   ├── runtime.ts          Generated module initialization
│   ├── client.ts           Direct delegation plus copy-and-release
│   ├── types.ts            Public raw-wrapper contract
│   └── wasm-modules.d.ts   Private bundler module declarations
├── tests/
│   ├── unit/               Controlled raw-wrapper lifecycle tests
│   ├── contract/           Real generated-WASM raw-property contract
│   ├── fixtures/           Policy-store fixtures
│   └── runners/            Node and browser runners
└── scripts/                Build, package staging, and installed-consumer checks
~~~

## Runtime and platform qualification

The published package has one raw WASM file at
dist/wasm/cedarling_wasm_bg.wasm. Private conditional exports preserve one
public root API while selecting a loading strategy:

| Runtime | Entry | Private load strategy |
| --- | --- | --- |
| Browser and bundlers | package root | Standard byte/asset import emitted by the bundler. |
| Node ESM | package root | Reads the package-relative shared WASM file. |
| Node CommonJS | package root require | Reads the same package-relative shared WASM file. |
| Edge and Workers | ./edge | Static precompiled WASM module import. |

Do not add a public runtime selector or expose generated glue. The edge entry
is ESM-only because Workers and Vercel Edge require ESM dependency graphs. The
build fails if the edge bundle gains dynamic WASM compilation or if public
bundles retain the generated package as a runtime dependency.

## Build outputs

~~~bash
npm run build
~~~

Build removes stale output, emits ESM and CommonJS declarations, bundles each
runtime entry, patches the generated glue's realm-sensitive WebAssembly-module
check, and copies the sole raw WASM payload. Each runtime directory contains
only its bundle and self-contained source map; declaration trees are kept for
both ESM and CommonJS type resolution.

`.build` is transient compiler and test output. `dist` is the package's
publishable surface and must contain only files selected by `package.json`.

## Test organization

The replacement tests prove:

- raw properties and archive Uint8Array values reach generated initialization by
  identity;
- every generated result wrapper is copied to ordinary data and released;
- shutdown releases the generated client after its generated shutdown operation;
- a real generated-WASM client initializes from raw properties, authorizes, and
  uses context data; and
- the staged package works as installed ESM and CommonJS.

Portable contracts run unchanged under Bun, Deno, and current Playwright
Chromium, Firefox, and WebKit. Firefox ESR has a separate WebDriver runner.
Runtime support is a claim only after the corresponding command succeeds.

## Bundler qualification

The browser root uses a standard WASM byte or asset import. Keep that contract
when changing the build; browser consumers must not configure a separate WASM
asset. Qualify any bundler-related change with the browser contract suite.

## npm scripts

`test:node`, `test:bun`, `test:deno`, `test:browser`, and
`test:firefox-esr` consume output prepared by `npm run test:prepare`. CI
prepares once so each runtime exercises the same SDK and compiled test output.

| Script | Purpose |
| --- | --- |
| `npm run build` | Clean output, emit declarations and bundles, and copy the shared WASM asset. |
| `npm run test:prepare` | Build the SDK and compile tests once. |
| `npm run test:node` | Run prepared controlled wrapper tests and real-WASM Node.js contracts. |
| `npm run test:bun` | Run prepared portable contracts in Bun. |
| `npm run test:deno` | Run prepared portable contracts in Deno. |
| `npm run test:browser` | Bundle prepared contracts and run Playwright Chromium, Firefox, and WebKit. |
| `npm run test:firefox-esr` | Bundle prepared contracts and run Firefox ESR through WebDriver. |
| `npm run package:verify` | Build, stage, type-check, install, and execute ESM/CommonJS consumers. |
| `npm run check` | Prepare, run Node tests, and verify installed consumers. |
| `npm run check:all` | Run `check` plus Bun, Deno, Playwright, and Firefox ESR qualification. |

## Local verification

Firefox ESR commands require an executable path. Set it before the full local
qualification gate:

~~~bash
CEDARLING_FIREFOX_ESR_BINARY=/path/to/firefox-esr npm run check:all
~~~

## Package staging and consumer verification

Stage a private tarball outside the repository so no transient artifact enters
the working tree:

~~~bash
npm run build && node scripts/stage-packages.mjs --output /tmp/cedarling-js-package
~~~

The underlying staging script also accepts an exact `--version` and an explicit
`--publishable` flag for release automation. `package:verify` is the local gate
for the installed ESM and CommonJS consumer contract.

## CI coverage

CI prepares one SDK artifact, checks installed consumers on Node.js 22, 24, and
26, then runs Bun, Deno LTS, latest Deno, Playwright Chromium/Firefox/WebKit,
and Firefox ESR qualification.

## Generated-boundary invariants

- Treat the generated declaration file as the capability source of truth.
- Add a public method only when it maps directly to a generated Cedarling
  capability and its generated outputs can be converted and released privately.

## Raw-wrapper maintenance

- Keep raw properties opaque to JavaScript. Add examples and links to core docs,
  not JavaScript-side property policy.
- Change package exports, shared-WASM loading, or build output only with full
  installed-package and supported-runtime qualification.
- Keep consumer material in README and build/qualification reasoning here.

## Maintainer checklist

- [ ] Keep raw properties and request JSON opaque to the wrapper.
- [ ] Keep generated wrappers private and release every copied result.
- [ ] Keep consumer guidance aligned with the current raw bootstrap-properties API.
- [ ] Run the appropriate package and runtime qualification gates.
