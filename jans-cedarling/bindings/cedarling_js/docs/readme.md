# Cedarling JavaScript SDK maintainer guide

This guide covers source builds, architecture, tests, package qualification,
and CI for `jans-cedarling/bindings/cedarling_js`. The package
[README](../README.md) is the consumer guide and must remain focused on SDK
installation and usage.

## Prerequisites and build order

Source development requires:

- Node.js 22, 24, or 26 with npm;
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

The generated package is a development-only build input. Rebuild it whenever
the Rust binding changes, then reinstall SDK dependencies so local
qualification uses the current generated output.

## Project structure

```text
cedarling_js/
├── src/
│   ├── authorization/  Public request validation and authorization types
│   ├── client/         Public facade, services, and lifecycle coordination
│   ├── configuration/  Typed/raw configuration preparation
│   ├── context/        Context-store input and public types
│   ├── engine/         Generated-boundary adaptation and runtime loading
│   ├── edge.ts         Explicit edge package entry
│   ├── node.ts         Private Node-family package entry
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

Consumers normally import the package root and receive one runtime value:
`createCedarling`. The published package contains exactly one raw WebAssembly
binary under `dist/wasm`; private conditional exports select how each runtime
loads that same file. The browser loader accepts the byte or URL form emitted
by a qualified bundler, the Node-family loader reads the package-relative file,
and hosts that require a precompiled WebAssembly module use the explicit
`./edge` subpath. No consumer locates, copies, serves, or configures the binary.
Generated classes, methods, result wrappers, and disposal hooks must never
enter the public API.

Keep these responsibilities separate:

- configuration modules validate, normalize, and detach caller input;
- the client facade owns public services, `Result<T>`, and lifecycle behavior;
- the engine seam isolates generated binding behavior from the client;
- the browser loader compiles emitted bytes or privately retrieves an emitted
  asset URL, the Node loader reads the shared file, and the edge loader accepts
  a host-precompiled module behind the same engine seam;
- generated adapters convert outputs to detached JavaScript-owned values;
- error modules own the single `CedarlingError` and `CedarlingErrorCode` model.

Do not add further runtime-specific public subpaths, feature-specific error
classes, error aliases, or separate error-code catalogues. Private conditional
export branches are permitted only when they preserve the same package-root
API and isolate a verified runtime loading difference.

## Build outputs

```bash
npm run typecheck
npm run build
```

`build` removes stale `dist`, compiles declarations, bundles the generated glue,
and copies one generated binary to `dist/wasm`. It emits a browser root that
lets qualified bundlers turn that file into bytes or an internal URL, Node ESM
and CommonJS roots that read the same package-relative file, and an explicit
edge artifact that statically imports the file as a precompiled module. The
generated package is not a runtime dependency, and none of the JavaScript
outputs contains another copy of the WebAssembly payload.

The build also replaces wasm-bindgen's realm-sensitive module identity check
with a WebAssembly brand check. This is required when a host bundler creates the
precompiled module in another realm. The build fails if the generated glue no
longer contains the expected upstream pattern, and the edge output is rejected
if it retains any dynamic module-compilation path.

The package provides:

- browser-root ESM through `dist/browser/index.js`;
- Node-family root ESM through `dist/esm/index.js`;
- Node-family root CommonJS through `dist/cjs/index.cjs`;
- precompiled-module ESM through `dist/edge/index.js`;
- ESM and CommonJS declarations through `dist/types`; and
- their sole shared payload through `dist/wasm/cedarling_wasm_bg.wasm`.

Each runtime directory contains only its bundle and source map. Every JavaScript
entry must expose the same sole runtime value. The explicit
`./edge` subpath is ESM-only and does not support CommonJS `require` or legacy
TypeScript `node10` resolution. This boundary follows [Node.js conditional
exports](https://nodejs.org/api/packages.html#conditional-exports), [Vercel
Edge's ESM-only dependency and direct-`require`
rules](https://vercel.com/docs/functions/runtimes/edge#unsupported-apis), and
[Cloudflare Workers' static `.wasm`/`.wasm?module` import
contract](https://developers.cloudflare.com/workers/runtime-apis/webassembly/javascript/#bundling).
Accordingly, `arethetypeswrong` is expected to report those two unsupported
resolutions for `./edge`; its ESM and bundler resolutions must remain green.

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

## Runtime and platform qualification

Runtime support and platform qualification are related but distinct:

- a supported runtime executes the shared contract suites in repository CI;
- a qualified platform has also consumed an installed SDK package through its
  own build and loading model; and
- platform limits such as deployment size remain the consumer's responsibility
  even when the SDK loading path is compatible.

The maintained runtime matrix is:

| Environment                   | Package entry | Qualification                                                             |
| ----------------------------- | ------------- | ------------------------------------------------------------------------- |
| Node.js 22                    | package root  | Unit tests, real-WASM contracts, and installed ESM and CommonJS consumers |
| Node.js 24 and 26             | package root  | Installed ESM and CommonJS consumers                                      |
| Bun current stable            | package root  | Shared portable contracts                                                 |
| Deno LTS and current stable   | package root  | Shared portable contracts                                                 |
| Chromium, Firefox, and WebKit | package root  | Shared portable contracts in current Playwright browsers                  |
| Firefox ESR                   | package root  | Shared portable contracts in the current ESR release                      |

The following platform paths were additionally qualified with an installed SDK
package:

- Cloudflare Workers uses `@janssenproject/cedarling/edge`; the current exact
  package completed initialization, authorization, and shutdown in local
  Workerd and a live Worker deployment.
- Vercel Edge uses `@janssenproject/cedarling/edge`; a Next.js production build
  and local edge route completed initialization, authorization, and shutdown.
  Live Vercel qualification is still pending. A prior candidate measured 1.73
  MB compressed and exceeded the Hobby limit, so a paid plan may be required.
  Recheck the built function size and current limits whenever the artifact changes.

Electron is not a separately qualified platform. Its main process and renderer
use the already-tested Node.js and browser package paths, but application
packaging and OS sandbox behavior remain outside this SDK matrix.

Do not turn a one-time platform qualification into an unconditional provider,
framework, plan, or version claim. Record the exact toolchain and deployment
evidence when repeating one of these checks.

## Bundler qualification

An installed package has completed browser initialization, authorization, and
shutdown after production builds with:

- esbuild 0.28.2;
- Vite 8.2.2, which uses Rolldown;
- webpack 5.109.2; and
- Next.js 16.3.2 with Turbopack for the qualified edge route.

These are maintainer qualification results.
They establish representative compatibility but do not prove compatibility
with every bundler or configuration. In particular, the Vite 8 result qualifies
Rolldown, not standalone Rollup. Repeat the installed-package bundler lab when
the shared loading architecture, package exports, or build output changes.
The browser entry combines
[esbuild's byte import](https://esbuild.github.io/content-types/#binary),
[webpack's bytes asset type](https://webpack.js.org/guides/asset-modules/),
and [Vite's explicit WebAssembly URL
form](https://vite.dev/guide/features#webassembly).
Consumers must not need custom handling for the package-internal WebAssembly
asset.

## npm scripts

Run all commands from `jans-cedarling/bindings/cedarling_js` after completing
the [prerequisites and build order](#prerequisites-and-build-order). Commands
ending in `:run` consume artifacts prepared by `npm run test:prepare`; their
paired commands prepare those artifacts first. CI prepares once and invokes the
`:run` commands so each runtime qualifies the identical output.

### Build and cleanup

| Script | Usage | Purpose |
| --- | --- | --- |
| `clean` | `npm run clean` | Removes all generated distribution and test output: `dist` and `.build`. |
| `clean:tests` | `npm run clean:tests` | Removes only compiled test and browser-runner output below `.build`. |
| `build` | `npm run build` | Cleans, emits declarations, creates package bundles, and copies the one shared WebAssembly asset. |
| `typecheck` | `npm run typecheck` | Type-checks source without writing output. |

### Test preparation

| Script | Usage | Purpose |
| --- | --- | --- |
| `test:compile` | `npm run test:compile` | Cleans compiled tests and emits the runtime-neutral test tree. |
| `test:prepare` | `npm run test:prepare` | Builds the SDK and compiles tests once for the `:run` test commands. |

### Node.js tests

| Script | Usage | Purpose |
| --- | --- | --- |
| `test:unit:run` | `npm run test:unit:run` after `test:prepare` | Runs unit suites against controlled engine fixtures. |
| `test:unit` | `npm run test:unit` | Prepares output, then runs the unit suites. |
| `test:contract:run` | `npm run test:contract:run` after `test:prepare` | Runs Node contract suites against the generated package. |
| `test:contract` | `npm run test:contract` | Prepares output, then runs Node contract suites. |

### Portable-runtime tests

| Script | Usage | Purpose |
| --- | --- | --- |
| `test:portable:bun:run` | `npm run test:portable:bun:run` after `test:prepare` | Runs portable contract suites with Bun. |
| `test:portable:bun` | `npm run test:portable:bun` | Prepares output, then runs portable contracts with Bun. |
| `test:portable:deno:run` | `npm run test:portable:deno:run` after `test:prepare` | Runs portable contracts with the minimum Deno permissions needed by the qualification harness. |
| `test:portable:deno` | `npm run test:portable:deno` | Prepares output, then runs portable contracts with Deno. |

### Browser tests

| Script | Usage | Purpose |
| --- | --- | --- |
| `test:browser:build` | `npm run test:browser:build` after `test:prepare` | Bundles the portable browser runner into `.build/browser`. |
| `test:browser:run` | `npm run test:browser:run` after `test:prepare` and `test:browser:build` | Runs the browser bundle in Playwright Chromium, Firefox, and WebKit. |
| `test:browser` | `npm run test:browser` | Prepares and bundles output, then runs the Playwright browser matrix. |
| `test:browser:firefox-esr:run` | `CEDARLING_FIREFOX_ESR_BINARY=/path/to/firefox-esr npm run test:browser:firefox-esr:run` after `test:prepare` and `test:browser:build` | Runs portable contracts in stock Firefox ESR through geckodriver. |
| `test:browser:firefox-esr` | `CEDARLING_FIREFOX_ESR_BINARY=/path/to/firefox-esr npm run test:browser:firefox-esr` | Prepares and bundles output, then runs Firefox ESR qualification. |

### Package qualification

| Script | Usage | Purpose |
| --- | --- | --- |
| `package:stage` | `npm run package:stage -- --output ./artifacts --version 1.0.0` | Builds and creates a private, exact-version SDK tarball for inspection or local use. |
| `package:verify` | `npm run package:verify` | Builds, stages, and verifies a clean installed ESM and CommonJS consumer. |

### Quality gates

| Script | Usage | Purpose |
| --- | --- | --- |
| `check` | `npm run check` | Runs type-checking, the Node unit and contract suites, and installed-consumer verification. |
| `check:all` | `CEDARLING_FIREFOX_ESR_BINARY=/path/to/firefox-esr npm run check:all` | Runs `check` plus Bun, Deno, Playwright-browser, and Firefox ESR qualification. |

## Local verification

Prepare production and test output once when running individual compiled
groups:

```bash
npm run test:prepare
npm run test:unit:run
npm run test:contract:run
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
CommonJS consumer verification. Compiled SDK modules, test runners, and the
browser-test bundle live under `.build/src`, `.build/tests`, and `.build/browser`.
Run `npm run check:all` for the full Bun, Deno, Playwright-browser, and
Firefox ESR matrix after installing those runtimes, Playwright browsers,
Firefox ESR, and geckodriver. The command prepares artifacts once and reuses
them across every runtime.

Qualify the portable contract list in other installed runtimes. Each command
rebuilds its prerequisites:

```bash
npm run test:portable:bun
npm run test:portable:deno
```

The Deno runner grants read access only to
`dist/wasm/cedarling_wasm_bg.wasm`. Its `--allow-env` permission belongs to the
QUnit qualification harness; consumer applications need read access to the
installed SDK asset, plus only the permissions their own code requires. CI
selects the latest stable Bun release and reports the resolved version so a
runtime change is visible in the job log.

Install the Playwright browsers once, then run browser qualification:

```bash
npm exec -- playwright install chromium firefox webkit
npm run test:browser
```

`test:browser` rebuilds SDK and test output, bundles only the portable suite
graph, and runs Chromium, Firefox, and WebKit.

Firefox ESR qualification uses stock Firefox through geckodriver because
Playwright requires its own patched Firefox build. Put geckodriver on `PATH`,
set `CEDARLING_FIREFOX_ESR_BINARY` to the ESR executable, then run
`npm run test:browser:firefox-esr`. `check:all` uses the same variable.

## Package staging and consumer verification

Local staging is private by default so a verification or demo tarball cannot be
published accidentally. Stage one exact version with:

```bash
npm run package:stage -- --output ./artifacts --version 1.0.0
```

The package manifest explicitly allowlists the runtime bundles, their source
maps, both declaration trees, the CommonJS package marker, and the shared WASM.
The staging module preserves package dependency metadata, rejects non-exact
dependency specifications, and emits JSON describing the tarball path, version,
and npm SHA-512 integrity. It also rejects intermediate JavaScript and requires
exactly one WebAssembly file at `dist/wasm/cedarling_wasm_bg.wasm`. Do not commit
generated tarballs or staging directories.

`npm run package:verify` stages a private tarball, installs it into a clean
offline consumer, checks ESM and CommonJS types and resolution, rejects a
separate generated-package installation, and executes real authorization. The
release workflow uses the same verifier's external-artifact mode against the
exact publishable tarball:

```bash
node scripts/stage-packages.mjs \
  --output ./artifacts --version 1.0.0 --publishable
node scripts/verify-consumer.mjs \
  --artifact ./artifacts/janssenproject-cedarling-1.0.0.tgz --version 1.0.0
```

`--publishable` is reserved for the release workflow; it omits the private
marker but does not publish by itself. CI publishes, signs, uploads, and computes
SLSA provenance from that one verified tarball. A rerun may reuse an existing
npm version only when the registry integrity exactly matches the staged
artifact; all other publication and release-upload failures are fatal.

## CI coverage

The `cedarling_js_tests` job in `.github/workflows/test-cedarling.yml`:

1. builds the current generated binding from the checked-out Rust source;
2. installs dependencies from `package-lock.json`;
3. prepares the immutable production, test, and browser artifacts once;
4. runs Node unit tests and contracts on Node.js 22;
5. verifies installed ESM and CommonJS consumers on Node.js 22, 24, and 26;
6. runs portable contracts under the current Bun release and under Deno LTS
   and latest stable; and
7. runs browser contracts in the current Chromium, Firefox, and WebKit; and
8. runs the same contracts in Mozilla's latest Firefox ESR through WebDriver.

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
- Runtime change: reuse portable suites and add a public subpath only for a
  genuinely distinct host loading contract.
- Packaging change: verify tarball contents, exact versions, ESM,
  CommonJS, and a clean installed consumer.
- Error change: preserve one error type and one error-code catalogue.
- Security-sensitive change: retain descriptor-safe input inspection,
  detachment, redaction, disposal, and fail-closed behavior.
- Handoff: run `npm run check`, Bun, Deno, and all three Playwright projects,
  then measure changed lines and files against the current `main` branch.
