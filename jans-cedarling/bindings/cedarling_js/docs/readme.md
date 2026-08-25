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
`createCedarling`. The root embeds the generated module and its bytes so
Node-family runtimes and browsers consume one self-contained artifact. Hosts
that require a precompiled WebAssembly module use the explicit `./edge`
subpath. Generated classes, methods, result wrappers, and disposal hooks must
never enter the public API.

Keep these responsibilities separate:

- configuration modules validate, normalize, and detach caller input;
- the client facade owns public services, `Result<T>`, and lifecycle behavior;
- the engine seam isolates generated binding behavior from the client;
- runtime loaders compile either embedded bytes or a host-provided precompiled
  module behind the same engine seam;
- generated adapters convert outputs to detached JavaScript-owned values;
- error modules own the single `CedarlingError` and `CedarlingErrorCode` model.

Do not add runtime selectors, further runtime-specific public subpaths,
feature-specific error classes, error aliases, or separate error-code
catalogues.

## Build outputs

```bash
npm run typecheck
npm run build
```

`build` removes stale `dist`, compiles declarations, embeds the generated module
and bytes in the root ESM and CommonJS artifacts, and creates the explicit edge
artifact with a colocated precompiled-module asset. The generated package is
not a runtime dependency.

The build also replaces wasm-bindgen's realm-sensitive module identity check
with a WebAssembly brand check. This is required when a host bundler creates the
precompiled module in another realm. The build fails if the generated glue no
longer contains the expected upstream pattern, and the edge output is rejected
if it retains any dynamic module-compilation path.

The root package export provides:

- root ESM through `dist/esm/index.js`;
- root CommonJS through `dist/cjs/index.cjs`; and
- precompiled-module ESM through `dist/edge/index.js`.

Root ESM and CommonJS must expose the same sole runtime value. The explicit
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

- Cloudflare Workers uses `@janssenproject/cedarling/edge`; local Workerd and a
  live Worker completed initialization, authorization, and shutdown.
- Vercel Edge uses `@janssenproject/cedarling/edge`; a Next.js production build
  and local edge route completed initialization, authorization, and shutdown.
  Live Vercel qualification is still pending. The currently measured 1.73 MB
  compressed function exceeds the Hobby limit, so Pro or Enterprise is
  required; Pro is the minimum suitable plan at the measured size. Recheck the
  built function size and current Vercel limits whenever the artifact changes.
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
the embedded loading architecture, package exports, or build output changes.
Consumers must not need custom handling for a separate WebAssembly asset.

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
CommonJS consumer verification. Run `npm run check:all` for the full Bun, Deno, Playwright-browser, and
Firefox ESR matrix after installing those runtimes, Playwright browsers,
Firefox ESR, and geckodriver. The command prepares artifacts once and reuses
them across every runtime.

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

The staging module copies only built SDK output and its consumer README,
preserves package dependency metadata, rejects non-exact dependency
specifications, and emits JSON describing the tarball path, version, and npm
SHA-512 integrity. Do not commit generated tarballs or staging directories.

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
