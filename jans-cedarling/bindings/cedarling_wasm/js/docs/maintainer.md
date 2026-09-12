# Maintaining the JavaScript package

This guide is for contributors who generate, build, validate, and release the
publishable `@janssenproject/cedarling_wasm` package. Application developers
should use the [consumer guide](../README.md); direct wasm-pack users should
use the [binding guide](../../README.md).

## Generated input and package output

Run this command from `jans-cedarling/bindings/cedarling_wasm` on a fresh checkout
and after relevant Rust source or dependency changes:

```sh
wasm-pack build --release --locked --target web --scope janssenproject
```

This generates `pkg/` in that directory, containing the JavaScript glue, TypeScript declarations, and WebAssembly binary.

From `js/`, the package build consumes `../pkg/`, uses `.build/` for intermediate output, and produces the publishable `dist/` directory below. It does not rebuild Rust:

```
dist/
├── browser/
├── esm/
├── cjs/
├── edge/
├── manual/
├── types/
│   ├── esm/
│   └── cjs/
└── wasm/
    └── cedarling_wasm_bg.wasm
```

The published package contains a single WebAssembly binary. The browser, Node.js, CommonJS, edge, and manual entry points use this same binary through their respective loading mechanisms.

**Important:**
Do not edit files in `pkg/` or `dist/` directly. These directories contain generated build artifacts and must be regenerated when the source changes.
Treat `js/` as the source of truth for handwritten package code, build configuration, and documentation.

## Build and qualify

Use Node.js 22, 24, or 26 and have `zip` on your PATH; verification builds policy archives from YAML fixtures.
From the same binding directory, install locked dependencies and browser engines:

```sh
cd js
npm ci --ignore-scripts
npx playwright install chromium firefox webkit
```

On Linux, add `--with-deps` to the Playwright command if browser system libraries are missing.

After generating `pkg/` and installing dependencies, run `npm run check` from
`js/` for complete package verification. Use the individual commands below for
targeted tasks:

| Command                  | Purpose                                                                                                                          |
| ------------------------ | -------------------------------------------------------------------------------------------------------------------------------- |
| `npm run format`         | Format handwritten package source, scripts, tests, configuration, and documentation.                                             |
| `npm run format:check`   | Check formatting without changing files.                                                                                         |
| `npm run lint`           | Check handwritten JavaScript scripts and tests for variable shadowing.                                                           |
| `npm run build`          | Assemble the shippable distribution from `../pkg/`.                                                                              |
| `npm test`               | Run focused initialization tests.                                                                                                |
| `npm run package:verify` | Build, pack, type-check, and execute installed ESM/CommonJS consumers; test edge packaging and automatic/manual browser loading. |
| `npm run check`          | Check formatting and lint, run runtime tests, and perform full packed-artifact verification.                                     |

Prettier is pinned locally for consistent CLI and editor formatting. Generated
output and the npm-managed lockfile are excluded; embedded examples are preserved.

ESLint checks handwritten `.mjs` files with `no-shadow`; generated files are
excluded. TypeScript source is checked by the compiler during the build.

`package:verify` verifies the installed tarball, not only the source checkout.
Test CI regenerates `pkg/`, installs locked dependencies and browser engines,
then runs `npm run check`, which includes this verification.

To stage a publishable archive from an existing build, run from `js/`, replacing `1.2.3` with the intended version:

```sh
node scripts/stage-packages.mjs --version 1.2.3 --publishable --output .build/packages
```

The source manifest stays private; `--publishable` removes that restriction only from the staged manifest.
The [release workflow](https://github.com/JanssenProject/jans/blob/main/.github/workflows/build-packages.yml) stages and publishes the archive separately from test CI.

## Public API boundary

The ESM-only `./manual` entry re-exports wasm-bindgen runtime values and
declarations directly. The root and `./edge` entries expose portable
initialization functions and generated types; the handwritten code adds only
portable WebAssembly loading for `init` and `initFromArchiveBytes`.

Callable JavaScript APIs use camelCase, including `jsonString()`.
Bootstrap properties retain uppercase `CEDARLING_*` keys; request JSON,
result/data fields, and serialization retain their generated names and shapes.
The automatic entries' `initWasm` and `initSync` are initialization adapters;
`free()` remains available on generated instances.

Use only package export-map paths in consumers: the root, `./edge`, `./manual`,
and `./wasm`. Files below `dist/` are package implementation details.
