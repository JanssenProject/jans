# Cedarling JavaScript SDK maintainer guide

This guide covers source builds, architecture, tests, package qualification,
and CI for `jans-cedarling/bindings/cedarling_js`. The package
[README](../README.md) is the consumer guide and must remain focused on SDK
installation and usage.

## Prerequisites and build order

Source development requires:

- Node.js 20.19 or newer with npm;
- the stable Rust toolchain with the `wasm32-unknown-unknown` target; and
- `wasm-pack` 0.14.0, matching the repository workflow.

Build the sibling generated package before installing SDK dependencies:

```bash
cd jans-cedarling/bindings/cedarling_wasm
wasm-pack build --release --locked --target web --scope janssenproject

cd ../cedarling_js
npm ci --ignore-scripts
npm run check
```

The SDK dependency points to `../cedarling_wasm/pkg`. Rebuild it whenever the
Rust binding changes, then reinstall SDK dependencies so local qualification
uses the current generated output.

## Project structure

```text
cedarling_js/
├── src/
│   ├── authorization/  Public request validation and authorization types
│   ├── client/         Public facade, services, and lifecycle coordination
│   ├── configuration/  Typed/raw configuration preparation
│   ├── context/        Context-store input and public types
│   ├── engine/         Generated-boundary adaptation and runtime loading
│   ├── entries/        Node-family package entry
│   ├── errors/         Sole error code catalogue and error normalization
│   ├── helpers/        Shared descriptor-safe validation mechanics
│   ├── issuers/        Issuer-reference validation and public types
│   ├── logs/           Retained-log queries and normalization
│   └── values/         JSON and Cedar value snapshots
├── tests/
│   ├── unit/           Focused behavior with controlled engine fixtures
│   ├── contract/       Public behavior against the real generated package
│   ├── fixtures/       Policy stores and reusable inputs
│   └── runners/        Node-family and browser suite launchers
├── scripts/            Clean, staging, and installed-consumer verification
├── package.json        Exports, commands, and dependencies
└── tsconfig.build.json Production TypeScript build
```

## Architecture boundaries

Consumers import only the package root and receive one runtime value:
`createCedarling`. Runtime selection stays inside package export conditions.
Generated classes, methods, result wrappers, and disposal hooks must never
enter the public API.

Keep these responsibilities separate:

- configuration modules validate, normalize, and detach caller input;
- the client facade owns public services, `Result<T>`, and lifecycle behavior;
- the engine seam isolates generated binding behavior from the client;
- runtime loaders deliver the generated module without changing public usage;
- generated adapters convert outputs to detached JavaScript-owned values;
- error modules own the single `CedarlingError` and `CedarlingErrorCode` model.

Do not add runtime selectors, runtime-specific public subpaths, feature-specific
error classes, error aliases, or separate error-code catalogues.

## Build outputs

```bash
npm run typecheck
npm run build
```

`build` removes stale `dist`, compiles the ESM declarations and entries, then
creates the Node-family CommonJS bundle at `dist/cjs/node.cjs`. The generated
package remains external to that bundle.

The root package export provides:

- browser ESM through `dist/index.js`;
- Node-family ESM through `dist/entries/node.js`; and
- Node-family CommonJS through `dist/cjs/node.cjs`.

ESM and CommonJS must expose the same sole runtime value.

## Test organization

QUnit is the shared runtime-neutral assertion framework. Behavioral suites are
registered once and loaded by thin host runners.

- Unit tests exercise validation, input detachment, error normalization,
  generated-protocol conversion, and lifecycle behavior with controlled
  engine fixtures.
- Contract tests execute the built SDK against the real generated package.
- Portable contracts exclude Node-specific servers and filesystem fixtures so
  the same list can run under Bun, Deno, and browsers.
- Playwright serves the browser bundle and runs that portable list in Chromium,
  Firefox, and WebKit.

Keep host setup in runners and behavior assertions in shared suites. Do not
copy a contract solely to qualify another runtime.

## Local verification

Prepare production and test output once when running individual compiled
groups:

```bash
npm run test:prepare
node .test-dist/runners/node.js unit
node .test-dist/runners/node.js contract
```

The self-contained Node commands rebuild their prerequisites:

```bash
npm run test:unit
npm run test:contract
```

Run the complete local Node and installed-package gate before handoff:

```bash
npm run check
```

`check` performs source type-checking, a clean production/test build, all Node
unit tests, all real-generated-package contracts, and clean installed ESM and
CommonJS consumer verification.

Qualify the portable contract list in other installed runtimes. Each command
rebuilds its prerequisites:

```bash
npm run test:portable:bun
npm run test:portable:deno
```

Install the Playwright browsers once, then run browser qualification:

```bash
npm exec -- playwright install chromium firefox webkit
npm run test:browser
```

`test:browser` rebuilds SDK and test output, bundles only the portable suite
graph, and runs Chromium, Firefox, and WebKit.

## Package staging and consumer verification

The repository uses private coordinated tarballs before publication and for
local demo consumption. Stage both packages with one exact version:

```bash
npm run package:stage -- --output ./artifacts --version 1.0.0
```

The staging module:

- copies only built SDK output and its consumer README;
- applies the same exact version to the SDK and generated package;
- replaces local dependency references with that exact version;
- rejects non-exact dependency specifications; and
- leaves both staged manifests private.

Do not commit generated tarballs or the staging directory.

Verify package contents and resolution with:

```bash
npm run package:verify
```

The verifier stages into a temporary directory, installs both tarballs into a
clean offline consumer, rejects duplicate generated-package installation, and
executes real authorization through both ESM and CommonJS.

Registry publication, credentials, provenance, and release promotion are not
implemented by these scripts.

## CI coverage

The `cedarling_js_tests` job in `.github/workflows/test-cedarling.yml`:

1. builds the current generated binding from the checked-out Rust source;
2. installs dependencies from `package-lock.json`;
3. runs type-checking, clean builds, Node unit tests, and Node contracts;
4. verifies staged ESM and CommonJS consumers;
5. runs portable contracts under Bun and Deno; and
6. runs browser contracts in Chromium, Firefox, and WebKit.

Preserve pinned actions, checksum-verified `wasm-pack` installation, runner
hardening, read-only permissions, and the separately visible JavaScript SDK
job when editing the workflow.

## Generated-boundary invariants

The generated package is an internal producer interface. The SDK must fail
safely when that protocol changes.

Maintain these invariants:

- validate required initializers and module output before creating a client;
- forget failed module initialization so a repaired loader can retry;
- validate optional service methods only when their public operation is used;
- preserve receiver binding when invoking generated methods;
- dispose generated result and context wrappers on success and failure;
- preserve the original conversion failure if disposal also fails;
- convert generated values into detached public data; and
- normalize raw failures without exposing secret-bearing messages or causes.

When Rust binding declarations change, update `src/engine/` first, then run the
full Node, portable-runtime, browser, and installed-consumer matrix.

## Configuration maintenance

`src/configuration/bootstrap.ts` maps typed SDK options to Cedarling bootstrap
properties. `src/configuration/prepare.ts` validates the mutually exclusive
typed and raw forms and derives immutable client capabilities.

When adding a typed option:

1. add it to the public configuration type;
2. add its accepted field to the fixed input catalogue;
3. validate and map it in the configuration modules;
4. add focused option tests for defaults, explicit values, and invalid input;
5. update the consumer README only when callers need the information; and
6. update this guide when the internal mapping or maintenance process changes.

Raw `bootstrapProperties` remain an advanced pass-through. Do not add SDK
aliases or silently inject typed defaults into that form.

## Maintainer checklist

- Public API change: update exported types, focused unit/contract coverage, and
  the consumer README.
- Generated binding change: update only the engine/runtime boundary first and
  run every qualification host.
- Runtime change: preserve one public package root and reuse portable suites.
- Packaging change: verify tarball contents, exact coordinated versions, ESM,
  CommonJS, and a clean installed consumer.
- Error change: preserve one error type and one error-code catalogue.
- Security-sensitive change: retain descriptor-safe input inspection,
  detachment, redaction, disposal, and fail-closed behavior.
- Handoff: run `npm run check`, Bun, Deno, and all three Playwright projects,
  then measure changed lines and files against the current `main` branch.
